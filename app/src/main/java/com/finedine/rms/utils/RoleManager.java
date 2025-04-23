package com.finedine.rms.utils;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class RoleManager {
    public static final String ROLE_CUSTOMER = "customer";
    public static final String ROLE_WAITER = "waiter";
    public static final String ROLE_CHEF = "chef";
    public static final String ROLE_MANAGER = "manager";

    public static boolean hasAccess(Context context, String requiredRole) {
        SharedPreferences prefs = context.getSharedPreferences("FineDinePrefs", MODE_PRIVATE);
        String userRole = prefs.getString("userRole", ROLE_CUSTOMER);

        switch (requiredRole) {
            case ROLE_MANAGER:
                return userRole.equals(ROLE_MANAGER);
            case ROLE_CHEF:
                return userRole.equals(ROLE_MANAGER) || userRole.equals(ROLE_CHEF);
            case ROLE_WAITER:
                return userRole.equals(ROLE_MANAGER) || userRole.equals(ROLE_WAITER);
            default:
                return true;
        }
    }

    public static void redirectIfUnauthorized(Activity activity, String requiredRole) {
        if (!hasAccess(activity, requiredRole)) {
            Toast.makeText(activity, "Unauthorized access", Toast.LENGTH_SHORT).show();
            activity.finish();
        }
    }

    public static String getCurrentUserRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("FineDinePrefs", MODE_PRIVATE);
        return prefs.getString("userRole", ROLE_CUSTOMER);
    }
}