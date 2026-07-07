'use strict';
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    sidebarLinks.forEach(function(link) {
        var linkPath = new URL(link.getAttribute('href'), window.location.origin).pathname;
        if (linkPath === currentPath) {
            link.classList.add('active');
        }
    });

    if (currentPath === '/teacher/profile') {
        var createCourseButton = document.getElementById('btn-create-course');
        if (createCourseButton) {
            createCourseButton.style.display = 'none';
        }
    }

    initProfileTabs();
    initProfileEditor();
    initTeacherStudentFilters();
    initCourseEnrollmentCta();
    initLearningProgress();
    initStudentCertificates();
    initAssessmentQuiz();
    initAssessmentSubmission();
});

function initCourseEnrollmentCta() {
    var enrollButton = document.querySelector('[data-enrollment-action="free"]');
    if (!enrollButton) {
        return;
    }

    var courseId = enrollButton.dataset.courseId;
    var message = document.getElementById('course-enrollment-message');
    var originalText = enrollButton.textContent;

    function setMessage(text, isError) {
        if (!message) {
            return;
        }
        message.textContent = text || '';
        message.style.color = isError ? 'var(--lumina-danger)' : 'var(--lumina-success)';
    }

    function parseJsonResponse(response, fallbackMessage) {
        return response.json()
            .catch(function() {
                return { message: fallbackMessage };
            })
            .then(function(body) {
                if (!response.ok) {
                    throw new Error(body.message || fallbackMessage);
                }
                return body;
            });
    }

    function refreshState() {
        return fetch('/student/courses/' + courseId + '/enrollment-state', {
            credentials: 'same-origin'
        }).then(function(response) {
            return parseJsonResponse(response, 'Unable to refresh enrollment state');
        }).then(function(apiResponse) {
            var state = apiResponse.data;
            if (!state) {
                return;
            }
            setMessage(state.message, false);
            if (state.action === 'CONTINUE_LEARNING') {
                var continueLink = document.createElement('a');
                continueLink.href = '/student/learning?courseId=' + encodeURIComponent(courseId);
                continueLink.className = enrollButton.className;
                continueLink.id = enrollButton.id;
                continueLink.textContent = 'Continue learning';
                enrollButton.replaceWith(continueLink);
            }
        });
    }

    enrollButton.addEventListener('click', function() {
        enrollButton.disabled = true;
        enrollButton.textContent = 'Enrolling...';
        setMessage('Confirming enrollment...', false);

        fetch('/student/courses/' + courseId + '/enroll', {
            method: 'POST',
            credentials: 'same-origin'
        }).then(function(response) {
            return parseJsonResponse(response, 'Unable to enroll in this course');
        }).then(function(apiResponse) {
            setMessage(apiResponse.message || 'Enrollment confirmed', false);
            return refreshState();
        }).catch(function(error) {
            setMessage(error.message, true);
            enrollButton.disabled = false;
            enrollButton.textContent = originalText;
        });
    });
}

function initLearningProgress() {
    var video = document.querySelector('[data-learning-video="true"]');
    var completeButton = document.querySelector('[data-lesson-complete="true"]');

    if (video) {
        initVideoProgress(video);
    }
    if (completeButton) {
        initLessonCompletion(completeButton);
    }
}

function parseLearningJson(response, fallbackMessage) {
    return response.json()
        .catch(function() {
            return { message: fallbackMessage };
        })
        .then(function(body) {
            if (!response.ok) {
                throw new Error(body.message || fallbackMessage);
            }
            return body;
        });
}

function applyLearningProgress(progress) {
    if (!progress) {
        return;
    }
    var lessonStatus = document.getElementById('lesson-status');
    if (lessonStatus && progress.completed) {
        lessonStatus.textContent = 'Completed';
        lessonStatus.classList.add('completed');
    }

    var help = document.getElementById('video-progress-help');
    if (help && typeof progress.watchedSeconds === 'number') {
        help.textContent = 'Saved through ' + progress.watchedSeconds + ' seconds.';
    }

    document.querySelectorAll('.learning-progress-summary .progress-bar-fill, .pc-progress .progress-bar-fill').forEach(function(fill) {
        if (progress.progressPercentage != null) {
            fill.style.width = progress.progressPercentage + '%';
        }
    });
}

