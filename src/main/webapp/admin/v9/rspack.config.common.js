const path = require("path");
const rspack = require("@rspack/core");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const { VueLoaderPlugin } = require('vue-loader');

const WP_DATA = {
    name: "WebJET CMS",
    generator: "WebJET CMS",
    author: "InterWay, a. s. - www.interway.sk",
    publicPath: "/admin/v9/dist/"
};

function generateHtmlPlugins(templateDir) {
    return {
        template: path.resolve(__dirname, "views") + '/pages' + templateDir + '.pug',
        filename: path.resolve(__dirname, "dist") + '/views' + templateDir + '.html',
        minify: {
            //https://github.com/DanielRuf/html-minifier-terser#options-quick-reference
            removeAttributeQuotes: false, //toto musi byt false inak nam nequotuje atributy kde vidi Thymeleaf premennu a potom to moze padnut
            removeEmptyAttributes: true,
            collapseWhitespace: false,
            removeComments: true,
            useShortDoctype: true,
            minifyCSS:true,
            minifyJS:false
        },
        inject: false,
        data: WP_DATA
    }
}

module.exports = {
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
            {
                test: /.pug$/,
                include: path.resolve(__dirname, 'views'),
                use: [
                    {
                        loader: 'pug-loader'
                    }
                ]
            },
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

        new HtmlWebpackPlugin(generateHtmlPlugins("/dashboard/overview")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/web-pages-list")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/media-groups")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/media")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/perex")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/component")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/linkcheck")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/attributes")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/webpages/mirroring")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/gallery")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/image-editor")),
        //new HtmlWebpackPlugin(generateHtmlPlugins("/apps/forms-list")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/audit-search")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/audit-notifications")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/insert-script")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/default")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/audit-changed-webpages")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/audit-awaiting-publish-webpages")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/audit-log-levels")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/apps/audit-log-files")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/templates/temps-list")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/templates/news")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/templates/temps-groups-list")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/configuration")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/translation-keys")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/redirect")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/domain-redirect")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/cronjob")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/missing-keys")),
        //new HtmlWebpackPlugin(generateHtmlPlugins("/settings/backup")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/update")),
        //new HtmlWebpackPlugin(generateHtmlPlugins("/settings/restart")),
        //new HtmlWebpackPlugin(generateHtmlPlugins("/settings/missing-keys")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/ai-assistants")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/ai-stats")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/cache-objects")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/persistent-cache-objects")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/database-delete")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/in-memory-logging")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/users/user-list")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/users/user-groups")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/users/permission-groups")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/users/self")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/users/passkey")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/files/index")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/dialog")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/wj_image")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/wj_link")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/folder_prop")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/file_prop")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/search/index")),

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
