package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FinalItineraryActivity extends AppCompatActivity {

    private RecyclerView rvFinalItinerary;
    private MaterialButton btnProceedToRating;
    private FinalItineraryAdapter adapter;
    private ArrayList<ItineraryItem> itemsToAdjust;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private static final String API_KEY = BuildConfig.apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_itinerary);

        rvFinalItinerary = findViewById(R.id.rv_final_itinerary);
        btnProceedToRating = findViewById(R.id.btn_proceed_to_rating);
        rvFinalItinerary.setLayoutManager(new LinearLayoutManager(this));

        // Get the items that were NOT removed from the previous screen
        itemsToAdjust = (ArrayList<ItineraryItem>) getIntent().getSerializableExtra("final_items");

        if (itemsToAdjust != null && !itemsToAdjust.isEmpty()) {
            adjustTimesWithAI();
        } else {
            Toast.makeText(this, "No activities remaining in your itinerary", Toast.LENGTH_SHORT).show();
        }

        btnProceedToRating.setOnClickListener(v -> {
            Intent intent = new Intent(FinalItineraryActivity.this, ItineraryDetails.class);
            startActivity(intent);
        });

        setupBottomNavigation();
    }

    private void adjustTimesWithAI() {
        runOnUiThread(() -> Toast.makeText(this, "Finalizing your adjusted schedule...", Toast.LENGTH_SHORT).show());
        
        backgroundExecutor.execute(() -> {
            StringBuilder itemsText = new StringBuilder();
            for (ItineraryItem item : itemsToAdjust) {
                itemsText.append("Day ").append(item.getDay())
                        .append(" | ").append(item.getTitle())
                        .append(" | ").append(item.getLocation())
                        .append("\n");
            }

            String prompt = "You are a professional travel planner. Below is a list of activities for a trip. " +
                    "Please adjust the times for each activity to create a logical, realistic daily schedule (start from morning, include gaps for travel). " +
                    "STRICTLY keep activities on their original assigned days. " +
                    "Return the result in exactly this format: Day [Num] | [Time] | [Cost PHP/Free] | [Title] | [Location]. One item per line.\n\n" +
                    "Activities to schedule:\n" + itemsText.toString();

            try {
                // Using gemini-1.5-flash for reliability
                GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);

                Content content = new Content.Builder().addText(prompt).build();
                ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

                responseFuture.addListener(() -> {
                    try {
                        GenerateContentResponse response = responseFuture.get();
                        String resultText = response.getText();
                        if (resultText != null) {
                            parseAndDisplayAdjustedItinerary(resultText);
                        } else {
                            runOnUiThread(this::displayOriginalItems);
                        }
                    } catch (Exception e) {
                        Log.e("AI_ADJUST_ERROR", "Error: " + e.getMessage());
                        runOnUiThread(this::displayOriginalItems);
                    }
                }, backgroundExecutor);
            } catch (Exception e) {
                Log.e("AI_ADJUST_ERROR", "Initialization Error: " + e.getMessage());
                runOnUiThread(this::displayOriginalItems);
            }
        });
    }

    private void parseAndDisplayAdjustedItinerary(String text) {
        List<ItineraryItem> adjustedItems = new ArrayList<>();
        String[] lines = text.split("\n");
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
                        adjustedItems.add(new ItineraryItem(day, time, cost, title, location));
                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", "Line format error: " + line);
                    }
                }
            }
        }
        runOnUiThread(() -> {
            if (adjustedItems.isEmpty()) {
                displayOriginalItems();
            } else {
                adapter = new FinalItineraryAdapter(adjustedItems);
                rvFinalItinerary.setAdapter(adapter);
            }
        });
    }

    private void displayOriginalItems() {
        adapter = new FinalItineraryAdapter(itemsToAdjust);
        rvFinalItinerary.setAdapter(adapter);
        Toast.makeText(this, "Showing unadjusted itinerary due to scheduling error.", Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNavigation() {
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
}
