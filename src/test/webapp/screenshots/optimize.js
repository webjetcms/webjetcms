const fs = require('node:fs/promises');
const path = require('node:path');
const { randomUUID } = require('node:crypto');
const sharp = require('sharp');

const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
const JPEG_SIGNATURE = Buffer.from([255, 216, 255]);
const DEFAULT_QUALITY = 90;
const DEFAULT_MAX_WIDTH = 760;

/** Find JPEG-named images without following symbolic links. */
async function* findImages(directory, screenshotsOnly) {
    const pattern = screenshotsOnly ? /^screenshot.*\.jpe?g$/i : /\.jpe?g$/i;
    const entries = await fs.readdir(directory, { withFileTypes: true });
    entries.sort((a, b) => a.name.localeCompare(b.name));
    for (const entry of entries) {
        const filename = path.join(directory, entry.name);
        if (entry.isDirectory()) yield* findImages(filename, screenshotsOnly);
        else if (entry.isFile() && pattern.test(entry.name)) yield filename;
    }
}

/** Detect APNG even when the image decoder only exposes its first frame. */
function isAnimatedPng(input) {
    for (let offset = 8; offset + 12 <= input.length;) {
        const length = input.readUInt32BE(offset);
        if (input.toString('ascii', offset + 4, offset + 8) === 'acTL') return true;
        offset += length + 12;
    }
    return false;
}

/** Convert PNG content or resize an oversized JPEG only when the result saves space. */
async function optimizeFile(filename, { quality, maxWidth, dryRun }) {
    const input = await fs.readFile(filename);
    const isPng = input.subarray(0, 8).equals(PNG_SIGNATURE);
    if (!isPng && !input.subarray(0, 3).equals(JPEG_SIGNATURE)) {
        return { status: 'skipped', reason: 'not PNG or JPEG content' };
    }
    if (isPng && isAnimatedPng(input)) return { status: 'skipped', reason: 'animated PNG' };

    const image = sharp(input);
    const metadata = await image.metadata();
    const { width, height } = metadata.autoOrient;
    const resized = width > maxWidth;
    if (!isPng && !resized) {
        return { status: 'skipped', reason: 'JPEG already within maximum width' };
    }
    if (metadata.hasAlpha && !(await image.stats()).isOpaque) {
        return { status: 'skipped', reason: 'transparent pixels' };
    }

    image.autoOrient();
    // Avoid rounding the scaled height down when JPEG decoding also shrinks the image.
    if (resized) image.resize({ width: maxWidth, withoutEnlargement: true, fastShrinkOnLoad: false });
    const { data: output, info } = await image.jpeg({
        quality,
        chromaSubsampling: '4:4:4',
        mozjpeg: true
    }).toBuffer({ resolveWithObject: true });
    if (output.length >= input.length) {
        return { status: 'skipped', reason: 'JPEG would not be smaller' };
    }

    if (!dryRun) {
        const temporary = path.join(path.dirname(filename), `.${path.basename(filename)}.${randomUUID()}.tmp`);
        const { mode } = await fs.stat(filename);
        try {
            await fs.writeFile(temporary, output, { flag: 'wx', mode });
            await fs.rename(temporary, filename);
        } finally {
            await fs.rm(temporary, { force: true });
        }
    }
    return {
        status: 'converted', before: input.length, after: output.length, resized,
        dimensions: `${width}x${height} -> ${info.width}x${info.height} px`
    };
}

function formatBytes(bytes) {
    return `${bytes.toLocaleString('en-US')} bytes (${(bytes / 1_000_000).toFixed(3)} MB)`;
}

/**
 * Optimize JPEG-named images under the supplied roots and report individual failures.
 * @param {string[]} roots Directories to scan recursively.
 * @param {object} [options] JPEG quality, maximum width, screenshot filter, dry-run mode and output callback.
 * @returns {Promise<object>} Counts and byte totals for converted files.
 */
