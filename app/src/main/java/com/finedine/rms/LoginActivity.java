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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private TextView forgotPasswordText;
    private ProgressBar progressBar;
    private SharedPrefsManager prefsManager;
    private String email;  // Store email for later use

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "LoginActivity onCreate starting");

            // Set the layout
            setContentView(R.layout.activity_login);
            Log.d(TAG, "LoginActivity - setContentView complete");

            // Initialize SharedPrefsManager
            prefsManager = new SharedPrefsManager(this);

            // Log that we're staying on the login screen
            Log.d(TAG, "Showing login screen regardless of login state");

            // Initialize UI components
            emailEditText = findViewById(R.id.emailInput);
            passwordEditText = findViewById(R.id.passwordInput);
            loginButton = findViewById(R.id.loginButton);
            registerButton = findViewById(R.id.registerText);
            forgotPasswordText = findViewById(R.id.forgotPasswordText);
            progressBar = findViewById(R.id.progressBar);

            // Hide progress initially
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

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

            Log.d(TAG, "LoginActivity setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error in LoginActivity onCreate", e);
            Toast.makeText(this, "Error initializing login: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                return;
            }

            if (password.isEmpty()) {
                passwordEditText.setError("Password is required");
                return;
            }

            authenticateUser(email, password);
        } catch (Exception e) {
            Log.e(TAG, "Error attempting login", e);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void authenticateUser(String email, String password) {
        try {
            // If Firebase is not available, use local authentication as fallback
            if (!FineDineApplication.isFirebaseInitialized()) {
                Log.w(TAG, "Firebase not initialized, using local authentication");
                authenticateLocally(email, password);
                return;
            }

            // Get Firebase Auth instance
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth == null) {
                Log.e(TAG, "Firebase Auth instance is null, using local authentication");
                authenticateLocally(email, password);
                return;
            }

            showProgressDialog("Authenticating...");

            // Attempt Firebase authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        try {
                            hideProgressDialog();
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase authentication successful");
                                // Get user ID from Firebase
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    getUserRole(user.getUid(), user.getEmail());
                                } else {
                                    Log.e(TAG, "Firebase user is null after successful login");
                                    showErrorMessage("Login failed, please try again");
                                    authenticateLocally(email, password);
                                }
                            } else {
                                Log.w(TAG, "Firebase authentication failed", task.getException());
                                showErrorMessage("Authentication failed: " +
                                        (task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error"));
                                // Try local authentication as fallback
                                authenticateLocally(email, password);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in Firebase auth callback", e);
                            hideProgressDialog();
                            showErrorMessage("Authentication error: " + e.getLocalizedMessage());
                            authenticateLocally(email, password);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error during authentication", e);
            hideProgressDialog();
            showErrorMessage("Authentication error: " + e.getLocalizedMessage());
            authenticateLocally(email, password);
        }
    }

    private void authenticateLocally(String email, String password) {
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

        // Invalid credentials
        resetUiAfterLoginAttempt();
        Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
    }

    private void getUserRole(String uid, String email) {
        // Determine role based on email prefix for Firebase users
        String role = "customer";

        if (email != null) {
            email = email.toLowerCase();
            if (email.startsWith("admin")) {
                role = "admin";
            } else if (email.startsWith("manager")) {
                role = "manager";
            } else if (email.startsWith("chef")) {
                role = "chef";
            } else if (email.startsWith("waiter")) {
                role = "waiter";
            }
        }

        handleSuccessfulLogin(role);
    }

    private void handleSuccessfulLogin(String role) {
        try {
            Log.d(TAG, "Login successful for role: " + role);

            // Hide progress
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            // Store user session
            SharedPrefsManager prefsManager = new SharedPrefsManager(this);
            prefsManager.saveUserSession(1, role, email != null ? email : "User");

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

            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error handling successful login", e);
            Toast.makeText(this, "Error navigating to next screen", Toast.LENGTH_SHORT).show();
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
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to register", e);
            Toast.makeText(this, "Register screen coming soon", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToForgotPassword() {
        try {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to forgot password", e);
            Toast.makeText(this, "Password recovery coming soon", Toast.LENGTH_SHORT).show();
        }
    }
}