function initVideoProgress(video) {
    var courseId = video.dataset.courseId;
    var lessonId = video.dataset.lessonId;
    var highestLocal = Number(video.dataset.watchedSeconds || 0);
    var lastSaved = highestLocal;
    var saveTimer = null;
    var saving = false;

    if (highestLocal > 0) {
        video.addEventListener('loadedmetadata', function() {
            if (video.currentTime < highestLocal) {
                video.currentTime = highestLocal;
            }
        }, { once: true });
    }

    function saveProgress(force) {
        var current = Math.floor(video.currentTime || 0);
        if (!Number.isFinite(current) || current < 0) {
            return Promise.resolve();
        }
        highestLocal = Math.max(highestLocal, current);
        if (!force && highestLocal - lastSaved < 15) {
            return Promise.resolve();
        }
        if (saving) {
            return Promise.resolve();
        }
        saving = true;
        return fetch('/student/courses/' + encodeURIComponent(courseId) + '/lessons/' + encodeURIComponent(lessonId) + '/video-progress', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ watchedSeconds: highestLocal })
        }).then(function(response) {
            return parseLearningJson(response, 'Unable to save video progress');
        }).then(function(apiResponse) {
            lastSaved = Math.max(lastSaved, apiResponse.data && apiResponse.data.watchedSeconds || highestLocal);
            applyLearningProgress(apiResponse.data);
        }).catch(function(error) {
            var help = document.getElementById('video-progress-help');
            if (help) {
                help.textContent = error.message;
            }
        }).finally(function() {
            saving = false;
        });
    }

    function scheduleSave() {
        window.clearTimeout(saveTimer);
        saveTimer = window.setTimeout(function() {
            saveProgress(false);
        }, 1000);
    }

    video.addEventListener('timeupdate', scheduleSave);
    video.addEventListener('pause', function() {
        saveProgress(true);
    });
    video.addEventListener('seeked', function() {
        saveProgress(false);
    });
    window.addEventListener('beforeunload', function() {
        var current = Math.floor(video.currentTime || 0);
        highestLocal = Math.max(highestLocal, current);
        if (highestLocal > lastSaved && navigator.sendBeacon) {
            var blob = new Blob([JSON.stringify({ watchedSeconds: highestLocal })], { type: 'application/json' });
            navigator.sendBeacon('/student/courses/' + encodeURIComponent(courseId) + '/lessons/' + encodeURIComponent(lessonId) + '/video-progress', blob);
        }
    });
}

function initLessonCompletion(button) {
    var message = document.getElementById('lesson-complete-message');
    button.addEventListener('click', function() {
        button.disabled = true;
        if (message) {
            message.textContent = 'Saving completion...';
        }
        fetch('/student/courses/' + encodeURIComponent(button.dataset.courseId) + '/lessons/' + encodeURIComponent(button.dataset.lessonId) + '/complete', {
            method: 'POST',
            credentials: 'same-origin'
        }).then(function(response) {
            return parseLearningJson(response, 'Unable to complete this lesson');
        }).then(function(apiResponse) {
            applyLearningProgress(apiResponse.data);
            if (message) {
                message.textContent = 'Lesson completed.';
            }
        }).catch(function(error) {
            button.disabled = false;
            if (message) {
                message.textContent = error.message;
            }
        });
    });
}

function assessmentJson(response, fallbackMessage) {
    return response.json().catch(function() {
        return { message: fallbackMessage };
    }).then(function(body) {
        if (!response.ok) {
            throw new Error(body.message || fallbackMessage);
        }
        return body;
    });
}

function setAssessmentMessage(element, text, type) {
    if (!element) {
        return;
    }
    element.textContent = text || '';
    element.classList.remove('error', 'success');
    if (type) {
        element.classList.add(type);
    }
}

