package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    Button btnGoogleLogin; // Kept to avoid layout errors, but logic removed
    TextView tvResult;
    DatabaseHelper dbHelper;

    // Static variable for one-time session email
    public static String sessionEmail = "";

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
                }
            }
        });

        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Google Sign-In is currently disabled. Use admin login.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, HomePage.class);
        startActivity(intent);
        finish();
    }
}