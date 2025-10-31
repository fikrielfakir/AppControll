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
        Schema::table('admob_accounts', function (Blueprint $table) {
            $table->foreignId('app_id')->nullable()->change();
            $table->string('admob_account_id')->nullable()->change();
            $table->string('app_name')->nullable()->change();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('admob_accounts', function (Blueprint $table) {
            $table->foreignId('app_id')->nullable(false)->change();
            $table->string('admob_account_id')->nullable(false)->change();
            $table->string('app_name')->nullable(false)->change();
        });
    }
};
