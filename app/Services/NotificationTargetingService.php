<?php

namespace App\Services;

use App\Models\Device;
use App\Models\NotificationEvent;
use Illuminate\Support\Collection;

class NotificationTargetingService
{
    public function getTargetedDevices(NotificationEvent $notification): Collection
    {
        $query = Device::where('app_id', $notification->app_id)
            ->whereNotNull('fcm_token');

        $rules = $notification->targeting_rules ?? [];

        if (isset($rules['countries']) && is_array($rules['countries']) && !empty($rules['countries'])) {
            $query->whereIn('country', $rules['countries']);
        }

        if (isset($rules['device_models']) && is_array($rules['device_models']) && !empty($rules['device_models'])) {
            $query->whereIn('device_model', $rules['device_models']);
        }

        if (isset($rules['app_versions']) && is_array($rules['app_versions']) && !empty($rules['app_versions'])) {
            $query->whereIn('app_version', $rules['app_versions']);
        }

        if (isset($rules['os_versions']) && is_array($rules['os_versions']) && !empty($rules['os_versions'])) {
            $query->whereIn('os_version', $rules['os_versions']);
        }

        if (isset($rules['languages']) && is_array($rules['languages']) && !empty($rules['languages'])) {
            $query->whereIn('language', $rules['languages']);
        }

        return $query->get();
    }

    public function applyFilters(Collection $devices, array $rules): Collection
    {
        return $devices->filter(function ($device) use ($rules) {
            if (isset($rules['countries']) && !in_array($device->country, $rules['countries'])) {
                return false;
            }

            if (isset($rules['device_models']) && !in_array($device->device_model, $rules['device_models'])) {
                return false;
            }

            if (isset($rules['app_versions']) && !in_array($device->app_version, $rules['app_versions'])) {
                return false;
            }

            return true;
        });
    }
}
