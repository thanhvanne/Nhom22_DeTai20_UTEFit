/* Lucian UX/UI Prompt #025 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#025 */
/* Lucian UX/UI Prompt #036 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#036 */
(() => {
    'use strict';

    const initializeFields = () => {
        document.querySelectorAll('.u025-f').forEach((field) => {
            const input = field.querySelector('.u025-in');
            const label = field.querySelector('.u025-lab');
            const line = field.querySelector('.u025-line');
            if (!input || !label || !line) return;

            const lift = () => {
                const focused = document.activeElement === input;
                const raised = focused || input.value.length > 0 || input.matches(':autofill');
                const gap = raised ? label.offsetWidth * .8 + 2 : 0;
                field.classList.toggle('is-focus', focused);
                field.classList.toggle('is-up', raised);
                line.style.setProperty('--gap', `${gap}px`);
            };

            input.addEventListener('focus', lift);
            input.addEventListener('blur', lift);
            input.addEventListener('input', lift);
            input.addEventListener('change', lift);
            lift();
        });

        document.querySelectorAll('.u036').forEach((field) => {
            const input = field.querySelector('input');
            const eye = field.querySelector('.u036-eye');
            if (!input || !eye) return;
            eye.hidden = false;
            eye.addEventListener('click', () => {
                const show = input.type === 'password';
                input.type = show ? 'text' : 'password';
                field.classList.toggle('is-show', show);
                eye.setAttribute('aria-pressed', String(show));
                eye.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
            });
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeFields, { once: true });
    } else {
        initializeFields();
    }
})();
