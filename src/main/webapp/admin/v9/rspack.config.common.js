const path = require("path");
const fs = require("fs");
const glob = require("glob");
const rspack = require("@rspack/core");
const { VueLoaderPlugin } = require('vue-loader');

//Try to use sass-embedded (native Dart binary, 2-5x faster) with fallback to JS sass
let sassImplementation;
let sassApi;
try {
    sassImplementation = require('sass-embedded');
    sassApi = 'modern-compiler';
    console.log("Using sass-embedded (native binary) - fast mode");
} catch (e) {
    sassImplementation = require('sass');
    sassApi = 'modern-compiler';
    console.log("sass-embedded not available, using sass (JS) - slower mode");
}

const WP_DATA = {
    name: "WebJET CMS",
    generator: "WebJET CMS",
    author: "InterWay, a. s. - www.interway.sk",
    publicPath: "/admin/v9/dist/"
};

class BuildTimestampPlugin {
    apply(compiler) {
        compiler.hooks.done.tap("BuildTimestampPlugin", () => {
            const timestamp = new Date().toLocaleTimeString('sk-SK', {
                hour12: false,
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });

            process.stdout.write("[" + timestamp + "] ");
        });
    }
}

//Scan views/pages/ for all PUG page templates (only those extending a layout, not fragments)
const pagesDir = path.resolve(__dirname, "views/pages");
const PAGES = glob.sync("**/*.pug", { cwd: pagesDir })
    .filter(file => {
        const firstLine = fs.readFileSync(path.join(pagesDir, file), 'utf8').split('\n')[0];
        return firstLine.startsWith('extends');
    })
    .map(file => "/" + file.replace(/\.pug$/, ''))
    .sort();

const config = {
    entry: {
        main: path.resolve(__dirname, "src") + "/js/app.js",
        appInit: path.resolve(__dirname, "src") + "/js/app-init.js",
        "pages_web-pages-list": path.resolve(__dirname, "src") + "/js/pages/web-pages-list/web-pages-list.js",
        "pages_gallery": path.resolve(__dirname, "src") + "/js/pages/gallery/gallery.js"
    },
    output: {
        filename: "js/[name].[contenthash].js",
        chunkFilename: 'js/[id].[contenthash].js',
        path: path.resolve(__dirname, "dist/"),
        publicPath: WP_DATA.publicPath,
        devtoolModuleFilenameTemplate: 'file:///[absolute-resource-path]'
    },
    module: {
        rules: [
            {
                test: /\.vue$/,
                loader: 'vue-loader',
                options: {
                    loaders: {
                        css: [rspack.CssExtractRspackPlugin.loader, {
                            loader: 'css-loader'
                        }],
                    },
                    cacheBusting: true
                }
            },
            //PUG templates are rendered outside module rules by shared dev/prod renderers
            {
                test: /\.m?js$/,
                include: path.resolve(__dirname, 'src/js'),
                use: [
                    {
                        loader: 'builtin:swc-loader',
                        options: {
                            jsc: {
                                parser: {
                                    syntax: "ecmascript",
                                    dynamicImport: true
                                }
                            },
                            env: {
                                targets: "last 5 versions, > 0.5%, not dead"
                            }
                        }
                    }
                ]
            },
            {
                test: /\.(png|ico|svg|jpe?g|gif)$/,
                exclude: [
                    path.resolve(__dirname, "src/fonts")
                ],
                type: "asset",
                generator: {
                     filename: 'images/[name][ext]'
                }
            },
            {
                test: /\.(woff|woff2|ttf|eot|svg)$/,
                exclude: [
                    path.resolve(__dirname, "src/images")
                ],
                type: "asset",
                generator: {
                     filename: 'fonts/[name][ext]'
                }
            },
            {
                test: /\.(scss|sass|css)$/,
                use: [
                    {
                        loader: rspack.CssExtractRspackPlugin.loader
                    },
                    {
                        loader: 'css-loader'
                    },
                    {
                        loader: "postcss-loader",
                        options: {
                            postcssOptions: {
                                plugins: [
                                    "autoprefixer"
                                ]
                            }
                        }
                    },
                    {
                        loader: "sass-loader",
                        options: {
                            api: sassApi,
                            implementation: sassImplementation,
                            sassOptions: {
                                quietDeps: true,
                                silenceDeprecations: ['import', 'global-builtin', 'color-functions']
                            }
                        }
                    }
                ]
            }
        ]
    },
    plugins: [
        new rspack.ProvidePlugin({
            $: 'jquery',
            jQuery: 'jquery',
            'window.jQuery': 'jquery'
        }),

        new rspack.CssExtractRspackPlugin({
            filename: "css/main.style.css",
            chunkFilename: "css/vendor-[id].style.css"
        }),

        //HTML output is generated in dev/prod configs by a shared PUG renderer

        new rspack.CopyRspackPlugin({
            patterns:[
                { from: 'src/images/logo-cms.png', to: 'images/' },
                { from: 'src/images/logo-cms.svg', to: 'images/' },
                { from: 'src/images/logo-dms.png', to: 'images/' },
                { from: 'src/images/logo-dms.svg', to: 'images/' },
                { from: 'src/images/logo-lms.png', to: 'images/' },
                { from: 'src/images/logo-lms.svg', to: 'images/' },
                { from: 'src/images/logo-msg.png', to: 'images/' },
                { from: 'src/images/logo-msg.svg', to: 'images/' },
                { from: 'src/images/logo-net.png', to: 'images/' },
                { from: 'src/images/logo-net.svg', to: 'images/' },
                { from: 'src/images/favicon-cms.ico', to: 'images/' },
                { from: 'src/images/throbber.gif', to: 'images/' },
                { from: 'src/images/40px.png', to: 'images/' }
            ]
        }),
        new VueLoaderPlugin(),
        new BuildTimestampPlugin(),

        //limit moment.js locales to only sk, cs, de
        new rspack.ContextReplacementPlugin(/moment[/\\]locale$/, /sk|cs|de/),
    ],
    resolve: {
        extensions: [ '.js', '.vue' ],
        alias: {
            'vue$': 'vue/dist/vue.esm-bundler.js',
        }
    },
    optimization: {
        //share single runtime across all entries so they use the same module instances (e.g. jQuery, bootstrap)
        runtimeChunk: 'single',
        splitChunks: {
            cacheGroups: {
                defaultVendors: false,
            },
        },
    }
};

module.exports = config;
module.exports.PAGES = PAGES;
module.exports.WP_DATA = WP_DATA;
