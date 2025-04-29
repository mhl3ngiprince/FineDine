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
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(context, "Authentication service unavailable", Toast.LENGTH_SHORT).show();
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
                return null;
            }

            return FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FirebaseAuth instance", e);
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