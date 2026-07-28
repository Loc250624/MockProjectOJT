'use strict';
document.addEventListener('DOMContentLoaded', function() {
    initPortalSidebarNav();
    initPortalSidebarDrawer();
    initTeacherAssessments();
});

function initPortalSidebarNav() {
    var currentPath = normalizePortalPath(window.location.pathname);
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    var activeLink = null;
    var activeScore = -1;

    sidebarLinks.forEach(function(link) {
        link.classList.remove('active');
        link.removeAttribute('aria-current');

        getPortalNavPatterns(link).forEach(function(pattern) {
            var score = scorePortalPath(pattern, currentPath);
            if (score > activeScore) {
                activeScore = score;
                activeLink = link;
            }
        });
    });

    if (activeLink) {
        activeLink.classList.add('active');
        activeLink.setAttribute('aria-current', 'page');
    }
}
function getPortalNavPatterns(link) {
    var raw = link.getAttribute('data-nav-match') || link.getAttribute('href') || '';
    return raw.split(',')
        .map(function(value) {
            var clean = value.trim();
            if (!clean) {
                return null;
            }
            var exact = clean.charAt(0) === '=';
            if (exact) {
                clean = clean.substring(1);
            }
            try {
                return { path: normalizePortalPath(new URL(clean, window.location.origin).pathname), exact: exact };
            } catch (error) {
                return { path: normalizePortalPath(clean), exact: exact };
            }
        })
        .filter(Boolean);
}

function normalizePortalPath(path) {
    var clean = String(path || '/').split('?')[0].replace(/\/+$/, '');
    return clean || '/';
}

function scorePortalPath(pattern, currentPath) {
    var patternPath = pattern && pattern.path;
    if (!patternPath || patternPath === '/auth/logout') {
        return -1;
    }
    if (patternPath.indexOf('*') !== -1) {
        return wildcardPortalPathMatch(patternPath, currentPath) ? patternPath.length + 5000 : -1;
    }
    if (patternPath === currentPath) {
        return patternPath.length + 10000;
    }
    if (!pattern.exact && patternPath !== '/' && currentPath.indexOf(patternPath + '/') === 0) {
        return patternPath.length;
    }
    return -1;
}

function wildcardPortalPathMatch(patternPath, currentPath) {
    var escaped = patternPath.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '[^/]+');
    return new RegExp('^' + escaped + '(?:/.*)?$').test(currentPath);
}

function initPortalSidebarDrawer() {
    var portal = document.querySelector('.lumina-portal');
    var sidebar = portal ? portal.querySelector('.lumina-sidebar') : null;
    var toggle = document.querySelector('[data-sidebar-toggle]');
    if (!portal || !sidebar || !toggle) {
        return;
    }

    var mediaQuery = window.matchMedia('(max-width: 768px)');
    var backdrop = portal.querySelector('[data-sidebar-backdrop]');
    if (!backdrop) {
        backdrop = document.createElement('button');
        backdrop.type = 'button';
        backdrop.className = 'sidebar-backdrop';
        backdrop.setAttribute('data-sidebar-backdrop', 'true');
        backdrop.setAttribute('aria-label', 'Close navigation');
        portal.appendChild(backdrop);
    }

    function setOpen(isOpen) {
        portal.classList.toggle('sidebar-open', isOpen);
        document.body.classList.toggle('sidebar-drawer-open', isOpen);
        toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        if (mediaQuery.matches) {
            sidebar.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
        } else {
            sidebar.removeAttribute('aria-hidden');
        }
        if (isOpen) {
            var focusTarget = sidebar.querySelector('[aria-current="page"], .sidebar-nav a, .sidebar-support-links a');
            if (focusTarget) {
                focusTarget.focus();
            }
        }
    }

    toggle.addEventListener('click', function() {
        setOpen(!portal.classList.contains('sidebar-open'));
    });
    backdrop.addEventListener('click', function() {
        setOpen(false);
    });
    sidebar.addEventListener('click', function(event) {
        if (mediaQuery.matches && event.target.closest('a')) {
            setOpen(false);
        }
    });
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && portal.classList.contains('sidebar-open')) {
            setOpen(false);
            toggle.focus();
        }
    });

    function syncMode() {
        if (!mediaQuery.matches) {
            setOpen(false);
            sidebar.removeAttribute('aria-hidden');
        } else if (!portal.classList.contains('sidebar-open')) {
            sidebar.setAttribute('aria-hidden', 'true');
        }
    }

    if (mediaQuery.addEventListener) {
        mediaQuery.addEventListener('change', syncMode);
    } else if (mediaQuery.addListener) {
        mediaQuery.addListener(syncMode);
    }
    syncMode();
}

