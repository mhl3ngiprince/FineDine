package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Simple standalone splash activity
 */
public class SplashDemoActivity extends AppCompatActivity {

    private static final String TAG = "SplashDemoActivity";
    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d(TAG, "SplashDemoActivity creating");

        try {
            Toast.makeText(this, "Loading app...", Toast.LENGTH_SHORT).show();

            // Set a timer to delay transition
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Splash timer complete");
                    // Navigate to login
                    try {
                        Intent intent = new Intent(SplashDemoActivity.this, LoginDemoActivity.class);
                        startActivity(intent);
                        // Don't finish this activity to prevent the app from exiting
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating from splash: " + e.getMessage(), e);
                        Toast.makeText(SplashDemoActivity.this,
                                "Navigation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }, SPLASH_DURATION);

            Log.d(TAG, "SplashDemoActivity setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up splash: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}