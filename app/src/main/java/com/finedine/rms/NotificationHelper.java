package com.finedine.rms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID = "finedine_channel_01";
    private static final String CHANNEL_NAME = "FineDine Notifications";

    public static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ (Android 8.0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("FineDine order and reservation notifications");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);

            // Register the channel with the system
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void sendOrderNotification(Context context, int orderId, int tableNumber) {
        Intent intent = new Intent(context, KitchenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                orderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning) // Make sure this resource exists
                .setContentTitle("New Order Received")
                .setContentText("Table #" + tableNumber)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.purple_500));

        sendNotification(context, orderId, builder);
    }

    public static void sendReservationNotification(Context context, String customerName, String time) {
        Intent intent = new Intent(context, ReservationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int)System.currentTimeMillis(), // Use timestamp as request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("New Reservation")
                .setContentText(customerName + " at " + time)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        sendNotification(context, (int)System.currentTimeMillis(), builder);
    }

    public static void sendLowStockNotification(Context context, String itemName) {
        Intent intent = new Intent(context, InventoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Low Stock Alert")
                .setContentText(itemName + " needs to be reordered")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.red));

        sendNotification(context, (int)System.currentTimeMillis(), builder);
    }

    private static void sendNotification(Context context, int notificationId, NotificationCompat.Builder builder) {
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted - notification won't be shown
                return;
            }
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }
}
