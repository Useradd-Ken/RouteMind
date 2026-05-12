package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

    private RecyclerView rvItinerary;
    private MaterialCardView cvItineraryFallback;
    private TextView tvItineraryFallback;
    private MaterialButton btnEditItinerary;
    private List<ItineraryStep> stepsList = new ArrayList<>();
    private ItineraryAdapter itineraryAdapter;
    private boolean isEditMode = false;

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

        initViews();
        displayData();

        findViewById(R.id.btn_book_now).setOnClickListener(v -> bookTour());

        btnEditItinerary.setOnClickListener(v -> toggleEditMode());
    }

    private void initViews() {
        rvItinerary = findViewById(R.id.rv_itinerary);
        cvItineraryFallback = findViewById(R.id.cv_itinerary_fallback);
        tvItineraryFallback = findViewById(R.id.tv_details_itinerary);
        btnEditItinerary = findViewById(R.id.btn_edit_itinerary);
    }

    private void toggleEditMode() {
        if (itineraryAdapter == null) return;

        isEditMode = !isEditMode;
        itineraryAdapter.setEditMode(isEditMode);

        if (isEditMode) {
            btnEditItinerary.setText("Done");
            btnEditItinerary.setIconResource(R.drawable.ic_add); // Using ic_add as a placeholder for checkmark if not exists
        } else {
            btnEditItinerary.setText("Edit");
            btnEditItinerary.setIconResource(R.drawable.ic_edit);
            updateItineraryString();
        }
    }

    private void updateItineraryString() {
        try {
            JSONArray array = new JSONArray();
            for (ItineraryStep step : stepsList) {
                JSONObject obj = new JSONObject();
                obj.put("time", step.getTime());
                obj.put("activity", step.getActivity());
                obj.put("location", step.getLocation());
                array.put(obj);
            }
            itinerary = array.toString();
        } catch (Exception e) {
            Log.e("TourDetails", "Error updating itinerary string", e);
        }
    }

    private void displayData() {
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

        setupItinerary();
    }

    private void setupItinerary() {
        if (itinerary == null || itinerary.isEmpty() || itinerary.equals("[]")) {
            rvItinerary.setVisibility(View.GONE);
            cvItineraryFallback.setVisibility(View.VISIBLE);
            tvItineraryFallback.setText("No itinerary provided.");
            btnEditItinerary.setVisibility(View.GONE);
            return;
        }

        try {
            JSONArray stepsArray = new JSONArray(itinerary);
            stepsList.clear();
            
            for (int i = 0; i < stepsArray.length(); i++) {
                JSONObject stepObj = stepsArray.getJSONObject(i);
                stepsList.add(new ItineraryStep(
                    stepObj.optString("time", ""),
                    stepObj.optString("activity", ""),
                    stepObj.optString("location", "")
                ));
            }

            if (!stepsList.isEmpty()) {
                rvItinerary.setVisibility(View.VISIBLE);
                cvItineraryFallback.setVisibility(View.GONE);
                btnEditItinerary.setVisibility(View.VISIBLE);

                rvItinerary.setLayoutManager(new LinearLayoutManager(this));
                itineraryAdapter = new ItineraryAdapter(stepsList, position -> {
                    stepsList.remove(position);
                    itineraryAdapter.notifyItemRemoved(position);
                    itineraryAdapter.notifyItemRangeChanged(position, stepsList.size());
                    if (stepsList.isEmpty()) {
                        rvItinerary.setVisibility(View.GONE);
                        cvItineraryFallback.setVisibility(View.VISIBLE);
                        tvItineraryFallback.setText("Itinerary is empty.");
                        btnEditItinerary.setVisibility(View.GONE);
                    }
                });
                rvItinerary.setAdapter(itineraryAdapter);
            } else {
                throw new Exception("Empty list");
            }

        } catch (Exception e) {
            rvItinerary.setVisibility(View.GONE);
            cvItineraryFallback.setVisibility(View.VISIBLE);
            tvItineraryFallback.setText(itinerary);
            btnEditItinerary.setVisibility(View.GONE);
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

            // Ensure current itinerary state is saved if it was edited
            updateItineraryString();

            saveTransaction(prefs, cat, price);
            updateSavedSuggestionsAfterRemoval(prefs, title);
            editor.apply();

            Snackbar.make(findViewById(android.R.id.content), "Tour added to your trip!", Snackbar.LENGTH_LONG).show();
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
            obj.put("title", title); // Optional: adding title to transaction context
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
