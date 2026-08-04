'use strict';

document.addEventListener('DOMContentLoaded', function() {
    initPortalSidebarNav();
    initPortalSidebarDrawer();
    initUserAdministration();
    initAdminTransactions();
});

function initPortalSidebarNav() {
    var currentPath = normalizePortalPath(window.location.pathname);
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    var activeLink = null;
    var activeScore = -1;

    sidebarLinks.forEach(function(link) {
        link.classList.remove('active');
        link.removeAttribute('aria-current');

        getPortalNavPatterns(link).forEach(function(pattern) {
            var score = scorePortalPath(pattern, currentPath);
            if (score > activeScore) {
                activeScore = score;
                activeLink = link;
            }
        });
    });

    if (activeLink) {
        activeLink.classList.add('active');
        activeLink.setAttribute('aria-current', 'page');
    }
}

function getPortalNavPatterns(link) {
    var raw = link.getAttribute('data-nav-match') || link.getAttribute('href') || '';
    return raw.split(',')
        .map(function(value) {
            var clean = value.trim();
            if (!clean) {
                return null;
            }
            var exact = clean.charAt(0) === '=';
            if (exact) {
                clean = clean.substring(1);
            }
            try {
                return { path: normalizePortalPath(new URL(clean, window.location.origin).pathname), exact: exact };
            } catch (error) {
                return { path: normalizePortalPath(clean), exact: exact };
            }
        })
        .filter(Boolean);
}

function normalizePortalPath(path) {
    var clean = String(path || '/').split('?')[0].replace(/\/+$/, '');
    return clean || '/';
}

function scorePortalPath(pattern, currentPath) {
    var patternPath = pattern && pattern.path;
    if (!patternPath || patternPath === '/auth/logout') {
        return -1;
    }
    if (patternPath.indexOf('*') !== -1) {
        return wildcardPortalPathMatch(patternPath, currentPath) ? patternPath.length + 5000 : -1;
    }
    if (patternPath === currentPath) {
        return patternPath.length + 10000;
    }
    if (!pattern.exact && patternPath !== '/' && currentPath.indexOf(patternPath + '/') === 0) {
        return patternPath.length;
    }
    return -1;
}

function wildcardPortalPathMatch(patternPath, currentPath) {
    var escaped = patternPath.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '[^/]+');
    return new RegExp('^' + escaped + '(?:/.*)?$').test(currentPath);
}

