package com.example.routemind;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class AdminPanelActivity extends AppCompatActivity {

    private static final String TAG = "AdminPanelActivity";
    TabLayout tabLayout;
    TextView tvContent;
    Button btnLogout;
    DBHelper DB;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        tabLayout = findViewById(R.id.admin_tabs);
        tvContent = findViewById(R.id.tv_admin_content);
        btnLogout = findViewById(R.id.btn_logout);
        DB = new DBHelper(this);
        
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firestore initialization failed: " + e.getMessage());
        }

        showUsers();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showUsers();
                        break;
                    case 1:
                        showReviews();
                        break;
                    case 2:
                        showDestinations();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnLogout.setOnClickListener(v -> {
            MainActivity.sessionEmail = "";
            Intent intent = new Intent(AdminPanelActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showUsers() {
        Cursor cursor = DB.getAllUsers();
        StringBuilder builder = new StringBuilder();
        builder.append("USERS IN DATABASE (Local):\n\n");
        if (cursor.getCount() == 0) {
            builder.append("No local users found.");
        } else {
            while (cursor.moveToNext()) {
                builder.append("Username: ").append(cursor.getString(0)).append("\n");
                builder.append("Email: ").append(cursor.getString(1)).append("\n");
                builder.append("Password: ").append(cursor.getString(2)).append("\n");
                builder.append("--------------------\n");
            }
        }
        cursor.close();
        tvContent.setText(builder.toString());
    }

    private void showReviews() {
        if (firestore == null) {
            showLocalReviews();
            return;
        }

        tvContent.setText("Loading reviews from Cloud...");
        firestore.collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("REVIEWS IN DATABASE (Cloud):\n\n");
                        if (task.getResult().isEmpty()) {
                            builder.append("No reviews found in Cloud.");
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                builder.append("User Email: ").append(document.getString("email")).append("\n");
                                builder.append("User Name: ").append(document.getString("userName")).append("\n");
                                builder.append("Itinerary: ").append(document.getString("itineraryId")).append("\n");
                                builder.append("Rating: ").append(document.get("rating")).append(" stars\n");
                                builder.append("Review: ").append(document.getString("review")).append("\n");
                                builder.append("Time: ").append(document.get("timestamp") != null ? document.get("timestamp").toString() : "N/A").append("\n");
                                builder.append("--------------------\n");
                            }
                        }
                        tvContent.setText(builder.toString());
                    } else {
                        Log.e(TAG, "Error getting reviews: ", task.getException());
                        showLocalReviews();
                    }
                });
    }

    private void showLocalReviews() {
        Cursor cursor = DB.getAllReviews();
        StringBuilder builder = new StringBuilder();
        builder.append("REVIEWS IN DATABASE (Local Fallback):\n\n");
        if (cursor.getCount() == 0) {
            builder.append("No local reviews found.");
        } else {
            while (cursor.moveToNext()) {
                builder.append("User Email: ").append(cursor.getString(1)).append("\n");
                builder.append("User Name: ").append(cursor.getString(2)).append("\n");
                builder.append("Itinerary: ").append(cursor.getString(3)).append("\n");
                builder.append("Rating: ").append(cursor.getFloat(4)).append(" stars\n");
                builder.append("Review: ").append(cursor.getString(5)).append("\n");
                builder.append("Time: ").append(cursor.getString(6)).append("\n");
                builder.append("--------------------\n");
            }
        }
        cursor.close();
        tvContent.setText(builder.toString());
    }

    private void showDestinations() {
        List<String> destinations = DB.getAllDestinations();
        StringBuilder builder = new StringBuilder();
        builder.append("DESTINATIONS IN DATABASE:\n\n");
        for (String dest : destinations) {
            builder.append("- ").append(dest).append("\n");
        }
        tvContent.setText(builder.toString());
    }
}
