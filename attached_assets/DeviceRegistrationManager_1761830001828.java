package com.moho.wood;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class DeviceRegistrationManager {
    private static final String TAG = "DeviceRegistration";
    private static final String PREF_DEVICE_ID = "device_id";
    private static final String PREF_FCM_TOKEN = "fcm_token";

    private Context context;
    private SharedPreferences preferences;
    private String baseUrl;
    private String packageName;

    public DeviceRegistrationManager(Context context, String baseUrl) {
        this.context = context;
        this.baseUrl = baseUrl;
        this.packageName = context.getPackageName();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Register device with backend
     */
    public void registerDevice(String fcmToken, String appVersion, final RegistrationCallback callback) {
        // Save FCM token
        preferences.edit().putString(PREF_FCM_TOKEN, fcmToken).apply();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/api/v1/device/register");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    JSONObject payload = new JSONObject();
                    payload.put("package_name", packageName);
                    payload.put("fcm_token", fcmToken);

                    JSONObject deviceInfo = new JSONObject();
                    deviceInfo.put("country", Locale.getDefault().getCountry());
                    deviceInfo.put("app_version", appVersion);
                    deviceInfo.put("android_version", Build.VERSION.SDK_INT);
                    deviceInfo.put("manufacturer", Build.MANUFACTURER);
                    deviceInfo.put("model", Build.MODEL);

                    payload.put("device_info", deviceInfo);

                    OutputStream os = connection.getOutputStream();
                    os.write(payload.toString().getBytes());
                    os.close();

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

                        // Parse and save device ID
                        JSONObject responseJson = new JSONObject(response.toString());
                        if (responseJson.has("device_id")) {
                            String deviceId = responseJson.getString("device_id");
                            preferences.edit().putString(PREF_DEVICE_ID, deviceId).apply();
                            Log.d(TAG, "Device registered successfully: " + deviceId);
                        }

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        Log.e(TAG, "Device registration failed: " + responseCode);
                        if (callback != null) {
                            callback.onError("Registration failed: " + responseCode);
                        }
                    }

                    connection.disconnect();

                } catch (Exception e) {
                    Log.e(TAG, "Error registering device: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * Get saved device ID
     */
    public String getDeviceId() {
        return preferences.getString(PREF_DEVICE_ID, null);
    }

    /**
     * Get saved FCM token
     */
    public String getFcmToken() {
        return preferences.getString(PREF_FCM_TOKEN, null);
    }

    /**
     * Track notification event
     */
    public void trackNotificationEvent(String notificationId, String event, final TrackingCallback callback) {
        String deviceId = getDeviceId();

        if (deviceId == null) {
            Log.w(TAG, "Device ID not available, cannot track notification event");
            if (callback != null) {
                callback.onError("Device not registered");
            }
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/api/v1/notifications/track");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    JSONObject payload = new JSONObject();
                    payload.put("notification_id", notificationId);
                    payload.put("device_id", deviceId);
                    payload.put("event", event);
                    payload.put("timestamp", System.currentTimeMillis());

                    OutputStream os = connection.getOutputStream();
                    os.write(payload.toString().getBytes());
                    os.close();

                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "Notification event tracked: " + event + " - " + responseCode);

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Tracking failed: " + responseCode);
                        }
                    }

                    connection.disconnect();

                } catch (Exception e) {
                    Log.e(TAG, "Error tracking notification: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * Callback interface for registration
     */
    public interface RegistrationCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Callback interface for tracking
     */
    public interface TrackingCallback {
        void onSuccess();
        void onError(String error);
    }
}