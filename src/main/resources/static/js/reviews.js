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

(() => {
    const video = document.getElementById('video');
    if (!video) return;
    const form = video.closest('form');
    const error = document.getElementById('review-video-error');
    const maxBytes = Number(video.dataset.maxBytes);
    const validate = () => {
        const file = video.files?.[0];
        let message = '';
        if (file && file.size > maxBytes) {
            message = 'Video không được vượt quá 50 MiB (52.428.800 byte).';
        } else if (file && !((file.type === 'video/mp4' && /\.mp4$/i.test(file.name)) ||
            (file.type === 'video/webm' && /\.webm$/i.test(file.name)))) {
            message = 'Chỉ nhận video MP4/WebM có loại file và phần mở rộng phù hợp. Server sẽ kiểm tra nội dung.';
        }
        video.setCustomValidity(message);
        if (error) error.textContent = message;
        globalThis.UTEFitValidation?.setFieldError(video, message);
        return !message;
    };
    video.addEventListener('change', validate);
    form?.addEventListener('submit', event => {
        if (!validate()) {
            event.preventDefault();
            video.focus();
        }
    });
})();
