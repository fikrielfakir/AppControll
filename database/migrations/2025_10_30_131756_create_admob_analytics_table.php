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
        Schema::create('admob_analytics', function (Blueprint $table) {
            $table->id();
            $table->string('package_name');
            $table->string('account_id')->nullable();
            $table->string('event');
            $table->string('ad_type')->nullable();
            $table->integer('value')->default(0);
            $table->json('metadata')->nullable();
            $table->timestamps();
            
            $table->index(['package_name', 'event']);
            $table->index(['account_id', 'created_at']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('admob_analytics');
    }
};
