const path = require("path");
const common = require("./webpack.common");
const { merge } = require("webpack-merge");
const { DefinePlugin } = require('webpack');

module.exports = merge(common, {
    mode: "development",
    devtool: 'source-map',
    module: {
        rules: [
        ]
    },
    plugins: [
        new DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("development"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false
        })
    ],
});
