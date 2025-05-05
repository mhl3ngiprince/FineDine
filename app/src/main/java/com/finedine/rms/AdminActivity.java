package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import com.finedine.rms.utils.EmailSender;
import com.finedine.rms.utils.SharedPrefsManager;

public class AdminActivity extends BaseActivity {
    private static final String TAG = "AdminActivity";
    private TextView tvStaffCount, tvReservationCount;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Setup navigation panel
        setupNavigationPanel("Administration");

        // Setup drawer menu
        setupDrawerMenu();

        try {
            // Initialize TextViews for counts
            tvStaffCount = findViewById(R.id.tvStaffCount);
            tvReservationCount = findViewById(R.id.tvReservationCount);

            // Set up click listeners for dashboard items
            CardView cardStaff = findViewById(R.id.cardStaff);
            CardView cardMenu = findViewById(R.id.cardMenu);
            CardView backButton = findViewById(R.id.backButton);
            CardView cardEmailSettings = findViewById(R.id.cardEmailSettings);
            CardView cardBackup = findViewById(R.id.cardBackup);

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

            if (cardEmailSettings != null) {
                cardEmailSettings.setOnClickListener(v -> {
                    onEmailSettingsClicked(v);
                });
            }

            if (cardBackup != null) {
                cardBackup.setOnClickListener(v -> {
                    onBackupClicked(v);
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

    public void onEmailSettingsClicked(View view) {
        configureEmailSettings();
    }

    public void onBackupClicked(View view) {
        Toast.makeText(this, "Backup/Restore functionality coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Set up the drawer navigation menu
     */
    private void setupDrawerMenu() {
        try {
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);

            // Set up toolbar if it exists
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
            }

            // Set up drawer toggle if drawer layout exists
            if (drawerLayout != null && navigationView != null) {
                // Set the drawer menu resource
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.admin_drawer_menu);

                // Set up toggle
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();

                // Set up navigation item selection
                navigationView.setNavigationItemSelectedListener(item -> {
                    int id = item.getItemId();

                    // Handle navigation item clicks
                    if (id == R.id.nav_dashboard) {
                        // Already on admin dashboard
                        drawerLayout.closeDrawers();
                    } else if (id == R.id.nav_staff) {
                        navigateToActivitySafely(StaffManagementActivity.class);
                    } else if (id == R.id.nav_menu) {
                        navigateToActivitySafely(MenuManagementActivity.class);
                    } else if (id == R.id.nav_orders) {
                        navigateToActivitySafely(OrderActivity.class);
                    } else if (id == R.id.nav_reservations) {
                        navigateToActivitySafely(ReservationActivity.class);
                    } else if (id == R.id.nav_inventory) {
                        navigateToActivitySafely(InventoryActivity.class);
                    } else if (id == R.id.nav_logout) {
                        logout();
                    }

                    drawerLayout.closeDrawers();
                    return true;
                });
            } else {
                Log.e(TAG, "Drawer layout or navigation view not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up drawer menu", e);
        }
    }

    private void configureEmailSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Settings");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_settings, null);
        builder.setView(dialogView);

        TextInputEditText etGmailEmail = dialogView.findViewById(R.id.etGmailEmail);
        TextInputEditText etGmailPassword = dialogView.findViewById(R.id.etGmailPassword);
        TextInputEditText etOutlookEmail = dialogView.findViewById(R.id.etOutlookEmail);
        TextInputEditText etOutlookPassword = dialogView.findViewById(R.id.etOutlookPassword);

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Save the email settings
            String gmailEmail = etGmailEmail.getText().toString().trim();
            String gmailPassword = etGmailPassword.getText().toString().trim();
            String outlookEmail = etOutlookEmail.getText().toString().trim();
            String outlookPassword = etOutlookPassword.getText().toString().trim();

            if (!gmailEmail.isEmpty() && !gmailPassword.isEmpty() &&
                    !outlookEmail.isEmpty() && !outlookPassword.isEmpty()) {

                // Save credentials - use gmail as the primary email
                EmailSender.setEmailCredentials(this, gmailEmail);
                Toast.makeText(this, "Email settings saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please fill in all email settings", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}