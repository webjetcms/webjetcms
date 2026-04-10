import fs from 'fs';
import crypto from 'crypto';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { fileURLToPath } from 'url';
import pug from 'pug';
import * as sass from 'sass';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const VIEWS_DIR = path.resolve(__dirname, 'views');
const PAGES_DIR = path.resolve(VIEWS_DIR, 'pages');
const DIST_DIR = path.resolve(__dirname, 'dist');
const MANIFEST_PATH = path.resolve(DIST_DIR, '.vite/manifest.json');
const PUBLIC_PATH = '/admin/v9/dist/';

// Path to independently compiled main CSS (set by scssCompilerPlugin, used by PUG templates)
let independentCssPath = null;

// Build metadata matching webpack's WPD (used by PUG templates via htmlWebpackPlugin emulation)
const WP_DATA = {
    name: "WebJET CMS",
    generator: "WebJET CMS",
    author: "InterWay, a. s. - www.interway.sk",
    publicPath: PUBLIC_PATH
};

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
 * Reads the Vite manifest and compiles all PUG page templates into dist/views/ HTML files.
 * Standalone function so it can be called from both writeBundle and the independent watcher.
 */
function compilePugTemplates() {
    if (!fs.existsSync(MANIFEST_PATH)) {
        console.error('[pug-templates] Vite manifest not found at', MANIFEST_PATH);
        return;
    }

    // Read manifest and collect entry CSS/JS files
    const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf-8'));
    const cssFiles = new Set();
    const jsFiles = [];

    for (const [, entry] of Object.entries(manifest)) {
        if (entry.isEntry) {
            jsFiles.push(PUBLIC_PATH + entry.file);
            if (entry.css) {
                for (const cssFile of entry.css) {
                    cssFiles.add(PUBLIC_PATH + cssFile);
                }
            }
        }
    }

    // Add independently compiled main stylesheet first (for proper CSS cascade order)
    const orderedCss = [];
    if (independentCssPath) {
        orderedCss.push(independentCssPath);
    }
    orderedCss.push(...cssFiles);

    // Ensure main (app.js) loads before appInit (app-init.js) — main sets up globals
    const mainIdx = jsFiles.findIndex(f => f.includes('/main.'));
    const initIdx = jsFiles.findIndex(f => f.includes('/appInit.'));
    if (mainIdx > 0 && initIdx >= 0 && mainIdx > initIdx) {
        const [main] = jsFiles.splice(mainIdx, 1);
        jsFiles.splice(initIdx, 0, main);
    }

    const wpFiles = { css: orderedCss, js: jsFiles };

    // Compile all PUG page templates
    const pugFiles = findPugFiles(PAGES_DIR);
    console.log(`[pug-templates] Compiling ${pugFiles.length} PUG templates...`);

    let success = 0;
    let failed = 0;

    for (const pugPath of pugFiles) {
        const relativePath = path.relative(PAGES_DIR, pugPath);
        const outputRelative = relativePath.replace(/\.pug$/, '.html');
        const outputPath = path.resolve(DIST_DIR, 'views', outputRelative);

        fs.mkdirSync(path.dirname(outputPath), { recursive: true });

        // Emulate htmlWebpackPlugin API for PUG templates
        const htmlWebpackPlugin = {
            options: {
                data: WP_DATA,
                filename: outputPath,
            },
            files: wpFiles,
        };

        try {
            let html = pug.renderFile(pugPath, {
                htmlWebpackPlugin,
                filename: pugPath, // needed for pug includes/extends resolution
                basedir: VIEWS_DIR,
            });

            // Remove HTML comments but preserve conditional comments (<!--[if ...)
            // and Thymeleaf parser-level comments (<!--/* ... */-->)
            html = html.replace(/<!--(?!\[if\s)(?!\/\*)([\s\S]*?)-->/g, '');

            fs.writeFileSync(outputPath, html, 'utf-8');
            success++;
        } catch (err) {
            failed++;
            console.error(`  ERROR: ${outputRelative}: ${err.message}`);
        }
    }

    console.log(`[pug-templates] Done: ${success} compiled, ${failed} failed`);
}

/**
 * Vite plugin that compiles PUG templates from views/pages/ into dist/views/ HTML files.
 * Replaces the external build-templates.mjs script.
 *
 * Emulates the htmlWebpackPlugin API so PUG templates need ZERO changes:
 *   - htmlWebpackPlugin.options.data  → WP_DATA (name, generator, author, publicPath)
 *   - htmlWebpackPlugin.files         → { css: string[], js: string[] }
 *   - htmlWebpackPlugin.options.filename → output file path (used by head.pug for page-specific JS)
 *
 * In watch mode (vite build --watch):
 *   - PUG files are NOT registered via addWatchFile (PUG changes don't trigger Vite rebuild)
 *   - writeBundle skips PUG compilation if the manifest hasn't changed since last run
 *   - An independent fs.watch watcher on views/ handles PUG-only changes (~1-2s)
 */
function pugTemplatesPlugin() {
    let lastManifestHash = null;
    let pugWatcherStarted = false;

    return {
        name: 'pug-templates',

        // In non-watch mode (single build), register PUG files so Rollup knows about them
        buildStart() {
            // Don't register PUG files as watched — we handle PUG watching independently
            // This prevents PUG changes from triggering a full Vite JS/CSS rebuild
        },

        // After Vite writes JS/CSS to dist/, compile PUG templates only if manifest changed
        writeBundle() {
            if (!fs.existsSync(MANIFEST_PATH)) {
                console.error('[pug-templates] Vite manifest not found at', MANIFEST_PATH);
                return;
            }

            // Check if manifest content changed since last compilation
            const manifestContent = fs.readFileSync(MANIFEST_PATH, 'utf-8');
            const manifestHash = crypto.createHash('md5').update(manifestContent).digest('hex');

            if (manifestHash === lastManifestHash) {
                console.log('[pug-templates] Manifest unchanged, skipping PUG compilation');
                return;
            }

            lastManifestHash = manifestHash;
            compilePugTemplates();

            // Start independent PUG watcher on first successful build (watch mode only)
            if (!pugWatcherStarted && this.meta.watchMode) {
                pugWatcherStarted = true;
                startPugWatcher();
            }
        },
    };
}

/**
 * Starts an independent file watcher on the views/ directory.
 * When any .pug file changes, recompiles all PUG templates without triggering Vite rebuild.
 * Uses Node's built-in fs.watch with recursive option (works on macOS and Windows).
 */
function startPugWatcher() {
    let debounceTimer = null;

    console.log('[pug-watcher] Watching views/ for PUG changes...');

    fs.watch(VIEWS_DIR, { recursive: true }, (eventType, filename) => {
        if (!filename || !filename.endsWith('.pug')) return;

        // Debounce: wait 300ms for batch changes (e.g. git checkout, save-all)
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            console.log(`[pug-watcher] PUG change detected: ${filename}`);
            compilePugTemplates();
        }, 300);
    });
}

