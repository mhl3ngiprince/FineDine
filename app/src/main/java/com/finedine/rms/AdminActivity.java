package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.finedine.rms.utils.BackupUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class AdminActivity extends BaseActivity {
    private static final String TAG = "AdminActivity";
    private TextView tvStaffCount, tvReservationCount;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use modern navigation panel
        setupModernNavigationPanel("Administration", R.layout.activity_admin);

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
            Button btnFirebaseTest = findViewById(R.id.btnFirebaseTest);

            if (btnFirebaseTest != null) {
                btnFirebaseTest.setOnClickListener(view -> {
                    Intent intent = new Intent(AdminActivity.this, FirebaseTestActivity.class);
                    startActivity(intent);
                });
            }

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
                    configureEmailSettings();
                });
            }

            if (cardBackup != null) {
                cardBackup.setOnClickListener(v -> {
                    showBackupDialog();
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

    private void showBackupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Backup & Restore");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_backup, null);
        builder.setView(dialogView);

        builder.setPositiveButton("Backup Now", (dialog, which) -> {
            Toast.makeText(this, "Backup initiated", Toast.LENGTH_SHORT).show();
            BackupUtils.backupDatabase(this);
        });

        builder.setNegativeButton("Restore", (dialog, which) -> {
            Toast.makeText(this, "Restore initiated", Toast.LENGTH_SHORT).show();
            BackupUtils.restoreDatabase(this);
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void backupDatabase() {
        try {
            File sd = getExternalFilesDir(null);
            File data = getDatabasePath("rms.db");
            File backup = new File(sd, "rms_backup.db");

            if (data.exists()) {
                FileChannel src = new FileInputStream(data).getChannel();
                FileChannel dst = new FileOutputStream(backup).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                Toast.makeText(this, "Backup successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Database file not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error backing up database", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreDatabase() {
        try {
            File sd = getExternalFilesDir(null);
            File data = getDatabasePath("rms.db");
            File backup = new File(sd, "rms_backup.db");

            if (backup.exists()) {
                FileChannel src = new FileInputStream(backup).getChannel();
                FileChannel dst = new FileOutputStream(data).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                Toast.makeText(this, "Restore successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Backup file not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error restoring database", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Set up the drawer navigation menu
     */
    private void setupDrawerMenu() {
        try {
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);

            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

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

        // Load existing settings if available
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        etGmailEmail.setText(prefs.getEmail());
        etGmailPassword.setText(prefs.getEmailPassword());
        etOutlookEmail.setText(prefs.getSecondaryEmail());
        etOutlookPassword.setText(prefs.getSecondaryEmailPassword());

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Save the email settings
            String gmailEmail = etGmailEmail.getText().toString().trim();
            String gmailPassword = etGmailPassword.getText().toString().trim();
            String outlookEmail = etOutlookEmail.getText().toString().trim();
            String outlookPassword = etOutlookPassword.getText().toString().trim();

            if (!isValidEmail(gmailEmail)) {
                etGmailEmail.setError("Invalid email format");
                return;
            }

            if (gmailPassword.isEmpty()) {
                etGmailPassword.setError("Password required");
                return;
            }

            if (!outlookEmail.isEmpty() && !isValidEmail(outlookEmail)) {
                etOutlookEmail.setError("Invalid email format");
                return;
            }

            if (!outlookEmail.isEmpty() && outlookPassword.isEmpty()) {
                etOutlookPassword.setError("Password required");
                return;
            }

            // Save credentials
            prefs.setEmailCredentials(gmailEmail, gmailPassword);
            if (!outlookEmail.isEmpty() && !outlookPassword.isEmpty()) {
                prefs.setSecondaryEmailCredentials(outlookEmail, outlookPassword);
            }
            EmailSender.setEmailCredentials(this, gmailEmail);
            Toast.makeText(this, "Email settings saved", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.setNeutralButton("Test Connection", (dialog, which) -> {
            String email = etGmailEmail.getText().toString().trim();
            String password = etGmailPassword.getText().toString().trim();

            if (!isValidEmail(email)) {
                etGmailEmail.setError("Invalid email format");
                return;
            }

            if (password.isEmpty()) {
                etGmailPassword.setError("Password required");
                return;
            }

            new Thread(() -> {
                boolean success = EmailSender.testConnection(email, password);
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "Connection successful", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Connection failed. Check credentials and network", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });
        builder.show();
    }

    private boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}