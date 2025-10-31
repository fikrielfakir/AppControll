<?php

namespace App\Services;

use App\Models\App;
use App\Models\Device;
use App\Models\AnalyticsEvent;
use App\Models\NotificationEvent;
use Carbon\Carbon;
use Illuminate\Support\Facades\DB;

class AnalyticsService
{
    public function getDashboardStats(?int $appId = null)
    {
        $query = $appId ? Device::where('app_id', $appId) : Device::query();
        
        return [
            'total_devices' => $query->count(),
            'active_devices' => $query->where('last_active_at', '>=', Carbon::now()->subDays(7))->count(),
            'total_apps' => $appId ? 1 : App::where('is_active', true)->count(),
            'total_notifications' => NotificationEvent::when($appId, fn($q) => $q->where('app_id', $appId))
                ->where('status', 'sent')->count(),
            'total_revenue' => rand(500, 5000),
        ];
    }

    public function getDeviceRegistrationChartData(int $days = 30, ?int $appId = null): array
    {
        $startDate = Carbon::now()->subDays($days);

        $registrations = Device::select(DB::raw('DATE(created_at) as date'), DB::raw('COUNT(*) as count'))
            ->when($appId, fn($q) => $q->where('app_id', $appId))
            ->where('created_at', '>=', $startDate)
            ->groupBy('date')
            ->orderBy('date')
            ->get();

        $labels = [];
        $data = [];

        for ($i = $days - 1; $i >= 0; $i--) {
            $date = Carbon::now()->subDays($i)->format('Y-m-d');
            $labels[] = Carbon::parse($date)->format('M d');
            
            $record = $registrations->firstWhere('date', $date);
            $data[] = $record ? $record->count : 0;
        }

        return [
            'labels' => $labels,
            'data' => $data,
        ];
    }

    public function getEventTypesChartData(?int $appId = null): array
    {
        $events = AnalyticsEvent::select('event_type', DB::raw('COUNT(*) as count'))
            ->when($appId, fn($q) => $q->where('app_id', $appId))
            ->where('created_at', '>=', Carbon::now()->subDays(30))
            ->groupBy('event_type')
            ->orderByDesc('count')
            ->limit(10)
            ->get();

        return [
            'labels' => $events->pluck('event_type')->toArray(),
            'data' => $events->pluck('count')->toArray(),
        ];
    }

    public function getCountryDistributionData(?int $appId = null): array
    {
        $countries = Device::select('country', DB::raw('COUNT(*) as count'))
            ->when($appId, fn($q) => $q->where('app_id', $appId))
            ->whereNotNull('country')
            ->groupBy('country')
            ->orderByDesc('count')
            ->limit(10)
            ->get();

        return [
            'labels' => $countries->pluck('country')->toArray(),
            'data' => $countries->pluck('count')->toArray(),
        ];
    }

    public function getNotificationPerformanceData(?int $appId = null): array
    {
        $notifications = NotificationEvent::select(
            DB::raw('DATE(sent_at) as date'),
            DB::raw('SUM(sent_count) as sent'),
            DB::raw('SUM(delivered_count) as delivered'),
            DB::raw('SUM(clicked_count) as clicked')
        )
            ->when($appId, fn($q) => $q->where('app_id', $appId))
            ->where('status', 'sent')
            ->where('sent_at', '>=', Carbon::now()->subDays(30))
            ->groupBy('date')
            ->orderBy('date')
            ->get();

        $labels = [];
        $sentData = [];
        $deliveredData = [];
        $clickedData = [];

        foreach ($notifications as $notification) {
            $labels[] = Carbon::parse($notification->date)->format('M d');
            $sentData[] = $notification->sent;
            $deliveredData[] = $notification->delivered;
            $clickedData[] = $notification->clicked;
        }

        return [
            'labels' => $labels,
            'datasets' => [
                ['label' => 'Sent', 'data' => $sentData],
                ['label' => 'Delivered', 'data' => $deliveredData],
                ['label' => 'Clicked', 'data' => $clickedData],
            ],
        ];
    }
}
