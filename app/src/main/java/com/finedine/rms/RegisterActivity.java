package com.finedine.rms;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import com.finedine.rms.utils.DialogUtils;
import com.finedine.rms.utils.FirebaseSafetyWrapper;
import com.finedine.rms.utils.InputValidator;
import com.finedine.rms.utils.NetworkUtils;
import com.finedine.rms.utils.RoleManager;
import com.finedine.rms.utils.SharedPrefsManager;

public class RegisterActivity extends AppCompatActivity {
    
    private static final String TAG = "RegisterActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText nameEditText;
    private EditText confirmPasswordEditText;
    private Spinner roleSpinner;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefsManager prefsManager;
    
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Starting RegisterActivity");
        setContentView(R.layout.activity_register);

        // Ensure the window background is properly set
        getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_luxury_pattern));
        Log.d(TAG, "Content view set successfully");

        // Initialize views first (so UI is visible even if Firebase fails)
        initializeViews();

        // Set up role spinner with user_roles array
        setupSpinner();

        // Initialize Firebase safely
        initializeFirebaseSafely();

        // Initialize SharedPrefsManager
        prefsManager = new SharedPrefsManager(this);

        // Set user as not logged in when entering register screen
        prefsManager.setUserLoggedIn(false);

        // Set up click listeners
        setupClickListeners();
    }

    /**
     * Initialize all views with null checks
     */
    private void initializeViews() {
        try {
            Log.d(TAG, "Initializing views");
            emailEditText = findViewById(R.id.emailInput);
            passwordEditText = findViewById(R.id.passwordInput);
            confirmPasswordEditText = findViewById(R.id.confirmPasswordInput);
            nameEditText = findViewById(R.id.nameInput);
            roleSpinner = findViewById(R.id.roleSpinner);
            registerButton = findViewById(R.id.registerButton);
            loginTextView = findViewById(R.id.loginTextView);
            progressBar = findViewById(R.id.progressBar);

            // Check for critical views
            if (emailEditText == null || passwordEditText == null ||
                    confirmPasswordEditText == null || registerButton == null) {
                Log.e(TAG, "Critical UI elements missing in RegisterActivity");
                throw new RuntimeException("Critical UI elements missing in RegisterActivity");
            }
            Log.d(TAG, "All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e; // Rethrow to be caught in onCreate
        }
    }

    /**
     * Set up the role spinner with error handling
     */
    private void setupSpinner() {
        try {
            if (roleSpinner == null) {
                Log.e(TAG, "Role spinner is null");
                return;
            }

            // Try to create adapter from resource
            ArrayAdapter<CharSequence> adapter;
            try {
                adapter = ArrayAdapter.createFromResource(this,
                        R.array.user_roles, android.R.layout.simple_spinner_item);
            } catch (Exception e) {
                Log.e(TAG, "Error creating adapter from resource", e);
                // Fallback to hardcoded array
                String[] roles = {"Customer", "Waiter", "Chef", "Manager", "Admin"};
                adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, roles);
            }

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            roleSpinner.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinner", e);
        }
    }

    /**
     * Set up click listeners for buttons
     */
    private void setupClickListeners() {
        try {
            if (registerButton != null) {
                registerButton.setOnClickListener(v -> attemptRegistration());
            }

            if (loginTextView != null) {
                loginTextView.setOnClickListener(v -> goToLogin());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    /**
     * Initialize Firebase safely to prevent crashes
     */
    private void initializeFirebaseSafely() {
        try {
            // Initialize Firebase components using the safety wrapper
            mAuth = FirebaseSafetyWrapper.getAuthInstance(this);
            db = FirebaseSafetyWrapper.getFirestoreInstance(this);

            if (mAuth == null || db == null) {
                Log.w(TAG, "Firebase initialization incomplete. Using local authentication only.");
                Toast.makeText(this,
                        "Some features may be limited due to authentication service issues.",
                        Toast.LENGTH_SHORT).show();
                // Don't disable register button - we'll handle this in attemptRegistration
            } else {
                Log.d(TAG, "Firebase authentication and Firestore initialized successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase components", e);
            Toast.makeText(this,
                    "Authentication services limited. Local mode only.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptRegistration() {
        try {
            // Verify critical components first
            if (emailEditText == null || passwordEditText == null ||
                    confirmPasswordEditText == null || nameEditText == null || roleSpinner == null) {
                Toast.makeText(this, "Registration form not properly initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get input values
            final String email = emailEditText.getText().toString().trim();
            final String password = passwordEditText.getText().toString().trim();
            final String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            final String name = nameEditText.getText().toString().trim();

            // Get spinner item safely
            String selectedRole;
            try {
                selectedRole = roleSpinner.getSelectedItem().toString();
                Log.d(TAG, "Selected role from spinner: " + selectedRole);
            } catch (Exception e) {
                Log.e(TAG, "Error getting selected role", e);
                selectedRole = "customer"; // Default role
            }

            // Input validation
            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                emailEditText.requestFocus();
                return;
            }

            // Allow simple usernames without email validation for demo accounts
            boolean isValidEmail = Patterns.EMAIL_ADDRESS.matcher(email).matches();
            boolean isSimpleUsername = email.equals("admin") || email.equals("manager") ||
                    email.equals("chef") || email.equals("waiter") || email.equals("customer");

            if (!isValidEmail && !isSimpleUsername) {
                emailEditText.setError("Please enter a valid email");
                emailEditText.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                passwordEditText.setError("Password is required");
                passwordEditText.requestFocus();
                return;
            }

            if (password.length() < 4) { // Reduced minimum length for demo accounts
                passwordEditText.setError("Password must be at least 4 characters");
                passwordEditText.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordEditText.setError("Passwords don't match");
                confirmPasswordEditText.requestFocus();
                return;
            }

            if (name.isEmpty()) {
                nameEditText.setError("Name is required");
                nameEditText.requestFocus();
                return;
            }

            // Show progress indicator
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (registerButton != null) {
                registerButton.setEnabled(false);
            }

            // Map the selected role to our constants
            final String roleToUse = mapSelectedRole(selectedRole);

            // Demo accounts handling - instant success
            if (isSimpleUsername || email.contains("finedine.com")) {
                // Register with same credentials for demo accounts
                Toast.makeText(this, "Creating demo account...", Toast.LENGTH_SHORT).show();

                // Small delay to simulate registration
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    registerLocally(email, password, name, roleToUse);
                }, 1500);
                return;
            }

            // Default registration approach
            if (mAuth != null) {
                registerWithFirebase(email, password, name, roleToUse);
            } else {
                // Fallback to local registration if Firebase is unavailable
                registerLocally(email, password, name, roleToUse);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in attemptRegistration", e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (registerButton != null) {
                registerButton.setEnabled(true);
            }
            Toast.makeText(this, "Registration error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void registerWithFirebase(String email, String password, String name, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration successful, get the user
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Save user data to Firestore
                        saveUserData(user, name, role);

                        // Save user info in SharedPreferences
                        if (prefsManager != null) {
                            String userId = user != null ? user.getUid() : "local-" + System.currentTimeMillis();
                            prefsManager.saveUserSession(userId.hashCode(), role, name, email);
                            prefsManager.setUserLoggedIn(true);
                            prefsManager.setUserRole(role);

                            // Also save credentials for login
                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.apply();
                        }

                        // Show success dialog instead of just a toast
                        new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("Registration Successful")
                                .setMessage("Your account has been created successfully! You will now be redirected to the appropriate screen.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    dialog.dismiss();
                                    // Navigate based on role after user acknowledges
                                    navigateBasedOnRole(role);
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        // If registration fails, display a message to the user
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());

                        // Hide progress and re-enable register button
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (registerButton != null) {
                            registerButton.setEnabled(true);
                        }

                        // Show error message
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerLocally(String email, String password, String name, String role) {
        try {
            // Save user info in SharedPreferences
            if (prefsManager != null) {
                String localUserId = "local-" + email.hashCode();
                prefsManager.saveUserSession(localUserId.hashCode(), role, name, email);
                prefsManager.setUserLoggedIn(true);
                prefsManager.setUserRole(role);

                // Also save credentials for login
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("email", email);
                editor.putString("password", password);
                editor.apply();
            }

            // Show success dialog instead of just a toast
            new AlertDialog.Builder(this)
                    .setTitle("Registration Successful")
                    .setMessage("Your account has been created successfully! You will now be redirected to the appropriate screen.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        // Navigate based on role after user acknowledges
                        navigateBasedOnRole(role);
                    })
                    .setCancelable(false)
                    .show();

            // Log success for debugging
            Log.d(TAG, "Successfully registered user: " + email + " with role: " + role);
        } catch (Exception e) {
            Log.e(TAG, "Error during local registration", e);
            Toast.makeText(this, "Registration error: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Reset UI
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (registerButton != null) {
                registerButton.setEnabled(true);
            }
        }
    }

    private String mapSelectedRole(String selectedRole) {
        if (selectedRole == null) {
            return "customer"; // Default if null
        }

        if (selectedRole.equalsIgnoreCase("Manager")) {
            return "manager";
        } else if (selectedRole.equalsIgnoreCase("Chef")) {
            return "chef";
        } else if (selectedRole.equalsIgnoreCase("Waiter")) {
            return "waiter";
        } else if (selectedRole.equalsIgnoreCase("Admin")) {
            return "admin";
        } else {
            return "customer"; // Default role
        }
    }

    private void saveUserData(FirebaseUser user, String name, String role) {
        if (user == null) {
            Log.w(TAG, "Failed to create user in Firebase");
            return;
        }

        try {
            // Create a User object
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", user.getUid());
            userData.put("name", name);
            userData.put("email", user.getEmail());
            userData.put("role", role);
            userData.put("createdAt", System.currentTimeMillis());

            // Save to Firestore
            db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created in Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user data in Firestore", e);
                });
        } catch (Exception e) {
            Log.e(TAG, "Error creating user data", e);
        }
    }

    private void navigateBasedOnRole(String role) {
        try {
            // Hide progress indicator
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (registerButton != null) {
                registerButton.setEnabled(true);
            }

            Log.d(TAG, "Navigating user to role-specific screen: " + role);

            // For chef role, use NavigationManager to ensure proper role handling
            if (role.equalsIgnoreCase("chef")) {
                NavigationManager.navigateBasedOnRole(this, role);
                return;
            }

            // Launch the appropriate activity
            Intent intent;

            switch (role.toLowerCase()) {
                case "manager":
                    intent = new Intent(this, ManagerDashboardActivity.class);
                    break;
                case "chef":
                    intent = new Intent(this, KitchenActivity.class);
                    break;
                case "waiter":
                    intent = new Intent(this, OrderActivity.class);
                    break;
                case "admin":
                    intent = new Intent(this, AdminActivity.class);
                    break;
                default:
                    intent = new Intent(this, ReservationActivity.class);
                    break;
            }

            // Add the role as an extra and clear flags
            intent.putExtra("user_role", role.toLowerCase());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Start the activity
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            Toast.makeText(this, "Error navigating: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Last resort: try NavigationManager for all roles
            try {
                NavigationManager.navigateBasedOnRole(this, role);
            } catch (Exception ne) {
                Log.e(TAG, "Even NavigationManager failed", ne);
            }
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Show a custom dialog confirming navigation to login screen
        new AlertDialog.Builder(this)
                .setTitle("Back to Login")
                .setMessage("Go back to the login screen?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    super.onBackPressed();
                    goToLogin();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release any resources if needed
        mAuth = null;
        db = null;
    }
}