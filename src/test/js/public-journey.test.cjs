const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const script = fs.readFileSync('src/main/resources/static/js/utefit-public-journey.js', 'utf8');
const fragment = fs.readFileSync('src/main/resources/templates/fragments/public-journey.html', 'utf8');
const home = fs.readFileSync('src/main/resources/templates/guest/home.html', 'utf8');
const finale = fs.readFileSync('src/main/resources/templates/fragments/public-finale.html', 'utf8');

const gallery = (count, reduced = false, brokenImage = false) => {
    const element = (properties = {}) => ({
        events: {}, attributes: {}, hidden: true, disabled: false, style: {},
        classList: { values: new Set(), add(value) { this.values.add(value); }, toggle(value, enabled) { if (enabled) this.values.add(value); else this.values.delete(value); } },
        addEventListener(type, handler) { this.events[type] = handler; },
        setAttribute(key, value) { this.attributes[key] = value; },
        removeAttribute(key) { delete this.attributes[key]; },
        ...properties
    });
    const previous = element();
    const next = element();
    const controls = element();
    const fill = element();
    const progress = element({ querySelector: () => fill });
    const slides = Array.from({ length: count }, (_, index) => element({ offsetLeft: index * 800 }));
    const viewport = element({ clientWidth: 1000, scrollLeft: 0,
        scrollTo(options) { this.lastScroll = options; this.scrollLeft = options.left; this.events.scroll?.(); }
    });
    const track = element({ scrollWidth: Math.max(1000, count * 800) });
    const selectors = { '.pj-clubs-viewport': viewport, '.pj-clubs-track': track, '.pj-gallery-controls': controls,
        '.pj-gallery-progress': progress, '.pj-previous': previous, '.pj-next': next };
    const clubs = element({ querySelector: (selector) => selectors[selector], querySelectorAll: () => slides });
    const imageContainer = element();
    const image = element({ complete: true, naturalWidth: brokenImage ? 0 : 100, closest: () => imageContainer, attributes: { src: '/missing.jpg' } });
    const document = { querySelector: (selector) => ({ '.pj-arena': element(), '.pj-training': element(), '.pj-clubs': clubs })[selector], querySelectorAll: () => [image] };
    vm.runInNewContext(script, { document, window: { addEventListener() {} }, matchMedia: () => ({ matches: reduced }) });
    return { previous, next, controls, fill, progress, slides, viewport, clubs, image, imageContainer };
};

test('zero and one branch never create duplicate slides or fake gallery controls', () => {
    assert.equal(gallery(0).controls.hidden, true);
    const single = gallery(1);
    assert.equal(single.controls.hidden, true);
    assert.equal(single.clubs.classList.values.has('has-single'), true);
});

test('native carousel advances and clamps to actual measured overflow', () => {
    const state = gallery(3);
    assert.equal(state.previous.disabled, true);
    state.next.events.click();
    assert.equal(state.viewport.scrollLeft, 800);
    state.next.events.click();
    assert.equal(state.viewport.scrollLeft, 1400);
    assert.equal(state.progress.attributes['aria-valuenow'], '100');
    assert.equal(state.next.disabled, true);
    state.previous.events.click();
    assert.equal(state.viewport.scrollLeft, 800);
});

