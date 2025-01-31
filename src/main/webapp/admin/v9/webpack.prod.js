const path = require("path");
const common = require("./webpack.common");
const { merge } = require("webpack-merge");
const { DefinePlugin } = require('webpack');
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
        mergeDuplicateChunks: true,
        chunkIds: "named"
    },
    module: {
        rules: [

        ]
    },
    plugins: [
        new DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("production"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false
        })
    ],
});
