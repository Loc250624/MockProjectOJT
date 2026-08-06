(function () {
    'use strict';

    var STORAGE_KEY = 'lumina-theme';
    var LIGHT = 'light';
    var DARK = 'dark';
    var SWITCH_SELECTOR = '[data-theme-switch]';
    var systemMedia = null;
    var initialized = false;

    function isTheme(value) {
        return value === LIGHT || value === DARK;
    }

    function readStoredTheme() {
        try {
            var value = window.localStorage.getItem(STORAGE_KEY);
            return isTheme(value) ? value : null;
        } catch (error) {
            return null;
        }
    }

    function writeStoredTheme(theme) {
        try {
            window.localStorage.setItem(STORAGE_KEY, theme);
        } catch (error) {
            // The current page still follows the selected theme without storage.
        }
    }

    function removeStoredTheme() {
        try {
            window.localStorage.removeItem(STORAGE_KEY);
        } catch (error) {
            // Storage is optional.
        }
    }

    function getSystemTheme() {
        try {
            if (!systemMedia && window.matchMedia) {
                systemMedia = window.matchMedia('(prefers-color-scheme: dark)');
            }
            return systemMedia && systemMedia.matches ? DARK : LIGHT;
        } catch (error) {
            return LIGHT;
        }
    }

    function getCurrentTheme() {
        var current = document.documentElement.getAttribute('data-theme');
        return isTheme(current) ? current : (readStoredTheme() || getSystemTheme());
    }

    function updateSwitches(theme) {
        var isDark = theme === DARK;
        document.querySelectorAll(SWITCH_SELECTOR).forEach(function (control) {
            control.setAttribute('aria-checked', String(isDark));
            control.setAttribute('aria-label', isDark ? 'Switch to light mode' : 'Switch to dark mode');
            control.setAttribute('data-theme-state', theme);
        });
    }

    function dispatchThemeChange(theme, source) {
        var event;
        try {
            event = new CustomEvent('lumina:themechange', {
                detail: { theme: theme, source: source || 'unknown' }
            });
        } catch (error) {
            event = document.createEvent('CustomEvent');
            event.initCustomEvent('lumina:themechange', false, false, {
                theme: theme,
                source: source || 'unknown'
            });
        }
        window.dispatchEvent(event);
    }

    function applyTheme(theme, options) {
        var safeTheme = isTheme(theme) ? theme : getSystemTheme();
        var settings = options || {};
        var previous = document.documentElement.getAttribute('data-theme');

        document.documentElement.setAttribute('data-theme', safeTheme);
        document.documentElement.style.colorScheme = safeTheme;
        updateSwitches(safeTheme);

        if (settings.persist === true) {
            writeStoredTheme(safeTheme);
        } else if (settings.clearStored === true) {
            removeStoredTheme();
        }

        if (previous !== safeTheme || settings.forceEvent === true) {
            dispatchThemeChange(safeTheme, settings.source);
        }
    }

    function toggleTheme() {
        applyTheme(getCurrentTheme() === DARK ? LIGHT : DARK, {
            persist: true,
            source: 'switch'
        });
    }

    function handleDocumentClick(event) {
        var control = event.target.closest ? event.target.closest(SWITCH_SELECTOR) : null;
        if (control) {
            toggleTheme();
        }
    }

    function handleStorage(event) {
        if (event.key !== STORAGE_KEY && event.key !== null) {
            return;
        }
        applyTheme(readStoredTheme() || getSystemTheme(), {
            persist: false,
            source: 'storage'
        });
    }

    function handleSystemThemeChange(event) {
        if (!readStoredTheme()) {
            applyTheme(event.matches ? DARK : LIGHT, {
                persist: false,
                source: 'system'
            });
        }
    }

    function bindSystemThemeListener() {
        try {
            if (!systemMedia && window.matchMedia) {
                systemMedia = window.matchMedia('(prefers-color-scheme: dark)');
            }
            if (!systemMedia) {
                return;
            }
            if (typeof systemMedia.addEventListener === 'function') {
                systemMedia.addEventListener('change', handleSystemThemeChange);
            } else if (typeof systemMedia.addListener === 'function') {
                systemMedia.addListener(handleSystemThemeChange);
            }
        } catch (error) {
            // System theme changes are an optional enhancement.
        }
    }

    function initialize() {
        if (initialized) {
            updateSwitches(getCurrentTheme());
            return;
        }
        initialized = true;

        applyTheme(readStoredTheme() || getCurrentTheme() || getSystemTheme(), {
            persist: false,
            forceEvent: true,
            source: 'initial'
        });

        document.addEventListener('click', handleDocumentClick);
        window.addEventListener('storage', handleStorage);
        bindSystemThemeListener();

        window.requestAnimationFrame(function () {
            document.documentElement.classList.add('theme-ready');
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize, { once: true });
    } else {
        initialize();
    }

    window.LuminaTheme = Object.freeze({
        getTheme: getCurrentTheme,
        setTheme: function (theme) {
            if (!isTheme(theme)) {
                throw new TypeError('Theme must be "light" or "dark".');
            }
            applyTheme(theme, { persist: true, source: 'api' });
        },
        useSystemTheme: function () {
            applyTheme(getSystemTheme(), { clearStored: true, source: 'api-system' });
        }
    });
}());
