package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    EditText username, email, password, repassword;
    Button signup;
    TextView login;
    DatabaseHelper dbHelper;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        username = findViewById(R.id.etUsername);
        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        repassword = findViewById(R.id.etConfirmPassword);
        signup = findViewById(R.id.btnSignup);
        login = findViewById(R.id.tvLogin);
        dbHelper = new DatabaseHelper(this);
        
        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            // Firebase not configured
        }

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = username.getText().toString().trim();
                String mail = email.getText().toString().trim();
                String pass = password.getText().toString().trim();
                String repass = repassword.getText().toString().trim();

                if (user.equals("") || mail.equals("") || pass.equals("") || repass.equals("")) {
                    Toast.makeText(SignUpActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
                } else if (!pass.equals(repass)) {
                    Toast.makeText(SignUpActivity.this, "Passwords not matching", Toast.LENGTH_SHORT).show();
                } else {
                    if (mAuth != null) {
                        // Firebase Auth Signup
                        mAuth.createUserWithEmailAndPassword(mail, pass)
                                .addOnCompleteListener(SignUpActivity.this, task -> {
                                    if (task.isSuccessful()) {
                                        dbHelper.insertUserData(user, mail, pass);
                                        dbHelper.addUserProfile(mail, user);
                                        Toast.makeText(SignUpActivity.this, "Registered successfully", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(SignUpActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Local fallback
                        if (!dbHelper.checkUsername(user)) {
                            if (dbHelper.insertUserData(user, mail, pass)) {
                                dbHelper.addUserProfile(mail, user);
                                Toast.makeText(SignUpActivity.this, "Registered successfully (Local)", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(SignUpActivity.this, "Registration failed locally", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "User already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}