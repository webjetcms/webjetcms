const path = require("path");
const common = require("./rspack.config.common");
const pugRenderer = require("./pug.render");
const rspack = require("@rspack/core");

const WP_DATA = common.WP_DATA;
const distDir = path.resolve(__dirname, "dist");

class PugBuildPlugin {
    apply(compiler) {
        compiler.hooks.afterEmit.tap("PugBuildPlugin", compilation => {
            const assetFiles = pugRenderer.getAssetFiles(compilation, WP_DATA.publicPath, Object.keys(common.entry));
            const ms = pugRenderer.compileAllPugPages({
                baseDir: __dirname,
                data: WP_DATA,
                distDir,
                files: assetFiles,
                pages: common.PAGES
            });

            console.log("PUG production rendering done in " + ms + " ms");
        });
    }
}

module.exports = {
    ...common,
    mode: "production",
    optimization: {
        ...common.optimization,
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
    plugins: [
        ...common.plugins,
        new PugBuildPlugin(),
        new rspack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("production"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false,
            '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false
        })
    ],
};
