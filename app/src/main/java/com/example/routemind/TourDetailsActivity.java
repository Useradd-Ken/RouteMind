package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TourDetailsActivity extends AppCompatActivity {

    private String title, description, itinerary, imageUrl, category, priceBreakdown;
    private double price;
    private boolean isBooked = false;
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
        priceBreakdown = intent.getStringExtra("PRICE_BREAKDOWN");
        isBooked = intent.getBooleanExtra("IS_BOOKED", false);

        ((TextView) findViewById(R.id.tv_details_title)).setText(title);
        ((TextView) findViewById(R.id.tv_details_description)).setText(description);
        ((TextView) findViewById(R.id.tv_details_price)).setText(String.format("₱%.2f", price));
        
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

        LinearLayout timelineContainer = findViewById(R.id.timeline_details_container);
        if (timelineContainer != null) {
            populateTimeline(timelineContainer);
            View defaultTextView = findViewById(R.id.tv_details_itinerary);
            if (defaultTextView != null) defaultTextView.setVisibility(View.GONE);
        }

        MaterialButton btnBook = findViewById(R.id.btn_book_now);
        if (isBooked) {
            btnBook.setVisibility(View.GONE);
        } else {
            btnBook.setOnClickListener(v -> bookTour());
        }
    }

    private void populateTimeline(LinearLayout container) {
        if (itinerary == null || itinerary.isEmpty()) return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        try {
            JSONArray array = null;
            if (itinerary.trim().startsWith("[")) {
                array = new JSONArray(itinerary);
            } else if (itinerary.trim().startsWith("{")) {
                JSONObject root = new JSONObject(itinerary);
                array = root.optJSONArray("itinerary");
                if (array == null) array = root.optJSONArray("activities");
            }
            
            if (array != null) {
                int lastDay = -1;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    int day = item.optInt("day", 1);
                    String time = item.optString("time", "");
                    String activity = item.optString("activity", "");
                    String location = item.optString("location", "");

                    if (day != lastDay) {
                        View dayView = inflater.inflate(R.layout.item_activity_row, container, false);
                        dayView.findViewById(R.id.tv_activity_time).setVisibility(View.INVISIBLE);
                        TextView tvDayTitle = dayView.findViewById(R.id.tv_activity_title);
                        tvDayTitle.setText("Day " + day);
                        tvDayTitle.setTextSize(18);
                        tvDayTitle.setTextColor(getResources().getColor(R.color.white));
                        dayView.findViewById(R.id.dot).setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.white)));
                        dayView.findViewById(R.id.tv_activity_subtitle).setVisibility(View.GONE);
                        container.addView(dayView);
                        lastDay = day;
                    }

                    View rowView = inflater.inflate(R.layout.item_activity_row, container, false);
                    ((TextView) rowView.findViewById(R.id.tv_activity_time)).setText(time);
                    ((TextView) rowView.findViewById(R.id.tv_activity_title)).setText(activity);
                    TextView tvSubtitle = rowView.findViewById(R.id.tv_activity_subtitle);
                    if (location != null && !location.isEmpty()) {
                        tvSubtitle.setText(location);
                        tvSubtitle.setVisibility(View.VISIBLE);
                    } else {
                        tvSubtitle.setVisibility(View.GONE);
                    }
                    if (i == array.length() - 1) rowView.findViewById(R.id.line_bottom).setVisibility(View.INVISIBLE);
                    container.addView(rowView);
                }
                return;
            }
        } catch (Exception e) {
            Log.e("TourDetails", "JSON parse error, falling back to text", e);
        }

        // Fallback to text splitting
        String[] rawLines = itinerary.split("\n");
        for (int i = 0; i < rawLines.length; i++) {
            String cleanLine = rawLines[i].trim();
            if (cleanLine.isEmpty()) continue;
            View rowView = inflater.inflate(R.layout.item_activity_row, container, false);
            TextView tvTime = rowView.findViewById(R.id.tv_activity_time);
            TextView tvActTitle = rowView.findViewById(R.id.tv_activity_title);
            if (i == 0) rowView.findViewById(R.id.line_top).setVisibility(View.INVISIBLE);
            if (i == rawLines.length - 1) rowView.findViewById(R.id.line_bottom).setVisibility(View.INVISIBLE);

            if (cleanLine.toLowerCase().startsWith("day")) {
                tvTime.setVisibility(View.INVISIBLE);
                tvActTitle.setText(cleanLine);
                tvActTitle.setTextSize(18);
                rowView.findViewById(R.id.dot).setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.white)));
            } else if (cleanLine.contains("•")) {
                String[] parts = cleanLine.split("•", 2);
                tvTime.setText(parts[0].trim());
                tvActTitle.setText(parts[1].trim());
            } else {
                tvTime.setText("--:--");
                tvActTitle.setText(cleanLine);
            }
            rowView.findViewById(R.id.tv_activity_subtitle).setVisibility(View.GONE);
            container.addView(rowView);
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
