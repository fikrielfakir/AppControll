<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class DeviceController extends Controller
{
    protected $deviceService;

    public function __construct()
    {
        $this->deviceService = new \App\Services\DeviceService();
    }

    public function register(Request $request)
    {
        $validated = $request->validate([
            'package_name' => 'required|string',
            'fcm_token' => 'required|string',
            'device_info' => 'nullable|array',
            'device_info.country' => 'nullable|string',
            'device_info.app_version' => 'nullable|string',
            'device_info.android_version' => 'nullable|string',
            'device_info.manufacturer' => 'nullable|string',
            'device_info.model' => 'nullable|string',
        ]);

        try {
            $deviceInfo = $validated['device_info'] ?? [];
            
            $deviceData = [
                'package_name' => $validated['package_name'],
                'fcm_token' => $validated['fcm_token'],
                'country' => $deviceInfo['country'] ?? null,
                'app_version' => $deviceInfo['app_version'] ?? null,
                'os_version' => $deviceInfo['android_version'] ?? null,
                'device_model' => $deviceInfo['model'] ?? null,
                'manufacturer' => $deviceInfo['manufacturer'] ?? null,
            ];

            $device = $this->deviceService->registerOrUpdateDevice($deviceData);
            
            return response()->json([
                'success' => true,
                'message' => 'Device registered successfully',
                'device_id' => (string) $device->id,
            ]);
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => $e->getMessage(),
            ], 400);
        }
    }
}
