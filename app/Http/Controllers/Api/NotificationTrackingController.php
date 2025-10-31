<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class NotificationTrackingController extends Controller
{
    public function delivered(Request $request)
    {
        $notificationId = $request->input('notification_id');
        
        $notification = \App\Models\NotificationEvent::find($notificationId);
        if ($notification) {
            $notification->increment('delivered_count');
        }

        return response()->json(['success' => true]);
    }

    public function clicked(Request $request)
    {
        $notificationId = $request->input('notification_id');
        
        $notification = \App\Models\NotificationEvent::find($notificationId);
        if ($notification) {
            $notification->increment('clicked_count');
        }

        return response()->json(['success' => true]);
    }
}