/**
 * Recursively copies a source directory to a destination, preserving structure.
 */
function copyAssetsDir(srcDir, destDir) {
    if (!fs.existsSync(srcDir)) return;
    fs.mkdirSync(destDir, { recursive: true });
    for (const entry of fs.readdirSync(srcDir, { withFileTypes: true })) {
        const srcPath = path.join(srcDir, entry.name);
        const destPath = path.join(destDir, entry.name);
        if (entry.isDirectory()) {
            copyAssetsDir(srcPath, destPath);
        } else {
            fs.copyFileSync(srcPath, destPath);
        }
    }
}

/**
 * Compiles ninja.scss independently using the Sass API (outside of Vite's pipeline).
 * Returns the public path to the compiled CSS file.
 */
function compileSassStyles(mode) {
    const inputFile = path.resolve(__dirname, 'src/scss/ninja.scss');
    const nodeModules = path.resolve(__dirname, 'node_modules');
    const start = Date.now();

    // Custom importer: resolves bare package imports (e.g. @import "datatables.net-bs5")
    // via the "style" field in package.json — same as Vite/webpack convention.
    const packageStyleImporter = {
        findFileUrl(url) {
            // Only handle bare package imports (not relative or absolute paths)
            if (url.startsWith('.') || url.startsWith('/') || url.startsWith('file:')) return null;

            // Extract package name (handles scoped packages like @tabler/icons)
            const parts = url.split('/');
            const pkgName = url.startsWith('@') ? parts.slice(0, 2).join('/') : parts[0];
            const subPath = url.startsWith('@') ? parts.slice(2).join('/') : parts.slice(1).join('/');
            const pkgJsonPath = path.resolve(nodeModules, pkgName, 'package.json');

            if (!fs.existsSync(pkgJsonPath)) return null;

            // If there's a sub-path, let Sass resolve it normally (with loadPaths)
            if (subPath) return null;

            // Resolve via "style" field in package.json
            const pkgJson = JSON.parse(fs.readFileSync(pkgJsonPath, 'utf-8'));
            if (pkgJson.style) {
                const stylePath = path.resolve(nodeModules, pkgName, pkgJson.style);
                if (fs.existsSync(stylePath)) {
                    return new URL('file://' + stylePath);
                }
            }
            return null;
        }
    };

    const result = sass.compile(inputFile, {
        loadPaths: [nodeModules],
        importers: [packageStyleImporter],
        sourceMap: mode === 'development',
        sourceMapIncludeSources: mode === 'development',
        style: mode === 'production' ? 'compressed' : 'expanded',
        silenceDeprecations: ['import', 'color-functions', 'global-builtin', 'if-function'],
    });

    let outputFilename;
    if (mode === 'development') {
        outputFilename = 'styles.css';
    } else {
        const hash = crypto.createHash('md5').update(result.css).digest('hex').slice(0, 8);
        outputFilename = `styles.${hash}.css`;
    }

    const outputDir = path.resolve(DIST_DIR, 'css');
    const outputPath = path.resolve(outputDir, outputFilename);
    fs.mkdirSync(outputDir, { recursive: true });

    // In production, clean up old hashed styles.*.css files
    if (mode === 'production' && fs.existsSync(outputDir)) {
        for (const file of fs.readdirSync(outputDir)) {
            if (file.startsWith('styles.') && file.endsWith('.css') && file !== outputFilename) {
                fs.unlinkSync(path.resolve(outputDir, file));
            }
        }
    }

    fs.writeFileSync(outputPath, result.css, 'utf-8');

    // Post-process: rewrite url() references so they resolve correctly from dist/css/.
    // Sass outputs paths relative to the source SCSS files, but the CSS is served from dist/css/.
    // - "@tabler/icons-webfont/dist/fonts/X" → "../fonts/X" (tabler font files are already in dist/fonts/)
    // - "../fonts/..." and "../images/..." are relative to src/scss/ — copy assets to dist/
    let css = fs.readFileSync(outputPath, 'utf-8');

    // Fix @tabler/icons-webfont paths: the fonts are already copied to dist/fonts/ by Vite (from JS imports)
    css = css.replace(/url\(["']?@tabler\/icons-webfont\/dist\/fonts\/([^"')]+)["']?\)/g,
        'url("../fonts/$1")');

    fs.writeFileSync(outputPath, css, 'utf-8');

    // Copy static font/image assets from src/ to dist/ (preserving directory structure)
    copyAssetsDir(path.resolve(__dirname, 'src/fonts'), path.resolve(DIST_DIR, 'fonts'));
    copyAssetsDir(path.resolve(__dirname, 'src/images'), path.resolve(DIST_DIR, 'images'));

    if (result.sourceMap && mode === 'development') {
        fs.writeFileSync(outputPath + '.map', JSON.stringify(result.sourceMap), 'utf-8');
    }

    independentCssPath = PUBLIC_PATH + 'css/' + outputFilename;
    console.log(`[scss-compiler] Compiled ${outputFilename} in ${Date.now() - start}ms`);
    return independentCssPath;
}

