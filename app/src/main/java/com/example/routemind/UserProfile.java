package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserProfile extends AppCompatActivity {

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
<<<<<<< Updated upstream
=======

        dbHelper = new DatabaseHelper(this);

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
>>>>>>> Stashed changes
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String updatedName = editName.getText().toString();
            if (MainActivity.sessionEmail != null) {
                dbHelper.addUser(MainActivity.sessionEmail, updatedName);
                Toast.makeText(UserProfile.this, "Profile and Password Updated Successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_user_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_activities) {
                startActivity(new Intent(getApplicationContext(), sqlite.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_maps) {
                return true;
            } else if (id == R.id.nav_trip_history) {
                startActivity(new Intent(getApplicationContext(), TripHistory.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_user_profile) {
                return true;
            }
            return false;
        });
    }
}