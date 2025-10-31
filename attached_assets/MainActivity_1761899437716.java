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
import android.net.NetworkInfo;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements UtilsAwv.Listener {
    private static final String TAG = "MainActivity";
    private static boolean isStarted = false;
    private WebServer androidWebServer;
    public UtilsAwv mwebView;
    public UtilsManager manager;
    public RelativeLayout relativeLayout;
    public Button btnNoInternetConnection;
    public Gdpr gdpr;

    private DeviceRegistrationManager deviceRegistrationManager;
    private PushNotificationManager pushNotificationManager;

    private static final String BASE_URL = "https://android-dashboard.magneseo.com";

    // Add flag to prevent multiple initializations
    private boolean isInitializing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "=== onCreate START ===");

        // Set uncaught exception handler for debugging
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "UNCAUGHT EXCEPTION: " + throwable.getMessage());
                Log.e(TAG, "Stack trace: " + getStackTraceString(new Exception(throwable)));
            }
        });

        try {
            Log.d(TAG, "Step 1: setContentView");
            setContentView(R.layout.activity_main);

            Log.d(TAG, "Step 2: init_screen");
            init_screen();

            Log.d(TAG, "Step 3: GDPR");
            try {
                gdpr = new Gdpr();
                gdpr.make(this);
            } catch (Exception e) {
                Log.e(TAG, "GDPR error (non-fatal): " + e.getMessage(), e);
            }

            Log.d(TAG, "Step 4: Get views");
            LinearLayout main = findViewById(R.id.main);
            if (main != null) {
                main.setVisibility(View.INVISIBLE);
                Log.d(TAG, "Main layout found and hidden");
            } else {
                Log.e(TAG, "Main layout is NULL!");
            }

            Log.d(TAG, "Step 5: Start web server");
            if (!isStarted && startAndroidWebServer()) {
                isStarted = true;
                Log.d(TAG, "Web server started successfully");
            }

            Log.d(TAG, "Step 6: Initialize WebView");
            mwebView = (UtilsAwv) findViewById(R.id.myWebView);
            if (mwebView != null) {
                mwebView.setListener(this, this);
                mwebView.setMixedContentAllowed(false);
                Log.d(TAG, "WebView initialized");
            } else {
                Log.e(TAG, "WebView is NULL!");
            }

            Log.d(TAG, "Step 7: Initialize UtilsManager");
            manager = new UtilsManager(this);
            manager.init();
            Log.d(TAG, "UtilsManager initialized");

            if (mwebView != null) {
                mwebView.setManager(manager);
            }

            Log.d(TAG, "Step 8: Get other views");
            relativeLayout = findViewById(R.id.relativeLayout);
            btnNoInternetConnection = findViewById(R.id.btnNoConnection);

            if (btnNoInternetConnection != null) {
                btnNoInternetConnection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        checkConnection(null);
                    }
                });
                Log.d(TAG, "No connection button configured");
            }

            Log.d(TAG, "Step 9: Check connection");
            checkConnection(savedInstanceState);

            Log.d(TAG, "Step 10: Show splash");
            if (manager != null) {
                manager.splash(true);
            }

            Log.d(TAG, "Step 11: Initialize backend (Firebase + Notifications)");
            initializeBackendIntegration();

            Log.d(TAG, "=== onCreate COMPLETE ===");
        } catch (Exception e) {
            Log.e(TAG, "CRASH in onCreate at: " + getStackTraceString(e));
            e.printStackTrace();
        }
    }

    private void initializeBackendIntegration() {
        if (isInitializing) {
            Log.w(TAG, "Backend already initializing");
            return;
        }

        isInitializing = true;
        Log.d(TAG, "=== initializeBackendIntegration START ===");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isFinishing() || isDestroyed()) {
                        Log.d(TAG, "Activity finishing, skipping backend init");
                        isInitializing = false;
                        return;
                    }

                    Log.d(TAG, "Backend Step 1: Initialize Firebase");
                    try {
                        FirebaseApp.initializeApp(MainActivity.this);
                        Log.d(TAG, "Firebase initialized successfully");
                    } catch (IllegalStateException e) {
                        Log.d(TAG, "Firebase already initialized");
                    } catch (Exception e) {
                        Log.e(TAG, "Firebase init error: " + e.getMessage(), e);
                    }

                    Log.d(TAG, "Backend Step 2: Create managers");
                    try {
                        deviceRegistrationManager = new DeviceRegistrationManager(MainActivity.this, BASE_URL);
                        pushNotificationManager = new PushNotificationManager(MainActivity.this, BASE_URL);
                        Log.d(TAG, "Managers created");
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating managers: " + e.getMessage(), e);
                        isInitializing = false;
                        return;
                    }

                    Log.d(TAG, "Backend Step 3: Get FCM token");
                    try {
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(new OnCompleteListener<String>() {
                                    @Override
                                    public void onComplete(Task<String> task) {
                                        if (isFinishing() || isDestroyed()) {
                                            isInitializing = false;
                                            return;
                                        }

                                        if (!task.isSuccessful()) {
                                            Log.w(TAG, "Fetching FCM token failed", task.getException());
                                            isInitializing = false;
                                            return;
                                        }

                                        String token = task.getResult();
                                        if (token != null && !token.isEmpty()) {
                                            Log.d(TAG, "FCM Token received");

                                            String appVersion = getAppVersion();
                                            Log.d(TAG, "App version: " + appVersion);

                                            Log.d(TAG, "Backend Step 4: Register device");
                                            if (deviceRegistrationManager != null) {
                                                deviceRegistrationManager.registerDevice(token, appVersion,
                                                        new DeviceRegistrationManager.RegistrationCallback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                Log.d(TAG, "Device registered successfully");
                                                                Log.d(TAG, "Backend Step 5: Fetch notifications");
                                                                fetchPendingNotifications();
                                                                isInitializing = false;
                                                            }

                                                            @Override
                                                            public void onError(String error) {
                                                                Log.e(TAG, "Device registration failed: " + error);
                                                                isInitializing = false;
                                                            }
                                                        });
                                            } else {
                                                isInitializing = false;
                                            }
                                        } else {
                                            Log.w(TAG, "FCM token is null or empty");
                                            isInitializing = false;
                                        }
                                    }
                                });
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting FCM token: " + e.getMessage(), e);
                        isInitializing = false;
                    }

                    Log.d(TAG, "=== initializeBackendIntegration COMPLETE ===");
                } catch (Exception e) {
                    Log.e(TAG, "Error in initializeBackendIntegration: " + getStackTraceString(e));
                    isInitializing = false;
                }
            }
        }).start();
    }

    private void fetchPendingNotifications() {
        Log.d(TAG, "=== fetchPendingNotifications START ===");

        if (pushNotificationManager == null) {
            Log.w(TAG, "PushNotificationManager is null");
            return;
        }

        if (isFinishing() || isDestroyed()) {
            return;
        }

        try {
            pushNotificationManager.fetchPendingNotifications(
                    new PushNotificationManager.NotificationCallback() {
                        @Override
                        public void onSuccess(java.util.List<PushNotificationManager.PushNotification> notifications) {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            if (notifications == null) {
                                Log.d(TAG, "No notifications received (null)");
                                return;
                            }

                            Log.d(TAG, "Fetched " + notifications.size() + " notifications");

                            for (int i = 0; i < notifications.size(); i++) {
                                PushNotificationManager.PushNotification notification = notifications.get(i);
                                Log.d(TAG, "Notification " + i + ": " +
                                        (notification != null ? notification.title : "null"));

                                if (notification != null && notification.showOnAppLaunch) {
                                    displayNotification(notification);
                                }
                            }

                            Log.d(TAG, "=== fetchPendingNotifications COMPLETE ===");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to fetch notifications: " + error);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching notifications: " + e.getMessage(), e);
        }
    }

    private void displayNotification(PushNotificationManager.PushNotification notification) {
        Log.d(TAG, "=== displayNotification START ===");

        if (notification == null || isFinishing() || isDestroyed()) {
            Log.w(TAG, "Cannot display notification - activity finishing or notification null");
            return;
        }

        Log.d(TAG, "Displaying notification: " + notification.title);

        // Track displayed event
        try {
            if (deviceRegistrationManager != null && notification.notificationId != null) {
                deviceRegistrationManager.trackNotificationEvent(
                        notification.notificationId,
                        "displayed",
                        null
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error tracking notification: " + e.getMessage(), e);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                try {
                    androidx.appcompat.app.AlertDialog.Builder builder =
                            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);

                    // ✅ FIX: Null checks for title and message
                    if (notification.title != null && !notification.title.isEmpty()) {
                        builder.setTitle(notification.title);
                    } else {
                        builder.setTitle("Notification");
                    }

                    if (notification.message != null && !notification.message.isEmpty()) {
                        builder.setMessage(notification.message);
                    }

                    builder.setCancelable(notification.cancelable);

                    if (notification.actionButtonText != null && !notification.actionButtonText.isEmpty()) {
                        builder.setPositiveButton(notification.actionButtonText,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.d(TAG, "Notification action clicked");
                                        try {
                                            if (deviceRegistrationManager != null && notification.notificationId != null) {
                                                deviceRegistrationManager.trackNotificationEvent(
                                                        notification.notificationId,
                                                        "clicked",
                                                        null
                                                );
                                            }
                                            handleNotificationAction(notification);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error handling action: " + e.getMessage(), e);
                                        }
                                    }
                                });
                    }

                    builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Notification dismissed");
                            try {
                                if (deviceRegistrationManager != null && notification.notificationId != null) {
                                    deviceRegistrationManager.trackNotificationEvent(
                                            notification.notificationId,
                                            "dismissed",
                                            null
                                    );
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error tracking dismiss: " + e.getMessage(), e);
                            }
                            dialog.dismiss();
                        }
                    });

                    if (!isFinishing() && !isDestroyed()) {
                        builder.show();
                        Log.d(TAG, "=== displayNotification COMPLETE ===");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying notification: " + getStackTraceString(e));
                }
            }
        });
    }

