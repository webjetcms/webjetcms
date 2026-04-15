/**
 * esbuild-based development watch script for WebJET CMS admin v9.
 *
 * Uses esbuild's incremental rebuild API for fast JS bundling (~1-3s per change
 * vs ~27s with Rollup). Reuses PUG and SCSS compilation logic from vite.config.js.
 *
 * Usage: node esbuild-watch.mjs          (watch mode)
 *        node esbuild-watch.mjs --build  (single build, no watch)
 *
 * Production builds still use `npm run prod` (vite build with Rollup + terser).
 */

import esbuild from 'esbuild';
import fs from 'fs';
import crypto from 'crypto';
import path from 'path';
import { fileURLToPath } from 'url';
import pug from 'pug';
import * as sass from 'sass';
import vue from 'esbuild-plugin-vue3';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const VIEWS_DIR = path.resolve(__dirname, 'views');
const PAGES_DIR = path.resolve(VIEWS_DIR, 'pages');
const DIST_DIR = path.resolve(__dirname, 'dist');
const MANIFEST_PATH = path.resolve(DIST_DIR, '.vite/manifest.json');
const PUBLIC_PATH = '/admin/v9/dist/';
const SRC_DIR = path.resolve(__dirname, 'src');

const isWatch = !process.argv.includes('--build');

// Path to independently compiled main CSS
let independentCssPath = null;

const WP_DATA = {
    name: "WebJET CMS",
    generator: "WebJET CMS",
    author: "InterWay, a. s. - www.interway.sk",
    publicPath: PUBLIC_PATH
};

// ─── esbuild plugins ────────────────────────────────────────────────────────

/**
 * Makes `import $ from 'jquery'` resolve to window.jQuery at runtime.
 * esbuild doesn't have Vite's virtual module system, so we use the onResolve/onLoad API.
 */
const jqueryExternalPlugin = {
    name: 'jquery-external',
    setup(build) {
        build.onResolve({ filter: /^jquery$/ }, () => ({
            path: 'jquery',
            namespace: 'jquery-global',
        }));
        build.onLoad({ filter: /.*/, namespace: 'jquery-global' }, () => ({
            contents: 'module.exports = window.jQuery;',
            loader: 'js',
        }));
    },
};

/**
 * Handles SCSS imports in JS files. esbuild can't compile SCSS natively,
 * so we compile it with the Sass API and return CSS.
 */
const scssPlugin = {
    name: 'scss-loader',
    setup(build) {
        build.onLoad({ filter: /\.scss$/ }, (args) => {
            const nodeModules = path.resolve(__dirname, 'node_modules');
            const result = sass.compile(args.path, {
                loadPaths: [nodeModules],
                sourceMap: false,
                style: 'expanded',
                silenceDeprecations: ['import', 'color-functions', 'global-builtin', 'if-function'],
            });
            return {
                contents: result.css,
                loader: 'css',
            };
        });
    },
};

/**
 * Fixes relative imports inside Vue SFC virtual modules.
 * The esbuild-plugin-vue3 creates virtual modules with namespace paths like
 * `sfc-script:/.../file.vue?type=script`, and esbuild can't resolve relative
 * imports from those. This plugin intercepts them and resolves relative to the
 * original .vue file's directory.
 */
const vueSfcResolverPlugin = {
    name: 'vue-sfc-resolver',
    setup(build) {
        build.onResolve({ filter: /^\.\.?\// }, (args) => {
            // Only handle imports from Vue SFC virtual modules
            if (!args.importer.includes('.vue?type=')) return undefined;

            // Extract the real .vue file path from the importer
            const vueFilePath = args.importer.replace(/\?type=.*$/, '').replace(/^sfc-script:/, '');
            const dir = path.dirname(vueFilePath);
            let resolved = path.resolve(dir, args.path);

            // Try with .vue extension if no extension specified
            if (!path.extname(resolved)) {
                if (fs.existsSync(resolved + '.vue')) {
                    resolved = resolved + '.vue';
                } else if (fs.existsSync(resolved + '.js')) {
                    resolved = resolved + '.js';
                } else if (fs.existsSync(resolved + '/index.js')) {
                    resolved = resolved + '/index.js';
                } else if (fs.existsSync(resolved + '/index.vue')) {
                    resolved = resolved + '/index.vue';
                }
            }

            if (fs.existsSync(resolved)) {
                return { path: resolved };
            }
            return undefined;
        });
    },
};