/**
 * Vite plugin that compiles ninja.scss independently from Vite's JS pipeline.
 * SCSS changes only rebuild CSS (seconds), JS is completely untouched.
 */
function scssCompilerPlugin(mode) {
    let scssWatcherStarted = false;

    return {
        name: 'scss-compiler',

        writeBundle() {
            compileSassStyles(mode);

            // Start independent SCSS watcher in watch mode
            if (!scssWatcherStarted && this.meta.watchMode) {
                scssWatcherStarted = true;
                startScssWatcher(mode);
            }
        },
    };
}

/**
 * Watches src/scss/ directory for changes and recompiles SCSS independently.
 * No Vite rebuild, no PUG recompilation — just CSS.
 */
function startScssWatcher(mode) {
    const scssDir = path.resolve(__dirname, 'src/scss');
    let debounceTimer = null;

    console.log('[scss-watcher] Watching src/scss/ for SCSS changes...');

    fs.watch(scssDir, { recursive: true }, (eventType, filename) => {
        if (!filename || (!filename.endsWith('.scss') && !filename.endsWith('.css'))) return;

        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            console.log(`[scss-watcher] SCSS change detected: ${filename}`);
            try {
                compileSassStyles(mode);
            } catch (err) {
                console.error(`[scss-watcher] SCSS compilation error: ${err.message}`);
            }
        }, 300);
    });
}

