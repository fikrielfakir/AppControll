@extends('layouts.app')

@section('content')
<div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
    <h1 class="h2"><i class="bi bi-currency-dollar"></i> AdMob Accounts</h1>
    <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addAccountModal">
        <i class="bi bi-plus-circle"></i> Add AdMob Account
    </button>
</div>

<div class="row mb-4">
    <div class="col-md-12">
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">AdMob Accounts</h5>
            </div>
            <div class="card-body">
                <table class="table table-hover" id="accountsTable">
                    <thead>
                        <tr>
                            <th>Account Name</th>
                            <th>Publisher ID</th>
                            <th>Status</th>
                            <th>Ad Units</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($accounts as $account)
                        <tr>
                            <td><strong>{{ $account->account_name }}</strong></td>
                            <td><code>{{ $account->publisher_id }}</code></td>
                            <td>
                                <span class="badge bg-{{ $account->status == 'active' ? 'success' : 'secondary' }}">
                                    {{ ucfirst($account->status) }}
                                </span>
                            </td>
                            <td>{{ $account->ad_units_count ?? 0 }}</td>
                            <td>
                                <button class="btn btn-sm btn-info" data-bs-toggle="modal" data-bs-target="#assignModal{{ $account->id }}">
                                    <i class="bi bi-link"></i> Assign
                                </button>
                                <button class="btn btn-sm btn-warning" data-bs-toggle="modal" data-bs-target="#editModal{{ $account->id }}">
                                    <i class="bi bi-pencil"></i> Edit
                                </button>
                                <form method="POST" action="{{ route('admob.destroy', $account->id) }}" class="d-inline">
                                    @csrf
                                    @method('DELETE')
                                    <button class="btn btn-sm btn-danger" onclick="return confirm('Are you sure?')">
                                        <i class="bi bi-trash"></i> Delete
                                    </button>
                                </form>
                            </td>
                        </tr>

                        <div class="modal fade" id="editModal{{ $account->id }}">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <form method="POST" action="{{ route('admob.update', $account->id) }}">
                                        @csrf
                                        @method('PUT')
                                        <div class="modal-header">
                                            <h5 class="modal-title">Edit AdMob Account</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                        </div>
                                        <div class="modal-body">
                                            <div class="mb-3">
                                                <label class="form-label">Account Name</label>
                                                <input type="text" name="account_name" class="form-control" value="{{ $account->account_name }}" required>
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">Publisher ID</label>
                                                <input type="text" name="publisher_id" class="form-control" value="{{ $account->publisher_id }}" required>
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">Status</label>
                                                <select name="status" class="form-select" required>
                                                    <option value="active" {{ $account->status == 'active' ? 'selected' : '' }}>Active</option>
                                                    <option value="inactive" {{ $account->status == 'inactive' ? 'selected' : '' }}>Inactive</option>
                                                </select>
                                            </div>
                                        </div>
                                        <div class="modal-footer">
                                            <button type="submit" class="btn btn-primary">Update Account</button>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>

                        <div class="modal fade" id="assignModal{{ $account->id }}">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <form method="POST" action="{{ route('admob.assign', [$account->id, '']) }}" id="assignForm{{ $account->id }}">
                                        @csrf
                                        <div class="modal-header">
                                            <h5 class="modal-title">Assign to App</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                        </div>
                                        <div class="modal-body">
                                            <div class="mb-3">
                                                <label class="form-label">Select App</label>
                                                <select name="app_id" class="form-select" required onchange="document.getElementById('assignForm{{ $account->id }}').action = '{{ route('admob.assign', [$account->id, '']) }}' + '/' + this.value">
                                                    <option value="">Choose app...</option>
                                                    @foreach($apps as $app)
                                                    <option value="{{ $app->id }}">{{ $app->app_name }} ({{ $app->package_name }})</option>
                                                    @endforeach
                                                </select>
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">AdMob App Account ID</label>
                                                <input type="text" name="account_id" class="form-control" placeholder="ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx">
                                                <small class="text-muted">App-level AdMob ID (with ~)</small>
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">Banner Ad Unit ID</label>
                                                <input type="text" name="banner_id" class="form-control" placeholder="ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx">
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">Interstitial Ad Unit ID</label>
                                                <input type="text" name="interstitial_id" class="form-control" placeholder="ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx">
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">Rewarded Ad Unit ID</label>
                                                <input type="text" name="rewarded_id" class="form-control" placeholder="ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx">
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">App Open Ad Unit ID</label>
                                                <input type="text" name="app_open_id" class="form-control" placeholder="ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx">
                                            </div>
                                            <div class="mb-3">
                                                <label class="form-label">Native Ad Unit ID</label>
                                                <input type="text" name="native_id" class="form-control" placeholder="ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx">
                                            </div>
                                        </div>
                                        <div class="modal-footer">
                                            <button type="submit" class="btn btn-primary">Assign Ad Units</button>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="addAccountModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="POST" action="{{ route('admob.store') }}">
                @csrf
                <div class="modal-header">
                    <h5 class="modal-title">Add AdMob Account</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Account Name</label>
                        <input type="text" name="account_name" class="form-control" placeholder="My AdMob Account" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Publisher ID</label>
                        <input type="text" name="publisher_id" class="form-control" placeholder="pub-xxxxxxxxxxxxxxxx" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Status</label>
                        <select name="status" class="form-select" required>
                            <option value="active">Active</option>
                            <option value="inactive">Inactive</option>
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="submit" class="btn btn-primary">Create Account</button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
$(document).ready(function() {
    $('#accountsTable').DataTable({
        order: [[0, 'asc']]
    });
});
</script>
@endsection
