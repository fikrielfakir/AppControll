<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\AdMobAccount;
use App\Models\AdMobAdUnit;
use App\Models\App;

class AdMobAccountController extends Controller
{
    public function indexAll()
    {
        $accounts = AdMobAccount::withCount('adUnits')->get();
        $apps = App::all();
        return view('admin.admob.index', compact('accounts', 'apps'));
    }

    public function create(Request $request)
    {
        $validated = $request->validate([
            'account_name' => 'required',
            'publisher_id' => 'required',
            'status' => 'required|in:active,inactive',
        ]);

        AdMobAccount::create($validated);
        return redirect()->back()->with('success', 'AdMob account created successfully');
    }

    public function update(Request $request, $id)
    {
        $account = AdMobAccount::findOrFail($id);
        
        $validated = $request->validate([
            'account_name' => 'required',
            'publisher_id' => 'required',
            'status' => 'required|in:active,inactive',
        ]);

        $account->update($validated);
        return redirect()->back()->with('success', 'AdMob account updated successfully');
    }

    public function assignToApp(Request $request, $admobId, $appId)
    {
        $validated = $request->validate([
            'banner_id' => 'nullable',
            'interstitial_id' => 'nullable',
            'rewarded_id' => 'nullable',
            'native_id' => 'nullable',
            'app_open_id' => 'nullable',
            'account_id' => 'nullable',
        ]);

        $app = App::findOrFail($appId);
        $account = AdMobAccount::findOrFail($admobId);
        
        $account->update([
            'banner_id' => $validated['banner_id'] ?? null,
            'interstitial_id' => $validated['interstitial_id'] ?? null,
            'rewarded_id' => $validated['rewarded_id'] ?? null,
            'native_id' => $validated['native_id'] ?? null,
            'app_open_id' => $validated['app_open_id'] ?? null,
            'account_id' => $validated['account_id'] ?? null,
        ]);
        
        $admobApp = \App\Models\AdMobApp::where('package_name', $app->package_name)->first();
        
        if ($admobApp) {
            $admobApp->update(['default_admob_account_id' => $admobId]);
        } else {
            \App\Models\AdMobApp::create([
                'package_name' => $app->package_name,
                'app_name' => $app->app_name,
                'platform' => 'android',
                'default_admob_account_id' => $admobId,
                'is_active' => true,
                'config' => ['version' => '1.0'],
            ]);
        }
        
        return redirect()->back()->with('success', 'AdMob account assigned to app successfully with ad units');
    }

    public function destroy($id)
    {
        AdMobAccount::findOrFail($id)->delete();
        return redirect()->back()->with('success', 'AdMob account deleted successfully');
    }
}
