package com.moho.wood;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
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

    // AdMob Config Manager
    protected AdMobConfigManager configManager;
    private static final String BASE_URL = "https://your-replit-app.replit.app";

    public void setContext(MainActivity act){
        activity = act;
    }

    @SuppressLint("HardwareIds")
    @SuppressWarnings( "deprecation" )
    public void init(){
        ApplicationInfo app = null;
        system = "00";
        try {
            app = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
            system = String.valueOf(app.metaData.getString("system"));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
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

        // Initialize AdMob Config Manager
        configManager = new AdMobConfigManager(activity, BASE_URL);

        // Set default IDs from resources (fallback)
        configManager.setDefaultIds(
                activity.getResources().getString(R.string.id_banner),
                activity.getResources().getString(R.string.id_inter),
                activity.getResources().getString(R.string.id_reward)
        );

        // Fetch config from server if needed
        if (configManager.needsUpdate()) {
            configManager.fetchConfig(new AdMobConfigManager.ConfigCallback() {
                @Override
                public void onSuccess() {
                    Log.d("Jacob_mlk", "AdMob config updated successfully");
                    initializeAds();
                }

                @Override
                public void onError(String error) {
                    Log.e("Jacob_mlk", "Failed to fetch AdMob config: " + error);
                    // Use cached or default IDs
                    initializeAds();
                }
            });
        } else {
            // Use cached config
            Log.d("Jacob_mlk", "Using cached AdMob config");
            initializeAds();
        }
    }

    private void initializeAds() {
        if(!enable_banner){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Jacob_mlk", "hide space of banner");
                    AdView banner = activity.findViewById(R.id.adView);
                    banner.setVisibility(View.GONE);
                }
            });
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
                Log.d("Jacob_mlk", "AdMob initialized");
            }
        });

        prepare_banner();
        prepare_inter();
        prepare_reward();
    }

    protected void show_banner(Boolean visible){
        if (visible) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AdView banner = activity.findViewById(R.id.adView);
                    banner.setVisibility(View.VISIBLE);
                }
            });
        } else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AdView banner = activity.findViewById(R.id.adView);
                    banner.setVisibility(View.GONE);
                }
            });
        }
    }

    protected void prepare_banner(){
        if(!enable_banner) return;

        mAdView = activity.findViewById(R.id.adView);

        // Get banner ID from config manager
        String bannerId = configManager.getBannerId();
        Log.d("Jacob_mlk", "Using banner ID: " + bannerId);

        if(!banner_at_bottom){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Jacob_mlk", "move banner to top");
                    LinearLayout main = activity.findViewById(R.id.main);
                    AdView banner = activity.findViewById(R.id.adView);
                    main.removeViewAt(1);
                    main.addView(banner, 0);
                }
            });
        }

        if(!banner_not_overlap){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Jacob_mlk", "set banner overlap");
                    AdView banner = activity.findViewById(R.id.adView);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) banner.getLayoutParams();
                    params.setMargins(0, -140,0,0);
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
                Log.d("Jacob", "Banner loaded successfully");
                // Track impression
                configManager.trackAdEvent("impression", "banner", 0);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                Log.d("Jacob", "Error load banner : "+ adError.getMessage());
            }

            @Override
            public void onAdOpened() {
            }

            @Override
            public void onAdClicked() {
                // Track click
                configManager.trackAdEvent("click", "banner", 0);
            }

            @Override
            public void onAdClosed() {
            }
        });
    }

    protected void prepare_inter(){
        if(!enable_inter) return;

        // Get interstitial ID from config manager
        String interstitialId = configManager.getInterstitialId();
        Log.d("Jacob_mlk", "Using interstitial ID: " + interstitialId);

        Bundle extras = new Bundle();
        extras.putString("npa", gdpr_personalized_ads());

        AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();

        InterstitialAd.load(activity, interstitialId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.i("Jacob", "Interstitial loaded");
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d("Jacob", "Interstitial dismissed");
                        prepare_inter();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.d("Jacob", "Interstitial failed to show");
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        mInterstitialAd = null;
                        Log.d("Jacob", "Interstitial shown");
                        // Track impression
                        configManager.trackAdEvent("impression", "interstitial", 0);
                    }

                    @Override
                    public void onAdClicked() {
                        // Track click
                        configManager.trackAdEvent("click", "interstitial", 0);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.i("Jacob", "Interstitial failed: " + loadAdError.getMessage());
                mInterstitialAd = null;
            }
        });
    }

    public void show_inter(){
        if(!enable_inter) return;

        if (mInterstitialAd == null) {
            Log.d("Jacob", "Interstitial not loaded yet");
            return;
        }

        Log.d("Jacob", "Showing interstitial");
        mInterstitialAd.show(activity);
    }

    public void prepare_reward(){
        if(!enable_reward) return;

        // Get rewarded ID from config manager
        String rewardedId = configManager.getRewardedId();
        Log.d("Jacob_mlk", "Using rewarded ID: " + rewardedId);

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(activity, rewardedId,
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d("Jacob Reward", "Failed: " + loadAdError.getMessage());
                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                        Log.d("Jacob Reward", "Ad loaded");
                        mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d("Jacob Reward", "Ad shown");
                                // Track impression
                                configManager.trackAdEvent("impression", "rewarded", 0);
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                Log.d("Jacob Reward", "Ad failed to show");
                                is_rewarded = "no";
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d("Jacob Reward", "Ad dismissed");
                                mRewardedAd = null;
                                is_rewarded = "no";
                                prepare_reward();
                            }

                            @Override
                            public void onAdClicked() {
                                // Track click
                                configManager.trackAdEvent("click", "rewarded", 0);
                            }
                        });
                    }
                });
    }

    public void show_reward(){
        if (mRewardedAd != null) {
            mRewardedAd.show(activity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    Log.d("Jacob Reward", "User earned reward");
                    int rewardAmount = rewardItem.getAmount();
                    String rewardType = rewardItem.getType();
                    is_rewarded = "yes";

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.reward(is_rewarded);
                        }
                    });
                }
            });
        } else {
            Log.d("Jacob Reward", "Rewarded ad not ready");
        }
    }

    public void on_pause(){
        if (mAdView != null) {
            if(enable_banner){
                mAdView.pause();
            }
        }
    }

    public void on_resume(){
        if (mAdView != null) {
            if(enable_banner){
                mAdView.resume();
            }
        }
    }

    public void on_destroy(){
        if (mAdView != null) {
            if(enable_banner) {
                mAdView.destroy();
            }
        }
    }

    @SuppressWarnings( "deprecation" )
    public boolean isConnectionAvailable(){
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        return ( cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting() );
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
        MobileAds.setAppMuted(val);
    }

    public String gdpr_personalized_ads() {
        if(!activity.getResources().getBoolean(R.bool.enable_gdpr)){
            return "0";
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.activity);
        return sharedPreferences.getString("IABTCF_VendorConsents", "0");
    }
}