<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Here is where you can register API routes for your application. These
| routes are loaded by the RouteServiceProvider and all of them will
| be assigned to the "api" middleware group. Make something great!
|
*/

Route::middleware('auth:sanctum')->get('/user', function (Request $request) {
    return $request->user();
});

Route::prefix('v1')->group(function () {
    Route::get('/config/{package_name}', [App\Http\Controllers\Api\ConfigController::class, 'getConfig']);
    
    Route::post('/device/register', [App\Http\Controllers\Api\DeviceController::class, 'register']);
    
    Route::prefix('notifications')->group(function () {
        Route::get('/pending', [App\Http\Controllers\Api\NotificationController::class, 'getPending']);
        Route::post('/track', [App\Http\Controllers\Api\NotificationController::class, 'track']);
        Route::post('/create', [App\Http\Controllers\Api\NotificationController::class, 'create']);
    });
    
    Route::prefix('analytics')->group(function () {
        Route::post('/admob', [App\Http\Controllers\Api\AnalyticsController::class, 'trackAdMobEvent']);
        Route::get('/{package_name}/stats', [App\Http\Controllers\Api\AnalyticsController::class, 'getStats']);
    });
});
