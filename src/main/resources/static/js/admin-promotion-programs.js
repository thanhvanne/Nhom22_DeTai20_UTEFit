(() => {
    const forms = [...document.querySelectorAll('[data-program-form]')];
    const reset = form => {
        form.removeAttribute('aria-busy');
        form.querySelectorAll('[data-program-submit]').forEach(button => {
            button.disabled = false;
            button.textContent = button.dataset.programSubmit;
            delete button.dataset.programSubmit;
        });
    };
    forms.forEach(form => {
        form.addEventListener('submit', event => {
            if (form.getAttribute('aria-busy') === 'true') { event.preventDefault(); return; }
            if (window.UTEFitValidation ? !window.UTEFitValidation.validateForm(form) : !form.checkValidity()) { event.preventDefault(); return; }
            const active = form.querySelector('[name="active"]');
            if (form.dataset.confirmPause === 'true' && active && active.value === 'false'
                && !window.confirm(`Tạm dừng ${form.dataset.programName}? Đơn đã chốt và thanh toán cũ không thay đổi.`)) {
                event.preventDefault(); return;
            }
            form.setAttribute('aria-busy', 'true');
            form.querySelectorAll('button[type="submit"]:not(:disabled)').forEach(button => {
                button.dataset.programSubmit = button.textContent;
                button.disabled = true;
                button.textContent = 'Đang lưu…';
            });
        });
    });
    window.addEventListener('pageshow', () => forms.forEach(reset));
    document.querySelector('[data-program-errors]')?.focus();
})();
