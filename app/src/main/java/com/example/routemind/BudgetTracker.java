package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
    private TextView tvRemainingBalance, tvTotalSpent, tvTotalLimit;
    private TextView tvFoodAmount, tvTransportAmount, tvStayAmount;
    private LinearProgressIndicator budgetProgress;
    private LinearLayout transactionListContainer;
    private LinearLayout suggestionsContainer;
    private TextView tvSuggestionsTitle;

    private double foodTotal = 0, transportTotal = 0, stayTotal = 0;
    private double totalBudget = 0;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "BudgetPrefs";
    private static final String PREF_SUGGESTIONS = "savedSuggestions";
    private static final String PREF_TRANSACTIONS = "savedTransactions";
    
    // API Key updated to latest provided
    private static final String GEMINI_API_KEY = "AIzaSyA4pwZY4jYN08D6IBmv_ENKktKXPCR7wro";

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

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadBudgetData();

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
        ImageView btnEditBudget = findViewById(R.id.btn_edit_budget);

        findViewById(R.id.btn_set_budget).setOnClickListener(v -> showSetBudgetDialog());
        findViewById(R.id.btn_reset_budget).setOnClickListener(v -> showResetConfirmationDialog());
        btnEditBudget.setOnClickListener(v -> showAddFundsDialog());

        findViewById(R.id.btn_add_expense_top).setOnClickListener(v -> {
            if (totalBudget <= 0) {
                Toast.makeText(this, "Please define your budget to start tracking expenses.", Toast.LENGTH_SHORT).show();
                showSetBudgetDialog();
            } else {
                showAddExpenseDialog();
            }
        });

        setupBottomNavigation();

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
            if (!savedJson.isEmpty()) {
                parseAndDisplaySuggestions(savedJson);
            } else {
                tvSuggestionsTitle.setVisibility(View.GONE);
                suggestionsContainer.setVisibility(View.GONE);
            }
        }

        loadTransactions();
        updateBudgetUI();
    }

    private void fetchRealAISuggestions(String dest, String interests, double budget, long days) {
        if (GEMINI_API_KEY.isEmpty()) {
            tvSuggestionsTitle.setText("Exploration features are currently offline.");
            return;
        }

        tvSuggestionsTitle.setVisibility(View.VISIBLE);
        suggestionsContainer.setVisibility(View.VISIBLE);
        tvSuggestionsTitle.setText("Curating handpicked experiences for your journey...");
        
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE));

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.temperature = 0.4f;
        GenerationConfig config = configBuilder.build();

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                GEMINI_API_KEY,
                config,
                safetySettings
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = "Create 5 realistic travel suggestions for " + dest + " focusing on " + interests +
            " for a trip duration of " + days + " days. The user has a budget of PHP " + budget + ". " +
            "Provide accurate prices. Return a JSON array of objects. Each object MUST have: " +
            "\"title\", \"description\", \"price\" (number), \"category\" (one of: \"Food\", \"Transport\", \"Stay\"), " +
            "\"itinerary\" (short summary), and \"imageUrl\". " +
            "For \"imageUrl\", use 'https://image.pollinations.ai/prompt/[descriptive_prompt]?width=800&height=600&nologo=true' " +
            "where [descriptive_prompt] is a short, specific description of a high-quality photo for the suggestion. " +
            "Return ONLY the JSON array.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String resultText = result.getText();
                    if (resultText == null || resultText.isEmpty()) throw new Exception("Empty AI response");
                    runOnUiThread(() -> {
                        sharedPreferences.edit().putString(PREF_SUGGESTIONS, resultText).apply();
                        parseAndDisplaySuggestions(resultText);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Processing error", e);
                    runOnUiThread(() -> tvSuggestionsTitle.setText("We encountered a minor issue syncing recommendations."));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Sync Failed", t);
                runOnUiThread(() -> {
                    tvSuggestionsTitle.setText("Unable to load suggestions. Please check your network connection.");
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

            JSONArray array = new JSONArray(cleanJson);
            suggestionsContainer.removeAllViews();
            
            if (array.length() == 0) {
                tvSuggestionsTitle.setText("No customized recommendations found for this trip.");
                return;
            }

            tvSuggestionsTitle.setVisibility(View.VISIBLE);
            suggestionsContainer.setVisibility(View.VISIBLE);
            tvSuggestionsTitle.setText("Personalized for your journey");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.optString("title", "Local Experience");
                String desc = obj.optString("description", "");
                double price = obj.optDouble("price", 0.0);
                String category = obj.optString("category", "Transport");
                String itinerary = obj.optString("itinerary", "");
                String imageUrl = obj.optString("imageUrl", "");

                View itemView = getLayoutInflater().inflate(R.layout.item_suggestion, suggestionsContainer, false);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_title)).setText(title);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_description)).setText(desc);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_price)).setText("₱" + String.format("%.2f", price));

                ImageView ivImage = itemView.findViewById(R.id.iv_suggestion_image);
                if (!imageUrl.isEmpty()) {
                    Glide.with(this).load(imageUrl).into(ivImage);
                }

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, TourDetailsActivity.class);
                    intent.putExtra("TITLE", title);
                    intent.putExtra("DESCRIPTION", desc);
                    intent.putExtra("PRICE", price);
                    intent.putExtra("ITINERARY", itinerary);
                    intent.putExtra("IMAGE_URL", imageUrl);
                    intent.putExtra("CATEGORY", category);
                    startActivity(intent);
                });

                itemView.findViewById(R.id.btn_select_suggestion).setOnClickListener(v -> {
                    if (totalBudget >= (foodTotal + transportTotal + stayTotal + price)) {
                        addExpense(category, price);
                        Toast.makeText(this, "Added to list: " + title, Toast.LENGTH_SHORT).show();
                        suggestionsContainer.removeView(itemView);
                        updateSavedSuggestionsAfterRemoval(title);
                    } else {
                        Toast.makeText(this, "Your current balance is insufficient for this booking.", Toast.LENGTH_SHORT).show();
                    }
                });

                suggestionsContainer.addView(itemView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Parsing Error", e);
        }
    }

    private void updateSavedSuggestionsAfterRemoval(String titleToRemove) {
        String savedJson = sharedPreferences.getString(PREF_SUGGESTIONS, "");
        if (savedJson.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(savedJson);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.optString("title").equals(titleToRemove)) {
                    newArray.put(obj);
                }
            }
            sharedPreferences.edit().putString(PREF_SUGGESTIONS, newArray.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_activities);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomePage.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_activities) {
                return true;
            } else if (itemId == R.id.nav_maps) {
                startActivity(new Intent(this, TripActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_trip_history) {
                startActivity(new Intent(this, TripHistory.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_user_profile) {
                startActivity(new Intent(this, UserProfile.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadBudgetData() {
        totalBudget = Double.longBitsToDouble(sharedPreferences.getLong("totalBudget", Double.doubleToLongBits(0)));
        foodTotal = Double.longBitsToDouble(sharedPreferences.getLong("foodTotal", Double.doubleToLongBits(0)));
        transportTotal = Double.longBitsToDouble(sharedPreferences.getLong("transportTotal", Double.doubleToLongBits(0)));
        stayTotal = Double.longBitsToDouble(sharedPreferences.getLong("stayTotal", Double.doubleToLongBits(0)));
    }

    private void saveBudgetData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("totalBudget", Double.doubleToLongBits(totalBudget));
        editor.putLong("foodTotal", Double.doubleToLongBits(foodTotal));
        editor.putLong("transportTotal", Double.doubleToLongBits(transportTotal));
        editor.putLong("stayTotal", Double.doubleToLongBits(stayTotal));
        editor.apply();
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Reset")
                .setMessage("This will clear your current budget and all recorded expenses. Are you sure?")
                .setPositiveButton("Reset Records", (dialog, which) -> resetBudgetData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetBudgetData() {
        totalBudget = 0; foodTotal = 0; transportTotal = 0; stayTotal = 0;
        sharedPreferences.edit().putString(PREF_SUGGESTIONS, "").apply();
        sharedPreferences.edit().putString(PREF_TRANSACTIONS, "").apply();
        saveBudgetData();
        transactionListContainer.removeAllViews();
        suggestionsContainer.removeAllViews();
        updateBudgetUI();
        Toast.makeText(this, "Records successfully cleared.", Toast.LENGTH_SHORT).show();
    }

    private void showSetBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Define Trip Budget");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Initial amount (₱)");
        builder.setView(input);
        builder.setPositiveButton("Save Budget", (d, w) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                totalBudget = Double.parseDouble(val);
                saveBudgetData();
                updateBudgetUI();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddFundsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Funds");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Top up amount (₱)");
        builder.setView(input);
        builder.setPositiveButton("Top Up", (d, w) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                totalBudget += Double.parseDouble(val);
                saveBudgetData();
                updateBudgetUI();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddExpenseDialog() {
        String[] categories = {"Food", "Transport", "Stay"};
        new AlertDialog.Builder(this)
                .setTitle("Log New Expense")
                .setItems(categories, (dialog, which) -> showAmountInputDialog(categories[which]))
                .show();
    }

    private void showAmountInputDialog(String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(category + " Expense");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Expense amount (₱)");
        builder.setView(input);
        builder.setPositiveButton("Log Expense", (d, w) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                addExpense(category, Double.parseDouble(val));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addExpense(String category, double amount) {
        if (category.equalsIgnoreCase("Food")) foodTotal += amount;
        else if (category.equalsIgnoreCase("Transport")) transportTotal += amount;
        else if (category.equalsIgnoreCase("Stay")) stayTotal += amount;
        saveBudgetData();
        saveTransaction(category, amount);
        addTransactionToUI(category, amount);
        updateBudgetUI();
    }

    private void saveTransaction(String category, double amount) {
        String savedTransactions = sharedPreferences.getString(PREF_TRANSACTIONS, "");
        try {
            JSONArray array;
            if (savedTransactions.isEmpty()) {
                array = new JSONArray();
            } else {
                array = new JSONArray(savedTransactions);
            }
            JSONObject obj = new JSONObject();
            obj.put("category", category);
            obj.put("amount", amount);
            array.put(obj);
            sharedPreferences.edit().putString(PREF_TRANSACTIONS, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving history", e);
        }
    }

    private void loadTransactions() {
        String savedTransactions = sharedPreferences.getString(PREF_TRANSACTIONS, "");
        if (savedTransactions.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(savedTransactions);
            transactionListContainer.removeAllViews();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                addTransactionToUI(obj.optString("category"), obj.optDouble("amount"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading history", e);
        }
    }

    private void addTransactionToUI(String category, double amount) {
        View transactionView = getLayoutInflater().inflate(R.layout.item_transaction, transactionListContainer, false);
        ((TextView) transactionView.findViewById(R.id.tv_transaction_name)).setText(category);
        ((TextView) transactionView.findViewById(R.id.tv_transaction_amount)).setText("-₱" + String.format("%.2f", amount));
        ImageView icon = transactionView.findViewById(R.id.iv_transaction_icon);
        if (category.equalsIgnoreCase("Food")) icon.setImageResource(R.drawable.ic_food);
        else if (category.equalsIgnoreCase("Transport")) icon.setImageResource(R.drawable.ic_transport);
        else if (category.equalsIgnoreCase("Stay")) icon.setImageResource(R.drawable.ic_hotel);
        
        transactionView.findViewById(R.id.btn_delete_transaction).setOnClickListener(v -> {
            removeExpense(category, amount);
            transactionListContainer.removeView(transactionView);
            removeTransactionFromPrefs(category, amount);
        });
        transactionListContainer.addView(transactionView, 0);
    }

    private void removeTransactionFromPrefs(String category, double amount) {
        String savedTransactions = sharedPreferences.getString(PREF_TRANSACTIONS, "");
        if (savedTransactions.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(savedTransactions);
            JSONArray newArray = new JSONArray();
            boolean removed = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!removed && obj.optString("category").equalsIgnoreCase(category) && obj.optDouble("amount") == amount) {
                    removed = true;
                    continue;
                }
                newArray.put(obj);
            }
            sharedPreferences.edit().putString(PREF_TRANSACTIONS, newArray.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error removing history entry", e);
        }
    }

    private void removeExpense(String category, double amount) {
        if (category.equalsIgnoreCase("Food")) foodTotal -= amount;
        else if (category.equalsIgnoreCase("Transport")) transportTotal -= amount;
        else if (category.equalsIgnoreCase("Stay")) stayTotal -= amount;
        saveBudgetData();
        updateBudgetUI();
    }

    private void updateBudgetUI() {
        double spent = foodTotal + transportTotal + stayTotal;
        double remaining = totalBudget - spent;
        tvFoodAmount.setText("₱" + (int)foodTotal);
        tvTransportAmount.setText("₱" + (int)transportTotal);
        tvStayAmount.setText("₱" + (int)stayTotal);
        tvTotalSpent.setText("₱" + (int)spent);
        tvTotalLimit.setText("₱" + (int)totalBudget);
        tvRemainingBalance.setText("₱" + String.format("%.2f", remaining));
        if (totalBudget > 0) {
            int progress = (int) ((spent / totalBudget) * 100);
            budgetProgress.setProgress(Math.min(progress, 100));
        } else {
            budgetProgress.setProgress(0);
        }
        if (remaining < 0) tvRemainingBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        else tvRemainingBalance.setTextColor(getResources().getColor(android.R.color.white));
    }
}
