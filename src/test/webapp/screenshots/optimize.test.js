const assert = require('node:assert/strict');
const { test } = require('node:test');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const sharp = require('sharp');
const { optimizeScreenshots, parseArguments } = require('./optimize');

// A complete two-frame, 1x1 RGBA APNG (red, then blue).
const ANIMATED_PNG = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACGFjVEwAAAACAAAAAPONk3AAAAAaZmNUTAAAAAAAAAABAAAAAQAAAAAAAAAAAAEACgAAWn8w0AAAAA1JREFUeJxj+M/A8B8ABQAB/4mZPR0AAAAaZmNUTAAAAAEAAAABAAAAAQAAAAAAAAAAAAEACgAAwQzaBAAAABFmZEFUAAAAAnicY2Bg+P8fAAMCAf/1e6XXAAAAAElFTkSuQmCC',
    'base64'
);

async function directory(t) {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), 'webjet-screenshot-test-'));
    t.after(() => fs.rm(root, { recursive: true, force: true }));
    return root;
}

/** Generate an opaque image with color detail and an optional alpha channel. */
async function pngFixture(channels = 3) {
    const width = 96;
    const height = 64;
    const pixels = Buffer.alloc(width * height * channels);
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const offset = (y * width + x) * channels;
            pixels[offset] = x * 2;
            pixels[offset + 1] = y * 3;
            pixels[offset + 2] = (x + y) % 256;
            if (channels === 4) pixels[offset + 3] = 255;
        }
    }
    return sharp(pixels, { raw: { width, height, channels } }).png({ compressionLevel: 0 }).toBuffer();
}

test('converts nested and localized PNG screenshots, preserves dimensions and skips other files', async t => {
    const root = await directory(t);
    const components = path.join(root, 'components');
    const apps = path.join(root, 'apps', 'nested');
    await fs.mkdir(components);
    await fs.mkdir(apps, { recursive: true });
    const png = await pngFixture();
    const jpeg = await sharp(png).jpeg().toBuffer();
    const converted = [path.join(components, 'screenshot-1.jpg'), path.join(apps, 'screenshot-2-cs.JPEG')];
    await fs.writeFile(converted[0], png);
    await fs.writeFile(converted[1], await pngFixture(4));
    const untouched = new Map([
        [path.join(components, 'screenshot-existing.jpg'), jpeg],
        [path.join(components, 'screenshot-real.png'), png],
        [path.join(components, 'photograph.jpg'), png],
        [path.join(root, 'screenshot-outside.jpg'), png]
    ]);
    for (const [filename, content] of untouched) await fs.writeFile(filename, content);

    const roots = [components, path.dirname(apps)];
    const first = await optimizeScreenshots(roots, { log: () => {} });
    assert.equal(first.converted, 2);
    assert.equal(first.skipped, 1);
    assert.equal(first.errors, 0);
    assert.ok(first.after < first.before);
    for (const filename of converted) {
        const metadata = await sharp(filename).metadata();
        assert.equal(metadata.format, 'jpeg');
        assert.equal(metadata.width, 96);
        assert.equal(metadata.height, 64);
        untouched.set(filename, await fs.readFile(filename));
    }

    const second = await optimizeScreenshots(roots, { log: () => {} });
    assert.equal(second.converted, 0);
    assert.equal(second.skipped, 3);
    for (const [filename, content] of untouched) assert.deepEqual(await fs.readFile(filename), content);
    assert.deepEqual((await fs.readdir(components)).sort(), [
        'photograph.jpg', 'screenshot-1.jpg', 'screenshot-existing.jpg', 'screenshot-real.png'
    ]);
});

