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
    initTeacherAssignmentEditor();
    initTeacherTestcases();
    initTeacherGrading();
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

function teacherAssignmentPayload(form) {
    var type = form.elements.type ? form.elements.type.value : 'CODING';
    var lessonIdValue = form.elements.lessonId ? form.elements.lessonId.value : '';
    var assigneeIds = Array.prototype.slice.call(form.querySelectorAll('input[name="assigneeStudentIds"]:checked'))
        .map(function(input) { return Number(input.value); })
        .filter(function(value) { return !Number.isNaN(value); });
    var problemStatement = form.elements.problemStatement ? form.elements.problemStatement.value.trim() : '';
    if (type === 'ESSAY' && form.elements.essayPrompt) {
        problemStatement = form.elements.essayPrompt.value.trim();
    }
    return {
        lessonId: lessonIdValue ? Number(lessonIdValue) : null,
        title: form.elements.title.value.trim(),
        type: type,
        problemStatement: problemStatement,
        instructions: form.elements.instructions ? form.elements.instructions.value.trim() : '',
        starterCode: form.elements.starterCode ? form.elements.starterCode.value : '',
        allowedLanguages: form.elements.allowedLanguages ? form.elements.allowedLanguages.value.trim() : '',
        timeLimitMs: form.elements.timeLimitMs && form.elements.timeLimitMs.value ? Number(form.elements.timeLimitMs.value) : 1000,
        maxScore: Number(form.elements.maxScore.value),
        dueDate: form.elements.dueDate && form.elements.dueDate.value ? form.elements.dueDate.value : null,
        status: form.elements.status.value,
        assigneeStudentIds: assigneeIds,
        questions: type === 'MCQ' ? teacherAssignmentQuestions(form) : []
    };
}

function validateTeacherAssignmentPayload(payload) {
    if (!payload.title) {
        return 'Title is required.';
    }
    if (payload.type === 'CODING' && !payload.problemStatement) {
        return 'Problem statement is required for coding assignments.';
    }
    if (payload.type === 'CODING' && !payload.allowedLanguages) {
        return 'Allowed languages are required.';
    }
    if (payload.type === 'CODING' && (Number.isNaN(payload.timeLimitMs) || payload.timeLimitMs <= 0)) {
        return 'Time limit must be greater than zero.';
    }
    if (payload.type === 'ESSAY' && !payload.problemStatement) {
        return 'Essay prompt is required.';
    }
    if (payload.type === 'MCQ') {
        if (!payload.questions.length) {
            return 'Add at least one MCQ question.';
        }
        for (var i = 0; i < payload.questions.length; i += 1) {
            if (!payload.questions[i].content) {
                return 'Every MCQ question needs text.';
            }
            if (payload.questions[i].options.length < 2) {
                return 'Every MCQ question needs at least two options.';
            }
            if (!payload.questions[i].options.some(function(option) { return option.correct; })) {
                return 'Every MCQ question needs at least one correct option.';
            }
        }
    }
    if (Number.isNaN(payload.maxScore) || payload.maxScore <= 0) {
        return 'Max score must be greater than zero.';
    }
    return null;
}

function teacherAssignmentQuestions(form) {
    return Array.prototype.slice.call(form.querySelectorAll('[data-assignment-question]'))
        .map(function(card, index) {
            var optionRows = Array.prototype.slice.call(card.querySelectorAll('[data-assignment-option]'));
            return {
                id: card.querySelector('input[name="questionId"]') && card.querySelector('input[name="questionId"]').value
                    ? Number(card.querySelector('input[name="questionId"]').value)
                    : null,
                content: card.querySelector('[name="questionContent"]').value.trim(),
                questionType: card.querySelector('[name="questionType"]').value,
                points: Number(card.querySelector('[name="questionPoints"]').value || 1),
                displayOrder: index + 1,
                options: optionRows.map(function(row) {
                    return {
                        content: row.querySelector('[name="optionContent"]').value.trim(),
                        correct: row.querySelector('[name="optionCorrect"]').checked
                    };
                }).filter(function(option) {
                    return option.content;
                })
            };
        })
        .filter(function(question) {
            return question.content || question.options.length;
        });
}