/**
 * Strips unused moment.js locale imports and transforms locale CJS files to ESM.
 * Equivalent to the momentLocalePlugin in vite.config.js.
 */
const momentLocalePlugin = {
    name: 'moment-locale',
    setup(build) {
        // Block dynamic locale requires (moment tries require('./locale/' + name))
        build.onResolve({ filter: /^\.\/locale\// }, (args) => {
            if (args.resolveDir.includes('node_modules/moment')) {
                // Allow specific locales that are explicitly imported
                const locale = args.path.replace('./locale/', '');
                if (['sk', 'cs', 'de'].includes(locale)) {
                    return undefined; // let esbuild resolve normally
                }
                // Block all other locales
                return { path: args.path, namespace: 'moment-empty' };
            }
        });
        build.onLoad({ filter: /.*/, namespace: 'moment-empty' }, () => ({
            contents: 'module.exports = {};',
            loader: 'js',
        }));
    },
};

/**
 * Resolves `global` to `globalThis` for browser compatibility (numeral/lodash use it).
 */
const globalPolyfillPlugin = {
    name: 'global-polyfill',
    setup(build) {
        build.onResolve({ filter: /^global$/ }, () => ({
            path: 'global',
            namespace: 'global-polyfill',
        }));
        build.onLoad({ filter: /.*/, namespace: 'global-polyfill' }, () => ({
            contents: 'module.exports = globalThis;',
            loader: 'js',
        }));
    },
};

// ─── Manifest generation ────────────────────────────────────────────────────

/**
 * Generates a Vite-compatible manifest.json from esbuild's metafile output.
 * The PUG templates read this manifest to inject correct JS/CSS paths.
 */
function generateManifest(metafile, entryPoints) {
    const manifest = {};

    // Map entry point names to their output files
    const entryNameToSource = {};
    for (const [name, entryPath] of Object.entries(entryPoints)) {
        // Normalize to relative path from the project root
        entryNameToSource[path.relative(__dirname, entryPath)] = name;
    }

    for (const [outputPath, output] of Object.entries(metafile.outputs)) {
        // Skip .map files
        if (outputPath.endsWith('.map')) continue;

        // Get relative output path from dist/
        const relOutput = path.relative('dist', outputPath);

        // Check if this is an entry point
        if (output.entryPoint) {
            const entryName = entryNameToSource[output.entryPoint];
            if (entryName) {
                const entry = {
                    file: relOutput,
                    isEntry: true,
                    src: output.entryPoint,
                };

                // Collect CSS outputs for this entry
                if (output.cssBundle) {
                    entry.css = [path.relative('dist', output.cssBundle)];
                }

                manifest[output.entryPoint] = entry;
            }
        }
    }

    // Ensure manifest directory exists
    const manifestDir = path.dirname(MANIFEST_PATH);
    fs.mkdirSync(manifestDir, { recursive: true });
    fs.writeFileSync(MANIFEST_PATH, JSON.stringify(manifest, null, 2), 'utf-8');
}

// ─── SCSS compilation (reused from vite.config.js logic) ────────────────────

