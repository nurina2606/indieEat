package com.example.indieeat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

public class LoginActivity extends AppCompatActivity {

    EditText editEmail, editPassword;
    ImageView passwordToggle;
    Button loginButton;
    FirebaseAuth auth;

    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // UI components
        editEmail = findViewById(R.id.loginEmail);
        editPassword = findViewById(R.id.loginPassword);
        passwordToggle = findViewById(R.id.passwordToggle);
        loginButton = findViewById(R.id.loginButton);
        auth = FirebaseAuth.getInstance();

        // Toggle password visibility
        passwordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggle.setImageResource(R.drawable.eyen); // Closed eye icon
                isPasswordVisible = false;
            } else {
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggle.setImageResource(R.drawable.eye); // Open eye icon
                isPasswordVisible = true;
            }
            editPassword.setSelection(editPassword.getText().length());
        });

        // Login button click handler
        loginButton.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = auth.getCurrentUser().getUid();
                            Log.d("LOGIN_UID", "UID: " + uid);

                            DatabaseReference userRef = FirebaseDatabase.getInstance()
                                    .getReference("users")  // ✅ must match your Firebase node
                                    .child(uid)
                                    .child("role");

                            userRef.get().addOnCompleteListener(roleTask -> {
                                if (roleTask.isSuccessful()) {
                                    if (roleTask.getResult().exists()) {
                                        String role = roleTask.getResult().getValue(String.class);
                                        Log.d("LOGIN_ROLE", "Fetched role: " + role);
                                        Toast.makeText(this, "Welcome " + role, Toast.LENGTH_SHORT).show();

                                        if ("Admin".equalsIgnoreCase(role)) {
                                            startActivity(new Intent(LoginActivity.this, HomepageActivity2.class));
                                        } else {
                                            startActivity(new Intent(LoginActivity.this, HomepageActivity.class));
                                        }
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Role not found in database", Toast.LENGTH_LONG).show();
                                        Log.e("LOGIN_ERROR", "Role field is missing in Firebase for UID: " + uid);
                                    }
                                } else {
                                    Toast.makeText(this, "Error fetching role: " + roleTask.getException(), Toast.LENGTH_LONG).show();
                                    Log.e("LOGIN_ERROR", "Failed to fetch role", roleTask.getException());
                                }
                            });

                        } else {
                            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                            Log.e("LOGIN_ERROR", "Authentication failed", task.getException());
                        }
                    });
        });
    }
}
