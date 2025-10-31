<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class AdMobApp extends Model
{
    use HasFactory;

    protected $table = 'admob_apps';

    protected $fillable = [
        'package_name',
        'app_name',
        'platform',
        'default_admob_account_id',
        'is_active',
        'config',
    ];

    protected $casts = [
        'is_active' => 'boolean',
        'config' => 'array',
    ];

    public function admobAccount()
    {
        return $this->belongsTo(AdMobAccount::class, 'default_admob_account_id');
    }

    public function app()
    {
        return $this->belongsTo(App::class, 'package_name', 'package_name');
    }
}
