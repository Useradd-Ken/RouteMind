package com.example.routemind;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ItineraryDetails extends AppCompatActivity {

    RatingBar ratingBar;
    EditText etReview;
    Button btnSubmit, btnBack;
    DBHelper DB;
    String itineraryId;
    boolean isUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_details);

        ratingBar = findViewById(R.id.rating_bar);
        etReview = findViewById(R.id.et_review);
        btnSubmit = findViewById(R.id.btn_submit_rating);
        btnBack = findViewById(R.id.btn_back);
        DB = new DBHelper(this);

        itineraryId = getIntent().getStringExtra("ITINERARY_ID");
        if (itineraryId == null) itineraryId = "default_trip";

        // Check if user has already reviewed this itinerary
        checkExistingReview();

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String review = etReview.getText().toString();
            String username = MainActivity.sessionEmail;

            if (username == null || username.isEmpty()) {
                username = "Anonymous";
            }

            if (rating == 0) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success;
            if (isUpdate) {
                success = DB.updateReview(username, itineraryId, rating, review);
            } else {
                success = DB.insertReview(username, itineraryId, rating, review);
            }

            if (success) {
                Toast.makeText(this, isUpdate ? "Review updated!" : "Review submitted!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Operation failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkExistingReview() {
        String username = MainActivity.sessionEmail;
        if (username == null || username.isEmpty()) return;

        Cursor cursor = DB.getUserReview(username, itineraryId);
        if (cursor.moveToFirst()) {
            // Review exists - index 3 is rating, index 4 is review in version 6 schema
            float existingRating = cursor.getFloat(3);
            String existingReview = cursor.getString(4);

            ratingBar.setRating(existingRating);
            etReview.setText(existingReview);
            btnSubmit.setText("Update Feedback");
            isUpdate = true;
        }
        cursor.close();
    }
}