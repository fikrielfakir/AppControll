<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class AdMobAccount extends Model
{
    use HasFactory;

    protected $table = 'admob_accounts';

    protected $fillable = [
        'account_name',
        'publisher_id',
        'status',
        'app_id',
        'admob_account_id',
        'app_name',
        'switching_strategy',
        'strategy_config',
        'weight',
        'usage_count',
        'is_active',
    ];

    protected $casts = [
        'strategy_config' => 'array',
        'is_active' => 'boolean',
    ];

    public function app()
    {
        return $this->belongsTo(App::class);
    }

    public function adUnits()
    {
        return $this->hasMany(AdMobAdUnit::class, 'account_id');
    }
}
