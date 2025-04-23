package com.finedine.rms;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private static final String PREF_NAME = "FineDinePrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(int userId, String role, String name) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_USER_NAME, name);
        editor.commit();
    }

    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_USER_ID, String.valueOf(pref.getInt(KEY_USER_ID, -1)));
        user.put(KEY_USER_ROLE, pref.getString(KEY_USER_ROLE, null));
        user.put(KEY_USER_NAME, pref.getString(KEY_USER_NAME, null));
        return user;
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getInt(KEY_USER_ID, -1) != -1;
    }
}