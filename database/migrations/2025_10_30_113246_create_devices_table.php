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
        Schema::create('devices', function (Blueprint $table) {
            $table->id();
            $table->foreignId('app_id')->constrained()->onDelete('cascade');
            $table->string('device_id')->unique();
            $table->string('fcm_token')->nullable();
            $table->string('device_model')->nullable();
            $table->string('os_version')->nullable();
            $table->string('app_version')->nullable();
            $table->string('country')->nullable();
            $table->string('language')->nullable();
            $table->timestamp('last_active_at')->nullable();
            $table->timestamps();
            
            $table->index('device_id');
            $table->index('app_id');
            $table->index(['app_id', 'country']);
            $table->index(['app_id', 'device_model']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('devices');
    }
};
