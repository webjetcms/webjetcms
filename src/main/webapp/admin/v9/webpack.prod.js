const path = require("path");
const common = require("./webpack.common");
const { merge } = require("webpack-merge");
const { DefinePlugin } = require('webpack');
const { CleanWebpackPlugin } = require("clean-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");

module.exports = merge(common, {
    mode: "production",
    optimization: {
        minimizer: [
            new TerserPlugin({
                terserOptions: {
                  output: {
                    comments: false,
                  },
                },
                extractComments: true,
            }),
        ],
        splitChunks: false,
        mergeDuplicateChunks: true
    },
    module: {
        rules: [

        ]
    },
    plugins: [
        new CleanWebpackPlugin(),
        new DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("production"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false
        })
    ],
});
