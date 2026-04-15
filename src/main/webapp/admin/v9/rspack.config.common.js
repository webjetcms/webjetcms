const path = require("path");
const rspack = require("@rspack/core");
const { VueLoaderPlugin } = require('vue-loader');

const WP_DATA = {
    name: "WebJET CMS",
    generator: "WebJET CMS",
    author: "InterWay, a. s. - www.interway.sk",
    publicPath: "/admin/v9/dist/"
};

//all PUG page templates to compile to HTML
const PAGES = [
    "/dashboard/overview",
    "/webpages/web-pages-list",
    "/webpages/media-groups",
    "/webpages/media",
    "/webpages/perex",
    "/webpages/component",
    "/webpages/linkcheck",
    "/webpages/attributes",
    "/webpages/mirroring",
    "/apps/gallery",
    "/apps/image-editor",
    //"/apps/forms-list",
    "/apps/audit-search",
    "/apps/audit-notifications",
    "/apps/insert-script",
    "/apps/default",
    "/apps/audit-changed-webpages",
    "/apps/audit-awaiting-publish-webpages",
    "/apps/audit-log-levels",
    "/apps/audit-log-files",
    "/templates/temps-list",
    "/templates/news",
    "/templates/temps-groups-list",
    "/settings/configuration",
    "/settings/translation-keys",
    "/settings/redirect",
    "/settings/domain-redirect",
    "/settings/cronjob",
    "/settings/missing-keys",
    //"/settings/backup",
    "/settings/update",
    //"/settings/restart",
    "/settings/ai-assistants",
    "/settings/ai-stats",
    "/settings/cache-objects",
    "/settings/persistent-cache-objects",
    "/settings/database-delete",
    "/settings/in-memory-logging",
    "/users/user-list",
    "/users/user-groups",
    "/users/permission-groups",
    "/users/self",
    "/users/passkey",
    "/files/index",
    "/files/dialog",
    "/files/wj_image",
    "/files/wj_link",
    "/files/folder_prop",
    "/files/file_prop",
    "/search/index",
];

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
            //pug-loader is not needed here - dev mode pre-compiles PUG at config time,
            //prod mode adds pug-loader in its own config
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
                            //use native Dart Sass binary (sass-embedded) with modern compiler API
                            //sass-embedded is 2-5x faster than the JS sass package
                            api: 'modern-compiler',
                            implementation: require('sass-embedded'),
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

        //HTML plugins are added in dev/prod configs (dev: pre-compiled PUG + HtmlRspackPlugin, prod: HtmlWebpackPlugin + pug-loader)

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
                { from: 'src/images/logo-net.svg', to: 'images/' }
            ]
        }),
        new VueLoaderPlugin(),

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
