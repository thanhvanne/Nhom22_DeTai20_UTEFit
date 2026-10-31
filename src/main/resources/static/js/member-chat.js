/* UI Member chat + notification, tích hợp Service do B sở hữu.
   DOM APIs + textContent: không đưa nội dung chat/notification vào innerHTML. */
(() => {
    'use strict';

    function subscribe(url, destination, callback, statusElement) {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            if (statusElement) statusElement.textContent = 'Chế độ tự cập nhật';
            return;
        }
        try {
            const client = Stomp.over(new SockJS(url));
            client.debug = null;
            client.connect({}, () => {
                if (statusElement) statusElement.textContent = 'Đang kết nối trực tiếp';
                client.subscribe(destination, callback);
            }, () => {
                if (statusElement) statusElement.textContent = 'Chế độ tự cập nhật';
            });
        } catch (_) {
            if (statusElement) statusElement.textContent = 'Chế độ tự cập nhật';
        }
    }

    const chat = document.getElementById('memberChat');
    if (chat) {
        const conversationId = Number(chat.dataset.conversationId);
        const currentUserId = Number(chat.dataset.currentUserId);
        const list = document.getElementById('chatMessages');
        const form = document.getElementById('chatForm');
        const input = document.getElementById('chatInput');
        const sendButton = document.getElementById('chatSend');
        const error = document.getElementById('chatError');
        const status = document.getElementById('chatConnection');
        let busy = false;
        let firstRender = true;

        function showError(message) {
            error.textContent = message || 'Không thể thực hiện thao tác.';
            error.hidden = false;
        }
        function timeOf(value) {
            if (!value) return '';
            const date = new Date(value);
            return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('vi-VN', {
                hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit'
            });
        }
        const historyCache = new Map();
        function render(messages) {
            for (const m of messages) historyCache.set(Number(m.id), m);
            messages = [...historyCache.values()].sort((a,b) => Number(a.id)-Number(b.id));
            const nearBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 90;
            const previousScroll = list.scrollTop;
            list.replaceChildren();
            if (messages.length === 0) {
                const p = document.createElement('p');
                p.className = 'text-secondary text-center';
                p.textContent = 'Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện.';
                list.appendChild(p);
            }
            for (const m of messages) {
                const mine = Number(m.senderId) === currentUserId;
                const row = document.createElement('div');
                row.className = 'd-flex ' + (mine ? 'justify-content-end' : 'justify-content-start');
                const bubble = document.createElement('div');
                bubble.className = 'rounded-3 px-3 py-2';
                bubble.style.maxWidth = '82%';
                bubble.style.wordBreak = 'break-word';
                bubble.style.whiteSpace = 'pre-wrap';
                bubble.style.background = mine ? 'var(--member-chat-outgoing, #2563eb)' : 'var(--member-chat-incoming, #e2e8f0)';
                bubble.style.color = mine ? 'var(--member-ink, #fff)' : 'var(--member-ink, #0f172a)';
                const text = document.createElement('div');
                text.textContent = m.content || '';
                const time = document.createElement('small');
                time.className = 'd-block mt-1';
                time.style.opacity = '.75';
                time.textContent = timeOf(m.sentAt);
                bubble.append(text, time);
                row.appendChild(bubble);
                list.appendChild(row);
            }
            if (firstRender || nearBottom) list.scrollTop = list.scrollHeight;
            else list.scrollTop = previousScroll;
            firstRender = false;
        }
        const olderButton = document.createElement('button');
        olderButton.type = 'button'; olderButton.className = 'btn btn-outline-secondary btn-sm';
        olderButton.textContent = 'Tải tin nhắn cũ'; list.before(olderButton);
        olderButton.addEventListener('click', async () => {
            if (!historyCache.size) return;
            olderButton.disabled = true;
            try {
                const before = Math.min(...historyCache.keys());
                const response = await fetch((chat.dataset.messagesUrl).replace(/\/messages$/, '/older') + '?beforeId=' + before, {credentials:'same-origin'});
                if (!response.ok) throw new Error('Không tải được tin cũ.');
                const data = await response.json(); render(data);
                olderButton.hidden = data.length < 50;
            } catch (e) { showError(e.message); } finally { olderButton.disabled = false; }
        });
        async function refresh() {
            if (busy) return;
            busy = true;
            try {
                const response = await fetch(chat.dataset.messagesUrl, {
                    credentials: 'same-origin', cache: 'no-store'
                });
                if (!response.ok) throw new Error('Không thể tải tin nhắn (' + response.status + ').');
                const messages = await response.json();
                render(messages);
                error.hidden = true;
            } catch (e) {
                showError(e.message);
            } finally {
                busy = false;
            }
        }
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            if (!input.value.trim()) return;
            sendButton.disabled = true;
            try {
                const response = await fetch(chat.dataset.sendUrl, {
                    method: 'POST', credentials: 'same-origin',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8', 'Accept': 'application/json'},
                    body: new URLSearchParams(new FormData(form))
                });
                if (!response.ok) {
                    const failure = await response.json().catch(() => ({}));
                    window.UTEFitValidation?.applyErrors(form, failure.fieldErrors || {content: failure.message || failure.error || 'Không gửi được tin nhắn.'});
                    throw new Error(failure.message || failure.error || 'Không gửi được tin nhắn (' + response.status + ').');
                }
                input.value = '';
                await refresh();
            } catch (e) {
                showError(e.message);
            } finally {
                sendButton.disabled = false;
                input.focus();
            }
        });
        subscribe(chat.dataset.wsUrl, '/user/queue/chat', (event) => {
            try {
                const m = JSON.parse(event.body);
                if (Number(m.conversationId) === conversationId) refresh();
            } catch (_) { /* Lần poll sau vẫn cập nhật. */ }
        }, status);
        refresh();
        setInterval(() => { if (!document.hidden) refresh(); }, 3000);
    }

    const notifications = document.getElementById('memberNotifications');
    if (notifications) {
        const banner = document.getElementById('notificationLive');
        const firstId = Number(notifications.dataset.latestId || 0);
        let reloading = false;
        const reload = () => {
            if (reloading) return;
            reloading = true;
            if (banner) banner.hidden = false;
            window.setTimeout(() => window.location.reload(), 500);
        };
        subscribe(notifications.dataset.wsUrl, '/user/queue/notifications', reload, null);
        // Kết nối realtime mất vẫn phát hiện notification mới từ DB.
        setInterval(async () => {
            if (document.hidden || reloading) return;
            try {
                const r = await fetch(notifications.dataset.recentUrl, {
                    credentials: 'same-origin', cache: 'no-store'
                });
                if (!r.ok) return;
                const items = await r.json();
                if (items.length && Number(items[0].id) !== firstId) reload();
            } catch (_) { /* Giữ nguyên trang khi offline */ }
        }, 20000);
    }
})();
