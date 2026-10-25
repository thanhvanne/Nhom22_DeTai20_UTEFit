const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/js/reviews.js'), 'utf8');
const maxBytes = 50 * 1024 * 1024;

function harness() {
    const events = new Map();
    const form = {addEventListener(name, callback) { events.set(`form:${name}`, callback); }};
    const error = {textContent: ''};
    const video = {
        dataset: {maxBytes: String(maxBytes)}, files: [], validity: '', reported: false,
        closest: () => form,
        setCustomValidity(message) { this.validity = message; },
        reportValidity() { this.reported = true; },
        addEventListener(name, callback) { events.set(`video:${name}`, callback); }
    };
    const nodes = {video, 'review-video-error': error, branchId: {disabled: true}, trainerId: {disabled: true}};
    vm.runInNewContext(source, {document: {getElementById: id => nodes[id]}});
    return {
        video, error,
        select(file) { video.files = file ? [file] : []; events.get('video:change')(); },
        submit() { let prevented = false; events.get('form:submit')({preventDefault() { prevented = true; }}); return prevented; }
    };
}

test('under and exactly fifty MiB are allowed even on edit forms with disabled target fields', () => {
    for (const size of [maxBytes - 1, maxBytes]) {
        const page = harness();
        page.select({size, type: 'video/mp4', name: 'experience.mp4'});
        assert.equal(page.video.validity, '');
        assert.equal(page.submit(), false);
    }
});

test('one byte over fifty MiB is rejected with explicit message and submission prevented', () => {
    const page = harness();
    page.select({size: maxBytes + 1, type: 'video/mp4', name: 'large.mp4'});
    assert.match(page.error.textContent, /50 MiB/);
    assert.equal(page.submit(), true);
    assert.equal(page.video.reported, true);
});

test('mime or extension mismatches are rejected, WebM remains supported', () => {
    const page = harness();
    for (const file of [{size: 1, type: 'image/png', name: 'renamed.mp4'}, {size: 1, type: 'video/mp4', name: 'file.exe'}]) {
        page.select(file);
        assert.match(page.video.validity, /MP4\/WebM/);
        assert.equal(page.submit(), true);
    }
    page.select({size: maxBytes, type: 'video/webm', name: 'experience.webm'});
    assert.equal(page.video.validity, '');
    assert.equal(page.error.textContent, '');
});

test('clearing optional video resets previous size error', () => {
    const page = harness();
    page.select({size: maxBytes + 1, type: 'video/mp4', name: 'large.mp4'});
    page.select(null);
    assert.equal(page.video.validity, '');
    assert.equal(page.submit(), false);
});
