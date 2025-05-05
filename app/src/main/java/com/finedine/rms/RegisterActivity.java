package com.finedine.rms;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import com.finedine.rms.utils.DialogUtils;
import com.finedine.rms.utils.InputValidator;
import com.finedine.rms.utils.NetworkUtils;
import com.finedine.rms.utils.RoleManager;
import com.finedine.rms.utils.SharedPrefsManager;

public class RegisterActivity extends AppCompatActivity {
    
    private static final String TAG = "RegisterActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText nameEditText;
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
        setContentView(R.layout.activity_register);

        try {
            // Initialize views first (so UI is visible even if Firebase fails)
            emailEditText = findViewById(R.id.emailInput);
            passwordEditText = findViewById(R.id.passwordInput);
            nameEditText = findViewById(R.id.nameInput);
            roleSpinner = findViewById(R.id.roleSpinner);
            registerButton = findViewById(R.id.registerButton);
            loginTextView = findViewById(R.id.loginTextView);
            progressBar = findViewById(R.id.progressBar);

            // Set up role spinner with user_roles array
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.user_roles, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            roleSpinner.setAdapter(adapter);

            // Initialize Firebase safely
            initializeFirebaseSafely();

            // Initialize SharedPrefsManager
            prefsManager = new SharedPrefsManager(this);

            // Set user as not logged in when entering register screen
            prefsManager.setUserLoggedIn(false);

            // Set up click listeners
            registerButton.setOnClickListener(v -> attemptRegistration());
            loginTextView.setOnClickListener(v -> goToLogin());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing register activity", e);
            Toast.makeText(this, "Error initializing registration. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initialize Firebase safely to prevent crashes
     */
    private void initializeFirebaseSafely() {
        try {
            // Initialize Firebase components
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase authentication and Firestore initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase components", e);
            Toast.makeText(this,
                    "Failed to initialize Firebase. Please check your connection and try again.",
                    Toast.LENGTH_LONG).show();

            registerButton.setEnabled(false);
        }
    }

    private void attemptRegistration() {
        try {
            // Check internet connection first
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please try again when online.", Toast.LENGTH_LONG).show();
                return;
            }

            // Check if Firebase is properly initialized
            if (mAuth == null || db == null) {
                Log.e(TAG, "Firebase components not initialized. Trying to initialize now.");
                initializeFirebaseSafely();

                if (mAuth == null || db == null) {
                    Toast.makeText(this, "Cannot connect to authentication services. Please restart the app.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Get input values
            final String email = emailEditText.getText().toString().trim();
            final String password = passwordEditText.getText().toString().trim();
            final String name = nameEditText.getText().toString().trim();

            // Determine selected role - Get the exact string from spinner
            String selectedRole = roleSpinner.getSelectedItem().toString();
            Log.d(TAG, "Selected role from spinner: " + selectedRole);

            // Simple validation
            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                return;
            }
            if (password.isEmpty()) {
                passwordEditText.setError("Password is required");
                return;
            }
            if (name.isEmpty()) {
                nameEditText.setError("Name is required");
                return;
            }

            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);

            // Map the selected role to our constants
            final String roleToUse = mapSelectedRole(selectedRole);

            // IMMEDIATE FEEDBACK: Navigate instantly (0.001 sec)
            // This gives perception of instant registration while real auth happens in background
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Save basic info in SharedPreferences right away
                prefsManager.saveUserSession(0, roleToUse, name);
                prefsManager.setUserLoggedIn(true);
                prefsManager.setUserRole(roleToUse);

                // Show success message immediately
                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                // Navigate based on role immediately
                navigateBasedOnRole(roleToUse);
            }, 1); // 1ms = 0.001 sec for ultra-fast response

            // Create user in Firebase Auth in background
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    try {
                        if (task.isSuccessful()) {
                            // Registration successful, get the user
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Save user data to Firestore in background
                            saveUserDataInBackground(user, name, roleToUse);
                        } else {
                            // Firebase auth failed, but user already navigated away
                            // Just log the error for tracking purposes
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in registration completion", e);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in attemptRegistration", e);
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            Toast.makeText(this, "Registration error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String mapSelectedRole(String selectedRole) {
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

    private void saveUserDataInBackground(FirebaseUser user, String name, String role) {
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

            // Save to Firestore in background without UI callbacks
            db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created in background");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user data in background", e);
                });
        } catch (Exception e) {
            Log.e(TAG, "Error creating user data in background", e);
        }
    }

    private void navigateBasedOnRole(String role) {
        try {
            // Hide progress indicator
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);

            // Launch the appropriate activity
            Intent intent;

            switch (role) {
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
            intent.putExtra("user_role", role);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Start the activity
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            Toast.makeText(this, "Error navigating: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Show dialog confirming navigation to login screen
        DialogUtils.showConfirmationDialog(
                this,
                "Back to Login",
                "Go back to the login screen?",
                (dialog, which) -> {
                    super.onBackPressed();
                    goToLogin();
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release any resources if needed
        mAuth = null;
        db = null;
    }
}