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
                return {
                    path: normalizePortalPath(new URL(clean, window.location.origin).pathname),
                    exact: exact
                };
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
        return wildcardPortalPathMatch(patternPath, currentPath)
            ? patternPath.length + 5000
            : -1;
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
    var escaped = patternPath
        .replace(/[.+?^${}()|[\]\\]/g, '\\$&')
        .replace(/\*/g, '[^/]+');
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
            var focusTarget = sidebar.querySelector(
                '[aria-current="page"], .sidebar-nav a, .sidebar-support-links a'
            );
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
    var token = tokenMeta && tokenMeta.content
        ? tokenMeta.content
        : teacherCookieValue('XSRF-TOKEN');
    var headerName = headerMeta && headerMeta.content
        ? headerMeta.content
        : 'X-XSRF-TOKEN';
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
    initTeacherQuizSettings();
    initTeacherQuizActions();
    document.querySelectorAll('[data-question-manager]').forEach(initTeacherQuestionManager);
    window.addEventListener('beforeunload', function(event) {
        if (document.querySelector('[data-question-manager][data-dirty="true"]')) {
            event.preventDefault();
            event.returnValue = '';
        }
    });
}

function teacherQuizPayload(form) {
    return {
        lessonId: Number(form.elements.lessonId.value),
        title: form.elements.title.value.trim(),
        passingScore: Number(form.elements.passingScore.value || 70),
        status: form.elements.status.value
    };
}

function initTeacherQuizSettings() {
    var createForm = document.getElementById('teacher-quiz-form');
    var courseId = document.body.dataset.courseId;
    if (createForm && courseId) {
        createForm.addEventListener('submit', function(event) {
            event.preventDefault();
            var message = document.getElementById('teacher-quiz-message');
            var payload = teacherQuizPayload(createForm);
            if (!payload.lessonId || !payload.title) {
                teacherAssessmentMessage(message, 'Lesson and title are required.', 'error');
                return;
            }
            teacherFetch('/api/teacher/courses/' + encodeURIComponent(courseId) + '/quizzes', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
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

    document.querySelectorAll('.teacher-quiz-update-form').forEach(function(form) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            var quizId = form.elements.quizId.value;
            var message = form.querySelector('.assessment-message');
            var payload = teacherQuizPayload(form);
            if (!payload.lessonId || !payload.title) {
                teacherAssessmentMessage(message, 'Lesson and title are required.', 'error');
                return;
            }
            teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(quizId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to update quiz');
            }).then(function() {
                teacherAssessmentMessage(message, 'Settings saved.', 'success');
            }).catch(function(error) {
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    });
}

function initTeacherQuizActions() {
    document.querySelectorAll('.delete-quiz-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-quiz-id]');
            if (!card || !window.confirm(
                'Delete this quiz? Quizzes with student history cannot be deleted.'
            )) {
                return;
            }
            teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId), {
                method: 'DELETE'
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to delete quiz');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                window.alert(error.message);
            });
        });
    });
}

