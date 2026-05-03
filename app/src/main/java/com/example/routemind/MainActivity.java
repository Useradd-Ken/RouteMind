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
    Button btnLogin;
    TextView tvResult, tvSignup;
    FirebaseAuth mAuth;
    DatabaseHelper dbHelper;

    public static String sessionEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            // Graceful failure if google-services.json is missing
        }
        
        dbHelper = new DatabaseHelper(this);

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
        tvResult   = findViewById(R.id.tvResult);
        tvSignup   = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Admin Access Check
                if (email.equals("admin@routemind.com") && pass.equals("admin123")) {
                    sessionEmail = "admin";
                    Toast.makeText(MainActivity.this, "Welcome Admin", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), AdminPanelActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }

                if (mAuth != null) {
                    loginUser(email, pass);
                } else {
                    // Fallback to SQLite if Firebase isn't configured
                    if (dbHelper.checkUser(email, pass)) {
                        sessionEmail = email;
                        navigateToHome();
                    } else {
                        Toast.makeText(MainActivity.this, "Login failed. Check credentials or Firebase config.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignUpActivity.class));
        });
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