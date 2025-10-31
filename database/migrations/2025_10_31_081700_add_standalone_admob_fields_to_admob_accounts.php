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
            $table->string('account_name')->nullable();
            $table->string('publisher_id')->nullable();
            $table->string('status')->default('active');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('admob_accounts', function (Blueprint $table) {
            $table->dropColumn(['account_name', 'publisher_id', 'status']);
        });
    }
};
