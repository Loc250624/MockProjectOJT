'use strict';

(function() {
    if (window.__luminaCsrfFetchPatched || typeof window.fetch !== 'function') {
        return;
    }

    var originalFetch = window.fetch.bind(window);
    var mutatingMethods = { POST: true, PUT: true, PATCH: true, DELETE: true };
    var currentToken = null;
    var inFlightToken = null;

    window.__luminaCsrfFetchPatched = true;
    window.LuminaCsrf = window.LuminaCsrf || {};
    window.LuminaCsrf.headers = csrfHeaders;
    window.LuminaCsrf.ensureCsrfToken = ensureCsrfToken;
    window.LuminaCsrf.refreshCsrfToken = refreshCsrfToken;
    window.LuminaCsrf.clearToken = clearToken;

    window.fetch = function(input, init) {
        var requestInit = Object.assign({}, init || {});
        var method = String(requestInit.method || (input && input.method) || 'GET').toUpperCase();
        var sameOrigin = isSameOrigin(input);

        if (!mutatingMethods[method] || !sameOrigin) {
            return originalFetch(input, requestInit);
        }

        requestInit.credentials = requestInit.credentials || (input && input.credentials) || 'same-origin';

        return ensureCsrfToken().catch(function () {
            return null;
        }).then(function () {
            requestInit.headers = csrfHeaders(requestInit.headers || (input && input.headers));
            return originalFetch(input, requestInit);
        }).then(function (response) {
            if (requestInit.__luminaCsrfRetry) {
                return response;
            }
            return isCsrfForbidden(response).then(function (csrfForbidden) {
                if (!csrfForbidden) {
                    return response;
                }
                return refreshCsrfToken().then(function () {
                    var retryInit = Object.assign({}, requestInit, { __luminaCsrfRetry: true });
                    retryInit.headers = csrfHeaders(retryInit.headers || (input && input.headers), true);
                    return originalFetch(input, retryInit);
                }).catch(function () {
                    return response;
                });
            });
        });
    };

    document.addEventListener('DOMContentLoaded', function () {
        ensureCsrfToken().catch(function () {});

        document.querySelectorAll('form[action$="/auth/logout"], [data-logout-submit="true"]').forEach(function (element) {
            var form = element.tagName === 'FORM' ? element : element.closest('form');
            if (form) {
                form.addEventListener('submit', clearToken);
            }
        });
    });

    function csrfHeaders(headers, forceCachedToken) {
        var merged = new Headers(headers || {});
        var tokenInfo = forceCachedToken && isUsableToken(currentToken) ? currentToken : readCsrfToken();
        var token = tokenInfo.token;
        var headerName = tokenInfo.headerName;

        if (token && headerName && !merged.has(headerName)) {
            merged.set(headerName, token);
        }

        return merged;
    }

    function ensureCsrfToken() {
        if (isUsableToken(currentToken)) {
            return Promise.resolve(currentToken);
        }

        var tokenInfo = readCsrfToken();
        if (isUsableToken(tokenInfo)) {
            currentToken = tokenInfo;
            return Promise.resolve(currentToken);
        }

        if (!inFlightToken) {
            inFlightToken = requestCsrfToken().finally(function () {
                inFlightToken = null;
            });
        }
        return inFlightToken;
    }

    function refreshCsrfToken() {
        currentToken = null;
        return requestCsrfToken();
    }

    function clearToken() {
        currentToken = null;
        inFlightToken = null;
    }

    function requestCsrfToken() {
        return originalFetch('/api/auth/csrf', {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('Unable to obtain CSRF token');
            }
            return response.json();
        }).then(function (body) {
            var tokenInfo = normalizeTokenResponse(body);
            if (!isUsableToken(tokenInfo)) {
                throw new Error('Backend returned an unusable CSRF token');
            }
            currentToken = tokenInfo;
            writeCsrfDataset(tokenInfo);
            return currentToken;
        });
    }

    function isCsrfForbidden(response) {
        if (!response || response.status !== 403 || typeof response.clone !== 'function') {
            return Promise.resolve(false);
        }
        return response.clone().json().then(function (body) {
            return body && (body.code === 'CSRF_TOKEN_MISSING' || body.code === 'CSRF_TOKEN_INVALID');
        }).catch(function () {
            return false;
        });
    }

    function isSameOrigin(input) {
        try {
            var rawUrl = typeof input === 'string' || input instanceof URL ? input : input.url;
            return new URL(rawUrl, window.location.href).origin === window.location.origin;
        } catch (error) {
            return false;
        }
    }

    function readCsrfToken() {
        return normalizeTokenResponse({
            headerName: getMetaContent('_csrf_header')
                || getDataValue('csrfHeader')
                || 'X-XSRF-TOKEN',
            parameterName: getMetaContent('_csrf_parameter')
                || getDataValue('csrfParameter')
                || '_csrf',
            token: getMetaContent('_csrf')
                || getDataValue('csrfToken')
                || getCookieValue('XSRF-TOKEN')
                || ''
        });
    }

    function normalizeTokenResponse(body) {
        if (body && body.data) {
            body = body.data;
        }
        return {
            headerName: cleanTokenPart(body && body.headerName),
            parameterName: cleanTokenPart(body && body.parameterName) || '_csrf',
            token: cleanTokenPart(body && body.token)
        };
    }

    function isUsableToken(tokenInfo) {
        return !!(tokenInfo
            && cleanTokenPart(tokenInfo.headerName)
            && cleanTokenPart(tokenInfo.token));
    }

    function cleanTokenPart(value) {
        if (typeof value !== 'string') {
            return '';
        }
        var clean = value.trim();
        return clean && clean !== 'null' && clean !== 'undefined' ? clean : '';
    }

    function writeCsrfDataset(tokenInfo) {
        var holder = document.querySelector('[data-csrf-token]');
        if (!holder || !holder.dataset) {
            holder = document.createElement('span');
            holder.hidden = true;
            holder.setAttribute('data-csrf-token', '');
            document.body.appendChild(holder);
        }
        holder.dataset.csrfToken = tokenInfo.token;
        holder.dataset.csrfHeader = tokenInfo.headerName;
        holder.dataset.csrfParameter = tokenInfo.parameterName;
    }

    function getMetaContent(name) {
        var meta = document.querySelector('meta[name="' + name + '"]');
        return cleanTokenPart(meta ? meta.getAttribute('content') : '');
    }

    function getDataValue(name) {
        var holder = document.querySelector('[data-csrf-token]');
        return cleanTokenPart(holder && holder.dataset ? holder.dataset[name] || '' : '');
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
