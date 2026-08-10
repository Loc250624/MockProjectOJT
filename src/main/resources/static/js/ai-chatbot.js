'use strict';

(function () {
    if (window.__luminaAiChatbotInitialized) {
        return;
    }

    var roots = Array.from(document.querySelectorAll('[data-ai-chatbot-root]'));
    if (!roots.length) {
        return;
    }
    window.__luminaAiChatbotInitialized = true;

    roots.slice(1).forEach(function (duplicate) {
        duplicate.hidden = true;
    });

    var root = roots[0];
    var toggle = root.querySelector('[data-ai-chatbot-toggle]');
    var launcherStatus = root.querySelector('[data-ai-chatbot-launcher-status]');
    var panel = root.querySelector('[data-ai-chatbot-panel]');
    var closeButton = root.querySelector('[data-ai-chatbot-close]');
    var content = root.querySelector('[data-ai-chatbot-content]');
    var contextBadge = root.querySelector('[data-ai-chatbot-context-badge]');
    var messages = root.querySelector('[data-ai-chatbot-messages]');
    var greeting = root.querySelector('[data-ai-chatbot-greeting]');
    var quickActionsContainer = root.querySelector('[data-ai-chatbot-quick-actions]');
    var suggestionsToggle = root.querySelector('[data-ai-chatbot-suggestions-toggle]');
    var relatedActionsContainer = root.querySelector('[data-ai-chatbot-related-actions]');
    var relatedButtonsContainer = root.querySelector('[data-ai-chatbot-related-buttons]');
    var form = root.querySelector('[data-ai-chatbot-form]');
    var input = root.querySelector('[data-ai-chatbot-input]');
    var sendButton = root.querySelector('[data-ai-chatbot-send]');
    var status = root.querySelector('[data-ai-chatbot-status]');
    var headerStatus = root.querySelector('[data-ai-chatbot-header-status]');

    if (!toggle || !panel || !closeButton || !content || !messages || !greeting || !quickActionsContainer
        || !suggestionsToggle || !relatedActionsContainer || !relatedButtonsContainer || !form || !input) {
        return;
    }

    var authenticated = root.dataset.chatAuthenticated === 'true';
    var storage = safeSessionStorage();
    var statusStorage = safeLocalStorage();
    var scope = normalizeScope(root.dataset.chatScope);
    var accountId = normalizeScope(root.dataset.aiAccountId || (authenticated ? scope : ''));
    var statusStorageKey = authenticated && accountId ? 'lumina:ai-status:' + accountId : '';
    var conversationStorageKey = 'ai-chatbot:' + scope + ':conversation-id';
    var conversationId = authenticated ? null : (readStored(conversationStorageKey) || createConversationId());
    var history = [];
    var shownRelatedQuestions = [];
    var recentlyShownSuggestions = [];
    var sending = false;
    var previouslyFocused = null;
    var latestPageData = currentPageContext();

    if (!authenticated) {
        writeStored(conversationStorageKey, conversationId);
    }
    bootstrapAvailability();
    renderContextUi(latestPageData.uiContext);
    renderInitialQuestions();
    relatedActionsContainer.hidden = true;

    toggle.addEventListener('click', function () {
        setPanelOpen(panel.hidden);
    });
    closeButton.addEventListener('click', closePanel);

    suggestionsToggle.addEventListener('click', function () {
        if (sending) {
            return;
        }
        if (quickActionsContainer.hidden) {
            renderQuickActions(contextualActions('initial'), false);
            quickActionsContainer.hidden = false;
            relatedActionsContainer.hidden = true;
            suggestionsToggle.setAttribute('aria-expanded', 'true');
        } else {
            quickActionsContainer.hidden = true;
            suggestionsToggle.setAttribute('aria-expanded', 'false');
        }
        updateSuggestionsToggle();
        scrollContent();
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (!sending) {
            sendChat(input.value, '');
        }
    });

    input.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (panel.hidden) {
            return;
        }
        if (event.key === 'Escape') {
            event.preventDefault();
            closePanel();
            return;
        }
        if (event.key === 'Tab') {
            keepFocusInPanel(event);
        }
    });

    function setPanelOpen(isOpen) {
        var wasHidden = panel.hidden;
        panel.hidden = !isOpen;
        toggle.hidden = isOpen;
        toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        if (isOpen) {
            latestPageData = currentPageContext();
            renderContextUi(latestPageData.uiContext);
            if (wasHidden) {
                previouslyFocused = document.activeElement;
            }
            renderInitialQuestions();
            input.focus();
            scrollMessages();
        }
    }

    function closePanel() {
        setPanelOpen(false);
        if (previouslyFocused && previouslyFocused !== toggle && document.contains(previouslyFocused)) {
            previouslyFocused.focus();
        } else {
            toggle.focus();
        }
    }

    function keepFocusInPanel(event) {
        var focusable = Array.from(panel.querySelectorAll(
            'button:not([disabled]), textarea:not([disabled]), input:not([disabled]), a[href]'
        )).filter(function (element) {
            return !element.hidden;
        });
        if (!focusable.length) {
            return;
        }
        var first = focusable[0];
        var last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    function renderInitialQuestions() {
        var hasUserHistory = history.some(function (turn) {
            return turn.role === 'user';
        });
        renderQuickActions(contextualActions('initial'), hasUserHistory);
        quickActionsContainer.hidden = hasUserHistory;
        suggestionsToggle.setAttribute('aria-expanded', hasUserHistory ? 'false' : 'true');
        updateSuggestionsToggle();
    }

    function beginSuggestionTransition() {
        quickActionsContainer.hidden = true;
        relatedActionsContainer.hidden = true;
        suggestionsToggle.setAttribute('aria-expanded', 'false');
        updateSuggestionsToggle();
    }

    function setLoading(isLoading) {
        sending = isLoading;
        input.disabled = isLoading;
        if (sendButton) {
            sendButton.disabled = isLoading;
        }
        Array.from(root.querySelectorAll('[data-ai-chatbot-action], [data-ai-chatbot-related-action]'))
            .forEach(function (button) {
                button.disabled = isLoading;
            });
        if (isLoading) {
            setStatus('', false);
        }
    }

    function bootstrapAvailability() {
        var stored = readStoredAvailability();
        renderAvailability(stored ? stored.state : 'checking');
        refreshAvailability();
        if (statusStorageKey) {
            window.addEventListener('storage', function (event) {
                if (event.key !== statusStorageKey) {
                    return;
                }
                var next = parseStoredAvailability(event.newValue);
                if (next) {
                    renderAvailability(next.state);
                }
            });
        }
    }

    function refreshAvailability() {
        var requestStartedAt = Date.now();
        var controller = window.AbortController ? new AbortController() : null;
        var timeoutId = controller ? window.setTimeout(function () {
            controller.abort();
        }, 3500) : null;
        return fetch('/api/ai-chatbot/status', {
            method: 'GET',
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
            cache: 'no-store',
            signal: controller ? controller.signal : undefined
        }).then(function (response) {
            return response.json().catch(function () {
                return {};
            }).then(function (body) {
                if (!response.ok) {
                    return { state: 'temporary_unavailable', updatedAt: requestStartedAt };
                }
                var payload = body && body.data ? body.data : body;
                return {
                    state: payload && payload.status === 'available' ? 'available' : 'temporary_unavailable',
                    updatedAt: Number.isFinite(payload && payload.updatedAt) ? payload.updatedAt : Date.now()
                };
            });
        }).catch(function () {
            return { state: 'temporary_unavailable', updatedAt: requestStartedAt };
        }).then(function (snapshot) {
            var stored = readStoredAvailability();
            if (stored && stored.updatedAt > snapshot.updatedAt) {
                renderAvailability(stored.state);
                return stored.state;
            }
            updateAvailability(snapshot.state, true, snapshot.updatedAt);
            return snapshot.state;
        }).finally(function () {
            if (timeoutId) {
                window.clearTimeout(timeoutId);
            }
        });
    }

    function updateAvailability(nextState, persist, observedAt) {
        var state = isAvailabilityState(nextState) ? nextState : 'temporary_unavailable';
        renderAvailability(state);
        if (persist) {
            writeStoredAvailability(state, observedAt);
        }
    }

    function renderAvailability(nextState) {
        var state = nextState === 'available' || nextState === 'temporary_unavailable' ? nextState : 'checking';
        var label = availabilityLabel(state);
        root.dataset.aiChatbotState = state;
        root.dataset.aiStatus = state;
        root.setAttribute('aria-label', label);
        root.setAttribute('title', label);
        if (launcherStatus) {
            launcherStatus.classList.toggle('is-available', state === 'available');
            launcherStatus.classList.toggle('is-checking', state === 'checking');
            launcherStatus.classList.toggle('is-unavailable', state === 'temporary_unavailable');
            launcherStatus.setAttribute('aria-label', label);
            launcherStatus.setAttribute('title', label);
        }
        if (!headerStatus) {
            return;
        }
        if (state === 'available') {
            headerStatus.textContent = 'Available';
        } else if (state === 'temporary_unavailable') {
            headerStatus.textContent = 'Temporarily Unavailable';
        } else {
            headerStatus.textContent = 'Checking availability...';
        }
        headerStatus.setAttribute('title', label);
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
            setStatus('Enter a question about the website.', true);
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
        beginSuggestionTransition();
        input.value = '';
        setLoading(true);

        var pageData = currentPageContext();
        latestPageData = pageData;
        var requestBody = {
            conversationId: conversationId,
            lessonId: pageData.lessonId,
            message: message,
            action: action || '',
            pageContext: pageData.pageContext
        };
        if (!authenticated) {
            requestBody.history = requestHistory;
            requestBody.recentMessages = requestHistory;
        }
        fetch('/api/ai-chatbot/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(requestBody)
        }).then(readChatResponse).then(function (apiResponse) {
            var data = apiResponse.data || {};
            if (isUuid(data.conversationId) && data.conversationId !== conversationId) {
                conversationId = data.conversationId;
                if (!authenticated) {
                    writeStored(conversationStorageKey, conversationId);
                }
            }
            var answer = data.answer || 'AI Chatbot does not have an answer yet.';
            appendMessage('assistant', answer);
            history.push({ role: 'assistant', content: answer });
            history = history.slice(-50);
            renderRelatedQuestions(data.suggestedQuestions || []);
            if (data.refused) {
                setStatus('This request is outside the available website guidance.', true);
            } else {
                setStatus('', false);
            }
            updateAvailability('available', true);
        }).catch(function (error) {
            if (error && (error.providerUnavailable || error.name === 'TypeError')) {
                updateAvailability('temporary_unavailable', true);
            }
            setStatus(error.message || 'AI Chatbot could not respond. Please try again later.', true);
            if (relatedButtonsContainer.childElementCount > 0) {
                relatedActionsContainer.hidden = false;
            }
        }).finally(function () {
            setLoading(false);
            input.focus();
        });
    }

    function appendMessage(role, text) {
        var item = document.createElement('div');
        item.className = 'ai-chatbot-message ' + (role === 'user' ? 'user' : 'assistant');
        item.setAttribute('data-ai-chatbot-persisted-message', '');

        if (role === 'assistant') {
            var avatar = document.createElement('span');
            avatar.className = 'ai-chatbot-message-avatar';
            avatar.setAttribute('aria-hidden', 'true');
            avatar.appendChild(svgIcon('sparkles'));
            item.appendChild(avatar);
        }

        var body = document.createElement('div');
        body.className = 'ai-chatbot-message-body';
        var label = document.createElement('strong');
        label.textContent = role === 'user' ? 'You' : 'AI Chatbot';
        var bubble = document.createElement('div');
        bubble.className = 'ai-chatbot-bubble';
        if (role === 'assistant') {
            renderMarkdown(bubble, text || '');
        } else {
            bubble.textContent = text || '';
        }
        body.appendChild(label);
        body.appendChild(bubble);
        if (role === 'assistant') {
            body.appendChild(messageTools(text || ''));
        }
        item.appendChild(body);
        messages.appendChild(item);
        scrollMessages();
    }

    function messageTools(text) {
        var tools = document.createElement('div');
        tools.className = 'ai-chatbot-message-tools';
        [
            { label: 'Helpful', icon: 'thumbs-up', action: markFeedback },
            { label: 'Not helpful', icon: 'thumbs-down', action: markFeedback },
            { label: 'Copy', icon: 'copy', action: copyAnswer }
        ].forEach(function (tool) {
            var button = document.createElement('button');
            button.type = 'button';
            button.appendChild(svgIcon(tool.icon));
            button.appendChild(document.createTextNode(tool.label));
            button.addEventListener('click', function () {
                tool.action(button, text);
            });
            tools.appendChild(button);
        });
        return tools;
    }

    function markFeedback(button) {
        Array.from(button.parentElement.querySelectorAll('button')).forEach(function (sibling) {
            sibling.removeAttribute('aria-pressed');
        });
        button.setAttribute('aria-pressed', 'true');
        setStatus('Thanks for the feedback.', false);
    }

    function copyAnswer(button, text) {
        var value = String(text || '');
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(value).then(function () {
                setStatus('Copied.', false);
            }).catch(function () {
                fallbackCopy(value);
            });
            return;
        }
        fallbackCopy(value);
        button.setAttribute('aria-pressed', 'true');
    }

    function fallbackCopy(value) {
        var helper = document.createElement('textarea');
        helper.value = value;
        helper.setAttribute('readonly', '');
        helper.style.position = 'fixed';
        helper.style.left = '-9999px';
        root.appendChild(helper);
        helper.select();
        try {
            document.execCommand('copy');
            setStatus('Copied.', false);
        } catch (error) {
            setStatus('Copy is unavailable in this browser.', true);
        }
        helper.remove();
    }

    function renderQuickActions(actions, excludeRecentlyShown) {
        quickActionsContainer.replaceChildren();
        selectUniqueActions(actions, excludeRecentlyShown ? recentSuggestionKeys() : [], 4).forEach(function (action) {
            quickActionsContainer.appendChild(actionButton(action, false));
            rememberSuggestion(action.label);
        });
        quickActionsContainer.hidden = quickActionsContainer.childElementCount === 0;
    }

    function renderRelatedQuestions(candidates) {
        quickActionsContainer.hidden = true;
        var fallbackActions = contextualActions('followup');
        var candidateActions = (Array.isArray(candidates) ? candidates : []).map(function (candidate) {
            return {
                icon: 'message-circle',
                label: String(candidate || ''),
                prompt: String(candidate || ''),
                action: ''
            };
        }).concat(fallbackActions);
        var excluded = history.filter(function (turn) {
            return turn.role === 'user';
        }).map(function (turn) {
            return turn.content;
        }).concat(shownRelatedQuestions).concat(recentlyShownSuggestions);
        var selected = selectUniqueActions(candidateActions, excluded, 3);
        relatedButtonsContainer.replaceChildren();
        selected.forEach(function (action) {
            relatedButtonsContainer.appendChild(actionButton(action, true));
            shownRelatedQuestions.push(action.label);
            rememberSuggestion(action.label);
        });
        relatedActionsContainer.hidden = selected.length === 0;
        updateSuggestionsToggle();
        scrollContent();
    }

    function actionButton(action, related) {
        var button = document.createElement('button');
        button.type = 'button';
        button.setAttribute(related ? 'data-ai-chatbot-related-action' : 'data-ai-chatbot-action', action.action || '');
        button.disabled = sending;
        button.appendChild(svgIcon(action.icon || 'message-circle'));
        var label = document.createElement('span');
        label.textContent = action.label;
        button.appendChild(label);
        button.addEventListener('click', function () {
            if (!sending) {
                sendChat(action.prompt || action.label, action.action || '');
            }
        });
        return button;
    }

    function updateSuggestionsToggle() {
        var hasActions = contextualActions('initial').length > 0;
        suggestionsToggle.hidden = !hasActions || (!history.some(function (turn) {
            return turn.role === 'user';
        }) && !quickActionsContainer.hidden);
    }

    function selectUniqueActions(candidates, excluded, maxItems) {
        var selected = [];
        var existing = Array.isArray(excluded) ? excluded.slice() : [];
        (Array.isArray(candidates) ? candidates : []).some(function (rawAction) {
            var label = String(rawAction.label || '').normalize('NFKC').replace(/\s+/g, ' ').trim();
            var prompt = String(rawAction.prompt || label).normalize('NFKC').replace(/\s+/g, ' ').trim();
            if (!label
                    || label.length > 72
                    || prompt.length > 220
                    || isAssistantLedPrompt(label)
                    || isDuplicateQuestion(label, existing.concat(selected.map(function (item) {
                        return item.label;
                    })))) {
                return false;
            }
            selected.push({
                icon: rawAction.icon || 'message-circle',
                label: label,
                prompt: prompt,
                action: rawAction.action || ''
            });
            return selected.length >= maxItems;
        });
        return selected;
    }

    function contextualActions(kind) {
        var pageData = latestPageData || currentPageContext();
        var context = pageData.uiContext || {};
        var pageType = context.pageType || 'navigation';
        var role = context.role || 'ANONYMOUS';
        var isFollowup = kind === 'followup';
        if (role === 'TEACHER') {
            return teacherActions(pageType, isFollowup);
        }
        if (role === 'ADMIN') {
            return adminActions(pageType, isFollowup);
        }
        if (pageType === 'lesson') {
            return [
                action('sparkles', 'Explain this lesson', 'Explain this lesson in simple terms.', 'EXPLAIN_LESSON'),
                action('list', 'Summarize', 'Summarize the current lesson.', 'SUMMARY'),
                action('brain', 'Test my understanding', 'Test my understanding of this lesson with one or two questions.', 'QUIZ_ME'),
                action('lightbulb', 'Give me a hint', 'Give me a hint about the current lesson without giving away quiz answers.', 'HINT')
            ];
        }
        if (pageType === 'quiz') {
            return [
                action('lightbulb', 'Give me a hint', 'Give me a hint for this quiz topic without revealing the correct answer.', 'QUIZ_HINT'),
                action('brain', 'Review the concept', 'Review the concept behind this quiz question.', 'CONCEPT_REVIEW'),
                action('code', 'Similar example', 'Show a similar example and explain the reasoning.', 'SIMILAR_EXAMPLE')
            ];
        }
        if (pageType === 'course') {
            return [
                action('book-open', 'What will I learn?', 'What will I learn from this course?', 'COURSE_OUTCOMES'),
                action('clock', 'Course duration', 'How long is this course?', 'COURSE_DURATION'),
                action('target', 'Is this for me?', 'Is this course suitable for my goals?', 'COURSE_FIT')
            ];
        }
        if (pageType === 'payment') {
            return [
                action('credit-card', 'How payment works', 'Explain how payment works on this page.', 'PAYMENT_HELP'),
                action('shield', 'Is payment secure?', 'Is payment secure on LumiNa?', 'PAYMENT_SECURITY'),
                action('file-text', 'Payment status help', 'Help me understand payment status on LumiNa.', 'PAYMENT_STATUS')
            ];
        }
        if (pageType === 'certificate') {
            return [
                action('award', 'Certificate steps', 'How do certificates work on LumiNa?', 'CERTIFICATE_HELP'),
                action('target', 'Eligibility', 'How can I check certificate eligibility?', 'CERTIFICATE_ELIGIBILITY'),
                action('file-text', 'Share certificate', 'How can I share a certificate from LumiNa?', 'CERTIFICATE_SHARE')
            ];
        }
        if (pageType === 'profile') {
            return [
                action('user', 'Profile help', 'What can I manage from my profile?', 'PROFILE_HELP'),
                action('shield', 'Account security', 'How do account and sign-in settings work here?', 'ACCOUNT_SECURITY'),
                action('message-circle', 'Where to update info', 'Where can I update my visible profile information?', 'PROFILE_NAVIGATION')
            ];
        }
        if (pageType === 'authentication') {
            return [
                action('user', 'Sign-in help', 'Help me understand sign-in and registration options.', 'AUTH_HELP'),
                action('shield', 'Login security', 'How does login security work on LumiNa?', 'AUTH_SECURITY'),
                action('message-circle', 'Account access', 'What should I do if I cannot access my account?', 'AUTH_ACCESS')
            ];
        }
        if (isFollowup) {
            return [
                action('message-circle', 'Show page guidance', 'What can I do from this page?', 'PAGE_HELP'),
                action('book-open', 'Find learning features', 'Where are the main learning features?', 'NAVIGATION_HELP'),
                action('target', 'Suggest next step', 'What is a sensible next step on this page?', 'NEXT_STEP')
            ];
        }
        return [
            action('message-circle', 'Show page guidance', 'What can I do from this page?', 'PAGE_HELP'),
            action('book-open', 'Find courses', 'Where can I find courses?', 'FIND_COURSES'),
            action('award', 'Certificates', 'How do certificates work?', 'CERTIFICATE_HELP')
        ];
    }

    function teacherActions(pageType) {
        if (pageType === 'quiz') {
            return [
                action('brain', 'Quiz setup help', 'Help me understand quiz management on this page.', 'TEACHER_QUIZ_HELP'),
                action('book-open', 'Lesson alignment', 'How should I align quizzes with lessons?', 'TEACHER_LESSON_HELP'),
                action('message-circle', 'Teacher navigation', 'Where can I manage related teacher features?', 'TEACHER_NAVIGATION')
            ];
        }
        if (pageType === 'course' || pageType === 'lesson') {
            return [
                action('book-open', 'Course management', 'Help me understand course and lesson management here.', 'TEACHER_COURSE_HELP'),
                action('file-text', 'Content guidance', 'What existing teacher tools can help with this content?', 'TEACHER_CONTENT_HELP'),
                action('message-circle', 'Teacher navigation', 'Where can I manage related teacher features?', 'TEACHER_NAVIGATION')
            ];
        }
        return [
            action('message-circle', 'Teacher screen help', 'What can teachers do from this screen?', 'TEACHER_SCREEN_HELP'),
            action('book-open', 'Course tools', 'Where are course and lesson tools?', 'TEACHER_COURSE_HELP'),
            action('target', 'Student progress', 'Where can I review student progress?', 'TEACHER_PROGRESS_HELP')
        ];
    }

    function adminActions(pageType) {
        if (pageType === 'payment') {
            return [
                action('credit-card', 'Payment workflows', 'Explain the admin payment workflow on LumiNa.', 'ADMIN_PAYMENT_HELP'),
                action('file-text', 'Transactions', 'Where can admins review transaction details?', 'ADMIN_TRANSACTION_HELP'),
                action('shield', 'Refund guidance', 'How should admins review refund-related screens?', 'ADMIN_REFUND_HELP')
            ];
        }
        return [
            action('message-circle', 'Admin screen help', 'What can admins do from this screen?', 'ADMIN_SCREEN_HELP'),
            action('user', 'User management', 'Where can admins manage users and roles?', 'ADMIN_USER_HELP'),
            action('book-open', 'Course governance', 'How does course approval or moderation work?', 'ADMIN_COURSE_HELP')
        ];
    }

    function action(icon, label, prompt, code) {
        return { icon: icon, label: label, prompt: prompt, action: code };
    }

    function renderContextUi(context) {
        var greetingBubble = greeting.querySelector('.ai-chatbot-bubble');
        if (greetingBubble) {
            greetingBubble.textContent = context.greeting;
        }
        if (contextBadge) {
            contextBadge.replaceChildren();
            if (context.badge) {
                contextBadge.appendChild(svgIcon(context.badgeIcon || 'message-circle'));
                contextBadge.appendChild(document.createTextNode(context.badge));
                contextBadge.hidden = false;
            } else {
                contextBadge.hidden = true;
            }
        }
    }

    function currentPageContext() {
        var path = safePath(window.location.pathname);
        var params = new URLSearchParams(window.location.search);
        var role = currentRole(path);
        var pageKey = 'navigation';
        var pageType = 'navigation';
        var entityType = 'navigation';
        var entityId = '';
        var lessonId = null;
        var courseId = numericId(params.get('courseId') || params.get('id'));
        var lessonTitle = '';
        var courseTitle = '';
        var badge = '';
        var badgeIcon = 'message-circle';
        var greetingText = 'Hello! I can help you navigate LumiNa, understand website features, and explain authorized lesson content. What would you like to know?';

        if (path === '/') {
            pageKey = 'public-home';
        } else if (path.indexOf('/auth/') === 0) {
            pageKey = 'authentication';
            pageType = 'authentication';
            entityType = 'authentication';
            badge = 'Account access';
            badgeIcon = 'user';
            greetingText = 'I can help explain LumiNa sign-in, registration, and account access options.';
        } else if (path.indexOf('/student/learning') === 0) {
            pageKey = 'student-learning';
            pageType = 'lesson';
            entityType = 'lesson';
            entityId = numericId(params.get('lessonId')) || dataValue('[data-learning-video], [data-quiz-panel], [data-lesson-complete]', 'lessonId');
            lessonId = entityId ? Number(entityId) : null;
            courseId = courseId || dataValue('[data-learning-video], [data-quiz-panel], [data-lesson-complete], [data-course-duration-display]', 'courseId');
            courseTitle = textFrom('main .learning-hero h1');
            lessonTitle = textFrom('main .learning-lesson-header h2').replace(/^\d+\.\s*/, '');
            badge = compactJoin([courseTitle, lessonTitle], ' > ');
            badgeIcon = 'book-open';
            greetingText = lessonTitle
                ? 'You are learning ' + lessonTitle + '. Ask me about this lesson or choose a suggestion below.'
                : 'You are in the course player. Ask me about the current lesson or choose a suggestion below.';
        } else if (path.indexOf('/quiz') >= 0) {
            pageKey = path.indexOf('/teacher/') === 0 ? 'teacher-quiz' : 'student-quiz';
            pageType = 'quiz';
            entityType = 'quiz';
            entityId = numericId(params.get('quizId') || params.get('id')) || dataValue('body', 'quizId');
            courseId = courseId || dataValue('body', 'courseId');
            lessonId = Number(dataValue('body', 'lessonId') || 0) || null;
            lessonTitle = textFrom('.dedicated-quiz-module');
            badge = compactJoin([textFrom('main h1') || textFrom('header h1') || 'Quiz', lessonTitle], ' > ');
            badgeIcon = 'brain';
            greetingText = 'I can help with concepts, hints, and reasoning for this quiz without giving away active graded answers.';
        } else if (path.indexOf('code-assignment') >= 0 || path.indexOf('/assignments') >= 0) {
            pageKey = path.indexOf('/teacher/') === 0 ? 'teacher-assignments' : 'student-coding-assignment';
            pageType = 'lesson';
            entityType = 'coding-assignment';
            entityId = numericId(params.get('id') || params.get('assignmentId'));
            badge = 'Coding assignment';
            badgeIcon = 'code';
        } else if (path.indexOf('/certificates') >= 0) {
            pageKey = 'certificates';
            pageType = 'certificate';
            entityType = 'certificate';
            entityId = numericId(params.get('id'));
            badge = 'Certificates';
            badgeIcon = 'award';
        } else if (path.indexOf('/payment') >= 0 || path.indexOf('/checkout') >= 0 || path.indexOf('/transactions') >= 0 || path.indexOf('/refunds') >= 0) {
            pageKey = 'payment';
            pageType = 'payment';
            entityType = 'payment';
            entityId = numericId(params.get('id') || params.get('orderId'));
            badge = 'Payment';
            badgeIcon = 'credit-card';
            greetingText = 'I can explain LumiNa payment, checkout, and payment-status guidance using only what is visible here.';
        } else if (path.indexOf('/blogs') >= 0) {
            pageKey = 'blogs';
            pageType = 'blog';
            entityType = 'blog';
            entityId = numericId(params.get('id')) || pathId(path);
            badge = 'Blog';
            badgeIcon = 'file-text';
        } else if (path.indexOf('/profile') >= 0) {
            pageKey = 'profile';
            pageType = 'profile';
            entityType = 'profile';
            badge = 'Profile';
            badgeIcon = 'user';
        } else if (path.indexOf('/dashboard') >= 0) {
            pageKey = path.replace(/^\/+/, '').replace(/\//g, '-');
            pageType = 'dashboard';
            entityType = 'dashboard';
            badge = role === 'ANONYMOUS' ? 'Dashboard' : titleCase(role) + ' dashboard';
            badgeIcon = 'target';
        } else if (path.indexOf('/courses') >= 0 || path.indexOf('/learning') >= 0) {
            pageKey = path.indexOf('/teacher/') === 0 ? 'teacher-courses' : 'courses';
            pageType = 'course';
            entityType = 'course';
            entityId = courseId || pathId(path);
            courseTitle = textFrom('main h1') || textFrom('.cd-hero h1') || textFrom('h1');
            badge = courseTitle || 'Course Details';
            badgeIcon = 'book-open';
            greetingText = courseTitle
                ? 'You are viewing ' + courseTitle + '. Ask me about this course or choose a suggestion below.'
                : 'You are viewing course details. Ask me about the course or choose a suggestion below.';
        }

        return {
            lessonId: lessonId,
            pageContext: {
                path: path,
                pageKey: safeToken(pageKey),
                entityType: safeToken(entityType),
                entityId: String(entityId || ''),
                visibleText: collectVisiblePageText(pageKey)
            },
            uiContext: {
                role: role,
                pageType: pageType,
                courseId: String(courseId || ''),
                courseTitle: courseTitle,
                lessonId: String(lessonId || ''),
                lessonTitle: lessonTitle,
                badge: badge,
                badgeIcon: badgeIcon,
                greeting: greetingText
            }
        };
    }

    function collectVisiblePageText(pageKey) {
        var selectors = [
            'main h1',
            'main h2',
            'main .section-lead',
            'main [data-ai-page-context]'
        ];
        if (pageKey === 'blogs') {
            selectors.push('main article', 'main tbody tr');
        }
        if (pageKey === 'courses' || pageKey === 'public-home' || pageKey === 'teacher-courses') {
            selectors.push('main .public-course-card', 'main .course-card');
        }

        var snippets = [];
        var seen = new Set();
        var totalChars = 0;
        Array.from(document.querySelectorAll(selectors.join(','))).some(function (element) {
            if (!isAllowedContextElement(element)) {
                return false;
            }
            var text = normalizeVisibleText(element.innerText || element.textContent);
            if (!text || seen.has(text)) {
                return false;
            }
            var snippet = text.slice(0, 420);
            if (totalChars + snippet.length > 3600) {
                return true;
            }
            seen.add(text);
            snippets.push(snippet);
            totalChars += snippet.length;
            return snippets.length >= 14;
        });
        return snippets;
    }

    function isAllowedContextElement(element) {
        if (!element || element.closest('[hidden], [aria-hidden="true"]')) {
            return false;
        }
        if (element.closest('[data-ai-chatbot-root], form, nav, header, footer')) {
            return false;
        }
        var summaryContainer = element.closest('[data-ai-page-context], .public-course-card, .course-card');
        if (summaryContainer && summaryContainer !== element) {
            return false;
        }
        var style = window.getComputedStyle ? window.getComputedStyle(element) : null;
        return !style || (style.display !== 'none' && style.visibility !== 'hidden');
    }

    function renderMarkdown(container, rawText) {
        var lines = String(rawText || '').replace(/\r\n?/g, '\n').split('\n');
        var paragraphLines = [];
        var list = null;
        var listType = '';
        var codeLines = [];
        var inCodeBlock = false;

        function flushParagraph() {
            if (!paragraphLines.length) {
                return;
            }
            var paragraph = document.createElement('p');
            appendInlineMarkdown(paragraph, paragraphLines.join(' '));
            container.appendChild(paragraph);
            paragraphLines = [];
        }

        function flushList() {
            if (list) {
                container.appendChild(list);
                list = null;
                listType = '';
            }
        }

        function flushCodeBlock() {
            var pre = document.createElement('pre');
            var code = document.createElement('code');
            code.textContent = codeLines.join('\n');
            pre.appendChild(code);
            container.appendChild(pre);
            codeLines = [];
        }

        lines.forEach(function (line) {
            if (/^\s*```/.test(line)) {
                if (inCodeBlock) {
                    flushCodeBlock();
                    inCodeBlock = false;
                } else {
                    flushParagraph();
                    flushList();
                    inCodeBlock = true;
                    codeLines = [];
                }
                return;
            }
            if (inCodeBlock) {
                codeLines.push(line);
                return;
            }

            var unordered = line.match(/^\s*[-*+]\s+(.+)$/);
            var ordered = line.match(/^\s*\d+[.)]\s+(.+)$/);
            var heading = line.match(/^\s{0,3}#{1,3}\s+(.+)$/);
            var itemText = unordered ? unordered[1] : (ordered ? ordered[1] : '');
            var nextListType = unordered ? 'ul' : (ordered ? 'ol' : '');

            if (!line.trim()) {
                flushParagraph();
                flushList();
                return;
            }
            if (heading) {
                flushParagraph();
                flushList();
                var headingParagraph = document.createElement('p');
                headingParagraph.className = 'ai-chatbot-markdown-heading';
                appendInlineMarkdown(headingParagraph, heading[1]);
                container.appendChild(headingParagraph);
                return;
            }
            if (nextListType) {
                flushParagraph();
                if (!list || listType !== nextListType) {
                    flushList();
                    list = document.createElement(nextListType);
                    listType = nextListType;
                }
                var item = document.createElement('li');
                appendInlineMarkdown(item, itemText);
                list.appendChild(item);
                return;
            }
            flushList();
            paragraphLines.push(line.trim());
        });

        if (inCodeBlock) {
            flushCodeBlock();
        }
        flushParagraph();
        flushList();
        if (!container.hasChildNodes()) {
            container.textContent = '';
        }
    }

    function appendInlineMarkdown(container, text) {
        var value = String(text || '');
        var pattern = /(`([^`]+)`)|(\*\*|__)(.+?)\3/g;
        var lastIndex = 0;
        var match;

        while ((match = pattern.exec(value)) !== null) {
            if (match.index > lastIndex) {
                container.appendChild(document.createTextNode(value.slice(lastIndex, match.index)));
            }
            if (match[2]) {
                var code = document.createElement('code');
                code.textContent = match[2];
                container.appendChild(code);
            } else {
                var strong = document.createElement('strong');
                strong.textContent = match[4];
                container.appendChild(strong);
            }
            lastIndex = pattern.lastIndex;
        }
        if (lastIndex < value.length) {
            container.appendChild(document.createTextNode(value.slice(lastIndex)));
        }
    }

    function isAssistantLedPrompt(value) {
        return /^(?:do you (?:need|want|have)|would you like|are you (?:interested|ready|curious)|can i help|shall i)\b/i
            .test(String(value || '').trim());
    }

    function isDuplicateQuestion(candidate, existing) {
        var normalizedCandidate = normalizeQuestion(candidate);
        if (!normalizedCandidate) {
            return true;
        }
        return existing.some(function (item) {
            var normalizedExisting = normalizeQuestion(item);
            return normalizedCandidate === normalizedExisting
                || jaccardSimilarity(normalizedCandidate, normalizedExisting) >= 0.82;
        });
    }

    function normalizeQuestion(value) {
        return String(value || '')
            .normalize('NFKC')
            .toLocaleLowerCase()
            .replace(/[\p{P}\p{S}]+/gu, ' ')
            .replace(/\s+/g, ' ')
            .trim();
    }

    function jaccardSimilarity(left, right) {
        var leftTokens = new Set(normalizeQuestion(left).split(' ').filter(Boolean));
        var rightTokens = new Set(normalizeQuestion(right).split(' ').filter(Boolean));
        if (!leftTokens.size || !rightTokens.size) {
            return 0;
        }
        var intersection = 0;
        leftTokens.forEach(function (token) {
            if (rightTokens.has(token)) {
                intersection += 1;
            }
        });
        return intersection / (leftTokens.size + rightTokens.size - intersection);
    }

    function restoreAuthenticatedHistory() {
        setLoading(true);
        setStatus('Restoring chat history...', false);
        fetch('/api/ai-chatbot/conversations/latest', {
            credentials: 'same-origin'
        }).then(readApiResponse).then(function (latestResponse) {
            var latest = latestResponse.data || null;
            if (!latest || !isUuid(latest.conversationId)) {
                greeting.hidden = false;
                history = [];
                renderInitialQuestions();
                return null;
            }
            conversationId = latest.conversationId;
            renderRelatedQuestions(latest.suggestedQuestions || []);
            return fetch('/api/ai-chatbot/conversations/' + encodeURIComponent(conversationId)
                + '/messages?limit=50', {
                credentials: 'same-origin'
            }).then(readApiResponse);
        }).then(function (messagesResponse) {
            if (!messagesResponse || !messagesResponse.data) {
                setStatus('', false);
                return;
            }
            var restoredMessages = Array.isArray(messagesResponse.data.messages)
                ? messagesResponse.data.messages
                : [];
            if (!restoredMessages.length) {
                setStatus('', false);
                return;
            }
            Array.from(messages.querySelectorAll('[data-ai-chatbot-persisted-message]')).forEach(function (message) {
                message.remove();
            });
            greeting.hidden = true;
            history = [];
            restoredMessages.forEach(function (message) {
                var role = String(message.role || '').toLowerCase() === 'user' ? 'user' : 'assistant';
                appendMessage(role, message.content || '');
                history.push({ role: role, content: message.content || '' });
            });
            history = history.slice(-50);
            renderInitialQuestions();
            setStatus('', false);
        }).catch(function () {
            setStatus('Chat history could not be restored. You can still ask a question.', true);
        }).finally(function () {
            setLoading(false);
        });
    }

    function readChatResponse(response) {
        return response.json().catch(function () {
            return { message: 'AI Chatbot could not respond. Please try again later.' };
        }).then(function (body) {
            if (!response.ok) {
                var error = new Error(body.message || 'AI Chatbot could not respond. Please try again later.');
                error.providerUnavailable = response.status === 503 || response.status >= 500;
                throw error;
            }
            return body;
        });
    }

    function readApiResponse(response) {
        return response.json().catch(function () {
            return { message: 'AI Chatbot could not load chat history.' };
        }).then(function (body) {
            if (!response.ok) {
                throw new Error(body.message || 'AI Chatbot could not load chat history.');
            }
            return body;
        });
    }

    function svgIcon(name) {
        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('focusable', 'false');
        svg.setAttribute('aria-hidden', 'true');
        iconPaths(name).forEach(function (pathValue) {
            var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('d', pathValue);
            svg.appendChild(path);
        });
        return svg;
    }

    function iconPaths(name) {
        var icons = {
            'award': ['M12 15a5 5 0 1 0 0-10 5 5 0 0 0 0 10z', 'M8.5 14.5 7 22l5-3 5 3-1.5-7.5'],
            'book-open': ['M4 5h6a3 3 0 0 1 3 3v13a3 3 0 0 0-3-3H4z', 'M20 5h-6a3 3 0 0 0-3 3v13a3 3 0 0 1 3-3h6z'],
            'brain': ['M8 6a3 3 0 0 1 5.5-1.7A3 3 0 0 1 19 6v1a3 3 0 0 1 1 5.5 3 3 0 0 1-2 5.2V18a3 3 0 0 1-5.5 1.7A3 3 0 0 1 7 18v-.3a3 3 0 0 1-2-5.2A3 3 0 0 1 6 7V6a3 3 0 0 1 2-2.8'],
            'clock': ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z', 'M12 6v6l4 2'],
            'code': ['M8 9 4 12l4 3', 'M16 9l4 3-4 3', 'M14 5l-4 14'],
            'copy': ['M9 9h10v10H9z', 'M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1'],
            'credit-card': ['M3 6h18v12H3z', 'M3 10h18', 'M7 15h3'],
            'file-text': ['M6 2h8l4 4v16H6z', 'M14 2v4h4', 'M8 13h8', 'M8 17h6'],
            'lightbulb': ['M9 18h6', 'M10 22h4', 'M8 14a6 6 0 1 1 8 0c-1 1-1 2-1 3H9c0-1 0-2-1-3z'],
            'list': ['M8 6h13', 'M8 12h13', 'M8 18h13', 'M3 6h.01', 'M3 12h.01', 'M3 18h.01'],
            'message-circle': ['M21 11.5a8.5 8.5 0 0 1-12.7 7.4L3 21l2.1-5.1A8.5 8.5 0 1 1 21 11.5z'],
            'shield': ['M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'],
            'sparkles': ['M12 3l1.2 3.8L17 8l-3.8 1.2L12 13l-1.2-3.8L7 8l3.8-1.2L12 3z', 'M19 14l.7 2.3L22 17l-2.3.7L19 20l-.7-2.3L16 17l2.3-.7L19 14z', 'M5 14l.7 2.3L8 17l-2.3.7L5 20l-.7-2.3L2 17l2.3-.7L5 14z'],
            'target': ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z', 'M12 18a6 6 0 1 0 0-12 6 6 0 0 0 0 12z', 'M12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4z'],
            'thumbs-down': ['M10 15v4a2 2 0 0 0 2 2l4-8V3H5.7a2 2 0 0 0-2 1.7L2 13a2 2 0 0 0 2 2h6z', 'M16 3h3a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2h-3'],
            'thumbs-up': ['M14 9V5a2 2 0 0 0-2-2L8 11v10h10.3a2 2 0 0 0 2-1.7L22 11a2 2 0 0 0-2-2h-6z', 'M8 21H5a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2h3'],
            'user': ['M20 21a8 8 0 0 0-16 0', 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z']
        };
        return icons[name] || icons['message-circle'];
    }

    function scrollMessages() {
        scrollContent();
    }

    function scrollContent() {
        content.scrollTop = content.scrollHeight;
    }

    function readStoredAvailability() {
        if (!statusStorageKey || !statusStorage) {
            return null;
        }
        try {
            return parseStoredAvailability(statusStorage.getItem(statusStorageKey));
        } catch (error) {
            return null;
        }
    }

    function writeStoredAvailability(state, observedAt) {
        if (!statusStorageKey || !statusStorage || !isAvailabilityState(state)) {
            return;
        }
        var updatedAt = Number.isFinite(observedAt) ? observedAt : Date.now();
        try {
            statusStorage.setItem(statusStorageKey, JSON.stringify({
                state: state,
                updatedAt: updatedAt
            }));
        } catch (error) {
            // Availability still renders correctly when localStorage is unavailable.
        }
    }

    function parseStoredAvailability(rawValue) {
        if (!rawValue) {
            return null;
        }
        try {
            var parsed = JSON.parse(rawValue);
            if (!parsed || !isAvailabilityState(parsed.state) || !Number.isFinite(parsed.updatedAt)) {
                return null;
            }
            return parsed;
        } catch (error) {
            return null;
        }
    }

    function isAvailabilityState(state) {
        return state === 'available' || state === 'temporary_unavailable';
    }

    function availabilityLabel(state) {
        if (state === 'available') {
            return 'AI Chatbot Available';
        }
        if (state === 'temporary_unavailable') {
            return 'AI Chatbot Temporarily Unavailable';
        }
        return 'Checking AI Chatbot availability';
    }

    function safeLocalStorage() {
        try {
            var testKey = 'ai-chatbot:local-storage-test';
            window.localStorage.setItem(testKey, '1');
            window.localStorage.removeItem(testKey);
            return window.localStorage;
        } catch (error) {
            return null;
        }
    }

    function safeSessionStorage() {
        try {
            var testKey = 'ai-chatbot:storage-test';
            window.sessionStorage.setItem(testKey, '1');
            window.sessionStorage.removeItem(testKey);
            return window.sessionStorage;
        } catch (error) {
            return null;
        }
    }

    function readStored(key) {
        try {
            return storage ? storage.getItem(key) : null;
        } catch (error) {
            return null;
        }
    }

    function writeStored(key, value) {
        try {
            if (storage) {
                storage.setItem(key, value);
            }
        } catch (error) {
            // Chat remains usable when browser storage is unavailable.
        }
    }

    function createConversationId() {
        if (window.crypto && typeof window.crypto.randomUUID === 'function') {
            return window.crypto.randomUUID();
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (character) {
            var random = Math.random() * 16 | 0;
            var value = character === 'x' ? random : (random & 0x3 | 0x8);
            return value.toString(16);
        });
    }

    function isUuid(value) {
        return typeof value === 'string'
            && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
    }

    function currentRole(path) {
        var chip = document.querySelector('[data-user-chip-role]');
        var chipRole = chip ? safeToken(chip.dataset.userChipRole).toUpperCase() : '';
        if (chipRole === 'STUDENT' || chipRole === 'TEACHER' || chipRole === 'ADMIN') {
            return chipRole;
        }
        if (path.indexOf('/student/') === 0) {
            return 'STUDENT';
        }
        if (path.indexOf('/teacher/') === 0) {
            return 'TEACHER';
        }
        if (path.indexOf('/admin/') === 0) {
            return 'ADMIN';
        }
        return authenticated ? 'USER' : 'ANONYMOUS';
    }

    function dataValue(selector, name) {
        var element = document.querySelector(selector);
        return element && element.dataset ? numericId(element.dataset[name]) : '';
    }

    function textFrom(selector) {
        var element = document.querySelector(selector);
        return normalizeVisibleText(element ? (element.innerText || element.textContent) : '');
    }

    function compactJoin(values, separator) {
        return values.filter(function (value) {
            return String(value || '').trim();
        }).join(separator);
    }

    function titleCase(value) {
        var lower = String(value || '').toLowerCase();
        return lower.charAt(0).toUpperCase() + lower.slice(1);
    }

    function rememberSuggestion(value) {
        var normalized = normalizeQuestion(value);
        if (!normalized) {
            return;
        }
        recentlyShownSuggestions = recentlyShownSuggestions.filter(function (item) {
            return item !== normalized;
        }).concat(normalized).slice(-18);
    }

    function recentSuggestionKeys() {
        return recentlyShownSuggestions.slice();
    }

    function normalizeVisibleText(value) {
        return String(value || '').replace(/\s+/g, ' ').trim();
    }

    function normalizeScope(value) {
        var normalized = String(value || 'anonymous').toLowerCase().replace(/[^a-z0-9@._-]/g, '-');
        return normalized.slice(0, 96) || 'anonymous';
    }

    function safePath(value) {
        var normalized = String(value || '/');
        return /^\/[A-Za-z0-9/_\-.]*$/.test(normalized) && normalized.length <= 180 ? normalized : '/';
    }

    function safeToken(value) {
        var normalized = String(value || '').toLowerCase().replace(/[^a-z0-9-]/g, '-');
        return normalized.slice(0, 64);
    }

    function numericId(value) {
        var normalized = String(value || '');
        return /^\d{1,18}$/.test(normalized) ? normalized : '';
    }

    function pathId(path) {
        var match = String(path || '').match(/\/(\d{1,18})(?:\/)?$/);
        return match ? match[1] : '';
    }

    if (authenticated) {
        restoreAuthenticatedHistory();
    }
})();
