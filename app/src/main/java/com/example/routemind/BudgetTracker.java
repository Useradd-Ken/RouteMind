package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BudgetTracker extends AppCompatActivity {
    private static final String TAG = "BudgetTracker";
    private TextView tvRemainingBalance, tvTotalSpent, tvTotalLimit, tvFoodAmount, tvTransportAmount, tvStayAmount, tvSuggestionsTitle;
    private LinearProgressIndicator budgetProgress;
    private LinearLayout transactionListContainer, suggestionsContainer;
    private ProgressBar loadingSpinner;
    private double foodTotal = 0, transportTotal = 0, stayTotal = 0, totalBudget = 0;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String PREF_NAME = "BudgetPrefs";
    private static final String PREF_SUGGESTIONS = "savedSuggestions";
    private static final String GEMINI_API_KEY = "AIzaSyC6pPLeFuVcEmQhCqG8N7mX_2b_xjx2xfU";
    private boolean isFetching = false;
    private String destinationName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_budget_tracker);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Ensure session username is populated
        if ((MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) && mAuth.getCurrentUser() != null) {
            MainActivity.sessionEmail = mAuth.getCurrentUser().getEmail();
            MainActivity.sessionUsername = dbHelper.getUsernameByEmail(MainActivity.sessionEmail);
            if (MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) {
                MainActivity.sessionUsername = dbHelper.getName(MainActivity.sessionEmail);
            }
            if (MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) {
                MainActivity.sessionUsername = MainActivity.sessionEmail.split("@")[0];
            }
        }

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadBudgetData();

        initViews();
        setupListeners();
        setupBottomNavigation();
        handleIntentData();
        loadTransactions();
        updateBudgetUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBudgetData();
        updateBudgetUI();
        loadTransactions();
        if (!isFetching) {
            String savedJson = sharedPreferences.getString(PREF_SUGGESTIONS, "");
            if (!savedJson.isEmpty()) parseAndDisplaySuggestions(savedJson);
        }
    }

    private void initViews() {
        tvRemainingBalance = findViewById(R.id.tv_remaining_balance);
        tvTotalSpent = findViewById(R.id.tv_total_spent);
        tvTotalLimit = findViewById(R.id.tv_total_limit);
        tvFoodAmount = findViewById(R.id.tv_food_amount);
        tvTransportAmount = findViewById(R.id.tv_transport_amount);
        tvStayAmount = findViewById(R.id.tv_stay_amount);
        budgetProgress = findViewById(R.id.budget_progress_indicator);
        transactionListContainer = findViewById(R.id.transaction_list_container);
        suggestionsContainer = findViewById(R.id.suggestions_container);
        tvSuggestionsTitle = findViewById(R.id.tv_suggestions_title);
        loadingSpinner = findViewById(R.id.ai_loading_spinner);
    }

    private void loadBudgetData() {
        // Fix for ClassCastException: Read the budget as long bits and convert back to double
        long budgetBits = sharedPreferences.getLong("totalBudget", Double.doubleToLongBits(0.0));
        totalBudget = Double.longBitsToDouble(budgetBits);

        foodTotal = dbHelper.getCategoryTotal("Food");
        transportTotal = dbHelper.getCategoryTotal("Transport");
        stayTotal = dbHelper.getCategoryTotal("Stay");
    }

    private void setupListeners() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, HomePage.class));
            finish();
        });

        findViewById(R.id.btn_set_budget).setOnClickListener(v -> showAddFundsDialog());
        findViewById(R.id.btn_edit_budget).setOnClickListener(v -> showAddFundsDialog());
        findViewById(R.id.btn_reset_budget).setOnClickListener(this::showResetConfirmationDialog);
        findViewById(R.id.btn_add_expense_top).setOnClickListener(v -> {
            if (totalBudget <= 0) {
                Snackbar.make(v, "Set a budget to get started!", Snackbar.LENGTH_LONG)
                        .setAction("Set Budget", view -> showAddFundsDialog()).show();
            } else {
                showAddExpenseDialog();
            }
        });
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("DESTINATION")) {
            destinationName = intent.getStringExtra("DESTINATION");
            fetchSuggestions(
                destinationName,
                intent.getStringExtra("INTERESTS"),
                intent.getDoubleExtra("BUDGET", 0),
                intent.getLongExtra("DURATION_DAYS", 1)
            );
        } else {
            String savedJson = sharedPreferences.getString(PREF_SUGGESTIONS, "");
            if (!savedJson.isEmpty()) parseAndDisplaySuggestions(savedJson);
            else {
                tvSuggestionsTitle.setVisibility(View.GONE);
                suggestionsContainer.setVisibility(View.GONE);
            }
        }
    }

    private void fetchSuggestions(String dest, String interests, double budget, long days) {
        if (isFetching) return;
        isFetching = true;
        
        tvSuggestionsTitle.setVisibility(View.VISIBLE);
        loadingSpinner.setVisibility(View.VISIBLE);
        suggestionsContainer.setVisibility(View.GONE);
        tvSuggestionsTitle.setText("Finding local gems for you...");
        
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.temperature = 0.4f;
        GenerationConfig config = configBuilder.build();
        
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY, config, safetySettings);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = "Create 5 realistic travel suggestions for " + dest + " focusing on " + interests +
                " for " + days + " days with a total budget of PHP " + budget + ". " +
                "Return exactly a JSON array of objects with these keys: \"title\", \"description\", \"price\", \"category\", \"itinerary\", \"imageUrl\", \"priceBreakdown\". " +
                "The \"price\" must be a numeric value representing the total cost in PHP. " +
                "The \"priceBreakdown\" should be a concise string explaining the estimated breakdown of the cost (e.g., 'Includes: Transport PHP 200, Food PHP 300, Fees PHP 100'). " +
                "For \"imageUrl\", use 'https://loremflickr.com/400/300/' followed by keywords related to the place.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                isFetching = false;
                try {
                    String resultText = result.getText();
                    if (resultText == null || resultText.isEmpty()) throw new Exception("Empty response");
                    runOnUiThread(() -> {
                        loadingSpinner.setVisibility(View.GONE);
                        suggestionsContainer.setVisibility(View.VISIBLE);
                        sharedPreferences.edit().putString(PREF_SUGGESTIONS, resultText).apply();
                        parseAndDisplaySuggestions(resultText);
                        
                        // Save the generated itinerary to SQLite
                        String startDate = getIntent().getStringExtra("START_DATE");
                        String endDate = getIntent().getStringExtra("END_DATE");
                        dbHelper.saveTrip(MainActivity.sessionUsername, dest, startDate, endDate, String.valueOf(budget), interests, resultText);

                        // Save the generated itinerary to Firebase Firestore "trips" collection
                        saveTripToFirebase(dest, startDate, endDate, budget, interests, resultText);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        loadingSpinner.setVisibility(View.GONE);
                        tvSuggestionsTitle.setText("We're having trouble loading suggestions. Please try again later.");
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                isFetching = false;
                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    tvSuggestionsTitle.setText("Connection issues. Please check your internet.");
                });
            }
        }, executor);
    }

    private void saveTripToFirebase(String dest, String startDate, String endDate, double budget, String interests, String itinerary) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("userId", user.getUid());
        tripData.put("username", MainActivity.sessionUsername);
        tripData.put("destination", dest);
        tripData.put("startDate", startDate);
        tripData.put("endDate", endDate);
        tripData.put("budget", budget);
        tripData.put("interests", interests);
        tripData.put("itineraryJson", itinerary);
        tripData.put("timestamp", System.currentTimeMillis());

        db.collection("trips").add(tripData)
            .addOnSuccessListener(documentReference -> Log.d(TAG, "Trip saved to Firestore"))
            .addOnFailureListener(e -> Log.e(TAG, "Error saving trip to Firestore", e));
    }

    private void parseAndDisplaySuggestions(String json) {
        try {
            String cleanJson = json.trim();
            if (cleanJson.contains("[") && cleanJson.contains("]")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("["), cleanJson.lastIndexOf("]") + 1);
            }
            if (cleanJson.isEmpty() || !cleanJson.startsWith("[")) return;
            JSONArray array = new JSONArray(cleanJson);
            suggestionsContainer.removeAllViews();
            if (array.length() == 0) {
                tvSuggestionsTitle.setText("No recommendations found for this area.");
                return;
            }
            tvSuggestionsTitle.setVisibility(View.VISIBLE);
            suggestionsContainer.setVisibility(View.VISIBLE);
            tvSuggestionsTitle.setText("Handpicked for you");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.optString("title", "Hidden Spot");
                
                double price = 0;
                if (obj.has("price")) {
                    Object priceObj = obj.get("price");
                    if (priceObj instanceof Number) {
                        price = ((Number) priceObj).doubleValue();
                    } else if (priceObj instanceof String) {
                        try {
                            price = Double.parseDouble(((String) priceObj).replaceAll("[^\\d.]", ""));
                        } catch (Exception ignored) {}
                    }
                }

                String category = obj.optString("category", "Activity");
                String priceBreakdown = obj.optString("priceBreakdown", "Details available upon selection.");
                String description = obj.optString("description", "");
                String itineraryDetails = obj.optString("itinerary", "");
                String imageUrl = obj.optString("imageUrl", "");

                View itemView = getLayoutInflater().inflate(R.layout.item_suggestion, suggestionsContainer, false);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_title)).setText(title);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_description)).setText(description);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_price)).setText(String.format("₱%.2f", price));

                Glide.with(this).load(imageUrl).centerCrop().placeholder(R.drawable.routemind).into((ImageView) itemView.findViewById(R.id.iv_suggestion_image));

                final double finalPrice = price;
                
                // Add to Trip Button Logic
                itemView.findViewById(R.id.btn_select_suggestion).setOnClickListener(v -> {
                    addToTrip(title, description, finalPrice, priceBreakdown, itineraryDetails, imageUrl, category);
                });

                // Detail Click Logic
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, TourDetailsActivity.class);
                    intent.putExtra("TITLE", title);
                    intent.putExtra("DESCRIPTION", description);
                    intent.putExtra("PRICE", finalPrice);
                    intent.putExtra("PRICE_BREAKDOWN", priceBreakdown);
                    intent.putExtra("ITINERARY", itineraryDetails);
                    intent.putExtra("IMAGE_URL", imageUrl);
                    intent.putExtra("CATEGORY", category);
                    intent.putExtra("DESTINATION", destinationName);
                    startActivity(intent);
                });
                suggestionsContainer.addView(itemView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing suggestions", e);
        }
    }

    private void addToTrip(String title, String desc, double price, String breakdown, String details, String image, String category) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Snackbar.make(suggestionsContainer, "Please log in to add items to your trip.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String username = MainActivity.sessionUsername;
        String id = db.collection("booked_itineraries").document().getId();
        Itinerary itineraryObj = new Itinerary(id, user.getUid(), username, title, desc, details, image, category, price, breakdown, destinationName);

        db.collection("booked_itineraries").document(id).set(itineraryObj)
                .addOnSuccessListener(aVoid -> {
                    // Also save locally
                    dbHelper.saveBookedItinerary(itineraryObj);
                    Snackbar.make(suggestionsContainer, "Added " + title + " to your trip!", Snackbar.LENGTH_LONG)
                            .setAction("View History", v -> startActivity(new Intent(this, TripHistory.class)))
                            .show();
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(suggestionsContainer, "Failed to add item. Try again.", Snackbar.LENGTH_SHORT).show();
                });
    }

    private void updateBudgetUI() {
        double spent = foodTotal + transportTotal + stayTotal;
        double remaining = totalBudget - spent;

        tvTotalLimit.setText(String.format("₱%.2f", totalBudget));
        tvRemainingBalance.setText(String.format("₱%.2f", remaining));
        tvTotalSpent.setText(String.format("₱%.2f", spent));
        tvFoodAmount.setText(String.format("₱%.2f", foodTotal));
        tvTransportAmount.setText(String.format("₱%.2f", transportTotal));
        tvStayAmount.setText(String.format("₱%.2f", stayTotal));

        if (totalBudget > 0) {
            int progress = (int) ((spent / totalBudget) * 100);
            budgetProgress.setProgress(Math.min(progress, 100));
            budgetProgress.setIndicatorColor(spent > totalBudget ? getColor(android.R.color.holo_red_light) : getColor(R.color.primary_color));
        } else {
            budgetProgress.setProgress(0);
        }
    }

    private void loadTransactions() {
        // Implementation for loading transactions
    }

    private void showAddFundsDialog() {
        // Implementation for adding budget
    }

    private void showResetConfirmationDialog(View v) {
        // Implementation for resetting budget
    }

    private void showAddExpenseDialog() {
        // Implementation for adding expense
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_activities);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) navigateTo(HomePage.class);
            else if (id == R.id.nav_maps) navigateTo(TripActivity.class);
            else if (id == R.id.nav_trip_history) navigateTo(TripHistory.class);
            else if (id == R.id.nav_user_profile) navigateTo(UserProfile.class);
            return id == R.id.nav_activities;
        });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(0, 0);
        finish();
    }
}
