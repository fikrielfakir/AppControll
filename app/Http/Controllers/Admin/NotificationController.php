<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class NotificationController extends Controller
{
    public function index()
    {
        $notifications = \App\Models\NotificationEvent::with('app')->orderBy('created_at', 'desc')->get();
        $apps = \App\Models\App::where('is_active', true)->get();
        
        $firebaseStatus = [
            'connected' => false,
            'message' => 'Firebase not configured'
        ];
        
        try {
            $firebase = new \App\Services\FirebaseService();
            $firebaseStatus = [
                'connected' => true,
                'message' => 'Firebase Cloud Messaging is active and ready to send notifications'
            ];
        } catch (\Exception $e) {
            $firebaseStatus = [
                'connected' => false,
                'message' => 'Firebase initialization failed: ' . $e->getMessage()
            ];
            \Log::error('Firebase initialization failed in admin panel: ' . $e->getMessage());
        }
        
        return view('admin.notifications.index', compact('notifications', 'apps', 'firebaseStatus'));
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'title' => 'required',
            'message' => 'required',
            'target_app' => 'nullable|exists:apps,id',
            'target_country' => 'nullable',
            'target_version' => 'nullable',
        ]);

        $targetingRules = [];
        if (!empty($validated['target_country']) && $validated['target_country'] !== 'ALL') {
            $targetingRules['countries'] = [$validated['target_country']];
        }
        if (!empty($validated['target_version'])) {
            $targetingRules['app_versions'] = [$validated['target_version']];
        }

        $notification = \App\Models\NotificationEvent::create([
            'app_id' => $validated['target_app'] ?? null,
            'title' => $validated['title'],
            'body' => $validated['message'],
            'status' => 'pending',
            'targeting_rules' => !empty($targetingRules) ? $targetingRules : null,
        ]);

        $service = new \App\Services\NotificationService(new \App\Services\NotificationTargetingService());
        $result = $service->sendNotification($notification);

        if ($result['success']) {
            return redirect()->back()->with('success', $result['message'] . ' - Sent to ' . $result['sent_count'] . ' devices, delivered to ' . $result['delivered_count']);
        } else {
            return redirect()->back()->with('error', $result['message']);
        }
    }

    public function send($id)
    {
        $notification = \App\Models\NotificationEvent::findOrFail($id);
        $service = new \App\Services\NotificationService(new \App\Services\NotificationTargetingService());
        
        $result = $service->sendNotification($notification);
        
        if ($result['success']) {
            return redirect()->back()->with('success', $result['message']);
        } else {
            return redirect()->back()->with('error', $result['message']);
        }
    }
}
