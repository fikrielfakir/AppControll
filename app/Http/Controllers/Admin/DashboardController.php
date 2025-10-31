<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class DashboardController extends Controller
{
    public function index()
    {
        $analyticsService = new \App\Services\AnalyticsService();
        $stats = $analyticsService->getDashboardStats();
        $apps = \App\Models\App::where('is_active', true)->get();
        
        return view('admin.dashboard', compact('stats', 'apps'));
    }
}
