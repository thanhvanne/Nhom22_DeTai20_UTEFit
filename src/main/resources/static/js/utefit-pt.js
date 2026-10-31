(() => {
    'use strict';
    const initialize = () => {
        const header = document.querySelector('.pt-header');
        if (!header) return;
        const pathname = window.location.pathname;
        const links = [...header.querySelectorAll('[data-pt-nav]')];
        const candidates = links.filter(link => {
            const route = new URL(link.href).pathname;
            return pathname === route || pathname.startsWith(route + '/')
                || (route === '/pt/chats' && pathname.startsWith('/pt/chat/'));
        });
        const current = candidates.sort((first, second) => second.pathname.length - first.pathname.length)[0];
        current?.setAttribute('aria-current', 'page');
        document.querySelectorAll('main table').forEach(table => {
            const headings = [...table.querySelectorAll('thead th')];
            const rows = [...table.querySelectorAll('tbody tr')];
            if (!headings.length || !rows.length || rows.some(row => row.children.length !== headings.length || row.querySelector('[colspan]'))) return;
            rows.forEach(row => [...row.children].forEach((cell, index) => {
                cell.dataset.ptLabel = headings[index].textContent.trim();
            }));
            table.setAttribute('data-pt-responsive', '');
        });
        let scrollFrame = 0;
        const paintHeader = () => {
            scrollFrame = 0;
            header.classList.toggle('is-scrolled', window.scrollY > 12);
        };
        const onScroll = () => {
            if (!scrollFrame) scrollFrame = window.requestAnimationFrame(paintHeader);
        };
        const mount = () => {
            paintHeader();
            window.addEventListener('scroll', onScroll, { passive: true });
        };
        mount();
        const navigation = document.getElementById('pt-navigation');
        const toggle = header.querySelector('.pt-menu-toggle');
        const desktop = window.matchMedia('(min-width: 1200px)');
        const closeDrawer = () => {
            if (desktop.matches) window.bootstrap?.Offcanvas.getInstance(navigation)?.hide();
        };
        navigation?.addEventListener('show.bs.offcanvas', () => toggle?.setAttribute('aria-expanded', 'true'));
        navigation?.addEventListener('hidden.bs.offcanvas', () => toggle?.setAttribute('aria-expanded', 'false'));
        desktop.addEventListener('change', closeDrawer);
        window.addEventListener('pagehide', () => {
            window.removeEventListener('scroll', onScroll);
            window.cancelAnimationFrame(scrollFrame);
            scrollFrame = 0;
            desktop.removeEventListener('change', closeDrawer);
        });
        window.addEventListener('pageshow', event => {
            if (event.persisted) {
                mount();
                desktop.addEventListener('change', closeDrawer);
            }
        });
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once: true });
    else initialize();
})();
