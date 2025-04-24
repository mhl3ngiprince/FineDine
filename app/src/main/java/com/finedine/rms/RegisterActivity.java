package com.finedine.rms;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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

        // SIMPLIFIED APPROACH - Skip Firebase for now and go straight to the activity
        String roleToUse = "customer"; // Default role

        // Map the selected role to our constants
        if (selectedRole.equalsIgnoreCase("Manager")) {
            roleToUse = "manager";
        } else if (selectedRole.equalsIgnoreCase("Chef")) {
            roleToUse = "chef";
        } else if (selectedRole.equalsIgnoreCase("Waiter")) {
            roleToUse = "waiter";
        }

        // Save role in shared preferences directly
        prefsManager.setUserRole(roleToUse);

        // Show a simple confirmation
        Toast.makeText(this, "Registration successful as " + roleToUse, Toast.LENGTH_SHORT).show();

        // Launch the appropriate activity directly based on the selected role
       /* Intent intent = null;

        if (roleToUse.equals("manager")) {
            intent = new Intent(this, ManagerDashboardActivity.class);
        } else if (roleToUse.equals("chef")) {
            intent = new Intent(this, KitchenActivity.class);
        } else {
            intent = new Intent(this, OrderActivity.class);
        }

        // Add the role as an extra
        intent.putExtra("user_role", roleToUse);

        // Hide progress indicator
        progressBar.setVisibility(View.GONE);
        registerButton.setEnabled(true);

        // Start the activity
        startActivity(intent);
        finish();*/
    }

    public void goToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}