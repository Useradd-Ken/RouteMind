package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private LinearLayout resultsContainer;
    private TextView tvSectionTitle;
    private View planTripCard;
    private Button btnGenerateNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        resultsContainer = findViewById(R.id.results_container);
        tvSectionTitle = findViewById(R.id.tv_section_title);
        planTripCard = findViewById(R.id.plan_trip_card);
        btnGenerateNow = findViewById(R.id.btn_generate_now);

        View.OnClickListener toTripActivity = v -> {
            startActivity(new Intent(HomePage.this, TripActivity.class));
        };

        if (planTripCard != null) {
            planTripCard.setOnClickListener(toTripActivity);
        }

        if (btnGenerateNow != null) {
            btnGenerateNow.setOnClickListener(toTripActivity);
        }

        EditText searchDestinations = findViewById(R.id.search_destinations);
        searchDestinations.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performSearch(searchDestinations.getText().toString());
                return true;
            }
            return false;
        });

        findViewById(R.id.category_food).setOnClickListener(v -> showExploreResults("Recommended Food", getFoodData()));
        findViewById(R.id.category_stays).setOnClickListener(v -> showExploreResults("Recommended Stays", getStaysData()));
        findViewById(R.id.category_places).setOnClickListener(v -> showExploreResults("Popular Places", getPlacesData()));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                resetUI();
                return true;
            } else if (itemId == R.id.nav_activities) {
                startActivity(new Intent(HomePage.this, BudgetTracker.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_trip_history) {
                startActivity(new Intent(HomePage.this, TripHistory.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_maps) {
                startActivity(new Intent(HomePage.this, TripActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_user_profile) {
                startActivity(new Intent(HomePage.this, UserProfile.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            resetUI();
            return;
        }
        List<ExploreItem> results = new ArrayList<>();
        for (ExploreItem item : getAllData()) {
            if (item.title.toLowerCase().contains(query.toLowerCase())) {
                results.add(item);
            }
        }
        showExploreResults("Search Results", results);
    }

    private void showExploreResults(String title, List<ExploreItem> items) {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText(title);
        planTripCard.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ExploreItem item : items) {
            View itemView = inflater.inflate(R.layout.item_explore, resultsContainer, false);
            ((TextView) itemView.findViewById(R.id.item_title)).setText(item.title);
            ((TextView) itemView.findViewById(R.id.item_subtitle)).setText(item.subtitle);
            ((ImageView) itemView.findViewById(R.id.item_image)).setImageResource(item.imageRes);
            resultsContainer.addView(itemView);
        }
    }

    private void resetUI() {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText("Recommended for You");
        planTripCard.setVisibility(View.VISIBLE);
    }

    private List<ExploreItem> getFoodData() {
        List<ExploreItem> list = new ArrayList<>();
        list.add(new ExploreItem("Jollibee", "Fast Food - 0.5km away", R.drawable.ic_food));
        list.add(new ExploreItem("Mang Inasal", "Filipino Cuisine - 1.2km away", R.drawable.ic_food));
        return list;
    }

    private List<ExploreItem> getStaysData() {
        List<ExploreItem> list = new ArrayList<>();
        list.add(new ExploreItem("Grand Hotel", "Luxury Stay - 2.0km away", R.drawable.ic_hotel));
        list.add(new ExploreItem("Budget Inn", "Affordable - 1.5km away", R.drawable.ic_hotel));
        return list;
    }

    private List<ExploreItem> getPlacesData() {
        List<ExploreItem> list = new ArrayList<>();
        list.add(new ExploreItem("City Park", "Nature - 3.0km away", R.drawable.ic_map));
        list.add(new ExploreItem("Historical Museum", "Culture - 1.0km away", R.drawable.ic_map));
        return list;
    }

    private List<ExploreItem> getAllData() {
        List<ExploreItem> list = new ArrayList<>();
        list.addAll(getFoodData());
        list.addAll(getStaysData());
        list.addAll(getPlacesData());
        return list;
    }

    private static class ExploreItem {
        String title, subtitle;
        int imageRes;
        ExploreItem(String title, String subtitle, int imageRes) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageRes = imageRes;
        }
    }
}