'use strict';

document.addEventListener('DOMContentLoaded', function () {
    initPasswordToggles();
    initOAuthButtons();
    initLoginForm();
    initRegisterForm();
    initOAuthCompleteForm();
    initLogoutForms();
});

function initOAuthButtons() {
    document.querySelectorAll('[data-oauth-provider]').forEach(function (button) {
        button.addEventListener('click', function (event) {
            if (button.getAttribute('aria-disabled') === 'true') {
                event.preventDefault();
                return;
            }

            setOAuthLoading(button, true);
        });
    });
}

function initLoginForm() {
    var loginForm = document.getElementById('loginForm');
    if (!loginForm) {
        return;
    }

    loginForm.addEventListener('submit', function (event) {
        event.preventDefault();
        clearFormState(loginForm);

        var payload = {
            email: getFieldValue('email'),
            password: getFieldValue('password')
        };

        if (!validateLogin(loginForm, payload)) {
            return;
        }

        submitAuthForm(loginForm, '/api/auth/login', payload, '/student/dashboard');
    });
}

function initRegisterForm() {
    var registerForm = document.getElementById('registerForm');
    if (!registerForm) {
        return;
    }

    registerForm.addEventListener('submit', function (event) {
        event.preventDefault();
        clearFormState(registerForm);

        var payload = {
            fullName: getFieldValue('fullName'),
            email: getFieldValue('email'),
            password: getFieldValue('password'),
            confirmPassword: getFieldValue('confirmPassword')
        };

        if (!validateRegister(registerForm, payload)) {
            return;
        }

        submitAuthForm(registerForm, '/api/auth/register', payload, '/student/dashboard');
    });
}

function initOAuthCompleteForm() {
    var completeForm = document.getElementById('oauthCompleteForm');
    if (!completeForm) {
        return;
    }

    completeForm.addEventListener('submit', function (event) {
        event.preventDefault();
        clearFormState(completeForm);

        var payload = {
            password: getFieldValue('password'),
            confirmPassword: getFieldValue('confirmPassword')
        };

        if (!validatePasswordPair(completeForm, payload)) {
            focusFirstInvalid(completeForm);
            return;
        }

        submitAuthForm(completeForm, '/api/auth/oauth2/complete', payload, '/student/dashboard');
    });
}

async function submitAuthForm(form, url, payload, fallbackRedirect) {
    var submitButton = form.querySelector('button[type="submit"]');
    setLoading(submitButton, true);
    setStatus(form, submitButton && submitButton.dataset.statusMessage
        ? submitButton.dataset.statusMessage
        : 'Checking your account...');

    try {
        var response = await fetch(url, {
            method: 'POST',
            credentials: 'same-origin',
            headers: csrfHeaders({ 'Content-Type': 'application/json', 'Accept': 'application/json' }),
            body: JSON.stringify(payload)
        });
        var body = await readJson(response);

        if (!response.ok || !body || body.code >= 400) {
            showFormError(form, normalizeApiMessage(body, response.status, 'Authentication failed. Please try again.'));
            return;
        }

        var redirectUrl = body.data && body.data.redirectUrl ? body.data.redirectUrl : fallbackRedirect;
        window.location.assign(redirectUrl);
    } catch (error) {
        console.error('Authentication request failed:', error);
        showFormError(form, 'A system error occurred. Please try again.');
    } finally {
        setStatus(form, '');
        setLoading(submitButton, false);
    }
}

function validateLogin(form, payload) {
    var valid = true;

    if (!isEmail(payload.email)) {
        setFieldError(form, 'email', 'Enter a valid email address.');
        valid = false;
    }
    if (!payload.password) {
        setFieldError(form, 'password', 'Enter your password.');
        valid = false;
    }

    return valid;
}

function validateRegister(form, payload) {
    var valid = true;

    if (!payload.fullName) {
        setFieldError(form, 'fullName', 'Enter your full name.');
        valid = false;
    }
    if (!isEmail(payload.email)) {
        setFieldError(form, 'email', 'Enter a valid email address.');
        valid = false;
    }
    valid = validatePasswordPair(form, payload) && valid;

    var terms = document.getElementById('terms');
    if (terms && !terms.checked) {
        setFieldError(form, 'terms', 'Confirm the student account policy.');
        valid = false;
    }

    return valid;
}

function validatePasswordPair(form, payload) {
    var valid = true;

    if (!payload.password || payload.password.length < 6) {
        setFieldError(form, 'password', 'Use at least 6 characters.');
        valid = false;
    }
    if (!payload.confirmPassword) {
        setFieldError(form, 'confirmPassword', 'Confirm your password.');
        valid = false;
    } else if (payload.password !== payload.confirmPassword) {
        setFieldError(form, 'confirmPassword', 'Passwords do not match.');
        valid = false;
    }

    return valid;
}

function initPasswordToggles() {
    document.querySelectorAll('[data-toggle-password]').forEach(function (button) {
        button.addEventListener('click', function () {
            var group = button.closest('.input-group');
            var input = group ? group.querySelector('input[type="password"], input[type="text"]') : null;
            if (!input) {
                return;
            }

            var showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            button.setAttribute('aria-pressed', showing ? 'false' : 'true');
            button.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
        });
    });
}

