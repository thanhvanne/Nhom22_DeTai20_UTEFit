const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/js/sepay-checkout.js'), 'utf8');

async function harness(state, failed = false, renderedState = 'PENDING') {
    const nodes = Object.fromEntries(['sepay-message', 'sepay-payment-status', 'sepay-membership-status', 'sepay-refresh', 'sepay-qr-image', 'sepay-expiry']
        .map(id => [id, {textContent: '', hidden: false, addEventListener() {}}]));
    nodes['sepay-checkout'] = {dataset: {statusUrl: '/owned/status', expiresAt: String(Date.now() + 60000), displayState: renderedState,
        membershipStatus: renderedState === 'PAID' ? 'ACTIVE' : 'PENDING', periodState: renderedState === 'PAID' ? 'CURRENT' : 'PENDING'}};
    nodes['sepay-transfer'] = {hidden: false};
    nodes['sepay-payment-status'].textContent = renderedState;
    if (renderedState !== 'PENDING') { delete nodes['sepay-expiry']; delete nodes['sepay-qr-image']; }
    const requests = []; const timers = []; const reloads = []; const intervals = [];
    vm.runInNewContext(source, {
        document: {hidden: false, getElementById: id => nodes[id], querySelectorAll: () => [], addEventListener() {}},
        window: {addEventListener() {}, location: {reload() { reloads.push(true); }}},
        setInterval() { intervals.push(true); }, clearInterval() {}, clearTimeout() {},
        setTimeout(callback, delay) { timers.push(delay); },
        fetch: async (url, options) => { requests.push({url, options}); if (failed) throw new Error('offline');
            return {ok: true, redirected: false, headers: {get: () => 'application/json'}, json: async () => state}; }
    });
    await new Promise(resolve => setImmediate(resolve));
    return {nodes, requests, timers, reloads, intervals};
}

test('verified paid state removes transfer layout and reloads server screen; polling only reads', async () => {
    const page = await harness({paymentStatus: 'PAID', membershipStatus: 'ACTIVE', expired: false, displayState: 'PAID', periodState: 'CURRENT'});
    assert.equal(page.nodes['sepay-qr-image'].hidden, true);
    assert.equal(page.nodes['sepay-transfer'].hidden, true);
    assert.equal(page.reloads.length, 1);
    assert.equal(page.requests[0].options.method, undefined);
    assert.equal(page.requests[0].options.cache, 'no-store');
    assert.equal(page.timers.length, 0);
});
test('review state warns not to transfer again and keeps read-only polling', async () => {
    const page = await harness({paymentStatus: 'PENDING', membershipStatus: 'PENDING', reconciliationRequired: true, displayState: 'REVIEW', periodState: 'PENDING'}, false, 'REVIEW');
    assert.match(page.nodes['sepay-message'].textContent, /đối soát/);
    assert.deepEqual(page.timers, [5000]);
});
test('expired pending order still polls for delayed receipts without showing QR', async () => {
    const page = await harness({paymentStatus: 'PENDING', membershipStatus: 'PENDING', expired: true, displayState: 'EXPIRED', periodState: 'PENDING'}, false, 'EXPIRED');
    assert.deepEqual(page.timers, [15000]);
});
test('paid reload has no countdown or reload loop', async () => {
    const page = await harness({paymentStatus: 'PAID', membershipStatus: 'ACTIVE', displayState: 'PAID', periodState: 'CURRENT'}, false, 'PAID');
    assert.equal(page.reloads.length, 0);
    assert.equal(page.intervals.length, 0);
    assert.deepEqual(page.timers, [15000]);
});
test('paid future period refreshes actual backend summary', async () => {
    const page = await harness({paymentStatus: 'PAID', membershipStatus: 'ACTIVE', displayState: 'PAID', periodState: 'FUTURE'}, false, 'PAID');
    assert.equal(page.reloads.length, 1);
});
test('refund replaces paid screen with server-rendered outcome', async () => {
    const page = await harness({paymentStatus: 'REFUNDED', membershipStatus: 'CANCELLED', displayState: 'REFUNDED', periodState: 'CANCELLED'}, false, 'PAID');
    assert.equal(page.reloads.length, 1);
});
test('cancellation request replaces QR with backend-rendered screen', async () => {
    const page = await harness({paymentStatus: 'CANCEL_REQUESTED', membershipStatus: 'PENDING', displayState: 'CANCEL_REQUESTED', periodState: 'PENDING'});
    assert.equal(page.reloads.length, 1);
    assert.equal(page.nodes['sepay-qr-image'].hidden, true);
});

test('cancelled order never starts a countdown or marks payment paid', async () => {
    const page = await harness({paymentStatus: 'CANCELLED', membershipStatus: 'PENDING', displayState: 'CANCELLED', periodState: 'PENDING'}, false, 'CANCELLED');
    assert.equal(page.intervals.length, 0);
    assert.equal(page.nodes['sepay-qr-image'], undefined);
    assert.equal(page.nodes['sepay-payment-status'].textContent, 'Đã hủy đơn chờ');
    assert.deepEqual(page.timers, [15000]);
});

test('invalid response cannot mark payment paid', async () => {
    const page = await harness({paymentStatus: 'PAID'});
    assert.equal(page.reloads.length, 0);
    assert.equal(page.nodes['sepay-payment-status'].textContent, 'PENDING');
    assert.match(page.nodes['sepay-message'].textContent, /kiểm tra lại/);
});
test('polling error gives retry instructions, never marks payment paid', async () => {
    const page = await harness({}, true);
    assert.match(page.nodes['sepay-message'].textContent, /kiểm tra lại/);
    assert.equal(page.nodes['sepay-payment-status'].textContent, 'PENDING');
});
