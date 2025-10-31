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
        return view('admin.notifications.index', compact('notifications', 'apps'));
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

        $validated['status'] = 'pending';
        $validated['app_id'] = $validated['target_app'] ?? null;
        $validated['body'] = $validated['message'];

        \App\Models\NotificationEvent::create($validated);
        return redirect()->back()->with('success', 'Notification created and will be sent shortly');
    }

    public function send($id)
    {
        $notification = \App\Models\NotificationEvent::findOrFail($id);
        $service = new \App\Services\NotificationService(new \App\Services\NotificationTargetingService());
        
        $result = $service->sendNotification($notification);
        
        return redirect()->back()->with('success', $result['message']);
    }
}
