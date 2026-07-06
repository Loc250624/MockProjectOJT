'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var page = document.getElementById('profile-page');
    if (!page) {
        return;
    }

    var endpoint = page.dataset.profileEndpoint;
    var placeholderSrc = page.dataset.placeholderSrc || '/images/avatar-placeholder.png';
    var editButton = document.getElementById('profile-edit-button');
    var modal = document.getElementById('profile-edit-modal');
    var form = document.getElementById('profile-edit-form');
    var saveButton = document.getElementById('profile-save-button');
    var message = document.getElementById('profile-message');
    var fullNameInput = document.getElementById('profile-full-name-input');
    var emailInput = document.getElementById('profile-email-input');
    var avatarUrlInput = document.getElementById('profile-avatar-url-input');
    var avatarImages = document.querySelectorAll('[data-profile-avatar]');
    var avatarPreview = document.querySelector('[data-profile-avatar-preview]');
    var closeButtons = document.querySelectorAll('[data-profile-close]');

    if (!endpoint || !editButton || !modal || !form) {
        return;
    }

    function setMessage(text, type) {
        if (!message) {
            return;
        }
        message.textContent = text || '';
        message.classList.remove('success', 'error');
        if (type) {
            message.classList.add(type);
        }
    }

    function setLoading(isLoading) {
        saveButton.disabled = isLoading;
        closeButtons.forEach(function(button) {
            button.disabled = isLoading;
        });
        if (isLoading) {
            setMessage('Saving profile...', '');
        }
    }

    function openModal() {
        setMessage('', '');
        modal.hidden = false;
        fullNameInput.focus();
    }

    function closeModal() {
        modal.hidden = true;
        fullNameInput.value = getText('fullName');
        emailInput.value = getText('email');
        avatarUrlInput.value = page.dataset.currentAvatarUrl || '';
        setPreview(avatarUrlInput.value || placeholderSrc);
    }

    function getText(field) {
        var node = document.querySelector('[data-profile-field="' + field + '"]');
        return node ? node.textContent.trim() : '';
    }

    function setText(field, value) {
        document.querySelectorAll('[data-profile-field="' + field + '"]').forEach(function(node) {
            node.textContent = value || '';
        });
    }

    function setPreview(src) {
        if (avatarPreview) {
            avatarPreview.src = src || placeholderSrc;
        }
    }

    function setAvatar(src) {
        var resolvedSrc = src || placeholderSrc;
        page.dataset.currentAvatarUrl = src || '';
        avatarImages.forEach(function(image) {
            image.src = resolvedSrc;
        });
        setPreview(resolvedSrc);
    }

    function parseApiResponse(response) {
        return response.json().catch(function() {
            return { message: 'Unable to update profile' };
        }).then(function(body) {
            if (!response.ok) {
                throw new Error(body.message || 'Unable to update profile');
            }
            return body;
        });
    }

    function validateAvatarUrl(url) {
        if (!url) {
            return true;
        }
        return /^https?:\/\/.+/i.test(url);
    }

    editButton.addEventListener('click', openModal);

    closeButtons.forEach(function(button) {
        button.addEventListener('click', closeModal);
    });

    avatarImages.forEach(function(image) {
        image.addEventListener('error', function() {
            if (image.src !== placeholderSrc) {
                image.src = placeholderSrc;
            }
        });
    });

    if (avatarPreview) {
        avatarPreview.addEventListener('error', function() {
            if (avatarPreview.src !== placeholderSrc) {
                avatarPreview.src = placeholderSrc;
            }
        });
    }

    avatarUrlInput.addEventListener('input', function() {
        setPreview(avatarUrlInput.value.trim() || placeholderSrc);
    });

    form.addEventListener('submit', function(event) {
        event.preventDefault();

        var avatarUrl = avatarUrlInput.value.trim();
        if (!validateAvatarUrl(avatarUrl)) {
            setMessage('Avatar URL must start with http:// or https://.', 'error');
            return;
        }

        setLoading(true);

        fetch(endpoint, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                fullName: fullNameInput.value.trim(),
                email: emailInput.value.trim(),
                avatarUrl: avatarUrl || null
            })
        })
            .then(parseApiResponse)
            .then(function(apiResponse) {
                var profile = apiResponse.data;
                if (!profile) {
                    throw new Error(apiResponse.message || 'Unable to update profile');
                }

                setText('fullName', profile.fullName);
                setText('email', profile.email);
                setAvatar(profile.avatarUrl);
                closeModal();
                setMessage(apiResponse.message || 'Profile updated successfully', 'success');
            })
            .catch(function(error) {
                setMessage(error.message, 'error');
            })
            .finally(function() {
                setLoading(false);
            });
    });
});
