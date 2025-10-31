<?php

use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider and all of them will
| be assigned to the "web" middleware group. Make something great!
|
*/

Route::get('/', function () {
    return redirect('/admin/login');
});

Route::prefix('admin')->group(function () {
    Route::get('login', [App\Http\Controllers\Admin\AuthController::class, 'showLogin'])->name('admin.login');
    Route::post('login', [App\Http\Controllers\Admin\AuthController::class, 'login']);
    Route::post('logout', [App\Http\Controllers\Admin\AuthController::class, 'logout'])->name('admin.logout');

    Route::middleware('auth')->group(function () {
        Route::get('dashboard', [App\Http\Controllers\Admin\DashboardController::class, 'index'])->name('admin.dashboard');
        
        Route::resource('apps', App\Http\Controllers\Admin\AppController::class);
        
        Route::get('admob', [App\Http\Controllers\Admin\AdMobAccountController::class, 'indexAll'])->name('admob.index');
        Route::post('admob', [App\Http\Controllers\Admin\AdMobAccountController::class, 'create'])->name('admob.store');
        Route::put('admob/{id}', [App\Http\Controllers\Admin\AdMobAccountController::class, 'update'])->name('admob.update');
        Route::delete('admob/{id}', [App\Http\Controllers\Admin\AdMobAccountController::class, 'destroy'])->name('admob.destroy');
        Route::post('admob/{admobId}/assign/{appId}', [App\Http\Controllers\Admin\AdMobAccountController::class, 'assignToApp'])->name('admob.assign');
        
        Route::get('notifications', [App\Http\Controllers\Admin\NotificationController::class, 'index'])->name('notifications.index');
        Route::post('notifications', [App\Http\Controllers\Admin\NotificationController::class, 'store'])->name('notifications.store');
        Route::post('notifications/{id}/send', [App\Http\Controllers\Admin\NotificationController::class, 'send'])->name('notifications.send');
        
        Route::get('devices', [App\Http\Controllers\Admin\DeviceController::class, 'index'])->name('devices.index');
        
        Route::get('analytics', [App\Http\Controllers\Admin\AnalyticsController::class, 'index'])->name('analytics.index');
    });
});
