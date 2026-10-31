const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const script = fs.readFileSync('src/main/resources/static/js/utefit-branch-media.js', 'utf8');

function preview(complete, naturalWidth) {
    const message = { hidden: true };
    const caption = { hidden: false };
    const classes = new Set(['has-photo']);
    const media = {
        classList: { remove: value => classes.delete(value), add: value => classes.add(value) },
        querySelector: selector => selector === '[data-branch-caption]' ? caption : message
    };
    const handlers = {};
    const image = {
        complete, naturalWidth, hidden: false,
        closest: () => media,
        addEventListener: (name, handler) => { handlers[name] = handler; }
    };
    vm.runInNewContext(script, { document: { querySelectorAll: () => [image] } });
    return { image, message, caption, classes, handlers };
}

test('cached image failure is visible even when error happened before deferred script', () => {
    const { image, message, caption, classes } = preview(true, 0);
    assert.equal(image.hidden, true);
    assert.equal(message.hidden, false);
    assert.equal(message.textContent, 'Không tải được ảnh chi nhánh');
    assert.equal(caption.hidden, true);
    assert.equal(classes.has('has-photo'), false);
    assert.equal(classes.has('is-missing'), true);
});

test('late image failure replaces broken preview with an explicit error', () => {
    const { image, message, handlers } = preview(false, 0);
    assert.equal(message.hidden, true);
    handlers.error();
    assert.equal(image.hidden, true);
    assert.equal(message.hidden, false);
});

test('valid illustration stays visible with its caption and without an approval step', () => {
    const { image, message } = preview(true, 596);
    assert.equal(image.hidden, false);
    assert.equal(message.hidden, true);
});
