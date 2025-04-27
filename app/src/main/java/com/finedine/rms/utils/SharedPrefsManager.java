package com.finedine.rms.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class SharedPrefsManager {
    private static final String TAG = "SharedPrefsManager";
    private static final String PREFS_NAME = "FineDinePrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences prefs;

    public SharedPrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserSession(int userId, String role, String name) {
        Log.d(TAG, "Saving user session - userId: " + userId + ", role: " + role + ", name: " + name);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_USER_ID, userId);
                editor.putString(KEY_USER_NAME, name);

                // Always set the role explicitly to ensure it's saved correctly
                if (role != null && !role.isEmpty()) {
                    Log.d(TAG, "Explicitly setting role: " + role);
                    editor.putString(KEY_USER_ROLE, role);
                } else {
                    Log.w(TAG, "Role is null or empty, using default 'customer'");
                    editor.putString(KEY_USER_ROLE, "customer");
                }

                editor.apply();
                Log.d(TAG, "User session saved successfully");
            } else {
                Log.w(TAG, "Not saving session due to old Android version");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving user session", e);
        }

        // Double check that role was saved correctly
        String savedRole = getUserRole();
        Log.d(TAG, "User session saved. Role confirmed: " + savedRole);

        // If role wasn't saved correctly, try a direct approach
        if (role != null && !role.equals(savedRole)) {
            Log.w(TAG, "Role mismatch, forcing direct role setting");
            setUserRole(role);
        }
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserRole() {
        String role = prefs.getString(KEY_USER_ROLE, "customer");
        Log.d(TAG, "Getting user role from SharedPreferences: " + role);
        return role;
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void clearUserSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit()
                    .remove(KEY_USER_ID)
                    .remove(KEY_USER_ROLE)
                    .remove(KEY_USER_NAME)
                    .remove(KEY_IS_LOGGED_IN)
                    .apply();
        }
    }

    public void saveFcmToken(String token) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        }
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    public void setLastSyncTime(long timestamp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply();
        }
    }

    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }

    // Helper method to directly set the role (for debugging)
    public void setUserRole(String role) {
        Log.d(TAG, "DIRECTLY setting user role to: " + role);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit()
                    .putString(KEY_USER_ROLE, role)
                    .apply();
        }
    }

    public void setUserLoggedIn(boolean isLoggedIn) {
        Log.d(TAG, "Setting user logged in status to: " + isLoggedIn);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
                    .apply();
        }
    }

    public boolean isUserLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
}