function initTeacherQuestionManager(panel) {
    var card = panel.closest('[data-quiz-id]');
    if (!card) {
        return;
    }
    var quizId = card.dataset.quizId;
    var select = panel.querySelector('[data-question-select]');
    var previousButton = panel.querySelector('[data-question-previous]');
    var nextButton = panel.querySelector('[data-question-next]');
    var addButton = panel.querySelector('[data-add-question]');
    var pageIndicator = panel.querySelector('[data-question-page-indicator]');
    var pageSummary = panel.querySelector('[data-question-page-summary]');
    var emptyState = panel.querySelector('[data-question-empty]');
    var form = panel.querySelector('[data-question-editor-form]');
    var deleteButton = panel.querySelector('[data-delete-question]');
    var title = panel.querySelector('[data-question-editor-title]');
    var unsaved = panel.querySelector('[data-unsaved-indicator]');
    var message = form.querySelector('.assessment-message');
    var optionsContainer = panel.querySelector('[data-options-container]');
    var state = {
        page: 0,
        totalPages: 0,
        totalItems: 0,
        items: [],
        selectedId: null,
        dirty: false,
        creating: false
    };

    function markDirty(dirty) {
        state.dirty = dirty;
        panel.dataset.dirty = dirty ? 'true' : 'false';
        unsaved.hidden = !dirty;
    }

    function canLeaveEditor() {
        return !state.dirty || window.confirm('Discard unsaved question changes?');
    }

    function questionLabel(question, index) {
        var number = state.page * 10 + index + 1;
        var excerpt = String(question.content || '').replace(/\s+/g, ' ').trim();
        if (excerpt.length > 54) {
            excerpt = excerpt.substring(0, 51) + '…';
        }
        return 'Question ' + String(number).padStart(2, '0') + ' — ' + excerpt;
    }

    function optionRow(option, index) {
        var row = document.createElement('div');
        row.className = 'teacher-answer-row';
        row.setAttribute('data-option-row', '');

        var number = document.createElement('span');
        number.className = 'teacher-option-index';
        number.textContent = String(index + 1);

        var input = document.createElement('input');
        input.type = 'text';
        input.maxLength = 1000;
        input.placeholder = 'Answer ' + String(index + 1);
        input.value = option && option.content ? option.content : '';
        input.setAttribute('data-option-content', '');
        input.setAttribute('aria-label', 'Answer ' + String(index + 1));

        var correctLabel = document.createElement('label');
        correctLabel.className = 'teacher-answer-correct';
        var radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'correct-answer-' + quizId;
        radio.checked = Boolean(option && option.correct);
        radio.setAttribute('data-option-correct', '');
        var correctText = document.createElement('span');
        correctText.textContent = 'Correct';
        correctLabel.appendChild(radio);
        correctLabel.appendChild(correctText);

        row.appendChild(number);
        row.appendChild(input);
        row.appendChild(correctLabel);
        return row;
    }

    function renderOptions(options) {
        optionsContainer.textContent = '';
        var values = Array.isArray(options) ? options.slice() : [];
        while (values.length < 4) {
            values.push({ content: '', correct: values.length === 0 });
        }
        values.forEach(function(option, index) {
            optionsContainer.appendChild(optionRow(option, index));
        });
    }

    function showQuestion(question) {
        state.creating = false;
        state.selectedId = question.id;
        form.hidden = false;
        emptyState.hidden = true;
        form.elements.questionId.value = question.id;
        form.elements.content.value = question.content || '';
        title.textContent = 'Edit question';
        deleteButton.hidden = false;
        renderOptions(question.options || []);
        teacherAssessmentMessage(message, '', null);
        markDirty(false);
    }

    function showNewQuestion() {
        state.creating = true;
        state.selectedId = null;
        form.hidden = false;
        emptyState.hidden = true;
        form.elements.questionId.value = '';
        form.elements.content.value = '';
        title.textContent = 'Add question';
        deleteButton.hidden = true;
        renderOptions([]);
        teacherAssessmentMessage(message, '', null);
        markDirty(false);
        form.elements.content.focus();
    }

    function renderPage(preferredQuestionId) {
        select.textContent = '';
        state.items.forEach(function(question, index) {
            var option = document.createElement('option');
            option.value = String(question.id);
            option.textContent = questionLabel(question, index);
            select.appendChild(option);
        });
        var pageCount = Math.max(1, state.totalPages);
        pageIndicator.textContent = 'Page ' + String(state.page + 1) + ' / ' + String(pageCount);
        pageSummary.textContent = String(state.totalItems) + ' / 100 active questions';
        previousButton.disabled = state.page <= 0;
        nextButton.disabled = state.totalPages === 0 || state.page >= state.totalPages - 1;
        addButton.disabled = state.totalItems >= 100;
        select.disabled = state.items.length === 0;

        if (state.items.length === 0) {
            state.selectedId = null;
            form.hidden = true;
            emptyState.hidden = false;
            markDirty(false);
            return;
        }

        var selected = state.items.find(function(item) {
            return item.id === preferredQuestionId;
        }) || state.items[0];
        select.value = String(selected.id);
        showQuestion(selected);
    }

    function loadPage(page, preferredQuestionId) {
        pageSummary.textContent = 'Loading questions…';
        return teacherFetch(
            '/api/teacher/quizzes/' + encodeURIComponent(quizId)
                + '/questions?page=' + encodeURIComponent(Math.max(0, page)),
            { method: 'GET' }
        ).then(function(response) {
            return teacherAssessmentJson(response, 'Unable to load questions');
        }).then(function(body) {
            var data = body.data || {};
            state.page = Number(data.page || 0);
            state.totalPages = Number(data.totalPages || 0);
            state.totalItems = Number(data.totalItems || 0);
            state.items = Array.isArray(data.items) ? data.items : [];
            renderPage(preferredQuestionId);
        }).catch(function(error) {
            pageSummary.textContent = error.message;
            teacherAssessmentMessage(message, error.message, 'error');
        });
    }

    function questionPayload() {
        var options = Array.prototype.map.call(
            optionsContainer.querySelectorAll('[data-option-row]'),
            function(row) {
                return {
                    content: row.querySelector('[data-option-content]').value.trim(),
                    correct: row.querySelector('[data-option-correct]').checked
                };
            }
        ).filter(function(option) {
            return option.content;
        });
        return {
            content: form.elements.content.value.trim(),
            questionType: 'SINGLE_CHOICE',
            points: 1,
            options: options
        };
    }

    function validationError(payload) {
        if (!payload.content) {
            return 'Question content is required.';
        }
        if (payload.options.length < 2) {
            return 'Enter at least two answers.';
        }
        if (payload.options.filter(function(option) { return option.correct; }).length !== 1) {
            return 'Select exactly one correct answer.';
        }
        return null;
    }

    panel.addEventListener('input', function(event) {
        if (event.target.closest('[data-question-editor-form]')) {
            markDirty(true);
        }
    });
    panel.addEventListener('change', function(event) {
        if (event.target.closest('[data-question-editor-form]')) {
            markDirty(true);
        }
    });

    select.addEventListener('change', function() {
        var nextId = Number(select.value);
        if (!canLeaveEditor()) {
            select.value = state.selectedId == null ? '' : String(state.selectedId);
            return;
        }
        var selected = state.items.find(function(item) {
            return item.id === nextId;
        });
        if (selected) {
            showQuestion(selected);
        }
    });

    previousButton.addEventListener('click', function() {
        if (state.page > 0 && canLeaveEditor()) {
            loadPage(state.page - 1);
        }
    });
    nextButton.addEventListener('click', function() {
        if (state.page < state.totalPages - 1 && canLeaveEditor()) {
            loadPage(state.page + 1);
        }
    });
    addButton.addEventListener('click', function() {
        if (state.totalItems < 100 && canLeaveEditor()) {
            showNewQuestion();
        }
    });

    form.addEventListener('submit', function(event) {
        event.preventDefault();
        var payload = questionPayload();
        var error = validationError(payload);
        if (error) {
            teacherAssessmentMessage(message, error, 'error');
            return;
        }
        var url = state.creating
            ? '/api/teacher/quizzes/' + encodeURIComponent(quizId) + '/questions'
            : '/api/teacher/questions/' + encodeURIComponent(form.elements.questionId.value);
        teacherFetch(url, {
            method: state.creating ? 'POST' : 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(function(response) {
            return teacherAssessmentJson(response, 'Unable to save question');
        }).then(function(body) {
            var saved = body.data || {};
            markDirty(false);
            teacherAssessmentMessage(message, 'Question saved.', 'success');
            var targetPage = state.creating
                ? Math.floor(state.totalItems / 10)
                : state.page;
            return loadPage(targetPage, saved.id);
        }).catch(function(saveError) {
            teacherAssessmentMessage(message, saveError.message, 'error');
        });
    });

    deleteButton.addEventListener('click', function() {
        var questionId = Number(form.elements.questionId.value);
        if (!questionId || !window.confirm('Delete this question?')) {
            return;
        }
        teacherFetch('/api/teacher/questions/' + encodeURIComponent(questionId), {
            method: 'DELETE'
        }).then(function(response) {
            return teacherAssessmentJson(response, 'Unable to delete question');
        }).then(function() {
            markDirty(false);
            var targetPage = state.items.length === 1 && state.page > 0
                ? state.page - 1
                : state.page;
            return loadPage(targetPage);
        }).catch(function(error) {
            teacherAssessmentMessage(message, error.message, 'error');
        });
    });

    loadPage(0);
}
