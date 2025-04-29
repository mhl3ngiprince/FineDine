package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.finedine.rms.utils.SharedPrefsManager;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected String userRole = "customer"; // Default role
    protected TextView navTitle;
    protected Button logoutButton;
    protected SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "BaseActivity onCreate");
            prefsManager = new SharedPrefsManager(this);
        } catch (Exception e) {
            Log.e(TAG, "Error in BaseActivity onCreate", e);
        }
    }

    protected void setupNavigationPanel(String activityTitle) {
        try {
            navTitle = findViewById(R.id.nav_title);
            if (navTitle != null) {
                navTitle.setText(activityTitle);
            }

            // Setup logout button if it exists
            logoutButton = findViewById(R.id.logoutButton);
            if (logoutButton != null) {
                logoutButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        logout();
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in setupNavigationPanel", e);
        }
    }

    protected void logout() {
        try {
            // Show confirmation toast
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

            // Clear user session
            if (prefsManager != null) {
                prefsManager.clearUserSession();
                prefsManager.setUserLoggedIn(false);
            }

            // Navigate to login screen
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            Toast.makeText(this, "Error logging out. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeAllButtonsVisible() {
        try {
            // Find all navigation buttons
            View dashboardBtn = findViewById(R.id.nav_dashboard);
            View staffBtn = findViewById(R.id.nav_staff);
            View menuBtn = findViewById(R.id.nav_menu);
            View inventoryBtn = findViewById(R.id.nav_inventory);
            View kitchenBtn = findViewById(R.id.nav_kitchen);

            // Make all buttons visible
            if (dashboardBtn != null) dashboardBtn.setVisibility(View.VISIBLE);
            if (staffBtn != null) staffBtn.setVisibility(View.VISIBLE);
            if (menuBtn != null) menuBtn.setVisibility(View.VISIBLE);
            if (inventoryBtn != null) inventoryBtn.setVisibility(View.VISIBLE);
            if (kitchenBtn != null) kitchenBtn.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error making all buttons visible", e);
        }
    }

    private void highlightCurrentActivityButton() {
        try {
            int buttonId = -1;

            Class<?> currentClass = this.getClass();

            if (OrderActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_orders;
            } else if (KitchenActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_kitchen;
            } else if (ReservationActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_reservations;
            } else if (InventoryActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_inventory;
            } else if (MenuManagementActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_menu;
            } else if (StaffManagementActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_staff;
            } else if (ManagerDashboardActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_dashboard;
            }

            if (buttonId != -1) {
                View button = findViewById(buttonId);
                if (button != null) {
                    button.setBackgroundTintList(getResources().getColorStateList(R.color.primary_green));

                    // If it's a MaterialButton, add extra styling
                    if (button instanceof MaterialButton) {
                        ((MaterialButton) button).setStrokeColorResource(R.color.white);
                        ((MaterialButton) button).setStrokeWidth(2);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error highlighting current button", e);
        }
    }

    protected void setupNavButton(int buttonId, final Class<?> destinationClass) {
        try {
            View view = findViewById(buttonId);
            if (view != null) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigateToActivitySafely(destinationClass);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation button for " + destinationClass.getSimpleName(), e);
        }
    }

    /**
     * Safe navigation helper that works around potential issues
     */
    protected void navigateToActivitySafely(Class<?> destinationClass) {
        try {
            // Don't navigate if we're already on this activity
            if (this.getClass().equals(destinationClass)) {
                Log.d(TAG, "Already on " + destinationClass.getSimpleName() + ", skipping navigation");
                return;
            }

            Log.d(TAG, "Navigating to " + destinationClass.getSimpleName());
            Intent intent = new Intent(this, destinationClass);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to " + destinationClass.getSimpleName(), e);
            Toast.makeText(this,
                    "Error navigating: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}