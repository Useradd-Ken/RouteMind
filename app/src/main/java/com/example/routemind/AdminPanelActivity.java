package com.example.routemind;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class AdminPanelActivity extends AppCompatActivity {

    TabLayout tabLayout;
    TextView tvContent;
    Button btnLogout;
    DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        tabLayout = findViewById(R.id.admin_tabs);
        tvContent = findViewById(R.id.tv_admin_content);
        btnLogout = findViewById(R.id.btn_logout);
        DB = new DBHelper(this);

        showUsers();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showUsers();
                        break;
                    case 1:
                        showReviews();
                        break;
                    case 2:
                        showDestinations();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnLogout.setOnClickListener(v -> {
            MainActivity.sessionEmail = "";
            Intent intent = new Intent(AdminPanelActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showUsers() {
        Cursor cursor = DB.getAllUsers();
        StringBuilder builder = new StringBuilder();
        builder.append("USERS IN DATABASE:\n\n");
        if (cursor.getCount() == 0) {
            builder.append("No users found.");
        } else {
            while (cursor.moveToNext()) {
                builder.append("Username: ").append(cursor.getString(0)).append("\n");
                builder.append("Email: ").append(cursor.getString(1)).append("\n");
                builder.append("Password: ").append(cursor.getString(2)).append("\n");
                builder.append("--------------------\n");
            }
        }
        cursor.close();
        tvContent.setText(builder.toString());
    }

    private void showReviews() {
        Cursor cursor = DB.getAllReviews();
        StringBuilder builder = new StringBuilder();
        builder.append("REVIEWS IN DATABASE:\n\n");
        if (cursor.getCount() == 0) {
            builder.append("No reviews found.");
        } else {
            while (cursor.moveToNext()) {
                builder.append("User: ").append(cursor.getString(1)).append("\n");
                builder.append("Rating: ").append(cursor.getFloat(2)).append(" stars\n");
                builder.append("Review: ").append(cursor.getString(3)).append("\n");
                builder.append("Time: ").append(cursor.getString(4)).append("\n");
                builder.append("--------------------\n");
            }
        }
        cursor.close();
        tvContent.setText(builder.toString());
    }

    private void showDestinations() {
        List<String> destinations = DB.getAllDestinations();
        StringBuilder builder = new StringBuilder();
        builder.append("DESTINATIONS IN DATABASE:\n\n");
        for (String dest : destinations) {
            builder.append("- ").append(dest).append("\n");
        }
        tvContent.setText(builder.toString());
    }
}