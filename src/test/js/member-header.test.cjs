const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function mountHeader(hasHeader = true) {
    const classes = new Set();
    const listeners = new Map();
    const frames = new Map();
    let frameId = 0;
    const header = { classList: { toggle(name, enabled) { enabled ? classes.add(name) : classes.delete(name); } } };
    const window = {
        location: { pathname: '/member/dashboard' },
        scrollY: 0,
        addEventListener(name, callback, options) {
            if (!listeners.has(name)) listeners.set(name, new Map());
            listeners.get(name).set(callback, options);
        },
        removeEventListener(name, callback) { listeners.get(name)?.delete(callback); },
        requestAnimationFrame(callback) { frames.set(++frameId, callback); return frameId; },
        cancelAnimationFrame(id) { frames.delete(id); }
    };
    const document = {
        readyState: 'complete',
        body: { classList: { contains: name => name === 'uf-member' } },
        querySelectorAll: () => [],
        getElementById: () => null,
        querySelector: selector => selector === '.member-header' && hasHeader ? header : null
    };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/utefit-member.js', 'utf8'), { window, document, Intl });
    return {
        window, classes, frames, listeners,
        emit(name, event = {}) { for (const callback of listeners.get(name)?.keys() || []) callback(event); },
        flush() { const callbacks = [...frames.values()]; frames.clear(); callbacks.forEach(callback => callback()); }
    };
}

test('member header updates its presentation once per frame with passive scroll', () => {
    const page = mountHeader();
    assert.equal(page.classes.has('is-scrolled'), false);
    page.window.scrollY = 240;
    page.emit('scroll');
    page.emit('scroll');
    assert.equal(page.frames.size, 1);
    assert.equal([...page.listeners.get('scroll').values()][0].passive, true);
    page.flush();
    assert.equal(page.classes.has('is-scrolled'), true);
    page.window.scrollY = 0;
    page.emit('scroll');
    page.flush();
    assert.equal(page.classes.has('is-scrolled'), false);
});

test('member header cleans up queued work and restores correctly from bfcache', () => {
    const page = mountHeader();
    page.window.scrollY = 200;
    page.emit('scroll');
    page.emit('pagehide');
    assert.equal(page.frames.size, 0);
    assert.equal(page.listeners.get('scroll').size, 0);
    page.emit('pageshow', { persisted: true });
    assert.equal(page.classes.has('is-scrolled'), true);
    assert.equal(page.listeners.get('scroll').size, 1);
    page.emit('pageshow', { persisted: true });
    assert.equal(page.listeners.get('scroll').size, 1);
});

test('pages without a member header do not attach a scroll listener', () => {
    const page = mountHeader(false);
    assert.equal(page.listeners.has('scroll'), false);
    assert.equal(page.frames.size, 0);
});
