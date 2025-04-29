package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";
    private DrawerLayout drawerLayout;
    private TextView tvStaffCount, tvReservationCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        try {
            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Administration");
            }

            // Set up navigation drawer
            drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);

            // Set up drawer toggle
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

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

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}