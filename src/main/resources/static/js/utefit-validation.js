(() => {
    'use strict';
    const core = window.UTEFitValidationCore;
    let contract;
    let unavailable = false;
    const crossFields = new Set(['password', 'newPassword', 'confirmPassword', 'startTime', 'endTime', 'openingTime', 'closingTime', 'startDate', 'endDate', 'from', 'to', 'scheduleDate']);
    let sequence = 0;
    const touched = new WeakSet();
    const pathOf = form => new URL(form.action || location.href, location.href).pathname;
    const controls = form => [...form.elements].filter(field => /^(INPUT|SELECT|TEXTAREA)$/u.test(field.tagName) && field.name && !field.disabled && !/^(submit|button|reset)$/u.test(field.type) && !field.name.startsWith('_'));
    const find = (form, name) => controls(form).find(field => field.name === name);
    const visible = field => field && field.type !== 'hidden' && field.getClientRects().length > 0;
    const focus = field => {
        if (!field) return;
        const panel = field.closest('[data-profile-panel]');
        if (panel?.hidden) document.querySelector(`[data-profile-open="${panel.dataset.profilePanel}"]`)?.click();
        const details = field.closest('details');
        if (details) details.open = true;
        if (!visible(field)) return;
        field.focus({ preventScroll: true });
        field.scrollIntoView({ block: 'center', behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
    };
    function setFieldError(field, message) {
        if (!field) return;
        let output = field._utefitError;
        if (!output) {
            const wrapper = field.closest('.u025-f, .u036, .input-group, .form-floating') || (field.parentElement.tagName === 'LABEL' ? field.parentElement : field);
            output = document.createElement('span');
            output.className = 'ut-field-error';
            output.id = `ut-field-error-${++sequence}`;
            output.setAttribute('aria-live', 'polite');
            output.dataset.fieldError = field.name;
            const inset = wrapper.matches('.u025-f');
            const existing = inset ? wrapper.querySelector(':scope > .text-danger, :scope > .invalid-feedback') : wrapper.nextElementSibling;
            if (existing?.matches('.text-danger, .invalid-feedback')) existing.hidden = true;
            if (inset) wrapper.append(output);
            else wrapper.after(output);
            field._utefitError = output;
            const described = new Set((field.getAttribute('aria-describedby') || '').split(/\s+/u).filter(Boolean));
            described.add(output.id);
            field.setAttribute('aria-describedby', [...described].join(' '));
        }
        output.textContent = message || '';
        output.hidden = !message;
        field.classList.toggle('is-invalid', Boolean(message));
        field.setAttribute('aria-invalid', String(Boolean(message)));
        field.closest('.u025-f, .u036')?.classList.toggle('has-validation-error', Boolean(message));
    }
    function messageFor(field) {
        const form = field.form;
        const rule = core.ruleFor(contract, pathOf(form), form.method, field.name);
        if (field.validity.customError) return field.validationMessage;
        if (field.type === 'file') {
            const file = field.files?.[0];
            if (!file) return field.required || pathOf(form).endsWith('/avatar') || form.dataset.staffProfileForm === 'avatar' ? 'Vui lòng chọn ảnh đại diện.' : '';
            const video = field.name === 'video';
            if (file.size > (video ? 50 : 5) * 1024 * 1024) return video ? 'Video tối đa 50 MiB.' : 'Ảnh tối đa 5 MiB.';
            if (!(video ? /\.(mp4|webm)$/iu : /\.(jpe?g|png|gif)$/iu).test(file.name)) return video ? 'Chỉ hỗ trợ video MP4 hoặc WebM.' : 'Chỉ hỗ trợ ảnh JPG, PNG hoặc GIF.';
            return '';
        }
        const value = field.type === 'checkbox' ? (field.checked ? field.value : '') : field.value;
        if ((field.required || rule?.required) && !value.trim()) return 'Vui lòng nhập hoặc chọn giá trị cho trường này.';
        if (!value && !field.validity.badInput) return '';
        if (field.validity.badInput) return 'Giá trị sai định dạng. Vui lòng nhập lại.';
        const error = core.errorFor(rule, value);
        if (error) return error;
        if (field.maxLength > 0 && value.length > field.maxLength) return `Trường này tối đa ${field.maxLength} ký tự.`;
        if (field.type === 'number') {
            if (field.validity.rangeUnderflow) return `Giá trị phải từ ${field.min} trở lên.`;
            if (field.validity.rangeOverflow) return `Giá trị không được vượt quá ${field.max}.`;
            if (field.validity.stepMismatch) return field.step === '1' || !field.step ? 'Vui lòng nhập số nguyên.' : 'Số chữ số thập phân không hợp lệ.';
        }
        if (field.type === 'date' && value && (!/^\d{4}-\d{2}-\d{2}$/u.test(value) || value.startsWith('0000-'))) return 'Vui lòng chọn ngày hợp lệ.';
        if (!rule?.pattern && field.validity.patternMismatch) return field.title || 'Định dạng chưa đúng. Vui lòng kiểm tra lại.';
        return '';
    }
    function validateField(field, shouldNormalize = false) {
        if (!contract || !field?.form || field.disabled) return true;
        const rule = core.ruleFor(contract, pathOf(field.form), field.form.method, field.name);
        if (shouldNormalize && field.type !== 'file' && rule?.normalize) field.value = core.normalize(field.value, rule);
        const message = messageFor(field);
        setFieldError(field, message);
        return !message;
    }
    function applyErrors(form, errors, shouldFocus = true) {
        if (!form) return;
        let first;
        Object.entries(errors || {}).forEach(([name, message]) => {
            const field = find(form, name);
            if (!field) return;
            touched.add(field);
            setFieldError(field, message);
            if (!first && field.type !== 'hidden') first = field;
        });
        first = controls(form).find(field => field.type !== 'hidden' && field.getAttribute('aria-invalid') === 'true') || first;
        if (shouldFocus && first) focus(first);
    }
    function validateForm(form, shouldFocus = true) {
        if (!contract) return false;
        let first;
        const fields = controls(form);
        fields.forEach(field => {
            touched.add(field);
            if (!validateField(field, true) && !first && visible(field)) first = field;
        });
        const values = Object.fromEntries(fields.filter(field => field.type !== 'file').map(field => [field.name, field.value]));
        const cross = core.crossErrors(pathOf(form), values);
        applyErrors(form, cross, false);
        if (!first) first = fields.find(field => visible(field) && field.getAttribute('aria-invalid') === 'true');
        if (first && shouldFocus) focus(first);
        return !fields.some(field => field.getAttribute('aria-invalid') === 'true');
    }
    document.addEventListener('submit', event => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || !controls(form).length) return;
        if (!unavailable && !validateForm(form)) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);
    async function initialize() {
        try {
            const response = await fetch('/js/validation-rules.json', { credentials: 'same-origin', cache: 'no-cache' });
            if (!response.ok) throw new Error('validation');
            contract = await response.json();
        } catch (_) {
            unavailable = true;
            document.querySelectorAll('form').forEach(form => {
                form.noValidate = false;
            });
            return;
        }
        document.querySelectorAll('form').forEach(form => {
            form.noValidate = true;
            controls(form).forEach(field => {
                const crossUpdate = () => {
                    if (!crossFields.has(field.name)) return;
                    const values = Object.fromEntries(controls(form).map(control => [control.name, control.value]));
                    const errors = core.crossErrors(pathOf(form), values);
                    controls(form).filter(control => crossFields.has(control.name) && touched.has(control)).forEach(control => setFieldError(control, errors[control.name] || messageFor(control)));
                };
                field.addEventListener('blur', () => { touched.add(field); validateField(field, true); crossUpdate(); });
                const update = () => {
                    if (touched.has(field)) validateField(field);
                    crossUpdate();
                    const confirmation = find(form, 'confirmPassword');
                    if (confirmation && touched.has(confirmation) && /^(password|newPassword|confirmPassword)$/u.test(field.name)) {
                        const values = Object.fromEntries(controls(form).map(control => [control.name, control.value]));
                        setFieldError(confirmation, core.crossErrors(pathOf(form), values).confirmPassword || messageFor(confirmation));
                    }
                };
                field.addEventListener('input', update);
                field.addEventListener('change', update);
            });
        });
        const payload = window.UTEFitValidationState || {};
        const candidates = [...document.forms].filter(candidate => pathOf(candidate) === payload.action);
        const score = candidate => Object.keys(payload.errors || {}).filter(name => {
            const field = find(candidate, name);
            return field && field.type !== 'hidden';
        }).length;
        let form = candidates.sort((first, second) => score(second) - score(first))[0];
        if (!form && payload.errors) form = [...document.forms].find(candidate => Object.keys(payload.errors).some(name => find(candidate, name)));
        if (form) {
            Object.entries(payload.values || {}).forEach(([name, value]) => {
                const fields = controls(form).filter(field => field.name === name);
                fields.forEach(field => {
                    if (/^(file|password|hidden)$/u.test(field.type)) return;
                    if (field.type === 'checkbox' || field.type === 'radio') field.checked = (Array.isArray(value) ? value : [value]).includes(field.value);
                    else field.value = Array.isArray(value) ? value[0] : value;
                    field.dispatchEvent(new Event('change', { bubbles: true }));
                });
            });
            applyErrors(form, payload.errors);
        }
        document.documentElement.dataset.validationReady = 'true';
        document.dispatchEvent(new Event('utefit:validation-ready'));
    }
    window.UTEFitValidation = { validateField, validateForm, setFieldError, applyErrors };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once: true });
    else initialize();
})();
