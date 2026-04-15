const fs = require("fs");
const path = require("path");
const pug = require("pug");
const common = require("./rspack.config.common");
const rspack = require("@rspack/core");
const { merge } = require("webpack-merge");

//Pre-compile all PUG templates at config load time (runs once, not on every rebuild)
//This eliminates 47 html-webpack-plugin child compilations that were the main watch-mode bottleneck
const WP_DATA = common.WP_DATA;
const publicPath = WP_DATA.publicPath;

const devFiles = {
    css: [publicPath + 'css/main.style.css'],
    js: [
        publicPath + 'js/runtime.js',
        publicPath + 'js/main.js',
        publicPath + 'js/appInit.js',
        publicPath + 'js/pages_web-pages-list.js',
        publicPath + 'js/pages_gallery.js'
    ]
};

function compilePugToHtml(templateDir) {
    const templatePath = path.resolve(__dirname, "views/pages" + templateDir + ".pug");
    const outputFile = path.resolve(__dirname, "dist/views" + templateDir + ".html");

    return pug.renderFile(templatePath, {
        htmlWebpackPlugin: {
            options: { data: WP_DATA, filename: outputFile },
            files: devFiles
        },
        require: function(p) {
            if (p.includes('favicon')) return publicPath + 'images/favicon-cms.ico';
            return publicPath + 'images/' + path.basename(p);
        }
    });
}

//Write pre-compiled HTML files directly to disk (no HtmlRspackPlugin needed)
console.log("Pre-compiling " + common.PAGES.length + " PUG templates...");
const startTime = Date.now();
common.PAGES.forEach(page => {
    const html = compilePugToHtml(page);
    const outputFile = path.resolve(__dirname, "dist/views" + page + ".html");
    fs.mkdirSync(path.dirname(outputFile), { recursive: true });
    fs.writeFileSync(outputFile, html);
});
console.log("PUG pre-compilation done in " + (Date.now() - startTime) + " ms");

module.exports = merge(common, {
    mode: "development",
    devtool: 'source-map',
    output: {
        //no contenthash in dev mode - prevents PUG recompilation on every JS change (watch mode)
        filename: "js/[name].js",
        chunkFilename: 'js/[id].js',
    },
    cache: true,
    experiments: {
        //enable incremental rebuilds - only reprocess changed modules in watch mode
        incremental: true,
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
