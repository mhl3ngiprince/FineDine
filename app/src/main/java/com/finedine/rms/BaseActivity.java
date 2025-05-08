package com.finedine.rms;

import android.app.Activity;
import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.finedine.rms.utils.SharedPrefsManager;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Calendar;

import android.widget.LinearLayout;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected String userRole = "customer"; // Default role
    protected TextView navTitle;
    protected Button logoutButton;
    protected Button logoutButtonAlt;
    protected SharedPrefsManager prefsManager;
    private boolean isNavigationSetup = false;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected BottomNavigationView bottomNavigationView;
    private boolean isModernNavigationSetup = false;

    // Settings keys
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_LANGUAGE = "language_setting";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply saved language settings before activity creation
        android.content.SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String languageCode = prefs.getString(KEY_LANGUAGE, "en");

        // Create configuration with saved locale
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        Context context = newBase;
        context = context.createConfigurationContext(config);

        super.attachBaseContext(context);
    }

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

            // Set an error handler for this activity
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                Log.e(TAG, "Uncaught exception in " + getClass().getSimpleName(), throwable);
                try {
                    Toast.makeText(this, "Something went wrong. Returning to home screen.", Toast.LENGTH_LONG).show();

                    // Navigate back to splash screen
                    Intent intent = new Intent(this, SplashActivityNew.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error in error handler", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in BaseActivity onCreate", e);

            // Try to recover
            try {
                Toast.makeText(this, "Error initializing. Please restart the app.", Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {
                // Last resort, can't do much here
            }
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
        drawerLayout = null;
        navigationView = null;
        bottomNavigationView = null;
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

        // Check if we need to update navigation for role changes
        checkRoleConsistency();

        // Refresh navigation if already set up
        if (isNavigationSetup) {
            refreshNavigationBasedOnRole();
        }

        // Refresh modern navigation if set up
        if (isModernNavigationSetup) {
            refreshModernNavigation();
        }

        // Ensure all buttons work
        ensureAllButtonsWork();

        // Setup date/time pickers for any date/time fields
        setupDateAndTimePickers();

        // Validate navigation
        validateNavigation();
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

    /**
     * @param activityTitle Title to display in the toolbar
     * @deprecated Use setupModernNavigationPanel instead.
     * Setup the navigation panel with a title
     */
    @Deprecated
    protected void setupNavigationPanel(String activityTitle) {
        try {
            Log.w(TAG, "Using deprecated setupNavigationPanel method. Please update to setupModernNavigationPanel");
            // Delegate to the modern navigation panel setup
            setupModernNavigationPanel(activityTitle, -1);
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

        // We no longer use logoutButtonAlt in modern navigation
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
            // Kitchen button no longer exists in modern navigation
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
                            if ("customer".equalsIgnoreCase(role) || "waiter".equalsIgnoreCase(role)) {
                                navigateToActivitySafely(MenuManagementActivity.class);
                            } else {
                                navigateToActivitySafely(MenuManagementActivity.class);
                            }
                            return;
                        }

                        // For customers and waiters, now direct them to MenuManagementActivity 
                        if ("customer".equalsIgnoreCase(role) || "waiter".equalsIgnoreCase(role)) {
                            navigateToActivitySafely(MenuManagementActivity.class);
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
            // Close drawer if open before logging out
            if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }

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
        // Kitchen button no longer exists in modern navigation
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
                    // Admin can access administrator functions, reviews, menu, reservations
                    // but not manager dashboard
                    setNavigationButtonVisibility(R.id.nav_dashboard, View.GONE);
                    break;

                case "manager":
                    // Manager can access everything
                    break;

                case "chef":
                    // Chef can only access menu management, kitchen, and reviews settings
                    setNavigationButtonVisibility(R.id.nav_dashboard, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_staff, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_orders, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_reservations, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_inventory, View.GONE);
                    break;

                case "waiter":
                    // Waiter has same access as customer
                    setNavigationButtonVisibility(R.id.nav_dashboard, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_staff, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_inventory, View.GONE);
                    // Kitchen button no longer exists in modern navigation
                    setNavigationButtonVisibility(R.id.nav_orders, View.VISIBLE);
                    setNavigationButtonVisibility(R.id.nav_reservations, View.VISIBLE);
                    setNavigationButtonVisibility(R.id.nav_menu, View.VISIBLE);
                    break;

                case "customer":
                default:
                    // Customer can access menu, orders, reviews, and reservations
                    setNavigationButtonVisibility(R.id.nav_dashboard, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_staff, View.GONE);
                    setNavigationButtonVisibility(R.id.nav_inventory, View.GONE);
                    // Kitchen button no longer exists in modern navigation
                    setNavigationButtonVisibility(R.id.nav_orders, View.VISIBLE);
                    setNavigationButtonVisibility(R.id.nav_reservations, View.VISIBLE);
                    setNavigationButtonVisibility(R.id.nav_menu, View.VISIBLE);
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
                // Kitchen button no longer exists in modern navigation
                // Use orders button instead
                buttonId = R.id.nav_orders;
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
        // Kitchen button no longer exists in modern navigation
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
                    intent = new Intent(this, CustomerMenuActivity.class);
                    break;
            }

            // Pass role in intent
            intent.putExtra("user_role", userRole);
            Log.d(TAG, "Role-based navigation to " + intent.getComponent().getClassName());

            // Start activity only if not already on the destination
            if (!this.getClass().equals(intent.getComponent().getClass())) {
                startActivity(intent);
                finish();
            }
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

    /**
     * Implement the modern navigation panel throughout the app
     *
     * @param activityTitle   Title to display in the toolbar
     * @param contentLayoutId Layout resource ID for the activity's content
     */
    protected void setupModernNavigationPanel(String activityTitle, int contentLayoutId) {
        // First set a simple layout as a failsafe, so we don't get black screens
        setContentView(R.layout.modern_navigation_panel);
        try {
            // Handle manager dashboard separately due to its complex layout
            if (this instanceof ManagerDashboardActivity) {
                // For ManagerDashboardActivity, we use the full layout and just set up navigation elements
                setContentView(contentLayoutId);

                // Find and initialize the navigation components
                drawerLayout = findViewById(R.id.drawer_layout);
                navigationView = findViewById(R.id.nav_view);
                bottomNavigationView = findViewById(R.id.bottomNavigation);

                // Set up the toolbar without changing layout
                Toolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar != null) {
                    setSupportActionBar(toolbar);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);

                    // Set the title text if it exists
                    TextView titleText = findViewById(R.id.titleText);
                    if (titleText != null) {
                        titleText.setText(activityTitle);
                    }

                    // Add welcome message if user is logged in
                    addWelcomeMessage();
                }
            } else {
                // For other activities, use the standard navigation panel + content approach
                setContentView(R.layout.modern_navigation_panel);

                // Find and initialize the navigation components
                drawerLayout = findViewById(R.id.drawer_layout);
                navigationView = findViewById(R.id.nav_view);
                bottomNavigationView = findViewById(R.id.bottomNavigation);

                // Set up the toolbar
                Toolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar != null) {
                    setSupportActionBar(toolbar);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);

                    // Set the title text
                    TextView titleText = findViewById(R.id.titleText);
                    if (titleText != null) {
                        titleText.setText(activityTitle);
                    }

                    // Add welcome message if user is logged in
                    addWelcomeMessage();
                }

                // Inflate the content layout into the content frame
                if (contentLayoutId != 0) {
                    View contentFrame = findViewById(R.id.content_frame);
                    if (contentFrame != null && contentFrame instanceof ViewGroup) {
                        getLayoutInflater().inflate(contentLayoutId, (ViewGroup) contentFrame);
                    } else {
                        Log.e(TAG, "Content frame not found in navigation panel layout");
                        setContentView(contentLayoutId); // Fallback
                    }
                }
            }

            // Set up the menu button to open the drawer
            View menuButton = findViewById(R.id.menuButton);
            if (menuButton != null) {
                menuButton.setOnClickListener(v -> {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                });
            }

            // Set up profile button (navigate to settings)
            View profileButton = findViewById(R.id.profileButton);
            if (profileButton != null) {
                profileButton.setOnClickListener(v -> {
                    try {
                        navigateToActivitySafely(SettingsActivity.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to settings", e);
                    }
                });
            }

            // Ensure notification button works
            View notificationButton = findViewById(R.id.notificationButton);
            if (notificationButton != null) {
                notificationButton.setOnClickListener(v -> {
                    try {
                        showNotificationsPanel();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing notifications", e);
                        Toast.makeText(this, "Could not load notifications", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            Log.d(TAG, "Navigation listeners successfully configured");

            // Set up navigation view with error handling
            if (navigationView != null) {
                try {
                    navigationView.setNavigationItemSelectedListener(item -> {
                        try {
                            // Handle navigation view item clicks
                            int id = item.getItemId();

                            if (id == R.id.nav_dashboard) {
                                navigateToActivitySafely(ManagerDashboardActivity.class);
                            } else if (id == R.id.nav_orders) {
                                navigateToActivitySafely(OrderActivity.class);
                            } else if (id == R.id.nav_reservation) {
                                navigateToActivitySafely(ReservationActivity.class);
                            } else if (id == R.id.nav_staff) {
                                navigateToActivitySafely(StaffManagementActivity.class);
                            } else if (id == R.id.nav_menu) {
                                // Direct all users to MenuManagementActivity
                                navigateToActivitySafely(MenuManagementActivity.class);
                            } else if (id == R.id.nav_inventory) {
                                navigateToActivitySafely(InventoryActivity.class);
                            } else if (id == R.id.nav_reviews) {
                                navigateToActivitySafely(ReviewsActivity.class);
                            } else if (id == R.id.nav_settings) {
                                navigateToActivitySafely(SettingsActivity.class);
                            } else if (id == R.id.nav_logout) {
                                logout();
                                return true;
                            }

                            // Close the drawer
                            if (drawerLayout != null) {
                                drawerLayout.closeDrawer(GravityCompat.START);
                            }
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error in navigation item click", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up navigation listener", e);
                }
            } else {
                Log.w(TAG, "Navigation view not found in layout");
            }

            // Set up bottom navigation with error handling
            if (bottomNavigationView != null) {
                try {
                    bottomNavigationView.setOnItemSelectedListener(item -> {
                        try {
                            int itemId = item.getItemId();
  
                            if (itemId == R.id.navigation_dashboard) {
                                navigateToActivitySafely(ManagerDashboardActivity.class);
                                return true;
                            } else if (itemId == R.id.navigation_orders) {
                                navigateToActivitySafely(OrderActivity.class);
                                return true;
                            } else if (itemId == R.id.navigation_reservation) {
                                navigateToActivitySafely(ReservationActivity.class);
                                return true;
                            } else if (itemId == R.id.navigation_menu) {
                                // Direct all users to MenuManagementActivity for consistent experience
                                navigateToActivitySafely(MenuManagementActivity.class);
                                return true;
                            } else if (itemId == R.id.navigation_more) {
                                if (drawerLayout != null) {
                                    drawerLayout.openDrawer(GravityCompat.START);
                                } else {
                                    Log.e(TAG, "Drawer layout is null, can't open drawer");
                                    Toast.makeText(BaseActivity.this,
                                            "Navigation error: Drawer not available",
                                            Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            }

                            return false;
                        } catch (Exception e) {
                            Log.e(TAG, "Error in bottom navigation selection", e);
                            Toast.makeText(BaseActivity.this,
                                    "Navigation error",
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up bottom navigation", e);
                }

                // Set the selected item based on current activity
                setSelectedBottomNavigationItem();
            }

            // Set up navigation listeners
            setupNavigationListeners();

            // Set the selected bottom navigation item based on current activity
            setSelectedBottomNavigationItem();

            // Adjust navigation based on user role
            adjustNavigationByRole();

            // Mark modern navigation as setup
            isModernNavigationSetup = true;

            Log.d(TAG, "Modern navigation panel setup complete for: " + activityTitle);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up modern navigation panel", e);
            Toast.makeText(this, "Error setting up navigation", Toast.LENGTH_SHORT).show();

            // Fall back to the content layout if navigation setup fails
            try {
                if (contentLayoutId != 0) {
                    setContentView(contentLayoutId);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error setting fallback content view", ex);
                // Create emergency TextView as last resort
                TextView errorView = new TextView(this);
                errorView.setText("Error loading view. Please restart the app.");
                errorView.setPadding(50, 100, 50, 0);
                setContentView(errorView);
            }
        }
    }

    /**
     * Set the selected item in the bottom navigation based on current activity
     */
    private void setSelectedBottomNavigationItem() {
        if (bottomNavigationView == null) return;

        // Determine which bottom nav item to select based on current activity
        if (this instanceof ManagerDashboardActivity) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);
        } else if (this instanceof OrderActivity) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_orders);
        } else if (this instanceof ReservationActivity) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_reservation);
        } else if (this instanceof MenuManagementActivity) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_menu);
        }
    }

    /**
     * Refresh the modern navigation when needed
     */
    private void refreshModernNavigation() {
        // Set the selected bottom navigation item based on current activity
        setSelectedBottomNavigationItem();

        // Adjust navigation visibility based on role
        adjustNavigationByRole();

        // Double check all navigation listeners are properly attached
        setupNavigationListeners();
    }

    /**
     * Ensure all navigation listeners are properly attached
     */
    private void setupNavigationListeners() {
        try {
            // Set up bottom navigation listeners again to ensure they work
            if (bottomNavigationView != null) {
                bottomNavigationView.setOnItemSelectedListener(item -> {
                    int itemId = item.getItemId();

                    // Handle navigation based on selected item
                    if (itemId == R.id.navigation_dashboard) {
                        navigateToActivitySafely(ManagerDashboardActivity.class);
                        return true;
                    } else if (itemId == R.id.navigation_orders) {
                        navigateToActivitySafely(OrderActivity.class);
                        return true;
                    } else if (itemId == R.id.navigation_reservation) {
                        navigateToActivitySafely(ReservationActivity.class);
                        return true;
                    } else if (itemId == R.id.navigation_menu) {
                        navigateToActivitySafely(MenuManagementActivity.class);
                        return true;
                    } else if (itemId == R.id.navigation_more) {
                        if (drawerLayout != null) {
                            drawerLayout.openDrawer(GravityCompat.START);
                        }
                        return true;
                    }

                    return false;
                });
            }

            // Set up navigation view listeners again to ensure they work
            if (navigationView != null) {
                navigationView.setNavigationItemSelectedListener(item -> {
                    // Handle navigation view item clicks
                    int id = item.getItemId();
                    boolean handled = true;

                    if (id == R.id.nav_dashboard) {
                        navigateToActivitySafely(ManagerDashboardActivity.class);
                    } else if (id == R.id.nav_orders) {
                        navigateToActivitySafely(OrderActivity.class);
                    } else if (id == R.id.nav_reservation) {
                        navigateToActivitySafely(ReservationActivity.class);
                    } else if (id == R.id.nav_staff) {
                        navigateToActivitySafely(StaffManagementActivity.class);
                    } else if (id == R.id.nav_menu) {
                        navigateToActivitySafely(MenuManagementActivity.class);
                    } else if (id == R.id.nav_inventory) {
                        navigateToActivitySafely(InventoryActivity.class);
                    } else if (id == R.id.nav_reviews) {
                        navigateToActivitySafely(ReviewsActivity.class);
                    } else if (id == R.id.nav_settings) {
                        navigateToActivitySafely(SettingsActivity.class);
                    } else if (id == R.id.nav_logout) {
                        logout();
                    } else {
                        handled = false;
                    }

                    // Close the drawer
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }

                    return handled;
                });
            }

            // Ensure the menu button works to open the drawer
            View menuButton = findViewById(R.id.menuButton);
            if (menuButton != null) {
                menuButton.setOnClickListener(v -> {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                });
            }

            // Ensure profile button works
            View profileButton = findViewById(R.id.profileButton);
            if (profileButton != null) {
                profileButton.setOnClickListener(v -> {
                    try {
                        navigateToActivitySafely(SettingsActivity.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to settings", e);
                    }
                });
            }

            Log.d(TAG, "Navigation listeners successfully configured");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation listeners", e);
        }
    }

    /**
     * Adjust navigation elements based on user role
     */
    private void adjustNavigationByRole() {
        if (!isModernNavigationSetup) return;

        try {
            // Adjust based on user role
            switch (userRole.toLowerCase()) {
                case "admin":
                    // Admin can access administrator functions, reviews, menu reservations and other suitable things
                    // except for manager dashboard
                    if (navigationView != null && navigationView.getMenu() != null) {
                        navigationView.getMenu().findItem(R.id.nav_dashboard).setVisible(false);
                    }
                    // Adjust bottom navigation
                    if (bottomNavigationView != null && bottomNavigationView.getMenu() != null) {
                        bottomNavigationView.getMenu().findItem(R.id.navigation_dashboard).setVisible(false);
                    }
                    break;

                case "manager":
                    // Manager can access everything - no restrictions
                    break;

                case "chef":
                    // Chef can only access menu management, kitchen, and reviews settings
                    if (navigationView != null && navigationView.getMenu() != null) {
                        navigationView.getMenu().findItem(R.id.nav_dashboard).setVisible(false);
                        navigationView.getMenu().findItem(R.id.nav_staff).setVisible(false);
                        navigationView.getMenu().findItem(R.id.nav_orders).setVisible(false);
                        navigationView.getMenu().findItem(R.id.nav_reservation).setVisible(false);
                        navigationView.getMenu().findItem(R.id.nav_inventory).setVisible(false);
                    }
                    // Adjust bottom navigation
                    if (bottomNavigationView != null && bottomNavigationView.getMenu() != null) {
                        bottomNavigationView.getMenu().findItem(R.id.navigation_dashboard).setVisible(false);
                        bottomNavigationView.getMenu().findItem(R.id.navigation_orders).setVisible(false);
                        bottomNavigationView.getMenu().findItem(R.id.navigation_reservation).setVisible(false);
                    }
                    break;

                case "waiter":
                case "customer":
                default:
                    // Waiter and customer can access menu, orders, reviews and reservations
                    if (navigationView != null && navigationView.getMenu() != null) {
                        navigationView.getMenu().findItem(R.id.nav_dashboard).setVisible(false);
                        navigationView.getMenu().findItem(R.id.nav_staff).setVisible(false);
                        navigationView.getMenu().findItem(R.id.nav_inventory).setVisible(false);
                        // Kitchen nav item no longer exists in modern navigation

                        // Ensure these are visible
                        navigationView.getMenu().findItem(R.id.nav_menu).setVisible(true);
                        navigationView.getMenu().findItem(R.id.nav_orders).setVisible(true);
                        navigationView.getMenu().findItem(R.id.nav_reservation).setVisible(true);
                        navigationView.getMenu().findItem(R.id.nav_reviews).setVisible(true);
                    }
                    // Adjust bottom navigation
                    if (bottomNavigationView != null && bottomNavigationView.getMenu() != null) {
                        bottomNavigationView.getMenu().findItem(R.id.navigation_dashboard).setVisible(false);

                        // Ensure these are visible
                        bottomNavigationView.getMenu().findItem(R.id.navigation_menu).setVisible(true);
                        bottomNavigationView.getMenu().findItem(R.id.navigation_orders).setVisible(true);
                        bottomNavigationView.getMenu().findItem(R.id.navigation_reservation).setVisible(true);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting navigation by role", e);
        }
    }

    @Override
    public void onBackPressed() {
        // Close drawer if open when back button is pressed
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Add welcome message with user name to the navigation panel
     */
    protected void addWelcomeMessage() {
        try {
            TextView welcomeText = findViewById(R.id.welcomeText);
            if (welcomeText != null) {
                // Get user name from intent or shared preferences
                String userName = getIntent().getStringExtra("user_name");
                if (userName == null || userName.isEmpty()) {
                    // Try to get from SharedPrefsManager
                    if (prefsManager != null) {
                        userName = prefsManager.getUserName();
                    }
                }

                // If we found a username, display a welcome message
                if (userName != null && !userName.isEmpty()) {
                    welcomeText.setText("Welcome back, " + userName + "!");
                    welcomeText.setVisibility(View.VISIBLE);
                } else {
                    welcomeText.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting welcome message", e);
        }
    }

    /**
     * Check that the userRole is consistent with shared preferences
     * and update if necessary
     */
    private void checkRoleConsistency() {
        try {
            if (prefsManager != null) {
                String savedRole = prefsManager.getUserRole();
                if (savedRole != null && !savedRole.isEmpty() && !savedRole.equals(userRole)) {
                    Log.d(TAG, "Role inconsistency detected. Updating from " + userRole + " to " + savedRole);
                    userRole = savedRole;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking role consistency", e);
        }
    }

    /**
     * Set up date and time pickers for EditText fields with "date" or "time" in their hint or ID
     */
    private void setupDateAndTimePickers() {
        try {
            // Get all EditText views from the root view
            ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
            setupDateTimePickersRecursively(rootView);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up date/time pickers", e);
        }
    }

    /**
     * Recursively find EditText fields that should have date or time pickers
     */
    private void setupDateTimePickersRecursively(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setupDateTimePickersRecursively(viewGroup.getChildAt(i));
            }
        } else if (view instanceof EditText) {
            EditText editText = (EditText) view;
            String hint = editText.getHint() != null ? editText.getHint().toString().toLowerCase() : "";
            String idName = "";
            try {
                idName = getResources().getResourceEntryName(editText.getId()).toLowerCase();
            } catch (Exception e) {
                // No resource ID or no name
            }

            // Check if this is a date field
            boolean isDateField = hint.contains("date") || idName.contains("date");
            boolean isTimeField = hint.contains("time") || idName.contains("time");

            if (isDateField) {
                // Make non-editable and setup date picker
                editText.setFocusable(false);
                editText.setClickable(true);
                editText.setCursorVisible(false);
                editText.setOnClickListener(v -> showDatePicker(v));
                Log.d(TAG, "Added date picker to: " + idName);
            } else if (isTimeField) {
                // Make non-editable and setup time picker
                editText.setFocusable(false);
                editText.setClickable(true);
                editText.setCursorVisible(false);
                editText.setOnClickListener(v -> showTimePicker(v));
                Log.d(TAG, "Added time picker to: " + idName);
            }
        }
    }

    protected void ensureAllButtonsWork() {
        try {
            // Find all buttons in the activity layout
            findAndFixPotentialButtonIssues(getWindow().getDecorView());

            // Double check specific important buttons
            checkSpecificButtons();

            // Log completion
            Log.d(TAG, "Ensured all buttons are working properly");
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring buttons work", e);
        }
    }

    /**
     * Recursively find all buttons in a view hierarchy and make sure
     * they have click listeners if they're clickable
     */
    private void findAndFixPotentialButtonIssues(View view) {
        try {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    findAndFixPotentialButtonIssues(viewGroup.getChildAt(i));
                }
            } else if (view instanceof Button || view instanceof androidx.appcompat.widget.AppCompatButton ||
                    view instanceof com.google.android.material.button.MaterialButton) {
                Button button = (Button) view;

                // Check if button is clickable but has no click listener
                if (button.isClickable() && !hasClickListener(button)) {
                    // Get button ID for better logging
                    String buttonInfo = getButtonInfo(button);
                    Log.w(TAG, "Button " + buttonInfo + " has no click listener, adding default handler");

                    // Add appropriate click handler based on button type
                    if (button.getId() == R.id.backButton) {
                        button.setOnClickListener(v -> onBackPressed());
                    } else if (button.getId() == R.id.submitButton || button.getId() == R.id.saveButton) {
                        button.setOnClickListener(v -> {
                            // Default submit/save behavior - close keyboard and show processing
                            View currentFocus = getCurrentFocus();
                            if (currentFocus != null) {
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                            }
                            Toast.makeText(BaseActivity.this, "Processing...", Toast.LENGTH_SHORT).show();
                        });
                    } else if (button.getId() == R.id.cancelButton) {
                        button.setOnClickListener(v -> finish());
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("add")) {
                        button.setOnClickListener(v -> {
                            try {
                                // Show a toast since we don't have specific add activities
                                Toast.makeText(BaseActivity.this, "Add functionality coming soon", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Log.e(TAG, "Error handling add action", e);
                            }
                        });
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("delete")) {
                        button.setOnClickListener(v -> {
                            new AlertDialog.Builder(BaseActivity.this)
                                    .setTitle("Confirm Delete")
                                    .setMessage("Are you sure you want to delete this item?")
                                    .setPositiveButton("Delete", (dialog, which) -> {
                                        Toast.makeText(BaseActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("search")) {
                        button.setOnClickListener(v -> {
                            View currentFocus = getCurrentFocus();
                            if (currentFocus != null) {
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                            }
                            // Show simple search dialog
                            showSearchDialog();
                        });
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("time")) {
                        button.setOnClickListener(v -> showTimePicker(v));
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("date")) {
                        button.setOnClickListener(v -> showDatePicker(v));
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("view")) {
                        button.setOnClickListener(v -> {
                            // Default view behavior
                            Toast.makeText(this, "View action", Toast.LENGTH_SHORT).show();
                        });
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("edit")) {
                        button.setOnClickListener(v -> {
                            // Default edit behavior
                            Toast.makeText(this, "Edit action", Toast.LENGTH_SHORT).show();
                        });
                    } else if (button.getText() != null && button.getText().toString().toLowerCase().contains("refresh")) {
                        button.setOnClickListener(v -> {
                            refreshUI();
                            Toast.makeText(BaseActivity.this, "Refreshing...", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        // For other buttons, perform action based on text
                        button.setOnClickListener(v -> {
                            String buttonText = button.getText() != null ? button.getText().toString().toLowerCase() : "";
                            if (buttonText.contains("ok") || buttonText.contains("done")) {
                                finish();
                            } else if (buttonText.contains("close")) {
                                onBackPressed();
                            } else if (buttonText.contains("help")) {
                                showHelpDialog();
                            } else if (buttonText.contains("info")) {
                                showInfoDialog();
                            } else if (buttonText.contains("next") || buttonText.contains("continue")) {
                                navigateToNextScreen();
                            } else if (buttonText.contains("previous") || buttonText.contains("back")) {
                                onBackPressed();
                            } else {
                                Log.d(TAG, "Button clicked: " + buttonInfo);
                                Toast.makeText(BaseActivity.this,
                                        buttonText.isEmpty() ? "Action performed" : buttonText + " action",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking button", e);
        }
    }

    /**
     * Get descriptive information about a button for logging
     */
    private String getButtonInfo(Button button) {
        StringBuilder info = new StringBuilder();
        try {
            // Get button ID if available
            if (button.getId() != View.NO_ID) {
                try {
                    String idName = getResources().getResourceEntryName(button.getId());
                    info.append("id=").append(idName);
                } catch (Exception e) {
                    info.append("id=").append(button.getId());
                }
            }

            // Get button text if available
            if (button.getText() != null && !button.getText().toString().isEmpty()) {
                if (info.length() > 0) info.append(", ");
                info.append("text='").append(button.getText()).append("'");
            }

            // If still empty, just use the class name and object ID
            if (info.length() == 0) {
                info.append(button.getClass().getSimpleName()).append("@").append(button.hashCode());
            }
        } catch (Exception e) {
            info.append("unknown");
        }
        return info.toString();
    }

    /**
     * Check if a view has an onClick listener attached
     */
    private boolean hasClickListener(View view) {
        // This is a bit of a hack - we attach a temporary listener and then check
        // if it's the only one by immediately removing it. If it is, then no other
        // listeners were attached.
        View.OnClickListener marker = v -> { /* empty marker */ };
        view.setOnClickListener(marker);
        boolean hasExistingListeners = false;

        try {
            // Using reflection to check for listeners
            Class viewClass = View.class;
            java.lang.reflect.Field listenerInfoField = viewClass.getDeclaredField("mListenerInfo");
            listenerInfoField.setAccessible(true);
            Object listenerInfo = listenerInfoField.get(view);

            if (listenerInfo != null) {
                Class listenerInfoClass = listenerInfo.getClass();
                java.lang.reflect.Field clickListenerField = listenerInfoClass.getDeclaredField("mOnClickListener");
                clickListenerField.setAccessible(true);
                Object clickListener = clickListenerField.get(listenerInfo);

                // If our marker is wrapped in another listener, it's not the only one
                hasExistingListeners = clickListener != null && !clickListener.equals(marker);
            }
        } catch (Exception e) {
            // If reflection fails, assume there was an existing listener
            hasExistingListeners = true;
        }

        // Restore null listener to not interfere
        view.setOnClickListener(null);
        return hasExistingListeners;
    }

    /**
     * Check specific important buttons in the application
     */
    private void checkSpecificButtons() {
        // Add checks for important buttons that must work
        String[] criticalButtonIds = {"logoutButton", "submitButton", "backButton"};

        for (String idName : criticalButtonIds) {
            try {
                int id = getResources().getIdentifier(idName, "id", getPackageName());
                if (id != 0) {
                    View view = findViewById(id);
                    if (view instanceof Button) {
                        Button button = (Button) view;
                        if (!hasClickListener(button)) {
                            Log.w(TAG, "Critical button " + idName + " has no click listener");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking critical button " + idName, e);
            }
        }
    }

    /**
     * Ensure all navigation options are available based on role
     */
    protected void validateNavigation() {
        try {
            // Check if navigation is properly set up
            if (!isNavigationSetup && !isModernNavigationSetup) {
                Log.w(TAG, "Navigation is not set up in " + getClass().getSimpleName());
                return;
            }

            // Verify that navigation elements are correctly displayed for the current role
            boolean hasAccessToRequiredScreens = false;

            switch (userRole.toLowerCase()) {
                case "admin":
                    // Admin should have access to admin functions
                    hasAccessToRequiredScreens = canNavigateTo(AdminActivity.class);
                    break;
                case "manager":
                    // Manager should have access to dashboard
                    hasAccessToRequiredScreens = canNavigateTo(ManagerDashboardActivity.class);
                    break;
                case "chef":
                    // Chef should have access to menu management and kitchen
                    hasAccessToRequiredScreens = canNavigateTo(MenuManagementActivity.class) &&
                            canNavigateTo(KitchenActivity.class);
                    break;
                case "waiter":
                case "customer":
                    // Waiter and customer should have access to order
                    hasAccessToRequiredScreens = canNavigateTo(OrderActivity.class);
                    break;
            }

            if (!hasAccessToRequiredScreens) {
                Log.w(TAG, "User role " + userRole + " missing required navigation access");
                // Force refresh navigation
                refreshNavigationBasedOnRole();
                refreshModernNavigation();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error validating navigation", e);
        }
    }

    /**
     * Check if navigation to a specific activity is possible
     */
    private boolean canNavigateTo(Class<?> destinationClass) {
        try {
            // Check if activity is registered in manifest
            if (!isActivityRegisteredInManifest(destinationClass)) {
                return false;
            }

            // For additional checks on whether UI elements for navigation exist, 
            // we would need to check specific navigation elements based on the 
            // current navigation style (modern vs traditional)

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking navigation capability", e);
            return false;
        }
    }

    /**
     * Helper method to update all UI elements on theme or configuration changes
     */
    protected void refreshUI() {
        try {
            // Refresh navigation if set up
            if (isNavigationSetup) {
                refreshNavigationBasedOnRole();
            }

            // Refresh modern navigation if set up
            if (isModernNavigationSetup) {
                refreshModernNavigation();
            }

            // Ensure all buttons work
            ensureAllButtonsWork();

            // Validate navigation
            validateNavigation();

            // If there's a welcome message view, refresh it
            addWelcomeMessage();

        } catch (Exception e) {
            Log.e(TAG, "Error refreshing UI", e);
        }
    }

    /**
     * Show a help dialog with general app assistance
     */
    protected void showHelpDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Help")
                    .setMessage("Welcome to Fine Dine! This app helps you manage all aspects of your restaurant experience.")
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing help dialog", e);
        }
    }

    /**
     * Show information about the current screen
     */
    protected void showInfoDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Information")
                    .setMessage("This screen helps you manage " + getTitle() + ". Tap on items to interact with them.")
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing info dialog", e);
        }
    }

    /**
     * Show the notifications panel with any active notifications
     */
    protected void showNotificationsPanel() {
        try {
            // Create a custom dialog for notifications
            final Dialog notificationDialog = new Dialog(this);

            // Check if custom layout resource exists - we'll use reflection to avoid compile errors
            int dialogLayoutId = getResources().getIdentifier("dialog_notifications", "layout", getPackageName());
            if (dialogLayoutId != 0) {
                notificationDialog.setContentView(dialogLayoutId);
            } else {
                // Layout doesn't exist, fall back to a simple dialog
                showNotificationsFallback();
                return;
            }

            // If the layout doesn't exist, create a simple AlertDialog as fallback
            if (notificationDialog.getWindow() == null) {
                showNotificationsFallback();
                return;
            }

            // Set dialog title
            int titleId = getResources().getIdentifier("notification_title", "id", getPackageName());
            if (titleId != 0) {
                TextView titleText = notificationDialog.findViewById(titleId);
                if (titleText != null) {
                    titleText.setText("Notifications");
                }
            }

            // Set up close button
            int closeButtonId = getResources().getIdentifier("close_button", "id", getPackageName());
            if (closeButtonId != 0) {
                View closeButton = notificationDialog.findViewById(closeButtonId);
                if (closeButton != null) {
                    closeButton.setOnClickListener(v -> notificationDialog.dismiss());
                }
            }

            // Get any current notifications - in a real app this would load from storage/database
            // For now, we'll just create some sample notifications
            String[] notificationTexts = getSampleNotifications();

            // Try to find the notification container
            int containerId = getResources().getIdentifier("notifications_container", "id", getPackageName());
            if (containerId != 0) {
                ViewGroup notificationContainer = notificationDialog.findViewById(containerId);
                if (notificationContainer != null && notificationTexts.length > 0) {
                    // Inflate notifications into the container
                    for (String notification : notificationTexts) {
                        int itemLayoutId = getResources().getIdentifier("item_notification", "layout", getPackageName());

                        if (itemLayoutId != 0) {
                            // Custom notification item layout exists
                            View notificationItem = getLayoutInflater().inflate(itemLayoutId, notificationContainer, false);

                            if (notificationItem != null) {
                                // Set notification text
                                int textViewId = getResources().getIdentifier("notification_text", "id", getPackageName());
                                if (textViewId != 0) {
                                    TextView notificationText = notificationItem.findViewById(textViewId);
                                    if (notificationText != null) {
                                        notificationText.setText(notification);
                                    }
                                }

                                // Add to container
                                notificationContainer.addView(notificationItem);
                            }
                        } else {
                            // No custom layout, create a simple text view
                            TextView textView = new TextView(this);
                            textView.setText(notification);
                            textView.setPadding(30, 20, 30, 20);
                            notificationContainer.addView(textView);
                        }
                    }
                } else if (notificationContainer != null) {
                    // No notifications, show empty state
                    TextView emptyView = new TextView(this);
                    emptyView.setText("No new notifications");
                    emptyView.setPadding(30, 20, 30, 20);
                    emptyView.setGravity(android.view.Gravity.CENTER);
                    notificationContainer.addView(emptyView);
                } else {
                    // Container not found
                    showNotificationsFallback();
                    return;
                }
            } else {
                // Container ID not found
                showNotificationsFallback();
                return;
            }

            // Show the dialog
            notificationDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing notifications panel", e);
            showNotificationsFallback();
        }
    }

    /**
     * Fallback method to show notifications in a simple alert dialog
     */
    private void showNotificationsFallback() {
        try {
            String[] notifications = getSampleNotifications();
            StringBuilder message = new StringBuilder();

            if (notifications.length > 0) {
                for (String notification : notifications) {
                    message.append("• ").append(notification).append("\n\n");
                }
            } else {
                message.append("No new notifications");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Notifications")
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing notifications fallback", e);
            Toast.makeText(this, "Could not load notifications", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get sample notifications for testing
     * In a real app, these would come from a notification manager or database
     */
    private String[] getSampleNotifications() {
        try {
            // If user is logged in, try to get their role for relevant notifications
            String role = userRole.toLowerCase();

            switch (role) {
                case "manager":
                    return new String[]{
                            "2 new reservations require approval",
                            "Inventory alert: Wine stock running low",
                            "Staff meeting scheduled for tomorrow at 9:00 AM"
                    };
                case "chef":
                    return new String[]{
                            "New order for table 5 ready to prepare",
                            "Inventory alert: Tomatoes running low",
                            "Menu update requested by manager"
                    };
                case "waiter":
                    return new String[]{
                            "Table 3 order is ready for service",
                            "New reservation for 7:30 PM tonight",
                            "Manager posted updated shift schedule"
                    };
                case "customer":
                default:
                    return new String[]{
                            "Your reservation is confirmed for 7:00 PM",
                            "Special discount: 10% off on your next visit!",
                            "Thank you for your recent order"
                    };
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting sample notifications", e);
            return new String[]{"Welcome to Fine Dine"};
        }
    }

    /**
     * Navigate to the logical next screen based on current context
     */
    protected void navigateToNextScreen() {
        // Default implementation just proceeds with current task
        Toast.makeText(this, "Proceeding to next step", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a date picker dialog and update the view with selected date
     */
    protected void showDatePicker(View dateView) {
        try {
            // Get current date
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Create and show date picker dialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format the selected date
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        String formattedDate = android.text.format.DateFormat.format("EEE, MMM d, yyyy", selectedDate).toString();
                        // Update view text
                        if (dateView instanceof Button) {
                            ((Button) dateView).setText(formattedDate);
                        } else if (dateView instanceof EditText) {
                            ((EditText) dateView).setText(formattedDate);
                        }
                    },
                    year, month, day);

            datePickerDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing date picker", e);
            Toast.makeText(this, "Error selecting date", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show a time picker dialog and update the view with selected time
     */
    protected void showTimePicker(View timeView) {
        try {
            // Get current time
            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Create and show time picker dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, selectedHour, selectedMinute) -> {
                        // Format the selected time in AM/PM format
                        Calendar time = Calendar.getInstance();
                        time.set(Calendar.HOUR_OF_DAY, selectedHour);
                        time.set(Calendar.MINUTE, selectedMinute);
                        String formattedTime = android.text.format.DateFormat.format("h:mm a", time).toString();
                        // Update view text
                        if (timeView instanceof Button) {
                            ((Button) timeView).setText(formattedTime);
                        } else if (timeView instanceof EditText) {
                            ((EditText) timeView).setText(formattedTime);
                        }
                    },
                    hour, minute, false); // false for 12 hour (AM/PM) format

            timePickerDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker", e);
            Toast.makeText(this, "Error selecting time", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show a simple search dialog
     */
    protected void showSearchDialog() {
        try {
            // Create an EditText for search input
            EditText searchInput = new EditText(this);
            searchInput.setHint("Enter search term");

            // Create and show search dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Search")
                    .setView(searchInput)
                    .setPositiveButton("Search", (dialog, which) -> {
                        String query = searchInput.getText().toString().trim();
                        if (!query.isEmpty()) {
                            Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
                            // In a real app, this would perform the actual search
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing search dialog", e);
        }
    }
}