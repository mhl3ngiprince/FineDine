package com.finedine.rms;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FineDineApplication extends Application {

    private static final String TAG = "FineDineApp";
    private static Context applicationContext;

    // Database instance for the whole application
    private AppDatabase appDatabase;

    // Thread pool for database operations
    private ExecutorService databaseExecutor;

    // Handler for main thread operations
    private Handler mainHandler;

    @Override
    public void onCreate() {
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);
            // You could also save logs to a file or send to a server
        });

        // Ensure we catch any exception but still continue app initialization
        try {
            // Call super.onCreate() first to ensure proper initialization
            super.onCreate();

            // Store application context for potential static access
            applicationContext = getApplicationContext();

            // Initialize main handler
            mainHandler = new Handler(Looper.getMainLooper());

            Log.d(TAG, "Application onCreate - starting");

            // Initialize database
            try {
                Log.d(TAG, "Initializing database...");
                databaseExecutor = Executors.newFixedThreadPool(4);
                appDatabase = AppDatabase.getDatabase(this);
                Log.d(TAG, "Database initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing database", e);
                showToastOnMainThread("Database initialization error");
            }

            // Initialize Firebase safely
            try {
                initializeFirebase();
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

    private void initializeFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");

            // Verify Firebase Auth is available
            try {
                Class.forName("com.google.firebase.auth.FirebaseAuth");
                // Force initialize FirebaseAuth to catch any early exceptions
                com.google.firebase.auth.FirebaseAuth.getInstance();
                Log.d(TAG, "Firebase Auth is available and initialized");
            } catch (Exception e) {
                Log.e(TAG, "Firebase Auth component not available: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Firebase was already initialized");
            // Ensure Firebase Auth is initialized even if Firebase was already initialized
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance();
                Log.d(TAG, "Firebase Auth verified on existing Firebase instance");
            } catch (Exception e) {
                Log.e(TAG, "Error verifying Firebase Auth on existing instance: " + e.getMessage());
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Clean up resources
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            try {
                databaseExecutor.shutdown();
                // Wait for tasks to complete with a timeout
                if (!databaseExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    databaseExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                databaseExecutor.shutdownNow();
                Thread.currentThread().interrupt(); // preserve interrupt status
            }
        }

        // Close database if needed
        if (appDatabase != null) {
            appDatabase.close();
            appDatabase = null;
        }

        Log.d(TAG, "Application terminated and resources cleaned up");
    }

    /**
     * Get the application context
     */
    public static Context getAppContext() {
        return applicationContext;
    }

    /**
     * Get the application's database instance
     */
    public AppDatabase getDatabase() {
        return appDatabase;
    }

    /**
     * Get the database executor service
     */
    public ExecutorService getDatabaseExecutor() {
        return databaseExecutor;
    }

    /**
     * Show a toast from any thread
     */
    public void showToastOnMainThread(final String message) {
        if (mainHandler != null) {
            mainHandler.post(() -> {
                try {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing toast", e);
                }
            });
        }
    }

    /**
     * Check if Firebase is initialized
     */
    public static boolean isFirebaseInitialized() {
        try {
            if (applicationContext == null) {
                Log.e(TAG, "Application context is null when checking Firebase initialization");
                return false;
            }

            boolean isInitialized = !FirebaseApp.getApps(applicationContext).isEmpty();

            // Additional check to see if Firebase Auth specifically is working
            if (isInitialized) {
                try {
                    // Try to get the FirebaseAuth instance to verify it's working
                    com.google.firebase.auth.FirebaseAuth.getInstance();
                } catch (Exception e) {
                    Log.e(TAG, "Firebase initialized but Auth is not working: " + e.getMessage());
                    return false;
                }
            }

            return isInitialized;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Firebase initialization", e);
            return false;
        }
    }

    /**
     * Get the Firebase Auth instance, creating it if needed
     */
    public static com.google.firebase.auth.FirebaseAuth getFirebaseAuth() {
        try {
            if (!isFirebaseInitialized()) {
                Log.e(TAG, "Firebase is not initialized, cannot get FirebaseAuth");
                return null;
            }
            return com.google.firebase.auth.FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error getting FirebaseAuth", e);
            return null;
        }
    }
}