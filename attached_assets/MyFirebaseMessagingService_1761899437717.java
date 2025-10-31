package com.moho.wood;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.game.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "default_channel";

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        try {
            // Send token to your backend
            DeviceRegistrationManager deviceManager =
                    new DeviceRegistrationManager(this, "https://android-dashboard.magneseo.com");

            String appVersion = getAppVersion();
            deviceManager.registerDevice(token, appVersion, new DeviceRegistrationManager.RegistrationCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Device registered successfully with new token");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to register device with new token: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onNewToken: " + e.getMessage(), e);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        try {
            // Check if message contains a notification payload
            if (remoteMessage.getNotification() != null) {
                String title = remoteMessage.getNotification().getTitle();
                String body = remoteMessage.getNotification().getBody();

                if (title != null && body != null) {
                    sendNotification(title, body);
                }
            }

            // Check if message contains a data payload
            if (remoteMessage.getData().size() > 0) {
                Log.d(TAG, "Message data payload: " + remoteMessage.getData());

                // Handle data payload
                String notificationId = remoteMessage.getData().get("notification_id");
                String title = remoteMessage.getData().get("title");
                String message = remoteMessage.getData().get("message");

                if (title != null && message != null) {
                    sendNotification(title, message);

                    // Track notification received
                    if (notificationId != null) {
                        trackNotificationReceived(notificationId);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onMessageReceived: " + e.getMessage(), e);
        }
    }

    private void sendNotification(String title, String messageBody) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            int flags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, flags
            );

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.about_icon)
                            .setContentTitle(title)
                            .setContentText(messageBody)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Create notification channel for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Default Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Default notification channel");
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }

            if (notificationManager != null) {
                notificationManager.notify(0, notificationBuilder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage(), e);
        }
    }

    private void trackNotificationReceived(String notificationId) {
        try {
            DeviceRegistrationManager deviceManager =
                    new DeviceRegistrationManager(this, "https://android-dashboard.magneseo.com");

            deviceManager.trackNotificationEvent(notificationId, "received", null);
        } catch (Exception e) {
            Log.e(TAG, "Error tracking notification: " + e.getMessage(), e);
        }
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
            return "1.0.0";
        }
    }
}