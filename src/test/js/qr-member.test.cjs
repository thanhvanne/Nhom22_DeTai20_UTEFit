const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/js/qr-member.js'), 'utf8');
const valid = {attendanceState: 'NONE', qrState: 'VALID', canIssue: true, expiresAt: 120000, serverTime: 0};
const flush = () => new Promise(resolve => setImmediate(resolve));

function element(properties = {}) {
    const events = new Map();
    return Object.assign({
        hidden: false, disabled: false, textContent: '', value: '',
        addEventListener(name, callback) { events.set(name, callback); },
        fire(name) { return events.get(name)?.(); }
    }, properties);
}

function harness(responses, initiallyReady = false) {
    const image = {removed: false, removeAttribute() { this.removed = true; }};
    const input = {value: 'test-token-not-a-real-credential'};
    const nodes = {
        'member-qr': element({dataset: {statusUrl: '/member/qr-checkin/status', tokenId: '30'}}),
        'member-checkin-status': element(), 'member-checkin-detail': element(),
        'member-qr-status': element(), 'member-qr-sync': element(),
        'member-qr-display': element({querySelector: selector => selector === 'input' ? input : image}),
        'member-qr-create': element({dataset: {canIssue: String(initiallyReady)}}), 'member-qr-form': element(),
        'member-qr-retry': element(),
        'member-qr-checkout-create': element({dataset: {canCheckout: 'false'}}),
        'member-qr-checkout-form': element()
    };
    const timeouts = new Map();
    const intervals = new Map();
    const requests = [];
    let now = 0;
    let timerId = 0;
    const document = element({hidden: false, getElementById: id => nodes[id]});
    const window = element({location: {href: 'https://utefit.test/member/qr-checkin'}});
    const queue = [...responses];
    const context = {
        document, window, URL, AbortController, performance: {now: () => now},
        setTimeout(callback, delay) { const id = ++timerId; timeouts.set(id, {callback, delay}); return id; },
        clearTimeout(id) { timeouts.delete(id); },
        setInterval(callback, delay) { const id = ++timerId; intervals.set(id, {callback, delay}); return id; },
        clearInterval(id) { intervals.delete(id); },
        async fetch(url, options) {
            requests.push({url, options});
            const next = queue.shift();
            if (next instanceof Error) throw next;
            if (typeof next === 'function') return next(options);
            return Object.assign({ok: true, status: 200, redirected: false,
                headers: {get: () => 'application/json'}, json: async () => next}, next?.response);
        }
    };
    vm.runInNewContext(source, context);
    return {
        nodes, input, image, intervals, timeouts, requests, document, window,
        advance(milliseconds) { now += milliseconds; for (const timer of [...intervals.values()]) timer.callback(); },
        async poll() {
            const pending = [...timeouts.entries()].find(([, timer]) => timer.delay === 4000);
            assert.ok(pending, 'next status poll must be scheduled');
            timeouts.delete(pending[0]);
            await pending[1].callback();
            await flush();
        }
    };
}

test('uses owned read-only status URL and countdown follows server clock, not device clock', async () => {
    const page = harness([valid]);
    await flush();
    assert.equal(page.requests[0].url, 'https://utefit.test/member/qr-checkin/status?tokenId=30');
    assert.equal(page.requests[0].options.credentials, 'same-origin');
    assert.equal(page.requests[0].options.cache, 'no-store');
    assert.equal(page.requests[0].options.method, undefined);
    assert.match(page.nodes['member-qr-status'].textContent, /120 giây/);
    assert.equal(page.nodes['member-qr-display'].hidden, false);
    page.advance(1000);
    assert.match(page.nodes['member-qr-status'].textContent, /119 giây/);
});

test('Admin check-in retires QR and timer; checkout enables creation but never restores consumed QR', async () => {
    const page = harness([valid,
        {...valid, attendanceState: 'OPEN', qrState: 'UNAVAILABLE', canIssue: false, checkinId: 40, branchName: 'Test', checkInAt: '2026-10-26T10:00:00'},
        {...valid, attendanceState: 'CHECKED_OUT', qrState: 'UNAVAILABLE', checkOutAt: '2026-10-26T11:00:00'}]);
    await flush();
    await page.poll();
    assert.match(page.nodes['member-checkin-status'].textContent, /Check-in thành công — Đang tập/);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
    assert.equal(page.nodes['member-qr-create'].disabled, true);
    assert.equal(page.input.value, '');
    assert.equal(page.image.removed, true);
    assert.equal(page.intervals.size, 0);
    const stoppedText = page.nodes['member-qr-status'].textContent;
    page.advance(1000);
    assert.equal(page.nodes['member-qr-status'].textContent, stoppedText);
    await page.poll();
    assert.match(page.nodes['member-checkin-status'].textContent, /Đã checkout/);
    assert.equal(page.nodes['member-qr-create'].disabled, false);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
    assert.equal(page.intervals.size, 0);
});

