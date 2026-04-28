package com.example.routemind;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TripActivity extends AppCompatActivity {

    private static final String TAG = "TripActivity";
    AutoCompleteTextView etDestination;
    EditText etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip, btnExportPdf, btnConfirmTrip;
    LinearLayout llPlanTrip, llLoading, llResult;
    TextView tvGeneratedItinerary;
    DatabaseHelper dbHelper;

    private GenerativeModelFutures model;

    private static final String[] DESTINATIONS = new String[] {
            "Manila", "Cebu", "Davao", "Palawan", "Bohol", "Boracay",
            "Baguio", "Vigan", "Siargao", "Zamboanga", "Legazpi",
            "Puerto Princesa", "Tagaytay", "Dumaguete", "Iloilo"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        dbHelper = new DatabaseHelper(this);
        
        // Fix: Use the correct field access for GenerationConfig.Builder
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.7f;
        GenerationConfig config = configBuilder.build();

        // Initialize Gemini Model
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash", 
                "AIzaSyCB0MXC5mXtboF3O1MsCEalyFZiDIwvWjk",
                config
        );
        model = GenerativeModelFutures.from(gm);

        etDestination = findViewById(R.id.etDestination);
        etStartDate   = findViewById(R.id.etStartDate);
        etEndDate     = findViewById(R.id.etEndDate);
        etBudget      = findViewById(R.id.etBudget);
        etInterests   = findViewById(R.id.etInterests);
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        btnExportPdf  = findViewById(R.id.btnExportPdf);
        btnConfirmTrip = findViewById(R.id.btnConfirmTrip);
        
        llPlanTrip    = findViewById(R.id.llPlanTrip);
        llLoading     = findViewById(R.id.llLoading);
        llResult      = findViewById(R.id.llResult);
        tvGeneratedItinerary = findViewById(R.id.tvGeneratedItinerary);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DESTINATIONS);
        etDestination.setAdapter(adapter);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnCreateTrip.setOnClickListener(v -> {
            String dest = etDestination.getText().toString();
            String start = etStartDate.getText().toString();
            String end = etEndDate.getText().toString();
            String budget = etBudget.getText().toString();
            String interests = etInterests.getText().toString();
            
            if (dest.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Please fill in Destination and Dates", Toast.LENGTH_SHORT).show();
                return;
            }
            
            generateRealAIItinerary(dest, start, end, budget, interests);
        });

        btnConfirmTrip.setOnClickListener(v -> confirmAndSaveTrip());

        setupNavigation();
    }

    private void generateRealAIItinerary(String destination, String start, String end, String budget, String interests) {
        llPlanTrip.setVisibility(View.GONE);
        llLoading.setVisibility(View.VISIBLE);

        String prompt = "Create a professional 3-day travel itinerary for " + destination + 
                        " from " + start + " to " + end + 
                        ". Budget is " + (budget.isEmpty() ? "flexible" : budget) + 
                        " and interests: " + (interests.isEmpty() ? "sightseeing" : interests) + 
                        ". Format with Day 1, Day 2, Day 3 headers.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    try {
                        String resultText = result.getText();
                        if (resultText != null && !resultText.isEmpty()) {
                            llLoading.setVisibility(View.GONE);
                            llResult.setVisibility(View.VISIBLE);
                            tvGeneratedItinerary.setText(resultText);
                        } else {
                            throw new Exception("AI returned empty text");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Process Success Error: ", e);
                        handleError("Response was blocked or empty. Try a different destination.");
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "AI Generation Error: ", t);
                runOnUiThread(() -> handleError(t.getMessage()));
            }
        }, executor);
    }

    private void handleError(String message) {
        llLoading.setVisibility(View.GONE);
        llPlanTrip.setVisibility(View.VISIBLE);
        
        String displayMsg = "AI Error: " + (message != null ? message : "Unknown error");
        if (displayMsg.contains("Unexpected Response")) {
            displayMsg = "API Key Error: Ensure 'Generative Language API' is enabled in Google Cloud Console for this key.";
        }
        
        Toast.makeText(TripActivity.this, displayMsg, Toast.LENGTH_LONG).show();
    }

    private void confirmAndSaveTrip() {
        String destination = etDestination.getText().toString();
        String startDate = etStartDate.getText().toString();
        String endDate = etEndDate.getText().toString();
        String budget = etBudget.getText().toString();
        String interests = etInterests.getText().toString();
        String itinerary = tvGeneratedItinerary.getText().toString();
        
        long id = dbHelper.saveTrip(destination, startDate, endDate, budget, interests, itinerary);

        if (id != -1) {
            Toast.makeText(this, "Trip Confirmed! Saved to Database.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, TripHistory.class));
            finish();
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_maps);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomePage.class));
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), BudgetTracker.class));
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                return true;
            }
            return false;
        });
    }

    private void showDatePicker(EditText editText) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, day) -> editText.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
}
