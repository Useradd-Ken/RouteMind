package com.example.routemind;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ItineraryDetails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_details);

        // Fetch extras from Intent
        String itinerary = getIntent().getStringExtra("itinerary");
        String destination = getIntent().getStringExtra("destination");

        TextView tvDestination = findViewById(R.id.tv_details_destination);
        TextView tvItinerary = findViewById(R.id.tv_full_itinerary);

        if (destination != null) tvDestination.setText(destination);
        if (itinerary != null) tvItinerary.setText(itinerary);

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        Button btnSubmit = findViewById(R.id.btn_submit_rating);
        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}