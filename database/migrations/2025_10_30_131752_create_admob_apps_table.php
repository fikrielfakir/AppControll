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
        Schema::create('admob_apps', function (Blueprint $table) {
            $table->id();
            $table->string('package_name')->unique();
            $table->string('app_name');
            $table->string('platform')->default('android');
            $table->foreignId('default_admob_account_id')->nullable()->constrained('admob_accounts')->onDelete('set null');
            $table->boolean('is_active')->default(true);
            $table->json('config')->nullable();
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('admob_apps');
    }
};
