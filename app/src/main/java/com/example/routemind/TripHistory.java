package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripHistory extends AppCompatActivity {

    private LinearLayout historyContainer;
    private ProgressBar loader;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        historyContainer = findViewById(R.id.history_container);
        loader = findViewById(R.id.history_loader);
        tvEmpty = findViewById(R.id.tv_history_empty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadTripHistory();
        setupBottomNavigation();
    }

    private void loadTripHistory() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            loader.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Please login to see your history.");
            return;
        }

        loader.setVisibility(View.VISIBLE);
        db.collection("booked_itineraries")
                .whereEqualTo("userId", user.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    loader.setVisibility(View.GONE);
                    if (error != null) {
                        Log.e("TripHistory", "Error loading history", error);
                        return;
                    }

                    historyContainer.removeAllViews();
                    if (value == null || value.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);
                    LayoutInflater inflater = LayoutInflater.from(TripHistory.this);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : value) {
                        Itinerary item = doc.toObject(Itinerary.class);

                        View v = inflater.inflate(R.layout.item_itinerary, historyContainer, false);
                        ((TextView) v.findViewById(R.id.tv_trip_title)).setText(item.getTitle());

                        String dateStr = item.getDestination() != null ? item.getDestination() : "Unknown Destination";
                        if (item.getTimestamp() > 0) {
                            dateStr += " • " + sdf.format(new Date(item.getTimestamp()));
                        }
                        ((TextView) v.findViewById(R.id.tv_trip_date)).setText(dateStr);

                        TextView tvStatus = v.findViewById(R.id.tv_trip_status);
                        tvStatus.setText(item.getCategory() != null ? item.getCategory() : "Booked");

                        v.setOnClickListener(view -> {
                            Intent intent = new Intent(TripHistory.this, ItineraryDetails.class);
                            intent.putExtra("itinerary", item);
                            startActivity(intent);
                        });

                        historyContainer.addView(v);
                    }
                });
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
