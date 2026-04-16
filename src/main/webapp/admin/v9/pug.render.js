const fs = require("fs");
const glob = require("glob");
const path = require("path");
const pug = require("pug");

function createPugRenderLocals(data, files, outputFile, publicPath) {
    return {
        htmlWebpackPlugin: {
            options: {
                data,
                filename: outputFile
            },
            files
        },
        require: function(assetPath) {
            if (assetPath.includes("favicon")) return publicPath + "images/favicon-cms.ico";
            return publicPath + "images/" + path.basename(assetPath);
        }
    };
}

function compilePugToHtml(options) {
    const templatePath = path.resolve(options.baseDir, "views/pages" + options.page + ".pug");
    const outputFile = path.resolve(options.distDir, "views" + options.page + ".html");

    return pug.renderFile(templatePath, createPugRenderLocals(options.data, options.files, outputFile, options.data.publicPath));
}

function writeHtmlFile(outputFile, html) {
    fs.mkdirSync(path.dirname(outputFile), { recursive: true });
    fs.writeFileSync(outputFile, html);
}

function compileAllPugPages(options) {
    const startTime = Date.now();

    options.pages.forEach(page => {
        const html = compilePugToHtml({
            baseDir: options.baseDir,
            data: options.data,
            distDir: options.distDir,
            files: options.files,
            page
        });
        writeHtmlFile(path.resolve(options.distDir, "views" + page + ".html"), html);
    });

    return Date.now() - startTime;
}

function compileSinglePugPage(options) {
    const html = compilePugToHtml(options);
    writeHtmlFile(path.resolve(options.distDir, "views" + options.page + ".html"), html);
}

function getAllPugFiles(baseDir) {
    return glob.sync("**/*.pug", {
        cwd: path.resolve(baseDir, "views"),
        absolute: true
    });
}

function createPugFileToPageMap(baseDir, pages) {
    const pugFileToPage = new Map();

    pages.forEach(page => {
        const pugPath = path.resolve(baseDir, "views/pages" + page + ".pug");
        pugFileToPage.set(pugPath, page);
    });

    return pugFileToPage;
}

function getAssetFiles(compilation, publicPath, entryOrder) {
    const files = {
        css: [],
        js: []
    };
    const seen = new Set();

    function addAsset(file) {
        if (seen.has(file) || file.endsWith(".map")) return;

        if (file.endsWith(".css")) {
            files.css.push(publicPath + file);
            seen.add(file);
            return;
        }

        if (file.endsWith(".js")) {
            files.js.push(publicPath + file);
            seen.add(file);
        }
    }

    entryOrder.forEach(name => {
        const entrypoint = compilation.entrypoints.get(name);
        if (!entrypoint) return;

        entrypoint.getFiles().forEach(addAsset);
    });

    compilation.entrypoints.forEach((entrypoint, name) => {
        if (entryOrder.includes(name)) return;
        entrypoint.getFiles().forEach(addAsset);
    });

    return files;
}

module.exports = {
    compileAllPugPages,
    compileSinglePugPage,
    createPugFileToPageMap,
    getAllPugFiles,
    getAssetFiles
};