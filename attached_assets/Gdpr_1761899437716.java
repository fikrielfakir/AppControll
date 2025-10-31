package com.moho.wood;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.ump.ConsentForm;
import com.game.R;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

public class Gdpr {
    private String TAG = "Gdpr";
    private Boolean under_age = false;
    private ConsentInformation consentInformation;
    private ConsentForm consentForm;
    private Activity activity;


    public void make(Activity activity){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "Activity is null or finishing, cannot show GDPR");
            return;
        }

        if(!activity.getResources().getBoolean(R.bool.enable_gdpr)){
            return;
        }

        this.activity = activity;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.activity);

        if (sharedPreferences.getBoolean("already_viewed_gdpr", false)){
            return;
        }

        under_age = activity.getResources().getBoolean(R.bool.under_age);

        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(under_age)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }

                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    }
                },
                new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(FormError formError) {
                        Log.e(TAG, "GDPR consent update failed: " + (formError != null ? formError.getMessage() : "Unknown error"));
                    }
                });

    }

    public void loadForm() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "Activity is null or finishing, cannot load form");
            return;
        }

        UserMessagingPlatform.loadConsentForm(
                activity,
                new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                    @Override
                    public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }

                        Gdpr.this.consentForm = consentForm;
                        if(consentInformation != null &&
                                consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {

                            consentForm.show(
                                    activity,
                                    new ConsentForm.OnConsentFormDismissedListener() {
                                        @Override
                                        public void onConsentFormDismissed(@Nullable FormError formError) {
                                            if (formError != null) {
                                                Log.e(TAG, "Consent form dismissed with error: " + formError.getMessage());
                                            }

                                            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                                loadForm();
                                            }
                                        }
                                    });

                            try {
                                SharedPreferences.Editor sharedPreferencesEditor =
                                        PreferenceManager.getDefaultSharedPreferences(activity).edit();
                                sharedPreferencesEditor.putBoolean("already_viewed_gdpr", true);
                                sharedPreferencesEditor.apply();
                            } catch (Exception e) {
                                Log.e(TAG, "Error saving GDPR preference: " + e.getMessage());
                            }
                        }
                    }
                },
                new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                    @Override
                    public void onConsentFormLoadFailure(FormError formError) {
                        Log.e(TAG, "Consent form load failed: " + (formError != null ? formError.getMessage() : "Unknown error"));
                    }
                }
        );
    }

}
