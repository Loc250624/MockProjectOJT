'use strict';
document.addEventListener('DOMContentLoaded', function () {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    sidebarLinks.forEach(function (link) {
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
            .catch(function () {
                return { message: fallbackMessage };
            })
            .then(function (body) {
                if (!response.ok) {
                    throw new Error(body.message || fallbackMessage);
                }
                return body;
            });
    }

    function refreshState() {
        return fetch('/student/courses/' + courseId + '/enrollment-state', {
            credentials: 'same-origin'
        }).then(function (response) {
            return parseJsonResponse(response, 'Unable to refresh enrollment state');
        }).then(function (apiResponse) {
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

    enrollButton.addEventListener('click', function () {
        enrollButton.disabled = true;
        enrollButton.textContent = 'Enrolling...';
        setMessage('Confirming enrollment...', false);

        fetch('/student/courses/' + courseId + '/enroll', {
            method: 'POST',
            credentials: 'same-origin'
        }).then(function (response) {
            return parseJsonResponse(response, 'Unable to enroll in this course');
        }).then(function (apiResponse) {
            setMessage(apiResponse.message || 'Enrollment confirmed', false);
            return refreshState();
        }).catch(function (error) {
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
    initStudentAssessmentPanels();
}

function parseLearningJson(response, fallbackMessage) {
    return response.json()
        .catch(function () {
            return { message: fallbackMessage };
        })
        .then(function (body) {
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

    var courseStatus = document.getElementById('course-status');
    if (courseStatus && progress.courseStatus) {
        courseStatus.textContent = progress.courseStatus === 'COMPLETED' ? 'Completed'
            : progress.courseStatus === 'IN_PROGRESS' ? 'In Progress'
                : 'Not Started';
    }

    var courseCount = document.getElementById('course-progress-count');
    if (courseCount && progress.completedLessons != null && progress.totalLessons != null) {
        courseCount.textContent = progress.completedLessons + ' of ' + progress.totalLessons + ' lessons';
    }

    var coursePercent = document.getElementById('course-progress-percent');
    if (coursePercent && progress.progressPercentage != null) {
        coursePercent.textContent = progress.progressPercentage + '%';
    }

    var help = document.getElementById('video-progress-help');
    if (help && typeof progress.lastPositionSeconds === 'number') {
        help.textContent = 'Saved at ' + progress.lastPositionSeconds + ' seconds.';
    } else if (help && typeof progress.watchedSeconds === 'number') {
        help.textContent = 'Saved through ' + progress.watchedSeconds + ' seconds.';
    }

    document.querySelectorAll('.learning-progress-summary .progress-bar-fill, .pc-progress .progress-bar-fill').forEach(function (fill) {
        if (progress.progressPercentage != null) {
            fill.style.width = progress.progressPercentage + '%';
        }
    });

    var nextLink = document.getElementById('next-lesson-link');
    if (nextLink && progress.nextLessonId != null) {
        nextLink.dataset.nextLessonId = progress.nextLessonId;
        if (progress.nextLessonAccessible) {
            nextLink.classList.remove('disabled');
            nextLink.setAttribute('aria-disabled', 'false');
            nextLink.removeAttribute('title');
            nextLink.href = '/student/learning?courseId=' + encodeURIComponent(nextLink.dataset.courseId) +
                '&lessonId=' + encodeURIComponent(progress.nextLessonId);
        } else {
            nextLink.classList.add('disabled');
            nextLink.setAttribute('aria-disabled', 'true');
            nextLink.href = '#';
            if (progress.nextLessonLockReason) {
                nextLink.title = progress.nextLessonLockReason;
            }
        }
    }
}

function Html5PlayerWrapper(video, options) {
    var self = this;
    this.video = video;

    if (options.onReady) {
        if (video.readyState >= 1) {
            options.onReady();
        } else {
            video.addEventListener('loadedmetadata', options.onReady, { once: true });
        }
    }
    if (options.onPlay) video.addEventListener('play', options.onPlay);
    if (options.onPause) video.addEventListener('pause', options.onPause);
    if (options.onTimeUpdate) {
        video.addEventListener('timeupdate', function () {
            options.onTimeUpdate(video.currentTime);
        });
    }
    if (options.onEnded) video.addEventListener('ended', options.onEnded);
    if (options.onSeeked) {
        video.addEventListener('seeked', function () {
            options.onSeeked(video.currentTime);
        });
    }
    if (options.onError) {
        video.addEventListener('error', function () {
            options.onError();
        });
    }

    this.seekTo = function (seconds) {
        video.currentTime = seconds;
    };
    this.getCurrentTime = function () {
        return video.currentTime;
    };
    this.getDuration = function () {
        return video.duration;
    };
    this.destroy = function () { };
}

function YouTubePlayerWrapper(iframe, options) {
    var self = this;
    var player = null;
    var progressInterval = null;

    function startTracking() {
        if (progressInterval) clearInterval(progressInterval);
        progressInterval = setInterval(function () {
            if (player && typeof player.getCurrentTime === 'function' && options.onTimeUpdate) {
                options.onTimeUpdate(player.getCurrentTime());
            }
        }, 1000);
    }

    function stopTracking() {
        if (progressInterval) {
            clearInterval(progressInterval);
            progressInterval = null;
        }
    }

    function onPlayerReady(event) {
        if (options.onReady) options.onReady();
    }

    function onPlayerStateChange(event) {
        if (event.data === 1) {
            startTracking();
            if (options.onPlay) options.onPlay();
        } else {
            stopTracking();
            if (event.data === 2) {
                if (options.onPause) options.onPause();
            } else if (event.data === 0) {
                if (options.onEnded) options.onEnded();
            }
        }
    }

    function createPlayer() {
        if (!iframe.id) {
            iframe.id = 'youtube-player-' + (iframe.dataset.lessonId || 'default');
        }
        player = new YT.Player(iframe.id, {
            events: {
                'onReady': onPlayerReady,
                'onStateChange': onPlayerStateChange
            }
        });
    }

    if (typeof YT === 'undefined' || typeof YT.Player === 'undefined') {
        var oldCallback = window.onYouTubeIframeAPIReady;
        window.onYouTubeIframeAPIReady = function () {
            if (oldCallback) oldCallback();
            createPlayer();
        };
        if (!document.querySelector('script[src="https://www.youtube.com/iframe_api"]')) {
            var tag = document.createElement('script');
            tag.src = "https://www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
        }
    } else {
        createPlayer();
    }

    this.seekTo = function (seconds) {
        if (player && typeof player.seekTo === 'function') {
            player.seekTo(seconds, true);
        }
    };
    this.getCurrentTime = function () {
        return player && typeof player.getCurrentTime === 'function' ? player.getCurrentTime() : 0;
    };
    this.getDuration = function () {
        return player && typeof player.getDuration === 'function' ? player.getDuration() : 0;
    };
    this.destroy = function () {
        stopTracking();
        if (player && typeof player.destroy === 'function') {
            player.destroy();
        }
    };
}

function initVideoProgress(element) {
    var courseId = element.dataset.courseId;
    var lessonId = element.dataset.lessonId;
    var highestLocal = Number(element.dataset.maxReachedSeconds || element.dataset.watchedSeconds || 0);
    var resumePosition = Number(element.dataset.lastPositionSeconds || highestLocal || 0);
    var knownDuration = Number(element.dataset.durationSeconds || 0);
    var lastSavedPosition = resumePosition;
    var lastSavedMax = highestLocal;
    var lastSentAt = 0;
    var saving = false;
    var playerWrapper = null;

    function finiteSeconds(value) {
        var numberValue = Number(value || 0);
        if (!Number.isFinite(numberValue) || numberValue < 0) {
            return 0;
        }
        return Math.floor(numberValue);
    }

    function currentDuration() {
        if (!playerWrapper) {
            return knownDuration;
        }
        var duration = finiteSeconds(playerWrapper.getDuration());
        if (duration > 0) {
            knownDuration = duration;
        }
        return knownDuration;
    }

    function buildProgressPayload(currentTime, eventType) {
        var current = finiteSeconds(currentTime);
        var duration = currentDuration();
        if (duration > 0 && current > duration) {
            current = duration;
        }
        highestLocal = Math.max(highestLocal, current);
        if (duration > 0 && highestLocal > duration) {
            highestLocal = duration;
        }
        return {
            watchedSeconds: highestLocal,
            currentTimeSeconds: current,
            maxReachedSeconds: highestLocal,
            durationSeconds: duration,
            eventType: eventType || 'TIME_UPDATE'
        };
    }

    function saveProgress(force, currentTime, eventType) {
        var current = Math.floor(currentTime || 0);
        if (!Number.isFinite(current) || current < 0) {
            return Promise.resolve();
        }
        var now = Date.now();
        var payload = buildProgressPayload(current, eventType);
        if (!force && now - lastSentAt < 10000 && payload.maxReachedSeconds - lastSavedMax < 10) {
            return Promise.resolve();
        }
        if (saving) {
            return Promise.resolve();
        }
        saving = true;
        lastSentAt = now;
        return fetch('/student/courses/' + encodeURIComponent(courseId) + '/lessons/' + encodeURIComponent(lessonId) + '/video-progress', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(payload)
        }).then(function (response) {
            return parseLearningJson(response, 'Unable to save video progress');
        }).then(function (apiResponse) {
            var data = apiResponse.data || {};
            lastSavedPosition = typeof data.lastPositionSeconds === 'number' ? data.lastPositionSeconds : payload.currentTimeSeconds;
            lastSavedMax = Math.max(lastSavedMax, data.maxReachedSeconds || data.watchedSeconds || payload.maxReachedSeconds);
            applyLearningProgress(apiResponse.data);
        }).catch(function (error) {
            var help = document.getElementById('video-progress-help');
            if (help) {
                help.textContent = error.message;
            }
        }).finally(function () {
            saving = false;
        });
    }

    function onReady() {
        if (resumePosition > 0) {
            playerWrapper.seekTo(resumePosition);
        }
    }

    function onPlay() { }

    function onPause() {
        if (playerWrapper) {
            saveProgress(true, playerWrapper.getCurrentTime(), 'PAUSE');
        }
    }

    function onTimeUpdate(currentTime) {
        saveProgress(false, currentTime, 'TIME_UPDATE');
    }

    function onSeeked(currentTime) {
        saveProgress(true, currentTime, 'SEEKED');
    }

    function onEnded() {
        if (playerWrapper) {
            saveProgress(true, playerWrapper.getDuration(), 'ENDED');
        }
    }

    function onError() {
        var errorMessage = document.getElementById('video-media-error');
        if (errorMessage) {
            errorMessage.hidden = false;
            errorMessage.textContent = 'This video cannot be loaded right now. Please try again later or contact support.';
        }
    }

    var tagName = element.tagName.toLowerCase();
    if (tagName === 'video') {
        playerWrapper = new Html5PlayerWrapper(element, {
            onReady: onReady,
            onPlay: onPlay,
            onPause: onPause,
            onTimeUpdate: onTimeUpdate,
            onEnded: onEnded,
            onSeeked: onSeeked,
            onError: onError
        });
    } else if (tagName === 'iframe') {
        playerWrapper = new YouTubePlayerWrapper(element, {
            onReady: onReady,
            onPlay: onPlay,
            onPause: onPause,
            onTimeUpdate: onTimeUpdate,
            onEnded: onEnded
        });
    }

    window.addEventListener('beforeunload', function () {
        if (playerWrapper) {
            var current = Math.floor(playerWrapper.getCurrentTime() || 0);
            var payload = buildProgressPayload(current, 'UNLOAD');
            if ((payload.maxReachedSeconds > lastSavedMax || payload.currentTimeSeconds !== lastSavedPosition) && navigator.sendBeacon) {
                var blob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
                navigator.sendBeacon('/student/courses/' + encodeURIComponent(courseId) + '/lessons/' + encodeURIComponent(lessonId) + '/video-progress', blob);
            }
        }
    });
}

function initLessonCompletion(button) {
    var message = document.getElementById('lesson-complete-message');
    button.addEventListener('click', function () {
        button.disabled = true;
        if (message) {
            message.textContent = 'Saving completion...';
        }
        fetch('/student/courses/' + encodeURIComponent(button.dataset.courseId) + '/lessons/' + encodeURIComponent(button.dataset.lessonId) + '/complete', {
            method: 'POST',
            credentials: 'same-origin'
        }).then(function (response) {
            return parseLearningJson(response, 'Unable to complete this lesson');
        }).then(function (apiResponse) {
            applyLearningProgress(apiResponse.data);
            if (message) {
                message.textContent = 'Lesson completed.';
            }
        }).catch(function (error) {
            button.disabled = false;
            if (message) {
                message.textContent = error.message;
            }
        });
    });
}

function initStudentAssessmentPanels() {
    var quizPanel = document.querySelector('[data-quiz-panel="true"]');
    var codePanel = document.querySelector('[data-code-panel="true"]');
    if (quizPanel) {
        initQuizPanel(quizPanel);
    }
    if (codePanel) {
        initCodePanel(codePanel);
    }
}

function createTextElement(tagName, className, text) {
    var element = document.createElement(tagName);
    if (className) {
        element.className = className;
    }
    element.textContent = text || '';
    return element;
}

function setPanelMessage(elementId, text, isError) {
    var element = document.getElementById(elementId);
    if (!element) {
        return;
    }
    element.textContent = text || '';
    element.style.color = isError ? 'var(--lumina-danger)' : 'var(--lumina-gray-500)';
}

function assessmentJson(response, fallbackMessage) {
    return response.json().catch(function () {
        return { message: fallbackMessage };
    }).then(function (body) {
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

function initQuizPanel(panel) {
    var courseId = panel.dataset.courseId;
    var lessonId = panel.dataset.lessonId;
    var form = document.getElementById('quiz-form');
    var loading = document.getElementById('quiz-loading');
    var unavailable = document.getElementById('quiz-unavailable');
    var questionsWrap = document.getElementById('quiz-questions');
    var saveButton = document.getElementById('quiz-save-button');
    var submitButton = document.getElementById('quiz-submit-button');
    var result = document.getElementById('quiz-result');
    var stateLabel = document.getElementById('quiz-state-label');
    var attempt = null;

    function endpoint(action) {
        return '/student/courses/' + encodeURIComponent(courseId) + '/lessons/' + encodeURIComponent(lessonId) + '/quiz' + (action || '');
    }

    function parseOptions(question) {
        try {
            var parsed = JSON.parse(question.optionsJson || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function selectedAnswers() {
        var answers = {};
        if (!attempt || !attempt.questions) {
            return answers;
        }
        attempt.questions.forEach(function (question) {
            var selector = '[name="quiz-question-' + question.id + '"]:checked';
            var checked = questionsWrap.querySelector(selector);
            if (checked) {
                answers[question.id] = checked.value;
            }
        });
        return answers;
    }

    function renderResult(data) {
        form.hidden = true;
        result.hidden = false;
        var score = data.score == null ? 'Not scored' : data.score + '%';
        var status = data.passed ? 'Passed' : 'Not passed';
        result.querySelector('span').textContent = status + '. Score: ' + score + '.';
        if (stateLabel) {
            stateLabel.textContent = data.status || 'Submitted';
        }
        if (data.passed) {
            var lessonStatus = document.getElementById('lesson-status');
            if (lessonStatus) {
                lessonStatus.textContent = 'Completed';
                lessonStatus.classList.add('completed');
            }
        }
    }

    function renderQuiz(data) {
        attempt = data;
        loading.hidden = true;
        if (data.unavailable) {
            unavailable.hidden = false;
            unavailable.querySelector('span').textContent = data.unavailableMessage || 'This quiz is not available right now.';
            if (stateLabel) {
                stateLabel.textContent = 'Unavailable';
            }
            return;
        }
        if (data.submitted) {
            renderResult(data);
            return;
        }

        questionsWrap.replaceChildren();
        (data.questions || []).forEach(function (question, index) {
            var block = document.createElement('fieldset');
            block.className = 'learning-question';
            block.appendChild(createTextElement('legend', null, (index + 1) + '. ' + (question.questionText || 'Question')));
            var options = parseOptions(question);
            if (!options.length) {
                block.appendChild(createTextElement('p', 'learning-help', 'No options are configured for this question.'));
            }
            options.forEach(function (option) {
                var label = document.createElement('label');
                label.className = 'learning-answer-option';
                var input = document.createElement('input');
                input.type = 'radio';
                input.name = 'quiz-question-' + question.id;
                input.value = String(option);
                if (data.answers && data.answers[question.id] === String(option)) {
                    input.checked = true;
                }
                label.appendChild(input);
                label.appendChild(createTextElement('span', null, String(option)));
                block.appendChild(label);
            });
            questionsWrap.appendChild(block);
        });
        form.hidden = false;
        if (stateLabel) {
            stateLabel.textContent = 'Draft';
        }
    }

    function sendQuiz(action, message) {
        if (!attempt || !attempt.attemptId) {
            return Promise.resolve();
        }
        setPanelMessage('quiz-message', message, false);
        var payload = {
            attemptId: attempt.attemptId,
            answers: selectedAnswers()
        };
        return fetch(endpoint(action), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(payload)
        }).then(function (response) {
            return parseLearningJson(response, 'Unable to update quiz attempt');
        }).then(function (apiResponse) {
            attempt = apiResponse.data;
            setPanelMessage('quiz-message', action === '/submit' ? 'Quiz submitted.' : 'Draft saved.', false);
            if (attempt.submitted) {
                renderResult(attempt);
            }
        });
    }

    fetch(endpoint(''), { credentials: 'same-origin' })
        .then(function (response) {
            return parseLearningJson(response, 'Unable to load quiz');
        })
        .then(function (apiResponse) {
            renderQuiz(apiResponse.data || {});
        })
        .catch(function (error) {
            loading.hidden = true;
            unavailable.hidden = false;
            unavailable.querySelector('span').textContent = error.message;
            if (stateLabel) stateLabel.textContent = 'Unavailable';
        });

    if (saveButton) {
        saveButton.addEventListener('click', function () {
            saveButton.disabled = true;
            sendQuiz('/save', 'Saving draft...')
                .catch(function (error) {
                    setPanelMessage('quiz-message', error.message, true);
                })
                .finally(function () {
                    saveButton.disabled = false;
                });
        });
    }
    if (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            submitButton.disabled = true;
            sendQuiz('/submit', 'Submitting quiz...')
                .catch(function (error) {
                    setPanelMessage('quiz-message', error.message, true);
                })
                .finally(function () {
                    submitButton.disabled = false;
                });
        });
    }
}

function initCodePanel(panel) {
    var courseId = panel.dataset.courseId;
    var lessonId = panel.dataset.lessonId;
    var form = document.getElementById('code-form');
    var loading = document.getElementById('code-loading');
    var unavailable = document.getElementById('code-unavailable');
    var languageSelect = document.getElementById('code-language');
    var editor = document.getElementById('code-editor');
    var problem = document.getElementById('code-problem');
    var examples = document.getElementById('code-examples');
    var timeLimit = document.getElementById('code-time-limit');
    var saveButton = document.getElementById('code-save-button');
    var submitButton = document.getElementById('code-submit-button');
    var stateLabel = document.getElementById('code-state-label');
    var assignment = null;

    function endpoint(action) {
        return '/student/courses/' + encodeURIComponent(courseId) + '/lessons/' + encodeURIComponent(lessonId) + '/coding-assignment' + (action || '');
    }

    function renderAssignment(data) {
        assignment = data;
        loading.hidden = true;
        if (data.unavailable) {
            unavailable.hidden = false;
            unavailable.querySelector('span').textContent = data.unavailableMessage || 'This exercise is not available right now.';
            if (stateLabel) stateLabel.textContent = 'Unavailable';
            return;
        }

        languageSelect.replaceChildren();
        (data.allowedLanguages || []).forEach(function (language) {
            var option = document.createElement('option');
            option.value = language;
            option.textContent = language;
            languageSelect.appendChild(option);
        });
        if (data.submittedLanguage) {
            languageSelect.value = data.submittedLanguage;
        }
        problem.textContent = data.problemStatement || 'No instructions are configured for this exercise.';
        editor.value = data.submittedCode || data.starterCode || '';
        timeLimit.textContent = data.timeLimitMs ? 'Time limit: ' + data.timeLimitMs + ' ms' : '';
        examples.replaceChildren();
        (data.examples || []).forEach(function (example, index) {
            var item = document.createElement('div');
            item.className = 'learning-code-example';
            item.appendChild(createTextElement('strong', null, 'Example ' + (index + 1)));
            item.appendChild(createTextElement('pre', null, example.inputData || ''));
            examples.appendChild(item);
        });
        if (data.submitted) {
            editor.readOnly = true;
            languageSelect.disabled = true;
            saveButton.disabled = true;
            submitButton.disabled = true;
            setPanelMessage('code-message', data.status === 'PENDING_REVIEW' ? 'Submitted for review.' : 'Submission reviewed.', false);
        } else {
            editor.readOnly = false;
            languageSelect.disabled = false;
            saveButton.disabled = false;
            submitButton.disabled = false;
        }
        if (stateLabel) {
            stateLabel.textContent = data.status || data.submissionState || 'Draft';
        }
        form.hidden = false;
    }

    function codePayload() {
        return {
            submissionId: assignment ? assignment.submissionId : null,
            language: languageSelect.value,
            code: editor.value
        };
    }

    function sendCode(action, message) {
        setPanelMessage('code-message', message, false);
        return fetch(endpoint(action), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(codePayload())
        }).then(function (response) {
            return parseLearningJson(response, 'Unable to update code submission');
        }).then(function (apiResponse) {
            assignment = apiResponse.data;
            renderAssignment(assignment);
            setPanelMessage('code-message', action === '/submit' ? 'Submitted for review.' : 'Draft saved.', false);
        });
    }

    fetch(endpoint(''), { credentials: 'same-origin' })
        .then(function (response) {
            return parseLearningJson(response, 'Unable to load coding exercise');
        })
        .then(function (apiResponse) {
            renderAssignment(apiResponse.data || {});
        })
        .catch(function (error) {
            loading.hidden = true;
            unavailable.hidden = false;
            unavailable.querySelector('span').textContent = error.message;
            if (stateLabel) stateLabel.textContent = 'Unavailable';
        });

    if (saveButton) {
        saveButton.addEventListener('click', function () {
            saveButton.disabled = true;
            sendCode('/save', 'Saving draft...')
                .catch(function (error) {
                    setPanelMessage('code-message', error.message, true);
                })
                .finally(function () {
                    if (!assignment || !assignment.submitted) {
                        saveButton.disabled = false;
                    }
                });
        });
    }
    if (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            submitButton.disabled = true;
            sendCode('/submit', 'Submitting code...')
                .catch(function (error) {
                    setPanelMessage('code-message', error.message, true);
                    submitButton.disabled = false;
                });
        });
    }

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
        questions.forEach(function (question, questionIndex) {
            question.classList.toggle('active', questionIndex === current);
        });
        navButtons.forEach(function (button, buttonIndex) {
            button.classList.toggle('active', buttonIndex === current);
        });
    }

    function collectAnswers() {
        return questions.map(function (question) {
            return {
                questionId: Number(question.dataset.questionId),
                selectedOptionIds: Array.prototype.slice.call(question.querySelectorAll('input:checked')).map(function (input) {
                    return Number(input.value);
                })
            };
        });
    }

    function markAnswered() {
        questions.forEach(function (question, index) {
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
        }).then(function (response) {
            return assessmentJson(response, 'Unable to start quiz');
        }).then(function (body) {
            attemptId = body.data.id;
            setAssessmentMessage(saveState, 'Draft active', null);
            return attemptId;
        });
    }

    function saveDraft() {
        return ensureAttempt().then(function (id) {
            return fetch('/api/student/quiz-attempts/' + encodeURIComponent(id) + '/draft', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({ answers: collectAnswers() })
            });
        }).then(function (response) {
            return assessmentJson(response, 'Unable to save draft');
        }).then(function () {
            setAssessmentMessage(message, 'Draft saved.', 'success');
            setAssessmentMessage(saveState, 'Saved', null);
        }).catch(function (error) {
            setAssessmentMessage(message, error.message, 'error');
        });
    }

    navButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            showQuestion(Number(button.dataset.questionIndex));
        });
    });
    document.querySelectorAll('.quiz-option input').forEach(function (input) {
        input.addEventListener('change', function () {
            markAnswered();
            input.closest('.quiz-options').querySelectorAll('.quiz-option').forEach(function (option) {
                option.classList.toggle('selected', option.querySelector('input:checked') !== null);
            });
        });
    });
    var previous = document.getElementById('previous-question-btn');
    if (previous) {
        previous.addEventListener('click', function () { showQuestion(current - 1); });
    }
    var next = document.getElementById('next-question-btn');
    if (next) {
        next.addEventListener('click', function () { showQuestion(current + 1); });
    }
    var save = document.getElementById('save-quiz-btn');
    if (save) {
        save.addEventListener('click', saveDraft);
    }
    var submit = document.getElementById('submit-quiz-btn');
    if (submit) {
        submit.addEventListener('click', function () {
            if (!window.confirm('Submit this quiz attempt? You cannot edit it after submission.')) {
                return;
            }
            ensureAttempt().then(function (id) {
                submit.disabled = true;
                return fetch('/api/student/quiz-attempts/' + encodeURIComponent(id) + '/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ answers: collectAnswers() })
                });
            }).then(function (response) {
                return assessmentJson(response, 'Unable to submit quiz');
            }).then(function (body) {
                window.location.href = '/student/quizzes/' + encodeURIComponent(body.data.id) + '/result';
            }).catch(function (error) {
                submit.disabled = false;
                setAssessmentMessage(message, error.message, 'error');
            });
        });
    }
    markAnswered();
    showQuestion(0);
    ensureAttempt().catch(function (error) {
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
        }).then(function (response) {
            return assessmentJson(response, 'Unable to save submission');
        }).then(function (apiResponse) {
            setAssessmentMessage(message, apiResponse.message, 'success');
            if (kind === 'submit' && apiResponse.data && apiResponse.data.id) {
                window.location.href = '/student/submissions/' + encodeURIComponent(apiResponse.data.id) + '/result';
            }
        }).catch(function (error) {
            setAssessmentMessage(message, error.message, 'error');
        });
    }

    if (draftButton) {
        draftButton.addEventListener('click', function () { send('draft'); });
    }
    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (window.confirm('Submit this assignment?')) {
            send('submit');
        }
    });

}


