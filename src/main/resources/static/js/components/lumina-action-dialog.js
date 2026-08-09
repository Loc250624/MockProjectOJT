(function initLuminaActionDialog(global) {
    'use strict';

    if (global.LuminaActionDialog) return;

    const ICONS = Object.freeze({
        info: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"></circle><path d="M12 10v6m0-9h.01"></path></svg>',
        success: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"></circle><path d="M8 12.2l2.6 2.6L16.5 9"></path></svg>',
        warning: '<svg viewBox="0 0 24 24"><path d="M10.3 3.8L2.8 17a2 2 0 001.74 3h14.92A2 2 0 0021.2 17L13.7 3.8a2 2 0 00-3.4 0z"></path><path d="M12 9v4m0 3h.01"></path></svg>',
        danger: '<svg viewBox="0 0 24 24"><path d="M4 7h16M9 7V4h6v3m3 0l-1 13H7L6 7m4 4v5m4-5v5"></path></svg>',
        quiz: '<svg viewBox="0 0 24 24"><path d="M8 4h8m-7 0v3h6V4m-8 3H5v13h14V7h-2"></path><path d="M9 12l1.5 1.5L14 10m-5 7h6"></path></svg>',
        payment: '<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"></rect><path d="M3 9h18M7 15h3"></path></svg>',
        publish: '<svg viewBox="0 0 24 24"><path d="M12 3v12m-4-8l4-4 4 4"></path><path d="M5 13v7h14v-7"></path></svg>',
        course: '<svg viewBox="0 0 24 24"><path d="M3 7l9-4 9 4-9 4-9-4z"></path><path d="M6 9.2V15l6 3 6-3V9.2M21 8v7"></path></svg>',
        user: '<svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"></circle><path d="M4 21a8 8 0 0116 0"></path></svg>'
    });

    const CHECK_ICON = '<span class="lumina-action-dialog__checklist-icon" aria-hidden="true"><svg viewBox="0 0 16 16"><path d="M3 8.2l3 3 7-7"></path></svg></span>';
    const ALLOWED_VARIANTS = new Set(['info', 'success', 'warning', 'danger']);
    const ALLOWED_SIZES = new Set(['compact', 'regular', 'wide']);
    let activeSession = null;

    const SELECTORS = Object.freeze({
        dialog: '#luminaActionDialog',
        icon: '[data-lumina-dialog-icon]',
        eyebrow: '[data-lumina-dialog-eyebrow]',
        badge: '[data-lumina-dialog-badge]',
        title: '[data-lumina-dialog-title]',
        subtitle: '[data-lumina-dialog-subtitle]',
        close: '[data-lumina-dialog-close]',
        objectWrap: '[data-lumina-dialog-object-wrap]',
        objectLabel: '[data-lumina-dialog-object-label]',
        object: '[data-lumina-dialog-object]',
        summary: '[data-lumina-dialog-summary]',
        notice: '[data-lumina-dialog-notice]',
        noticeTitle: '[data-lumina-dialog-notice-title]',
        noticeText: '[data-lumina-dialog-notice-text]',
        checklist: '[data-lumina-dialog-checklist]',
        acknowledgementWrap: '[data-lumina-dialog-acknowledgement-wrap]',
        acknowledgement: '[data-lumina-dialog-acknowledgement]',
        acknowledgementLabel: '[data-lumina-dialog-acknowledgement-label]',
        error: '[data-lumina-dialog-error]',
        status: '[data-lumina-dialog-status]',
        cancel: '[data-lumina-dialog-cancel]',
        submit: '[data-lumina-dialog-submit]',
        submitText: '[data-lumina-dialog-submit-text]'
    });

    function fallbackMarkup() {
        return [
            '<dialog id="luminaActionDialog" class="lumina-action-dialog" data-variant="info" data-size="regular" aria-labelledby="luminaActionDialogTitle" aria-describedby="luminaActionDialogDescription">',
            '<div class="lumina-action-dialog__shell"><div class="lumina-action-dialog__accent" aria-hidden="true"></div>',
            '<header class="lumina-action-dialog__header">',
            '<div class="lumina-action-dialog__visual" data-lumina-dialog-icon aria-hidden="true"></div>',
            '<div class="lumina-action-dialog__heading-copy"><div class="lumina-action-dialog__heading-row"><p class="lumina-action-dialog__eyebrow" data-lumina-dialog-eyebrow>Confirmation</p><span class="lumina-action-dialog__badge" data-lumina-dialog-badge hidden></span></div>',
            '<h2 id="luminaActionDialogTitle" class="lumina-action-dialog__title" data-lumina-dialog-title>Please confirm this action</h2>',
            '<p id="luminaActionDialogDescription" class="lumina-action-dialog__subtitle" data-lumina-dialog-subtitle></p></div>',
            '<button type="button" class="lumina-action-dialog__close" data-lumina-dialog-close aria-label="Close dialog"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"></path></svg></button>',
            '</header><div class="lumina-action-dialog__body">',
            '<section class="lumina-action-dialog__object" data-lumina-dialog-object-wrap hidden><span class="lumina-action-dialog__object-label" data-lumina-dialog-object-label>Selected item</span><strong class="lumina-action-dialog__object-value" data-lumina-dialog-object></strong></section>',
            '<dl class="lumina-action-dialog__summary" data-lumina-dialog-summary hidden></dl>',
            '<section class="lumina-action-dialog__notice" data-lumina-dialog-notice hidden><div class="lumina-action-dialog__notice-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 8v5m0 3h.01M10.3 3.8L2.8 17a2 2 0 001.74 3h14.92A2 2 0 0021.2 17L13.7 3.8a2 2 0 00-3.4 0z"></path></svg></div><div><strong data-lumina-dialog-notice-title></strong><p data-lumina-dialog-notice-text></p></div></section>',
            '<ul class="lumina-action-dialog__checklist" data-lumina-dialog-checklist hidden></ul>',
            '<label class="lumina-action-dialog__acknowledgement" data-lumina-dialog-acknowledgement-wrap hidden><input type="checkbox" data-lumina-dialog-acknowledgement><span class="lumina-action-dialog__checkbox" aria-hidden="true"><svg viewBox="0 0 16 16"><path d="M3.2 8.2l3 3 6.6-6.6"></path></svg></span><span data-lumina-dialog-acknowledgement-label></span></label>',
            '<p class="lumina-action-dialog__error" data-lumina-dialog-error role="alert" hidden></p><p class="lumina-action-dialog__status" data-lumina-dialog-status aria-live="polite"></p>',
            '</div><footer class="lumina-action-dialog__footer"><button type="button" class="lumina-action-dialog__button lumina-action-dialog__button--secondary" data-lumina-dialog-cancel>Cancel</button><button type="button" class="lumina-action-dialog__button lumina-action-dialog__button--primary" data-lumina-dialog-submit><span class="lumina-action-dialog__spinner" aria-hidden="true"></span><span data-lumina-dialog-submit-text>Confirm</span><svg class="lumina-action-dialog__button-arrow" viewBox="0 0 20 20" aria-hidden="true"><path d="M4 10h12m-4-4l4 4-4 4"></path></svg></button></footer>',
            '</div></dialog>'
        ].join('');
    }

    function getDialog() {
        let dialog = document.querySelector(SELECTORS.dialog);
        if (dialog) return dialog;
        const host = document.createElement('div');
        host.innerHTML = fallbackMarkup();
        dialog = host.firstElementChild;
        document.body.appendChild(dialog);
        return dialog;
    }

    function getParts(dialog) {
        const parts = {};
        Object.entries(SELECTORS).forEach(([key, selector]) => {
            if (key === 'dialog') return;
            parts[key] = dialog.querySelector(selector);
        });
        return parts;
    }

    function normalizedText(value, fallback) {
        if (value === undefined || value === null) return fallback;
        return String(value);
    }

    function normalizeOptions(input) {
        const options = input && typeof input === 'object' ? input : {};
        const variant = ALLOWED_VARIANTS.has(options.variant) ? options.variant : 'info';
        const size = ALLOWED_SIZES.has(options.size) ? options.size : 'regular';
        const acknowledgement = options.acknowledgement && typeof options.acknowledgement === 'object'
            ? options.acknowledgement
            : null;

        return {
            variant,
            size,
            icon: ICONS[options.icon] ? options.icon : variant,
            eyebrow: normalizedText(options.eyebrow, 'Confirmation'),
            badge: normalizedText(options.badge, ''),
            title: normalizedText(options.title, 'Please confirm this action'),
            subtitle: normalizedText(options.subtitle, ''),
            objectLabel: normalizedText(options.objectLabel, 'Selected item'),
            objectName: normalizedText(options.objectName, ''),
            summary: Array.isArray(options.summary) ? options.summary.slice(0, 8) : [],
            notice: options.notice && typeof options.notice === 'object' ? options.notice : null,
            checklist: Array.isArray(options.checklist) ? options.checklist.slice(0, 10) : [],
            acknowledgement,
            cancelText: normalizedText(options.cancelText, 'No'),
            confirmText: normalizedText(options.confirmText, 'Yes'),
            loadingText: normalizedText(options.loadingText, 'Processing...'),
            errorText: normalizedText(options.errorText, 'The action could not be completed. Please try again.'),
            closeOnEscape: options.closeOnEscape !== false,
            closeOnBackdrop: options.closeOnBackdrop !== false,
            initialFocus: options.initialFocus || 'confirm',
            onConfirm: typeof options.onConfirm === 'function' ? options.onConfirm : null
        };
    }

    function setText(element, value) {
        element.textContent = value || '';
    }

    function setOptional(element, value) {
        setText(element, value);
        element.hidden = !value;
    }

    function renderSummary(container, items) {
        container.replaceChildren();
        items.forEach((entry) => {
            if (!entry || typeof entry !== 'object') return;
            const item = document.createElement('div');
            item.className = 'lumina-action-dialog__summary-item';
            const term = document.createElement('dt');
            const description = document.createElement('dd');
            term.textContent = normalizedText(entry.label, 'Information');
            description.textContent = normalizedText(entry.value, '—');
            item.append(term, description);
            container.appendChild(item);
        });
        container.hidden = container.childElementCount === 0;
    }

    function renderChecklist(container, values) {
        container.replaceChildren();
        values.forEach((value) => {
            const text = typeof value === 'object' && value !== null ? value.text : value;
            if (text === undefined || text === null || String(text).trim() === '') return;
            const item = document.createElement('li');
            item.className = 'lumina-action-dialog__checklist-item';
            item.innerHTML = CHECK_ICON;
            const label = document.createElement('span');
            label.textContent = String(text);
            item.appendChild(label);
            container.appendChild(item);
        });
        container.hidden = container.childElementCount === 0;
    }

    function clearError(session) {
        session.parts.error.textContent = '';
        session.parts.error.hidden = true;
    }

    function showError(session, error) {
        const message = error instanceof Error ? error.message : normalizedText(error, session.options.errorText);
        session.parts.error.textContent = message || session.options.errorText;
        session.parts.error.hidden = false;
    }

    function setLoading(session, loading) {
        session.loading = loading;
        session.dialog.dataset.loading = String(loading);
        session.parts.close.disabled = loading;
        session.parts.cancel.disabled = loading;
        session.parts.submit.disabled = loading;
        session.parts.acknowledgement.disabled = loading;
        session.parts.submitText.textContent = loading ? session.options.loadingText : session.options.confirmText;
        session.parts.status.textContent = loading ? session.options.loadingText : '';
    }

    function render(session) {
        const { dialog, parts, options } = session;
        dialog.dataset.variant = options.variant;
        dialog.dataset.size = options.size;
        dialog.dataset.loading = 'false';
        dialog.classList.remove('is-closing');

        parts.icon.innerHTML = ICONS[options.icon];
        setText(parts.eyebrow, options.eyebrow);
        setOptional(parts.badge, options.badge);
        setText(parts.title, options.title);
        setText(parts.subtitle, options.subtitle);
        parts.subtitle.hidden = !options.subtitle;

        setText(parts.objectLabel, options.objectLabel);
        setText(parts.object, options.objectName);
        parts.objectWrap.hidden = !options.objectName;
        renderSummary(parts.summary, options.summary);

        if (options.notice) {
            setText(parts.noticeTitle, normalizedText(options.notice.title, 'Please note'));
            setText(parts.noticeText, normalizedText(options.notice.text, ''));
            parts.notice.hidden = false;
        } else {
            parts.notice.hidden = true;
        }

        renderChecklist(parts.checklist, options.checklist);

        if (options.acknowledgement) {
            setText(parts.acknowledgementLabel, normalizedText(options.acknowledgement.label, 'I understand the impact of this action.'));
            parts.acknowledgement.checked = Boolean(options.acknowledgement.checked);
            parts.acknowledgementWrap.hidden = false;
        } else {
            parts.acknowledgement.checked = false;
            parts.acknowledgementWrap.hidden = true;
        }

        setText(parts.cancel, options.cancelText);
        setText(parts.submitText, options.confirmText);
        parts.status.textContent = '';
        clearError(session);
    }

    function getFocusable(dialog) {
        return Array.from(dialog.querySelectorAll([
            'button:not([disabled])',
            'input:not([disabled])',
            'select:not([disabled])',
            'a[href]',
            '[tabindex]:not([tabindex="-1"])'
        ].join(','))).filter((element) => !element.hidden && element.offsetParent !== null);
    }

    function focusInitial(session) {
        const mapping = {
            cancel: session.parts.cancel,
            acknowledgement: session.parts.acknowledgement,
            confirm: session.parts.submit
        };
        const target = mapping[session.options.initialFocus] || session.parts.submit;
        global.requestAnimationFrame(() => target && target.focus());
    }

    function restorePageState(session) {
        document.body.classList.remove('lumina-dialog-open');
        if (session.trigger && typeof session.trigger.focus === 'function' && document.contains(session.trigger)) {
            global.requestAnimationFrame(() => session.trigger.focus());
        }
    }

    function finish(session, confirmed) {
        if (activeSession !== session) return;
        session.dialog.removeEventListener('keydown', handleKeydown);
        session.dialog.classList.add('is-closing');
        const close = () => {
            if (session.dialog.open) session.dialog.close();
            session.dialog.classList.remove('is-closing');
            activeSession = null;
            restorePageState(session);
            session.resolve({
                confirmed,
                acknowledged: session.parts.acknowledgement.checked
            });
        };
        global.setTimeout(close, global.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 145);
    }

    function validate(session) {
        const acknowledgement = session.options.acknowledgement;
        if (acknowledgement && acknowledgement.required !== false && !session.parts.acknowledgement.checked) {
            showError(session, normalizedText(acknowledgement.errorText, 'Please confirm that you understand the impact of this action.'));
            session.parts.acknowledgement.focus();
            return false;
        }
        return true;
    }

    async function confirmSession(session) {
        if (session.loading || !validate(session)) return;
        clearError(session);
        const payload = {
            acknowledged: session.parts.acknowledgement.checked
        };

        if (!session.options.onConfirm) {
            finish(session, true);
            return;
        }

        setLoading(session, true);
        try {
            await session.options.onConfirm(payload);
            finish(session, true);
        } catch (error) {
            setLoading(session, false);
            showError(session, error);
        }
    }

    function handleKeydown(event) {
        const session = activeSession;
        if (!session) return;

        if (event.key === 'Escape') {
            event.preventDefault();
            if (!session.loading && session.options.closeOnEscape) finish(session, false);
            return;
        }

        if (event.key !== 'Tab') return;
        const focusable = getFocusable(session.dialog);
        if (!focusable.length) {
            event.preventDefault();
            return;
        }
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    function bind(session) {
        const { dialog, parts } = session;
        parts.close.onclick = () => !session.loading && finish(session, false);
        parts.cancel.onclick = () => !session.loading && finish(session, false);
        parts.submit.onclick = () => confirmSession(session);
        parts.acknowledgement.onchange = () => clearError(session);

        dialog.oncancel = (event) => {
            event.preventDefault();
            if (!session.loading && session.options.closeOnEscape) finish(session, false);
        };

        dialog.onclick = (event) => {
            if (session.loading || !session.options.closeOnBackdrop || event.target !== dialog) return;
            finish(session, false);
        };

        dialog.addEventListener('keydown', handleKeydown);
    }

    function closeActiveImmediately() {
        const session = activeSession;
        if (!session) return;
        session.dialog.removeEventListener('keydown', handleKeydown);
        if (session.dialog.open) session.dialog.close();
        session.dialog.classList.remove('is-closing');
        activeSession = null;
        restorePageState(session);
        session.resolve({
            confirmed: false,
            acknowledged: session.parts.acknowledgement.checked
        });
    }

    function open(input) {
        return new Promise((resolve) => {
            closeActiveImmediately();
            const dialog = getDialog();
            const session = {
                dialog,
                parts: getParts(dialog),
                options: normalizeOptions(input),
                trigger: document.activeElement,
                resolve,
                loading: false
            };
            activeSession = session;
            render(session);
            bind(session);
            document.body.classList.add('lumina-dialog-open');
            dialog.showModal();
            focusInitial(session);
        });
    }

    const presets = Object.freeze({
        quizStart(data) {
            const values = data || {};
            return open({
                variant: 'info',
                icon: 'quiz',
                eyebrow: 'Quiz attempt',
                badge: '',
                title: 'Ready to start the quiz?',
                subtitle: '',
                objectLabel: 'Quiz',
                objectName: values.quizName || 'Selected quiz',
                summary: [
                    { label: 'Questions', value: values.questionCount || '10 questions' },
                    { label: 'Time limit', value: values.duration || '20 minutes' },
                    { label: 'Passing score', value: values.passingScore || 'Configured score' },
                    { label: 'Attempts', value: values.attempts || 'Unlimited' }
                ],
                notice: {
                    title: 'The timer begins after confirmation',
                    text: 'Do not close or refresh the page while your attempt is being created.'
                },
                confirmText: 'Yes',
                cancelText: 'No',
                ...values.options
            });
        },

        quizSubmit(data) {
            const values = data || {};
            return open({
                variant: 'warning',
                icon: 'quiz',
                eyebrow: 'Submit quiz',
                badge: values.timeRemaining || 'Final submission',
                title: 'Submit your answers now?',
                subtitle: 'You will not be able to edit this attempt after submission.',
                summary: [
                    { label: 'Answered', value: values.answered || '0/10' },
                    { label: 'Unanswered', value: values.unanswered || '10' },
                    { label: 'Time remaining', value: values.timeRemaining || '—' },
                    { label: 'Current attempt', value: values.attemptLabel || 'Active' }
                ],
                acknowledgement: {
                    label: 'I understand that this attempt will be finalized.',
                    required: true
                },
                confirmText: 'Yes',
                cancelText: 'No',
                ...values.options
            });
        },

        blogSubmit(data) {
            const values = data || {};
            return open({
                variant: 'warning',
                icon: 'publish',
                eyebrow: values.isResubmit ? 'Resubmit blog' : 'Submit blog',
                badge: 'Pending Review',
                title: values.isResubmit ? 'Send this blog for review again?' : 'Send this blog for review?',
                subtitle: 'The blog will enter the moderation queue for an administrator to review.',
                objectLabel: 'Blog title',
                objectName: values.blogName || 'Selected blog',
                summary: [
                    { label: 'Current status', value: values.currentStatus || 'Draft' },
                    { label: 'New status', value: 'Pending Review' }
                ],
                confirmText: 'Yes',
                cancelText: 'No',
                ...values.options
            });
        },

        courseSubmit(data) {
            const values = data || {};
            return open({
                variant: 'warning',
                icon: 'course',
                eyebrow: values.withdraw ? 'Withdraw course' : 'Course review',
                badge: values.withdraw ? 'Return to draft' : 'Pending approval',
                title: values.withdraw ? 'Withdraw this course from review?' : 'Submit this course for approval?',
                subtitle: values.withdraw
                    ? 'The course will leave the review queue and return to an editable state.'
                    : 'Check that all required learning content is ready before submission.',
                objectLabel: 'Course',
                objectName: values.courseName || 'Selected course',
                summary: values.summary || [],
                checklist: values.checklist || [
                    'Course information and thumbnail are complete.',
                    'Lessons and videos are available to learners.',
                    'Quiz configuration and question bank are ready.'
                ],
                confirmText: 'Yes',
                cancelText: 'No',
                ...values.options
            });
        },

        payment(data) {
            const values = data || {};
            return open({
                variant: 'info',
                icon: 'payment',
                eyebrow: 'Secure checkout',
                badge: values.method || 'VNPAY',
                title: 'Confirm your payment details',
                subtitle: 'You will be redirected to the payment gateway after confirmation.',
                objectLabel: 'Course',
                objectName: values.courseName || 'Selected course',
                summary: [
                    { label: 'Course price', value: values.usd || '—' },
                    { label: 'Amount to pay', value: values.vnd || '—' },
                    { label: 'Payment method', value: values.method || 'VNPAY' },
                    { label: 'Order code', value: values.orderCode || 'Created on payment' }
                ],
                notice: {
                    title: 'Complete the payment on the gateway page',
                    text: 'Do not close the browser until the result is returned to the E-Learning website.'
                },
                confirmText: 'Yes',
                cancelText: 'No',
                ...values.options
            });
        },

        danger(data) {
            const values = data || {};
            return open({
                variant: 'danger',
                icon: values.icon || 'danger',
                eyebrow: values.eyebrow || 'Danger zone',
                badge: values.badge || 'Irreversible action',
                title: values.title || 'Delete this item?',
                subtitle: values.subtitle || 'Review the impact carefully before continuing.',
                objectLabel: values.objectLabel || 'Selected item',
                objectName: values.objectName || '',
                summary: values.summary || [],
                notice: values.notice || {
                    title: 'This action may affect related data',
                    text: 'Only continue when you are certain this is the intended item.'
                },
                checklist: values.checklist || [],
                acknowledgement: values.acknowledgement || {
                    label: 'I understand the effect of this action.',
                    required: true
                },
                confirmText: 'Yes',
                cancelText: 'No',
                ...values.options
            });
        }
    });

    global.LuminaActionDialog = Object.freeze({
        open,
        presets,
        close: closeActiveImmediately,
        icons: Object.freeze(Object.keys(ICONS))
    });
})(window);
