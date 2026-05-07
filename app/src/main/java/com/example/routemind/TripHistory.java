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

        View.OnClickListener detailsListener = v -> startActivity(new Intent(TripHistory.this, ItineraryDetails.class));

        MaterialCardView card1 = findViewById(R.id.card_itinerary1);
        MaterialCardView card2 = findViewById(R.id.card_itinerary2);
        MaterialCardView card3 = findViewById(R.id.card_itinerary3);

        if (card1 != null) card1.setOnClickListener(detailsListener);
        if (card2 != null) card2.setOnClickListener(detailsListener);
        if (card3 != null) card3.setOnClickListener(detailsListener);

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_trip_history);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) navigateTo(HomePage.class);
            else if (id == R.id.nav_activities) navigateTo(BudgetTracker.class);
            else if (id == R.id.nav_maps) navigateTo(TripActivity.class);
            else if (id == R.id.nav_user_profile) navigateTo(UserProfile.class);
            return id == R.id.nav_trip_history;
        });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(0, 0);
        finish();
    }
}
