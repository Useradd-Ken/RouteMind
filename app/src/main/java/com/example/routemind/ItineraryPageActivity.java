package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ItineraryPageActivity extends AppCompatActivity {

    private RecyclerView rvItinerary;
    private LinearLayout layoutDaySelector;
    private View loadingLayout;
    private View contentLayout;

    private List<ItineraryItem> allItems = new ArrayList<>();
    private ItineraryAdapter adapter;
    private int selectedDay = 1;
    private DatabaseHelper dbHelper;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    private static final String PREF_NAME = "BudgetPrefs";
    private static final String API_KEY = BuildConfig.apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary_page);

        dbHelper = new DatabaseHelper(this);
        
        layoutDaySelector = findViewById(R.id.layout_day_selector);
        rvItinerary = findViewById(R.id.rv_itinerary);
        loadingLayout = findViewById(R.id.loading_layout);
        contentLayout = findViewById(R.id.content_layout);
        Button btnGenerateGuide = findViewById(R.id.btn_generate_guide);
        
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));

        // Start loading data in the background to avoid ANR "failed to complete startup"
        backgroundExecutor.execute(this::loadData);

        btnGenerateGuide.setOnClickListener(v -> {
            ArrayList<ItineraryItem> selectedItems = new ArrayList<>();
            ArrayList<ItineraryItem> remainingItems = new ArrayList<>();
            for (ItineraryItem item : allItems) {
                if (item.isSelected()) {
                    selectedItems.add(item);
                } else {
                    remainingItems.add(item);
                }
            }

            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Please select at least one activity to remove", Toast.LENGTH_SHORT).show();
            } else {
                // Remove selected items from database
                backgroundExecutor.execute(() -> {
                    for (ItineraryItem item : selectedItems) {
                        dbHelper.deleteItineraryItem(item.getId());
                    }
                    runOnUiThread(() -> {
                        Intent intent = new Intent(ItineraryPageActivity.this, FinalItineraryActivity.class);
                        // Pass the ones that were NOT selected (remainingItems) to the final itinerary
                        intent.putExtra("final_items", remainingItems);
                        startActivity(intent);
                        // Refresh the current list to show items are removed
                        loadData();
                    });
                });
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_maps);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomePage.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), BudgetTracker.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadData() {
        allItems = dbHelper.getAllItineraryItems();
        runOnUiThread(() -> {
            if (adapter == null) {
                adapter = new ItineraryAdapter(filterItemsByDay(selectedDay));
                rvItinerary.setAdapter(adapter);
            } else {
                adapter.setItineraryItems(filterItemsByDay(selectedDay));
            }
            setupDaySelector();

            // Auto-generate if empty after initial load
            if (allItems.isEmpty()) {
                generateAIItinerary();
            }
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                loadingLayout.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);
            } else {
                loadingLayout.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void generateAIItinerary() {
        showLoading(true);
        backgroundExecutor.execute(() -> {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            final String destination = prefs.getString("destination", "Cebu");
            String startDate = prefs.getString("startDate", "");
            String endDate = prefs.getString("endDate", "");
            long budgetLong = prefs.getLong("totalBudget", Double.doubleToLongBits(0.0));
            double budget = Double.longBitsToDouble(budgetLong);
            String interests = prefs.getString("interests", "");

            int finalDays = calculateDays(startDate, endDate);

            // Database read moved to background thread
            String placesContext = dbHelper.getPlacesContext();

            String prompt = "Create a " + finalDays + "-day " + destination + " itinerary (Budget: " + budget + " PHP). " +
                    "Interests: " + interests + ". " +
                    "Include these places if possible: " + placesContext + "." +
                    "Format: Day [Num] | [Time] | [Cost PHP/Free] | [Title] | [Location]. One item per line.";

            try {

                GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY);
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);

                Content content = new Content.Builder()
                        .addText(prompt)
                        .build();

                ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);
                responseFuture.addListener(() -> {
                    try {
                        GenerateContentResponse response = responseFuture.get();
                        if (response != null && response.getText() != null) {
                            backgroundExecutor.execute(() -> parseAndSaveItinerary(response.getText()));
                        } else {
                            showError("AI returned an empty response.");
                            showLoading(false);
                        }
                    } catch (Exception e) {
                        Log.e("AI_ERROR", "Error generating itinerary: " + e.getMessage(), e);
                        showError("AI error: " + e.getLocalizedMessage());
                        showLoading(false);
                    }
                }, backgroundExecutor);
            } catch (Exception e) {
                Log.e("AI_ERROR", "Error initializing GenerativeModel: " + e.getMessage(), e);
                showError("AI initialization error.");
                showLoading(false);
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private int calculateDays(String start, String end) {
        if (start == null || start.isEmpty() || end == null || end.isEmpty()) return 3;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            if (d1 == null || d2 == null) return 3;

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(d1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(d2);
            endCal.set(Calendar.HOUR_OF_DAY, 0);
            endCal.set(Calendar.MINUTE, 0);
            endCal.set(Calendar.SECOND, 0);
            endCal.set(Calendar.MILLISECOND, 0);

            if (startCal.after(endCal)) return 1;

            int daysCount = 0;
            while (!startCal.after(endCal)) {
                daysCount++;
                startCal.add(Calendar.DAY_OF_YEAR, 1);
            }
            return daysCount;
        } catch (ParseException e) {
            Log.e("DATE_ERROR", "Failed to parse dates: " + start + ", " + end);
            return 3;
        }
    }

    private void parseAndSaveItinerary(String text) {
        dbHelper.clearItinerary();
        String[] lines = text.split("\n");
        boolean hasItems = false;
        for (String line : lines) {
            if (line.contains("|")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    try {
                        String dayPart = parts[0].replaceAll("[^0-9]", "").trim();
                        if (dayPart.isEmpty()) continue;
                        int day = Integer.parseInt(dayPart);
                        String time = parts[1].trim();
                        String cost = parts[2].trim();
                        String title = parts[3].trim();
                        String location = parts[4].trim();
                        dbHelper.addItineraryItem(new ItineraryItem(day, time, cost, title, location));
                        hasItems = true;
                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", "Failed to parse line: " + line);
                    }
                }
            }
        }
        if (hasItems) {
            loadData(); // This will trigger runOnUiThread and UI update
            showLoading(false);
        } else {
            Log.e("AI_RESPONSE", "No valid itinerary items found in response: " + text);
            showError("AI generated an invalid itinerary format.");
            showLoading(false);
        }
    }

    private void setupDaySelector() {
        layoutDaySelector.removeAllViews();
        List<Integer> days = new ArrayList<>();
        for (ItineraryItem item : allItems) {
            if (!days.contains(item.getDay())) {
                days.add(item.getDay());
            }
        }
        days.sort(Integer::compareTo);

        if (days.isEmpty()) return;

        for (int day : days) {
            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            btn.setLayoutParams(params);
            btn.setText("Day " + day);

            if (day == selectedDay) {
                btn.setBackgroundColor(getResources().getColor(R.color.primary_color, getTheme()));
                btn.setTextColor(getResources().getColor(R.color.white, getTheme()));
            }

            btn.setOnClickListener(v -> {
                selectedDay = day;
                adapter.setItineraryItems(filterItemsByDay(selectedDay));
                setupDaySelector();
            });

            layoutDaySelector.addView(btn);
        }
    }

    private List<ItineraryItem> filterItemsByDay(int day) {
        List<ItineraryItem> filtered = new ArrayList<>();
        for (ItineraryItem item : allItems) {
            if (item.getDay() == day) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
