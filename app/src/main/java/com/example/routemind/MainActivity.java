package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin, btnGoogleLogin;
    TextView tvResult, tvSignup;
    FirebaseAuth mAuth;
    DatabaseHelper dbHelper;

    public static String sessionEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Local Database
        dbHelper = new DatabaseHelper(this);

        // Initialize Firebase
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            // Graceful failure if google-services.json is missing
        }
        
        // Auto-login check
        if (mAuth != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                sessionEmail = currentUser.getEmail();
                navigateToHome();
            }
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvResult   = findViewById(R.id.tvResult);
        tvSignup   = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Admin / Static Login Logic (Combined from both sides)
                if ((email.equals("admin@routemind.com") && pass.equals("admin123")) || 
                    (email.equals("admin") && pass.equals("1234"))) {
                    
                    sessionEmail = "admin@routemind.com";
                    navigateToHome();
                    return;
                }

                // Local Fallback shortcut (from main)
                if (email.contains("@") && pass.equals("1234")) {
                    sessionEmail = email;
                    navigateToHome();
                    return;
                }

                // Default Firebase Login
                if (mAuth != null) {
                    loginUser(email, pass);
                } else {
                    tvResult.setText("Firebase not configured. Use admin login.");
                    Toast.makeText(MainActivity.this, "Firebase not configured. Use admin login.", Toast.LENGTH_LONG).show();
                }
            }
        });

        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignUpActivity.class));
        });

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Google Sign-In is currently disabled. Use admin login.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        sessionEmail = user != null ? user.getEmail() : email;
                        Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        tvResult.setText("Login failed: " + task.getException().getMessage());
                        Toast.makeText(MainActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHome() {
        startActivity(new Intent(MainActivity.this, HomePage.class));
        finish();
    }
}
