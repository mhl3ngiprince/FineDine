package com.finedine.rms.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseConnectionHelper {
    private static final String TAG = "FirebaseConnection";

    public static boolean isFirebaseAvailable(Context context) {
        // Check if Firebase is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            return false;
        }

        // Check network connectivity
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static FirebaseFirestore getFirestoreInstance(Context context) {
        try {
            if (!isFirebaseAvailable(context)) {
                return null;
            }
            return FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error getting Firestore instance: " + e.getMessage());
            return null;
        }
    }
}
