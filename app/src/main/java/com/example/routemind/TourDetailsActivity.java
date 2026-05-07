package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
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

        Intent intent = getIntent();
        title = intent.getStringExtra("TITLE");
        description = intent.getStringExtra("DESCRIPTION");
        itinerary = intent.getStringExtra("ITINERARY");
        imageUrl = intent.getStringExtra("IMAGE_URL");
        category = intent.getStringExtra("CATEGORY");
        price = intent.getDoubleExtra("PRICE", 0);

        ((TextView) findViewById(R.id.tv_details_title)).setText(title);
        ((TextView) findViewById(R.id.tv_details_description)).setText(description);
        ((TextView) findViewById(R.id.tv_details_itinerary)).setText(itinerary);
        ((TextView) findViewById(R.id.tv_details_price)).setText(String.format("₱%.2f", price));
        
        ImageView ivDetailsImage = findViewById(R.id.iv_details_image);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.routemind)
                .error(R.drawable.routemind)
                .into(ivDetailsImage);

        findViewById(R.id.btn_book_now).setOnClickListener(v -> bookTour());
    }

    private void bookTour() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        double totalBudget = Double.longBitsToDouble(prefs.getLong("totalBudget", Double.doubleToLongBits(0)));
        double foodTotal = Double.longBitsToDouble(prefs.getLong("foodTotal", Double.doubleToLongBits(0)));
        double transportTotal = Double.longBitsToDouble(prefs.getLong("transportTotal", Double.doubleToLongBits(0)));
        double stayTotal = Double.longBitsToDouble(prefs.getLong("stayTotal", Double.doubleToLongBits(0)));

        double remaining = totalBudget - (foodTotal + transportTotal + stayTotal);

        if (remaining >= price) {
            SharedPreferences.Editor editor = prefs.edit();
            String cat = (category == null || category.isEmpty()) ? "Transport" : category;
            
            if (cat.equalsIgnoreCase("Food")) {
                foodTotal += price;
                editor.putLong("foodTotal", Double.doubleToLongBits(foodTotal));
            } else if (cat.equalsIgnoreCase("Stay")) {
                stayTotal += price;
                editor.putLong("stayTotal", Double.doubleToLongBits(stayTotal));
            } else {
                transportTotal += price;
                editor.putLong("transportTotal", Double.doubleToLongBits(transportTotal));
            }

            saveTransaction(prefs, cat, price);
            updateSavedSuggestionsAfterRemoval(prefs, title);
            editor.apply();

            Snackbar.make(findViewById(android.R.id.content), "Booking confirmed!", Snackbar.LENGTH_LONG).show();
            findViewById(android.R.id.content).postDelayed(() -> {
                Intent backIntent = new Intent(this, BudgetTracker.class);
                backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(backIntent);
                finish();
            }, 1500);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Insufficient balance.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveTransaction(SharedPreferences prefs, String category, double amount) {
        try {
            JSONArray array = new JSONArray(prefs.getString(PREF_TRANSACTIONS, "[]"));
            JSONObject obj = new JSONObject();
            obj.put("category", category);
            obj.put("amount", amount);
            array.put(obj);
            prefs.edit().putString(PREF_TRANSACTIONS, array.toString()).apply();
        } catch (Exception e) {
            Log.e("TourDetails", "Save error", e);
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
                if (!obj.optString("title").equals(titleToRemove)) newArray.put(obj);
            }
            prefs.edit().putString("savedSuggestions", newArray.toString()).apply();
        } catch (Exception ignored) {}
    }
}
