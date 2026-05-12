package com.example.routemind;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.annotations.SerializedName;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class HomePage extends AppCompatActivity {
    private static final String TAG = "HomePage";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LinearLayout resultsContainer, feedbackContainer;
    private TextView tvSectionTitle, tvGreeting;
    private View planTripCard;
    private ProgressBar loadingSpinner;
    private PhotonService photonService;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentDestination = null; 
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final String GEMINI_API_KEY = "AIzaSyC6pPLeFuVcEmQhCqG8N7mX_2b_xjx2xfU";
    private FirebaseFirestore db;

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

        FirebaseApp.initializeApp(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase Firestore initialization failed", e);
        }

        resultsContainer = findViewById(R.id.results_container);
        feedbackContainer = findViewById(R.id.feedback_container);
        tvSectionTitle = findViewById(R.id.tv_section_title);
        tvGreeting = findViewById(R.id.tv_greeting);
        planTripCard = findViewById(R.id.plan_trip_card);
        loadingSpinner = findViewById(R.id.loading_spinner);
        
        setGreeting();

        findViewById(R.id.btn_generate_now).setOnClickListener(v -> startActivity(new Intent(this, TripActivity.class)));
        
        photonService = new Retrofit.Builder().baseUrl("https://photon.komoot.io/").addConverterFactory(GsonConverterFactory.create()).build().create(PhotonService.class);
        
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
                } else if (query.isEmpty()) clearDisplay();
            }
        });
        
        findViewById(R.id.category_food).setOnClickListener(v -> fetchAIRecommendations("restaurants", "the best places to eat"));
        findViewById(R.id.category_stays).setOnClickListener(v -> fetchAIRecommendations("hotels", "comfortable places to stay"));
        findViewById(R.id.category_places).setOnClickListener(v -> fetchAIRecommendations("tourist attractions", "exciting places to visit"));
        
        setupBottomNavigation();
        checkLocationPermissionAndFetch();
        loadCommunityFeed();
    }

    private void loadCommunityFeed() {
        if (db != null) {
            db.collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore Listen failed.", error);
                        displayFeedbacks(new ArrayList<>());
                        return;
                    }
                    List<Feedback> feedbackList = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Feedback f = doc.toObject(Feedback.class);
                                feedbackList.add(f);
                            } catch (Exception e) {
                                Log.e(TAG, "Error deserializing feedback doc", e);
                            }
                        }
                    }
                    displayFeedbacks(feedbackList);
                });
        } else displayFeedbacks(new ArrayList<>());
    }

    private void displayFeedbacks(List<Feedback> list) {
        feedbackContainer.removeAllViews();
        list.add(new Feedback("1", "Alex Rivera", "", "Siargao", "The surfing was incredible!", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800", 5.0f, System.currentTimeMillis() - 100000));
        list.add(new Feedback("2", "Sarah Jenkins", "", "Baguio City", "So cold and beautiful.", "https://images.unsplash.com/photo-1530789253388-582c481c54b0?w=800", 4.5f, System.currentTimeMillis() - 500000));
        list.sort((f1, f2) -> Long.compare(f2.getTimestampMillis(), f1.getTimestampMillis()));
        
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Feedback f : list) {
            View v = inflater.inflate(R.layout.item_feedback, feedbackContainer, false);
            ((TextView) v.findViewById(R.id.tv_user_name)).setText(f.getUserName());
            ((TextView) v.findViewById(R.id.tv_destination_tag)).setText(f.getDestination());
            ((TextView) v.findViewById(R.id.tv_feedback_comment)).setText(f.getComment());
            ((RatingBar) v.findViewById(R.id.feedback_rating)).setRating(f.getRating());
            Glide.with(this).load(f.getImageUrl()).centerCrop().placeholder(R.drawable.routemind).into((ImageView) v.findViewById(R.id.iv_feedback_image));
            long diff = System.currentTimeMillis() - f.getTimestampMillis();
            ((TextView) v.findViewById(R.id.tv_feedback_time)).setText(diff < 3600000 ? (diff / 60000) + "m ago" : (diff / 3600000) + "h ago");
            feedbackContainer.addView(v);
        }
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = (hour < 12) ? "Good morning" : (hour < 16) ? "Good afternoon" : (hour < 21) ? "Good evening" : "Hello";
        tvGreeting.setText(String.format("%s, traveler!", g));
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else fetchCurrentLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) fetchCurrentLocation();
            else { currentDestination = "Manila"; updateTitleOnly(); }
        }
    }

    private void fetchCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) updateLocationName(location.getLatitude(), location.getLongitude());
                else requestNewLocation();
            });
        } catch (SecurityException e) { currentDestination = "Manila"; updateTitleOnly(); }
    }

    private void requestNewLocation() {
        try {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMaxUpdates(1).build();
            fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    if (result.getLastLocation() != null) updateLocationName(result.getLastLocation().getLatitude(), result.getLastLocation().getLongitude());
                    else { currentDestination = "Manila"; updateTitleOnly(); }
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) { currentDestination = "Manila"; updateTitleOnly(); }
    }

    private void updateLocationName(double lat, double lon) {
        try {
            List<Address> addresses = new Geocoder(this, Locale.getDefault()).getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality() != null ? addresses.get(0).getLocality() : addresses.get(0).getSubAdminArea();
                if (city != null) currentDestination = city;
            }
        } catch (IOException ignored) {}
        if (currentDestination == null) currentDestination = "Manila";
        updateTitleOnly();
    }

    private synchronized void updateTitleOnly() {
        tvSectionTitle.setText(currentDestination == null ? "Finding your location..." : "Exploring " + currentDestination);
    }

    private void performSearch(String query) {
        tvSectionTitle.setText(String.format("Looking for \"%s\"...", query));
        loadingSpinner.setVisibility(View.VISIBLE);
        resultsContainer.removeAllViews();
        photonService.search(query, 10).enqueue(new Callback<>() {
            @Override public void onResponse(@NonNull Call<PhotonResponse> call, @NonNull Response<PhotonResponse> response) {
                loadingSpinner.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().features != null) {
                    List<ExploreItem> results = new ArrayList<>();
                    for (Feature f : response.body().features) {
                        if (f.properties.name == null) continue;
                        String kw = (f.properties.name + "," + (f.properties.city != null ? f.properties.city : "")).replace(" ", ",");
                        results.add(new ExploreItem(f.properties.name, f.properties.getDisplayName(), "https://loremflickr.com/400/300/" + kw + "/all"));
                    }
                    if (results.isEmpty()) tvSectionTitle.setText(String.format("No results found for \"%s\"", query));
                    else showExploreResults("Search results", results, true);
                }
            }
            @Override public void onFailure(@NonNull Call<PhotonResponse> call, @NonNull Throwable t) {
                loadingSpinner.setVisibility(View.GONE);
                tvSectionTitle.setText("Check your connection and try again");
            }
        });
    }

    private void fetchAIRecommendations(String category, String displayCategory) {
        String location = currentDestination != null ? currentDestination : "Manila";
        tvSectionTitle.setText(String.format("Finding %s in %s...", displayCategory, location));
        loadingSpinner.setVisibility(View.VISIBLE);
        resultsContainer.removeAllViews();
        
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.temperature = 0.4f;
        GenerationConfig config = configBuilder.build();

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", GEMINI_API_KEY, config);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = "List 10 popular " + category + " in " + location + ". Format as a JSON array where each object has \"title\", \"subtitle\", and \"imageUrl\". Return ONLY the JSON array.";
        Futures.addCallback(model.generateContent(new Content.Builder().addText(prompt).build()), new FutureCallback<>() {
            @Override public void onSuccess(GenerateContentResponse result) {
                try {
                    String json = result.getText();
                    if (json == null) return;
                    JSONArray array = new JSONArray(json.substring(json.indexOf("["), json.lastIndexOf("]") + 1));
                    List<ExploreItem> items = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        items.add(new ExploreItem(obj.getString("title"), obj.getString("subtitle"), obj.getString("imageUrl")));
                    }
                    runOnUiThread(() -> {
                        loadingSpinner.setVisibility(View.GONE);
                        showExploreResults("Top picks in " + location, items, false);
                    });
                } catch (Exception e) { runOnUiThread(() -> { loadingSpinner.setVisibility(View.GONE); tvSectionTitle.setText("Unable to load recommendations."); }); }
            }
            @Override public void onFailure(@NonNull Throwable t) { runOnUiThread(() -> { loadingSpinner.setVisibility(View.GONE); tvSectionTitle.setText("Trouble reaching server."); }); }
        }, Executors.newSingleThreadExecutor());
    }

    private void showExploreResults(String title, List<ExploreItem> items, boolean isSearch) {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText(title);
        planTripCard.setVisibility(View.VISIBLE);
        for (ExploreItem item : items) {
            View v = getLayoutInflater().inflate(R.layout.item_explore, resultsContainer, false);
            ((TextView) v.findViewById(R.id.item_title)).setText(item.title);
            ((TextView) v.findViewById(R.id.item_subtitle)).setText(item.subtitle);
            Glide.with(this).load(item.imageUrl).placeholder(R.drawable.routemind).error(R.drawable.ic_map).centerCrop().into((ImageView) v.findViewById(R.id.item_image));
            v.setOnClickListener(view -> { if (isSearch) { currentDestination = item.title; updateTitleOnly(); fetchAIRecommendations("tourist attractions", "exciting places to visit"); } });
            resultsContainer.addView(v);
        }
    }

    private void clearDisplay() { resultsContainer.removeAllViews(); updateTitleOnly(); }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { clearDisplay(); return true; }
            else if (id == R.id.nav_activities) { startActivity(new Intent(this, BudgetTracker.class)); return true; }
            else if (id == R.id.nav_itineraries) { startActivity(new Intent(this, ItinerariesActivity.class)); return true; }
            else if (id == R.id.nav_trip_history) { startActivity(new Intent(this, TripHistory.class)); return true; }
            else if (id == R.id.nav_maps) { startActivity(new Intent(this, TripActivity.class)); return true; }
            else if (id == R.id.nav_user_profile) { startActivity(new Intent(this, UserProfile.class)); return true; }
            return false;
        });
    }

    private static class ExploreItem {
        String title, subtitle, imageUrl;
        ExploreItem(String t, String s, String i) { this.title = t; this.subtitle = s; this.imageUrl = i; }
    }

    public interface PhotonService { @GET("api/") Call<PhotonResponse> search(@retrofit2.http.Query("q") String query, @retrofit2.http.Query("limit") int limit); }
    public static class PhotonResponse { @SerializedName("features") public List<Feature> features; }
    public static class Feature { @SerializedName("properties") public Properties properties; @SerializedName("geometry") public Geometry geometry; }
    public static class Properties {
        @SerializedName("name") public String name;
        @SerializedName("city") public String city;
        @SerializedName("country") public String country;
        public String getDisplayName() {
            StringBuilder sb = new StringBuilder();
            if (name != null) sb.append(name);
            if (city != null && !city.equalsIgnoreCase(name)) { if (sb.length() > 0) sb.append(", "); sb.append(city); }
            if (country != null) { if (sb.length() > 0) sb.append(", "); sb.append(country); }
            return sb.toString();
        }
    }
    public static class Geometry { @SerializedName("coordinates") public List<Double> coordinates; }

    public static class PhotonAutocompleteAdapter extends ArrayAdapter<Feature> implements Filterable {
        private final PhotonService service;
        private List<Feature> resultList = new ArrayList<>();
        public PhotonAutocompleteAdapter(@NonNull Context context) {
            super(context, R.layout.item_dropdown, R.id.tv_dropdown_item);
            service = new Retrofit.Builder().baseUrl("https://photon.komoot.io/").addConverterFactory(GsonConverterFactory.create()).build().create(PhotonService.class);
        }
        @Override public int getCount() { return resultList.size(); }
        @Nullable @Override public Feature getItem(int pos) { return (pos >= 0 && pos < resultList.size()) ? resultList.get(pos) : null; }
        @NonNull @Override public View getView(int pos, @Nullable View v, @NonNull ViewGroup p) {
            if (v == null) v = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown, p, false);
            Feature f = getItem(pos);
            if (f != null && f.properties != null) ((TextView) v.findViewById(R.id.tv_dropdown_item)).setText(f.properties.getDisplayName());
            return v;
        }
        @NonNull @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence c) {
                    FilterResults fr = new FilterResults();
                    if (c != null && c.length() >= 1) {
                        try {
                            Response<PhotonResponse> r = service.search(c.toString(), 15).execute();
                            if (r.isSuccessful() && r.body() != null) { fr.values = r.body().features; fr.count = r.body().features != null ? r.body().features.size() : 0; }
                        } catch (Exception ignored) {}
                    }
                    return fr;
                }
                @Override protected void publishResults(CharSequence c, FilterResults fr) {
                    if (fr != null && fr.values != null) { resultList = (List<Feature>) fr.values; notifyDataSetChanged(); }
                    else { resultList = new ArrayList<>(); notifyDataSetInvalidated(); }
                }
                @Override public CharSequence convertResultToString(Object r) { return (r instanceof Feature) ? ((Feature) r).properties.getDisplayName() : super.convertResultToString(r); }
            };
        }
    }
}
