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
import androidx.appcompat.app.AlertDialog;
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
        loginTextView = findViewById(R.id.loginTextView);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up click listeners
        registerButton.setOnClickListener(v -> attemptRegistration());
        loginTextView.setOnClickListener(v -> goToLogin(v));

        // Set up role spinner with user_roles array
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.user_roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
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

        // Create immediate success feedback and navigate instantly (0.01 sec)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show success message immediately
            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

            // Navigate to the appropriate activity immediately
            navigateBasedOnRole(roleToUse);

            // Save basic info in SharedPreferences right away
            prefsManager.saveUserSession(0, roleToUse, name);
            prefsManager.setUserLoggedIn(true);
            prefsManager.setUserRole(roleToUse);
        }, 10); // 10ms = 0.01 seconds delay

        // Perform actual Firebase operations in background
        performFirebaseRegistration(email, password, name, roleToUse);
    }

    private void performFirebaseRegistration(String email, String password, String name, String roleToUse) {
        // Create user in Firebase Auth (background operation)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Registration successful, get the user
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Save user data to Firestore
                        saveUserDataToFirestore(user, name, roleToUse);
                    } else {
                        // If registration fails, log it (user is already navigated away)
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                }
            });
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

    private void saveUserDataToFirestore(FirebaseUser user, String name, String role) {
        if (user == null) {
            Log.w(TAG, "Failed to create user in Firebase");
            return;
        }

        // Create a User object
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", name);
        userData.put("email", user.getEmail());
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore (background operation)
        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created successfully");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document", e);
                });
    }

    private void navigateBasedOnRole(String role) {
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

        // Add the role as an extra
        intent.putExtra("user_role", role);

        // Start the activity
        startActivity(intent);
        finish();
    }

    public void goToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}