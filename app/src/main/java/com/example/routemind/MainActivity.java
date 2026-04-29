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

public class MainActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    ImageView btnGoogleLogin;
    TextView tvResult, tvSignup;
    DBHelper DB;

    // Static variable for session email
    public static String sessionEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvResult   = findViewById(R.id.tvResult);
        tvSignup   = findViewById(R.id.tvSignup);
        DB = new DBHelper(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsername.getText().toString();
                String pass = etPassword.getText().toString();

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
                }
            }
        });

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