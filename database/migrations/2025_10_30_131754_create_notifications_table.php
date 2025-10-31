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
        Schema::create('notifications', function (Blueprint $table) {
            $table->id();
            $table->string('notification_id')->unique();
            $table->string('package_name')->nullable();
            $table->string('title');
            $table->text('message');
            $table->string('type')->default('popup');
            $table->string('priority')->default('normal');
            $table->string('image_url')->nullable();
            $table->string('action_button_text')->nullable();
            $table->string('action_type')->nullable();
            $table->text('action_value')->nullable();
            $table->boolean('cancelable')->default(true);
            $table->integer('max_displays')->default(1);
            $table->integer('display_interval_hours')->default(24);
            $table->boolean('show_on_app_launch')->default(false);
            $table->timestamp('scheduled_at')->nullable();
            $table->timestamp('expires_at')->nullable();
            $table->string('status')->default('pending');
            $table->json('targeting')->nullable();
            $table->timestamps();
            
            $table->index(['package_name', 'status']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('notifications');
    }
};
