<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\Device;
use App\Models\App;

class DeviceController extends Controller
{
    public function index(Request $request)
    {
        $query = Device::with('app');
        
        if ($request->has('app_id') && $request->app_id) {
            $query->where('app_id', $request->app_id);
        }
        
        if ($request->has('country') && $request->country) {
            $query->where('country', $request->country);
        }
        
        if ($request->has('date_from') && $request->date_from) {
            $query->where('last_active_at', '>=', $request->date_from);
        }
        
        $devices = $query->orderBy('last_active_at', 'desc')->paginate(50);
        $apps = App::all();
        $countries = Device::select('country')->distinct()->whereNotNull('country')->pluck('country');
        
        $activeDevices = Device::where('last_active_at', '>=', now()->subDays(7))->count();
        
        return view('admin.devices.index', compact('devices', 'apps', 'countries', 'activeDevices'));
    }
}
