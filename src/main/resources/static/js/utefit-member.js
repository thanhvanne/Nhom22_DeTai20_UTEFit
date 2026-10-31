/* Lucian UX/UI Prompt #039 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#039 */
/* Lucian UX/UI Prompt #286 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#286 */
(() => {
    'use strict';
    const initialize = () => {
        if (!document.body.classList.contains('uf-member')) return;
        const tooltipElements = document.querySelectorAll('.member-header [data-bs-toggle="tooltip"]');
        const mountTooltips = () => tooltipElements.forEach(element => {
            window.bootstrap?.Tooltip.getOrCreateInstance(element, { container: document.body, trigger: 'hover focus' });
        });
        mountTooltips();
        window.addEventListener('pagehide', () => tooltipElements.forEach(element => window.bootstrap?.Tooltip.getInstance(element)?.dispose()));
        window.addEventListener('pageshow', event => { if (event.persisted) mountTooltips(); });
        const pathname = window.location.pathname;
        document.querySelectorAll('[data-member-nav]').forEach(link => {
            const href = new URL(link.href).pathname;
            const active = pathname === href || pathname.startsWith(href + '/')
                || (link.dataset.memberNav === 'chats' && pathname.startsWith('/member/chat/'))
                || (link.dataset.memberNav === 'payments' && pathname.startsWith('/payments/vnpay/'));
            if (active) link.setAttribute('aria-current', 'page');
        });
        const navigation = document.getElementById('member-navigation');
        const menuToggle = document.querySelector('.member-menu-toggle');
        const header = document.querySelector('.member-header');
        if (header) {
            let scrollFrame = 0;
            const updateHeader = () => {
                scrollFrame = 0;
                header.classList.toggle('is-scrolled', window.scrollY > 12);
            };
            const onScroll = () => {
                if (!scrollFrame) scrollFrame = window.requestAnimationFrame(updateHeader);
            };
            const attachHeader = () => {
                updateHeader();
                window.addEventListener('scroll', onScroll, { passive: true });
            };
            attachHeader();
            window.addEventListener('pagehide', () => {
                window.removeEventListener('scroll', onScroll);
                window.cancelAnimationFrame(scrollFrame);
                scrollFrame = 0;
            });
            window.addEventListener('pageshow', event => { if (event.persisted) attachHeader(); });
        }
        if (navigation && menuToggle && header) {
            navigation.addEventListener('show.bs.offcanvas', () => {
                header.classList.add('is-open');
                menuToggle.setAttribute('aria-expanded', 'true');
            });
            navigation.addEventListener('hidden.bs.offcanvas', () => {
                header.classList.remove('is-open');
                menuToggle.setAttribute('aria-expanded', 'false');
            });
            const desktop = window.matchMedia('(min-width: 1200px)');
            const closeDrawer = () => {
                if (desktop.matches) window.bootstrap?.Offcanvas.getInstance(navigation)?.hide();
            };
            desktop.addEventListener('change', closeDrawer);
            window.addEventListener('pagehide', () => desktop.removeEventListener('change', closeDrawer));
            window.addEventListener('pageshow', event => {
                if (event.persisted) desktop.addEventListener('change', closeDrawer);
            });
        }
        const labels = new Map([
            ['PENDING', 'Chờ xử lý'], ['PAID', 'Đã thanh toán'], ['ACTIVE', 'Đang hoạt động'],
            ['EXPIRED', 'Đã hết hạn'], ['CANCELLED', 'Đã hủy'], ['CANCEL_REQUESTED', 'Chờ duyệt hủy'],
            ['FAILED', 'Không thành công'], ['REFUNDED', 'Đã hoàn tiền'], ['REFUND_PENDING', 'Chờ hoàn tiền'],
            ['REVIEW_REQUIRED', 'Cần đối soát'], ['ACCEPTED', 'Đã chấp nhận'], ['REJECTED', 'Đã từ chối'],
            ['COMPLETED', 'Đã hoàn thành'], ['SCHEDULED', 'Đã lên lịch'], ['REGISTERED', 'Đã đăng ký'],
            ['ATTENDED', 'Đã tham gia'], ['CASH', 'Tiền mặt'], ['SEPAY', 'Chuyển khoản VietQR'],
            ['VNPAY', 'VNPay'], ['SINGLE_BRANCH', 'Một chi nhánh'], ['MULTI_BRANCH', 'Nhiều chi nhánh'],
            ['ALL_BRANCHES', 'Tất cả chi nhánh'], ['ALL', 'Tất cả dịch vụ'], ['GYM', 'Phòng gym'], ['YOGA', 'Yoga']
        ]);
        document.querySelectorAll('[data-uf-label]').forEach(element => {
            if (element.matches('input, select, textarea, option') || element.children.length) return;
            const translated = labels.get(element.textContent.trim());
            if (translated) element.textContent = translated;
        });
        const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
        document.querySelectorAll('[data-uf-money]').forEach(element => {
            if (element.closest('#sepay-checkout') || element.matches('input, select, textarea, option') || element.children.length) return;
            const value = element.textContent.trim();
            if (/^[+-]?\d+(?:\.\d{1,2})?$/.test(value)) element.textContent = money.format(Number(value));
        });
        document.querySelectorAll('[data-uf-date]').forEach(element => {
            if (element.matches('input, select, textarea, option') || element.children.length) return;
            element.textContent = element.textContent.replace(/\b(\d{4})-(\d{2})-(\d{2})(?=T|\b)/g, '$3/$2/$1');
        });
        const filters = document.querySelector('.uf-chat-filters');
        if (!filters) return;
        const buttons = [...filters.querySelectorAll('.uf-chat-filter')];
        if (!buttons.length) return;
        const pill = document.createElement('span');
        pill.className = 'u039-pill';
        pill.setAttribute('aria-hidden', 'true');
        filters.prepend(pill);
        filters.classList.add('is-enhanced');
        const movePill = () => {
            const selected = buttons.find(button => button.getAttribute('aria-pressed') === 'true');
            if (!selected) return;
            pill.style.setProperty('--x', `${selected.offsetLeft}px`);
            pill.style.setProperty('--w', `${selected.offsetWidth}px`);
        };
        buttons.forEach((button, index) => button.addEventListener('keydown', event => {
            const direction = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
            if (!direction) return;
            event.preventDefault();
            const next = buttons[(index + direction + buttons.length) % buttons.length];
            next.focus();
            next.click();
        }));
        const changes = new MutationObserver(movePill);
        buttons.forEach(button => changes.observe(button, { attributes: true, attributeFilter: ['aria-pressed'] }));
        const sizes = new ResizeObserver(movePill);
        sizes.observe(filters);
        movePill();
        window.addEventListener('pagehide', () => { changes.disconnect(); sizes.disconnect(); }, { once: true });
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once: true });
    else initialize();
})();