test('dry-run predicts exact savings without writing files', async t => {
    const root = await directory(t);
    const filename = path.join(root, 'screenshot-1.jpg');
    const png = await pngFixture();
    await fs.writeFile(filename, png);
    const predicted = await optimizeScreenshots([root], { dryRun: true, log: () => {} });
    assert.equal(predicted.converted, 1);
    assert.deepEqual(await fs.readFile(filename), png);
    assert.deepEqual(await fs.readdir(root), ['screenshot-1.jpg']);
    const actual = await optimizeScreenshots([root], { log: () => {} });
    assert.deepEqual(actual, predicted);
});

test('keeps transparent and animated PNG files unchanged', async t => {
    const root = await directory(t);
    const transparent = await sharp({
        create: { width: 96, height: 64, channels: 4, background: { r: 255, g: 255, b: 255, alpha: 0.5 } }
    }).png().toBuffer();
    await fs.writeFile(path.join(root, 'screenshot-transparent.jpg'), transparent);
    await fs.writeFile(path.join(root, 'screenshot-animated.jpg'), ANIMATED_PNG);
    const messages = [];
    const summary = await optimizeScreenshots([root], { log: message => messages.push(message) });
    assert.equal(summary.converted, 0);
    assert.equal(summary.skipped, 2);
    assert.equal(summary.errors, 0);
    assert.ok(messages.some(message => message.includes('transparent pixels')));
    assert.ok(messages.some(message => message.includes('animated PNG')));
    assert.deepEqual(await fs.readFile(path.join(root, 'screenshot-transparent.jpg')), transparent);
    assert.deepEqual(await fs.readFile(path.join(root, 'screenshot-animated.jpg')), ANIMATED_PNG);
});

test('keeps PNG when a JPEG would be larger', async t => {
    const root = await directory(t);
    const filename = path.join(root, 'screenshot-tiny.jpg');
    const input = await sharp({ create: { width: 1, height: 1, channels: 3, background: 'white' } }).png().toBuffer();
    await fs.writeFile(filename, input);
    const messages = [];
    const summary = await optimizeScreenshots([root], { log: message => messages.push(message) });
    assert.equal(summary.converted, 0);
    assert.equal(summary.skipped, 1);
    assert.ok(messages.some(message => message.includes('JPEG would not be smaller')));
    assert.deepEqual(await fs.readFile(filename), input);
});

test('reports a damaged PNG and continues converting later files', async t => {
    const root = await directory(t);
    const damaged = (await pngFixture()).subarray(0, 40);
    await fs.writeFile(path.join(root, 'screenshot-1-broken.jpg'), damaged);
    await fs.writeFile(path.join(root, 'screenshot-2-valid.jpg'), await pngFixture());
    const messages = [];
    const summary = await optimizeScreenshots([root], { log: message => messages.push(message) });
    assert.equal(summary.errors, 1);
    assert.equal(summary.converted, 1);
    assert.ok(messages.some(message => message.startsWith('ERROR ')));
    assert.deepEqual(await fs.readFile(path.join(root, 'screenshot-1-broken.jpg')), damaged);
    assert.deepEqual((await fs.readdir(root)).sort(), ['screenshot-1-broken.jpg', 'screenshot-2-valid.jpg']);
});

test('validates quality and command-line arguments before modifying files', () => {
    assert.deepEqual(parseArguments([]), { quality: 90, dryRun: false });
    assert.deepEqual(parseArguments(['--dry-run', '--quality', '85']), { quality: 85, dryRun: true });
    for (const value of ['0', '101', '85.5', 'NaN', '', '-1']) {
        assert.throws(() => parseArguments(['--quality', value]), /integer from 1 to 100/);
    }
    assert.throws(() => parseArguments(['--quality']), /integer from 1 to 100/);
    assert.throws(() => parseArguments(['--unknown']), /Unknown argument/);
    const command = spawnSync(process.execPath, [path.join(__dirname, 'optimize.js'), '--quality', '101'], {
        cwd: os.tmpdir(), encoding: 'utf8'
    });
    assert.equal(command.status, 1);
    assert.match(command.stderr, /integer from 1 to 100/);
});
