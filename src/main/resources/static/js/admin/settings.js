'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var form = document.getElementById('admin-settings-form');
    if (!form) {
        return;
    }

    var GROUPS = [
        { id: 'general', title: 'General' },
        { id: 'learning', title: 'Learning' },
        { id: 'payments', title: 'Payments' },
        { id: 'maintenance', title: 'Maintenance' },
        { id: 'advanced', title: 'Advanced' }
    ];

    var FIELDS = {
        'site.name': {
            group: 'general',
            label: 'Site name',
            help: 'Name displayed across the platform.',
            kind: 'text'
        },
        'site.supportEmail': {
            group: 'general',
            label: 'Support email',
            help: 'Contact email shown to learners and teachers.',
            kind: 'email'
        },
        'learning.defaultEnrollmentStatus': {
            group: 'learning',
            label: 'Default enrollment',
            help: 'Choose what happens after a student enrolls.',
            kind: 'enum',
            labels: {
                ACTIVE: 'Activate immediately',
                INACTIVE: 'Do not activate automatically'
            }
        },
        'commerce.currency': {
            group: 'payments',
            label: 'Platform currency',
            help: 'Currency displayed for course prices, payments and revenue.',
            kind: 'enum',
            labels: {
                VND: 'Vietnamese Dong (VND)',
                USD: 'US Dollar (USD)'
            }
        },
        'commerce.teacherCommissionRate': {
            group: 'payments',
            label: 'Teacher revenue share',
            help: 'Percentage of eligible course revenue received by the teacher.',
            kind: 'percent'
        },
        'site.maintenanceMode': {
            group: 'maintenance',
            label: 'Maintenance mode',
            help: 'Temporarily restrict user access while platform maintenance is being performed.',
            kind: 'switch'
        }
    };

    var ORDERED_KEYS = Object.keys(FIELDS);

    var state = {
        settings: [],
        settingsByKey: {},
        originalValues: {},
        dirtyKeys: new Set(),
        activeTab: 'general',
        saving: false
    };

    var elements = {
        form: form,
        tabs: document.getElementById('settings-tabs'),
        groups: document.getElementById('settings-groups'),
        loading: document.getElementById('settings-loading'),
        empty: document.getElementById('settings-empty'),
        error: document.getElementById('settings-error'),
        message: document.getElementById('settings-message'),
        saveBar: document.getElementById('settings-save-bar'),
        dirtyCount: document.getElementById('settings-dirty-count'),
        discard: document.getElementById('settings-discard'),
        saveAll: document.getElementById('settings-save-all')
    };

    elements.saveAll.addEventListener('click', saveDirtySettings);
    elements.discard.addEventListener('click', discardChanges);
    elements.form.addEventListener('submit', function(event) {
        event.preventDefault();
        saveDirtySettings();
    });
    elements.tabs.addEventListener('click', function(event) {
        var tab = event.target.closest('[data-settings-tab]');
        if (!tab) {
            return;
        }
        activateTab(tab.dataset.settingsTab);
    });

    loadSettings();

    function loadSettings() {
        setLoading(true);
        setError('');
        setMessage('', true);

        fetch('/api/admin/settings', {
            method: 'GET',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
        })
            .then(readJsonResponse)
            .then(function(apiResponse) {
                var incoming = apiResponse.data || [];
                state.settings = ORDERED_KEYS
                    .map(function(key) {
                        return incoming.find(function(setting) {
                            return setting.key === key;
                        });
                    })
                    .filter(Boolean);
                state.settingsByKey = indexByKey(state.settings);
                state.originalValues = {};
                state.settings.forEach(function(setting) {
                    state.originalValues[setting.key] = presentationValue(setting);
                });
                state.dirtyKeys.clear();
                renderSettings();
            })
            .catch(function(error) {
                state.settings = [];
                state.settingsByKey = {};
                state.originalValues = {};
                state.dirtyKeys.clear();
                renderSettings();
                setError(error.message || 'Unable to load settings.');
            })
            .finally(function() {
                setLoading(false);
            });
    }

    function renderSettings() {
        elements.groups.replaceChildren();
        elements.form.hidden = state.settings.length === 0;
        elements.empty.hidden = state.settings.length !== 0 || !!elements.error.textContent;

        if (!state.settings.length) {
            updateSaveState();
            return;
        }

        GROUPS.forEach(function(group) {
            var panel = document.createElement('section');
            panel.className = 'admin-settings-panel';
            panel.dataset.settingsPanel = group.id;
            panel.id = 'settings-panel-' + group.id;
            panel.setAttribute('role', 'tabpanel');
            panel.setAttribute('aria-labelledby', 'settings-tab-' + group.id);

            if (group.id === 'advanced') {
                panel.appendChild(createAdvancedPanel());
            } else {
                var keys = ORDERED_KEYS.filter(function(key) {
                    return FIELDS[key].group === group.id && state.settingsByKey[key];
                });
                keys.forEach(function(key) {
                    panel.appendChild(createSettingField(state.settingsByKey[key], FIELDS[key]));
                });
            }

            elements.groups.appendChild(panel);
        });

        GROUPS.forEach(function(group) {
            var tab = elements.tabs.querySelector('[data-settings-tab="' + cssEscape(group.id) + '"]');
            if (tab) {
                tab.id = 'settings-tab-' + group.id;
                tab.setAttribute('aria-controls', 'settings-panel-' + group.id);
                tab.setAttribute('role', 'tab');
            }
        });
        elements.tabs.setAttribute('role', 'tablist');
        activateTab(state.activeTab);
        updateSaveState();
    }

    function createSettingField(setting, field) {
        var wrapper = document.createElement('div');
        wrapper.className = 'admin-setting-row admin-setting-row-' + field.kind;

        var meta = document.createElement('div');
        meta.className = 'admin-setting-meta';

        var label = document.createElement('label');
        label.htmlFor = controlId(setting.key);
        label.textContent = field.label;

        var help = document.createElement('p');
        help.id = controlId(setting.key) + '-help';
        help.textContent = field.help;

        meta.append(label, help);

        var controlWrap = document.createElement('div');
        controlWrap.className = 'admin-setting-control';
        var control = createControl(setting, field);
        control.dataset.settingKey = setting.key;
        control.dataset.kind = field.kind;
        control.dataset.originalValue = state.originalValues[setting.key];
        control.disabled = !setting.editable;
        control.setAttribute('aria-describedby', help.id + ' ' + control.id + '-validation');

        control.addEventListener('input', function() {
            if (field.kind === 'percent') {
                sanitizePercentInput(control);
                updateRevenuePreview(control);
            }
            handleFieldChange(setting, control);
        });
        control.addEventListener('change', function() {
            if (field.kind === 'switch') {
                handleMaintenanceChange(setting, control);
                return;
            }
            handleFieldChange(setting, control);
        });
        control.addEventListener('blur', function() {
            if (field.kind === 'percent' && control.value.trim() !== '') {
                control.value = normalizePercentForDisplay(control.value);
                updateRevenuePreview(control);
                handleFieldChange(setting, control);
            }
        });

        if (field.kind === 'switch') {
            controlWrap.appendChild(control.switchControl);
        } else {
            controlWrap.appendChild(control);
        }

        if (field.kind === 'percent') {
            var suffix = document.createElement('span');
            suffix.className = 'admin-setting-input-suffix';
            suffix.textContent = '%';
            var inputShell = document.createElement('div');
            inputShell.className = 'admin-setting-input-shell';
            inputShell.append(control, suffix);
            controlWrap.replaceChildren(inputShell);
            controlWrap.appendChild(createRevenuePreview(control));
        }

        if (setting.key === 'commerce.currency') {
            controlWrap.appendChild(createCurrencyPreview());
        }

        var validation = document.createElement('div');
        validation.className = 'admin-setting-validation';
        validation.id = control.id + '-validation';
        validation.setAttribute('role', 'alert');
        controlWrap.appendChild(validation);

        wrapper.append(meta, controlWrap);
        return wrapper;
    }

    function createControl(setting, field) {
        var value = presentationValue(setting);
        if (field.kind === 'switch') {
            return createSwitch(setting, field, value);
        }
        if (field.kind === 'enum') {
            return createSelect(setting, field, value);
        }

        var control = document.createElement('input');
        control.id = controlId(setting.key);
        control.name = setting.key;
        control.className = 'admin-setting-input';
        control.value = value;
        control.type = field.kind === 'email' ? 'email' : 'text';
        control.autocomplete = field.kind === 'email' ? 'email' : 'off';
        if (field.kind === 'percent') {
            control.inputMode = 'decimal';
            control.placeholder = '70';
            control.maxLength = 6;
        } else {
            control.maxLength = field.kind === 'email' ? 320 : 120;
        }
        return control;
    }

    function createSelect(setting, field, value) {
        var control = document.createElement('select');
        control.id = controlId(setting.key);
        control.name = setting.key;
        control.className = 'admin-setting-input';
        (setting.options || []).forEach(function(option) {
            var item = document.createElement('option');
            item.value = option;
            item.textContent = enumLabel(field, option);
            control.appendChild(item);
        });
        control.value = value;
        return control;
    }

    function createSwitch(setting, field, value) {
        var input = document.createElement('input');
        input.id = controlId(setting.key);
        input.name = setting.key;
        input.type = 'checkbox';
        input.checked = value === 'true';
        input.setAttribute('role', 'switch');

        var label = document.createElement('label');
        label.className = 'admin-setting-switch';
        label.htmlFor = input.id;

        var track = document.createElement('span');
        track.className = 'admin-setting-switch-track';
        track.setAttribute('aria-hidden', 'true');

        var text = document.createElement('span');
        text.className = 'admin-setting-switch-state';
        text.textContent = input.checked ? 'On' : 'Off';

        input.addEventListener('change', function() {
            text.textContent = input.checked ? 'On' : 'Off';
        });

        label.append(input, track, text);
        input.switchControl = label;
        return input;
    }

    function createCurrencyPreview() {
        var preview = document.createElement('p');
        preview.className = 'admin-setting-example';
        preview.textContent = 'Example: 499,000 VND';
        return preview;
    }

    function createRevenuePreview(control) {
        var preview = document.createElement('div');
        preview.className = 'admin-settings-revenue-example';
        preview.dataset.revenueExample = control.id;
        preview.innerHTML =
            '<div><span>Course revenue</span><strong>500,000 VND</strong></div>' +
            '<div><span>Teacher receives</span><strong data-teacher-receives>350,000 VND</strong></div>' +
            '<div><span>Platform receives</span><strong data-platform-receives>150,000 VND</strong></div>';
        updateRevenuePreview(control, preview);
        return preview;
    }

    function updateRevenuePreview(control, preview) {
        var target = preview || document.querySelector('[data-revenue-example="' + cssEscape(control.id) + '"]');
        if (!target) {
            return;
        }
        var percent = Number(normalizePercentForDisplay(control.value));
        if (!Number.isFinite(percent) || percent < 0 || percent > 100) {
            percent = 70;
        }
        var revenue = 500000;
        var teacherAmount = Math.round(revenue * (percent / 100));
        var platformAmount = revenue - teacherAmount;
        target.querySelector('[data-teacher-receives]').textContent = formatVnd(teacherAmount);
        target.querySelector('[data-platform-receives]').textContent = formatVnd(platformAmount);
    }

    function createAdvancedPanel() {
        var section = document.createElement('div');
        section.className = 'admin-settings-advanced';

        var intro = document.createElement('p');
        intro.className = 'admin-settings-advanced-intro';
        intro.textContent = 'Technical information is read-only and kept separate from everyday editing.';
        section.appendChild(intro);

        state.settings.forEach(function(setting) {
            var details = document.createElement('details');
            details.className = 'admin-settings-advanced-item';
            var summary = document.createElement('summary');
            summary.textContent = FIELDS[setting.key].label;
            details.appendChild(summary);

            var list = document.createElement('dl');
            list.append(
                detail('Storage key', setting.key),
                detail('Type', setting.type || ''),
                detail('Current raw value', setting.value),
                detail('Storage', setting.storage || ''),
                detail('Applies', setting.effectiveBehavior || '')
            );
            details.appendChild(list);
            section.appendChild(details);
        });

        return section;
    }

    function detail(label, value) {
        var wrapper = document.createElement('div');
        var term = document.createElement('dt');
        var description = document.createElement('dd');
        term.textContent = label;
        description.textContent = value == null || value === '' ? 'Not provided' : String(value);
        wrapper.append(term, description);
        return wrapper;
    }

    function handleFieldChange(setting, control) {
        var validation = validateField(setting, control);
        setFieldValidation(control, validation);
        markDirty(setting.key, currentPresentationValue(control) !== state.originalValues[setting.key]);
    }

    function handleMaintenanceChange(setting, control) {
        var originalOff = state.originalValues[setting.key] === 'false';
        if (originalOff && control.checked) {
            confirmMaintenanceEnable().then(function(confirmed) {
                if (!confirmed) {
                    control.checked = false;
                    syncSwitchText(control);
                }
                handleFieldChange(setting, control);
            });
            return;
        }
        handleFieldChange(setting, control);
    }

    function confirmMaintenanceEnable() {
        if (!window.LuminaActionDialog) {
            return Promise.resolve(true);
        }
        return window.LuminaActionDialog.open({
            variant: 'warning',
            icon: 'warning',
            eyebrow: 'Maintenance',
            title: 'Enable Maintenance Mode?',
            subtitle: 'Students and teachers may temporarily lose access to platform features. Administrator access will remain available.',
            confirmText: 'Enable Maintenance',
            cancelText: 'Cancel',
            initialFocus: 'cancel'
        }).then(function(result) {
            return result.confirmed;
        });
    }

    function validateField(setting, control) {
        var field = FIELDS[setting.key];
        var value = currentPresentationValue(control);
        if (setting.required && value === '') {
            return field.label + ' is required.';
        }
        if (field.kind === 'text') {
            if (value.indexOf('<') !== -1 || value.indexOf('>') !== -1) {
                return 'HTML markup is not allowed.';
            }
            if (setting.minValue != null && value.length < Number(setting.minValue)) {
                return 'Enter at least ' + setting.minValue + ' characters.';
            }
            if (setting.maxValue != null && value.length > Number(setting.maxValue)) {
                return 'Enter ' + setting.maxValue + ' characters or fewer.';
            }
        }
        if (field.kind === 'email' && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value)) {
            return 'Enter a valid email address.';
        }
        if (field.kind === 'enum' && (setting.options || []).indexOf(value) === -1) {
            return 'Choose a supported option.';
        }
        if (field.kind === 'percent') {
            if (!/^\d+(\.\d{1,2})?$/.test(value)) {
                return 'Enter a number from 0 to 100.';
            }
            var numeric = Number(value);
            if (!Number.isFinite(numeric) || numeric < 0 || numeric > 100) {
                return 'Enter a number from 0 to 100.';
            }
        }
        return '';
    }

    function saveDirtySettings() {
        if (!state.dirtyKeys.size || state.saving) {
            return;
        }

        var values = {};
        var hasValidationError = false;
        state.dirtyKeys.forEach(function(key) {
            var setting = state.settingsByKey[key];
            var control = elements.form.querySelector('[data-setting-key="' + cssEscape(key) + '"]');
            if (!setting || !control) {
                return;
            }
            var validation = validateField(setting, control);
            setFieldValidation(control, validation);
            hasValidationError = hasValidationError || !!validation;
            values[key] = rawValueForSave(setting, control);
        });

        if (hasValidationError) {
            setMessage('Please fix validation errors before saving.', false);
            return;
        }

        state.saving = true;
        updateSaveState();
        setMessage('', true);
        setError('');

        fetch('/api/admin/settings/bulk', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify({ values: values })
        })
            .then(readJsonResponse)
            .then(function(apiResponse) {
                var incoming = apiResponse.data || [];
                state.settings = ORDERED_KEYS
                    .map(function(key) {
                        return incoming.find(function(setting) {
                            return setting.key === key;
                        });
                    })
                    .filter(Boolean);
                state.settingsByKey = indexByKey(state.settings);
                state.originalValues = {};
                state.settings.forEach(function(setting) {
                    state.originalValues[setting.key] = presentationValue(setting);
                });
                state.dirtyKeys.clear();
                renderSettings();
                setMessage('Settings saved successfully', true);
            })
            .catch(function(error) {
                setServerFieldError(error.message || '');
                setMessage(error.message || 'Settings could not be saved. No changes were applied.', false);
            })
            .finally(function() {
                state.saving = false;
                updateSaveState();
            });
    }

    function discardChanges() {
        state.dirtyKeys.clear();
        setMessage('', true);
        setError('');
        renderSettings();
    }

    function rawValueForSave(setting, control) {
        var field = FIELDS[setting.key];
        var value = currentPresentationValue(control);
        if (field.kind === 'percent') {
            return percentToFraction(value);
        }
        return value;
    }

    function currentPresentationValue(control) {
        var kind = control.dataset.kind;
        if (kind === 'switch') {
            return control.checked ? 'true' : 'false';
        }
        if (kind === 'percent') {
            return normalizePercentForDisplay(control.value);
        }
        return String(control.value == null ? '' : control.value).trim();
    }

    function presentationValue(setting) {
        var field = FIELDS[setting.key];
        var value = String(setting.value == null ? '' : setting.value).trim();
        if (field.kind === 'switch') {
            return value.toLowerCase() === 'true' ? 'true' : 'false';
        }
        if (field.kind === 'percent') {
            return fractionToPercent(value);
        }
        return value;
    }

    function fractionToPercent(value) {
        var numeric = Number(value);
        if (!Number.isFinite(numeric)) {
            return '';
        }
        return stripTrailingZeros(String((numeric * 100).toFixed(2)));
    }

    function percentToFraction(value) {
        var numeric = Number(value);
        if (!Number.isFinite(numeric)) {
            return '';
        }
        return (numeric / 100).toFixed(2);
    }

    function normalizePercentForDisplay(value) {
        var clean = String(value == null ? '' : value).trim();
        if (clean === '') {
            return '';
        }
        var numeric = Number(clean);
        if (!Number.isFinite(numeric)) {
            return clean;
        }
        return stripTrailingZeros(numeric.toFixed(2));
    }

    function stripTrailingZeros(value) {
        return String(value).replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '');
    }

    function sanitizePercentInput(control) {
        var clean = control.value.replace(/[^\d.]/g, '');
        var firstDot = clean.indexOf('.');
        if (firstDot !== -1) {
            clean = clean.slice(0, firstDot + 1) + clean.slice(firstDot + 1).replace(/\./g, '');
        }
        clean = clean.replace(/^(\d{0,3})(\.\d{0,2})?.*$/, function(match, whole, decimal) {
            return whole + (decimal || '');
        });
        if (clean !== control.value) {
            control.value = clean;
        }
    }

    function setFieldValidation(control, message) {
        var validation = document.getElementById(control.id + '-validation');
        if (!validation) {
            return;
        }
        validation.textContent = message;
        var invalid = !!message;
        var target = control.closest('.admin-setting-switch') || control;
        target.classList.toggle('is-invalid', invalid);
        control.setAttribute('aria-invalid', invalid ? 'true' : 'false');
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
        var dirtyCount = state.dirtyKeys.size;
        elements.saveBar.hidden = dirtyCount === 0;
        elements.saveAll.disabled = state.saving || dirtyCount === 0;
        elements.discard.disabled = state.saving;
        elements.dirtyCount.textContent = dirtyCount === 1 ? '1 unsaved change' : dirtyCount + ' unsaved changes';
        elements.saveAll.textContent = state.saving ? 'Saving...' : 'Save changes';
    }

    function activateTab(tabId) {
        var exists = GROUPS.some(function(group) {
            return group.id === tabId;
        });
        state.activeTab = exists ? tabId : 'general';

        Array.from(elements.tabs.querySelectorAll('[data-settings-tab]')).forEach(function(tab) {
            var active = tab.dataset.settingsTab === state.activeTab;
            tab.classList.toggle('is-active', active);
            tab.setAttribute('aria-selected', active ? 'true' : 'false');
            tab.tabIndex = active ? 0 : -1;
        });

        Array.from(elements.groups.querySelectorAll('[data-settings-panel]')).forEach(function(panel) {
            panel.hidden = panel.dataset.settingsPanel !== state.activeTab;
        });
    }

    function setServerFieldError(message) {
        state.settings.forEach(function(setting) {
            if (message.indexOf(setting.key) !== 0) {
                return;
            }
            var control = elements.form.querySelector('[data-setting-key="' + cssEscape(setting.key) + '"]');
            if (control) {
                setFieldValidation(control, friendlyServerMessage(setting, message));
            }
        });
    }

    function friendlyServerMessage(setting, message) {
        var field = FIELDS[setting.key];
        if (!field) {
            return message;
        }
        return message.replace(setting.key, field.label);
    }

    function syncSwitchText(control) {
        var text = control.closest('.admin-setting-switch').querySelector('.admin-setting-switch-state');
        if (text) {
            text.textContent = control.checked ? 'On' : 'Off';
        }
    }

    function enumLabel(field, option) {
        return field.labels && field.labels[option] ? field.labels[option] : option;
    }

    function controlId(key) {
        return 'setting-' + key.replace(/[^a-zA-Z0-9_-]/g, '-');
    }

    function indexByKey(settings) {
        return settings.reduce(function(acc, setting) {
            acc[setting.key] = setting;
            return acc;
        }, {});
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

    function formatVnd(amount) {
        return new Intl.NumberFormat('en-US', {
            maximumFractionDigits: 0
        }).format(amount) + ' VND';
    }
});
