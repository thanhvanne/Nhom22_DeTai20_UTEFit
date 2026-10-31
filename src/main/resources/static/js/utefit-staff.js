(() => {
    'use strict';
    if (!document.querySelector('.pt-header, .manager-header')) return;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
    const accounts = [...document.querySelectorAll('[data-staff-account]')];
    const closeAccount = (account, restore = false) => {
        if (!account.open || account.classList.contains('is-closing')) return;
        account.classList.add('is-closing');
        if (restore) account.querySelector('summary').focus();
        window.setTimeout(() => {
            account.open = false;
            account.classList.remove('is-closing');
            account.querySelector('summary').setAttribute('aria-expanded', 'false');
        }, reduced.matches ? 0 : 150);
    };
    accounts.forEach(account => {
        const summary = account.querySelector('summary');
        const items = [...account.querySelectorAll('.staff-account-menu a, .staff-account-menu button')];
        summary.addEventListener('click', event => {
            event.preventDefault();
            if (account.open) closeAccount(account);
            else {
                accounts.filter(other => other !== account).forEach(other => closeAccount(other));
                account.open = true;
                summary.setAttribute('aria-expanded', 'true');
            }
        });
        account.addEventListener('keydown', event => {
            if (event.key === 'Escape') { event.preventDefault(); closeAccount(account, true); return; }
            if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
            event.preventDefault(); account.open = true; summary.setAttribute('aria-expanded', 'true');
            const current = items.indexOf(document.activeElement);
            const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1
                : event.key === 'ArrowDown' ? (current + 1) % items.length : current < 0 ? items.length - 1 : (current - 1 + items.length) % items.length;
            items[next]?.focus();
        });
        account.addEventListener('focusout', () => window.setTimeout(() => {
            if (!account.contains(document.activeElement)) closeAccount(account);
        }, 0));
    });
    document.addEventListener('click', event => accounts.forEach(account => { if (!account.contains(event.target)) closeAccount(account); }));

    const header = document.querySelector('[data-staff-header]');
    if (header) {
        let frame = 0;
        const paint = () => { frame = 0; header.classList.toggle('is-scrolled', window.scrollY > 12); };
        const scroll = () => { if (!frame) frame = window.requestAnimationFrame(paint); };
        const mount = () => { paint(); window.addEventListener('scroll', scroll, { passive: true }); };
        mount();
        window.addEventListener('pagehide', () => { window.removeEventListener('scroll', scroll); window.cancelAnimationFrame(frame); frame = 0; });
        window.addEventListener('pageshow', event => { if (event.persisted) mount(); });
        const navigation = header.querySelector('.manager-navigation');
        const toggle = header.querySelector('.manager-menu-toggle');
        navigation?.addEventListener('show.bs.offcanvas', () => toggle?.setAttribute('aria-expanded', 'true'));
        navigation?.addEventListener('hidden.bs.offcanvas', () => toggle?.setAttribute('aria-expanded', 'false'));
        const desktop = window.matchMedia('(min-width: 992px)');
        const closeDrawer = () => window.bootstrap?.Offcanvas.getInstance(navigation)?.hide();
        desktop.addEventListener('change', () => { if (desktop.matches) closeDrawer(); });
        const highlight = () => header.querySelectorAll('[data-manager-nav]').forEach(link => {
            const target = new URL(link.href);
            const active = window.location.pathname.startsWith('/manager/schedules') ? target.hash === '#manager-schedules'
                : target.pathname === window.location.pathname && target.hash === window.location.hash;
            if (active) link.setAttribute('aria-current', target.hash ? 'location' : 'page');
            else link.removeAttribute('aria-current');
        });
        highlight(); window.addEventListener('hashchange', highlight);
        navigation?.addEventListener('click', event => { if (event.target.closest('a') && !desktop.matches) closeDrawer(); });
    }

    const dialog = document.querySelector('[data-staff-dialog]');
    const fullImage = dialog?.querySelector('[data-staff-avatar-full]');
    const closeButton = dialog?.querySelector('[data-staff-avatar-close]');
    let returnFocus;
    let dialogTimer;
    const closeViewer = () => {
        if (!dialog?.open || dialog.classList.contains('is-closing')) return;
        dialog.classList.add('is-closing');
        dialogTimer = window.setTimeout(() => dialog.close(), reduced.matches ? 0 : 150);
    };
    const openViewer = (image, trigger) => {
        if (!dialog || !image || !image.complete || !image.naturalWidth || typeof dialog.showModal !== 'function' || dialog.open) return;
        returnFocus = trigger; fullImage.src = image.currentSrc || image.src;
        dialog.classList.remove('is-closing'); dialog.showModal(); document.body.classList.add('staff-viewing'); closeButton.focus();
    };
    closeButton?.addEventListener('click', closeViewer);
    dialog?.addEventListener('click', event => { if (event.target === dialog) closeViewer(); });
    dialog?.addEventListener('cancel', event => { event.preventDefault(); closeViewer(); });
    dialog?.addEventListener('keydown', event => { if (event.key === 'Tab') { event.preventDefault(); closeButton.focus(); } });
    dialog?.addEventListener('close', () => { window.clearTimeout(dialogTimer); document.body.classList.remove('staff-viewing'); if (returnFocus?.isConnected) returnFocus.focus({ preventScroll: true }); });
    document.querySelectorAll('[data-staff-avatar]').forEach(image => {
        const button = image.closest('[data-staff-avatar-view]');
        const update = () => {
            const ready = image.complete && image.naturalWidth > 0;
            image.hidden = !ready;
            if (button) {
                button.disabled = !ready || !dialog || typeof dialog.showModal !== 'function';
                const hint = button.parentElement.querySelector('[data-staff-avatar-hint]');
                if (hint) hint.textContent = ready ? 'Nhấn vào ảnh để phóng to' : 'Ảnh đại diện của bạn';
            }
        };
        image.addEventListener('load', update); image.addEventListener('error', update); update();
        button?.addEventListener('click', () => openViewer(image, button));
    });

    const resetBusy = form => {
        form.removeAttribute('aria-busy');
        form.querySelectorAll('.is-submitting').forEach(button => { button.classList.remove('is-submitting'); button.removeAttribute('aria-disabled'); });
    };
    const busy = (form, submitter) => {
        form.setAttribute('aria-busy', 'true');
        if (submitter) { submitter.classList.add('is-submitting'); submitter.setAttribute('aria-disabled', 'true'); }
    };
    document.querySelectorAll('[data-staff-submit]').forEach(form => form.addEventListener('submit', event => {
        if (event.defaultPrevented) return;
        if (form.getAttribute('aria-busy') === 'true') { event.preventDefault(); return; }
        busy(form, event.submitter);
    }));
    window.addEventListener('pageshow', () => document.querySelectorAll('[data-staff-submit],[data-staff-profile-form]').forEach(resetBusy));
    document.querySelectorAll('[data-staff-file-label]').forEach(input => input.addEventListener('change', () => {
        const label = input.closest('.staff-upload')?.querySelector('[data-staff-file-name]');
        if (label) label.textContent = input.files?.[0]?.name || 'Ảnh trên thiết bị · tối đa 10 MB';
    }));

    const editor = document.querySelector('[data-staff-profile-editor]');
    if (document.body.classList.contains('uf-page-pt-profile-detail')) {
        try {
            const savedAt = Number(sessionStorage.getItem('utefit.pt.profile.saved'));
            sessionStorage.removeItem('utefit.pt.profile.saved');
            if (savedAt && Date.now() - savedAt < 15000 && !document.querySelector('[data-staff-server-success]')) {
                const notice = document.createElement('div');
                notice.className = 'alert alert-success'; notice.setAttribute('role', 'status');
                notice.textContent = 'Cập nhật hồ sơ PT thành công.';
                document.querySelector('.staff-profile-hero')?.before(notice);
            }
        } catch {}
    }
    if (editor) {
        const fileInput = editor.querySelector('[data-staff-avatar-file]');
        const preview = editor.querySelector('[data-staff-preview]');
        const previewPanel = editor.querySelector('[data-staff-preview-panel]');
        const previewButton = editor.querySelector('[data-staff-preview-view]');
        const avatarForm = editor.querySelector('[data-staff-profile-form="avatar"]');
        const information = editor.querySelector('[data-staff-profile-form="information"]');
        const fields = ['specialization', 'yearsExperience', 'bio'];
        const initial = fields.map(name => information.elements.namedItem(name).value);
        let generation = 0;
        let previewUrl;
        const error = (form, text) => { const element = form.querySelector('[data-staff-form-error]'); element.textContent = text; element.hidden = !text; };
        const clear = () => {
            generation += 1; if (previewUrl) URL.revokeObjectURL(previewUrl); previewUrl = null;
            preview.removeAttribute('src'); previewPanel.hidden = true; fileInput.value = ''; fileInput.setCustomValidity('');
            editor.querySelector('[data-staff-file-name]').textContent = 'JPG, PNG hoặc GIF · tối đa 5 MB'; error(avatarForm, '');
        };
        fileInput.addEventListener('change', async () => {
            const file = fileInput.files?.[0]; const request = ++generation;
            if (previewUrl) URL.revokeObjectURL(previewUrl); previewUrl = null; previewPanel.hidden = true;
            fileInput.setCustomValidity(''); error(avatarForm, '');
            if (!file) { clear(); return; }
            const message = !['image/jpeg', 'image/png', 'image/gif'].includes(file.type) ? 'Chỉ hỗ trợ JPG, PNG hoặc GIF.'
                : file.size === 0 || file.size > 5 * 1024 * 1024 ? 'Ảnh phải có dữ liệu và không vượt quá 5 MB.' : '';
            if (message) { fileInput.setCustomValidity(message); error(avatarForm, message); return; }
            const candidate = URL.createObjectURL(file); const image = new Image(); image.src = candidate;
            try {
                await image.decode();
                if (request !== generation) { URL.revokeObjectURL(candidate); return; }
                previewUrl = candidate; preview.src = candidate; previewPanel.hidden = false;
                editor.querySelector('[data-staff-file-name]').textContent = file.name;
            } catch {
                URL.revokeObjectURL(candidate);
                if (request === generation) { fileInput.setCustomValidity('Không đọc được ảnh. Vui lòng chọn ảnh khác.'); error(avatarForm, 'Không đọc được ảnh. Vui lòng chọn ảnh khác.'); }
            }
        });
        previewButton.addEventListener('click', () => openViewer(preview, previewButton));
        editor.querySelector('[data-staff-preview-clear]').addEventListener('click', clear);
        window.addEventListener('pagehide', event => { if (!event.persisted && previewUrl) URL.revokeObjectURL(previewUrl); });
        editor.querySelectorAll('[data-staff-profile-form]').forEach(form => form.addEventListener('submit', async event => {
            if (event.defaultPrevented) return;
            event.preventDefault();
            if (editor.getAttribute('aria-busy') === 'true') return;
            if (window.UTEFitValidation ? !window.UTEFitValidation.validateForm(form) : !form.checkValidity()) return;
            if (form === avatarForm) {
                if (!fileInput.files?.length || !previewUrl) { error(form, 'Vui lòng chọn và xem trước ảnh hợp lệ trước khi lưu.'); return; }
                const changed = fields.some((name, index) => information.elements.namedItem(name).value !== initial[index]);
                if (changed && !window.confirm('Thông tin chuyên môn chưa lưu. Gửi kèm các thay đổi này khi cập nhật ảnh?')) return;
                if (window.UTEFitValidation ? !window.UTEFitValidation.validateForm(information) : !information.checkValidity()) return;
                fields.forEach(name => { avatarForm.elements.namedItem(name).value = information.elements.namedItem(name).value; });
            } else if (fileInput.files?.length && !window.confirm('Ảnh mới chưa lưu. Tiếp tục lưu thông tin và bỏ lựa chọn ảnh này?')) return;
            error(form, ''); busy(form, event.submitter); editor.setAttribute('aria-busy', 'true');
            try {
                const response = await fetch(form.action, { method: form.method.toUpperCase(), body: new FormData(form, event.submitter), credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } });
                if (!response.ok && response.headers.get('content-type')?.includes('application/json')) {
                    const failure = await response.json();
                    window.UTEFitValidation?.applyErrors(form, failure.fieldErrors || {});
                    throw new Error(failure.message || failure.error || 'Vui lòng kiểm tra dữ liệu.');
                }
                const destination = new URL(response.url);
                if (!response.ok || destination.origin !== location.origin || !destination.pathname.endsWith('/pt/profile') || !response.headers.get('content-type')?.includes('text/html')) throw new Error('Không nhận được phản hồi lưu hợp lệ. Kiểm tra hồ sơ hoặc phiên đăng nhập trước khi gửi lại.');
                const page = new DOMParser().parseFromString(await response.text(), 'text/html');
                const failure = page.querySelector('[data-staff-server-error]');
                if (failure) { error(form, failure.textContent.trim()); return; }
                if (!page.querySelector('[data-staff-server-success]')) throw new Error('Chưa xác nhận được kết quả lưu. Kiểm tra hồ sơ trước khi thử lại.');
                try { sessionStorage.setItem('utefit.pt.profile.saved', String(Date.now())); } catch {}
                window.location.assign(response.url);
            } catch (failure) { error(form, failure.message || 'Mất kết nối. Kiểm tra hồ sơ trước khi gửi lại.'); }
            finally { resetBusy(form); editor.removeAttribute('aria-busy'); }
        }));
    }

    if (!reduced.matches && 'IntersectionObserver' in window) {
        const sections = [...document.querySelectorAll('[data-staff-reveal], .pt-work-panel')];
        const observer = new IntersectionObserver(entries => entries.forEach(entry => {
            if (!entry.isIntersecting) return;
            entry.target.animate?.([{ opacity: .55, transform: 'translateY(8px)' }, { opacity: 1, transform: 'none' }], { duration: 280, delay: (sections.indexOf(entry.target) % 3) * 40, easing: 'ease-out' });
            observer.unobserve(entry.target);
        }), { threshold: .06 });
        sections.forEach(section => observer.observe(section));
    }
})();
