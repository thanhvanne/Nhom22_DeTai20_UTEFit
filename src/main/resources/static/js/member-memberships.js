(() => {
    'use strict';

    const filterContainer = document.querySelector('[data-branch-filter]');
    const branchFilter = document.getElementById('membership-branch-filter');
    const result = document.querySelector('[data-branch-filter-result]');
    const branches = Array.from(document.querySelectorAll('.membership-catalog-branch'));
    const expandButton = document.querySelector('[data-catalog-expand]');

    if (!filterContainer || !branchFilter || !result || branches.length === 0) {
        return;
    }

    const updateExpandButton = () => {
        if (!expandButton) return;
        const visibleBranches = branches.filter(branch => !branch.hidden);
        expandButton.disabled = visibleBranches.length === 0;
        expandButton.textContent = visibleBranches.some(branch => !branch.open)
            ? 'Mở tất cả chi nhánh' : 'Thu gọn danh sách';
    };

    branches.forEach(branch => {
        const count = branch.querySelectorAll('.membership-option').length;
        const label = branch.querySelector('[data-branch-option-count]');
        if (label) label.textContent = `${count} gói tập`;
        branch.addEventListener('toggle', updateExpandButton);
    });

    const updateBranches = () => {
        const selectedBranch = branchFilter.value;
        let visibleOptions = 0;

        branches.forEach((branch, index) => {
            branch.hidden = selectedBranch !== '' && branch.dataset.branchId !== selectedBranch;
            branch.open = !branch.hidden && (selectedBranch !== '' || index === 0);
            if (!branch.hidden) {
                visibleOptions += branch.querySelectorAll('.membership-option').length;
            }
        });

        result.hidden = false;
        result.textContent = visibleOptions > 0
            ? `Đang hiển thị ${visibleOptions} gói tập. Chỉ kỳ đã thanh toán mới được sử dụng.`
            : 'Chưa có gói đang mở bán tại chi nhánh đã chọn. Hãy chọn chi nhánh khác.';
        updateExpandButton();
    };

    expandButton?.addEventListener('click', () => {
        const visibleBranches = branches.filter(branch => !branch.hidden);
        const expand = visibleBranches.some(branch => !branch.open);
        visibleBranches.forEach(branch => { branch.open = expand; });
        updateExpandButton();
    });
    branchFilter.addEventListener('change', updateBranches);
    filterContainer.hidden = false;
    updateBranches();
})();
