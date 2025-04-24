package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.utils.NetworkUtils;
import com.finedine.rms.utils.SharedPrefsManager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button createAccountButton;
    private ProgressBar progressBar;

    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        emailEditText = findViewById(R.id.emailInput);
        passwordEditText = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.registerText);
        progressBar = findViewById(R.id.progressBar);

        // Initialize SharedPrefsManager
        prefsManager = new SharedPrefsManager(this);

        // Set up click listeners
        loginButton.setOnClickListener(v -> attemptLogin());
        createAccountButton.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        // Check internet connection first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please try again when online.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Get input values
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();
        
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
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        try {
            // SIMPLIFIED LOGIN - Skip Firebase for demo
            // For testing purposes, detect the role from email
            String roleToUse = "customer"; // Default role

            if (email.contains("manager")) {
                roleToUse = "manager";
            } else if (email.contains("chef")) {
                roleToUse = "chef";
            } else if (email.contains("waiter")) {
                roleToUse = "waiter";
            }

            // Save the role in SharedPreferences
            prefsManager.setUserRole(roleToUse);

            // Show success message
            Toast.makeText(this, "Login successful as " + roleToUse, Toast.LENGTH_SHORT).show();

            // Navigate based on role
            navigateDirectly(roleToUse);
        } catch (Exception e) {
            // Log any errors
            Log.e(TAG, "Login failed: " + e.getMessage(), e);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            // Hide progress
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        }
    }

    private void navigateDirectly(String roleToUse) {
        try {
            Intent intent;

            if ("manager".equals(roleToUse)) {
                intent = new Intent(this, ManagerDashboardActivity.class);
            } else if ("chef".equals(roleToUse)) {
                intent = new Intent(this, KitchenActivity.class);
            } else if ("waiter".equals(roleToUse)) {
                intent = new Intent(this, OrderActivity.class);
            } else {
                // Default to OrderActivity for customers
                intent = new Intent(this, OrderActivity.class);
            }

            // Add the role info explicitly
            intent.putExtra("user_role", roleToUse);

            // Save login state
            prefsManager.setUserLoggedIn(true);
            prefsManager.setUserRole(roleToUse);

            // Log navigation attempt
            Log.d(TAG, "Navigating to role: " + roleToUse);

            // Start activity
            startActivity(intent);
            finish();
        } catch (Exception e) {
            // Log the error and show a helpful message
            Log.e(TAG, "Navigation failed: " + e.getMessage(), e);
            Toast.makeText(this, 
                    "Error starting app: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            
            // Reset login state on error
            prefsManager.setUserLoggedIn(false);
        }
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}