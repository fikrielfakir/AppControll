<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class AppController extends Controller
{
    public function index()
    {
        $apps = \App\Models\App::orderBy('created_at', 'desc')->get();
        return view('admin.apps.index', compact('apps'));
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'package_name' => 'required|unique:apps',
            'app_name' => 'required',
            'icon_url' => 'nullable|url',
            'fcm_server_key' => 'nullable',
        ]);

        $app = \App\Models\App::create($validated);
        
        \App\Models\AdMobApp::create([
            'package_name' => $validated['package_name'],
            'app_name' => $validated['app_name'],
            'platform' => 'android',
            'is_active' => true,
            'config' => ['version' => '1.0'],
        ]);
        
        return redirect()->back()->with('success', 'App created successfully');
    }

    public function update(Request $request, $id)
    {
        $app = \App\Models\App::findOrFail($id);
        $validated = $request->validate([
            'package_name' => 'required|unique:apps,package_name,' . $id,
            'app_name' => 'required',
            'icon_url' => 'nullable|url',
            'fcm_server_key' => 'nullable',
        ]);

        $app->update($validated);
        return redirect()->back()->with('success', 'App updated successfully');
    }

    public function destroy($id)
    {
        \App\Models\App::findOrFail($id)->delete();
        return redirect()->back()->with('success', 'App deleted successfully');
    }
}
