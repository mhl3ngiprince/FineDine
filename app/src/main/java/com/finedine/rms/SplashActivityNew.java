package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivityNew extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d(TAG, "SplashActivity onCreate called");

        // Use a handler to delay the transition to LoginActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToLogin();
            }
        }, SPLASH_DELAY);
    }

    private void navigateToLogin() {
        try {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to login", e);
            finish();
        }
    }
}