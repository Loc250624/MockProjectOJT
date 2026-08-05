'use strict';

document.addEventListener('DOMContentLoaded', function() {
    initPortalSidebarNav();
    initPortalSidebarDrawer();
    initTeacherVideoMetadata();
    initTeacherAssessments();
});

var teacherYouTubeApiPromise = null;

function initTeacherVideoMetadata() {
    var form = document.querySelector('[data-video-metadata-form]');
    if (!form) {
        return;
    }

    var urlInput = form.querySelector('[data-video-url-input]');
    var durationInput = form.querySelector('[data-video-duration-input]');
    var status = form.querySelector('[data-video-metadata-status]');
    var probe = form.querySelector('[data-video-duration-probe]');
    var submitButton = form.querySelector('[data-video-submit]');
    var debounceTimer = null;
    var detectionSequence = 0;
    var detectedUrl = '';
    var submittingAfterDetection = false;

    function setStatus(type, message) {
        status.classList.remove('is-loading', 'is-ready', 'is-error');
        if (type) {
            status.classList.add('is-' + type);
        }
        status.textContent = message;
    }

    function setSubmitEnabled(enabled) {
        submitButton.disabled = !enabled;
        submitButton.setAttribute('aria-disabled', enabled ? 'false' : 'true');
    }

    function currentUrl() {
        return urlInput.value.trim();
    }

    function detectCurrentUrl() {
        var url = currentUrl();
        var sequence = ++detectionSequence;
        detectedUrl = '';
        durationInput.value = '';
        probe.innerHTML = '';

        if (!url) {
            setSubmitEnabled(false);
            setStatus('', 'Enter a video URL. Its length will be detected automatically before saving.');
            return Promise.reject(new Error('Video URL is required.'));
        }

        setSubmitEnabled(false);
        setStatus('loading', 'Checking the video and detecting its length...');

        return detectVideoDuration(url, probe).then(function(seconds) {
            if (sequence !== detectionSequence || url !== currentUrl()) {
                throw new Error('The video URL changed during detection.');
            }
            var roundedSeconds = Math.ceil(Number(seconds));
            if (!Number.isFinite(roundedSeconds) || roundedSeconds < 1) {
                throw new Error('The video does not provide a valid length.');
            }
            durationInput.value = String(roundedSeconds);
            detectedUrl = url;
            setSubmitEnabled(true);
            setStatus('ready', 'Video ready — ' + formatVideoDuration(roundedSeconds) + ' detected automatically.');
            return roundedSeconds;
        }).catch(function(error) {
            if (sequence === detectionSequence) {
                durationInput.value = '';
                detectedUrl = '';
                setSubmitEnabled(false);
                setStatus('error', error.message || 'Unable to detect the video length.');
            }
            throw error;
        });
    }

    urlInput.addEventListener('input', function() {
        window.clearTimeout(debounceTimer);
        detectionSequence += 1;
        detectedUrl = '';
        durationInput.value = '';
        setSubmitEnabled(false);
        setStatus('loading', 'Waiting for the complete video URL...');
        debounceTimer = window.setTimeout(function() {
            detectCurrentUrl().catch(function() {});
        }, 500);
    });

    urlInput.addEventListener('blur', function() {
        window.clearTimeout(debounceTimer);
        if (currentUrl() && detectedUrl !== currentUrl()) {
            detectCurrentUrl().catch(function() {});
        }
    });

    form.addEventListener('submit', function(event) {
        if (submittingAfterDetection) {
            return;
        }
        if (detectedUrl === currentUrl() && Number(durationInput.value) > 0) {
            return;
        }
        event.preventDefault();
        window.clearTimeout(debounceTimer);
        detectCurrentUrl().then(function() {
            submittingAfterDetection = true;
            form.requestSubmit();
        }).catch(function() {
            urlInput.focus();
        });
    });

    setSubmitEnabled(false);
    if (currentUrl()) {
        detectCurrentUrl().catch(function() {});
    }
}

