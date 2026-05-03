package com.example.routemind;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TripActivity extends AppCompatActivity {

    private static final String TAG = "TripActivity";
    AutoCompleteTextView etDestination;
    EditText etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip, btnConfirmTrip, btnExportPdf;
    LinearLayout llPlanTrip, llLoading, llResult;
    TextView tvGeneratedItinerary;
    DatabaseHelper dbHelper;

    private GenerativeModelFutures model;
    private String generatedItineraryText = "";

    // Gemini API Key provided by user
    private static final String GEMINI_API_KEY = "AIzaSyCNTfZbubk2FKaqhkBGVeGa9kXdXIQZQaw";
    private static final String PREF_NAME = "BudgetPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        setupAI();

        // Fetch destinations from SQLite DB
        List<String> destinations = dbHelper.getAllDestinations();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, destinations);
        etDestination.setAdapter(adapter);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnCreateTrip.setOnClickListener(v -> generateItinerary());

        btnConfirmTrip.setOnClickListener(v -> saveTripAndContinue());
        
        btnExportPdf.setOnClickListener(v -> 
            Toast.makeText(this, "Exporting to PDF feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        setupNavigation();
    }

    private void initializeViews() {
        etDestination = findViewById(R.id.etDestination);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etBudget = findViewById(R.id.etBudget);
        etInterests = findViewById(R.id.etInterests);
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        
        llPlanTrip = findViewById(R.id.llPlanTrip);
        llLoading = findViewById(R.id.llLoading);
        llResult = findViewById(R.id.llResult);
        tvGeneratedItinerary = findViewById(R.id.tvGeneratedItinerary);
        btnConfirmTrip = findViewById(R.id.btnConfirmTrip);
        btnExportPdf = findViewById(R.id.btnExportPdf);
    }

    private void setupAI() {
        try {
            GenerativeModel generativeModel = new GenerativeModel(
                    "gemini-1.5-flash",
                    GEMINI_API_KEY
            );
            model = GenerativeModelFutures.from(generativeModel);
        } catch (Exception e) {
            Log.e(TAG, "AI Setup error", e);
        }
    }

    private void generateItinerary() {
        String destination = etDestination.getText().toString().trim();
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();
        String budget = etBudget.getText().toString().trim();
        String interests = etInterests.getText().toString().trim();

        if (destination.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please fill in Destination and Dates", Toast.LENGTH_SHORT).show();
            return;
        }

        long days = getDaysBetween(startDate, endDate);
        if (days <= 0) {
            Toast.makeText(this, "Check your dates: End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show Loading UI
        llPlanTrip.setVisibility(View.GONE);
        llLoading.setVisibility(View.VISIBLE);
        llResult.setVisibility(View.GONE);

        String prompt = String.format(Locale.getDefault(),
                "Create a detailed %d-day travel itinerary for %s with a budget of ₱%s. " +
                "Interests: %s. " +
                "Format the response clearly with daily sections (Morning, Afternoon, Evening). " +
                "Include estimated costs for activities if possible. Keep it concise.",
                days, destination, budget, interests.isEmpty() ? "General sightseeing" : interests);

        Content content = new Content.Builder().addText(prompt).build();
        
        try {
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    runOnUiThread(() -> {
                        try {
                            generatedItineraryText = result.getText();
                            if (generatedItineraryText == null || generatedItineraryText.isEmpty()) {
                                tvGeneratedItinerary.setText("AI could not generate an itinerary. It might have been blocked by safety filters or returned an empty response.");
                            } else {
                                tvGeneratedItinerary.setText(generatedItineraryText);
                            }
                            llLoading.setVisibility(View.GONE);
                            llResult.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            onFailure(e);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    runOnUiThread(() -> {
                        llLoading.setVisibility(View.GONE);
                        llPlanTrip.setVisibility(View.VISIBLE);
                        String errorMsg = "AI Error: " + t.getMessage();
                        Log.e(TAG, "AI Error details", t);
                        
                        // Check if it's an API Key or model issue
                        if (errorMsg.contains("API_KEY_INVALID") || errorMsg.contains("403")) {
                            errorMsg = "Invalid API Key or Gemini API not enabled in Google Cloud Console.";
                        } else if (errorMsg.contains("500") || errorMsg.contains("Unexpected API response")) {
                            errorMsg = "Gemini Server Error. Please try again later or check your network.";
                        }
                        
                        Toast.makeText(TripActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    });
                }
            }, this.getMainExecutor());
        } catch (Exception e) {
            llLoading.setVisibility(View.GONE);
            llPlanTrip.setVisibility(View.VISIBLE);
            Toast.makeText(this, "AI Execution error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTripAndContinue() {
        String destination = etDestination.getText().toString();
        String startDate = etStartDate.getText().toString();
        String endDate = etEndDate.getText().toString();
        String budget = etBudget.getText().toString();

        // Save to Database
        dbHelper.addTrip(destination, startDate, endDate, generatedItineraryText);

        // Save Budget to Preferences
        if (!budget.isEmpty()) {
            try {
                double budgetValue = Double.parseDouble(budget);
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                sharedPreferences.edit().putLong("totalBudget", Double.doubleToLongBits(budgetValue)).apply();
            } catch (NumberFormatException ignored) {}
        }

        Toast.makeText(this, "Trip Saved Successfully!", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(this, BudgetTracker.class);
        startActivity(intent);
        finish();
    }

    private long getDaysBetween(String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            long diff = d2.getTime() - d1.getTime();
            return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_maps);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomePage.class));
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), BudgetTracker.class));
                finish();
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                finish();
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                finish();
                return true;
            }
            return id == R.id.nav_maps;
        });
    }

    private void showDatePicker(EditText editText) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            editText.setText(day + "/" + (month + 1) + "/" + year);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
}
