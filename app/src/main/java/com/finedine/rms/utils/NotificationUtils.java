package com.finedine.rms.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.finedine.rms.InventoryActivity;
import com.finedine.rms.KitchenActivity;
import com.finedine.rms.R;

public class NotificationUtils {
    private static final String CHANNEL_NAME = "FineDine Notifications";
    private static final String KITCHEN_CHANNEL_ID = "fine_dine_kitchen_channel";
    private static final String KITCHEN_CHANNEL_NAME = "Kitchen Order Alerts";

    // Static channel IDs for different notification types
    public static final String NEW_ORDER_CHANNEL_ID = "fine_dine_new_order_channel";
    public static final String ORDER_READY_CHANNEL_ID = "fine_dine_order_ready_channel";
    public static final String WAITER_PICKUP_CHANNEL_ID = "fine_dine_waiter_pickup_channel";
    public static final int NOTIFICATION_ID_ORDER_PREFIX = 1000;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NEW_ORDER_CHANNEL_ID,
                    "New Order Notifications",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("FineDine new order notifications");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

            NotificationChannel kitchenChannel = new NotificationChannel(
                    KITCHEN_CHANNEL_ID,
                    KITCHEN_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);

            kitchenChannel.setDescription("FineDine kitchen staff order alerts");
            kitchenChannel.enableLights(true);
            kitchenChannel.setLightColor(Color.RED);
            kitchenChannel.enableVibration(true);
            kitchenChannel.setShowBadge(true);
            kitchenChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            manager.createNotificationChannel(kitchenChannel);

            NotificationChannel orderReadyChannel = new NotificationChannel(
                    ORDER_READY_CHANNEL_ID,
                    "Order Ready Notifications",
                    NotificationManager.IMPORTANCE_HIGH);

            orderReadyChannel.setDescription("FineDine order ready notifications");
            orderReadyChannel.enableLights(true);
            orderReadyChannel.setLightColor(Color.GREEN);
            orderReadyChannel.enableVibration(true);

            manager.createNotificationChannel(orderReadyChannel);

            NotificationChannel waiterPickupChannel = new NotificationChannel(
                    WAITER_PICKUP_CHANNEL_ID,
                    "Waiter Pickup Notifications",
                    NotificationManager.IMPORTANCE_HIGH);

            waiterPickupChannel.setDescription("FineDine waiter pickup notifications");
            waiterPickupChannel.enableLights(true);
            waiterPickupChannel.setLightColor(Color.YELLOW);
            waiterPickupChannel.enableVibration(true);

            manager.createNotificationChannel(waiterPickupChannel);
        }
    }

    public void sendOrderNotification(Context context, int orderId, int tableNumber) {
        try {
            if (context == null) {
                throw new NullPointerException("Cannot send notification with null context");
            }

            // Create notification channel if needed
            createNotificationChannel(context);

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NEW_ORDER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("New Order #" + orderId)
                    .setContentText("New order for Table " + tableNumber + " is ready to prepare")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 500, 250, 500});

            // Create pending intent for notification click
            Intent intent = new Intent(context, KitchenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("order_id", orderId);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, orderId,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(pendingIntent);

            // Show the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Check for notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(NOTIFICATION_ID_ORDER_PREFIX + orderId, builder.build());
                }
            } else {
                notificationManager.notify(NOTIFICATION_ID_ORDER_PREFIX + orderId, builder.build());
            }

            // Log success
            Log.d(this.getClass().getName(), "Order notification sent for Order #" + orderId + ", Table " + tableNumber);

            // Try to send Firebase Cloud message (if available)
            sendFirebaseCloudMessage(context, orderId, tableNumber);

        } catch (Exception e) {
            Log.e(this.getClass().getName(), "Error sending order notification: " + e.getMessage(), e);
            // Show toast as fallback
            showToastOnMainThread(context, "New order for Table " + tableNumber);
        }
    }

    // Try to send Firebase Cloud Message but don't let it block notifications if it fails
    private void sendFirebaseCloudMessage(Context context, int orderId, int tableNumber) {
        try {
            // Check if Firebase is available
            if (!isFirebaseAvailable()) {
                Log.w(this.getClass().getName(), "Firebase not available, skipping cloud message");
                return;
            }

            // This would normally send a Firebase Cloud Message
            Log.d(this.getClass().getName(), "Firebase message would be sent here for order #" + orderId);
        } catch (Exception e) {
            // Just log the error, don't let it affect the notification flow
            Log.e(this.getClass().getName(), "Error sending Firebase message: " + e.getMessage(), e);
        }
    }

    // Helper to check if Firebase is available
    private boolean isFirebaseAvailable() {
        try {
            // Try to access FirebaseApp to check if it's initialized
            Class<?> firebaseAppClass = Class.forName("com.google.firebase.FirebaseApp");
            return true;
        } catch (Exception e) {
            Log.w(this.getClass().getName(), "Firebase SDK not available: " + e.getMessage());
            return false;
        }
    }

    // Show toast from any thread
    private void showToastOnMainThread(Context context, String message) {
        try {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "Error showing toast", e);
        }
    }

    /**
     * Create and show a basic notification without any advanced options
     * Static helper method for emergency fallback notifications
     */
    public static void createBasicNotification(Context context, String title, String message, String channelId) {
        try {
            // If context is null we can't do anything
            if (context == null) return;

            // Create the notification builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            // Show notification if we have permission
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                }
            } else {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (Exception e) {
            Log.e("NotificationUtils", "Error creating basic notification: " + e.getMessage(), e);
        }
    }

    public void sendReservationNotification(Context context, String customerName, String time) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NEW_ORDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("New Reservation")
                .setContentText(customerName + " at " + time)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }

    public void sendLowStockNotification(Context context, String itemName) {
        Intent intent = new Intent(context, InventoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NEW_ORDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Low Stock Alert")
                .setContentText(itemName + " needs to be reordered")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.red));

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }

    public void sendOrderReadyNotification(Context context, int orderId, int tableNumber, String customerName) {
        Intent intent = new Intent(context, KitchenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                orderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ORDER_READY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Your Order is Ready!")
                .setContentText("Hi " + customerName + ", your order is ready for collection")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.purple_500))
                .setVibrate(new long[]{1000, 1000, 1000});

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify(orderId, builder.build());
    }

    public void sendWaiterPickupNotification(Context context, int orderId, int tableNumber) {
        Intent intent = new Intent(context, KitchenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                orderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WAITER_PICKUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Order Ready for Pickup")
                .setContentText("Order #" + orderId + " for Table #" + tableNumber + " is ready for service")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.purple_500))
                .setVibrate(new long[]{1000, 1000, 1000}); // More noticeable vibration

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Use a different notification ID for waiter to ensure both notifications appear
        NotificationManagerCompat.from(context).notify(orderId + 10000, builder.build());
    }

    public void sendNewOrderNotification(Context context, int orderId, int tableNumber) {
        Intent intent = new Intent(context, KitchenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                orderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NEW_ORDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("New Order Received")
                .setContentText("New order #" + orderId + " from Table #" + tableNumber + " is pending")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.blue))
                .setVibrate(new long[]{1000, 1000, 1000});

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify(orderId + 20000, builder.build());
    }
}