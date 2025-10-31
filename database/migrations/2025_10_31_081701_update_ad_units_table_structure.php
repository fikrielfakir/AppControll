<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        if (!Schema::hasTable('ad_units')) {
            Schema::create('ad_units', function (Blueprint $table) {
                $table->id();
                $table->foreignId('account_id')->constrained('admob_accounts')->onDelete('cascade');
                $table->foreignId('app_id')->constrained('apps')->onDelete('cascade');
                $table->string('banner_id')->nullable();
                $table->string('interstitial_id')->nullable();
                $table->string('rewarded_id')->nullable();
                $table->string('native_id')->nullable();
                $table->timestamps();
            });
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('ad_units');
    }
};
