package com.moho.wood;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.game.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

public class UtilsManager extends UtilsAdmob {
    private static final String TAG = "UtilsManager";
    LinearLayout adBanner;
    private CountDownTimer splashTimer = null;
    private ReviewManager reviewManager;
    private ReviewInfo reviewInfo;

    public UtilsManager(MainActivity activity) {
        setContext(activity);
        this.activity = activity;
        this.reviewManager = ReviewManagerFactory.create(activity);
    }

    public String action(String query){
        String[] action = query.split("\\|");
        String result = "ok";
        switch (action[0]){
            case "show_splash":
                splash(true);
                break;
            case "hide_splash":
                splash(false);
                break;
            case "show_privacy":
                try {
                    Intent myIntent = new Intent(activity, PrivacyActivity.class);
                    activity.startActivity(myIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error showing privacy: " + e.getMessage());
                }
                break;
            case "go_back":
                go_back();
                break;
            case "show_toast":
                if (action.length > 1) {
                    showToast(action[1], activity);
                }
                break;
            case "show_banner":
                break;
            case "exit_game":
                exit_game();
                break;
            case "show_more":
                more_games();
                break;
            case "show_review":
                Review();
                break;
            case "show_rate":
                rate();
                break;
            case "show_share":
                share();
                break;
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    public static Spanned extractHtml(String html){
        if(html == null){
            return new SpannableString("");
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    public void showToast(String toast, Context context) {
        if (context == null || toast == null) {
            return;
        }
        try {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void share(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    activity.getResources().getString(R.string.app_name)+"\n" +
                            R.string.share_description + "\n"+
                            "https://play.google.com/store/apps/details?id=" + activity.getApplication().getPackageName()
            );
            activity.startActivity(Intent.createChooser(shareIntent,"Share..."));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing: " + e.getMessage());
        }
    }

    private void rate(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            Uri uri = Uri.parse("market://details?id=" + activity.getApplication().getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            }
            activity.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getApplication().getPackageName())));
            } catch (Exception ex) {
                Log.e(TAG, "Error opening Play Store: " + ex.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rating: " + e.getMessage());
        }
    }

    private void more_games(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getApplication().getPackageName())));
        }
        catch (Exception e){
            Log.d(TAG, "More Games Exception: " + e.getMessage());
        }
    }

    private void Review() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (reviewManager == null) {
            Log.e(TAG, "ReviewManager is null");
            return;
        }

        reviewManager.requestReviewFlow().addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
            @Override
            public void onComplete(@NonNull Task<ReviewInfo> task) {
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                if (task.isSuccessful()) {
                    reviewInfo = task.getResult();
                    if (reviewInfo != null) {
                        reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (activity != null && !activity.isFinishing()) {
                                    Toast.makeText(activity, "Review Completed, Thank You!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (activity != null && !activity.isFinishing()) {
                                    Toast.makeText(activity, "Rating Failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(activity, "In-App Request Failed", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (activity != null && !activity.isFinishing()) {
                    Toast.makeText(activity, "In-App Request Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void exit_game(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    Log.d(TAG, "Confirmation Exit the game");
                    activity.onBackPressed();
                }
            }
        });
    }

    public void splash(Boolean visible){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        LinearLayout main = activity.findViewById(R.id.main);
        if (main == null) {
            Log.e(TAG, "Main layout is null, cannot show/hide splash");
            return;
        }

        if(splashTimer != null){
            splashTimer.cancel();
            splashTimer = null;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                try {
                    if(visible) {
                        main.setVisibility(View.GONE);

                        long delay = activity.getResources().getInteger(R.integer.splash_delay);
                        splashTimer = new CountDownTimer(delay, 1000) {
                            public void onTick(long millisUntilFinished) { }

                            public void onFinish() {
                                if (activity != null && !activity.isFinishing() && !activity.isDestroyed() && main != null) {
                                    main.setVisibility(View.VISIBLE);
                                }
                            }
                        }.start();
                    }
                    else{
                        main.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in splash: " + e.getMessage());
                }
            }
        });
    }

    public void go_back(){
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    Log.d(TAG, "Go to the main menu");
                    activity.onBackPressed();
                }
            }
        });
    }
}
