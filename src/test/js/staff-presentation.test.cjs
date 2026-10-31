const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function mount({ role = 'PT', avatar = true } = {}) {
    let document;
    class Element {
        constructor() {
            this.events = new Map(); this.attributes = new Map(); this.selectors = new Map(); this.open = false;
            const classes = new Set();
            this.classList = { add: name => classes.add(name), remove: name => classes.delete(name), contains: name => classes.has(name), toggle: (name, value) => value ? classes.add(name) : classes.delete(name) };
        }
        addEventListener(name, callback) { this.events.set(name, [...(this.events.get(name) || []), callback]); }
        removeEventListener(name, callback) { this.events.set(name, (this.events.get(name) || []).filter(item => item !== callback)); }
        emit(name, data = {}) { const event = { key: '', defaultPrevented: false, target: this, preventDefault() { this.defaultPrevented = true; }, ...data }; this.events.get(name)?.forEach(callback => callback(event)); return event; }
        querySelector(selector) { return this.selectors.get(selector) || null; }
        querySelectorAll(selector) { return this.selectors.get(selector) || []; }
        setAttribute(name, value) { this.attributes.set(name, value); }
        getAttribute(name) { return this.attributes.get(name) || null; }
        removeAttribute(name) { this.attributes.delete(name); }
        focus() { document.activeElement = this; }
        contains(element) { return element === this || element === summary || items.includes(element); }
    }
    const header = new Element(); const account = new Element(); const summary = new Element();
    const items = [new Element(), new Element(), new Element()];
    account.selectors.set('summary', summary); account.selectors.set('.staff-account-menu a, .staff-account-menu button', items);
    const form = new Element(); const submitter = new Element(); submitter.name = 'result'; submitter.value = 'ATTENDED'; submitter.disabled = false;
    form.selectors.set('.is-submitting', [submitter]);
    const dialog = new Element(); const close = new Element(); const image = new Element(); const full = new Element(); const trigger = new Element();
    dialog.selectors.set('[data-staff-avatar-full]', full); dialog.selectors.set('[data-staff-avatar-close]', close);
    dialog.showModal = () => { dialog.open = true; }; dialog.close = () => { dialog.open = false; dialog.emit('close'); };
    image.complete = true; image.naturalWidth = avatar ? 200 : 0; image.currentSrc = '/avatar.png';
    image.closest = () => trigger; trigger.parentElement = new Element(); trigger.selectors.set('[data-staff-avatar]', image); trigger.isConnected = true;
    document = new Element(); document.body = new Element();
    document.selectors.set('.pt-header, .manager-header', role ? header : null);
    document.selectors.set('[data-staff-header]', role === 'MANAGER' ? header : null);
    document.selectors.set('[data-staff-account]', [account]); document.selectors.set('[data-staff-dialog]', dialog);
    document.selectors.set('[data-staff-avatar]', [image]); document.selectors.set('[data-staff-avatar-view]', [trigger]);
    document.selectors.set('[data-staff-submit]', [form]); document.selectors.set('[data-staff-submit],[data-staff-profile-form]', [form]);
    const window = new Element(); const timers = []; const frames = new Map();
    window.matchMedia = () => ({ matches: false, addEventListener() {} });
    window.setTimeout = callback => { timers.push(callback); return timers.length; }; window.clearTimeout = () => {};
    window.requestAnimationFrame = callback => { frames.set(frames.size + 1, callback); return frames.size; }; window.cancelAnimationFrame = index => frames.delete(index);
    window.scrollY = 0; window.location = { pathname: '/manager', hash: '' };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/utefit-staff.js', 'utf8'), { window, document, URL });
    return { window, document, account, summary, items, form, submitter, dialog, close, image, full, trigger, header, frames, flush() { while (timers.length) timers.shift()(); for (const callback of frames.values()) callback(); frames.clear(); } };
}

test('staff UI does not mount on Member, Admin or Public', () => {
    const page = mount({ role: null });
    assert.equal(page.window.events.size, 0); assert.equal(page.summary.events.size, 0);
});

test('account dropdown supports arrow keys, Escape and focus restoration', () => {
    const page = mount(); page.summary.focus();
    page.account.emit('keydown', { key: 'ArrowUp' });
    assert.equal(page.document.activeElement, page.items[2]); assert.equal(page.account.open, true);
    page.account.emit('keydown', { key: 'Home' }); assert.equal(page.document.activeElement, page.items[0]);
    page.account.emit('keydown', { key: 'ArrowDown' }); assert.equal(page.document.activeElement, page.items[1]);
    page.account.emit('keydown', { key: 'Escape' }); page.flush();
    assert.equal(page.account.open, false); assert.equal(page.document.activeElement, page.summary);
    assert.equal(page.summary.getAttribute('aria-expanded'), 'false');
});

test('outside click closes account dropdown without changing navigation', () => {
    const page = mount(); page.summary.emit('click'); assert.equal(page.account.open, true);
    page.document.emit('click', { target: {} }); page.flush(); assert.equal(page.account.open, false);
});

test('submit feedback prevents duplicates but preserves named submitter payload', () => {
    const page = mount(); const first = page.form.emit('submit', { submitter: page.submitter });
    assert.equal(first.defaultPrevented, false); assert.equal(page.submitter.disabled, false);
    assert.equal(page.submitter.name, 'result'); assert.equal(page.submitter.value, 'ATTENDED');
    assert.equal(page.form.emit('submit', { submitter: page.submitter }).defaultPrevented, true);
    page.window.emit('pageshow', { persisted: true }); assert.equal(page.form.getAttribute('aria-busy'), null);
    assert.equal(page.submitter.classList.contains('is-submitting'), false);
});

test('cancelled confirmation never enters loading state', () => {
    const page = mount(); page.form.emit('submit', { defaultPrevented: true, submitter: page.submitter });
    assert.equal(page.form.getAttribute('aria-busy'), null);
});

test('avatar viewer opens saved image, closes with X, backdrop or Escape and restores focus', () => {
    const page = mount(); assert.equal(page.trigger.disabled, false);
    for (const closeWith of [() => page.close.emit('click'), () => page.dialog.emit('click'), () => page.dialog.emit('cancel')]) {
        page.trigger.emit('click'); assert.equal(page.dialog.open, true); assert.equal(page.full.src, '/avatar.png');
        closeWith(); page.flush(); assert.equal(page.dialog.open, false); assert.equal(page.document.activeElement, page.trigger);
    }
});

test('missing or broken avatar never opens an empty lightbox', () => {
    const page = mount({ avatar: false }); assert.equal(page.trigger.disabled, true);
    page.trigger.emit('click'); assert.equal(page.dialog.open, false); assert.equal(page.image.hidden, true);
});

test('Manager sticky state is batched, cleans up, and resumes after bfcache', () => {
    const page = mount({ role: 'MANAGER' }); page.window.scrollY = 200;
    page.window.emit('scroll'); page.window.emit('scroll'); assert.equal(page.frames.size, 1); page.flush();
    assert.equal(page.header.classList.contains('is-scrolled'), true);
    page.window.emit('pagehide'); assert.equal(page.window.events.get('scroll').length, 0);
    page.window.scrollY = 0; page.window.emit('pageshow', { persisted: true });
    assert.equal(page.header.classList.contains('is-scrolled'), false);
});
