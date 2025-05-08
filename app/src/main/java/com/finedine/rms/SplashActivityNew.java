package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.utils.SharedPrefsManager;

public class SplashActivityNew extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 1000; // 1 second
    private Handler mainHandler;
    private boolean isNavigating = false;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            mainHandler = new Handler(Looper.getMainLooper());
            Log.d(TAG, "SplashActivity onCreate starting with minimal dependencies");

            // First priority - set the view
            setContentView(R.layout.activity_splash);

            // Start animations immediately to improve perceived performance
            startInitialAnimations();

            // Schedule the transition to login screen
            scheduleNavigation();

            // Initialize other components in the background
            initializeServicesInBackground();

        } catch (Exception e) {
            Log.e(TAG, "Critical error in SplashActivity onCreate", e);
            emergencyNavigateToLogin();
        }
    }

    /**
     * Start basic animations without waiting for other initializations
     */
    private void startInitialAnimations() {
        try {
            // Find logo view - one of the first visible elements
            ImageView logoView = findViewById(R.id.ivLogo);
            if (logoView != null) {
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(500);
                fadeIn.setFillAfter(true);
                logoView.startAnimation(fadeIn);
            }

            // Show progress bar
            ProgressBar progressBar = findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initial animations", e);
            // Non-critical, continue app flow
        }
    }

    /**
     * Initialize app services in background to avoid blocking UI
     */
    private void initializeServicesInBackground() {
        new Thread(() -> {
            try {
                // Initialize SharedPrefsManager
                prefsManager = new SharedPrefsManager(this);
                if (prefsManager != null) {
                    prefsManager.clearUserSession();
                    prefsManager.setUserLoggedIn(false);
                    Log.d(TAG, "Cleared previous login state");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SharedPrefsManager", e);
            }

            // Run secondary animations on UI thread
            runOnUiThread(this::startSecondaryAnimations);
        }).start();
    }

    /**
     * Secondary animations for text and other non-critical elements
     */
    private void startSecondaryAnimations() {
        try {
            // Find views
            TextView appNameView = findViewById(R.id.tvAppName);
            TextView taglineView = findViewById(R.id.tvTagline);

            // Animate app name with slight delay
            if (appNameView != null) {
                mainHandler.postDelayed(() -> {
                    AlphaAnimation textFadeIn = new AlphaAnimation(0.0f, 1.0f);
                    textFadeIn.setDuration(500);
                    textFadeIn.setFillAfter(true);
                    appNameView.startAnimation(textFadeIn);
                }, 200);
            }

            // Animate tagline with slightly longer delay
            if (taglineView != null) {
                mainHandler.postDelayed(() -> {
                    AlphaAnimation taglineFadeIn = new AlphaAnimation(0.0f, 1.0f);
                    taglineFadeIn.setDuration(500);
                    taglineFadeIn.setFillAfter(true);
                    taglineView.startAnimation(taglineFadeIn);
                }, 400);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in secondary animations", e);
            // Non-critical, continue app flow
        }
    }

    /**
     * Schedule navigation with main and backup timers
     */
    private void scheduleNavigation() {
        // Main timer
        mainHandler.postDelayed(this::navigateToLogin, SPLASH_DELAY);

        // Backup timer in case something hangs
        mainHandler.postDelayed(() -> {
            if (!isNavigating && !isFinishing()) {
                Log.w(TAG, "Backup timer triggered navigation");
                navigateToLogin();
            }
        }, SPLASH_DELAY + 2000);
    }

    /**
     * Navigate to the login screen
     */
    private void navigateToLogin() {
        if (isNavigating || isFinishing()) return;
        isNavigating = true;

        try {
            Log.d(TAG, "Navigating to LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Try to apply animation, but don't fail if not possible
            try {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } catch (Exception e) {
                Log.e(TAG, "Could not apply transition animation", e);
            }

            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to login", e);
            emergencyNavigateToLogin();
        }
    }

    /**
     * Emergency navigation when normal navigation fails
     */
    private void emergencyNavigateToLogin() {
        try {
            // Create intent with clear flags to ensure fresh start
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in emergency navigation", e);
            finishAffinity(); // Last resort - exit the app
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "SplashActivity onPause called");

        // Cancel any pending handlers
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SplashActivity onDestroy called");

        // Ensure all handlers are cleaned up
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
    }
}