// Inline plugin to fix moment.js locale handling:
// 1. Strips unused locale imports (replaces moment-locales-webpack-plugin)
// 2. Transforms locale CJS files to ESM so they use the same moment instance
//    as the main import (avoids CJS require creating a duplicate moment copy)
function momentLocalePlugin() {
    return {
        name: 'moment-locale-plugin',
        enforce: 'pre',
        resolveId(source) {
            // moment tries to require('./locale/' + name) dynamically — return empty module
            if (source === 'moment/locale') {
                return '\0moment-locale-empty';
            }
            return null;
        },
        load(id) {
            if (id === '\0moment-locale-empty') {
                return 'export default {};';
            }
            return null;
        },
        transform(code, id) {
            // Transform moment locale CJS files to ESM.
            // Locale files have a UMD wrapper with require('../moment') that creates
            // a separate moment instance via getAugmentedNamespace. We extract the
            // factory function and call it with the ESM moment default export.
            if (id.includes('node_modules/moment/locale/') && id.endsWith('.js')) {
                const factoryStart = code.indexOf('(function (moment) {');
                const factoryEnd = code.lastIndexOf('})));');
                if (factoryStart === -1 || factoryEnd === -1) return null;
                // Extract the factory: (function (moment) { ... })
                const factory = code.substring(factoryStart, factoryEnd + 2);
                const transformed = `import moment from 'moment';\n${factory}(moment);`;
                return { code: transformed, map: null };
            }
        }
    };
}

