<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class NotificationEvent extends Model
{
    use HasFactory;

    protected $fillable = [
        'app_id',
        'title',
        'body',
        'targeting_rules',
        'status',
        'sent_count',
        'delivered_count',
        'clicked_count',
        'sent_at',
    ];

    protected $casts = [
        'targeting_rules' => 'array',
        'sent_at' => 'datetime',
    ];

    public function app()
    {
        return $this->belongsTo(App::class);
    }
}