function initAssessmentQuiz() {
    var page = document.querySelector('.assessment-page[data-quiz-id]');
    if (!page || !page.dataset.quizId) {
        return;
    }
    var quizId = page.dataset.quizId;
    var message = document.getElementById('quiz-message');
    var saveState = document.getElementById('quiz-save-state');
    var questions = Array.prototype.slice.call(document.querySelectorAll('.quiz-question'));
    var navButtons = Array.prototype.slice.call(document.querySelectorAll('.q-nav-btn'));
    if (!questions.length) {
        return;
    }
    var current = 0;
    var attemptId = null;

    function showQuestion(index) {
        current = Math.max(0, Math.min(index, questions.length - 1));
        questions.forEach(function(question, questionIndex) {
            question.classList.toggle('active', questionIndex === current);
        });
        navButtons.forEach(function(button, buttonIndex) {
            button.classList.toggle('active', buttonIndex === current);
        });
    }

    function collectAnswers() {
        return questions.map(function(question) {
            return {
                questionId: Number(question.dataset.questionId),
                selectedOptionIds: Array.prototype.slice.call(question.querySelectorAll('input:checked')).map(function(input) {
                    return Number(input.value);
                })
            };
        });
    }

    function markAnswered() {
        questions.forEach(function(question, index) {
            if (navButtons[index]) {
                navButtons[index].classList.toggle('answered', question.querySelectorAll('input:checked').length > 0);
            }
        });
    }

    function ensureAttempt() {
        if (attemptId) {
            return Promise.resolve(attemptId);
        }
        setAssessmentMessage(saveState, 'Starting...', null);
        return fetch('/api/student/quizzes/' + encodeURIComponent(quizId) + '/attempts', {
            method: 'POST',
            credentials: 'same-origin'
        }).then(function(response) {
            return assessmentJson(response, 'Unable to start quiz');
        }).then(function(body) {
            attemptId = body.data.id;
            setAssessmentMessage(saveState, 'Draft active', null);
            return attemptId;
        });
    }

    function saveDraft() {
        return ensureAttempt().then(function(id) {
            return fetch('/api/student/quiz-attempts/' + encodeURIComponent(id) + '/draft', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({ answers: collectAnswers() })
            });
        }).then(function(response) {
            return assessmentJson(response, 'Unable to save draft');
        }).then(function() {
            setAssessmentMessage(message, 'Draft saved.', 'success');
            setAssessmentMessage(saveState, 'Saved', null);
        }).catch(function(error) {
            setAssessmentMessage(message, error.message, 'error');
        });
    }

    navButtons.forEach(function(button) {
        button.addEventListener('click', function() {
            showQuestion(Number(button.dataset.questionIndex));
        });
    });
    document.querySelectorAll('.quiz-option input').forEach(function(input) {
        input.addEventListener('change', function() {
            markAnswered();
            input.closest('.quiz-options').querySelectorAll('.quiz-option').forEach(function(option) {
                option.classList.toggle('selected', option.querySelector('input:checked') !== null);
            });
        });
    });
    var previous = document.getElementById('previous-question-btn');
    if (previous) {
        previous.addEventListener('click', function() { showQuestion(current - 1); });
    }
    var next = document.getElementById('next-question-btn');
    if (next) {
        next.addEventListener('click', function() { showQuestion(current + 1); });
    }
    var save = document.getElementById('save-quiz-btn');
    if (save) {
        save.addEventListener('click', saveDraft);
    }
    var submit = document.getElementById('submit-quiz-btn');
    if (submit) {
        submit.addEventListener('click', function() {
            if (!window.confirm('Submit this quiz attempt? You cannot edit it after submission.')) {
                return;
            }
            ensureAttempt().then(function(id) {
                submit.disabled = true;
                return fetch('/api/student/quiz-attempts/' + encodeURIComponent(id) + '/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ answers: collectAnswers() })
                });
            }).then(function(response) {
                return assessmentJson(response, 'Unable to submit quiz');
            }).then(function(body) {
                window.location.href = '/student/quizzes/' + encodeURIComponent(body.data.id) + '/result';
            }).catch(function(error) {
                submit.disabled = false;
                setAssessmentMessage(message, error.message, 'error');
            });
        });
    }
    markAnswered();
    showQuestion(0);
    ensureAttempt().catch(function(error) {
        setAssessmentMessage(message, error.message, 'error');
    });
}

