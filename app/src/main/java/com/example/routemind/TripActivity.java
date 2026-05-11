package com.example.routemind;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TripActivity extends AppCompatActivity {
    private static final String TAG = "TripActivity";
    private AutoCompleteTextView etDestination;
    private EditText etStartDate, etEndDate, etBudget, etInterests;
    private static final String PREF_NAME = "BudgetPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        etDestination = findViewById(R.id.etDestination);
        etStartDate   = findViewById(R.id.etStartDate);
        etEndDate     = findViewById(R.id.etEndDate);
        etBudget      = findViewById(R.id.etBudget);
        etInterests   = findViewById(R.id.etInterests);
        
        HomePage.PhotonAutocompleteAdapter adapter = new HomePage.PhotonAutocompleteAdapter(this);
        etDestination.setAdapter(adapter);
        etDestination.setThreshold(1);
        etDestination.setOnItemClickListener((parent, view, position, id) -> {
            HomePage.Feature feature = (HomePage.Feature) parent.getItemAtPosition(position);
            if (feature != null && feature.properties != null) {
                etDestination.setText(feature.properties.getDisplayName());
            }
        });

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            startActivity(new Intent(this, HomePage.class));
            finish();
        });

        findViewById(R.id.btnCreateTrip).setOnClickListener(v -> handleTripCreation(v));
        setupBottomNavigation();
    }

    private void handleTripCreation(android.view.View v) {
        String dest = etDestination.getText().toString();
        String start = etStartDate.getText().toString();
        String end = etEndDate.getText().toString();
        String budget = etBudget.getText().toString();
        String interests = etInterests.getText().toString();

        if (dest.isEmpty() || start.isEmpty() || end.isEmpty() || budget.isEmpty()) {
            Snackbar.make(v, "Please fill in all required fields.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        long days = calculateDays(start, end);
        if (days < 1) {
            Snackbar.make(v, "Return date must be after start date.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        double budgetValue;
        try {
            budgetValue = Double.parseDouble(budget);
        } catch (NumberFormatException e) {
            Snackbar.make(v, "Invalid budget amount.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong("totalBudget", Double.doubleToLongBits(budgetValue));
        editor.putLong("foodTotal", 0);
        editor.putLong("transportTotal", 0);
        editor.putLong("stayTotal", 0);
        editor.putString("savedSuggestions", "[]"); 
        editor.putString("savedTransactions", "[]");
        editor.apply();

        Snackbar.make(v, "Preparing your itinerary...", Snackbar.LENGTH_SHORT).show();
        v.postDelayed(() -> {
            Intent intent = new Intent(this, BudgetTracker.class);
            intent.putExtra("DESTINATION", dest);
            intent.putExtra("START_DATE", start);
            intent.putExtra("END_DATE", end);
            intent.putExtra("BUDGET", budgetValue);
            intent.putExtra("INTERESTS", interests);
            intent.putExtra("DURATION_DAYS", days);
            startActivity(intent);
            finish();
        }, 1500);
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_maps);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) navigateTo(HomePage.class);
            else if (id == R.id.nav_activities) navigateTo(BudgetTracker.class);
            else if (id == R.id.nav_trip_history) navigateTo(TripHistory.class);
            else if (id == R.id.nav_user_profile) navigateTo(UserProfile.class);
            return id == R.id.nav_maps;
        });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        finish();
    }

    private long calculateDays(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            if (d1 != null && d2 != null) {
                return TimeUnit.DAYS.convert(d2.getTime() - d1.getTime(), TimeUnit.MILLISECONDS) + 1;
            }
        } catch (ParseException e) { Log.e(TAG, "Date error", e); }
        return 0;
    }

    private void showDatePicker(EditText et) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> et.setText(d + "/" + (m + 1) + "/" + y),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
}
