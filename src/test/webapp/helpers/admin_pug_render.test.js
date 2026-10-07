const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const { JSDOM } = require("jsdom");
const pugRenderer = require("../../../main/webapp/admin/v9/pug.render.js");

const webappDir = path.resolve(__dirname, "../../../main/webapp");
const baseDir = path.join(webappDir, "admin/v9");

test("tree width settings includes resolve from compiled distribution templates", (t) => {
    const distDir = fs.mkdtempSync(path.join(os.tmpdir(), "wj-admin-pug-"));
    t.after(() => fs.rmSync(distDir, { recursive: true, force: true }));

    pugRenderer.compileAllPugPages({
        baseDir,
        distDir,
        data: { publicPath: "/admin/v9/dist/" },
        files: { js: [], css: [] },
        pages: ["/settings/configuration", "/settings/translation-keys"]
    });

    const templateFiles = [
        path.join(distDir, "views/settings/configuration.html"),
        path.join(distDir, "views/settings/translation-keys.html"),
        ...["news/admin/index.html", "blog/admin/index.html", "file-archive/admin/folder-tree-archive.html", "enumeration/admin/index.html"]
            .map(file => path.join(webappDir, "apps", file))
    ];

    for (const templateFile of templateFiles) {
        const document = JSDOM.fragment(fs.readFileSync(templateFile, "utf8"));
        const include = document.querySelector('[data-th-include$="/tree-width-settings.html"]');
        assert.ok(include, `Missing tree width settings include in ${templateFile}`);
        const includePath = include.getAttribute("data-th-include");
        assert.ok(includePath.startsWith("admin/v9/dist/"), `Include must use a compiled template: ${includePath}`);
        const fragmentFile = path.join(distDir, includePath.slice("admin/v9/dist/".length));
        assert.ok(fs.existsSync(fragmentFile), `Missing compiled fragment: ${includePath}`);

        const fragment = JSDOM.fragment(fs.readFileSync(fragmentFile, "utf8"));
        assert.equal(fragment.querySelectorAll("#jstreeSettingsModal").length, 1);
        assert.equal(fragment.querySelectorAll("#jstree-settings-treeWidth option").length, 18);
        assert.equal(fragment.querySelector("h5").getAttribute("data-th-text"), "#{datatables.button.settings.js}");
        assert.ok(fragment.querySelector('[data-th-if="${showDeletedTypes == true}"]'));
        assert.equal(fragment.querySelectorAll("link, script[src]").length, 0, "Fragment must not include page layout assets");

        if (templateFile.endsWith("enumeration/admin/index.html")) {
            assert.equal(include.getAttribute("data-th-with"), "showDeletedTypes=true");
        }
    }
});
