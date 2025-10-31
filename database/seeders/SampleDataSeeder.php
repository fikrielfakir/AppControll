<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class SampleDataSeeder extends Seeder
{
    public function run(): void
    {
        $appId = DB::table('apps')->insertGetId([
            'name' => 'Test Application',
            'package_name' => 'com.moho.wood',
            'icon_url' => null,
            'fcm_server_key' => null,
            'is_active' => true,
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        $admobAccountId = DB::table('admob_accounts')->insertGetId([
            'app_id' => $appId,
            'admob_account_id' => 'pub-1234567890123456',
            'app_name' => 'Test App AdMob',
            'switching_strategy' => 'random',
            'strategy_config' => null,
            'weight' => 1,
            'usage_count' => 0,
            'is_active' => true,
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        DB::table('admob_apps')->insert([
            'package_name' => 'com.moho.wood',
            'app_name' => 'Test Application',
            'platform' => 'android',
            'default_admob_account_id' => null,
            'is_active' => true,
            'config' => json_encode([
                'gdpr_enabled' => true,
                'under_age' => false,
            ]),
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }
}
