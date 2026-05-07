package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    public static String sessionEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        View btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString();
            String pass = etPassword.getText().toString();

            if (user.contains("@") && pass.equals("1234")) {
                sessionEmail = user;
                navigateToHome();
            } else if (user.equals("admin") && pass.equals("1234")) {
                sessionEmail = "admin@routemind.com";
                navigateToHome();
            } else {
                Snackbar.make(v, "The credentials you entered don't match. Please try again.", Snackbar.LENGTH_LONG).show();
            }
        });

        btnGoogleLogin.setOnClickListener(v -> {
            sessionEmail = "google_user@gmail.com";
            navigateToHome();
        });
    }

    private void navigateToHome() {
        startActivity(new Intent(MainActivity.this, HomePage.class));
        finish();
    }
}
