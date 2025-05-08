package com.finedine.rms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.finedine.rms.utils.SharedPrefsManager;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "FineDineNotificationChannel";
    private static final String CHANNEL_NAME = "Fine Dine Notifications";
    private static final String CHANNEL_DESC = "Notifications from Fine Dine Restaurant Management System";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );
        }

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Process data payload
            processDataPayload(remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Save the new token to shared preferences
        saveTokenToPrefs(token);

        // If you want to send the token to your server, do it here
        sendRegistrationToServer(token);
    }

    private void processDataPayload(Map<String, String> data) {
        try {
            // Check for notification type
            String notificationType = data.get("type");
            if (notificationType == null) {
                // Default processing if no type specified
                String title = data.get("title");
                String message = data.get("message");

                if (title != null && message != null) {
                    sendNotification(title, message);
                }
                return;
            }

            // Process based on notification type
            switch (notificationType) {
                case "order":
                    processOrderNotification(data);
                    break;
                case "kitchen":
                    processKitchenNotification(data);
                    break;
                case "reservation":
                    processReservationNotification(data);
                    break;
                default:
                    // Default notification with title and body from data
                    String title = data.get("title");
                    String message = data.get("message");

                    if (title != null && message != null) {
                        sendNotification(title, message);
                    }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing data payload", e);
        }
    }

    private void processOrderNotification(Map<String, String> data) {
        try {
            // Get order details
            String orderId = data.get("order_id");
            String orderStatus = data.get("status");
            String message = data.get("message");

            // Create notification title
            String title = "Order Update";
            if (orderId != null) {
                title = "Order #" + orderId + " Update";
            }

            // Create intent to open OrderActivity
            Intent intent = new Intent(this, OrderActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (orderId != null) {
                intent.putExtra("order_id", orderId);
            }

            // Send notification
            sendNotification(title, message, intent);
        } catch (Exception e) {
            Log.e(TAG, "Error processing order notification", e);
        }
    }

    private void processKitchenNotification(Map<String, String> data) {
        try {
            // Get kitchen notification details
            String title = data.get("title");
            String message = data.get("message");

            // Create intent to open KitchenActivity
            Intent intent = new Intent(this, KitchenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Send notification
            sendNotification(title != null ? title : "Kitchen Alert", message, intent);
        } catch (Exception e) {
            Log.e(TAG, "Error processing kitchen notification", e);
        }
    }

    private void processReservationNotification(Map<String, String> data) {
        try {
            // Get reservation details
            String reservationId = data.get("reservation_id");
            String customerName = data.get("customer_name");
            String message = data.get("message");

            // Create notification title
            String title = "Reservation Update";
            if (customerName != null) {
                title = "Reservation: " + customerName;
            }

            // Create intent to open ReservationActivity
            Intent intent = new Intent(this, ReservationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (reservationId != null) {
                intent.putExtra("reservation_id", reservationId);
            }

            // Send notification
            sendNotification(title, message, intent);
        } catch (Exception e) {
            Log.e(TAG, "Error processing reservation notification", e);
        }
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sendNotification(title, messageBody, intent);
    }

    private void sendNotification(String title, String messageBody, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT
        );

        String channelId = CHANNEL_ID;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // For Android Oreo and above, a notification channel is required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(getNotificationId(), notificationBuilder.build());
    }

    /**
     * Generate a unique ID for each notification to prevent overwriting
     */
    private int getNotificationId() {
        return (int) System.currentTimeMillis();
    }

    /**
     * Save FCM token to SharedPreferences
     */
    private void saveTokenToPrefs(String token) {
        try {
            SharedPrefsManager prefsManager = new SharedPrefsManager(getApplicationContext());
            prefsManager.saveFcmToken(token);
            Log.d(TAG, "FCM token saved to SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving FCM token to SharedPreferences", e);
        }
    }

    /**
     * Send token to app server if needed
     */
    private void sendRegistrationToServer(String token) {
        // TODO: implement sending token to server if needed
        // This could be sending the token to your backend server,
        // or storing it in Firebase Firestore/Database for use in messaging
        Log.d(TAG, "FCM token registration pending implementation");
    }
}