function compileSassStyles() {
    const inputFile = path.resolve(__dirname, 'src/scss/ninja.scss');
    const nodeModules = path.resolve(__dirname, 'node_modules');
    const start = Date.now();

    const packageStyleImporter = {
        findFileUrl(url) {
            if (url.startsWith('.') || url.startsWith('/') || url.startsWith('file:')) return null;
            const parts = url.split('/');
            const pkgName = url.startsWith('@') ? parts.slice(0, 2).join('/') : parts[0];
            const subPath = url.startsWith('@') ? parts.slice(2).join('/') : parts.slice(1).join('/');
            const pkgJsonPath = path.resolve(nodeModules, pkgName, 'package.json');
            if (!fs.existsSync(pkgJsonPath)) return null;
            if (subPath) return null;
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
        sourceMap: true,
        sourceMapIncludeSources: true,
        style: 'expanded',
        silenceDeprecations: ['import', 'color-functions', 'global-builtin', 'if-function'],
    });

    const outputFilename = 'styles.css';
    const outputDir = path.resolve(DIST_DIR, 'css');
    const outputPath = path.resolve(outputDir, outputFilename);
    fs.mkdirSync(outputDir, { recursive: true });

    fs.writeFileSync(outputPath, result.css, 'utf-8');

    // Fix @tabler/icons-webfont url() paths
    let css = fs.readFileSync(outputPath, 'utf-8');
    css = css.replace(/url\(["']?@tabler\/icons-webfont\/dist\/fonts\/([^"')]+)["']?\)/g,
        'url("../fonts/$1")');
    fs.writeFileSync(outputPath, css, 'utf-8');

    // Copy font/image assets
    copyAssetsDir(path.resolve(__dirname, 'src/fonts'), path.resolve(DIST_DIR, 'fonts'));
    copyAssetsDir(path.resolve(__dirname, 'src/images'), path.resolve(DIST_DIR, 'images'));

    if (result.sourceMap) {
        fs.writeFileSync(outputPath + '.map', JSON.stringify(result.sourceMap), 'utf-8');
    }

    independentCssPath = PUBLIC_PATH + 'css/' + outputFilename;
    console.log(`[scss-compiler] Compiled ${outputFilename} in ${Date.now() - start}ms`);
}

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

// ─── PUG compilation (reused from vite.config.js logic) ─────────────────────

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

function readManifestFiles() {
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

    const orderedCss = [];
    if (independentCssPath) {
        orderedCss.push(independentCssPath);
    }
    orderedCss.push(...cssFiles);

    // Ensure main (app.js) loads before appInit (app-init.js)
    const mainIdx = jsFiles.findIndex(f => f.includes('/main.'));
    const initIdx = jsFiles.findIndex(f => f.includes('/appInit.'));
    if (mainIdx > 0 && initIdx >= 0 && mainIdx > initIdx) {
        const [main] = jsFiles.splice(mainIdx, 1);
        jsFiles.splice(initIdx, 0, main);
    }

    return { css: orderedCss, js: jsFiles };
}

