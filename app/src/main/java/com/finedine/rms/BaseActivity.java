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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.button.MaterialButton;
import com.finedine.rms.utils.SharedPrefsManager;

import java.lang.ref.WeakReference;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected String userRole = "customer"; // Default role
    protected TextView navTitle;
    protected Button logoutButton;
    protected Button logoutButtonAlt;
    protected SharedPrefsManager prefsManager;
    private boolean isNavigationSetup = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "BaseActivity onCreate: " + getClass().getSimpleName());

            // Initialize SharedPrefsManager only once
            if (prefsManager == null) {
                prefsManager = new SharedPrefsManager(getApplicationContext());
            }

            // Get user role from SharedPrefs or intent
            initUserRole();
        } catch (Exception e) {
            Log.e(TAG, "Error in BaseActivity onCreate", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BaseActivity onDestroy: " + getClass().getSimpleName());
        // Clean up resources
        cleanupReferences();
    }

    private void cleanupReferences() {
        navTitle = null;
        logoutButton = null;
        logoutButtonAlt = null;
        // Don't set prefsManager to null as it uses application context
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "BaseActivity onPause: " + getClass().getSimpleName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "BaseActivity onResume: " + getClass().getSimpleName());

        // Refresh navigation if already set up
        if (isNavigationSetup) {
            refreshNavigationBasedOnRole();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current user role
        outState.putString("current_user_role", userRole);
        Log.d(TAG, "BaseActivity onSaveInstanceState: " + getClass().getSimpleName());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore user role
        if (savedInstanceState.containsKey("current_user_role")) {
            userRole = savedInstanceState.getString("current_user_role", "customer");
            Log.d(TAG, "Restored user role: " + userRole);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "BaseActivity onConfigurationChanged: " + getClass().getSimpleName());
    }

    private void initUserRole() {
        // Get role from SharedPrefsManager first
        if (prefsManager != null) {
            String savedRole = prefsManager.getUserRole();
            if (savedRole != null && !savedRole.isEmpty()) {
                userRole = savedRole;
                Log.d(TAG, "Got role from SharedPrefs: " + userRole);
                return;
            }
        }

        // Fall back to intent extra
        if (getIntent() != null && getIntent().hasExtra("user_role")) {
            String intentRole = getIntent().getStringExtra("user_role");
            if (intentRole != null && !intentRole.isEmpty()) {
                userRole = intentRole;
                Log.d(TAG, "Got role from Intent: " + userRole);
                // Save this to SharedPrefs for consistency
                if (prefsManager != null) {
                    prefsManager.setUserRole(userRole);
                }
            }
        }

        Log.d(TAG, "Using role: " + userRole);
    }

    protected void setupNavigationPanel(String activityTitle) {
        try {
            // Find navigation title
            navTitle = findViewById(R.id.nav_title);
            if (navTitle != null) {
                navTitle.setText(activityTitle);
            } else {
                Log.w(TAG, "Navigation title view not found");
            }

            // Setup logout buttons
            setupLogoutButtons();

            // Validate the navigation panel inflation
            if (!validateNavigationPanelInflation()) {
                Log.e(TAG, "Navigation panel validation failed");
                return;
            }

            // Setup all navigation buttons
            setupAllNavigationButtons();

            // Make all buttons visible based on user role
            setupButtonVisibilityByRole();

            // Highlight the current activity's button
            highlightCurrentActivityButton();

            // Mark navigation as setup
            isNavigationSetup = true;

        } catch (Exception e) {
            Log.e(TAG, "Error in setupNavigationPanel", e);
            Toast.makeText(this, "Navigation setup error", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLogoutButtons() {
        // Setup primary logout button
        logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            Log.d(TAG, "Found logout button, setting up click listener");
            logoutButton.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked");
                logout();
            });
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "Logout button not found in layout");
        }

        // Setup alternate logout button
        logoutButtonAlt = findViewById(R.id.logoutButtonAlt);
        if (logoutButtonAlt != null) {
            Log.d(TAG, "Found alternate logout button, setting up click listener");
            logoutButtonAlt.setOnClickListener(v -> {
                Log.d(TAG, "Alternate logout button clicked");
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                logout();
            });
            logoutButtonAlt.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Validates that the navigation panel and its key elements are properly inflated
     * @return true if panel is valid, false otherwise
     */
    private boolean validateNavigationPanelInflation() {
        View navPanel = findViewById(R.id.navigation_panel);
        if (navPanel == null) {
            Log.e(TAG, "CRITICAL: Navigation panel not found in layout!");
            return false;
        }

        Log.d(TAG, "Navigation panel found in layout");

        boolean valid = true;

        // Check for critical buttons and log issues
        if (findViewById(R.id.nav_inventory) == null) {
            Log.e(TAG, "Inventory button missing from navigation panel");
            valid = false;
        }

        if (findViewById(R.id.nav_menu) == null) {
            Log.e(TAG, "Menu button missing from navigation panel");
            valid = false;
        }

        if (findViewById(R.id.nav_reservations) == null) {
            Log.e(TAG, "Reservations button missing from navigation panel");
            valid = false;
        }

        if (findViewById(R.id.nav_orders) == null) {
            Log.e(TAG, "Orders button missing from navigation panel");
            valid = false;
        }

        return valid;
    }

    private void setupAllNavigationButtons() {
        try {
            // Setup standard navigation buttons using a common pattern
            setupNavButton(R.id.nav_orders, OrderActivity.class);
            setupNavButton(R.id.nav_kitchen, KitchenActivity.class);
            setupNavButton(R.id.nav_reservations, ReservationActivity.class);
            setupNavButton(R.id.nav_staff, StaffManagementActivity.class);
            setupNavButton(R.id.nav_dashboard, ManagerDashboardActivity.class);

            // Setup special case buttons
            setupInventoryButton();
            setupMenuButton();

            Log.d(TAG, "All navigation buttons have been set up");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation buttons", e);
        }
    }

    /**
     * Specifically set up the inventory navigation button with robust error handling
     */
    private void setupInventoryButton() {
        try {
            // First verify the activity class exists
            Class<?> inventoryClass = null;
            try {
                inventoryClass = Class.forName("com.finedine.rms.InventoryActivity");
                if (!isActivityRegisteredInManifest(inventoryClass)) {
                    Log.e(TAG, "InventoryActivity not registered in manifest - button will not work");
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "InventoryActivity class not found - button will not work", e);
            }

            // Final class reference for lambda
            final Class<?> finalInventoryClass = inventoryClass;

            View inventoryBtn = findViewById(R.id.nav_inventory);
            if (inventoryBtn != null) {
                inventoryBtn.setOnClickListener(v -> {
                    Log.d(TAG, "Inventory button clicked");
                    Toast.makeText(this, "Opening Inventory...", Toast.LENGTH_SHORT).show();
                    if (finalInventoryClass != null) {
                        navigateToActivitySafely(finalInventoryClass);
                    } else {
                        Toast.makeText(this, "Error: Inventory activity not available", Toast.LENGTH_SHORT).show();
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
     * Specifically set up the menu navigation button with role-based behavior
     */
    private void setupMenuButton() {
        try {
            View menuBtn = findViewById(R.id.nav_menu);
            if (menuBtn != null) {
                menuBtn.setOnClickListener(v -> {
                    Log.d(TAG, "Menu button clicked");
                    Toast.makeText(this, "Opening Menu...", Toast.LENGTH_SHORT).show();
                    try {
                        // Determine which activity to open based on user role
                        String role = (prefsManager != null) ? prefsManager.getUserRole() : userRole;

                        // Check if we're already on MenuItemDetailActivity
                        if (this instanceof MenuItemDetailActivity) {
                            // Go back to appropriate menu listing activity instead of trying to navigate to itself
                            if ("customer".equalsIgnoreCase(role)) {
                                navigateToActivitySafely(OrderActivity.class);
                            } else {
                                navigateToActivitySafely(MenuManagementActivity.class);
                            }
                            return;
                        }

                        if ("customer".equalsIgnoreCase(role)) {
                            // Customers see the menu via OrderActivity
                            navigateToActivitySafely(OrderActivity.class);
                        } else {
                            // Staff use MenuManagementActivity
                            try {
                                Class<?> menuClass = Class.forName("com.finedine.rms.MenuManagementActivity");
                                navigateToActivitySafely(menuClass);
                            } catch (ClassNotFoundException e) {
                                Log.e(TAG, "MenuManagementActivity class not found", e);
                                Toast.makeText(this, "Menu management not available", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start menu activity", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            Log.d(TAG, activityClass.getSimpleName() + " is registered in manifest");
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
            Log.d(TAG, "Logout method called");

            // First try the clean logout via LogoutActivity
            try {
                Intent logoutIntent = new Intent(this, LogoutActivity.class);
                logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logoutIntent);
                finish();
                return; // If successful, we're done
            } catch (Exception e) {
                Log.e(TAG, "Error starting LogoutActivity, falling back to manual logout", e);
            }

            // Manual logout as fallback
            performManualLogout();

        } catch (Exception e) {
            Log.e(TAG, "Critical failure during logout", e);
            // Last resort - try to force app restart
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        }
    }

    private void performManualLogout() {
        try {
            // Clear user session
            if (prefsManager != null) {
                Log.d(TAG, "Clearing user session");
                prefsManager.clearUserSession();
                prefsManager.setUserLoggedIn(false);
            } else {
                Log.e(TAG, "prefsManager is null, creating new instance");
                prefsManager = new SharedPrefsManager(getApplicationContext());
                prefsManager.clearUserSession();
                prefsManager.setUserLoggedIn(false);
            }

            // Double check the session was cleared
            if (prefsManager != null && prefsManager.isUserLoggedIn()) {
                Log.e(TAG, "Failed to clear user session, forcing manual clear");
                // Force clear using SharedPreferences directly
                getSharedPreferences("FineDinePrefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit();
            }

            // Navigate to login screen directly
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Manual logout failed", e);
            finishAffinity(); // Last resort - close the app
        }
    }

    private void setupButtonVisibilityByRole() {
        try {
            // Make all buttons visible by default
            makeAllButtonsVisible();

            // Then adjust visibility based on user role
            adjustButtonVisibilityByRole();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up button visibility", e);
        }
    }

    private void makeAllButtonsVisible() {
        Log.d(TAG, "Making all navigation buttons visible");
        setNavigationButtonVisibility(R.id.nav_orders, View.VISIBLE);
        setNavigationButtonVisibility(R.id.nav_reservations, View.VISIBLE);
        setNavigationButtonVisibility(R.id.nav_dashboard, View.VISIBLE);
        setNavigationButtonVisibility(R.id.nav_staff, View.VISIBLE);
        setNavigationButtonVisibility(R.id.nav_menu, View.VISIBLE);
        setNavigationButtonVisibility(R.id.nav_inventory, View.VISIBLE);
        setNavigationButtonVisibility(R.id.nav_kitchen, View.VISIBLE);
    }

    private void setNavigationButtonVisibility(int buttonId, int visibility) {
        View button = findViewById(buttonId);
        if (button != null) {
            button.setVisibility(visibility);
        }
    }

    private void adjustButtonVisibilityByRole() {
        try {
            // Get current user role
            String role = userRole;
            Log.d(TAG, "Adjusting button visibility for role: " + role);

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
                    setNavigationButtonVisibility(R.id.nav_dashboard, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_staff, View.GONE);
                    break;

                case "waiter":
                    // Waiter can access orders and reservations
                    setNavigationButtonVisibility(R.id.nav_dashboard, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_staff, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_inventory, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_kitchen, View.GONE);
                    break;

                case "customer":
                default:
                    // Customer can only access reservations and menu
                    setNavigationButtonVisibility(R.id.nav_orders, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_dashboard, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_staff, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_inventory, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_kitchen, View.GONE);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting button visibility by role", e);
        }
    }

    /**
     * Refresh the navigation panel when role changes
     */
    protected void refreshNavigationBasedOnRole() {
        if (isNavigationSetup) {
            setupButtonVisibilityByRole();
            highlightCurrentActivityButton();
        }
    }

    private void highlightCurrentActivityButton() {
        try {
            int buttonId = -1;
            Class<?> currentClass = this.getClass();

            // Reset all button styles first
            resetAllButtonStyles();

            // Determine which button to highlight
            if (OrderActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_orders;
            } else if (KitchenActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_kitchen;
            } else if (ReservationActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_reservations;
            } else if (InventoryActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_inventory;
            } else if (MenuManagementActivity.class.isAssignableFrom(currentClass) ||
                    (OrderActivity.class.isAssignableFrom(currentClass) && "customer".equals(userRole))) {
                buttonId = R.id.nav_menu;
            } else if (StaffManagementActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_staff;
            } else if (ManagerDashboardActivity.class.isAssignableFrom(currentClass)) {
                buttonId = R.id.nav_dashboard;
            }

            // Highlight the selected button
            if (buttonId != -1) {
                highlightButton(buttonId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error highlighting current button", e);
        }
    }

    private void resetAllButtonStyles() {
        resetButtonStyle(R.id.nav_orders);
        resetButtonStyle(R.id.nav_kitchen);
        resetButtonStyle(R.id.nav_reservations);
        resetButtonStyle(R.id.nav_inventory);
        resetButtonStyle(R.id.nav_menu);
        resetButtonStyle(R.id.nav_staff);
        resetButtonStyle(R.id.nav_dashboard);
    }

    private void resetButtonStyle(int buttonId) {
        View button = findViewById(buttonId);
        if (button != null && button instanceof MaterialButton) {
            ((MaterialButton) button).setBackgroundTintList(
                    getResources().getColorStateList(R.color.med_blue_dark));
            ((MaterialButton) button).setStrokeColorResource(R.color.text_white);
            ((MaterialButton) button).setStrokeWidth(1);
        }
    }

    private void highlightButton(int buttonId) {
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

    protected void setupNavButton(int buttonId, final Class<?> destinationClass) {
        try {
            View view = findViewById(buttonId);
            if (view != null) {
                Log.d(TAG, "Setting up navigation button for " + destinationClass.getSimpleName());

                // Use a weak reference to avoid memory leaks
                WeakReference<BaseActivity> weakActivity = new WeakReference<>(this);

                view.setOnClickListener(v -> {
                    BaseActivity activity = weakActivity.get();
                    if (activity != null && !activity.isFinishing() &&
                            activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        Log.d(TAG, "Navigation button clicked for " + destinationClass.getSimpleName());
                        activity.navigateToActivitySafely(destinationClass);
                    }
                });
            } else {
                Log.w(TAG, "Button with ID " + getResourceName(buttonId) + " not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation button for " + destinationClass.getSimpleName(), e);
        }
    }

    /**
     * Navigate to a specific activity based on user role
     */
    protected void navigateBasedOnRole() {
        try {
            Log.d(TAG, "Navigating based on role: " + userRole);
            Intent intent;

            switch (userRole.toLowerCase()) {
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
                    intent = new Intent(this, OrderActivity.class);
                    break;
            }

            // Pass role in intent
            intent.putExtra("user_role", userRole);
            Log.d(TAG, "Role-based navigation to " + intent.getComponent().getClassName());

            // Start activity
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating based on role", e);
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
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
     * Safe navigation helper with comprehensive error handling
     */
    protected void navigateToActivitySafely(Class<?> destinationClass) {
        try {
            // Don't navigate if we're already on this activity
            if (this.getClass().equals(destinationClass)) {
                Log.d(TAG, "Already on " + destinationClass.getSimpleName() + ", skipping navigation");
                return;
            }

            // Check if activity is declared in manifest
            if (!isActivityRegisteredInManifest(destinationClass)) {
                Log.e(TAG, "Activity not registered in manifest: " + destinationClass.getName());
                Toast.makeText(this,
                        "Navigation error: " + destinationClass.getSimpleName() + " not registered",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Create intent and add user role
            Intent intent = new Intent(this, destinationClass);
            intent.putExtra("user_role", userRole);

            // Start the activity
            Log.d(TAG, "Navigating to " + destinationClass.getSimpleName());
            startActivity(intent);

            // Show brief toast confirmation
            String destinationName = destinationClass.getSimpleName().replace("Activity", "");
            Toast.makeText(this, "Opening " + destinationName, Toast.LENGTH_SHORT).show();

        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Activity not found: " + destinationClass.getSimpleName(), e);
            Toast.makeText(this, "Error: Activity not found", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to " + destinationClass.getSimpleName(), e);
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
        }
    }
}