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

                // Enable offline persistence for Realtime Database
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);

                NotificationUtils.createNotificationChannel(this);
            } catch (Exception e) {
                Log.e(TAG, "Firebase initialization failed", e);
            }
        }
    }
}