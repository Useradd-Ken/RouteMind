package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    Button btnGoogleLogin; // Kept to avoid layout errors, but logic removed
    TextView tvResult;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvResult   = findViewById(R.id.tvResult);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsername.getText().toString();
                String pass = etPassword.getText().toString();

<<<<<<< Updated upstream
                if (user.equals("admin") && pass.equals("1234")) {
                    Intent intent = new Intent(MainActivity.this, TripActivity.class);
                    startActivity(intent);
                } else {
                    tvResult.setText("Login failed!");
=======
                // Static Login Logic
                if (user.equals("admin") && pass.equals("1234")) {
                    sessionEmail = "admin@routemind.com";
                    if (dbHelper.getName(sessionEmail) == null) {
                        dbHelper.addUser(sessionEmail, "Administrator");
                    }
                    navigateToHome();
                } else if (user.contains("@") && pass.equals("1234")) {
                    sessionEmail = user;
                    if (dbHelper.getName(user) == null) {
                        dbHelper.addUser(user, user.split("@")[0]);
                    }
                    navigateToHome();
                } else {
                    tvResult.setText("Login failed! Use 'admin' and '1234'");
>>>>>>> Stashed changes
                }
            }
        });

        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
<<<<<<< Updated upstream
                // Link to TripActivity for now as requested
                Intent intent = new Intent(MainActivity.this, TripActivity.class);
                startActivity(intent);
=======
                Toast.makeText(MainActivity.this, "Google Sign-In is currently disabled. Use admin login.", Toast.LENGTH_SHORT).show();
>>>>>>> Stashed changes
            }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
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
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, HomePage.class);
        startActivity(intent);
        finish();
    }
}