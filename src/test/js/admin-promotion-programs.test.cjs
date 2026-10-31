const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/resources/static/js/admin-promotion-programs.js', 'utf8');

function setup({ active = 'true', pause = false, confirm = true, valid = true } = {}) {
    const listeners = {};
    const windowListeners = {};
    const attributes = {};
    const button = { disabled: false, textContent: 'Lưu chương trình', dataset: {} };
    let confirmation = '';
    const form = {
        dataset: { confirmPause: String(pause), programName: 'WELCOME' },
        addEventListener: (name, callback) => { listeners[name] = callback; },
        getAttribute: name => attributes[name],
        setAttribute: (name, value) => { attributes[name] = value; },
        removeAttribute: name => { delete attributes[name]; },
        checkValidity: () => valid,
        reportValidity: () => {},
        querySelector: () => ({ value: active }),
        querySelectorAll: selector => selector === '[data-program-submit]' ? (button.dataset.programSubmit ? [button] : []) : [button]
    };
    vm.runInNewContext(source, {
        document: { querySelectorAll: () => [form], querySelector: () => null },
        window: { addEventListener: (name, callback) => { windowListeners[name] = callback; }, confirm: message => { confirmation = message; return confirm; } }
    });
    return { button, attributes, windowListeners, confirmation: () => confirmation, submit: () => {
        const event = { prevented: false, preventDefault() { this.prevented = true; } };
        listeners.submit(event); return event;
    } };
}

test('program save shows loading and blocks repeated submits without changing any form value', () => {
    const fixture = setup();
    assert.equal(fixture.submit().prevented, false);
    assert.equal(fixture.attributes['aria-busy'], 'true');
    assert.equal(fixture.button.disabled, true);
    assert.equal(fixture.button.textContent, 'Đang lưu…');
    assert.equal(fixture.submit().prevented, true);
    fixture.windowListeners.pageshow();
    assert.equal(fixture.button.disabled, false);
    assert.equal(fixture.button.textContent, 'Lưu chương trình');
    assert.equal(fixture.attributes['aria-busy'], undefined);
});

test('pausing asks for confirmation and cancelled confirmation leaves submission usable', () => {
    const fixture = setup({ pause: true, active: 'false', confirm: false });
    assert.equal(fixture.submit().prevented, true);
    assert.match(fixture.confirmation(), /Tạm dừng WELCOME/);
    assert.equal(fixture.button.disabled, false);
    assert.equal(fixture.attributes['aria-busy'], undefined);
});

test('invalid native form never enters loading state and activating never asks to pause', () => {
    const invalid = setup({ valid: false });
    assert.equal(invalid.submit().prevented, true);
    assert.equal(invalid.button.disabled, false);
    const enabled = setup({ pause: true, active: 'true' });
    assert.equal(enabled.submit().prevented, false);
    assert.equal(enabled.confirmation(), '');
});
