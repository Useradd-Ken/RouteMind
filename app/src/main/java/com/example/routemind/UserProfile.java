package com.example.routemind;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

public class UserProfile extends AppCompatActivity {

    TextInputEditText editEmail, editName;
    Button btnSave, btnLogout;
    DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        editEmail = findViewById(R.id.edit_email);
        editName = findViewById(R.id.edit_name);
        btnSave = findViewById(R.id.btn_save);
        btnLogout = findViewById(R.id.btn_logout);
        DB = new DBHelper(this);

        // Fetch data from DB using session username
        if (MainActivity.sessionEmail != null && !MainActivity.sessionEmail.isEmpty()) {
            Cursor cursor = DB.getUserData(MainActivity.sessionEmail);
            if (cursor.moveToFirst()) {
                String username = cursor.getString(0);
                String email = cursor.getString(1);
                
                editName.setText(username);
                editEmail.setText(email);
            }
            cursor.close();
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSave.setOnClickListener(v -> {
            Toast.makeText(UserProfile.this, "Profile and Password Updated Successfully!", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            MainActivity.sessionEmail = "";
            Toast.makeText(UserProfile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
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
}