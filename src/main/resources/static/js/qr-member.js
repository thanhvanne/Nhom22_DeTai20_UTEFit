(() => {
    const panel = document.getElementById('member-qr');
    if (!panel) return;
    const message = document.getElementById('member-qr-status');
    const update = () => {
        const remaining = Math.max(0, Math.ceil((Number(panel.dataset.expires) - Date.now()) / 1000));
        message.textContent = remaining > 0 ? 'QR còn hiệu lực ' + remaining + ' giây.' : 'QR đã hết hạn. Hãy tạo mã mới.';
        if (remaining === 0) {
            panel.querySelector('img').hidden = true;
            panel.querySelector('input').value = '';
        }
    };
    update();
    const timer = setInterval(update, 1000);
    window.addEventListener('pagehide', () => clearInterval(timer));
})();
