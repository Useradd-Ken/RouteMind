package com.example.routemind;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TripActivity extends AppCompatActivity {

    EditText etDestination, etStartDate, etEndDate, etBudget, etInterests;
    Button btnCreateTrip;
    ImageView btnBack;

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

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Goes back to the previous screen
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

                Toast.makeText(TripActivity.this,
                        "Trip Created:\n" +
                                "Destination: " + destination + "\n" +
                                "Dates: " + startDate + " - " + endDate + "\n" +
                                "Budget: " + budget + "\n" +
                                "Interests: " + interests,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}