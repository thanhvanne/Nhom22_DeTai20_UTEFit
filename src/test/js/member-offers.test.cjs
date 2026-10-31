const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const flush = () => new Promise(resolve => setImmediate(resolve));

function element() {
    const listeners = new Map();
    return { hidden: false, textContent: '', disabled: false,
        addEventListener(name, listener) { listeners.set(name, listener); },
        emit(name, event) { return listeners.get(name)?.(event); }
    };
}
function response(body, ok = true) { return { ok, headers: { get: () => 'application/json' }, json: async () => body }; }
const valid = { originalPrice: 500000, discount: 50000, finalPrice: 450000, name: 'Chào mừng' };
function mount(fetcher) {
    const controls = new Map(['[data-offer-quote]', '[data-offer-error]', '[type=submit]', '[data-offer-retry]', '[data-offer-name]', '[data-offer-original]', '[data-offer-discount]', '[data-offer-total]'].map(selector => [selector, element()]));
    const values = { branchId: '1', planId: '2', promotionCode: '', offerSelection: 'AUTO', _csrf: 'must-not-be-in-url', amount: '1' };
    const form = element();
    form.querySelector = selector => controls.get(selector);
    form.elements = { namedItem: name => ({ value: values[name] }) };
    const window = element();
    const timers = new Map();
    let timerId = 0;
    window.setTimeout = callback => { timers.set(++timerId, callback); return timerId; };
    window.clearTimeout = id => timers.delete(id);
    const requests = [];
    const fetch = (url, options) => { requests.push({ url, options }); return fetcher(url, options); };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/member-offers.js', 'utf8'), { window, document: { querySelectorAll: () => [form] }, fetch, Intl, URLSearchParams, AbortController });
    return { form, controls, values, requests, window, flushTimer() { const callbacks = [...timers.values()]; timers.clear(); callbacks.forEach(callback => callback()); } };
}

test('quote requests only server-priced identifiers, never frontend amount or CSRF in URL', async () => {
    const page = mount(async () => response(valid));
    await flush();
    assert.match(page.requests[0].url, /branchId=1&planId=2&promotionCode=&offerSelection=AUTO/);
    assert.doesNotMatch(page.requests[0].url, /csrf|amount|quotedPrice/);
    assert.equal(page.requests[0].options.method, undefined);
    assert.equal(page.controls.get('[type=submit]').disabled, false);
    assert.equal(page.controls.get('[data-offer-name]').textContent, 'Chào mừng');
    assert.equal(page.controls.get('[data-offer-quote]').hidden, false);
});

test('expired/exhausted code errors block submit and retry can recover', async () => {
    let failure = true;
    const page = mount(async () => failure ? response({ error: 'Mã đã hết lượt' }, false) : response(valid));
    await flush();
    assert.equal(page.controls.get('[type=submit]').disabled, true);
    assert.equal(page.controls.get('[data-offer-error]').textContent, 'Mã đã hết lượt');
    assert.equal(page.controls.get('[data-offer-retry]').hidden, false);
    failure = false;
    await page.controls.get('[data-offer-retry]').emit('click');
    assert.equal(page.controls.get('[type=submit]').disabled, false);
});

test('an aborted stale quote cannot re-enable buying or overwrite the latest chosen code', async () => {
    const pending = [];
    const page = mount(() => new Promise(resolve => pending.push(resolve)));
    page.values.promotionCode = 'NEWCODE';
    page.form.emit('input');
    pending[0](response(valid));
    await flush();
    assert.equal(page.controls.get('[type=submit]').disabled, true);
    assert.equal(page.controls.get('[data-offer-quote]').hidden, true);
    page.flushTimer();
    pending[1](response({ ...valid, name: 'Mã NEWCODE', finalPrice: 490000, discount: 10000 }));
    await flush();
    assert.equal(page.controls.get('[data-offer-name]').textContent, 'Mã NEWCODE');
    assert.equal(page.controls.get('[type=submit]').disabled, false);
    assert.match(page.requests[1].url, /promotionCode=NEWCODE/);
});

test('invalid/free responses and login redirects never enable a payment action', async () => {
    const free = mount(async () => response({ ...valid, finalPrice: 0 })); await flush();
    assert.equal(free.controls.get('[type=submit]').disabled, true);
    const login = mount(async () => ({ redirected: true, headers: { get: () => 'text/html' } })); await flush();
    assert.equal(login.controls.get('[type=submit]').disabled, true);
    assert.match(login.controls.get('[data-offer-error]').textContent, /đăng nhập/);
});
