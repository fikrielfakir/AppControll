<?php

namespace App\Services;

use App\Models\App;
use App\Models\AdMobAccount;
use App\Models\Device;
use Carbon\Carbon;

class AdMobStrategyService
{
    public function getAdMobAccountForDevice(string $packageName, ?string $deviceId = null): ?AdMobAccount
    {
        $app = App::where('package_name', $packageName)->where('is_active', true)->first();
        
        if (!$app) {
            return null;
        }

        $accounts = $app->admobAccounts()->where('is_active', true)->get();

        if ($accounts->isEmpty()) {
            return null;
        }

        if ($accounts->count() === 1) {
            $account = $accounts->first();
            $account->increment('usage_count');
            return $account;
        }

        $device = $deviceId ? Device::where('device_id', $deviceId)->first() : null;
        
        foreach ($accounts as $account) {
            $selected = $this->evaluateAccount($account, $device);
            if ($selected) {
                $account->increment('usage_count');
                return $account;
            }
        }
        
        $sequentialAccounts = $accounts->where('switching_strategy', 'sequential');
        if ($sequentialAccounts->isNotEmpty()) {
            return $this->getSequentialAccount($sequentialAccounts);
        }
        
        $weightedAccounts = $accounts->where('switching_strategy', 'weighted');
        if ($weightedAccounts->isNotEmpty()) {
            return $this->getWeightedAccount($weightedAccounts);
        }
        
        $randomAccounts = $accounts->where('switching_strategy', 'random');
        if ($randomAccounts->isNotEmpty()) {
            return $this->getRandomAccount($randomAccounts);
        }
        
        $account = $accounts->random();
        $account->increment('usage_count');
        return $account;
    }

    protected function evaluateAccount(AdMobAccount $account, ?Device $device): bool
    {
        return match ($account->switching_strategy) {
            'time_based' => $this->evaluateTimeBasedAccount($account),
            'location_based' => $this->evaluateLocationBasedAccount($account, $device),
            'device_based' => $this->evaluateDeviceBasedAccount($account, $device),
            default => false,
        };
    }

    protected function evaluateTimeBasedAccount(AdMobAccount $account): bool
    {
        $config = $account->strategy_config ?? [];
        if (!isset($config['start_hour']) || !isset($config['end_hour'])) {
            return false;
        }

        $hour = Carbon::now()->hour;
        $startHour = (int) $config['start_hour'];
        $endHour = (int) $config['end_hour'];
        
        return $this->isHourInRange($hour, $startHour, $endHour);
    }

    protected function evaluateLocationBasedAccount(AdMobAccount $account, ?Device $device): bool
    {
        if (!$device || !$device->country) {
            return false;
        }

        $config = $account->strategy_config ?? [];
        if (!isset($config['countries']) || !is_array($config['countries'])) {
            return false;
        }
        
        return in_array($device->country, $config['countries']);
    }

    protected function evaluateDeviceBasedAccount(AdMobAccount $account, ?Device $device): bool
    {
        if (!$device) {
            return false;
        }

        $config = $account->strategy_config ?? [];
        
        if (isset($config['device_models']) && is_array($config['device_models'])) {
            if ($device->device_model && in_array($device->device_model, $config['device_models'])) {
                return true;
            }
        }
        
        if (isset($config['os_versions']) && is_array($config['os_versions'])) {
            if ($device->os_version && in_array($device->os_version, $config['os_versions'])) {
                return true;
            }
        }
        
        return false;
    }

    protected function getRandomAccount($accounts): AdMobAccount
    {
        $account = $accounts->random();
        $account->increment('usage_count');
        return $account;
    }

    protected function getSequentialAccount($accounts): AdMobAccount
    {
        $account = $accounts->sortBy('usage_count')->first();
        $account->increment('usage_count');
        return $account;
    }

    protected function getWeightedAccount($accounts): AdMobAccount
    {
        $totalWeight = $accounts->sum('weight');
        $random = rand(1, max(1, $totalWeight));
        
        $currentWeight = 0;
        foreach ($accounts as $account) {
            $currentWeight += $account->weight;
            if ($random <= $currentWeight) {
                $account->increment('usage_count');
                return $account;
            }
        }
        
        $account = $accounts->first();
        $account->increment('usage_count');
        return $account;
    }

    protected function isHourInRange(int $hour, int $start, int $end): bool
    {
        if ($start <= $end) {
            return $hour >= $start && $hour <= $end;
        } else {
            return $hour >= $start || $hour <= $end;
        }
    }
}
