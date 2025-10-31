package com.moho.wood;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final String TAG = "NotificationManager";

    private Context context;
    private String baseUrl;
    private String packageName;

    public NotificationManager(Context context, String baseUrl) {
        this.context = context;
        this.baseUrl = baseUrl;
        this.packageName = context.getPackageName();
    }

    /**
     * Fetch pending notifications from backend
     */
    public void fetchPendingNotifications(final NotificationCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlString = baseUrl + "/api/v1/notifications/pending?package_name=" + packageName;
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        // Parse notifications
                        JSONObject responseJson = new JSONObject(response.toString());
                        List<PushNotification> notifications = new ArrayList<>();

                        if (responseJson.has("notifications")) {
                            JSONArray notificationsArray = responseJson.getJSONArray("notifications");

                            for (int i = 0; i < notificationsArray.length(); i++) {
                                JSONObject notifJson = notificationsArray.getJSONObject(i);
                                PushNotification notification = parseNotification(notifJson);
                                if (notification != null) {
                                    notifications.add(notification);
                                }
                            }
                        }

                        Log.d(TAG, "Fetched " + notifications.size() + " pending notifications");

                        if (callback != null) {
                            callback.onSuccess(notifications);
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch notifications: " + responseCode);
                        if (callback != null) {
                            callback.onError("Request failed: " + responseCode);
                        }
                    }

                    connection.disconnect();

                } catch (Exception e) {
                    Log.e(TAG, "Error fetching notifications: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * Parse notification from JSON
     */
    private PushNotification parseNotification(JSONObject json) {
        try {
            PushNotification notification = new PushNotification();

            notification.notificationId = json.optString("notification_id");
            notification.title = json.optString("title");
            notification.message = json.optString("message");
            notification.type = json.optString("type", "popup");
            notification.priority = json.optString("priority", "normal");

            // Parse content
            if (json.has("content")) {
                JSONObject content = json.getJSONObject("content");
                notification.imageUrl = content.optString("image_url", null);
                notification.actionButtonText = content.optString("action_button_text", null);
                notification.actionType = content.optString("action_type", null);
                notification.actionValue = content.optString("action_value", null);
                notification.cancelable = content.optBoolean("cancelable", true);
            }

            // Parse display rules
            if (json.has("display_rules")) {
                JSONObject displayRules = json.getJSONObject("display_rules");
                notification.maxDisplays = displayRules.optInt("max_displays", 1);
                notification.displayIntervalHours = displayRules.optInt("display_interval_hours", 24);
                notification.showOnAppLaunch = displayRules.optBoolean("show_on_app_launch", false);
            }

            return notification;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification: " + e.getMessage());
            return null;
        }
    }

    /**
     * Notification data class
     */
    public static class PushNotification {
        public String notificationId;
        public String title;
        public String message;
        public String type;
        public String priority;
        public String imageUrl;
        public String actionButtonText;
        public String actionType;
        public String actionValue;
        public boolean cancelable;
        public int maxDisplays;
        public int displayIntervalHours;
        public boolean showOnAppLaunch;
    }

    /**
     * Callback interface for fetching notifications
     */
    public interface NotificationCallback {
        void onSuccess(List<PushNotification> notifications);
        void onError(String error);
    }
}