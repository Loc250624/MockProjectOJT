'use strict';

document.addEventListener('DOMContentLoaded', function() {
    highlightCurrentAdminNav();
    initUserAdministration();
});

function highlightCurrentAdminNav() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a');
    sidebarLinks.forEach(function(link) {
        if (link.getAttribute('href') === currentPath) {
            link.style.background = 'rgba(233,69,96,0.2)';
            link.style.color = '#e94560';
        }
    });
}

function initUserAdministration() {
    var tableBody = document.getElementById('users-table-body');
    if (!tableBody) {
        return;
    }

    var state = {
        page: 0,
        size: 10,
        sortBy: 'createdAt',
        sortDir: 'desc',
        users: [],
        selectedUser: null,
        pendingStatusUserId: null,
        pendingDeleteUserId: null
    };

    var portal = document.querySelector('.lumina-portal');
    var currentAdminId = portal ? Number(portal.getAttribute('data-current-admin-id')) : NaN;

    var elements = {
        searchInput: document.getElementById('user-search-input'),
        roleFilter: document.getElementById('user-role-filter'),
        statusFilter: document.getElementById('user-status-filter'),
        providerFilter: document.getElementById('user-provider-filter'),
        tableBody: tableBody,
        loading: document.getElementById('users-loading'),
        feedback: document.getElementById('users-feedback'),
        emptyState: document.getElementById('users-empty-state'),
        error: document.getElementById('users-error'),
        pagination: document.getElementById('users-pagination'),
        summary: document.getElementById('users-result-summary'),
        panel: document.getElementById('user-panel'),
        panelName: document.getElementById('user-panel-name'),
        panelEmail: document.getElementById('user-panel-email'),
        panelRole: document.getElementById('user-panel-role'),
        panelStatus: document.getElementById('user-panel-status'),
        panelAvatar: document.getElementById('user-panel-avatar'),
        panelCreatedAt: document.getElementById('user-panel-created-at'),
        panelProvider: document.getElementById('user-panel-provider'),
        panelStatusAction: document.getElementById('user-panel-status-action'),
        panelDeleteAction: document.getElementById('user-panel-delete-action')
    };

    var debouncedLoadUsers = debounce(function() {
        state.page = 0;
        loadUsers();
    }, 400);

    elements.searchInput.addEventListener('input', debouncedLoadUsers);
    [elements.roleFilter, elements.statusFilter, elements.providerFilter].forEach(function(filter) {
        filter.addEventListener('change', function() {
            state.page = 0;
            loadUsers();
        });
    });

    elements.tableBody.addEventListener('click', function(event) {
        var action = event.target.closest('[data-user-id]');
        if (!action) {
            return;
        }
        var userId = Number(action.getAttribute('data-user-id'));
        var user = state.users.find(function(item) {
            return item.id === userId;
        });
        if (user) {
            openUserPanel(user);
        }
    });

    elements.panelStatusAction.addEventListener('click', function() {
        if (!state.selectedUser || state.pendingStatusUserId) {
            return;
        }
        changeSelectedUserStatus();
    });

    elements.panelDeleteAction.addEventListener('click', function() {
        if (!state.selectedUser || state.pendingDeleteUserId) {
            return;
        }
        softDeleteSelectedUser();
    });

    loadUsers();

    function loadUsers() {
        setLoading(true);
        setError('');
        setFeedback('', false);

        var params = new URLSearchParams();
        appendParam(params, 'keyword', elements.searchInput.value.trim());
        appendParam(params, 'role', elements.roleFilter.value);
        appendParam(params, 'status', elements.statusFilter.value);
        appendParam(params, 'authProvider', elements.providerFilter.value);
        params.set('page', state.page);
        params.set('size', state.size);
        params.set('sortBy', state.sortBy);
        params.set('sortDir', state.sortDir);

        fetch('/api/admin/users?' + params.toString(), {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Unable to load users. Status ' + response.status);
                }
                return response.json();
            })
            .then(function(apiResponse) {
                var page = apiResponse.data;
                state.users = page.content || [];
                renderUsers(state.users);
                renderSummary(page);
                renderPagination(page);
                elements.emptyState.style.display = state.users.length ? 'none' : 'block';
            })
            .catch(function(error) {
                state.users = [];
                renderUsers([]);
                renderSummary(null);
                renderPagination(null);
                elements.emptyState.style.display = 'none';
                setError(error.message || 'Unable to load users.');
            })
            .finally(function() {
                setLoading(false);
            });
    }

    function renderUsers(users) {
        elements.tableBody.innerHTML = users.map(function(user) {
            var initials = getInitials(user.fullName || user.email || String(user.id));
            var roleBadgeClass = user.role === 'TEACHER' ? 'badge badge-primary' : 'badge';
            var roleStyle = user.role === 'TEACHER' ? '' : ' style="background:var(--lumina-gray-100);color:var(--lumina-gray-700);"';
            return '<tr data-user-id="' + escapeHtml(user.id) + '" style="cursor:pointer;">' +
                '<td><input type="checkbox" style="accent-color:var(--lumina-blue);" disabled></td>' +
                '<td>' +
                    '<div style="display:flex;align-items:center;gap:0.75rem;">' +
                        '<div style="width:36px;height:36px;border-radius:50%;background:var(--lumina-blue-pale);color:var(--lumina-blue);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:0.875rem;">' + escapeHtml(initials) + '</div>' +
                        '<div>' +
                            '<div style="font-weight:600;color:var(--lumina-gray-900);">' + escapeHtml(user.fullName || 'Unnamed user') + '</div>' +
                            '<div style="font-size:0.75rem;color:var(--lumina-gray-500);">' + escapeHtml(user.email || '') + '</div>' +
                        '</div>' +
                    '</div>' +
                '</td>' +
                '<td><span class="' + roleBadgeClass + '"' + roleStyle + '>' + escapeHtml(user.role || '--') + '</span></td>' +
                '<td>' + statusBadge(user.status) + '</td>' +
                '<td style="font-size:0.8125rem;color:var(--lumina-gray-600);">' + escapeHtml(formatDate(user.createdAt)) + '</td>' +
                '<td style="text-align:center;"><button type="button" class="btn btn-ghost btn-sm" data-user-id="' + escapeHtml(user.id) + '" title="View user details">...</button></td>' +
            '</tr>';
        }).join('');
    }

    function renderSummary(page) {
        if (!page || page.totalElements === 0) {
            elements.summary.textContent = 'Showing 0 users';
            return;
        }
        var start = page.number * page.size + 1;
        var end = Math.min((page.number + 1) * page.size, page.totalElements);
        elements.summary.textContent = 'Showing ' + start + '-' + end + ' of ' + page.totalElements + ' users';
    }

    function renderPagination(page) {
        if (!page || page.totalPages <= 1) {
            elements.pagination.innerHTML = '';
            return;
        }

        var buttons = [];
        buttons.push(pageButton('Previous', page.number - 1, page.first));
        for (var i = 0; i < page.totalPages; i += 1) {
            if (i === 0 || i === page.totalPages - 1 || Math.abs(i - page.number) <= 1) {
                buttons.push(pageButton(String(i + 1), i, false, i === page.number));
            } else if (buttons[buttons.length - 1] !== '<span class="page-btn dots">...</span>') {
                buttons.push('<span class="page-btn dots">...</span>');
            }
        }
        buttons.push(pageButton('Next', page.number + 1, page.last));
        elements.pagination.innerHTML = buttons.join('');

        elements.pagination.querySelectorAll('button[data-page]').forEach(function(button) {
            button.addEventListener('click', function() {
                state.page = Number(button.getAttribute('data-page'));
                loadUsers();
            });
        });
    }

    function pageButton(label, pageNumber, disabled, active) {
        return '<button type="button" class="page-btn' + (active ? ' active' : '') + '" data-page="' + pageNumber + '"' + (disabled ? ' disabled' : '') + '>' + escapeHtml(label) + '</button>';
    }

    function openUserPanel(user) {
        state.selectedUser = user;
        elements.panelName.textContent = user.fullName || 'Unnamed user';
        elements.panelEmail.textContent = user.email || '';
        elements.panelRole.textContent = user.role || '--';
        elements.panelStatus.textContent = formatEnum(user.status);
        elements.panelStatus.className = user.status === 'ACTIVE' ? 'badge status-active badge-dot' : 'badge';
        if (user.status === 'BLOCKED') {
            elements.panelStatus.style.background = 'var(--lumina-danger-bg)';
            elements.panelStatus.style.color = 'var(--lumina-danger)';
        } else if (user.status === 'DELETED') {
            elements.panelStatus.style.background = 'var(--lumina-gray-200)';
            elements.panelStatus.style.color = 'var(--lumina-gray-700)';
        } else {
            elements.panelStatus.removeAttribute('style');
        }
        elements.panelCreatedAt.textContent = formatDate(user.createdAt);
        elements.panelProvider.textContent = user.authProvider || '--';
        renderPanelAvatar(user);
        renderStatusAction(user);
        renderDeleteAction(user);
        elements.panel.classList.add('open');
    }

    function renderStatusAction(user) {
        var isSelfAdmin = Number.isFinite(currentAdminId) && user.role === 'ADMIN' && Number(user.id) === currentAdminId;
        var isDeleted = user.status === 'DELETED';
        elements.panelStatusAction.disabled = isSelfAdmin || isDeleted || state.pendingStatusUserId === user.id;
        elements.panelStatusAction.style.color = user.status === 'BLOCKED' ? 'var(--lumina-blue)' : 'var(--lumina-danger)';
        elements.panelStatusAction.style.borderColor = user.status === 'BLOCKED' ? 'var(--lumina-blue-pale)' : 'var(--lumina-danger-bg)';

        if (isSelfAdmin) {
            elements.panelStatusAction.textContent = 'Cannot Modify Own Account';
        } else if (isDeleted) {
            elements.panelStatusAction.textContent = 'Status Locked for Deleted Account';
        } else if (state.pendingStatusUserId === user.id) {
            elements.panelStatusAction.textContent = 'Updating...';
        } else if (user.status === 'BLOCKED') {
            elements.panelStatusAction.textContent = 'Unblock User Account';
        } else {
            elements.panelStatusAction.textContent = 'Block User Account';
        }
    }

    function renderDeleteAction(user) {
        var isSelfAdmin = Number.isFinite(currentAdminId) && user.role === 'ADMIN' && Number(user.id) === currentAdminId;
        var isDeleted = user.status === 'DELETED';
        elements.panelDeleteAction.disabled = isSelfAdmin || isDeleted || state.pendingDeleteUserId === user.id;

        if (isSelfAdmin) {
            elements.panelDeleteAction.textContent = 'Cannot Soft-delete Own Account';
        } else if (isDeleted) {
            elements.panelDeleteAction.textContent = 'Already Deleted';
        } else if (state.pendingDeleteUserId === user.id) {
            elements.panelDeleteAction.textContent = 'Soft-deleting...';
        } else {
            elements.panelDeleteAction.textContent = 'Soft-delete Account';
        }
    }

    function changeSelectedUserStatus() {
        var user = state.selectedUser;
        if (user.status === 'DELETED') {
            setFeedback('Deleted accounts cannot be blocked or unblocked.', false);
            return;
        }
        var shouldUnblock = user.status === 'BLOCKED';
        var action = shouldUnblock ? 'unblock' : 'block';
        var confirmMessage = shouldUnblock
            ? 'Unblock this user account?'
            : 'Block this user account?';

        if (Number.isFinite(currentAdminId) && user.role === 'ADMIN' && Number(user.id) === currentAdminId) {
            setFeedback('You cannot modify your own admin account.', false);
            return;
        }
        if (!window.confirm(confirmMessage)) {
            return;
        }

        state.pendingStatusUserId = user.id;
        renderStatusAction(user);
        setFeedback('', false);
        setError('');

        fetch('/api/admin/users/' + encodeURIComponent(user.id) + '/' + action, {
            method: 'PATCH',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(function(response) {
                return response.json().catch(function() {
                    return { message: 'Unable to update user account.' };
                }).then(function(apiResponse) {
                    if (!response.ok) {
                        throw new Error(apiResponse.message || 'Unable to update user account.');
                    }
                    return apiResponse;
                });
            })
            .then(function(apiResponse) {
                var updatedUser = apiResponse.data;
                state.users = state.users.map(function(item) {
                    return item.id === updatedUser.id ? updatedUser : item;
                });
                state.selectedUser = updatedUser;
                renderUsers(state.users);
                openUserPanel(updatedUser);
                setFeedback(apiResponse.message || 'User account updated successfully.', true);
            })
            .catch(function(error) {
                setFeedback(error.message || 'Unable to update user account.', false);
                renderStatusAction(user);
            })
            .finally(function() {
                state.pendingStatusUserId = null;
                if (state.selectedUser) {
                    renderStatusAction(state.selectedUser);
                    renderDeleteAction(state.selectedUser);
                }
            });
    }

    function softDeleteSelectedUser() {
        var user = state.selectedUser;
        var isSelfAdmin = Number.isFinite(currentAdminId) && user.role === 'ADMIN' && Number(user.id) === currentAdminId;
        if (isSelfAdmin) {
            setFeedback('You cannot soft-delete your own admin account.', false);
            return;
        }
        if (user.status === 'DELETED') {
            setFeedback('This account has already been soft-deleted.', false);
            return;
        }
        if (!window.confirm('Soft-delete this account? The user will no longer be able to log in, but course history, payments, blogs, comments, and other historical data will be retained.')) {
            return;
        }

        state.pendingDeleteUserId = user.id;
        renderDeleteAction(user);
        renderStatusAction(user);
        setFeedback('', false);
        setError('');

        fetch('/api/admin/users/' + encodeURIComponent(user.id), {
            method: 'DELETE',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(function(response) {
                return response.json().catch(function() {
                    return { message: 'Unable to soft-delete user account.' };
                }).then(function(apiResponse) {
                    if (!response.ok) {
                        throw new Error(apiResponse.message || 'Unable to soft-delete user account.');
                    }
                    return apiResponse;
                });
            })
            .then(function(apiResponse) {
                var updatedUser = apiResponse.data;
                state.users = state.users.map(function(item) {
                    return item.id === updatedUser.id ? updatedUser : item;
                });
                state.selectedUser = updatedUser;
                renderUsers(state.users);
                openUserPanel(updatedUser);
                setFeedback(apiResponse.message || 'User account has been soft-deleted. Historical data remains preserved.', true);
            })
            .catch(function(error) {
                setFeedback(error.message || 'Unable to soft-delete user account.', false);
                renderDeleteAction(user);
                renderStatusAction(user);
            })
            .finally(function() {
                state.pendingDeleteUserId = null;
                if (state.selectedUser) {
                    renderDeleteAction(state.selectedUser);
                    renderStatusAction(state.selectedUser);
                }
            });
    }

    function renderPanelAvatar(user) {
        elements.panelAvatar.innerHTML = '';
        elements.panelAvatar.style.backgroundImage = '';
        if (user.avatarUrl) {
            var img = document.createElement('img');
            img.src = user.avatarUrl;
            img.alt = '';
            img.style.width = '100%';
            img.style.height = '100%';
            img.style.objectFit = 'cover';
            img.style.borderRadius = '50%';
            img.onerror = function() {
                elements.panelAvatar.innerHTML = escapeHtml(getInitials(user.fullName || user.email || String(user.id)));
            };
            elements.panelAvatar.appendChild(img);
            return;
        }
        elements.panelAvatar.textContent = getInitials(user.fullName || user.email || String(user.id));
    }

    function statusBadge(status) {
        if (status === 'ACTIVE') {
            return '<span class="badge status-active badge-dot">Active</span>';
        }
        if (status === 'BLOCKED') {
            return '<span class="badge" style="background:var(--lumina-danger-bg);color:var(--lumina-danger);">Blocked</span>';
        }
        if (status === 'DELETED') {
            return '<span class="badge" style="background:var(--lumina-gray-200);color:var(--lumina-gray-700);">Deleted</span>';
        }
        return '<span class="badge">' + escapeHtml(status || '--') + '</span>';
    }

    function setLoading(isLoading) {
        elements.loading.style.display = isLoading ? 'block' : 'none';
    }

    function setError(message) {
        elements.error.textContent = message;
        elements.error.style.display = message ? 'block' : 'none';
    }

    function setFeedback(message, success) {
        elements.feedback.textContent = message;
        elements.feedback.style.display = message ? 'block' : 'none';
        elements.feedback.style.color = success ? 'var(--lumina-success)' : 'var(--lumina-danger)';
    }
}

function appendParam(params, name, value) {
    if (value) {
        params.set(name, value);
    }
}

function debounce(callback, wait) {
    var timeoutId;
    return function() {
        window.clearTimeout(timeoutId);
        timeoutId = window.setTimeout(callback, wait);
    };
}

function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function getInitials(value) {
    return String(value || '--')
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map(function(part) {
            return part.charAt(0).toUpperCase();
        })
        .join('') || '--';
}

function formatDate(value) {
    if (!value) {
        return '--';
    }
    var date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '--';
    }
    return date.toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: '2-digit'
    });
}

function formatEnum(value) {
    if (!value) {
        return '--';
    }
    return value.charAt(0) + value.slice(1).toLowerCase();
}
