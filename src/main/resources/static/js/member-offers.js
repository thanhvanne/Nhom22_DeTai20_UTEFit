(() => {
    'use strict';
    const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
    const forms = [...document.querySelectorAll('[data-offer-form]')];
    forms.forEach(form => {
        const quote = form.querySelector('[data-offer-quote]');
        const error = form.querySelector('[data-offer-error]');
        const button = form.querySelector('[type=submit]');
        const retry = form.querySelector('[data-offer-retry]');
        let timer;
        let controller;
        async function refresh() {
            window.clearTimeout(timer);
            controller?.abort();
            controller = new AbortController();
            const current = controller;
            const parameters = new URLSearchParams();
            ['branchId', 'planId', 'promotionCode', 'offerSelection'].forEach(name => parameters.set(name, form.elements.namedItem(name)?.value || ''));
            error.hidden = false;
            error.textContent = 'Đang kiểm tra ưu đãi…';
            quote.hidden = true;
            button.disabled = true;
            retry.hidden = true;
            try {
                const response = await fetch('/member/memberships/quote?' + parameters.toString(), { credentials: 'same-origin', headers: { Accept: 'application/json' }, cache: 'no-store', signal: current.signal });
                if (controller !== current || current.signal.aborted) return;
                if (response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Phiên đăng nhập đã hết hạn. Tải lại trang để tiếp tục.');
                const result = await response.json();
                if (result.fieldErrors) window.UTEFitValidation?.applyErrors(form, result.fieldErrors, false);
                if (!response.ok) throw new Error(result.error || 'Chưa thể kiểm tra ưu đãi.');
                if (controller !== current || current.signal.aborted) return;
                if (![result.originalPrice, result.discount, result.finalPrice].every(value => Number.isFinite(Number(value))) || Number(result.finalPrice) <= 0) throw new Error('Giá trả về chưa hợp lệ. Vui lòng tải lại trang.');
                form.querySelector('[data-offer-name]').textContent = result.name;
                form.querySelector('[data-offer-original]').textContent = money.format(result.originalPrice);
                form.querySelector('[data-offer-discount]').textContent = '−' + money.format(result.discount);
                form.querySelector('[data-offer-total]').textContent = money.format(result.finalPrice);
                quote.hidden = false;
                error.hidden = true;
                button.disabled = false;
            } catch (failure) {
                if (failure.name === 'AbortError' || controller !== current || current.signal.aborted) return;
                error.textContent = failure.message || 'Không thể tải giá. Vui lòng thử lại.';
                retry.hidden = false;
            }
        }
        function schedule() {
            window.clearTimeout(timer);
            controller?.abort();
            quote.hidden = true;
            button.disabled = true;
            timer = window.setTimeout(refresh, 250);
        }
        form.addEventListener('input', schedule);
        form.addEventListener('change', schedule);
        retry.addEventListener('click', refresh);
        if ('IntersectionObserver' in window) {
            const observer = new IntersectionObserver(entries => {
                if (!entries.some(entry => entry.isIntersecting)) return;
                observer.disconnect();
                refresh();
            }, { rootMargin: '120px' });
            observer.observe(form);
            window.addEventListener('pagehide', () => observer.disconnect(), { once: true });
            window.addEventListener('pageshow', event => { if (event.persisted) refresh(); });
        } else refresh();
        window.addEventListener('pagehide', () => { window.clearTimeout(timer); controller?.abort(); });
    });
})();