async function optimizeScreenshots(roots, { quality = DEFAULT_QUALITY, maxWidth = DEFAULT_MAX_WIDTH, screenshotsOnly = true, dryRun = false, log = console.log } = {}) {
    if (!Number.isInteger(quality) || quality < 1 || quality > 100) {
        throw new Error('Quality must be an integer from 1 to 100.');
    }
    if (!Number.isSafeInteger(maxWidth) || maxWidth < 1) {
        throw new Error('Maximum width must be a positive integer.');
    }

    const summary = { converted: 0, resized: 0, skipped: 0, errors: 0, before: 0, after: 0 };
    log(`${dryRun ? 'Dry run' : 'Optimization'}: JPEG quality ${quality}, maximum width ${maxWidth} px, 4:4:4 chroma.`);
    for (const root of roots) {
        try {
            log(`Scanning ${root}: ${screenshotsOnly ? 'screenshot*.jpg/jpeg' : 'all .jpg/.jpeg files'}.`);
            for await (const filename of findImages(root, screenshotsOnly)) {
                const label = path.join(path.basename(root), path.relative(root, filename));
                try {
                    const result = await optimizeFile(filename, { quality, maxWidth, dryRun });
                    summary[result.status]++;
                    if (result.status === 'converted') {
                        summary.before += result.before;
                        summary.after += result.after;
                        if (result.resized) summary.resized++;
                        log(`${dryRun ? 'WOULD CONVERT' : 'CONVERTED'} ${label}: ${formatBytes(result.before)} -> ${formatBytes(result.after)}; ${result.dimensions}`);
                    } else {
                        log(`SKIPPED ${label}: ${result.reason}`);
                    }
                } catch (error) {
                    summary.errors++;
                    log(`ERROR ${label}: ${error.message}`);
                }
            }
        } catch (error) {
            summary.errors++;
            log(`ERROR scanning ${root}: ${error.message}`);
        }
    }

    const saved = summary.before - summary.after;
    const percent = summary.before === 0 ? 0 : saved / summary.before * 100;
    log(`${dryRun ? 'Would convert' : 'Converted'}: ${summary.converted} (resized: ${summary.resized}); skipped: ${summary.skipped}; errors: ${summary.errors}.`);
    log(`Converted files: ${formatBytes(summary.before)} -> ${formatBytes(summary.after)}. Saved: ${formatBytes(saved)} (${percent.toFixed(1)}%).`);
    return summary;
}

/** Parse the standalone command without starting screenshot generation. */
function parseArguments(args) {
    const options = { quality: DEFAULT_QUALITY, maxWidth: DEFAULT_MAX_WIDTH, dryRun: false };
    for (let index = 0; index < args.length; index++) {
        const arg = args[index];
        if (arg === '--dry-run') options.dryRun = true;
        else if (arg === '--help') options.help = true;
        else if (arg === '--quality') {
            const value = args[++index];
            if (!/^\d+$/.test(value || '') || Number(value) < 1 || Number(value) > 100) {
                throw new Error('--quality requires an integer from 1 to 100.');
            }
            options.quality = Number(value);
        } else if (arg === '--max-width') {
            const value = args[++index];
            if (!/^\d+$/.test(value || '') || !Number.isSafeInteger(Number(value)) || Number(value) < 1) {
                throw new Error('--max-width requires a positive integer.');
            }
            options.maxWidth = Number(value);
        } else if (arg && !arg.startsWith('-')) {
            if (options.directory !== undefined) throw new Error('Only one directory can be supplied.');
            options.directory = arg;
        } else {
            throw new Error(`Unknown argument: ${arg}`);
        }
    }
    return options;
}

async function main() {
    const options = parseArguments(process.argv.slice(2));
    if (options.help) {
        console.log('Usage: npm run scr:optimize -- [directory] [--dry-run] [--quality 1-100] [--max-width pixels]');
        console.log(`Defaults: quality ${DEFAULT_QUALITY}, maximum width ${DEFAULT_MAX_WIDTH} px.`);
        console.log('With a directory: scan all .jpg/.jpeg files recursively; relative paths use the current working directory.');
        console.log('Without a directory: scan only screenshot*.jpg/jpeg under webapp/components and webapp/apps.');
        return;
    }
    const webapp = path.resolve(__dirname, '../../../main/webapp');
    const roots = options.directory !== undefined ? [path.resolve(options.directory)] : [
        path.join(webapp, 'components'),
        path.join(webapp, 'apps')
    ];
    const summary = await optimizeScreenshots(roots, { ...options, screenshotsOnly: options.directory === undefined });
    if (summary.errors > 0) process.exitCode = 1;
}

if (require.main === module) {
    main().catch(error => {
        console.error(error.message);
        process.exitCode = 1;
    });
}

module.exports = { optimizeScreenshots, parseArguments };
