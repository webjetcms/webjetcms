import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Inline plugin to strip moment.js locale imports (replaces moment-locales-webpack-plugin)
// We explicitly import sk, cs, de locales in app.js, so we only need to prevent
// moment from dynamically requiring ALL locales via require('./locale/' + name)
function momentStripLocales() {
    return {
        name: 'moment-strip-locales',
        resolveId(source) {
            // moment tries to require('./locale/' + name) dynamically
            // Rollup/Vite will resolve known static imports but ignore the dynamic ones
            // This is handled automatically by Vite's tree-shaking, but we add
            // an explicit empty module for the dynamic context to be safe
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
        }
    };
}

export default defineConfig(({ mode }) => ({
    base: '/admin/v9/dist/',

    resolve: {
        extensions: ['.js', '.vue'],
        alias: {
            'vue': 'vue/dist/vue.esm-bundler.js',
        }
    },

    define: {
        '__VUE_OPTIONS_API__': true,
        '__VUE_PROD_DEVTOOLS__': false,
        '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false,
        // Polyfill Node.js 'global' for browser (used by numeral/lodash)
        'global': 'globalThis',
    },

    plugins: [
        vue(),
        momentStripLocales(),
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
        emptyOutDir: false, // don't delete dist/views which we generate separately
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
            'jquery',
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
