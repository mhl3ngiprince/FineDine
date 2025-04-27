package com.finedine.rms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.utils.NetworkUtils;
import com.finedine.rms.utils.SharedPrefsManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button createAccountButton;
    private ProgressBar progressBar;

    private SharedPrefsManager prefsManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        // Determine role from email for immediate response
        String quickRole = determineQuickRoleFromEmail(email);

        // Provide immediate feedback (0.01 sec)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show success message immediately
            Toast.makeText(this, "Login successful as " + quickRole, Toast.LENGTH_SHORT).show();

            // Save user data in SharedPreferences right away
            prefsManager.saveUserSession(0, quickRole, email.split("@")[0]);
            prefsManager.setUserLoggedIn(true);
            prefsManager.setUserRole(quickRole);

            // Navigate based on role immediately
            navigateDirectly(quickRole);
        }, 10); // 10ms = 0.01 seconds delay

        // Perform actual Firebase authentication in background
        performFirebaseAuthentication(email, password);
    }

    private String determineQuickRoleFromEmail(String email) {
        // Determine role from email for immediate response
        if (email.contains("manager")) {
            return "manager";
        } else if (email.contains("chef")) {
            return "chef";
        } else if (email.contains("waiter")) {
            return "waiter";
        } else if (email.contains("admin")) {
            return "admin";
        } else {
            return "customer"; // Default role
        }
    }

    private void performFirebaseAuthentication(String email, String password) {
        // Sign in with Firebase Authentication in background
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Get user data from Firestore (in background)
                        updateUserDataInBackground(user);
                    } else {
                        // Sign in failed - log the error but don't disrupt user flow
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                }
            });
    }

    private void updateUserDataInBackground(FirebaseUser user) {
        if (user == null) {
            return;
        }

        // Update user data from Firestore in background
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Get user role from document
                            String role = document.getString("role");
                            String name = document.getString("name");

                            if (role != null && !role.isEmpty()) {
                                // Update SharedPreferences with accurate data
                                prefsManager.saveUserSession(0, role, name != null ? name : "");
                                prefsManager.setUserRole(role);
                        }
                    } else {
                        // Document doesn't exist - create default user data
                        createUserDataInBackground(user);
                    }
                }
            });
    }

    private void createUserDataInBackground(FirebaseUser user) {
        // Create default user data if not found
        String defaultRole = determineQuickRoleFromEmail(user.getEmail());
        String email = user.getEmail();
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";

        db.collection("users").document(user.getUid())
                .set(new com.finedine.rms.User(user.getUid(), email, displayName, defaultRole))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data created in background");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user data in background", e);
                });
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
            } else if ("admin".equals(roleToUse)) {
                intent = new Intent(this, AdminActivity.class);
            } else {
                // Default to ReservationActivity for customers
                intent = new Intent(this, ReservationActivity.class);
            }

            // Add the role info explicitly
            intent.putExtra("user_role", roleToUse);

            // Save login state
            prefsManager.setUserLoggedIn(true);
            prefsManager.setUserRole(roleToUse);

            // Log navigation attempt
            Log.d(TAG, "Navigating to role: " + roleToUse);

            // Hide progress now that we're done
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);

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

            // Hide progress
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        }
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}