function initPortalSidebarDrawer() {
    var portal = document.querySelector('.lumina-portal');
    var sidebar = portal ? portal.querySelector('.lumina-sidebar') : null;
    var toggle = document.querySelector('[data-sidebar-toggle]');
    if (!portal || !sidebar || !toggle) {
        return;
    }

    var mediaQuery = window.matchMedia('(max-width: 768px)');
    var backdrop = portal.querySelector('[data-sidebar-backdrop]');
    if (!backdrop) {
        backdrop = document.createElement('button');
        backdrop.type = 'button';
        backdrop.className = 'sidebar-backdrop';
        backdrop.setAttribute('data-sidebar-backdrop', 'true');
        backdrop.setAttribute('aria-label', 'Close navigation');
        portal.appendChild(backdrop);
    }

    function setOpen(isOpen) {
        portal.classList.toggle('sidebar-open', isOpen);
        document.body.classList.toggle('sidebar-drawer-open', isOpen);
        toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        if (mediaQuery.matches) {
            sidebar.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
        } else {
            sidebar.removeAttribute('aria-hidden');
        }
        if (isOpen) {
            var focusTarget = sidebar.querySelector('[aria-current="page"], .sidebar-nav a, .sidebar-support-links a');
            if (focusTarget) {
                focusTarget.focus();
            }
        }
    }

    toggle.addEventListener('click', function() {
        setOpen(!portal.classList.contains('sidebar-open'));
    });
    backdrop.addEventListener('click', function() {
        setOpen(false);
    });
    sidebar.addEventListener('click', function(event) {
        if (mediaQuery.matches && event.target.closest('a')) {
            setOpen(false);
        }
    });
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && portal.classList.contains('sidebar-open')) {
            setOpen(false);
            toggle.focus();
        }
    });

    function syncMode() {
        if (!mediaQuery.matches) {
            setOpen(false);
            sidebar.removeAttribute('aria-hidden');
        } else if (!portal.classList.contains('sidebar-open')) {
            sidebar.setAttribute('aria-hidden', 'true');
        }
    }

    if (mediaQuery.addEventListener) {
        mediaQuery.addEventListener('change', syncMode);
    } else if (mediaQuery.addListener) {
        mediaQuery.addListener(syncMode);
    }
    syncMode();
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
        pendingDeleteUserId: null,
        exporting: false,
        creating: false,
        createModalLastFocus: null
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
        panelDeleteAction: document.getElementById('user-panel-delete-action'),
        exportButton: document.getElementById('export-users-csv'),
        openCreateButton: document.getElementById('open-create-user'),
        createModal: document.getElementById('create-user-modal'),
        createForm: document.getElementById('create-user-form'),
        createError: document.getElementById('create-user-error'),
        createSubmit: document.getElementById('create-user-submit')
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

    elements.exportButton.addEventListener('click', exportUsersCsv);
    elements.openCreateButton.addEventListener('click', openCreateUserModal);
    elements.createForm.addEventListener('submit', createUser);
    elements.createModal.querySelectorAll('[data-create-user-close]').forEach(function(control) {
        control.addEventListener('click', function() {
            closeCreateUserModal(true);
        });
    });
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && !elements.createModal.hidden && !state.creating) {
            closeCreateUserModal(true);
        }
    });

    loadUsers();

    function currentUserFilterParams() {
        var params = new URLSearchParams();
        appendParam(params, 'keyword', elements.searchInput.value.trim());
        appendParam(params, 'role', elements.roleFilter.value);
        appendParam(params, 'status', elements.statusFilter.value);
        appendParam(params, 'authProvider', elements.providerFilter.value);
        params.set('sortBy', state.sortBy);
        params.set('sortDir', state.sortDir);
        return params;
    }

    function exportUsersCsv() {
        if (state.exporting) {
            return;
        }
        state.exporting = true;
        elements.exportButton.disabled = true;
        elements.exportButton.textContent = 'Exporting...';
        setError('');

        fetch('/api/admin/users/export?' + currentUserFilterParams().toString(), {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'text/csv' }
        })
            .then(function(response) {
                if (!response.ok) {
                    return response.json().catch(function() {
                        return { message: 'Unable to export users.' };
                    }).then(function(payload) {
                        throw new Error(payload.message || 'Unable to export users.');
                    });
                }
                return response.blob().then(function(blob) {
                    return { blob: blob, disposition: response.headers.get('Content-Disposition') || '' };
                });
            })
            .then(function(download) {
                var fileNameMatch = /filename="?([^";]+)"?/i.exec(download.disposition);
                var fileName = fileNameMatch ? fileNameMatch[1] : 'admin-users.csv';
                var downloadUrl = URL.createObjectURL(download.blob);
                var link = document.createElement('a');
                link.href = downloadUrl;
                link.download = fileName;
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.setTimeout(function() {
                    URL.revokeObjectURL(downloadUrl);
                }, 1000);
                setFeedback('CSV export downloaded successfully.', true);
            })
            .catch(function(error) {
                setError(error.message || 'Unable to export users.');
            })
            .finally(function() {
                state.exporting = false;
                elements.exportButton.disabled = false;
                elements.exportButton.textContent = 'Export CSV';
            });
    }

    function openCreateUserModal() {
        state.createModalLastFocus = document.activeElement;
        setCreateError('');
        elements.createModal.hidden = false;
        document.body.classList.add('admin-user-modal-open');
        window.setTimeout(function() {
            elements.createForm.elements.fullName.focus();
        }, 0);
    }

    function closeCreateUserModal(resetForm) {
        if (state.creating) {
            return;
        }
        elements.createModal.hidden = true;
        document.body.classList.remove('admin-user-modal-open');
        setCreateError('');
        if (resetForm) {
            elements.createForm.reset();
        }
        if (state.createModalLastFocus && typeof state.createModalLastFocus.focus === 'function') {
            state.createModalLastFocus.focus();
        }
    }

    function createUser(event) {
        event.preventDefault();
        if (state.creating || !elements.createForm.reportValidity()) {
            return;
        }

        var form = elements.createForm.elements;
        if (form.password.value !== form.confirmPassword.value) {
            setCreateError('Passwords do not match.');
            form.confirmPassword.focus();
            return;
        }

        state.creating = true;
        setCreateError('');
        setCreateFormBusy(true);
        fetch('/api/admin/users', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                fullName: form.fullName.value.trim(),
                email: form.email.value.trim(),
                role: form.role.value,
                password: form.password.value,
                confirmPassword: form.confirmPassword.value
            })
        })
            .then(function(response) {
                return response.json().catch(function() {
                    return { message: 'Unable to create user account.' };
                }).then(function(apiResponse) {
                    if (!response.ok) {
                        throw new Error(apiResponse.message || 'Unable to create user account.');
                    }
                    return apiResponse;
                });
            })
            .then(function(apiResponse) {
                state.creating = false;
                setCreateFormBusy(false);
                closeCreateUserModal(true);
                state.page = 0;
                loadUsers();
                setFeedback(apiResponse.message || 'User account created successfully.', true);
            })
            .catch(function(error) {
                setCreateError(error.message || 'Unable to create user account.');
            })
            .finally(function() {
                state.creating = false;
                setCreateFormBusy(false);
            });
    }

    function setCreateFormBusy(busy) {
        Array.prototype.forEach.call(elements.createForm.elements, function(control) {
            control.disabled = busy;
        });
        elements.createSubmit.textContent = busy ? 'Creating...' : 'Create User';
    }

    function setCreateError(message) {
        elements.createError.textContent = message;
        elements.createError.hidden = !message;
    }

    function loadUsers() {
        setLoading(true);
        setError('');
        setFeedback('', false);

        var params = currentUserFilterParams();
        params.set('page', state.page);
        params.set('size', state.size);

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

function initAdminTransactions() {
    var tableBody = document.getElementById('transactions-table-body');
    if (!tableBody) {
        return;
    }

    var state = {
        page: 0,
        size: 10,
        sortBy: 'createdAt',
        sortDir: 'desc',
        transactions: [],
        selectedTransactionId: null
    };

    var elements = {
        searchInput: document.getElementById('transaction-search-input'),
        statusFilter: document.getElementById('transaction-status-filter'),
        methodFilter: document.getElementById('transaction-method-filter'),
        fromFilter: document.getElementById('transaction-from-filter'),
        toFilter: document.getElementById('transaction-to-filter'),
        sortFilter: document.getElementById('transaction-sort-filter'),
        tableBody: tableBody,
        mobileList: document.getElementById('transactions-mobile-list'),
        loading: document.getElementById('transactions-loading'),
        error: document.getElementById('transactions-error'),
        emptyState: document.getElementById('transactions-empty-state'),
        pagination: document.getElementById('transactions-pagination'),
        summary: document.getElementById('transactions-result-summary'),
        summarySuccessAmount: document.getElementById('txn-summary-success-amount'),
        summarySuccessCount: document.getElementById('txn-summary-success-count'),
        summaryPending: document.getElementById('txn-summary-pending'),
        summaryFailed: document.getElementById('txn-summary-failed'),
        summaryRefunded: document.getElementById('txn-summary-refunded'),
        summaryTotal: document.getElementById('txn-summary-total'),
        panel: document.getElementById('transaction-panel'),
        panelClose: document.getElementById('transaction-panel-close'),
        panelLoading: document.getElementById('transaction-panel-loading'),
        panelError: document.getElementById('transaction-panel-error'),
        panelContent: document.getElementById('transaction-panel-content'),
        panelRef: document.getElementById('transaction-panel-ref'),
        panelStatus: document.getElementById('transaction-panel-status'),
        panelAmount: document.getElementById('transaction-panel-amount'),
        panelPayer: document.getElementById('transaction-panel-payer'),
        panelOrder: document.getElementById('transaction-panel-order'),
        panelCourse: document.getElementById('transaction-panel-course'),
        panelMethod: document.getElementById('transaction-panel-method'),
        panelCreated: document.getElementById('transaction-panel-created'),
        panelUpdated: document.getElementById('transaction-panel-updated'),
        panelOrderTotal: document.getElementById('transaction-panel-order-total'),
        panelOrderPaid: document.getElementById('transaction-panel-order-paid')
    };

    var debouncedLoadTransactions = debounce(function() {
        state.page = 0;
        loadTransactions();
    }, 350);

    elements.searchInput.addEventListener('input', debouncedLoadTransactions);
    [elements.statusFilter, elements.methodFilter, elements.fromFilter, elements.toFilter, elements.sortFilter].forEach(function(filter) {
        filter.addEventListener('change', function() {
            state.page = 0;
            loadTransactions();
        });
    });

    elements.tableBody.addEventListener('click', function(event) {
        var action = event.target.closest('[data-transaction-id]');
        if (action) {
            openTransactionPanel(Number(action.getAttribute('data-transaction-id')));
        }
    });

    elements.mobileList.addEventListener('click', function(event) {
        var action = event.target.closest('[data-transaction-id]');
        if (action) {
            openTransactionPanel(Number(action.getAttribute('data-transaction-id')));
        }
    });

    elements.panelClose.addEventListener('click', function() {
        elements.panel.classList.remove('open');
    });

    loadSummary();
    loadTransactions();

    function loadSummary() {
        fetch('/api/admin/transactions/summary', {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Unable to load transaction summary.');
                }
                return response.json();
            })
            .then(function(apiResponse) {
                renderTransactionSummary(apiResponse.data || {});
            })
            .catch(function() {
                renderTransactionSummary(null);
            });
    }

    function loadTransactions() {
        setTransactionsLoading(true);
        setTransactionsError('');

        if (elements.fromFilter.value && elements.toFilter.value && elements.fromFilter.value > elements.toFilter.value) {
            renderTransactions([]);
            renderTransactionPagination(null);
            renderTransactionPageSummary(null);
            elements.emptyState.hidden = true;
            setTransactionsError('From date must be before or equal to To date.');
            setTransactionsLoading(false);
            return;
        }

        var sortParts = (elements.sortFilter.value || 'createdAt:desc').split(':');
        state.sortBy = sortParts[0] || 'createdAt';
        state.sortDir = sortParts[1] || 'desc';

        var params = new URLSearchParams();
        appendParam(params, 'keyword', elements.searchInput.value.trim());
        appendParam(params, 'status', elements.statusFilter.value);
        appendParam(params, 'paymentMethod', elements.methodFilter.value);
        appendParam(params, 'from', elements.fromFilter.value);
        appendParam(params, 'to', elements.toFilter.value);
        params.set('page', state.page);
        params.set('size', state.size);
        params.set('sortBy', state.sortBy);
        params.set('sortDir', state.sortDir);

        fetch('/api/admin/transactions?' + params.toString(), {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        })
            .then(function(response) {
                return response.json().catch(function() {
                    return { message: 'Unable to load transactions.' };
                }).then(function(apiResponse) {
                    if (!response.ok) {
                        throw new Error(apiResponse.message || 'Unable to load transactions.');
                    }
                    return apiResponse;
                });
            })
            .then(function(apiResponse) {
                var page = apiResponse.data;
                state.transactions = page.content || [];
                renderTransactions(state.transactions);
                renderTransactionPageSummary(page);
                renderTransactionPagination(page);
                elements.emptyState.hidden = state.transactions.length > 0;
            })
            .catch(function(error) {
                state.transactions = [];
                renderTransactions([]);
                renderTransactionPageSummary(null);
                renderTransactionPagination(null);
                elements.emptyState.hidden = true;
                setTransactionsError(error.message || 'Unable to load transactions.');
            })
            .finally(function() {
                setTransactionsLoading(false);
            });
    }

    function renderTransactionSummary(summary) {
        if (!summary) {
            elements.summarySuccessAmount.textContent = '--';
            elements.summarySuccessCount.textContent = '-- successful transactions';
            elements.summaryPending.textContent = '--';
            elements.summaryFailed.textContent = '--';
            elements.summaryRefunded.textContent = '--';
            elements.summaryTotal.textContent = '-- total transactions';
            return;
        }
        elements.summarySuccessAmount.textContent = formatMoney(summary.successfulAmount, 'VND');
        elements.summarySuccessCount.textContent = formatCount(summary.successfulTransactions) + ' successful transactions';
        elements.summaryPending.textContent = formatCount(summary.pendingTransactions);
        elements.summaryFailed.textContent = formatCount(summary.failedTransactions);
        elements.summaryRefunded.textContent = formatCount(summary.refundedTransactions);
        elements.summaryTotal.textContent = formatCount(summary.totalTransactions) + ' total transactions';
    }

    function renderTransactions(transactions) {
        elements.tableBody.innerHTML = transactions.map(function(transaction) {
            return '<tr>' +
                '<td>' + referenceCell(transaction) + '</td>' +
                '<td>' + payerCell(transaction) + '</td>' +
                '<td>' + orderCell(transaction) + '</td>' +
                '<td class="admin-money-cell">' + escapeHtml(formatMoney(transaction.amount, transaction.currency)) + '</td>' +
                '<td>' + escapeHtml(transaction.paymentMethod || '--') + '</td>' +
                '<td>' + transactionStatusBadge(transaction.status) + '</td>' +
                '<td class="admin-time-cell">' + escapeHtml(formatDateTime(transaction.createdAt)) + '</td>' +
                '<td style="text-align:center;"><button type="button" class="btn btn-ghost btn-sm" data-transaction-id="' + escapeHtml(transaction.id) + '" title="View transaction detail">View</button></td>' +
            '</tr>';
        }).join('');

        elements.mobileList.innerHTML = transactions.map(function(transaction) {
            return '<article class="admin-transaction-card">' +
                '<div class="admin-transaction-card-head">' +
                    '<div>' + referenceCell(transaction) + '</div>' +
                    transactionStatusBadge(transaction.status) +
                '</div>' +
                '<div class="admin-transaction-card-amount">' + escapeHtml(formatMoney(transaction.amount, transaction.currency)) + '</div>' +
                '<div class="admin-transaction-card-row"><span>Payer</span><strong>' + escapeHtml(displayPayer(transaction)) + '</strong></div>' +
                '<div class="admin-transaction-card-row"><span>Order</span><strong>' + escapeHtml(displayOrder(transaction)) + '</strong></div>' +
                '<div class="admin-transaction-card-row"><span>Method</span><strong>' + escapeHtml(transaction.paymentMethod || '--') + '</strong></div>' +
                '<div class="admin-transaction-card-row"><span>Time</span><strong>' + escapeHtml(formatDateTime(transaction.createdAt)) + '</strong></div>' +
                '<button type="button" class="btn btn-secondary btn-sm" data-transaction-id="' + escapeHtml(transaction.id) + '">Detail</button>' +
            '</article>';
        }).join('');
    }

    function renderTransactionPageSummary(page) {
        if (!page || page.totalElements === 0) {
            elements.summary.textContent = 'Showing 0 transactions';
            return;
        }
        var start = page.number * page.size + 1;
        var end = Math.min((page.number + 1) * page.size, page.totalElements);
        elements.summary.textContent = 'Showing ' + start + '-' + end + ' of ' + page.totalElements + ' transactions';
    }

    function renderTransactionPagination(page) {
        if (!page || page.totalPages <= 1) {
            elements.pagination.innerHTML = '';
            return;
        }

        var buttons = [];
        buttons.push(transactionPageButton('Previous', page.number - 1, page.first));
        for (var i = 0; i < page.totalPages; i += 1) {
            if (i === 0 || i === page.totalPages - 1 || Math.abs(i - page.number) <= 1) {
                buttons.push(transactionPageButton(String(i + 1), i, false, i === page.number));
            } else if (buttons[buttons.length - 1] !== '<span class="page-btn dots">...</span>') {
                buttons.push('<span class="page-btn dots">...</span>');
            }
        }
        buttons.push(transactionPageButton('Next', page.number + 1, page.last));
        elements.pagination.innerHTML = buttons.join('');

        elements.pagination.querySelectorAll('button[data-page]').forEach(function(button) {
            button.addEventListener('click', function() {
                state.page = Number(button.getAttribute('data-page'));
                loadTransactions();
            });
        });
    }

    function transactionPageButton(label, pageNumber, disabled, active) {
        return '<button type="button" class="page-btn' + (active ? ' active' : '') + '" data-page="' + pageNumber + '"' + (disabled ? ' disabled' : '') + '>' + escapeHtml(label) + '</button>';
    }

    function openTransactionPanel(transactionId) {
        if (!Number.isFinite(transactionId)) {
            return;
        }
        state.selectedTransactionId = transactionId;
        elements.panel.classList.add('open');
        elements.panelLoading.hidden = false;
        elements.panelError.hidden = true;
        elements.panelContent.hidden = true;

        fetch('/api/admin/transactions/' + encodeURIComponent(transactionId), {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        })
            .then(function(response) {
                return response.json().catch(function() {
                    return { message: 'Unable to load transaction detail.' };
                }).then(function(apiResponse) {
                    if (!response.ok) {
                        throw new Error(apiResponse.message || 'Unable to load transaction detail.');
                    }
                    return apiResponse;
                });
            })
            .then(function(apiResponse) {
                renderTransactionDetail(apiResponse.data || {});
                elements.panelContent.hidden = false;
            })
            .catch(function(error) {
                elements.panelError.textContent = error.message || 'Unable to load transaction detail.';
                elements.panelError.hidden = false;
            })
            .finally(function() {
                elements.panelLoading.hidden = true;
            });
    }

    function renderTransactionDetail(transaction) {
        elements.panelRef.textContent = transaction.transactionRef || ('Transaction #' + (transaction.id || '--'));
        elements.panelStatus.className = transactionStatusBadgeClass(transaction.status);
        elements.panelStatus.textContent = formatEnum(transaction.status);
        elements.panelAmount.textContent = formatMoney(transaction.amount, transaction.currency);
        elements.panelPayer.textContent = displayPayer(transaction);
        elements.panelOrder.textContent = displayOrder(transaction);
        elements.panelCourse.textContent = transaction.courseTitle || '--';
        elements.panelMethod.textContent = transaction.paymentMethod || '--';
        elements.panelCreated.textContent = formatDateTime(transaction.createdAt);
        elements.panelUpdated.textContent = formatDateTime(transaction.updatedAt);
        elements.panelOrderTotal.textContent = formatMoney(transaction.orderTotalAmount, transaction.currency);
        elements.panelOrderPaid.textContent = formatMoney(transaction.orderPaidAmount, transaction.currency);
    }

    function referenceCell(transaction) {
        var reference = transaction.transactionRef || ('#' + transaction.id);
        return '<div class="admin-ref-text">' + escapeHtml(reference || '--') + '</div>';
    }

    function payerCell(transaction) {
        return '<div class="admin-payer-cell">' +
            '<div>' + escapeHtml(transaction.studentName || 'Unknown payer') + '</div>' +
            '<span>' + escapeHtml(transaction.studentEmail || '--') + '</span>' +
        '</div>';
    }

    function orderCell(transaction) {
        return '<div class="admin-order-cell">' +
            '<div>' + escapeHtml(displayOrder(transaction)) + '</div>' +
            '<span>' + escapeHtml(transaction.courseTitle || '--') + '</span>' +
        '</div>';
    }

    function displayPayer(transaction) {
        if (transaction.studentName && transaction.studentEmail) {
            return transaction.studentName + ' (' + transaction.studentEmail + ')';
        }
        return transaction.studentName || transaction.studentEmail || '--';
    }

    function displayOrder(transaction) {
        return transaction.orderCode || (transaction.orderId ? 'Order #' + transaction.orderId : '--');
    }

    function setTransactionsLoading(isLoading) {
        elements.loading.hidden = !isLoading;
    }

    function setTransactionsError(message) {
        elements.error.textContent = message;
        elements.error.hidden = !message;
    }
}

function transactionStatusBadge(status) {
    return '<span class="' + transactionStatusBadgeClass(status) + '">' + escapeHtml(formatEnum(status)) + '</span>';
}

function transactionStatusBadgeClass(status) {
    if (status === 'SUCCESS') {
        return 'badge status-active badge-dot';
    }
    if (status === 'PENDING') {
        return 'badge status-pending badge-dot';
    }
    if (status === 'FAILED') {
        return 'badge admin-badge-danger';
    }
    if (status === 'REFUNDED') {
        return 'badge admin-badge-muted';
    }
    return 'badge';
}

function formatMoney(amount, currency) {
    if (amount == null || amount === '') {
        return '--';
    }
    var numericAmount = Number(amount);
    if (Number.isNaN(numericAmount)) {
        return '--';
    }
    var currencyCode = currency || 'VND';
    return new Intl.NumberFormat(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(numericAmount) + ' ' + currencyCode;
}

function formatCount(value) {
    var numericValue = Number(value || 0);
    return new Intl.NumberFormat().format(numericValue);
}

function formatDateTime(value) {
    if (!value) {
        return '--';
    }
    var date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '--';
    }
    return date.toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
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
