<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class AnalyticsController extends Controller
{
    public function track(Request $request)
    {
        $validated = $request->validate([
            'package_name' => 'required',
            'device_id' => 'required',
            'event_type' => 'required',
            'event_name' => 'required',
            'event_data' => 'nullable|array',
        ]);

        $app = \App\Models\App::where('package_name', $validated['package_name'])->first();
        if (!$app) {
            return response()->json(['error' => 'App not found'], 404);
        }

        $device = \App\Models\Device::where('device_id', $validated['device_id'])->first();

        \App\Models\AnalyticsEvent::create([
            'app_id' => $app->id,
            'device_id' => $device ? $device->id : null,
            'event_type' => $validated['event_type'],
            'event_name' => $validated['event_name'],
            'event_data' => $validated['event_data'] ?? null,
            'event_timestamp' => now(),
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Event tracked successfully',
        ]);
    }

    public function trackAdMobEvent(Request $request)
    {
        $validated = $request->validate([
            'package_name' => 'required|string',
            'account_id' => 'nullable|string',
            'event' => 'required|string',
            'ad_type' => 'nullable|string',
            'value' => 'sometimes|integer',
        ]);

        $app = \Illuminate\Support\Facades\DB::table('admob_apps')
            ->where('package_name', $validated['package_name'])
            ->where('is_active', true)
            ->first();

        if (!$app) {
            return response()->json(['error' => 'Invalid package name'], 404);
        }

        if (isset($validated['account_id'])) {
            $account = \Illuminate\Support\Facades\DB::table('admob_accounts')
                ->where('account_id', $validated['account_id'])
                ->first();

            if (!$account) {
                return response()->json(['error' => 'Invalid account ID'], 404);
            }

            if ($app->default_admob_account_id != $account->id) {
                return response()->json(['error' => 'Account ID does not match app configuration'], 403);
            }
        }

        \Illuminate\Support\Facades\DB::table('admob_analytics')->insert([
            'package_name' => $validated['package_name'],
            'account_id' => $validated['account_id'] ?? null,
            'event' => $validated['event'],
            'ad_type' => $validated['ad_type'] ?? null,
            'value' => $validated['value'] ?? 0,
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return response()->json(['success' => true]);
    }

    public function getStats(Request $request, string $packageName)
    {
        $app = \Illuminate\Support\Facades\DB::table('admob_apps')
            ->where('package_name', $packageName)
            ->where('is_active', true)
            ->first();

        if (!$app) {
            return response()->json(['error' => 'Invalid package name'], 404);
        }

        $startDate = $request->input('start_date', now()->subDays(7)->toDateString());
        $endDate = $request->input('end_date', now()->toDateString());

        $stats = \Illuminate\Support\Facades\DB::table('admob_analytics')
            ->select('event', 'ad_type', \Illuminate\Support\Facades\DB::raw('COUNT(*) as count'), \Illuminate\Support\Facades\DB::raw('SUM(value) as total_value'))
            ->where('package_name', $packageName)
            ->whereBetween('created_at', [$startDate, $endDate])
            ->groupBy('event', 'ad_type')
            ->get();

        return response()->json([
            'package_name' => $packageName,
            'start_date' => $startDate,
            'end_date' => $endDate,
            'stats' => $stats,
        ]);
    }
}
