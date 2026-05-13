package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;

public class TourDetailsActivity extends AppCompatActivity {

    private String title, description, itinerary, imageUrl, category, priceBreakdown, destination, originalItineraryId;
    private double price;
    private static final String PREF_NAME = "BudgetPrefs";
    private static final String PREF_TRANSACTIONS = "savedTransactions";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;
    
    private EditText etTitle, etDescription, etItinerary;
    private ImageButton btnEditToggle;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_details);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        // Safety check for session username
        if ((MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) && mAuth.getCurrentUser() != null) {
            MainActivity.sessionEmail = mAuth.getCurrentUser().getEmail();
            MainActivity.sessionUsername = dbHelper.getUsernameByEmail(MainActivity.sessionEmail);
            if (MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) {
                MainActivity.sessionUsername = dbHelper.getName(MainActivity.sessionEmail);
            }
            if (MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) {
                MainActivity.sessionUsername = MainActivity.sessionEmail.split("@")[0];
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Intent intent = getIntent();
        title = intent.getStringExtra("TITLE");
        description = intent.getStringExtra("DESCRIPTION");
        itinerary = intent.getStringExtra("ITINERARY");
        imageUrl = intent.getStringExtra("IMAGE_URL");
        category = intent.getStringExtra("CATEGORY");
        price = intent.getDoubleExtra("PRICE", 0);
        priceBreakdown = intent.getStringExtra("PRICE_BREAKDOWN");
        destination = intent.getStringExtra("DESTINATION");
        originalItineraryId = intent.getStringExtra("ORIGINAL_ITINERARY_ID");

        etTitle = findViewById(R.id.et_details_title);
        etDescription = findViewById(R.id.et_details_description);
        etItinerary = findViewById(R.id.et_details_itinerary);
        btnEditToggle = findViewById(R.id.btn_edit_toggle);

        etTitle.setText(title);
        etDescription.setText(description);
        etItinerary.setText(itinerary);
        ((TextView) findViewById(R.id.tv_details_price)).setText(String.format("₱%.2f", price));
        
        TextView tvBreakdown = findViewById(R.id.tv_details_price_breakdown);
        if (priceBreakdown != null && !priceBreakdown.isEmpty()) {
            tvBreakdown.setText(priceBreakdown);
        } else {
            tvBreakdown.setText("Price breakdown not available.");
        }
        
        ImageView ivDetailsImage = findViewById(R.id.iv_details_image);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.routemind)
                .error(R.drawable.routemind)
                .into(ivDetailsImage);

        btnEditToggle.setOnClickListener(v -> toggleEditing());
        findViewById(R.id.btn_book_now).setOnClickListener(v -> bookTour());
    }

    private void toggleEditing() {
        isEditing = !isEditing;
        // The user clarified that editing the itinerary means the schedule itself
        etItinerary.setEnabled(isEditing);
        
        // Optionally keep title/description editable as well for personalization
        etTitle.setEnabled(isEditing);
        etDescription.setEnabled(isEditing);
        
        if (isEditing) {
            btnEditToggle.setImageResource(android.R.drawable.ic_menu_save);
            Snackbar.make(btnEditToggle, "You can now modify the itinerary steps!", Snackbar.LENGTH_SHORT).show();
            etItinerary.requestFocus();
        } else {
            btnEditToggle.setImageResource(android.R.drawable.ic_menu_edit);
            title = etTitle.getText().toString();
            description = etDescription.getText().toString();
            itinerary = etItinerary.getText().toString();
            Snackbar.make(btnEditToggle, "Changes saved to your personalized version.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void bookTour() {
        // Sync latest edits
        title = etTitle.getText().toString();
        description = etDescription.getText().toString();
        itinerary = etItinerary.getText().toString();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Snackbar.make(findViewById(android.R.id.content), "Please log in to add trips.", Snackbar.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        double totalBudget = Double.longBitsToDouble(prefs.getLong("totalBudget", Double.doubleToLongBits(0)));
        double foodTotal = Double.longBitsToDouble(prefs.getLong("foodTotal", Double.doubleToLongBits(0)));
        double transportTotal = Double.longBitsToDouble(prefs.getLong("transportTotal", Double.doubleToLongBits(0)));
        double stayTotal = Double.longBitsToDouble(prefs.getLong("stayTotal", Double.doubleToLongBits(0)));

        double remaining = totalBudget - (foodTotal + transportTotal + stayTotal);

        if (remaining >= price) {
            SharedPreferences.Editor editor = prefs.edit();
            String cat = (category == null || category.isEmpty()) ? "Transport" : category;
            
            if (cat.equalsIgnoreCase("Food")) {
                foodTotal += price;
                editor.putLong("foodTotal", Double.doubleToLongBits(foodTotal));
            } else if (cat.equalsIgnoreCase("Stay")) {
                stayTotal += price;
                editor.putLong("stayTotal", Double.doubleToLongBits(stayTotal));
            } else {
                transportTotal += price;
                editor.putLong("transportTotal", Double.doubleToLongBits(transportTotal));
            }

            // Create NEW data for the current user in Firebase and Local DB
            saveToFirestoreAndLocal();
            
            // Increment usage count on the original source itinerary in Firebase
            if (originalItineraryId != null && !originalItineraryId.isEmpty()) {
                db.collection("booked_itineraries").document(originalItineraryId)
                        .update("usageCount", FieldValue.increment(1))
                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to increment usage", e));
            }

            saveTransaction(prefs, cat, price);
            updateSavedSuggestionsAfterRemoval(prefs, title);
            editor.apply();

            Snackbar.make(findViewById(android.R.id.content), "Itinerary added to your trip successfully!", Snackbar.LENGTH_LONG).show();
            findViewById(android.R.id.content).postDelayed(() -> {
                Intent backIntent = new Intent(this, BudgetTracker.class);
                backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(backIntent);
                finish();
            }, 1500);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Insufficient budget for this itinerary.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveToFirestoreAndLocal() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String username = MainActivity.sessionUsername;
        
        // Create a unique copy of the itinerary for this user
        String id = db.collection("booked_itineraries").document().getId();
        Itinerary itineraryObj = new Itinerary(id, user.getUid(), username, title, description, itinerary, imageUrl, category, price, priceBreakdown, destination);
        
        // Save as a NEW document so the user has their own record
        db.collection("booked_itineraries").document(id).set(itineraryObj);
        
        // Sync to local SQLite for offline access and trip tracking
        dbHelper.saveBookedItinerary(itineraryObj);
    }

    private void saveTransaction(SharedPreferences prefs, String category, double amount) {
        try {
            JSONArray array = new JSONArray(prefs.getString(PREF_TRANSACTIONS, "[]"));
            JSONObject obj = new JSONObject();
            obj.put("category", category);
            obj.put("amount", amount);
            array.put(obj);
            prefs.edit().putString(PREF_TRANSACTIONS, array.toString()).apply();
            
            dbHelper.addExpense(MainActivity.sessionUsername, category, amount, System.currentTimeMillis());
        } catch (Exception e) {
            Log.e("TourDetails", "Transaction save error", e);
        }
    }

    private void updateSavedSuggestionsAfterRemoval(SharedPreferences prefs, String titleToRemove) {
        String savedJson = prefs.getString("savedSuggestions", "");
        if (savedJson.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(savedJson);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.optString("title").equals(titleToRemove)) newArray.put(obj);
            }
            prefs.edit().putString("savedSuggestions", newArray.toString()).apply();
        } catch (Exception ignored) {}
    }
}
