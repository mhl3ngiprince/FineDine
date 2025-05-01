package com.finedine.rms;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
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

            // Validate that the navigation panel is properly inflated
            validateNavigationPanelInflation();

            // Setup all navigation buttons
            setupAllNavigationButtons();

            // Make all buttons visible (based on user role)
            makeAllButtonsVisible();

            // Highlight the current activity's button
            highlightCurrentActivityButton();

        } catch (Exception e) {
            Log.e(TAG, "Error in setupNavigationPanel", e);
        }
    }

    /**
     * Validates that the navigation panel and its key elements are properly inflated
     */
    private void validateNavigationPanelInflation() {
        View navPanel = findViewById(R.id.navigation_panel);
        if (navPanel == null) {
            Log.e(TAG, "CRITICAL: Navigation panel not found in layout!");
        } else {
            Log.d(TAG, "Navigation panel found in layout");

            // Check for critical buttons
            if (findViewById(R.id.nav_inventory) == null) {
                Log.e(TAG, "Inventory button missing from navigation panel");
            }

            if (findViewById(R.id.nav_menu) == null) {
                Log.e(TAG, "Menu button missing from navigation panel");
            }

            if (findViewById(R.id.nav_reservations) == null) {
                Log.e(TAG, "Reservations button missing from navigation panel");
            }

            if (findViewById(R.id.nav_orders) == null) {
                Log.e(TAG, "Orders button missing from navigation panel");
            }
        }
    }

    private void setupAllNavigationButtons() {
        try {
            // Setup all navigation buttons
            setupNavButton(R.id.nav_orders, OrderActivity.class);
            setupNavButton(R.id.nav_kitchen, KitchenActivity.class);
            setupNavButton(R.id.nav_reservations, ReservationActivity.class);

            // Ensure these specific buttons are properly set up
            setupInventoryButton();
            setupMenuButton();

            setupNavButton(R.id.nav_staff, StaffManagementActivity.class);
            setupNavButton(R.id.nav_dashboard, ManagerDashboardActivity.class);

            Log.d(TAG, "All navigation buttons have been set up");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation buttons", e);
        }
    }

    /**
     * Specifically set up the inventory navigation button with extra logging
     */
    private void setupInventoryButton() {
        try {
            // First verify the activity class exists
            try {
                Class<?> activityClass = Class.forName("com.finedine.rms.InventoryActivity");
                if (!isActivityRegisteredInManifest(activityClass)) {
                    Log.e(TAG, "InventoryActivity not registered in manifest - button will not work");
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "InventoryActivity class not found - buttons will not work", e);
            }

            View inventoryBtn = findViewById(R.id.nav_inventory);
            if (inventoryBtn != null) {
                Log.d(TAG, "Setting up inventory button with special attention");
                inventoryBtn.setOnClickListener(v -> {
                    Log.d(TAG, "Inventory button clicked! Navigating to InventoryActivity");
                    Toast.makeText(this, "Opening Inventory...", Toast.LENGTH_SHORT).show();
                    try {
                        Class<?> activityClass = Class.forName("com.finedine.rms.InventoryActivity");
                        if (isActivityRegisteredInManifest(activityClass)) {
                            Intent intent = new Intent(this, activityClass);
                            intent.putExtra("user_role", prefsManager != null ? prefsManager.getUserRole() : "customer");
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Error: InventoryActivity not registered in manifest", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start InventoryActivity", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Log.e(TAG, "CRITICAL: nav_inventory button not found in the layout!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up inventory button", e);
        }
    }

    /**
     * Specifically set up the menu navigation button with extra logging
     */
    private void setupMenuButton() {
        try {
            // First verify the activity class exists
            try {
                Class<?> activityClass = Class.forName("com.finedine.rms.MenuManagementActivity");
                if (!isActivityRegisteredInManifest(activityClass)) {
                    Log.e(TAG, "MenuManagementActivity not registered in manifest - button will not work");
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "MenuManagementActivity class not found - buttons will not work", e);
            }

            View menuBtn = findViewById(R.id.nav_menu);
            if (menuBtn != null) {
                Log.d(TAG, "Setting up menu button with special attention");
                menuBtn.setOnClickListener(v -> {
                    Log.d(TAG, "Menu button clicked!");
                    Toast.makeText(this, "Opening Menu...", Toast.LENGTH_SHORT).show();
                    try {
                        // Determine which activity to open based on user role
                        String role = prefsManager != null ? prefsManager.getUserRole() : "customer";
                        Class<?> targetClass;

                        if ("customer".equalsIgnoreCase(role)) {
                            // Customers see the menu via OrderActivity
                            targetClass = Class.forName("com.finedine.rms.OrderActivity");
                        } else {
                            // Staff use MenuManagementActivity
                            targetClass = Class.forName("com.finedine.rms.MenuManagementActivity");
                        }

                        if (isActivityRegisteredInManifest(targetClass)) {
                            Intent intent = new Intent(this, targetClass);
                            intent.putExtra("user_role", role);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Error: Activity not registered in manifest", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start menu activity", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Log.e(TAG, "CRITICAL: nav_menu button not found in the layout!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up menu button", e);
        }
    }

    /**
     * Check if an activity class is properly registered in the AndroidManifest
     */
    private boolean isActivityRegisteredInManifest(Class<?> activityClass) {
        try {
            ComponentName componentName = new ComponentName(this, activityClass);
            PackageManager packageManager = getPackageManager();
            packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA);
            // If we get here, the activity info was found
            Log.d(TAG, activityClass.getSimpleName() + " is properly registered in manifest");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, activityClass.getSimpleName() + " is NOT registered in the manifest", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if activity is registered", e);
            return false;
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
            View ordersBtn = findViewById(R.id.nav_orders);
            View reservationsBtn = findViewById(R.id.nav_reservations);
            View dashboardBtn = findViewById(R.id.nav_dashboard);
            View staffBtn = findViewById(R.id.nav_staff);
            View menuBtn = findViewById(R.id.nav_menu);
            View inventoryBtn = findViewById(R.id.nav_inventory);
            View kitchenBtn = findViewById(R.id.nav_kitchen);

            // Make all buttons visible
            if (ordersBtn != null) ordersBtn.setVisibility(View.VISIBLE);
            if (reservationsBtn != null) reservationsBtn.setVisibility(View.VISIBLE);
            if (dashboardBtn != null) dashboardBtn.setVisibility(View.VISIBLE);
            if (staffBtn != null) staffBtn.setVisibility(View.VISIBLE);
            if (menuBtn != null) menuBtn.setVisibility(View.VISIBLE);
            if (inventoryBtn != null) inventoryBtn.setVisibility(View.VISIBLE);
            if (kitchenBtn != null) kitchenBtn.setVisibility(View.VISIBLE);

            // Adjust button visibility based on user role
            adjustButtonVisibilityByRole();
        } catch (Exception e) {
            Log.e(TAG, "Error making all buttons visible", e);
        }
    }

    private void adjustButtonVisibilityByRole() {
        try {
            // Get current user role
            if (prefsManager == null) {
                prefsManager = new SharedPrefsManager(this);
            }
            String role = prefsManager.getUserRole();
            if (role == null || role.isEmpty()) {
                // Try to get role from intent
                role = getIntent().getStringExtra("user_role");
                if (role == null || role.isEmpty()) {
                    role = "customer"; // Default to customer if no role is found
                }
            }

            Log.d(TAG, "Adjusting button visibility for role: " + role);

            // Find all navigation buttons
            View ordersBtn = findViewById(R.id.nav_orders);
            View reservationsBtn = findViewById(R.id.nav_reservations);
            View dashboardBtn = findViewById(R.id.nav_dashboard);
            View staffBtn = findViewById(R.id.nav_staff);
            View menuBtn = findViewById(R.id.nav_menu);
            View inventoryBtn = findViewById(R.id.nav_inventory);
            View kitchenBtn = findViewById(R.id.nav_kitchen);

            // Set visibility based on role
            switch (role.toLowerCase()) {
                case "admin":
                    // Admin can access everything
                    break;

                case "manager":
                    // Manager can access almost everything
                    break;

                case "chef":
                    // Chef can only access kitchen, orders, inventory and menu
                    if (dashboardBtn != null) dashboardBtn.setVisibility(View.GONE);
                    if (staffBtn != null) staffBtn.setVisibility(View.GONE);
                    break;

                case "waiter":
                    // Waiter can access orders and reservations
                    if (dashboardBtn != null) dashboardBtn.setVisibility(View.GONE);
                    if (staffBtn != null) staffBtn.setVisibility(View.GONE);
                    if (menuBtn != null) menuBtn.setVisibility(View.GONE);
                    if (inventoryBtn != null) inventoryBtn.setVisibility(View.GONE);
                    if (kitchenBtn != null) kitchenBtn.setVisibility(View.GONE);
                    break;

                case "customer":
                default:
                    // Customer can only access reservations and menu
                    if (ordersBtn != null) ordersBtn.setVisibility(View.GONE);
                    if (dashboardBtn != null) dashboardBtn.setVisibility(View.GONE);
                    if (staffBtn != null) staffBtn.setVisibility(View.GONE);
                    if (menuBtn != null) menuBtn.setVisibility(View.VISIBLE);
                    if (inventoryBtn != null) inventoryBtn.setVisibility(View.GONE);
                    if (kitchenBtn != null) kitchenBtn.setVisibility(View.GONE);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting button visibility by role", e);
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
                Log.d(TAG, "Setting up navigation button for " + destinationClass.getSimpleName() + " with ID " + getResourceName(buttonId));
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Navigation button clicked for " + destinationClass.getSimpleName());
                        navigateToActivitySafely(destinationClass);
                    }
                });
            } else {
                Log.w(TAG, "Button with ID " + getResourceName(buttonId) + " not found in the layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation button for " + destinationClass.getSimpleName(), e);
        }
    }

    private void navigateBasedOnRole(String role) {
        try {
            Log.d(TAG, "Navigating to role: " + role);
            Intent intent;

            if (role == null || role.isEmpty()) {
                role = "customer";
            }

            switch (role.toLowerCase()) {
                case "admin":
                    intent = new Intent(this, AdminActivity.class);
                    break;
                case "manager":
                    intent = new Intent(this, ManagerDashboardActivity.class);
                    break;
                case "chef":
                    intent = new Intent(this, KitchenActivity.class);
                    break;
                case "waiter":
                    intent = new Intent(this, OrderActivity.class);
                    break;
                case "customer":
                default:
                    // Change to OrderActivity (menu) for customers
                    intent = new Intent(this, OrderActivity.class);
                    break;
            }

            // Pass role in intent
            intent.putExtra("user_role", role);

            Log.d(TAG, "Role-based navigation: " + role + " → " + intent.getComponent().getClassName());
            // Start activity
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error navigating based on role", e);
        }
    }

    // Helper method to get resource name from ID for better logging
    private String getResourceName(int resId) {
        try {
            return getResources().getResourceName(resId);
        } catch (Exception e) {
            return String.valueOf(resId);
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

            // Verify the destination class exists and is accessible
            try {
                Class.forName(destinationClass.getName());
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Destination class not found: " + destinationClass.getName(), e);
                Toast.makeText(this, "Navigation error: Activity not found. Check manifest registration.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the activity is declared in the manifest
            try {
                // Try creating an intent to verify the activity is properly declared
                Intent intentCheck = new Intent(this, destinationClass);
                if (intentCheck.resolveActivity(getPackageManager()) == null) {
                    Log.e(TAG, "Activity not declared in AndroidManifest.xml: " + destinationClass.getName());
                    Toast.makeText(this,
                            "Activity " + destinationClass.getSimpleName() + " not registered in manifest",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking activity registration: " + destinationClass.getName(), e);
            }

            Log.d(TAG, "Navigating to " + destinationClass.getSimpleName());
            Intent intent = new Intent(this, destinationClass);

            // Pass along user role
            if (prefsManager != null) {
                String role = prefsManager.getUserRole();
                if (role != null && !role.isEmpty()) {
                    intent.putExtra("user_role", role);
                    Log.d(TAG, "Added user_role to intent: " + role);
                }
            }

            // Start the activity
            startActivity(intent);

            // Show toast to indicate navigation
            String destinationName = destinationClass.getSimpleName().replace("Activity", "");
            Toast.makeText(this, "Opening " + destinationName, Toast.LENGTH_SHORT).show();

        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Activity not found: " + destinationClass.getSimpleName(), e);
            Toast.makeText(this,
                    "Error: Activity " + destinationClass.getSimpleName() + " not found. Check manifest registration.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to " + destinationClass.getSimpleName(), e);
            Toast.makeText(this,
                    "Navigation error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}