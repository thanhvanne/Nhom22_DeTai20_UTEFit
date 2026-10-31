const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function mount(pathname, hasHeader = true, tables = []) {
    const events = new Map();
    const frames = new Map();
    const classes = new Set();
    const links = ['/pt/dashboard', '/pt/trainer-requests', '/pt/classes', '/pt/personal-schedules', '/pt/profile', '/pt/chats', '/notifications'].map(route => {
        const link = new URL(route, 'https://local.test');
        link.attributes = {};
        link.setAttribute = (name, value) => { link.attributes[name] = value; };
        return link;
    });
    const header = {
        querySelectorAll: () => links,
        querySelector: () => null,
        classList: { toggle(name, enabled) { enabled ? classes.add(name) : classes.delete(name); } }
    };
    let nextFrame = 0;
    const window = {
        location: { pathname }, scrollY: 0,
        matchMedia: () => ({ matches: false, addEventListener() {}, removeEventListener() {} }),
        addEventListener(name, callback, options) {
            if (!events.has(name)) events.set(name, new Map());
            events.get(name).set(callback, options);
        },
        removeEventListener(name, callback) { events.get(name)?.delete(callback); },
        requestAnimationFrame(callback) { frames.set(++nextFrame, callback); return nextFrame; },
        cancelAnimationFrame(id) { frames.delete(id); }
    };
    const document = {
        readyState: 'complete', querySelector: () => hasHeader ? header : null,
        querySelectorAll: () => tables, getElementById: () => null
    };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/utefit-pt.js', 'utf8'), { window, document, URL });
    return {
        window, events, frames, classes, links,
        emit(name, event = {}) { [...(events.get(name)?.keys() || [])].forEach(callback => callback(event)); },
        flush() { const callbacks = [...frames.values()]; frames.clear(); callbacks.forEach(callback => callback()); }
    };
}

test('PT nav selects profile subpage without confusing personal schedules or classes', () => {
    for (const [route, expected] of [['/pt/profile/edit','/pt/profile'],['/pt/classes/12','/pt/classes'],['/pt/personal-schedules/create','/pt/personal-schedules'],['/pt/chat/member/12','/pt/chats'],['/notifications','/notifications']]) {
        const page = mount(route);
        assert.deepEqual(page.links.filter(link => link.attributes['aria-current'] === 'page').map(link => link.pathname), [expected]);
    }
});

test('PT sticky presentation batches scroll, cleans up, and resumes from bfcache', () => {
    const page = mount('/pt/dashboard');
    page.window.scrollY = 200;
    page.emit('scroll');
    page.emit('scroll');
    assert.equal(page.frames.size, 1);
    assert.equal([...page.events.get('scroll').values()][0].passive, true);
    page.flush();
    assert.equal(page.classes.has('is-scrolled'), true);
    page.emit('pagehide');
    assert.equal(page.events.get('scroll').size, 0);
    page.window.scrollY = 0;
    page.emit('pageshow', { persisted: true });
    assert.equal(page.classes.has('is-scrolled'), false);
    assert.equal(page.events.get('scroll').size, 1);
});

test('PT presentation does nothing on non-PT pages', () => {
    const page = mount('/member/dashboard', false);
    assert.equal(page.events.size, 0);
});

test('responsive table enhancement keeps cells and form markup intact, only adding labels', () => {
    const cells = [{ dataset: {} }, { dataset: {} }];
    const row = { children: cells, querySelector: () => null };
    const attributes = {};
    const table = { querySelectorAll: selector => selector === 'thead th' ? [{ textContent: 'Hội viên' }, { textContent: 'Hành động' }] : [row], setAttribute(name, value) { attributes[name] = value; } };
    mount('/pt/trainer-requests', true, [table]);
    assert.deepEqual(cells.map(cell => cell.dataset.ptLabel), ['Hội viên', 'Hành động']);
    assert.equal(attributes['data-pt-responsive'], '');
    assert.equal(row.children, cells);
});
