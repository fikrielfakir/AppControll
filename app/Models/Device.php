<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Device extends Model
{
    use HasFactory;

    protected $fillable = [
        'app_id',
        'device_id',
        'fcm_token',
        'device_model',
        'manufacturer',
        'os_version',
        'app_version',
        'version',
        'country',
        'language',
        'last_active_at',
    ];
    protected $dates = [
    'last_active_at',
    'created_at',
    'updated_at',
];

    protected $casts = [
        'last_active_at' => 'datetime',
    ];

    public function app()
    {
        return $this->belongsTo(App::class);
    }

    public function analyticsEvents()
    {
        return $this->hasMany(AnalyticsEvent::class);
    }
}
