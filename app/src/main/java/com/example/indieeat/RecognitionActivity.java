package com.example.indieeat;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;

public class RecognitionActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 1001;

    private ImageView imageView;
    private TextView resultText;
    private Button captureButton, saveCalButton, retakeButton;
    private ImageButton homeButton, profileButton, foodButton, logoutButton;

    private Uri imageUri;
    private File imageFile;

    private String[] classNames;
    private final Map<String, String> offlineCalories = new HashMap<>();

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private String currentCalories;
    private String predictedFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        captureButton = findViewById(R.id.captureButton);
        homeButton = findViewById(R.id.homeButton);
        profileButton = findViewById(R.id.profileButton);
        foodButton = findViewById(R.id.foodButton);
        logoutButton = findViewById(R.id.logoutButton);
        saveCalButton = findViewById(R.id.saveCalButton);
        retakeButton = findViewById(R.id.retakeButton);

        saveCalButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.GONE);

        // Load dynamic labels from file
        classNames = loadClassLabels();

        // Optionally map known calorie values
        offlineCalories.put("chai", "30 kcal");
        offlineCalories.put("chapati", "104 kcal");
        offlineCalories.put("dhokla", "160 kcal");
        offlineCalories.put("jalebi", "150 kcal");
        offlineCalories.put("kulfi", "130 kcal");

        homeButton.setOnClickListener(v -> {
            startActivity(new Intent(this, HomepageActivity.class));
            finish();
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        foodButton.setOnClickListener(v ->
                Toast.makeText(this, "You're already on Food Recognition", Toast.LENGTH_SHORT).show()
        );

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RecognitionActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        captureButton.setOnClickListener(v -> openCamera());
        retakeButton.setOnClickListener(v -> resetUI());
        saveCalButton.setOnClickListener(v -> saveDailyCalories());
    }

    private String[] loadClassLabels() {
        List<String> labels = new ArrayList<>();

        try {
            InputStream is = getAssets().open("label_classes.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    labels.add(line.trim());
                } else {
                    // You can handle empty lines if needed
                }
            }

            reader.close();

            if (!labels.isEmpty()) {
                return labels.toArray(new String[0]);
            } else {
                Toast.makeText(this, "Label file is empty", Toast.LENGTH_SHORT).show();
                return new String[]{"unknown"};
            }

        } catch (IOException e) {
            Toast.makeText(this, "Error loading class labels", Toast.LENGTH_SHORT).show();
            return new String[]{"unknown"};
        }
    }


    private void openCamera() {
        Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            imageFile = createImageFile();
            imageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", imageFile);
            camIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(camIntent, CAMERA_REQUEST_CODE);
        } catch (IOException e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return File.createTempFile("IMG_" + ts, ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == CAMERA_REQUEST_CODE && res == Activity.RESULT_OK && imageFile != null && imageFile.exists()) {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(imageFile));
                imageView.setImageBitmap(bmp);
                classifyImage(bmp);
                saveCalButton.setVisibility(View.VISIBLE);
                retakeButton.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                resultText.setText("❌ Image error: " + e.getMessage());
            }
        } else {
            resultText.setText("❌ Camera canceled or failed.");
        }
    }

    private MappedByteBuffer loadModel() throws IOException {
        AssetFileDescriptor fd = getAssets().openFd("food.tflite");
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel fc = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getLength());
    }

    private void classifyImage(Bitmap bitmap) {
        try {
            Bitmap rsz = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
            float[][][][] input = new float[1][224][224][3];
            for (int x = 0; x < 224; x++) {
                for (int y = 0; y < 224; y++) {
                    int px = rsz.getPixel(x, y);
                    input[0][x][y][0] = ((px >> 16) & 0xFF) / 255f;
                    input[0][x][y][1] = ((px >> 8) & 0xFF) / 255f;
                    input[0][x][y][2] = (px & 0xFF) / 255f;
                }
            }
            Interpreter itp = new Interpreter(loadModel());
            float[][] out = new float[1][classNames.length];
            itp.run(input, out);
            float[] conf = out[0];
            int mi = 0;
            float maxc = 0;
            for (int i = 0; i < conf.length; i++) {
                if (conf[i] > maxc) {
                    maxc = conf[i];
                    mi = i;
                }
            }
            String res = classNames[mi];
            predictedFood = res;
            currentCalories = offlineCalories.getOrDefault(res, "0 kcal");

            resultText.setText(String.format("🍽️ Result: %s\n📊 %.2f%%\n🔥 Calories: %s",
                    res, maxc * 100, currentCalories));

        } catch (Exception e) {
            resultText.setText("❌ Error: " + e.getMessage());
        }
    }

    private void saveDailyCalories() {
        if (currentCalories == null || predictedFood == null) {
            Toast.makeText(this, "Nothing to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int calValue = Integer.parseInt(currentCalories.replace("kcal", "").trim());
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String foodUid = userRef.child("calorieIntake").child(today).push().getKey();

            Map<String, Object> data = new HashMap<>();
            data.put("food", predictedFood);
            data.put("calories", calValue);
            data.put("timestamp", System.currentTimeMillis());

            userRef.child("calorieIntake")
                    .child(today)
                    .child(foodUid)
                    .setValue(data)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Saved " + predictedFood + ": " + calValue + " kcal", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomepageActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Error saving calories.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetUI() {
        imageView.setImageResource(android.R.color.transparent);
        resultText.setText("");
        saveCalButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.GONE);
    }
}
