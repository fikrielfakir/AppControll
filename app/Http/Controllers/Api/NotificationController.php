<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Services\FirebaseService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class NotificationController extends Controller
{
    protected $firebase;

    public function __construct(FirebaseService $firebase)
    {
        $this->firebase = $firebase;
    }

    public function getPending(Request $request)
    {
        $packageName = $request->input('package_name');

        $notifications = DB::table('notifications')
            ->where(function ($query) use ($packageName) {
                $query->where('package_name', $packageName)
                      ->orWhereNull('package_name');
            })
            ->where('status', 'pending')
            ->where(function ($query) {
                $query->whereNull('scheduled_at')
                      ->orWhere('scheduled_at', '<=', now());
            })
            ->where(function ($query) {
                $query->whereNull('expires_at')
                      ->orWhere('expires_at', '>', now());
            })
            ->get();

        $formattedNotifications = $notifications->map(function ($notif) {
            return [
                'notification_id' => $notif->notification_id,
                'title' => $notif->title,
                'message' => $notif->message,
                'type' => $notif->type,
                'priority' => $notif->priority,
                'content' => [
                    'image_url' => $notif->image_url,
                    'action_button_text' => $notif->action_button_text,
                    'action_type' => $notif->action_type,
                    'action_value' => $notif->action_value,
                    'cancelable' => (bool) $notif->cancelable,
                ],
                'display_rules' => [
                    'max_displays' => $notif->max_displays,
                    'display_interval_hours' => $notif->display_interval_hours,
                    'show_on_app_launch' => (bool) $notif->show_on_app_launch,
                ],
            ];
        });

        return response()->json([
            'notifications' => $formattedNotifications,
        ]);
    }

    public function track(Request $request)
    {
        $validated = $request->validate([
            'notification_id' => 'required|string',
            'device_id' => 'required|string',
            'event' => 'required|string|in:displayed,clicked,dismissed',
            'timestamp' => 'required|integer',
        ]);

        $notification = DB::table('notifications')
            ->where('notification_id', $validated['notification_id'])
            ->first();

        if (!$notification) {
            return response()->json(['error' => 'Notification not found'], 404);
        }

        DB::table('notification_tracking')->insert([
            'notification_id' => $notification->id,
            'device_id' => $validated['device_id'],
            'event' => $validated['event'],
            'event_at' => now(),
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return response()->json(['success' => true]);
    }

    public function create(Request $request)
    {
        $validated = $request->validate([
            'package_name' => 'nullable|string',
            'title' => 'required|string',
            'message' => 'required|string',
            'type' => 'sometimes|string',
            'priority' => 'sometimes|string',
            'image_url' => 'nullable|string',
            'action_button_text' => 'nullable|string',
            'action_type' => 'nullable|string',
            'action_value' => 'nullable|string',
            'cancelable' => 'sometimes|boolean',
            'max_displays' => 'sometimes|integer',
            'display_interval_hours' => 'sometimes|integer',
            'show_on_app_launch' => 'sometimes|boolean',
            'scheduled_at' => 'nullable|date',
            'expires_at' => 'nullable|date',
            'send_now' => 'sometimes|boolean',
        ]);

        $notificationId = Str::uuid()->toString();

        DB::table('notifications')->insert([
            'notification_id' => $notificationId,
            'package_name' => $validated['package_name'] ?? null,
            'title' => $validated['title'],
            'message' => $validated['message'],
            'type' => $validated['type'] ?? 'popup',
            'priority' => $validated['priority'] ?? 'normal',
            'image_url' => $validated['image_url'] ?? null,
            'action_button_text' => $validated['action_button_text'] ?? null,
            'action_type' => $validated['action_type'] ?? null,
            'action_value' => $validated['action_value'] ?? null,
            'cancelable' => $validated['cancelable'] ?? true,
            'max_displays' => $validated['max_displays'] ?? 1,
            'display_interval_hours' => $validated['display_interval_hours'] ?? 24,
            'show_on_app_launch' => $validated['show_on_app_launch'] ?? false,
            'scheduled_at' => $validated['scheduled_at'] ?? null,
            'expires_at' => $validated['expires_at'] ?? null,
            'status' => 'pending',
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        if ($request->input('send_now', false)) {
            $devices = DB::table('devices')
                ->where('package_name', $validated['package_name'])
                ->pluck('fcm_token')
                ->toArray();

            if (!empty($devices)) {
                $this->firebase->sendMulticastNotification(
                    $devices,
                    $validated['title'],
                    $validated['message'],
                    ['notification_id' => $notificationId]
                );
            }
        }

        return response()->json([
            'success' => true,
            'notification_id' => $notificationId,
        ]);
    }
}
