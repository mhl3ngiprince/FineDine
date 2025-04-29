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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.finedine.rms.utils.NetworkUtils;
import com.finedine.rms.utils.SharedPrefsManager;
import com.finedine.rms.utils.FirebaseSafetyWrapper;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button createAccountButton;
    private ProgressBar progressBar;
    private TextView forgotPasswordText;

    // Initialize these only when needed
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LoginActivity onCreate - starting");

        // Set content view first
        setContentView(R.layout.activity_login);
        Log.d(TAG, "LoginActivity - setContentView complete");

        try {
            // Initialize SharedPrefsManager
            prefsManager = new SharedPrefsManager(this);

            // Check if user is already logged in
            if (prefsManager.isUserLoggedIn()) {
                String role = prefsManager.getUserRole();
                Log.d(TAG, "User already logged in with role: " + role);
                navigateBasedOnRole(role);
                return;
            }

            // Initialize UI components first
            emailEditText = findViewById(R.id.emailInput);
            passwordEditText = findViewById(R.id.passwordInput);
            loginButton = findViewById(R.id.loginButton);
            createAccountButton = findViewById(R.id.registerText);
            progressBar = findViewById(R.id.progressBar);
            forgotPasswordText = findViewById(R.id.forgotPasswordText);

            // Hide progress bar initially
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            // Set up click listeners
            if (loginButton != null) {
                loginButton.setOnClickListener(v -> attemptLogin());
            } else {
                Log.e(TAG, "Login button is null!");
                Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            }

            if (createAccountButton != null) {
                createAccountButton.setOnClickListener(v -> navigateToRegister());
            } else {
                Log.e(TAG, "Create account button is null!");
            }

            if (forgotPasswordText != null) {
                forgotPasswordText.setOnClickListener(v -> attemptForgotPassword());
            } else {
                Log.e(TAG, "Forgot password text is null!");
            }

            // Check if user is already logged in
            if (prefsManager.isUserLoggedIn()) {
                String role = prefsManager.getUserRole();
                Log.d(TAG, "User already logged in with role: " + role);
                navigateBasedOnRole(role);
                return;
            }

            Log.d(TAG, "LoginActivity onCreate - completed successfully");

            // Pre-initialize Firebase safely
            mAuth = FirebaseSafetyWrapper.getAuthInstance(this);
            db = FirebaseSafetyWrapper.getFirestoreInstance(this);

        } catch (Exception e) {
            Log.e(TAG, "Error in LoginActivity onCreate", e);
            Toast.makeText(this, "Error initializing login screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToRegister() {
        try {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to register", e);
            Toast.makeText(this, "Could not open registration screen", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean initializeFirebaseSafely() {
        return FirebaseSafetyWrapper.initializeFirebase(this);
    }

    private void attemptLogin() {
        try {
            Log.d(TAG, "Attempting login");

            // Show progress
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Disable login button to prevent double-clicks
            if (loginButton != null) {
                loginButton.setEnabled(false);
            }

            // Validate form
            String email = emailEditText != null ? emailEditText.getText().toString().trim() : "";
            String password = passwordEditText != null ? passwordEditText.getText().toString().trim() : "";

            if (email.isEmpty()) {
                if (emailEditText != null) {
                    emailEditText.setError("Email is required");
                    emailEditText.requestFocus();
                }
                resetUiAfterLoginAttempt();
                return;
            }

            if (password.isEmpty()) {
                if (passwordEditText != null) {
                    passwordEditText.setError("Password is required");
                    passwordEditText.requestFocus();
                }
                resetUiAfterLoginAttempt();
                return;
            }

            // For demo purposes, allow default login
            if (email.equals("admin@finedine.com") && password.equals("admin123")) {
                handleDemoLogin("admin");
                return;
            } else if (email.equals("manager@finedine.com") && password.equals("manager123")) {
                handleDemoLogin("manager");
                return;
            } else if (email.equals("chef@finedine.com") && password.equals("chef123")) {
                handleDemoLogin("chef");
                return;
            } else if (email.equals("waiter@finedine.com") && password.equals("waiter123")) {
                handleDemoLogin("waiter");
                return;
            } else if (email.equals("customer@finedine.com") && password.equals("customer123")) {
                handleDemoLogin("customer");
                return;
            }

            // Initialize Firebase if needed
            mAuth = FirebaseSafetyWrapper.getAuthInstance(this);
            if (mAuth == null) {
                Toast.makeText(this, "Authentication service unavailable", Toast.LENGTH_SHORT).show();
                resetUiAfterLoginAttempt();
                return;
            }

            // Attempt login with Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        try {
                            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                fetchUserDataAndNavigate(user);
                            } else {
                                String errorMsg = "Authentication failed";
                                if (task.getException() != null) {
                                    errorMsg = task.getException().getMessage();
                                }
                                Log.w(TAG, "signInWithEmail:failure: " + errorMsg);
                                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                resetUiAfterLoginAttempt();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in login completion handler", e);
                            Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            resetUiAfterLoginAttempt();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "Login failure", e);
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetUiAfterLoginAttempt();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error attempting login", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetUiAfterLoginAttempt();
        }
    }

    private void attemptForgotPassword() {
        try {
            // Show dialog or activity for forgot password
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error attempting forgot password", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDemoLogin(String role) {
        // Save user session with demo data
        if (prefsManager != null) {
            prefsManager.saveUserSession(12345, role, role.substring(0, 1).toUpperCase() + role.substring(1));
            prefsManager.setUserLoggedIn(true);
            prefsManager.setUserRole(role);
        }

        // Navigate based on role
        navigateBasedOnRole(role);
    }

    private void resetUiAfterLoginAttempt() {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (loginButton != null) {
                loginButton.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting UI", e);
        }
    }

    private void fetchUserDataAndNavigate(FirebaseUser user) {
        try {
            if (user == null) {
                Log.e(TAG, "User is null");
                Toast.makeText(this, "Login error: Missing user data", Toast.LENGTH_SHORT).show();
                resetUiAfterLoginAttempt();
                return;
            }

            db = FirebaseSafetyWrapper.getFirestoreInstance(this);
            if (db == null) {
                // Fallback to a default role if Firestore isn't available
                saveUserSessionAndNavigate(user.getUid(), "customer", user.getDisplayName());
                return;
            }

            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        try {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    // Get user data
                                    String role = document.getString("role");
                                    String name = document.getString("name");

                                    if (role == null || role.isEmpty()) {
                                        role = "customer"; // Default role
                                    }

                                    if (name == null || name.isEmpty()) {
                                        name = "User";
                                    }

                                    saveUserSessionAndNavigate(user.getUid(), role, name);
                                } else {
                                    Log.w(TAG, "User document does not exist");
                                    // Default to customer role if no document exists
                                    saveUserSessionAndNavigate(user.getUid(), "customer", user.getDisplayName());
                                }
                            } else {
                                Log.w(TAG, "Failed to get user data");
                                // Default to customer role if fetch fails
                                saveUserSessionAndNavigate(user.getUid(), "customer", user.getDisplayName());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in fetchUserDataAndNavigate", e);
                            Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            resetUiAfterLoginAttempt();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching user data", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetUiAfterLoginAttempt();
        }
    }

    private void saveUserSessionAndNavigate(String userId, String role, String name) {
        // Save user session
        if (prefsManager != null) {
            prefsManager.saveUserSession(userId.hashCode(), role, name != null ? name : "User");
            prefsManager.setUserLoggedIn(true);
            prefsManager.setUserRole(role);
        }

        // Navigate based on role
        navigateBasedOnRole(role);
    }

    private void navigateBasedOnRole(String role) {
        try {
            Log.d(TAG, "Navigating based on role: " + role);

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

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error navigating based on role", e);
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetUiAfterLoginAttempt();
        }
    }
}