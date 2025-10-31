@extends('layouts.app')

@section('content')
<div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
    <h1 class="h2"><i class="bi bi-bell"></i> Push Notifications</h1>
    <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#sendNotificationModal">
        <i class="bi bi-send"></i> Send Notification
    </button>
</div>

@if(session('success'))
<div class="alert alert-success alert-dismissible fade show">
    <i class="bi bi-check-circle"></i> {{ session('success') }}
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
@endif

@if(session('error'))
<div class="alert alert-danger alert-dismissible fade show">
    <i class="bi bi-exclamation-triangle"></i> {{ session('error') }}
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
@endif

<div class="row mb-3">
    <div class="col-md-12">
        @if($firebaseStatus['connected'])
        <div class="alert alert-success">
            <i class="bi bi-check-circle-fill"></i> <strong>Firebase Connected:</strong> {{ $firebaseStatus['message'] }}
        </div>
        @else
        <div class="alert alert-danger">
            <i class="bi bi-exclamation-triangle-fill"></i> <strong>Firebase Error:</strong> {{ $firebaseStatus['message'] }}
            <br><small class="mt-2 d-block">Please check <code>FIREBASE_CREDENTIALS</code> in your .env file and ensure the service account JSON file exists. See <strong>FIREBASE_SETUP.md</strong> for details.</small>
        </div>
        @endif
    </div>
</div>

<div class="row mb-4">
    <div class="col-md-12">
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">Notification History</h5>
            </div>
            <div class="card-body">
                <table class="table table-hover" id="notificationsTable">
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Message</th>
                            <th>Target App</th>
                            <th>Country</th>
                            <th>Status</th>
                            <th>Sent At</th>
                            <th>Delivered</th>
                        </tr>
                    </thead>
                    <tbody>
                        @forelse($notifications as $notification)
                        <tr>
                            <td>{{ $notification->title }}</td>
                            <td>{{ Str::limit($notification->body, 50) }}</td>
                            <td>{{ $notification->app ? $notification->app->app_name : 'All Apps' }}</td>
                            <td>{{ $notification->targeting_rules['countries'][0] ?? 'All' }}</td>
                            <td>
                                @if($notification->status == 'sent')
                                    <span class="badge bg-success">Sent</span>
                                @elseif($notification->status == 'pending')
                                    <span class="badge bg-warning">Pending</span>
                                @else
                                    <span class="badge bg-secondary">{{ ucfirst($notification->status) }}</span>
                                @endif
                            </td>
                            <td>{{ $notification->sent_at ? $notification->sent_at->format('Y-m-d H:i') : 'Not sent' }}</td>
                            <td>
                                @if($notification->sent_count > 0)
                                    {{ $notification->delivered_count }} / {{ $notification->sent_count }}
                                    <small class="text-muted">({{ round(($notification->delivered_count / $notification->sent_count) * 100) }}%)</small>
                                @else
                                    -
                                @endif
                            </td>
                        </tr>
                        @empty
                        <tr>
                            <td colspan="7" class="text-center text-muted">No notifications sent yet</td>
                        </tr>
                        @endforelse
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="sendNotificationModal">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <form method="POST" action="{{ route('notifications.store') }}">
                @csrf
                <div class="modal-header">
                    <h5 class="modal-title">Send Push Notification</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Title</label>
                        <input type="text" name="title" class="form-control" placeholder="Notification title" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Message</label>
                        <textarea name="message" class="form-control" rows="3" placeholder="Notification message" required></textarea>
                    </div>
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Target App</label>
                                <select name="target_app" class="form-select">
                                    <option value="">All Apps</option>
                                    @if(isset($apps))
                                        @foreach($apps ?? [] as $app)
                                        <option value="{{ $app->id }}">{{ $app->app_name }}</option>
                                        @endforeach
                                    @endif
                                </select>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label class="form-label">Target Country</label>
                                <input type="text" name="target_country" class="form-control" placeholder="e.g., US, UK, ALL">
                            </div>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Target Version (Optional)</label>
                        <input type="text" name="target_version" class="form-control" placeholder="e.g., 1.0.0">
                    </div>
                    <div class="alert alert-info">
                        <i class="bi bi-info-circle"></i> This notification will be sent via Firebase Cloud Messaging (FCM) to all matching devices.
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-send"></i> Send Now
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
$(document).ready(function() {
    $('#notificationsTable').DataTable({
        order: [[5, 'desc']]
    });
});
</script>
@endsection
