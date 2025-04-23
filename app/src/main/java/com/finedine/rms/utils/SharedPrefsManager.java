package com.finedine.rms.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class SharedPrefsManager {
    private static final String PREFS_NAME = "FineDinePrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_LAST_SYNC = "last_sync";

    private final SharedPreferences prefs;

    public SharedPrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserSession(int userId, String role, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit()
                    .putInt(KEY_USER_ID, userId)
                    .putString(KEY_USER_ROLE, role)
                    .putString(KEY_USER_NAME, name)
                    .apply();
        }
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "customer");
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
}