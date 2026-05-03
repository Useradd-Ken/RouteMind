package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    private static final String TAG = "UserProfile";
    DatabaseHelper dbHelper;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        dbHelper = new DatabaseHelper(this);
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firestore initialization failed: " + e.getMessage());
        }

        EditText editEmail = findViewById(R.id.edit_email);
        EditText editName = findViewById(R.id.edit_name);

        // Fetch session email from MainActivity
        if (MainActivity.sessionEmail != null && !MainActivity.sessionEmail.isEmpty()) {
            editEmail.setText(MainActivity.sessionEmail);
            
            // Fetch name from SQLite
            String name = dbHelper.getName(MainActivity.sessionEmail);
            if (name != null) {
                editName.setText(name);
            }
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String updatedName = editName.getText().toString();
            String email = MainActivity.sessionEmail;

            if (email != null && !email.isEmpty()) {
                // 1. Save to Local SQLite
                dbHelper.addUserProfile(email, updatedName);
                
                // 2. Sync to Remote Firestore (Creates 'users' collection/doc if doesn't exist)
                if (firestore != null) {
                    syncUserProfileToFirestore(email, updatedName);
                }

                Toast.makeText(UserProfile.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout Button Logic
        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            MainActivity.sessionEmail = "";
            Intent intent = new Intent(UserProfile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_user_profile);

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
                startActivity(new Intent(getApplicationContext(), TripActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_user_profile) {
                return true;
            }
            return false;
        });
    }

    private void syncUserProfileToFirestore(String email, String name) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("name", name);
        userMap.put("lastUpdated", com.google.firebase.Timestamp.now());

        // Using email as document ID (sanitized)
        String docId = email.replace(".", "_");

        firestore.collection("users")
                .document(docId)
                .set(userMap, SetOptions.merge()) // merge() ensures creation of path if missing
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile synced to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "User profile sync failed", e));
    }
}
