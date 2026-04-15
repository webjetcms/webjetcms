const fs = require("fs");
const glob = require("glob");
const path = require("path");
const pug = require("pug");
const common = require("./rspack.config.common");
const rspack = require("@rspack/core");
const { merge } = require("webpack-merge");

//Pre-compile all PUG templates at config load time (runs once, not on every rebuild)
//This eliminates 47 html-webpack-plugin child compilations that were the main watch-mode bottleneck
const WP_DATA = common.WP_DATA;
const publicPath = WP_DATA.publicPath;
const viewsDir = path.resolve(__dirname, "views");

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

function compileAllPugPages() {
    const startTime = Date.now();
    common.PAGES.forEach(page => {
        const html = compilePugToHtml(page);
        const outputFile = path.resolve(__dirname, "dist/views" + page + ".html");
        fs.mkdirSync(path.dirname(outputFile), { recursive: true });
        fs.writeFileSync(outputFile, html);
    });
    return Date.now() - startTime;
}

function compileSinglePugPage(page) {
    const html = compilePugToHtml(page);
    const outputFile = path.resolve(__dirname, "dist/views" + page + ".html");
    fs.mkdirSync(path.dirname(outputFile), { recursive: true });
    fs.writeFileSync(outputFile, html);
}

//Collect all .pug files for watching (pages + partials + modals + mixins)
const allPugFiles = glob.sync("**/*.pug", { cwd: viewsDir, absolute: true });

//Map page .pug file paths to their PAGES entry for targeted recompilation
const pugFileToPage = new Map();
common.PAGES.forEach(page => {
    const pugPath = path.resolve(__dirname, "views/pages" + page + ".pug");
    pugFileToPage.set(pugPath, page);
});

/**
 * Custom rspack plugin that watches .pug files and re-compiles them on change.
 * - If a page .pug file changes: only that page is re-compiled
 * - If a shared partial/layout/mixin changes: all pages are re-compiled
 */
class PugWatchPlugin {
    apply(compiler) {
        //Add all .pug files to watched dependencies so rspack detects changes
        compiler.hooks.afterCompile.tap("PugWatchPlugin", (compilation) => {
            allPugFiles.forEach(f => compilation.fileDependencies.add(f));
        });

        //On watch rebuild, check if any .pug files changed and re-compile
        compiler.hooks.watchRun.tap("PugWatchPlugin", (compiler) => {
            const changed = compiler.modifiedFiles;
            if (!changed) return;

            const changedPugFiles = [];
            for (const f of changed) {
                if (f.endsWith(".pug")) changedPugFiles.push(f);
            }
            if (changedPugFiles.length === 0) return;

            //Check if any changed file is a shared partial (not in pages/)
            const hasSharedChange = changedPugFiles.some(f => !pugFileToPage.has(f));

            if (hasSharedChange) {
                //Shared partial changed - must recompile all pages
                console.log("PUG shared partial changed, re-compiling all " + common.PAGES.length + " pages...");
                const ms = compileAllPugPages();
                console.log("PUG re-compilation done in " + ms + " ms");
            } else {
                //Only page-specific files changed - recompile only those
                changedPugFiles.forEach(f => {
                    const page = pugFileToPage.get(f);
                    if (page) {
                        const startTime = Date.now();
                        compileSinglePugPage(page);
                        console.log("PUG re-compiled " + page + " in " + (Date.now() - startTime) + " ms");
                    }
                });
            }
        });
    }
}

//Initial compilation: write all pre-compiled HTML files to disk
console.log("Pre-compiling " + common.PAGES.length + " PUG templates...");
const ms = compileAllPugPages();
console.log("PUG pre-compilation done in " + ms + " ms");

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
        new PugWatchPlugin(),
        new rspack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("development"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false,
            '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false
        })
    ],
});
