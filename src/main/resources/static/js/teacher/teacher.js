'use strict';

document.addEventListener('DOMContentLoaded', function() {
    initPortalSidebarNav();
    initPortalSidebarDrawer();
    initProgressDistributionLineChart();
    initTeacherVideoForms();
    initTeacherAssessments();
});

function initTeacherVideoForms() {
    document.querySelectorAll('[data-video-metadata-form]').forEach(function(form) {
        var sourceTypeInput = form.querySelector('[data-video-source-type-input]');
        var sourceOptions = Array.prototype.slice.call(form.querySelectorAll('[data-video-source-option]'));
        var uploadPanel = form.querySelector('[data-video-upload-panel]');
        var urlPanel = form.querySelector('[data-video-url-panel]');
        var fileInput = form.querySelector('[data-video-file-input]');
        var urlInput = form.querySelector('[data-video-url-input]');
        var durationInput = form.querySelector('[data-video-duration-input]');
        var status = form.querySelector('[data-video-metadata-status]');
        var submit = form.querySelector('[data-video-submit]');
        var probe = form.querySelector('[data-video-duration-probe]');
        var state = {
            mode: sourceTypeInput && sourceTypeInput.value === 'UPLOAD' ? 'UPLOAD' : 'URL',
            checking: false,
            ready: Number(durationInput && durationInput.value || 0) > 0,
            error: '',
            validatedUrl: ''
        };
        var requestId = 0;

        if (state.mode !== 'UPLOAD' && state.ready && urlInput && urlInput.value.trim()) {
            state.validatedUrl = urlInput.value.trim();
        }

        function selectedMode() {
            var selected = sourceOptions.filter(function(option) { return option.checked; })[0];
            return selected && selected.value === 'UPLOAD' ? 'UPLOAD' : 'URL';
        }

        function setStatus(text, type) {
            if (!status) return;
            status.textContent = text;
            status.classList.remove('is-loading', 'is-ready', 'is-error');
            if (type) status.classList.add(type);
        }

        function formatDuration(seconds) {
            seconds = Math.max(0, Number(seconds) || 0);
            var minutes = Math.floor(seconds / 60);
            var remainder = seconds % 60;
            return String(minutes) + ':' + String(remainder).padStart(2, '0');
        }

        function setDuration(seconds, messagePrefix) {
            var rounded = Math.ceil(Number(seconds));
            if (!Number.isFinite(rounded) || rounded <= 0) {
                throw new Error('We could not read the video duration. Choose another video or URL.');
            }
            durationInput.value = String(rounded);
            state.ready = true;
            state.error = '';
            setStatus((messagePrefix || 'Video ready') + ' · ' + formatDuration(rounded), 'is-ready');
            updateSubmit();
        }

        function clearDuration() {
            if (durationInput) durationInput.value = '';
            state.ready = false;
            state.validatedUrl = '';
        }

        function currentUrl() {
            return urlInput ? urlInput.value.trim() : '';
        }

        function isCurrentUrlReady() {
            return state.mode === 'URL'
                && state.ready
                && !state.error
                && currentUrl()
                && state.validatedUrl === currentUrl();
        }

        function updateSubmit() {
            if (!submit) return;
            var invalid = state.checking || !state.ready || Boolean(state.error);
            if (state.mode === 'UPLOAD') {
                var hasExistingUpload = sourceTypeInput && sourceTypeInput.value === 'UPLOAD'
                    && Number(durationInput && durationInput.value || 0) > 0;
                invalid = invalid || !(fileInput && fileInput.files && fileInput.files.length > 0) && !hasExistingUpload;
            } else {
                invalid = invalid || !currentUrl() || !isCurrentUrlReady();
            }
            submit.disabled = invalid;
        }

        function syncMode() {
            state.mode = selectedMode();
            if (uploadPanel) uploadPanel.hidden = state.mode !== 'UPLOAD';
            if (urlPanel) urlPanel.hidden = state.mode !== 'URL';
            if (fileInput) fileInput.disabled = state.mode !== 'UPLOAD';
            if (urlInput) urlInput.disabled = state.mode !== 'URL';
            if (state.mode === 'UPLOAD') {
                if (sourceTypeInput) sourceTypeInput.value = 'UPLOAD';
                if (fileInput && fileInput.files && fileInput.files.length > 0) {
                    checkUploadFile();
                } else if (Number(durationInput && durationInput.value || 0) > 0) {
                    state.ready = true;
                    setStatus('Existing upload ready · ' + formatDuration(Number(durationInput.value)), 'is-ready');
                } else {
                    clearDuration();
                    setStatus('Choose a video file. Its length will be detected before saving.', null);
                }
            } else {
                classifyUrlAndUpdateSource();
                if (urlInput && urlInput.value.trim()) {
                    if (isCurrentUrlReady()) {
                        setStatus((isYouTubeUrl(currentUrl()) ? 'YouTube video ready' : 'Remote video ready') + ' - ' + formatDuration(Number(durationInput.value)), 'is-ready');
                    } else {
                        checkUrl();
                    }
                } else {
                    clearDuration();
                    setStatus('Enter a YouTube URL or direct HTTPS video URL.', null);
                }
            }
            updateSubmit();
        }

        function classifyUrlAndUpdateSource() {
            var url = urlInput ? urlInput.value.trim() : '';
            if (!sourceTypeInput) return;
            sourceTypeInput.value = isYouTubeUrl(url) ? 'YOUTUBE' : 'DIRECT_URL';
        }

        function markUrlDirty() {
            var url = currentUrl();
            requestId++;
            state.error = '';
            if (url !== state.validatedUrl) {
                clearDuration();
                setStatus(url ? 'Checking video...' : 'Enter a YouTube URL or direct HTTPS video URL.', url ? 'is-loading' : null);
            }
            classifyUrlAndUpdateSource();
            updateSubmit();
        }

        function fail(message, id) {
            if (id && id !== requestId) return;
            state.checking = false;
            state.error = message;
            clearDuration();
            setStatus(message, 'is-error');
            updateSubmit();
        }

        function checkUploadFile() {
            var id = ++requestId;
            state.checking = true;
            state.error = '';
            clearDuration();
            updateSubmit();
            var file = fileInput && fileInput.files ? fileInput.files[0] : null;
            if (!file) {
                fail('Select a video file to upload.', id);
                return;
            }
            if (file.type && !isSupportedVideoMime(file.type)) {
                fail('Unsupported video file type.', id);
                return;
            }
            setStatus('Checking video...', 'is-loading');
            detectDirectVideoDuration(URL.createObjectURL(file), true)
                .then(function(seconds) {
                    if (id !== requestId) return;
                    state.checking = false;
                    setDuration(seconds, file.name || 'Video ready');
                })
                .catch(function(error) {
                    fail(error.message, id);
                });
        }

        function checkUrl() {
            var id = ++requestId;
            var url = currentUrl();
            if (state.validatedUrl === url && state.ready && !state.error) {
                updateSubmit();
                return;
            }
            state.checking = true;
            state.error = '';
            clearDuration();
            classifyUrlAndUpdateSource();
            updateSubmit();
            if (!url) {
                fail('Enter a video URL.', id);
                return;
            }
            if (isYouTubeUrl(url)) {
                setStatus('Checking YouTube video...', 'is-loading');
                detectYouTubeDuration(url)
                    .then(function(seconds) {
                        if (id !== requestId) return;
                        state.checking = false;
                        state.validatedUrl = url;
                        setDuration(seconds, 'YouTube video ready');
                    })
                    .catch(function(error) {
                        fail(error.message || 'This YouTube URL is not valid.', id);
                    });
                return;
            }
            if (!isDirectVideoUrl(url)) {
                fail('The remote URL is not a supported public video.', id);
                return;
            }
            setStatus('Checking video...', 'is-loading');
            detectDirectVideoDuration(url, false)
                .then(function(seconds) {
                    if (id !== requestId) return;
                    state.checking = false;
                    state.validatedUrl = url;
                    setDuration(seconds, 'Remote video ready');
                })
                .catch(function(error) {
                    fail(error.message, id);
                });
        }

        sourceOptions.forEach(function(option) {
            option.addEventListener('change', syncMode);
        });
        if (fileInput) {
            fileInput.addEventListener('change', checkUploadFile);
        }
        if (urlInput) {
            var debouncedCheckUrl = debounce(checkUrl, 450);
            urlInput.addEventListener('input', function() {
                markUrlDirty();
                debouncedCheckUrl();
            });
            urlInput.addEventListener('blur', function() {
                if (debouncedCheckUrl.cancel) debouncedCheckUrl.cancel();
                if (currentUrl() && !isCurrentUrlReady()) checkUrl();
            });
        }
        form.addEventListener('submit', function(event) {
            state.mode = selectedMode();
            if (state.mode === 'URL') {
                classifyUrlAndUpdateSource();
            } else if (sourceTypeInput) {
                sourceTypeInput.value = 'UPLOAD';
            }
            updateSubmit();
            if (submit && submit.disabled) {
                event.preventDefault();
                if (!state.error) {
                    setStatus('Complete the video source before saving.', 'is-error');
                }
                return;
            }
            if (submit) submit.disabled = true;
            setStatus('Saving video...', 'is-loading');
        });

        syncMode();
    });
}

