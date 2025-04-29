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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private TextView forgotPasswordText;
    private ProgressBar progressBar;
    private SharedPrefsManager prefsManager;

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

            // Check if already logged in
            if (prefsManager != null && prefsManager.isUserLoggedIn()) {
                String role = prefsManager.getUserRole();
                Log.d(TAG, "User already logged in as: " + role);
                navigateBasedOnRole(role);
                return;
            }

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
            String email = emailEditText.getText().toString().trim();
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

            // Show progress
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Disable login button
            loginButton.setEnabled(false);

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
            } else if (email.contains("@") && password.length() >= 4) {
                // Any valid email format with password >= 4 chars
                handleSuccessfulLogin("customer");
                return;
            }

            // Invalid credentials
            resetUiAfterLoginAttempt();
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error attempting login", e);
            resetUiAfterLoginAttempt();
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetUiAfterLoginAttempt() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (loginButton != null) {
            loginButton.setEnabled(true);
        }
    }

    private void handleSuccessfulLogin(String role) {
        try {
            // Save login session
            if (prefsManager != null) {
                prefsManager.saveUserSession(123, role, "User");
                prefsManager.setUserLoggedIn(true);
                prefsManager.setUserRole(role);
            }

            // Navigate to appropriate screen
            navigateBasedOnRole(role);
        } catch (Exception e) {
            Log.e(TAG, "Error handling successful login", e);
            resetUiAfterLoginAttempt();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    intent = new Intent(this, ReservationActivity.class);
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