package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    ImageView btnGoogleLogin;
    TextView tvResult;

    public static String sessionEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                if (user.equals("admin") && pass.equals("1234")) {
                    sessionEmail = user;
                    Intent intent = new Intent(MainActivity.this, UserProfile.class);
                    startActivity(intent);
                } else {
                    tvResult.setText("Login failed!");
                }
            }
        });

        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionEmail = "google_user@example.com";
                Intent intent = new Intent(MainActivity.this, UserProfile.class);
                startActivity(intent);
            }
        });

        View bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            BottomNavigationView bottomNavigationView = (BottomNavigationView) bottomNav;
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
    }
}