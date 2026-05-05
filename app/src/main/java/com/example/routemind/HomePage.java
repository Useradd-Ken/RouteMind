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
import java.util.Calendar;
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
    private TextView tvSectionTitle, tvGreeting;
    private View planTripCard;
    private PhotonService photonService;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0; 
    private double currentLon = 0;
    private String currentDestination = "current location";
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final String GEMINI_API_KEY = "AIzaSyA4pwZY4jYN08D6IBmv_ENKktKXPCR7wro";

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
        tvGreeting = findViewById(R.id.tv_greeting);
        planTripCard = findViewById(R.id.plan_trip_card);
        
        setGreeting();

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
        
        findViewById(R.id.category_food).setOnClickListener(v -> fetchAIRecommendations("restaurants", "top dining spots"));
        findViewById(R.id.category_stays).setOnClickListener(v -> fetchAIRecommendations("hotels", "premium accommodations"));
        findViewById(R.id.category_places).setOnClickListener(v -> fetchAIRecommendations("tourist attractions", "must-visit locations"));
        
        setupBottomNavigation();
        checkLocationPermissionAndFetch();
    }

    private void setGreeting() {
        if (tvGreeting == null) return;
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (timeOfDay >= 0 && timeOfDay < 12) greeting = "Good morning";
        else if (timeOfDay >= 12 && timeOfDay < 16) greeting = "Good afternoon";
        else if (timeOfDay >= 16 && timeOfDay < 21) greeting = "Good evening";
        else greeting = "Hello";
        tvGreeting.setText(greeting + ", traveler!");
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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
                    updateTitleOnly();
                }
            });
        } catch (SecurityException e) {
            updateTitleOnly();
        }
    }

    private void updateLocationName(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality() != null ? address.getLocality() : address.getSubAdminArea();
                if (city != null) currentDestination = city;
            }
        } catch (IOException ignored) {}
        updateTitleOnly();
    }

    private synchronized void updateTitleOnly() {
        tvSectionTitle.setText("Explore " + currentDestination);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                updateTitleOnly();
            }
        }
    }

    private void performSearch(String query) {
        tvSectionTitle.setText("Finding matches for \"" + query + "\"...");
        photonService.search(query, 10).enqueue(new Callback<PhotonResponse>() {
            @Override
            public void onResponse(Call<PhotonResponse> call, Response<PhotonResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().features != null) {
                    List<ExploreItem> results = new ArrayList<>();
                    for (Feature f : response.body().features) {
                        if (f.properties.name == null) continue;
                        ExploreItem item = new ExploreItem(f.properties.name, f.properties.getDisplayName(), "");
                        item.lon = f.geometry.coordinates.get(0);
                        item.lat = f.geometry.coordinates.get(1);
                        String key = f.properties.name.toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
                        item.imageUrl = "https://image.pollinations.ai/prompt/professional%20photo%20of%20" + key.replace(" ", "%20") + "?width=400&height=300&nologo=true";
                        results.add(item);
                    }
                    if (results.isEmpty()) {
                        tvSectionTitle.setText("No destinations found for \"" + query + "\"");
                        resultsContainer.removeAllViews();
                    } else {
                        showExploreResults("Select your destination", results, true);
                    }
                }
            }
            @Override public void onFailure(Call<PhotonResponse> call, Throwable t) {
                tvSectionTitle.setText("Connectivity issue. Please check your network.");
            }
        });
    }

    private void fetchAIRecommendations(String category, String displayCategory) {
        if (GEMINI_API_KEY.isEmpty()) {
            tvSectionTitle.setText("Personalized discovery is offline.");
            return;
        }

        tvSectionTitle.setText("Curating " + displayCategory + " in " + currentDestination + "...");
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE));
        
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.temperature = 0.4f;
        GenerationConfig config = configBuilder.build();
        
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY, config, safetySettings);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        
        String prompt = "List 10 specific popular " + category + " in " + currentDestination + ". " +
                "Format: raw JSON array of objects {\"title\", \"subtitle\", \"imageUrl\"}. " +
                "Description in 'subtitle'. For 'imageUrl', use 'https://image.pollinations.ai/prompt/[descriptive_prompt]?width=400&height=300&nologo=true' " +
                "where [descriptive_prompt] is a short, specific description of a high-quality photo for the item. " +
                "Return ONLY the array.";
        
        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String json = result.getText();
                    if (json == null || json.isEmpty()) throw new Exception("Empty AI output");
                    String cleanJson = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
                    JSONArray array = new JSONArray(cleanJson);
                    List<ExploreItem> items = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        items.add(new ExploreItem(obj.getString("title"), obj.getString("subtitle"), obj.getString("imageUrl")));
                    }
                    runOnUiThread(() -> showExploreResults("Featured " + displayCategory, items, false));
                } catch (Exception e) {
                    runOnUiThread(() -> tvSectionTitle.setText("We couldn't refresh recommendations right now."));
                }
            }
            @Override public void onFailure(Throwable t) {
                runOnUiThread(() -> tvSectionTitle.setText("Recommendation engine unavailable."));
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
            ImageView img = itemView.findViewById(R.id.item_image);
            Glide.with(this).load(item.imageUrl).placeholder(R.drawable.ic_map).centerCrop().into(img);
            
            itemView.setOnClickListener(v -> {
                if (isSearch) {
                    currentLat = item.lat;
                    currentLon = item.lon;
                    currentDestination = item.title;
                    Toast.makeText(this, "Discovery region updated to " + item.title, Toast.LENGTH_SHORT).show();
                    ((EditText)findViewById(R.id.search_destinations)).setText("");
                    ((EditText)findViewById(R.id.search_destinations)).clearFocus();
                    updateTitleOnly();
                    // Automatic generation of "Places" on selection
                    fetchAIRecommendations("tourist attractions", "must-visit attractions");
                }
            });
            resultsContainer.addView(itemView);
        }
    }

    private void clearDisplay() {
        resultsContainer.removeAllViews();
        updateTitleOnly();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { clearDisplay(); return true; }
            else if (id == R.id.nav_activities) { startActivity(new Intent(this, BudgetTracker.class)); return true; }
            else if (id == R.id.nav_trip_history) { startActivity(new Intent(this, TripHistory.class)); return true; }
            else if (id == R.id.nav_maps) { startActivity(new Intent(this, TripActivity.class)); return true; }
            else if (id == R.id.nav_user_profile) { startActivity(new Intent(this, UserProfile.class)); return true; }
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
        @GET("api/") Call<PhotonResponse> search(@Query("q") String query, @Query("limit") int limit);
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
            String d = name != null ? name : "";
            if (city != null && !city.equalsIgnoreCase(name)) d += ", " + city;
            if (country != null) d += ", " + country;
            return d;
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
        @Nullable @Override public Feature getItem(int position) { 
            if (position >= 0 && position < resultList.size()) return resultList.get(position);
            return null;
        }
        @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown, parent, false);
            Feature feature = getItem(position);
            if (feature != null && feature.properties != null) {
                ((TextView) convertView.findViewById(R.id.tv_dropdown_item)).setText(feature.properties.getDisplayName());
            }
            return convertView;
        }
        @NonNull @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults fr = new FilterResults();
                    if (constraint != null && constraint.length() >= 1) {
                        try {
                            Response<PhotonResponse> r = service.search(constraint.toString(), 10).execute();
                            if (r.isSuccessful() && r.body() != null) {
                                fr.values = r.body().features;
                                fr.count = r.body().features != null ? r.body().features.size() : 0;
                            }
                        } catch (Exception e) {
                            Log.e("PhotonAdapter", "Search error", e);
                        }
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
                @Override public CharSequence convertResultToString(Object r) { 
                    if (r instanceof Feature) return ((Feature) r).properties.getDisplayName();
                    return super.convertResultToString(r);
                }
            };
        }
    }
}
