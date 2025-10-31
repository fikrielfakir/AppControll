<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class AdMobAdUnit extends Model
{
    use HasFactory;

    protected $table = 'ad_units';

    protected $fillable = [
        'account_id',
        'app_id',
        'banner_id',
        'interstitial_id',
        'rewarded_id',
        'native_id',
    ];

    public function account()
    {
        return $this->belongsTo(AdMobAccount::class, 'account_id');
    }

    public function app()
    {
        return $this->belongsTo(App::class);
    }
}
