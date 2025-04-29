package com.finedine.rms;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.utils.FirebaseSafetyWrapper;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText emailEditText;
    private Button resetButton;
    private Button backButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize UI components
        emailEditText = findViewById(R.id.emailInput);
        resetButton = findViewById(R.id.resetButton);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase Auth safely
        mAuth = FirebaseSafetyWrapper.getAuthInstance(this);

        // Hide progress bar initially
        progressBar.setVisibility(View.GONE);

        // Set click listeners
        resetButton.setOnClickListener(v -> attemptPasswordReset());
        backButton.setOnClickListener(v -> finish());
    }

    private void attemptPasswordReset() {
        // Get email address
        String email = emailEditText.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        resetButton.setEnabled(false);

        // If using demo accounts
        if (email.equals("admin@finedine.com") ||
                email.equals("manager@finedine.com") ||
                email.equals("chef@finedine.com") ||
                email.equals("waiter@finedine.com") ||
                email.equals("customer@finedine.com")) {
            Toast.makeText(this, "Password reset instructions sent to demo account email (for demo purposes only).", Toast.LENGTH_LONG).show();
            resetButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Using Firebase Auth to send password reset email
        if (mAuth == null) {
            Toast.makeText(this, "Authentication service unavailable", Toast.LENGTH_SHORT).show();
            resetButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    resetButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Password reset instructions have been sent to your email",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = "Failed to send password reset email";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Password reset failed", task.getException());
                    }
                });
    }
}