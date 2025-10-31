<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('admob_accounts', function (Blueprint $table) {
            $table->string('account_id')->nullable();
            $table->string('banner_id')->nullable();
            $table->string('interstitial_id')->nullable();
            $table->string('rewarded_id')->nullable();
            $table->string('app_open_id')->nullable();
            $table->string('native_id')->nullable();
        });
    }

    public function down(): void
    {
        Schema::table('admob_accounts', function (Blueprint $table) {
            $table->dropColumn(['account_id', 'banner_id', 'interstitial_id', 'rewarded_id', 'app_open_id', 'native_id']);
        });
    }
};
