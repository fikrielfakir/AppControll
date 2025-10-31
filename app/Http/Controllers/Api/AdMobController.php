<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class AdMobController extends Controller
{
    protected $strategyService;

    public function __construct()
    {
        $this->strategyService = new \App\Services\AdMobStrategyService();
    }

    public function getConfig(Request $request)
    {
        $packageName = $request->input('package_name');
        $deviceId = $request->input('device_id');

        if (!$packageName) {
            return response()->json(['error' => 'package_name is required'], 400);
        }

        $account = $this->strategyService->getAdMobAccountForDevice($packageName, $deviceId);

        if (!$account) {
            return response()->json(['error' => 'No AdMob account found'], 404);
        }

        return response()->json([
            'success' => true,
            'admob_account_id' => $account->admob_account_id,
            'app_name' => $account->app_name,
        ]);
    }
}
