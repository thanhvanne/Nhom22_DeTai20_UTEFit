(() => {
    'use strict';

    if (!window.gsap || !window.ScrollTrigger || !document.body.classList.contains('uf-area-guest')) return;

    const { gsap, ScrollTrigger } = window;
    gsap.registerPlugin(ScrollTrigger);
    let media;

    const mount = () => {
        if (media) return;
        media = gsap.matchMedia();
        media.add({ desktop: '(min-width: 992px)', animate: '(prefers-reduced-motion: no-preference)' }, (context) => {
            if (!context.conditions.animate) return;
            const hero = document.querySelector('.uf-campaign-opening');
            const heading = document.querySelector('main h1');
            const intro = document.querySelector('.hero-description, .ut-subtitle, .trainers-hero p');
            const entrance = gsap.timeline({ defaults: { duration: .55, ease: 'power2.out', clearProps: 'transform,opacity' } });
            if (heading) entrance.from(heading, { y: 20, opacity: .65 }, 0);
            if (intro) entrance.from(intro, { y: 12, opacity: .75 }, .08);

            const sections = hero
                ? document.querySelectorAll('.site-section:not(.uf-home-reviews):not(.uf-home-commitment)')
                : document.querySelectorAll('main > .row, .trainers-grid, .trainer-profile-layout, .branch-detail-grid');

            sections.forEach((section) => {
                const targets = [...section.querySelectorAll('.section-heading h2, .section-heading > p, figure, .program-card, .pricing-card, .ut-card, .trainer-card, .uf-coach-spread h2, .uf-coach-spread p')].slice(0, 10);
                if (!targets.length) return;
                gsap.from(targets, { y: 18, opacity: .72, duration: .5, stagger: .07, ease: 'power2.out', clearProps: 'transform,opacity', scrollTrigger: { trigger: section, start: 'top 88%', once: true } });
            });

            if (hero && context.conditions.desktop) {
                const scene = gsap.timeline({ scrollTrigger: { trigger: hero, start: 'top top', end: 'bottom top', scrub: .6 } });
                scene.to(hero.querySelector('.uf-home-masthead > img'), { yPercent: 9, scale: 1.04, ease: 'none' }, 0);
                scene.to(hero.querySelector('.uf-hero-near'), { yPercent: -16, ease: 'none' }, 0);
            }

            const refresh = () => ScrollTrigger.refresh();
            if (document.readyState === 'complete') refresh();
            else window.addEventListener('load', refresh, { once: true });
            return () => window.removeEventListener('load', refresh);
        });
    };

    window.addEventListener('pagehide', () => { media?.revert(); media = undefined; });
    window.addEventListener('pageshow', mount);
    mount();
})();
