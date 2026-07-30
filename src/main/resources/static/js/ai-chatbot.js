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
    var panel = root.querySelector('[data-ai-chatbot-panel]');
    var closeButton = root.querySelector('[data-ai-chatbot-close]');
    var content = root.querySelector('[data-ai-chatbot-content]');
    var messages = root.querySelector('[data-ai-chatbot-messages]');
    var greeting = root.querySelector('[data-ai-chatbot-greeting]');
    var quickActionsContainer = root.querySelector('[data-ai-chatbot-quick-actions]');
    var quickActions = Array.from(root.querySelectorAll('[data-ai-chatbot-action]'));
    var relatedActionsContainer = root.querySelector('[data-ai-chatbot-related-actions]');
    var relatedButtonsContainer = root.querySelector('[data-ai-chatbot-related-buttons]');
    var form = root.querySelector('[data-ai-chatbot-form]');
    var input = root.querySelector('[data-ai-chatbot-input]');
    var sendButton = root.querySelector('[data-ai-chatbot-send]');
    var status = root.querySelector('[data-ai-chatbot-status]');

    if (!toggle || !panel || !closeButton || !content || !messages || !greeting || !quickActionsContainer
        || !relatedActionsContainer || !relatedButtonsContainer || !form || !input) {
        return;
    }

    var authenticated = root.dataset.chatAuthenticated === 'true';
    var storage = safeSessionStorage();
    var scope = normalizeScope(root.dataset.chatScope);
    var conversationStorageKey = 'ai-chatbot:' + scope + ':conversation-id';
    var conversationId = authenticated ? null : (readStored(conversationStorageKey) || createConversationId());
    var history = [];
    var shownRelatedQuestions = [];
    var sending = false;
    var previouslyFocused = null;

    if (!authenticated) {
        writeStored(conversationStorageKey, conversationId);
    }
    renderInitialQuestions();
    renderRelatedQuestions([]);

    toggle.addEventListener('click', function () {
        setPanelOpen(panel.hidden);
    });
    closeButton.addEventListener('click', closePanel);

    quickActions.forEach(function (button) {
        button.addEventListener('click', function () {
            if (sending) {
                return;
            }
            sendChat(button.textContent.trim(), button.dataset.aiChatbotAction || '');
        });
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
        quickActionsContainer.hidden = history.some(function (turn) {
            return turn.role === 'user';
        });
    }

    function beginSuggestionTransition() {
        quickActionsContainer.hidden = true;
        relatedActionsContainer.hidden = true;
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
        Array.from(relatedButtonsContainer.querySelectorAll('button')).forEach(function (button) {
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
        var requestBody = {
            conversationId: conversationId,
            lessonId: pageData.lessonId,
            message: message,
            action: action || '',
            pageContext: pageData.pageContext
        };
        if (!authenticated) {
            requestBody.recentMessages = requestHistory;
        }
        fetch('/api/ai-chatbot/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(requestBody)
        }).then(function (response) {
            return response.json().catch(function () {
                return { message: 'AI Chatbot could not respond. Please try again later.' };
            }).then(function (body) {
                if (!response.ok) {
                    throw new Error(body.message || 'AI Chatbot could not respond. Please try again later.');
                }
                return body;
            });
        }).then(function (apiResponse) {
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
            setStatus(data.refused ? 'This request is outside the available website guidance.' : '', Boolean(data.refused));
        }).catch(function (error) {
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
        var label = document.createElement('strong');
        label.textContent = role === 'user' ? 'You' : 'AI Chatbot';
        var bubble = document.createElement('div');
        bubble.className = 'ai-chatbot-bubble';
        if (role === 'assistant') {
            renderMarkdown(bubble, text || '');
        } else {
            bubble.textContent = text || '';
        }
        item.appendChild(label);
        item.appendChild(bubble);
        messages.appendChild(item);
        scrollMessages();
    }

    function renderRelatedQuestions(candidates) {
        quickActionsContainer.hidden = true;
        var excluded = quickActions.map(function (button) {
            return button.textContent.trim();
        }).concat(history.filter(function (turn) {
            return turn.role === 'user';
        }).map(function (turn) {
            return turn.content;
        })).concat(shownRelatedQuestions);
        var selected = selectUniqueQuestions(candidates, excluded, 4);
        relatedButtonsContainer.replaceChildren();
        selected.forEach(function (question) {
            var button = document.createElement('button');
            button.type = 'button';
            button.textContent = question;
            button.setAttribute('data-ai-chatbot-related-action', '');
            button.disabled = sending;
            button.addEventListener('click', function () {
                if (!sending) {
                    sendChat(question, '');
                }
            });
            relatedButtonsContainer.appendChild(button);
        });
        shownRelatedQuestions = shownRelatedQuestions.concat(selected);
        relatedActionsContainer.hidden = selected.length === 0;
        scrollContent();
    }

    function selectUniqueQuestions(candidates, excluded, maxItems) {
        var selected = [];
        var existing = Array.isArray(excluded) ? excluded.slice() : [];
        (Array.isArray(candidates) ? candidates : []).some(function (rawCandidate) {
            var candidate = String(rawCandidate || '').normalize('NFKC').replace(/\s+/g, ' ').trim();
            if (!candidate || isDuplicateQuestion(candidate, existing.concat(selected))) {
                return false;
            }
            selected.push(candidate);
            return selected.length >= maxItems;
        });
        return selected;
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

    function renderMarkdown(container, rawText) {
        var lines = String(rawText || '').replace(/\r\n?/g, '\n').split('\n');
        var paragraphLines = [];
        var list = null;
        var listType = '';

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

        lines.forEach(function (line) {
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

        flushParagraph();
        flushList();
        if (!container.hasChildNodes()) {
            container.textContent = '';
        }
    }

    function appendInlineMarkdown(container, text) {
        var value = String(text || '');
        var boldPattern = /(\*\*|__)(.+?)\1/g;
        var lastIndex = 0;
        var match;

        while ((match = boldPattern.exec(value)) !== null) {
            if (match.index > lastIndex) {
                container.appendChild(document.createTextNode(value.slice(lastIndex, match.index)));
            }
            var strong = document.createElement('strong');
            strong.textContent = match[2];
            container.appendChild(strong);
            lastIndex = boldPattern.lastIndex;
        }
        if (lastIndex < value.length) {
            container.appendChild(document.createTextNode(value.slice(lastIndex)));
        }
    }

    function currentPageContext() {
        var path = safePath(window.location.pathname);
        var params = new URLSearchParams(window.location.search);
        var pageKey = 'navigation';
        var entityType = 'navigation';
        var entityId = '';
        var lessonId = null;

        if (path === '/') {
            pageKey = 'public-home';
        } else if (path.indexOf('/auth/') === 0) {
            pageKey = 'authentication';
            entityType = 'authentication';
        } else if (path.indexOf('/student/learning') === 0) {
            pageKey = 'student-learning';
            entityType = 'lesson';
            entityId = numericId(params.get('lessonId'));
            lessonId = entityId ? Number(entityId) : null;
        } else if (path.indexOf('/quiz') >= 0) {
            pageKey = path.indexOf('/teacher/') === 0 ? 'teacher-quiz' : 'student-quiz';
            entityType = 'quiz';
            entityId = numericId(params.get('quizId') || params.get('id'));
        } else if (path.indexOf('code-assignment') >= 0 || path.indexOf('/assignments') >= 0) {
            pageKey = path.indexOf('/teacher/') === 0 ? 'teacher-assignments' : 'student-coding-assignment';
            entityType = 'coding-assignment';
            entityId = numericId(params.get('id') || params.get('assignmentId'));
        } else if (path.indexOf('/certificates') >= 0) {
            pageKey = 'certificates';
            entityType = 'certificate';
            entityId = numericId(params.get('id'));
        } else if (path.indexOf('/payment') >= 0 || path.indexOf('/checkout') >= 0 || path.indexOf('/transactions') >= 0) {
            pageKey = 'payment';
            entityType = 'payment';
            entityId = numericId(params.get('id') || params.get('orderId'));
        } else if (path.indexOf('/blogs') >= 0) {
            pageKey = 'blogs';
            entityType = 'blog';
            entityId = numericId(params.get('id')) || pathId(path);
        } else if (path.indexOf('/profile') >= 0) {
            pageKey = 'profile';
            entityType = 'profile';
        } else if (path.indexOf('/dashboard') >= 0) {
            pageKey = path.replace(/^\/+/, '').replace(/\//g, '-');
            entityType = 'dashboard';
        } else if (path.indexOf('/courses') >= 0 || path.indexOf('/learning') >= 0) {
            pageKey = path.indexOf('/teacher/') === 0 ? 'teacher-courses' : 'courses';
            entityType = 'course';
            entityId = numericId(params.get('id') || params.get('courseId')) || pathId(path);
        }

        return {
            lessonId: lessonId,
            pageContext: {
                path: path,
                pageKey: safeToken(pageKey),
                entityType: safeToken(entityType),
                entityId: entityId,
                visibleText: collectVisiblePageText(pageKey)
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

    function normalizeVisibleText(value) {
        return String(value || '').replace(/\s+/g, ' ').trim();
    }

    function scrollMessages() {
        scrollContent();
    }

    function scrollContent() {
        content.scrollTop = content.scrollHeight;
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
