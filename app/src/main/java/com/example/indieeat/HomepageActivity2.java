package com.example.indieeat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class HomepageActivity2 extends AppCompatActivity {

    private ImageButton homeButton, reportButton, profileButton;
    private TextView userCountTextView, adminCountTextView;
    private BarChart foodLogBarChart;

    private FirebaseUser currentUser;
    private DatabaseReference usersRef;

    private final String[] foodCategories = {"chai", "chapati", "dhokla", "jalebi", "kulfi"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage2);

        // Initialize UI components
        homeButton = findViewById(R.id.homeButton);
        reportButton = findViewById(R.id.reportButton);
        profileButton = findViewById(R.id.profileButton);
        userCountTextView = findViewById(R.id.userCountTextView);
        adminCountTextView = findViewById(R.id.adminCountTextView);
        foodLogBarChart = findViewById(R.id.foodLogBarChart);

        // 🔴 Logout button setup
        ImageButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(HomepageActivity2.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomepageActivity2.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        fetchUserAndAdminCounts();
        fetchFoodLogData();

        homeButton.setOnClickListener(v -> {
            // Already on Homepage
        });

        reportButton.setOnClickListener(v -> {
            startActivity(new Intent(HomepageActivity2.this, AdminReportActivity.class));
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(HomepageActivity2.this, AdminProfileActivity.class));
        });
    }

    private void fetchUserAndAdminCounts() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int userCount = 0;
                int adminCount = 0;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Object roleObj = userSnapshot.child("role").getValue();
                    if (roleObj != null) {
                        String role = roleObj.toString().trim();
                        if (role.equalsIgnoreCase("User")) {
                            userCount++;
                        } else if (role.equalsIgnoreCase("Admin")) {
                            adminCount++;
                        }
                    } else {
                        Log.w("ROLE_NULL", "Missing role for user: " + userSnapshot.getKey());
                    }
                }

                userCountTextView.setText("Users: " + userCount);
                adminCountTextView.setText("Admins: " + adminCount);
                Log.d("ROLE_COUNT", "Users: " + userCount + ", Admins: " + adminCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userCountTextView.setText("Error loading data");
                adminCountTextView.setText("");
                Log.e("FIREBASE_COUNT", "Failed to fetch counts: " + error.getMessage());
            }
        });
    }

    private void fetchFoodLogData() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int[] foodCounts = new int[foodCategories.length];

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (!userSnapshot.hasChild("calorieIntake")) continue;

                    for (DataSnapshot dateSnapshot : userSnapshot.child("calorieIntake").getChildren()) {
                        for (DataSnapshot foodSnapshot : dateSnapshot.getChildren()) {
                            Object nameObj = foodSnapshot.child("food").getValue();
                            if (nameObj == null) continue;

                            String foodName = nameObj.toString().toLowerCase().trim();
                            Log.d("FOOD_NAME_FOUND", "User: " + userSnapshot.getKey() + ", Food: " + foodName);

                            for (int i = 0; i < foodCategories.length; i++) {
                                if (foodName.equals(foodCategories[i])) {
                                    foodCounts[i]++;
                                    break;
                                }
                            }
                        }
                    }
                }

                Log.d("FOOD_COUNTS", java.util.Arrays.toString(foodCounts));
                setupBarChart(foodCounts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomepageActivity2.this, "Failed to load food log data", Toast.LENGTH_SHORT).show();
                Log.e("FOOD_LOG_ERROR", error.getMessage());
            }
        });
    }

    private void setupBarChart(int[] foodCounts) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < foodCounts.length; i++) {
            entries.add(new BarEntry(i, foodCounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(new int[]{
                R.color.purple_200,
                R.color.teal_200,
                R.color.pink,
                R.color.blue,
                R.color.yellow
        }, this);
        dataSet.setDrawValues(false); // Hide values above bars

        BarData barData = new BarData(dataSet);
        foodLogBarChart.setData(barData);
        foodLogBarChart.getDescription().setEnabled(false);
        foodLogBarChart.getLegend().setEnabled(false);

        // ✅ Animate chart
        foodLogBarChart.animateY(1000);

        // ✅ X-Axis setup
        XAxis xAxis = foodLogBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(foodCategories));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setDrawGridLines(false); // ✅ Remove vertical grid lines

        // ✅ Y-Axis setup
        YAxis leftAxis = foodLogBarChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setDrawGridLines(false); // ✅ Remove horizontal grid lines

        foodLogBarChart.getAxisRight().setEnabled(false); // ✅ Hide right Y-axis

        foodLogBarChart.setFitBars(true); // Ensure bars fit chart
        foodLogBarChart.invalidate();     // Refresh chart
    }
}
