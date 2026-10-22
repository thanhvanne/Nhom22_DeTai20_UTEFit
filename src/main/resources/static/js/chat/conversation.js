/* Chat PT / legacy Member: gửi bằng HTTP POST + CSRF; nhận STOMP và polling dự phòng.
   Không dùng innerHTML để tránh XSS từ nội dung tin nhắn. */
(() => {
  'use strict';

  const root = document.getElementById('ptChat');
  if (!root) return;

  const conversationId = Number(root.dataset.conversationId);
  const messagesUrl = root.dataset.messagesUrl;
  const sendUrl = root.dataset.sendUrl;
  const wsUrl = root.dataset.wsUrl || '/ws';
  const list = document.getElementById('chatMessages');
  const form = document.getElementById('ptChatForm');
  const input = document.getElementById('ptChatInput');
  const button = document.getElementById('ptChatSend');
  const status = document.getElementById('chatConnection');
  const error = document.getElementById('chatError');

  if (!conversationId || !messagesUrl || !sendUrl || !list || !form || !input) {
    console.error('[UTEFIT-CHAT] Thiếu cấu hình hội thoại/REST.');
    if (status) status.textContent = 'Không thể tải cuộc trò chuyện';
    return;
  }

  let fetching = false;
  let sending = false;
  let firstRender = true;
  let lastSnapshot = '';
  let socket = null;
  let stomp = null;
  let online = false;
  let retryTimer = null;

  function showError(message) {
    if (!error) return;
    error.textContent = message || '';
    error.hidden = !message;
  }

  function showConnection(text) {
    if (status) status.textContent = text;
  }

  function formatTime(value) {
    if (!value) return '';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value).slice(0, 16).replace('T', ' ')
        : date.toLocaleString('vi-VN', {
          day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
        });
  }

  function render(items) {
    const snapshot = JSON.stringify(items.map(m => [m.id, m.content, m.readAt, m.mine]));
    if (snapshot === lastSnapshot) return;
    lastSnapshot = snapshot;

    const nearBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 100;
    const previousTop = list.scrollTop;
    const fragment = document.createDocumentFragment();

    if (items.length === 0) {
      const p = document.createElement('p');
      p.className = 'text-muted text-center';
      p.textContent = 'Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện.';
      fragment.appendChild(p);
    }

    for (const msg of items) {
      const row = document.createElement('div');
      row.className = 'd-flex ' + (msg.mine ? 'justify-content-end' : 'justify-content-start');

      const bubble = document.createElement('div');
      bubble.className = 'p-2 rounded-3 ' + (msg.mine ? 'bg-primary text-white' : 'bg-light text-dark');
      bubble.style.maxWidth = '78%';
      bubble.style.whiteSpace = 'pre-wrap';
      bubble.style.overflowWrap = 'anywhere';

      const content = document.createElement('div');
      content.textContent = msg.content || '';
      const time = document.createElement('small');
      time.className = 'd-block opacity-75 mt-1 text-end';
      time.textContent = formatTime(msg.sentAt);
      bubble.append(content, time);
      row.appendChild(bubble);
      fragment.appendChild(row);
    }

    list.replaceChildren(fragment);
    if (firstRender || nearBottom) list.scrollTop = list.scrollHeight;
    else list.scrollTop = previousTop;
    firstRender = false;
  }

  async function refresh() {
    if (fetching || document.hidden) return;
    fetching = true;
    try {
      const response = await fetch(messagesUrl, {
        credentials: 'same-origin', cache: 'no-store'
      });
      if (!response.ok) {
        throw new Error('Không tải được tin nhắn (HTTP ' + response.status + ').');
      }
      const items = await response.json();
      if (!Array.isArray(items)) throw new Error('Dữ liệu chat không hợp lệ.');
      render(items);
      showError('');
    } catch (e) {
      showError(e.message);
    } finally {
      fetching = false;
    }
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (sending) return;
    const content = input.value.trim();
    if (!content) return;
    sending = true;
    if (button) button.disabled = true;
    showError('');
    try {
      const data = new URLSearchParams(new FormData(form));
      data.set('content', content);
      const response = await fetch(sendUrl, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
        body: data
      });
      if (!response.ok) {
        let detail = '';
        try {
          const body = await response.json();
          detail = body.detail || body.message || '';
        } catch (_) { /* API không trả JSON */ }
        throw new Error('Gửi thất bại (HTTP ' + response.status + '). ' + detail);
      }
      input.value = '';
      await refresh();
    } catch (e) {
      // Giữ nguyên nội dung input để người dùng gửi lại, không làm mất tin.
      showError(e.message);
    } finally {
      sending = false;
      if (button) button.disabled = false;
      input.focus();
    }
  });

  function scheduleReconnect() {
    if (retryTimer != null) return;
    retryTimer = window.setTimeout(() => {
      retryTimer = null;
      if (!document.hidden) connectRealtime();
    }, 6000);
  }

  function connectRealtime() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
      showConnection('Tự cập nhật mỗi 3 giây');
      return;
    }
    if (online) return;
    try {
      socket = new SockJS(wsUrl);
      stomp = Stomp.over(socket);
      stomp.debug = null;
      stomp.connect({}, () => {
        online = true;
        showConnection('Đang kết nối trực tiếp');
        stomp.subscribe('/user/queue/chat', frame => {
          try {
            const msg = JSON.parse(frame.body);
            if (Number(msg.conversationId) === conversationId) refresh();
          } catch (e) {
            console.warn('[UTEFIT-CHAT] STOMP payload không hợp lệ', e);
          }
        });
        refresh();
      }, err => {
        console.warn('[UTEFIT-CHAT] WebSocket chưa kết nối, dùng polling:', err);
        online = false;
        showConnection('Tự cập nhật mỗi 3 giây');
        scheduleReconnect();
      });
    } catch (e) {
      console.warn('[UTEFIT-CHAT] Không thể khởi tạo WebSocket:', e);
      online = false;
      showConnection('Tự cập nhật mỗi 3 giây');
      scheduleReconnect();
    }
  }

  showConnection('Đang tải tin nhắn...');
  refresh();
  connectRealtime();
  const pollTimer = window.setInterval(() => {
    if (!document.hidden) refresh();
  }, 3000);
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) refresh();
  });
  window.addEventListener('beforeunload', () => {
    window.clearInterval(pollTimer);
    if (retryTimer != null) window.clearTimeout(retryTimer);
    try { if (stomp && stomp.connected) stomp.disconnect(); } catch (_) {}
    try { if (socket) socket.close(); } catch (_) {}
  });
})();