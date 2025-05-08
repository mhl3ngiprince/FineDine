package com.finedine.rms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.finedine.rms.utils.FirebaseFallbackManager;
import com.finedine.rms.utils.SharedPrefsManager;
import com.finedine.rms.NavigationManager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private TextView forgotPasswordText;
    private ProgressBar progressBar;
    private CheckBox rememberMeCheckbox;
    private ViewGroup loginContainer;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            getTheme().applyStyle(R.style.Theme_FineDineAppCompat, true);
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_login);

            // Initialize views
            initializeViews();

            // Set up click listeners
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in LoginActivity onCreate", e);
            Toast.makeText(this, "Login screen initialization error", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        try {
            // Initialize UI components
            emailEditText = findViewById(R.id.emailInput);
            passwordEditText = findViewById(R.id.passwordInput);
            loginButton = findViewById(R.id.loginButton);
            forgotPasswordText = findViewById(R.id.forgotPasswordText);
            registerButton = findViewById(R.id.registerText);
            progressBar = findViewById(R.id.progressBar);
            rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox);

            // Properly get the content view as ViewGroup
            loginContainer = (ViewGroup) findViewById(android.R.id.content);

            // Hide progress initially
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up views", e);
        }
    }

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
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    private void attemptLogin() {
        try {
            // Get values from form
            final String email = emailEditText.getText().toString().trim();
            final String password = passwordEditText.getText().toString().trim();

            // Save credentials if "Remember Me" is checked
            if (rememberMeCheckbox != null && rememberMeCheckbox.isChecked()) {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("remember_me", true);
                editor.putString("email", email);
                editor.putString("password", password);
                editor.apply();
            }

            // Authenticate user
            authenticateUser(email, password);

        } catch (Exception e) {
            Log.e(TAG, "Error during login attempt", e);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void authenticateUser(String email, String password) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // First try with demo credentials
            if (authenticateWithDemoCredentials(email, password)) {
                // Determine user role from email
                String role = determineUserRole(email);
                handleSuccessfulLogin(role);
                return;
            }

            // Then try with Firebase fallback if demo fails
            try {
                boolean firebaseResult = FirebaseFallbackManager.authenticateUserFallback(this, email, password);
                if (firebaseResult) {
                    String role = determineUserRole(email);
                    handleSuccessfulLogin(role);
                    return;
                }
            } catch (Exception firebaseEx) {
                Log.w(TAG, "Firebase authentication failed, continuing with local check", firebaseEx);
            }

            // Check if user exists in SharedPreferences
            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String savedEmail = userPrefs.getString("email", "");
            String savedPassword = userPrefs.getString("password", "");

            if (email.equals(savedEmail) && password.equals(savedPassword)) {
                String role = determineUserRole(email);
                handleSuccessfulLogin(role);
                return;
            }

            // All authentication methods failed
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Invalid credentials. Please check your email and password.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Authentication failed for: " + email);

        } catch (Exception e) {
            Log.e(TAG, "Authentication error", e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Error during login: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean authenticateWithDemoCredentials(String email, String password) {
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
                return true;
            }
        }
        return false;
    }

    private String determineUserRole(String email) {
        if (email == null) return "customer";

        email = email.toLowerCase();
        String role = "customer"; // Default role

        if (email.equals("chef") || email.equals("chef@finedine.com") || email.contains("chef")) {
            role = "chef";
        } else if (email.equals("admin") || email.equals("admin@finedine.com") || email.contains("admin")) {
            role = "admin";
        } else if (email.equals("manager") || email.equals("manager@finedine.com") || email.contains("manager")) {
            role = "manager";
        } else if (email.equals("waiter") || email.equals("waiter@finedine.com") || email.contains("waiter")) {
            role = "waiter";
        }

        Log.d(TAG, "Determined role '" + role + "' for email: " + email);
        return role;
    }

    private void handleSuccessfulLogin(String role) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            // Store user session
            if (prefsManager == null) {
                prefsManager = new SharedPrefsManager(this);
            }
            String email = emailEditText.getText().toString();
            String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;

            // Confirm the role in logs
            Log.d(TAG, "Login successful, using role: " + role);

            // Save role in preferences before navigation
            prefsManager.saveUserSession(email.hashCode(), role, name, email);
            prefsManager.setUserLoggedIn(true);
            prefsManager.setUserRole(role);

            // Special handling for chef role
            if (role.equalsIgnoreCase("chef")) {
                try {
                    Log.d(TAG, "Navigating directly to KitchenActivity for chef role");
                    Intent intent = new Intent(this, KitchenActivity.class);
                    intent.putExtra("user_role", "chef");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Direct navigation to KitchenActivity failed, trying NavigationManager", e);
                }
            }

            // Use NavigationManager for standard navigation
            NavigationManager.navigateBasedOnRole(this, role);

        } catch (Exception e) {
            Log.e(TAG, "Error handling successful login: " + e.getMessage(), e);

            // Add debug info about the specific error
            if (e instanceof android.content.ActivityNotFoundException) {
                Log.e(TAG, "ActivityNotFoundException - the activity is not registered in the manifest");
            } else if (e instanceof ClassNotFoundException) {
                Log.e(TAG, "ClassNotFoundException - the class does not exist");
            } else if (e instanceof ClassCastException) {
                Log.e(TAG, "ClassCastException - the class cannot be cast to Activity");
            } else if (e instanceof SecurityException) {
                Log.e(TAG, "SecurityException - you don't have permission to start this activity");
            } else if (e instanceof android.content.pm.PackageManager.NameNotFoundException) {
                Log.e(TAG, "NameNotFoundException - the component is not found in the manifest");
            }

            Toast.makeText(this, "Error navigating to next screen", Toast.LENGTH_SHORT).show();

            // Try a direct navigation to proper activity based on role
            try {
                Class<?> targetClass;
                switch (role.toLowerCase()) {
                    case "chef":
                        targetClass = KitchenActivity.class;
                        break;
                    case "admin":
                        targetClass = AdminActivity.class;
                        break;
                    case "manager":
                        targetClass = ManagerDashboardActivity.class;
                        break;
                    case "waiter":
                        targetClass = OrderActivity.class;
                        break;
                    default:
                        targetClass = CustomerMenuActivity.class;
                }

                Intent intent = new Intent(this, targetClass);
                intent.putExtra("user_role", role);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } catch (Exception fallbackEx) {
                Log.e(TAG, "Direct navigation also failed", fallbackEx);

                // Last resort fallback
                try {
                    Intent lastResortIntent = new Intent(this, OrderActivity.class);
                    lastResortIntent.putExtra("user_role", role);
                    lastResortIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(lastResortIntent);
                    finish();
                } catch (Exception lastEx) {
                    Log.e(TAG, "All navigation attempts failed", lastEx);
                }
            }
        }
    }

    private void navigateToRegister() {
        try {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to register", e);
            Toast.makeText(this, "Unable to access registration screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToForgotPassword() {
        try {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to forgot password", e);
            Toast.makeText(this, "Password recovery feature is currently unavailable", Toast.LENGTH_SHORT).show();
        }
    }
}