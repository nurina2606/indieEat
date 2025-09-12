package com.example.indieeat;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.core.graphics.drawable.DrawableCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomepageActivity extends AppCompatActivity {

   private ProgressBar calorieProgressBar;
   private TextView calorieText, bmiSummaryTextView, calorieSummaryTextView, exceededTextView;
   private Spinner monthSpinner;
   private BarChart weeklyChart;
   private LineChart yearlyChart;
   private ImageButton logoutButton;

   private FirebaseAuth mAuth;
   private DatabaseReference userRef;
   private String userId;
   private int calorieGoal = 2000;
   private int dailyCalories = 0;
   private double bmi = 0.0;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_homepage);
      setTitle("Homepage");

      mAuth = FirebaseAuth.getInstance();
      userId = mAuth.getCurrentUser().getUid();
      userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

      calorieProgressBar = findViewById(R.id.calorieProgressBar);
      calorieText = findViewById(R.id.calorieText);
      bmiSummaryTextView = findViewById(R.id.bmiSummaryTextView);
      calorieSummaryTextView = findViewById(R.id.calorieSummaryTextView);
      exceededTextView = findViewById(R.id.exceededTextView);
      monthSpinner = findViewById(R.id.monthSpinner);
      weeklyChart = findViewById(R.id.weeklyChart);
      yearlyChart = findViewById(R.id.yearlyChart);
      logoutButton = findViewById(R.id.logoutButton); // Make sure this ID exists in XML

      logoutButton.setOnClickListener(v -> {
         FirebaseAuth.getInstance().signOut();
         Toast.makeText(HomepageActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
         Intent intent = new Intent(HomepageActivity.this, MainActivity.class);
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
         startActivity(intent);
         finish();
      });

      findViewById(R.id.homeButton).setOnClickListener(v -> {});
      findViewById(R.id.foodButton).setOnClickListener(v -> startActivity(new Intent(this, RecognitionActivity.class)));
      findViewById(R.id.profileButton).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

      fetchUserData();
      setupMonthSpinner();
      loadYearlyChart();
   }

   private void fetchUserData() {
      userRef.addListenerForSingleValueEvent(new ValueEventListener() {
         @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
            try {
               float weight = Float.parseFloat(snapshot.child("weight").getValue(String.class));
               float height = Float.parseFloat(snapshot.child("height").getValue(String.class));
               int age = Integer.parseInt(snapshot.child("age").getValue(String.class));
               String gender = snapshot.child("gender").getValue(String.class);
               String activity = snapshot.child("activityLevel").getValue(String.class);
               String goal = snapshot.child("goals").getValue(String.class);

               bmi = weight / (float) Math.pow(height / 100.0, 2);
               bmiSummaryTextView.setText(String.format("BMI: %.2f", bmi));

               calorieGoal = estimateCalories(gender, weight, height, age, activity, goal);
               calorieSummaryTextView.setText("Estimated Daily Calories: " + calorieGoal + " kcal");
               calorieProgressBar.setMax(calorieGoal);

               fetchTodayCalories();
            } catch (Exception e) {
               bmiSummaryTextView.setText("BMI: N/A");
               calorieSummaryTextView.setText("Calories: N/A");
            }
         }

         @Override public void onCancelled(@NonNull DatabaseError error) {}
      });
   }

   private void fetchTodayCalories() {
      DatabaseReference intakeRef = FirebaseDatabase.getInstance()
              .getReference("users")
              .child(userId)
              .child("calorieIntake");

      String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

      intakeRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
         @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
            int total = 0;
            for (DataSnapshot entry : snapshot.getChildren()) {
               Integer cal = entry.child("calories").getValue(Integer.class);
               if (cal != null) total += cal;
            }
            dailyCalories = total;
            updateProgressBar();
         }

         @Override public void onCancelled(@NonNull DatabaseError error) {}
      });
   }

   private void updateProgressBar() {
      calorieProgressBar.setProgress(dailyCalories, false); // Disable animation
      calorieText.setText(dailyCalories + " kcal");

      if (dailyCalories == 0) {
         // Gray progress
         calorieProgressBar.setProgressTintList(getResources().getColorStateList(R.color.gray));
      } else if (dailyCalories > calorieGoal) {
         // Red if exceeded
         calorieProgressBar.setProgressTintList(getResources().getColorStateList(R.color.red));
         exceededTextView.setVisibility(View.VISIBLE);
         exceededTextView.setText("You have exceeded today calories");
      } else {
         // Green if within limit
         calorieProgressBar.setProgressTintList(getResources().getColorStateList(R.color.green));
         exceededTextView.setVisibility(View.GONE);
      }

      // Always keep the background (track) gray
      calorieProgressBar.setProgressBackgroundTintList(getResources().getColorStateList(R.color.light_gray));
   }



   private void setupMonthSpinner() {
      monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String month = parent.getItemAtPosition(position).toString();
            loadWeeklyChart(month);
         }

         @Override public void onNothingSelected(AdapterView<?> parent) {}
      });
   }

   private void loadWeeklyChart(String monthName) {
      DatabaseReference calRef = FirebaseDatabase.getInstance()
              .getReference("users")
              .child(userId)
              .child("calorieIntake");

      Map<String, String> monthMap = new HashMap<>();
      monthMap.put("January", "01"); monthMap.put("February", "02"); monthMap.put("March", "03");
      monthMap.put("April", "04"); monthMap.put("May", "05"); monthMap.put("June", "06");
      monthMap.put("July", "07"); monthMap.put("August", "08"); monthMap.put("September", "09");
      monthMap.put("October", "10"); monthMap.put("November", "11"); monthMap.put("December", "12");

      String selectedMonth = monthMap.get(monthName);
      if (selectedMonth == null) return;

      calRef.addListenerForSingleValueEvent(new ValueEventListener() {
         @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
            Map<Integer, Integer> weeklyTotals = new HashMap<>();

            for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
               String dateKey = dateSnapshot.getKey();
               if (dateKey != null && dateKey.split("-")[1].equals(selectedMonth)) {
                  int week = getWeekOfMonth(dateKey);
                  for (DataSnapshot entry : dateSnapshot.getChildren()) {
                     Integer cal = entry.child("calories").getValue(Integer.class);
                     if (cal != null)
                        weeklyTotals.put(week, weeklyTotals.getOrDefault(week, 0) + cal);
                  }
               }
            }

            List<BarEntry> entries = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
               entries.add(new BarEntry(i, weeklyTotals.getOrDefault(i, 0)));
            }

            BarDataSet dataSet = new BarDataSet(entries, "");
            dataSet.setColor(getResources().getColor(R.color.teal_200));
            dataSet.setDrawValues(false);

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.5f);
            weeklyChart.setData(data);

            weeklyChart.animateY(1000);
            weeklyChart.getLegend().setEnabled(false);
            weeklyChart.getDescription().setEnabled(false);
            weeklyChart.setDrawGridBackground(false);
            weeklyChart.getAxisRight().setEnabled(false);

            XAxis xAxis = weeklyChart.getXAxis();
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new ValueFormatter() {
               @Override public String getFormattedValue(float value) {
                  return "Week " + ((int) value);
               }
            });

            YAxis leftAxis = weeklyChart.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            leftAxis.setGranularity(1f);

            weeklyChart.invalidate();
         }

         private int getWeekOfMonth(String dateStr) {
            try {
               SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
               Date date = format.parse(dateStr);
               Calendar cal = Calendar.getInstance();
               cal.setTime(date);
               return cal.get(Calendar.WEEK_OF_MONTH);
            } catch (Exception e) {
               return 1;
            }
         }

         @Override public void onCancelled(@NonNull DatabaseError error) {}
      });
   }

   private void loadYearlyChart() {
      DatabaseReference calRef = FirebaseDatabase.getInstance()
              .getReference("users")
              .child(userId)
              .child("calorieIntake");

      calRef.addListenerForSingleValueEvent(new ValueEventListener() {
         @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
            int[] monthlyTotals = new int[12];

            for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
               String dateKey = dateSnapshot.getKey();
               int monthIndex = getMonthIndex(dateKey);
               for (DataSnapshot entry : dateSnapshot.getChildren()) {
                  Integer cal = entry.child("calories").getValue(Integer.class);
                  if (cal != null) {
                     monthlyTotals[monthIndex] += cal;
                  }
               }
            }

            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
               entries.add(new Entry(i + 1, monthlyTotals[i]));
            }

            LineDataSet dataSet = new LineDataSet(entries, "");
            dataSet.setColor(getResources().getColor(R.color.teal_200));
            dataSet.setCircleColor(getResources().getColor(R.color.teal_200));
            dataSet.setDrawValues(false);

            LineData data = new LineData(dataSet);
            yearlyChart.setData(data);

            yearlyChart.animateY(1000);
            yearlyChart.getLegend().setEnabled(false);
            yearlyChart.getDescription().setEnabled(false);
            yearlyChart.setDrawGridBackground(false);
            yearlyChart.getAxisRight().setEnabled(false);

            XAxis xAxis = yearlyChart.getXAxis();
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new ValueFormatter() {
               private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
               @Override public String getFormattedValue(float value) {
                  int index = (int) value - 1;
                  return (index >= 0 && index < months.length) ? months[index] : "";
               }
            });

            YAxis leftAxis = yearlyChart.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            leftAxis.setGranularity(1f);

            yearlyChart.invalidate();
         }

         @Override public void onCancelled(@NonNull DatabaseError error) {}
      });
   }

   private int getMonthIndex(String dateStr) {
      try {
         return Integer.parseInt(dateStr.split("-")[1]) - 1;
      } catch (Exception e) {
         return 0;
      }
   }

   private int estimateCalories(String gender, float weight, float height, int age,
                                String activityLevel, String goal) {
      double bmr = gender.equalsIgnoreCase("Male")
              ? 10 * weight + 6.25 * height - 5 * age + 5
              : 10 * weight + 6.25 * height - 5 * age - 161;

      double multiplier = 1.2;
      if ("Lightly Active".equalsIgnoreCase(activityLevel)) multiplier = 1.375;
      else if ("Moderately Active".equalsIgnoreCase(activityLevel)) multiplier = 1.55;
      else if ("Very Active".equalsIgnoreCase(activityLevel)) multiplier = 1.725;
      else if ("Super Active".equalsIgnoreCase(activityLevel)) multiplier = 1.9;

      double total = bmr * multiplier;

      if ("Lose Weight".equalsIgnoreCase(goal)) total -= 500;
      else if ("Gain Muscle".equalsIgnoreCase(goal)) total += 500;

      return (int) total;
   }
}
