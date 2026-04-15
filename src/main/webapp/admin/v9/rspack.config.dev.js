const common = require("./rspack.config.common");
const rspack = require("@rspack/core");
const { merge } = require("webpack-merge");

module.exports = merge(common, {
    mode: "development",
    devtool: 'source-map',
    output: {
        //no contenthash in dev mode - prevents PUG recompilation on every JS change (watch mode)
        filename: "js/[name].js",
        chunkFilename: 'js/[id].js',
    },
    plugins: [
        new rspack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("development"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false,
            '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false
        })
    ],
});
