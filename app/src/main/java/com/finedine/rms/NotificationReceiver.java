package com.finedine.rms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract any data from the intent
        String action = intent.getAction();
        Log.d(TAG, "Received notification action: " + action);

        if ("com.finedine.rms.NOTIFICATION_CLICKED".equals(action)) {
            // Handle notification click
            handleNotificationClick(context, intent);
        } else if ("com.finedine.rms.NOTIFICATION_DISMISSED".equals(action)) {
            // Handle notification dismiss
            handleNotificationDismiss(context, intent);
        }
    }

    private void handleNotificationClick(Context context, Intent intent) {
        // Get any extras from the intent
        String orderId = intent.getStringExtra("order_id");

        if (orderId != null) {
            // Open relevant activity based on the notification data
            Intent activityIntent = new Intent(context, OrderActivity.class);
            activityIntent.putExtra("order_id", orderId);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }

    private void handleNotificationDismiss(Context context, Intent intent) {
        // Perform any cleanup or logging when notification is dismissed
        Log.d(TAG, "Notification dismissed");
    }
}