test('unused QR expires locally and stops countdown while status polling continues', async () => {
    const page = harness([valid]);
    await flush();
    page.advance(120000);
    assert.match(page.nodes['member-qr-status'].textContent, /QR hết hạn/);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
    assert.equal(page.intervals.size, 0);
    assert.equal(page.nodes['member-qr-create'].disabled, false);
    assert.ok([...page.timeouts.values()].some(timer => timer.delay === 4000));
});

test('network failure hides QR but preserves creation for last known ready state and exposes retry', async () => {
    const page = harness([valid, new Error('offline'), valid]);
    await flush();
    await page.poll();
    assert.equal(page.nodes['member-qr-display'].hidden, true);
    assert.equal(page.nodes['member-qr-create'].disabled, false);
    assert.equal(page.nodes['member-qr-retry'].disabled, false);
    assert.equal(page.intervals.size, 0);
    await page.poll();
    assert.equal(page.nodes['member-qr-display'].hidden, false);
    assert.equal(page.nodes['member-qr-create'].disabled, false);
});

test('login redirect or forbidden stops polling without displaying stale QR', async () => {
    for (const response of [{redirected: true}, {ok: false, status: 403}]) {
        const page = harness([{response}]);
        await flush();
        assert.equal(page.nodes['member-qr-display'].hidden, true);
        assert.equal(page.nodes['member-qr-create'].disabled, true);
        assert.equal(page.timeouts.size, 0);
        assert.match(page.nodes['member-qr-sync'].textContent, /đăng nhập/);
    }
});

test('unknown token and revoked token cannot show QR or claim successful check-in', async () => {
    const unknown = harness([{response: {ok: false, status: 404}}]);
    await flush();
    assert.equal(unknown.timeouts.size, 0);
    assert.equal(unknown.nodes['member-qr-create'].disabled, true);
    const revoked = harness([{...valid, qrState: 'UNAVAILABLE'}]);
    await flush();
    assert.equal(revoked.nodes['member-qr-display'].hidden, true);
    assert.doesNotMatch(revoked.nodes['member-checkin-status'].textContent, /thành công/);
    assert.equal(revoked.nodes['member-qr-create'].disabled, false);
});

test('background pauses timers and foreground synchronizes before showing QR again', async () => {
    const page = harness([valid, {...valid, attendanceState: 'OPEN', canIssue: false}]);
    await flush();
    page.document.hidden = true;
    page.document.fire('visibilitychange');
    assert.equal(page.timeouts.size, 0);
    assert.equal(page.intervals.size, 0);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
    page.document.hidden = false;
    page.document.fire('visibilitychange');
    await flush();
    assert.match(page.nodes['member-checkin-status'].textContent, /Đang tập/);
    assert.equal(page.nodes['member-qr-create'].disabled, true);
});

test('late response after pagehide is ignored; bfcache pageshow refreshes immediately', async () => {
    let release;
    const page = harness([() => new Promise(resolve => { release = resolve; }), {...valid, attendanceState: 'OPEN', canIssue: false}]);
    page.window.fire('pagehide');
    assert.equal(page.requests[0].options.signal.aborted, true);
    release({ok: true, status: 200, redirected: false, headers: {get: () => 'application/json'}, json: async () => valid});
    await flush();
    assert.equal(page.nodes['member-qr-display'].hidden, true);
    assert.equal(page.intervals.size, 0);
    page.window.fire('pageshow');
    await flush();
    assert.match(page.nodes['member-checkin-status'].textContent, /Đang tập/);
});

test('malformed status cannot enable a QR or creation', async () => {
    const page = harness([{attendanceState: 'OPEN', qrState: 'INVALID', canIssue: true}]);
    await flush();
    assert.equal(page.nodes['member-qr-create'].disabled, true);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
});