// CONTINUED FROM PART 1...

    private void handleNotificationAction(PushNotificationManager.PushNotification notification) {
        Log.d(TAG, "=== handleNotificationAction: " +
                (notification != null ? notification.actionType : "null") + " ===");

        if (notification == null || notification.actionType == null) {
            return;
        }

        try {
            switch (notification.actionType) {
                case "url":
                    if (notification.actionValue != null && !notification.actionValue.isEmpty()) {
                        Log.d(TAG, "Opening URL: " + notification.actionValue);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                android.net.Uri.parse(notification.actionValue));
                        startActivity(browserIntent);
                    }
                    break;

                case "webview":
                    if (notification.actionValue != null && !notification.actionValue.isEmpty() && mwebView != null) {
                        Log.d(TAG, "Loading in webview: " + notification.actionValue);
                        mwebView.loadUrl(notification.actionValue);
                    }
                    break;

                case "rate":
                    Log.d(TAG, "Opening rate dialog");
                    if (manager != null) {
                        manager.action("show_rate");
                    }
                    break;

                case "share":
                    Log.d(TAG, "Opening share dialog");
                    if (manager != null) {
                        manager.action("show_share");
                    }
                    break;

                default:
                    Log.w(TAG, "Unknown action type: " + notification.actionType);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling action: " + getStackTraceString(e));
        }
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0.0";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
            return "1.0.0";
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting app version: " + e.getMessage());
            return "1.0.0";
        }
    }

    @SuppressWarnings("deprecation")
    private void init_screen(){
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error in init_screen: " + getStackTraceString(e));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            if (mwebView != null) {
                mwebView.saveState(outState);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving instance state: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        try {
            if (mwebView != null) {
                mwebView.restoreState(savedInstanceState);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring instance state: " + e.getMessage(), e);
        }
    }

    public void checkConnection(Bundle savedInstanceState){
        Log.d(TAG, "=== checkConnection START ===");
        try {
            boolean needConnection = getResources().getBoolean(R.bool.need_connection);
            boolean isConnected;
            String url = "http://localhost:8490/index.html";

            if (needConnection) {
                isConnected = isConnectionAvailable();
                Log.d(TAG, "Connection required: " + isConnected);
            } else {
                isConnected = true;
                Log.d(TAG, "Connection not required");
            }

            if (isConnected) {
                if (savedInstanceState == null && mwebView != null) {
                    Log.d(TAG, "Loading URL: " + url);
                    mwebView.loadUrl(url);
                }
                if (mwebView != null) {
                    mwebView.setVisibility(View.VISIBLE);
                }
                if (relativeLayout != null) {
                    relativeLayout.setVisibility(View.GONE);
                }
            } else {
                Log.d(TAG, "No connection - showing error screen");
                if (mwebView != null) {
                    mwebView.setVisibility(View.GONE);
                }
                if (relativeLayout != null) {
                    relativeLayout.setVisibility(View.VISIBLE);
                }
            }
            Log.d(TAG, "=== checkConnection COMPLETE ===");
        } catch (Exception e) {
            Log.e(TAG, "Error in checkConnection: " + getStackTraceString(e));
        }
    }

    @SuppressWarnings("deprecation")
    public boolean isConnectionAvailable(){
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        } catch (Exception e) {
            Log.e(TAG, "Error checking connection: " + e.getMessage());
            return false;
        }
    }

    private boolean startAndroidWebServer() {
        Log.d(TAG, "=== startAndroidWebServer START ===");
        if (!isStarted) {
            try {
                int port = 8490;
                AssetManager am = getAssets();
                String localPath = "game";
                AndroidFile f = new AndroidFile(localPath);
                f.setAssetManager(am);
                Log.d(TAG, "Starting server on port " + port + " at: " + f.getPath());
                androidWebServer = new WebServer(port, f);
                Log.d(TAG, "=== startAndroidWebServer COMPLETE ===");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Server start failed: " + getStackTraceString(e));
            }
        }
        return false;
    }

    private boolean stopAndroidWebServer() {
        try {
            if (isStarted && androidWebServer != null) {
                androidWebServer.stop();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping web server: " + e.getMessage(), e);
        }
        return false;
    }

    public class WebServer extends NanoHTTPD {
        public WebServer(int port, AndroidFile wwwroot) throws IOException {
            super(port, wwwroot);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== onResume ===");
        try {
            if (mwebView != null) {
                mwebView.onResume();
            }
            if (manager != null) {
                manager.on_resume();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        Log.d(TAG, "=== onPause ===");
        try {
            if (mwebView != null) {
                mwebView.onPause();
            }
            if (manager != null) {
                manager.on_pause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause: " + e.getMessage(), e);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "=== onDestroy ===");
        try {
            if (mwebView != null) {
                mwebView.onDestroy();
            }
            if (manager != null) {
                manager.on_destroy();
            }

            stopAndroidWebServer();
            isStarted = false;

            // Clean up references
            deviceRegistrationManager = null;
            pushNotificationManager = null;

        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            if (mwebView != null) {
                mwebView.onActivityResult(requestCode, resultCode, intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onActivityResult: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            openQuitDialog();
        } catch (Exception e) {
            Log.e(TAG, "Error in onBackPressed: " + e.getMessage(), e);
            super.onBackPressed();
        }
    }

    public void reward(String state){
        try {
            if (mwebView != null && state != null) {
                mwebView.loadUrl("javascript:gradle.reward('" + state + "')");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in reward: " + e.getMessage(), e);
        }
    }

    public void openQuitDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        try {
            androidx.appcompat.app.AlertDialog.Builder alert;
            alert = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
            alert.setTitle(getString(R.string.app_name));
            alert.setIcon(R.drawable.about_icon);
            alert.setMessage(getString(R.string.sure_quit));

            alert.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            finishAndRemoveTask();
                        } else {
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error finishing activity: " + e.getMessage(), e);
                        finish();
                    }
                }
            });

            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            if (!isFinishing() && !isDestroyed()) {
                alert.show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing quit dialog: " + getStackTraceString(e));
        }
    }

    private String getStackTraceString(Exception e) {
        if (e == null) {
            return "null exception";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");

        try {
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append("  at ").append(element.toString()).append("\n");
            }
        } catch (Exception ex) {
            sb.append("Error getting stack trace");
        }

        return sb.toString();
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        Log.d(TAG, "Page started: " + url);
    }

    @Override
    public void onPageFinished(String url) {
        Log.d(TAG, "Page finished: " + url);
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        Log.e(TAG, "Page error: " + errorCode + " - " + description + " - " + failingUrl);
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType,
                                    long contentLength, String contentDisposition, String userAgent) {
    }

    @Override
    public void onExternalPageRequest(String url) {
        Log.d(TAG, "External page request: " + url);
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG, "Memory is Low");
        super.onLowMemory();
    }
}