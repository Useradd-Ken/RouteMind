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
import java.util.List;

public class TripActivity extends AppCompatActivity {

    AutoCompleteTextView etDestination;
    EditText etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip;
    ImageView btnBack;
    DBHelper DB;

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
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        btnBack       = findViewById(R.id.btnBack);
        DB = new DBHelper(this);

        // Fetch destinations from SQLite DB
        List<String> destinations = DB.getAllDestinations();
        
        // Set up AutoComplete for Destination using DB data
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, destinations);
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

                // Sync budget with BudgetTracker
                if (!budget.isEmpty()) {
                    try {
                        double budgetValue = Double.parseDouble(budget);
                        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putLong("totalBudget", Double.doubleToLongBits(budgetValue));
                        editor.apply();
                    } catch (NumberFormatException e) {
                        // ignore invalid number
                    }
                }

                Toast.makeText(TripActivity.this,
                        "Trip Created Successfully!\nYour budget has been updated in the Budget Tracker.",
                        Toast.LENGTH_LONG).show();
                
                // Redirect to Budget Tracker to see the update
                startActivity(new Intent(TripActivity.this, BudgetTracker.class));
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
                    String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    editText.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }
}