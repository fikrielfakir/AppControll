# Android Integration Guide

This document explains how to integrate your Android applications with the Laravel backend dashboard.

## Overview

The Android Management Platform provides centralized control over:
- **AdMob Configuration**: Remote configuration of ad unit IDs
- **Device Registration**: Track and manage installed devices
- **Push Notifications**: Send targeted notifications to users
- **Analytics Tracking**: Monitor AdMob performance and user behavior

## Base URL Configuration

**Your Replit Base URL:**
```
https://7ef8caf9-d51c-4eeb-8c2a-b180a4f83fc4-00-1vzbsfju3ze94.janeway.replit.dev
```

Update this URL in all your Android app files that connect to the backend.

---

## API Endpoints

### 1. Get AdMob Configuration

**Endpoint**: `GET /api/v1/config/{package_name}`

**Description**: Fetches AdMob account configuration for the specified app.

**Response Example**:
```json
{
  "admob_accounts": [
    {
      "account_id": "acc_123456",
      "status": "active",
      "banner_id": "ca-app-pub-XXXXXXXX/XXXXXXXXXX",
      "interstitial_id": "ca-app-pub-XXXXXXXX/XXXXXXXXXX",
      "rewarded_id": "ca-app-pub-XXXXXXXX/XXXXXXXXXX",
      "app_open_id": "ca-app-pub-XXXXXXXX/XXXXXXXXXX",
      "native_id": "ca-app-pub-XXXXXXXX/XXXXXXXXXX"
    }
  ],
  "app_config": {}
}
```

---

### 2. Register Device

**Endpoint**: `POST /api/v1/device/register`

**Description**: Registers a new device or updates an existing one.

**Request Body**:
```json
{
  "package_name": "com.example.app",
  "fcm_token": "firebase_token_here",
  "device_info": {
    "country": "US",
    "app_version": "1.0.0",
    "android_version": "33",
    "manufacturer": "Samsung",
    "model": "SM-G998B"
  }
}
```

**Response**:
```json
{
  "success": true,
  "message": "Device registered successfully",
  "device_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 3. Get Pending Notifications

**Endpoint**: `GET /api/v1/notifications/pending?package_name={package_name}`

**Description**: Fetches pending notifications for the app.

**Response Example**:
```json
{
  "notifications": [
    {
      "notification_id": "notif_123",
      "title": "New Update Available",
      "message": "Check out our latest features!",
      "type": "popup",
      "priority": "high",
      "content": {
        "image_url": "https://example.com/image.jpg",
        "action_button_text": "Update Now",
        "action_type": "url",
        "action_value": "https://play.google.com/store/apps/details?id=com.example.app",
        "cancelable": true
      },
      "display_rules": {
        "max_displays": 3,
        "display_interval_hours": 24,
        "show_on_app_launch": true
      }
    }
  ]
}
```

---

### 4. Track Notification Event

**Endpoint**: `POST /api/v1/notifications/track`

**Description**: Tracks notification interactions.

**Request Body**:
```json
{
  "notification_id": "notif_123",
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "event": "displayed",
  "timestamp": 1698765432000
}
```

**Events**: `displayed`, `clicked`, `dismissed`

**Response**:
```json
{
  "success": true
}
```

---

### 5. Track AdMob Analytics

**Endpoint**: `POST /api/v1/analytics/admob`

**Description**: Tracks AdMob ad events.

**Request Body**:
```json
{
  "package_name": "com.example.app",
  "account_id": "acc_123456",
  "event": "impression",
  "ad_type": "banner",
  "value": 0
}
```

**Events**: `impression`, `click`, `load`, `load_failed`

**Ad Types**: `banner`, `interstitial`, `rewarded`, `native`, `app_open`

---

## Android Integration

### Step 1: Add Java Classes

Copy these Java files to your Android project (package: `com.moho.wood` or update package name):

1. **AdMobConfigManager.java** - Manages AdMob configuration
2. **DeviceRegistrationManager.java** - Handles device registration
3. **PushNotificationManager.java** - Fetches and manages notifications
4. **MyFirebaseMessagingService.java** - Handles FCM push notifications

### Step 2: Update AndroidManifest.xml

Add Firebase Messaging Service:

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### Step 3: Add Dependencies

Add to your `build.gradle`:

```gradle
dependencies {
    // Firebase
    implementation 'com.google.firebase:firebase-messaging:23.0.0'
    implementation 'com.google.firebase:firebase-analytics:21.0.0'
    
    // AdMob
    implementation 'com.google.android.gms:play-services-ads:22.0.0'
}
```

### Step 4: Initialize in MainActivity

```java
public class MainActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://7ef8caf9-d51c-4eeb-8c2a-b180a4f83fc4-00-1vzbsfju3ze94.janeway.replit.dev";
    private AdMobConfigManager adMobConfigManager;
    private DeviceRegistrationManager deviceRegistrationManager;
    private PushNotificationManager pushNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize managers
        adMobConfigManager = new AdMobConfigManager(this, BASE_URL);
        deviceRegistrationManager = new DeviceRegistrationManager(this, BASE_URL);
        pushNotificationManager = new PushNotificationManager(this, BASE_URL);
        
        // Set default AdMob IDs as fallback
        adMobConfigManager.setDefaultIds(
            getString(R.string.id_banner),
            getString(R.string.id_inter),
            getString(R.string.id_reward)
        );
        
        // Fetch AdMob config
        adMobConfigManager.fetchConfig(new AdMobConfigManager.ConfigCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "AdMob config loaded");
                initializeAds();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "AdMob config error: " + error);
                initializeAds(); // Use defaults
            }
        });
        
        // Register device with FCM
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    String appVersion = getAppVersion();
                    
                    deviceRegistrationManager.registerDevice(
                        token, 
                        appVersion,
                        new DeviceRegistrationManager.RegistrationCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Device registered");
                                fetchPendingNotifications();
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Registration error: " + error);
                            }
                        }
                    );
                }
            });
    }
    
    private void fetchPendingNotifications() {
        pushNotificationManager.fetchPendingNotifications(
            new PushNotificationManager.NotificationCallback() {
                @Override
                public void onSuccess(List<PushNotificationManager.PushNotification> notifications) {
                    for (PushNotificationManager.PushNotification notif : notifications) {
                        if (notif.showOnAppLaunch) {
                            displayNotification(notif);
                        }
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Notification fetch error: " + error);
                }
            }
        );
    }
    
    private void initializeAds() {
        // Use configured ad unit IDs
        String bannerId = adMobConfigManager.getBannerId();
        String interstitialId = adMobConfigManager.getInterstitialId();
        String rewardedId = adMobConfigManager.getRewardedId();
        
        // Initialize your ads with these IDs
    }
    
    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }
}
```

### Step 5: Track AdMob Events

```java
// Track banner impression
adMobConfigManager.trackAdEvent("impression", "banner", 0);

