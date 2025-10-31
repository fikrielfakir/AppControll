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
        Schema::create('admob_ad_units', function (Blueprint $table) {
            $table->id();
            $table->foreignId('admob_app_id')->constrained('admob_apps')->onDelete('cascade');
            $table->foreignId('admob_account_id')->constrained('admob_accounts')->onDelete('cascade');
            $table->string('ad_unit_id');
            $table->string('ad_type');
            $table->boolean('is_active')->default(true);
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('admob_ad_units');
    }
};
