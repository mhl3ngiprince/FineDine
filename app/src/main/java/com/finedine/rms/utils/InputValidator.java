package com.finedine.rms.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.RequiresApi;

public class InputValidator {

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.length() >= 2;
    }


    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public static boolean isValidPhone(String phone) {
        return !TextUtils.isEmpty(phone) &&
                Patterns.PHONE.matcher(phone).matches();
    }

    public static boolean isValidPrice(String price) {
        try {
            double value = Double.parseDouble(price);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidQuantity(String quantity) {
        try {
            int qty = Integer.parseInt(quantity);
            return qty > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}