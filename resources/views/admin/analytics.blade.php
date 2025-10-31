@extends('layouts.app')

@section('content')
<div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
    <h1 class="h2"><i class="bi bi-bar-chart-line"></i> Analytics</h1>
    <form method="GET" action="{{ route('analytics.index') }}" class="d-flex">
        <select name="app_id" class="form-select me-2" onchange="this.form.submit()">
            <option value="">All Apps</option>
            @foreach($apps as $app)
                <option value="{{ $app->id }}" {{ request('app_id') == $app->id ? 'selected' : '' }}>
                    {{ $app->app_name }}
                </option>
            @endforeach
        </select>
    </form>
</div>

<div class="row mb-4">
    <div class="col-md-3 mb-3">
        <div class="card stat-card border-primary">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <p class="text-muted mb-1">Total Devices</p>
                        <h2 class="mb-0">{{ $stats['total_devices'] ?? 0 }}</h2>
                    </div>
                    <div class="text-primary">
                        <i class="bi bi-phone" style="font-size: 2.5rem;"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <div class="col-md-3 mb-3">
        <div class="card stat-card border-success">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <p class="text-muted mb-1">Active Today</p>
                        <h2 class="mb-0">{{ $stats['active_today'] ?? 0 }}</h2>
                    </div>
                    <div class="text-success">
                        <i class="bi bi-check-circle" style="font-size: 2.5rem;"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <div class="col-md-3 mb-3">
        <div class="card stat-card border-warning">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <p class="text-muted mb-1">Total Events</p>
                        <h2 class="mb-0">{{ $stats['total_events'] ?? 0 }}</h2>
                    </div>
                    <div class="text-warning">
                        <i class="bi bi-lightning" style="font-size: 2.5rem;"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <div class="col-md-3 mb-3">
        <div class="card stat-card border-info">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <p class="text-muted mb-1">Ad Revenue</p>
                        <h2 class="mb-0">${{ number_format($stats['total_revenue'] ?? 0, 2) }}</h2>
                    </div>
                    <div class="text-info">
                        <i class="bi bi-currency-dollar" style="font-size: 2.5rem;"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row mb-4">
    <div class="col-md-8">
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-graph-up"></i> Device Registrations (Last 30 Days)</h5>
            </div>
            <div class="card-body">
                <canvas id="deviceChart" height="80"></canvas>
            </div>
        </div>
    </div>
    
    <div class="col-md-4">
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-pie-chart"></i> Event Types</h5>
            </div>
            <div class="card-body">
                <canvas id="eventChart" height="200"></canvas>
            </div>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-md-12">
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-globe"></i> Geographic Distribution</h5>
            </div>
            <div class="card-body">
                <canvas id="countryChart" height="60"></canvas>
            </div>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
const deviceCtx = document.getElementById('deviceChart').getContext('2d');
new Chart(deviceCtx, {
    type: 'line',
    data: {
        labels: @json($deviceChart['labels'] ?? []),
        datasets: [{
            label: 'New Devices',
            data: @json($deviceChart['data'] ?? []),
            borderColor: '#0d6efd',
            backgroundColor: 'rgba(13, 110, 253, 0.1)',
            tension: 0.4,
            fill: true
        }]
    },
    options: {
        responsive: true,
        plugins: {
            legend: {
                display: false
            }
        },
        scales: {
            y: {
                beginAtZero: true
            }
        }
    }
});

const eventCtx = document.getElementById('eventChart').getContext('2d');
new Chart(eventCtx, {
    type: 'doughnut',
    data: {
        labels: @json($eventChart['labels'] ?? []),
        datasets: [{
            data: @json($eventChart['data'] ?? []),
            backgroundColor: [
                'rgba(13, 110, 253, 0.7)',
                'rgba(25, 135, 84, 0.7)',
                'rgba(255, 193, 7, 0.7)',
                'rgba(220, 53, 69, 0.7)',
                'rgba(13, 202, 240, 0.7)'
            ],
            borderColor: [
                'rgb(13, 110, 253)',
                'rgb(25, 135, 84)',
                'rgb(255, 193, 7)',
                'rgb(220, 53, 69)',
                'rgb(13, 202, 240)'
            ],
            borderWidth: 2
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false
    }
});

const countryCtx = document.getElementById('countryChart').getContext('2d');
new Chart(countryCtx, {
    type: 'bar',
    data: {
        labels: @json($countryChart['labels'] ?? []),
        datasets: [{
            label: 'Devices',
            data: @json($countryChart['data'] ?? []),
            backgroundColor: 'rgba(13, 110, 253, 0.7)',
            borderColor: 'rgb(13, 110, 253)',
            borderWidth: 2
        }]
    },
    options: {
        responsive: true,
        plugins: {
            legend: {
                display: false
            }
        },
        scales: {
            y: {
                beginAtZero: true
            }
        }
    }
});
</script>
@endsection
