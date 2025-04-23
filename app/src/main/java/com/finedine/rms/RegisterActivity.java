package com.finedine.rms;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
        
        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefsManager = new SharedPrefsManager(this);
        
        // Initialize views
        emailEditText = findViewById(R.id.emailInput);
        passwordEditText = findViewById(R.id.passwordInput);
        nameEditText = findViewById(R.id.nameInput);
        roleSpinner = findViewById(R.id.roleSpinner);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView); // Make sure this ID exists in activity_register.xml
        progressBar = findViewById(R.id.progressBar);
        
        // Set up click listeners
        registerButton.setOnClickListener(v -> attemptRegistration());
        loginTextView.setOnClickListener(v -> navigateToLogin());
    }
    
    private void attemptRegistration() {
        // Check internet connection first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please try again when online.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Get input values
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();
        final String name = nameEditText.getText().toString().trim();
        
        // Determine selected role
        String role = RoleManager.ROLE_CUSTOMER; // Default role
        
        String selectedRole = roleSpinner.getSelectedItem().toString();
        switch (selectedRole) {
            case "Manager":
                role = RoleManager.ROLE_MANAGER;
                break;
            case "Chef":
                role = RoleManager.ROLE_CHEF;
                break;
            case "Waiter":
                role = RoleManager.ROLE_WAITER;
                break;
        }
        
        // Validate inputs
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }
        
        if (!InputValidator.isValidEmail(email)) {
            emailEditText.setError("Please enter a valid email address");
            emailEditText.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }
        
        if (!InputValidator.isValidPassword(password)) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }
        
        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }
        
        // Show progress
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            DialogUtils.showProgressDialog(this, "Creating account...");
        }
        registerButton.setEnabled(false);
        
        // Store the final role value for use in the callback
        final String finalRole = role;
        
        // Attempt registration with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Registration success
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user.getUid(), name, email, finalRole);
                        } else {
                            completeRegistration(false, "Failed to get user after registration");
                        }
                    } else {
                        // Registration failed
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        completeRegistration(false, errorMessage);
                    }
                }
            });
    }
    
    private void saveUserData(String userId, String name, String email, String role) {
        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        
        // Save to Firestore
        db.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User data saved successfully for " + userId);
                
                // Save to SharedPreferences
                prefsManager.saveUserSession(userId.hashCode(), role, name);
                
                completeRegistration(true, null);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error saving user data", e);
                completeRegistration(false, "Failed to save user data: " + e.getMessage());
            });
    }
    
    private void completeRegistration(boolean success, String errorMessage) {
        // Hide progress
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        } else {
            DialogUtils.hideProgressDialog(null);
        }
        registerButton.setEnabled(true);
        
        if (success) {
            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
            
            // Get current user and role from preferences
            String role = prefsManager.getUserRole();
            
            // Navigate based on role
            navigateBasedOnRole(role);
        } else {
            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    private void navigateBasedOnRole(String role) {
        Intent intent;
        
        switch (role) {
            case RoleManager.ROLE_MANAGER:
                intent = new Intent(this, ManagerDashboardActivity.class);
                break;
            case RoleManager.ROLE_CHEF:
                intent = new Intent(this, KitchenActivity.class);
                break;
            case RoleManager.ROLE_WAITER:
                intent = new Intent(this, OrderActivity.class);
                break;
            default:
                intent = new Intent(this, OrderActivity.class);
                break;
        }
        
        startActivity(intent);
        finish();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}