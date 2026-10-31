(function () {
    'use strict';
    const directory = document.querySelector('.pd-page');
    if (!directory) return;
    const portraits = directory.querySelectorAll('.pd-portrait img');
    portraits.forEach(image => {
        const fallback = () => image.hidden = true;
        image.addEventListener('error', fallback, { once: true });
        if (image.complete && !image.naturalWidth) fallback();
    });
    if (!window.gsap) return;
    const media = gsap.matchMedia();
    media.add('(prefers-reduced-motion: no-preference)', () => {
        const intro = directory.querySelector('.pd-intro');
        if (!intro) return;
        const photograph = intro.querySelector('.pd-photo img');
        const copy = intro.querySelectorAll('.pd-intro-copy > *');
        const timeline = gsap.timeline({ defaults: { ease: 'power3.out' } });
        if (photograph) timeline.from(photograph, { scale: 1.07, duration: 1.1 }, 0);
        timeline.from(copy, { y: 18, opacity: 0, duration: .65, stagger: .1, clearProps: 'transform,opacity' }, .1);
        return () => timeline.revert();
    });
    window.addEventListener('pagehide', () => media.revert(), { once: true });
}());