test('keyboard Home and End work without hijacking document keys', () => {
    const state = gallery(3);
    let prevented = false;
    state.viewport.events.keydown({ key: 'End', target: state.viewport, preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(state.viewport.scrollLeft, 1400);
    state.viewport.events.keydown({ key: 'Home', target: state.viewport, preventDefault() {} });
    assert.equal(state.viewport.scrollLeft, 0);
    state.viewport.events.keydown({ key: 'End', target: {}, preventDefault() { throw new Error('Child key intercepted'); } });
    assert.equal(state.viewport.scrollLeft, 0);
});

test('reduced motion uses instant carousel controls', () => {
    const state = gallery(3, true);
    state.next.events.click();
    assert.equal(state.viewport.lastScroll.behavior, 'instant');
});

test('broken branch image reveals honest placeholder without substituting a stock photo', () => {
    const state = gallery(2, false, true);
    assert.equal(state.imageContainer.classList.values.has('is-missing'), true);
    assert.equal(state.image.attributes.src, undefined);
});

test('no GSAP leaves readable flow and working native carousel', () => {
    const state = gallery(3);
    assert.equal(state.clubs.classList.values.has('is-enhanced'), false);
    state.next.events.click();
    assert.equal(state.viewport.scrollLeft, 800);
});

test('real catalog loops exist once, and no trainer roster or fake availability is invented', () => {
    assert.equal((fragment.match(/th:each="b : \$\{featuredBranches\}"/g) || []).length, 1);
    assert.equal((fragment.match(/th:each="c : \$\{featuredClasses\}"/g) || []).length, 1);
    assert.doesNotMatch(home, /featuredBranches|featuredClasses/);
    assert.doesNotMatch(fragment, /featuredTrainers|\bPAID\b|\bOPEN\b|\bFULL\b|Math\.random/);
    assert.match(fragment, /fragments\/branch-media :: publicMedia\(\$\{b\},'gallery'\)/);
    assert.doesNotMatch(fragment, /th:src="\$\{b.imageUrl\}"/);
    assert.match(fragment, /#lists\.isEmpty\(featuredBranches\)/);
    assert.match(fragment, /#lists\.isEmpty\(featuredClasses\)/);
});

test('existing pricing, reviews, footer and approved V2 scripts stay integrated', () => {
    for (const binding of ['featuredPlans', 'featuredReviews', 'r.videoUrl', 'r.imageUrl', 'r.author', 'p.durationDescription']) assert.ok(finale.includes(binding));
    for (const route of ['/register', '/reviews', '/member/memberships(planId=${p.id})']) assert.ok(finale.includes(route));
    assert.ok(home.includes('fragments/public-finale :: finale'));
    for (const component of ['fragments/footer :: footer', 'fragments/public-cinematic :: experience', 'utefit-public-surface.js(v=2)', 'utefit-public-cinematic.js(v=2)']) assert.ok(home.includes(component));
});

test('scroll choreography has three independent ordered timelines and no wheel interception or business requests', () => {
    for (const identifier of ['utefit-public-arena', 'utefit-public-training', 'utefit-public-clubs']) assert.ok(script.includes(identifier));
    assert.match(script, /clipPath: 'inset\(100% 34% 0% 34%\)'/);
    assert.match(script, /clipPath: 'inset\(100% 0% 0% 0%\)'/);
    assert.match(script, /storySignature/);
    assert.doesNotMatch(script, /fetch\(|XMLHttpRequest|addEventListener\(['"](?:wheel|touchmove)|setInterval/);
});

test('desktop horizontal story uses a native sticky pin and measured runway without JS vertical compensation', () => {
    const styles = fs.readFileSync('src/main/resources/static/css/utefit-public-journey.css', 'utf8');
    assert.match(styles, /\.pj-clubs\.is-enhanced \.pj-clubs-stage \{ position: sticky; top: var\(--pj-nav\)/);
    assert.match(styles, /height: calc\(var\(--pj-height\) \+ var\(--pj-gallery-distance, 0px\)\)/);
    const gallerySetup = script.slice(script.indexOf('if (scope.conditions.desktop && slides.length'), script.indexOf('const refresh ='));
    assert.doesNotMatch(gallerySetup, /pinType|pinSpacing|anticipatePin|\bpin:|yPercent|y:|top:|left:|margin:|height:|width:/);
    assert.match(gallerySetup, /start: galleryStart/);
    assert.match(gallerySetup, /getBoundingClientRect\(\)\.top \+ window.scrollY - navHeight\(\)/);
    assert.match(script, /galleryLead \+ galleryTravel \+ tail\(\)/);
    assert.match(script, /setTrackX\(-travelled\)/);
    assert.match(script, /galleryTravel = distance\(\)/);
    assert.doesNotMatch(script, /galleryTall|min-height: 900px|scrollHeight > Math/);
    assert.match(script, /slides\.length > 1 && distance\(\) > 0/);
});

test('homepage owns each real dataset once and opts out of legacy presentation animations', () => {
    const combined = home + fragment + finale;
    for (const dataset of ['featuredPlans', 'featuredReviews', 'featuredClasses', 'featuredBranches']) {
        assert.equal((combined.match(new RegExp('th:each="[^"]+ : \\$\\{' + dataset + '\\}"', 'g')) || []).length, 1);
    }
    assert.doesNotMatch(home, /uf-discovery-nav|uf-home-pricing|uf-home-reviews|uf-home-commitment|training-detail.jpg/);
    assert.match(fragment, /<details class="pj-class-disclosure">/);
    assert.match(fragment, /<a class="pj-mode-group" href="#lop-tap">/);
    assert.match(fragment, /<a class="pj-mode-personal" href="#huan-luyen-vien">/);
    assert.doesNotMatch(fragment, /pj-training-modes" aria-hidden/);
});

test('final conversion preserves empty states, real data and read-only existing routes', () => {
    for (const binding of ['#lists.isEmpty(featuredPlans)', '#lists.isEmpty(featuredReviews)', 'r.rating', 'r.branch', 'r.trainer', 'r.comment', 'r.author', 'r.imageUrl', 'r.videoUrl', 'p.price', 'p.description', 'p.durationDescription']) assert.ok(finale.includes(binding));
    assert.doesNotMatch(finale, /th:utext|<form|method="post"|action=|@membershipDisplay.name\(p.name\)\}\|/);
    const finaleScript = fs.readFileSync('src/main/resources/static/js/utefit-public-finale.js', 'utf8');
    assert.match(finaleScript, /prefers-reduced-motion: no-preference/);
    assert.match(finaleScript, /min-width: 1101px/);
    assert.match(finaleScript, /panel.inert = false/);
    assert.match(finaleScript, /media\?\.revert\(\)/);
    assert.doesNotMatch(finaleScript, /fetch\(|XMLHttpRequest|setInterval|addEventListener\(['"](?:wheel|touchmove)/);
});

test('transparent overlap wrappers do not intercept controls in the previous scene', () => {
    const journeyStyles = fs.readFileSync('src/main/resources/static/css/utefit-public-journey.css', 'utf8');
    const finaleStyles = fs.readFileSync('src/main/resources/static/css/utefit-public-finale.css', 'utf8');
    for (const selector of ['.pj-arena', '.pj-training', '.pj-training-stage']) {
        const escaped = selector.replaceAll('.', '\\.');
        assert.match(journeyStyles, new RegExp(escaped + ' \\{[^}]*pointer-events: none'));
    }
    assert.match(journeyStyles, /\.pj-clubs \{ pointer-events: none/);
    assert.match(journeyStyles, /\.pj-arena-window \{[^}]*pointer-events: auto/);
    assert.match(journeyStyles, /\.pj-training-entry \{ pointer-events: auto/);
    assert.match(finaleStyles, /\.pf-entry \{ pointer-events: auto/);
    assert.ok(finale.indexOf('class="pf-chapters"') < finale.lastIndexOf('</div>'));
});

test('training has a scroll-masked entry and synchronizes the actual image wipe with copy masks', () => {
    assert.match(script, /pj-training-overlap/);
    assert.match(script, /clipPath: 'polygon\(0% 100%, 100% 100%, 100% 100%, 0% 100%\)'/);
    assert.match(script, /clipPath: 'inset\(0% 0% 0% 100%\)'/);
    assert.match(script, /to\(personalLines/);
    assert.match(script, /to\(personalDetails/);
    assert.match(fragment, /id="pj-class-catalog"/);
    assert.match(fragment, /<details th:each="c/);
});

test('gallery labels demo images and never presents the abstract fallback as a facility plan', () => {
    const media = fs.readFileSync('src/main/resources/templates/fragments/branch-media.html', 'utf8');
    assert.match(media, /Chưa có ảnh chi nhánh/);
    assert.match(media, /Ảnh minh họa/);
    assert.match(media, /không phải sơ đồ cơ sở/);
    assert.match(media, /pj-club-drawing/);
    assert.doesNotMatch(fragment, /<span>UTE<span>FIT/);
});
