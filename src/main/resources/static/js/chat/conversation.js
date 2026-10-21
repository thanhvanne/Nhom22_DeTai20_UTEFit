(function () {
  const root = document.getElementById('ptChat');
  if (!root) return;

  const wsEndpoint = root.dataset.wsUrl || '/ws';
  const backUrl = root.dataset.backUrl || '/pt/chats';
  const conversationIdStr = root.dataset.conversationId;
  const conversationId = conversationIdStr ? Number(conversationIdStr) : null;

  const messagesEl = document.getElementById('chatMessages');
  const form = document.getElementById('ptChatForm');
  const input = document.getElementById('ptChatInput');
  const btn = document.getElementById('ptChatSend');
  const statusEl = document.getElementById('chatConnection');
  const errEl = document.getElementById('chatError');

  if (!conversationId) {
    console.error('[PT-CHAT] Missing conversationId');
    if (statusEl) statusEl.textContent = 'Thiếu conversationId.';
    return;
  }

  let sock = null;
  let stomp = null;
  let sub = null;
  let connected = false;
  let attempt = 0;
  const MAX_ATTEMPT = 30;

  const queue = []; // tin nhắn chờ khi chưa connect

  function setStatus(text) { if (statusEl) statusEl.textContent = text || ''; }
  function setError(text) {
    if (!errEl) return;
    if (text) { errEl.hidden = false; errEl.textContent = text; }
    else { errEl.hidden = true; errEl.textContent = ''; }
  }
  function scrollBottom() { if (messagesEl) messagesEl.scrollTop = messagesEl.scrollHeight; }
  function fmtTime(raw) {
    if (!raw) return '';
    try {
      const d = /^\d+$/.test(String(raw)) ? new Date(Number(raw)) : new Date(String(raw));
      return isNaN(d.getTime()) ? '' : d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } catch { return ''; }
  }
  function formatExistingTimes() {
    messagesEl.querySelectorAll('[data-sent-at]').forEach(n => {
      const t = fmtTime(n.getAttribute('data-sent-at'));
      if (t) n.textContent = t;
    });
  }

  function appendMessage(msg) {
    if (msg && msg.conversationId != null && String(msg.conversationId) !== String(conversationId)) return;

    const mine = !!msg.mine;
    const row = document.createElement('div');
    row.className = mine ? 'd-flex justify-content-end' : 'd-flex justify-content-start';

    const bubble = document.createElement('div');
    bubble.className = mine ? 'p-2 rounded-3 bg-primary text-white' : 'p-2 rounded-3 bg-light text-dark';
    bubble.style.maxWidth = '70%';

    const content = document.createElement('div');
    content.textContent = msg.content || '';
    bubble.appendChild(content);

    const time = document.createElement('div');
    time.className = 'small opacity-75 mt-1 text-end';
    time.textContent = fmtTime(msg.sentAt) || '';
    bubble.appendChild(time);

    row.appendChild(bubble);
    messagesEl.appendChild(row);
    scrollBottom();
  }

  function unsub() { try { if (sub) sub.unsubscribe(); } catch {} sub = null; }
  function disconnect() {
    unsub();
    try { if (stomp) stomp.disconnect(); } catch {}
    stomp = null;
    try { if (sock) sock.close(); } catch {}
    sock = null;
    connected = false;
  }

  function subscribe() {
    unsub();
    sub = stomp.subscribe('/user/queue/chat', function (frame) {
      try { appendMessage(JSON.parse(frame.body)); }
      catch (e) { console.error('[PT-CHAT] parse message error', e); }
    });
  }

  function flushQueue() {
    while (queue.length > 0 && stomp && stomp.connected) {
      const content = queue.shift();
      stomp.send('/app/chat.send', {}, JSON.stringify({ conversationId, content }));
    }
  }

  function scheduleReconnect() {
    connected = false; attempt++;
    if (attempt > MAX_ATTEMPT) { setStatus('Không thể kết nối chat. Vui lòng tải lại trang.'); return; }
    const delay = Math.min(2500, 400 + attempt * 120);
    setStatus('Mất kết nối. Đang thử kết nối lại... (' + attempt + ')');
    setTimeout(connect, delay);
  }

  function connect() {
    if (stomp && stomp.connected) return;

    disconnect();
    setError('');
    setStatus(attempt === 0 ? 'Đang kết nối…' : 'Đang kết nối lại… (' + attempt + ')');

    sock = new SockJS(wsEndpoint);
    sock.onclose = function () {
      if (connected) console.warn('[PT-CHAT] socket closed');
      scheduleReconnect();
    };

    stomp = Stomp.over(sock);
    stomp.debug = null;
    stomp.heartbeat.outgoing = 10000;
    stomp.heartbeat.incoming = 10000;

    stomp.connect({}, function () {
      attempt = 0;
      connected = true;
      setStatus('Đang kết nối trực tiếp');
      subscribe();
      flushQueue();
      scrollBottom();
    }, function (err) {
      console.error('[PT-CHAT] connect error', err);
      scheduleReconnect();
    });
  }

  function sendMessage() {
    const val = (input.value || '').trim();
    if (!val) return;
    input.value = '';

    if (!stomp || !stomp.connected || !connected) {
      queue.push(val);
      setStatus('Đang kết nối… tin nhắn sẽ gửi khi kết nối xong.');
      connect();
      return;
    }

    try {
      stomp.send('/app/chat.send', {}, JSON.stringify({ conversationId, content: val }));
    } catch (e) {
      console.error('[PT-CHAT] send error', e);
      queue.push(val);
      scheduleReconnect();
    }
  }

  function bindEvents() {
    // chặn submit tuyệt đối
    form.addEventListener('submit', function (e) { e.preventDefault(); e.stopPropagation(); sendMessage(); return false; });
    btn.addEventListener('click', function (e) { e.preventDefault(); sendMessage(); });
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); e.stopPropagation(); sendMessage(); }
    });

    // Nút/back + phím Esc
    const backBtn = document.getElementById('ptChatBackBtn');
    if (backBtn) backBtn.addEventListener('click', () => disconnect());
    window.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { disconnect(); window.location.href = backUrl; }
    });

    // BFCache
    window.addEventListener('pageshow', function (e) { if (e.persisted) { attempt = 0; connect(); } });
    window.addEventListener('beforeunload', function () { disconnect(); });
  }

  function init() {
    bindEvents();
    formatExistingTimes();
    scrollBottom();
    connect();
  }

  if (document.readyState === 'complete') init();
  else window.addEventListener('load', init);
})();