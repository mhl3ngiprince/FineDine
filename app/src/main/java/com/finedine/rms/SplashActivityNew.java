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
    private static final int SPLASH_DELAY = 3000; // 3 seconds
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            Log.d(TAG, "SplashActivity onCreate starting");

            try {
                setContentView(R.layout.activity_splash);
                Log.d(TAG, "Splash layout loaded successfully");
            } catch (Exception e) {
                View view = new View(this);
                view.setBackgroundColor(getResources().getColor(android.R.color.white, null));
                setContentView(view);

                Toast.makeText(this, "Welcome to Fine Dine", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(this::navigateToLogin, 1500);

                return;
            }

            Log.d(TAG, "SplashActivity onCreate called");

            try {
                prefsManager = new SharedPrefsManager(this);

                if (prefsManager != null) {
                    prefsManager.clearUserSession();
                    prefsManager.setUserLoggedIn(false);
                    Log.d(TAG, "Cleared previous login state");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SharedPrefsManager", e);
                prefsManager = null;
            }

            createAndStartAnimations();

            Toast.makeText(this, "Welcome to Fine Dine", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    navigateToLogin();
                } catch (Exception e) {
                    Log.e(TAG, "Error in delayed navigation", e);
                    fallbackNavigation();
                }
            }, SPLASH_DELAY);
        } catch (Exception e) {
            Log.e(TAG, "Critical error in SplashActivity onCreate", e);
            fallbackNavigation();
        }
    }

    private void fallbackNavigation() {
        try {
            Toast.makeText(this, "Starting Fine Dine...", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Critical failure in fallback navigation", e);
                    finishAffinity(); // Last resort - exit the app
                }
            }, 1000);
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in fallbackNavigation", e);
            finishAffinity(); // Last resort - exit the app
        }
    }

    private void createAndStartAnimations() {
        try {
            // Find views - with null checks
            ImageView logoView = findViewById(R.id.ivLogo);
            TextView appNameView = findViewById(R.id.tvAppName);
            TextView taglineView = findViewById(R.id.tvTagline);
            ProgressBar progressBar = findViewById(R.id.progressBar);

            // Create fade-in animation
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1000);
            fadeIn.setFillAfter(true);

            // Apply animations with null checks
            if (logoView != null) {
                logoView.startAnimation(fadeIn);
                Log.d(TAG, "Applied animation to logo");
            }

            if (appNameView != null) {
                // Delayed animation for title
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AlphaAnimation textFadeIn = new AlphaAnimation(0.0f, 1.0f);
                    textFadeIn.setDuration(1000);
                    textFadeIn.setFillAfter(true);
                    appNameView.startAnimation(textFadeIn);
                }, 500);
            }

            if (taglineView != null) {
                // Delayed animation for tagline
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AlphaAnimation taglineFadeIn = new AlphaAnimation(0.0f, 1.0f);
                    taglineFadeIn.setDuration(800);
                    taglineFadeIn.setFillAfter(true);
                    taglineView.startAnimation(taglineFadeIn);
                }, 800);
            }

            // Ensure progress bar is showing
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error applying animations", e);
        }
    }

    private void navigateToLogin() {
        try {
            Log.d(TAG, "Navigating to LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to login", e);
            Toast.makeText(this, "Error starting application", Toast.LENGTH_LONG).show();
            fallbackNavigation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "SplashActivity onPause called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SplashActivity onDestroy called");
    }
}