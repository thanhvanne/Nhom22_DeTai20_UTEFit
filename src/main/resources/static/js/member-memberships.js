(() => {
    'use strict';

    const filterContainer = document.querySelector('[data-branch-filter]');
    const branchFilter = document.getElementById('membership-branch-filter');
    const result = document.querySelector('[data-branch-filter-result]');
    const branches = Array.from(document.querySelectorAll('.membership-catalog-branch'));

    if (!filterContainer || !branchFilter || !result || branches.length === 0) {
        return;
    }

    const updateBranches = () => {
        const selectedBranch = branchFilter.value;
        let visibleOptions = 0;

        branches.forEach((branch) => {
            branch.hidden = selectedBranch !== '' && branch.dataset.branchId !== selectedBranch;
            if (!branch.hidden) {
                visibleOptions += branch.querySelectorAll('.membership-option').length;
            }
        });

        result.hidden = false;
        result.textContent = visibleOptions > 0
            ? `Đang hiển thị ${visibleOptions} gói tập. Chỉ kỳ đã thanh toán mới được sử dụng.`
            : 'Chưa có gói đang mở bán tại chi nhánh đã chọn. Hãy chọn chi nhánh khác.';
    };

    branchFilter.addEventListener('change', updateBranches);
    filterContainer.hidden = false;
    updateBranches();
})();