function teacherAssessmentJson(response, fallbackMessage) {
    return response.json().catch(function() {
        return { message: fallbackMessage };
    }).then(function(body) {
        if (!response.ok) {
            throw new Error(body.message || fallbackMessage);
        }
        return body;
    });
}

function teacherCookieValue(name) {
    return document.cookie.split(';').map(function(part) {
        return part.trim();
    }).filter(function(part) {
        return part.indexOf(name + '=') === 0;
    }).map(function(part) {
        return decodeURIComponent(part.substring(name.length + 1));
    })[0] || '';
}

function teacherCsrfHeaders(existingHeaders) {
    var headers = new Headers(existingHeaders || {});
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    var token = tokenMeta && tokenMeta.content ? tokenMeta.content : teacherCookieValue('XSRF-TOKEN');
    var headerName = headerMeta && headerMeta.content ? headerMeta.content : 'X-XSRF-TOKEN';
    if (token && !headers.has(headerName)) {
        headers.set(headerName, token);
    }
    return headers;
}

function teacherFetch(url, options) {
    var requestOptions = options || {};
    requestOptions.headers = teacherCsrfHeaders(requestOptions.headers);
    requestOptions.credentials = requestOptions.credentials || 'same-origin';
    return fetch(url, requestOptions);
}

function teacherAssessmentMessage(element, text, type) {
    if (!element) {
        return;
    }
    element.textContent = text || '';
    element.classList.remove('error', 'success');
    if (type) {
        element.classList.add(type);
    }
}

function initTeacherAssessments() {
    initTeacherQuizBuilder();
    initTeacherQuizActions();
}

function parseTeacherQuestionOptions(rawValue) {
    try {
        var parsed = JSON.parse(rawValue || '[]');
        if (Array.isArray(parsed)) {
            return parsed.map(function(option) {
                if (typeof option === 'string') {
                    return { content: option.trim(), correct: false };
                }
                return {
                    content: option && option.content ? String(option.content).trim() : '',
                    correct: option && option.correct === true
                };
            }).filter(function(option) { return option.content; });
        }
    } catch (ignored) {
        // Fall through to the legacy delimiter reader for pre-normalized markup.
    }
    return (rawValue || '').split('|').map(function(raw) {
        var text = raw.trim();
        var correct = text.charAt(0) === '*';
        return { content: correct ? text.substring(1).trim() : text, correct: correct };
    }).filter(function(option) { return option.content; });
}

function readTeacherQuestionOptionRows(form) {
    var rows = Array.prototype.slice.call(form.querySelectorAll('[data-option-row]'));
    if (!rows.length) {
        return parseTeacherQuestionOptions(form.elements.options ? form.elements.options.value : '');
    }
    return rows.map(function(row) {
        var contentInput = row.querySelector('[data-option-content]');
        var correctInput = row.querySelector('[data-option-correct]');
        return {
            content: contentInput ? contentInput.value.trim() : '',
            correct: correctInput ? correctInput.checked : false
        };
    }).filter(function(option) { return option.content; });
}

function writeTeacherQuestionOptionRows(form) {
    var hidden = form.elements.options;
    if (!hidden) {
        return;
    }
    hidden.value = JSON.stringify(readTeacherQuestionOptionRows(form));
}

