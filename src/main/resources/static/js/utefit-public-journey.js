(() => {
    'use strict';

    const arena = document.querySelector('.pj-arena');
    const training = document.querySelector('.pj-training');
    const clubs = document.querySelector('.pj-clubs');
    if (!arena || !training || !clubs) return;
    const navigation = document.querySelector('.pc-nav, .ut-site-header');
    const story = document.querySelector('.pc-story');
    const viewport = clubs.querySelector('.pj-clubs-viewport');
    const track = clubs.querySelector('.pj-clubs-track');
    const slides = [...clubs.querySelectorAll('.pj-club')];
    const controls = clubs.querySelector('.pj-gallery-controls');
    const progress = clubs.querySelector('.pj-gallery-progress');
    const progressFill = progress?.querySelector('span');
    const previous = clubs.querySelector('.pj-previous');
    const next = clubs.querySelector('.pj-next');
    let galleryTrigger;
    let galleryLead = 0;
    let galleryTravel = 0;
    let media;
    let resizeTimer = 0;
    let mounted = false;
    const motion = () => !matchMedia('(prefers-reduced-motion: reduce)').matches;
    const navHeight = () => navigation?.getBoundingClientRect().height || 0;
    const pageTop = (element) => {
        let top = 0;
        let ancestor = element;
        while (ancestor) { top += ancestor.offsetTop; ancestor = ancestor.offsetParent; }
        return top;
    };
    const distance = () => Math.max(0, (track?.scrollWidth || 0) - (viewport?.clientWidth || 0));
    const galleryPosition = () => galleryTrigger ? Math.min(1, Math.max(0, (window.scrollY - galleryTrigger.start - galleryLead) / galleryTravel)) : distance() ? viewport.scrollLeft / distance() : 0;
    const updateProgress = (value) => {
        if (!progress) return;
        const clamped = Math.min(1, Math.max(0, value));
        progress.setAttribute('aria-valuenow', String(Math.round(clamped * 100)));
        progressFill.style.transform = `scaleX(${clamped})`;
        previous.disabled = clamped <= .001;
        next.disabled = clamped >= .999;
    };
    const goTo = (index) => {
        if (!viewport || !slides.length) return;
        const slide = slides[Math.min(slides.length - 1, Math.max(0, index))];
        const left = Math.min(distance(), slide.offsetLeft - slides[0].offsetLeft);
        if (galleryTrigger && distance()) {
            window.scrollTo({ top: galleryTrigger.start + galleryLead + left, behavior: motion() ? 'smooth' : 'instant' });
        } else viewport.scrollTo({ left, behavior: motion() ? 'smooth' : 'instant' });
    };
    const currentIndex = () => {
        const left = galleryPosition() * distance();
        return slides.reduce((closest, slide, index) => Math.abs(Math.min(distance(), slide.offsetLeft - slides[0].offsetLeft) - left) < Math.abs(Math.min(distance(), slides[closest].offsetLeft - slides[0].offsetLeft) - left) ? index : closest, 0);
    };
    const galleryKey = (event) => {
        if (event.target !== viewport) return;
        const keys = { ArrowRight: currentIndex() + 1, ArrowLeft: currentIndex() - 1, Home: 0, End: slides.length - 1 };
        if (!(event.key in keys)) return;
        event.preventDefault();
        goTo(keys[event.key]);
    };
    const galleryFocus = (event) => {
        if (!galleryTrigger || event.target === viewport) return;
        const index = slides.indexOf(event.target.closest('.pj-club'));
        if (index >= 0) { viewport.scrollLeft = 0; goTo(index); }
    };
    const nativeProgress = () => { if (!galleryTrigger) updateProgress(galleryPosition()); };
    const images = [...document.querySelectorAll('.pj-club-media img')];
    const failedImage = (image) => { image.closest('.pj-club-media').classList.add('is-missing'); image.removeAttribute('src'); };
    images.forEach((image) => {
        image.addEventListener('error', () => failedImage(image), { once: true });
        if (image.complete && !image.naturalWidth) failedImage(image);
    });
    clubs.classList.toggle('has-single', slides.length === 1);
    if (slides.length > 1) {
        controls.hidden = false;
        previous.addEventListener('click', () => goTo(currentIndex() - 1));
        next.addEventListener('click', () => goTo(currentIndex() + 1));
        viewport.addEventListener('keydown', galleryKey);
        viewport.addEventListener('focusin', galleryFocus);
        viewport.addEventListener('scroll', nativeProgress, { passive: true });
        updateProgress(0);
    }


    let trainingTrigger;
    const mount = () => {
        if (mounted || !window.gsap || !window.ScrollTrigger) return;
        mounted = true;
        const { gsap, ScrollTrigger } = window;
        gsap.registerPlugin(ScrollTrigger);
        media = gsap.matchMedia();
        media.add({ motion: '(prefers-reduced-motion: no-preference)', tall: '(min-height: 700px)', desktop: '(min-width: 1101px)' }, (scope) => {
            if (!scope.conditions.motion) return;
            let disposed = false;
            const height = () => Math.max(620, innerHeight - navHeight());
            const measure = () => {
                [arena, training, clubs].forEach(element => {
                    element.style.setProperty('--pj-height', String(element === clubs ? Math.max(320, innerHeight - navHeight()) : height()) + 'px');
                    element.style.setProperty('--pj-nav', navHeight() + 'px');
                });
                arena.style.setProperty('--pj-overlap', story?.classList.contains('is-animated') ? height() + 'px' : '0px');
                training.style.setProperty('--pj-training-overlap', height() + 'px');
            };
            measure();
            const finale = document.querySelector('.pf-story');
            const hasBridge = scope.conditions.desktop && scope.conditions.tall && slides.length > 1 && distance() > 0;
            if (hasBridge) training.classList.add('has-exit');
            if (scope.conditions.tall) {
            arena.classList.add('is-enhanced');
            const windowFrame = arena.querySelector('.pj-arena-window');
            const photo = arena.querySelector('.pj-arena-photo');
            const lines = arena.querySelectorAll('.pj-line > span');
            const copy = arena.querySelectorAll('.pj-arena-copy p, .pj-arena-copy a, .pj-photo-note');
            gsap.set(windowFrame, { clipPath: 'inset(100% 34% 0% 34%)' });
            gsap.set(photo, { scale: 1.22, yPercent: 5 });
            gsap.set(lines, { yPercent: 110 });
            gsap.set(copy, { autoAlpha: 0 });
            arena.querySelector('.pj-arena-copy').inert = true;
            const arenaTimeline = gsap.timeline({ defaults: { ease: 'none' }, scrollTrigger: {
                id: 'utefit-public-arena', trigger: arena, start: () => pageTop(arena) - navHeight() + 2,
                end: () => pageTop(arena) - navHeight() + arena.offsetHeight - arena.querySelector('.pj-arena-stage').offsetHeight + 2,
                scrub: .22, invalidateOnRefresh: true,
                onUpdate: (self) => { arena.dataset.progress = self.progress.toFixed(4); arena.querySelector('.pj-arena-copy').inert = self.progress < .4; }
            } });
            arenaTimeline.to(windowFrame, { clipPath: 'inset(24% 12% 12% 12%)', duration: .32 }, 0)
                .to(windowFrame, { clipPath: 'inset(0% 0% 0% 0%)', duration: .3 }, .32)
                .to(photo, { scale: 1, yPercent: -2, duration: 1 }, 0)
                .to(lines, { yPercent: 0, duration: .24, stagger: .05, ease: 'power3.out' }, .42)
                .to(copy, { autoAlpha: 1, duration: .16, stagger: .025 }, .58)
                .to(arena.querySelector('.pj-arena-blue'), { opacity: .7, duration: .22 }, .78);

                training.classList.add('is-enhanced');
                const entry = training.querySelector('.pj-training-entry');
                const personal = training.querySelector('.pj-training-scene--personal');
                const groupCopy = training.querySelector('.pj-training-copy--group');
                const groupLines = groupCopy.querySelectorAll('.pj-line > span');
                const groupDetails = groupCopy.querySelectorAll(':scope > p, :scope > .pj-training-actions, .pj-class-disclosure > summary');
                const personalCopy = training.querySelector('.pj-training-copy--personal');
                const personalLines = personalCopy.querySelectorAll('.pj-line > span');
                const personalDetails = personalCopy.querySelectorAll('p, .pj-training-actions');
                gsap.set(entry, { clipPath: 'polygon(0% 100%, 100% 100%, 100% 100%, 0% 100%)' });
                gsap.set(personal, { clipPath: 'inset(0% 0% 0% 100%)' });
                gsap.set(groupCopy, { clipPath: 'inset(0% 0% 0% 0%)' });
                gsap.set(groupLines, { yPercent: 110 });
                gsap.set(groupDetails, { clipPath: 'inset(100% 0% 0% 0%)', y: 12 });
                gsap.set(personalLines, { yPercent: 110 });
                gsap.set(personalDetails, { clipPath: 'inset(100% 0% 0% 0%)', y: 16 });
                gsap.set(training.querySelector('.pj-mode-personal'), { opacity: .4 });
                gsap.set(training.querySelector('.pj-training-photo--group img'), { scale: 1.1, xPercent: 3 });
                gsap.set(training.querySelector('.pj-training-photo--personal img'), { scale: 1.12, xPercent: 6 });
                groupCopy.inert = true;
                personalCopy.inert = true;
                const trainingStart = () => ScrollTrigger.getById('utefit-public-arena')?.end ?? pageTop(training) - navHeight() + 2;
                const trainingTimeline = gsap.timeline({ defaults: { ease: 'none' }, scrollTrigger: {
                    id: 'utefit-public-training', trigger: training.querySelector('.pj-training-story'),
                    start: trainingStart,
                    end: () => trainingStart() + height() * 1.6,
                    scrub: .2, invalidateOnRefresh: true,
                    onUpdate: self => {
                        training.dataset.progress = self.progress.toFixed(4);
                        groupCopy.inert = self.progress < .34 || self.progress > .57;
                        personalCopy.inert = self.progress < .88;
                        if (groupCopy.inert) training.querySelector('.pj-class-disclosure')?.removeAttribute('open');
                        training.querySelector('.pj-mode-group')?.setAttribute('aria-current', String(self.progress < .6));
                        training.querySelector('.pj-mode-personal')?.setAttribute('aria-current', String(self.progress >= .6));
                    }
                } });
                trainingTrigger = trainingTimeline.scrollTrigger;
                trainingTimeline.to(entry, { clipPath: 'polygon(0% 0%, 100% 0%, 100% 100%, 0% 100%)', duration: .3 }, 0)
                    .to(groupLines, { yPercent: 0, stagger: .025, duration: .12, ease: 'power3.out' }, .19)
                    .to(groupDetails, { clipPath: 'inset(0% 0% 0% 0%)', y: 0, stagger: .015, duration: .09 }, .23)
                    .to(training.querySelector('.pj-training-photo--group img'), { scale: 1, xPercent: -2, duration: .8 }, 0)
                    .to(groupCopy, { clipPath: 'inset(0% 0% 100% 0%)', duration: .18 }, .47)
                    .to(personal, { clipPath: 'inset(0% 0% 0% 0%)', duration: .34 }, .48)
                    .to(training.querySelector('.pj-training-photo--personal img'), { scale: 1, xPercent: 0, duration: .5 }, .5)
                    .to(personalLines, { yPercent: 0, stagger: .04, duration: .18, ease: 'power3.out' }, .65)
                    .to(personalDetails, { clipPath: 'inset(0% 0% 0% 0%)', y: 0, stagger: .025, duration: .16 }, .74)
                    .to(training.querySelector('.pj-training-modes i'), { scaleX: .3, duration: .2 }, .48)
                    .to(training.querySelector('.pj-mode-group'), { opacity: .4, duration: .3 }, .48)
                    .to(training.querySelector('.pj-mode-personal'), { opacity: 1, duration: .3 }, .48);
            }
            if (scope.conditions.desktop && slides.length > 1 && distance() > 0) {
                viewport.scrollLeft = 0;
                clubs.classList.add('is-enhanced');
                if (hasBridge) clubs.classList.add('has-entry');
                measure();
                galleryLead = hasBridge ? height() * .45 : 0;
                galleryTravel = distance();
                const tail = () => finale && scope.conditions.tall ? height() * .45 : 0;
                clubs.dataset.galleryLead = String(galleryLead);
                clubs.dataset.galleryTail = String(tail());
                if (hasBridge) gsap.set(clubs.querySelector('.pj-clubs-stage'), { clipPath: 'polygon(0% 100%, 100% 100%, 100% 100%, 0% 100%)' });
                gsap.set(track, { x: 0 });
                const clock = { progress: 0 };
                let previousReveal = -1;
                const setTrackX = gsap.quickSetter(track, 'x', 'px');
                const stage = clubs.querySelector('.pj-clubs-stage');
                const measureGallery = () => {
                    galleryLead = hasBridge ? height() * .45 : 0;
                    galleryTravel = distance();
                    clubs.dataset.galleryLead = String(galleryLead);
                    clubs.dataset.galleryTail = String(tail());
                    clubs.style.setProperty('--pj-gallery-distance', galleryLead + galleryTravel + tail() + 'px');
                };
                measureGallery();
                const galleryStart = () => clubs.getBoundingClientRect().top + window.scrollY - navHeight();
                const galleryTimeline = gsap.timeline({ defaults: { ease: 'none' }, scrollTrigger: {
                    id: 'utefit-public-clubs', trigger: clubs,
                    start: galleryStart,
                    end: () => galleryStart() + galleryLead + galleryTravel + tail(),
                    scrub: .2, invalidateOnRefresh: true,
                    onRefreshInit: () => { measure(); measureGallery(); }
                } });
                galleryTrigger = galleryTimeline.scrollTrigger;
                galleryTimeline.to(clock, { progress: 1, duration: 1, onUpdate: () => {
                    const elapsed = clock.progress * (galleryLead + galleryTravel + tail());
                    const travelled = Math.min(galleryTravel, Math.max(0, elapsed - galleryLead));
                    setTrackX(-travelled);
                    if (galleryLead) {
                        const reveal = Math.max(0, 100 * (1 - elapsed / galleryLead));
                        if (reveal !== previousReveal) {
                            stage.style.clipPath = 'inset(' + reveal + '% 0% 0% 0%)';
                            previousReveal = reveal;
                        }
                    }
                    updateProgress(galleryTravel ? travelled / galleryTravel : 0);
                } });
            }
            const refresh = () => {
                clearTimeout(resizeTimer);
                resizeTimer = setTimeout(() => { if (!disposed) { measure(); ScrollTrigger.refresh(); } }, 180);
            };
            const storySignature = () => String(story?.className) + '|' + story?.style.getPropertyValue('--pc-height') + '|' + story?.style.getPropertyValue('--pc-nav-height');
            let previousSignature = storySignature();
            const observer = new MutationObserver(() => {
                const signature = storySignature();
                if (signature !== previousSignature) { previousSignature = signature; refresh(); }
            });
            if (story) observer.observe(story, { attributes: true, attributeFilter: ['class', 'style'] });
            const resizeObserver = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(refresh);
            if (viewport) resizeObserver?.observe(viewport);
            const sceneImages = [...training.querySelectorAll('img'), ...clubs.querySelectorAll('img')];
            sceneImages.forEach(image => image.addEventListener('load', refresh));
            window.addEventListener('resize', refresh, { passive: true });
            document.fonts?.ready.then(() => { if (!disposed) refresh(); });
            return () => {
                disposed = true;
                clearTimeout(resizeTimer);
                observer.disconnect();
                resizeObserver?.disconnect();
                sceneImages.forEach(image => image.removeEventListener('load', refresh));
                window.removeEventListener('resize', refresh);
                galleryTrigger = undefined;
                trainingTrigger = undefined;
                [arena, training, clubs].forEach(element => { element.classList.remove('is-enhanced'); delete element.dataset.progress; });
                clubs.classList.remove('has-entry');
                training.classList.remove('has-exit');
                delete clubs.dataset.galleryLead;
                delete clubs.dataset.galleryTail;
                galleryLead = 0;
                galleryTravel = 0;
                [arena, training, clubs].forEach(element => ['--pj-height', '--pj-nav', '--pj-overlap', '--pj-training-overlap', '--pj-gallery-distance'].forEach(key => element.style.removeProperty(key)));
                arena.querySelector('.pj-arena-copy').inert = false;
                training.querySelectorAll('.pj-training-copy').forEach(element => { element.inert = false; });
                updateProgress(0);
            };
        });
    };
    const stop = () => { media?.revert(); media = undefined; mounted = false; };
    document.querySelectorAll('a[href="#lop-tap"], a[href="#huan-luyen-vien"]').forEach(link => link.addEventListener('click', event => {
        if (!trainingTrigger) return;
        event.preventDefault();
        const fraction = link.hash === '#lop-tap' ? .36 : .96;
        history.replaceState(null, '', link.hash);
        scrollTo({ top: trainingTrigger.start + (trainingTrigger.end - trainingTrigger.start) * fraction, behavior: motion() ? 'smooth' : 'instant' });
    }));
    window.addEventListener('pagehide', stop);
    window.addEventListener('pageshow', mount);
    mount();
})();
