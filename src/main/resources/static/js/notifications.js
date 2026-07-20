'use strict';

(function() {
    if (window.__luminaNotificationsLoaded) {
        return;
    }
    window.__luminaNotificationsLoaded = true;

    document.addEventListener('DOMContentLoaded', function() {
        var bell = document.querySelector('[data-notification-bell="true"]');
        var popover = document.querySelector('[data-notification-popover]');
        var center = document.querySelector('[data-notification-center]');

        if (bell && popover) {
            initNotificationPopover(bell, popover);
        }
        if (center) {
            initNotificationCenter(center);
        }
    });

    function api(path, options) {
        return fetch(path, Object.assign({ credentials: 'same-origin' }, options || {}))
            .then(function(response) {
                return response.json()
                    .catch(function() {
                        return { message: 'Unable to load notifications' };
                    })
                    .then(function(body) {
                        if (!response.ok) {
                            throw new Error(body.message || 'Unable to load notifications');
                        }
                        return body.data;
                    });
            });
    }

    function initNotificationPopover(bell, popover) {
        var list = popover.querySelector('[data-notification-list]');
        var countBadge = bell.querySelector('[data-notification-count]');
        var markAll = popover.querySelector('[data-notification-mark-all]');

        refreshUnreadCount(countBadge);

        bell.addEventListener('click', function(event) {
            event.stopPropagation();
            var willOpen = popover.hidden;
            popover.hidden = !willOpen;
            bell.setAttribute('aria-expanded', String(willOpen));
            if (willOpen) {
                loadPreview(list, countBadge);
            }
        });

        document.addEventListener('click', function(event) {
            if (!popover.hidden && !popover.contains(event.target) && !bell.contains(event.target)) {
                closePopover(bell, popover);
            }
        });

        document.addEventListener('keydown', function(event) {
            if (event.key === 'Escape') {
                closePopover(bell, popover);
                bell.focus();
            }
        });

        if (markAll) {
            markAll.addEventListener('click', function() {
                setButtonBusy(markAll, true, 'Marking...');
                api('/api/notifications/read-all', { method: 'POST' })
                    .then(function(data) {
                        updateAllCounts(data && data.unreadCount);
                        loadPreview(list, countBadge);
                    })
                    .catch(function(error) {
                        renderState(list, error.message);
                    })
                    .finally(function() {
                        setButtonBusy(markAll, false);
                    });
            });
        }
    }

    function closePopover(bell, popover) {
        popover.hidden = true;
        bell.setAttribute('aria-expanded', 'false');
    }

    function refreshUnreadCount(countBadge) {
        return api('/api/notifications/unread-count')
            .then(function(data) {
                var count = data && data.unreadCount;
                if (countBadge) {
                    updateCount(countBadge, count);
                } else {
                    updateAllCounts(count);
                }
                return Number(count || 0);
            })
            .catch(function() {
                if (countBadge) {
                    updateCount(countBadge, 0);
                } else {
                    updateAllCounts(0);
                }
                return 0;
            });
    }

    function updateCount(countBadge, count) {
        if (!countBadge) {
            return;
        }
        var value = Number(count || 0);
        countBadge.textContent = value > 99 ? '99+' : String(value);
        countBadge.hidden = value <= 0;
    }

    function updateAllCounts(count) {
        document.querySelectorAll('[data-notification-count]').forEach(function(countBadge) {
            updateCount(countBadge, count);
        });
    }

    function loadPreview(list, countBadge) {
        renderState(list, 'Loading...');
        api('/api/notifications?page=0&size=5')
            .then(function(data) {
                renderNotifications(list, data.items || [], true, function() {
                    refreshUnreadCount(countBadge);
                });
            })
            .catch(function(error) {
                renderState(list, error.message);
            });
    }

    function initNotificationCenter(center) {
        var list = center.querySelector('[data-notification-center-list]');
        var loadMore = center.querySelector('[data-notification-load-more]');
        var markAll = center.querySelector('[data-notification-center-mark-all]');
        var page = 0;
        var size = Number(center.dataset.pageSize || 10);
        var loading = false;
        var markingAll = false;
        var unreadCount = 0;

        function syncMarkAllState() {
            if (markAll) {
                markAll.disabled = loading || markingAll || unreadCount <= 0;
            }
        }

        function refreshCenterUnreadCount() {
            return refreshUnreadCount()
                .then(function(count) {
                    unreadCount = count;
                    syncMarkAllState();
                    return count;
                });
        }

        function load(reset) {
            if (loading) {
                return;
            }
            loading = true;
            syncMarkAllState();
            if (reset) {
                page = 0;
                renderState(list, 'Loading...');
                if (loadMore) {
                    loadMore.hidden = true;
                }
            } else if (loadMore) {
                setButtonBusy(loadMore, true, 'Loading...');
            }
            api('/api/notifications?page=' + page + '&size=' + size)
                .then(function(data) {
                    renderNotifications(list, data.items || [], false, function() {
                        refreshCenterUnreadCount().then(function() {
                            load(true);
                        });
                    }, !reset);
                    if (loadMore) {
                        loadMore.hidden = !data.hasNext;
                    }
                    page = Number(data.page || page) + 1;
                })
                .catch(function(error) {
                    if (reset) {
                        renderState(list, error.message);
                    }
                })
                .finally(function() {
                    loading = false;
                    if (loadMore) {
                        setButtonBusy(loadMore, false);
                    }
                    syncMarkAllState();
                });
        }

        if (loadMore) {
            loadMore.addEventListener('click', function() {
                load(false);
            });
        }
        if (markAll) {
            markAll.addEventListener('click', function() {
                markingAll = true;
                syncMarkAllState();
                setButtonBusy(markAll, true, 'Marking...');
                api('/api/notifications/read-all', { method: 'POST' })
                    .then(function(data) {
                        unreadCount = Number(data && data.unreadCount || 0);
                        updateAllCounts(unreadCount);
                        load(true);
                    })
                    .catch(function(error) {
                        renderState(list, error.message);
                    })
                    .finally(function() {
                        markingAll = false;
                        setButtonBusy(markAll, false);
                        syncMarkAllState();
                    });
            });
        }
        syncMarkAllState();
        refreshCenterUnreadCount();
        load(true);
    }

    function renderNotifications(list, items, compact, afterRead, append) {
        if (!append) {
            list.innerHTML = '';
        }
        if (!items.length && !append) {
            renderState(list, 'No notifications yet.');
            return;
        }
        items.forEach(function(item) {
            list.appendChild(notificationElement(item, compact, afterRead));
        });
    }

    function notificationElement(item, compact, afterRead) {
        var row = document.createElement(item.targetPath ? 'a' : 'div');
        row.className = compact ? 'notification-preview-item' : 'notif-item notification-center-item';
        if (!item.read) {
            row.className += ' unread';
        }
        if (item.targetPath) {
            row.href = item.targetPath;
        }
        row.dataset.notificationId = item.id;

        var icon = document.createElement('span');
        icon.className = compact ? 'notification-preview-icon' : 'notif-icon notif-icon-system';
        icon.textContent = iconForType(item.type);

        var body = document.createElement('span');
        body.className = compact ? 'notification-preview-body' : 'notif-body';

        var title = document.createElement(compact ? 'strong' : 'div');
        title.className = compact ? '' : 'notif-title';
        title.textContent = item.title || 'Notification';

        var message = document.createElement('span');
        message.className = compact ? 'notification-preview-message' : 'notif-desc';
        message.textContent = item.message || '';

        var time = document.createElement('span');
        time.className = compact ? 'notification-preview-time' : 'notif-time';
        time.textContent = formatTime(item.createdAt);

        body.appendChild(title);
        body.appendChild(message);
        body.appendChild(time);
        row.appendChild(icon);
        row.appendChild(body);

        row.addEventListener('click', function() {
            if (!item.read) {
                api('/api/notifications/' + encodeURIComponent(item.id) + '/read', { method: 'POST' })
                    .then(function() {
                        if (afterRead) {
                            afterRead();
                        }
                    })
                    .catch(function() {});
            }
        });
        return row;
    }

    function renderState(list, message) {
        list.innerHTML = '';
        var state = document.createElement('div');
        state.className = 'notification-state';
        state.textContent = message;
        list.appendChild(state);
    }

    function setButtonBusy(button, isBusy, busyLabel) {
        if (!button) {
            return;
        }
        var label = button.querySelector('span');
        if (label && !button.dataset.defaultLabel) {
            button.dataset.defaultLabel = label.textContent;
        }
        if (label) {
            label.textContent = isBusy ? busyLabel : button.dataset.defaultLabel;
        }
        button.disabled = isBusy;
    }

    function iconForType(type) {
        if (!type) {
            return 'i';
        }
        if (type.indexOf('ENROLLMENT') >= 0) {
            return '+';
        }
        if (type.indexOf('COURSE') >= 0) {
            return 'C';
        }
        if (type.indexOf('PAYMENT') >= 0) {
            return '$';
        }
        return 'i';
    }

    function formatTime(value) {
        if (!value) {
            return '';
        }
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '';
        }
        return date.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }
})();
