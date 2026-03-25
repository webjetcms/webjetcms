/**
 * build-templates.mjs
 *
 * Compiles PUG templates to HTML, injecting asset paths from Vite's manifest.
 * Emulates the htmlWebpackPlugin API so PUG templates need ZERO changes.
 *
 * Usage:
 *   node build-templates.mjs           # one-time build
 *   node build-templates.mjs --watch   # watch mode for PUG changes
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import pug from 'pug';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DIST_DIR = path.resolve(__dirname, 'dist');
const VIEWS_DIR = path.resolve(__dirname, 'views');
const PAGES_DIR = path.resolve(VIEWS_DIR, 'pages');
const MANIFEST_PATH = path.resolve(DIST_DIR, '.vite/manifest.json');

const PUBLIC_PATH = '/admin/v9/dist/';

// Build metadata matching webpack's WPD
const WP_DATA = {
    name: "WebJET CMS",
    generator: "WebJET CMS",
    author: "InterWay, a. s. - www.interway.sk",
    publicPath: PUBLIC_PATH
};

// HTML minification options matching webpack's HtmlWebpackPlugin config
const MINIFY_OPTIONS = {
    removeAttributeQuotes: false, // CRITICAL: Preserves Thymeleaf attributes
    removeEmptyAttributes: true,
    collapseWhitespace: false,
    removeComments: true,
    useShortDoctype: true,
};

/**
 * Read Vite manifest and extract CSS/JS file paths.
 * Returns { css: string[], js: string[] } matching htmlWebpackPlugin.files format.
 */
function readManifest() {
    if (!fs.existsSync(MANIFEST_PATH)) {
        console.error('ERROR: Vite manifest not found at', MANIFEST_PATH);
        console.error('Run "vite build" first before building templates.');
        process.exit(1);
    }

    const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf-8'));
    const cssFiles = new Set();
    const jsFiles = [];

    // Collect all entry point JS and their CSS
    for (const [key, entry] of Object.entries(manifest)) {
        if (entry.isEntry) {
            jsFiles.push(PUBLIC_PATH + entry.file);
            if (entry.css) {
                for (const cssFile of entry.css) {
                    cssFiles.add(PUBLIC_PATH + cssFile);
                }
            }
        }
    }

    // Ensure main (app.js) loads before appInit (app-init.js) — main sets up globals
    const mainIdx = jsFiles.findIndex(f => f.includes('/main.'));
    const initIdx = jsFiles.findIndex(f => f.includes('/appInit.'));
    if (mainIdx > 0 && initIdx >= 0 && mainIdx > initIdx) {
        const [main] = jsFiles.splice(mainIdx, 1);
        jsFiles.splice(initIdx, 0, main);
    }

    return {
        css: [...cssFiles],
        js: jsFiles,
    };
}

/**
 * Recursively find all .pug files in a directory.
 */
function findPugFiles(dir) {
    const results = [];
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            results.push(...findPugFiles(fullPath));
        } else if (entry.name.endsWith('.pug')) {
            results.push(fullPath);
        }
    }
    return results;
}

/**
 * Minimal HTML comment removal (matching webpack's removeComments: true).
 * Preserves Thymeleaf expressions and conditional comments.
 */
