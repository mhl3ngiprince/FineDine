package com.finedine.rms;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ManagerDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ManagerDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        try {
            // Set title
            TextView titleTextView = findViewById(R.id.titleTextView);
            if (titleTextView != null) {
                titleTextView.setText("Manager Dashboard");
            }

            // Set welcome message
            String role = getIntent().getStringExtra("user_role");
            if (role == null) role = "manager";

            TextView welcomeTextView = findViewById(R.id.welcomeTextView);
            if (welcomeTextView != null) {
                welcomeTextView.setText("Welcome, " + role + "!");
            }

            // Setup back button
            Button backButton = findViewById(R.id.backButton);
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    finish();
                });
            }

            Toast.makeText(this, "Manager Dashboard Loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ManagerDashboardActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}