function createAssignmentQuestionCard() {
    var card = document.createElement('article');
    card.className = 'teacher-question-card';
    card.setAttribute('data-assignment-question', '');
    card.innerHTML = [
        '<div class="assessment-grid-4">',
        '<label class="assessment-wide"><span>Question</span><input name="questionContent" placeholder="Question text"></label>',
        '<label><span>Question Type</span><select name="questionType"><option value="SINGLE_CHOICE">Single choice</option><option value="MULTIPLE_CHOICE">Multiple choice</option></select></label>',
        '<label><span>Points</span><input name="questionPoints" type="number" min="0.01" step="0.01" value="1"></label>',
        '</div>',
        '<div class="teacher-assignment-options">',
        '<label class="teacher-assignment-option" data-assignment-option><input name="optionCorrect" type="checkbox"><input name="optionContent" placeholder="Answer option"></label>',
        '<label class="teacher-assignment-option" data-assignment-option><input name="optionCorrect" type="checkbox"><input name="optionContent" placeholder="Answer option"></label>',
        '</div>',
        '<button class="btn btn-secondary remove-assignment-question" type="button">Remove</button>'
    ].join('');
    return card;
}

function refreshAssignmentTypePanels(form) {
    if (!form || !form.elements.type) {
        return;
    }
    var type = form.elements.type.value;
    document.querySelectorAll('[data-assignment-type-panel]').forEach(function(panel) {
        panel.hidden = panel.dataset.assignmentTypePanel !== type;
    });
}

