<?php

namespace App\Services;

use Kreait\Firebase\Factory;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification as FCMNotification;
use Kreait\Firebase\Exception\MessagingException;
use Illuminate\Support\Facades\Log;

class FirebaseService
{
    protected $messaging;

    public function __construct()
    {
        $credentialsPath = env('FIREBASE_CREDENTIALS');
        
        if (!$credentialsPath || !file_exists($credentialsPath)) {
            throw new \Exception('Firebase credentials not found. Please set FIREBASE_CREDENTIALS in .env');
        }
        
        $factory = (new Factory)->withServiceAccount($credentialsPath);
        $this->messaging = $factory->createMessaging();
    }

    public function sendNotification(string $fcmToken, string $title, string $body, array $data = [])
    {
        try {
            $notification = FCMNotification::create($title, $body);

            $message = CloudMessage::withTarget('token', $fcmToken)
                ->withNotification($notification)
                ->withData($data);

            $this->messaging->send($message);

            return ['success' => true];
        } catch (MessagingException $e) {
            Log::error('Firebase notification failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }

    public function sendMulticastNotification(array $fcmTokens, string $title, string $body, array $data = [])
    {
        try {
            $notification = FCMNotification::create($title, $body);

            $message = CloudMessage::new()
                ->withNotification($notification)
                ->withData($data);

            $report = $this->messaging->sendMulticast($message, $fcmTokens);

            return [
                'success' => true,
                'successful' => $report->successes()->count(),
                'failed' => $report->failures()->count(),
            ];
        } catch (MessagingException $e) {
            Log::error('Firebase multicast notification failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }

    public function sendToTopic(string $topic, string $title, string $body, array $data = [])
    {
        try {
            $notification = FCMNotification::create($title, $body);

            $message = CloudMessage::withTarget('topic', $topic)
                ->withNotification($notification)
                ->withData($data);

            $this->messaging->send($message);

            return ['success' => true];
        } catch (MessagingException $e) {
            Log::error('Firebase topic notification failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }

    public function subscribeToTopic(string $fcmToken, string $topic)
    {
        try {
            $this->messaging->subscribeToTopic($topic, $fcmToken);
            return ['success' => true];
        } catch (MessagingException $e) {
            Log::error('Firebase topic subscription failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }
}
