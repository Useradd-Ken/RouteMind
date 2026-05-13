package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TourDetailsActivity extends AppCompatActivity {

    private String title, description, itinerary, imageUrl, category, priceBreakdown;
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
        toolbar.setNavigationOnClickListener(v -> finish());

        Intent intent = getIntent();
        title = intent.getStringExtra("TITLE");
        description = intent.getStringExtra("DESCRIPTION");
        itinerary = intent.getStringExtra("ITINERARY");
        imageUrl = intent.getStringExtra("IMAGE_URL");
        category = intent.getStringExtra("CATEGORY");
        price = intent.getDoubleExtra("PRICE", 0);
        priceBreakdown = intent.getStringExtra("PRICE_BREAKDOWN");

        ((TextView) findViewById(R.id.tv_details_title)).setText(title);
        ((TextView) findViewById(R.id.tv_details_description)).setText(description);
        ((TextView) findViewById(R.id.tv_details_price)).setText(String.format("₱%,.2f", price));
        ((TextView) findViewById(R.id.tv_details_category)).setText(category != null ? category : "Activity");
        
        TextView tvBreakdown = findViewById(R.id.tv_details_price_breakdown);
        if (priceBreakdown != null && !priceBreakdown.isEmpty()) {
            tvBreakdown.setText(priceBreakdown);
        } else {
            tvBreakdown.setText("Price breakdown not available.");
        }
        
        ImageView ivDetailsImage = findViewById(R.id.iv_details_image);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.routemind)
                .error(R.drawable.routemind)
                .into(ivDetailsImage);

        setupItinerary();

        findViewById(R.id.btn_book_now).setOnClickListener(v -> bookTour());
        findViewById(R.id.btn_edit_itinerary).setOnClickListener(v -> {
            Snackbar.make(v, "Itinerary editing coming soon!", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setupItinerary() {
        RecyclerView rv = findViewById(R.id.rv_itinerary);
        View fallback = findViewById(R.id.cv_itinerary_fallback);
        
        try {
            JSONArray array = new JSONArray(itinerary);
            if (array.length() > 0) {
                List<ItineraryStep> steps = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    steps.add(new ItineraryStep(
                            obj.optString("time"),
                            obj.optString("activity"),
                            obj.optString("location")
                    ));
                }
                
                rv.setLayoutManager(new LinearLayoutManager(this));
                rv.setAdapter(new ItineraryAdapter(steps));
                rv.setVisibility(View.VISIBLE);
                fallback.setVisibility(View.GONE);
            } else {
                showFallbackText();
            }
        } catch (Exception e) {
            Log.e("TourDetails", "JSON Parse error", e);
            showFallbackText();
        }
    }

    private void showFallbackText() {
        findViewById(R.id.rv_itinerary).setVisibility(View.GONE);
        findViewById(R.id.cv_itinerary_fallback).setVisibility(View.VISIBLE);
        TextView tvItinerary = findViewById(R.id.tv_details_itinerary);
        if (itinerary == null || itinerary.isEmpty() || itinerary.equals("[]")) {
            tvItinerary.setText("No specific itinerary details provided.");
        } else {
            tvItinerary.setText(itinerary); // Show as is if parsing failed but text exists
        }
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

            Snackbar.make(findViewById(android.R.id.content), "Added to your trip!", Snackbar.LENGTH_LONG).show();
            findViewById(android.R.id.content).postDelayed(() -> {
                Intent backIntent = new Intent(this, BudgetTracker.class);
                backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(backIntent);
                finish();
            }, 1500);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Insufficient balance in your budget.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveTransaction(SharedPreferences prefs, String category, double amount) {
        try {
            JSONArray array = new JSONArray(prefs.getString(PREF_TRANSACTIONS, "[]"));
            JSONObject obj = new JSONObject();
            obj.put("category", category);
            obj.put("amount", amount);
            obj.put("title", title);
            obj.put("timestamp", System.currentTimeMillis());
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

    private static class ItineraryStep {
        String time, activity, location;
        ItineraryStep(String t, String a, String l) { this.time = t; this.activity = a; this.location = l; }
    }

    private static class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {
        private final List<ItineraryStep> steps;
        ItineraryAdapter(List<ItineraryStep> steps) { this.steps = steps; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_step, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ItineraryStep step = steps.get(position);
            holder.tvTime.setText(step.time);
            holder.tvActivity.setText(step.activity);
            holder.tvLocation.setText(step.location);
            holder.line.setVisibility(position == steps.size() - 1 ? View.INVISIBLE : View.VISIBLE);
        }
        @Override public int getItemCount() { return steps.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvActivity, tvLocation;
            View line;
            ViewHolder(View v) {
                super(v);
                tvTime = v.findViewById(R.id.tv_step_time);
                tvActivity = v.findViewById(R.id.tv_step_activity);
                tvLocation = v.findViewById(R.id.tv_step_location);
                line = v.findViewById(R.id.timeline_line);
            }
        }
    }
}
