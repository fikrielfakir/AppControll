<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class App extends Model
{
    use HasFactory;

    protected $fillable = [
        'package_name',
        'app_name',
        'icon_url',
        'fcm_server_key',
        'is_active',
    ];

    protected $casts = [
        'is_active' => 'boolean',
    ];

    public function admobAccounts()
    {
        return $this->hasMany(AdMobAccount::class);
    }

    public function devices()
    {
        return $this->hasMany(Device::class);
    }

    public function analyticsEvents()
    {
        return $this->hasMany(AnalyticsEvent::class);
    }

    public function notificationEvents()
    {
        return $this->hasMany(NotificationEvent::class);
    }
}
