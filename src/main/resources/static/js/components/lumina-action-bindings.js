(function initLuminaActionBindings(global) {
    'use strict';

    if (global.LuminaActionBindings) return;

    function asBoolean(value) {
        return value === 'true';
    }

    function asList(value) {
        return String(value || '')
            .split('|')
            .map(function(item) { return item.trim(); })
            .filter(Boolean);
    }

    function addSummary(summary, label, value) {
        if (value) summary.push({ label: label, value: value });
    }

    function sharedOptions(form) {
        var data = form.dataset;
        var summary = [];
        addSummary(summary, 'Current status', data.dialogCurrentStatus);
        addSummary(summary, 'New status', data.dialogNextStatus);
        addSummary(summary, data.dialogOwnerLabel || 'Owner', data.dialogOwner);
        addSummary(summary, data.dialogExtraLabel || 'Details', data.dialogExtraValue);

        var requireAcknowledgement = asBoolean(data.dialogRequireAcknowledgement);
        return {
            variant: data.dialogVariant || 'warning',
            icon: data.dialogIcon || undefined,
            eyebrow: data.dialogEyebrow || 'Please confirm',
            badge: data.dialogBadge || '',
            title: data.dialogTitle || 'Please confirm this action',
            subtitle: data.dialogSubtitle || '',
            objectLabel: data.dialogObjectLabel || 'Selected item',
            objectName: data.dialogObject || '',
            summary: summary,
            notice: data.dialogNoticeText ? {
                title: data.dialogNoticeTitle || 'Please note',
                text: data.dialogNoticeText
            } : null,
            checklist: asList(data.dialogChecklist),
            acknowledgement: requireAcknowledgement ? {
                label: data.dialogAcknowledgementLabel || 'I understand the effect of this action.',
                required: true
            } : null,
            confirmText: data.dialogConfirmText || 'Confirm',
            cancelText: data.dialogCancelText || 'Cancel',
            loadingText: data.dialogLoadingText || 'Processing...'
        };
    }

    function presetOverrides(form) {
        var data = form.dataset;
        var options = {};
        if (data.dialogTitle) options.title = data.dialogTitle;
        if (data.dialogSubtitle) options.subtitle = data.dialogSubtitle;
        if (data.dialogBadge) options.badge = data.dialogBadge;
        if (data.dialogConfirmText) options.confirmText = data.dialogConfirmText;
        if (data.dialogCancelText) options.cancelText = data.dialogCancelText;
        if (data.dialogLoadingText) options.loadingText = data.dialogLoadingText;
        return options;
    }

    function openForForm(form) {
        var data = form.dataset;
        var options = sharedOptions(form);
        var overrides = presetOverrides(form);
        if (data.dialogPreset === 'blog-submit') {
            return global.LuminaActionDialog.presets.blogSubmit({
                blogName: data.dialogObject,
                currentStatus: data.dialogCurrentStatus,
                isResubmit: data.dialogCurrentStatus === 'REJECTED',
                options: overrides
            });
        }
        if (data.dialogPreset === 'course-submit') {
            return global.LuminaActionDialog.presets.courseSubmit({
                courseName: data.dialogObject,
                withdraw: asBoolean(data.dialogWithdraw),
                summary: options.summary,
                checklist: options.checklist,
                options: overrides
            });
        }
        if (data.dialogPreset === 'payment') {
            return global.LuminaActionDialog.presets.payment({
                courseName: data.dialogObject,
                usd: data.dialogUsd,
                vnd: data.dialogVnd,
                method: data.dialogPaymentMethod,
                options: overrides
            });
        }
        if (data.dialogPreset === 'danger') {
            return global.LuminaActionDialog.presets.danger({
                title: options.title,
                subtitle: options.subtitle,
                objectLabel: options.objectLabel,
                objectName: options.objectName,
                summary: options.summary,
                notice: options.notice,
                checklist: options.checklist,
                acknowledgement: options.acknowledgement,
                confirmText: options.confirmText,
                cancelText: options.cancelText,
                options: options
            });
        }
        return global.LuminaActionDialog.open(options);
    }

    async function handleSubmit(event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.matches('[data-lumina-confirm]')) return;

        if (form.dataset.luminaConfirmBypass === 'true') {
            delete form.dataset.luminaConfirmBypass;
            return;
        }

        event.preventDefault();
        event.stopImmediatePropagation();
        if (form.dataset.luminaConfirmPending === 'true') return;
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        if (!global.LuminaActionDialog) return;

        form.dataset.luminaConfirmPending = 'true';
        var submitter = event.submitter;
        try {
            var result = await openForForm(form);
            if (!result.confirmed) return;
            form.dataset.luminaConfirmBypass = 'true';
            form.requestSubmit(submitter && submitter.form === form ? submitter : undefined);
        } finally {
            delete form.dataset.luminaConfirmPending;
        }
    }

    document.addEventListener('submit', handleSubmit, true);
    global.LuminaActionBindings = Object.freeze({ openForForm: openForForm });
})(window);
