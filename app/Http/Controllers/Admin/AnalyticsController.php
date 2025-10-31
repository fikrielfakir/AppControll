<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class AnalyticsController extends Controller
{
    public function index(Request $request)
    {
        $analyticsService = new \App\Services\AnalyticsService();
        $appId = $request->get('app_id');
        
        $stats = $analyticsService->getDashboardStats($appId);
        $deviceChart = $analyticsService->getDeviceRegistrationChartData(30, $appId);
        $eventChart = $analyticsService->getEventTypesChartData($appId);
        $countryChart = $analyticsService->getCountryDistributionData($appId);
        $apps = \App\Models\App::where('is_active', true)->get();
        
        return view('admin.analytics', compact('stats', 'deviceChart', 'eventChart', 'countryChart', 'apps'));
    }
}
