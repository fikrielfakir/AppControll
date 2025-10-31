package com.moho.wood;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdMobConfigManager {
    private static final String TAG = "AdMobConfigManager";
    private static final String PREF_BANNER_ID = "admob_banner_id";
    private static final String PREF_INTERSTITIAL_ID = "admob_interstitial_id";
    private static final String PREF_REWARDED_ID = "admob_rewarded_id";
    private static final String PREF_APP_OPEN_ID = "admob_app_open_id";
    private static final String PREF_NATIVE_ID = "admob_native_id";
    private static final String PREF_ACCOUNT_ID = "current_account_id";
    private static final String PREF_LAST_UPDATE = "admob_last_update";
    private static final long UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours

    private Context context;
    private SharedPreferences preferences;
    private String baseUrl;
    private String packageName;

    // Default IDs (fallback)
    private String defaultBannerId;
    private String defaultInterstitialId;
    private String defaultRewardedId;

    public AdMobConfigManager(Context context, String baseUrl) {
        this.context = context;
        this.baseUrl = baseUrl;
        this.packageName = context.getPackageName();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Set default AdMob IDs as fallback
     */
    public void setDefaultIds(String bannerId, String interstitialId, String rewardedId) {
        this.defaultBannerId = bannerId;
        this.defaultInterstitialId = interstitialId;
        this.defaultRewardedId = rewardedId;
    }

    /**
     * Fetch AdMob configuration from server
     */
    public void fetchConfig(final ConfigCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String configUrl = baseUrl + "/api/v1/config/" + packageName;
                    URL url = new URL(configUrl);
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

                        parseAndSaveConfig(response.toString());

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        Log.e(TAG, "Server returned error: " + responseCode);
                        if (callback != null) {
                            callback.onError("Server error: " + responseCode);
                        }
                    }
                    connection.disconnect();

                } catch (Exception e) {
                    Log.e(TAG, "Error fetching config: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * Parse JSON config and save to SharedPreferences
     */
    private void parseAndSaveConfig(String jsonResponse) {
        try {
            JSONObject config = new JSONObject(jsonResponse);
            SharedPreferences.Editor editor = preferences.edit();

            // Get the first active AdMob account
            if (config.has("admob_accounts")) {
                JSONArray accounts = config.getJSONArray("admob_accounts");

                if (accounts.length() > 0) {
                    // Find first active account or use first account
                    JSONObject activeAccount = null;

                    for (int i = 0; i < accounts.length(); i++) {
                        JSONObject account = accounts.getJSONObject(i);
                        if (account.has("status") && account.getString("status").equals("active")) {
                            activeAccount = account;
                            break;
                        }
                    }

                    // If no active account found, use first one
                    if (activeAccount == null && accounts.length() > 0) {
                        activeAccount = accounts.getJSONObject(0);
                    }

                    if (activeAccount != null) {
                        if (activeAccount.has("banner_id")) {
                            editor.putString(PREF_BANNER_ID, activeAccount.getString("banner_id"));
                        }

                        if (activeAccount.has("interstitial_id")) {
                            editor.putString(PREF_INTERSTITIAL_ID, activeAccount.getString("interstitial_id"));
                        }

                        if (activeAccount.has("rewarded_id")) {
                            editor.putString(PREF_REWARDED_ID, activeAccount.getString("rewarded_id"));
                        }

                        if (activeAccount.has("app_open_id")) {
                            editor.putString(PREF_APP_OPEN_ID, activeAccount.getString("app_open_id"));
                        }

                        if (activeAccount.has("native_id")) {
                            editor.putString(PREF_NATIVE_ID, activeAccount.getString("native_id"));
                        }

                        // Store account ID for analytics tracking
                        if (activeAccount.has("account_id")) {
                            editor.putString(PREF_ACCOUNT_ID, activeAccount.getString("account_id"));
                        }
                    }
                }
            }

            editor.putLong(PREF_LAST_UPDATE, System.currentTimeMillis());
            editor.apply();

            Log.d(TAG, "AdMob config saved successfully");

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing config: " + e.getMessage());
        }
    }

    /**
     * Track AdMob analytics event
     */
    public void trackAdEvent(String event, String adType, int value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String accountId = preferences.getString(PREF_ACCOUNT_ID, "");

                    URL url = new URL(baseUrl + "/api/v1/analytics/admob");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("package_name", packageName);
                    payload.put("account_id", accountId);
                    payload.put("event", event);
                    payload.put("ad_type", adType);
                    payload.put("value", value);

                    OutputStream os = connection.getOutputStream();
                    os.write(payload.toString().getBytes());
                    os.close();

                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "Analytics tracked: " + responseCode);

                    connection.disconnect();

                } catch (Exception e) {
                    Log.e(TAG, "Error tracking analytics: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Check if config needs update
     */
    public boolean needsUpdate() {
        long lastUpdate = preferences.getLong(PREF_LAST_UPDATE, 0);
        return (System.currentTimeMillis() - lastUpdate) > UPDATE_INTERVAL;
    }

    /**
     * Get banner ad unit ID
     */
    public String getBannerId() {
        return preferences.getString(PREF_BANNER_ID, defaultBannerId);
    }

    /**
     * Get interstitial ad unit ID
     */
    public String getInterstitialId() {
        return preferences.getString(PREF_INTERSTITIAL_ID, defaultInterstitialId);
    }

    /**
     * Get rewarded ad unit ID
     */
    public String getRewardedId() {
        return preferences.getString(PREF_REWARDED_ID, defaultRewardedId);
    }

    /**
     * Get app open ad unit ID
     */
    public String getAppOpenId() {
        return preferences.getString(PREF_APP_OPEN_ID, null);
    }

    /**
     * Get native ad unit ID
     */
    public String getNativeId() {
        return preferences.getString(PREF_NATIVE_ID, null);
    }

    /**
     * Force update config from server
     */
    public void forceUpdate(ConfigCallback callback) {
        fetchConfig(callback);
    }

    /**
     * Clear saved config (for testing)
     */
    public void clearConfig() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREF_BANNER_ID);
        editor.remove(PREF_INTERSTITIAL_ID);
        editor.remove(PREF_REWARDED_ID);
        editor.remove(PREF_APP_OPEN_ID);
        editor.remove(PREF_NATIVE_ID);
        editor.remove(PREF_ACCOUNT_ID);
        editor.remove(PREF_LAST_UPDATE);
        editor.apply();
    }

    /**
     * Callback interface for async config fetch
     */
    public interface ConfigCallback {
        void onSuccess();
        void onError(String error);
    }
}