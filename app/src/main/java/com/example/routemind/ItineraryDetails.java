package com.example.routemind;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ItineraryDetails extends AppCompatActivity {

    private static final String TAG = "ItineraryDetails";
    RatingBar ratingBar;
    EditText etReview;
    Button btnSubmit, btnBack;
    TextView tvDestination;
    DBHelper DB;
    DatabaseHelper dbHelper;
    String itineraryId;
    boolean isUpdate = false;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_details);

        ratingBar = findViewById(R.id.rating_bar);
        etReview = findViewById(R.id.et_review);
        btnSubmit = findViewById(R.id.btn_submit_rating);
        btnBack = findViewById(R.id.btn_back);
        tvDestination = findViewById(R.id.tv_details_header);
        DB = new DBHelper(this);
        dbHelper = new DatabaseHelper(this);
        
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firestore initialization failed: " + e.getMessage());
        }

        String destination = getIntent().getStringExtra("destination");
        itineraryId = getIntent().getStringExtra("ITINERARY_ID");
        
        if (destination != null) tvDestination.setText(destination);
        if (itineraryId == null) itineraryId = "default_trip";

        checkExistingReview();

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String review = etReview.getText().toString();
            String userEmail = MainActivity.sessionEmail;
            
            if (userEmail == null || userEmail.isEmpty()) {
                userEmail = "anonymous@routemind.com";
            }

            // Fetch User Data (Name)
            String userName = dbHelper.getName(userEmail);
            if (userName == null || userName.isEmpty()) {
                userName = "Guest User";
            }

            if (rating == 0) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Save to SQLite (Local)
            boolean success;
            if (isUpdate) {
                success = DB.updateReview(userEmail, userName, itineraryId, rating, review);
            } else {
                success = DB.insertReview(userEmail, userName, itineraryId, rating, review);
            }

            // 2. Save to Firestore (Remote) - Creates collection/doc if doesn't exist
            if (firestore != null) {
                syncToFirestore(userEmail, userName, itineraryId, rating, review);
            }

            if (success) {
                Toast.makeText(this, isUpdate ? "Review updated!" : "Review submitted!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Local save failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncToFirestore(String email, String name, String itinId, float rating, String reviewText) {
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("email", email);
        reviewData.put("userName", name);
        reviewData.put("itineraryId", itinId);
        reviewData.put("rating", rating);
        reviewData.put("review", reviewText);
        reviewData.put("timestamp", com.google.firebase.Timestamp.now());

        // Document ID is a combination of email and itinerary to allow updates
        String docId = email.replace(".", "_") + "_" + itinId;

        firestore.collection("reviews")
                .document(docId)
                .set(reviewData, SetOptions.merge()) // merge() creates the "part" (fields/doc/collection) if missing
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Sync to Firestore successful"))
                .addOnFailureListener(e -> Log.e(TAG, "Sync to Firestore failed", e));
    }

    private void checkExistingReview() {
        String username = MainActivity.sessionEmail;
        if (username == null || username.isEmpty()) return;

        Cursor cursor = DB.getUserReview(username, itineraryId);
        if (cursor.moveToFirst()) {
            float existingRating = cursor.getFloat(4);
            String existingReview = cursor.getString(5);

            ratingBar.setRating(existingRating);
            etReview.setText(existingReview);
            btnSubmit.setText("Update Feedback");
            isUpdate = true;
        }
        cursor.close();
    }
}
