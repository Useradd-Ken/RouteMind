package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
<<<<<<< Updated upstream
    ImageView btnGoogleLogin;
    TextView tvResult, tvSignup;
    DBHelper DB;

    // Static variable for session email
=======
    TextView tvResult, tvSignup;
    FirebaseAuth mAuth;

>>>>>>> Stashed changes
    public static String sessionEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

<<<<<<< Updated upstream
=======
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            // Graceful failure if google-services.json is missing
        }
        
        if (mAuth != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                sessionEmail = currentUser.getEmail();
                navigateToHome();
            }
        }

>>>>>>> Stashed changes
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        tvResult   = findViewById(R.id.tvResult);
        tvSignup   = findViewById(R.id.tvSignup);
<<<<<<< Updated upstream
        DB = new DBHelper(this);
=======
>>>>>>> Stashed changes

        btnLogin.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

<<<<<<< Updated upstream
                if (user.equals("") || pass.equals("")) {
                    Toast.makeText(MainActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
                } else {
                    // Admin Access Check
                    if (user.equals("admin") && pass.equals("admin123")) {
                        Toast.makeText(MainActivity.this, "Welcome Admin", Toast.LENGTH_SHORT).show();
                        sessionEmail = "admin";
                        Intent intent = new Intent(getApplicationContext(), AdminPanelActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    // Regular User Check
                    Boolean checkuserpass = DB.checkUsernamePassword(user, pass);
                    if (checkuserpass) {
                        Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        sessionEmail = user;
                        Intent intent = new Intent(getApplicationContext(), HomePage.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    }
=======
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            } else {
                if (email.equals("admin@routemind.com") && pass.equals("admin123")) {
                    sessionEmail = "admin";
                    navigateToHome();
                    return;
                }

                if (mAuth != null) {
                    loginUser(email, pass);
                } else {
                    Toast.makeText(MainActivity.this, "Firebase not configured. Use admin login.", Toast.LENGTH_LONG).show();
>>>>>>> Stashed changes
                }
            }
        });

<<<<<<< Updated upstream
        tvSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionEmail = "google_user@gmail.com";
                Intent intent = new Intent(MainActivity.this, HomePage.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
=======
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
>>>>>>> Stashed changes
