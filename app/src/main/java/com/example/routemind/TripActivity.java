package com.example.routemind;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Locale;

public class TripActivity extends AppCompatActivity {

    AutoCompleteTextView etDestination;
    EditText etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip;
    ImageView btnBack;
    private DatabaseHelper dbHelper;

    private static final String PREF_NAME = "BudgetPrefs";

    // Sample destinations for suggestions
    private static final String[] DESTINATIONS = new String[] {
            "Manila", "Cebu", "Davao", "Palawan", "Bohol", "Boracay",
            "Baguio", "Vigan", "Siargao", "Zamboanga", "Legazpi",
            "Puerto Princesa", "Tagaytay", "Dumaguete", "Iloilo"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        dbHelper = new DatabaseHelper(this);

        etDestination = findViewById(R.id.etDestination);
        etStartDate   = findViewById(R.id.etStartDate);
        etEndDate     = findViewById(R.id.etEndDate);
        etBudget      = findViewById(R.id.etBudget);
        etInterests   = findViewById(R.id.etInterests);
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        btnBack       = findViewById(R.id.btnBack);

        // Set up AutoComplete for Destination
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DESTINATIONS);
        etDestination.setAdapter(adapter);

        // Set up DatePickers for Start Date and End Date
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TripActivity.this, HomePage.class));
                finish();
            }
        });

        btnCreateTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String destination = etDestination.getText().toString();
                String startDate   = etStartDate.getText().toString();
                String endDate     = etEndDate.getText().toString();
                String budget      = etBudget.getText().toString();
                String interests   = etInterests.getText().toString();

                if (destination.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
                    Toast.makeText(TripActivity.this, "Please fill in Destination and Dates", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Clear previous itinerary data to force AI re-generation for the new trip
                dbHelper.clearItinerary();

                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                
                editor.putString("destination", destination);
                editor.putString("startDate", startDate);
                editor.putString("endDate", endDate);
                editor.putString("interests", interests);

                if (!budget.isEmpty()) {
                    try {
                        double budgetValue = Double.parseDouble(budget);
                        editor.putLong("totalBudget", Double.doubleToLongBits(budgetValue));
                    } catch (NumberFormatException e) {
                        // ignore invalid number
                    }
                }
                editor.apply();

                Toast.makeText(TripActivity.this,
                        "Trip Created Successfully!",
                        Toast.LENGTH_LONG).show();
                
                // Redirect to Itinerary Page
                startActivity(new Intent(TripActivity.this, ItineraryPageActivity.class));
                finish();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_maps);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomePage.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), BudgetTracker.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void showDatePicker(EditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year1);
                    editText.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }
}
