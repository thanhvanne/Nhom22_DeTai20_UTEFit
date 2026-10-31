(() => {
    'use strict';
    const story = document.querySelector('.pf-story');
    if (!story) return;
    const navigation = document.querySelector('.pc-nav, .ut-site-header');
    const panels = [...story.querySelectorAll('.pf-panel')];
    const links = [...story.querySelectorAll('.pf-chapters a')];
    let trigger;
    let media;
    let mounted = false;
    let refreshTimer = 0;
    let activePanel = -1;
    const motion = () => !matchMedia('(prefers-reduced-motion: reduce)').matches;
    const pageTop = element => {
        let top = 0;
        let ancestor = element;
        while (ancestor) { top += ancestor.offsetTop; ancestor = ancestor.offsetParent; }
        return top;
    };
    const setActive = index => {
        if (activePanel === index) return;
        activePanel = index;
        panels.forEach((panel, position) => {
            panel.inert = position !== index;
            if (position !== index) panel.querySelectorAll('video').forEach(video => video.pause());
        });
        links.forEach((link, position) => link.setAttribute('aria-current', String(position === index)));
    };
    const seek = hash => {
        if (!trigger) return false;
        const index = panels.findIndex(panel => '#' + panel.id === hash);
        if (index < 0) return false;
        const fraction = [.34, .69, 1][index];
        scrollTo({ top: trigger.start + (trigger.end - trigger.start) * fraction, behavior: motion() ? 'smooth' : 'instant' });
        return true;
    };
    links.forEach(link => link.addEventListener('click', event => {
        if (!seek(link.hash)) return;
        event.preventDefault();
        history.replaceState(null, '', link.hash);
    }));
    const menu = document.querySelector('.pc-menu');
    menu?.addEventListener('keydown', event => {
        if (event.key === 'Escape') { menu.open = false; menu.querySelector('summary')?.focus(); }
    });
    const mount = () => {
        if (mounted || !window.gsap || !window.ScrollTrigger) return;
        mounted = true;
        const { gsap, ScrollTrigger } = window;
        media = gsap.matchMedia();
        media.add('(min-width: 1101px) and (min-height: 700px) and (prefers-reduced-motion: no-preference)', () => {
            let disposed = false;
            const height = () => Math.max(620, innerHeight - (navigation?.offsetHeight || 0));
            const measure = () => {
                const clubs = document.querySelector('.pj-clubs');
                const overlap = clubs?.classList.contains('is-enhanced') ? height() + Number(clubs.dataset.galleryTail || 0) : 0;
                story.style.setProperty('--pf-height', height() + 'px');
                story.style.setProperty('--pf-nav', (navigation?.offsetHeight || 0) + 'px');
                story.style.setProperty('--pf-overlap', overlap + 'px');
            };
            story.classList.add('is-enhanced');
            measure();
            const entry = story.querySelector('.pf-entry');
            const reviews = story.querySelector('.pf-reviews');
            const conversion = story.querySelector('.pf-conversion');
            gsap.set(entry, { clipPath: 'inset(0% 100% 0% 0%)' });
            gsap.set(reviews, { clipPath: 'inset(100% 0% 0% 0%)' });
            gsap.set(conversion, { clipPath: 'inset(0% 0% 100% 0%)' });
            setActive(0);
            const timeline = gsap.timeline({ defaults: { ease: 'none' }, scrollTrigger: {
                id: 'utefit-public-finale', trigger: story,
                start: () => pageTop(story) - (navigation?.offsetHeight || 0),
                end: () => pageTop(story) - (navigation?.offsetHeight || 0) + height() * 2.2,
                scrub: .2, invalidateOnRefresh: true,
                onUpdate: self => {
                    story.dataset.progress = self.progress.toFixed(4);
                    setActive(self.progress < .59 ? 0 : self.progress < .95 ? 1 : 2);
                },
                onRefreshInit: measure
            } });
            trigger = timeline.scrollTrigger;
            timeline.to(entry, { clipPath: 'inset(0% 0% 0% 0%)', duration: .45 }, 0)
                .fromTo(story.querySelectorAll('.pf-plan'), { x: 20 }, { x: 0, stagger: .04, duration: .4 }, .4)
                .to(reviews, { clipPath: 'inset(0% 0% 0% 0%)', duration: .35 }, .95)
                .fromTo(story.querySelectorAll('.pf-review'), { y: 28 }, { y: 0, stagger: .04, duration: .4 }, 1.2)
                .to(conversion, { clipPath: 'inset(0% 0% 0% 0%)', duration: .45 }, 1.75)
                .fromTo(conversion.querySelectorAll('h2 span'), { xPercent: 8 }, { xPercent: 0, stagger: .04, duration: .3 }, 1.86);
            const refresh = () => {
                clearTimeout(refreshTimer);
                refreshTimer = setTimeout(() => { if (!disposed) { measure(); ScrollTrigger.refresh(); } }, 210);
            };
            window.addEventListener('resize', refresh, { passive: true });
            const images = [...story.querySelectorAll('img,video')];
            images.forEach(image => image.addEventListener('load', refresh));
            document.fonts?.ready.then(() => { if (!disposed) refresh(); });
            const hash = location.hash;
            if (panels.some(panel => '#' + panel.id === hash)) requestAnimationFrame(() => seek(hash));
            return () => {
                disposed = true;
                clearTimeout(refreshTimer);
                window.removeEventListener('resize', refresh);
                images.forEach(image => image.removeEventListener('load', refresh));
                trigger = undefined;
                activePanel = -1;
                story.classList.remove('is-enhanced');
                delete story.dataset.progress;
                ['--pf-height', '--pf-nav', '--pf-overlap'].forEach(property => story.style.removeProperty(property));
                panels.forEach(panel => { panel.inert = false; });
                links.forEach(link => link.removeAttribute('aria-current'));
            };
        });
    };
    window.addEventListener('pagehide', () => { media?.revert(); media = undefined; mounted = false; });
    window.addEventListener('pageshow', mount);
    mount();
})();
