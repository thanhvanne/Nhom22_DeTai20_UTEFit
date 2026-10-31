/* Lucian UX/UI Prompt #050 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#050 */
(() => {
    'use strict';
    const root = document.querySelector('.uf-page-member-dashboard .ut-dashboard');
    if (!root || !window.gsap) return;
    let media;
    let entered = false;
    const initialize = () => {
        media = window.gsap.matchMedia();
        media.add({
            all: '(min-width: 0px)',
            reduced: '(prefers-reduced-motion: reduce)',
            hover: '(hover: hover) and (pointer: fine)'
        }, context => {
            if (context.conditions.reduced) return;
            const cleanups = [];
            const listen = (element, event, handler) => {
                element.addEventListener(event, handler);
                cleanups.push(() => element.removeEventListener(event, handler));
            };
            if (!entered) {
                entered = true;
                const timeline = window.gsap.timeline({ defaults: {
                    duration: .5, ease: 'power3.out', clearProps: 'transform,opacity,visibility'
                } });
                timeline.addLabel('hero', 0)
                    .from(root.querySelector('.ut-greeting'), { y: 12, autoAlpha: 0 }, 'hero')
                    .from(root.querySelector('h1'), { y: 16, autoAlpha: 0 }, 'hero+=0.06')
                    .from(root.querySelector('.member-hero-description'), { y: 12, autoAlpha: 0 }, 'hero+=0.12')
                    .from(root.querySelectorAll('.ut-hero-links .btn'), { y: 10, autoAlpha: 0, stagger: .05 }, 'hero+=0.18')
                    .from(root.querySelector('.member-hero-visual img'), { scale: 1.06, autoAlpha: 0, duration: .75 }, 'hero')
                    .addLabel('panels', .28)
                    .from(root.querySelectorAll('.ut-dash-panel'), { y: 18, autoAlpha: 0, duration: .45, stagger: .07 }, 'panels');
                listen(root, 'focusin', () => timeline.progress(1));
            }
            if (context.conditions.hover) {
                root.querySelectorAll('.ut-quick-action').forEach((action, index) => {
                    const icon = action.querySelector('.uf-icon');
                    context.add('enterIcon' + index, () => window.gsap.to(icon, {
                        y: -4, rotation: -5, duration: .25, ease: 'power2.out', overwrite: 'auto'
                    }));
                    context.add('leaveIcon' + index, () => window.gsap.to(icon, {
                        y: 0, rotation: 0, duration: .3, ease: 'power2.out', overwrite: 'auto', clearProps: 'transform'
                    }));
                    listen(action, 'pointerenter', context['enterIcon' + index]);
                    listen(action, 'pointerleave', context['leaveIcon' + index]);
                });
                const pass = root.querySelector('.u050');
                let bounds;
                let pointer;
                let frame = 0;
                const paint = () => {
                    frame = 0;
                    if (!bounds || !pointer) return;
                    const horizontal = Math.max(0, Math.min(1, (pointer.clientX - bounds.left) / bounds.width));
                    const vertical = Math.max(0, Math.min(1, (pointer.clientY - bounds.top) / bounds.height));
                    pass.classList.add('is-live');
                    pass.style.setProperty('--mx', (horizontal * 100).toFixed(1) + '%');
                    pass.style.setProperty('--my', (vertical * 100).toFixed(1) + '%');
                    pass.style.setProperty('--ry', ((horizontal - .5) * 14).toFixed(2) + 'deg');
                    pass.style.setProperty('--rx', ((.5 - vertical) * 10).toFixed(2) + 'deg');
                };
                const reset = () => {
                    cancelAnimationFrame(frame);
                    frame = 0;
                    bounds = null;
                    pointer = null;
                    pass.classList.remove('is-live');
                    ['--mx', '--my', '--rx', '--ry'].forEach(property => pass.style.removeProperty(property));
                };
                listen(pass, 'pointerenter', event => {
                    if (event.pointerType !== 'touch') bounds = pass.getBoundingClientRect();
                });
                listen(pass, 'pointermove', event => {
                    if (event.pointerType === 'touch' || !bounds) return;
                    pointer = event;
                    if (!frame) frame = requestAnimationFrame(paint);
                });
                listen(pass, 'pointerleave', reset);
                listen(window, 'scroll', reset);
                listen(window, 'resize', reset);
                cleanups.push(reset);
            }
            return () => cleanups.forEach(cleanup => cleanup());
        }, root);
    };
    initialize();
    window.addEventListener('pagehide', () => media.revert());
    window.addEventListener('pageshow', event => { if (event.persisted) initialize(); });
})();
