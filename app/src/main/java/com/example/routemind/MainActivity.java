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
    public static String sessionEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

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
            navigateToHome();
        });

        findViewById(R.id.tvSignup).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            sessionEmail = currentUser.getEmail();
            navigateToHome();
        }
    }

    private void navigateToHome() {
        startActivity(new Intent(MainActivity.this, HomePage.class));
        finish();
    }
}
