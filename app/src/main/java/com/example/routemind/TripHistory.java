package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class TripHistory extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trip_history);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Handle card clicks to open details with unique IDs
        MaterialCardView card1 = findViewById(R.id.card_itinerary1);
        MaterialCardView card2 = findViewById(R.id.card_itinerary2);
        MaterialCardView card3 = findViewById(R.id.card_itinerary3);

        if (card1 != null) card1.setOnClickListener(v -> openDetails("palawan_trip_1"));
        if (card2 != null) card2.setOnClickListener(v -> openDetails("cebu_trip_1"));
        if (card3 != null) card3.setOnClickListener(v -> openDetails("baguio_trip_1"));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_trip_history);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomePage.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), BudgetTracker.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
                startActivity(new Intent(getApplicationContext(), TripActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_trip_history) {
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void openDetails(String itineraryId) {
        Intent intent = new Intent(TripHistory.this, ItineraryDetails.class);
        intent.putExtra("ITINERARY_ID", itineraryId);
        startActivity(intent);
    }
}