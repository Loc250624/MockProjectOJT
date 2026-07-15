'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var form = document.getElementById('admin-settings-form');
    if (!form) {
        return;
    }

    var state = {
        settings: [],
        dirtyKeys: new Set(),
        saving: false
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
        control.dataset.originalValue = setting.sensitive ? '' : normalizeControlValue(setting, setting.value);
        control.disabled = !setting.editable;
        control.addEventListener('input', function() {
            handleFieldChange(setting, control);
        });
        control.addEventListener('change', function() {
            handleFieldChange(setting, control);
        });

        wrapper.innerHTML = '<div class="admin-setting-meta">' +
            '<label for="' + escapeHtml(control.id) + '" title="' + escapeHtml(setting.key) + '">' + escapeHtml(setting.label || formatSettingName(setting.key)) + '</label>' +
            '<p>' + escapeHtml(setting.description || '') + '</p>' +
            '<span class="admin-setting-type">' + escapeHtml(setting.type || 'STRING') + settingMeta(setting) + '</span>' +
            '</div>';

        var controlWrap = document.createElement('div');
        controlWrap.className = 'admin-setting-control';
        controlWrap.appendChild(control);
        if (setting.unit) {
            var unit = document.createElement('span');
            unit.className = 'admin-setting-unit';
            unit.textContent = setting.unit;
            controlWrap.appendChild(unit);
        }
        var validation = document.createElement('div');
        validation.className = 'admin-setting-validation';
        validation.id = control.id + '-validation';
        validation.setAttribute('role', 'alert');
        control.setAttribute('aria-describedby', validation.id);
        controlWrap.appendChild(validation);
        wrapper.appendChild(controlWrap);
        return wrapper;
    }

    function createControl(setting) {
        var type = setting.type || 'STRING';
        var id = 'setting-' + setting.key.replace(/[^a-zA-Z0-9_-]/g, '-');
        var value = normalizeControlValue(setting, setting.value);
        var control;

        if (type === 'BOOLEAN') {
            control = document.createElement('input');
            control.type = 'checkbox';
            control.checked = value === 'true';
        } else if (type === 'ENUM') {
            control = document.createElement('select');
            (setting.options || []).forEach(function(option) {
                var item = document.createElement('option');
                item.value = option;
                item.textContent = option;
                control.appendChild(item);
            });
            control.value = value;
        } else {
            control = document.createElement('input');
            control.type = inputType(type, setting);
            control.value = setting.sensitive ? '' : value;
            control.maxLength = type === 'EMAIL' ? 320 : (type === 'URL' ? 2048 : 500);
            if (type === 'INTEGER') {
                control.step = '1';
            }
            if (type === 'DECIMAL') {
                control.step = '0.01';
            }
            if ((type === 'INTEGER' || type === 'DECIMAL') && setting.minValue != null) {
                control.min = setting.minValue;
            }
            if ((type === 'INTEGER' || type === 'DECIMAL') && setting.maxValue != null) {
                control.max = setting.maxValue;
            }
            if (setting.sensitive) {
                control.placeholder = 'Leave blank to keep current value';
                control.autocomplete = 'new-password';
            }
        }

        control.id = id;
        control.name = setting.key;
        control.className = type === 'BOOLEAN' ? 'admin-setting-checkbox' : 'admin-setting-input';
        return control;
    }

    function inputType(type, setting) {
        if (setting.sensitive) {
            return 'password';
        }
        if (type === 'INTEGER' || type === 'DECIMAL') {
            return 'number';
        }
        if (type === 'EMAIL') {
            return 'email';
        }
        if (type === 'URL') {
            return 'url';
        }
        return 'text';
    }

    function handleFieldChange(setting, control) {
        var message = validateField(setting, control);
        setFieldValidation(control, message);
        markDirty(setting.key, getControlValue(control) !== control.dataset.originalValue);
    }

    function validateField(setting, control) {
        var value = getControlValue(control);
        var type = setting.type || 'STRING';
        if (setting.sensitive && value === '') {
            return '';
        }
        if (setting.required && value === '') {
            return 'Value is required.';
        }
        if (type === 'INTEGER' && !/^-?\d+$/.test(value)) {
            return 'Enter a whole number.';
        }
        if (type === 'DECIMAL' && !/^-?\d+(\.\d+)?$/.test(value)) {
            return 'Enter a decimal number.';
        }
        if ((type === 'INTEGER' || type === 'DECIMAL') && setting.minValue != null && Number(value) < Number(setting.minValue)) {
            return 'Minimum is ' + setting.minValue + '.';
        }
        if ((type === 'INTEGER' || type === 'DECIMAL') && setting.maxValue != null && Number(value) > Number(setting.maxValue)) {
            return 'Maximum is ' + setting.maxValue + '.';
        }
        if (type === 'EMAIL' && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value)) {
            return 'Enter a valid email address.';
        }
        if (type === 'URL' && !/^https?:\/\/[^/\s]+\S*$/i.test(value)) {
            return 'Enter a valid HTTP or HTTPS URL.';
        }
        if (type === 'ENUM' && (setting.options || []).indexOf(value) === -1) {
            return 'Choose a supported value.';
        }
        return '';
    }

    function setFieldValidation(control, message) {
        var validation = document.getElementById(control.id + '-validation');
        if (!validation) {
            return;
        }
        validation.textContent = message;
        control.classList.toggle('is-invalid', !!message);
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
        elements.saveAll.disabled = state.saving || state.dirtyKeys.size === 0;
        elements.saveAll.textContent = state.saving
            ? 'Saving...'
            : (state.dirtyKeys.size ? 'Save Changes (' + state.dirtyKeys.size + ')' : 'Save Changes');
    }

    function saveDirtySettings() {
        if (!state.dirtyKeys.size || elements.saveAll.disabled) {
            return;
        }

        var values = {};
        var hasValidationError = false;
        state.dirtyKeys.forEach(function(key) {
            var setting = state.settings.find(function(item) {
                return item.key === key;
            });
            var control = elements.form.querySelector('[data-setting-key="' + cssEscape(key) + '"]');
            if (setting && control) {
                var validation = validateField(setting, control);
                setFieldValidation(control, validation);
                hasValidationError = hasValidationError || !!validation;
                values[key] = getControlValue(control);
            }
        });

        if (hasValidationError) {
            setMessage('Please fix validation errors before saving.', false);
            return;
        }

        state.saving = true;
        updateSaveState();
        setMessage('', false);
        setError('');

        fetch('/api/admin/settings/bulk', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
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
                setServerFieldError(error.message || '');
                setError(error.message || 'Unable to save settings.');
            })
            .finally(function() {
                state.saving = false;
                updateSaveState();
            });
    }

    function setServerFieldError(message) {
        state.settings.forEach(function(setting) {
            if (message.indexOf(setting.key) !== 0) {
                return;
            }
            var control = elements.form.querySelector('[data-setting-key="' + cssEscape(setting.key) + '"]');
            if (control) {
                setFieldValidation(control, message);
            }
        });
    }

    function getControlValue(control) {
        if (control.type === 'checkbox') {
            return control.checked ? 'true' : 'false';
        }
        return String(control.value == null ? '' : control.value).trim();
    }

    function normalizeControlValue(setting, value) {
        if (setting.type === 'BOOLEAN') {
            return String(value).toLowerCase() === 'true' ? 'true' : 'false';
        }
        return String(value == null ? '' : value).trim();
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

    function settingMeta(setting) {
        var parts = [];
        if (setting.unit) {
            parts.push(setting.unit);
        }
        if (setting.sensitive) {
            parts.push('secret');
        }
        if (!setting.editable) {
            parts.push('locked');
        }
        if (setting.effectiveBehavior) {
            parts.push(setting.effectiveBehavior);
        }
        return parts.length ? ' - ' + parts.map(escapeHtml).join(' - ') : '';
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
