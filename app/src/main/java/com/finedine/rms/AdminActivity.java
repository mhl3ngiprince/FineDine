package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;

public class AdminActivity extends BaseActivity {
    private static final String TAG = "AdminActivity";
    private TextView tvStaffCount, tvReservationCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Setup navigation panel
        setupNavigationPanel("Administration");

        try {
            // Initialize TextViews for counts
            tvStaffCount = findViewById(R.id.tvStaffCount);
            tvReservationCount = findViewById(R.id.tvReservationCount);

            // Set up click listeners for dashboard items
            CardView cardStaff = findViewById(R.id.cardStaff);
            CardView cardMenu = findViewById(R.id.cardMenu);
            CardView backButton = findViewById(R.id.backButton);

            if (cardStaff != null) {
                cardStaff.setOnClickListener(v -> {
                    Toast.makeText(this, "Opening Staff Management", Toast.LENGTH_SHORT).show();
                    try {
                        Intent intent = new Intent(this, StaffManagementActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to Staff Management", e);
                        Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (cardMenu != null) {
                cardMenu.setOnClickListener(v -> {
                    Toast.makeText(this, "Opening Menu Management", Toast.LENGTH_SHORT).show();
                    try {
                        Intent intent = new Intent(this, MenuManagementActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to Menu Management", e);
                        Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    Toast.makeText(this, "Returning to Login", Toast.LENGTH_SHORT).show();
                    try {
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error returning to login", e);
                    }
                });
            }

            // Set welcome message
            String role = getIntent().getStringExtra("user_role");
            if (role == null) role = "admin";

            // Set example data for display
            if (tvStaffCount != null) {
                tvStaffCount.setText("12");
            }

            if (tvReservationCount != null) {
                tvReservationCount.setText("27");
            }

            Toast.makeText(this, "Welcome to Admin Dashboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdminActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}