// Track interstitial click
adMobConfigManager.trackAdEvent("click", "interstitial", 0);

// Track rewarded video completion
adMobConfigManager.trackAdEvent("completed", "rewarded", 10);
```

### Step 6: Track Notification Events

```java
// Track when notification is displayed
deviceRegistrationManager.trackNotificationEvent(
    notificationId, 
    "displayed", 
    null
);

// Track when notification is clicked
deviceRegistrationManager.trackNotificationEvent(
    notificationId, 
    "clicked", 
    null
);
```

---

## Dashboard Setup

### 1. Create an App

1. Login to admin dashboard at `/admin/login`
2. Go to **Apps** → **Add New App**
3. Enter:
   - **Package Name**: Your Android app package name (e.g., `com.example.app`)
   - **App Name**: Display name
   - **Platform**: Android
   - **Status**: Active

### 2. Configure AdMob Account

1. Go to **AdMob Accounts** → **Add New**
2. Enter:
   - **Account Name**: Descriptive name
   - **Publisher ID**: Your AdMob publisher ID
   - **Banner ID**: Ad unit ID for banner ads
   - **Interstitial ID**: Ad unit ID for interstitial ads
   - **Rewarded ID**: Ad unit ID for rewarded ads
   - **App Open ID**: (Optional) Ad unit ID for app open ads
   - **Native ID**: (Optional) Ad unit ID for native ads
   - **Status**: Active

3. Assign the AdMob account to your app in the app settings

### 3. Create Push Notifications

1. Go to **Notifications** → **Create New**
2. Configure:
   - **Title**: Notification title
   - **Message**: Notification message
   - **Type**: `popup` or `system`
   - **Priority**: `high`, `normal`, or `low`
   - **Target**: Specific app or all apps
   - **Display Rules**:
     - Max displays per device
     - Display interval (hours)
     - Show on app launch
   - **Schedule**: Send immediately or schedule for later

---

## Testing

### Test Device Registration

```bash
curl -X POST https://7ef8caf9-d51c-4eeb-8c2a-b180a4f83fc4-00-1vzbsfju3ze94.janeway.replit.dev/api/v1/device/register \
  -H "Content-Type: application/json" \
  -d '{
    "package_name": "com.example.app",
    "fcm_token": "test_token",
    "device_info": {
      "country": "US",
      "app_version": "1.0.0",
      "android_version": "33",
      "manufacturer": "Google",
      "model": "Pixel 7"
    }
  }'
```

### Test AdMob Config

```bash
curl https://7ef8caf9-d51c-4eeb-8c2a-b180a4f83fc4-00-1vzbsfju3ze94.janeway.replit.dev/api/v1/config/com.example.app
```

### Test Notifications

```bash
curl "https://7ef8caf9-d51c-4eeb-8c2a-b180a4f83fc4-00-1vzbsfju3ze94.janeway.replit.dev/api/v1/notifications/pending?package_name=com.example.app"
```

---

## Security Notes

1. **HTTPS Required**: All API calls must use HTTPS in production
2. **Package Name Validation**: Only registered apps can access configurations
3. **Rate Limiting**: API endpoints are rate-limited to prevent abuse
4. **FCM Token Security**: Tokens are encrypted in the database
5. **No Authentication**: Public API endpoints for Android apps (app authentication via package name)

---

## Troubleshooting

### AdMob Config Not Loading

- Verify the app is registered in the dashboard with correct package name
- Check that an AdMob account is assigned to the app
- Ensure the AdMob account status is "active"
- Verify network connectivity and BASE_URL

### Device Registration Fails

- Check that the package name matches exactly
- Verify FCM token is valid
- Check server logs for detailed error messages
- Ensure the app exists in the dashboard

### Notifications Not Showing

- Verify notifications are created and status is "pending"
- Check scheduling and expiration dates
- Ensure targeting matches your app
- Verify display rules allow showing the notification
- Check FCM token is valid and device is registered

### Analytics Not Recording

- Verify package name is correct
- Check that app exists in dashboard
- Ensure AdMob account is properly configured
- Check server logs for errors

---

## Support

For issues or questions:
1. Check server logs at `storage/logs/laravel.log`
2. Review API response error messages
3. Verify all configuration steps are completed
4. Test with curl commands to isolate Android vs server issues
