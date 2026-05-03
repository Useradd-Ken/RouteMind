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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.List;

public class TripActivity extends AppCompatActivity {

    AutoCompleteTextView etDestination;
    EditText etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip;
    DatabaseHelper dbHelper;

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
        dbHelper = new DatabaseHelper(this);

        // Fetch destinations from SQLite DB
        List<String> destinations = dbHelper.getAllDestinations();
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, destinations);
        etDestination.setAdapter(adapter);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnCreateTrip.setOnClickListener(v -> {
            String destination = etDestination.getText().toString();
            String startDate   = etStartDate.getText().toString();
            String endDate     = etEndDate.getText().toString();
            String budget      = etBudget.getText().toString();

            if (destination.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(TripActivity.this, "Please fill in Destination and Dates", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!budget.isEmpty()) {
                try {
                    double budgetValue = Double.parseDouble(budget);
                    SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong("totalBudget", Double.doubleToLongBits(budgetValue));
                    editor.apply();
                } catch (NumberFormatException ignored) {}
            }

            Toast.makeText(TripActivity.this, "Trip Created Successfully!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(TripActivity.this, BudgetTracker.class));
            finish();
        });

        setupNavigation();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_maps);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomePage.class));
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), BudgetTracker.class));
                finish();
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                finish();
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                finish();
                return true;
            }
            return id == R.id.nav_maps;
        });
    }

    private void showDatePicker(EditText editText) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            editText.setText(day + "/" + (month + 1) + "/" + year);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
}
