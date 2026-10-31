const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

class Element {
    constructor(dataset = {}) {
        this.dataset = dataset;
        this.attributes = new Map();
        this.events = new Map();
        this.selectors = new Map();
        this.hidden = false;
        this.offsetWidth = 120;
        this.offsetLeft = 0;
        this.textContent = '';
        this.style = { setProperty() {} };
        const classes = new Set();
        this.classList = { add: name => classes.add(name), remove: name => classes.delete(name), contains: name => classes.has(name) };
    }
    addEventListener(name, listener) { this.events.set(name, [...(this.events.get(name) || []), listener]); }
    emit(name, event = {}) { this.events.get(name)?.forEach(listener => listener(event)); }
    querySelector(selector) { return this.selectors.get(selector) || null; }
    querySelectorAll(selector) { return this.selectors.get(selector) || []; }
    setAttribute(name, value) { this.attributes.set(name, value); }
    getAttribute(name) { return this.attributes.get(name) ?? null; }
    removeAttribute(name) { this.attributes.delete(name); }
    focus() { this.focused = true; }
    setCustomValidity(message) { this.validationMessage = message; }
}

function mount({ initialTab = 'overview', savedAvatar = true, serverError = false, previous = null, storageThrows = false } = {}) {
    const root = new Element({ initialTab });
    const tabs = ['overview', 'edit', 'security'].map(name => new Element({ profileTab: name }));
    const panels = ['overview', 'edit', 'security'].map(name => new Element({ profilePanel: name }));
    const openers = ['edit', 'security'].map(name => new Element({ profileOpen: name }));
    const dialog = new Element();
    dialog.showModal = () => { dialog.open = true; };
    dialog.close = () => { dialog.open = false; dialog.emit('close'); };
    const avatar = new Element();
    Object.assign(avatar, { complete: true, naturalWidth: savedAvatar ? 120 : 0, currentSrc: '/saved-avatar.png' });
    const view = new Element();
    view.selectors.set('[data-profile-avatar]', savedAvatar ? avatar : null);
    const preview = new Element();
    const file = new Element();
    file.files = [];
    const window = new Element();
    window.matchMedia = () => ({ matches: true });
    window.setTimeout = callback => { callback(); return 1; };
    window.clearTimeout = () => {};
    window.confirm = () => true;
    const body = new Element();
    const fullImage = new Element();
    const closeButton = new Element();
    const error = new Element();
    const filename = new Element();
    const forms = ['edit', 'edit', 'security'].map(name => {
        const form = new Element({ profileForm: name });
        const button = new Element({ busyLabel: 'Đang lưu…' });
        button.textContent = 'Lưu thay đổi';
        form.selectors.set('[type=submit]', button);
        form.selectors.set('input:not([type=hidden])', [Object.assign(new Element(), { value: '', type: 'text' })]);
        return form;
    });
    for (const [selector, element] of Object.entries({
        '[data-profile-tab]': tabs, '[data-profile-panel]': panels, '[data-profile-tabs]': new Element(),
        '[data-profile-open]': openers, '#profile-avatar-dialog': dialog, '[data-avatar-view]': view,
        '[data-avatar-full]': fullImage, '[data-avatar-hint]': new Element(), '[data-profile-avatar]': savedAvatar ? [avatar] : [],
        '[data-avatar-close]': closeButton, '#member-avatar-upload': file, '[data-avatar-preview]': preview,
        '#profile-avatar-file': filename, '#profile-avatar-error': error, '[data-profile-form]': forms,
        '[data-profile-server-error]': serverError ? new Element() : null
    })) root.selectors.set(selector, element);
    const document = { querySelector: () => root, body, activeElement: view };
    const storage = new Map(previous ? [['utefit-profile-submit', JSON.stringify(previous)]] : []);
    const sessionStorage = {
        getItem(key) { if (storageThrows) throw new Error('Storage unavailable'); return storage.get(key); },
        removeItem: key => storage.delete(key), setItem: (key, value) => storage.set(key, value)
    };
    const revoked = [];
    let objectId = 0;
    const URL = { createObjectURL: () => `blob:test-${++objectId}`, revokeObjectURL: address => revoked.push(address) };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/utefit-member-profile.js', 'utf8'), { document, window, sessionStorage, URL });
    return { root, tabs, panels, openers, dialog, avatar, view, preview, file, window, fullImage, closeButton, error, filename, forms, body, revoked, storage };
}