function initAssessmentSubmission() {
    var form = document.getElementById('assignment-submit-form');
    if (!form) {
        return;
    }
    var page = document.querySelector('.assessment-page[data-assignment-id]');
    var assignmentId = page && page.dataset.assignmentId;
    var message = document.getElementById('assignment-message');
    var draftButton = document.getElementById('save-assignment-draft');

    function payload() {
        return {
            contentText: form.elements.contentText.value.trim(),
            codeLanguage: form.elements.codeLanguage.value.trim(),
            codeContent: form.elements.codeContent.value.trim(),
            filePath: form.elements.filePath.value.trim()
        };
    }

    function send(kind) {
        var body = payload();
        if (kind === 'submit' && !body.contentText && !body.codeContent && !body.filePath) {
            setAssessmentMessage(message, 'Please provide text, code, or a file path before submitting.', 'error');
            return Promise.resolve();
        }
        return fetch('/api/student/assignments/' + encodeURIComponent(assignmentId) + '/submissions/' + kind, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(body)
        }).then(function(response) {
            return assessmentJson(response, 'Unable to save submission');
        }).then(function(apiResponse) {
            setAssessmentMessage(message, apiResponse.message, 'success');
            if (kind === 'submit' && apiResponse.data && apiResponse.data.id) {
                window.location.href = '/student/submissions/' + encodeURIComponent(apiResponse.data.id) + '/result';
            }
        }).catch(function(error) {
            setAssessmentMessage(message, error.message, 'error');
        });
    }

    if (draftButton) {
        draftButton.addEventListener('click', function() { send('draft'); });
    }
    form.addEventListener('submit', function(event) {
        event.preventDefault();
        if (window.confirm('Submit this assignment?')) {
            send('submit');
        }
    });
}

function switchProfileTab(tabName) {
    var tabs = document.querySelectorAll('[data-profile-tab]');
    var panels = document.querySelectorAll('[data-profile-panel]');
    tabs.forEach(function(tab) {
        tab.classList.toggle('active', tab.dataset.profileTab === tabName);
    });
    panels.forEach(function(panel) {
        panel.classList.toggle('active', panel.dataset.profilePanel === tabName);
    });
}

function initProfileTabs() {
    var tabs = document.querySelectorAll('[data-profile-tab]');
    if (!tabs.length) {
        return;
    }

    tabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
            switchProfileTab(tab.dataset.profileTab);
        });
    });
}