export default defineConfig(({ mode }) => ({
    base: '/admin/v9/dist/',

    resolve: {
        extensions: ['.js', '.vue'],
        alias: {
            'vue': 'vue/dist/vue.esm-bundler.js',
        },
    },

    define: {
        '__VUE_OPTIONS_API__': true,
        '__VUE_PROD_DEVTOOLS__': false,
        '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false,
        // Polyfill Node.js 'global' for browser (used by numeral/lodash)
        'global': 'globalThis',
    },

    plugins: [
        // jQuery is loaded synchronously via <script> tag in head.pug.
        // This plugin makes all import/require('jquery') use window.jQuery
        // instead of bundling a separate copy.
        // The .cjs extension tells Vite's commonjs plugin to treat this as CJS,
        // so require('jquery') returns window.jQuery directly (not a namespace wrapper).
        {
            name: 'jquery-external',
            enforce: 'pre',
            resolveId(source) {
                if (source === 'jquery') {
                    return { id: '\0jquery-global.cjs', moduleSideEffects: true };
                }
            },
            load(id) {
                if (id === '\0jquery-global.cjs') {
                    return 'module.exports = window.jQuery;';
                }
            }
        },
        vue(),
        momentLocalePlugin(),
        // Copy jQuery to dist for synchronous <script> loading
        {
            name: 'copy-jquery',
            writeBundle() {
                const src = path.resolve(__dirname, 'node_modules/jquery/dist/jquery.min.js');
                const dest = path.resolve(__dirname, 'dist/js/jquery.min.js');
                fs.mkdirSync(path.dirname(dest), { recursive: true });
                fs.copyFileSync(src, dest);
            }
        },
        // Compile main SCSS independently (outside Vite's JS pipeline)
        scssCompilerPlugin(mode),
        // Compile PUG templates from views/pages/ into dist/views/ HTML
        pugTemplatesPlugin(),
    ],

    css: {
        preprocessorOptions: {
            scss: {
                // Resolve bare package imports from node_modules
                loadPaths: [path.resolve(__dirname, 'node_modules')],
                // Ignore bootstrap warnings https://getbootstrap.com/docs/5.3/getting-started/vite/
                silenceDeprecations: [
                    'import',
                    'color-functions',
                    'global-builtin',
                    'if-function',
                ],
            }
        }
    },

    build: {
        outDir: 'dist',
        emptyOutDir: false, // writeBundle plugins (copy-jquery, pug-templates) write into dist/
        manifest: true,
        sourcemap: mode === 'development' ? true : false,

        rollupOptions: {
            // Suppress known harmless warnings:
            // - MIXED_DYNAMIC_STATIC: vue-server-monitoring.vue imported both ways (intentional)
            onwarn(warning, warn) {
                if (warning.code === 'IMPORT_IS_UNDEFINED') return;
                if (warning.message && warning.message.includes('dynamic import will not move module')) return;
                warn(warning);
            },
            input: {
                main: path.resolve(__dirname, 'src/js/app.js'),
                appInit: path.resolve(__dirname, 'src/js/app-init.js'),
                'pages_web-pages-list': path.resolve(__dirname, 'src/js/pages/web-pages-list/web-pages-list.js'),
                'pages_gallery': path.resolve(__dirname, 'src/js/pages/gallery/gallery.js'),
            },
            output: {
                // In dev mode, use stable file names (no content hashes) so the manifest
                // never changes between rebuilds — this lets us skip PUG recompilation.
                // Browser cache is disabled during development so hashes are not needed.
                entryFileNames: mode === 'development' ? 'js/[name].js' : 'js/[name].[hash].js',
                chunkFileNames: mode === 'development' ? 'js/[name].js' : 'js/[name].[hash].js',
                assetFileNames: (assetInfo) => {
                    const name = assetInfo.name || '';
                    if (name.endsWith('.css')) {
                        return mode === 'development' ? 'css/[name][extname]' : 'css/[name].[hash][extname]';
                    }
                    if (/\.(woff2?|ttf|eot)$/.test(name)) {
                        return 'fonts/[name][extname]';
                    }
                    if (/\.(png|jpe?g|gif|svg|ico)$/.test(name)) {
                        return 'images/[name][extname]';
                    }
                    return mode === 'development' ? 'assets/[name][extname]' : 'assets/[name].[hash][extname]';
                },
            },
        },

        // Use terser for production minification (like webpack's TerserPlugin)
        minify: mode === 'production' ? 'terser' : false,
        terserOptions: {
            output: {
                comments: false,
            },
        },
    },

    // Optimize CommonJS/AMD dependencies so Vite pre-bundles them with all internal deps resolved
    optimizeDeps: {
        include: [
            'bootstrap',
            'datatables.net',
            'datatables.net-bs5',
            'datatables.net-buttons-bs5',
            'jstree',
            'moment',
            'quill',
        ]
    },
}));
