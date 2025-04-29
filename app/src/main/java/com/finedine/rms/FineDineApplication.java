package com.finedine.rms;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class FineDineApplication extends Application {

    private static final String TAG = "FineDineApp";
    private static Context applicationContext;

    @Override
    public void onCreate() {
        // Ensure we catch any exception but still continue app initialization
        try {
            // Call super.onCreate() first to ensure proper initialization
            super.onCreate();

            // Store application context for potential static access
            applicationContext = getApplicationContext();

            Log.d(TAG, "Application onCreate - starting");

            // Initialize basic app components, but not Firebase yet
            // Firebase will be initialized on-demand when needed

            // Initialize Firebase safely
            try {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase, continuing without it", e);
            }

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
        try {
            return FirebaseApp.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}