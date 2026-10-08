const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {JSDOM} = require("jsdom");

const adminDirectory = path.resolve(__dirname, "../../../main/webapp/admin/v9");
const readAdmin = file => fs.readFileSync(path.join(adminDirectory, file), "utf8").replace(/\r\n/g, "\n");

/** Creates a real jsTree using the shared administration configuration and text normalization. */
async function createTree(t, nodes) {
    const dom = new JSDOM('<!doctype html><div id="SomStromcek"></div>', {runScripts: "outside-only"});
    const {window} = dom;
    t.after(() => window.close());
    window.eval(fs.readFileSync(require.resolve("jquery/dist/jquery.js"), "utf8"));
    window.eval(fs.readFileSync(require.resolve("jstree/dist/jstree.js"), "utf8"));
    window.eval(readAdmin("src/js/libs/tools/tools.js").replace("export class Tools", "window.Tools = class Tools"));

    const webjet = readAdmin("src/js/webjet.js");
    const normalizeStart = webjet.indexOf("    function internationalToEnglish(");
    assert.ok(normalizeStart >= 0, "The production text normalizer must exist");
    window.eval(webjet.slice(normalizeStart, webjet.indexOf("\n    }", normalizeStart) + 6));
    window.WJ = {internationalToEnglish: window.internationalToEnglish, translate: key => key};

    // Capture the production tree options before replacing the remote data source with fixtures.
    const $ = window.jQuery;
    const originalJstree = $.fn.jstree;
    let options;
    $.fn.jstree = function(config) {
        options = config;
        return this;
    };
    const appInit = readAdmin("src/js/app-init.js");
    const treeStart = appInit.indexOf("    var somStromcek = $('#SomStromcek');");
    const treeEnd = appInit.indexOf("\n    //\n    var select =", treeStart);
    assert.ok(treeStart >= 0 && treeEnd > treeStart, "The production tree initialization must exist");
    window.eval(appInit.slice(treeStart, treeEnd));
    $.fn.jstree = originalJstree;
    options.core.data = nodes;
    const element = $("#SomStromcek");
    const ready = new Promise(resolve => element.one("ready.jstree", resolve));
    element.jstree(options);
    await ready;
    return {tree: element.jstree(true), window};
}

test("Tree search matches escaped labels and preserves their rendered text", async t => {
    const nodes = [
        {id: "ampersand", text: "R&amp;D", query: "R&D"},
        {id: "quotes", text: "Tom&#39;s &quot;News&quot;", query: 'Tom\'s "News"'},
        {id: "markup", text: "&lt;b&gt;News&lt;/b&gt;", query: "<b>News</b>"},
        {id: "entity", text: "Literal &amp;amp; value", query: "Literal &amp; value"},
        {id: "accent", text: "Caf\u00e9 &amp; R\u00e9sum\u00e9", query: "CAFE & RESUME"},
        {id: "icon", text: '<span class="ti ti-folder"></span>Sales &amp; News', query: "Sales & News"},
        {id: "plain", text: "Plain news", query: "PLAIN NEWS"}
    ];
    const {tree, window} = await createTree(t, nodes);
    for (let round = 0; round < 2; round++) {
        for (const node of nodes) {
            tree.search(node.query);
            assert.deepEqual(Array.from(tree._data.search.res), [node.id], `Search must find ${node.id}`);
            assert.equal(tree.get_node(node.id).state.hidden, false);
            assert.equal(tree.get_node(node.id).text, node.text, "Search must preserve the escaped source label");
            tree.clear_search();
            assert.ok(nodes.every(item => !tree.get_node(item.id).state.hidden), "Clearing search must restore all nodes");
        }
    }
    assert.equal(window.document.querySelector("#markup_anchor b"), null, "Encoded markup must remain text");
    assert.equal(window.document.querySelector("#markup_anchor").textContent, "<b>News</b>");
    assert.equal(window.document.querySelector("#entity_anchor").textContent, "Literal &amp; value");
});

test("Tree search ignores icon attributes and keeps HTML-like labels escaped", async t => {
    const label = '&lt;img src=x onerror=&quot;window.searchExecuted=true&quot;&gt;';
    const {tree, window} = await createTree(t, [
        {id: "icon", text: '<span class="ti ti-folder"></span>News'},
        {id: "html", text: label}
    ]);
    tree.search("ti-folder");
    assert.deepEqual(Array.from(tree._data.search.res), [], "Icon attributes are not part of the visible label");
    tree.search('<img src=x onerror="window.searchExecuted=true">');
    assert.deepEqual(Array.from(tree._data.search.res), ["html"]);
    assert.equal(tree.get_node("html").text, label);
    assert.equal(window.document.querySelector("#html_anchor img"), null);
    assert.equal(window.searchExecuted, undefined);
});
