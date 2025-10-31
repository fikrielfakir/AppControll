<?php

namespace Database\Seeders;

// use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     */
    public function run(): void
    {
        \App\Models\User::create([
            'name' => 'Admin User',
            'username' => 'admin',
            'email' => 'admin@example.com',
            'password' => bcrypt('admin123'),
            'role' => 'admin',
        ]);

        $app = \App\Models\App::create([
            'package_name' => 'com.example.testapp',
            'app_name' => 'Test Application',
            'icon_url' => 'https://via.placeholder.com/512',
            'fcm_server_key' => 'dummy_fcm_key_for_testing',
            'is_active' => true,
        ]);

        \App\Models\AdMobAccount::create([
            'app_id' => $app->id,
            'admob_account_id' => 'ca-app-pub-1234567890',
            'app_name' => 'Test AdMob Account',
            'switching_strategy' => 'random',
            'weight' => 1,
            'is_active' => true,
        ]);
    }
}