function initLogoutForms() {
    // Legacy anchor logout handler. Sidebar logout is a server-rendered POST form.
    const logoutLinks = document.querySelectorAll('a.logout-btn[href$="/auth/logout"], a[data-logout-link="true"]');
    logoutLinks.forEach(link => {
        link.addEventListener("click", function(e) {
            e.preventDefault();
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/auth/logout';
            const csrfInput = document.querySelector('input[name="_csrf"], meta[name="_csrf"]');
            if (csrfInput) {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = csrfInput.getAttribute('name') || '_csrf';
                input.value = csrfInput.value || csrfInput.getAttribute('content') || '';
                form.appendChild(input);
            }
            document.body.appendChild(form);
            form.submit();
        });
    });
}

function csrfHeaders(headers) {
    var merged = Object.assign({}, headers);
    var token = getCsrfToken();
    if (token) {
        merged[getCsrfHeaderName()] = token;
    }
    return merged;
}

function getCsrfToken() {
    return getMetaContent('_csrf')
        || getDataValue('csrfToken')
        || getCookieValue('XSRF-TOKEN');
}

function getCsrfHeaderName() {
    return getMetaContent('_csrf_header')
        || getDataValue('csrfHeader')
        || 'X-XSRF-TOKEN';
}

function getCsrfParameterName() {
    return getMetaContent('_csrf_parameter')
        || getDataValue('csrfParameter')
        || '_csrf';
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
    return document.cookie.split(';').map(function (part) {
        return part.trim();
    }).filter(function (part) {
        return part.indexOf(prefix) === 0;
    }).map(function (part) {
        return decodeURIComponent(part.substring(prefix.length));
    })[0] || '';
}

function clearFormState(form) {
    showFormError(form, '');
    setStatus(form, '');
    form.querySelectorAll('[data-error-for]').forEach(function (element) {
        element.textContent = '';
    });
    form.querySelectorAll('[aria-invalid="true"]').forEach(function (element) {
        element.removeAttribute('aria-invalid');
    });
}

function setFieldError(form, fieldName, message) {
    var field = form.querySelector('[name="' + fieldName + '"]');
    var error = form.querySelector('[data-error-for="' + fieldName + '"]');

    if (field) {
        field.setAttribute('aria-invalid', 'true');
    }
    if (error) {
        error.textContent = message;
    }
}

function showFormError(form, message) {
    var box = form.querySelector('[data-form-error]');
    if (!box) {
        return;
    }
    box.textContent = message || '';
    box.hidden = !message;
}

function setStatus(form, message) {
    var box = form.querySelector('[data-form-status]');
    if (!box) {
        return;
    }
    box.textContent = message || '';
    box.hidden = !message;
}

function setLoading(button, loading) {
    if (!button) {
        return;
    }

    var label = button.querySelector('span');
    var defaultLabel = button.dataset.defaultLabel || (label ? label.textContent : 'Submit');
    var loadingLabel = button.dataset.loadingLabel || 'Please wait...';
    button.disabled = loading;
    button.classList.toggle('btn-loading', loading);
    button.setAttribute('aria-busy', loading ? 'true' : 'false');
    if (label) {
        label.textContent = loading ? loadingLabel : defaultLabel;
    }
}

function setOAuthLoading(button, loading) {
    if (!button) {
        return;
    }

    var label = button.querySelector('span:last-child');
    if (label && !button.dataset.defaultLabel) {
        button.dataset.defaultLabel = label.textContent;
    }

    button.classList.toggle('btn-loading', loading);
    button.setAttribute('aria-busy', loading ? 'true' : 'false');
    button.setAttribute('aria-disabled', loading ? 'true' : 'false');

    if (label) {
        label.textContent = loading
                ? button.dataset.loadingLabel || 'Opening provider...'
                : button.dataset.defaultLabel;
    }
}

function normalizeApiMessage(body, status, fallback) {
    if (!body) {
        return fallback;
    }
    var message = body.message || body.error || '';
    var lowered = message.toLowerCase();

    if (status === 403 || lowered.indexOf('csrf') >= 0 || lowered.indexOf('access denied') >= 0) {
        return 'Your security token expired. Refresh the page and try again.';
    }
    if (status === 409 || lowered.indexOf('already exists') >= 0 || lowered.indexOf('collided') >= 0) {
        return 'An account already uses this verified email. Sign in from the login page and try again.';
    }
    if (lowered.indexOf('expired') >= 0 || lowered.indexOf('sign in again') >= 0) {
        return 'This registration session expired. Start again from the login page.';
    }
    if (lowered.indexOf('password') >= 0 || lowered.indexOf('validation') >= 0) {
        return message;
    }
    if (status === 401) {
        return 'The account could not be authenticated. Check your credentials and try again.';
    }
    return fallback;
}

async function readJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return null;
    }
}

function getFieldValue(id) {
    var field = document.getElementById(id);
    return field ? field.value.trim() : '';
}

function isEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value || '');
}

function focusFirstInvalid(form) {
    var firstInvalid = form.querySelector('[aria-invalid="true"]');
    if (firstInvalid && typeof firstInvalid.focus === 'function') {
        firstInvalid.focus();
    }
}
