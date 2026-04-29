package com.example.routemind;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class TripHistory extends AppCompatActivity {

    LinearLayout llTripList;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trip_history);
        
<<<<<<< Updated upstream
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
=======
        dbHelper = new DatabaseHelper(this);
        llTripList = findViewById(R.id.llTripList);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
>>>>>>> Stashed changes
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadTripHistory();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_trip_history);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
<<<<<<< Updated upstream
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), sqlite.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
=======
                startActivity(new Intent(getApplicationContext(), HomePage.class));
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), BudgetTracker.class));
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
                startActivity(new Intent(getApplicationContext(), TripActivity.class));
                finish();
>>>>>>> Stashed changes
                return true;
            } else if (id == R.id.nav_trip_history) {
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadTripHistory() {
        llTripList.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("trips", null, null, null, null, null, "id DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String destination = cursor.getString(cursor.getColumnIndexOrThrow("destination"));
                String startDate = cursor.getString(cursor.getColumnIndexOrThrow("start_date"));
                String endDate = cursor.getString(cursor.getColumnIndexOrThrow("end_date"));
                String itinerary = cursor.getString(cursor.getColumnIndexOrThrow("itinerary"));

                View tripView = LayoutInflater.from(this).inflate(R.layout.item_trip_history, llTripList, false);
                
                TextView tvTitle = tripView.findViewById(R.id.tv_trip_title);
                TextView tvDate = tripView.findViewById(R.id.tv_trip_date);
                
                tvTitle.setText(destination);
                tvDate.setText(startDate + " - " + endDate);

                tripView.setOnClickListener(v -> {
                    Intent intent = new Intent(TripHistory.this, ItineraryDetails.class);
                    intent.putExtra("itinerary", itinerary);
                    intent.putExtra("destination", destination);
                    startActivity(intent);
                });

                llTripList.addView(tripView);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            TextView tvNoTrips = new TextView(this);
            tvNoTrips.setText("No trips found in history.");
            tvNoTrips.setPadding(50, 50, 50, 50);
            llTripList.addView(tvNoTrips);
        }
    }
}