const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { createRankMap, rankAt, phaseAt, layoutFor, createField, pointAt } = require('../../main/resources/static/js/utefit-public-surface.js');

const sourceRanks = createRankMap(64);
const targetRanks = createRankMap(64, true);
const samples = Array.from({ length: 80 }, (_, index) => [(index % 10 + .5) / 10, (Math.floor(index / 10) + .5) / 8]);
const makeField = (count = 80) => createField(samples, samples.slice().reverse(), count, sourceRanks, targetRanks);

test('solid endpoints have full image surfaces and absolutely no particle contribution', () => {
    for (const rank of [.025, .2, .5, .8, .975]) {
        assert.deepEqual([phaseAt(0, rank, rank).dissolve, phaseAt(0, rank, rank).assemble, phaseAt(0, rank, rank).opacity], [0, 0, 0]);
        assert.deepEqual([phaseAt(1, rank, rank).dissolve, phaseAt(1, rank, rank).assemble, phaseAt(1, rank, rank).opacity], [1, 1, 0]);
    }
});

test('five acceptance phases distinguish solid, partial erosion, cloud and solid reconstruction', () => {
    const phases = [0, .25, .5, .75, 1].map((progress) => phaseAt(progress));
    assert.ok(phases[1].dissolve > 0 && phases[1].dissolve < 1);
    assert.equal(phases[1].assemble, 0);
    assert.deepEqual([phases[2].dissolve, phases[2].assemble, phases[2].opacity], [1, 0, 1]);
    assert.ok(phases[3].assemble > 0 && phases[3].assemble < 1);
    assert.equal(phases[4].opacity, 0);
});

test('surface removal and particle birth share the exact same spatial rank', () => {
    for (const sourceRank of [.1, .3, .7, .9]) {
        const birth = phaseAt(.25, sourceRank).birth;
        assert.ok(Math.abs(phaseAt(birth, sourceRank).dissolve - sourceRank) < 1e-10);
        assert.equal(phaseAt(birth - .02, sourceRank).opacity, 0);
    }
});

test('particle arrival and target surface restoration share the same spatial rank', () => {
    for (const targetRank of [.1, .3, .7, .9]) {
        const arrival = phaseAt(.75, .5, targetRank).arrival;
        assert.ok(Math.abs(phaseAt(arrival, .5, targetRank).assemble - targetRank) < 1e-10);
        assert.equal(phaseAt(arrival + .02, .5, targetRank).opacity, 0);
    }
});

test('rank maps are deterministic, directional and locally irregular, not global opacity', () => {
    assert.deepEqual(createRankMap(64), sourceRanks);
    assert.notDeepEqual(targetRanks, sourceRanks);
    assert.ok(rankAt(sourceRanks, .1, .5) > rankAt(sourceRanks, .9, .5));
    assert.notEqual(rankAt(sourceRanks, .5, .2), rankAt(sourceRanks, .5, .8));
    assert.ok([...sourceRanks.pixels.filter((_, index) => index % 4 === 0)].every((value) => value > 0 && value < 255));
});

test('image samples and seeded attributes are stable across particle budgets', () => {
    const small = makeField(20);
    const large = makeField(200);
    assert.deepEqual(small.vertices, large.vertices.slice(0, 160));
    assert.deepEqual(small.timings, large.timings.slice(0, 40));
    assert.deepEqual(small, makeField(20));
    for (let index = 0; index < small.count; index++) {
        const horizontal = small.vertices[index * 8];
        const vertical = small.vertices[index * 8 + 1];
        assert.ok(samples.some((sample) => Math.abs(sample[0] - horizontal) < 1e-6 && Math.abs(sample[1] - vertical) < 1e-6));
    }
});

test('forward and reverse trajectories are exactly identical and use real depth', () => {
    const field = makeField();
    const layout = layoutFor(1440, 912);
    const steps = [0, .25, .5, .75, 1];
    const forward = steps.map((progress) => Array.from({ length: field.count }, (_, index) => pointAt(field, index, progress, layout)));
    steps.slice().reverse().forEach((progress, step) => {
        assert.deepEqual(Array.from({ length: field.count }, (_, index) => pointAt(field, index, progress, layout)), forward[steps.length - step - 1]);
    });
    assert.ok(forward[2].some((point) => Math.abs(point.depth) > 100));
});

test('responsive layouts retain complete endpoint rectangles and finite flight positions', () => {
    const field = makeField(20);
    for (const [width, height] of [[1440, 912], [768, 948], [390, 772]]) {
        const layout = layoutFor(width, height);
        for (const rectangle of [layout.source, layout.target]) {
            assert.ok(rectangle[0] >= 0 && rectangle[0] + rectangle[2] <= width);
            assert.ok(rectangle[1] >= 0 && rectangle[1] + rectangle[3] <= height);
        }
        for (const progress of [-1, 0, .25, .5, .75, 1, 2]) {
            for (let index = 0; index < field.count; index++) {
                const point = pointAt(field, index, progress, layout);
                assert.ok([point.x, point.y, point.depth, point.opacity].every(Number.isFinite));
            }
        }
    }
});

test('missing image samples fail before enhancement rather than creating a fake primitive object', () => {
    assert.throws(() => createField([], samples, 20, sourceRanks, targetRanks), /Missing opaque image samples/);
});

test('photoreal deployment assets are local WebP with a separate lightweight mobile pair', () => {
    const sizes = {};
    for (const object of ['barbell', 'kettlebell']) {
        for (const variant of ['', '-mobile']) {
            const name = `cinema-${object}-v2${variant}.webp`;
            const bytes = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/images', name));
            assert.equal(bytes.toString('ascii', 0, 4), 'RIFF');
            assert.equal(bytes.toString('ascii', 8, 12), 'WEBP');
            sizes[name] = bytes.length;
        }
        assert.ok(sizes[`cinema-${object}-v2-mobile.webp`] < sizes[`cinema-${object}-v2.webp`]);
    }
    assert.ok(sizes['cinema-barbell-v2-mobile.webp'] + sizes['cinema-kettlebell-v2-mobile.webp'] < 130000);
});

test('all static fallback posters use full raster objects rather than old vectors or particle canvases', () => {
    const template = fs.readFileSync(path.resolve(__dirname, '../../main/resources/templates/fragments/public-cinematic.html'), 'utf8');
    assert.equal((template.match(/<picture>/g) || []).length, 2);
    assert.equal((template.match(/<source media="\(max-width: 600px\)"/g) || []).length, 2);
    assert.ok(template.includes('cinema-barbell-v2.webp') && template.includes('cinema-kettlebell-v2.webp'));
    assert.ok(!template.includes('public-barbell.svg') && !template.includes('public-kettlebell.svg'));
});
