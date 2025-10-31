<?php

namespace App\Services;

use App\Models\NotificationEvent;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\Log;

class NotificationService
{
    protected NotificationTargetingService $targetingService;
    protected ?FirebaseService $firebaseService = null;

    public function __construct(NotificationTargetingService $targetingService)
    {
        $this->targetingService = $targetingService;
        
        try {
            $this->firebaseService = new FirebaseService();
        } catch (\Exception $e) {
            Log::error('Failed to initialize Firebase service: ' . $e->getMessage());
            $this->firebaseService = null;
        }
    }

    public function sendNotification(NotificationEvent $notification): array
    {
        if (!$this->firebaseService) {
            $notification->update([
                'status' => 'failed',
                'sent_count' => 0,
                'delivered_count' => 0,
            ]);
            
            return [
                'success' => false,
                'message' => 'Firebase service not available. Please check Firebase credentials configuration.',
                'sent_count' => 0,
            ];
        }

        $devices = $this->targetingService->getTargetedDevices($notification);

        if ($devices->isEmpty()) {
            return [
                'success' => false,
                'message' => 'No devices match targeting criteria',
                'sent_count' => 0,
            ];
        }

        $sentCount = 0;
        $deliveredCount = 0;

        $tokens = $devices->pluck('fcm_token')->filter()->chunk(500);

        $additionalData = [
            'notification_id' => (string) $notification->id,
            'type' => $notification->type ?? 'popup',
        ];

        foreach ($tokens as $tokenChunk) {
            $result = $this->sendBatchNotification(
                $tokenChunk->toArray(),
                $notification->title,
                $notification->body,
                $additionalData
            );

            $sentCount += count($tokenChunk);
            $deliveredCount += $result['successful'] ?? 0;
        }

        if ($deliveredCount === 0 && $sentCount > 0) {
            $notification->update([
                'status' => 'failed',
                'sent_count' => $sentCount,
                'delivered_count' => 0,
            ]);

            return [
                'success' => false,
                'message' => 'All notifications failed to deliver via Firebase',
                'sent_count' => $sentCount,
                'delivered_count' => 0,
            ];
        }

        $notification->update([
            'status' => 'sent',
            'sent_count' => $sentCount,
            'delivered_count' => $deliveredCount,
            'sent_at' => now(),
        ]);

        return [
            'success' => true,
            'message' => 'Notification sent successfully via Firebase',
            'sent_count' => $sentCount,
            'delivered_count' => $deliveredCount,
        ];
    }

    protected function sendBatchNotification(array $tokens, string $title, string $body, array $data = []): array
    {
        try {
            $result = $this->firebaseService->sendMulticastNotification(
                $tokens,
                $title,
                $body,
                $data
            );

            if ($result['success']) {
                Log::info('Firebase notification sent', [
                    'sent' => count($tokens),
                    'successful' => $result['successful'] ?? 0,
                    'failed' => $result['failed'] ?? 0,
                ]);

                return [
                    'sent' => count($tokens),
                    'successful' => $result['successful'] ?? 0,
                    'failed' => $result['failed'] ?? 0,
                ];
            }

            Log::error('Firebase notification failed', [
                'error' => $result['error'] ?? 'Unknown error',
            ]);

            return [
                'sent' => count($tokens),
                'successful' => 0,
                'failed' => count($tokens),
            ];
        } catch (\Exception $e) {
            Log::error('Firebase notification exception', [
                'message' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return [
                'sent' => count($tokens),
                'successful' => 0,
                'failed' => count($tokens),
            ];
        }
    }
}
