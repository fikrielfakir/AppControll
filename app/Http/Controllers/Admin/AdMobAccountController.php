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
        ]);

        $validated['account_id'] = $admobId;
        $validated['app_id'] = $appId;

        AdMobAdUnit::create($validated);
        return redirect()->back()->with('success', 'Ad units assigned successfully');
    }

    public function destroy($id)
    {
        AdMobAccount::findOrFail($id)->delete();
        return redirect()->back()->with('success', 'AdMob account deleted successfully');
    }
}
