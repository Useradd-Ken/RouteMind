package com.example.routemind;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
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
import java.util.concurrent.TimeUnit;

public class TripActivity extends AppCompatActivity {

    private static final String TAG = "TripActivity";
    AutoCompleteTextView etDestination;
    EditText etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip, btnConfirmTrip;
    ImageView btnBack;
    LinearLayout llPlanTrip, llLoading, llResult;
    TextView tvGeneratedItinerary;
    DatabaseHelper dbHelper;
    private static final String PREF_NAME = "BudgetPrefs";
    
    // Updated Key and Model Configuration
    private static final String GEMINI_API_KEY = "AIzaSyADZfoNfykxPDCL8-x3Z4thbog60BICQnk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        etDestination = findViewById(R.id.etDestination);
        etStartDate   = findViewById(R.id.etStartDate);
        etEndDate     = findViewById(R.id.etEndDate);
        etBudget      = findViewById(R.id.etBudget);
        etInterests   = findViewById(R.id.etInterests);
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        btnConfirmTrip = findViewById(R.id.btnConfirmTrip);
        btnBack       = findViewById(R.id.btn_back);
        
        llPlanTrip    = findViewById(R.id.llPlanTrip);
        llLoading     = findViewById(R.id.llLoading);
        llResult      = findViewById(R.id.llResult);
        tvGeneratedItinerary = findViewById(R.id.tvGeneratedItinerary);
        
        dbHelper      = new DatabaseHelper(this);

        // Use Photon AI for smarter destination autocomplete
        HomePage.PhotonAutocompleteAdapter adapter = new HomePage.PhotonAutocompleteAdapter(this);
        etDestination.setAdapter(adapter);
        etDestination.setThreshold(3);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnCreateTrip.setOnClickListener(v -> {
            Log.d(TAG, "Generate Itinerary button clicked");
            String destination = etDestination.getText().toString();
            String startDateStr = etStartDate.getText().toString();
            String endDateStr   = etEndDate.getText().toString();
            String budget      = etBudget.getText().toString();
            String interests   = etInterests.getText().toString();

            if (destination.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
                Toast.makeText(TripActivity.this, "Please fill in Destination and Dates", Toast.LENGTH_SHORT).show();
                return;
            }

            long days = calculateDays(startDateStr, endDateStr);
            if (days < 1) {
                Toast.makeText(TripActivity.this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                return;
            }

            generateAIItinerary(destination, startDateStr, endDateStr, budget, interests, days);
        });

        btnConfirmTrip.setOnClickListener(v -> {
            String destination = etDestination.getText().toString();
            String startDateStr = etStartDate.getText().toString();
            String endDateStr   = etEndDate.getText().toString();
            String budget      = etBudget.getText().toString();
            String interests   = etInterests.getText().toString();
            String itinerary   = tvGeneratedItinerary.getText().toString();
            long days = calculateDays(startDateStr, endDateStr);

            saveAndMoveToBudget(destination, startDateStr, endDateStr, budget, interests, days, itinerary);
        });

        setupNavigation();
    }

    private void generateAIItinerary(String dest, String start, String end, String budget, String interests, long days) {
        Log.d(TAG, "Requesting AI Itinerary for " + dest + " (" + days + " days)");
        llPlanTrip.setVisibility(View.GONE);
        llLoading.setVisibility(View.VISIBLE);

        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE));

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.7f;
        GenerationConfig config = configBuilder.build();

        // Use v1 endpoint to avoid 404s common with older SDK defaults
        // Fixed: RequestOptions constructor requires (Long timeout, String apiVersion) in Java
        RequestOptions requestOptions = new RequestOptions(null, "v1");
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY, config, safetySettings, requestOptions);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = "Act as a professional travel guide. Create a detailed " + days + "-day travel itinerary for " + dest + 
                        " from " + start + " to " + end + 
                        ". My budget is " + (budget.isEmpty() ? "standard" : "PHP " + budget) + 
                        " and I am interested in " + (interests.isEmpty() ? "sightseeing and food" : interests) + 
                        ". Format the output clearly with Day 1, Day 2, etc.";

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
                            Log.d(TAG, "AI Itinerary generated successfully");
                            llLoading.setVisibility(View.GONE);
                            llResult.setVisibility(View.VISIBLE);
                            tvGeneratedItinerary.setText(resultText);
                        } else {
                            handleAIError("AI returned an empty response.");
                        }
                    } catch (Exception e) {
                        handleAIError("Error processing AI response: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "AI Generation failed", t);
                runOnUiThread(() -> handleAIError(t.getMessage()));
            }
        }, executor);
    }

    private void handleAIError(String error) {
        llLoading.setVisibility(View.GONE);
        llPlanTrip.setVisibility(View.VISIBLE);
        String message = "AI Error: " + error;
        if (error != null && error.contains("404")) {
            message = "AI Model not found. Please check API Key configuration.";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void saveAndMoveToBudget(String destination, String startDateStr, String endDateStr, String budget, String interests, long days, String itinerary) {
        double budgetValue = 0;
        if (!budget.isEmpty()) {
            try {
                budgetValue = Double.parseDouble(budget);
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong("totalBudget", Double.doubleToLongBits(budgetValue));
                editor.apply();
            } catch (NumberFormatException ignored) {}
        }

        dbHelper.saveTrip(destination, startDateStr, endDateStr, budget, interests, itinerary);
        Toast.makeText(TripActivity.this, "Trip Saved Successfully!", Toast.LENGTH_LONG).show();
        
        Intent intent = new Intent(TripActivity.this, BudgetTracker.class);
        intent.putExtra("DESTINATION", destination);
        intent.putExtra("START_DATE", startDateStr);
        intent.putExtra("END_DATE", endDateStr);
        intent.putExtra("BUDGET", budgetValue);
        intent.putExtra("INTERESTS", interests);
        intent.putExtra("DURATION_DAYS", days);
        startActivity(intent);
        finish();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_maps);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomePage.class));
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(this, BudgetTracker.class));
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(this, TripHistory.class));
                finish();
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(this, UserProfile.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private long calculateDays(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            if (d1 != null && d2 != null) {
                long diff = d2.getTime() - d1.getTime();
                return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void showDatePicker(EditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    editText.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }
}
