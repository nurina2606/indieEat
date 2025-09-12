package com.example.indieeat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class SignupActivity extends AppCompatActivity {

    EditText editUsername, editPhone, editEmail, editPassword, editConfirmPassword;
    ImageView passwordToggle, confirmPasswordToggle;
    Button signupButton;

    FirebaseAuth auth;
    DatabaseReference userRef;

    boolean isPasswordVisible = false;
    boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Bind views
        editUsername = findViewById(R.id.editUsername);
        editPhone = findViewById(R.id.editPhone);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        passwordToggle = findViewById(R.id.passwordToggle);
        confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle);
        signupButton = findViewById(R.id.signupButton);

        auth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        // Password toggle logic
        passwordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggle.setImageResource(R.drawable.eyen);
                isPasswordVisible = false;
            } else {
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggle.setImageResource(R.drawable.eye);
                isPasswordVisible = true;
            }
            editPassword.setSelection(editPassword.getText().length());
        });

        confirmPasswordToggle.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                editConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                confirmPasswordToggle.setImageResource(R.drawable.eyen);
                isConfirmPasswordVisible = false;
            } else {
                editConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                confirmPasswordToggle.setImageResource(R.drawable.eye);
                isConfirmPasswordVisible = true;
            }
            editConfirmPassword.setSelection(editConfirmPassword.getText().length());
        });

        // Signup logic
        signupButton.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString();
            String confirmPassword = editConfirmPassword.getText().toString();

            if (username.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            String defaultRole = "User"; // Default role for all new signups

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser != null) {
                                String uid = firebaseUser.getUid();
                                User user = new User(username, phone, email, password, defaultRole);
                                userRef.child(uid).setValue(user)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignupActivity.this, ProfileActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                            Log.e("SignupDebug", "Firebase write failed: ", e);
                                        });
                            }
                        } else {
                            Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("SignupDebug", "Firebase auth failed: ", task.getException());
                        }
                    });
        });
    }

    public static class User {
        public String username, phone, email, password, role;

        public User() {}

        public User(String username, String phone, String email, String password, String role) {
            this.username = username;
            this.phone = phone;
            this.email = email;
            this.password = password;
            this.role = role;
        }
    }
}
