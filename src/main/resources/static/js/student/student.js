'use strict';
document.addEventListener('DOMContentLoaded', function () {
    var currentPath = window.location.pathname;
    initPortalSidebarNav();
    initPortalSidebarDrawer();

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
    initAiTutor();
    initStudentCertificates();
    initAssessmentQuiz();
    initAssessmentSubmission();
});

function initPortalSidebarNav() {
    var currentPath = normalizePortalPath(window.location.pathname);
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-support-links a');
    var activeLink = null;
    var activeScore = -1;

    sidebarLinks.forEach(function (link) {
        link.classList.remove('active');
        link.removeAttribute('aria-current');

        getPortalNavPatterns(link).forEach(function (pattern) {
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
        .map(function (value) {
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

    toggle.addEventListener('click', function () {
        setOpen(!portal.classList.contains('sidebar-open'));
    });
    backdrop.addEventListener('click', function () {
        setOpen(false);
    });
    sidebar.addEventListener('click', function (event) {
        if (mediaQuery.matches && event.target.closest('a')) {
            setOpen(false);
        }
    });
    document.addEventListener('keydown', function (event) {
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
            window.location.href = '/student/my-courses';
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

function initAiTutor() {
    var root = document.querySelector('[data-ai-tutor]');
    if (!root) {
        return;
    }

    var lessonId = root.dataset.lessonId;
    var toggle = root.querySelector('[data-ai-tutor-toggle]');
    var panel = root.querySelector('[data-ai-tutor-panel]');
    var closeButton = root.querySelector('[data-ai-tutor-close]');
    var messages = root.querySelector('[data-ai-tutor-messages]');
    var form = root.querySelector('[data-ai-tutor-form]');
    var input = root.querySelector('[data-ai-tutor-input]');
    var sendButton = root.querySelector('[data-ai-tutor-send]');
    var status = root.querySelector('[data-ai-tutor-status]');
    var quickActions = Array.from(root.querySelectorAll('[data-ai-tutor-action]'));
    var history = [];
    var sending = false;

    if (!lessonId || !toggle || !panel || !form || !input || !messages) {
        return;
    }

    appendMessage('assistant', 'I am ready to help with this lesson. Would you like a summary, a simpler explanation, an example, or a quick check?');

    toggle.addEventListener('click', function () {
        setPanelOpen(panel.hidden);
    });
    if (closeButton) {
        closeButton.addEventListener('click', function () {
            setPanelOpen(false);
            toggle.focus();
        });
    }
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !panel.hidden) {
            setPanelOpen(false);
            toggle.focus();
        }
    });

    quickActions.forEach(function (button) {
        button.addEventListener('click', function () {
            if (sending) {
                return;
            }
            var action = button.dataset.aiTutorAction || '';
            sendChat(button.textContent.trim(), action);
        });
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (sending) {
            return;
        }
        sendChat(input.value, '');
    });

    input.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    function setPanelOpen(isOpen) {
        panel.hidden = !isOpen;
        toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        if (isOpen) {
            input.focus();
            scrollMessages();
        }
    }

    function setLoading(isLoading) {
        sending = isLoading;
        input.disabled = isLoading;
        if (sendButton) {
            sendButton.disabled = isLoading;
        }
        quickActions.forEach(function (button) {
            button.disabled = isLoading;
        });
        if (isLoading) {
            setStatus('Thinking...', false);
        }
    }

    function setStatus(text, isError) {
        if (!status) {
            return;
        }
        status.textContent = text || '';
        status.classList.toggle('error', Boolean(isError));
    }

    function sendChat(rawMessage, action) {
        var message = String(rawMessage || '').trim();
        if (!message) {
            setStatus('Enter a question about the current lesson.', true);
            input.focus();
            return;
        }
        if (message.length > 1200) {
            setStatus('Your question is too long. Please shorten it.', true);
            input.focus();
            return;
        }

        setPanelOpen(true);
        var requestHistory = history.slice(-6);
        appendMessage('user', message);
        history.push({ role: 'user', content: message });
        input.value = '';
        setLoading(true);

        fetch('/api/student/ai-tutor/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
                lessonId: Number(lessonId),
                message: message,
                action: action || '',
                history: requestHistory
            })
        }).then(function (response) {
            return response.json()
                .catch(function () {
                    return { message: 'AI Tutor could not respond. Please try again later.' };
                })
                .then(function (body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'AI Tutor could not respond. Please try again later.');
                    }
                    return body;
                });
        }).then(function (apiResponse) {
            var data = apiResponse.data || {};
            var answer = data.answer || 'AI Tutor does not have an answer yet.';
            appendMessage('assistant', answer);
            history.push({ role: 'assistant', content: answer });
            history = history.slice(-8);
            setStatus(data.refused ? 'Outside the current lesson scope.' : '', Boolean(data.refused));
        }).catch(function (error) {
            history = history.slice(0, -1);
            setStatus(error.message || 'AI Tutor could not respond. Please try again later.', true);
        }).finally(function () {
            setLoading(false);
            input.focus();
        });
    }

    function appendMessage(role, text) {
        var item = document.createElement('div');
        item.className = 'ai-tutor-message ' + (role === 'user' ? 'user' : 'assistant');
        var label = document.createElement('strong');
        label.textContent = role === 'user' ? 'You' : 'AI Tutor';
        var bubble = document.createElement('div');
        bubble.className = 'ai-tutor-bubble';
        bubble.textContent = text || '';
        item.appendChild(label);
        item.appendChild(bubble);
        messages.appendChild(item);
        scrollMessages();
    }

    function scrollMessages() {
        messages.scrollTop = messages.scrollHeight;
    }
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
    var completionRequestSent = false;
    var pendingCompletionSave = false;
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
        var completionEvent = payload.eventType === 'ENDED'
            || (payload.durationSeconds > 0 && payload.maxReachedSeconds * 100 >= payload.durationSeconds * 90);
        if (completionEvent && completionRequestSent) {
            return Promise.resolve();
        }
        if (!force && now - lastSentAt < 10000 && payload.maxReachedSeconds - lastSavedMax < 10) {
            return Promise.resolve();
        }
        if (saving) {
            if (completionEvent) {
                pendingCompletionSave = true;
            }
            return Promise.resolve();
        }
        saving = true;
        if (completionEvent) {
            completionRequestSent = true;
        }
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
            if (completionEvent && !data.completed) {
                completionRequestSent = false;
            }
            applyLearningProgress(apiResponse.data);
        }).catch(function (error) {
            if (completionEvent) {
                completionRequestSent = false;
            }
            var help = document.getElementById('video-progress-help');
            if (help) {
                help.textContent = error.message;
            }
        }).finally(function () {
            saving = false;
            if (pendingCompletionSave) {
                pendingCompletionSave = false;
                saveProgress(true, currentDuration(), 'ENDED');
            }
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
        var duration = currentDuration();
        var nearCompletion = duration > 0 && finiteSeconds(currentTime) * 100 >= duration * 90;
        saveProgress(nearCompletion, currentTime, 'TIME_UPDATE');
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
    var review = document.getElementById('quiz-review');
    var retakeButton = document.getElementById('quiz-retake-button');
    var stateLabel = document.getElementById('quiz-state-label');
    var attempt = null;

    function endpoint(action) {
        return '/student/courses/' + encodeURIComponent(courseId) + '/lessons/' + encodeURIComponent(lessonId) + '/quiz' + (action || '');
    }

    function parseOptions(question) {
        try {
            var parsed = JSON.parse(question.optionsJson || '[]');
            if (!Array.isArray(parsed)) {
                return [];
            }
            return parsed.map(function(option) {
                if (option && typeof option === 'object') {
                    return {
                        content: String(option.content || ''),
                        correct: option.correct === true
                    };
                }
                return { content: String(option), correct: null };
            }).filter(function(option) { return option.content; });
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
        if (retakeButton) {
            retakeButton.disabled = false;
            retakeButton.hidden = false;
        }
        var score = data.score == null ? 'Not scored' : data.score + '%';
        var status = data.passed ? 'Passed' : 'Failed';
        result.querySelector('span').textContent = status + '. Score: ' + score + '.';
        result.classList.toggle('passed', data.passed === true);
        result.classList.toggle('failed', data.passed !== true);
        renderQuizReview(data);
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
        applyLearningProgress(data.learningProgress);
    }

    function renderQuizReview(data) {
        if (!review) {
            return;
        }
        review.replaceChildren();
        (data.questions || []).forEach(function(question, index) {
            var item = document.createElement('div');
            item.className = 'learning-result-item';
            item.appendChild(createTextElement('strong', null, (index + 1) + '. ' + (question.questionText || 'Question')));
            var selected = data.answers && data.answers[question.id] ? String(data.answers[question.id]) : 'No answer';
            var options = parseOptions(question);
            var selectedOption = options.filter(function(option) { return option.content === selected; })[0];
            if (selectedOption && selectedOption.correct === true) {
                item.classList.add('correct');
            } else if (selectedOption && selectedOption.correct === false) {
                item.classList.add('wrong');
            }
            item.appendChild(createTextElement('span', null, 'Selected: ' + selected));
            review.appendChild(item);
        });
    }

    function renderQuiz(data) {
        attempt = data;
        loading.hidden = true;
        if (data.unavailable) {
            result.hidden = true;
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

        unavailable.hidden = true;
        result.hidden = true;
        if (retakeButton) {
            retakeButton.hidden = true;
            retakeButton.disabled = false;
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
            options.slice(0, 4).forEach(function (option) {
                var label = document.createElement('label');
                label.className = 'learning-answer-option';
                var input = document.createElement('input');
                input.type = 'radio';
                input.name = 'quiz-question-' + question.id;
                input.value = option.content;
                if (data.answers && data.answers[question.id] === option.content) {
                    input.checked = true;
                    label.classList.add('selected');
                }
                label.appendChild(input);
                label.appendChild(createTextElement('span', null, option.content));
                input.addEventListener('change', function() {
                    block.querySelectorAll('.learning-answer-option').forEach(function(optionLabel) {
                        optionLabel.classList.toggle('selected', optionLabel.querySelector('input:checked') !== null);
                    });
                });
                block.appendChild(label);
            });
            questionsWrap.appendChild(block);
        });
        form.hidden = false;
        setPanelMessage('quiz-message', '', false);
        if (stateLabel) {
            stateLabel.textContent = 'Draft';
        }
    }

    function loadQuiz(message) {
        if (message) {
            setPanelMessage('quiz-message', message, false);
        }
        return fetch(endpoint(''), { credentials: 'same-origin' })
            .then(function(response) {
                return parseLearningJson(response, 'Unable to load quiz');
            })
            .then(function(apiResponse) {
                renderQuiz(apiResponse.data || {});
            });
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

    loadQuiz()
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
    if (retakeButton) {
        retakeButton.addEventListener('click', function() {
            retakeButton.disabled = true;
            loading.hidden = false;
            loadQuiz('Starting a new attempt...')
                .catch(function(error) {
                    setPanelMessage('quiz-message', error.message, true);
                    retakeButton.disabled = false;
                })
                .finally(function() {
                    loading.hidden = true;
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
    var runButton = document.getElementById('code-run-button');
    var submitButton = document.getElementById('code-submit-button');
    var codeResult = document.getElementById('code-result');
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
        editor.readOnly = false;
        languageSelect.disabled = false;
        saveButton.disabled = false;
        if (runButton) runButton.disabled = false;
        submitButton.disabled = false;
        if (data.submitted) {
            setPanelMessage('code-message', data.status || data.submissionState || 'Submitted', false);
        }
        renderCodeResult(data);
        if (stateLabel) {
            stateLabel.textContent = data.status || data.submissionState || 'Draft';
        }
        form.hidden = false;
    }

    function renderCodeResult(data) {
        if (!codeResult) {
            return;
        }
        var hasResult = data.judgeStatus || data.outputLog || data.totalTests != null;
        codeResult.hidden = !hasResult;
        if (!hasResult) {
            return;
        }
        codeResult.classList.toggle('passed', data.judgeStatus === 'PASSED');
        codeResult.classList.toggle('failed', data.judgeStatus && data.judgeStatus !== 'PASSED');
        var summary = data.judgeStatus || 'Run complete';
        if (data.totalTests != null) {
            summary += ' - ' + (data.passedTests || 0) + ' / ' + data.totalTests + ' tests';
        }
        codeResult.querySelector('span').textContent = summary;
        codeResult.querySelector('pre').textContent = data.outputLog || '';
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
            applyLearningProgress(assignment && assignment.learningProgress);
            setPanelMessage('code-message', (apiResponse.data && (apiResponse.data.status || apiResponse.data.submissionState))
                || (action === '/run' ? 'Code run complete.' : 'Draft saved.'), false);
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
                    saveButton.disabled = false;
                });
        });
    }
    if (runButton) {
        runButton.addEventListener('click', function() {
            runButton.disabled = true;
            sendCode('/run', 'Running code...')
                .catch(function(error) {
                    setPanelMessage('code-message', error.message, true);
                })
                .finally(function() {
                    runButton.disabled = false;
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
    var assignmentType = page && page.dataset.assignmentType ? page.dataset.assignmentType : 'CODING';
    var message = document.getElementById('assignment-message');
    var draftButton = document.getElementById('save-assignment-draft');
    var runButton = document.getElementById('run-assignment-code');
    var runResult = document.getElementById('assignment-run-result');

    function fieldValue(name) {
        return form.elements[name] ? form.elements[name].value.trim() : '';
    }

    function mcqAnswers() {
        return Array.prototype.slice.call(form.querySelectorAll('[data-assignment-answer]')).map(function(question) {
            return {
                questionId: Number(question.dataset.assignmentAnswer),
                selectedOptionIds: Array.prototype.slice.call(question.querySelectorAll('input:checked'))
                    .map(function(input) { return Number(input.value); })
                    .filter(function(value) { return !Number.isNaN(value); })
            };
        });
    }

    function payload() {
        return {
            contentText: fieldValue('contentText'),
            codeLanguage: fieldValue('codeLanguage'),
            codeContent: fieldValue('codeContent'),
            filePath: fieldValue('filePath'),
            answers: assignmentType === 'MCQ' ? mcqAnswers() : []
        };
    }

    function renderRunResult(data) {
        if (!runResult || !data) {
            return;
        }
        var hasResult = data.judgeStatus || data.outputLog || data.totalTests != null;
        runResult.hidden = !hasResult;
        if (!hasResult) {
            return;
        }
        runResult.classList.toggle('passed', data.judgeStatus === 'PASSED');
        runResult.classList.toggle('failed', data.judgeStatus && data.judgeStatus !== 'PASSED');
        var summary = data.judgeStatus || 'Run complete';
        if (data.totalTests != null) {
            summary += ' - ' + (data.passedTests || 0) + ' / ' + data.totalTests + ' tests';
        }
        runResult.querySelector('span').textContent = summary;
        runResult.querySelector('pre').textContent = data.outputLog || '';
    }

    function send(kind) {
        var body = payload();
        if (kind === 'run' && assignmentType !== 'CODING') {
            setAssessmentMessage(message, 'Only coding assignments can be run.', 'error');
            return Promise.resolve();
        }
        if (kind === 'submit' && assignmentType === 'MCQ' && body.answers.some(function(answer) {
            return !answer.selectedOptionIds.length;
        })) {
            setAssessmentMessage(message, 'Please answer every MCQ question before submitting.', 'error');
            return Promise.resolve();
        }
        if (kind === 'submit' && assignmentType === 'ESSAY' && !body.contentText && !body.filePath) {
            setAssessmentMessage(message, 'Please provide an essay response or file path before submitting.', 'error');
            return Promise.resolve();
        }
        if (kind === 'submit' && assignmentType === 'CODING' && !body.codeContent && !body.filePath) {
            setAssessmentMessage(message, 'Please provide code or a file path before submitting.', 'error');
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
            renderRunResult(apiResponse.data);
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
    if (runButton) {
        runButton.addEventListener('click', function() {
            runButton.disabled = true;
            send('run').finally(function() {
                runButton.disabled = false;
            });
        });
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

    function appendIconLabel(parent, iconName, label) {
        var icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        icon.setAttribute('class', 'lumina-icon');
        icon.setAttribute('viewBox', '0 0 24 24');
        icon.setAttribute('fill', 'none');
        icon.setAttribute('stroke', 'currentColor');
        icon.setAttribute('stroke-width', '2');
        icon.setAttribute('stroke-linecap', 'round');
        icon.setAttribute('stroke-linejoin', 'round');
        icon.setAttribute('aria-hidden', 'true');
        var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', iconName === 'view' ? 'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z' : 'M12 4v12 m5-5-5 5-5-5 M5 20h14');
        icon.appendChild(path);
        if (iconName === 'view') {
            var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttribute('cx', '12');
            circle.setAttribute('cy', '12');
            circle.setAttribute('r', '3');
            icon.appendChild(circle);
        }
        var text = document.createElement('span');
        text.textContent = label;
        parent.replaceChildren(icon, text);
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
        appendIconLabel(download, 'download', 'Download PDF');
        download.addEventListener('click', function () {
            downloadCertificate(certificate.id, download);
        });
        var verify = document.createElement('a');
        verify.className = 'btn btn-secondary btn-sm flex-1';
        verify.href = certificate.verifyUrl || ('/certificates/verify/' + encodeURIComponent(certificate.verificationCode));
        appendIconLabel(verify, 'view', 'Verify');
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
        link.className = 'action-btn';
        appendIconLabel(link, 'view', 'View');
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
        button.disabled = true;
        appendIconLabel(button, 'download', 'Preparing...');
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
            appendIconLabel(button, 'download', 'Download PDF');
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