test('profile defaults to overview; tabs and keyboard preserve mounted forms', () => {
    const page = mount();
    assert.deepEqual(page.panels.map(panel => panel.hidden), [false, true, true]);
    page.openers[0].emit('click');
    assert.deepEqual(page.panels.map(panel => panel.hidden), [true, false, true]);
    assert.equal(page.tabs[1].focused, true);
    let prevented = false;
    page.tabs[1].emit('keydown', { key: 'ArrowRight', preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(page.panels[2].hidden, false);
    assert.equal(page.tabs[2].tabIndex, 0);
    assert.equal(page.forms[0], page.root.querySelectorAll('[data-profile-form]')[0]);
    page.tabs[2].emit('keydown', { key: 'Home', preventDefault() {} });
    assert.equal(page.panels[0].hidden, false);
});

test('server validation selects its tab and flash errors restore only recent submission metadata', () => {
    assert.equal(mount({ initialTab: 'security' }).panels[2].hidden, false);
    const page = mount({ serverError: true, previous: { tab: 'edit', time: Date.now() } });
    assert.equal(page.panels[1].hidden, false);
    assert.equal(page.storage.size, 0);
    assert.equal(mount({ serverError: true, previous: { tab: 'security', time: 0 } }).panels[0].hidden, false);
    assert.equal(mount({ storageThrows: true }).panels[0].hidden, false);
});

test('avatar viewer uses saved source and closes with button, backdrop and Escape, restoring focus', () => {
    const page = mount();
    for (const close of [() => page.closeButton.emit('click'), () => page.dialog.emit('click', { target: page.dialog }), () => page.dialog.emit('cancel', { preventDefault() {} })]) {
        page.view.emit('click');
        assert.equal(page.dialog.open, true);
        assert.equal(page.fullImage.src, '/saved-avatar.png');
        assert.equal(page.body.classList.contains('uf-profile-viewing'), true);
        let trapped = false;
        page.dialog.emit('keydown', { key: 'Tab', preventDefault() { trapped = true; } });
        assert.equal(trapped, true);
        assert.equal(page.closeButton.focused, true);
        close();
        assert.equal(page.dialog.open, false);
        assert.equal(page.view.focused, true);
        assert.equal(page.body.classList.contains('uf-profile-viewing'), false);
    }
});

test('missing and broken avatars cannot open empty lightboxes', () => {
    const empty = mount({ savedAvatar: false });
    assert.equal(empty.view.disabled, true);
    empty.view.emit('click');
    assert.notEqual(empty.dialog.open, true);
    const broken = mount();
    broken.avatar.emit('error');
    assert.equal(broken.avatar.hidden, true);
    assert.equal(broken.view.disabled, true);
});

test('preview validates MIME, size and decode without replacing saved avatar or storing private form data', () => {
    const page = mount();
    page.file.files = [{ name: 'photo.png', type: 'image/png', size: 1024 }];
    page.file.emit('change');
    page.preview.setAttribute('src', page.preview.src);
    page.preview.emit('load');
    assert.equal(page.preview.hidden, false);
    assert.equal(page.file.validationMessage, '');
    page.view.emit('click');
    assert.equal(page.fullImage.src, '/saved-avatar.png');
    page.file.files = [{ name: 'unsafe.svg', type: 'image/svg+xml', size: 20 }];
    page.file.emit('change');
    assert.equal(page.error.hidden, false);
    assert.deepEqual(page.revoked, ['blob:test-1']);
    assert.equal(page.preview.hidden, true);
    page.file.files = [{ name: 'large.png', type: 'image/png', size: 5 * 1024 * 1024 + 1 }];
    page.file.emit('change');
    assert.match(page.file.validationMessage, /5 MB/);
    page.file.files = [{ name: 'broken.png', type: 'image/png', size: 100 }];
    page.file.emit('change');
    page.preview.emit('error');
    assert.match(page.file.validationMessage, /Không thể đọc/);
    assert.equal(page.storage.size, 0);
});

test('submit loading blocks duplicate submissions and resets on bfcache restoration', () => {
    const page = mount();
    const form = page.forms[0];
    const button = form.querySelector('[type=submit]');
    form.emit('submit', { defaultPrevented: true });
    assert.notEqual(button.disabled, true);
    form.emit('submit', {});
    assert.equal(button.disabled, true);
    assert.equal(button.textContent, 'Đang lưu…');
    let blocked = false;
    form.emit('submit', { preventDefault() { blocked = true; } });
    assert.equal(blocked, true);
    const stored = JSON.parse(page.storage.get('utefit-profile-submit'));
    assert.deepEqual(Object.keys(stored), ['tab', 'time']);
    page.window.emit('pageshow');
    assert.equal(button.disabled, false);
    assert.equal(button.textContent, 'Lưu thay đổi');
});

test('saving another form asks before discarding unrelated unsaved drafts', () => {
    const page = mount();
    const draft = page.forms[1].querySelectorAll('input:not([type=hidden])')[0];
    draft.value = 'Unsaved profile draft';
    page.window.confirm = () => false;
    let cancelled = false;
    page.forms[0].emit('submit', { preventDefault() { cancelled = true; } });
    assert.equal(cancelled, true);
    assert.notEqual(page.forms[0].querySelector('[type=submit]').disabled, true);
    assert.equal(draft.value, 'Unsaved profile draft');
    assert.equal(page.storage.size, 0);
});