function switchProfileTab(tabName) {
    var tabs = document.querySelectorAll('[data-profile-tab]');
    var panels = document.querySelectorAll('[data-profile-panel]');
    tabs.forEach(function (tab) {
        tab.classList.toggle('active', tab.dataset.profileTab === tabName);
    });
    panels.forEach(function (panel) {
        panel.classList.toggle('active', panel.dataset.profilePanel === tabName);
    });
}

function initProfileTabs() {
    var tabs = document.querySelectorAll('[data-profile-tab]');
    if (!tabs.length) {
        return;
    }

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
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
        document.querySelectorAll('[data-profile-avatar="true"]').forEach(function (image) {
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
            .catch(function () {
                return { message: fallbackMessage };
            })
            .then(function (body) {
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
        probe.onload = function () {
            clearAvatarChoice();
            selectedAvatarUrl = normalizedUrl;
            if (avatarUrlInput) avatarUrlInput.value = normalizedUrl;
            if (avatarFileName) avatarFileName.textContent = 'Using image URL';
            setAvatarPreview(normalizedUrl);
            setMessage('Preview loaded. Click Save to fetch and store the image.', 'loading');
        };
        probe.onerror = function () {
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
        }).then(function (response) {
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
        }).then(function (response) {
            return parseJsonResponse(response, 'Unable to upload avatar');
        });
    }

    function useAvatarUrl(url) {
        return fetch('/api/profile/avatar/url', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ imageUrl: url })
        }).then(function (response) {
            return parseJsonResponse(response, 'Unable to update avatar from URL');
        });
    }

    function removeAvatar() {
        return fetch('/api/profile/avatar', {
            method: 'DELETE',
            credentials: 'same-origin'
        }).then(function (response) {
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

    cancelButton.addEventListener('click', function () {
        setAvatarPreview(currentAvatarSrc);
        closeForm();
        setMessage('', 'loading');
        switchProfileTab('overview');
    });

    avatarInput.addEventListener('change', function () {
        setSelectedAvatarFile(avatarInput.files && avatarInput.files[0]);
    });

    avatarUrlButton.addEventListener('click', function () {
        avatarUrlPanel.hidden = !avatarUrlPanel.hidden;
        if (!avatarUrlPanel.hidden) {
            avatarUrlInput.focus();
        }
    });

    avatarUrlPreviewButton.addEventListener('click', function () {
        setSelectedAvatarUrl(avatarUrlInput.value);
    });

    removeAvatarButton.addEventListener('click', function () {
        clearAvatarChoice();
        shouldRemoveAvatar = true;
        setAvatarPreview(placeholderSrc);
        setMessage('Photo will be removed after you click Save.', 'loading');
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        setLoading(true);

        updateProfile()
            .then(function (profileResponse) {
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
            .then(function (apiResponse) {
                if (!apiResponse.data) {
                    throw new Error(apiResponse.message || 'Unable to update profile');
                }
                applyProfile(apiResponse.data);
                closeForm();
                setMessage(apiResponse.message || 'Profile updated successfully', 'success');
                switchProfileTab('overview');
            })
            .catch(function (error) {
                setMessage(error.message, 'error');
            })
            .finally(function () {
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
        cards.forEach(function (card) {
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
            .catch(function () {
                return { message: fallbackMessage };
            })
            .then(function (body) {
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
        download.addEventListener('click', function () {
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
        items.forEach(function (certificate) {
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
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('Unable to download this certificate.');
            }
            return response.blob();
        }).then(function (blob) {
            var url = URL.createObjectURL(blob);
            var anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = 'lumina-certificate-' + certificateId + '.pdf';
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            URL.revokeObjectURL(url);
        }).catch(function (error) {
            setMessage(error.message, true);
        }).finally(function () {
            button.disabled = false;
            button.textContent = original;
        });
    }

    fetch('/api/student/certificates', {
        credentials: 'same-origin'
    }).then(function (response) {
        return parseJsonResponse(response, 'Unable to load certificates');
    }).then(function (apiResponse) {
        var data = apiResponse.data || {};
        renderCertificates(data.content || [], data.totalElements || 0);
        setMessage('', false);
    }).catch(function (error) {
        setMessage(error.message, true);
        empty.hidden = false;
    }).finally(function () {
        loading.hidden = true;
    });
}
