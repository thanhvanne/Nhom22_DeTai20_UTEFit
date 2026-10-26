(() => {
    'use strict';

    const labels = new Map([
        ['PENDING', 'Chờ xử lý'], ['PAID', 'Đã thanh toán'],
        ['ACTIVE', 'Đang hoạt động'], ['INACTIVE', 'Ngừng hoạt động'],
        ['EXPIRED', 'Đã hết hạn'], ['CANCELLED', 'Đã hủy'],
        ['CANCEL_REQUESTED', 'Chờ duyệt hủy'], ['FAILED', 'Không thành công'],
        ['REFUNDED', 'Đã hoàn tiền'], ['REFUND_PENDING', 'Chờ hoàn tiền'],
        ['REVIEW_REQUIRED', 'Cần đối soát'], ['ACCEPTED', 'Đã chấp nhận'],
        ['REJECTED', 'Đã từ chối'], ['CONFIRMED', 'Đã xác nhận'],
        ['COMPLETED', 'Đã hoàn thành'], ['OPEN', 'Đang mở'], ['FULL', 'Đã đủ chỗ'],
        ['CLOSED', 'Đã đóng'], ['SCHEDULED', 'Đã lên lịch'],
        ['APPROVED', 'Đã duyệt'], ['BLOCKED', 'Đã khóa'], ['LOCKED', 'Đã khóa'],
        ['REGISTERED', 'Đã đăng ký'], ['ATTENDED', 'Đã tham gia'],
        ['UNVERIFIED', 'Chưa xác minh'],
        ['CASH', 'Tiền mặt'], ['SEPAY', 'Chuyển khoản VietQR'], ['VNPAY', 'VNPay'],
        ['MEMBER', 'Hội viên'], ['ADMIN', 'Quản trị viên'], ['PT', 'Huấn luyện viên'],
        ['MANAGER', 'Quản lý chi nhánh'], ['ROLE_ADMIN', 'Quản trị viên'],
        ['SINGLE_BRANCH', 'Một chi nhánh'], ['MULTI_BRANCH', 'Nhiều chi nhánh'],
        ['ALL_BRANCHES', 'Tất cả chi nhánh'], ['ALL', 'Tất cả dịch vụ'],
        ['GYM', 'Phòng gym'], ['YOGA', 'Yoga'], ['Dashboard', 'Tổng quan'],
        ['Dashboard tổng quan', 'Tổng quan hệ thống'], ['Chưa checkout', 'Chưa ra về']
    ]);
    const money = new Intl.NumberFormat('vi-VN', {
        style: 'currency', currency: 'VND', maximumFractionDigits: 0
    });

    const renderLabels = () => {
        document.querySelectorAll('[data-uf-label]').forEach((element) => {
            if (element.matches('input, select, textarea') || element.children.length > 0
                || (element.matches('option') && !element.hasAttribute('value'))) {
                return;
            }
            const text = element.textContent.trim();
            const translated = labels.get(text) || text
                .replace(/\bDashboard\b/g, 'Tổng quan')
                .replace(/\bManager\b/g, 'Quản lý')
                .replace(/\bAdmin\b/g, 'quản trị viên')
                .replace(/\bPayment\b/g, 'Giao dịch')
                .replace(/\bCASH\b/g, 'tiền mặt')
                .replace(/\bMember\b/g, 'hội viên')
                .replace(/\bMembership\b/g, 'gói tập')
                .replace(/Access Denied/g, 'Không có quyền truy cập')
                .replace(/Fitness Center/g, 'Trung tâm thể hình');
            if (translated !== text) {
                element.textContent = translated;
            }
        });
        document.querySelectorAll('[data-uf-money]').forEach((element) => {
            if (element.matches('input, select, textarea, option') || element.children.length > 0) {
                return;
            }
            const text = element.textContent.trim();
            const numeric = text.match(/^([+-]?\d+(?:\.\d{1,2})?)\s*(?:VND|đ|₫)?$/i);
            if (numeric) {
                const amount = Number(numeric[1]);
                if (Number.isFinite(amount)) {
                    element.textContent = money.format(amount);
                }
            }
        });
        document.querySelectorAll('[data-uf-date]').forEach((element) => {
            if (element.matches('input, select, textarea, option') || element.children.length > 0) {
                return;
            }
            element.textContent = element.textContent.replace(/\b(\d{4})-(\d{2})-(\d{2})(?=T|\b)/g, '$3/$2/$1')
                .replace(/(\d{2}\/\d{2}\/\d{4})T(\d{2}:\d{2})(?::\d{2}(?:\.\d+)?)?/g, '$1 $2');
        });
    };

    const renderCharts = () => {
        if (!window.Chart) {
            return;
        }
        const chartColors = ['#e85466', '#c98a9b', '#abb8c9', '#d8b67f'];
        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        Object.values(window.Chart.instances).forEach((chart) => {
            chart.stop?.();
            chart.options.color = '#b9bec7';
            chart.options.font = { family: 'Be Vietnam Pro', size: 12 };
            chart.options.animation = reducedMotion ? false : { duration: 180 };
            const revenue = chart.data.datasets.some(dataset => /doanh thu|VND/i.test(dataset.label || ''));
            chart.data.datasets.forEach((dataset, index) => {
                const color = chartColors[index % chartColors.length];
                dataset.borderColor = color;
                dataset.backgroundColor = chart.config.type === 'line' ? `${color}20` : color;
                dataset.pointBackgroundColor = color;
                dataset.pointBorderColor = color;
            });
            Object.entries(chart.options.scales || {}).forEach(([axisName, axis]) => {
                axis.grid = { ...axis.grid, color: '#ffffff0c' };
                axis.border = { ...axis.border, color: '#454a52' };
                axis.ticks = { ...axis.ticks, color: '#a7abb2' };
                const valueAxis = chart.options.indexAxis === 'y' ? 'x' : 'y';
                if (revenue && axisName === valueAxis) {
                    axis.ticks.callback = value => money.format(Number(value));
                }
            });
            chart.update('none');
        });
    };

    const renderImageFallbacks = () => {
        document.querySelectorAll('.trainer-card-image > img, .trainer-profile-image > img').forEach((image) => {
            const showFallback = () => {
                if (image.parentElement.querySelector('.uf-media-fallback')) {
                    return;
                }
                const fallback = document.createElement('span');
                fallback.className = 'uf-media-fallback';
                fallback.setAttribute('role', 'img');
                fallback.setAttribute('aria-label', image.alt || 'Ảnh huấn luyện viên đang cập nhật');
                const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                icon.setAttribute('viewBox', '0 0 48 48');
                icon.setAttribute('aria-hidden', 'true');
                icon.innerHTML = '<circle cx="24" cy="16" r="8"/><path d="M8 43v-5a16 16 0 0 1 32 0v5"/>';
                const caption = document.createElement('span');
                caption.textContent = 'Ảnh đang cập nhật';
                fallback.append(icon, caption);
                image.classList.add('uf-image-unavailable');
                image.parentElement.append(fallback);
            };
            image.addEventListener('error', showFallback, { once: true });
            if (image.complete && image.naturalWidth === 0) {
                showFallback();
            }
        });
    };

    const initialize = () => {
        if (!document.body.classList.contains('uf-theme')) {
            return;
        }
        renderLabels();
        renderCharts();
        renderImageFallbacks();
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize, { once: true });
    } else {
        initialize();
    }
})();