function compileSinglePugTemplate(pugPath, wpFiles) {
    const relativePath = path.relative(PAGES_DIR, pugPath);
    const outputRelative = relativePath.replace(/\.pug$/, '.html');
    const outputPath = path.resolve(DIST_DIR, 'views', outputRelative);

    fs.mkdirSync(path.dirname(outputPath), { recursive: true });

    const htmlWebpackPlugin = {
        options: { data: WP_DATA, filename: outputPath },
        files: wpFiles,
    };

    try {
        let html = pug.renderFile(pugPath, {
            htmlWebpackPlugin,
            filename: pugPath,
            basedir: VIEWS_DIR,
        });
        html = html.replace(/<!--(?!\[if\s)(?!\/\*)([\s\S]*?)-->/g, '');
        fs.writeFileSync(outputPath, html, 'utf-8');
        return true;
    } catch (err) {
        console.error(`  ERROR: ${outputRelative}: ${err.message}`);
        return false;
    }
}

function compilePugTemplates() {
    if (!fs.existsSync(MANIFEST_PATH)) {
        console.error('[pug-templates] Vite manifest not found at', MANIFEST_PATH);
        return;
    }

    const wpFiles = readManifestFiles();
    const pugFiles = findPugFiles(PAGES_DIR);
    const pugStart = Date.now();
    console.log(`[pug-templates] Compiling ${pugFiles.length} PUG templates...`);

    let success = 0;
    let failed = 0;

    for (const pugPath of pugFiles) {
        if (compileSinglePugTemplate(pugPath, wpFiles)) {
            success++;
        } else {
            failed++;
        }
    }

    console.log(`[pug-templates] Done: ${success} compiled, ${failed} failed in ${Date.now() - pugStart}ms`);
}

// ─── Incremental PUG compilation ────────────────────────────────────────────

function buildPugDependencyMap() {
    const allPugFiles = findPugFiles(VIEWS_DIR);
    const forwardDeps = new Map();

    for (const pugFile of allPugFiles) {
        const deps = new Set();
        try {
            const content = fs.readFileSync(pugFile, 'utf-8');
            const dir = path.dirname(pugFile);
            const regex = /^\s*(?:extends|include)\s+([^\s(]+)/gm;
            let match;
            while ((match = regex.exec(content)) !== null) {
                let depPath = match[1];
                let resolved;
                if (depPath.startsWith('/')) {
                    resolved = path.resolve(VIEWS_DIR, depPath.slice(1));
                } else {
                    resolved = path.resolve(dir, depPath);
                }
                if (!resolved.endsWith('.pug')) {
                    resolved += '.pug';
                }
                if (fs.existsSync(resolved)) {
                    deps.add(resolved);
                }
            }
        } catch (err) {
            // skip unreadable files
        }
        forwardDeps.set(pugFile, deps);
    }

    const pageFiles = findPugFiles(PAGES_DIR);
    const reverseDeps = new Map();

    for (const page of pageFiles) {
        const visited = new Set();
        const stack = [page];
        while (stack.length > 0) {
            const current = stack.pop();
            if (visited.has(current)) continue;
            visited.add(current);
            const deps = forwardDeps.get(current);
            if (deps) {
                for (const dep of deps) {
                    stack.push(dep);
                }
            }
        }
        for (const dep of visited) {
            if (dep === page) continue;
            if (!reverseDeps.has(dep)) {
                reverseDeps.set(dep, new Set());
            }
            reverseDeps.get(dep).add(page);
        }
    }

    return reverseDeps;
}

let pugDependencyMap = null;

function compilePugIncremental(changedAbsPath) {
    if (!fs.existsSync(MANIFEST_PATH)) {
        console.error('[pug-templates] Vite manifest not found at', MANIFEST_PATH);
        return;
    }

    const wpFiles = readManifestFiles();
    const pugStart = Date.now();
    const isPageFile = changedAbsPath.startsWith(PAGES_DIR + path.sep);

    if (isPageFile) {
        console.log(`[pug-templates] Compiling 1 changed page...`);
        const ok = compileSinglePugTemplate(changedAbsPath, wpFiles);
        console.log(`[pug-templates] Done: ${ok ? 1 : 0} compiled, ${ok ? 0 : 1} failed in ${Date.now() - pugStart}ms`);
        return;
    }

    if (!pugDependencyMap) {
        pugDependencyMap = buildPugDependencyMap();
    }

    const affectedPages = pugDependencyMap.get(changedAbsPath);

    if (!affectedPages || affectedPages.size === 0) {
        pugDependencyMap = buildPugDependencyMap();
        const retryPages = pugDependencyMap.get(changedAbsPath);
        if (!retryPages || retryPages.size === 0) {
            console.log(`[pug-templates] Unknown partial, full recompilation...`);
            compilePugTemplates();
            return;
        }
        compilePageSet(retryPages, wpFiles, pugStart);
        return;
    }

    compilePageSet(affectedPages, wpFiles, pugStart);
}

function compilePageSet(pages, wpFiles, pugStart) {
    console.log(`[pug-templates] Compiling ${pages.size} affected page(s)...`);
    let success = 0;
    let failed = 0;
    for (const pagePath of pages) {
        if (compileSinglePugTemplate(pagePath, wpFiles)) {
            success++;
        } else {
            failed++;
        }
    }
    console.log(`[pug-templates] Done: ${success} compiled, ${failed} failed in ${Date.now() - pugStart}ms`);
}

// ─── PUG watcher ────────────────────────────────────────────────────────────

function startPugWatcher() {
    let debounceTimer = null;
    pugDependencyMap = buildPugDependencyMap();
    console.log('[pug-watcher] Watching views/ for PUG changes (incremental mode)...');

    fs.watch(VIEWS_DIR, { recursive: true }, (eventType, filename) => {
        if (!filename || !filename.endsWith('.pug')) return;
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            console.log(`[pug-watcher] PUG change detected: ${filename}`);
            const changedAbsPath = path.resolve(VIEWS_DIR, filename);
            compilePugIncremental(changedAbsPath);
        }, 300);
    });
}