function minifyHtml(html) {
    if (!MINIFY_OPTIONS.removeComments) return html;
    // Remove HTML comments but preserve conditional comments (<!--[if ...)
    // and Thymeleaf parser-level comments (<!--/* ... */-->)
    return html.replace(/<!--(?!\[if\s)(?!\/\*)([\s\S]*?)-->/g, '');
}

/**
 * Compile a single PUG file to HTML.
 */
function compilePugFile(pugPath, wpFiles) {
    const relativePath = path.relative(PAGES_DIR, pugPath);
    const outputRelative = relativePath.replace(/\.pug$/, '.html');
    const outputPath = path.resolve(DIST_DIR, 'views', outputRelative);

    // Create output directory
    const outputDir = path.dirname(outputPath);
    fs.mkdirSync(outputDir, { recursive: true });

    // Emulate htmlWebpackPlugin API for PUG templates
    const htmlWebpackPlugin = {
        options: {
            data: WP_DATA,
            filename: outputPath,
        },
        files: wpFiles,
    };

    try {
        const html = pug.renderFile(pugPath, {
            htmlWebpackPlugin,
            filename: pugPath, // needed for pug includes/extends resolution
            basedir: VIEWS_DIR,
            // provide require function for any remaining require() calls in pug
            // (favicon is the only one, now changed to publicPath + ...)
            require: (modulePath) => {
                // For images, return the public path
                if (/\.(ico|png|svg|jpg|gif)$/i.test(modulePath)) {
                    const basename = path.basename(modulePath);
                    return PUBLIC_PATH + 'images/' + basename;
                }
                return modulePath;
            },
        });

        const minified = minifyHtml(html);
        fs.writeFileSync(outputPath, minified, 'utf-8');

        return { success: true, output: outputRelative };
    } catch (err) {
        return { success: false, output: outputRelative, error: err.message };
    }
}

/**
 * Build all templates.
 */
function buildAll() {
    console.log('[build-templates] Reading Vite manifest...');
    const wpFiles = readManifest();
    console.log(`[build-templates] Found ${wpFiles.css.length} CSS and ${wpFiles.js.length} JS files`);

    const pugFiles = findPugFiles(PAGES_DIR);
    console.log(`[build-templates] Compiling ${pugFiles.length} PUG templates...`);

    let success = 0;
    let failed = 0;

    for (const pugFile of pugFiles) {
        const result = compilePugFile(pugFile, wpFiles);
        if (result.success) {
            success++;
        } else {
            failed++;
            console.error(`  ERROR: ${result.output}: ${result.error}`);
        }
    }

    console.log(`[build-templates] Done: ${success} compiled, ${failed} failed`);

    if (failed > 0) {
        process.exit(1);
    }
}

/**
 * Wait for the Vite manifest file to appear (used in watch mode when
 * vite build --watch may not have finished its first build yet).
 */
function waitForManifest(timeoutMs = 60000) {
    return new Promise((resolve, reject) => {
        if (fs.existsSync(MANIFEST_PATH)) {
            resolve();
            return;
        }
        console.log('[build-templates] Waiting for Vite manifest...');
        const start = Date.now();
        const interval = setInterval(() => {
            if (fs.existsSync(MANIFEST_PATH)) {
                clearInterval(interval);
                resolve();
            } else if (Date.now() - start > timeoutMs) {
                clearInterval(interval);
                reject(new Error('Timed out waiting for Vite manifest'));
            }
        }, 500);
    });
}

/**
 * Watch mode: rebuild on PUG or manifest changes.
 */
async function watchMode() {
    console.log('[build-templates] Starting watch mode...');

    // Wait for Vite's first build to produce the manifest
    await waitForManifest();
    buildAll();

    const debounceTimers = new Map();

    function debouncedRebuild(key, label) {
        if (debounceTimers.has(key)) {
            clearTimeout(debounceTimers.get(key));
        }
        debounceTimers.set(key, setTimeout(() => {
            debounceTimers.delete(key);
            console.log(`[build-templates] ${label}, rebuilding all...`);
            try {
                buildAll();
            } catch (err) {
                console.error(`[build-templates] Build error: ${err.message}`);
            }
        }, 300));
    }

    // Watch PUG templates
    fs.watch(VIEWS_DIR, { recursive: true }, (eventType, filename) => {
        if (!filename || !filename.endsWith('.pug')) return;
        debouncedRebuild('pug:' + filename, `Changed: ${filename}`);
    });

    // Watch Vite manifest — rebuild templates when assets change
    const manifestDir = path.dirname(MANIFEST_PATH);
    if (fs.existsSync(manifestDir)) {
        fs.watch(manifestDir, (eventType, filename) => {
            if (filename === 'manifest.json') {
                debouncedRebuild('manifest', 'Vite manifest changed');
            }
        });
    }
}

// Main
const isWatch = process.argv.includes('--watch');

if (isWatch) {
    watchMode().catch(err => {
        console.error(`[build-templates] ${err.message}`);
        process.exit(1);
    });
} else {
    buildAll();
}
