(() => {
    'use strict';

    document.querySelectorAll('[data-branch-image]').forEach((image) => {
        const showError = () => {
            const media = image.closest('[data-branch-media]');
            image.hidden = true;
            media.classList.remove('has-photo');
            media.classList.add('is-missing');
            const message = media.querySelector('[data-branch-fallback-note]');
            if (message) {
                message.hidden = false;
                message.textContent = 'Không tải được ảnh chi nhánh';
            }
            const caption = media.querySelector('[data-branch-caption]');
            if (caption) caption.hidden = true;
        };
        image.addEventListener('error', showError, { once: true });
        if (image.complete && image.naturalWidth === 0) showError();
    });
})();
