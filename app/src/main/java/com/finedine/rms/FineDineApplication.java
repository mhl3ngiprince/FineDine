package com.finedine.rms;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;
import com.finedine.rms.utils.NotificationUtils;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.finedine.rms.utils.SharedPrefsManager;

public class FineDineApplication extends Application {

    private static final String TAG = "FineDineApp";
    private static Context applicationContext;
    private static final String CHANNEL_ID = "FineDineNotificationChannel";

    // Settings constants
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_THEME = "theme_setting";
    private static final String KEY_LANGUAGE = "language_setting";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";
    private static final String THEME_SYSTEM = "system";

    // Database instance for the whole application
    private AppDatabase appDatabase;

    // Thread pool for database operations
    private ExecutorService databaseExecutor;

    // Handler for main thread operations
    private Handler mainHandler;

    @Override
    public void onCreate() {
        // Set global exception handler to prevent crashes
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);
            // Log to file or analytics service in a production app

            // Show a friendly message to the user
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(),
                        "Something went wrong. The app will restart.",
                        Toast.LENGTH_LONG).show();
            });

            // Restart the app after a brief delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), SplashActivityNew.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // Force restart if needed
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }, 1000);
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

            // Create a separate thread for non-critical initialization to avoid blocking the main thread
            Thread initializationThread = new Thread(() -> {
                // Initialize non-critical components in background
                try {
                    initializeTheme();
                    initializeLanguage();
                    createNotificationChannel();
                    // Try Firebase last, since it's most likely to fail
                    safeInitializeFirebase();

                    Log.d(TAG, "Background initialization completed successfully");
                } catch (Throwable t) {
                    Log.e(TAG, "Error during background initialization", t);
                    // Non-critical error, app can continue
                }
            });
            initializationThread.setDaemon(true);
            initializationThread.start();

            // Initialize database in a controlled manner
            initializeDatabase();

            Log.d(TAG, "Application onCreate - critical initialization completed");
        } catch (Throwable t) {
            // Catch absolutely everything to prevent app crashes
            try {
                Log.e(TAG, "Critical error in application startup, but continuing", t);
            } catch (Throwable ignored) {
                // Even logging might fail, but we want to continue anyway
            }
        }
    }

    private void initializeDatabase() {
        try {
            Log.d(TAG, "Starting database initialization...");
            databaseExecutor = Executors.newFixedThreadPool(2); // Reduced thread count

            // Move database initialization to background thread
            databaseExecutor.execute(() -> {
                try {
                    // Add small delay to ensure UI is responsive first
                    SystemClock.sleep(200);
                    appDatabase = AppDatabase.getDatabase(this);
                    Log.d(TAG, "Database initialized successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing database", e);
                    showToastOnMainThread("Database initialization error");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating database executor", e);
            // Try with single thread as fallback
            try {
                databaseExecutor = Executors.newSingleThreadExecutor();
                Log.d(TAG, "Created fallback single thread executor for database");
            } catch (Exception ex) {
                Log.e(TAG, "Critical failure creating database executor", ex);
            }
        }
    }

    private void safeInitializeFirebase() {
        try {
            Log.d(TAG, "Starting Firebase initialization");
            // Attempt firebase initialization with timeout
            ExecutorService firebaseExecutor = Executors.newSingleThreadExecutor();
            firebaseExecutor.submit(() -> {
                try {
                    initializeFirebase();

                    // Check if Firebase Messaging is available
                    boolean fcmAvailable = isFirebaseMessagingAvailable();
                    Log.d(TAG, "Firebase Messaging available: " + fcmAvailable);
                } catch (Exception e) {
                    Log.e(TAG, "Error in Firebase initialization thread", e);
                }
            });

            // Don't wait indefinitely for Firebase
            firebaseExecutor.shutdown();
            if (!firebaseExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                Log.e(TAG, "Firebase initialization timed out after 3 seconds");
                firebaseExecutor.shutdownNow();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase safely", e);
        }
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");

                // Verify Firebase Auth is available
                try {
                    Class.forName("com.google.firebase.auth.FirebaseAuth");
                    // Force initialize FirebaseAuth to catch any early exceptions
                    com.google.firebase.auth.FirebaseAuth.getInstance();
                    Log.d(TAG, "Firebase Auth is available and initialized");

                    // Initialize Firestore and Database
                    try {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance();
                        Log.d(TAG, "Firebase Firestore initialized successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing Firebase Firestore: " + e.getMessage());
                    }

                    try {
                        com.google.firebase.database.FirebaseDatabase.getInstance();
                        Log.d(TAG, "Firebase Realtime Database initialized successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing Firebase Realtime Database: " + e.getMessage());
                    }

                    // Run verification of all Firebase components
                    mainHandler.postDelayed(() -> {
                        try {
                            Map<String, Boolean> componentStatus = verifyFirebaseComponents();
                            Log.d(TAG, "Firebase component verification results:");
                            for (Map.Entry<String, Boolean> entry : componentStatus.entrySet()) {
                                Log.d(TAG, entry.getKey() + ": " + (entry.getValue() ? "OK" : "FAILED"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error verifying Firebase components", e);
                        }
                    }, 3000); // Delay verification to allow initialization to complete
                } catch (Exception e) {
                    Log.e(TAG, "Firebase Auth component not available: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase, will continue without it: " + e.getMessage());
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createNotificationChannel(this);
        }
    }

    private void initializeTheme() {
        try {
            SharedPreferences settingsPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String savedTheme = settingsPrefs.getString(KEY_THEME, THEME_SYSTEM);

            switch (savedTheme) {
                case THEME_LIGHT:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    Log.d(TAG, "Theme set to light mode");
                    break;
                case THEME_DARK:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    Log.d(TAG, "Theme set to dark mode");
                    break;
                case THEME_SYSTEM:
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    Log.d(TAG, "Theme set to follow system");
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing theme, using system default", e);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void initializeLanguage() {
        try {
            SharedPreferences settingsPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String languageCode = settingsPrefs.getString(KEY_LANGUAGE, "en");

            // Set locale based on saved language preference
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            Configuration config = getResources().getConfiguration();
            config.setLocale(locale);

            getResources().updateConfiguration(config, getResources().getDisplayMetrics());

            Log.d(TAG, "Language set to: " + languageCode);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing language, using default", e);
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

    /**
     * Verify all Firebase components are properly initialized and functional
     * @return A map with status of each Firebase component
     */
    public static Map<String, Boolean> verifyFirebaseComponents() {
        Map<String, Boolean> status = new HashMap<>();

        try {
            // Check base Firebase initialization
            boolean firebaseInitialized = !FirebaseApp.getApps(applicationContext).isEmpty();
            status.put("Firebase Core", firebaseInitialized);

            if (!firebaseInitialized) {
                Log.e(TAG, "Firebase core not initialized");
                return status;
            }

            // Check Firebase Auth
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                status.put("Firebase Auth", auth != null);
            } catch (Exception e) {
                Log.e(TAG, "Firebase Auth verification failed", e);
                status.put("Firebase Auth", false);
            }

            // Check Firebase Firestore
            try {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                status.put("Firebase Firestore", firestore != null);
            } catch (Exception e) {
                Log.e(TAG, "Firebase Firestore verification failed", e);
                status.put("Firebase Firestore", false);
            }

            // Check Firebase Realtime Database
            try {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                status.put("Firebase Database", database != null);
            } catch (Exception e) {
                Log.e(TAG, "Firebase Database verification failed", e);
                status.put("Firebase Database", false);
            }

            // Check Firebase Storage
            try {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                status.put("Firebase Storage", storage != null);
            } catch (Exception e) {
                Log.e(TAG, "Firebase Storage verification failed", e);
                status.put("Firebase Storage", false);
            }

            return status;
        } catch (Exception e) {
            Log.e(TAG, "Error verifying Firebase components", e);
            status.put("Error", false);
            return status;
        }
    }

    /**
     * Test Firebase connectivity by performing a simple read operation
     *
     * @param callback Callback to handle result
     */
    public static void testFirebaseConnectivity(FirebaseTestCallback callback) {
        try {
            // Check Firebase Firestore connectivity
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Set timeout for operation
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                Log.e(TAG, "Firebase connectivity test timed out");
                callback.onResult(false, "Operation timed out");
            };

            // Set a 5-second timeout
            handler.postDelayed(timeoutRunnable, 5000);

            // Try to read a test document
            db.collection("app_status").document("connectivity")
                    .get()
                    .addOnCompleteListener(task -> {
                        // Cancel the timeout
                        handler.removeCallbacks(timeoutRunnable);

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase connectivity test successful");
                            callback.onResult(true, "Connected successfully");
                        } else {
                            Log.e(TAG, "Firebase connectivity test failed", task.getException());
                            callback.onResult(false, "Connection failed: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error testing Firebase connectivity", e);
            callback.onResult(false, "Error: " + e.getMessage());
        }
    }

    /**
     * Callback interface for Firebase connectivity test
     */
    public interface FirebaseTestCallback {
        void onResult(boolean isConnected, String message);
    }

    /**
     * Check if Firebase Cloud Messaging is properly set up
     *
     * @return true if FCM is properly set up, false otherwise
     */
    public static boolean isFirebaseMessagingAvailable() {
        try {
            // First verify Firebase core is initialized
            if (!isFirebaseInitialized()) {
                Log.e(TAG, "Firebase core not initialized, FCM cannot work");
                return false;
            }

            // Verify Firebase Messaging class is available
            Class.forName("com.google.firebase.messaging.FirebaseMessaging");

            // Get FCM instance and verify it's working
            com.google.firebase.messaging.FirebaseMessaging fcm =
                    com.google.firebase.messaging.FirebaseMessaging.getInstance();

            // Check if we have an FCM token stored
            SharedPrefsManager prefsManager = new SharedPrefsManager(applicationContext);
            String token = prefsManager.getFcmToken();

            // If we don't have a token stored, request a new one 
            // (this will trigger onNewToken in the service)
            if (token == null || token.isEmpty()) {
                Log.d(TAG, "No FCM token found, requesting a new one");
                fcm.getToken()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String newToken = task.getResult();
                                Log.d(TAG, "New FCM token generated");
                                prefsManager.saveFcmToken(newToken);
                            } else {
                                Log.e(TAG, "Failed to get FCM token", task.getException());
                            }
                        });
                // Return true because we were able to request a token
                return true;
            }

            Log.d(TAG, "FCM is available and token exists");
            return true;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Firebase Messaging class not found", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking FCM availability", e);
            return false;
        }
    }
}