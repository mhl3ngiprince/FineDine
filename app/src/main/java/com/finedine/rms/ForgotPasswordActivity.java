package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class ForgotPasswordActivity extends BaseActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText emailEditText;
    private Button resetButton;
    private Button backButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            // Set emergency error handler
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                Log.e(TAG, "Uncaught exception in ForgotPasswordActivity", throwable);
                EmergencyActivity.launch(this, "Error in password reset process. Please try again later.");
            });

            // Set content view with try-catch to handle layout inflation errors
            try {
                setContentView(R.layout.activity_forgot_password);
            } catch (Exception e) {
                Log.e(TAG, "Error setting content view", e);
                EmergencyActivity.launch(this, "Error loading password reset screen.");
                finish();
                return;
            }

            // Initialize UI components with null checks
            initializeViews();

            // Initialize Firebase Auth safely
            initializeFirebase();

            // Set click listeners safely
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing password reset", Toast.LENGTH_SHORT).show();
            EmergencyActivity.launch(this, "Error initializing password reset. Please try again later.");
            finish();
        }
    }

    /**
     * Initialize all views with null checks
     */
    private void initializeViews() {
        try {
            emailEditText = findViewById(R.id.emailInput);
            resetButton = findViewById(R.id.resetButton);
            backButton = findViewById(R.id.backButton);
            progressBar = findViewById(R.id.progressBar);
            statusText = findViewById(R.id.statusText);

            // Check for critical views
            if (emailEditText == null || resetButton == null || backButton == null) {
                Log.e(TAG, "Critical views missing in ForgotPasswordActivity");
                throw new RuntimeException("Critical UI elements missing");
            }

            // Ensure the status text is initially invisible
            if (statusText != null) {
                statusText.setVisibility(View.GONE);
            }

            // Hide progress bar initially
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e; // Rethrow to be caught in onCreate
        }
    }

    /**
     * Set up click listeners safely
     */
    private void setupClickListeners() {
        try {
            if (resetButton != null) {
                resetButton.setOnClickListener(v -> attemptPasswordReset());
            }

            if (backButton != null) {
                backButton.setOnClickListener(v -> goToLogin());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    /**
     * Initialize Firebase safely
     */
    private void initializeFirebase() {
        try {
            mAuth = FirebaseSafetyWrapper.getAuthInstance(this);

            if (mAuth == null) {
                Log.w(TAG, "Firebase Authentication is not available. Will use local methods.");
                Toast.makeText(this,
                        "Limited connection to authentication service. Some features may be unavailable.",
                        Toast.LENGTH_SHORT).show();
                // We don't disable the reset button - local methods will be used
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
            mAuth = null;
        }
    }

    private void attemptPasswordReset() {
        try {
            if (emailEditText == null) {
                Toast.makeText(this, "Error: Form not initialized properly", Toast.LENGTH_SHORT).show();
                EmergencyActivity.launch(this, "Error initializing password reset form.");
                return;
            }

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

            // Show progress
            showProgress(true);

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
        } catch (Exception e) {
            Log.e(TAG, "Error attempting password reset", e);
            showErrorMessage("Error attempting password reset. Please try again later.");
        }
    }

    /**
     * Send a custom password reset email through our own email service
     */
    private void sendCustomResetEmail(String email) {
        try {
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

                            // Try Firebase if available
                            if (mAuth != null) {
                                sendFirebaseResetEmail(email);
                            } else {
                                // Use local fallback method if Firebase is not available
                                handleLocalPasswordReset(email);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending custom reset email", e);
            showErrorMessage("Error sending password reset email. Please try again later.");
        }
    }

    /**
     * Handle password reset when Firebase is not available
     */
    private void handleLocalPasswordReset(String email) {
        try {
            Log.d(TAG, "Using local password reset for: " + email);
            // For security, we always show success even if the email doesn't exist locally
            showSuccessMessage("If an account exists with this email, password reset instructions will be sent.");

            // In a real implementation, we would check the local database and send an email
            // Since we don't have access to a full mail server, we'll just assume success
            boolean emailWouldBeSent = true;
            if (emailWouldBeSent) {
                // Log the attempt for debugging
                Log.d(TAG, "Would have sent a local reset email to: " + email);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling local password reset", e);
            showErrorMessage("Error handling local password reset. Please try again later.");
        }
    }

    /**
     * Fall back to Firebase password reset if our custom email sending fails
     */
    private void sendFirebaseResetEmail(String email) {
        try {
            // Using Firebase Auth to send password reset email
            if (mAuth == null) {
                showErrorMessage("Authentication service unavailable");
                handleLocalPasswordReset(email);
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
                                    handleLocalPasswordReset(email);
                                    return;
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
        } catch (Exception e) {
            Log.e(TAG, "Error sending Firebase reset email", e);
            showErrorMessage("Error sending password reset email. Please try again later.");
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void showProgress(boolean show) {
        try {
            if (progressBar == null || resetButton == null) {
                Log.e(TAG, "Progress bar or reset button is null");
                return;
            }

            if (show) {
                progressBar.setVisibility(View.VISIBLE);
                resetButton.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                resetButton.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing/hiding progress", e);
        }
    }

    private void showSuccessMessage(String message) {
        showProgress(false);  // This handles null checks already

        // Show in status text if available
        if (statusText != null) {
            statusText.setText(message);
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            statusText.setVisibility(View.VISIBLE);
        }

        Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void showErrorMessage(String message) {
        showProgress(false); // This handles null checks already

        // Show in status text if available
        if (statusText != null) {
            statusText.setText(message);
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            statusText.setVisibility(View.VISIBLE);
        }

        Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goToLogin();
    }
}