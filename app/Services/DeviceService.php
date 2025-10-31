<?php

namespace App\Services;

use App\Models\App;
use App\Models\Device;
use Carbon\Carbon;

class DeviceService
{
    public function registerOrUpdateDevice(array $data): Device
    {
        $app = App::where('package_name', $data['package_name'])->firstOrFail();

        $device = Device::where('fcm_token', $data['fcm_token'])
            ->where('app_id', $app->id)
            ->first();

        if ($device) {
            $device->update([
                'device_model' => $data['device_model'] ?? $device->device_model,
                'os_version' => $data['os_version'] ?? $device->os_version,
                'app_version' => $data['app_version'] ?? $device->app_version,
                'country' => $data['country'] ?? $device->country,
                'language' => $data['language'] ?? $device->language,
                'last_active_at' => Carbon::now(),
            ]);
        } else {
            $device = Device::create([
                'app_id' => $app->id,
                'device_id' => \Illuminate\Support\Str::uuid()->toString(),
                'fcm_token' => $data['fcm_token'],
                'device_model' => $data['device_model'] ?? null,
                'os_version' => $data['os_version'] ?? null,
                'app_version' => $data['app_version'] ?? null,
                'country' => $data['country'] ?? null,
                'language' => $data['language'] ?? null,
                'last_active_at' => Carbon::now(),
            ]);
        }

        return $device;
    }

    public function updateLastActive(string $deviceId): void
    {
        Device::where('device_id', $deviceId)->update([
            'last_active_at' => Carbon::now(),
        ]);
    }

    public function updateFcmToken(string $deviceId, string $fcmToken): void
    {
        Device::where('device_id', $deviceId)->update([
            'fcm_token' => $fcmToken,
            'last_active_at' => Carbon::now(),
        ]);
    }
}
