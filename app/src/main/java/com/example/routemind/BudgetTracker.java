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
import com.google.ai.client.generativeai.type.RequestOptions;
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
    private ImageView btnEditBudget;

    private double foodTotal = 0, transportTotal = 0, stayTotal = 0;
    private double totalBudget = 0;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "BudgetPrefs";
    private static final String GEMINI_API_KEY = "AIzaSyDqx0-E-Q_QFKODuJswSp912UXqcdtsO44";

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
        btnEditBudget = findViewById(R.id.btn_edit_budget);

        findViewById(R.id.btn_set_budget).setOnClickListener(v -> showSetBudgetDialog());
        findViewById(R.id.btn_reset_budget).setOnClickListener(v -> showResetConfirmationDialog());
        btnEditBudget.setOnClickListener(v -> showAddFundsDialog());

        findViewById(R.id.btn_add_expense_top).setOnClickListener(v -> {
            if (totalBudget <= 0) {
                Toast.makeText(this, "Please set your budget first!", Toast.LENGTH_SHORT).show();
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
            if (tvSuggestionsTitle != null) tvSuggestionsTitle.setVisibility(View.GONE);
            if (suggestionsContainer != null) suggestionsContainer.setVisibility(View.GONE);
        }

        updateBudgetUI();
    }

    private void fetchRealAISuggestions(String dest, String interests, double budget, long days) {
        if (GEMINI_API_KEY.isEmpty()) {
            if (tvSuggestionsTitle != null) tvSuggestionsTitle.setText("API Key Missing");
            return;
        }

        if (tvSuggestionsTitle != null) {
            tvSuggestionsTitle.setVisibility(View.VISIBLE);
            tvSuggestionsTitle.setText("AI Generating Suggestions...");
        }
        if (suggestionsContainer != null) suggestionsContainer.setVisibility(View.VISIBLE);
        
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE));

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.temperature = 0.4f;
        GenerationConfig config = configBuilder.build();

        RequestOptions requestOptions = new RequestOptions();
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", GEMINI_API_KEY, config, safetySettings, requestOptions);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = "Create 3 realistic travel tour suggestions for " + dest + " focusing on " + interests + 
            " for a trip duration of " + days + " days. The user has a budget of PHP " + budget + ". " +
            "Return a JSON array of objects with: \"title\", \"description\", \"price\" (number), \"itinerary\" (1-sentence), and \"imageUrl\".";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String resultText = result.getText();
                    runOnUiThread(() -> parseAndDisplaySuggestions(resultText));
                } catch (Exception e) {
                    runOnUiThread(() -> tvSuggestionsTitle.setText("AI Response Error"));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> tvSuggestionsTitle.setText("AI Suggestion Failed"));
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
                tvSuggestionsTitle.setText("No recommendations found");
                return;
            }

            tvSuggestionsTitle.setText("AI Recommended Tours");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.optString("title", "Local Trip");
                String desc = obj.optString("description", "");
                double price = obj.optDouble("price", 0.0);
                String itinerary = obj.optString("itinerary", "");
                String imageUrl = obj.optString("imageUrl", "");

                View itemView = getLayoutInflater().inflate(R.layout.item_suggestion, suggestionsContainer, false);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_title)).setText(title);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_description)).setText(desc);
                ((TextView) itemView.findViewById(R.id.tv_suggestion_price)).setText("₱" + String.format("%.2f", price));

                ImageView ivImage = itemView.findViewById(R.id.iv_suggestion_image);
                Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_launcher_background).into(ivImage);

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, TourDetailsActivity.class);
                    intent.putExtra("TITLE", title);
                    intent.putExtra("DESCRIPTION", desc);
                    intent.putExtra("PRICE", price);
                    intent.putExtra("ITINERARY", itinerary);
                    intent.putExtra("IMAGE_URL", imageUrl);
                    startActivity(intent);
                });

                itemView.findViewById(R.id.btn_select_suggestion).setOnClickListener(v -> {
                    if (totalBudget >= price) {
                        addExpense("Transport", price);
                        Toast.makeText(this, "Booked: " + title, Toast.LENGTH_SHORT).show();
                        suggestionsContainer.removeView(itemView);
                    } else {
                        Toast.makeText(this, "Insufficient budget!", Toast.LENGTH_SHORT).show();
                    }
                });

                suggestionsContainer.addView(itemView);
            }
        } catch (Exception e) {
            tvSuggestionsTitle.setText("AI Parsing Error");
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_activities);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomePage.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_activities) {
                return true;
            } else if (itemId == R.id.nav_maps) {
                startActivity(new Intent(this, TripActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_trip_history) {
                startActivity(new Intent(this, TripHistory.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_user_profile) {
                startActivity(new Intent(this, UserProfile.class));
                overridePendingTransition(0, 0);
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
                .setTitle("Reset Budget Tracker")
                .setMessage("Are you sure you want to reset everything?")
                .setPositiveButton("Reset", (dialog, which) -> resetBudgetData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetBudgetData() {
        totalBudget = 0; foodTotal = 0; transportTotal = 0; stayTotal = 0;
        saveBudgetData();
        transactionListContainer.removeAllViews();
        updateBudgetUI();
    }

    private void showSetBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Initial Budget");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Initial amount in ₱");
        builder.setView(input);
        builder.setPositiveButton("Set", (d, w) -> {
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
        builder.setTitle("Add More Funds");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount to add in ₱");
        builder.setView(input);
        builder.setPositiveButton("Add", (d, w) -> {
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
                .setTitle("Log Expense")
                .setItems(categories, (dialog, which) -> showAmountInputDialog(categories[which]))
                .show();
    }

    private void showAmountInputDialog(String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New " + category + " Expense");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount in ₱");
        builder.setView(input);
        builder.setPositiveButton("Deduct", (d, w) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                addExpense(category, Double.parseDouble(val));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addExpense(String category, double amount) {
        if (category.equals("Food")) foodTotal += amount;
        else if (category.equals("Transport")) transportTotal += amount;
        else if (category.equals("Stay")) stayTotal += amount;
        saveBudgetData();
        addTransactionToUI(category, amount);
        updateBudgetUI();
    }

    private void addTransactionToUI(String category, double amount) {
        View transactionView = getLayoutInflater().inflate(R.layout.item_transaction, transactionListContainer, false);
        ((TextView) transactionView.findViewById(R.id.tv_transaction_name)).setText(category);
        ((TextView) transactionView.findViewById(R.id.tv_transaction_amount)).setText("-₱" + String.format("%.2f", amount));
        ImageView icon = transactionView.findViewById(R.id.iv_transaction_icon);
        if (category.equals("Food")) icon.setImageResource(R.drawable.ic_food);
        else if (category.equals("Transport")) icon.setImageResource(R.drawable.ic_transport);
        else if (category.equals("Stay")) icon.setImageResource(R.drawable.ic_hotel);
        transactionView.findViewById(R.id.btn_delete_transaction).setOnClickListener(v -> {
            removeExpense(category, amount);
            transactionListContainer.removeView(transactionView);
        });
        transactionListContainer.addView(transactionView, 0);
    }

    private void removeExpense(String category, double amount) {
        if (category.equals("Food")) foodTotal -= amount;
        else if (category.equals("Transport")) transportTotal -= amount;
        else if (category.equals("Stay")) stayTotal -= amount;
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
        if (btnEditBudget != null) btnEditBudget.setVisibility(totalBudget > 0 ? View.VISIBLE : View.GONE);
        if (remaining < 0) tvRemainingBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        else tvRemainingBalance.setTextColor(getResources().getColor(android.R.color.white));
    }
}
