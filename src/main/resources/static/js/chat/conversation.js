(function () {
  const wsEndpoint = document.body.dataset.wsEndpoint || '/ws';
  const conversationIdRaw = document.body.dataset.conversationId;

  const conversationId = conversationIdRaw ? Number(conversationIdRaw) : null;

  const container = document.getElementById('chatMessages');
  const input = document.getElementById('messageInput');
  const btn = document.getElementById('btnSend');
  const statusEl = document.getElementById('chatStatus');

  if (!conversationId) {
    console.error('[CHAT] Missing conversationId');
    if (statusEl) statusEl.textContent = 'Không tìm thấy conversationId.';
    return;
  }
  if (!container || !input || !btn) {
    console.error('[CHAT] Missing DOM elements');
    return;
  }
  if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
    console.error('[CHAT] SockJS/Stomp not loaded');
    if (statusEl) statusEl.textContent = 'Thiếu thư viện SockJS/STOMP.';
    return;
  }

  let stompClient = null;
  let connected = false;
  let subscription = null;

  // nếu user bấm gửi trước khi connect kịp -> queue
  const pendingQueue = [];

  let attempt = 0;
  const MAX_ATTEMPT = 20;

  function setStatus(text) {
    if (statusEl) statusEl.textContent = text || '';
  }

  function scrollBottom() {
    container.scrollTop = container.scrollHeight;
  }

  // format thời gian hiện có (server in raw)
  function formatExistingTimes() {
    const nodes = container.querySelectorAll('.bubble-time[data-sent-at]');
    nodes.forEach(n => {
      const raw = n.getAttribute('data-sent-at');
      const t = formatTime(raw);
      if (t) n.textContent = t;
    });
  }

  function formatTime(raw) {
    if (!raw) return '';
    try {
      // raw có thể là ISO string hoặc epoch millis
      let d;
      if (/^\d+$/.test(raw)) d = new Date(Number(raw));
      else d = new Date(raw);

      if (isNaN(d.getTime())) return '';
      return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return '';
    }
  }

  function appendMessage(msg) {
    // nếu backend có gửi conversationId thì lọc đúng cuộc hội thoại
    if (msg && msg.conversationId != null) {
      if (String(msg.conversationId) !== String(conversationId)) return;
    }

    const mine = !!msg.mine;

    const row = document.createElement('div');
    row.className = 'bubble-row ' + (mine ? 'me' : 'other');

    const bubble = document.createElement('div');
    bubble.className = 'bubble ' + (mine ? 'me' : 'other');

    const contentDiv = document.createElement('div');
    contentDiv.textContent = msg.content || '';
    bubble.appendChild(contentDiv);

    const timeDiv = document.createElement('div');
    timeDiv.className = 'bubble-time';
    const timeText = formatTime(msg.sentAt);
    timeDiv.textContent = timeText || '';
    bubble.appendChild(timeDiv);

    row.appendChild(bubble);
    container.appendChild(row);
    scrollBottom();
  }

  function setUiConnected(on) {
    connected = on;
    // không disable input để user vẫn gõ; nếu chưa connect sẽ queue
    btn.disabled = false;
    input.disabled = false;
  }

  function subscribe() {
    if (!stompClient || !stompClient.connected) return;

    if (subscription) {
      try { subscription.unsubscribe(); } catch (e) {}
      subscription = null;
    }

    subscription = stompClient.subscribe('/user/queue/chat', function (message) {
      const msg = JSON.parse(message.body);
      appendMessage(msg);
    });
  }

  function flushQueue() {
    while (pendingQueue.length > 0 && stompClient && stompClient.connected) {
      const content = pendingQueue.shift();
      stompClient.send('/app/chat.send', {}, JSON.stringify({
        conversationId: conversationId,
        content: content
      }));
    }
  }

  function connect() {
    // tránh connect chồng
    if (stompClient && stompClient.connected) return;

    attempt++;
    setStatus('Đang kết nối chat... (' + attempt + ')');

    const socket = new SockJS(wsEndpoint);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
      attempt = 0;
      setUiConnected(true);
      setStatus('Đã kết nối.');
      subscribe();
      flushQueue();
    }, function (err) {
      console.error('[CHAT] connect error:', err);
      setUiConnected(false);
      setStatus('Mất kết nối. Đang thử kết nối lại...');

      if (attempt < MAX_ATTEMPT) {
        const delay = Math.min(2000, 400 + attempt * 150);
        setTimeout(connect, delay);
      } else {
        setStatus('Không thể kết nối chat. Vui lòng tải lại trang.');
      }
    });
  }

  function sendMessage() {
    const content = (input.value || '').trim();
    if (!content) return;

    input.value = '';

    // chưa connected => queue + connect lại (khỏi cần reload)
    if (!stompClient || !stompClient.connected || !connected) {
      pendingQueue.push(content);
      setStatus('Đang kết nối... tin nhắn sẽ được gửi khi kết nối xong.');
      connect();
      return;
    }

    stompClient.send('/app/chat.send', {}, JSON.stringify({
      conversationId: conversationId,
      content: content
    }));
  }

  function init() {
    // event
    btn.addEventListener('click', sendMessage);
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });

    formatExistingTimes();
    scrollBottom();
    connect();
  }

  // chạy chắc chắn sau khi load xong
  if (document.readyState === 'complete') init();
  else window.addEventListener('load', init);

  // back/forward cache: quay lại trang mà websocket chết => tự connect lại
  window.addEventListener('pageshow', function (e) {
    if (e.persisted) {
      attempt = 0;
      connect();
    }
  });

  window.addEventListener('beforeunload', function () {
    try {
      if (stompClient) stompClient.disconnect();
    } catch (e) {}
  });
})();