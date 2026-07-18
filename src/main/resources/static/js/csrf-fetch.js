'use strict';

(function() {
    if (window.__luminaCsrfFetchPatched || typeof window.fetch !== 'function') {
        return;
    }

    var originalFetch = window.fetch.bind(window);
    var mutatingMethods = { POST: true, PUT: true, PATCH: true, DELETE: true };

    window.__luminaCsrfFetchPatched = true;
    window.LuminaCsrf = window.LuminaCsrf || {};
    window.LuminaCsrf.headers = csrfHeaders;

    window.fetch = function(input, init) {
        var requestInit = Object.assign({}, init || {});
        var method = String(requestInit.method || input.method || 'GET').toUpperCase();

        if (mutatingMethods[method] && isSameOrigin(input)) {
            requestInit.headers = csrfHeaders(requestInit.headers || input.headers);
        }

        return originalFetch(input, requestInit);
    };

    function csrfHeaders(headers) {
        var merged = new Headers(headers || {});
        var token = getCsrfToken();
        var headerName = getCsrfHeaderName();

        if (token && headerName && !merged.has(headerName)) {
            merged.set(headerName, token);
        }

        return merged;
    }

    function isSameOrigin(input) {
        try {
            var rawUrl = typeof input === 'string' || input instanceof URL ? input : input.url;
            return new URL(rawUrl, window.location.href).origin === window.location.origin;
        } catch (error) {
            return false;
        }
    }

    function getCsrfToken() {
        return getCookieValue('XSRF-TOKEN')
            || getMetaContent('_csrf')
            || getDataValue('csrfToken')
            || '';
    }

    function getCsrfHeaderName() {
        return getMetaContent('_csrf_header')
            || getDataValue('csrfHeader')
            || 'X-XSRF-TOKEN';
    }

    function getMetaContent(name) {
        var meta = document.querySelector('meta[name="' + name + '"]');
        return meta ? meta.getAttribute('content') : '';
    }

    function getDataValue(name) {
        var holder = document.querySelector('[data-csrf-token]');
        return holder && holder.dataset ? holder.dataset[name] || '' : '';
    }

    function getCookieValue(name) {
        var prefix = name + '=';
        return document.cookie.split(';').map(function(part) {
            return part.trim();
        }).filter(function(part) {
            return part.indexOf(prefix) === 0;
        }).map(function(part) {
            return decodeURIComponent(part.substring(prefix.length));
        })[0] || '';
    }
})();
