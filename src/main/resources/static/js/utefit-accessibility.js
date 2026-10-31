(() => {
    'use strict';

    const initializeAccessibility = () => {
        if (!document.body.classList.contains('uf-theme')) return;
        const main = document.querySelector('main');
        const skip = document.querySelector('.uf-skip-link');
        if (main && skip) {
            if (!main.id) main.id = 'utefit-content';
            main.tabIndex = -1;
            skip.href = `#${main.id}`;
            skip.hidden = false;
        }

        document.querySelectorAll('input:not([type=hidden]), select, textarea').forEach((input) => {
            if (input.labels?.length || input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby')) return;
            const container = input.closest('.mb-3, .col-md-6, .col-md-4, .col-md-3, .col-md-5');
            const label = container?.querySelector('label');
            if (label && input.id && !label.htmlFor) label.htmlFor = input.id;
            else if (input.getAttribute('placeholder')) input.setAttribute('aria-label', input.getAttribute('placeholder'));
        });

        document.querySelectorAll('.table-responsive').forEach((region) => {
            if (!region.querySelector('table')) return;
            region.tabIndex = 0;
            region.setAttribute('role', 'region');
            if (!region.hasAttribute('aria-label') && !region.hasAttribute('aria-labelledby')) {
                const title = region.closest('section, .surface-card, .ut-panel, main')?.querySelector('h1, h2, h3, h4, h5');
                region.setAttribute('aria-label', title?.textContent.trim() || 'Bảng dữ liệu');
            }
        });

        document.querySelectorAll('.table').forEach((table) => {
            const headers = [...table.querySelectorAll('thead tr:first-child > th')];
            const numeric = [];
            headers.forEach((header, index) => {
                if (!header.hasAttribute('scope')) header.scope = 'col';
                if (/^(số tiền|giá|đơn giá|doanh thu|tổng tiền|số lượng|sức chứa|số buổi|giảm giá|thời hạn)/i.test(header.textContent.trim())) {
                    header.classList.add('uf-number');
                    numeric.push(index);
                }
            });
            table.querySelectorAll('tbody tr').forEach((row) => {
                if (row.children.length !== headers.length || row.querySelector('[colspan]')) return;
                numeric.forEach((index) => row.children[index]?.classList.add('uf-number'));
            });
        });

        document.querySelectorAll('.page-item.active .page-link').forEach((link) => link.setAttribute('aria-current', 'page'));
    };

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initializeAccessibility, { once: true });
    else initializeAccessibility();
})();
