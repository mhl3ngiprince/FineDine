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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import com.finedine.rms.utils.DialogUtils;
import com.finedine.rms.utils.NetworkUtils;
import com.finedine.rms.utils.RoleManager;
import com.finedine.rms.utils.SharedPrefsManager;

public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;



    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefsManager = new SharedPrefsManager(this);

        // Enable offline persistence for Firestore
        db.setFirestoreSettings(
            new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        );

        // Initialize views
        emailEditText = findViewById(R.id.emailInput);
        passwordEditText = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        // Set up click listeners
        loginButton.setOnClickListener(v -> attemptLogin());
        
        // Create notification channel for the app
        //NotificationHelper.createNotificationChannel(this);
        
        // Check and request notification permissions
        requestNotificationPermissionIfNeeded();
        
        // For debugging - log if Firebase is initialized
        Log.d(TAG, "Firebase Auth instance: " + mAuth);
        Log.d(TAG, "Firebase Auth current user: " + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "null"));
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            // Fetch user role and navigate accordingly
            fetchUserRoleAndNavigate(currentUser.getUid());
        }
    }
    
    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                new String[] { android.Manifest.permission.POST_NOTIFICATIONS },
                101
            );
        }
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
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            DialogUtils.showProgressDialog(this, "Signing in...");
        }
        loginButton.setEnabled(false);
        
        // Log authentication attempt
        Log.d(TAG, "Attempting login with email: " + email);
        
        // Attempt login with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    // Hide progress
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        DialogUtils.hideProgressDialog(null);
                    }
                    loginButton.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRoleAndNavigate(user.getUid());
                        } else {
                            Log.w(TAG, "User is null after successful login");
                            Toast.makeText(LoginActivity.this, "Authentication failed: User is null", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Sign in failed
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        
                        // Provide specific error messages based on exception type
                        String errorMessage = "Authentication failed";
                        if (task.getException() != null) {
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "No user found with this email address";
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid email or password";
                            } else if (task.getException() instanceof FirebaseNetworkException) {
                                errorMessage = "Network error. Please check your connection";
                            } else {
                                errorMessage = "Authentication failed: " + task.getException().getMessage();
                            }
                        }
                        
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            });
    }
    
    private void fetchUserRoleAndNavigate(String userId) {
        Log.d(TAG, "Fetching user role for UID: " + userId);
        
        // First check if we have the role cached in SharedPreferences
        String cachedRole = prefsManager.getUserRole();
        if (cachedRole != null && !cachedRole.isEmpty()) {
            Log.d(TAG, "Using cached role: " + cachedRole);
            navigateBasedOnRole(cachedRole);
            return;
        }
        
        // If not in cache, fetch from Firestore
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            DialogUtils.showProgressDialog(this, "Loading user data...");
        }
        
        db.collection("users")
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                // Hide progress
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    DialogUtils.hideProgressDialog(null);
                }
                
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        // Get user data
                        String userName = document.getString("name");
                        String userRole = document.getString("role");
                        
                        Log.d(TAG, "User role from Firestore: " + userRole);
                        
                        // Save to SharedPreferences
                        prefsManager.saveUserSession(userId.hashCode(), userRole, userName);
                        
                        // Navigate based on role
                        navigateBasedOnRole(userRole);
                    } else {
                        Log.w(TAG, "User document doesn't exist");
                        
                        // For testing purposes or first-time login, we'll assume customer role if not found
                        String defaultRole = RoleManager.ROLE_CUSTOMER;
                        Toast.makeText(LoginActivity.this, 
                                "User profile not found, using default role: " + defaultRole, 
                                Toast.LENGTH_SHORT).show();
                        
                        prefsManager.saveUserSession(userId.hashCode(), defaultRole, "User");
                        navigateBasedOnRole(defaultRole);
                    }
                } else {
                    Log.w(TAG, "Error fetching user data", task.getException());
                    Toast.makeText(LoginActivity.this, 
                            "Failed to fetch user data: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                            Toast.LENGTH_SHORT).show();
                    
                    // Navigate to default role anyway so user isn't stuck
                    navigateBasedOnRole(RoleManager.ROLE_CUSTOMER);
                }
            });
    }
    
    private void navigateBasedOnRole(String userRole) {
        Intent intent;
        
        // Make sure role is not null
        if (userRole == null || userRole.isEmpty()) {
            userRole = RoleManager.ROLE_CUSTOMER;
        }
        
        Log.d(TAG, "Navigating based on role: " + userRole);
        
        switch (userRole) {
            case RoleManager.ROLE_MANAGER:
                intent = new Intent(this, ManagerDashboardActivity.class);
                break;
            case RoleManager.ROLE_CHEF:
                intent = new Intent(this, KitchenActivity.class);
                break;
            case RoleManager.ROLE_WAITER:
                intent = new Intent(this, OrderActivity.class);
                break;
            case RoleManager.ROLE_CUSTOMER:
            default:
                intent = new Intent(this, OrderActivity.class);
                break;
        }
        
        startActivity(intent);
        finish();
    }
    
    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}