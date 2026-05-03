package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

public class TourDetailsActivity extends AppCompatActivity {

    private String title, description, itinerary, imageUrl;
    private double price;
    private static final String PREF_NAME = "BudgetPrefs";

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
        price = getIntent().getDoubleExtra("PRICE", 0);

        // Bind views
        ((TextView) findViewById(R.id.tv_details_title)).setText(title);
        ((TextView) findViewById(R.id.tv_details_description)).setText(description);
        ((TextView) findViewById(R.id.tv_details_itinerary)).setText(itinerary);
        ((TextView) findViewById(R.id.tv_details_price)).setText("₱" + String.format("%.2f", price));
        
        ImageView ivDetailsImage = findViewById(R.id.iv_details_image);
        
        // Load image using Glide if URL exists, fallback to placeholder
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
            transportTotal += price;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("transportTotal", Double.doubleToLongBits(transportTotal));
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
}
