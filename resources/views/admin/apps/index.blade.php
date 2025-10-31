@extends('layouts.app')

@section('content')
<div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
    <h1 class="h2"><i class="bi bi-phone"></i> Apps</h1>
    <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addModal">
        <i class="bi bi-plus-circle"></i> Add App
    </button>
</div>
<table class="table" id="appsTable">
    <thead>
        <tr>
            <th>Package Name</th>
            <th>App Name</th>
            <th>Status</th>
            <th>Created</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
        @foreach($apps as $app)
        <tr>
            <td>{{ $app->package_name }}</td>
            <td>{{ $app->app_name }}</td>
            <td><span class="badge bg-{{ $app->is_active ? 'success' : 'danger' }}">{{ $app->is_active ? 'Active' : 'Inactive' }}</span></td>
            <td>{{ $app->created_at->format('Y-m-d') }}</td>
            <td>
                <button class="btn btn-sm btn-warning" data-bs-toggle="modal" data-bs-target="#editModal{{ $app->id }}">
                    <i class="bi bi-pencil"></i> Edit
                </button>
                <form method="POST" action="{{ route('apps.destroy', $app->id) }}" class="d-inline">
                    @csrf
                    @method('DELETE')
                    <button class="btn btn-sm btn-danger" onclick="return confirm('Are you sure?')">
                        <i class="bi bi-trash"></i> Delete
                    </button>
                </form>
            </td>
        </tr>
        @endforeach
    </tbody>
</table>

@foreach($apps as $app)
<div class="modal fade" id="editModal{{ $app->id }}">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="POST" action="{{ route('apps.update', $app->id) }}">
                @csrf
                @method('PUT')
                <div class="modal-header">
                    <h5 class="modal-title">Edit App</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Package Name</label>
                        <input type="text" name="package_name" class="form-control" value="{{ $app->package_name }}" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">App Name</label>
                        <input type="text" name="app_name" class="form-control" value="{{ $app->app_name }}" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Icon URL</label>
                        <input type="url" name="icon_url" class="form-control" value="{{ $app->icon_url }}">
                    </div>
                    <div class="mb-3">
                        <label class="form-label">FCM Server Key</label>
                        <textarea name="fcm_server_key" class="form-control" rows="3">{{ $app->fcm_server_key }}</textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="submit" class="btn btn-primary">Update App</button>
                </div>
            </form>
        </div>
    </div>
</div>
@endforeach

<div class="modal fade" id="addModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="POST" action="{{ route('apps.store') }}">
                @csrf
                <div class="modal-header">
                    <h5 class="modal-title">Add App</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Package Name</label>
                        <input type="text" name="package_name" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">App Name</label>
                        <input type="text" name="app_name" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Icon URL</label>
                        <input type="url" name="icon_url" class="form-control">
                    </div>
                    <div class="mb-3">
                        <label class="form-label">FCM Server Key</label>
                        <textarea name="fcm_server_key" class="form-control" rows="3"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="submit" class="btn btn-primary">Create App</button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
$(document).ready(function() {
    $('#appsTable').DataTable();
});
</script>
@endsection
