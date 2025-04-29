package com.finedine.rms;

import android.app.Application;
import android.util.Log;

public class FineDineApplication extends Application {

    private static final String TAG = "FineDineApp";

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            Log.d(TAG, "Application onCreate - starting");

            // Don't initialize Firebase at startup
            // This prevents crashes during initialization

            Log.d(TAG, "Application onCreate - completed without Firebase initialization");
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
     * Check if Firebase is initialized - always return false for safety
     */
    public static boolean isFirebaseInitialized() {
        return false;
    }
}