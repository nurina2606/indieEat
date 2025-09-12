package com.example.indieeat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton profileButton, foodButton, homeButton, logoutButton;
    private EditText usernameEditText, phoneEditText, emailEditText, passwordEditText;
    private EditText weightEditText, heightEditText, bmiEditText, ageEditText;
    private Spinner genderSpinner, activityLevelSpinner, goalsSpinner;
    private Button editButton, saveButton;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setTitle("Profile");

        // Bottom navigation + logout
        profileButton = findViewById(R.id.profileButton);
        foodButton = findViewById(R.id.foodButton);
        homeButton = findViewById(R.id.homeButton);
        logoutButton = findViewById(R.id.logoutButton); // ← Make sure this exists in the layout

        profileButton.setOnClickListener(v ->
                Toast.makeText(this, "You're already on Profile", Toast.LENGTH_SHORT).show());

        foodButton.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, RecognitionActivity.class)));

        homeButton.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, HomepageActivity.class)));

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Inputs
        usernameEditText = findViewById(R.id.username);
        phoneEditText = findViewById(R.id.phone);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        weightEditText = findViewById(R.id.weight);
        heightEditText = findViewById(R.id.height);
        bmiEditText = findViewById(R.id.bmi);
        ageEditText = findViewById(R.id.age);
        genderSpinner = findViewById(R.id.genderSpinner);
        activityLevelSpinner = findViewById(R.id.activitySpinner);
        goalsSpinner = findViewById(R.id.goalSpinner);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        // Spinners
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this, R.array.gender_options, R.layout.spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> activityAdapter = ArrayAdapter.createFromResource(
                this, R.array.activity_level_options, R.layout.spinner_item);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityLevelSpinner.setAdapter(activityAdapter);

        ArrayAdapter<CharSequence> goalsAdapter = ArrayAdapter.createFromResource(
                this, R.array.goal_options, R.layout.spinner_item);
        goalsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goalsSpinner.setAdapter(goalsAdapter);

        // Load and disable
        setEditable(false);
        loadUserProfile();

        editButton.setOnClickListener(v -> {
            setEditable(true);
            saveButton.setEnabled(true);
        });

        saveButton.setOnClickListener(v -> {
            saveUserProfile();
            setEditable(false);
            saveButton.setEnabled(false);
        });
    }

    private void setEditable(boolean enabled) {
        usernameEditText.setEnabled(enabled);
        phoneEditText.setEnabled(enabled);
        emailEditText.setEnabled(enabled);
        passwordEditText.setEnabled(enabled);
        weightEditText.setEnabled(enabled);
        heightEditText.setEnabled(enabled);
        ageEditText.setEnabled(enabled);
        genderSpinner.setEnabled(enabled);
        activityLevelSpinner.setEnabled(enabled);
        goalsSpinner.setEnabled(enabled);
        bmiEditText.setEnabled(false);
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();

        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String weight = snapshot.child("weight").getValue(String.class);
                    String height = snapshot.child("height").getValue(String.class);

                    usernameEditText.setText(snapshot.child("username").getValue(String.class));
                    phoneEditText.setText(snapshot.child("phone").getValue(String.class));
                    emailEditText.setText(snapshot.child("email").getValue(String.class));
                    passwordEditText.setText(snapshot.child("password").getValue(String.class));
                    weightEditText.setText(weight);
                    heightEditText.setText(height);
                    ageEditText.setText(snapshot.child("age").getValue(String.class));

                    setSpinnerValue(genderSpinner, snapshot.child("gender").getValue(String.class));
                    setSpinnerValue(activityLevelSpinner, snapshot.child("activityLevel").getValue(String.class));
                    setSpinnerValue(goalsSpinner, snapshot.child("goals").getValue(String.class));

                    calculateAndDisplayBMI(weight, height);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile() {
        String userId = auth.getCurrentUser().getUid();

        String username = usernameEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String weight = weightEditText.getText().toString();
        String height = heightEditText.getText().toString();
        String age = ageEditText.getText().toString();
        String gender = genderSpinner.getSelectedItem().toString();
        String activityLevel = activityLevelSpinner.getSelectedItem().toString();
        String goals = goalsSpinner.getSelectedItem().toString();

        calculateAndDisplayBMI(weight, height);

        User user = new User(username, phone, email, password, weight, height, age, gender, activityLevel, goals);

        databaseReference.child(userId).setValue(user).addOnSuccessListener(aVoid ->
                Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(ProfileActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void calculateAndDisplayBMI(String weightStr, String heightStr) {
        try {
            float weight = Float.parseFloat(weightStr);
            float heightCm = Float.parseFloat(heightStr);
            float heightM = heightCm / 100f;
            float bmi = weight / (heightM * heightM);
            bmiEditText.setText(String.format("%.2f", bmi));
        } catch (Exception e) {
            bmiEditText.setText("N/A");
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) spinner.setSelection(position);
    }

    public static class User {
        public String username, phone, email, password, weight, height, age, gender, activityLevel, goals;

        public User() {}

        public User(String username, String phone, String email, String password,
                    String weight, String height, String age,
                    String gender, String activityLevel, String goals) {
            this.username = username;
            this.phone = phone;
            this.email = email;
            this.password = password;
            this.weight = weight;
            this.height = height;
            this.age = age;
            this.gender = gender;
            this.activityLevel = activityLevel;
            this.goals = goals;
        }
    }
}
