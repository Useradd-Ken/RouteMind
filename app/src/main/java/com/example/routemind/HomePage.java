package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";
    private LinearLayout resultsContainer;
    private TextView tvSectionTitle;
    private View planTripCard;
    private EditText searchDestinations;
    
    private GenerativeModelFutures aiModel;
    private DatabaseHelper dbHelper;

    // Gemini API Key (Verified working in TripActivity)
    private static final String GEMINI_API_KEY = "AIzaSyCNTfZbubk2FKaqhkBGVeGa9kXdXIQZQaw";

    // Default context
    private String currentPlaceName = "Manila";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private boolean isProgrammaticChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);

        dbHelper = DatabaseHelper.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        resultsContainer = findViewById(R.id.results_container);
        tvSectionTitle = findViewById(R.id.tv_section_title);
        planTripCard = findViewById(R.id.plan_trip_card);
        searchDestinations = findViewById(R.id.search_destinations);

        findViewById(R.id.btn_generate_now).setOnClickListener(v -> startActivity(new Intent(this, TripActivity.class)));

        setupAI();
        setupSearchListener();

        findViewById(R.id.category_food).setOnClickListener(v -> fetchAICategoryData("Best local restaurants and delicacies"));
        findViewById(R.id.category_stays).setOnClickListener(v -> fetchAICategoryData("Top-rated hotels and stays"));
        findViewById(R.id.category_places).setOnClickListener(v -> fetchAICategoryData("Must-visit tourist attractions"));

        clearDisplay();
        setupBottomNavigation();
    }

    private void setupAI() {
        try {
            GenerativeModel generativeModel = new GenerativeModel("gemini-1.5-flash", GEMINI_API_KEY);
            aiModel = GenerativeModelFutures.from(generativeModel);
        } catch (Exception e) {
            Log.e(TAG, "AI Setup error", e);
        }
    }

    private void setupSearchListener() {
        searchDestinations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isProgrammaticChange) return;
                String query = s.toString().trim();
                searchHandler.removeCallbacksAndMessages(null); 
                if (query.length() > 2) {
                    searchHandler.postDelayed(() -> performAISearch(query), 800);
                } else if (query.isEmpty()) {
                    clearDisplay();
                }
            }
        });
    }

    private void performAISearch(String query) {
        tvSectionTitle.setText("AI is finding destinations...");
        String prompt = "List 5 unique travel destinations in the Philippines that match '" + query + "'. " +
                "Format EXACTLY: Name | Short Description | Highlight";

        callAI(prompt, "Search Results", true);
    }

    private void fetchAICategoryData(String category) {
        tvSectionTitle.setText("AI is finding " + category + "...");
        String prompt = "Find 5 " + category + " in or near " + currentPlaceName + ". " +
                "Format EXACTLY: Name | Description | Recommended Activity";

        callAI(prompt, "AI Recommendations in " + currentPlaceName, false);
    }

    private void callAI(String prompt, String title, boolean isSearch) {
        if (aiModel == null) return;
        
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = aiModel.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    String text = result.getText();
                    if (text != null && !text.isEmpty()) {
                        parseAndShowAIResults(title, text, isSearch);
                    } else {
                        tvSectionTitle.setText("No results found.");
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    Log.e(TAG, "AI Error", t);
                    Toast.makeText(HomePage.this, "AI is currently busy.", Toast.LENGTH_SHORT).show();
                    showExploreResults("Recommended for You", getSampleTrips(), false);
                });
            }
        }, this.getMainExecutor());
    }

    private void parseAndShowAIResults(String title, String text, boolean isSearch) {
        List<ExploreItem> list = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.contains("|")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String name = parts[0].trim().replaceAll("^\\d+\\.\\s+", "").replace("- ", "");
                    String desc = parts[1].trim();
                    String itin = parts.length > 2 ? parts[2].trim() : "Plan your trip now!";
                    
                    ExploreItem item = new ExploreItem(name, desc, R.drawable.ic_map, itin);
                    list.add(item);
                }
            }
        }
        
        if (list.isEmpty()) {
            showExploreResults("Recommended for You", getSampleTrips(), false);
        } else {
            showExploreResults(title, list, isSearch);
        }
    }

    private void showExploreResults(String title, List<ExploreItem> items, boolean isSearch) {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText(title);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ExploreItem item : items) {
            View itemView = inflater.inflate(R.layout.item_explore, resultsContainer, false);
            ((TextView) itemView.findViewById(R.id.item_title)).setText(item.title);
            ((TextView) itemView.findViewById(R.id.item_subtitle)).setText(item.subtitle);
            ImageView imageView = itemView.findViewById(R.id.item_image);
            imageView.setImageResource(item.imageRes);
            
            itemView.setOnClickListener(v -> {
                if (item.itinerary != null) {
                    Intent intent = new Intent(this, ItineraryDetails.class);
                    intent.putExtra("destination", item.title);
                    intent.putExtra("itinerary", item.itinerary);
                    startActivity(intent);
                }
                
                if (isSearch) {
                    currentPlaceName = item.title;
                    isProgrammaticChange = true;
                    searchDestinations.setText(item.title);
                    isProgrammaticChange = false;
                    hideKeyboard();
                }
            });
            resultsContainer.addView(itemView);
        }
    }

    private void clearDisplay() {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText("AI Smart Recommendations");
        String prompt = "Suggest 3 unique and trending travel destinations in the Philippines. " +
                "Format EXACTLY: Name | Why visit | Highlight";
        callAI(prompt, "Recommended for You", false);
        if (planTripCard != null) planTripCard.setVisibility(View.VISIBLE);
    }

    private List<ExploreItem> getSampleTrips() {
        List<ExploreItem> list = new ArrayList<>();
        list.add(new ExploreItem("Batanes", "Culture & Nature", R.drawable.ic_map, "Explore Basco hills."));
        list.add(new ExploreItem("Siargao", "Adventure", R.drawable.ic_map, "Surf at Cloud 9."));
        return list;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) { clearDisplay(); return true; }
            else if (itemId == R.id.nav_activities) { startActivity(new Intent(this, BudgetTracker.class)); return true; }
            else if (itemId == R.id.nav_trip_history) { startActivity(new Intent(this, TripHistory.class)); return true; }
            else if (itemId == R.id.nav_maps) { startActivity(new Intent(this, TripActivity.class)); return true; }
            else if (itemId == R.id.nav_user_profile) { startActivity(new Intent(this, UserProfile.class)); return true; }
            return false;
        });
    }

    private static class ExploreItem {
        String title, subtitle, itinerary;
        int imageRes;
        ExploreItem(String title, String subtitle, int imageRes, String itinerary) {
            this.title = title; this.subtitle = subtitle; this.imageRes = imageRes; this.itinerary = itinerary;
        }
    }
}
