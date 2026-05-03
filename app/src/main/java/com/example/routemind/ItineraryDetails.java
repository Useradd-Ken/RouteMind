package com.example.routemind;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ItineraryDetails extends AppCompatActivity {

    RatingBar ratingBar;
    EditText etReview;
    TextView tvDestination, tvFullItinerary;
    Button btnSubmit, btnBack;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String itineraryId;
    boolean isUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ratingBar = findViewById(R.id.rating_bar);
        etReview = findViewById(R.id.et_review);
        tvDestination = findViewById(R.id.tv_details_destination);
        tvFullItinerary = findViewById(R.id.tv_full_itinerary);
        btnSubmit = findViewById(R.id.btn_submit_rating);
        btnBack = findViewById(R.id.btn_back);

        itineraryId = getIntent().getStringExtra("ITINERARY_ID");
        String destination = getIntent().getStringExtra("destination");
        String itineraryText = getIntent().getStringExtra("itinerary");

        if (itineraryId == null) itineraryId = "default_trip";
        
        if (destination != null) tvDestination.setText(destination);
        if (itineraryText != null) tvFullItinerary.setText(itineraryText);

        checkExistingReview();

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String review = etReview.getText().toString().trim();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(this, "Please log in to submit a review", Toast.LENGTH_SHORT).show();
                return;
            }

            if (rating == 0) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            saveReviewToFirestore(currentUser.getUid(), currentUser.getEmail(), rating, review);
        });
    }

    private void checkExistingReview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("reviews")
                .document(itineraryId + "_" + currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double rating = documentSnapshot.getDouble("rating");
                        String review = documentSnapshot.getString("review");

                        if (rating != null) ratingBar.setRating(rating.floatValue());
                        etReview.setText(review);
                        btnSubmit.setText("Update Feedback");
                        isUpdate = true;
                    }
                });
    }

    private void saveReviewToFirestore(String userId, String userEmail, float rating, String review) {
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("userId", userId);
        reviewData.put("userEmail", userEmail);
        reviewData.put("itineraryId", itineraryId);
        reviewData.put("rating", rating);
        reviewData.put("review", review);
        reviewData.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("reviews")
                .document(itineraryId + "_" + userId)
                .set(reviewData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isUpdate ? "Review updated!" : "Review submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
