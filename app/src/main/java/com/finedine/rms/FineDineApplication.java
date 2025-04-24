package com.finedine.rms;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.finedine.rms.utils.NotificationUtils;

public class FineDineApplication extends Application {

    private static final String TAG = "FineDineApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialization successful");

                // Enable offline persistence for Firestore
                FirebaseFirestore.getInstance().setFirestoreSettings(
                    new FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                );

                // Log a reminder about Firebase Security Rules
                Log.w(TAG, "IMPORTANT: Make sure your Firebase Security Rules allow read/write access. " +
                        "Current rules may be causing PERMISSION_DENIED errors.");
                Log.i(TAG, "Suggested Firestore Rules:\n" +
                        "rules_version = '2';\n" +
                        "service cloud.firestore {\n" +
                        "  match /databases/{database}/documents {\n" +
                        "    match /{document=**} {\n" +
                        "      allow read, write: if request.auth != null;\n" +
                        "    }\n" +
                        "  }\n" +
                        "}");

                // Enable offline persistence for Realtime Database
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);

                NotificationUtils.createNotificationChannel(this);
            } catch (Exception e) {
                Log.e(TAG, "Firebase initialization failed", e);
            }
        }
    }
}