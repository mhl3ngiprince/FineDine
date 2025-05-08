package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Simple standalone login activity
 */
public class LoginDemoActivity extends BaseActivity {

    private static final String TAG = "LoginDemoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "LoginDemoActivity creating");

        try {
            // Setup modern navigation panel
            setupModernNavigationPanel("Login", R.layout.activity_login);

            // Find views
            EditText emailInput = findViewById(R.id.emailInput);
            EditText passwordInput = findViewById(R.id.passwordInput);
            Button loginButton = findViewById(R.id.loginButton);
            Button registerButton = findViewById(R.id.registerText);
            TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

            // Set click listeners
            if (loginButton != null) {
                loginButton.setOnClickListener(v -> {
                    String email = emailInput.getText().toString().trim();
                    String password = passwordInput.getText().toString().trim();

                    // Validate inputs
                    if (email.isEmpty()) {
                        emailInput.setError("Please enter email");
                        return;
                    }

                    if (password.isEmpty()) {
                        passwordInput.setError("Please enter password");
                        return;
                    }

                    Toast.makeText(LoginDemoActivity.this,
                            "Login successful with: " + email, Toast.LENGTH_SHORT).show();

                    // Handle demo credentials
                    navigateBasedOnCredentials(email, password);
                });
            }

            if (registerButton != null) {
                registerButton.setOnClickListener(v -> {
                    Toast.makeText(this, "Register clicked", Toast.LENGTH_SHORT).show();
                    // Navigate to register activity if it exists
                    try {
                        Intent intent = new Intent(this, RegisterActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to register: " + e.getMessage());
                        Toast.makeText(this, "Registration coming soon!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (forgotPasswordText != null) {
                forgotPasswordText.setOnClickListener(v -> {
                    Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show();
                    // Navigate to forgot password activity if it exists
                    try {
                        Intent intent = new Intent(this, ForgotPasswordActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to forgot password: " + e.getMessage());
                        Toast.makeText(this, "Password recovery coming soon!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            Log.d(TAG, "LoginDemoActivity setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up login: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void navigateBasedOnCredentials(String email, String password) {
        try {
            // Demo credentials for different roles
            if (email.equals("admin") && password.equals("admin")) {
                navigateToRole("admin");
            } else if (email.equals("manager") && password.equals("manager")) {
                navigateToRole("manager");
            } else if (email.equals("chef") && password.equals("chef")) {
                navigateToRole("chef");
            } else if (email.equals("waiter") && password.equals("waiter")) {
                navigateToRole("waiter");
            } else if (email.contains("@") && password.length() >= 3) {
                // Any reasonable email/password is a customer
                navigateToRole("customer");
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Try: admin/admin, manager/manager, chef/chef, or waiter/waiter", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in credential processing: " + e.getMessage(), e);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToRole(String role) {
        try {
            Log.d(TAG, "Navigating to role: " + role);
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
                    intent = new Intent(this, ReservationActivity.class);
                    break;
            }

            // Save role in intent
            intent.putExtra("user_role", role);

            // Start activity
            startActivity(intent);

            // Show success toast
            Toast.makeText(this, "Logged in as: " + role, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            Toast.makeText(this,
                    "Cannot navigate to " + role + " screen: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}