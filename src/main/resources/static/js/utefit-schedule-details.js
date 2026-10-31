(() => {
    'use strict';
    const dialogs = document.querySelectorAll('.ut-session-dialog');
    if (!dialogs.length) return;
    const labels = new Map([
        ['SCHEDULED', 'Đã lên lịch'], ['CANCELLED', 'Đã hủy'],
        ['COMPLETED', 'Đã hoàn thành'], ['REGISTERED', 'Đã đăng ký'],
        ['ATTENDED', 'Đã tham gia'], ['PENDING', 'Chờ xác nhận'],
        ['CONFIRMED', 'Đã xác nhận'], ['ABSENT', 'Vắng mặt']
    ]);
    let returnFocus;
    dialogs.forEach(dialog => {
        dialog.querySelectorAll('[data-session-status]').forEach(status => {
            status.textContent = labels.get(status.textContent.trim()) || status.textContent;
        });
        dialog.querySelectorAll('[data-session-close]').forEach(button => {
            button.addEventListener('click', () => dialog.close());
        });
        dialog.addEventListener('keydown', event => {
            if (event.key !== 'Tab') return;
            const buttons = [...dialog.querySelectorAll('[data-session-close]')];
            const first = buttons[0];
            const last = buttons[buttons.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        });
        dialog.addEventListener('close', () => {
            document.body.classList.remove('ut-session-viewing');
            if (returnFocus?.isConnected) returnFocus.focus({ preventScroll: true });
        });
    });
    document.addEventListener('click', event => {
        const dialog = document.querySelector('.ut-session-dialog[open]');
        if (!dialog || dialog.contains(event.target) && event.target !== dialog) return;
        const bounds = dialog.getBoundingClientRect();
        if (event.clientX < bounds.left || event.clientX > bounds.right
            || event.clientY < bounds.top || event.clientY > bounds.bottom) dialog.close();
    }, true);
    document.querySelectorAll('[data-session-open]').forEach(button => {
        button.addEventListener('click', () => {
            const dialog = document.getElementById(button.getAttribute('aria-controls'));
            if (!dialog || dialog.open) return;
            returnFocus = button;
            dialog.showModal();
            document.body.classList.add('ut-session-viewing');
        });
    });
})();
