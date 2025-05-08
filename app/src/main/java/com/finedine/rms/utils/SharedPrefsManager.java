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
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_EMAIL = "email_address";
    private static final String KEY_EMAIL_PASSWORD = "email_password";
    private static final String KEY_SECONDARY_EMAIL = "secondary_email";
    private static final String KEY_SECONDARY_PASSWORD = "secondary_password";
    private static final String KEY_FIREBASE_UID = "firebase_uid";
    private static final String KEY_RESTAURANT_ID = "restaurant_id";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_INSTALL_ID = "install_id";
    private static final String KEY_PRINTER_ENABLED = "printer_enabled";
    private static final String KEY_SMS_NOTIFICATIONS_ENABLED = "sms_notifications_enabled";
    private static final String KEY_KITCHEN_PHONE_NUMBER = "kitchen_phone_number";
    private static final String KEY_PENDING_SYNC_ORDERS = "pending_sync_orders";
    private static final String KEY_PREFIX_ORDER_DATA = "order_data_";
    private static final String KEY_PREFIX_ORDER_ITEMS = "order_items_";

    private final SharedPreferences prefs;
    private static SharedPrefsManager instance;

    public SharedPrefsManager(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null in SharedPrefsManager constructor");
            throw new IllegalArgumentException("Context cannot be null");
        }

        try {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            // Test immediately if we can access the prefs
            prefs.getBoolean(KEY_IS_LOGGED_IN, false);
            Log.d(TAG, "SharedPreferences initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SharedPreferences", e);
            throw e; // Re-throw to let caller know initialization failed
        }
    }

    public static synchronized SharedPrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefsManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveUserSession(int userId, String role, String name, String email) {
        Log.d(TAG, "Saving user session - userId: " + userId + ", role: " + role + ", name: " + name + ", email: " + email);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_USER_ID, userId);
                editor.putString(KEY_USER_NAME, name);
                editor.putString(KEY_USER_EMAIL, email);

                // Always set the role explicitly to ensure it's saved correctly
                if (role != null && !role.isEmpty()) {
                    Log.d(TAG, "Explicitly setting role: " + role);
                    editor.putString(KEY_USER_ROLE, role);
                } else {
                    Log.w(TAG, "Role is null or empty, using default 'customer'");
                    editor.putString(KEY_USER_ROLE, "customer");
                }

                // Always set user as logged in when saving a session
                editor.putBoolean(KEY_IS_LOGGED_IN, true);

                // Use commit() instead of apply() to ensure immediate write
                boolean success = editor.commit();

                if (success) {
                    Log.d(TAG, "User session saved successfully with commit()");
                } else {
                    Log.w(TAG, "Commit returned false, session may not be saved properly");
                }
            } else {
                Log.w(TAG, "Not saving session due to old Android version");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving user session", e);
        }

        // Double check that role was saved correctly
        String savedRole = getUserRole();
        boolean isLoggedIn = isUserLoggedIn();
        Log.d(TAG, "User session saved. Role: " + savedRole + ", IsLoggedIn: " + isLoggedIn);

        // If role wasn't saved correctly, try a direct approach
        if (role != null && !role.equals(savedRole)) {
            Log.w(TAG, "Role mismatch, forcing direct role setting");
            setUserRole(role);
        }

        // If login status wasn't saved correctly, try a direct approach
        if (!isLoggedIn) {
            Log.w(TAG, "Login status mismatch, forcing direct login setting");
            setUserLoggedIn(true);
        }
    }

    /**
     * Overloaded method for backward compatibility - creates a default email from the name
     */
    public void saveUserSession(int userId, String role, String name) {
        String email = name != null ? name.toLowerCase().replace(" ", "_") + "@user.finedine.com" : "user@finedine.com";
        saveUserSession(userId, role, name, email);
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

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void clearUserSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit()
                    .remove(KEY_USER_ID)
                    .remove(KEY_USER_ROLE)
                    .remove(KEY_USER_NAME)
                    .remove(KEY_USER_EMAIL)
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

        // Verify the value was saved correctly
        boolean storedValue = isUserLoggedIn();
        if (storedValue != isLoggedIn) {
            Log.w(TAG, "Login status mismatch! Expected: " + isLoggedIn + ", Got: " + storedValue);
            // Try using commit instead of apply as a fallback
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).commit();
        }
    }

    public boolean isUserLoggedIn() {
        boolean loggedIn = false;
        try {
            loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
            Log.d(TAG, "Checking if user is logged in: " + loggedIn);
        } catch (Exception e) {
            Log.e(TAG, "Error checking login status, assuming not logged in", e);
        }
        return loggedIn;
    }

    public void saveEmail(String email) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_EMAIL, email).apply();
        }
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void saveEmailPassword(String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_EMAIL_PASSWORD, password).apply();
        }
    }

    public String getEmailPassword() {
        return prefs.getString(KEY_EMAIL_PASSWORD, "");
    }

    public void saveSecondaryEmail(String email) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_SECONDARY_EMAIL, email).apply();
        }
    }

    public String getSecondaryEmail() {
        return prefs.getString(KEY_SECONDARY_EMAIL, "");
    }

    public void saveSecondaryEmailPassword(String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_SECONDARY_PASSWORD, password).apply();
        }
    }

    public String getSecondaryEmailPassword() {
        return prefs.getString(KEY_SECONDARY_PASSWORD, "");
    }

    public void setEmailCredentials(String email, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_EMAIL_PASSWORD, password);
            editor.apply();
        }
    }

    public void setSecondaryEmailCredentials(String email, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_SECONDARY_EMAIL, email);
            editor.putString(KEY_SECONDARY_PASSWORD, password);
            editor.apply();
        }
    }

    public void saveFirebaseUid(String uid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_FIREBASE_UID, uid).apply();
        }
    }

    public String getFirebaseUid() {
        return prefs.getString(KEY_FIREBASE_UID, "");
    }

    public String getRestaurantId() {
        return prefs.getString(KEY_RESTAURANT_ID, "1");
    }

    public void setRestaurantId(String restaurantId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_RESTAURANT_ID, restaurantId).apply();
        }
    }

    public String getApiToken() {
        return prefs.getString(KEY_API_TOKEN, null);
    }

    public void setApiToken(String token) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_API_TOKEN, token).apply();
        }
    }

    public String getInstallId() {
        String installId = prefs.getString(KEY_INSTALL_ID, null);
        if (installId == null) {
            // Generate a new install ID if none exists
            installId = "FDRMS-" + java.util.UUID.randomUUID().toString();
            setInstallId(installId);
        }
        return installId;
    }

    private void setInstallId(String installId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_INSTALL_ID, installId).apply();
        }
    }

    public boolean isPrinterEnabled() {
        return prefs.getBoolean(KEY_PRINTER_ENABLED, false);
    }

    public void setPrinterEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putBoolean(KEY_PRINTER_ENABLED, enabled).apply();
        }
    }

    public boolean isSmsNotificationsEnabled() {
        return prefs.getBoolean(KEY_SMS_NOTIFICATIONS_ENABLED, false);
    }

    public void setSmsNotificationsEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putBoolean(KEY_SMS_NOTIFICATIONS_ENABLED, enabled).apply();
        }
    }

    public String getKitchenPhoneNumber() {
        return prefs.getString(KEY_KITCHEN_PHONE_NUMBER, null);
    }

    public void setKitchenPhoneNumber(String phoneNumber) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().putString(KEY_KITCHEN_PHONE_NUMBER, phoneNumber).apply();
        }
    }

    public void addPendingSyncOrder(String orderId) {
        try {
            // Get current list of pending orders
            java.util.Set<String> pendingOrders = getPendingSyncOrders();

            // Add the new order ID
            pendingOrders.add(orderId);

            // Save the updated list
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                prefs.edit().putStringSet(KEY_PENDING_SYNC_ORDERS, pendingOrders).apply();
            } else {
                // Fallback for older Android versions
                String ordersList = android.text.TextUtils.join(",", pendingOrders);
                prefs.edit().putString(KEY_PENDING_SYNC_ORDERS, ordersList).apply();
            }

            Log.d(TAG, "Added order to pending sync list: " + orderId);
        } catch (Exception e) {
            Log.e(TAG, "Error adding pending sync order", e);
        }
    }

    public java.util.Set<String> getPendingSyncOrders() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                return prefs.getStringSet(KEY_PENDING_SYNC_ORDERS, new java.util.HashSet<String>());
            } else {
                // Fallback for older Android versions
                String ordersList = prefs.getString(KEY_PENDING_SYNC_ORDERS, "");
                java.util.Set<String> result = new java.util.HashSet<>();
                if (!ordersList.isEmpty()) {
                    String[] orders = ordersList.split(",");
                    for (String order : orders) {
                        result.add(order.trim());
                    }
                }
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting pending sync orders", e);
            return new java.util.HashSet<>();
        }
    }

    public void removePendingSyncOrder(String orderId) {
        try {
            // Get current list of pending orders
            java.util.Set<String> pendingOrders = getPendingSyncOrders();

            // Remove the order ID
            pendingOrders.remove(orderId);

            // Save the updated list
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                prefs.edit().putStringSet(KEY_PENDING_SYNC_ORDERS, pendingOrders).apply();
            } else {
                // Fallback for older Android versions
                String ordersList = android.text.TextUtils.join(",", pendingOrders);
                prefs.edit().putString(KEY_PENDING_SYNC_ORDERS, ordersList).apply();
            }

            // Also remove the order data
            prefs.edit()
                    .remove(KEY_PREFIX_ORDER_DATA + orderId)
                    .remove(KEY_PREFIX_ORDER_ITEMS + orderId)
                    .apply();

            Log.d(TAG, "Removed order from pending sync list: " + orderId);
        } catch (Exception e) {
            Log.e(TAG, "Error removing pending sync order", e);
        }
    }

    public void storePendingOrderData(String orderId, String orderJson, String itemsJson) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_PREFIX_ORDER_DATA + orderId, orderJson);
                editor.putString(KEY_PREFIX_ORDER_ITEMS + orderId, itemsJson);
                editor.apply();
                Log.d(TAG, "Stored pending order data for: " + orderId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing pending order data", e);
        }
    }

    public String getPendingOrderData(String orderId) {
        return prefs.getString(KEY_PREFIX_ORDER_DATA + orderId, null);
    }

    public String getPendingOrderItems(String orderId) {
        return prefs.getString(KEY_PREFIX_ORDER_ITEMS + orderId, null);
    }
}