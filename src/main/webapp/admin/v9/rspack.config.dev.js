const common = require("./rspack.config.common");
const rspack = require("@rspack/core");
const { merge } = require("webpack-merge");

module.exports = merge(common, {
    mode: "development",
    devtool: 'source-map',
    plugins: [
        new rspack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("development"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false,
            '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false
        })
    ],
});
