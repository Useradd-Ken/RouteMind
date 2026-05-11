package com.example.routemind;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TripHistory extends AppCompatActivity {

    private static final String TAG = "TripHistory";
    LinearLayout llTripList;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_trip_history);
            
            dbHelper = new DatabaseHelper(this);
            llTripList = findViewById(R.id.llTripList);

            View mainView = findViewById(R.id.main);
            if (mainView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
            }

            loadTripHistory();
            setupNavigation();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading Trip History", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_trip_history);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomePage.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_activities) {
                    startActivity(new Intent(this, BudgetTracker.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_maps) {
                    startActivity(new Intent(this, TripActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_trip_history) {
                    return true;
                } else if (id == R.id.nav_user_profile) {
                    startActivity(new Intent(this, UserProfile.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadTripHistory() {
        if (llTripList == null) return;
        llTripList.removeAllViews();
        
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            // Get trips ordered by latest first
            cursor = db.query(DatabaseHelper.TABLE_TRIPS, null, null, null, null, null, DatabaseHelper.COLUMN_TRIP_ID + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRIP_ID);
                    int destIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DESTINATION);
                    int startIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_START_DATE);
                    int endIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_END_DATE);
                    int itinIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ITINERARY);

                    if (idIndex == -1 || destIndex == -1) continue;

                    int id = cursor.getInt(idIndex);
                    String destination = cursor.getString(destIndex);
                    String startDate = startIndex != -1 ? cursor.getString(startIndex) : "Unknown Date";
                    String endDate = endIndex != -1 ? cursor.getString(endIndex) : "";
                    String itinerary = itinIndex != -1 ? cursor.getString(itinIndex) : "";

                    View tripView = LayoutInflater.from(this).inflate(R.layout.item_itinerary, llTripList, false);
                    
                    TextView tvTitle = tripView.findViewById(R.id.tv_trip_title);
                    TextView tvDate = tripView.findViewById(R.id.tv_trip_date);
                    
                    if (tvTitle != null) tvTitle.setText(destination);
                    if (tvDate != null) {
                        String dateStr = startDate + (endDate != null && !endDate.isEmpty() ? " - " + endDate : "");
                        tvDate.setText(dateStr);
                    }

                    tripView.setOnClickListener(v -> {
                        Intent intent = new Intent(TripHistory.this, ItineraryDetails.class);
                        intent.putExtra("itinerary", itinerary);
                        intent.putExtra("destination", destination);
                        intent.putExtra("ITINERARY_ID", String.valueOf(id));
                        startActivity(intent);
                    });

                    llTripList.addView(tripView);
                } while (cursor.moveToNext());
            } else {
                showNoTripsMessage();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading trips from database", e);
            showNoTripsMessage();
        } finally {
            if (cursor != null) cursor.close();
            // We don't close the db here as it might be managed/cached by SQLiteOpenHelper
        }
    }

    private void showNoTripsMessage() {
        TextView tvNoTrips = new TextView(this);
        tvNoTrips.setText("No trips found in history.");
        tvNoTrips.setPadding(50, 50, 50, 50);
        llTripList.addView(tvNoTrips);
    }
}
