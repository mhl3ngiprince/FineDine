package com.finedine.rms.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Utility class to check if resources exist and are accessible
 */
public class ResourceChecker {
    private static final String TAG = "ResourceChecker";

    /**
     * Check if a drawable resource is available and accessible
     *
     * @param context The application or activity context
     * @param resId   The drawable resource ID to check
     * @return true if the resource is available, false otherwise
     */
    public static boolean isDrawableAvailable(@NonNull Context context, @DrawableRes int resId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, resId);
            return drawable != null;
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Drawable resource not found: " + resId, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking drawable resource: " + resId, e);
            return false;
        }
    }

    /**
     * Validate critical resources needed for the app to function
     *
     * @param context The application or activity context
     * @return true if all critical resources are available, false otherwise
     */
    public static boolean validateCriticalResources(@NonNull Context context) {
        try {
            // Add resource IDs that are critical for the app to function
            final int[] criticalDrawableIds = {
                    com.finedine.rms.R.drawable.logoj,
                    com.finedine.rms.R.drawable.bg_luxury_pattern,
                    com.finedine.rms.R.drawable.bg_luxe_frame,
                    com.finedine.rms.R.drawable.circle_burgundy,
                    com.finedine.rms.R.drawable.corner_gold
            };

            // Check each drawable
            for (int resId : criticalDrawableIds) {
                if (!isDrawableAvailable(context, resId)) {
                    Log.e(TAG, "Critical drawable resource missing: " + resId);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating critical resources", e);
            return false;
        }
    }
}