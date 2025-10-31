@extends('layouts.app')

@section('content')
<div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
    <h1 class="h2"><i class="bi bi-tablet"></i> Devices & Users</h1>
</div>

<div class="row mb-4">
    <div class="col-md-3">
        <div class="card stat-card border-success">
            <div class="card-body">
                <p class="text-muted mb-1">Total Devices</p>
                <h2 class="mb-0">{{ $devices->total() }}</h2>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card stat-card border-primary">
            <div class="card-body">
                <p class="text-muted mb-1">Active (Last 7 Days)</p>
                <h2 class="mb-0">{{ $activeDevices }}</h2>
            </div>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h5 class="mb-0">Filter Devices</h5>
    </div>
    <div class="card-body">
        <form method="GET" action="{{ route('devices.index') }}" class="row g-3">
            <div class="col-md-3">
                <label class="form-label">App</label>
                <select name="app_id" class="form-select">
                    <option value="">All Apps</option>
                    @foreach($apps as $app)
                    <option value="{{ $app->id }}" {{ request('app_id') == $app->id ? 'selected' : '' }}>
                        {{ $app->app_name }}
                    </option>
                    @endforeach
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label">Country</label>
                <select name="country" class="form-select">
                    <option value="">All Countries</option>
                    @foreach($countries as $country)
                    <option value="{{ $country }}" {{ request('country') == $country ? 'selected' : '' }}>
                        {{ $country }}
                    </option>
                    @endforeach
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label">Active From</label>
                <input type="date" name="date_from" class="form-control" value="{{ request('date_from') }}">
            </div>
            <div class="col-md-3">
                <label class="form-label">&nbsp;</label>
                <button type="submit" class="btn btn-primary w-100">
                    <i class="bi bi-funnel"></i> Filter
                </button>
            </div>
        </form>
    </div>
</div>

<div class="card mt-4">
    <div class="card-header">
        <h5 class="mb-0">Device List</h5>
    </div>
    <div class="card-body">
        <table class="table table-hover" id="devicesTable">
            <thead>
                <tr>
                    <th>Device ID</th>
                    <th>App</th>
                    <th>Country</th>
                    <th>Version</th>
                    <th>Last Active</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                @foreach($devices as $device)
                <tr>
                    <td><code>{{ Str::limit($device->device_id, 20) }}</code></td>
                    <td>{{ $device->app ? $device->app->app_name : 'N/A' }}</td>
                    <td>
                        <span class="badge bg-secondary">{{ $device->country ?? 'Unknown' }}</span>
                    </td>
                    <td>{{ $device->version ?? 'N/A' }}</td>
                    <td>{{ $device->last_active_at ? $device->last_active_at->diffForHumans() : 'Never' }}</td>
                    <td>
                        @if($device->last_active_at && $device->last_active_at->isAfter(now()->subDays(7)))
                            <span class="badge bg-success">Active</span>
                        @else
                            <span class="badge bg-warning">Inactive</span>
                        @endif
                    </td>
                </tr>
                @endforeach
            </tbody>
        </table>
        
        <div class="mt-3">
            {{ $devices->links() }}
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
$(document).ready(function() {
    $('#devicesTable').DataTable({
        paging: false,
        searching: true,
        order: [[4, 'desc']]
    });
});
</script>
@endsection
