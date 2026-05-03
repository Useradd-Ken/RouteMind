package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class HomePage extends AppCompatActivity {

    private LinearLayout resultsContainer;
    private TextView tvSectionTitle;
    private View planTripCard;
    private PhotonService photonService;
    private double currentLat = 14.5995;
    private double currentLon = 120.9842;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

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

        findViewById(R.id.btn_generate_now).setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, TripActivity.class));
        });

        Retrofit photonRetrofit = new Retrofit.Builder()
                .baseUrl("https://photon.komoot.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        photonService = photonRetrofit.create(PhotonService.class);

        EditText searchDestinations = findViewById(R.id.search_destinations);
        
        searchDestinations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() > 2) {
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, 500); 
                } else if (query.isEmpty()) {
                    resetUI();
                }
            }
        });

        searchDestinations.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchDestinations.getText().toString().trim();
                if (!query.isEmpty()) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        findViewById(R.id.category_food).setOnClickListener(v -> fetchCategoryData("restaurant", "Nearby Food"));
        findViewById(R.id.category_stays).setOnClickListener(v -> fetchCategoryData("hotel", "Nearby Stays"));
        findViewById(R.id.category_places).setOnClickListener(v -> fetchCategoryData("tourist_attraction", "Popular Places"));

        resetUI();
        setupBottomNavigation();
    }

    private void performSearch(String query) {
        tvSectionTitle.setText("Searching for \"" + query + "\"...");
        photonService.search(query, 15).enqueue(new Callback<PhotonResponse>() {
            @Override
            public void onResponse(Call<PhotonResponse> call, Response<PhotonResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().features != null) {
                    List<ExploreItem> results = new ArrayList<>();
                    for (Feature f : response.body().features) {
                        if (f.properties.name == null) continue;
                        ExploreItem item = new ExploreItem(f.properties.name, f.properties.getDisplayName(), "");
                        try {
                            item.lon = f.geometry.coordinates.get(0);
                            item.lat = f.geometry.coordinates.get(1);
                            item.imageUrl = "https://loremflickr.com/400/300/" + f.properties.name.replace(" ", "");
                            results.add(item);
                        } catch (Exception ignored) {}
                    }
                    if (results.isEmpty()) {
                        tvSectionTitle.setText("No results found for \"" + query + "\"");
                        resultsContainer.removeAllViews();
                    } else {
                        showExploreResults("Search Results", results, true);
                    }
                }
            }
            @Override public void onFailure(Call<PhotonResponse> call, Throwable t) {
                tvSectionTitle.setText("Connection failed");
            }
        });
    }

    private void fetchCategoryData(String category, String title) {
        tvSectionTitle.setText("Finding " + title + "...");
        String query = category.equals("tourist_attraction") ? "tourism" : category;
        photonService.searchNearby(query, currentLat, currentLon, 15).enqueue(new Callback<PhotonResponse>() {
            @Override
            public void onResponse(Call<PhotonResponse> call, Response<PhotonResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().features != null) {
                    List<ExploreItem> items = new ArrayList<>();
                    for (Feature f : response.body().features) {
                        if (f.properties.name == null) continue;
                        ExploreItem item = new ExploreItem(f.properties.name, f.properties.getDisplayName(), "");
                        item.lon = f.geometry.coordinates.get(0);
                        item.lat = f.geometry.coordinates.get(1);
                        item.imageUrl = "https://loremflickr.com/400/300/" + category;
                        items.add(item);
                    }
                    showExploreResults(title, items, false);
                }
            }
            @Override public void onFailure(Call<PhotonResponse> call, Throwable t) {}
        });
    }

    private void showExploreResults(String title, List<ExploreItem> items, boolean isSearch) {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText(title);
        planTripCard.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ExploreItem item : items) {
            View itemView = inflater.inflate(R.layout.item_explore, resultsContainer, false);
            ((TextView) itemView.findViewById(R.id.item_title)).setText(item.title);
            ((TextView) itemView.findViewById(R.id.item_subtitle)).setText(item.subtitle);
            
            ImageView imageView = itemView.findViewById(R.id.item_image);
            if (item.imageRes != 0) {
                imageView.setImageResource(item.imageRes);
            } else if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(this).load(item.imageUrl).placeholder(R.drawable.ic_map).centerCrop().into(imageView);
            }

            itemView.setOnClickListener(v -> {
                if (item.itinerary != null) {
                    Intent intent = new Intent(HomePage.this, ItineraryDetails.class);
                    intent.putExtra("destination", item.title);
                    intent.putExtra("itinerary", item.itinerary);
                    intent.putExtra("ITINERARY_ID", "sample_" + item.title.toLowerCase().replace(" ", "_"));
                    startActivity(intent);
                } else if (isSearch) {
                    currentLat = item.lat;
                    currentLon = item.lon;
                    Toast.makeText(this, "Discovery region updated to " + item.title, Toast.LENGTH_SHORT).show();
                    ((EditText)findViewById(R.id.search_destinations)).setText("");
                    ((EditText)findViewById(R.id.search_destinations)).clearFocus();
                    fetchCategoryData("tourist_attraction", "Recommended in " + item.title);
                }
            });
            resultsContainer.addView(itemView);
        }
    }

    private void resetUI() {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText("Recommended for You");
        planTripCard.setVisibility(View.VISIBLE);
        
        List<ExploreItem> samples = getSampleTrips();
        showExploreResults("Recommended for You", samples, false);
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

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) { resetUI(); return true; }
            else if (itemId == R.id.nav_activities) { startActivity(new Intent(this, BudgetTracker.class)); return true; }
            else if (itemId == R.id.nav_trip_history) { startActivity(new Intent(this, TripHistory.class)); return true; }
            else if (itemId == R.id.nav_maps) { startActivity(new Intent(this, TripActivity.class)); return true; }
            else if (itemId == R.id.nav_user_profile) { startActivity(new Intent(this, UserProfile.class)); return true; }
            return false;
        });
    }

    private static class ExploreItem {
        String title, subtitle, imageUrl, itinerary;
        int imageRes;
        double lat, lon;
        
        ExploreItem(String title, String subtitle, String imageUrl) {
            this.title = title; this.subtitle = subtitle; this.imageUrl = imageUrl;
        }
        
        ExploreItem(String title, String subtitle, int imageRes, String itinerary) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageRes = imageRes;
            this.itinerary = itinerary;
        }
    }

    public interface PhotonService {
        @GET("api/")
        Call<PhotonResponse> search(@Query("q") String query, @Query("limit") int limit);
        @GET("api/")
        Call<PhotonResponse> searchNearby(@Query("q") String query, @Query("lat") double lat, @Query("lon") double lon, @Query("limit") int limit);
    }

    public static class PhotonResponse { @SerializedName("features") public List<Feature> features; }
    public static class Feature { @SerializedName("properties") public Properties properties; @SerializedName("geometry") public Geometry geometry; }
    public static class Properties {
        @SerializedName("name") public String name;
        @SerializedName("city") public String city;
        @SerializedName("country") public String country;
        public String getDisplayName() {
            return (name != null ? name : "") + (city != null ? ", " + city : "") + (country != null ? ", " + country : "");
        }
    }
    public static class Geometry { @SerializedName("coordinates") public List<Double> coordinates; }
}