function debounce(callback, delay) {
    var handle = null;
    var debounced = function() {
        var args = arguments;
        window.clearTimeout(handle);
        handle = window.setTimeout(function() {
            callback.apply(null, args);
        }, delay);
    };
    debounced.cancel = function() {
        window.clearTimeout(handle);
        handle = null;
    };
    return debounced;
}

function isSupportedVideoMime(type) {
    return [
        'video/mp4',
        'video/webm',
        'video/ogg',
        'video/quicktime',
        'application/vnd.apple.mpegurl',
        'application/x-mpegurl'
    ].indexOf(String(type || '').toLowerCase()) >= 0;
}

function isYouTubeUrl(url) {
    return /^(?:https?:\/\/)?(?:www\.|m\.)?(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/)|youtu\.be\/)([a-zA-Z0-9_-]{11})(?:\S*)?$/i.test(url || '');
}

function extractYouTubeId(url) {
    var match = /^(?:https?:\/\/)?(?:www\.|m\.)?(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/)|youtu\.be\/)([a-zA-Z0-9_-]{11})(?:\S*)?$/i.exec(url || '');
    return match ? match[1] : null;
}

function isDirectVideoUrl(url) {
    return /^https:\/\/.+\.(mp4|webm|ogg|ogv|mov|m4v|m3u8)(?:[?#].*)?$/i.test(url || '');
}

function detectDirectVideoDuration(src, revokeWhenDone) {
    return new Promise(function(resolve, reject) {
        var video = document.createElement('video');
        var timeout = window.setTimeout(function() {
            cleanup();
            reject(new Error('We could not read the video duration. Choose another video or URL.'));
        }, 12000);
        function cleanup() {
            window.clearTimeout(timeout);
            video.removeAttribute('src');
            video.load();
            if (revokeWhenDone) {
                URL.revokeObjectURL(src);
            }
        }
        video.preload = 'metadata';
        video.addEventListener('loadedmetadata', function() {
            var seconds = Math.ceil(Number(video.duration));
            cleanup();
            if (Number.isFinite(seconds) && seconds > 0) {
                resolve(seconds);
            } else {
                reject(new Error('We could not read the video duration. Choose another video or URL.'));
            }
        }, { once: true });
        video.addEventListener('error', function() {
            cleanup();
            reject(new Error('The remote URL is not a supported public video.'));
        }, { once: true });
        video.src = src;
    });
}

var teacherYouTubeApiPromise = null;

function loadTeacherYouTubeApi() {
    if (window.YT && window.YT.Player) {
        return Promise.resolve(window.YT);
    }
    if (teacherYouTubeApiPromise) {
        return teacherYouTubeApiPromise;
    }
    teacherYouTubeApiPromise = new Promise(function(resolve, reject) {
        var previousReady = window.onYouTubeIframeAPIReady;
        window.onYouTubeIframeAPIReady = function() {
            if (typeof previousReady === 'function') previousReady();
            resolve(window.YT);
        };
        if (!document.querySelector('script[src="https://www.youtube.com/iframe_api"]')) {
            var script = document.createElement('script');
            script.src = 'https://www.youtube.com/iframe_api';
            script.onerror = function() {
                reject(new Error('Unable to load the YouTube player API.'));
            };
            document.head.appendChild(script);
        }
        window.setTimeout(function() {
            if (!(window.YT && window.YT.Player)) {
                reject(new Error('Unable to load the YouTube player API.'));
            }
        }, 12000);
    });
    return teacherYouTubeApiPromise;
}

function detectYouTubeDuration(url) {
    var videoId = extractYouTubeId(url);
    if (!videoId) {
        return Promise.reject(new Error('This YouTube URL is not valid.'));
    }
    return loadTeacherYouTubeApi().then(function(YT) {
        return new Promise(function(resolve, reject) {
            var holder = document.createElement('div');
            holder.style.width = '1px';
            holder.style.height = '1px';
            holder.style.overflow = 'hidden';
            holder.setAttribute('aria-hidden', 'true');
            document.body.appendChild(holder);
            var timeout = window.setTimeout(function() {
                cleanup();
                reject(new Error('We could not read the video duration. Choose another video or URL.'));
            }, 12000);
            var player = new YT.Player(holder, {
                videoId: videoId,
                events: {
                    onReady: function() {
                        var seconds = player.getDuration();
                        cleanup();
                        if (Number(seconds) > 0) {
                            resolve(Math.ceil(Number(seconds)));
                        } else {
                            reject(new Error('We could not read the video duration. Choose another video or URL.'));
                        }
                    },
                    onError: function() {
                        cleanup();
                        reject(new Error('This YouTube URL is not valid.'));
                    }
                }
            });
            function cleanup() {
                window.clearTimeout(timeout);
                try {
                    if (player && player.destroy) player.destroy();
                } catch (ignored) {}
                holder.remove();
            }
        });
    });
}

function initProgressDistributionLineChart() {
    var plot = document.querySelector('[data-progress-line-chart]');
    if (!plot) {
        return;
    }

    var values = [
        Number(plot.getAttribute('data-not-started')) || 0,
        Number(plot.getAttribute('data-in-progress')) || 0,
        Number(plot.getAttribute('data-completed')) || 0
    ];
    var maximum = Math.max.apply(null, values.concat([1]));
    var xPositions = [60, 500, 940];
    var coordinates = values.map(function(value, index) {
        var y = 20 + (1 - (value / maximum)) * 200;
        return {
            x: xPositions[index],
            y: y,
            xPercent: xPositions[index] / 10,
            yPercent: y / 2.4
        };
    });
    var path = coordinates.reduce(function(result, point, index) {
        if (index === 0) return 'M ' + point.x.toFixed(2) + ' ' + point.y.toFixed(2);
        var previous = coordinates[index - 1];
        var middleX = (previous.x + point.x) / 2;
        return result + ' C ' + middleX.toFixed(2) + ' ' + previous.y.toFixed(2) + ', ' +
            middleX.toFixed(2) + ' ' + point.y.toFixed(2) + ', ' + point.x.toFixed(2) + ' ' + point.y.toFixed(2);
    }, '');
    var grid = [20, 70, 120, 170, 220].map(function(y) {
        return '<line x1="0" y1="' + y + '" x2="1000" y2="' + y + '"></line>';
    }).join('');

    plot.innerHTML = '<svg class="report-line-svg" viewBox="0 0 1000 240" preserveAspectRatio="none" aria-hidden="true" focusable="false">' +
        '<defs><linearGradient id="progressDistributionAreaGradient" x1="0" y1="0" x2="0" y2="1">' +
        '<stop offset="0%" stop-color="#5b61ff" stop-opacity="0.24"></stop>' +
        '<stop offset="100%" stop-color="#5b61ff" stop-opacity="0"></stop></linearGradient></defs>' +
        '<g class="report-line-grid">' + grid + '</g>' +
        '<path class="report-line-area" fill="url(#progressDistributionAreaGradient)" d="' + path + ' L 940 220 L 60 220 Z"></path>' +
        '<path class="report-line-path" vector-effect="non-scaling-stroke" d="' + path + '"></path>' +
        '</svg>';

    coordinates.forEach(function(point, index) {
        var marker = document.createElement('span');
        marker.className = 'report-line-point';
        marker.style.left = point.xPercent.toFixed(2) + '%';
        marker.style.top = point.yPercent.toFixed(2) + '%';
        marker.title = values[index].toLocaleString();
        marker.setAttribute('aria-hidden', 'true');
        plot.appendChild(marker);
    });

    var yAxis = document.querySelector('[data-progress-line-y-axis]');
    if (yAxis) {
        [1, 0.75, 0.5, 0.25, 0].forEach(function(ratio) {
            var tick = document.createElement('span');
            tick.textContent = Math.round(maximum * ratio).toLocaleString();
            yAxis.appendChild(tick);
        });
    }

    var stats = document.querySelector('[data-progress-line-stats]');
    if (stats) {
        var labels = ['Not started', 'In progress', 'Completed'];
        var largestIndex = values.indexOf(Math.max.apply(null, values));
        var total = values.reduce(function(sum, value) { return sum + value; }, 0);
        [
            { label: 'Students', value: total.toLocaleString() },
            { label: 'Largest group', value: total ? labels[largestIndex] : 'None' },
            { label: 'Completion', value: (total ? ((values[2] / total) * 100).toFixed(1) : '0.0') + '%' },
            { label: 'Categories', value: '3' }
        ].forEach(function(item) {
            var stat = document.createElement('div');
            stat.className = 'report-line-stat';
            var name = document.createElement('span');
            name.textContent = item.label;
            var value = document.createElement('strong');
            value.textContent = item.value;
            stat.appendChild(name);
            stat.appendChild(value);
            stats.appendChild(stat);
        });
    }
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

function teacherOpenActionDialog(options, fallbackMessageElement) {
    if (window.LuminaActionDialog && typeof window.LuminaActionDialog.open === 'function') {
        return window.LuminaActionDialog.open(options);
    }
    teacherAssessmentMessage(
        fallbackMessageElement,
        (options && (options.subtitle || options.title)) || 'This action needs confirmation before it can continue.',
        'error'
    );
    return Promise.resolve({ confirmed: false });
}

function teacherDangerDialog(options, fallbackMessageElement) {
    return teacherOpenActionDialog(Object.assign({ variant: 'danger', icon: 'danger' }, options), fallbackMessageElement);
}

function teacherSingleConfirmTask(task) {
    var submitted = false;
    var lastError = null;
    return function() {
        if (submitted) {
            throw lastError || new Error('This action is already being processed.');
        }
        submitted = true;
        return Promise.resolve()
            .then(task)
            .catch(function(error) {
                lastError = error;
                throw error;
            });
    };
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
        button.addEventListener('click', async function() {
            var card = button.closest('[data-quiz-id]');
            var message = card ? card.querySelector('.teacher-quiz-update-form .assessment-message') : null;
            if (!card || button.dataset.dialogPending === 'true') {
                return;
            }
            button.dataset.dialogPending = 'true';
            try {
                var result = await teacherDangerDialog({
                    title: 'Delete this quiz?',
                    subtitle: 'Quizzes with student history cannot be deleted.',
                    objectLabel: 'Quiz',
                    objectName: (card.querySelector('h3') || {}).textContent || 'Selected quiz',
                    notice: {
                        title: 'This removes the quiz setup',
                        text: 'Existing student history is protected by the server and will block deletion.'
                    },
                    acknowledgement: {
                        label: 'I understand this quiz will be removed when deletion is allowed.',
                        required: true
                    },
                    confirmText: 'Yes',
                    cancelText: 'No',
                    loadingText: 'Deleting quiz...',
                    onConfirm: teacherSingleConfirmTask(function() {
                        return teacherFetch('/api/teacher/quizzes/' + encodeURIComponent(card.dataset.quizId), {
                            method: 'DELETE'
                        }).then(function(response) {
                            return teacherAssessmentJson(response, 'Unable to delete quiz');
                        });
                    })
                }, message);
                if (result.confirmed) {
                    window.location.reload();
                }
            } catch (error) {
                teacherAssessmentMessage(message, error.message, 'error');
            } finally {
                delete button.dataset.dialogPending;
            }
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

    function markDirty(dirty) {
        state.dirty = dirty;
        panel.dataset.dirty = dirty ? 'true' : 'false';
        unsaved.hidden = !dirty;
    }

    function canLeaveEditor() {
        if (!state.dirty) {
            return Promise.resolve(true);
        }
        if (panel.dataset.leaveDialogPending === 'true') {
            return Promise.resolve(false);
        }
        panel.dataset.leaveDialogPending = 'true';
        return teacherOpenActionDialog({
            variant: 'warning',
            icon: 'quiz',
            eyebrow: 'Unsaved question',
            title: 'Discard unsaved question changes?',
            subtitle: 'Your current edits will be lost if you continue.',
            confirmText: 'Yes',
            cancelText: 'No',
            loadingText: 'Discarding changes...'
        }, message).then(function(result) {
            return Boolean(result.confirmed);
        }).catch(function(error) {
            teacherAssessmentMessage(message, error.message, 'error');
            return false;
        }).finally(function() {
            delete panel.dataset.leaveDialogPending;
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

    questionList.addEventListener('click', async function(event) {
        var button = event.target.closest('[data-question-id]');
        if (!button) {
            return;
        }
        var nextId = Number(button.dataset.questionId);
        if (!(await canLeaveEditor())) {
            return;
        }
        var selected = state.items.find(function(item) {
            return item.id === nextId;
        });
        if (selected) {
            showQuestion(selected);
        }
    });

    previousButton.addEventListener('click', async function() {
        if (state.page > 0 && await canLeaveEditor()) {
            loadPage(state.page - 1);
        }
    });
    nextButton.addEventListener('click', async function() {
        if (state.page < state.totalPages - 1 && await canLeaveEditor()) {
            loadPage(state.page + 1);
        }
    });
    pageButtons.addEventListener('click', async function(event) {
        var button = event.target.closest('[data-question-page]');
        if (!button) {
            return;
        }
        var requestedPage = Number(button.dataset.questionPage);
        if (!(await canLeaveEditor())) {
            return;
        }
        if (requestedPage >= 0
                && requestedPage < state.totalPages
                && requestedPage !== state.page) {
            loadPage(requestedPage);
        }
    });
    addButton.addEventListener('click', async function() {
        if (state.totalItems < 100 && await canLeaveEditor()) {
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
                ? Math.floor(state.totalItems / state.size)
                : state.page;
            return loadPage(targetPage, saved.id);
        }).catch(function(saveError) {
            teacherAssessmentMessage(message, saveError.message, 'error');
        });
    });

    deleteButton.addEventListener('click', async function() {
        var questionId = Number(form.elements.questionId.value);
        if (!questionId || deleteButton.dataset.dialogPending === 'true') {
            return;
        }
        deleteButton.dataset.dialogPending = 'true';
        try {
            var result = await teacherDangerDialog({
                title: 'Delete this question?',
                subtitle: 'This removes the question from this quiz bank.',
                objectLabel: 'Question',
                objectName: title.textContent || 'Selected question',
                notice: {
                    title: 'Question bank update',
                    text: 'Student attempts already created are handled by the server.'
                },
                acknowledgement: {
                    label: 'I understand this question will be deleted when allowed.',
                    required: true
                },
                confirmText: 'Yes',
                cancelText: 'No',
                loadingText: 'Deleting question...',
                onConfirm: teacherSingleConfirmTask(function() {
                    return teacherFetch('/api/teacher/questions/' + encodeURIComponent(questionId), {
                        method: 'DELETE'
                    }).then(function(response) {
                        return teacherAssessmentJson(response, 'Unable to delete question');
                    });
                })
            }, message);
            if (!result.confirmed) {
                return;
            }
            markDirty(false);
            var targetPage = state.items.length === 1 && state.page > 0
                ? state.page - 1
                : state.page;
            await loadPage(targetPage);
        } catch (error) {
            teacherAssessmentMessage(message, error.message, 'error');
        } finally {
            delete deleteButton.dataset.dialogPending;
        }
    });

    loadPage(0);
}
