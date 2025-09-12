package com.example.indieeat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private EditText username, phone, email, password;
    private MaterialButton editButton, saveButton;
    private ImageButton homeButton, reportButton, profileButton, logoutButton;

    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        TextView title = findViewById(R.id.profileTitle);
        title.setText("Admin Profile Page");

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        // UI elements
        username = findViewById(R.id.username);
        phone = findViewById(R.id.phone);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);

        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        setFieldsEnabled(false);
        saveButton.setEnabled(false);

        loadProfileData(); // Load admin data from Firebase

        editButton.setOnClickListener(v -> {
            isEditing = true;
            setFieldsEnabled(true);
            saveButton.setEnabled(true);
            editButton.setEnabled(false);
        });

        saveButton.setOnClickListener(v -> {
            isEditing = false;
            setFieldsEnabled(false);
            saveButton.setEnabled(false);
            editButton.setEnabled(true);
            updateProfileData();
        });

        // Bottom Nav
        homeButton = findViewById(R.id.homeButton);
        reportButton = findViewById(R.id.reportButton);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton); // ✅ Added logout button

        homeButton.setOnClickListener(view -> {
            startActivity(new Intent(AdminProfileActivity.this, HomepageActivity2.class));
            finish();
        });

        reportButton.setOnClickListener(view -> {
            startActivity(new Intent(AdminProfileActivity.this, AdminReportActivity.class));
            finish();
        });

        profileButton.setOnClickListener(view -> {
            startActivity(new Intent(AdminProfileActivity.this, AdminProfileActivity.class));
            finish();
        });

        // ✅ Logout functionality
        logoutButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(AdminProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        username.setEnabled(enabled);
        phone.setEnabled(enabled);
        email.setEnabled(enabled);
        password.setEnabled(enabled);
    }

    private void loadProfileData() {
        if (currentUser != null) {
            String uid = currentUser.getUid();

            userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String phoneVal = snapshot.child("phone").getValue(String.class);
                        String emailVal = snapshot.child("email").getValue(String.class);
                        String passwordVal = snapshot.child("password").getValue(String.class);

                        username.setText(name);
                        phone.setText(phoneVal);
                        email.setText(emailVal);
                        password.setText(passwordVal);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(AdminProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateProfileData() {
        if (currentUser != null) {
            String uid = currentUser.getUid();

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", username.getText().toString().trim());
            updates.put("phone", phone.getText().toString().trim());
            updates.put("email", email.getText().toString().trim());
            updates.put("password", password.getText().toString().trim());

            userRef.child(uid).updateChildren(updates)
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        }
    }
}
