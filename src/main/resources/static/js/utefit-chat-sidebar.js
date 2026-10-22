/*
 * UTEFit Chat Sidebar.
 *
 * Tìm kiếm và lọc trên danh sách hội thoại
 * đã được backend cung cấp.
 *
 * Không gọi API mới.
 * Không thay đổi logic WebSocket hoặc HTTP chat.
 */

(() => {
    'use strict';

    const search = document.getElementById('ufChatSearch');
    const list = document.getElementById('ufChatList');
    const empty = document.getElementById('ufChatNoMatches');

    const filters = Array.from(
        document.querySelectorAll('.uf-chat-filter')
    );

    if (!search || !list) {
        return;
    }

    const items = Array.from(
        list.querySelectorAll('[data-chat-item]')
    );

    let filter = 'all';

    // Tìm tên tiếng Việt không phân biệt dấu.
    const normalized = value =>
        String(value || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/đ/g, 'd')
            .replace(/Đ/g, 'D')
            .toLowerCase()
            .trim();

    function apply() {

        const keyword = normalized(search.value);
        let visible = 0;

        for (const item of items) {

            const matchesText = normalized(
                item.dataset.search
            ).includes(keyword);

            const matchesUnread =
                filter === 'all'
                || Number(item.dataset.unread || 0) > 0;

            const show = matchesText && matchesUnread;

            item.hidden = !show;

            if (show) {
                visible++;
            }
        }

        if (empty) {
            empty.hidden =
                visible !== 0 || items.length === 0;
        }
    }

    // Tìm kiếm tức thời.
    search.addEventListener('input', apply);

    // Chuyển bộ lọc.
    for (const button of filters) {

        button.addEventListener('click', () => {

            filter =
                button.dataset.filter === 'unread'
                    ? 'unread'
                    : 'all';

            for (const tab of filters) {

                const active = tab === button;

                tab.classList.toggle(
                    'is-active',
                    active
                );

                tab.setAttribute(
                    'aria-pressed',
                    String(active)
                );
            }

            apply();
        });
    }

    // Hiển thị trạng thái ban đầu.
    apply();

})();