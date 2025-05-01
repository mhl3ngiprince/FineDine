package com.finedine.rms.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A safety wrapper for Firebase operations to prevent app crashes
 * when Firebase is not available or initialized properly
 */
public class FirebaseSafetyWrapper {
    private static final String TAG = "FirebaseSafety";

    /**
     * Safely initialize Firebase, returning success/failure status
     */
    public static boolean initializeFirebase(Context context) {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                Log.d(TAG, "Firebase initialized successfully");
                return true;
            } else {
                Log.d(TAG, "Firebase was already initialized");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage(), e);

            // Provide more detailed error information for debugging
            if (context != null) {
                try {
                    // Check if google-services.json exists by attempting to get the default web client id
                    String resourceName = context.getPackageName() + ":string/default_web_client_id";
                    int resId = context.getResources().getIdentifier(resourceName, null, null);

                    if (resId == 0) {
                        Log.e(TAG, "Could not find default_web_client_id. This suggests google-services.json may be missing or invalid.");
                    } else {
                        Log.d(TAG, "default_web_client_id found, google-services.json appears to be present.");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error checking Firebase resources: " + ex.getMessage(), ex);
                }

                Toast.makeText(context, "Authentication service unavailable", Toast.LENGTH_SHORT).show();
            }

            return false;
        }
    }

    /**
     * Safely get FirebaseAuth, returns null if unavailable
     */
    public static FirebaseAuth getAuthInstance(Context context) {
        try {
            // Try to initialize Firebase if needed
            if (!initializeFirebase(context)) {
                Log.e(TAG, "Failed to initialize Firebase, returning null auth instance");
                return null;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            Log.d(TAG, "Successfully obtained FirebaseAuth instance");
            return auth;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FirebaseAuth instance: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Safely get FirebaseFirestore, returns null if unavailable
     */
    public static FirebaseFirestore getFirestoreInstance(Context context) {
        try {
            // Try to initialize Firebase if needed
            if (!initializeFirebase(context)) {
                return null;
            }

            return FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FirebaseFirestore instance", e);
            return null;
        }
    }
}