(() => {
    'use strict';

    const story = document.querySelector('.pc-story');
    if (!story || !window.gsap || !window.ScrollTrigger || !window.UTEFitSurface) return;
    const { gsap, ScrollTrigger, UTEFitSurface } = window;
    gsap.registerPlugin(ScrollTrigger);
    const canvas = story.querySelector('.pc-particles');
    const stage = story.querySelector('.pc-stage');
    const hero = story.querySelector('.pc-scene--hero');
    const resolve = story.querySelector('.pc-scene--resolve');
    const images = [...story.querySelectorAll('.pc-poster img')];
    const navigation = document.querySelector('.pc-nav, .ut-site-header');
    let media;

    const mount = () => {
        if (media) return;
        media = gsap.matchMedia();
        media.add({ motion: '(prefers-reduced-motion: no-preference)', tall: '(min-height: 700px)' }, (scope) => {
            if (!scope.conditions.motion || !scope.conditions.tall) return;
            let renderer;
            let navObserver;
            let disposed = false;
            let request = 0;
            let resizeTimer = 0;
            let lastRendered = -1;
            const state = { progress: 0 };
            let imageSources = images.map((image) => image.currentSrc).join('|');
            const draw = () => {
                if (request) cancelAnimationFrame(request);
                request = 0;
                if (disposed || document.hidden || !renderer) return;
                const result = renderer.render(state.progress);
                if (!result) return;
                stage.dataset.renderMode = 'image-surface-webgl';
                stage.dataset.particlePhase = result.phase;
                stage.dataset.particleCount = String(renderer.count);
                stage.dataset.visibleParticles = String(result.detached);
                stage.dataset.sourceErosion = result.dissolve.toFixed(4);
                stage.dataset.targetReconstruction = result.assemble.toFixed(4);
                stage.dataset.scVerifyState = `${result.phase}:${result.dissolve.toFixed(3)}:${result.assemble.toFixed(3)}:${result.detached}`;
                stage.dataset.scVerifyHold = String(state.progress <= .15 || state.progress >= .94);
                hero.inert = state.progress > .36;
                resolve.inert = state.progress < .78;
                lastRendered = state.progress;
            };
            const schedule = () => {
                if (!disposed && !request && !document.hidden && lastRendered !== state.progress) request = requestAnimationFrame(draw);
            };
            const measure = () => {
                if (disposed || !renderer) return;
                const navHeight = navigation?.getBoundingClientRect().height || 0;
                const height = Math.max(620, window.innerHeight - navHeight);
                story.style.setProperty('--pc-nav-height', `${navHeight}px`);
                story.style.setProperty('--pc-height', `${height}px`);
                renderer.resize(stage.clientWidth, height, Math.min(window.devicePixelRatio || 1, window.innerWidth > 1100 ? 1.75 : 1.4));
                lastRendered = -1;
                draw();
            };
            const refresh = () => {
                clearTimeout(resizeTimer);
                resizeTimer = setTimeout(() => { measure(); ScrollTrigger.refresh(); }, 160);
                const requested = images.map((image) => image.currentSrc).join('|');
                if (requested !== imageSources) {
                    Promise.all(images.map((image) => image.decode())).then(() => {
                        if (disposed || !renderer || images.map((image) => image.currentSrc).join('|') !== requested) return;
                        renderer.replaceImages(images);
                        imageSources = requested;
                        lastRendered = -1;
                        draw();
                    }).catch(() => {});
                }
            };
            const visibility = () => {
                if (document.hidden) { cancelAnimationFrame(request); request = 0; }
                else { lastRendered = -1; schedule(); }
            };
            const scrollStart = () => {
                let top = 0;
                let ancestor = story;
                while (ancestor) { top += ancestor.offsetTop; ancestor = ancestor.offsetParent; }
                return top - (navigation?.offsetHeight || 0);
            };
            scope.add('initializeSurface', () => {
                if (disposed || renderer) return;
                renderer = UTEFitSurface.createRenderer(canvas, images, 12000);
                if (!renderer) return;
                imageSources = images.map((image) => image.currentSrc).join('|');
                story.classList.add('is-animated');
                measure();
                gsap.set(hero, { autoAlpha: 1 });
                gsap.set(hero.querySelectorAll('.pc-line > span'), { yPercent: 0 });
                gsap.set(resolve, { autoAlpha: 0 });
                gsap.set(resolve.querySelectorAll('.pc-line > span'), { yPercent: 110 });
                gsap.set(resolve.querySelectorAll('p, .pc-actions'), { autoAlpha: 0, y: 12 });
                gsap.set(story.querySelector('.pc-studio-light'), { xPercent: 0, opacity: 1 });
                gsap.set(story.querySelector('.pc-shadow--source'), { opacity: 1 });
                gsap.set(story.querySelector('.pc-shadow--target'), { opacity: 0 });
                const timeline = gsap.timeline({ defaults: { ease: 'none' }, scrollTrigger: {
                    id: 'utefit-public-cinematic', trigger: story, start: scrollStart,
                    end: () => scrollStart() + story.offsetHeight - stage.offsetHeight,
                    scrub: .18, invalidateOnRefresh: true, onRefresh: measure
                } });
                timeline.to(state, { progress: 1, duration: 1, onUpdate: draw }, 0)
                    .to(hero, { autoAlpha: 0, duration: .17 }, .21)
                    .to(hero.querySelectorAll('.pc-line > span'), { yPercent: -105, stagger: .03, duration: .2 }, .21)
                    .to(story.querySelector('.pc-studio-light'), { xPercent: -18, opacity: .8, duration: 1 }, 0)
                    .to(story.querySelector('.pc-shadow--source'), { opacity: 0, duration: .25 }, .16)
                    .to(story.querySelector('.pc-shadow--target'), { opacity: 1, duration: .27 }, .66)
                    .to(resolve, { autoAlpha: 1, duration: .08 }, .68)
                    .to(resolve.querySelectorAll('.pc-line > span'), { yPercent: 0, stagger: .04, duration: .15, ease: 'power3.out' }, .69)
                    .to(resolve.querySelectorAll('p, .pc-actions'), { autoAlpha: 1, y: 0, duration: .12, stagger: .03 }, .79);
                navObserver = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(refresh);
                if (navigation) navObserver?.observe(navigation);
                window.addEventListener('resize', refresh, { passive: true });
                document.addEventListener('visibilitychange', visibility);
                document.fonts?.ready.then(() => { if (!disposed) refresh(); });
                draw();
            });
            Promise.all(images.map((image) => image.decode())).then(() => {
                if (!disposed) scope.initializeSurface();
            }).catch(() => {});
            const imageLoad = () => {
                if (renderer) refresh();
                else Promise.all(images.map((image) => image.decode())).then(() => {
                    if (!disposed) scope.initializeSurface();
                }).catch(() => {});
            };
            images.forEach((image) => image.addEventListener('load', imageLoad));

            return () => {
                disposed = true;
                clearTimeout(resizeTimer);
                cancelAnimationFrame(request);
                navObserver?.disconnect();
                window.removeEventListener('resize', refresh);
                document.removeEventListener('visibilitychange', visibility);
                images.forEach((image) => image.removeEventListener('load', imageLoad));
                renderer?.destroy();
                story.classList.remove('is-animated');
                story.style.removeProperty('--pc-nav-height');
                story.style.removeProperty('--pc-height');
                hero.inert = false;
                resolve.inert = false;
                ['renderMode', 'particlePhase', 'particleCount', 'visibleParticles', 'sourceErosion', 'targetReconstruction', 'scVerifyState', 'scVerifyHold']
                    .forEach((key) => { delete stage.dataset[key]; });
            };
        });
    };
    const stop = () => { media?.revert(); media = undefined; };
    canvas.addEventListener('webglcontextlost', (event) => { event.preventDefault(); stop(); });
    canvas.addEventListener('webglcontextrestored', mount);
    window.addEventListener('pagehide', stop);
    window.addEventListener('pageshow', mount);
    const menu = document.querySelector('.pc-menu');
    menu?.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') { menu.open = false; menu.querySelector('summary').focus(); }
    });
    mount();
})();