function initTeacherAssignmentEditor() {
    var form = document.getElementById('teacher-assignment-form');
    var courseId = document.body.dataset.courseId;
    var message = document.getElementById('teacher-assignment-message');
    if (form) {
        refreshAssignmentTypePanels(form);
        if (form.elements.type) {
            form.elements.type.addEventListener('change', function() {
                refreshAssignmentTypePanels(form);
            });
        }
        var addQuestionButton = document.getElementById('add-assignment-question');
        var questionList = document.getElementById('assignment-question-list');
        if (addQuestionButton && questionList) {
            addQuestionButton.addEventListener('click', function() {
                questionList.appendChild(createAssignmentQuestionCard());
            });
        }
        form.addEventListener('click', function(event) {
            if (!event.target.classList.contains('remove-assignment-question')) {
                return;
            }
            var card = event.target.closest('[data-assignment-question]');
            if (card) {
                card.remove();
            }
        });
    }
    if (form && courseId) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            var assignmentId = form.dataset.assignmentId;
            var payload = teacherAssignmentPayload(form);
            var error = validateTeacherAssignmentPayload(payload);
            if (error) {
                teacherAssessmentMessage(message, error, 'error');
                return;
            }
            teacherFetch(assignmentId
                ? '/api/teacher/assignments/' + encodeURIComponent(assignmentId)
                : '/api/teacher/courses/' + encodeURIComponent(courseId) + '/assignments', {
                method: assignmentId ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(payload)
            }).then(function(response) {
                return teacherAssessmentJson(response, assignmentId ? 'Unable to update assignment' : 'Unable to create assignment');
            }).then(function() {
                teacherAssessmentMessage(message, assignmentId ? 'Assignment saved.' : 'Assignment created.', 'success');
                window.setTimeout(function() { window.location.reload(); }, 400);
            }).catch(function(error) {
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    }

    document.querySelectorAll('.delete-assignment-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            if (!form || !form.dataset.assignmentId || !window.confirm('Archive this assignment? Assignments without submissions may be deleted; submitted assignments are archived.')) {
                return;
            }
            button.disabled = true;
            teacherFetch('/api/teacher/assignments/' + encodeURIComponent(form.dataset.assignmentId), {
                method: 'DELETE',
                credentials: 'same-origin'
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to delete assignment');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                button.disabled = false;
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    });

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

function initTeacherTestcases() {
    var form = document.getElementById('teacher-testcase-form');
    var assignmentId = document.body.dataset.assignmentId;
    var message = document.getElementById('teacher-testcase-message');
    if (form && assignmentId) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            var error = validateTeacherTestcaseForm(form);
            if (error) {
                teacherAssessmentMessage(message, error, 'error');
                return;
            }
            teacherFetch('/api/teacher/assignments/' + encodeURIComponent(assignmentId) + '/testcases', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(teacherTestcasePayload(form))
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to create testcase');
            }).then(function() {
                teacherAssessmentMessage(message, 'Testcase created.', 'success');
                window.location.reload();
            }).catch(function(error) {
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    }

    document.querySelectorAll('.teacher-testcase-row').forEach(function(row) {
        row.addEventListener('submit', function(event) {
            event.preventDefault();
            var rowMessage = row.querySelector('.assessment-message');
            var error = validateTeacherTestcaseForm(row);
            if (error) {
                teacherAssessmentMessage(rowMessage, error, 'error');
                return;
            }
            teacherFetch('/api/teacher/testcases/' + encodeURIComponent(row.dataset.testcaseId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(teacherTestcasePayload(row))
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to update testcase');
            }).then(function() {
                teacherAssessmentMessage(rowMessage, 'Testcase updated.', 'success');
            }).catch(function(error) {
                teacherAssessmentMessage(rowMessage, error.message, 'error');
            });
        });

        var deleteButton = row.querySelector('.testcase-delete-btn');
        if (deleteButton) {
            deleteButton.addEventListener('click', function() {
                var rowMessage = row.querySelector('.assessment-message');
                if (!window.confirm('Delete this testcase?')) {
                    return;
                }
                deleteButton.disabled = true;
                teacherFetch('/api/teacher/testcases/' + encodeURIComponent(row.dataset.testcaseId), {
                    method: 'DELETE',
                    credentials: 'same-origin'
                }).then(function(response) {
                    return teacherAssessmentJson(response, 'Unable to delete testcase');
                }).then(function() {
                    row.remove();
                }).catch(function(error) {
                    deleteButton.disabled = false;
                    teacherAssessmentMessage(rowMessage, error.message, 'error');
                });
            });
        }
    });
}

function teacherTestcasePayload(form) {
    return {
        input: form.elements.input.value.trim(),
        expectedOutput: form.elements.expectedOutput.value.trim(),
        hidden: form.elements.hidden.checked,
        points: Number(form.elements.points.value),
        displayOrder: form.elements.displayOrder.value ? Number(form.elements.displayOrder.value) : null
    };
}

function validateTeacherTestcaseForm(form) {
    var payload = teacherTestcasePayload(form);
    if (!payload.input) {
        return 'Input is required.';
    }
    if (!payload.expectedOutput) {
        return 'Expected output is required.';
    }
    if (Number.isNaN(payload.points) || payload.points <= 0) {
        return 'Weight must be greater than zero.';
    }
    if (payload.displayOrder !== null && (Number.isNaN(payload.displayOrder) || payload.displayOrder < 0)) {
        return 'Order cannot be negative.';
    }
    return null;
}

function initTeacherGrading() {
    var viewer = document.getElementById('teacher-submission-viewer');

    function setViewerText(selector, text) {
        if (!viewer) {
            return;
        }
        var element = viewer.querySelector(selector);
        if (element) {
            element.textContent = text || '';
        }
    }

    function renderViewerStats(submission) {
        if (!viewer) {
            return;
        }
        var stats = viewer.querySelector('[data-viewer-stats]');
        if (!stats) {
            return;
        }
        stats.replaceChildren();
        [
            ['Type', submission.assignmentType || 'Assignment'],
            ['Attempt', submission.attemptNo != null ? submission.attemptNo : '1'],
            ['Status', submission.status || 'Unknown'],
            ['Score', submission.score != null ? submission.score + ' / ' + (submission.maxScore || 100) : 'Not graded'],
            ['Judge', submission.judgeStatus || 'Not run'],
            ['Tests', submission.totalTests != null ? (submission.passedTests || 0) + ' / ' + submission.totalTests : 'Not run']
        ].forEach(function(row) {
            var wrap = document.createElement('div');
            var dt = document.createElement('dt');
            var dd = document.createElement('dd');
            dt.textContent = row[0];
            dd.textContent = row[1];
            wrap.appendChild(dt);
            wrap.appendChild(dd);
            stats.appendChild(wrap);
        });
    }

    function renderSubmissionViewer(submission) {
        if (!viewer) {
            return;
        }
        viewer.hidden = false;
        setViewerText('[data-viewer-title]', submission.studentName || 'Submission');
        setViewerText('[data-viewer-meta]', (submission.courseTitle || 'Course') + ' / ' + (submission.assignmentTitle || 'Assignment'));
        setViewerText('[data-viewer-code]', submission.codeContent || 'No code submitted.');
        setViewerText('[data-viewer-output]', submission.outputLog || 'No judge output yet.');
        setViewerText('[data-viewer-text]', submission.contentText || 'No written answer.');
        setViewerText('[data-viewer-message]', '');
        renderViewerStats(submission);
        renderViewerAnswers(submission);
        viewer.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function renderViewerAnswers(submission) {
        if (!viewer) {
            return;
        }
        var target = viewer.querySelector('[data-viewer-answers]');
        if (!target) {
            return;
        }
        target.replaceChildren();
        if (!submission.answers || !submission.answers.length) {
            target.textContent = 'No MCQ answers.';
            return;
        }
        submission.answers.forEach(function(question) {
            var article = document.createElement('article');
            article.className = 'teacher-viewer-answer';
            var title = document.createElement('strong');
            title.textContent = question.content || 'Question';
            article.appendChild(title);
            var list = document.createElement('ul');
            var selected = question.selectedOptionIds || [];
            (question.options || []).forEach(function(option) {
                var item = document.createElement('li');
                var isSelected = selected.indexOf(option.id) !== -1;
                item.className = isSelected ? 'selected' : '';
                item.textContent = (option.content || 'Option') + (isSelected ? ' (selected)' : '');
                list.appendChild(item);
            });
            article.appendChild(list);
            target.appendChild(article);
        });
    }

    function loadSubmissionIntoViewer(submissionId, button) {
        if (!submissionId || !viewer) {
            return;
        }
        if (button) {
            button.disabled = true;
        }
        viewer.hidden = false;
        setViewerText('[data-viewer-message]', 'Loading submission...');
        teacherFetch('/api/teacher/submissions/' + encodeURIComponent(submissionId), {
            method: 'GET',
            credentials: 'same-origin'
        }).then(function(response) {
            return teacherAssessmentJson(response, 'Unable to load submission');
        }).then(function(apiResponse) {
            renderSubmissionViewer(apiResponse.data || {});
        }).catch(function(error) {
            setViewerText('[data-viewer-message]', error.message);
        }).finally(function() {
            if (button) {
                button.disabled = false;
            }
        });
    }

    document.querySelectorAll('.view-submission-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-submission-id]');
            if (!card || !viewer) {
                return;
            }
            loadSubmissionIntoViewer(card.dataset.submissionId, button);
        });
    });

    if (viewer) {
        var closeButton = viewer.querySelector('[data-viewer-close]');
        if (closeButton) {
            closeButton.addEventListener('click', function() {
                viewer.hidden = true;
            });
        }
        var requestedSubmissionId = new URLSearchParams(window.location.search).get('submissionId');
        if (requestedSubmissionId) {
            loadSubmissionIntoViewer(requestedSubmissionId, null);
        }
    }

    document.querySelectorAll('.teacher-grade-form').forEach(function(form) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            var card = form.closest('[data-submission-id]');
            var submitter = event.submitter || form.querySelector('[data-publish="true"]');
            var publish = submitter ? submitter.dataset.publish !== 'false' : true;
            var message = form.querySelector('.assessment-message');
            var score = Number(form.elements.score.value);
            var maxScore = Number(form.elements.score.max || 100);
            var feedback = form.elements.feedback.value.trim();
            if (Number.isNaN(score) || score < 0 || score > maxScore) {
                teacherAssessmentMessage(message, 'Score must be between 0 and ' + maxScore + '.', 'error');
                return;
            }
            if (!feedback) {
                teacherAssessmentMessage(message, 'Feedback is required.', 'error');
                return;
            }
            teacherAssessmentMessage(message, publish ? 'Publishing grade...' : 'Saving draft...', null);
            teacherFetch('/api/teacher/submissions/' + encodeURIComponent(card.dataset.submissionId) + '/grade', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({
                    score: score,
                    feedback: feedback,
                    publish: publish
                })
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to grade submission');
            }).then(function() {
                teacherAssessmentMessage(message, publish ? 'Grade published.' : 'Draft grade saved.', 'success');
                window.setTimeout(function() {
                    window.location.reload();
                }, 500);
            }).catch(function(error) {
                teacherAssessmentMessage(message, error.message, 'error');
            });
        });
    });

    document.querySelectorAll('.judge-submission-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-submission-id]');
            if (!card || !window.confirm('Send this submission to the configured code judge?')) {
                return;
            }
            button.disabled = true;
            teacherFetch('/api/teacher/submissions/' + encodeURIComponent(card.dataset.submissionId) + '/judge', {
                method: 'POST',
                credentials: 'same-origin'
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to judge submission');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                button.disabled = false;
                window.alert(error.message);
            });
        });
    });
}
