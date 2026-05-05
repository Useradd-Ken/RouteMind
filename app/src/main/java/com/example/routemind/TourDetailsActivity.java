package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

public class TourDetailsActivity extends AppCompatActivity {

    private String title, description, itinerary, imageUrl, category;
    private double price;
    private static final String PREF_NAME = "BudgetPrefs";
    private static final String PREF_TRANSACTIONS = "savedTransactions";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Get data from intent
        title = getIntent().getStringExtra("TITLE");
        description = getIntent().getStringExtra("DESCRIPTION");
        itinerary = getIntent().getStringExtra("ITINERARY");
        imageUrl = getIntent().getStringExtra("IMAGE_URL");
        category = getIntent().getStringExtra("CATEGORY");
        price = getIntent().getDoubleExtra("PRICE", 0);

        // Bind views
        ((TextView) findViewById(R.id.tv_details_title)).setText(title);
        ((TextView) findViewById(R.id.tv_details_description)).setText(description);
        ((TextView) findViewById(R.id.tv_details_itinerary)).setText(itinerary);
        ((TextView) findViewById(R.id.tv_details_price)).setText("₱" + String.format("%.2f", price));
        
        ImageView ivDetailsImage = findViewById(R.id.iv_details_image);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(ivDetailsImage);
        } else {
            ivDetailsImage.setImageResource(R.drawable.ic_launcher_background);
        }

        findViewById(R.id.btn_book_now).setOnClickListener(v -> bookTour());
    }

    private void bookTour() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        double totalBudget = Double.longBitsToDouble(sharedPreferences.getLong("totalBudget", Double.doubleToLongBits(0)));
        double foodTotal = Double.longBitsToDouble(sharedPreferences.getLong("foodTotal", Double.doubleToLongBits(0)));
        double transportTotal = Double.longBitsToDouble(sharedPreferences.getLong("transportTotal", Double.doubleToLongBits(0)));
        double stayTotal = Double.longBitsToDouble(sharedPreferences.getLong("stayTotal", Double.doubleToLongBits(0)));

        double totalSpent = foodTotal + transportTotal + stayTotal;
        double remaining = totalBudget - totalSpent;

        if (remaining >= price) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            
            // Apply expense to the correct category
            if (category == null || category.isEmpty()) category = "Transport"; // Fallback
            
            if (category.equalsIgnoreCase("Food")) {
                foodTotal += price;
                editor.putLong("foodTotal", Double.doubleToLongBits(foodTotal));
            } else if (category.equalsIgnoreCase("Stay")) {
                stayTotal += price;
                editor.putLong("stayTotal", Double.doubleToLongBits(stayTotal));
            } else {
                transportTotal += price;
                editor.putLong("transportTotal", Double.doubleToLongBits(transportTotal));
            }

            // Save transaction for history
            saveTransaction(sharedPreferences, category, price);

            // Remove from saved suggestions once booked
            updateSavedSuggestionsAfterRemoval(sharedPreferences, title);

            editor.apply();

            Toast.makeText(this, "Successfully booked: " + title, Toast.LENGTH_LONG).show();
            
            Intent intent = new Intent(this, BudgetTracker.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Insufficient budget to book this tour!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTransaction(SharedPreferences sharedPreferences, String category, double amount) {
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
            Log.e("TourDetails", "Error saving transaction", e);
        }
    }

    private void updateSavedSuggestionsAfterRemoval(SharedPreferences prefs, String titleToRemove) {
        String savedJson = prefs.getString("savedSuggestions", "");
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
            prefs.edit().putString("savedSuggestions", newArray.toString()).apply();
        } catch (Exception ignored) {}
    }
}
