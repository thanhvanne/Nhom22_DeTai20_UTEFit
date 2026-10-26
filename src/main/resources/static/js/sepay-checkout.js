(() => {
    const checkout = document.getElementById('sepay-checkout');
    if (!checkout) return;
    const message = document.getElementById('sepay-message');
    const paymentStatus = document.getElementById('sepay-payment-status');
    const membershipStatus = document.getElementById('sepay-membership-status');
    const refresh = document.getElementById('sepay-refresh');
    const renderedState = checkout.dataset.displayState;
    const transfer = document.getElementById('sepay-transfer');
    let timer;
    let countdown;
    let navigating = false;
    let inFlight = false;
    let failures = 0;
    const stop = () => { clearTimeout(timer); timer = undefined; };
    const hideQr = () => { const qr = document.getElementById('sepay-qr-image'); if (qr) qr.hidden = true; };
    const expiry = () => {
        const label = document.getElementById('sepay-expiry');
        if (!label || navigating || renderedState !== 'PENDING') return;
        const seconds = Math.max(0, Math.ceil((Number(checkout.dataset.expiresAt) - Date.now()) / 1000));
        label.textContent = seconds > 0 ? `Còn ${seconds} giây để thanh toán.` : 'Đã hết thời gian thanh toán. Không chuyển thêm; cần Admin đối soát nếu đã chuyển.';
        if (!seconds) hideQr();
    };
    const poll = async () => {
        stop();
        if (inFlight || document.hidden || navigating) return;
        inFlight = true;
        refresh.disabled = true;
        try {
            const response = await fetch(checkout.dataset.statusUrl, {credentials: 'same-origin', cache: 'no-store', headers: {'Accept': 'application/json'}});
            if (!response.ok || response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('session');
            const state = await response.json();
            if (!['PENDING', 'PAID', 'REVIEW', 'FAILED', 'EXPIRED', 'REFUNDED', 'CANCEL_REQUESTED', 'CANCELLED'].includes(state.displayState)
                || !['PENDING', 'PAID', 'FAILED', 'REFUNDED', 'CANCEL_REQUESTED', 'CANCELLED'].includes(state.paymentStatus)
                || typeof state.membershipStatus !== 'string' || typeof state.periodState !== 'string') throw new Error('invalid-status');
            if (state.displayState !== renderedState || state.membershipStatus !== checkout.dataset.membershipStatus
                || state.periodState !== checkout.dataset.periodState) {
                navigating = true;
                if (transfer) transfer.hidden = true;
                hideQr();
                clearInterval(countdown);
                message.textContent = 'Trạng thái đã thay đổi. Đang tải thông tin mới từ hệ thống…';
                window.location.reload();
                return;
            }
            paymentStatus.textContent = {PENDING: 'Chờ thanh toán', PAID: 'Đã thanh toán', FAILED: 'Không thành công', REFUNDED: 'Đã hoàn tiền', CANCEL_REQUESTED: 'Đang yêu cầu hủy', CANCELLED: 'Đã hủy đơn chờ'}[state.paymentStatus];
            membershipStatus.textContent = state.membershipStatus;
            failures = 0;
            if (state.paymentStatus === 'PAID') {
                hideQr();
                message.textContent = 'Thanh toán đã được hệ thống xác nhận. Thông tin kỳ tập được hiển thị ở trên.';
                timer = setTimeout(poll, 15000);
            } else if (state.displayState === 'REVIEW') {
                hideQr();
                message.textContent = 'Đã ghi nhận giao dịch cần Admin đối soát. Không chuyển thêm tiền; liên hệ quầy với mã thanh toán.';
                timer = setTimeout(poll, 5000);
            } else if (state.paymentStatus === 'CANCEL_REQUESTED' || state.paymentStatus === 'CANCELLED') {
                hideQr();
                message.textContent = state.paymentStatus === 'CANCEL_REQUESTED'
                    ? 'Đang chờ Admin xử lý yêu cầu hủy. Không chuyển tiền vào mã đơn này.'
                    : 'Đơn đã hủy. Nếu ngân hàng đã ghi nhận tiền, cần Admin đối soát/hoàn tiền riêng.';
                timer = setTimeout(poll, 15000);
            } else if (state.paymentStatus !== 'PENDING' || state.expired) {
                hideQr();
                message.textContent = 'Giao dịch không còn trong thời gian chờ hợp lệ. Không chuyển thêm; liên hệ Admin nếu đã chuyển tiền.';
                if (state.paymentStatus === 'PENDING') timer = setTimeout(poll, 15000);
            } else {
                message.textContent = 'Đang chờ Webhook đã xác thực. Kiểm tra trạng thái không tự đánh dấu PAID.';
                timer = setTimeout(poll, 5000);
            }
        } catch (error) {
            failures += 1;
            message.textContent = 'Chưa đọc được trạng thái. Kiểm tra kết nối/đăng nhập và bấm kiểm tra lại; không chuyển khoản lần nữa.';
            if (failures < 3) timer = setTimeout(poll, 15000);
        } finally {
            inFlight = false;
            refresh.disabled = navigating;
            expiry();
        }
    };
    document.querySelectorAll('[data-copy]').forEach(button => button.addEventListener('click', async () => {
        if (navigating || transfer?.hidden || renderedState !== 'PENDING') return;
        try {
            await navigator.clipboard.writeText(document.getElementById(button.dataset.copy).textContent.trim());
            message.textContent = 'Đã sao chép. Giữ nguyên nội dung và số tiền.';
        } catch (error) {
            message.textContent = 'Không sao chép tự động được. Vui lòng chọn và sao chép nội dung hiển thị.';
        }
    }));
    refresh.addEventListener('click', poll);
    document.addEventListener('visibilitychange', () => document.hidden ? stop() : poll());
    if (renderedState === 'PENDING') countdown = setInterval(expiry, 1000);
    window.addEventListener('pagehide', () => { stop(); clearInterval(countdown); });
    window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
    expiry();
    poll();
})();
