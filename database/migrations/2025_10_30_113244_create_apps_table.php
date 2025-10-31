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
        Schema::create('apps', function (Blueprint $table) {
            $table->id();
            $table->string('package_name')->unique();
            $table->string('app_name');
            $table->string('icon_url')->nullable();
            $table->text('fcm_server_key')->nullable();
            $table->boolean('is_active')->default(true);
            $table->timestamps();
            
            $table->index('package_name');
            $table->index('is_active');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('apps');
    }
};
