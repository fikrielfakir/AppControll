package com.moho.wood;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.game.R;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UtilsAdmob {
    private static final String TAG = "UtilsAdmob";
    protected Boolean is_testing = false;
    protected String system = "00";
    protected Boolean enable_banner = true;
    protected Boolean enable_inter  = true;
    protected Boolean enable_reward = true;
    protected Boolean banner_at_bottom = true;
    protected Boolean banner_not_overlap = false;
    protected AdView mAdView = null;
    protected MainActivity activity;
    protected InterstitialAd mInterstitialAd = null;
    protected RewardedAd mRewardedAd;
    protected String is_rewarded = "no";

    protected AdMobConfigManager configManager;
    private static final String BASE_URL = "https://android-dashboard.magneseo.com";

    // Add flag to prevent multiple initializations
    private boolean isInitializing = false;
    private boolean isInitialized = false;

    public void setContext(MainActivity act){
        activity = act;
    }

    @SuppressLint("HardwareIds")
    @SuppressWarnings( "deprecation" )
    public void init(){
        // Prevent multiple initializations
        if (isInitializing || isInitialized) {
            Log.w(TAG, "Already initializing or initialized");
            return;
        }

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "Activity is null or finishing, cannot initialize ads");
            return;
        }

        isInitializing = true;

        try {
            ApplicationInfo app = null;
            system = "00";
            try {
                app = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
                if (app != null && app.metaData != null) {
                    system = String.valueOf(app.metaData.getString("system"));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package not found: " + e.getMessage());
            }

            is_testing = activity.getResources().getBoolean(R.bool.is_testing);
            enable_banner = activity.getResources().getBoolean(R.bool.enable_banner);
            banner_at_bottom = activity.getResources().getBoolean(R.bool.banner_at_bottom);
            banner_not_overlap = activity.getResources().getBoolean(R.bool.banner_not_overlap);
            enable_inter  = activity.getResources().getBoolean(R.bool.enable_inter);
            enable_reward  = activity.getResources().getBoolean(R.bool.enable_reward);

            if(!isConnectionAvailable() || !Objects.equals(system, new String(Base64.decode("Q09ERTky", Base64.DEFAULT)))){
                enable_banner  = false;
                enable_inter   = false;
                enable_reward  = false;
            }

            configManager = new AdMobConfigManager(activity, BASE_URL);

            configManager.setDefaultIds(
                    activity.getResources().getString(R.string.id_banner),
                    activity.getResources().getString(R.string.id_inter),
                    activity.getResources().getString(R.string.id_reward)
            );

            if (configManager.needsUpdate()) {
                configManager.fetchConfig(new AdMobConfigManager.ConfigCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "AdMob config updated successfully");
                        initializeAds();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to fetch AdMob config: " + error);
                        initializeAds();
                    }
                });
            } else {
                Log.d(TAG, "Using cached AdMob config");
                initializeAds();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in init(): " + e.getMessage(), e);
            isInitializing = false;
        }
    }

    private void initializeAds() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            isInitializing = false;
            return;
        }

        try {
            if(!enable_banner){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        Log.d(TAG, "hide space of banner");
                        AdView banner = activity.findViewById(R.id.adView);
                        if (banner != null) {
                            banner.setVisibility(View.GONE);
                        }
                    }
                });
                isInitializing = false;
                isInitialized = true;
                return;
            }

            if(is_testing) {
                @SuppressLint("HardwareIds")
                String android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
                String deviceId = md5(android_id).toUpperCase();
                Log.d("device_id", "DEVICE ID : " + deviceId);
                List<String> testDevices = new ArrayList<>();
                testDevices.add(AdRequest.DEVICE_ID_EMULATOR);
                testDevices.add(deviceId);

                RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                        .setTestDeviceIds(testDevices)
                        .build();
                MobileAds.setRequestConfiguration(requestConfiguration);
            }

            MobileAds.initialize(activity, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    Log.d(TAG, "AdMob initialized");
                    isInitialized = true;
                    isInitializing = false;
                }
            });

            prepare_banner();
            prepare_inter();
            prepare_reward();

        } catch (Exception e) {
            Log.e(TAG, "Error in initializeAds(): " + e.getMessage(), e);
            isInitializing = false;
        }
    }

    protected void show_banner(Boolean visible){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            if (visible) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                            AdView banner = activity.findViewById(R.id.adView);
                            if (banner != null) {
                                banner.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            } else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                            AdView banner = activity.findViewById(R.id.adView);
                            if (banner != null) {
                                banner.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing/hiding banner: " + e.getMessage(), e);
        }
    }

    protected void prepare_banner(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if(!enable_banner) return;

        try {
            mAdView = activity.findViewById(R.id.adView);
            if (mAdView == null) {
                Log.e(TAG, "AdView is null, cannot prepare banner");
                return;
            }

            String bannerId = configManager.getBannerId();
            if (bannerId == null || bannerId.isEmpty()) {
                Log.e(TAG, "Banner ID is null or empty");
                return;
            }

            Log.d(TAG, "Using banner ID: " + bannerId);

            if(!banner_at_bottom){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                            Log.d(TAG, "move banner to top");
                            LinearLayout main = activity.findViewById(R.id.main);
                            AdView banner = activity.findViewById(R.id.adView);
                            if (main != null && banner != null) {
                                main.removeViewAt(1);
                                main.addView(banner, 0);
                            }
                        }
                    }
                });
            }

            if(!banner_not_overlap){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                            Log.d(TAG, "set banner overlap");
                            AdView banner = activity.findViewById(R.id.adView);
                            if (banner != null) {
                                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) banner.getLayoutParams();
                                if (params != null) {
                                    params.setMargins(0, -140,0,0);
                                }
                            }
                        }
                    }
                });
            }

            Bundle extras = new Bundle();
            extras.putString("npa", gdpr_personalized_ads());

            AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();
            mAdView.setAdUnitId(bannerId);
            mAdView.loadAd(adRequest);

            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "Banner loaded successfully");
                    if (configManager != null) {
                        configManager.trackAdEvent("impression", "banner", 0);
                    }
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    Log.d(TAG, "Error load banner : "+ adError.getMessage());
                }

                @Override
                public void onAdOpened() {
                }

                @Override
                public void onAdClicked() {
                    if (configManager != null) {
                        configManager.trackAdEvent("click", "banner", 0);
                    }
                }

                @Override
                public void onAdClosed() {
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing banner: " + e.getMessage(), e);
        }
    }

    protected void prepare_inter(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if(!enable_inter) return;

        try {
            // ✅ FIX: Use getInterstitialId() instead of getBannerId()
            String interstitialId = configManager.getInterstitialId();
            if (interstitialId == null || interstitialId.isEmpty()) {
                Log.e(TAG, "Interstitial ID is null or empty");
                return;
            }

            Log.d(TAG, "Using interstitial ID: " + interstitialId);

            Bundle extras = new Bundle();
            extras.putString("npa", gdpr_personalized_ads());

            AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();

            InterstitialAd.load(activity, interstitialId, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.i(TAG, "Interstitial loaded");
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial dismissed");
                            prepare_inter();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            Log.d(TAG, "Interstitial failed to show");
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            mInterstitialAd = null;
                            Log.d(TAG, "Interstitial shown");
                            if (configManager != null) {
                                configManager.trackAdEvent("impression", "interstitial", 0);
                            }
                        }

                        @Override
                        public void onAdClicked() {
                            if (configManager != null) {
                                configManager.trackAdEvent("click", "interstitial", 0);
                            }
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.i(TAG, "Interstitial failed: " + loadAdError.getMessage());
                    mInterstitialAd = null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing interstitial: " + e.getMessage(), e);
        }
    }

    public void show_inter(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if(!enable_inter) return;

        try {
            if (mInterstitialAd == null) {
                Log.d(TAG, "Interstitial not loaded yet");
                return;
            }

            Log.d(TAG, "Showing interstitial");
            mInterstitialAd.show(activity);
        } catch (Exception e) {
            Log.e(TAG, "Error showing interstitial: " + e.getMessage(), e);
        }
    }

    public void prepare_reward(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if(!enable_reward) return;

        try {
            String rewardedId = configManager.getRewardedId();
            if (rewardedId == null || rewardedId.isEmpty()) {
                Log.e(TAG, "Rewarded ID is null or empty");
                return;
            }

            Log.d(TAG, "Using rewarded ID: " + rewardedId);

            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(activity, rewardedId,
                    adRequest, new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Log.d(TAG, "Reward Failed: " + loadAdError.getMessage());
                            mRewardedAd = null;
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            mRewardedAd = rewardedAd;
                            Log.d(TAG, "Reward Ad loaded");
                            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdShowedFullScreenContent() {
                                    Log.d(TAG, "Reward Ad shown");
                                    if (configManager != null) {
                                        configManager.trackAdEvent("impression", "rewarded", 0);
                                    }
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    Log.d(TAG, "Reward Ad failed to show");
                                    is_rewarded = "no";
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "Reward Ad dismissed");
                                    mRewardedAd = null;
                                    is_rewarded = "no";
                                    prepare_reward();
                                }

                                @Override
                                public void onAdClicked() {
                                    if (configManager != null) {
                                        configManager.trackAdEvent("click", "rewarded", 0);
                                    }
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing rewarded ad: " + e.getMessage(), e);
        }
    }

    public void show_reward(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            if (mRewardedAd != null) {
                mRewardedAd.show(activity, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        Log.d(TAG, "User earned reward");
                        int rewardAmount = rewardItem.getAmount();
                        String rewardType = rewardItem.getType();
                        is_rewarded = "yes";

                        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                        activity.reward(is_rewarded);
                                    }
                                }
                            });
                        }
                    }
                });
            } else {
                Log.d(TAG, "Rewarded ad not ready");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing rewarded ad: " + e.getMessage(), e);
        }
    }

    public void on_pause(){
        try {
            if (mAdView != null) {
                if(enable_banner){
                    mAdView.pause();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on pause: " + e.getMessage(), e);
        }
    }

    public void on_resume(){
        try {
            if (mAdView != null) {
                if(enable_banner){
                    mAdView.resume();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on resume: " + e.getMessage(), e);
        }
    }

    public void on_destroy(){
        try {
            if (mAdView != null) {
                if(enable_banner) {
                    mAdView.destroy();
                }
            }
            mInterstitialAd = null;
            mRewardedAd = null;
            activity = null;
        } catch (Exception e) {
            Log.e(TAG, "Error on destroy: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings( "deprecation" )
    public boolean isConnectionAvailable(){
        if (activity == null) {
            return false;
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking connection: " + e.getMessage());
            return false;
        }
    }

    public String md5(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void disable_sounds(boolean val){
        try {
            MobileAds.setAppMuted(val);
        } catch (Exception e) {
            Log.e(TAG, "Error disabling sounds: " + e.getMessage(), e);
        }
    }

    public String gdpr_personalized_ads() {
        if (activity == null) {
            return "0";
        }

        try {
            if(!activity.getResources().getBoolean(R.bool.enable_gdpr)){
                return "0";
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.activity);
            return sharedPreferences.getString("IABTCF_VendorConsents", "0");
        } catch (Exception e) {
            Log.e(TAG, "Error getting GDPR consent: " + e.getMessage(), e);
            return "0";
        }
    }
}