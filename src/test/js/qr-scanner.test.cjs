const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const crypto = require('node:crypto');

const root = path.resolve(__dirname, '../../..');
const source = fs.readFileSync(path.join(root, 'src/main/resources/static/js/qr-scanner.js'), 'utf8');
const token = `UTEFITQR.${'A'.repeat(43)}`;

function element(properties = {}) {
    const listeners = new Map();
    return Object.assign({
        value: '', hidden: false, disabled: false, textContent: '',
        addEventListener(name, callback) { listeners.set(name, callback); },
        fire(name) { return listeners.get(name)?.(); }
    }, properties);
}

function harness(options = {}) {
    let stopped = 0;
    let requested = 0;
    let decoded = 0;
    const track = element({stop() { stopped += 1; }});
    const stream = {getTracks: () => [track], getVideoTracks: () => [track]};
    const video = element({
        hidden: true, readyState: 2, videoWidth: 1280, videoHeight: 720,
        pause() {}, async play() {}
    });
    const nodes = {
        'qr-camera-start': element(), 'qr-camera-stop': element({hidden: true}),
        'qr-camera': video, 'qr-camera-status': element(), 'qr-image': element(),
        'qr-checkin-form': element({elements: {branchId: element({value: '1'}), qrToken: element()}})
    };
    const canvas = {getContext: () => ({
        drawImage() {}, getImageData: () => ({data: new Uint8ClampedArray(4), width: 1, height: 1})
    })};
    const document = element({hidden: false, getElementById: id => nodes[id], createElement: () => canvas});
    const window = element({
        isSecureContext: options.secure !== false,
        jsQR: options.missingDecoder ? undefined : () => { decoded += 1; return options.decoded ? {data: options.decoded} : null; },
        BarcodeDetector: options.detector
    });
    const timers = new Set();
    const images = [];
    let revoked = 0;
    const context = {
        window, document, navigator: {mediaDevices: {getUserMedia: async constraints => {
            requested += 1;
            assert.equal(constraints.audio, false);
            assert.equal(constraints.video.facingMode.ideal, 'environment');
            if (options.error) throw {name: options.error};
            return options.acquire ? options.acquire(stream) : stream;
        }}},
        setTimeout(callback) { timers.add(callback); return callback; },
        clearTimeout(callback) { timers.delete(callback); },
        Image: function () { this.naturalWidth = 1280; this.naturalHeight = 720; images.push(this); },
        URL: {createObjectURL: () => 'blob:local-only', revokeObjectURL: () => { revoked += 1; }}
    };
    vm.runInNewContext(source, context);
    return {
        nodes, video, window, document, timers, images, track,
        stats: () => ({stopped, requested, decoded, revoked}),
        start: () => nodes['qr-camera-start'].fire('click'),
        stop: () => nodes['qr-camera-stop'].fire('click'),
        value: () => nodes['qr-checkin-form'].elements.qrToken.value,
        status: () => nodes['qr-camera-status'].textContent
    };
}

test('branch and HTTPS are required before requesting camera', async () => {
    const missingBranch = harness();
    missingBranch.nodes['qr-checkin-form'].elements.branchId.value = '';
    await missingBranch.start();
    assert.equal(missingBranch.stats().requested, 0);
    const insecure = harness({secure: false});
    await insecure.start();
    assert.equal(insecure.stats().requested, 0);
    assert.match(insecure.status(), /HTTPS/);
});

test('Safari without BarcodeDetector uses local decoder, stops tracks, never submits form', async () => {
    const page = harness({decoded: token});
    await page.start();
    assert.equal(page.value(), token);
    assert.equal(page.stats().decoded, 1);
    assert.equal(page.stats().stopped, 1);
    assert.equal(page.video.hidden, true);
    assert.equal(page.timers.size, 0);
});

test('Safari local decoder accepts distinct checkout prefix without submitting a client-selected action', async () => {
    const checkoutToken = `UTEFITQO.${'B'.repeat(43)}`;
    const page = harness({decoded: checkoutToken});
    await page.start();
    assert.equal(page.value(), checkoutToken);
    assert.equal(page.stats().stopped, 1);
    assert.match(page.status(), /backend xác định Check-in hay Check-out/);
});

