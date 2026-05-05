package com.example.routemind;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TripActivity extends AppCompatActivity {

    AutoCompleteTextView etDestination;
    EditText etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip;
    ImageView btnBack;
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

        HomePage.PhotonAutocompleteAdapter adapter = new HomePage.PhotonAutocompleteAdapter(this);
        etDestination.setAdapter(adapter);
        // Changed threshold to 1 to match activity_trip.xml and improve responsiveness
        etDestination.setThreshold(1);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(TripActivity.this, HomePage.class));
            finish();
        });

        btnCreateTrip.setOnClickListener(v -> {
            String destination = etDestination.getText().toString();
            String startDateStr = etStartDate.getText().toString();
            String endDateStr   = etEndDate.getText().toString();
            String budget      = etBudget.getText().toString();
            String interests   = etInterests.getText().toString();

            if (destination.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
                Toast.makeText(TripActivity.this, "Please fill in Destination and Dates", Toast.LENGTH_SHORT).show();
                return;
            }

            long days = calculateDays(startDateStr, endDateStr);
            if (days < 1) {
                Toast.makeText(TripActivity.this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                return;
            }

            double budgetValue = 0;
            if (!budget.isEmpty()) {
                try {
                    budgetValue = Double.parseDouble(budget);
                } catch (NumberFormatException ignored) {}
            }

            // Reset Budget Tracker Data for a fresh Itinerary
            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("totalBudget", Double.doubleToLongBits(budgetValue));
            editor.putLong("foodTotal", Double.doubleToLongBits(0));
            editor.putLong("transportTotal", Double.doubleToLongBits(0));
            editor.putLong("stayTotal", Double.doubleToLongBits(0));
            editor.putString("savedSuggestions", ""); 
            editor.putString("savedTransactions", "");
            editor.apply();

            Toast.makeText(TripActivity.this, "Initializing your trip itinerary...", Toast.LENGTH_LONG).show();
            
            Intent intent = new Intent(TripActivity.this, BudgetTracker.class);
            intent.putExtra("DESTINATION", destination);
            intent.putExtra("START_DATE", startDateStr);
            intent.putExtra("END_DATE", endDateStr);
            intent.putExtra("BUDGET", budgetValue);
            intent.putExtra("INTERESTS", interests);
            intent.putExtra("DURATION_DAYS", days);
            startActivity(intent);
            finish();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_maps);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomePage.class));
                finish();
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(this, BudgetTracker.class));
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(this, TripHistory.class));
                finish();
                return true;
            } else if (id == R.id.nav_user_profile) {
                startActivity(new Intent(this, UserProfile.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private long calculateDays(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            if (d1 != null && d2 != null) {
                long diff = d2.getTime() - d1.getTime();
                return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void showDatePicker(EditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
            editText.setText(selectedDate);
        }, year, month, day).show();
    }
}
