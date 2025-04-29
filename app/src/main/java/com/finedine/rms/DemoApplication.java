package com.finedine.rms;

import android.app.Application;
import android.util.Log;

/**
 * Simple application class for demo
 */
public class DemoApplication extends Application {
    private static final String TAG = "DemoApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Demo application starting");
    }
}