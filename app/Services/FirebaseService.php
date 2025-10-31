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
    protected $isConfigured = false;

    public function __construct()
    {
        $credentialsPath = env('FIREBASE_CREDENTIALS');
        
        if ($credentialsPath && file_exists($credentialsPath)) {
            try {
                $factory = (new Factory)->withServiceAccount($credentialsPath);
                $this->messaging = $factory->createMessaging();
                $this->isConfigured = true;
            } catch (\Exception $e) {
                Log::warning('Firebase initialization failed: ' . $e->getMessage());
                $this->isConfigured = false;
            }
        } else {
            Log::warning('Firebase credentials not configured. Push notifications will not be available.');
            $this->isConfigured = false;
        }
    }
    
    protected function checkConfiguration()
    {
        if (!$this->isConfigured) {
            throw new \Exception('Firebase is not configured. Please set FIREBASE_CREDENTIALS in .env and ensure the service account JSON file exists.');
        }
    }

    public function sendNotification(string $fcmToken, string $title, string $body, array $data = [])
    {
        try {
            $this->checkConfiguration();
            
            $notification = FCMNotification::create($title, $body);

            $message = CloudMessage::withTarget('token', $fcmToken)
                ->withNotification($notification)
                ->withData($data);

            $this->messaging->send($message);

            return ['success' => true];
        } catch (\Exception $e) {
            Log::error('Firebase notification failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }

    public function sendMulticastNotification(array $fcmTokens, string $title, string $body, array $data = [])
    {
        try {
            $this->checkConfiguration();
            
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
        } catch (\Exception $e) {
            Log::error('Firebase multicast notification failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }

    public function sendToTopic(string $topic, string $title, string $body, array $data = [])
    {
        try {
            $this->checkConfiguration();
            
            $notification = FCMNotification::create($title, $body);

            $message = CloudMessage::withTarget('topic', $topic)
                ->withNotification($notification)
                ->withData($data);

            $this->messaging->send($message);

            return ['success' => true];
        } catch (\Exception $e) {
            Log::error('Firebase topic notification failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }

    public function subscribeToTopic(string $fcmToken, string $topic)
    {
        try {
            $this->checkConfiguration();
            
            $this->messaging->subscribeToTopic($topic, $fcmToken);
            return ['success' => true];
        } catch (\Exception $e) {
            Log::error('Firebase topic subscription failed: ' . $e->getMessage());
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }
}
