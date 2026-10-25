(() => {
    const checkout = document.getElementById('sepay-checkout');
    if (!checkout) return;
    const message = document.getElementById('sepay-message');
    const paymentStatus = document.getElementById('sepay-payment-status');
    const membershipStatus = document.getElementById('sepay-membership-status');
    const refresh = document.getElementById('sepay-refresh');
    let timer;
    let inFlight = false;
    let failures = 0;
    const stop = () => { clearTimeout(timer); timer = undefined; };
    const hideQr = () => { const qr = document.getElementById('sepay-qr-image'); if (qr) qr.hidden = true; };
    const expiry = () => {
        if (paymentStatus.textContent.trim() === 'PAID') {
            document.getElementById('sepay-expiry').textContent = 'Đã được backend xác nhận; không chuyển thêm tiền.';
            return;
        }
        const seconds = Math.max(0, Math.ceil((Number(checkout.dataset.expiresAt) - Date.now()) / 1000));
        document.getElementById('sepay-expiry').textContent = seconds > 0 ? `Còn ${seconds} giây để thanh toán.` : 'Đã hết thời gian thanh toán. Không chuyển thêm; cần Admin đối soát nếu đã chuyển.';
        if (!seconds) hideQr();
    };
    const poll = async () => {
        stop();
        if (inFlight || document.hidden) return;
        inFlight = true;
        refresh.disabled = true;
        try {
            const response = await fetch(checkout.dataset.statusUrl, {credentials: 'same-origin', cache: 'no-store', headers: {'Accept': 'application/json'}});
            if (!response.ok || response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('session');
            const state = await response.json();
            paymentStatus.textContent = state.paymentStatus;
            membershipStatus.textContent = state.membershipStatus;
            failures = 0;
            if (state.paymentStatus === 'PAID') {
                hideQr();
                message.textContent = state.membershipStatus === 'ACTIVE' ? 'Backend đã xác nhận thanh toán. Gói tập đã ACTIVE.' : 'Đã ghi nhận thanh toán; liên hệ Admin về trạng thái gói.';
            } else if (state.paymentStatus !== 'PENDING' || state.expired) {
                hideQr();
                message.textContent = 'Giao dịch không còn trong thời gian chờ hợp lệ. Không chuyển thêm; liên hệ Admin nếu đã chuyển tiền.';
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
            refresh.disabled = false;
            expiry();
        }
    };
    document.querySelectorAll('[data-copy]').forEach(button => button.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(document.getElementById(button.dataset.copy).textContent.trim());
            message.textContent = 'Đã sao chép. Giữ nguyên nội dung và số tiền.';
        } catch (error) {
            message.textContent = 'Không sao chép tự động được. Vui lòng chọn và sao chép nội dung hiển thị.';
        }
    }));
    refresh.addEventListener('click', poll);
    document.addEventListener('visibilitychange', () => document.hidden ? stop() : poll());
    const countdown = setInterval(expiry, 1000);
    window.addEventListener('pagehide', () => { stop(); clearInterval(countdown); });
    expiry();
    poll();
})();
