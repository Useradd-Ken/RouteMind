package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    TextInputEditText editEmail, editName, editPassword;
    TextView tvProfileName;
    Button btnSave, btnLogout;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editEmail = findViewById(R.id.edit_email);
        editName = findViewById(R.id.edit_name);
        editPassword = findViewById(R.id.edit_password);
        tvProfileName = findViewById(R.id.tv_profile_name);
        btnSave = findViewById(R.id.btn_save);
        btnLogout = findViewById(R.id.btn_logout);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // Fetch user data from Firestore
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String fullName = documentSnapshot.getString("fullName");
                            String email = documentSnapshot.getString("email");
                            
                            // Use fullName if username is null
                            String displayName = (username != null) ? username : fullName;
                            
                            editName.setText(displayName);
                            editEmail.setText(email);
                            tvProfileName.setText(displayName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserProfile.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                    });
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSave.setOnClickListener(v -> {
            if (currentUser != null) {
                String updatedName = editName.getText().toString().trim();
                String newPassword = editPassword.getText().toString().trim();

                // 1. Update Firestore Profile Name
                Map<String, Object> updates = new HashMap<>();
                updates.put("username", updatedName);

                db.collection("users").document(currentUser.getUid())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            tvProfileName.setText(updatedName);
                            Toast.makeText(UserProfile.this, "Name Updated Successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UserProfile.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                // 2. Update Password if provided
                if (!newPassword.isEmpty()) {
                    if (newPassword.length() < 6) {
                        Toast.makeText(UserProfile.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    } else {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(UserProfile.this, "Password Updated!", Toast.LENGTH_SHORT).show();
                                        editPassword.setText(""); // Clear field
                                    } else {
                                        Toast.makeText(UserProfile.this, "Password update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                }
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            MainActivity.sessionEmail = "";
            Toast.makeText(UserProfile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserProfile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        setupNavigation();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_user_profile);

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
            } else if (id == R.id.nav_maps) {
                startActivity(new Intent(getApplicationContext(), TripActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                finish();
                return true;
            } else return id == R.id.nav_user_profile;
        });
    }
}