// ─── SCSS watcher ───────────────────────────────────────────────────────────

function startScssWatcher() {
    const scssDir = path.resolve(__dirname, 'src/scss');
    let debounceTimer = null;
    console.log('[scss-watcher] Watching src/scss/ for SCSS changes...');

    fs.watch(scssDir, { recursive: true }, (eventType, filename) => {
        if (!filename || (!filename.endsWith('.scss') && !filename.endsWith('.css'))) return;
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            console.log(`[scss-watcher] SCSS change detected: ${filename}`);
            try {
                compileSassStyles();
            } catch (err) {
                console.error(`[scss-watcher] SCSS compilation error: ${err.message}`);
            }
        }, 300);
    });
}

// ─── Copy jQuery ────────────────────────────────────────────────────────────

function copyJquery() {
    const src = path.resolve(__dirname, 'node_modules/jquery/dist/jquery.min.js');
    const dest = path.resolve(DIST_DIR, 'js/jquery.min.js');
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
}

// ─── Main ───────────────────────────────────────────────────────────────────

const entryPoints = {
    main: path.resolve(__dirname, 'src/js/app.js'),
    appInit: path.resolve(__dirname, 'src/js/app-init.js'),
    'pages_web-pages-list': path.resolve(__dirname, 'src/js/pages/web-pages-list/web-pages-list.js'),
    'pages_gallery': path.resolve(__dirname, 'src/js/pages/gallery/gallery.js'),
};

