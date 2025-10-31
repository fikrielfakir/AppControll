<?php

namespace App\Services;

use App\Models\NotificationEvent;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class NotificationService
{
    protected NotificationTargetingService $targetingService;

    public function __construct(NotificationTargetingService $targetingService)
    {
        $this->targetingService = $targetingService;
    }

    public function sendNotification(NotificationEvent $notification): array
    {
        $devices = $this->targetingService->getTargetedDevices($notification);

        if ($devices->isEmpty()) {
            return [
                'success' => false,
                'message' => 'No devices match targeting criteria',
                'sent_count' => 0,
            ];
        }

        $fcmServerKey = $notification->app->fcm_server_key;
        if (!$fcmServerKey) {
            return [
                'success' => false,
                'message' => 'FCM server key not configured for this app',
                'sent_count' => 0,
            ];
        }

        $sentCount = 0;
        $deliveredCount = 0;

        $tokens = $devices->pluck('fcm_token')->filter()->chunk(1000);

        foreach ($tokens as $tokenChunk) {
            $result = $this->sendBatchNotification(
                $fcmServerKey,
                $tokenChunk->toArray(),
                $notification->title,
                $notification->body
            );

            $sentCount += $result['sent'];
            $deliveredCount += $result['delivered'];
        }

        $notification->update([
            'status' => 'sent',
            'sent_count' => $sentCount,
            'delivered_count' => $deliveredCount,
            'sent_at' => now(),
        ]);

        return [
            'success' => true,
            'message' => 'Notification sent successfully',
            'sent_count' => $sentCount,
            'delivered_count' => $deliveredCount,
        ];
    }

    protected function sendBatchNotification(string $fcmServerKey, array $tokens, string $title, string $body): array
    {
        try {
            $response = Http::withHeaders([
                'Authorization' => 'key=' . $fcmServerKey,
                'Content-Type' => 'application/json',
            ])->post('https://fcm.googleapis.com/fcm/send', [
                'registration_ids' => $tokens,
                'notification' => [
                    'title' => $title,
                    'body' => $body,
                    'sound' => 'default',
                ],
                'priority' => 'high',
            ]);

            if ($response->successful()) {
                $data = $response->json();
                return [
                    'sent' => count($tokens),
                    'delivered' => $data['success'] ?? 0,
                ];
            }

            Log::error('FCM notification failed', [
                'status' => $response->status(),
                'body' => $response->body(),
            ]);

            return [
                'sent' => count($tokens),
                'delivered' => 0,
            ];
        } catch (\Exception $e) {
            Log::error('FCM notification exception', [
                'message' => $e->getMessage(),
            ]);

            return [
                'sent' => count($tokens),
                'delivered' => 0,
            ];
        }
    }
}