function initProfileEditor() {
    var form = document.getElementById('profile-edit-form');
    if (!form) {
        return;
    }

    var editButton = document.getElementById('profile-edit-button');
    var quickAvatarEditButton = document.getElementById('profile-avatar-quick-edit');
    var cancelButton = document.getElementById('profile-cancel-button');
    var saveButton = document.getElementById('profile-save-button');
    var message = document.getElementById('profile-message');
    var toast = document.getElementById('profile-toast');
    var nameText = document.getElementById('profile-name');
    var emailText = document.getElementById('profile-email');
    var avatarImage = document.getElementById('profile-avatar');
    var avatarPreview = document.getElementById('profile-avatar-preview');
    var fullNameInput = document.getElementById('profile-full-name-input');
    var emailInput = document.getElementById('profile-email-input');
    var avatarInput = document.getElementById('profile-avatar-input');
    var avatarFileName = document.getElementById('profile-avatar-file-name');
    var avatarUrlButton = document.getElementById('profile-avatar-url-button');
    var avatarUrlPanel = document.getElementById('profile-avatar-url-panel');
    var avatarUrlInput = document.getElementById('profile-avatar-url-input');
    var avatarUrlPreviewButton = document.getElementById('profile-avatar-preview-url-button');
    var removeAvatarButton = document.getElementById('profile-avatar-remove-button');
    var placeholderSrc = form.dataset.placeholderSrc || avatarImage.dataset.placeholderSrc || '/images/avatar-placeholder.png';
    var selectedAvatarFile = null;
    var selectedAvatarUrl = '';
    var shouldRemoveAvatar = false;
    var currentAvatarSrc = avatarImage.src;

    function setMessage(text, type) {
        message.textContent = text || '';
        if (toast) {
            toast.textContent = text || '';
            toast.hidden = !text;
            toast.classList.toggle('success', type === 'success');
            toast.classList.toggle('error', type === 'error');
        }
        if (type === 'success') {
            message.style.color = 'var(--lumina-success)';
        } else if (type === 'error') {
            message.style.color = 'var(--lumina-danger)';
        } else {
            message.style.color = 'var(--lumina-gray-500)';
        }
    }

    function setLoading(isLoading) {
        saveButton.disabled = isLoading;
        cancelButton.disabled = isLoading;
        editButton.disabled = isLoading;
        if (avatarUrlButton) avatarUrlButton.disabled = isLoading;
        if (avatarUrlPreviewButton) avatarUrlPreviewButton.disabled = isLoading;
        if (removeAvatarButton) removeAvatarButton.disabled = isLoading;
        if (isLoading) {
            setMessage('Saving profile...', 'loading');
        }
    }

    function setAvatarPreview(src) {
        var nextSrc = src || placeholderSrc;
        document.querySelectorAll('[data-profile-avatar="true"]').forEach(function(image) {
            image.src = nextSrc;
        });
        if (avatarPreview) {
            avatarPreview.src = nextSrc;
        }
    }

    function clearAvatarChoice() {
        selectedAvatarFile = null;
        selectedAvatarUrl = '';
        shouldRemoveAvatar = false;
        avatarInput.value = '';
        if (avatarUrlInput) avatarUrlInput.value = '';
        if (avatarFileName) avatarFileName.textContent = 'No file selected';
    }

    function closeForm() {
        fullNameInput.value = nameText.textContent.trim();
        emailInput.value = emailText.textContent.trim();
        clearAvatarChoice();
        if (avatarUrlPanel) {
            avatarUrlPanel.hidden = true;
        }
    }

    function parseJsonResponse(response, fallbackMessage) {
        return response.json()
            .catch(function() {
                return { message: fallbackMessage };
            })
            .then(function(body) {
                if (!response.ok) {
                    throw new Error(body.message || fallbackMessage);
                }
                return body;
            });
    }

    function setSelectedAvatarFile(file) {
        if (!file) {
            return;
        }
        if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
            setMessage('Only JPEG, PNG, and WEBP images are supported.', 'error');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            setMessage('Avatar image must not exceed 5MB.', 'error');
            return;
        }

        clearAvatarChoice();
        selectedAvatarFile = file;
        if (avatarFileName) avatarFileName.textContent = file.name || 'Selected image';
        setAvatarPreview(URL.createObjectURL(file));
        setMessage('Image selected. Click Save to upload.', 'loading');
    }

    function setSelectedAvatarUrl(url) {
        var normalizedUrl = (url || '').trim();
        if (!normalizedUrl) {
            setMessage('Image URL is required.', 'error');
            return false;
        }
        if (!/^https?:\/\/.+/i.test(normalizedUrl)) {
            setMessage('Image URL must start with http:// or https://.', 'error');
            return false;
        }

        setMessage('Checking image URL...', 'loading');
        var probe = new Image();
        probe.onload = function() {
            clearAvatarChoice();
            selectedAvatarUrl = normalizedUrl;
            if (avatarUrlInput) avatarUrlInput.value = normalizedUrl;
            if (avatarFileName) avatarFileName.textContent = 'Using image URL';
            setAvatarPreview(normalizedUrl);
            setMessage('Preview loaded. Click Save to fetch and store the image.', 'loading');
        };
        probe.onerror = function() {
            setMessage('The image URL could not be loaded. Please use a public JPG, PNG, or WEBP URL.', 'error');
        };
        probe.src = normalizedUrl;
        return true;
    }

    function updateProfile() {
        return fetch('/api/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
                fullName: fullNameInput.value.trim(),
                email: emailInput.value.trim()
            })
        }).then(function(response) {
            return parseJsonResponse(response, 'Unable to update profile');
        });
    }

    function uploadAvatar(file) {
        var formData = new FormData();
        formData.append('file', file, file.name || 'avatar.jpg');
        return fetch('/api/profile/avatar/upload', {
            method: 'POST',
            credentials: 'same-origin',
            body: formData
        }).then(function(response) {
            return parseJsonResponse(response, 'Unable to upload avatar');
        });
    }

    function useAvatarUrl(url) {
        return fetch('/api/profile/avatar/url', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ imageUrl: url })
        }).then(function(response) {
            return parseJsonResponse(response, 'Unable to update avatar from URL');
        });
    }

    function removeAvatar() {
        return fetch('/api/profile/avatar', {
            method: 'DELETE',
            credentials: 'same-origin'
        }).then(function(response) {
            return parseJsonResponse(response, 'Unable to remove avatar');
        });
    }

    function applyProfile(profile) {
        nameText.textContent = profile.fullName || '';
        emailText.textContent = profile.email || '';
        setAvatarPreview(profile.avatarUrl || placeholderSrc);
        currentAvatarSrc = avatarImage.src;
    }

    function openEditTab() {
        currentAvatarSrc = avatarImage.src;
        setMessage('', 'loading');
        switchProfileTab('edit');
        fullNameInput.focus();
    }

    editButton.addEventListener('click', openEditTab);
    if (quickAvatarEditButton) {
        quickAvatarEditButton.addEventListener('click', openEditTab);
    }

    cancelButton.addEventListener('click', function() {
        setAvatarPreview(currentAvatarSrc);
        closeForm();
        setMessage('', 'loading');
        switchProfileTab('overview');
    });

    avatarInput.addEventListener('change', function() {
        setSelectedAvatarFile(avatarInput.files && avatarInput.files[0]);
    });

    avatarUrlButton.addEventListener('click', function() {
        avatarUrlPanel.hidden = !avatarUrlPanel.hidden;
        if (!avatarUrlPanel.hidden) {
            avatarUrlInput.focus();
        }
    });

    avatarUrlPreviewButton.addEventListener('click', function() {
        setSelectedAvatarUrl(avatarUrlInput.value);
    });

    removeAvatarButton.addEventListener('click', function() {
        clearAvatarChoice();
        shouldRemoveAvatar = true;
        setAvatarPreview(placeholderSrc);
        setMessage('Photo will be removed after you click Save.', 'loading');
    });

    form.addEventListener('submit', function(event) {
        event.preventDefault();
        setLoading(true);

        updateProfile()
            .then(function(profileResponse) {
                if (selectedAvatarFile) {
                    return uploadAvatar(selectedAvatarFile);
                }
                if (selectedAvatarUrl) {
                    return useAvatarUrl(selectedAvatarUrl);
                }
                if (shouldRemoveAvatar) {
                    return removeAvatar();
                }
                return profileResponse;
            })
            .then(function(apiResponse) {
                if (!apiResponse.data) {
                    throw new Error(apiResponse.message || 'Unable to update profile');
                }
                applyProfile(apiResponse.data);
                closeForm();
                setMessage(apiResponse.message || 'Profile updated successfully', 'success');
                switchProfileTab('overview');
            })
            .catch(function(error) {
                setMessage(error.message, 'error');
            })
            .finally(function() {
                setLoading(false);
            });
    });
}

