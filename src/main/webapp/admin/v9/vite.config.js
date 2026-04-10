import fs from 'fs';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { fileURLToPath } from 'url';
import pug from 'pug';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const VIEWS_DIR = path.resolve(__dirname, 'views');
const PAGES_DIR = path.resolve(VIEWS_DIR, 'pages');
const DIST_DIR = path.resolve(__dirname, 'dist');
const MANIFEST_PATH = path.resolve(DIST_DIR, '.vite/manifest.json');
const PUBLIC_PATH = '/admin/v9/dist/';

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
 * Vite plugin that compiles PUG templates from views/pages/ into dist/views/ HTML files.
 * Replaces the external build-templates.mjs script.
 *
 * Emulates the htmlWebpackPlugin API so PUG templates need ZERO changes:
 *   - htmlWebpackPlugin.options.data  → WP_DATA (name, generator, author, publicPath)
 *   - htmlWebpackPlugin.files         → { css: string[], js: string[] }
 *   - htmlWebpackPlugin.options.filename → output file path (used by head.pug for page-specific JS)
 */
function pugTemplatesPlugin() {
    return {
        name: 'pug-templates',

        // Register all .pug files as watched so vite build --watch triggers rebuild on PUG changes
        buildStart() {
            if (!fs.existsSync(VIEWS_DIR)) return;
            for (const pugFile of findPugFiles(VIEWS_DIR)) {
                this.addWatchFile(pugFile);
            }
        },

        // After Vite writes JS/CSS to dist/, read the manifest and compile PUG→HTML
        writeBundle() {
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

            // Ensure main (app.js) loads before appInit (app-init.js) — main sets up globals
            const mainIdx = jsFiles.findIndex(f => f.includes('/main.'));
            const initIdx = jsFiles.findIndex(f => f.includes('/appInit.'));
            if (mainIdx > 0 && initIdx >= 0 && mainIdx > initIdx) {
                const [main] = jsFiles.splice(mainIdx, 1);
                jsFiles.splice(initIdx, 0, main);
            }

            const wpFiles = { css: [...cssFiles], js: jsFiles };

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
        },
    };
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
                return `import moment from 'moment';\n${factory}(moment);`;
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
                    'mixed-decls',
                    'color-functions',
                    'global-builtin',
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
            input: {
                main: path.resolve(__dirname, 'src/js/app.js'),
                appInit: path.resolve(__dirname, 'src/js/app-init.js'),
                'pages_web-pages-list': path.resolve(__dirname, 'src/js/pages/web-pages-list/web-pages-list.js'),
                'pages_gallery': path.resolve(__dirname, 'src/js/pages/gallery/gallery.js'),
            },
            output: {
                entryFileNames: 'js/[name].[hash].js',
                chunkFileNames: 'js/[name].[hash].js',
                assetFileNames: (assetInfo) => {
                    const name = assetInfo.name || '';
                    if (name.endsWith('.css')) {
                        return 'css/[name].[hash][extname]';
                    }
                    if (/\.(woff2?|ttf|eot)$/.test(name)) {
                        return 'fonts/[name][extname]';
                    }
                    if (/\.(png|jpe?g|gif|svg|ico)$/.test(name)) {
                        return 'images/[name][extname]';
                    }
                    return 'assets/[name].[hash][extname]';
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
