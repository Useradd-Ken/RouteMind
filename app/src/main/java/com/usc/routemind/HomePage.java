package com.usc.routemind;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.routemind.R;
import com.example.routemind.BudgetTracker;
import com.example.routemind.TripActivity;
import com.example.routemind.TripHistory;
import com.example.routemind.UserProfile;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";
    private LinearLayout resultsContainer;
    private TextView tvSectionTitle;
    private View planTripCard;
    private EditText searchDestinations;
    
    private NominatimService nominatimService;
    private OverpassService overpassService;

    // Default context: Cebu City
    private double currentLat = 10.3157;
    private double currentLon = 123.8854;
    private String currentPlaceName = "Cebu City";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private boolean isProgrammaticChange = false;

    private final String APP_USER_AGENT = "RouteMindApp/1.0 (Contact: routemind@example.com)";

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
        searchDestinations = findViewById(R.id.search_destinations);

        findViewById(R.id.btn_generate_now).setOnClickListener(v -> startActivity(new Intent(this, TripActivity.class)));

        nominatimService = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(NominatimService.class);

        overpassService = new Retrofit.Builder()
                .baseUrl("https://overpass-api.de/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(OverpassService.class);

        searchDestinations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isProgrammaticChange) return;
                String query = s.toString().trim();
                searchHandler.removeCallbacksAndMessages(null); 
                if (query.length() > 2) {
                    searchHandler.postDelayed(() -> performSearch(query), 800);
                } else if (query.isEmpty()) {
                    clearDisplay();
                }
            }
        });

        searchDestinations.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchDestinations.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });

        findViewById(R.id.category_food).setOnClickListener(v -> fetchCategoryData("restaurant"));
        findViewById(R.id.category_stays).setOnClickListener(v -> fetchCategoryData("hotel"));
        findViewById(R.id.category_places).setOnClickListener(v -> fetchCategoryData("tourist"));

        clearDisplay();
        setupBottomNavigation();
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;
        tvSectionTitle.setText("Searching for \"" + query + "\"...");
        nominatimService.search(query, "json", 1, 10, "ph", APP_USER_AGENT).enqueue(new Callback<List<NominatimService.Place>>() {
            @Override
            public void onResponse(Call<List<NominatimService.Place>> call, Response<List<NominatimService.Place>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExploreItem> results = new ArrayList<>();
                    for (NominatimService.Place p : response.body()) {
                        String title = p.displayName.split(",")[0];
                        ExploreItem item = new ExploreItem(title, p.displayName, R.drawable.ic_map);
                        try {
                            item.lat = Double.parseDouble(p.lat);
                            item.lon = Double.parseDouble(p.lon);
                            results.add(item);
                        } catch (Exception ignored) {}
                    }
                    showExploreResults("Search Results", results, true);
                }
            }
            @Override public void onFailure(Call<List<NominatimService.Place>> call, Throwable t) {
                tvSectionTitle.setText("Search error");
            }
        });
    }

    private void fetchCategoryData(String type) {
        String label = type.equals("restaurant") ? "Food" : (type.equals("hotel") ? "Stays" : "Places");
        tvSectionTitle.setText("Finding " + label + " in " + currentPlaceName + "...");
        
        String latStr = String.format(Locale.US, "%.6f", currentLat);
        String lonStr = String.format(Locale.US, "%.6f", currentLon);
        
        String opQuery;
        if (type.equals("restaurant")) {
            // Refined Food query: increased radius and stricter filtering for generic names
            opQuery = "[out:json][timeout:25];nwr[\"amenity\"~\"restaurant|cafe|fast_food|food_court\"][\"name\"]" +
                      "(around:8000," + latStr + "," + lonStr + ");" +
                      "nwr._[\"name\"!~\"^(restaurant|restaurants|cafe|café|food court|fast food|canteen|eatery|karinderya|carenderia|bakery|coffee shop|unnamed)$\",i];" +
                      "out center 60;";
        } else if (type.equals("hotel")) {
            opQuery = "[out:json][timeout:25];nwr[\"tourism\"~\"hotel|hostel|resort|guest_house|motel\"][\"name\"]" +
                      "(around:10000," + latStr + "," + lonStr + ");" +
                      "nwr._[\"name\"!~\"^(hotel|hotels|hostel|resort|motel|lodging|accommodation|inn|boarding house|guest house)$\",i];" +
                      "out center 60;";
        } else {
            // Refined places search: focuses on actual tourist spots and strictly filters out offices/info centers
            opQuery = "[out:json][timeout:25];(" +
                      "nwr[\"tourism\"~\"attraction|museum|viewpoint|zoo|theme_park|gallery|aquarium\"](around:15000," + latStr + "," + lonStr + ");" +
                      "nwr[\"historic\"~\"monument|castle|ruins|heritage|fort|shrine|tomb|archaeological_site\"](around:15000," + latStr + "," + lonStr + ");" +
                      "nwr[\"leisure\"~\"park|garden|nature_reserve|water_park\"](around:15000," + latStr + "," + lonStr + ");" +
                      "nwr[\"natural\"~\"beach|peak|volcano|waterfall\"](around:20000," + latStr + "," + lonStr + ");" +
                      "nwr[\"amenity\"=\"place_of_worship\"](around:10000," + latStr + "," + lonStr + ");" +
                      ");" +
                      "nwr._[\"name\"][!\"office\"][\"tourism\"!=\"information\"]" +
                      "[\"name\"!~\"Tourism|Office|Information|Center|Agency|Authority|Department|Bureau|Station|Hall|Hospital|Clinic|Government|Official\",i];" +
                      "out center 80;";
        }

        overpassService.getPlaces(opQuery, APP_USER_AGENT).enqueue(new Callback<OverpassService.OverpassResponse>() {
            @Override
            public void onResponse(Call<OverpassService.OverpassResponse> call, Response<OverpassService.OverpassResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().elements != null) {
                    List<ExploreItem> items = new ArrayList<>();
                    for (OverpassService.Element e : response.body().elements) {
                        if (e.tags != null && e.tags.name != null) {
                            String name = e.tags.name.trim();
                            
                            // Prefer brand if name is generic
                            if (isGenericName(name) && e.tags.brand != null) {
                                name = e.tags.brand.trim();
                            }
                            
                            if (isGenericName(name) || name.length() < 3) continue;

                            int icon = type.equals("restaurant") ? R.drawable.ic_food : (type.equals("hotel") ? R.drawable.ic_hotel : R.drawable.ic_map);
                            ExploreItem item = new ExploreItem(name, e.tags.getCleanAddress(), icon);
                            item.lat = e.getLat();
                            item.lon = e.getLon();
                            items.add(item);
                        }
                    }
                    if (items.isEmpty()) {
                        tvSectionTitle.setText("No " + label.toLowerCase() + " found here");
                        resultsContainer.removeAllViews();
                    } else {
                        showExploreResults("Top " + label + " in " + currentPlaceName, items, false);
                    }
                }
            }
            @Override public void onFailure(Call<OverpassService.OverpassResponse> call, Throwable t) {
                tvSectionTitle.setText("Map Error");
            }
        });
    }

    private boolean isGenericName(String name) {
        if (name == null) return true;
        String n = name.toLowerCase(Locale.US);
        return n.equals("restaurant") || n.equals("restaurants") || n.equals("cafe") || 
               n.equals("café") || n.equals("hotel") || n.equals("fast food") || 
               n.equals("food court") || n.equals("unnamed") || n.equals("canteen") ||
               n.equals("eatery") || n.equals("karinderya") || n.equals("carenderia") ||
               n.equals("lodging") || n.equals("boarding house") || n.equals("inn");
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
            ((ImageView) itemView.findViewById(R.id.item_image)).setImageResource(item.imageRes);
            
            itemView.setOnClickListener(v -> {
                currentLat = item.lat;
                currentLon = item.lon;
                currentPlaceName = item.title;

                isProgrammaticChange = true;
                searchDestinations.setText(item.title);
                isProgrammaticChange = false;
                
                searchDestinations.clearFocus();
                searchHandler.removeCallbacksAndMessages(null);
                hideKeyboard();

                if (isSearch) {
                    Toast.makeText(this, "Destination: " + item.title, Toast.LENGTH_SHORT).show();
                    fetchCategoryData("tourist");
                } else {
                    Toast.makeText(this, "Selected: " + item.title, Toast.LENGTH_SHORT).show();
                }
            });
            resultsContainer.addView(itemView);
        }
    }

    private void clearDisplay() {
        resultsContainer.removeAllViews();
        tvSectionTitle.setText("Explore Your Next Destination");
        planTripCard.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                clearDisplay();
                return true;
            } else if (itemId == R.id.nav_activities) {
                startActivity(new Intent(this, BudgetTracker.class));
                return true;
            } else if (itemId == R.id.nav_trip_history) {
                startActivity(new Intent(this, TripHistory.class));
                return true;
            } else if (itemId == R.id.nav_maps) {
                startActivity(new Intent(this, TripActivity.class));
                return true;
            } else if (itemId == R.id.nav_user_profile) {
                startActivity(new Intent(this, UserProfile.class));
                return true;
            }
            return false;
        });
    }

    private static class ExploreItem {
        String title, subtitle;
        int imageRes;
        double lat, lon;
        ExploreItem(String title, String subtitle, int imageRes) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageRes = imageRes;
        }
    }

    public interface NominatimService {
        @GET("search")
        Call<List<Place>> search(
            @Query("q") String query,
            @Query("format") String format,
            @Query("addressdetails") int addressDetails,
            @Query("limit") int limit,
            @Query("countrycodes") String countryCodes,
            @Header("User-Agent") String userAgent
        );

        class Place {
            @SerializedName("display_name") public String displayName;
            @SerializedName("lat") public String lat;
            @SerializedName("lon") public String lon;
        }
    }

    public interface OverpassService {
        @GET("interpreter")
        Call<OverpassResponse> getPlaces(
            @Query("data") String data,
            @Header("User-Agent") String userAgent
        );

        class OverpassResponse {
            @SerializedName("elements") public List<Element> elements;
        }

        class Element {
            @SerializedName("lat") public Double lat;
            @SerializedName("lon") public Double lon;
            @SerializedName("center") public Center center;
            @SerializedName("tags") public Tags tags;
            public double getLat() { return lat != null ? lat : (center != null ? center.lat : 0); }
            public double getLon() { return lon != null ? lon : (center != null ? center.lon : 0); }
        }

        class Center {
            @SerializedName("lat") public Double lat;
            @SerializedName("lon") public Double lon;
        }

        class Tags {
            @SerializedName("name") public String name;
            @SerializedName("addr:street") public String street;
            @SerializedName("addr:city") public String city;
            @SerializedName("addr:suburb") public String suburb;
            @SerializedName("addr:neighborhood") public String neighborhood;
            @SerializedName("addr:barangay") public String barangay;
            @SerializedName("brand") public String brand;
            @SerializedName("cuisine") public String cuisine;
            
            public String getCleanAddress() {
                StringBuilder sb = new StringBuilder();
                if (street != null) sb.append(street).append(", ");
                String area = barangay != null ? barangay : (neighborhood != null ? neighborhood : suburb);
                if (area != null) sb.append(area).append(", ");
                if (city != null) sb.append(city);
                String addr = sb.toString().trim();
                if (addr.endsWith(",")) addr = addr.substring(0, addr.length() - 1);
                
                if (addr.isEmpty()) {
                    if (cuisine != null) return cuisine + " cuisine";
                    return "Nearby location";
                }
                return addr;
            }
        }
    }
}
