/* Chọn PT hợp lệ theo chi nhánh.
   Backend vẫn kiểm tra bằng chứng thực tế. */

(() => {
    const branch = document.getElementById('branchId');
    const trainer = document.getElementById('trainerId');

    if (!branch || !trainer || branch.disabled || trainer.disabled) {
        return;
    }

    function filter() {
        const selected = branch.value;

        for (const option of trainer.options) {
            if (!option.value) {
                continue;
            }

            option.hidden =
                option.dataset.branch !== selected;

            option.disabled =
                option.dataset.branch !== selected;
        }

        if (
            trainer.selectedOptions.length &&
            trainer.selectedOptions[0].disabled
        ) {
            trainer.value = '';
        }
    }

    branch.addEventListener('change', filter);
    filter();
})();