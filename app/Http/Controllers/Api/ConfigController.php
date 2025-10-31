<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class ConfigController extends Controller
{
    public function getConfig(Request $request, string $packageName)
    {
        $app = DB::table('admob_apps')
            ->where('package_name', $packageName)
            ->where('is_active', true)
            ->first();

        if (!$app) {
            return response()->json([
                'admob_accounts' => [],
                'message' => 'App not configured'
            ], 404);
        }

        if (!$app->default_admob_account_id) {
            return response()->json([
                'admob_accounts' => [],
                'message' => 'No AdMob account assigned to this app'
            ], 404);
        }

        $account = DB::table('admob_accounts')
            ->where('id', $app->default_admob_account_id)
            ->where('status', 'active')
            ->first();

        if (!$account) {
            return response()->json([
                'admob_accounts' => [],
                'message' => 'Assigned AdMob account is inactive or not found'
            ], 404);
        }

        $formattedAccount = [
            'account_id' => $account->account_id,
            'status' => $account->status,
            'banner_id' => $account->banner_id,
            'interstitial_id' => $account->interstitial_id,
            'rewarded_id' => $account->rewarded_id,
            'app_open_id' => $account->app_open_id,
            'native_id' => $account->native_id,
        ];

        return response()->json([
            'admob_accounts' => [$formattedAccount],
            'app_config' => json_decode($app->config),
        ]);
    }
}
