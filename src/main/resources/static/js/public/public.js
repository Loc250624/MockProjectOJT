'use strict';

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-public-nav-toggle]').forEach(function (toggle) {
        var header = toggle.closest('.lumina-header');
        if (!header) return;

        toggle.addEventListener('click', function () {
            var isOpen = header.classList.toggle('public-nav-open');
            toggle.setAttribute('aria-expanded', String(isOpen));
        });
    });

    document.querySelectorAll('[data-accordion]').forEach(function (accordion) {
        var triggers = accordion.querySelectorAll('[data-accordion-trigger]');

        triggers.forEach(function (trigger) {
            trigger.addEventListener('click', function () {
                var panelId = trigger.getAttribute('aria-controls');
                var panel = panelId ? document.getElementById(panelId) : null;
                if (!panel) return;

                var willOpen = trigger.getAttribute('aria-expanded') !== 'true';

                triggers.forEach(function (otherTrigger) {
                    var otherPanelId = otherTrigger.getAttribute('aria-controls');
                    var otherPanel = otherPanelId ? document.getElementById(otherPanelId) : null;
                    otherTrigger.setAttribute('aria-expanded', 'false');
                    if (otherPanel) otherPanel.hidden = true;
                });

                trigger.setAttribute('aria-expanded', String(willOpen));
                panel.hidden = !willOpen;
            });
        });
    });

    document.querySelectorAll('[data-loading-form]').forEach(function (form) {
        form.addEventListener('submit', function () {
            var loading = document.querySelector('[data-loading-message]');
            if (loading) loading.hidden = false;
        });
    });
});
