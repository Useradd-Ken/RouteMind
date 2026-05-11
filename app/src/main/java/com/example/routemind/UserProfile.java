package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class UserProfile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        EditText editEmail = findViewById(R.id.edit_email);
        if (MainActivity.sessionEmail != null && !MainActivity.sessionEmail.isEmpty()) {
            editEmail.setText(MainActivity.sessionEmail);
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> 
            Snackbar.make(v, "Profile saved successfully.", Snackbar.LENGTH_SHORT).show()
        );

        // Implementation of the Sign Out button
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            MainActivity.sessionEmail = ""; // Clear session email
            
            // Redirect to Login (MainActivity) and clear the activity stack
            Intent intent = new Intent(UserProfile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_user_profile);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) navigateTo(HomePage.class);
            else if (id == R.id.nav_activities) navigateTo(BudgetTracker.class);
            else if (id == R.id.nav_maps) navigateTo(TripActivity.class);
            else if (id == R.id.nav_trip_history) navigateTo(TripHistory.class);
            return id == R.id.nav_user_profile;
        });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(0, 0);
        finish();
    }
}
