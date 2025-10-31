# Android App Integration Guide

## Overview
Your Laravel backend is now fully integrated with the Android app. The platform provides remote configuration for AdMob ads, push notifications, device management, and analytics tracking.

## Base URL
The Android app uses: `https://android-dashboard.magneseo.com`

Make sure to update this URL in the Java files if you're using a different domain.

## API Endpoints

### 1. **Get AdMob Configuration**
```
GET /api/v1/config/{package_name}
```

**Response:**
```json
{
  "admob_accounts": [
    {
      "account_id": "pub-xxxxxxxxxxxxx",
      "status": "active",
      "banner_id": "ca-app-pub-xxxxxxxxxxxxx/xxxxxxxxxx",
      "interstitial_id": "ca-app-pub-xxxxxxxxxxxxx/xxxxxxxxxx",
      "rewarded_id": "ca-app-pub-xxxxxxxxxxxxx/xxxxxxxxxx",
      "app_open_id": "ca-app-pub-xxxxxxxxxxxxx/xxxxxxxxxx",
      "native_id": "ca-app-pub-xxxxxxxxxxxxx/xxxxxxxxxx"
    }
  ],
  "app_config": {}
}
```

### 2. **Register Device**
```
POST /api/v1/device/register
```

**Request Body:**
```json
{
  "package_name": "com.moho.wood",
  "fcm_token": "device_fcm_token_here",
  "device_info": {
    "country": "US",
    "app_version": "1.0.0",
    "android_version": "30",
    "manufacturer": "Samsung",
    "model": "Galaxy S21"
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Device registered successfully",
  "device_id": "uuid-here"
}
```

### 3. **Get Pending Notifications**
```
GET /api/v1/notifications/pending?package_name={package_name}
```

**Response:**
```json
{
  "notifications": [
    {
      "notification_id": "uuid",
      "title": "Welcome!",
      "message": "Thank you for using our app",
      "type": "popup",
      "priority": "normal",
      "content": {
        "image_url": "https://example.com/image.jpg",
        "action_button_text": "Open",
        "action_type": "url",
        "action_value": "https://example.com",
        "cancelable": true
      },
      "display_rules": {
        "max_displays": 1,
        "display_interval_hours": 24,
        "show_on_app_launch": true
      }
    }
  ]
}
```

### 4. **Track Notification Events**
```
POST /api/v1/notifications/track
```

**Request Body:**
```json
{
  "notification_id": "uuid",
  "device_id": "device_uuid",
  "event": "displayed",
  "timestamp": 1699999999999
}
```

Events: `displayed`, `clicked`, `dismissed`

### 5. **Track AdMob Analytics**
```
POST /api/v1/analytics/admob
```

**Request Body:**
```json
{
  "package_name": "com.moho.wood",
  "account_id": "pub-xxxxxxxxxxxxx",
  "event": "impression",
  "ad_type": "banner",
  "value": 0
}
```

## Setup Instructions

### Step 1: Register Your App
1. Log into the admin dashboard at `https://your-domain.com`
2. Navigate to **Apps** section
3. Click **Add New App**
4. Enter:
   - **App Name**: Your app name
   - **Package Name**: `com.moho.wood` (must match exactly)
   - **Is Active**: Check this box
5. Click **Save**

### Step 2: Configure AdMob Account
1. Navigate to **AdMob Accounts** section
2. Click **Add New Account**
3. Enter your AdMob details:
   - **Account ID**: Your AdMob publisher ID (e.g., `pub-xxxxxxxxxxxxx`)
   - **Banner ID**: Your AdMob banner ad unit ID
   - **Interstitial ID**: Your AdMob interstitial ad unit ID
   - **Rewarded ID**: Your AdMob rewarded ad unit ID
   - **App Open ID**: (Optional) Your app open ad unit ID
   - **Native ID**: (Optional) Your native ad unit ID
   - **Status**: Set to "Active"
4. Click **Save**

### Step 3: Link AdMob Account to App
1. Go back to your app in the **Apps** section
2. Edit the app
3. Select the AdMob account you just created as the **Default AdMob Account**
4. Click **Save**

### Step 4: Test the Integration

#### Test Device Registration
When your Android app launches, it should automatically:
1. Initialize Firebase and get an FCM token
2. Register the device with your backend
3. Fetch the AdMob configuration
4. Load ads with the remote configuration

#### Test AdMob Configuration
Your app will automatically fetch AdMob IDs from the server every 24 hours (or on first launch).

#### Create a Test Notification
1. Go to **Notifications** section
2. Click **Create Notification**
3. Enter:
   - **Title**: "Test Notification"
   - **Message**: "This is a test"
   - **Package Name**: `com.moho.wood`
   - **Show on App Launch**: Check this
4. Click **Send**
5. Restart your Android app to see the notification

## Android App Files Reference

Your app should already have these manager classes integrated:

1. **AdMobConfigManager.java** - Fetches and caches AdMob configuration
2. **DeviceRegistrationManager.java** - Registers device and tracks events
3. **PushNotificationManager.java** - Fetches and displays notifications
4. **MyFirebaseMessagingService.java** - Handles FCM messages
5. **UtilsAdmob.java** - Manages AdMob ads with remote config

## Features

### ✅ Remote AdMob Configuration
- Update ad unit IDs without releasing a new app version
- Switch between different AdMob accounts
- Enable/disable ads remotely

### ✅ Device Management
- Track all installed devices
- View device details (country, version, manufacturer, model)
- Monitor FCM token status

### ✅ Push Notifications
- Send targeted notifications to specific apps
- Schedule notifications for future delivery
- Track notification performance (displayed, clicked, dismissed)
- Set display rules (max displays, intervals, launch triggers)

### ✅ Analytics
- Track AdMob ad impressions, clicks, and revenue
- Monitor device registrations
- Analyze notification engagement

## Database Schema

The platform uses the following tables:

- **admob_apps** - Registered Android applications
- **devices** - Registered devices with FCM tokens
- **admob_accounts** - AdMob account configurations
- **admob_ad_units** - Individual ad unit details
- **notifications** - Push notification queue
- **notification_tracking** - Notification event tracking
- **admob_analytics** - AdMob analytics events
- **analytics_events** - General analytics events

## Security Notes

1. All API endpoints are **public** (no authentication required for app access)
2. Validation is performed on package names to prevent unauthorized access
3. Only active apps can fetch configurations
4. FCM tokens are stored securely and not exposed in API responses

## Troubleshooting

### App Not Receiving AdMob Config
1. Verify the package name matches exactly in both app and dashboard
2. Ensure the app is marked as "Active" in the dashboard
3. Check that an AdMob account is assigned to the app
4. Verify the AdMob account status is "Active"

### Device Not Registering
1. Check Firebase is properly initialized in the app
2. Verify FCM token is being generated
3. Check server logs for validation errors
4. Ensure the package name is correct

### Notifications Not Showing
1. Verify notification status is "pending"
2. Check scheduled_at and expires_at dates
3. Ensure package_name matches or is null (for all apps)
4. Verify show_on_app_launch is enabled for launch notifications

## Support

For issues or questions, check the Laravel logs:
```bash
tail -f storage/logs/laravel.log
```

## Next Steps

1. Register your first app in the dashboard
2. Configure your AdMob account
3. Send a test notification
4. Monitor analytics and device registrations
5. Customize notification display rules based on user behavior

Your Android Management Platform is ready to use! 🚀
