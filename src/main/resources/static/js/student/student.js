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
    initStudentCertificates();
    initDedicatedQuizAttempt();
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

function applyLearningProgress(progress) {
    if (!progress) {
        return;
    }
    var lessonStatus = document.getElementById('lesson-status');
    if (lessonStatus && progress.completed) {
        lessonStatus.textContent = 'Completed';
        lessonStatus.classList.add('completed');
    }

    if (progress.completed && progress.lessonId != null) {
        document.querySelectorAll('[data-lesson-state="' + progress.lessonId + '"]').forEach(function (state) {
            state.textContent = 'Completed lesson';
        });
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
    if (help && progress.completed) {
        help.textContent = 'Video completed.';
    } else if (help && typeof progress.watchedSeconds === 'number' && typeof progress.videoDurationSeconds === 'number') {
        help.textContent = 'Watched ' + progress.watchedSeconds + ' of ' + progress.videoDurationSeconds + ' seconds.';
    } else if (help && typeof progress.lastPositionSeconds === 'number') {
        help.textContent = 'Saved at ' + progress.lastPositionSeconds + ' seconds.';
    }

    var videoProgressFill = document.getElementById('video-progress-fill');
    if (videoProgressFill && progress.lessonProgressPercentage != null) {
        videoProgressFill.style.width = (progress.completed ? 100 : progress.lessonProgressPercentage) + '%';
        var videoProgress = videoProgressFill.parentElement;
        if (videoProgress) {
            videoProgress.setAttribute('aria-valuenow', progress.completed ? '100' : String(progress.lessonProgressPercentage));
        }
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
            window.setTimeout(options.onReady, 0);
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
    this.pause = function () {
        if (typeof video.pause === 'function') {
            video.pause();
        }
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
        }, 250);
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
        if (player && typeof player.getCurrentTime === 'function' && options.onTimeUpdate) {
            options.onTimeUpdate(player.getCurrentTime());
        }
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
    this.pause = function () {
        if (player && typeof player.pauseVideo === 'function') {
            player.pauseVideo();
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
    var previewMode = element.dataset.previewMode === 'true';
    var previewPercentLimit = Number(element.dataset.previewLimit || 50);
    var paywallShown = false;

    function enforcePreviewLimit(currentTime) {
        if (!previewMode) {
            return false;
        }
        var duration = currentDuration();
        if (duration <= 0) {
            return false;
        }
        var limit = duration * (previewPercentLimit / 100);
        if (currentTime >= limit) {
            if (playerWrapper) {
                playerWrapper.pause();
                if (currentTime > limit) {
                    playerWrapper.seekTo(limit);
                }
            }
            if (!paywallShown) {
                paywallShown = true;
                showPaywallDialog();
            }
            return true;
        }
        return false;
    }

    function showPaywallDialog() {
        if (window.LuminaActionDialog) {
            window.LuminaActionDialog.open({
                variant: 'warning',
                icon: 'payment',
                eyebrow: 'Preview Limit',
                title: 'Unlock Full Course',
                subtitle: 'You have watched 50% of the first lesson. Purchase the course to continue learning.',
                cancelText: 'No',
                confirmText: 'Yes',
                onConfirm: function () {
                    window.location.href = '/student/checkout?courseId=' + courseId;
                }
            });
        } else {
            var fallback = document.querySelector('[data-paywall-message]');
            if (!fallback) {
                fallback = document.createElement('div');
                fallback.setAttribute('data-paywall-message', '');
                fallback.setAttribute('role', 'status');
                fallback.style.cssText = 'position:fixed;right:1rem;bottom:1rem;z-index:60;max-width:320px;padding:1rem;border-radius:8px;background:#fff7ed;border:1px solid #fed7aa;color:#9a3412;box-shadow:0 10px 30px rgba(15,23,42,0.18);font-size:0.875rem;line-height:1.5;';
                document.body.appendChild(fallback);
            }
            fallback.textContent = 'Please purchase the course to continue learning.';
        }
    }

    function finiteSeconds(value) {
        var numberValue = Number(value || 0);
        if (!Number.isFinite(numberValue) || numberValue < 0) {
            return 0;
        }
        return Math.floor(numberValue);
    }

    function finiteDurationSeconds(value) {
        var numberValue = Number(value || 0);
        if (!Number.isFinite(numberValue) || numberValue <= 0) {
            return 0;
        }
        return Math.ceil(numberValue);
    }

    function formatDuration(totalSeconds) {
        if (totalSeconds <= 0) {
            return 'No video duration';
        }
        if (totalSeconds < 60) {
            return 'Less than 1 min';
        }
        var totalMinutes = Math.ceil(totalSeconds / 60);
        var hours = Math.floor(totalMinutes / 60);
        var minutes = totalMinutes % 60;
        if (hours === 0) {
            return totalMinutes + (totalMinutes === 1 ? ' min' : ' mins');
        }
        if (minutes === 0) {
            return hours + (hours === 1 ? ' hr' : ' hrs');
        }
        return hours + (hours === 1 ? ' hr ' : ' hrs ')
            + minutes + (minutes === 1 ? ' min' : ' mins');
    }

    function updateDurationDisplays(previousDuration, duration) {
        if (duration <= 0 || duration === previousDuration) {
            return;
        }
        element.dataset.durationSeconds = String(duration);
        document.querySelectorAll('[data-lesson-duration="' + lessonId + '"]').forEach(function (durationElement) {
            durationElement.textContent = formatDuration(duration);
        });

        var courseDuration = document.querySelector('[data-course-duration-display]');
        if (courseDuration) {
            var total = finiteDurationSeconds(courseDuration.dataset.durationSeconds);
            var correctedTotal = Math.max(0, total - Math.max(0, previousDuration) + duration);
            courseDuration.dataset.durationSeconds = String(correctedTotal);
            courseDuration.textContent = formatDuration(correctedTotal);
        }
    }

    function playerDurationSeconds() {
        return playerWrapper ? finiteDurationSeconds(playerWrapper.getDuration()) : 0;
    }

    function currentDuration() {
        var duration = playerDurationSeconds();
        if (duration > knownDuration || (knownDuration <= 0 && duration > 0)) {
            var previousDuration = knownDuration;
            knownDuration = duration;
            updateDurationDisplays(previousDuration, duration);
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
        if (previewMode) {
            return Promise.resolve();
        }
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
        synchronizePlayerDuration(0);
    }

    function synchronizePlayerDuration(attempt) {
        var previousDuration = knownDuration;
        var detectedDuration = currentDuration();
        if (detectedDuration > previousDuration) {
            saveProgress(true, playerWrapper ? playerWrapper.getCurrentTime() : 0, 'TIME_UPDATE');
            return;
        }
        if (playerDurationSeconds() <= 0 && attempt < 20) {
            window.setTimeout(function () {
                synchronizePlayerDuration(attempt + 1);
            }, 250);
        }
    }

    function onPlay() {
        if (playerWrapper) {
            enforcePreviewLimit(playerWrapper.getCurrentTime());
        }
    }

    function onPause() {
        if (playerWrapper) {
            if (previewMode) {
                var duration = currentDuration();
                if (duration > 0 && playerWrapper.getCurrentTime() >= duration * (previewPercentLimit / 100)) {
                    return;
                }
            }
            saveProgress(true, playerWrapper.getCurrentTime(), 'PAUSE');
        }
    }

    function onTimeUpdate(currentTime) {
        if (enforcePreviewLimit(currentTime)) {
            return;
        }
        var duration = currentDuration();
        var nearCompletion = duration > 0 && finiteSeconds(currentTime) * 100 >= duration * 90;
        saveProgress(nearCompletion, currentTime, 'TIME_UPDATE');
    }

    function onSeeked(currentTime) {
        if (enforcePreviewLimit(currentTime)) {
            return;
        }
        saveProgress(true, currentTime, 'SEEKED');
    }

    function onEnded() {
        if (playerWrapper) {
            if (previewMode) {
                return;
            }
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
            onEnded: onEnded,
            onSeeked: onSeeked
        });
    }

    window.addEventListener('beforeunload', function () {
        if (previewMode) {
            return;
        }
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
    if (quizPanel) {
        initQuizOverview(quizPanel);
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

function initQuizOverview(panel) {
    var courseId = panel.dataset.courseId;
    var lessonId = panel.dataset.lessonId;
    var stateRoot = document.getElementById('quiz-overview-state');
    var uiState = 'loading';

    if (!stateRoot) {
        return;
    }

    function attemptUrl(data) {
        return '/student/courses/' + encodeURIComponent(courseId)
            + '/lessons/' + encodeURIComponent(lessonId)
            + '/quiz/attempt/' + encodeURIComponent(data.attemptId);
    }

    function setState(nextState) {
        uiState = nextState;
        stateRoot.dataset.state = nextState;
        stateRoot.setAttribute('aria-busy', nextState === 'loading' ? 'true' : 'false');
    }

    function renderLoading() {
        setState('loading');
        stateRoot.replaceChildren();
        var skeleton = document.createElement('div');
        skeleton.className = 'quiz-overview-skeleton';
        skeleton.setAttribute('aria-hidden', 'true');
        for (var index = 0; index < 3; index += 1) {
            skeleton.appendChild(document.createElement('span'));
        }
        stateRoot.appendChild(skeleton);
    }

    function renderError(error) {
        setState('error');
        stateRoot.replaceChildren();
        var errorCard = document.createElement('div');
        errorCard.className = 'quiz-overview-error';
        errorCard.appendChild(createTextElement(
            'strong', null, 'We could not load this quiz overview.'));
        errorCard.appendChild(createTextElement(
            'span', null, error && error.message
                ? error.message
                : 'Please try again in a moment.'));
        var retry = document.createElement('button');
        retry.type = 'button';
        retry.className = 'btn btn-outline';
        retry.textContent = 'Try Again';
        retry.addEventListener('click', loadOverview);
        errorCard.appendChild(retry);
        stateRoot.appendChild(errorCard);
    }

    function addMetadata(list, label, value) {
        if (value == null || value === '') {
            return;
        }
        var item = document.createElement('div');
        item.className = 'quiz-overview-meta-item';
        item.appendChild(createTextElement('span', null, label));
        item.appendChild(createTextElement('strong', null, value));
        list.appendChild(item);
    }

    function startOrOpen(data, button) {
        if (uiState === 'loading' || uiState === 'confirming') {
            return;
        }
        if (data.attemptId) {
            window.location.assign(attemptUrl(data));
            return;
        }
        setState('confirming');
        button.disabled = true;
        button.textContent = 'Reviewing rules...';
        LuminaActionDialog.presets.quizStart({
            quizName: data.title || 'Selected quiz',
            questionCount: String(data.questionCount || 10) + ' questions',
            duration: String(data.durationMinutes || 20) + ' minutes',
            passingScore: data.passingScore == null ? 'Configured score' : data.passingScore + '%',
            options: {
                loadingText: 'Creating attempt...',
                onConfirm: function () {
                    setState('loading');
                    button.textContent = 'Starting...';
                    return fetch('/api/student/quizzes/' + encodeURIComponent(data.quizId) + '/attempts', {
                        method: 'POST',
                        credentials: 'same-origin'
                    }).then(function (response) {
                        return assessmentJson(response, 'Unable to start this quiz');
                    }).then(function (body) {
                        var attempt = body.data || {};
                        if (!attempt.id) {
                            throw new Error('The quiz attempt could not be opened.');
                        }
                        data.attemptId = attempt.id;
                        window.location.assign(attemptUrl(data));
                    }).catch(function (error) {
                        setState('error');
                        throw error;
                    });
                }
            }
        }).then(function (result) {
            if (!result.confirmed && uiState === 'confirming') {
                setState('ready');
                button.disabled = false;
                button.textContent = 'Start Quiz';
            }
        });
    }

    function renderReady(data) {
        setState('ready');
        stateRoot.replaceChildren();
        var card = document.createElement('article');
        card.className = 'quiz-overview-card';
        card.appendChild(createTextElement('p', 'quiz-overview-eyebrow', 'Graded assessment'));
        card.appendChild(createTextElement('h3', null, data.title || 'Quiz'));
        if (data.description) {
            card.appendChild(createTextElement('p', 'quiz-overview-description', data.description));
        }

        var metadata = document.createElement('div');
        metadata.className = 'quiz-overview-metadata';
        if (Number(data.questionCount) > 0) {
            addMetadata(metadata, 'Questions', String(data.questionCount));
        }
        if (Number(data.durationMinutes) > 0) {
            addMetadata(metadata, 'Time limit', data.durationMinutes + ' minutes');
        }
        if (data.passingScore != null) {
            addMetadata(metadata, 'Passing score', data.passingScore + '%');
        }
        if (data.score != null && data.attemptStatus !== 'DRAFT') {
            addMetadata(metadata, 'Latest score', data.score + '%');
        }
        if (metadata.childElementCount) {
            card.appendChild(metadata);
        }

        var actionRow = document.createElement('div');
        actionRow.className = 'quiz-overview-actions';
        var action = document.createElement('button');
        action.type = 'button';
        action.className = 'btn btn-primary quiz-overview-primary-action';
        action.textContent = data.attemptStatus === 'DRAFT'
            ? 'Continue Quiz'
            : (data.attemptId ? 'Review Result' : 'Start Quiz');
        action.addEventListener('click', function () {
            startOrOpen(data, action);
        });
        actionRow.appendChild(action);
        card.appendChild(actionRow);
        stateRoot.appendChild(card);
    }

    function loadOverview() {
        renderLoading();
        fetch('/api/student/courses/' + encodeURIComponent(courseId)
            + '/lessons/' + encodeURIComponent(lessonId) + '/quiz-overview', {
            credentials: 'same-origin'
        }).then(function (response) {
            return assessmentJson(response, 'Unable to load this quiz');
        }).then(function (body) {
            renderReady(body.data || {});
        }).catch(renderError);
    }

    loadOverview();
}

function initDedicatedQuizAttempt() {
    var page = document.querySelector('.dedicated-quiz-page[data-attempt-id]');
    if (!page) {
        return;
    }
    var attemptId = page.dataset.attemptId;
    var quizId = page.dataset.quizId;
    var courseId = page.dataset.courseId;
    var lessonId = page.dataset.lessonId;
    var message = document.getElementById('quiz-message');
    var retake = document.getElementById('retake-quiz-btn');
    var uiState = page.dataset.quizMode === 'submitted' ? 'submitted' : 'ready';
    var questions = Array.prototype.slice.call(
        document.querySelectorAll('.dedicated-quiz-question'));
    var navButtons = Array.prototype.slice.call(
        document.querySelectorAll('.dedicated-quiz-nav-item'));
    var current = 0;
    var countdown = document.getElementById('quiz-countdown');
    var countdownValue = document.getElementById('quiz-countdown-value');
    var countdownInterval = null;
    var automaticSubmitRetry = null;

    function attemptUrl(id) {
        return '/student/courses/' + encodeURIComponent(courseId)
            + '/lessons/' + encodeURIComponent(lessonId)
            + '/quiz/attempt/' + encodeURIComponent(id);
    }

    function setState(nextState, feedback, type) {
        uiState = nextState;
        page.dataset.uiState = nextState;
        setAssessmentMessage(message, feedback || '', type || null);
        var submitting = nextState === 'submitting';
        var saving = nextState === 'saving';
        var confirming = nextState === 'confirming';
        document.querySelectorAll('[data-submit-quiz="true"]').forEach(function (button) {
            button.disabled = submitting || saving || confirming;
            button.textContent = submitting
                ? 'Submitting...'
                : (confirming ? 'Reviewing...' : 'Submit Quiz');
        });
        var saveButton = document.getElementById('save-quiz-btn');
        if (saveButton) {
            saveButton.disabled = submitting || saving || confirming;
            saveButton.textContent = saving ? 'Saving...' : 'Save Draft';
        }
        if (questions.length) {
            updateProgress();
        }
    }

    if (retake) {
        retake.addEventListener('click', function () {
            if (uiState === 'loading' || uiState === 'confirming') {
                return;
            }
            setState('confirming', 'Review the quiz rules before starting again.');
            retake.disabled = true;
            LuminaActionDialog.presets.quizStart({
                quizName: (document.querySelector('.dedicated-quiz-heading h1') || {}).textContent || 'Selected quiz',
                questionCount: (page.dataset.questionCount || '10') + ' questions',
                duration: (page.dataset.durationMinutes || '20') + ' minutes',
                passingScore: page.dataset.passingScore ? page.dataset.passingScore + '%' : 'Configured score',
                options: {
                    loadingText: 'Creating attempt...',
                    onConfirm: function () {
                        setState('loading', 'Opening a new attempt...');
                        return fetch('/api/student/quizzes/' + encodeURIComponent(quizId) + '/attempts', {
                            method: 'POST',
                            credentials: 'same-origin'
                        }).then(function (response) {
                            return assessmentJson(response, 'Unable to start a new attempt');
                        }).then(function (body) {
                            window.location.assign(attemptUrl(body.data.id));
                        }).catch(function (error) {
                            setState('error', error.message, 'error');
                            throw error;
                        });
                    }
                }
            }).then(function (result) {
                if (!result.confirmed && uiState === 'confirming') {
                    retake.disabled = false;
                    setState('submitted');
                }
            });
        });
        return;
    }

    if (!questions.length) {
        setState('error', 'This attempt does not contain any questions.', 'error');
        return;
    }

    function questionAnswered(question) {
        return question.querySelector('input:checked') !== null;
    }

    function updateOptionStates(question) {
        question.querySelectorAll('.dedicated-quiz-option').forEach(function (option) {
            option.classList.toggle(
                'is-selected', option.querySelector('input:checked') !== null);
        });
    }

    function updateProgress() {
        var answered = 0;
        var firstUnanswered = -1;
        var busy = uiState === 'saving' || uiState === 'submitting' || uiState === 'confirming';
        questions.forEach(function (question, index) {
            var isAnswered = questionAnswered(question);
            if (isAnswered) {
                answered += 1;
            } else if (firstUnanswered === -1) {
                firstUnanswered = index;
            }
            if (!navButtons[index]) {
                return;
            }
            navButtons[index].classList.toggle('is-current', index === current);
            navButtons[index].classList.toggle(
                'is-answered', index !== current && isAnswered);
            navButtons[index].classList.toggle(
                'is-unanswered', index !== current && !isAnswered);
            if (index === current) {
                navButtons[index].setAttribute('aria-current', 'step');
            } else {
                navButtons[index].removeAttribute('aria-current');
            }
        });
        var lastUnlocked = firstUnanswered === -1
            ? questions.length - 1
            : firstUnanswered;
        navButtons.forEach(function (button, index) {
            button.disabled = busy || (index > lastUnlocked && index !== current);
            button.setAttribute('aria-disabled', button.disabled ? 'true' : 'false');
        });
        var count = document.getElementById('quiz-answered-count');
        if (count) {
            count.textContent = String(answered);
        }
        var previous = document.getElementById('previous-question-btn');
        var next = document.getElementById('next-question-btn');
        var clear = document.getElementById('clear-selection-btn');
        if (previous) {
            previous.disabled = busy || current === 0;
        }
        if (next) {
            next.disabled = busy || !questionAnswered(questions[current]);
        }
        if (clear) {
            clear.disabled = busy;
        }
        document.querySelectorAll('[data-submit-quiz="true"]').forEach(function (button) {
            button.disabled = busy || firstUnanswered !== -1;
            button.setAttribute('aria-disabled', button.disabled ? 'true' : 'false');
            button.title = firstUnanswered !== -1
                ? 'Answer all questions before submitting'
                : '';
        });
    }

    function showQuestion(index, focusHeading) {
        current = Math.max(0, Math.min(index, questions.length - 1));
        questions.forEach(function (question, questionIndex) {
            var active = questionIndex === current;
            question.classList.toggle('is-active', active);
            question.setAttribute('aria-hidden', active ? 'false' : 'true');
        });
        var next = document.getElementById('next-question-btn');
        if (next) {
            next.textContent = current === questions.length - 1
                ? 'Review Answers'
                : 'Next Question';
        }
        updateProgress();
        if (focusHeading) {
            var heading = questions[current].querySelector('h2');
            if (heading) {
                heading.focus();
            }
        }
    }

    function firstUnansweredIndex() {
        return questions.findIndex(function (question) {
            return !questionAnswered(question);
        });
    }

    function requireAnswer(questionIndex) {
        showQuestion(questionIndex, true);
        setState(
            'ready',
            'Select at least one answer before continuing. All questions are required.',
            'error');
        var firstInput = questions[questionIndex].querySelector('input');
        if (firstInput) {
            firstInput.focus();
        }
    }

    function collectAnswers() {
        return questions.map(function (question) {
            return {
                questionId: Number(question.dataset.questionId),
                selectedOptionIds: Array.prototype.slice.call(
                    question.querySelectorAll('input:checked'))
                    .map(function (input) {
                        return Number(input.value);
                    })
            };
        });
    }

    function updateAttempt(method, endpoint, fallbackMessage) {
        return fetch(endpoint, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ answers: collectAnswers() })
        }).then(function (response) {
            return assessmentJson(response, fallbackMessage);
        });
    }

    navButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            var target = Number(button.dataset.questionIndex);
            if (button.disabled) {
                return;
            }
            if (target > current && !questionAnswered(questions[current])) {
                requireAnswer(current);
                return;
            }
            showQuestion(target, true);
        });
    });
    questions.forEach(function (question) {
        question.querySelectorAll('input').forEach(function (input) {
            input.addEventListener('change', function () {
                updateOptionStates(question);
                updateProgress();
                setState('ready', 'Changes are not saved yet.');
            });
        });
    });

    var previous = document.getElementById('previous-question-btn');
    if (previous) {
        previous.addEventListener('click', function () {
            showQuestion(current - 1, true);
        });
    }
    var next = document.getElementById('next-question-btn');
    if (next) {
        next.addEventListener('click', function () {
            if (!questionAnswered(questions[current])) {
                requireAnswer(current);
                return;
            }
            if (current < questions.length - 1) {
                showQuestion(current + 1, true);
                return;
            }
            var unansweredIndex = firstUnansweredIndex();
            if (unansweredIndex !== -1) {
                requireAnswer(unansweredIndex);
                return;
            }
            setState('ready', 'All questions are answered. Submit when you are ready.', 'success');
            var submitButton = document.querySelector('[data-submit-quiz="true"]');
            if (submitButton) {
                submitButton.focus();
            }
        });
    }
    var clear = document.getElementById('clear-selection-btn');
    if (clear) {
        clear.addEventListener('click', function () {
            questions[current].querySelectorAll('input:checked').forEach(function (input) {
                input.checked = false;
            });
            updateOptionStates(questions[current]);
            updateProgress();
            setState('ready', 'Selection cleared. Save the draft to persist this change.');
        });
    }
    var save = document.getElementById('save-quiz-btn');
    if (save) {
        save.addEventListener('click', function () {
            if (uiState === 'saving' || uiState === 'submitting') {
                return;
            }
            setState('saving');
            updateAttempt(
                'PUT',
                '/api/student/quiz-attempts/' + encodeURIComponent(attemptId) + '/draft',
                'Unable to save this draft'
            ).then(function () {
                setState('ready', 'Draft saved.', 'success');
            }).catch(function (error) {
                setState('error', error.message, 'error');
            });
        });
    }
    function stopCountdown() {
        if (countdownInterval !== null) {
            window.clearInterval(countdownInterval);
            countdownInterval = null;
        }
    }

    function performQuizSubmission(automatic) {
        stopCountdown();
        setState(
            'submitting',
            automatic ? 'Time is up. Your quiz is being submitted automatically.' : '');
        return updateAttempt(
            'POST',
            '/api/student/quiz-attempts/' + encodeURIComponent(attemptId) + '/submit',
            'Unable to submit this quiz'
        ).then(function () {
            setState('submitted');
            window.location.assign(attemptUrl(attemptId));
        }).catch(function (error) {
            setState('error', error.message, 'error');
            if (automatic && automaticSubmitRetry === null) {
                automaticSubmitRetry = window.setTimeout(function () {
                    automaticSubmitRetry = null;
                    submitQuiz(true);
                }, 5000);
                return;
            }
            throw error;
        });
    }

    function submitQuiz(automatic) {
        if (uiState === 'submitted' || uiState === 'submitting') {
            return;
        }
        if (uiState === 'saving') {
            if (automatic && automaticSubmitRetry === null) {
                automaticSubmitRetry = window.setTimeout(function () {
                    automaticSubmitRetry = null;
                    submitQuiz(true);
                }, 250);
            }
            return;
        }
        var unansweredIndex = firstUnansweredIndex();
        if (!automatic && unansweredIndex !== -1) {
            requireAnswer(unansweredIndex);
            return;
        }
        if (automatic) {
            if (window.LuminaActionDialog && typeof LuminaActionDialog.close === 'function') {
                LuminaActionDialog.close();
            }
            performQuizSubmission(true);
            return;
        }

        setState('confirming');
        var answeredCount = questions.filter(questionAnswered).length;
        LuminaActionDialog.presets.quizSubmit({
            answered: answeredCount + '/' + questions.length,
            unanswered: String(questions.length - answeredCount),
            timeRemaining: countdownValue ? countdownValue.textContent : 'Not available',
            options: {
                loadingText: 'Submitting quiz...',
                onConfirm: function () {
                    if (uiState === 'submitting' || uiState === 'submitted') {
                        throw new Error('This quiz is already being submitted.');
                    }
                    return performQuizSubmission(false);
                }
            }
        }).then(function (result) {
            if (!result.confirmed && uiState === 'confirming') {
                setState('ready');
            }
        });
    }

    function formatCountdown(totalSeconds) {
        var hours = Math.floor(totalSeconds / 3600);
        var minutes = Math.floor((totalSeconds % 3600) / 60);
        var seconds = totalSeconds % 60;
        var minuteText = String(minutes).padStart(2, '0');
        var secondText = String(seconds).padStart(2, '0');
        return hours > 0
            ? String(hours).padStart(2, '0') + ':' + minuteText + ':' + secondText
            : minuteText + ':' + secondText;
    }

    function initCountdown() {
        if (!countdown || !countdownValue) {
            return;
        }
        var durationMinutes = Number(page.dataset.durationMinutes);
        var remainingAtLoad = Number(page.dataset.remainingSeconds);
        if (!Number.isFinite(durationMinutes)
                || durationMinutes <= 0
                || !Number.isFinite(remainingAtLoad)
                || remainingAtLoad < 0) {
            countdown.hidden = true;
            return;
        }
        var deadline = Date.now() + remainingAtLoad * 1000;

        function updateCountdown() {
            var remainingSeconds = Math.max(
                0,
                Math.ceil((deadline - Date.now()) / 1000));
            countdownValue.textContent = formatCountdown(remainingSeconds);
            countdown.setAttribute(
                'aria-label',
                'Time remaining: ' + formatCountdown(remainingSeconds));
            countdown.classList.toggle(
                'is-warning',
                remainingSeconds > 0 && remainingSeconds <= 300);
            countdown.classList.toggle('is-expired', remainingSeconds === 0);
            if (remainingSeconds === 0) {
                stopCountdown();
                submitQuiz(true);
            }
        }

        updateCountdown();
        if (countdownInterval === null && uiState !== 'submitting') {
            countdownInterval = window.setInterval(updateCountdown, 250);
        }
    }

    document.querySelectorAll('[data-submit-quiz="true"]').forEach(function (submit) {
        submit.addEventListener('click', function () {
            if (uiState === 'submitting' || uiState === 'saving') {
                return;
            }
            submitQuiz(false);
        });
    });

    questions.forEach(updateOptionStates);
    showQuestion(0, false);
    initCountdown();
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
        document.querySelectorAll('[data-user-chip-avatar]').forEach(function (shell) {
            var image = shell.querySelector('[data-user-chip-image]');
            if (!src) {
                if (image) image.remove();
                return;
            }
            if (!image) {
                image = document.createElement('img');
                image.className = 'user-chip-avatar-image';
                image.dataset.userChipImage = 'true';
                image.alt = '';
                image.referrerPolicy = 'no-referrer';
                image.addEventListener('error', function () { image.remove(); });
                shell.appendChild(image);
            }
            image.src = src;
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
        var initial = (profile.fullName || 'U').trim().charAt(0).toUpperCase() || 'U';
        document.querySelectorAll('[data-profile-field="fullName"]').forEach(function (node) {
            node.textContent = profile.fullName || '';
        });
        document.querySelectorAll('[data-user-chip-initial]').forEach(function (node) {
            node.textContent = initial;
        });
        setAvatarPreview(profile.avatarUrl || '');
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
    var loadingText = document.getElementById('certificate-loading-text');
    var empty = document.getElementById('certificate-empty');
    var total = document.getElementById('certificate-total');
    var message = document.getElementById('certificate-message');
    var historyCard = document.getElementById('certificate-history-card');
    var slowNetworkTimer = null;

    function setMessage(text, isError) {
        if (!message) {
            return;
        }
        message.textContent = text || '';
        message.style.color = isError ? 'var(--lumina-danger)' : 'var(--lumina-gray-600)';
    }

    function setLoading(isLoading) {
        page.setAttribute('aria-busy', isLoading ? 'true' : 'false');
        if (loading) {
            loading.hidden = !isLoading;
        }
        if (loadingText) {
            loadingText.textContent = 'Loading certificates...';
        }
        if (slowNetworkTimer) {
            window.clearTimeout(slowNetworkTimer);
            slowNetworkTimer = null;
        }
        if (isLoading && loadingText) {
            slowNetworkTimer = window.setTimeout(function () {
                loadingText.textContent = 'Still loading certificates...';
            }, 3500);
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
        var certificates = Array.isArray(items) ? items : [];
        if (grid) {
            grid.replaceChildren();
            grid.hidden = certificates.length === 0;
        }
        if (tableBody) {
            tableBody.replaceChildren();
        }
        if (total) {
            total.textContent = String(totalElements || certificates.length);
        }
        if (empty) {
            empty.hidden = certificates.length > 0;
        }
        if (historyCard) {
            historyCard.hidden = certificates.length === 0;
        }
        certificates.forEach(function (certificate) {
            if (grid) {
                grid.appendChild(renderCard(certificate));
            }
            if (tableBody) {
                tableBody.appendChild(renderRow(certificate));
            }
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

    setLoading(true);
    setMessage('', false);
    if (grid) {
        grid.hidden = true;
    }
    if (empty) {
        empty.hidden = true;
    }
    if (historyCard) {
        historyCard.hidden = true;
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
        if (grid) {
            grid.replaceChildren();
            grid.hidden = true;
        }
        if (tableBody) {
            tableBody.replaceChildren();
        }
        if (total) {
            total.textContent = '0';
        }
        if (historyCard) {
            historyCard.hidden = true;
        }
        setMessage(error.message, true);
        if (empty) {
            empty.hidden = false;
        }
    }).finally(function () {
        setLoading(false);
    });
}