function initTeacherStudentFilters() {
    var searchInput = document.getElementById('teacher-student-search');
    var statusFilter = document.getElementById('teacher-student-status-filter');
    var cards = document.querySelectorAll('.teacher-student-card');
    if (!cards.length || !searchInput || !statusFilter) {
        return;
    }

    function applyFilters() {
        var searchTerm = searchInput.value.trim().toLowerCase();
        var status = statusFilter.value;
        cards.forEach(function(card) {
            var matchesSearch = !searchTerm || (card.dataset.search || '').toLowerCase().includes(searchTerm);
            var matchesStatus = !status || card.dataset.status === status;
            card.hidden = !(matchesSearch && matchesStatus);
        });
    }

    searchInput.addEventListener('input', applyFilters);
    statusFilter.addEventListener('change', applyFilters);
}

function initStudentCertificates() {
    var page = document.getElementById('student-certificates-page');
    if (!page) {
        return;
    }

    var grid = document.getElementById('certificate-grid');
    var tableBody = document.getElementById('certificate-history-body');
    var loading = document.getElementById('certificate-loading');
    var empty = document.getElementById('certificate-empty');
    var total = document.getElementById('certificate-total');
    var message = document.getElementById('certificate-message');
    var historyCard = document.getElementById('certificate-history-card');

    function setMessage(text, isError) {
        if (!message) {
            return;
        }
        message.textContent = text || '';
        message.style.color = isError ? 'var(--lumina-danger)' : 'var(--lumina-gray-600)';
    }

    function parseJsonResponse(response, fallbackMessage) {
        return response.json()
            .catch(function() {
                return { message: fallbackMessage };
            })
            .then(function(body) {
                if (!response.ok) {
                    throw new Error(body.message || fallbackMessage);
                }
                return body;
            });
    }

    function formatDate(value) {
        if (!value) {
            return 'Not available';
        }
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return 'Not available';
        }
        return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: '2-digit' });
    }

    function appendText(parent, tagName, className, text) {
        var element = document.createElement(tagName);
        if (className) {
            element.className = className;
        }
        element.textContent = text || '';
        parent.appendChild(element);
        return element;
    }

    function renderCard(certificate) {
        var card = document.createElement('div');
        card.className = 'cert-card';
        var inner = document.createElement('div');
        inner.className = 'cert-card-inner';
        card.appendChild(inner);

        appendText(inner, 'div', 'cert-ribbon', certificate.status === 'ACTIVE' ? 'C' : 'R');
        appendText(inner, 'div', null, 'Certificate of Completion').style.cssText = 'font-size:0.6875rem;font-weight:700;color:var(--lumina-blue);text-transform:uppercase;letter-spacing:0.1em;margin-bottom:0.75rem;';
        appendText(inner, 'div', 'cert-course', certificate.courseName);
        appendText(inner, 'div', 'cert-meta', 'Issued to ' + (certificate.studentName || 'Student') + ' on ' + formatDate(certificate.issuedAt));
        appendText(inner, 'div', 'cert-meta', 'Teacher: ' + (certificate.teacherName || 'LumiNa Instructor'));

        var codeBox = document.createElement('div');
        codeBox.className = 'cert-id-box';
        appendText(codeBox, 'span', 'cert-id-label', 'Verification Code');
        appendText(codeBox, 'span', 'cert-id-value', certificate.verificationCode);
        inner.appendChild(codeBox);

        var actions = document.createElement('div');
        actions.style.cssText = 'display:flex;gap:0.75rem;flex-wrap:wrap;';
        var download = document.createElement('button');
        download.type = 'button';
        download.className = 'btn btn-primary btn-sm flex-1';
        download.textContent = 'Download PDF';
        download.addEventListener('click', function() {
            downloadCertificate(certificate.id, download);
        });
        var verify = document.createElement('a');
        verify.className = 'btn btn-secondary btn-sm flex-1';
        verify.href = certificate.verifyUrl || ('/certificates/verify/' + encodeURIComponent(certificate.verificationCode));
        verify.textContent = 'Verify';
        actions.appendChild(download);
        actions.appendChild(verify);
        inner.appendChild(actions);
        return card;
    }

    function renderRow(certificate) {
        var row = document.createElement('tr');
        appendText(row, 'td', null, certificate.courseName);
        appendText(row, 'td', null, certificate.teacherName);
        appendText(row, 'td', null, formatDate(certificate.issuedAt));
        appendText(row, 'td', null, certificate.verificationCode).style.fontFamily = 'var(--font-mono)';
        var statusCell = document.createElement('td');
        appendText(statusCell, 'span', certificate.status === 'ACTIVE' ? 'badge badge-success badge-dot' : 'badge badge-danger badge-dot', certificate.status);
        row.appendChild(statusCell);
        var actionCell = document.createElement('td');
        var link = document.createElement('a');
        link.href = certificate.verifyUrl || ('/certificates/verify/' + encodeURIComponent(certificate.verificationCode));
        link.textContent = 'View';
        link.style.cssText = 'color:var(--lumina-blue);font-weight:600;font-size:0.8125rem;';
        actionCell.appendChild(link);
        row.appendChild(actionCell);
        return row;
    }

    function renderCertificates(items, totalElements) {
        grid.replaceChildren();
        tableBody.replaceChildren();
        total.textContent = String(totalElements || items.length);
        empty.hidden = items.length > 0;
        historyCard.hidden = items.length === 0;
        items.forEach(function(certificate) {
            grid.appendChild(renderCard(certificate));
            tableBody.appendChild(renderRow(certificate));
        });
    }

    function downloadCertificate(certificateId, button) {
        var original = button.textContent;
        button.disabled = true;
        button.textContent = 'Preparing...';
        fetch('/api/student/certificates/' + encodeURIComponent(certificateId) + '/download', {
            credentials: 'same-origin'
        }).then(function(response) {
            if (!response.ok) {
                throw new Error('Unable to download this certificate.');
            }
            return response.blob();
        }).then(function(blob) {
            var url = URL.createObjectURL(blob);
            var anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = 'lumina-certificate-' + certificateId + '.pdf';
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            URL.revokeObjectURL(url);
        }).catch(function(error) {
            setMessage(error.message, true);
        }).finally(function() {
            button.disabled = false;
            button.textContent = original;
        });
    }

    fetch('/api/student/certificates', {
        credentials: 'same-origin'
    }).then(function(response) {
        return parseJsonResponse(response, 'Unable to load certificates');
    }).then(function(apiResponse) {
        var data = apiResponse.data || {};
        renderCertificates(data.content || [], data.totalElements || 0);
        setMessage('', false);
    }).catch(function(error) {
        setMessage(error.message, true);
        empty.hidden = false;
    }).finally(function() {
        loading.hidden = true;
    });
}
