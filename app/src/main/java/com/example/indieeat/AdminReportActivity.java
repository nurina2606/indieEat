package com.example.indieeat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class AdminReportActivity extends AppCompatActivity {

    private RecyclerView userRecyclerView;
    private ReportAdapter userAdapter;

    private DatabaseReference userRef;

    private ImageButton homeButton, reportButton, profileButton, logoutButton;
    private LinearLayout userReportBox;

    private static final String TAG = "AdminReportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report);

        userRecyclerView = findViewById(R.id.userRecyclerView);
        userReportBox = findViewById(R.id.userReportBox);

        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new ReportAdapter(this, this::refreshUsers);
        userRecyclerView.setAdapter(userAdapter);

        userRef = FirebaseDatabase.getInstance().getReference("users");

        loadReports(userAdapter, userRecyclerView);

        // Bottom navigation
        homeButton = findViewById(R.id.homeButton);
        reportButton = findViewById(R.id.reportButton);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        homeButton.setOnClickListener(view -> {
            startActivity(new Intent(AdminReportActivity.this, HomepageActivity2.class));
            finish();
        });

        reportButton.setOnClickListener(view -> {
            startActivity(new Intent(AdminReportActivity.this, AdminReportActivity.class));
            finish();
        });

        profileButton.setOnClickListener(view -> {
            startActivity(new Intent(AdminReportActivity.this, AdminProfileActivity.class));
            finish();
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(AdminReportActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminReportActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadReports(ReportAdapter adapter, RecyclerView recyclerView) {
        Log.d(TAG, "Loading user reports");

        userRef.orderByChild("role").equalTo("User")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<UserModel> reportList = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            UserModel user = child.getValue(UserModel.class);
                            if (user != null) {
                                reportList.add(user);
                            }
                        }

                        if (reportList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            Toast.makeText(AdminReportActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                        } else {
                            adapter.setUserList(reportList);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase error: " + error.getMessage());
                        Toast.makeText(AdminReportActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void refreshUsers() {
        loadReports(userAdapter, userRecyclerView);
    }
}
