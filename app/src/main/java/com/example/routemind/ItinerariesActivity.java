package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ItinerariesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ItineraryAdapter adapter;
    private List<Itinerary> itineraryList;
    private FirebaseFirestore db;
    private ProgressBar loadingSpinner;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itineraries);

        db = FirebaseFirestore.getInstance();
        itineraryList = new ArrayList<>();
        
        loadingSpinner = findViewById(R.id.loading_spinner);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        recyclerView = findViewById(R.id.rv_itineraries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new ItineraryAdapter(this, itineraryList);
        recyclerView.setAdapter(adapter);

        setupBottomNavigation();
        fetchItineraries();

        swipeRefresh.setOnRefreshListener(this::fetchItineraries);
    }

    private void fetchItineraries() {
        loadingSpinner.setVisibility(View.VISIBLE);
        db.collection("itineraries")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itineraryList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Itinerary itinerary = document.toObject(Itinerary.class);
                        itinerary.setId(document.getId());
                        itineraryList.add(itinerary);
                    }
                    adapter.notifyDataSetChanged();
                    loadingSpinner.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("ItinerariesActivity", "Error fetching itineraries", e);
                    loadingSpinner.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_itineraries);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) navigateTo(HomePage.class);
            else if (id == R.id.nav_activities) navigateTo(BudgetTracker.class);
            else if (id == R.id.nav_itineraries) return true;
            else if (id == R.id.nav_maps) navigateTo(TripActivity.class);
            else if (id == R.id.nav_trip_history) navigateTo(TripHistory.class);
            else if (id == R.id.nav_user_profile) navigateTo(UserProfile.class);
            return true;
        });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(0, 0);
        finish();
    }
}
