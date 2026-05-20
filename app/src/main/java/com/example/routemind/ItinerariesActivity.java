package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItinerariesActivity extends AppCompatActivity {

    private static final String TAG = "ItinerariesActivity";
    private ItineraryAdapter adapter;
    private List<Itinerary> itineraryList;
    private FirebaseFirestore db;
    private ListenerRegistration itineraryListener;
    private ProgressBar loadingSpinner;
    private SwipeRefreshLayout swipeRefresh;
    private MaterialButton btnCurrent, btnBooked;
    private View indicatorCurrent, indicatorBooked;
    private String currentCollection = "cached_itineraries";
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itineraries);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase Firestore initialization failed", e);
        }

        itineraryList = new ArrayList<>();
        loadingSpinner = findViewById(R.id.loading_spinner);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        RecyclerView recyclerView = findViewById(R.id.rv_itineraries);

        btnCurrent = findViewById(R.id.btn_current);
        btnBooked = findViewById(R.id.btn_booked);
        indicatorCurrent = findViewById(R.id.indicator_current);
        indicatorBooked = findViewById(R.id.indicator_booked);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItineraryAdapter(this, itineraryList);
        recyclerView.setAdapter(adapter);

        btnCurrent.setOnClickListener(v -> {
            if (!currentCollection.equals("cached_itineraries")) {
                currentCollection = "cached_itineraries";
                adapter.setBooked(false);
                updateTabUI(true);
                loadItineraries();
            }
        });

        btnBooked.setOnClickListener(v -> {
            if (!currentCollection.equals("booked_itineraries")) {
                currentCollection = "booked_itineraries";
                adapter.setBooked(true);
                updateTabUI(false);
                loadItineraries();
            }
        });

        setupBottomNavigation();
        loadItineraries();
        swipeRefresh.setOnRefreshListener(this::loadItineraries);
    }

    private void updateTabUI(boolean isCurrent) {
        indicatorCurrent.setVisibility(isCurrent ? View.VISIBLE : View.INVISIBLE);
        indicatorBooked.setVisibility(isCurrent ? View.INVISIBLE : View.VISIBLE);

        btnCurrent.setTextColor(getResources().getColor(isCurrent ? R.color.primary_color : R.color.text_secondary, getTheme()));
        btnBooked.setTextColor(getResources().getColor(isCurrent ? R.color.text_secondary : R.color.primary_color, getTheme()));

        btnCurrent.setTypeface(null, isCurrent ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        btnBooked.setTypeface(null, !isCurrent ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadItineraries() {
        if (db == null) return;

        if (itineraryListener != null) {
            itineraryListener.remove();
        }

        loadingSpinner.setVisibility(View.VISIBLE);

        itineraryListener = db.collection(currentCollection)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    loadingSpinner.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    if (error != null) {
                        Log.e(TAG, "Firestore listen failed: " + error.getMessage());
                        return;
                    }

                    itineraryList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            try {
                                Itinerary itinerary = new Itinerary();
                                itinerary.setId(document.getId());

                                // 1. Try extracting from 'itineraryJson' field (String field)
                                String jsonString = document.getString("itineraryJson");
                                if (jsonString != null && !jsonString.isEmpty()) {
                                    parseFullJson(itinerary, jsonString);
                                }

                                // 2. If title is still null, it means itineraryJson didn't provide it or doesn't exist
                                if (itinerary.getTitle() == null) {
                                    itinerary.setTitle(document.getString("title"));
                                    itinerary.setDescription(document.getString("description"));
                                    itinerary.setCategory(document.getString("category"));
                                    itinerary.setImageUrl(document.getString("imageUrl"));
                                    Double p = document.getDouble("price");
                                    if (p != null) itinerary.setPrice(p);
                                    itinerary.setPriceBreakdown(document.getString("priceBreakdown"));
                                }

                                // 3. Ensure we have the activities list if not already parsed
                                if (itinerary.getActivities().isEmpty()) {
                                    Object rawItinerary = document.get("itinerary");
                                    if (rawItinerary != null) {
                                        String rawStr = "";
                                        if (rawItinerary instanceof Map || rawItinerary instanceof List) {
                                            rawStr = gson.toJson(rawItinerary);
                                        } else if (rawItinerary instanceof String) {
                                            rawStr = (String) rawItinerary;
                                        }
                                        itinerary.setItinerary(rawStr);
                                        itinerary.setActivities(parseActivitiesList(rawStr));
                                    }
                                }

                                itineraryList.add(itinerary);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing document: " + document.getId(), e);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void parseFullJson(Itinerary itinerary, String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("title")) itinerary.setTitle(obj.getString("title"));
            if (obj.has("description")) itinerary.setDescription(obj.getString("description"));
            if (obj.has("category")) itinerary.setCategory(obj.getString("category"));
            if (obj.has("price")) itinerary.setPrice(obj.optDouble("price", 0.0));
            if (obj.has("imageUrl")) itinerary.setImageUrl(obj.getString("imageUrl"));

            String activitiesJson = "";
            if (obj.has("itinerary")) {
                activitiesJson = obj.get("itinerary").toString();
            } else if (obj.has("activities")) {
                activitiesJson = obj.get("activities").toString();
            }
            
            if (!activitiesJson.isEmpty()) {
                itinerary.setItinerary(activitiesJson);
                itinerary.setActivities(parseActivitiesList(activitiesJson));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse full JSON string", e);
        }
    }

    private List<ItineraryItem> parseActivitiesList(String activitiesJson) {
        List<ItineraryItem> list = new ArrayList<>();
        if (activitiesJson == null || activitiesJson.isEmpty()) return list;
        try {
            JSONArray arr = null;
            if (activitiesJson.trim().startsWith("[")) {
                arr = new JSONArray(activitiesJson);
            } else if (activitiesJson.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(activitiesJson);
                arr = obj.optJSONArray("itinerary");
                if (arr == null) arr = obj.optJSONArray("activities");
            }

            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    ItineraryItem act = new ItineraryItem();
                    act.setDay(item.optInt("day", 1));
                    act.setTime(item.optString("time", "--:--"));
                    act.setActivity(item.optString("activity", ""));
                    act.setLocation(item.optString("location", ""));
                    list.add(act);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Activities JSON is not structured or parse error");
        }
        return list;
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
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (itineraryListener != null) {
            itineraryListener.remove();
            itineraryListener = null;
        }
    }
}
