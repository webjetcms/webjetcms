const path = require("path");
const common = require("./rspack.config.common");
const rspack = require("@rspack/core");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const { merge } = require("webpack-merge");

const WP_DATA = common.WP_DATA;

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

const htmlPlugins = common.PAGES.map(page => new HtmlWebpackPlugin(generateHtmlPlugins(page)));

module.exports = merge(common, {
    mode: "production",
    optimization: {
        minimizer: [
            new rspack.SwcJsMinimizerRspackPlugin({
                extractComments: true,
                minimizerOptions: {
                    compress: {
                        drop_console: false
                    },
                    format: {
                        comments: false
                    }
                }
            }),
            new rspack.LightningCssMinimizerRspackPlugin()
        ],
        splitChunks: false,
        mergeDuplicateChunks: true,
        chunkIds: "named"
    },
    module: {
        rules: [
            {
                test: /.pug$/,
                include: path.resolve(__dirname, 'views'),
                use: [{ loader: 'pug-loader' }]
            }
        ]
    },
    plugins: [
        ...htmlPlugins,
        new rspack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("production"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false,
            '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false
        })
    ],
});
