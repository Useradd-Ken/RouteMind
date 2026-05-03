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

        // Initialize with sample trips
        resetUI();

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
            
            // Link to details if it's a trip
            if (item.itinerary != null) {
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(HomePage.this, ItineraryDetails.class);
                    intent.putExtra("destination", item.title);
                    intent.putExtra("itinerary", item.itinerary);
                    intent.putExtra("ITINERARY_ID", "sample_" + item.title.toLowerCase().replace(" ", "_"));
                    startActivity(intent);
                });
            }
            
            resultsContainer.addView(itemView);
        }
    }

    private void resetUI() {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText("Recommended for You");
        planTripCard.setVisibility(View.VISIBLE);
        
        // Show Featured Itineraries
        for (ExploreItem item : getSampleTrips()) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_explore, resultsContainer, false);
            ((TextView) itemView.findViewById(R.id.item_title)).setText(item.title);
            ((TextView) itemView.findViewById(R.id.item_subtitle)).setText(item.subtitle);
            ((ImageView) itemView.findViewById(R.id.item_image)).setImageResource(item.imageRes);
            
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HomePage.this, ItineraryDetails.class);
                intent.putExtra("destination", item.title);
                intent.putExtra("itinerary", item.itinerary);
                intent.putExtra("ITINERARY_ID", "sample_" + item.title.toLowerCase().replace(" ", "_"));
                startActivity(intent);
            });
            resultsContainer.addView(itemView);
        }
    }

    private List<ExploreItem> getSampleTrips() {
        List<ExploreItem> list = new ArrayList<>();
        list.add(new ExploreItem("Batanes Escape", "3 Days - Culture & Nature", R.drawable.ic_map, 
            "Day 1: Arrival at Basco, Mt. Carmel Chapel, Fundacion Pacita.\n" +
            "Day 2: Batan South Tour - Chawa View Deck, Mahatao Pier, Tayid Lighthouse.\n" +
            "Day 3: Sabtang Island - Nakabuang Beach, Savidug Stone Houses."));
        
        list.add(new ExploreItem("Siargao Surfing", "4 Days - Adventure", R.drawable.ic_map,
            "Day 1: Cloud 9 Surfing, Shaka Cafe.\n" +
            "Day 2: Guyam, Daku, and Naked Island Hopping.\n" +
            "Day 3: Sugba Lagoon & Magpupungko Rock Pools.\n" +
            "Day 4: Coconut Mountain View & Maasin River."));

        list.add(new ExploreItem("Boracay Bliss", "3 Days - Relaxation", R.drawable.ic_map,
            "Day 1: White Beach Sunset, Dinner at D'Mall.\n" +
            "Day 2: Island Hopping (Puka Beach, Crystal Cove).\n" +
            "Day 3: Parasailing and Helmet Diving."));

        list.add(new ExploreItem("El Nido Adventure", "4 Days - Nature", R.drawable.ic_map,
            "Day 1: Nacpan Beach and Lio Tourism Estate.\n" +
            "Day 2: Tour A (Big Lagoon, Secret Lagoon).\n" +
            "Day 3: Tour C (Hidden Beach, Helicopter Island).\n" +
            "Day 4: Taraw Cliff Hike and Canopy Walk."));

        list.add(new ExploreItem("Baguio City Tour", "2 Days - Refreshing", R.drawable.ic_map,
            "Day 1: Burnham Park, Session Road, Cathedral.\n" +
            "Day 2: Mines View Park, Wright Park, The Mansion."));

        list.add(new ExploreItem("Cebu Heritage", "3 Days - History & Sea", R.drawable.ic_map,
            "Day 1: Magellan's Cross, Fort San Pedro, Taoist Temple.\n" +
            "Day 2: Oslob Whale Shark Watching & Sumilon Island.\n" +
            "Day 3: Kawasan Falls Canyoneering."));

        return list;
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
        list.addAll(getSampleTrips());
        return list;
    }

    private static class ExploreItem {
        String title, subtitle, itinerary;
        int imageRes;
        ExploreItem(String title, String subtitle, int imageRes) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageRes = imageRes;
        }
        ExploreItem(String title, String subtitle, int imageRes, String itinerary) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageRes = imageRes;
            this.itinerary = itinerary;
        }
    }
}
