(() => {
    'use strict';

    const initializePresentation = () => {
        if (!document.body.classList.contains('uf-area-guest')
            || window.matchMedia('(prefers-reduced-motion: reduce)').matches
            || !('IntersectionObserver' in window)) {
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('uf-studio-enter');
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.08 });

        document.querySelectorAll('.section-heading h2, .section-heading p, .uf-home-branches figure, .uf-home-branches .program-card, .uf-home-pricing .pricing-card, .uf-home-classes figure, .uf-home-classes .program-card, .uf-home-reviews article, .uf-training-detail, .uf-promo-card').forEach((element) => {
            observer.observe(element);
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializePresentation, { once: true });
    } else {
        initializePresentation();
    }
})();
