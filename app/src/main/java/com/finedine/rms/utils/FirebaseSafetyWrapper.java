package com.finedine.rms.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

                // Get and log certificate fingerprints for easier integration
                String fingerprints = CertificateHelper.getCertificateFingerprints(context);
                Log.d(TAG, "Certificate fingerprints: \n" + fingerprints);

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

    /**
     * Safely get FirebaseStorage, returns null if unavailable
     */
    public static FirebaseStorage getStorageInstance(Context context) {
        try {
            // Try to initialize Firebase if needed
            if (!initializeFirebase(context)) {
                return null;
            }

            return FirebaseStorage.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FirebaseStorage instance", e);
            return null;
        }
    }

    /**
     * Save data to Firestore with timeout and fallback
     *
     * @param context        Application context
     * @param collectionPath Collection path
     * @param documentId     Document ID (optional, can be null for auto-generation)
     * @param data           Data to save
     * @param callback       Callback for result
     */
    public static void saveToFirestore(Context context, String collectionPath, String documentId,
                                       Map<String, Object> data, FirestoreCallback callback) {
        try {
            FirebaseFirestore db = getFirestoreInstance(context);
            if (db == null) {
                Log.e(TAG, "Firestore not available for save operation");
                if (callback != null) {
                    callback.onFailure("Firestore service unavailable");
                }
                return;
            }

            // Create document reference
            DocumentReference docRef;
            if (documentId != null && !documentId.isEmpty()) {
                docRef = db.collection(collectionPath).document(documentId);
            } else {
                docRef = db.collection(collectionPath).document();
            }

            // Set a timeout flag
            AtomicBoolean isTimedOut = new AtomicBoolean(false);

            // Create timeout handler on the main thread
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isTimedOut.getAndSet(true)) {
                    Log.w(TAG, "Firestore save operation timed out");
                    if (callback != null) {
                        callback.onFailure("Operation timed out");
                    }
                }
            }, 10000); // 10 second timeout

            // Perform the save operation with metadata
            Map<String, Object> enhancedData = new HashMap<>(data);
            enhancedData.put("timestamp", System.currentTimeMillis());
            enhancedData.put("device_id", android.provider.Settings.Secure.getString(
                    context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));

            docRef.set(enhancedData)
                    .addOnSuccessListener(result -> {
                        if (!isTimedOut.getAndSet(true)) {
                            Log.d(TAG, "Successfully saved document to " + collectionPath);
                            if (callback != null) {
                                callback.onSuccess(docRef.getId());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isTimedOut.getAndSet(true)) {
                            Log.e(TAG, "Error saving to Firestore: " + e.getMessage(), e);
                            if (callback != null) {
                                callback.onFailure(e.getMessage());
                            }
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in saveToFirestore", e);
            if (callback != null) {
                callback.onFailure("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Update Firebase user profile
     *
     * @param context     Application context
     * @param displayName User's display name
     * @param photoUrl    User's photo URL
     * @param callback    Callback for result
     */
    public static void updateUserProfile(Context context, String displayName, String photoUrl,
                                         FirebaseCallback callback) {
        try {
            FirebaseAuth auth = getAuthInstance(context);
            if (auth == null || auth.getCurrentUser() == null) {
                Log.e(TAG, "Firebase Auth not available or no user signed in");
                if (callback != null) {
                    callback.onFailure("Authentication service unavailable");
                }
                return;
            }

            // Build profile update request
            UserProfileChangeRequest.Builder profileBuilder =
                    new UserProfileChangeRequest.Builder();

            if (displayName != null && !displayName.isEmpty()) {
                profileBuilder.setDisplayName(displayName);
            }

            if (photoUrl != null && !photoUrl.isEmpty()) {
                try {
                    profileBuilder.setPhotoUri(Uri.parse(photoUrl));
                } catch (Exception e) {
                    Log.e(TAG, "Invalid photo URL: " + photoUrl, e);
                }
            }

            auth.getCurrentUser().updateProfile(profileBuilder.build())
                    .addOnSuccessListener(result -> {
                        Log.d(TAG, "User profile updated successfully");
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update user profile", e);
                        if (callback != null) {
                            callback.onFailure(e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error updating user profile", e);
            if (callback != null) {
                callback.onFailure("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Upload image to Firebase Storage
     *
     * @param context     Application context
     * @param imageUri    Local URI of the image to upload
     * @param storagePath Path in storage where to upload (e.g., "images/users/")
     * @param callback    Callback for result
     */
    public static void uploadImage(Context context, Uri imageUri, String storagePath,
                                   StorageCallback callback) {
        try {
            FirebaseStorage storage = getStorageInstance(context);
            if (storage == null) {
                Log.e(TAG, "Firebase Storage not available");
                if (callback != null) {
                    callback.onFailure("Storage service unavailable");
                }
                return;
            }

            // Generate a unique filename based on timestamp
            String filename = "image_" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = storage.getReference()
                    .child(storagePath)
                    .child(filename);

            // Start upload with progress tracking
            storageRef.putFile(imageUri)
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) /
                                taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "Upload progress: " + progress + "%");
                        if (callback != null) {
                            callback.onProgress((int) progress);
                        }
                    })
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Image uploaded successfully");

                        // Get the download URL
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            Log.d(TAG, "Image download URL: " + downloadUrl);
                            if (callback != null) {
                                callback.onSuccess(downloadUrl);
                            }
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL", e);
                            if (callback != null) {
                                callback.onFailure("Upload succeeded but failed to get URL: " + e.getMessage());
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image", e);
                        if (callback != null) {
                            callback.onFailure("Upload failed: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image", e);
            if (callback != null) {
                callback.onFailure("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Firebase operation callback interface
     */
    public interface FirebaseCallback {
        void onSuccess();

        void onFailure(String error);
    }

    /**
     * Firestore operation callback interface
     */
    public interface FirestoreCallback {
        void onSuccess(String documentId);

        void onFailure(String error);
    }

    /**
     * Storage operation callback interface
     */
    public interface StorageCallback {
        void onSuccess(String downloadUrl);

        void onProgress(int percentage);

        void onFailure(String error);
    }

    /**
     * Show certificate fingerprints in a dialog for Firebase setup
     *
     * @param context Activity context
     */
    public static void showCertificateFingerprints(Context context) {
        try {
            String fingerprints = CertificateHelper.getCertificateFingerprints(context);

            // Show dialog with fingerprint information
            new AlertDialog.Builder(context)
                    .setTitle("Firebase Certificate Fingerprints")
                    .setMessage(fingerprints)
                    .setPositiveButton("Copy SHA-256", (dialog, which) -> {
                        // Copy SHA-256 to clipboard
                        android.content.ClipboardManager clipboard =
                                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        String sha256 = extractSha256FromText(fingerprints);
                        android.content.ClipData clip =
                                android.content.ClipData.newPlainText("SHA-256 Fingerprint", sha256);
                        clipboard.setPrimaryClip(clip);
                        android.widget.Toast.makeText(context,
                                "SHA-256 fingerprint copied to clipboard",
                                android.widget.Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Close", null)
                    .show();

            // Also log the fingerprints for accessing from logcat
            Log.d(TAG, "Certificate fingerprints: \n" + fingerprints);
        } catch (Exception e) {
            Log.e(TAG, "Error showing certificate fingerprints", e);
            android.widget.Toast.makeText(context,
                    "Error retrieving certificate fingerprints: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Extract SHA-256 fingerprint from the full text
     */
    private static String extractSha256FromText(String text) {
        try {
            if (text == null || text.isEmpty()) {
                return "";
            }

            // Find the SHA-256 line
            int sha256Index = text.indexOf("SHA-256:");
            if (sha256Index != -1) {
                // Extract from after "SHA-256: " to the end of that line
                int startIndex = sha256Index + 9; // Length of "SHA-256: " is 9
                int endIndex = text.indexOf('\n', startIndex);
                if (endIndex == -1) {
                    endIndex = text.length();
                }
                return text.substring(startIndex, endIndex).trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting SHA-256", e);
        }
        return "";
    }
}