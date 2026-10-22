/* Đánh dấu đúng menu trang hiện tại.
   Dropdown hover do CSS xử lý trên desktop. */

(() => {
    'use strict';

    const nav = document.querySelector('.ut-site-header');
    if (!nav) return;

    const clean = value => {
        let pathname;

        try {
            pathname = new URL(
                value,
                window.location.origin
            ).pathname;
        } catch (_) {
            return '';
        }

        return pathname.length > 1
            ? pathname.replace(/\/+$/, '')
            : pathname;
    };

    const current = clean(window.location.pathname);
    const links = [...nav.querySelectorAll('a[href]')];

    let bestLink = null;
    let score = -1;

    for (const link of links) {
        const url = new URL(
            link.href,
            window.location.origin
        );

        if (url.origin !== window.location.origin) {
            continue;
        }

        const path = clean(url.pathname);

        if (!path || path === '#' || path === '/auth/logout') {
            continue;
        }

        // Ưu tiên URL khớp chính xác.
        // Trang con chỉ khớp khi có ranh giới "/".
        const matched =
            current === path
            || (
                path !== '/'
                && current.startsWith(path + '/')
            );

        // Route chat PT cũ vẫn phải làm sáng menu chat.
        const ptChat =
            link.classList.contains('ut-pt-chat')
            && current.startsWith('/pt/chat/');

        if (!(matched || ptChat)) {
            continue;
        }

        const rank = ptChat ? 999 : path.length;

        if (rank > score) {
            bestLink = link;
            score = rank;
        }
    }

    if (bestLink) {
        bestLink.classList.add('ut-is-current');
        bestLink.setAttribute('aria-current', 'page');

        const parent = bestLink.closest('.nav-item.dropdown');

        // Nếu người dùng chọn một submenu,
        // đánh dấu luôn nhóm dropdown cha.
        if (parent) {
            parent.classList.add('ut-group-current');

            const toggle = parent.querySelector(
                ':scope > .dropdown-toggle'
            );

            if (toggle) {
                toggle.classList.add('ut-is-current');
            }
        }
    }

    // PT có cả mục Tin nhắn trên menu
    // và biểu tượng Tin nhắn bên phải.
    if (
        current === '/pt/chats'
        || current.startsWith('/pt/chat/')
    ) {
        nav.querySelectorAll('.ut-pt-chat').forEach(el => {
            el.classList.add('ut-is-current');
        });
    }

    // Tô biểu tượng chat của Member.
    if (
        current === '/member/chats'
        || current.startsWith('/member/chats/')
    ) {
        nav.querySelectorAll(
            '.ut-nav-icon[href*="/member/chats"]'
        ).forEach(el => {
            el.classList.add('ut-is-current');
        });
    }
})();