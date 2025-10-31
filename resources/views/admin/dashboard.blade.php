@extends('layouts.app')

@section('content')
<div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
    <h1 class="h2"><i class="bi bi-speedometer2"></i> Dashboard</h1>
</div>

<div class="row mb-4">
    <div class="col-md-3 mb-3">
        <div class="card stat-card border-success">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <p class="text-muted mb-1">Total Apps</p>
                        <h2 class="mb-0">{{ $stats['total_apps'] ?? 0 }}</h2>
                    </div>
                    <div class="text-success">
                        <i class="bi bi-phone" style="font-size: 2.5rem;"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <div class="col-md-3 mb-3">
        <div class="card stat-card border-primary">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <p class="text-muted mb-1">Active Users</p>
                        <h2 class="mb-0">{{ $stats['active_devices'] ?? 0 }}</h2>
                    </div>
                    <div class="text-primary">
                        <i class="bi bi-people" style="font-size: 2.5rem;"></i>
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
                        <p class="text-muted mb-1">Notifications Sent</p>
                        <h2 class="mb-0">{{ $stats['total_notifications'] ?? 0 }}</h2>
                    </div>
                    <div class="text-warning">
                        <i class="bi bi-bell" style="font-size: 2.5rem;"></i>
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
                        <p class="text-muted mb-1">Total Revenue</p>
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
                <h5 class="mb-0"><i class="bi bi-graph-up"></i> Active Users Over Time</h5>
            </div>
            <div class="card-body">
                <canvas id="usersChart" height="80"></canvas>
            </div>
        </div>
    </div>
    
    <div class="col-md-4">
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-list-ul"></i> Active Apps</h5>
            </div>
            <div class="card-body" style="max-height: 300px; overflow-y: auto;">
                @if($apps->count() > 0)
                    <ul class="list-group list-group-flush">
                        @foreach($apps as $app)
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <strong>{{ $app->app_name }}</strong>
                                <br><small class="text-muted">{{ $app->package_name }}</small>
                            </div>
                            <span class="badge bg-success">Active</span>
                        </li>
                        @endforeach
                    </ul>
                @else
                    <p class="text-muted">No active apps</p>
                @endif
            </div>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-md-12">
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-bar-chart"></i> Revenue by App</h5>
            </div>
            <div class="card-body">
                <canvas id="revenueChart" height="60"></canvas>
            </div>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
const usersCtx = document.getElementById('usersChart').getContext('2d');
new Chart(usersCtx, {
    type: 'line',
    data: {
        labels: ['7 days ago', '6 days ago', '5 days ago', '4 days ago', '3 days ago', '2 days ago', 'Yesterday', 'Today'],
        datasets: [{
            label: 'Active Users',
            data: [120, 150, 180, 170, 200, 220, 250, {{ $stats['active_devices'] ?? 0 }}],
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

const revenueCtx = document.getElementById('revenueChart').getContext('2d');
new Chart(revenueCtx, {
    type: 'bar',
    data: {
        labels: @json($apps->pluck('app_name')),
        datasets: [{
            label: 'Revenue ($)',
            data: @json($apps->map(fn($app) => rand(100, 1000))),
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
