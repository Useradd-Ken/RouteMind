package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.slider.Slider;
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
                Snackbar.make(v, getString(R.string.start_budget_prompt), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.set_budget), view -> showAddFundsDialog()).show();
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
        tvSuggestionsTitle.setText(R.string.ai_finding_gems);
        
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
                        tvSuggestionsTitle.setText(R.string.ai_processing_error);
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                isAIFetching = false;
                runOnUiThread(() -> {
                    aiLoadingSpinner.setVisibility(View.GONE);
                    tvSuggestionsTitle.setText(R.string.ai_connection_error);
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
                tvSuggestionsTitle.setText(R.string.ai_no_suggestions);
                return;
            }
            tvSuggestionsTitle.setVisibility(View.VISIBLE);
            suggestionsContainer.setVisibility(View.VISIBLE);
            tvSuggestionsTitle.setText(R.string.ai_recommended_title);

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

                View itemView = getLayoutInflater().inflate(R.layout.item_suggestion, suggestionsContainer, false);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_title)).setText(title);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_description)).setText(obj.optString("description", ""));
                ((TextView) itemView.findViewById(R.id.tv_suggestion_price)).setText(getString(R.string.currency_format_decimal, price));

                Glide.with(this).load(obj.optString("imageUrl", "")).centerCrop().placeholder(R.drawable.routemind).into((ImageView) itemView.findViewById(R.id.iv_suggestion_image));

                final double finalPrice = price;
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, TourDetailsActivity.class);
                    intent.putExtra("TITLE", title);
                    intent.putExtra("DESCRIPTION", obj.optString("description", ""));
                    intent.putExtra("PRICE", finalPrice);
                    intent.putExtra("PRICE_BREAKDOWN", priceBreakdown);
                    intent.putExtra("ITINERARY", obj.optString("itinerary", ""));
                    intent.putExtra("IMAGE_URL", obj.optString("imageUrl", ""));
                    intent.putExtra("CATEGORY", category);
                    startActivity(intent);
                });

                itemView.findViewById(R.id.btn_select_suggestion).setOnClickListener(v -> {
                    if (totalBudget >= (foodTotal + transportTotal + stayTotal + finalPrice)) {
                        addExpense(category, finalPrice);
                        Snackbar.make(v, getString(R.string.ai_added_success, title), Snackbar.LENGTH_SHORT).show();
                        suggestionsContainer.removeView(itemView);
                        updateSavedSuggestionsAfterRemoval(title);
                    } else {
                        Snackbar.make(v, getString(R.string.budget_low_error), Snackbar.LENGTH_LONG).show();
                    }
                });
                suggestionsContainer.addView(itemView);
            }
        } catch (Exception e) { Log.e(TAG, "Parsing Error", e); }
    }

    private void updateSavedSuggestionsAfterRemoval(String titleToRemove) {
        String savedJson = sharedPreferences.getString(PREF_SUGGESTIONS, "");
        if (savedJson.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(savedJson);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.optString("title").equals(titleToRemove)) newArray.put(obj);
            }
            sharedPreferences.edit().putString(PREF_SUGGESTIONS, newArray.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_activities);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) { startActivity(new Intent(this, HomePage.class)); finish(); return true; }
            else if (itemId == R.id.nav_activities) return true;
            else if (itemId == R.id.nav_maps) { startActivity(new Intent(this, TripActivity.class)); finish(); return true; }
            else if (itemId == R.id.nav_trip_history) { startActivity(new Intent(this, TripHistory.class)); finish(); return true; }
            else if (itemId == R.id.nav_user_profile) { startActivity(new Intent(this, UserProfile.class)); finish(); return true; }
            return false;
        });
    }

    private void loadBudgetData() {
        totalBudget = Double.longBitsToDouble(sharedPreferences.getLong("totalBudget", Double.doubleToLongBits(0)));
        foodTotal = Double.longBitsToDouble(sharedPreferences.getLong("foodTotal", Double.doubleToLongBits(0)));
        transportTotal = Double.longBitsToDouble(sharedPreferences.getLong("transportTotal", Double.doubleToLongBits(0)));
        stayTotal = Double.longBitsToDouble(sharedPreferences.getLong("stayTotal", Double.doubleToLongBits(0)));
    }

    private void updateBudgetUI() {
        double totalSpent = foodTotal + transportTotal + stayTotal;
        tvTotalLimit.setText(getString(R.string.currency_format_decimal, totalBudget));
        tvTotalSpent.setText(getString(R.string.currency_format_decimal, totalSpent));
        tvRemainingBalance.setText(getString(R.string.currency_format_decimal, totalBudget - totalSpent));
        tvFoodAmount.setText(getString(R.string.currency_format_decimal, foodTotal));
        tvTransportAmount.setText(getString(R.string.currency_format_decimal, transportTotal));
        tvStayAmount.setText(getString(R.string.currency_format_decimal, stayTotal));
        budgetProgress.setProgress(totalBudget > 0 ? Math.min((int) ((totalSpent / totalBudget) * 100), 100) : 0);
    }

    private void showAddFundsDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_funds, null);
        EditText et = v.findViewById(R.id.et_fund_amount);
        new AlertDialog.Builder(this).setTitle(R.string.add_funds).setView(v)
                .setPositiveButton(R.string.add_funds, (d, w) -> {
                    if (!et.getText().toString().isEmpty()) {
                        totalBudget += Double.parseDouble(et.getText().toString());
                        saveBudgetData(); updateBudgetUI();
                        Snackbar.make(tvTotalLimit, R.string.funds_added_success, Snackbar.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void showAddExpenseDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        EditText et = v.findViewById(R.id.et_expense_amount);
        Slider s = v.findViewById(R.id.expense_slider);
        final String[] cat = {"Food"};
        v.findViewById(R.id.btn_cat_food).setOnClickListener(view -> cat[0] = "Food");
        v.findViewById(R.id.btn_cat_transport).setOnClickListener(view -> cat[0] = "Transport");
        v.findViewById(R.id.btn_cat_stay).setOnClickListener(view -> cat[0] = "Stay");
        s.addOnChangeListener((sl, val, from) -> { if (from) et.setText(String.valueOf((int) val)); });
        new AlertDialog.Builder(this).setTitle(R.string.log_expense).setView(v)
                .setPositiveButton(R.string.log_expense, (d, w) -> {
                    if (!et.getText().toString().isEmpty()) addExpense(cat[0], Double.parseDouble(et.getText().toString()));
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void addExpense(String c, double a) {
        if (c.equals("Food")) foodTotal += a;
        else if (c.equals("Transport")) transportTotal += a;
        else stayTotal += a;
        saveTransaction(c, a); saveBudgetData(); updateBudgetUI();
    }

    private void saveTransaction(String c, double a) {
        try {
            long timestamp = System.currentTimeMillis();
            dbHelper.addExpense(c, a, timestamp);
            loadTransactions();
        } catch (Exception e) { Log.e(TAG, "Save Error", e); }
    }

    private void loadTransactions() {
        try {
            transactionListContainer.removeAllViews();
            Cursor cursor = dbHelper.getAllExpenses();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String category = cursor.getString(1);
                    double amount = cursor.getDouble(2);

                    View v = getLayoutInflater().inflate(R.layout.item_transaction, transactionListContainer, false);
                    ((TextView) v.findViewById(R.id.tv_transaction_category)).setText(category);
                    ((TextView) v.findViewById(R.id.tv_transaction_amount)).setText(getString(R.string.expense_format, amount));
                    
                    v.setOnLongClickListener(view -> { 
                        showDeleteTransactionDialog(id, category, amount); 
                        return true; 
                    });
                    transactionListContainer.addView(v);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) { Log.e(TAG, "Load Error", e); }
    }

    private void showDeleteTransactionDialog(int id, String category, double amount) {
        new AlertDialog.Builder(this).setTitle(R.string.delete_expense).setMessage(R.string.delete_expense_msg)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    if (category.equals("Food")) foodTotal -= amount;
                    else if (category.equals("Transport")) transportTotal -= amount;
                    else stayTotal -= amount;
                    dbHelper.deleteExpense(id);
                    saveBudgetData(); updateBudgetUI(); loadTransactions();
                    Snackbar.make(tvTotalLimit, R.string.expense_removed, Snackbar.LENGTH_SHORT).show();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void showResetConfirmationDialog(View view) {
        new AlertDialog.Builder(this).setTitle(R.string.reset_all_data).setMessage(R.string.reset_msg)
                .setPositiveButton(R.string.reset_everything, (d, w) -> {
                    sharedPreferences.edit().clear().apply();
                    dbHelper.clearAllExpenses();
                    totalBudget = 0; foodTotal = 0; transportTotal = 0; stayTotal = 0;
                    updateBudgetUI(); loadTransactions(); suggestionsContainer.removeAllViews();
                    tvSuggestionsTitle.setVisibility(View.GONE);
                    Snackbar.make(view, R.string.reset_success, Snackbar.LENGTH_LONG).show();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void saveBudgetData() {
        sharedPreferences.edit().putLong("totalBudget", Double.doubleToLongBits(totalBudget))
                .putLong("foodTotal", Double.doubleToLongBits(foodTotal))
                .putLong("transportTotal", Double.doubleToLongBits(transportTotal))
                .putLong("stayTotal", Double.doubleToLongBits(stayTotal)).apply();
    }
}