test('manual retry recovers from temporary API failure without hiding the functional area', async () => {
    const page = harness([new Error('offline'), {...valid, attendanceState: 'CHECKED_OUT', qrState: 'NONE'}]);
    await flush();
    assert.match(page.nodes['member-qr-sync'].textContent, /Thử lại/);
    assert.equal(page.nodes['member-qr-retry'].disabled, false);
    page.nodes['member-qr-retry'].fire('click');
    await flush();
    assert.match(page.nodes['member-checkin-status'].textContent, /Đã checkout/);
    assert.equal(page.nodes['member-qr-create'].disabled, false);
    assert.equal(page.nodes['member-qr-form'].hidden, false);
});

test('failure while exercising never enables another check-in', async () => {
    const page = harness([{...valid, attendanceState: 'OPEN', canIssue: false}, new Error('offline')]);
    await flush();
    await page.poll();
    assert.equal(page.nodes['member-qr-create'].disabled, true);
    assert.equal(page.nodes['member-qr-retry'].disabled, false);
    assert.match(page.nodes['member-checkin-status'].textContent, /Đang tập/);
});

test('retry after unknown token reads only own attendance without the unavailable token id', async () => {
    const page = harness([{response: {ok: false, status: 404}}, {...valid, qrState: 'NONE', attendanceState: 'CHECKED_OUT'}]);
    await flush();
    page.nodes['member-qr-retry'].fire('click');
    await flush();
    assert.equal(page.requests[1].url, 'https://utefit.test/member/qr-checkin/status');
    assert.equal(page.nodes['member-qr-create'].disabled, false);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
});

test('server-rendered checkout keeps create and retry available even when initial polling fails', async () => {
    const page = harness([new Error('offline')], true);
    await flush();
    assert.equal(page.nodes['member-qr-create'].disabled, false);
    assert.equal(page.nodes['member-qr-create'].hidden, false);
    assert.equal(page.nodes['member-qr-form'].hidden, false);
    assert.equal(page.nodes['member-qr-retry'].disabled, false);
});

test('open session shows separate checkout action without allowing another check-in', async () => {
    const page = harness([{...valid, attendanceState: 'OPEN', canIssue: false, canCheckout: true, qrPurpose: 'CHECK_IN', qrState: 'UNAVAILABLE'}]);
    await flush();
    assert.equal(page.nodes['member-qr-create'].disabled, true);
    assert.equal(page.nodes['member-qr-checkout-create'].disabled, false);
    assert.equal(page.nodes['member-qr-checkout-form'].hidden, false);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
});

test('valid checkout QR stays visible and counts down while exercising then retires after checkout', async () => {
    const page = harness([
        {...valid, attendanceState: 'OPEN', canIssue: false, canCheckout: true, qrPurpose: 'CHECK_OUT'},
        {...valid, attendanceState: 'CHECKED_OUT', canIssue: true, canCheckout: false, qrPurpose: 'CHECK_OUT', qrState: 'UNAVAILABLE'}
    ]);
    await flush();
    assert.equal(page.nodes['member-qr-display'].hidden, false);
    assert.match(page.nodes['member-qr-status'].textContent, /QR Check-out còn hiệu lực 120 giây/);
    assert.equal(page.nodes['member-qr-create'].disabled, true);
    await page.poll();
    assert.equal(page.nodes['member-qr-display'].hidden, true);
    assert.equal(page.intervals.size, 0);
    assert.match(page.nodes['member-checkin-status'].textContent, /Check-out thành công — Đã rời phòng tập/);
    assert.equal(page.nodes['member-qr-create'].disabled, false);
    assert.equal(page.nodes['member-qr-checkout-form'].hidden, true);
});

test('expired checkout QR can be regenerated only as checkout while session stays open', async () => {
    const page = harness([{...valid, attendanceState: 'OPEN', canIssue: false, canCheckout: true, qrPurpose: 'CHECK_OUT'}]);
    await flush();
    page.advance(120000);
    assert.match(page.nodes['member-qr-status'].textContent, /QR Check-out hết hạn/);
    assert.equal(page.nodes['member-qr-create'].disabled, true);
    assert.equal(page.nodes['member-qr-checkout-create'].disabled, false);
    assert.equal(page.nodes['member-qr-display'].hidden, true);
});

test('submitting checkout QR form stops polling and countdown without checking out locally', async () => {
    const page = harness([{...valid, attendanceState: 'OPEN', canIssue: false, canCheckout: true, qrPurpose: 'CHECK_OUT'}]);
    await flush();
    page.nodes['member-qr-checkout-form'].fire('submit');
    assert.equal(page.intervals.size, 0);
    assert.equal(page.timeouts.size, 0);
    assert.match(page.nodes['member-checkin-status'].textContent, /Đang tập/);
    assert.equal(page.requests.length, 1);
});
