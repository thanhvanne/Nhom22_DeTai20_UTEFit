(() => {
    'use strict';
    const root = document.querySelector('[data-member-profile]');
    if (!root) return;

    const tabs = [...root.querySelectorAll('[data-profile-tab]')];
    const panels = [...root.querySelectorAll('[data-profile-panel]')];
    const tabList = root.querySelector('[data-profile-tabs]');
    const storageKey = 'utefit-profile-submit';
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let activeTab = 'overview';

    function moveIndicator() {
        const selected = tabs.find(tab => tab.dataset.profileTab === activeTab);
        if (!selected) return;
        tabList.style.setProperty('--profile-tab-width', `${selected.offsetWidth}px`);
        tabList.style.setProperty('--profile-tab-left', `${selected.offsetLeft}px`);
    }

    function activateTab(name, focusTab = false) {
        const selected = tabs.find(tab => tab.dataset.profileTab === name);
        if (!selected) return;
        activeTab = name;
        tabs.forEach(tab => {
            const selectedTab = tab === selected;
            tab.setAttribute('aria-selected', String(selectedTab));
            tab.tabIndex = selectedTab ? 0 : -1;
        });
        panels.forEach(panel => {
            panel.hidden = panel.dataset.profilePanel !== name;
            panel.setAttribute('role', 'tabpanel');
            panel.tabIndex = 0;
        });
        moveIndicator();
        if (focusTab) selected.focus({ preventScroll: true });
    }

    let initialTab = root.dataset.initialTab || 'overview';
    try {
        const previous = JSON.parse(sessionStorage.getItem(storageKey) || 'null');
        sessionStorage.removeItem(storageKey);
        if (initialTab === 'overview' && root.querySelector('[data-profile-server-error]') && previous && Date.now() - previous.time < 300000) initialTab = previous.tab;
    } catch {}
    if (!tabs.some(tab => tab.dataset.profileTab === initialTab)) initialTab = 'overview';
    tabList.hidden = false;
    activateTab(initialTab);
    tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => activateTab(tab.dataset.profileTab));
        tab.addEventListener('keydown', event => {
            let nextIndex;
            if (event.key === 'ArrowRight') nextIndex = (index + 1) % tabs.length;
            if (event.key === 'ArrowLeft') nextIndex = (index + tabs.length - 1) % tabs.length;
            if (event.key === 'Home') nextIndex = 0;
            if (event.key === 'End') nextIndex = tabs.length - 1;
            if (nextIndex === undefined) return;
            event.preventDefault();
            activateTab(tabs[nextIndex].dataset.profileTab, true);
        });
    });
    root.querySelectorAll('[data-profile-open]').forEach(button => {
        button.hidden = false;
        button.addEventListener('click', () => activateTab(button.dataset.profileOpen, true));
    });
    window.addEventListener('resize', moveIndicator, { passive: true });
    document.fonts?.ready.then(moveIndicator);

    const dialog = root.querySelector('#profile-avatar-dialog');
    const viewButton = root.querySelector('[data-avatar-view]');
    const avatar = viewButton.querySelector('[data-profile-avatar]');
    const fullImage = root.querySelector('[data-avatar-full]');
    const hint = root.querySelector('[data-avatar-hint]');
    const closeButton = root.querySelector('[data-avatar-close]');
    let closeTimer;
    let returnFocus;

    function closeViewer() {
        if (!dialog.open || dialog.classList.contains('is-closing')) return;
        dialog.classList.add('is-closing');
        closeTimer = window.setTimeout(() => dialog.close(), reducedMotion.matches ? 0 : 150);
    }

    function updateAvatarState() {
        const available = avatar && !avatar.hidden && avatar.complete && avatar.naturalWidth > 0;
        viewButton.disabled = !available || typeof dialog.showModal !== 'function';
        hint.textContent = available ? 'Nhấn vào ảnh để phóng to' : 'Ảnh đại diện của bạn';
    }

    root.querySelectorAll('[data-profile-avatar]').forEach(image => {
        image.addEventListener('load', updateAvatarState);
        image.addEventListener('error', () => {
            image.hidden = true;
            updateAvatarState();
            if (image === avatar) closeViewer();
        });
        if (image.complete && image.naturalWidth === 0) image.hidden = true;
    });
    updateAvatarState();
    viewButton.addEventListener('click', () => {
        if (viewButton.disabled || dialog.open) return;
        returnFocus = document.activeElement;
        fullImage.src = avatar.currentSrc || avatar.src;
        dialog.classList.remove('is-closing');
        dialog.showModal();
        document.body.classList.add('uf-profile-viewing');
    });
    closeButton.addEventListener('click', closeViewer);
    dialog.addEventListener('keydown', event => {
        if (event.key !== 'Tab') return;
        event.preventDefault();
        closeButton.focus();
    });
    dialog.addEventListener('click', event => { if (event.target === dialog) closeViewer(); });
    dialog.addEventListener('cancel', event => { event.preventDefault(); closeViewer(); });
    dialog.addEventListener('close', () => {
        window.clearTimeout(closeTimer);
        dialog.classList.remove('is-closing');
        document.body.classList.remove('uf-profile-viewing');
        fullImage.removeAttribute('src');
        returnFocus?.focus({ preventScroll: true });
    });
    fullImage.addEventListener('error', () => {
        closeViewer();
        if (avatar) avatar.hidden = true;
        updateAvatarState();
    });

    const fileInput = root.querySelector('#member-avatar-upload');
    const preview = root.querySelector('[data-avatar-preview]');
    const fileName = root.querySelector('#profile-avatar-file');
    const fileError = root.querySelector('#profile-avatar-error');
    let previewUrl;

    function releasePreview() {
        preview.hidden = true;
        preview.removeAttribute('src');
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = undefined;
    }

    function showFileError(message) {
        fileInput.setCustomValidity(message);
        fileInput.setAttribute('aria-invalid', String(Boolean(message)));
        fileError.textContent = message;
        fileError.hidden = !message;
        window.UTEFitValidation?.setFieldError(fileInput, message);
    }

    fileInput.addEventListener('change', () => {
        releasePreview();
        showFileError('');
        const file = fileInput.files[0];
        fileName.textContent = file ? file.name : 'Chưa chọn ảnh mới.';
        if (!file) return;
        if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
            showFileError('Vui lòng chọn ảnh JPG, PNG hoặc GIF.');
            return;
        }
        if (file.size === 0 || file.size > 5 * 1024 * 1024) {
            showFileError('Ảnh phải có dung lượng lớn hơn 0 và không quá 5 MB.');
            return;
        }
        fileInput.setCustomValidity('Đang kiểm tra ảnh. Vui lòng đợi một chút.');
        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
    });
    preview.addEventListener('load', () => {
        if (!previewUrl || preview.getAttribute('src') !== previewUrl) return;
        preview.hidden = false;
        showFileError('');
        fileName.textContent = `${fileInput.files[0].name} · Xem trước, chưa lưu`;
    });
    preview.addEventListener('error', () => {
        if (!previewUrl) return;
        releasePreview();
        showFileError('Không thể đọc ảnh này. Vui lòng chọn ảnh khác.');
    });

    const forms = [...root.querySelectorAll('[data-profile-form]')];
    const buttonLabels = new Map();
    const initialValues = new Map(forms.map(form => [form, [...form.querySelectorAll('input:not([type=hidden])')].map(input => [input, input.value])]));
    function hasUnsavedChanges(form) {
        return initialValues.get(form).some(([input, value]) => input.type === 'file' ? input.files.length > 0 : input.value !== value);
    }
    forms.forEach(form => {
        const button = form.querySelector('[type=submit]');
        buttonLabels.set(button, button.textContent);
        form.addEventListener('submit', event => {
            if (event.defaultPrevented) return;
            if (form.getAttribute('aria-busy') === 'true') {
                event.preventDefault();
                return;
            }
            const otherDraft = forms.some(other => other !== form && hasUnsavedChanges(other));
            if (otherDraft && !window.confirm('Biểu mẫu khác đang có thay đổi chưa lưu. Tiếp tục sẽ tải lại trang và bỏ những thay đổi đó. Bạn có muốn tiếp tục?')) {
                event.preventDefault();
                return;
            }
            try { sessionStorage.setItem(storageKey, JSON.stringify({ tab: form.dataset.profileForm, time: Date.now() })); } catch {}
            form.setAttribute('aria-busy', 'true');
            button.disabled = true;
            button.textContent = button.dataset.busyLabel || 'Đang lưu…';
        });
    });
    window.addEventListener('pageshow', () => {
        forms.forEach(form => {
            form.removeAttribute('aria-busy');
            const button = form.querySelector('[type=submit]');
            button.disabled = false;
            button.textContent = buttonLabels.get(button);
        });
        moveIndicator();
    });
    window.addEventListener('pagehide', event => {
        if (dialog.open) dialog.close();
        if (!event.persisted) releasePreview();
    });
})();
