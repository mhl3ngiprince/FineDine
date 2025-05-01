package com.finedine.rms;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.utils.EmailSender;
import com.finedine.rms.utils.FirebaseSafetyWrapper;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText emailEditText;
    private Button resetButton;
    private Button backButton;
    private ProgressBar progressBar;
    private TextView statusText;
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
        statusText = findViewById(R.id.statusText);

        // Ensure the status text is initially invisible
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }

        // Initialize Firebase Auth safely
        mAuth = FirebaseSafetyWrapper.getAuthInstance(this);

        // Check if Firebase is properly initialized
        if (mAuth == null) {
            Log.e(TAG, "Firebase Authentication is not initialized.");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error")
                    .setMessage("The password reset service is currently unavailable. Please try again later.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }

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

        // Clear any previous status
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }

        // If using demo accounts
        if (email.equals("admin@finedine.com") ||
                email.equals("manager@finedine.com") ||
                email.equals("chef@finedine.com") ||
                email.equals("waiter@finedine.com") ||
                email.equals("customer@finedine.com")) {
            showSuccessMessage("Password reset instructions sent to demo account email (for demo purposes only).");
            return;
        }

        // First try to send email through our own email service
        sendCustomResetEmail(email);
    }

    /**
     * Send a custom password reset email through our own email service
     */
    private void sendCustomResetEmail(String email) {
        showProgress(true);

        // Try to send through Gmail first
        EmailSender.sendPasswordResetEmail(ForgotPasswordActivity.this, email, EmailSender.EmailProvider.GMAIL, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                // Email sent successfully
                showSuccessMessage("Password reset instructions have been sent to your email");
                emailEditText.setText(""); // Clear the email field on success
            }

            @Override
            public void onFailure(String error) {
                // Gmail failed, try Outlook
                EmailSender.sendPasswordResetEmail(ForgotPasswordActivity.this, email, EmailSender.EmailProvider.OUTLOOK, new EmailSender.EmailCallback() {
                    @Override
                    public void onSuccess() {
                        // Email sent successfully through Outlook
                        showSuccessMessage("Password reset instructions have been sent to your email");
                        emailEditText.setText(""); // Clear the email field on success
                    }

                    @Override
                    public void onFailure(String outlookError) {
                        // Both Gmail and Outlook failed, fall back to Firebase
                        Log.e(TAG, "Gmail error: " + error);
                        Log.e(TAG, "Outlook error: " + outlookError);
                        sendFirebaseResetEmail(email);
                    }
                });
            }
        });
    }

    /**
     * Fall back to Firebase password reset if our custom email sending fails
     */
    private void sendFirebaseResetEmail(String email) {
        // Using Firebase Auth to send password reset email
        if (mAuth == null) {
            showErrorMessage("Authentication service unavailable");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    resetButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        showSuccessMessage("Password reset instructions have been sent to your email");
                        emailEditText.setText(""); // Clear the email field on success
                    } else {
                        String errorMessage = "Failed to send password reset email";

                        // Get more specific error message
                        if (task.getException() != null) {
                            Exception exception = task.getException();

                            if (exception instanceof FirebaseAuthInvalidUserException) {
                                // This exception occurs when there's no user record with this email
                                // We don't want to expose that an email doesn't exist for privacy reasons
                                // So we show a generic success message instead
                                showSuccessMessage("If an account exists with this email, password reset instructions will be sent.");
                                return;
                            } else if (exception instanceof FirebaseNetworkException) {
                                errorMessage = "Network error. Please check your internet connection and try again.";
                            } else {
                                errorMessage = exception.getMessage();
                                if (errorMessage == null || errorMessage.isEmpty()) {
                                    errorMessage = "Unknown error occurred. Please try again later.";
                                }
                            }
                        }

                        showErrorMessage(errorMessage);
                        Log.e(TAG, "Password reset failed", task.getException());
                    }
                });
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            resetButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            resetButton.setEnabled(true);
        }
    }

    private void showSuccessMessage(String message) {
        resetButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        // Show in status text if available
        if (statusText != null) {
            statusText.setText(message);
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            statusText.setVisibility(View.VISIBLE);
        }

        Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void showErrorMessage(String message) {
        resetButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        // Show in status text if available
        if (statusText != null) {
            statusText.setText(message);
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            statusText.setVisibility(View.VISIBLE);
        }

        Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
    }
}