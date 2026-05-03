package com.example.routemind;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ItineraryDetails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_details);

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        Button btnSubmit = findViewById(R.id.btn_submit_rating);
        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}