async function main() {
    // Clean dist directory
    if (fs.existsSync(DIST_DIR)) {
        fs.rmSync(DIST_DIR, { recursive: true, force: true });
    }
    fs.mkdirSync(DIST_DIR, { recursive: true });

    const buildOptions = {
        entryPoints,
        bundle: true,
        splitting: true,
        format: 'esm',
        outdir: DIST_DIR,
        sourcemap: true,
        metafile: true,
        target: ['es2020'],
        // Entry points use stable names (no hashes) matching vite dev mode.
        // Chunks MUST use hashes — esbuild names all shared chunks "chunk" so they'd collide without one.
        entryNames: 'js/[name]',
        chunkNames: 'js/[name]-[hash]',
        assetNames: 'fonts/[name]',
        loader: {
            '.woff': 'copy',
            '.woff2': 'copy',
            '.ttf': 'copy',
            '.eot': 'copy',
            '.svg': 'copy',
            '.png': 'copy',
            '.jpg': 'copy',
            '.gif': 'copy',
        },
        plugins: [
            jqueryExternalPlugin,
            momentLocalePlugin,
            scssPlugin,
            vueSfcResolverPlugin,
            vue(),
        ],
        // Define globals matching vite.config.js
        define: {
            '__VUE_OPTIONS_API__': 'true',
            '__VUE_PROD_DEVTOOLS__': 'false',
            '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': 'false',
            'global': 'globalThis',
            'process.env.NODE_ENV': '"development"',
        },
        // Resolve aliases matching vite.config.js
        alias: {
            'vue': 'vue/dist/vue.esm-bundler.js',
        },
        logLevel: 'info',
    };

    let lastManifestHash = null;

    if (isWatch) {
        // Use esbuild's context API for incremental rebuilds
        const ctx = await esbuild.context(buildOptions);

        // Initial build
        console.log('Starting initial build...');
        const initialStart = Date.now();
        const result = await ctx.rebuild();
        console.log(`Initial JS build in ${Date.now() - initialStart}ms`);

        // Generate manifest, compile SCSS, PUG, copy jQuery
        generateManifest(result.metafile, entryPoints);
        lastManifestHash = crypto.createHash('md5')
            .update(fs.readFileSync(MANIFEST_PATH, 'utf-8'))
            .digest('hex');

        compileSassStyles();
        copyJquery();
        compilePugTemplates();

        // Start independent PUG and SCSS watchers
        startPugWatcher();
        startScssWatcher();

        // Watch JS/Vue source files for changes
        const chokidar = await import('chokidar').catch(() => null);

        if (chokidar) {
            // Use chokidar for precise file watching
            const watchPaths = [
                path.resolve(__dirname, 'src/js'),
                path.resolve(__dirname, 'src/vue'),
                path.resolve(__dirname, 'npm_packages'),
            ].filter(p => fs.existsSync(p));

            const watcher = chokidar.default.watch(watchPaths, {
                ignoreInitial: true,
                ignored: /node_modules/,
                persistent: true,
                usePolling: false,
            });

            console.log('[js-watcher] Watching src/js/, src/vue/, npm_packages/ for JS changes...');

            let jsDebounce = null;
            const handleChange = (filePath) => {
                clearTimeout(jsDebounce);
                jsDebounce = setTimeout(async () => {
                    const relPath = path.relative(__dirname, filePath);
                    console.log(`[js-watcher] Change detected: ${relPath}`);
                    const start = Date.now();
                    try {
                        const result = await ctx.rebuild();
                        console.log(`JS rebuild in ${Date.now() - start}ms`);

                        // Regenerate manifest and check if PUG needs recompilation
                        generateManifest(result.metafile, entryPoints);
                        const newHash = crypto.createHash('md5')
                            .update(fs.readFileSync(MANIFEST_PATH, 'utf-8'))
                            .digest('hex');
                        if (newHash !== lastManifestHash) {
                            lastManifestHash = newHash;
                            compilePugTemplates();
                        } else {
                            console.log('[pug-templates] Manifest unchanged, skipping PUG compilation');
                        }
                    } catch (err) {
                        console.error(`[js-watcher] Build error: ${err.message}`);
                    }
                }, 100);
            };

            watcher.on('change', handleChange);
            watcher.on('add', handleChange);
            watcher.on('unlink', handleChange);
        } else {
            // Fallback: use fs.watch (less precise but no extra dependency)
            console.log('[js-watcher] chokidar not available, using fs.watch...');

            const watchDirs = [
                path.resolve(__dirname, 'src/js'),
                path.resolve(__dirname, 'src/vue'),
                path.resolve(__dirname, 'npm_packages'),
            ];

            let jsDebounce = null;

            for (const dir of watchDirs) {
                if (!fs.existsSync(dir)) continue;
                fs.watch(dir, { recursive: true }, (eventType, filename) => {
                    if (!filename) return;
                    if (!filename.endsWith('.js') && !filename.endsWith('.vue') && !filename.endsWith('.ts')) return;

                    clearTimeout(jsDebounce);
                    jsDebounce = setTimeout(async () => {
                        console.log(`[js-watcher] Change detected: ${filename}`);
                        const start = Date.now();
                        try {
                            const result = await ctx.rebuild();
                            console.log(`JS rebuild in ${Date.now() - start}ms`);

                            generateManifest(result.metafile, entryPoints);
                            const newHash = crypto.createHash('md5')
                                .update(fs.readFileSync(MANIFEST_PATH, 'utf-8'))
                                .digest('hex');
                            if (newHash !== lastManifestHash) {
                                lastManifestHash = newHash;
                                compilePugTemplates();
                            } else {
                                console.log('[pug-templates] Manifest unchanged, skipping PUG compilation');
                            }
                        } catch (err) {
                            console.error(`[js-watcher] Build error: ${err.message}`);
                        }
                    }, 100);
                });
            }

            console.log('[js-watcher] Watching src/js/, src/vue/, npm_packages/ for JS changes...');
        }

        console.log('\nWatching for changes. Press Ctrl+C to stop.\n');
    } else {
        // Single build (no watch)
        console.log('Building...');
        const start = Date.now();
        const result = await esbuild.build(buildOptions);
        console.log(`JS build in ${Date.now() - start}ms`);

        generateManifest(result.metafile, entryPoints);
        compileSassStyles();
        copyJquery();
        compilePugTemplates();

        console.log('Build complete.');
    }
}

main().catch((err) => {
    console.error('Build failed:', err);
    process.exit(1);
});
