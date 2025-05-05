package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.utils.SharedPrefsManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private TextView forgotPasswordText;
    private ProgressBar progressBar;
    private Button restoreMenuButton;
    private int logoClickCount = 0;
    private SharedPrefsManager prefsManager;
    private String email;  // Store email for later use

    private AppDatabase appDatabase;
    private ExecutorService databaseExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "LoginActivity onCreate starting");

            // Set the layout
            setContentView(R.layout.activity_login);
            Log.d(TAG, "LoginActivity - setContentView complete");

            // Initialize views first to avoid NullPointerExceptions 
            initializeViews();

            // Ensure Firebase is initialized
            if (!FineDineApplication.isFirebaseInitialized()) {
                Log.w(TAG, "Firebase not properly initialized, attempting to initialize now");
                try {
                    if (FirebaseApp.getApps(this).isEmpty()) {
                        FirebaseApp.initializeApp(this);
                        Log.d(TAG, "Firebase initialized in LoginActivity");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initialize Firebase in LoginActivity", e);
                    // Show a subtle message that offline mode is active
                    Toast.makeText(this, "Offline mode active", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "Firebase already initialized");
            }

            // Initialize SharedPrefsManager
            prefsManager = new SharedPrefsManager(this);

            // Get database instances
            appDatabase = ((FineDineApplication) getApplication()).getDatabase();
            databaseExecutor = ((FineDineApplication) getApplication()).getDatabaseExecutor();

            // Log that we're staying on the login screen
            Log.d(TAG, "Showing login screen regardless of login state");

            // Set up click listeners
            if (loginButton != null) {
                loginButton.setOnClickListener(v -> attemptLogin());
            }

            if (registerButton != null) {
                registerButton.setOnClickListener(v -> navigateToRegister());
            }

            if (forgotPasswordText != null) {
                forgotPasswordText.setOnClickListener(v -> navigateToForgotPassword());
            }

            // Setup logo click to reveal restore button (secret gesture)
            View logo = findViewById(R.id.logoImage);
            if (logo != null) {
                logo.setOnClickListener(v -> {
                    logoClickCount++;
                    if (logoClickCount >= 5) {
                        // Show restore button after 5 clicks on logo
                        if (restoreMenuButton != null) {
                            restoreMenuButton.setVisibility(View.VISIBLE);
                            Toast.makeText(LoginActivity.this, "Menu restoration option enabled", Toast.LENGTH_SHORT).show();
                        }
                        logoClickCount = 0;
                    }
                });
            }

            // Setup restore menu button
            if (restoreMenuButton != null) {
                restoreMenuButton.setOnClickListener(v -> {
                    // Show confirmation dialog
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Restore Menu Items")
                            .setMessage("This will reset all menu items to their original state. Any custom menu items will be deleted. Continue?")
                            .setPositiveButton("Restore", (dialog, which) -> {
                                forceRestoreMenuItems();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }

            // Ensure we have test users in database
            ensureTestUsers();

            // Force reset menu items to restore any missing images
            resetMenuItemsDatabase();

            Log.d(TAG, "LoginActivity setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error in LoginActivity onCreate", e);
            Toast.makeText(this, "Error initializing login: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initialize all the views to prevent NullPointerExceptions
     */
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailInput);
        passwordEditText = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        progressBar = findViewById(R.id.progressBar);
        restoreMenuButton = findViewById(R.id.restoreMenuButton);

        // Hide progress initially
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Hide restore menu button initially
        if (restoreMenuButton != null) {
            restoreMenuButton.setVisibility(View.GONE);
        }
    }

    private void attemptLogin() {
        try {
            // Get input values
            email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Validate inputs
            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                emailEditText.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                passwordEditText.setError("Password is required");
                passwordEditText.requestFocus();
                return;
            }

            // Check network connectivity before attempting Firebase auth
            if (FineDineApplication.isFirebaseInitialized() && !isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection. Using local authentication.", Toast.LENGTH_SHORT).show();
                authenticateWithHardcodedCredentials(email, password);
                return;
            }

            authenticateUser(email, password);
        } catch (Exception e) {
            Log.e(TAG, "Error attempting login", e);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager =
                    (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.net.Network network = connectivityManager.getActiveNetwork();
                    android.net.NetworkCapabilities capabilities =
                            connectivityManager.getNetworkCapabilities(network);
                    return capabilities != null &&
                            (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
                } else {
                    // For older Android versions
                    android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability", e);
        }
        return false;
    }

    private void authenticateUser(String email, String password) {
        // Show progress dialog
        showProgressDialog("Authenticating...");

        // Prioritize Firebase authentication
        if (FineDineApplication.isFirebaseInitialized()) {
            Log.d(TAG, "Using Firebase authentication");
            authenticateWithFirebase(email, password);
        } else {
            // If Firebase is not available, fall back to local auth
            Log.w(TAG, "Firebase not initialized, falling back to local authentication");
            authenticateWithHardcodedCredentials(email, password);
        }
    }

    /**
     * Firebase authentication method
     */
    private void authenticateWithFirebase(String email, String password) {
        try {
            // Get Firebase Auth instance
            FirebaseAuth mAuth = FineDineApplication.getFirebaseAuth();
            if (mAuth == null) {
                Log.e(TAG, "Firebase Auth instance is null, using local authentication");
                authenticateWithHardcodedCredentials(email, password);
                return;
            }

            // Attempt Firebase authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        hideProgressDialog();
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase authentication successful");
                            // Get user ID from Firebase
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String role = determineUserRole(user.getEmail());
                                handleSuccessfulLogin(role);
                            } else {
                                Log.e(TAG, "Firebase user is null after successful login");
                                showErrorMessage("Login error: User data unavailable");
                                authenticateWithHardcodedCredentials(email, password);
                            }
                        } else {
                            Log.w(TAG, "Firebase authentication failed", task.getException());
                            String errorMessage = task.getException() != null ?
                                    task.getException().getLocalizedMessage() : "Unknown error";

                            // Try local authentication as fallback instead of showing error
                            Log.d(TAG, "Trying local authentication as fallback");
                            authenticateWithHardcodedCredentials(email, password);
                        }
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Log.e(TAG, "Firebase auth exception", e);
                        // Fallback to local authentication
                        authenticateWithHardcodedCredentials(email, password);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error with Firebase authentication", e);
            hideProgressDialog();
            showErrorMessage("Firebase error: " + e.getLocalizedMessage());
            authenticateWithHardcodedCredentials(email, password);
        }
    }

    private String determineUserRole(String email) {
        if (email == null) return "customer";

        email = email.toLowerCase();
        if (email.contains("admin")) {
            return "admin";
        } else if (email.contains("manager")) {
            return "manager";
        } else if (email.contains("chef")) {
            return "chef";
        } else if (email.contains("waiter")) {
            return "waiter";
        } else {
            return "customer";
        }
    }

    private void authenticateWithHardcodedCredentials(String email, String password) {
        // For demo mode, use hardcoded credentials
        if (email.equals("admin") && password.equals("admin")) {
            handleSuccessfulLogin("admin");
            return;
        } else if (email.equals("manager") && password.equals("manager")) {
            handleSuccessfulLogin("manager");
            return;
        } else if (email.equals("chef") && password.equals("chef")) {
            handleSuccessfulLogin("chef");
            return;
        } else if (email.equals("waiter") && password.equals("waiter")) {
            handleSuccessfulLogin("waiter");
            return;
        } else if (email.equals("customer") && password.equals("customer")) {
            handleSuccessfulLogin("customer");
            return;
        }

        // When testing, allow easy login (remove in production)
        if (password.equals("test123") || password.equals("finedine")) {
            // For testing purposes, determine role based on email
            String role = determineUserRole(email);
            handleSuccessfulLogin(role);
            return;
        }

        // Additional easy login options based on email format
        if (email.contains("@") || email.length() > 5) {
            if (email.toLowerCase().contains("admin")) {
                handleSuccessfulLogin("admin");
                return;
            } else if (email.toLowerCase().contains("manager")) {
                handleSuccessfulLogin("manager");
                return;
            } else if (email.toLowerCase().contains("chef")) {
                handleSuccessfulLogin("chef");
                return;
            } else if (email.toLowerCase().contains("waiter")) {
                handleSuccessfulLogin("waiter");
                return;
            } else if (email.toLowerCase().contains("customer")) {
                handleSuccessfulLogin("customer");
                return;
            }
        }

        // Try database authentication
        authenticateWithDatabase(email, password);
    }

    private void authenticateWithDatabase(String email, String password) {
        if (appDatabase == null || databaseExecutor == null) {
            hideProgressDialog();
            showErrorMessage("Database not initialized. Please restart the app.");
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                final User user = appDatabase.userDao().login(email, password);

                runOnUiThread(() -> {
                    if (user != null) {
                        // Login successful
                        handleSuccessfulLogin(user.role);
                    } else {
                        // Invalid credentials
                        hideProgressDialog();
                        showErrorMessage("Invalid email or password");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Database authentication error", e);
                runOnUiThread(() -> {
                    hideProgressDialog();
                    showErrorMessage("Database error: " + e.getMessage());
                });
            }
        });
    }

    private void handleSuccessfulLogin(String role) {
        try {
            Log.d(TAG, "Login successful for role: " + role);

            // Hide progress
            hideProgressDialog();

            // Prevent null roles
            if (role == null || role.trim().isEmpty()) {
                Log.w(TAG, "Role is null or empty, using customer as default");
                role = "customer";
            }

            // Store user session
            if (prefsManager == null) {
                prefsManager = new SharedPrefsManager(this);
            }
            prefsManager.saveUserSession(1, role, email != null ? email : "User");
            prefsManager.setUserLoggedIn(true);

            // Figure out which activity to start based on role
            Intent intent;
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

            Log.d(TAG, "Navigating to " + intent.getComponent().getClassName() + " for role: " + role);
            intent.putExtra("user_role", role);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Apply animation for smoother transition
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error handling successful login", e);
            Toast.makeText(this, "Error navigating to next screen", Toast.LENGTH_SHORT).show();
            hideProgressDialog();
        }
    }

    private void showProgressDialog(String message) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            if (loginButton != null) {
                loginButton.setEnabled(false);
            }

            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing progress", e);
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (loginButton != null) {
                loginButton.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding progress", e);
        }
    }

    private void showErrorMessage(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message", e);
        }
    }

    private void resetUiAfterLoginAttempt() {
        hideProgressDialog();
    }

    /**
     * Ensure we have test users in the database
     */
    private void ensureTestUsers() {
        // Add users to local database
        if (appDatabase != null && databaseExecutor != null) {
            databaseExecutor.execute(() -> {
                try {
                    List<User> users = appDatabase.userDao().getAllUsers();
                    if (users == null || users.isEmpty()) {
                        Log.d(TAG, "No users found, adding test users to local database");

                        // Add default users
                        appDatabase.userDao().insert(new User("Admin User", "admin", "admin", "admin"));
                        appDatabase.userDao().insert(new User("Manager User", "manager", "manager", "manager"));
                        appDatabase.userDao().insert(new User("Chef User", "chef", "chef", "chef"));
                        appDatabase.userDao().insert(new User("Waiter User", "waiter", "waiter", "waiter"));
                        appDatabase.userDao().insert(new User("Customer User", "customer", "customer", "customer"));

                        Log.d(TAG, "Added test users to local database");
                    } else {
                        Log.d(TAG, "Found " + users.size() + " users in local database");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking/adding test users to local database", e);
                }
            });
        }

        // Try to add users to Firebase if it's available
        if (FineDineApplication.isFirebaseInitialized()) {
            ensureFirebaseTestUsers();
        }
    }

    /**
     * Ensure we have test users in Firebase Auth
     */
    private void ensureFirebaseTestUsers() {
        if (!FineDineApplication.isFirebaseInitialized()) {
            Log.e(TAG, "Firebase not initialized, cannot create test users");
            return;
        }

        FirebaseAuth firebaseAuth = FineDineApplication.getFirebaseAuth();
        if (firebaseAuth == null) {
            Log.e(TAG, "Firebase Auth is null, cannot create test users");
            return;
        }

        // Define test user credentials
        String[][] testUsers = {
                {"admin@finedine.com", "admin123"},
                {"manager@finedine.com", "manager123"},
                {"chef@finedine.com", "chef123"},
                {"waiter@finedine.com", "waiter123"},
                {"customer@finedine.com", "customer123"}
        };

        // Try to create each test user
        for (String[] userData : testUsers) {
            final String email = userData[0];
            final String password = userData[1];

            // First try to sign in to check if user exists
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // User exists, sign out
                            Log.d(TAG, "Firebase test user exists: " + email);
                            firebaseAuth.signOut();
                        } else {
                            // User doesn't exist, create it
                            firebaseAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(createTask -> {
                                        if (createTask.isSuccessful()) {
                                            Log.d(TAG, "Created Firebase user: " + email);
                                            firebaseAuth.signOut();
                                        } else {
                                            Log.e(TAG, "Failed to create user: " + email, createTask.getException());
                                        }
                                    });
                        }
                    });
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

            // Start activity
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to role", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToRegister() {
        try {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to register", e);
            Toast.makeText(this, "Register screen coming soon", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToForgotPassword() {
        try {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to forgot password", e);
            Toast.makeText(this, "Password recovery coming soon", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reset and repopulate menu items database
     */
    private void resetMenuItemsDatabase() {
        if (appDatabase != null && databaseExecutor != null) {
            databaseExecutor.execute(() -> {
                try {
                    // First clear existing menu items to avoid duplicates
                    appDatabase.menuItemDao().deleteAll();
                    Log.d(TAG, "Cleared existing menu items");

                    // Add premium menu items with their images
                    MenuItem[] premiumItems = MenuItem.premiumMenu();
                    for (MenuItem item : premiumItems) {
                        appDatabase.menuItemDao().insert(item);
                    }

                    Log.d(TAG, "Successfully restored " + premiumItems.length + " menu items with images");

                } catch (Exception e) {
                    Log.e(TAG, "Error resetting menu items database", e);
                }
            });
        }
    }

    /**
     * Force restore menu items with UI feedback
     */
    private void forceRestoreMenuItems() {
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Restoring menu items...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (appDatabase != null && databaseExecutor != null) {
            databaseExecutor.execute(() -> {
                try {
                    // First clear existing menu items
                    appDatabase.menuItemDao().deleteAll();

                    // Add premium menu items with their images
                    MenuItem[] premiumItems = MenuItem.premiumMenu();
                    for (MenuItem item : premiumItems) {
                        appDatabase.menuItemDao().insert(item);
                    }

                    // Update UI on the main thread
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(this, "Successfully restored all menu items!", Toast.LENGTH_LONG).show();
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error force-restoring menu items database", e);
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(this, "Error restoring menu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "Database not initialized properly", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Display confirmation dialog before exiting app
        new android.app.AlertDialog.Builder(this)
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit Fine Dine?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Call super to handle back navigation properly
                    super.onBackPressed();
                    // Close the app
                    finishAffinity();
                })
                .setNegativeButton("No", null)
                .show();
    }
}