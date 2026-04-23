const path = require("path");
const fs = require("fs");
const http = require("http");
const common = require("./rspack.config.common");
const pugRenderer = require("./pug.render");
const rspack = require("@rspack/core");

//Pre-compile all PUG templates at config load time (runs once, not on every rebuild)
//This eliminates 47 html-webpack-plugin child compilations that were the main watch-mode bottleneck
const WP_DATA = common.WP_DATA;
const publicPath = WP_DATA.publicPath;
const distDir = path.resolve(__dirname, "dist");
const devReloadEnabled = process.env.ADMIN_V9_DEV_RELOAD === "1";
const devReloadPort = Number(process.env.ADMIN_V9_DEV_RELOAD_PORT || 35729);
const devReloadClientSource = path.resolve(__dirname, "src/dev/dev-reload-client.js");
const devReloadClientOutput = path.resolve(distDir, "js/dev-reload-client.js");
const devReloadClientPublicPath = publicPath + "js/dev-reload-client.js";

function formatLogTimestamp(date = new Date()) {
    return date.toTimeString().slice(0, 8);
}

function formatDuration(ms) {
    if (ms < 1000) return ms + " ms";

    return (ms / 1000).toFixed(2) + " s";
}

function logWithTimestamp(message) {
    console.log("[" + formatLogTimestamp() + "] " + message);
}

function writeDevReloadClient() {
    if (!devReloadEnabled) return;

    const source = fs.readFileSync(devReloadClientSource, "utf8");
    const clientCode = source.replace(/__ADMIN_V9_DEV_RELOAD_PORT__/g, String(devReloadPort));

    fs.mkdirSync(path.dirname(devReloadClientOutput), { recursive: true });
    fs.writeFileSync(devReloadClientOutput, clientCode);
}

function notifyDevReloadServer() {
    if (!devReloadEnabled) return;

    const payload = JSON.stringify({
        kind: arguments[0] || "full",
    });

    const request = http.request({
        hostname: "127.0.0.1",
        port: devReloadPort,
        path: "/reload",
        method: "POST",
        timeout: 1000,
        headers: {
            "Content-Type": "application/json",
            "Content-Length": Buffer.byteLength(payload),
        },
    }, (response) => {
        response.resume();
    });

    request.on("error", () => {});
    request.on("timeout", () => request.destroy());
    request.end(payload);
}

function isCssOnlyChange(modifiedFiles) {
    if (!modifiedFiles || modifiedFiles.size === 0) return false;

    let hasCssSource = false;

    for (const filePath of modifiedFiles) {
        if (filePath.endsWith(".css") || filePath.endsWith(".scss") || filePath.endsWith(".sass")) {
            hasCssSource = true;
            continue;
        }

        return false;
    }

    return hasCssSource;
}

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

if (devReloadEnabled) {
    writeDevReloadClient();
    devFiles.js.push(devReloadClientPublicPath);
}

//Collect all .pug files for watching (pages + partials + modals + mixins)
const allPugFiles = pugRenderer.getAllPugFiles(__dirname);

//Map page .pug file paths to their PAGES entry for targeted recompilation
const pugFileToPage = pugRenderer.createPugFileToPageMap(__dirname, common.PAGES);

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
                logWithTimestamp("PUG shared partial changed, re-compiling all " + common.PAGES.length + " pages...");
                const ms = pugRenderer.compileAllPugPages({
                    baseDir: __dirname,
                    data: WP_DATA,
                    distDir,
                    files: devFiles,
                    pages: common.PAGES
                });
                logWithTimestamp("PUG re-compilation done in " + formatDuration(ms));
            } else {
                //Only page-specific files changed - recompile only those
                changedPugFiles.forEach(f => {
                    const page = pugFileToPage.get(f);
                    if (page) {
                        const startTime = Date.now();
                        pugRenderer.compileSinglePugPage({
                            baseDir: __dirname,
                            data: WP_DATA,
                            distDir,
                            files: devFiles,
                            page
                        });
                        logWithTimestamp("PUG re-compiled " + page + " in " + formatDuration(Date.now() - startTime));
                    }
                });
            }
        });
    }
}

class DevReloadPlugin {
    constructor() {
        this.isInitialBuild = true;
        this.lastReloadKind = "full";
    }

    apply(compiler) {
        if (!devReloadEnabled) return;

        compiler.hooks.beforeRun.tap("DevReloadPlugin", () => {
            writeDevReloadClient();
        });

        compiler.hooks.watchRun.tap("DevReloadPlugin", (watchCompiler) => {
            writeDevReloadClient();
            this.lastReloadKind = isCssOnlyChange(watchCompiler.modifiedFiles) ? "css" : "full";
        });

        compiler.hooks.done.tap("DevReloadPlugin", (stats) => {
            if (stats.hasErrors()) return;

            if (this.isInitialBuild) {
                this.isInitialBuild = false;
                return;
            }

            notifyDevReloadServer(this.lastReloadKind);
            this.lastReloadKind = "full";
        });
    }
}

//Initial compilation: write all pre-compiled HTML files to disk
logWithTimestamp("Pre-compiling " + common.PAGES.length + " PUG templates...");
const ms = pugRenderer.compileAllPugPages({
    baseDir: __dirname,
    data: WP_DATA,
    distDir,
    files: devFiles,
    pages: common.PAGES
});
logWithTimestamp("PUG pre-compilation done in " + formatDuration(ms));

module.exports = {
    ...common,
    mode: "development",
    devtool: 'source-map',
    output: {
        ...common.output,
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
        ...common.plugins,
        new PugWatchPlugin(),
        new DevReloadPlugin(),
        new rspack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify("development"),
            '__VUE_OPTIONS_API__': true,
            '__VUE_PROD_DEVTOOLS__': false,
            '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': false
        })
    ],
};
