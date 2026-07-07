'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var form = document.getElementById('admin-settings-form');
    if (!form) {
        return;
    }

    var state = {
        settings: [],
        dirtyKeys: new Set()
    };

    var elements = {
        form: form,
        groups: document.getElementById('settings-groups'),
        loading: document.getElementById('settings-loading'),
        empty: document.getElementById('settings-empty'),
        error: document.getElementById('settings-error'),
        message: document.getElementById('settings-message'),
        saveAll: document.getElementById('settings-save-all')
    };

    elements.saveAll.addEventListener('click', saveDirtySettings);
    elements.form.addEventListener('submit', function(event) {
        event.preventDefault();
        saveDirtySettings();
    });

    loadSettings();

    function loadSettings() {
        setLoading(true);
        setError('');
        setMessage('', false);

        fetch('/api/admin/settings', {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        })
            .then(readJsonResponse)
            .then(function(apiResponse) {
                state.settings = apiResponse.data || [];
                state.dirtyKeys.clear();
                renderSettings();
            })
            .catch(function(error) {
                state.settings = [];
                renderSettings();
                setError(error.message || 'Unable to load settings.');
            })
            .finally(function() {
                setLoading(false);
            });
    }

    function renderSettings() {
        elements.groups.innerHTML = '';
        elements.form.hidden = state.settings.length === 0;
        elements.empty.hidden = state.settings.length !== 0 || !!elements.error.textContent;
        updateSaveState();

        if (!state.settings.length) {
            return;
        }

        var grouped = state.settings.reduce(function(acc, setting) {
            var category = setting.category || 'General';
            if (!acc[category]) {
                acc[category] = [];
            }
            acc[category].push(setting);
            return acc;
        }, {});

        Object.keys(grouped).sort().forEach(function(category) {
            var section = document.createElement('section');
            section.className = 'admin-settings-category';
            section.innerHTML = '<div class="admin-settings-category-head">' +
                '<h2>' + escapeHtml(category) + '</h2>' +
                '<span>' + grouped[category].length + ' settings</span>' +
                '</div>';

            grouped[category].forEach(function(setting) {
                section.appendChild(createSettingField(setting));
            });

            elements.groups.appendChild(section);
        });
    }

    function createSettingField(setting) {
        var wrapper = document.createElement('div');
        wrapper.className = 'admin-setting-row';

        var control = createControl(setting);
        control.dataset.settingKey = setting.key;
        control.dataset.originalValue = setting.value == null ? '' : setting.value;
        control.disabled = !setting.editable;
        control.addEventListener('input', function() {
            markDirty(setting.key, getControlValue(control) !== control.dataset.originalValue);
        });
        control.addEventListener('change', function() {
            markDirty(setting.key, getControlValue(control) !== control.dataset.originalValue);
        });

        wrapper.innerHTML = '<div class="admin-setting-meta">' +
            '<label for="' + escapeHtml(control.id) + '" title="' + escapeHtml(setting.key) + '">' + escapeHtml(formatSettingName(setting.key)) + '</label>' +
            '<p>' + escapeHtml(setting.description || '') + '</p>' +
            '<span class="admin-setting-type">' + escapeHtml(setting.type || 'STRING') + (setting.editable ? '' : ' - locked') + '</span>' +
            '</div>';
        var controlWrap = document.createElement('div');
        controlWrap.className = 'admin-setting-control';
        controlWrap.appendChild(control);
        wrapper.appendChild(controlWrap);
        return wrapper;
    }

    function createControl(setting) {
        var type = setting.type || 'STRING';
        var id = 'setting-' + setting.key.replace(/[^a-zA-Z0-9_-]/g, '-');
        var value = setting.value == null ? '' : setting.value;
        var control;

        if (type === 'BOOLEAN') {
            control = document.createElement('select');
            control.innerHTML = '<option value="true">true</option><option value="false">false</option>';
            control.value = value === 'true' ? 'true' : 'false';
        } else {
            control = document.createElement('input');
            control.type = type === 'INTEGER' || type === 'DECIMAL' ? 'number' : (type === 'EMAIL' ? 'email' : 'text');
            if (type === 'DECIMAL') {
                control.step = '0.01';
            }
            if (type === 'INTEGER') {
                control.step = '1';
            }
            control.value = value;
            control.maxLength = type === 'EMAIL' ? 320 : 500;
        }

        control.id = id;
        control.name = setting.key;
        control.className = 'admin-setting-input';
        return control;
    }

    function markDirty(key, isDirty) {
        if (isDirty) {
            state.dirtyKeys.add(key);
        } else {
            state.dirtyKeys.delete(key);
        }
        updateSaveState();
    }

    function updateSaveState() {
        elements.saveAll.disabled = state.dirtyKeys.size === 0;
        elements.saveAll.textContent = state.dirtyKeys.size ? 'Save Changes (' + state.dirtyKeys.size + ')' : 'Save Changes';
    }

    function saveDirtySettings() {
        if (!state.dirtyKeys.size || elements.saveAll.disabled) {
            return;
        }

        var values = {};
        state.dirtyKeys.forEach(function(key) {
            var control = elements.form.querySelector('[data-setting-key="' + cssEscape(key) + '"]');
            if (control) {
                values[key] = getControlValue(control);
            }
        });

        elements.saveAll.disabled = true;
        setMessage('', false);
        setError('');

        fetch('/api/admin/settings/bulk', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ values: values })
        })
            .then(readJsonResponse)
            .then(function(apiResponse) {
                state.settings = apiResponse.data || [];
                state.dirtyKeys.clear();
                renderSettings();
                setMessage(apiResponse.message || 'Settings saved successfully.', true);
            })
            .catch(function(error) {
                setError(error.message || 'Unable to save settings.');
                updateSaveState();
            });
    }

    function getControlValue(control) {
        return String(control.value == null ? '' : control.value).trim();
    }

    function readJsonResponse(response) {
        return response.json().catch(function() {
            return { message: 'Unexpected server response.' };
        }).then(function(apiResponse) {
            if (!response.ok) {
                throw new Error(apiResponse.message || 'Request failed with status ' + response.status);
            }
            return apiResponse;
        });
    }

    function setLoading(isLoading) {
        elements.loading.style.display = isLoading ? 'flex' : 'none';
    }

    function setError(message) {
        elements.error.textContent = message;
        elements.error.hidden = !message;
        if (message) {
            elements.empty.hidden = true;
        }
    }

    function setMessage(message, success) {
        elements.message.textContent = message;
        elements.message.hidden = !message;
        elements.message.className = 'admin-settings-message ' + (success ? 'success' : 'danger');
    }

    function cssEscape(value) {
        if (window.CSS && typeof window.CSS.escape === 'function') {
            return window.CSS.escape(value);
        }
        return String(value).replace(/"/g, '\\"');
    }

    function formatSettingName(key) {
        var rawName = String(key || '').split('.').pop() || key || '';
        return rawName
            .replace(/[_-]+/g, ' ')
            .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
            .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
            .trim()
            .replace(/\s+/g, ' ')
            .replace(/\w\S*/g, function(word) {
                return word.charAt(0).toUpperCase() + word.slice(1);
            });
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
});