test('unsupported native format falls back to local decoder', async () => {
    class Detector { static async getSupportedFormats() { return ['ean_13']; } }
    const page = harness({decoded: token, detector: Detector});
    await page.start();
    assert.equal(page.value(), token);
    assert.equal(page.stats().decoded, 1);
});

test('native decoder failure falls back without requesting a second camera', async () => {
    class Detector { async detect() { throw new Error('native unavailable'); } }
    const page = harness({decoded: token, detector: Detector});
    await page.start();
    assert.equal(page.value(), token);
    assert.equal(page.stats().requested, 1);
});

test('permission denial provides a recoverable message and restores controls', async () => {
    const page = harness({error: 'NotAllowedError'});
    await page.start();
    assert.match(page.status(), /bị từ chối/);
    assert.equal(page.nodes['qr-camera-start'].disabled, false);
    assert.equal(page.nodes['qr-camera-stop'].hidden, true);
});

test('stop during pending camera permission stops a late-arriving stream', async () => {
    let release;
    const page = harness({acquire: stream => new Promise(resolve => { release = () => resolve(stream); })});
    const pending = page.start();
    await Promise.resolve();
    page.stop();
    release();
    await pending;
    assert.equal(page.stats().stopped, 1);
    assert.equal(page.video.srcObject, null);
    assert.equal(page.timers.size, 0);
});

test('a late native decode after stop cannot fill the token', async () => {
    let release;
    class Detector { detect() { return new Promise(resolve => { release = () => resolve([{rawValue: token}]); }); } }
    const page = harness({detector: Detector});
    const pending = page.start();
    while (!release) await Promise.resolve();
    page.stop();
    release();
    await pending;
    assert.equal(page.value(), '');
    assert.equal(page.stats().stopped, 1);
});

test('foreign QR does not fill input and stop clears the polling timer', async () => {
    const page = harness({decoded: 'https://example.invalid/not-a-checkin-token'});
    await page.start();
    assert.equal(page.value(), '');
    assert.equal(page.timers.size, 1);
    page.stop();
    assert.equal(page.timers.size, 0);
    assert.equal(page.stats().stopped, 1);
});

test('tab hidden, page exit, submit, branch change and track interruption release the camera', async () => {
    for (const action of ['hidden', 'pagehide', 'submit', 'branch', 'ended']) {
        const page = harness();
        await page.start();
        if (action === 'hidden') { page.document.hidden = true; page.document.fire('visibilitychange'); }
        if (action === 'pagehide') page.window.fire('pagehide');
        if (action === 'submit') page.nodes['qr-checkin-form'].fire('submit');
        if (action === 'branch') page.nodes['qr-checkin-form'].elements.branchId.fire('change');
        if (action === 'ended') page.track.fire('ended');
        assert.equal(page.stats().stopped, 1, action);
        assert.equal(page.video.srcObject, null, action);
        assert.equal(page.timers.size, 0, action);
    }
});

test('image fallback decodes locally and revokes object URL', () => {
    const page = harness({decoded: token});
    page.nodes['qr-image'].files = [{type: 'image/png', size: 1024}];
    page.nodes['qr-image'].fire('change');
    page.images[0].onload();
    assert.equal(page.value(), token);
    assert.equal(page.stats().requested, 0);
    assert.equal(page.stats().revoked, 1);
});

test('oversized or non-image fallback is rejected without camera or decode', () => {
    for (const file of [{type: 'image/png', size: 11 * 1024 * 1024}, {type: 'application/pdf', size: 123}]) {
        const page = harness();
        page.nodes['qr-image'].files = [file];
        page.nodes['qr-image'].fire('change');
        assert.equal(page.images.length, 0);
        assert.equal(page.stats().requested, 0);
        assert.match(page.status(), /10 MB/);
    }
});

test('pinned jsQR artifact matches reviewed hash and can decode an empty frame safely', () => {
    const vendorPath = path.join(root, 'src/main/resources/static/vendor/jsqr/jsQR-1.4.0.js');
    const bytes = fs.readFileSync(vendorPath);
    assert.equal(crypto.createHash('sha256').update(bytes).digest('hex'), 'bc40c8a15196236b2314db0856f72ca0b49980cd5413b8c852a7349f5fee0859');
    const decode = require(vendorPath);
    assert.equal(decode(new Uint8ClampedArray(32 * 32 * 4), 32, 32), null);
});
