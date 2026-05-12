package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etRegUsername);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        Button btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvLoginLink).setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Snackbar.make(v, "Please fill in all fields", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Snackbar.make(v, "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Create user in Firebase
            mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Save to local DB for profile info
                            dbHelper.addUserProfile(email, user);
                            MainActivity.sessionEmail = email;
                            
                            Snackbar.make(v, "Registration successful!", Snackbar.LENGTH_SHORT).show();
                            v.postDelayed(() -> {
                                startActivity(new Intent(RegisterActivity.this, HomePage.class));
                                finishAffinity();
                            }, 1000);
                        }
                    } else {
                        Snackbar.make(v, "Registration failed: " + task.getException().getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
        });
    }
}
