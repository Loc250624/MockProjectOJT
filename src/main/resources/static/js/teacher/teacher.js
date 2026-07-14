'use strict';
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    sidebarLinks.forEach(function(link) {
        var linkPath = new URL(link.getAttribute('href'), window.location.origin).pathname;
        if (linkPath === currentPath || (linkPath === '/teacher/analytics' && currentPath === '/instructor/analytics')) {
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

function initTeacherQuizBuilder() {
    var form = document.getElementById('teacher-quiz-form');
    var courseId = document.body.dataset.courseId;
    var message = document.getElementById('teacher-quiz-message');
    if (form && courseId) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            var body = {
                lessonId: Number(form.elements.lessonId.value),
                title: form.elements.title.value.trim(),
                description: form.elements.description.value.trim(),
                durationMinutes: Number(form.elements.durationMinutes.value),
                maxAttempts: Number(form.elements.maxAttempts.value),
                passingScore: Number(form.elements.passingScore.value),
                status: form.elements.status.value
            };
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

    document.querySelectorAll('.delete-quiz-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-quiz-id]');
            if (!card || !window.confirm('Delete this quiz?')) {
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
            var options = questionForm.elements.options.value.split('|').map(function(raw) {
                var text = raw.trim();
                var correct = text.charAt(0) === '*';
                return { content: correct ? text.substring(1).trim() : text, correct: correct };
            }).filter(function(option) { return option.content; });
            if (!options.length || !options.some(function(option) { return option.correct; })) {
                window.alert('Add at least one option and mark a correct option with *.');
                return;
            }
            fetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId) + '/questions', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({
                    content: questionForm.elements.content.value.trim(),
                    questionType: questionForm.elements.questionType.value,
                    points: Number(questionForm.elements.points.value || 1),
                    options: options
                })
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to add question');
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
    if (!form || !assignmentId) {
        return;
    }
    form.addEventListener('submit', function(event) {
        event.preventDefault();
        fetch('/api/teacher/assignments/' + encodeURIComponent(assignmentId) + '/testcases', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
                input: form.elements.input.value.trim(),
                expectedOutput: form.elements.expectedOutput.value.trim(),
                hidden: form.elements.hidden.checked,
                points: Number(form.elements.points.value || 1),
                displayOrder: form.elements.displayOrder.value ? Number(form.elements.displayOrder.value) : null
            })
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

function initTeacherGrading() {
    document.querySelectorAll('.teacher-grade-form').forEach(function(form) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            var card = form.closest('[data-submission-id]');
            fetch('/api/teacher/submissions/' + encodeURIComponent(card.dataset.submissionId) + '/grade', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({
                    score: Number(form.elements.score.value),
                    feedback: form.elements.feedback.value.trim()
                })
            }).then(function(response) {
                return teacherAssessmentJson(response, 'Unable to grade submission');
            }).then(function() {
                window.location.reload();
            }).catch(function(error) {
                window.alert(error.message);
            });
        });
    });

    document.querySelectorAll('.judge-submission-btn').forEach(function(button) {
        button.addEventListener('click', function() {
            var card = button.closest('[data-submission-id]');
            if (!card || !window.confirm('Run the safe mock judge for this submission?')) {
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
