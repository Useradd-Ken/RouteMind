package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class sqlite extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sqlite);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_activities);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                navigateTo(HomePage.class);
                return true;
            } else if (id == R.id.nav_activities) {
                return true;
            } else if (id == R.id.nav_itineraries) {
                navigateTo(ItinerariesActivity.class);
                return true;
            } else if (id == R.id.nav_maps) {
                navigateTo(TripActivity.class);
                return true;
            } else if (id == R.id.nav_trip_history) {
                navigateTo(TripHistory.class);
                return true;
            } else if (id == R.id.nav_user_profile) {
                navigateTo(UserProfile.class);
                return true;
            }
            return false;
        });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(getApplicationContext(), cls));
        overridePendingTransition(0, 0);
        finish();
    }
}