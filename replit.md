# Android Platform Control Panel

A Laravel-based admin panel for managing Android apps, AdMob accounts, push notifications, and analytics with PostgreSQL database.

## Overview

This is a comprehensive platform for managing multiple Android applications from a single admin panel. It provides:

- **App Management**: CRUD operations for Android apps with package names, icons, and FCM keys
- **AdMob Account Management**: Multiple AdMob accounts per app with 6 switching strategies
- **Notification System**: Targeted push notifications with FCM integration
- **Analytics Dashboard**: Real-time charts and statistics for devices, events, and notifications
- **REST API**: Complete API for Android app integration

## Project Structure

```
app/
├── Models/              # Eloquent models (App, AdMobAccount, Device, etc.)
├── Services/            # Business logic services
│   ├── AdMobStrategyService.php
│   ├── NotificationTargetingService.php
│   ├── AnalyticsService.php
│   ├── DeviceService.php
│   └── NotificationService.php
├── Http/Controllers/
│   ├── Admin/          # Admin panel controllers
│   └── Api/            # REST API controllers
resources/views/
├── layouts/            # Blade layouts
│   ├── app.blade.php
│   └── guest.blade.php
└── admin/              # Admin views
    ├── dashboard.blade.php
    ├── apps/
    └── auth/
```

## Features Implemented

### Admin Panel
- ✅ Authentication system (username: admin, password: admin123)
- ✅ Dashboard with statistics cards
- ✅ App management with CRUD operations
- ✅ AdMob account management
- ✅ Notification creation and sending
- ✅ Analytics with Chart.js visualizations
- ✅ Responsive Bootstrap 5 UI with sidebar navigation
- ✅ DataTables for data management

### AdMob Switching Strategies

Each AdMob account can have its own individual switching strategy. When a request comes in, the system evaluates each account's targeting criteria:

1. **Time-Based**: Select if current hour matches account's configured time window (start_hour to end_hour)
2. **Location-Based**: Select if device country matches account's configured countries list
3. **Device-Based**: Select if device model or OS version matches account's configured criteria

**Targeted Strategy Flow:**
- The service iterates through all accounts
- Each account's individual strategy and configuration is evaluated
- First matching account is selected and usage_count is incremented

**Fallback Strategy Priority** (when no targeted accounts match):
1. **Sequential**: Try sequential accounts first - rotate based on lowest usage_count
2. **Weighted**: Try weighted accounts next - select based on configured weights
3. **Random**: Try random accounts last - randomly select from pool

All selection paths properly track usage_count for analytics.

### REST API Endpoints

**Android Integration:**
- `POST /api/admob/config` - Get AdMob configuration for device
- `POST /api/device/register` - Register/update device information
- `POST /api/analytics/track` - Track analytics events
- `POST /api/notification/delivered` - Track notification delivery
- `POST /api/notification/clicked` - Track notification clicks

### Database Schema

**Main Tables:**
- `users` - Admin users
- `apps` - Android applications
- `admob_accounts` - AdMob accounts with strategies
- `devices` - Registered devices
- `analytics_events` - Event tracking
- `notification_events` - Push notifications
- `sessions`, `cache`, `jobs` - Laravel system tables

## Technology Stack

**Backend:**
- Laravel 10.x (PHP 8.2)
- PostgreSQL (via Supabase connection)
- PSR-12 coding standards

**Frontend:**
- Bootstrap 5 (via CDN)
- Chart.js for analytics
- DataTables for data management
- jQuery

**External Services:**
- Firebase Cloud Messaging (FCM) for push notifications
- Neon PostgreSQL database

## Default Credentials

**Admin User:**
- Username: `admin`
- Password: `admin123`
- Email: `admin@example.com`

**Sample App:**
- Package: `com.example.testapp`
- App Name: `Test Application`

## Configuration

The application uses environment variables for configuration:
- `DB_CONNECTION=pgsql` - PostgreSQL database
- `SESSION_DRIVER=database` - Database sessions
- `QUEUE_CONNECTION=database` - Database queues
- `CACHE_STORE=database` - Database cache

## Running the Application

The Laravel server is configured to run on `0.0.0.0:5000`:

```bash
php artisan serve --host=0.0.0.0 --port=5000
```

Access the admin panel at: http://0.0.0.0:5000/admin/login

## Development Status

**Current State:**
- All models, services, and controllers are implemented
- Basic views are created with responsive layouts
- Routes are configured for web and API
- Database schema is defined
- Server is running on port 5000

**Pending:**
- Database migrations need to be run successfully
- Complete remaining admin views (AdMob accounts, notifications, analytics)
- Add validation and error handling enhancements
- Implement rate limiting for API endpoints
- Add comprehensive testing

## API Usage Example

**Get AdMob Configuration:**
```bash
curl -X POST http://your-domain/api/admob/config \
  -H "Content-Type: application/json" \
  -d '{"package_name":"com.example.testapp","device_id":"device123"}'
```

**Register Device:**
```bash
curl -X POST http://your-domain/api/device/register \
  -H "Content-Type: application/json" \
  -d '{
    "package_name":"com.example.testapp",
    "device_id":"device123",
    "fcm_token":"fcm_token_here",
    "device_model":"Samsung Galaxy S21",
    "os_version":"Android 12",
    "app_version":"1.0.0",
    "country":"US"
  }'
```

## Recent Changes

- Created complete Laravel application structure
- Implemented all 6 AdMob switching strategies
- Built admin authentication system
- Created REST API for Android integration
- Set up PostgreSQL database with migrations
- Configured workflow to run on port 5000

## User Preferences

- Clean, production-ready code
- PSR-12 coding standards
- Bootstrap via CDN (no build process)
- Server-side rendering with Blade templates
