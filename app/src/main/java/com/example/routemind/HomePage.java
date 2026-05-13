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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.firestore.ListenerRegistration;
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
    private ListenerRegistration feedbackListener;

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
            feedbackListener = db.collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null) {
                        Log.e(TAG, "Firestore Listen failed.", error);
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
        }
    }

    private void displayFeedbacks(List<Feedback> list) {
        if (isFinishing() || isDestroyed()) return;
        feedbackContainer.removeAllViews();
        if (list.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No reviews yet. Be the first to share!");
            tv.setPadding(32, 32, 32, 32);
            feedbackContainer.addView(tv);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Feedback f : list) {
            View v = inflater.inflate(R.layout.item_feedback, feedbackContainer, false);
            ((TextView) v.findViewById(R.id.tv_user_name)).setText(f.getUserName());
            ((TextView) v.findViewById(R.id.tv_destination_tag)).setText(f.getDestination());
            ((TextView) v.findViewById(R.id.tv_feedback_comment)).setText(f.getComment());
            ((RatingBar) v.findViewById(R.id.feedback_rating)).setRating(f.getRating());
            
            View imageContainer = v.findViewById(R.id.cv_feedback_image_container);
            ImageView feedbackImage = v.findViewById(R.id.iv_feedback_image);
            
            if (f.getImageUrl() != null && !f.getImageUrl().isEmpty()) {
                imageContainer.setVisibility(View.VISIBLE);
                if (!isFinishing() && !isDestroyed()) {
                    Glide.with(this)
                            .load(f.getImageUrl())
                            .centerCrop()
                            .placeholder(R.drawable.routemind)
                            .into(feedbackImage);
                }
            } else {
                imageContainer.setVisibility(View.GONE);
            }

            // Itinerary logic
            View itineraryContainer = v.findViewById(R.id.cv_itinerary_container);
            if (f.getItineraryId() != null && !f.getItineraryId().isEmpty()) {
                itineraryContainer.setVisibility(View.VISIBLE);
                ((TextView) v.findViewById(R.id.tv_itinerary_name)).setText(f.getItineraryTitle() != null ? f.getItineraryTitle() : "Shared Itinerary");
                
                TextView tvUsage = v.findViewById(R.id.tv_usage_count);
                db.collection("booked_itineraries").document(f.getItineraryId()).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        long count = documentSnapshot.getLong("usageCount") != null ? documentSnapshot.getLong("usageCount") : 0;
                        tvUsage.setText(count + (count == 1 ? " user used this" : " users used this"));
                    }
                });

                itineraryContainer.setOnClickListener(view -> {
                    db.collection("booked_itineraries").document(f.getItineraryId()).get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Itinerary itinerary = doc.toObject(Itinerary.class);
                            if (itinerary != null) {
                                Intent intent = new Intent(this, TourDetailsActivity.class);
                                intent.putExtra("TITLE", itinerary.getTitle());
                                intent.putExtra("DESCRIPTION", itinerary.getDescription());
                                intent.putExtra("ITINERARY", itinerary.getItineraryDetails());
                                intent.putExtra("IMAGE_URL", itinerary.getImageUrl());
                                intent.putExtra("CATEGORY", itinerary.getCategory());
                                intent.putExtra("PRICE", itinerary.getPrice());
                                intent.putExtra("PRICE_BREAKDOWN", itinerary.getPriceBreakdown());
                                intent.putExtra("DESTINATION", itinerary.getDestination());
                                intent.putExtra("ORIGINAL_ITINERARY_ID", f.getItineraryId());
                                startActivity(intent);
                            }
                        }
                    });
                });
            } else {
                itineraryContainer.setVisibility(View.GONE);
            }
            
            long timestamp = f.getTimestampMillis();
            if (timestamp > 0) {
                long diff = System.currentTimeMillis() - timestamp;
                String timeAgo = diff < 60000 ? "Just now" : 
                                 diff < 3600000 ? (diff / 60000) + "m ago" : 
                                 diff < 86400000 ? (diff / 3600000) + "h ago" : 
                                 (diff / 86400000) + "d ago";
                ((TextView) v.findViewById(R.id.tv_feedback_time)).setText(timeAgo);
            }
            feedbackContainer.addView(v);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (feedbackListener != null) {
            feedbackListener.remove();
            feedbackListener = null;
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

        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY, config);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = "List 10 popular " + category + " in " + location + ". Return exactly a JSON array of objects with \"title\", \"description\", and \"imageUrl\" keys. For \"imageUrl\", use 'https://loremflickr.com/400/300/' followed by the place name.";

        Content content = new Content.Builder().addText(prompt).build();
        Futures.addCallback(model.generateContent(content), new FutureCallback<>() {
            @Override public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    try {
                        JSONArray array = new JSONArray(result.getText());
                        List<ExploreItem> items = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            items.add(new ExploreItem(obj.getString("title"), obj.getString("description"), obj.getString("imageUrl")));
                        }
                        showExploreResults(displayCategory.substring(0, 1).toUpperCase() + displayCategory.substring(1), items, false);
                    } catch (Exception e) { tvSectionTitle.setText("AI is taking a break. Try again later!"); }
                });
            }
            @Override public void onFailure(@NonNull Throwable t) {
                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    tvSectionTitle.setText("Check your connection and try again");
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void showExploreResults(String title, List<ExploreItem> items, boolean isSearch) {
        tvSectionTitle.setText(title + " in " + (currentDestination != null ? currentDestination : "Manila"));
        resultsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ExploreItem item : items) {
            View v = inflater.inflate(R.layout.item_explore, resultsContainer, false);
            ((TextView) v.findViewById(R.id.item_title)).setText(item.title);
            ((TextView) v.findViewById(R.id.item_subtitle)).setText(item.description);
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this).load(item.imageUrl).centerCrop().placeholder(R.drawable.routemind).into((ImageView) v.findViewById(R.id.item_image));
            }
            v.setOnClickListener(view -> {
                if (isSearch) {
                    Intent intent = new Intent(this, TripActivity.class);
                    intent.putExtra("DESTINATION", item.title);
                    startActivity(intent);
                }
            });
            resultsContainer.addView(v);
        }
    }

    private void clearDisplay() {
        resultsContainer.removeAllViews();
        updateTitleOnly();
        loadingSpinner.setVisibility(View.GONE);
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_activities) navigateTo(BudgetTracker.class);
            else if (id == R.id.nav_maps) navigateTo(TripActivity.class);
            else if (id == R.id.nav_trip_history) navigateTo(TripHistory.class);
            else if (id == R.id.nav_user_profile) navigateTo(UserProfile.class);
            return id == R.id.nav_home;
        });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(0, 0);
        finish();
    }

    public static class ExploreItem {
        String title, description, imageUrl;
        public ExploreItem(String title, String description, String imageUrl) {
            this.title = title; this.description = description; this.imageUrl = imageUrl;
        }
    }

    public interface PhotonService {
        @GET("api/") Call<PhotonResponse> search(@retrofit2.http.Query("q") String q, @retrofit2.http.Query("limit") int limit);
    }

    public static class PhotonResponse { @SerializedName("features") List<Feature> features; }
    public static class Feature { @SerializedName("properties") Properties properties; }
    public static class Properties {
        @SerializedName("name") String name;
        @SerializedName("city") String city;
        @SerializedName("country") String country;
        public String getDisplayName() { return name + (city != null ? ", " + city : "") + (country != null ? ", " + country : ""); }
    }

    public static class PhotonAutocompleteAdapter extends ArrayAdapter<Feature> implements Filterable {
        private List<Feature> results = new ArrayList<>();
        private final PhotonService service;

        public PhotonAutocompleteAdapter(Context context) {
            super(context, R.layout.item_dropdown, R.id.tv_dropdown_item);
            service = new Retrofit.Builder()
                    .baseUrl("https://photon.komoot.io/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(PhotonService.class);
        }

        @Override
        public int getCount() { return results.size(); }

        @Override
        public Feature getItem(int index) { return results.get(index); }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null && constraint.length() > 0) {
                        try {
                            Response<PhotonResponse> response = service.search(constraint.toString(), 10).execute();
                            if (response.isSuccessful() && response.body() != null) {
                                results = response.body().features;
                                filterResults.values = results;
                                filterResults.count = results.size();
                            }
                        } catch (IOException e) {
                            Log.e("PhotonAdapter", "Error fetching suggestions", e);
                        }
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown, parent, false);
            }
            Feature feature = getItem(position);
            TextView tv = convertView.findViewById(R.id.tv_dropdown_item);
            if (feature != null && feature.properties != null) {
                tv.setText(feature.properties.getDisplayName());
            }
            return convertView;
        }
    }
}
