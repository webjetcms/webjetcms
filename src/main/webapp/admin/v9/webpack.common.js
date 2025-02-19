const path = require("path");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const autoprefixer = require('autoprefixer');
const CopyPlugin = require('copy-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader');
//const WebjetModifyPlugin = require('./plugins/modify-plugin/index.js');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const MomentLocalesPlugin = require('moment-locales-webpack-plugin');

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
        devtoolModuleFilenameTemplate: 'file:///[absolute-resource-path]'  // map to source with absolute file path not webpack:// protocol
    },
    module: {
        rules: [
            {
                test: /\.vue$/,
                loader: 'vue-loader',
                options: {
                    loaders: {
                        css: ['vue-style-loader', {
                            loader: 'css-loader'
                        }],
                        js: ['babel-loader']
                    },
                    cacheBusting: true
                }
            },
            {
                test: /\.html$/,
                use: [
                    {
                        loader: "html-loader",
                        options: {
                            interpolate: true
                        }
                    }
                ]
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
                        loader: 'babel-loader',
                        options: {
                            presets: ['@babel/preset-env'],
                            plugins: [
                                //['@babel/plugin-proposal-class-properties', { loose: true }], - https://www.npmjs.com/package/@babel/plugin-proposal-class-properties
                                //["@babel/plugin-proposal-private-property-in-object", { "loose": true }], - https://www.npmjs.com/package/@babel/plugin-proposal-private-property-in-object
                                //["@babel/plugin-proposal-private-methods", { "loose": true }], - https://www.npmjs.com/package/@babel/plugin-proposal-private-methods
                                //'@babel/plugin-syntax-dynamic-import'
                            ]
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
                        loader: MiniCssExtractPlugin.loader
                    },
                    {
                        loader: 'css-loader'
                    },
                    {
                        loader: "postcss-loader",
                        options: {
                            postcssOptions: {
                                autoprefixer: {
                                    browsers: ["last 10 versions"]
                                },
                                sourceMap: true,
                                plugins: [
                                    autoprefixer
                                ]
                            }
                        }
                    },
                    {
                        loader: "sass-loader"
                    }
                ]
            }
        ]
    },
    plugins: [
        new MiniCssExtractPlugin({
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

        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/cache-objects")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/persistent-cache-objects")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/database-delete")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/settings/in-memory-logging")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/users/user-list")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/users/user-groups")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/users/permission-groups")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/users/self")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/files/index")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/dialog")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/wj_image")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/wj_link")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/folder_prop")),
        new HtmlWebpackPlugin(generateHtmlPlugins("/files/file_prop")),

        new HtmlWebpackPlugin(generateHtmlPlugins("/search/index")),

        //new WebjetModifyPlugin(['imageEditor']),

        new CopyPlugin({
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

        //ignore all locale files of moment.js (we must specifically import ones we need)
        new MomentLocalesPlugin({
            localesToKeep: ['sk', 'cs', 'de'],
        }),

        //new BundleAnalyzerPlugin()
    ],
    resolve: {
        extensions: [ '.js', '.vue' ],
        alias: {
            'vue$': 'vue/dist/vue.esm-bundler.js',
        }
    },
    optimization: {
        splitChunks: {
            cacheGroups: {
                defaultVendors: false,
                // prior to webpack 5:
                //vendors: false,
            },
        },
    }
};
