package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class UserProfile extends AppCompatActivity {

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        dbHelper = new DatabaseHelper(this);

        EditText editEmail = findViewById(R.id.edit_email);
        EditText editName = findViewById(R.id.edit_name);
        TextView tvProfileName = findViewById(R.id.tv_profile_name);

        // Fetch session email from MainActivity
        String email = MainActivity.sessionEmail;
        if (email != null && !email.isEmpty()) {
            editEmail.setText(email);
            
            // Fetch name from SQLite
            String name = dbHelper.getName(email);
            if (name != null) {
                editName.setText(name);
                tvProfileName.setText(name);
            }
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String updatedName = editName.getText().toString().trim();
            if (MainActivity.sessionEmail != null && !MainActivity.sessionEmail.isEmpty()) {
                dbHelper.addUserProfile(MainActivity.sessionEmail, updatedName);
                tvProfileName.setText(updatedName);
                Toast.makeText(UserProfile.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UserProfile.this, "Error: No active session", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout Button Logic
        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}
            
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
            } else if (id == R.id.nav_user_profile) {
                return true;
            }
            return false;
        });
    }
}
