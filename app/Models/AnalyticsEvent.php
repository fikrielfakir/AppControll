<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class AnalyticsEvent extends Model
{
    use HasFactory;

    protected $fillable = [
        'app_id',
        'device_id',
        'event_type',
        'event_name',
        'event_data',
        'event_timestamp',
    ];

    protected $casts = [
        'event_data' => 'array',
        'event_timestamp' => 'datetime',
    ];

    public function app()
    {
        return $this->belongsTo(App::class);
    }

    public function device()
    {
        return $this->belongsTo(Device::class);
    }
}
