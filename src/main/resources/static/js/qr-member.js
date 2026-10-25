(() => {
    const panel = document.getElementById('member-qr');
    if (!panel) return;
    const attendance = document.getElementById('member-checkin-status');
    const detail = document.getElementById('member-checkin-detail');
    const message = document.getElementById('member-qr-status');
    const sync = document.getElementById('member-qr-sync');
    const display = document.getElementById('member-qr-display');
    const create = document.getElementById('member-qr-create');
    const form = document.getElementById('member-qr-form');
    const retry = document.getElementById('member-qr-retry');
    const checkoutCreate = document.getElementById('member-qr-checkout-create');
    const checkoutForm = document.getElementById('member-qr-checkout-form');
    const url = new URL(panel.dataset.statusUrl, window.location.href);
    if (panel.dataset.tokenId) url.searchParams.set('tokenId', panel.dataset.tokenId);
    let pollTimer = null;
    let countdownTimer = null;
    let timeoutTimer = null;
    let controller = null;
    let paused = false;
    let generation = 0;
    let retired = false;
    let expiresAt = null;
    let serverTime = 0;
    let receivedAt = 0;
    let lastCanIssue = create.dataset?.canIssue === 'true';
    let lastCanCheckout = checkoutCreate?.dataset?.canCheckout === 'true';
    let qrPurpose = 'CHECK_IN';

    const stopCountdown = () => {
        clearInterval(countdownTimer);
        countdownTimer = null;
    };
    const hideQr = () => {
        if (display) display.hidden = true;
    };
    const retireQr = () => {
        retired = true;
        stopCountdown();
        hideQr();
        const input = display?.querySelector('input');
        if (input) input.value = '';
        display?.querySelector('img')?.removeAttribute('src');
    };
    const countdown = () => {
        const remaining = Math.max(0, Math.ceil((expiresAt - serverTime - (performance.now() - receivedAt)) / 1000));
        if (!remaining) {
            retireQr();
            message.textContent = qrPurpose === 'CHECK_OUT' ? 'QR Check-out hết hạn. Tạo mã mới nếu bạn vẫn đang tập.' : 'QR hết hạn. Hãy tạo mã mới nếu bạn chưa check-in.';
            return;
        }
        message.textContent = (qrPurpose === 'CHECK_OUT' ? 'QR Check-out' : 'QR') + ' còn hiệu lực ' + remaining + ' giây.';
    };
    const formatTime = value => typeof value === 'string' ? value.replace('T', ' ') : '';
    const render = state => {
        stopCountdown();
        qrPurpose = state.qrPurpose ?? 'CHECK_IN';
        attendance.className = 'alert ' + (state.attendanceState === 'OPEN' ? 'alert-success' : 'alert-info');
        if (state.attendanceState === 'OPEN') {
            attendance.textContent = 'Check-in thành công — Đang tập. Tạo QR Check-out khi ra về.';
        } else if (state.attendanceState === 'CHECKED_OUT') {
            attendance.textContent = 'Check-out thành công — Đã rời phòng tập (Đã checkout). Bạn có thể tạo QR Check-in cho lượt tiếp theo.';
        } else {
            attendance.textContent = 'Chưa có lượt check-in. Tạo QR để check-in tại quầy.';
        }
        detail.textContent = state.checkinId == null ? '' :
            'Chi nhánh: ' + state.branchName + ' · Vào: ' + formatTime(state.checkInAt) +
            (state.checkOutAt ? ' · Ra: ' + formatTime(state.checkOutAt) : '');
        lastCanIssue = state.canIssue && state.attendanceState !== 'OPEN';
        lastCanCheckout = state.canCheckout === true && state.attendanceState === 'OPEN';
        create.disabled = !lastCanIssue;
        if (checkoutCreate) checkoutCreate.disabled = !lastCanCheckout;
        if (checkoutForm) checkoutForm.hidden = !lastCanCheckout;
        if (state.attendanceState === 'OPEN' && qrPurpose !== 'CHECK_OUT') {
            retireQr();
            message.textContent = 'QR Check-in không dùng để ra về. Bạn đang tập; hãy tạo QR Check-out riêng.';
        } else if (state.qrState === 'UNAVAILABLE') {
            retireQr();
            message.textContent = 'QR đã được sử dụng hoặc thu hồi. Không thể dùng lại; tạo mã mới cho lượt tiếp theo.';
        } else if (state.qrState === 'EXPIRED') {
            retireQr();
            message.textContent = qrPurpose === 'CHECK_OUT' ? 'QR Check-out hết hạn. Tạo mã mới nếu bạn vẫn đang tập.' : 'QR hết hạn. Hãy tạo mã mới nếu bạn chưa check-in.';
        } else if (state.qrState === 'VALID' && display && !retired &&
            (qrPurpose !== 'CHECK_OUT' || state.attendanceState === 'OPEN')) {
            expiresAt = state.expiresAt;
            serverTime = state.serverTime;
            receivedAt = performance.now();
            display.hidden = false;
            countdown();
            if (!retired) countdownTimer = setInterval(countdown, 1000);
        } else {
            hideQr();
            message.textContent = retired ? 'QR cũ không còn hiển thị. Tạo mã mới cho lượt tiếp theo.' : 'Tạo QR khi sẵn sàng check-in tại quầy.';
        }
        sync.textContent = 'Trạng thái được cập nhật tự động mỗi 4 giây.';
    };

    const poll = async () => {
        if (paused || document.hidden) return;
        if (retry) retry.disabled = true;
        const requestGeneration = generation;
        const requestController = new AbortController();
        controller = requestController;
        timeoutTimer = setTimeout(() => requestController.abort(), 8000);
        try {
            const response = await fetch(url.href, {
                credentials: 'same-origin', cache: 'no-store', signal: requestController.signal,
                headers: {Accept: 'application/json'}
            });
            if (requestGeneration !== generation) return;
            if (response.redirected || response.status === 401 || response.status === 403) {
                paused = true;
                lastCanIssue = false;
                lastCanCheckout = false;
                stopCountdown();
                hideQr();
                create.disabled = true;
                if (checkoutCreate) checkoutCreate.disabled = true;
                sync.textContent = 'Phiên đăng nhập không còn hợp lệ. Tải lại trang và đăng nhập Member để tiếp tục.';
                return;
            }
            if (response.status === 404) {
                retireQr();
                create.disabled = !lastCanIssue;
                if (checkoutCreate) checkoutCreate.disabled = !lastCanCheckout;
                paused = true;
                if (url.searchParams.has('tokenId')) {
                    url.searchParams.delete('tokenId');
                    sync.textContent = 'QR không còn khả dụng. Nhấn Thử lại để đọc trạng thái hiện tại của chính bạn.';
                } else {
                    sync.textContent = 'Máy chủ chưa cung cấp API trạng thái QR. Khởi động lại Spring Boot bản mới, tải lại trang rồi nhấn Thử lại.';
                }
                return;
            }
            if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Status unavailable');
            const state = await response.json();
            if (requestGeneration !== generation) return;
            if (!['OPEN', 'CHECKED_OUT', 'NONE'].includes(state.attendanceState) ||
                !['VALID', 'EXPIRED', 'UNAVAILABLE', 'NONE'].includes(state.qrState) ||
                typeof state.canIssue !== 'boolean' ||
                (state.qrPurpose != null && !['CHECK_IN', 'CHECK_OUT', 'NONE'].includes(state.qrPurpose)) ||
                (state.canCheckout != null && typeof state.canCheckout !== 'boolean') ||
                (state.qrState === 'VALID' && (!Number.isFinite(state.expiresAt) || !Number.isFinite(state.serverTime)))) {
                throw new Error('Invalid status response');
            }
            render(state);
        } catch (error) {
            if (requestGeneration !== generation) return;
            stopCountdown();
            hideQr();
            create.disabled = !lastCanIssue;
            if (checkoutCreate) checkoutCreate.disabled = !lastCanCheckout;
            sync.textContent = 'Chưa đồng bộ được trạng thái. QR tạm ẩn; nhấn Thử lại hoặc chờ cập nhật tự động. Nút tạo QR dựa trạng thái gần nhất; backend vẫn kiểm tra lại trước khi tạo.';
        } finally {
            if (requestGeneration === generation) {
                clearTimeout(timeoutTimer);
                controller = null;
                if (retry) retry.disabled = false;
                if (!paused && !document.hidden) pollTimer = setTimeout(poll, 4000);
            }
        }
    };
    const pause = () => {
        paused = true;
        generation += 1;
        clearTimeout(pollTimer);
        clearTimeout(timeoutTimer);
        controller?.abort();
        controller = null;
        stopCountdown();
        hideQr();
        create.disabled = true;
        if (checkoutCreate) checkoutCreate.disabled = true;
    };
    const resume = () => {
        pause();
        paused = false;
        create.disabled = !lastCanIssue;
        if (checkoutCreate) checkoutCreate.disabled = !lastCanCheckout;
        sync.textContent = 'Đang đồng bộ trạng thái...';
        poll();
    };
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) pause();
        else resume();
    });
    window.addEventListener('pagehide', pause);
    window.addEventListener('pageshow', () => {
        if (paused && !document.hidden) resume();
    });
    form.addEventListener('submit', () => {
        pause();
        create.disabled = true;
    });
    checkoutForm?.addEventListener('submit', pause);
    retry?.addEventListener('click', resume);
    resume();
})();
