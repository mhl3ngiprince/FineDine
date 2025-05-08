package com.finedine.rms.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.finedine.rms.R;

import java.io.ByteArrayOutputStream;

public class ImageLoader {
    private static final String TAG = "ImageLoader";

    /**
     * Load a menu image from URL with improved error handling and caching
     */
    public static void loadMenuImage(Context context, String imageUrl, ImageView imageView) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context or ImageView is null");
            return;
        }

        try {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();

            Glide.with(context.getApplicationContext())
                    .load(imageUrl)
                    .apply(options)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load image: " + imageUrl, e);
                            return false; // Let Glide handle the error image
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            return false; // Let Glide handle the resource
                        }
                    })
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading menu image from URL", e);
            // Fallback to placeholder in case of exception
            imageView.setImageResource(R.drawable.ic_broken_image);
        }
    }

    /**
     * Load a menu image from resource ID with improved error handling and caching
     */
    public static void loadMenuImage(Context context, int resourceId, ImageView imageView) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context or ImageView is null");
            return;
        }

        try {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();

            Glide.with(context.getApplicationContext())
                    .load(resourceId)
                    .apply(options)
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading menu image from resource", e);
            // Fallback to placeholder in case of exception
            imageView.setImageResource(R.drawable.ic_broken_image);
        }
    }

    /**
     * Load a profile image from URL with circle crop
     */
    public static void loadProfileImage(Context context, String imageUrl, ImageView imageView) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context or ImageView is null");
            return;
        }

        try {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop();

            Glide.with(context.getApplicationContext())
                    .load(imageUrl)
                    .apply(options)
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile image from URL", e);
            // Fallback to placeholder in case of exception
            imageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    /**
     * Load a profile image from resource ID with circle crop
     */
    public static void loadProfileImage(Context context, int resourceId, ImageView imageView) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context or ImageView is null");
            return;
        }

        try {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop();

            Glide.with(context.getApplicationContext())
                    .load(resourceId)
                    .apply(options)
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile image from resource", e);
            // Fallback to placeholder in case of exception
            imageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    /**
     * Encode a bitmap image to base64 string
     */
    @SuppressLint("NewApi")
    public static String encodeImage(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Cannot encode null bitmap");
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Decode a base64 string to bitmap image
     */
    public static Bitmap decodeImage(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) {
            Log.e(TAG, "Cannot decode null or empty string");
            return null;
        }

        try {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding image", e);
            return null;
        }
    }

    /**
     * Clear image cache for a specific view
     */
    public static void clearImageCache(Context context, ImageView imageView) {
        if (context != null && imageView != null) {
            try {
                Glide.with(context.getApplicationContext()).clear(imageView);
            } catch (Exception e) {
                Log.e(TAG, "Error clearing image cache", e);
            }
        }
    }

    /**
     * Clear all Glide image caches
     */
    public static void clearAllCaches(Context context) {
        if (context != null) {
            try {
                Glide.get(context.getApplicationContext()).clearMemory();
                new Thread(() -> {
                    try {
                        Glide.get(context.getApplicationContext()).clearDiskCache();
                    } catch (Exception e) {
                        Log.e(TAG, "Error clearing disk cache", e);
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing caches", e);
            }
        }
    }
}