function syncTeacherCorrectControls(form) {
    var typeField = form.elements.questionType;
    if (!typeField) {
        return;
    }
    var singleChoice = typeField.value === 'SINGLE_CHOICE';
    form.querySelectorAll('[data-option-correct]').forEach(function(input) {
        input.type = singleChoice ? 'radio' : 'checkbox';
        input.name = singleChoice
            ? 'correctOption-' + (form.elements.questionId ? form.elements.questionId.value : form.closest('[data-quiz-id]').dataset.quizId)
            : 'correctOption';
    });
}

function teacherQuizPayload(form) {
    return {
        lessonId: Number(form.elements.lessonId.value),
        title: form.elements.title.value.trim(),
        description: form.elements.description ? form.elements.description.value.trim() : '',
        durationMinutes: Number(form.elements.durationMinutes.value),
        maxAttempts: Number(form.elements.maxAttempts.value),
        passingScore: Number(form.elements.passingScore.value),
        status: form.elements.status.value
    };
}

function teacherQuestionPayload(form) {
    var options = readTeacherQuestionOptionRows(form);
    writeTeacherQuestionOptionRows(form);
    return {
        content: form.elements.content.value.trim(),
        questionType: form.elements.questionType.value,
        points: Number(form.elements.points.value || 1),
        displayOrder: form.elements.displayOrder && form.elements.displayOrder.value
            ? Number(form.elements.displayOrder.value)
            : null,
        options: options
    };
}

function validateTeacherQuestionPayload(payload) {
    if (!payload.content) {
        return 'Question content is required.';
    }
    if (payload.options.length < 2) {
        return 'Add at least two options.';
    }
    var correctCount = payload.options.filter(function(option) { return option.correct; }).length;
    if (correctCount < 1) {
        return 'Mark at least one correct option.';
    }
    if (payload.questionType === 'SINGLE_CHOICE' && correctCount !== 1) {
        return 'Single choice questions need exactly one correct option.';
    }
    return null;
}

