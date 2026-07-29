(function () {
    'use strict';

    var DEBOUNCE_MS = 300;

    document.querySelectorAll('[data-live-search-form]').forEach(function (form) {
        var input = form.querySelector('[data-live-search-input]');
        var targetSelector = form.getAttribute('data-live-search-target');
        var countSelector = form.getAttribute('data-live-search-count-target');
        var loadingSelector = form.getAttribute('data-live-search-loading-target');
        var errorSelector = form.getAttribute('data-live-search-error-target');

        if (!input || !targetSelector) {
            return;
        }

        var debounceTimer;
        var activeRequest;
        var latestRequestId = 0;

        function element(selector) {
            return selector ? document.querySelector(selector) : null;
        }

        function setLoading(isLoading) {
            var loading = element(loadingSelector);
            var target = element(targetSelector);
            if (loading) {
                loading.hidden = !isLoading;
            }
            if (target) {
                target.setAttribute('aria-busy', String(isLoading));
            }
        }

        function showError(message) {
            var error = element(errorSelector);
            if (!error) {
                return;
            }
            error.textContent = message || '';
            error.hidden = !message;
        }

        function synchronizeHiddenSearchFields() {
            document.querySelectorAll('input[type="hidden"]').forEach(function (hiddenInput) {
                if (hiddenInput.name === input.name && !form.contains(hiddenInput)) {
                    hiddenInput.value = input.value;
                }
            });
        }

        function buildUrl() {
            var url = new URL(form.action, window.location.href);
            var params = new URLSearchParams(new FormData(form));
            params.delete('page');
            url.search = params.toString();
            return url;
        }

        async function loadResults() {
            var requestId = ++latestRequestId;
            if (activeRequest) {
                activeRequest.abort();
            }
            activeRequest = new AbortController();

            var url = buildUrl();
            showError('');
            setLoading(true);

            try {
                var response = await fetch(url.toString(), {
                    method: 'GET',
                    headers: {'X-Requested-With': 'XMLHttpRequest'},
                    signal: activeRequest.signal
                });
                if (!response.ok) {
                    throw new Error('Search request failed with status ' + response.status);
                }

                var html = await response.text();
                if (requestId !== latestRequestId) {
                    return;
                }

                var parsedDocument = new DOMParser().parseFromString(html, 'text/html');
                var nextTarget = parsedDocument.querySelector(targetSelector);
                var currentTarget = element(targetSelector);
                if (!nextTarget || !currentTarget) {
                    throw new Error('Search results were not present in the response.');
                }

                currentTarget.replaceWith(nextTarget);

                if (countSelector) {
                    var nextCount = parsedDocument.querySelector(countSelector);
                    var currentCount = element(countSelector);
                    if (nextCount && currentCount) {
                        currentCount.textContent = nextCount.textContent;
                    }
                }

                window.history.replaceState({}, '', url.pathname + url.search + url.hash);
            } catch (error) {
                if (error.name !== 'AbortError' && requestId === latestRequestId) {
                    showError('Unable to update search results. Please try again.');
                }
            } finally {
                if (requestId === latestRequestId) {
                    setLoading(false);
                }
            }
        }

        function scheduleSearch() {
            window.clearTimeout(debounceTimer);
            synchronizeHiddenSearchFields();
            debounceTimer = window.setTimeout(loadResults, DEBOUNCE_MS);
        }

        input.addEventListener('input', scheduleSearch);
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            window.clearTimeout(debounceTimer);
            synchronizeHiddenSearchFields();
            loadResults();
        });
    });
})();
