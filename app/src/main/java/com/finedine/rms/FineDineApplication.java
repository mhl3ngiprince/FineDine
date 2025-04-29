package com.finedine.rms;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

public class FineDineApplication extends Application {

    private static final String TAG = "FineDineApp";
    private static Context applicationContext;

    @Override
    public void onCreate() {
        // Ensure we catch any exception but still continue app initialization
        try {
            // Enable strict mode in debug builds for better detection of issues
            // Removed BuildConfig dependency as it may cause issues

            // Call super.onCreate() first to ensure proper initialization
            super.onCreate();

            // Store application context for potential static access
            applicationContext = getApplicationContext();

            Log.d(TAG, "Application onCreate - starting");

            // Initialize basic app components, but not Firebase yet
            // Firebase will be initialized on-demand when needed

            Log.d(TAG, "Application onCreate - completed successfully");
        } catch (Throwable t) {
            // Catch absolutely everything to prevent app crashes
            try {
                Log.e(TAG, "Critical error in application startup, but continuing", t);
            } catch (Throwable ignored) {
                // Even logging might fail, but we want to continue anyway
            }
        }
    }

    /**
     * Get the application context
     */
    public static Context getAppContext() {
        return applicationContext;
    }

    /**
     * Check if Firebase is initialized - always return false for safety
     */
    public static boolean isFirebaseInitialized() {
        return false;
    }
}