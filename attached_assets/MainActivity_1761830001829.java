package com.moho.wood;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.game.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements UtilsAwv.Listener {
    private static boolean isStarted = false;
    private WebServer androidWebServer;
    public UtilsAwv mwebView;
    public UtilsManager manager;
    public RelativeLayout relativeLayout;
    public Button btnNoInternetConnection;
    public Gdpr gdpr;

    // Backend integration managers
    private DeviceRegistrationManager deviceRegistrationManager;
    private NotificationManager notificationManager;

    private static final String BASE_URL = "https://your-replit-app.replit.app";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init_screen();

        gdpr = new Gdpr();
        gdpr.make(this);

        LinearLayout main = findViewById(R.id.main);
        main.setVisibility(View.INVISIBLE);

        if (!isStarted && startAndroidWebServer()) {
            isStarted = true;
        }

        mwebView = (UtilsAwv) findViewById(R.id.myWebView);
        mwebView.setListener(this, this);
        mwebView.setMixedContentAllowed(false);
        manager = new UtilsManager(this);
        manager.init();
        mwebView.setManager(manager);

        relativeLayout = findViewById(R.id.relativeLayout);
        btnNoInternetConnection = findViewById(R.id.btnNoConnection);

        btnNoInternetConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkConnection(null);
            }
        });
        checkConnection(savedInstanceState);

        manager.splash(true);

        // Initialize backend integration
        initializeBackendIntegration();
    }

    /**
     * Initialize backend integration for device registration and notifications
     */
    private void initializeBackendIntegration() {
        // Initialize managers
        deviceRegistrationManager = new DeviceRegistrationManager(this, BASE_URL);
        notificationManager = new NotificationManager(this, BASE_URL);

        // Get FCM token and register device
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get FCM token
                        String token = task.getResult();
                        Log.d("MainActivity", "FCM Token: " + token);

                        // Get app version
                        String appVersion = getAppVersion();

                        // Register device with backend
                        deviceRegistrationManager.registerDevice(token, appVersion,
                                new DeviceRegistrationManager.RegistrationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d("MainActivity", "Device registered successfully");

                                        // Fetch pending notifications after successful registration
                                        fetchPendingNotifications();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e("MainActivity", "Device registration failed: " + error);
                                    }
                                });
                    }
                });
    }

    /**
     * Fetch pending notifications from backend
     */
    private void fetchPendingNotifications() {
        notificationManager.fetchPendingNotifications(
                new NotificationManager.NotificationCallback() {
                    @Override
                    public void onSuccess(java.util.List<NotificationManager.PushNotification> notifications) {
                        Log.d("MainActivity", "Fetched " + notifications.size() + " notifications");

                        // Process notifications that should be shown on app launch
                        for (NotificationManager.PushNotification notification : notifications) {
                            if (notification.showOnAppLaunch) {
                                // Display the notification
                                displayNotification(notification);
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("MainActivity", "Failed to fetch notifications: " + error);
                    }
                });
    }

    /**
     * Display a notification to the user
     */
    private void displayNotification(NotificationManager.PushNotification notification) {
        // Track that notification was displayed
        deviceRegistrationManager.trackNotificationEvent(
                notification.notificationId,
                "displayed",
                null
        );

        // Show dialog with notification content
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                androidx.appcompat.app.AlertDialog.Builder builder =
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);

                builder.setTitle(notification.title);
                builder.setMessage(notification.message);
                builder.setCancelable(notification.cancelable);

                // Add action button if specified
                if (notification.actionButtonText != null && !notification.actionButtonText.isEmpty()) {
                    builder.setPositiveButton(notification.actionButtonText,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Track click
                                    deviceRegistrationManager.trackNotificationEvent(
                                            notification.notificationId,
                                            "clicked",
                                            null
                                    );

                                    // Handle action
                                    handleNotificationAction(notification);
                                }
                            });
                }

                // Add dismiss button
                builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Track dismissal
                        deviceRegistrationManager.trackNotificationEvent(
                                notification.notificationId,
                                "dismissed",
                                null
                        );
                        dialog.dismiss();
                    }
                });

                builder.show();
            }
        });
    }

    /**
     * Handle notification action based on type
     */
    private void handleNotificationAction(NotificationManager.PushNotification notification) {
        if (notification.actionType == null) return;

        switch (notification.actionType) {
            case "url":
                // Open URL in browser
                if (notification.actionValue != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse(notification.actionValue));
                    startActivity(browserIntent);
                }
                break;

            case "webview":
                // Load URL in webview
                if (notification.actionValue != null && mwebView != null) {
                    mwebView.loadUrl(notification.actionValue);
                }
                break;

            case "rate":
                // Open Play Store for rating
                manager.action("show_rate");
                break;

            case "share":
                // Share app
                manager.action("show_share");
                break;
        }
    }

    /**
     * Get app version
     */
    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    @SuppressWarnings( "deprecation" )
    private void init_screen(){
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState ) {
        super.onSaveInstanceState(outState);
        mwebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mwebView.restoreState(savedInstanceState);
    }

    public void checkConnection(Bundle savedInstanceState){
        boolean needConnection = getResources().getBoolean(R.bool.need_connection);
        boolean isConnected;
        String url = "http://localhost:8490/index.html";

        if (needConnection) {
            isConnected = isConnectionAvailable();
        }
        else{
            isConnected = true;
        }

        if (isConnected){
            if (savedInstanceState == null) {
                mwebView.loadUrl(url);
            }
            mwebView.setVisibility(View.VISIBLE);
            relativeLayout.setVisibility(View.GONE);
        }
        else{
            mwebView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings( "deprecation" )
    public boolean isConnectionAvailable(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return ( cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting() );
    }

    private boolean startAndroidWebServer() {
        if (!isStarted) {
            try {
                int port = 8490;
                AssetManager am = getAssets();
                String localPath = "game";
                AndroidFile f = new AndroidFile(localPath);
                f.setAssetManager( am );
                Log.d("Gradle start 8490", f.getPath());
                androidWebServer = new WebServer(port, f);
                return true;
            }
            catch (Exception e) {
                Log.w("Gradle not 8490", "The server could not start."+e);
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean stopAndroidWebServer() {
        if (isStarted && androidWebServer != null) {
            androidWebServer.stop();
            return true;
        }
        return false;
    }

    public class WebServer extends NanoHTTPD
    {
        public WebServer(int port, AndroidFile wwwroot ) throws IOException {
            super(port, wwwroot);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mwebView.onResume();
        manager.on_resume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        mwebView.onPause();
        manager.on_pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mwebView.onDestroy();
        manager.on_destroy();

        stopAndroidWebServer();
        isStarted = false;

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mwebView.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onBackPressed() {
        openQuitDialog();
    }

    public void reward(String state){
        mwebView.loadUrl("javascript:gradle.reward('"+state+"')");
    }

    public void openQuitDialog() {
        androidx.appcompat.app.AlertDialog.Builder alert;
        alert = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
        alert.setTitle(getString(R.string.app_name));
        alert.setIcon(R.drawable.about_icon);
        alert.setMessage(getString(R.string.sure_quit));

        alert.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    finishAndRemoveTask();
                }
                else {
                    finish();
                }
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.show();
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(String url) {
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType,
                                    long contentLength, String contentDisposition, String userAgent) { }

    @Override
    public void onExternalPageRequest(String url) { }

    @Override
    public void onLowMemory() {
        Log.d("TAG_MEMORY", "Memory is Low");
        super.onLowMemory();
    }
}