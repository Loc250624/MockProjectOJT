'use strict';
console.log('Teacher JS loaded');
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    sidebarLinks.forEach(function(link) {
        var linkPath = new URL(link.getAttribute('href'), window.location.origin).pathname;
        if (linkPath === currentPath) {
            link.classList.add('active');
        }
    });
    initTeacherAssessments();
});

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
    initTeacherTestcases();
    initTeacherGrading();
}

function parseTeacherQuestionOptions(rawValue) {
    return (rawValue || '').split('|').map(function(raw) {
        var text = raw.trim();
        var correct = text.charAt(0) === '*';
        return { content: correct ? text.substring(1).trim() : text, correct: correct };
    }).filter(function(option) { return option.content; });
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
    var options = parseTeacherQuestionOptions(form.elements.options.value);
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
        return 'Mark at least one correct option with *.';
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
            fetch('/api/teacher/courses/' + encodeURIComponent(courseId) + '/quizzes', {
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
            fetch('/api/teacher/quizzes/' + encodeURIComponent(quizId), {
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

    document.querySelectorAll('.delete-quiz-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-quiz-id]');
            if (!card || button.disabled || !window.confirm('Archive this quiz? Quizzes without attempts may be deleted; attempted quizzes are archived.')) {
                return;
            }
            fetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId), {
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
            fetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId) + '/questions', {
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
            fetch('/api/teacher/questions/' + encodeURIComponent(questionId), {
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
            fetch('/api/teacher/questions/' + encodeURIComponent(row.dataset.questionId), {
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
            fetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId) + '/questions/reorder', {
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
            fetch('/api/teacher/assignments/' + encodeURIComponent(assignmentId) + '/testcases', {
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
            fetch('/api/teacher/testcases/' + encodeURIComponent(row.dataset.testcaseId), {
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
                fetch('/api/teacher/testcases/' + encodeURIComponent(row.dataset.testcaseId), {
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
            fetch('/api/teacher/submissions/' + encodeURIComponent(card.dataset.submissionId) + '/grade', {
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
            fetch('/api/teacher/submissions/' + encodeURIComponent(card.dataset.submissionId) + '/judge', {
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
