package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BudgetTracker extends AppCompatActivity {
    private static final String TAG = "BudgetTracker";
    private TextView tvRemainingBalance, tvTotalSpent, tvTotalLimit, tvFoodAmount, tvTransportAmount, tvStayAmount, tvSuggestionsTitle;
    private LinearProgressIndicator budgetProgress;
    private LinearLayout transactionListContainer, suggestionsContainer;
    private ProgressBar aiLoadingSpinner;
    private double foodTotal = 0, transportTotal = 0, stayTotal = 0, totalBudget = 0;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;
    private static final String PREF_NAME = "BudgetPrefs";
    private static final String PREF_SUGGESTIONS = "savedSuggestions";
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private boolean isAIFetching = false;

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
        if (!isAIFetching) {
            String savedJson = sharedPreferences.getString(PREF_SUGGESTIONS, "");
            if (!savedJson.isEmpty()) parseAndDisplaySuggestions(savedJson);
        }
    }

    private void loadBudgetData() {
        totalBudget = Double.longBitsToDouble(sharedPreferences.getLong("totalBudget", 0));
        foodTotal = Double.longBitsToDouble(sharedPreferences.getLong("foodTotal", 0));
        transportTotal = Double.longBitsToDouble(sharedPreferences.getLong("transportTotal", 0));
        stayTotal = Double.longBitsToDouble(sharedPreferences.getLong("stayTotal", 0));
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
        aiLoadingSpinner = findViewById(R.id.ai_loading_spinner);
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
                Snackbar.make(v, "Please set a budget first.", Snackbar.LENGTH_LONG)
                        .setAction("SET BUDGET", view -> showAddFundsDialog()).show();
            } else {
                showAddExpenseDialog();
            }
        });
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("DESTINATION")) {
            fetchRealAISuggestions(
                intent.getStringExtra("DESTINATION"),
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

    private void fetchRealAISuggestions(String dest, String interests, double budget, long days) {
        if (isAIFetching) return;
        isAIFetching = true;
        
        tvSuggestionsTitle.setVisibility(View.VISIBLE);
        aiLoadingSpinner.setVisibility(View.VISIBLE);
        suggestionsContainer.setVisibility(View.GONE);
        tvSuggestionsTitle.setText("AI is finding the best gems for you...");
        
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.temperature = 0.4f;
        GenerationConfig config = configBuilder.build();
        
        // Corrected model name from gemini-2.5-flash to gemini-1.5-flash
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY, config, safetySettings);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = "Create 5 realistic travel suggestions for " + dest + " focusing on " + interests +
                " for " + days + " days with a total budget of PHP " + budget + ". " +
                "Return exactly a JSON array of objects with these keys: \"title\", \"description\", \"price\", \"category\", \"itinerary\", \"imageUrl\", \"priceBreakdown\". " +
                "The \"price\" must be a numeric value representing the total cost in PHP. " +
                "The \"priceBreakdown\" should be a concise string explaining the estimated breakdown of the cost (e.g., 'Includes: Transport PHP 200, Food PHP 300, Fees PHP 100'). " +
                "The \"itinerary\" MUST be a JSON array of objects, each with \"time\", \"activity\", and \"location\" keys. " +
                "For \"imageUrl\", use 'https://loremflickr.com/800/600/' followed by keywords related to the place.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                isAIFetching = false;
                try {
                    String resultText = result.getText();
                    if (resultText == null || resultText.isEmpty()) throw new Exception("Empty AI response");
                    runOnUiThread(() -> {
                        aiLoadingSpinner.setVisibility(View.GONE);
                        suggestionsContainer.setVisibility(View.VISIBLE);
                        sharedPreferences.edit().putString(PREF_SUGGESTIONS, resultText).apply();
                        parseAndDisplaySuggestions(resultText);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        aiLoadingSpinner.setVisibility(View.GONE);
                        tvSuggestionsTitle.setText("AI processing error.");
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                isAIFetching = false;
                runOnUiThread(() -> {
                    aiLoadingSpinner.setVisibility(View.GONE);
                    tvSuggestionsTitle.setText("AI connection error.");
                });
            }
        }, executor);
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
                tvSuggestionsTitle.setText("No suggestions found.");
                return;
            }
            tvSuggestionsTitle.setVisibility(View.VISIBLE);
            suggestionsContainer.setVisibility(View.VISIBLE);
            tvSuggestionsTitle.setText("AI Recommendations for you");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.optString("title", "Local Pick");
                
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

                String category = obj.optString("category", "Transport");
                String priceBreakdown = obj.optString("priceBreakdown", "No breakdown available.");
                String itineraryJson = obj.optString("itinerary", "[]");

                View itemView = getLayoutInflater().inflate(R.layout.item_suggestion, suggestionsContainer, false);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_title)).setText(title);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_description)).setText(obj.optString("description", ""));
                ((TextView) itemView.findViewById(R.id.tv_suggestion_price)).setText(String.format("₱%,.2f", price));

                Glide.with(this).load(obj.optString("imageUrl", "")).centerCrop().placeholder(R.drawable.routemind).into((ImageView) itemView.findViewById(R.id.iv_suggestion_image));

                final double finalPrice = price;
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, TourDetailsActivity.class);
                    intent.putExtra("TITLE", title);
                    intent.putExtra("DESCRIPTION", obj.optString("description", ""));
                    intent.putExtra("PRICE", finalPrice);
                    intent.putExtra("PRICE_BREAKDOWN", priceBreakdown);
                    intent.putExtra("ITINERARY", itineraryJson);
                    intent.putExtra("IMAGE_URL", obj.optString("imageUrl", ""));
                    intent.putExtra("CATEGORY", category);
                    startActivity(intent);
                });
                suggestionsContainer.addView(itemView);
            }
        } catch (Exception e) {
            tvSuggestionsTitle.setText("AI processing error.");
        }
    }

    private void loadTransactions() {
        transactionListContainer.removeAllViews();
    }

    private void updateBudgetUI() {
        double spent = foodTotal + transportTotal + stayTotal;
        double remaining = totalBudget - spent;
        tvTotalLimit.setText(String.format("₱%,.2f", totalBudget));
        tvTotalSpent.setText(String.format("₱%,.2f", spent));
        tvRemainingBalance.setText(String.format("₱%,.2f", remaining));
        tvFoodAmount.setText(String.format("₱%,.2f", foodTotal));
        tvTransportAmount.setText(String.format("₱%,.2f", transportTotal));
        tvStayAmount.setText(String.format("₱%,.2f", stayTotal));
        
        if (totalBudget > 0) {
            int progress = (int) ((spent / totalBudget) * 100);
            budgetProgress.setProgress(Math.min(progress, 100));
        } else {
            budgetProgress.setProgress(0);
        }
    }

    private void showAddFundsDialog() { }
    private void showAddExpenseDialog() { }
    private void showResetConfirmationDialog(View v) { }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_activities);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, HomePage.class)); finish(); return true; }
            else if (id == R.id.nav_maps) { startActivity(new Intent(this, TripActivity.class)); finish(); return true; }
            return true;
        });
    }
}