function initTeacherQuizBuilder() {
    var form = document.getElementById('teacher-quiz-form');
    var courseId = document.body.dataset.courseId;
    var message = document.getElementById('teacher-quiz-message');
    if (form && courseId) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            var body = teacherQuizPayload(form);
            if (!body.title || !body.lessonId) {
                teacherAssessmentMessage(message, 'Lesson ID and title are required.', 'error');
                return;
            }
            teacherFetch('/api/teacher/courses/' + encodeURIComponent(courseId) + '/quizzes', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(body)
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to create quiz');
            }).then(function() {
                teacherAssessmentMessage(message, 'Quiz created.', 'success');
                window.location.reload();
            }).catch(function(error) {
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    }

    document.querySelectorAll('.teacher-quiz-update-form').forEach(function(updateForm) {
        updateForm.addEventListener('submit', function(event) {
            event.preventDefault();
            var message = updateForm.querySelector('.assessment-message');
            var quizId = updateForm.elements.quizId.value;
            var body = teacherQuizPayload(updateForm);
            if (!body.title || !body.lessonId) {
                teacherAssessmentMessage(message, 'Lesson and title are required.', 'error');
                return;
            }
            teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(quizId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(body)
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to update quiz');
            }).then(function() {
                teacherAssessmentMessage(message, 'Quiz saved.', 'success');
                window.setTimeout(function() { window.location.reload(); }, 400);
            }).catch(function(error) {
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    });

    document.querySelectorAll('.teacher-question-form, .teacher-question-update-form').forEach(function(questionForm) {
        syncTeacherCorrectControls(questionForm);
        if (questionForm.elements.questionType) {
            questionForm.elements.questionType.addEventListener('change', function() {
                syncTeacherCorrectControls(questionForm);
            });
        }
        questionForm.querySelectorAll('[data-option-content], [data-option-correct]').forEach(function(input) {
            input.addEventListener('input', function() { writeTeacherQuestionOptionRows(questionForm); });
            input.addEventListener('change', function() { writeTeacherQuestionOptionRows(questionForm); });
        });
    });

    document.querySelectorAll('.archive-quiz-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-quiz-id]');
            if (!card || button.disabled || !window.confirm('Archive this quiz? Student history, progress, and certificates remain available.')) {
                return;
            }
            teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId) + '/archive', {
                method: 'POST',
                credentials: 'same-origin'
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to archive quiz');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                window.alert(error.message);
            });
        });
    });

    document.querySelectorAll('.teacher-question-form').forEach(function(questionForm) {
        questionForm.addEventListener('submit', function(event) {
            event.preventDefault();
            var card = questionForm.closest('[data-quiz-id]');
            var payload = teacherQuestionPayload(questionForm);
            var validationError = validateTeacherQuestionPayload(payload);
            if (validationError) {
                window.alert(validationError);
                return;
            }
            teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId) + '/questions', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(payload)
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to add question');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                window.alert(error.message);
            });
        });
    });

    document.querySelectorAll('.teacher-question-update-form').forEach(function(questionForm) {
        questionForm.addEventListener('submit', function(event) {
            event.preventDefault();
            var message = questionForm.querySelector('.assessment-message');
            var questionId = questionForm.elements.questionId.value;
            var payload = teacherQuestionPayload(questionForm);
            var validationError = validateTeacherQuestionPayload(payload);
            if (validationError) {
                teacherAssessmentMessage(message, validationError, 'error');
                return;
            }
            teacherFetch('/api/teacher/questions/' + encodeURIComponent(questionId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(payload)
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to update question');
            }).then(function() {
                teacherAssessmentMessage(message, 'Question saved.', 'success');
                window.setTimeout(function() { window.location.reload(); }, 400);
            }).catch(function(error) {
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    });

    document.querySelectorAll('.delete-question-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var row = button.closest('[data-question-id]');
            if (!row || !window.confirm('Delete this question? Questions with submitted answers are protected.')) {
                return;
            }
            teacherFetch('/api/teacher/questions/' + encodeURIComponent(row.dataset.questionId), {
                method: 'DELETE',
                credentials: 'same-origin'
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to delete question');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                window.alert(error.message);
            });
        });
    });

    document.querySelectorAll('.question-move-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-quiz-id]');
            var row = button.closest('[data-question-id]');
            var list = row ? row.parentElement : null;
            if (!card || !row || !list) {
                return;
            }
            if (button.dataset.direction === 'up' && row.previousElementSibling) {
                list.insertBefore(row, row.previousElementSibling);
            }
            if (button.dataset.direction === 'down' && row.nextElementSibling) {
                list.insertBefore(row.nextElementSibling, row);
            }
            var order = Array.prototype.map.call(list.querySelectorAll('[data-question-id]'), function(item) {
                return Number(item.dataset.questionId);
            });
            teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId) + '/questions/reorder', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(order)
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to reorder questions');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                window.alert(error.message);
            });
        });
    });
}

function initTeacherQuizActions() {
    document.querySelectorAll('.delete-quiz-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-quiz-id]');
            if (!card || button.disabled || !window.confirm('Permanently delete this quiz? This only succeeds before student attempts exist.')) {
                return;
            }
            teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId), {
                method: 'DELETE',
                credentials: 'same-origin'
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to delete quiz');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                window.alert(error.message);
            });
        });
    });

    document.querySelectorAll('.quiz-preview-toggle').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-quiz-id]');
            var preview = card ? card.querySelector('[data-quiz-preview]') : null;
            if (!preview) {
                return;
            }
            var open = preview.hidden;
            preview.hidden = !open;
            button.setAttribute('aria-expanded', String(open));
            button.textContent = open ? 'Hide Preview' : 'Preview';
        });
    });
}
