package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;
    public static String sessionEmail = "";
    public static String sessionUsername = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        View btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Snackbar.make(v, "Please fill in all fields", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Syncing with existing Firebase Authentication
            mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sessionEmail = user.getEmail();
                            updateSessionUsername();
                            navigateToHome();
                        }
                    } else {
                        Snackbar.make(v, "Authentication failed: " + task.getException().getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
        });

        btnGoogleLogin.setOnClickListener(v -> {
            // Placeholder for Google Sign-In logic
            sessionEmail = "google_user@gmail.com";
            sessionUsername = "Google User";
            navigateToHome();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            sessionEmail = currentUser.getEmail();
            updateSessionUsername();
            navigateToHome();
        }
    }

    private void updateSessionUsername() {
        sessionUsername = dbHelper.getUsernameByEmail(sessionEmail);
        if (sessionUsername == null || sessionUsername.isEmpty()) {
            sessionUsername = dbHelper.getName(sessionEmail); // Try profile name
        }
        if (sessionUsername == null || sessionUsername.isEmpty()) {
            sessionUsername = sessionEmail.split("@")[0]; // Fallback
        }
    }

    private void navigateToHome() {
        startActivity(new Intent(MainActivity.this, HomePage.class));
        finish();
    }
}
