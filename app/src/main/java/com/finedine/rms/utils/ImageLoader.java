package com.finedine.rms.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import android.widget.ImageView;

//import com.bumptech.glide.Glide;

import com.bumptech.glide.Glide;
import com.finedine.rms.R;

import java.io.ByteArrayOutputStream;

//import missing.namespace.R;

public class ImageLoader {
    public static void loadMenuImage(Context context, String imageUrl, ImageView imageView) {
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(imageView);
    }

    public static void loadMenuImage(Context context, int resourceId, ImageView imageView) {
        Glide.with(context)
                .load(resourceId)
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(imageView);
    }

    public static void loadProfileImage(Context context, String imageUrl, ImageView imageView) {
        Glide.with(context)
                .load(imageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(imageView);
    }

    public static void loadProfileImage(Context context, int resourceId, ImageView imageView) {
        Glide.with(context)
                .load(resourceId)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(imageView);
    }

    @SuppressLint("NewApi")
    public static String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();

            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        }



        public static Bitmap decodeImage (String encodedImage){
            byte[] decodedBytes = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            }
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }
    }