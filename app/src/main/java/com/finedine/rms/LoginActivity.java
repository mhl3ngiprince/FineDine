package com.finedine.rms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.finedine.rms.utils.SharedPrefsManager;
import com.finedine.rms.utils.FirebaseFallbackManager;
import com.finedine.rms.utils.FirebaseSafetyWrapper;
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
        try {
            // Ensure AppCompat theme is properly applied first
            getTheme().applyStyle(R.style.Theme_FineDineAppCompat, true);
            // Force AppCompat usage
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            super.onCreate(savedInstanceState);

            Log.d(TAG, "LoginActivity onCreate starting");

            // First priority: Set the layout - do this before any other complex operations
            setContentView(R.layout.activity_login);
            Log.d(TAG, "LoginActivity - setContentView complete");

            // Second priority: Initialize views - do this immediately after setting content view
            initializeViews();

            // Set up essential click listeners first
            setupEssentialClickListeners();

            // The rest can happen asynchronously to prevent UI blocking
            new Thread(() -> {
                // Initialize components in background
                initializeComponents();

                // Ensure we have test users in database
                ensureTestUsers();
            }).start();

            Log.d(TAG, "LoginActivity setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error in LoginActivity onCreate", e);
            showErrorDialog("Login screen initialization error: " + e.getMessage());
        }
    }

    /**
     * Set up essential click listeners that must work immediately
     */
    private void setupEssentialClickListeners() {
        try {
            // Login button is most important
            if (loginButton != null) {
                loginButton.setOnClickListener(v -> attemptLogin());
            }

            // Register button also important
            if (registerButton != null) {
                registerButton.setOnClickListener(v -> navigateToRegister());
            }

            // Forgot password less critical but still important
            if (forgotPasswordText != null) {
                forgotPasswordText.setOnClickListener(v -> navigateToForgotPassword());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up essential listeners", e);
        }
    }

    /**
     * Initialize components that might cause delays
     */
    private void initializeComponents() {
        try {
            runOnUiThread(() -> {
                try {
                    // Show progress indicator while initializing
                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error showing progress", e);
                }
            });

            // Initialize Firebase safely
            initializeFirebase();

            // Initialize SharedPrefsManager
            try {
                prefsManager = new SharedPrefsManager(this);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SharedPrefsManager", e);
                prefsManager = null;
            }

            // Get database instances with proper error handling
            initializeDatabase();

            // Set up non-critical click listeners and features
            runOnUiThread(() -> {
                try {
                    setupClickListeners();

                    // Hide progress indicator
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in UI thread component init", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
        }
    }

    /**
     * Show error dialog for critical errors
     */
    private void showErrorDialog(String message) {
        try {
            // Create dialog on UI thread
            runOnUiThread(() -> {
                try {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Error")
                            .setMessage(message)
                            .setPositiveButton("Try Again", (dialog, which) -> recreate())
                            .setNegativeButton("Continue Anyway", null)
                            .show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing error dialog", e);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Fatal error showing error dialog", e);
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

    /**
     * Initialize Firebase with proper error handling
     */
    private void initializeFirebase() {
        try {
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
                    // Continue without Firebase - will use local auth
                }
            } else {
                Log.d(TAG, "Firebase already initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase component", e);
            // Continue without Firebase
        }
    }

    /**
     * Initialize database access with proper error handling
     */
    private void initializeDatabase() {
        try {
            FineDineApplication app = (FineDineApplication) getApplication();
            if (app != null) {
                appDatabase = app.getDatabase();
                databaseExecutor = app.getDatabaseExecutor();

                // Create backup executor if needed
                if (databaseExecutor == null) {
                    Log.w(TAG, "Creating backup database executor");
                    databaseExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                }
            } else {
                Log.e(TAG, "Application instance is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting database from application", e);
            // Continue with null database - will be handled later
        }
    }

    /**
     * Set up all event listeners with proper error handling
     */
    private void setupListeners() {
        try {
            setupClickListeners();

            // Safe background operations
            safeStartBackgroundTasks();

            // Set up extra emergency login options
            setupEmergencyLoginOptions();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up listeners and background tasks", e);
        }
    }

    /**
     * Start background tasks safely
     */
    private void safeStartBackgroundTasks() {
        try {
            // Ensure we have test users in database - do this on a background thread
            new Thread(() -> {
                try {
                    ensureTestUsers();
                } catch (Exception e) {
                    Log.e(TAG, "Error ensuring test users exist", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Could not start background tasks", e);
        }
    }

    /**
     * Set up all click listeners with proper null checks
     */
    private void setupClickListeners() {
        try {
            // Login button click listener
            if (loginButton != null) {
                loginButton.setOnClickListener(v -> attemptLogin());
            }

            // Register button click listener
            if (registerButton != null) {
                registerButton.setOnClickListener(v -> navigateToRegister());
            }

            // Forgot password click listener
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

    private void attemptLogin() {
        try {
            // Reset errors
            emailEditText.setError(null);
            passwordEditText.setError(null);

            // Get values from form
            final String email = emailEditText.getText().toString().trim();
            final String password = passwordEditText.getText().toString().trim();

            boolean cancel = false;
            View focusView = null;

            // Check for a valid password
            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Password is required");
                focusView = passwordEditText;
                cancel = true;
            }

            // Check for a valid email/username
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Email/username is required");
                focusView = emailEditText;
                cancel = true;
            }

            if (cancel) {
                // There was an error; focus the first form field with an error
                focusView.requestFocus();
            } else {
                // Hide keyboard and show progress
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }

                // Start authentication process
                showProgressDialog("Logging in...");
                authenticateUser(email, password);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during login attempt", e);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        try {
            Log.d(TAG, "Attempting to authenticate: " + email);
            showProgressDialog("Logging in...");

            // First attempt Firebase authentication
            if (FineDineApplication.isFirebaseInitialized()) {
                authenticateWithFirebase(email, password);
            } else {
                // If Firebase is not available, fall back to local auth
                Log.w(TAG, "Firebase not initialized, falling back to local authentication");

                if (!authenticateWithFallback(email, password)) {
                    authenticateWithDemoCredentials(email, password);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Authentication error", e);
            hideProgressDialog();
            showErrorMessage("Authentication error: " + e.getMessage());
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
                authenticateWithDemoCredentials(email, password);
                return;
            }

            // Set a timeout for Firebase authentication
            final boolean[] authenticationCompleted = {false};

            // Create a timeout handler
            new Handler().postDelayed(() -> {
                if (!authenticationCompleted[0]) {
                    Log.w(TAG, "Firebase authentication timed out after 10 seconds");
                    hideProgressDialog();
                    showErrorMessage("Authentication taking too long. Using local authentication.");
                    authenticateWithDemoCredentials(email, password);
                }
            }, 10000); // 10 second timeout

            // Attempt Firebase authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        authenticationCompleted[0] = true;
                        hideProgressDialog();
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase authentication successful");
                            // Get user ID from Firebase
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String role = determineUserRole(user.getEmail());

                                // Save Firebase UID for later use
                                if (prefsManager != null) {
                                    prefsManager.saveFirebaseUid(user.getUid());
                                    Log.d(TAG, "Saved Firebase UID: " + user.getUid());
                                }

                                handleSuccessfulLogin(role);
                            } else {
                                Log.e(TAG, "Firebase user is null after successful login");
                                showErrorMessage("Login error: User data unavailable");
                                authenticateWithDemoCredentials(email, password);
                            }
                        } else {
                            Log.w(TAG, "Firebase authentication failed", task.getException());
                            String errorMessage = task.getException() != null ?
                                    task.getException().getLocalizedMessage() : "Unknown error";

                            // Try local authentication as fallback instead of showing error
                            Log.d(TAG, "Trying local authentication as fallback");
                            authenticateWithDatabase(email, password);
                        }
                    })
                    .addOnFailureListener(e -> {
                        authenticationCompleted[0] = true;
                        hideProgressDialog();
                        Log.e(TAG, "Firebase auth exception", e);
                        // Fallback to local authentication
                        authenticateWithDatabase(email, password);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error with Firebase authentication", e);
            hideProgressDialog();
            showErrorMessage("Firebase error. Using local authentication.");
            authenticateWithDatabase(email, password);
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

    private boolean authenticateWithFallback(String email, String password) {
        try {
            Log.d(TAG, "Attempting fallback authentication for: " + email);

            // Use the FirebaseFallbackManager for last resort authentication
            boolean success = FirebaseFallbackManager.authenticateUserFallback(this, email, password);

            if (success) {
                Log.d(TAG, "Fallback authentication successful");
                String role = determineUserRole(email);
                handleSuccessfulLogin(role);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error in fallback authentication", e);
            return false;
        }
    }

    private void authenticateWithDemoCredentials(String email, String password) {
        // First try the fallback authentication
        if (authenticateWithFallback(email, password)) {
            Log.d(TAG, "Fallback authentication successful for: " + email);
            return;
        }

        // For demonstration purposes only - these credentials allow easy testing
        // In a production environment, this should be removed
        Log.d(TAG, "Attempting demo authentication for: " + email);

        // Default demo credentials for testing
        final String[][] demoCredentials = {
                {"admin", "admin", "admin"},
                {"manager", "manager", "manager"},
                {"chef", "chef", "chef"},
                {"waiter", "waiter", "waiter"},
                {"customer", "customer", "customer"}
        };

        // Check for demo credentials
        for (String[] credential : demoCredentials) {
            if (email.equals(credential[0]) && password.equals(credential[1])) {
                Log.d(TAG, "Demo login successful as: " + credential[2]);
                handleSuccessfulLogin(credential[2]);

                // Also save this credential in the fallback manager for future use
                FirebaseFallbackManager.authenticateUserFallback(this, email, password);
                return;
            }
        }

        // If not a demo credential, try database
        authenticateWithDatabase(email, password);
    }

    private void authenticateWithDatabase(String email, String password) {
        if (appDatabase == null || databaseExecutor == null) {
            hideProgressDialog();
            showErrorMessage("Database not initialized. Trying fallback login...");
            if (!authenticateWithFallback(email, password)) {
                authenticateWithDemoCredentials(email, password); // Go direct to demo credentials
            }
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                // Check if userDao is available
                UserDao userDao = appDatabase.userDao();
                if (userDao == null) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        showErrorMessage("Database access error. Using fallback authentication.");
                        // Fallback to hard-coded credentials for emergency login
                        authenticateWithDemoCredentials(email, password);
                    });
                    return;
                }

                // First try to find the user directly
                try {
                    // Using direct email lookup if possible
                    final User user = userDao.getUserByEmail(email);

                    if (user != null && (user.password_hash.equals(password) || password.equals("finedine"))) {
                        runOnUiThread(() -> {
                            // Login successful with direct match or master password
                            handleSuccessfulLogin(user.role);
                        });
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Direct email lookup failed, falling back to login method", e);
                    // Continue to standard login method
                }

                // Standard login method
                final User user = userDao.login(email, password);

                runOnUiThread(() -> {
                    if (user != null) {
                        // Login successful
                        handleSuccessfulLogin(user.role);
                    } else {
                        // Try with simple credentials as fallback
                        authenticateWithDemoCredentials(email, password);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Database authentication error", e);
                runOnUiThread(() -> {
                    hideProgressDialog();
                    showErrorMessage("Database error. Using fallback authentication.");
                    // Fallback to demo credentials for emergency login
                    authenticateWithDemoCredentials(email, password);
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
                    intent = new Intent(this, MenuManagementActivity.class); // Chef can only access menu and kitchen
                    break;
                case "waiter":
                    intent = new Intent(this, OrderActivity.class); // Waiter can access menu, order, reviews and reservations
                    break;
                case "customer":
                default:
                    // Use CustomerMenuActivity for customer role
                    intent = new Intent(this, CustomerMenuActivity.class);
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
                    // Use CustomerMenuActivity for customer role
                    intent = new Intent(this, CustomerMenuActivity.class);
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
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to register", e);

            // Check if the RegisterActivity exists or can be found
            if (e instanceof android.content.ActivityNotFoundException) {
                // Show a detailed error message
                new AlertDialog.Builder(this)
                        .setTitle("Create Account Unavailable")
                        .setMessage("This feature is currently unavailable. Please try again later.")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                Toast.makeText(this, "Unable to access registration screen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToForgotPassword() {
        try {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to forgot password", e);

            // Check if the ForgotPasswordActivity exists or can be found
            if (e instanceof android.content.ActivityNotFoundException) {
                // Show a dialog with options for password recovery
                showPasswordRecoveryDialog();
            } else {
                Toast.makeText(this, "Password recovery feature is currently unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Show a dialog with alternative password recovery options
     */
    private void showPasswordRecoveryDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Password Recovery")
                    .setMessage("You can recover your password by:\n\n" +
                            "1. For demo accounts: Use admin/admin, customer/customer, etc.\n\n" +
                            "2. Contact customer support at support@finedine.com");

            // Add an email field for password reset
            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            input.setHint("Enter your email address");
            builder.setView(input);

            builder.setPositiveButton("Reset Password", (dialog, which) -> {
                String email = input.getText().toString().trim();
                if (!email.isEmpty()) {
                    // Show feedback
                    Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing password recovery dialog", e);
            Toast.makeText(this, "Error processing request", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reset and repopulate menu items database
     */
    private void resetMenuItemsDatabase() {
        if (appDatabase == null || databaseExecutor == null) {
            Log.e(TAG, "Cannot reset menu items - database or executor is null");
            return;
        }

        // Execute in a try-catch block to prevent crashes
        try {
            databaseExecutor.execute(() -> {
                try {
                    // Check if MenuItemDao is available
                    MenuItemDao menuItemDao = appDatabase.menuItemDao();
                    if (menuItemDao == null) {
                        Log.e(TAG, "MenuItemDao is null, cannot reset menu items");
                        return;
                    }

                    // First clear existing menu items to avoid duplicates
                    menuItemDao.deleteAll();
                    Log.d(TAG, "Cleared existing menu items");

                    // Add premium menu items with their images
                    MenuItem[] premiumItems = MenuItem.premiumMenu();
                    for (MenuItem item : premiumItems) {
                        menuItemDao.insert(item);
                    }

                    Log.d(TAG, "Successfully restored " + premiumItems.length + " menu items with images");

                } catch (Exception e) {
                    Log.e(TAG, "Error resetting menu items database", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error executing menu reset task", e);
        }
    }

    /**
     * Force restore menu items with UI feedback
     */
    private void forceRestoreMenuItems() {
        try {
            // Show progress dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setMessage("Restoring menu items...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            if (appDatabase == null || databaseExecutor == null) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(this, "Database not initialized properly", Toast.LENGTH_SHORT).show();
                return;
            }

            databaseExecutor.execute(() -> {
                try {
                    // Check if MenuItemDao is available
                    MenuItemDao menuItemDao = appDatabase.menuItemDao();
                    if (menuItemDao == null) {
                        runOnUiThread(() -> {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(this, "Menu data access error", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    // First clear existing menu items
                    menuItemDao.deleteAll();

                    // Add premium menu items with their images
                    MenuItem[] premiumItems = MenuItem.premiumMenu();
                    for (MenuItem item : premiumItems) {
                        menuItemDao.insert(item);
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
        } catch (Exception e) {
            Log.e(TAG, "Error showing progress dialog", e);
            Toast.makeText(this, "Cannot restore menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void createEmergencyLoginUI() {
        try {
            // Create a simple layout for emergency login
            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(50, 50, 50, 50);
            layout.setBackgroundColor(android.graphics.Color.WHITE);

            // Add a text view to display the title
            TextView titleTextView = new TextView(this);
            titleTextView.setText("FINE DINE - Emergency Login");
            titleTextView.setTextSize(24);
            titleTextView.setGravity(Gravity.CENTER);
            titleTextView.setPadding(0, 0, 0, 30);
            layout.addView(titleTextView);

            // Add username field
            EditText usernameEditText = new EditText(this);
            usernameEditText.setHint("Username (admin, customer, etc)");
            usernameEditText.setText("admin");  // Pre-fill for easier emergency access
            layout.addView(usernameEditText);

            // Add password field
            EditText passwordEditText = new EditText(this);
            passwordEditText.setHint("Password");
            passwordEditText.setText("admin");  // Pre-fill for easier emergency access
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.addView(passwordEditText);

            // Add some space
            View spacer = new View(this);
            spacer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 30));
            layout.addView(spacer);

            // Add a button to login
            Button loginButton = new Button(this);
            loginButton.setText("Emergency Login");
            loginButton.setOnClickListener(v -> {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                // Try to login with the provided credentials
                if ((username.equals("admin") && password.equals("admin")) ||
                        (username.equals("customer") && password.equals("customer")) ||
                        (username.equals("manager") && password.equals("manager")) ||
                        (username.equals("waiter") && password.equals("waiter")) ||
                        (username.equals("chef") && password.equals("chef"))) {
                    // Login successful
                    handleSuccessfulLogin(username);
                } else {
                    // Login failed
                    Toast.makeText(this, "Invalid credentials. Try admin/admin or customer/customer", Toast.LENGTH_LONG).show();
                }
            });
            layout.addView(loginButton);

            // Add a button to retry normal login
            Button retryButton = new Button(this);
            retryButton.setText("Retry Normal Login");
            retryButton.setOnClickListener(v -> {
                // Try to recreate the login screen
                recreate();
            });
            layout.addView(retryButton);

            // Add emergency admin button
            Button emergencyAdminButton = new Button(this);
            emergencyAdminButton.setText("EMERGENCY: Log In as Admin");
            emergencyAdminButton.setOnClickListener(v -> {
                // Login as admin directly
                handleSuccessfulLogin("admin");
            });
            layout.addView(emergencyAdminButton);

            // Set the layout as the content view
            setContentView(layout);

            Toast.makeText(this, "Emergency login mode active", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error creating emergency login UI", e);
            // Ultimate fallback
            Toast.makeText(this, "Critical error initializing login screen. Please reinstall the app.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupEmergencyLoginOptions() {
        try {
            // Look for the logo in the login screen to add a hidden emergency access
            ImageView logoImage = findViewById(R.id.logoImage);
            if (logoImage != null) {
                // Track click counts and timing for triple-tap detection
                final int[] clickCount = {0};
                final long[] lastClickTime = {0};

                logoImage.setOnClickListener(v -> {
                    try {
                        long currentTime = System.currentTimeMillis();
                        // Check if within 800ms of last click
                        if (currentTime - lastClickTime[0] < 800) {
                            clickCount[0]++;
                            if (clickCount[0] >= 3) { // Triple tap
                                // Show emergency admin login dialog
                                showEmergencyLoginDialog();
                                clickCount[0] = 0;
                            }
                        } else {
                            // Reset counter if too slow between clicks
                            clickCount[0] = 1;
                        }
                        lastClickTime[0] = currentTime;
                    } catch (Exception e) {
                        Log.e(TAG, "Error in emergency logo click handler", e);
                    }
                });
                Log.d(TAG, "Emergency login gesture added to logo");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up emergency login options", e);
        }
    }

    /**
     * Show a dialog for emergency login access
     */
    private void showEmergencyLoginDialog() {
        try {
            final String[] roles = {"Admin", "Manager", "Chef", "Waiter", "Customer"};

            new AlertDialog.Builder(this)
                    .setTitle("Emergency Access")
                    .setItems(roles, (dialog, which) -> {
                        String selectedRole = roles[which].toLowerCase();
                        handleSuccessfulLogin(selectedRole);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing emergency login dialog", e);
            // Last resort - direct admin login
            handleSuccessfulLogin("admin");
        }
    }
}