function detectVideoDuration(url, probe) {
    var youtubeId = extractTeacherYouTubeId(url);
    if (youtubeId) {
        return detectYouTubeDuration(youtubeId, probe);
    }
    if (/\.(mp4|webm|ogg|ogv|mov|m4v|m3u8)(?:[?#].*)?$/i.test(url)) {
        return detectDirectVideoDuration(url, probe);
    }
    return Promise.reject(new Error('Use a valid YouTube URL or a direct public video file URL.'));
}

function extractTeacherYouTubeId(value) {
    try {
        var parsed = new URL(value);
        var host = parsed.hostname.toLowerCase().replace(/^www\./, '').replace(/^m\./, '');
        var id = null;
        if (host === 'youtu.be') {
            id = parsed.pathname.split('/').filter(Boolean)[0];
        } else if (host === 'youtube.com') {
            id = parsed.searchParams.get('v');
            if (!id) {
                var parts = parsed.pathname.split('/').filter(Boolean);
                if (parts[0] === 'embed' || parts[0] === 'shorts') {
                    id = parts[1];
                }
            }
        }
        return id && /^[A-Za-z0-9_-]{11}$/.test(id) ? id : null;
    } catch (error) {
        return null;
    }
}

function loadTeacherYouTubeApi() {
    if (window.YT && typeof window.YT.Player === 'function') {
        return Promise.resolve(window.YT);
    }
    if (teacherYouTubeApiPromise) {
        return teacherYouTubeApiPromise;
    }

    teacherYouTubeApiPromise = new Promise(function(resolve, reject) {
        var previousReady = window.onYouTubeIframeAPIReady;
        var timeoutId = window.setTimeout(function() {
            reject(new Error('YouTube took too long to provide the video length. Please try again.'));
        }, 15000);

        window.onYouTubeIframeAPIReady = function() {
            if (typeof previousReady === 'function') {
                previousReady();
            }
            window.clearTimeout(timeoutId);
            resolve(window.YT);
        };

        var existingScript = document.querySelector('script[src="https://www.youtube.com/iframe_api"]');
        if (!existingScript) {
            var script = document.createElement('script');
            script.src = 'https://www.youtube.com/iframe_api';
            script.async = true;
            script.onerror = function() {
                window.clearTimeout(timeoutId);
                reject(new Error('Unable to load YouTube metadata. Check the network connection and try again.'));
            };
            document.head.appendChild(script);
        }
    });
    return teacherYouTubeApiPromise;
}

function detectYouTubeDuration(videoId, probe) {
    return loadTeacherYouTubeApi().then(function(YT) {
        return new Promise(function(resolve, reject) {
            var playerHost = document.createElement('div');
            probe.innerHTML = '';
            probe.appendChild(playerHost);
            var settled = false;
            var attempts = 0;
            var player;

            function finish(error, seconds) {
                if (settled) {
                    return;
                }
                settled = true;
                if (player && typeof player.destroy === 'function') {
                    player.destroy();
                }
                if (error) {
                    reject(error);
                } else {
                    resolve(seconds);
                }
            }

            function readDuration() {
                var seconds = player && Number(player.getDuration());
                if (Number.isFinite(seconds) && seconds > 0) {
                    finish(null, seconds);
                    return;
                }
                attempts += 1;
                if (attempts >= 24) {
                    finish(new Error('Unable to read this YouTube video length. The video may be private, live, or unavailable.'));
                    return;
                }
                window.setTimeout(readDuration, 250);
            }

            player = new YT.Player(playerHost, {
                width: '200',
                height: '200',
                videoId: videoId,
                playerVars: { autoplay: 0, controls: 0, playsinline: 1 },
                events: {
                    onReady: readDuration,
                    onError: function() {
                        finish(new Error('YouTube could not load this video. Check that it is public and available.'));
                    }
                }
            });
        });
    });
}

function detectDirectVideoDuration(url, probe) {
    return new Promise(function(resolve, reject) {
        var video = document.createElement('video');
        var timeoutId;
        var settled = false;
        probe.innerHTML = '';
        probe.appendChild(video);

        function finish(error, seconds) {
            if (settled) {
                return;
            }
            settled = true;
            window.clearTimeout(timeoutId);
            video.removeAttribute('src');
            video.load();
            if (error) {
                reject(error);
            } else {
                resolve(seconds);
            }
        }

        video.preload = 'metadata';
        video.muted = true;
        video.addEventListener('loadedmetadata', function() {
            var seconds = Number(video.duration);
            if (Number.isFinite(seconds) && seconds > 0) {
                finish(null, seconds);
            } else {
                finish(new Error('The video file does not provide a valid length.'));
            }
        });
        video.addEventListener('error', function() {
            finish(new Error('Unable to load the public video URL or read its length.'));
        });
        timeoutId = window.setTimeout(function() {
            finish(new Error('The video took too long to load. Check the URL and try again.'));
        }, 15000);
        video.src = url;
        video.load();
    });
}

function formatVideoDuration(totalSeconds) {
    var hours = Math.floor(totalSeconds / 3600);
    var minutes = Math.floor((totalSeconds % 3600) / 60);
    var seconds = totalSeconds % 60;
    var parts = [];
    if (hours > 0) {
        parts.push(hours + 'h');
    }
    if (minutes > 0 || hours > 0) {
        parts.push(minutes + 'm');
    }
    parts.push(seconds + 's');
    return parts.join(' ');
}

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
            if (!card) {
                return;
            }
            var quizTitle = card.querySelector('h3');
            LuminaActionDialog.presets.danger({
                title: 'Delete this quiz?',
                objectLabel: 'Quiz',
                objectName: quizTitle ? quizTitle.textContent.trim() : 'Selected quiz',
                notice: {
                    title: 'Student history is protected',
                    text: 'Quizzes with student attempt history cannot be deleted.'
                },
                checklist: [
                    'The quiz will be removed from its lesson.',
                    'Questions belonging to this quiz may also be removed.'
                ],
                confirmText: 'Delete quiz',
                cancelText: 'Keep quiz',
                options: {
                    loadingText: 'Deleting quiz...',
                    onConfirm: function () {
                        return teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId), {
                            method: 'DELETE'
                        }).then(function(response) {
                            return teacherAssessmentJson(response, 'Unable to delete quiz');
                        }).then(function() {
                            window.location.reload();
                        });
                    }
                }
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
    var questionList = panel.querySelector('[data-question-list]');
    var previousButton = panel.querySelector('[data-question-previous]');
    var nextButton = panel.querySelector('[data-question-next]');
    var pageButtons = panel.querySelector('[data-question-page-buttons]');
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
        size: 10,
        totalPages: 0,
        totalItems: 0,
        items: [],
        selectedId: null,
        dirty: false,
        creating: false
    };
    var leavePromptOpen = false;

    function markDirty(dirty) {
        state.dirty = dirty;
        panel.dataset.dirty = dirty ? 'true' : 'false';
        unsaved.hidden = !dirty;
    }

    function leaveEditor(action) {
        if (!state.dirty) {
            action();
            return;
        }
        if (leavePromptOpen) {
            return;
        }
        leavePromptOpen = true;
        LuminaActionDialog.open({
            variant: 'warning',
            icon: 'warning',
            eyebrow: 'Unsaved question',
            badge: 'Changes not saved',
            title: 'Discard unsaved question changes?',
            subtitle: 'Your edits in the question editor have not been saved.',
            objectLabel: 'Question',
            objectName: form.elements.content.value.trim() || 'New question',
            notice: {
                title: 'Unsaved edits will be lost',
                text: 'Continue editing if you still need these changes.'
            },
            confirmText: 'Discard changes',
            cancelText: 'Continue editing',
            initialFocus: 'cancel'
        }).then(function(result) {
            leavePromptOpen = false;
            if (result.confirmed) {
                action();
            }
        });
    }

    function questionLabel(question, index) {
        var number = state.page * state.size + index + 1;
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
        syncSelectedQuestion();
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
        syncSelectedQuestion();
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

    function syncSelectedQuestion() {
        questionList.querySelectorAll('[data-question-id]').forEach(function(button) {
            var selected = Number(button.dataset.questionId) === state.selectedId;
            button.classList.toggle('active', selected);
            button.setAttribute('aria-selected', selected ? 'true' : 'false');
        });
    }

    function visibleQuestionPages(pageCount) {
        if (pageCount <= 5) {
            return Array.from({ length: pageCount }, function(_, index) {
                return index;
            });
        }
        if (state.page <= 2) {
            return [0, 1, 2, null, pageCount - 1];
        }
        if (state.page >= pageCount - 3) {
            return [0, null, pageCount - 3, pageCount - 2, pageCount - 1];
        }
        return [0, null, state.page, null, pageCount - 1];
    }

    function renderPage(preferredQuestionId) {
        questionList.textContent = '';
        state.items.forEach(function(question, index) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'teacher-question-list-item';
            button.dataset.questionId = String(question.id);
            button.setAttribute('role', 'option');
            button.setAttribute('aria-selected', 'false');
            button.textContent = questionLabel(question, index);
            questionList.appendChild(button);
        });
        var pageCount = Math.max(1, state.totalPages);
        pageIndicator.textContent = 'Page ' + String(state.page + 1) + ' / ' + String(pageCount);
        pageButtons.textContent = '';
        visibleQuestionPages(pageCount).forEach(function(pageIndex) {
            if (pageIndex === null) {
                var ellipsis = document.createElement('span');
                ellipsis.className = 'teacher-question-page-ellipsis';
                ellipsis.textContent = '…';
                ellipsis.setAttribute('aria-hidden', 'true');
                pageButtons.appendChild(ellipsis);
                return;
            }
            var pageButton = document.createElement('button');
            pageButton.type = 'button';
            pageButton.className = 'teacher-question-page-button';
            pageButton.dataset.questionPage = String(pageIndex);
            pageButton.textContent = String(pageIndex + 1);
            pageButton.setAttribute('aria-label', 'Go to question page ' + String(pageIndex + 1));
            pageButton.classList.toggle('active', pageIndex === state.page);
            pageButton.disabled = pageIndex === state.page;
            pageButton.setAttribute('aria-current', pageIndex === state.page ? 'page' : 'false');
            pageButtons.appendChild(pageButton);
        });
        var firstItem = state.totalItems === 0 ? 0 : state.page * state.size + 1;
        var lastItem = firstItem === 0 ? 0 : firstItem + state.items.length - 1;
        pageSummary.textContent = 'Showing ' + String(firstItem) + '–' + String(lastItem)
            + ' of ' + String(state.totalItems) + ' questions';
        previousButton.disabled = state.page <= 0;
        nextButton.disabled = state.totalPages === 0 || state.page >= state.totalPages - 1;
        addButton.disabled = state.totalItems >= 100;

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
            state.size = Math.max(1, Number(data.size || state.size));
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

    questionList.addEventListener('click', function(event) {
        var button = event.target.closest('[data-question-id]');
        if (!button) {
            return;
        }
        var nextId = Number(button.dataset.questionId);
        leaveEditor(function() {
            var selected = state.items.find(function(item) {
                return item.id === nextId;
            });
            if (selected) {
                showQuestion(selected);
            }
        });
    });

    previousButton.addEventListener('click', function() {
        if (state.page > 0) {
            leaveEditor(function() { loadPage(state.page - 1); });
        }
    });
    nextButton.addEventListener('click', function() {
        if (state.page < state.totalPages - 1) {
            leaveEditor(function() { loadPage(state.page + 1); });
        }
    });
    pageButtons.addEventListener('click', function(event) {
        var button = event.target.closest('[data-question-page]');
        if (!button) {
            return;
        }
        var requestedPage = Number(button.dataset.questionPage);
        leaveEditor(function() {
            if (requestedPage >= 0
                    && requestedPage < state.totalPages
                    && requestedPage !== state.page) {
                loadPage(requestedPage);
            }
        });
    });
    addButton.addEventListener('click', function() {
        if (state.totalItems < 100) {
            leaveEditor(showNewQuestion);
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
                ? Math.floor(state.totalItems / state.size)
                : state.page;
            return loadPage(targetPage, saved.id);
        }).catch(function(saveError) {
            teacherAssessmentMessage(message, saveError.message, 'error');
        });
    });

    deleteButton.addEventListener('click', function() {
        var questionId = Number(form.elements.questionId.value);
        if (!questionId) {
            return;
        }
        LuminaActionDialog.presets.danger({
            title: 'Delete this question?',
            objectLabel: 'Question',
            objectName: form.elements.content.value.trim() || 'Selected question',
            checklist: [
                'The question will be removed from the quiz question bank.',
                'This action is blocked when protected student history depends on it.'
            ],
            confirmText: 'Delete question',
            cancelText: 'Keep question',
            options: {
                loadingText: 'Deleting question...',
                onConfirm: function () {
                    return teacherFetch('/api/teacher/questions/' + encodeURIComponent(questionId), {
                        method: 'DELETE'
                    }).then(function(response) {
                        return teacherAssessmentJson(response, 'Unable to delete question');
                    }).then(function() {
                        markDirty(false);
                        var targetPage = state.items.length === 1 && state.page > 0
                            ? state.page - 1
                            : state.page;
                        return loadPage(targetPage);
                    });
                }
            }
        });
    });

    loadPage(0);
}
