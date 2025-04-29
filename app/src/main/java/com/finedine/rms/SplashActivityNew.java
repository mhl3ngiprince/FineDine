package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.utils.SharedPrefsManager;

public class SplashActivityNew extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 3000; // 3 seconds
    private SharedPrefsManager prefsManager;
    private ImageView logoImageView;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        try {
            Log.d(TAG, "SplashActivity onCreate called");

            // Initialize UI elements
            logoImageView = findViewById(R.id.logoImageView);
            titleTextView = findViewById(R.id.titleTextView);

            // Apply fade-in animation
            applyAnimations();

            // Initialize SharedPrefsManager
            prefsManager = new SharedPrefsManager(this);

            // Use a handler to delay the transition
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    checkLoginStatusAndNavigate();
                } catch (Exception e) {
                    Log.e(TAG, "Error in delayed navigation", e);
                    navigateToLogin();
                }
            }, SPLASH_DELAY);
        } catch (Exception e) {
            Log.e(TAG, "Error in SplashActivity onCreate", e);
            // Try to navigate anyway after a short delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> navigateToLogin(), 1000);
        }
    }

    private void applyAnimations() {
        try {
            // Create fade-in animation
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1500);
            fadeIn.setFillAfter(true);

            // Apply to logo and title
            if (logoImageView != null) {
                logoImageView.startAnimation(fadeIn);
            }

            // Delayed animation for title
            if (titleTextView != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Animation titleFadeIn = new AlphaAnimation(0.0f, 1.0f);
                    titleFadeIn.setDuration(1000);
                    titleFadeIn.setFillAfter(true);
                    titleTextView.startAnimation(titleFadeIn);
                }, 500);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying animations", e);
        }
    }

    private void checkLoginStatusAndNavigate() {
        try {
            // Check if user is already logged in
            if (prefsManager != null && prefsManager.isUserLoggedIn()) {
                String role = prefsManager.getUserRole();
                Log.d(TAG, "User already logged in with role: " + role);
                navigateBasedOnRole(role);
            } else {
                navigateToLogin();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking login status", e);
            // Fallback to login screen if any error occurs
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        try {
            Log.d(TAG, "Navigating to LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close splash activity
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to login", e);
            Toast.makeText(this, "Error starting application", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void navigateBasedOnRole(String role) {
        try {
            Log.d(TAG, "Navigating based on role: " + role);
            Intent intent;

            // Default to customer role if role is invalid
            if (role == null || role.trim().isEmpty()) {
                role = "customer";
            }

            switch (role.toLowerCase()) {
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
                case "customer":
                default:
                    intent = new Intent(this, ReservationActivity.class);
                    break;
            }

            // Pass role in intent
            intent.putExtra("user_role", role);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating based on role", e);
            // Fallback to login if navigation fails
            navigateToLogin();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't finish in onPause to prevent premature destruction
        Log.d(TAG, "SplashActivity onPause called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SplashActivity onDestroy called");
    }
}