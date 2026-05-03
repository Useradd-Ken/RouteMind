package com.example.routemind;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.annotations.SerializedName;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class HomePage extends AppCompatActivity {
    private static final String TAG = "HomePage";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LinearLayout resultsContainer;
    private TextView tvSectionTitle;
    private View planTripCard;
    private PhotonService photonService;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 14.5995; // Default fallback: Manila
    private double currentLon = 120.9842;
    private String currentDestination = "Manila";
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final String GEMINI_API_KEY = "";

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
                    clearDisplay();
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
        
        findViewById(R.id.category_food).setOnClickListener(v -> fetchAIRecommendations("restaurants", "Best Food in " + currentDestination));
        findViewById(R.id.category_stays).setOnClickListener(v -> fetchAIRecommendations("hotels", "Top Stays in " + currentDestination));
        findViewById(R.id.category_places).setOnClickListener(v -> fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + currentDestination));
        
        setupBottomNavigation();
        checkLocationPermissionAndFetch();
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }
    }

    private void fetchCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLon = location.getLongitude();
                    updateLocationName(currentLat, currentLon);
                } else {
                    Log.d(TAG, "Location is null, using Manila as fallback.");
                    fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + currentDestination);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
            fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + currentDestination);
        }
    }

    private void updateLocationName(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                if (city == null) city = address.getSubAdminArea();
                if (city != null) {
                    currentDestination = city;
                    tvSectionTitle.setText("Explore " + currentDestination);
                    fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + currentDestination);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + currentDestination);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Permission denied. Using default location.", Toast.LENGTH_SHORT).show();
                fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + currentDestination);
            }
        }
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
                            item.imageUrl = "https://loremflickr.com/400/300/" + f.properties.name.toLowerCase().replace(" ", "");
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

    private void fetchAIRecommendations(String category, String title) {
        tvSectionTitle.setText("Finding " + title + "...");
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE));
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.temperature = 0.4f;
        GenerationConfig config = configBuilder.build();
        RequestOptions requestOptions = new RequestOptions();
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY, config, safetySettings, requestOptions);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        String prompt = "List 10 specific and accurate popular " + category + " in " + currentDestination + ". For each, provide a short 1-sentence description and a relevant imageUrl using: https://loremflickr.com/400/300/" + currentDestination.toLowerCase().replace(" ", "") + ",[place_name_keyword]. Return as a JSON array of objects with fields: \"title\", \"subtitle\", \"imageUrl\". Return ONLY the raw JSON array.";
        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String resultText = result.getText();
                    String cleanJson = resultText.trim();
                    if (cleanJson.contains("[") && cleanJson.contains("]")) {
                        cleanJson = cleanJson.substring(cleanJson.indexOf("["), cleanJson.lastIndexOf("]") + 1);
                    }
                    JSONArray array = new JSONArray(cleanJson);
                    List<ExploreItem> items = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        items.add(new ExploreItem(obj.getString("title"), obj.getString("subtitle"), obj.getString("imageUrl")));
                    }
                    runOnUiThread(() -> showExploreResults(title, items, false));
                } catch (Exception e) {
                    Log.e(TAG, "AI Error", e);
                }
            }
            @Override public void onFailure(Throwable t) {
                Log.e(TAG, "AI Fetch Failed", t);
            }
        }, executor);
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
            Glide.with(this).load(item.imageUrl).placeholder(R.drawable.ic_map).centerCrop().into(imageView);
            itemView.setOnClickListener(v -> {
                if (isSearch) {
                    currentLat = item.lat;
                    currentLon = item.lon;
                    currentDestination = item.title;
                    Toast.makeText(this, "Discovery region updated to " + item.title, Toast.LENGTH_SHORT).show();
                    ((EditText)findViewById(R.id.search_destinations)).setText("");
                    ((EditText)findViewById(R.id.search_destinations)).clearFocus();
                    fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + item.title);
                }
            });
            resultsContainer.addView(itemView);
        }
    }

    private void clearDisplay() {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText("Explore " + currentDestination);
        planTripCard.setVisibility(View.VISIBLE);
        fetchAIRecommendations("tourist attractions", "Must-Visit Places in " + currentDestination);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) { clearDisplay(); return true; }
            else if (itemId == R.id.nav_activities) { startActivity(new Intent(this, BudgetTracker.class)); return true; }
            else if (itemId == R.id.nav_trip_history) { startActivity(new Intent(this, TripHistory.class)); return true; }
            else if (itemId == R.id.nav_maps) { startActivity(new Intent(this, TripActivity.class)); return true; }
            else if (itemId == R.id.nav_user_profile) { startActivity(new Intent(this, UserProfile.class)); return true; }
            return false;
        });
    }

    private static class ExploreItem {
        String title, subtitle, imageUrl;
        double lat, lon;
        ExploreItem(String title, String subtitle, String imageUrl) {
            this.title = title; this.subtitle = subtitle; this.imageUrl = imageUrl;
        }
    }

    public interface PhotonService {
        @GET("api/")
        Call<PhotonResponse> search(@Query("q") String query, @Query("limit") int limit);
        @GET("api/")
        Call<PhotonResponse> searchNearby(@Query("q") String query, @Query("lat") double lat, @Query("lon") double lon, @Query("limit") int limit);
    }

    public static class PhotonResponse { @SerializedName("features") public List<Feature> features; }
    public static class Feature { 
        @SerializedName("properties") public Properties properties; 
        @SerializedName("geometry") public Geometry geometry; 
    }
    public static class Properties {
        @SerializedName("name") public String name;
        @SerializedName("city") public String city;
        @SerializedName("country") public String country;
        public String getDisplayName() {
            String display = (name != null ? name : "");
            if (city != null && !city.equalsIgnoreCase(name)) display += ", " + city;
            if (country != null) display += ", " + country;
            return display;
        }
    }
    public static class Geometry { @SerializedName("coordinates") public List<Double> coordinates; }

    public static class PhotonAutocompleteAdapter extends ArrayAdapter<Feature> implements Filterable {
        private final PhotonService service;
        private List<Feature> resultList = new ArrayList<>();
        public PhotonAutocompleteAdapter(@NonNull Context context) {
            super(context, R.layout.item_dropdown);
            service = new Retrofit.Builder().baseUrl("https://photon.komoot.io/").addConverterFactory(GsonConverterFactory.create()).build().create(PhotonService.class);
        }
        @Override public int getCount() { return resultList.size(); }
        @Nullable @Override public Feature getItem(int position) { return (position >= 0 && position < resultList.size()) ? resultList.get(position) : null; }
        @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown, parent, false);
            TextView tv = convertView.findViewById(R.id.tv_dropdown_item);
            Feature item = getItem(position);
            if (item != null) tv.setText(item.properties.getDisplayName());
            return convertView;
        }
        @NonNull @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults fr = new FilterResults();
                    if (constraint != null && constraint.length() >= 1) {
                        try {
                            Response<PhotonResponse> response = service.search(constraint.toString(), 15).execute();
                            if (response.isSuccessful() && response.body() != null) {
                                fr.values = response.body().features;
                                fr.count = response.body().features.size();
                            }
                        } catch (Exception ignored) {}
                    }
                    return fr;
                }
                @Override protected void publishResults(CharSequence constraint, FilterResults fr) {
                    if (fr != null && fr.values != null) {
                        resultList = (List<Feature>) fr.values;
                        notifyDataSetChanged();
                    } else {
                        resultList = new ArrayList<>();
                        notifyDataSetInvalidated();
                    }
                }
                @Override public CharSequence convertResultToString(Object r) { return ((Feature) r).properties.getDisplayName(); }
            };
        }
    }
}
