package com.usc.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.example.routemind.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class BudgetTracker extends AppCompatActivity {

    private TextView tvRemainingBalance, tvTotalSpent, tvTotalLimit;
    private TextView tvFoodAmount, tvTransportAmount, tvStayAmount;
    private LinearProgressIndicator budgetProgress;
    private LinearLayout transactionListContainer;
    private ImageView btnEditBudget;

    private double foodTotal = 0, transportTotal = 0, stayTotal = 0;
    private double totalBudget = 0;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "BudgetPrefs";

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
        btnEditBudget = findViewById(R.id.btn_edit_budget);

        findViewById(R.id.btn_set_budget).setOnClickListener(v -> showSetBudgetDialog());
        findViewById(R.id.btn_reset_budget).setOnClickListener(v -> showResetConfirmationDialog());
        btnEditBudget.setOnClickListener(v -> showAddFundsDialog());

        View.OnClickListener expenseListener = v -> {
            if (totalBudget <= 0) {
                Toast.makeText(this, "Please set your budget first!", Toast.LENGTH_SHORT).show();
                showSetBudgetDialog();
            } else {
                showAddExpenseDialog();
            }
        };

        findViewById(R.id.btn_add_expense_top).setOnClickListener(expenseListener);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_activities);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(BudgetTracker.this, HomePage.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_activities) {
                return true;
            }
            return itemId == R.id.nav_trip_history || itemId == R.id.nav_maps || itemId == R.id.nav_user_profile;
        });

        updateBudgetUI();
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
                .setMessage("Are you sure you want to reset everything? This will delete your budget, expenses, and history.")
                .setPositiveButton("Reset", (dialog, which) -> resetBudgetData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetBudgetData() {
        totalBudget = 0;
        foodTotal = 0;
        transportTotal = 0;
        stayTotal = 0;
        
        saveBudgetData();
        transactionListContainer.removeAllViews();
        updateBudgetUI();
        
        Toast.makeText(this, "Budget tracker has been reset", Toast.LENGTH_SHORT).show();
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
                double amount = Double.parseDouble(val);
                totalBudget += amount;
                saveBudgetData();
                updateBudgetUI();
                Toast.makeText(this, "₱" + (int)amount + " added to budget", Toast.LENGTH_SHORT).show();
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
                double amount = Double.parseDouble(val);
                addExpense(category, amount);
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
        else icon.setImageResource(R.drawable.ic_hotel);

        transactionView.findViewById(R.id.btn_delete_transaction).setOnClickListener(v -> {
            removeExpense(category, amount, transactionView);
        });

        transactionListContainer.addView(transactionView, 0);
    }

    private void removeExpense(String category, double amount, View view) {
        if (category.equals("Food")) foodTotal -= amount;
        else if (category.equals("Transport")) transportTotal -= amount;
        else if (category.equals("Stay")) stayTotal -= amount;

        saveBudgetData();
        transactionListContainer.removeView(view);
        updateBudgetUI();
        Toast.makeText(this, "Transaction removed", Toast.LENGTH_SHORT).show();
    }

    private void updateBudgetUI() {
        double spent = foodTotal + transportTotal + stayTotal;
        double remaining = totalBudget - spent;
        int progress = (totalBudget > 0) ? (int) ((spent / totalBudget) * 100) : 0;

        tvFoodAmount.setText("₱" + (int)foodTotal);
        tvTransportAmount.setText("₱" + (int)transportTotal);
        tvStayAmount.setText("₱" + (int)stayTotal);
        tvTotalSpent.setText("₱" + (int)spent);
        tvTotalLimit.setText("₱" + (int)totalBudget);
        tvRemainingBalance.setText("₱" + String.format("%.2f", remaining));
        budgetProgress.setProgress(progress);

        btnEditBudget.setVisibility(totalBudget > 0 ? View.VISIBLE : View.GONE);

        if (remaining < 0) tvRemainingBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        else tvRemainingBalance.setTextColor(getResources().getColor(android.R.color.white));
    }
}