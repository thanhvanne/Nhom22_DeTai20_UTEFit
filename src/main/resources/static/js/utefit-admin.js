(() => {
    'use strict';

    if (!document.body.classList.contains('uf-admin')) return;

    const labels = new Map([
        ['ADMIN', 'Quản trị viên'], ['MEMBER', 'Hội viên'], ['PT', 'Huấn luyện viên'],
        ['MANAGER', 'Quản lý chi nhánh'], ['ACTIVE', 'Đang hoạt động'],
        ['LOCKED', 'Đã khóa'], ['UNVERIFIED', 'Chưa xác minh'], ['PENDING', 'Chờ xử lý'], ['CASH', 'Tiền mặt'],
        ['SEPAY', 'Chuyển khoản VietQR'], ['VNPAY', 'VNPay'], ['INACTIVE', 'Ngừng hoạt động'],
        ['OPEN', 'Đang mở'], ['FULL', 'Đã đủ chỗ'], ['COMPLETED', 'Đã hoàn thành'],
        ['PAID', 'Đã thanh toán'], ['CANCELLED', 'Đã hủy'], ['CANCEL_REQUESTED', 'Chờ duyệt hủy'],
        ['FAILED', 'Không thành công'], ['REFUNDED', 'Đã hoàn tiền']
    ]);
    const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });

    document.querySelectorAll('[data-uf-label]').forEach((element) => {
        if (element.children.length || element.matches('input, select, textarea')) return;
        const text = element.textContent.trim();
        const translated = labels.get(text) || text.replace(/\bPayment\b/g, 'giao dịch');
        if (translated !== text) element.textContent = translated;
    });

    document.querySelectorAll('[data-uf-money]').forEach((element) => {
        if (element.children.length || element.matches('input, select, textarea, option')) return;
        const numeric = element.textContent.trim().match(/^([+-]?\d+(?:\.\d{1,2})?)\s*(?:VND|đ|₫)?$/i);
        if (numeric && Number.isFinite(Number(numeric[1]))) element.textContent = money.format(Number(numeric[1]));
    });

    document.querySelectorAll('[data-uf-date]').forEach((element) => {
        if (element.children.length || element.matches('input, select, textarea, option')) return;
        element.textContent = element.textContent.replace(/\b(\d{4})-(\d{2})-(\d{2})(?=T|\b)/g, '$3/$2/$1')
            .replace(/(\d{2}\/\d{2}\/\d{4})T(\d{2}:\d{2})(?::\d{2}(?:\.\d+)?)?/g, '$1 $2');
    });

    document.querySelectorAll('.admin-sidebar .nav-link.active, .page-item.active .page-link').forEach((link) => link.setAttribute('aria-current', 'page'));
    document.querySelectorAll('.table-responsive').forEach((region) => {
        region.tabIndex = 0;
        region.setAttribute('role', 'region');
        if (!region.hasAttribute('aria-label')) region.setAttribute('aria-label', document.querySelector('.admin-topbar h1')?.textContent.trim() || 'Bảng dữ liệu');
    });
    document.querySelectorAll('.admin-data-table').forEach((table) => {
        table.setAttribute('role', 'table');
        table.querySelectorAll('thead, tbody').forEach((group) => group.setAttribute('role', 'rowgroup'));
        table.querySelectorAll('tr').forEach((row) => row.setAttribute('role', 'row'));
        table.querySelectorAll('th').forEach((cell) => {
            cell.scope = 'col';
            cell.setAttribute('role', 'columnheader');
            if (/^(Số tiền|Giá niêm yết|Sức chứa|Giảm|Số lượng)$/.test(cell.textContent.trim())) cell.classList.add('admin-number');
        });
        table.querySelectorAll('td').forEach((cell) => cell.setAttribute('role', 'cell'));
    });

    document.querySelectorAll('.is-invalid').forEach((field) => {
        field.setAttribute('aria-invalid', 'true');
        const container = field.parentElement.classList.contains('input-group') ? field.parentElement.parentElement : field.parentElement;
        const feedback = container.querySelector('.invalid-feedback, .text-danger.small');
        if (!field.id || !feedback?.textContent.trim()) return;
        if (!feedback.id) feedback.id = `${field.id}-feedback`;
        const descriptions = new Set((field.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean));
        descriptions.add(feedback.id);
        field.setAttribute('aria-describedby', [...descriptions].join(' '));
    });

    document.querySelectorAll('.alert:not([role])').forEach((alert) => {
        alert.setAttribute('role', alert.classList.contains('alert-danger') ? 'alert' : 'status');
    });
})();
