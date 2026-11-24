const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const {JSDOM} = require("jsdom");

/** Uses real Editor events and Ajax handling, replacing only the modal display and HTTP transport. */
function fixture(t, itemType = "pages") {
    const dom = new JSDOM('<!doctype html><table id="mirrors"><thead><tr><th>Title</th></tr></thead></table><div id="mirrors_modal"></div>', {url: "https://example.test/"});
    t.after(() => dom.window.close());
    const $ = require("jquery")(dom.window);
    require("datatables.net")(dom.window, $);
    require("datatables.net-editor")(dom.window, $);
    const modal = $("#mirrors_modal");
    $.fn.dataTable.Editor.display.autotest = {
        init() { return this; },
        open(editor, form, callback) { modal.empty().append(form); callback(); },
        close(editor, callback) { callback(); },
        node() { return modal[0]; }
    };
    $("#mirrors").DataTable({
        data: [{id: 101, title: "Autotest first page"}, {id: 102, title: "Autotest second page"}],
        columns: [{data: "title"}], rowId: "id", paging: false, searching: false, info: false
    });
    const submissions = [];
    const editor = new $.fn.dataTable.Editor({
        table: "#mirrors", idSrc: "id", fields: [{name: "title"}], display: "autotest",
        ajax(method, url, data, success) { submissions.push(data); success({data: []}); }
    });
    const requests = [];
    $.ajaxTransport("+*", options => {
        const request = {options};
        return {
            send(headers, complete) {
                request.respond = (json, status = 200) => complete(status, status === 200 ? "OK" : "Service unavailable", {
                    text: JSON.stringify(json)
                }, "Content-Type: application/json");
                requests.push(request);
            },
            abort() {}
        };
    });
    const filename = path.resolve(__dirname, "../../../main/webapp/admin/v9/src/js/pages/web-pages-list/delete-mirrors.js");
    const context = vm.createContext({$, WJ: {translate: key => key}});
    vm.runInContext(fs.readFileSync(filename, "utf8").replace(/^export /gm, ""), context, {filename});
    const endpoint = "/admin/rest/" + (itemType === "pages" ? "web-pages" : "groups") + "/deletion-preview";
    context.bindDeleteMirrors({EDITOR: editor, DATA: {id: "mirrors"}}, endpoint, itemType);
    const open = (rows = [0, 1]) => editor.remove(rows, {
        message: '<div class="col-sm-7 offset-sm-4">Confirm deletion</div>',
        buttons: [
            {text: "Cancel", className: "btn-close-editor", action() { this.close(); }},
            {text: "Delete", className: "autotest-delete", action() { this.submit(); }}
        ]
    });
    return {editor, modal, requests, submissions, open,
        submit: () => modal.find("button.autotest-delete"),
        output: () => modal.find(".delete-mirrors-info")};
}

/** Checks the button and the Editor submit guard, including keyboard or programmatic submission. */
function assertBlocked(f) {
    assert.equal(f.submit().prop("disabled"), true);
    f.editor.submit();
    assert.equal(f.submissions.length, 0);
}

for (const itemType of ["pages", "groups"]) {
    test(`The ${itemType} preview blocks deletion until mirror warnings are loaded`, t => {
        const f = fixture(t, itemType);
        f.open();
        assertBlocked(f);
        assert.equal(f.modal.find("button.btn-close-editor").prop("disabled"), false);
        assert.equal(new URLSearchParams(f.requests[0].options.data).get("ids"), "101,102");
        const fullPath = '/Autotest/<b>Mirror</b>';
        f.requests[0].respond({items: [{fullPath, domainName: "autotest.example"}], hiddenMirrors: true});
        assert.equal(f.output().hasClass("alert-warning"), true);
        assert.equal(f.output().find("p").first().text(), `editor.delete_mirrors.${itemType}.js`);
        assert.equal(f.output().find("li").text(), fullPath + " (autotest.example)");
        assert.equal(f.output().find("b").length, 0);
        assert.equal(f.output().find("p").last().text(), "editor.delete_mirrors.hidden.js");
        assert.equal(f.submit().prop("disabled"), false);
        f.editor.submit();
        assert.equal(f.submissions.length, 1);
        assert.equal(f.submissions[0].action, "remove");
    });
}

test("An empty preview removes the warning and permits deletion", t => {
    const f = fixture(t);
    f.open();
    f.requests[0].respond({items: [], hiddenMirrors: false});
    assert.equal(f.output().length, 0);
    assert.equal(f.submit().prop("disabled"), false);
    f.editor.submit();
    assert.equal(f.submissions.length, 1);
});

test("Preselected cleanup submits every selected page after its file preview is ready", t => {
    const f = fixture(t);
    f.open();
    f.requests[0].respond({items: [], hiddenMirrors: false, pageFileCleanupAvailable: true, pageFileCleanupDefaultChecked: true});
    const choice = f.modal.find(".delete-page-files");
    const input = choice.find("input");
    assert.equal(input.prop("checked"), true);
    assertBlocked(f);
    assert.equal(new URLSearchParams(f.requests[1].options.data).get("ids"), "101,102");
    f.requests[1].respond([
        {fullPath: "/News/<b>Story</b>", files: ["/files/story & photos.pdf"]},
        {fullPath: "/Archive/Story", files: []}
    ]);
    assert.equal(choice.find("strong").eq(0).text(), "/News/<b>Story</b>");
    assert.equal(choice.find("strong").eq(0).siblings("ul").find("li").text(), "/files/story & photos.pdf");
    assert.equal(choice.find("strong").eq(1).text(), "/Archive/Story");
    assert.equal(choice.find("strong").eq(1).next().text(), "editor.delete_files.empty.js");
    assert.equal(choice.find("b").length, 0);
    assert.equal(input.prop("checked"), true);
    assert.equal(f.submit().prop("disabled"), false);
    f.editor.submit();
    for (const row of Object.values(f.submissions[0].data)) assert.equal(row.editorFields.deleteUnusedPageFiles, true);
});

test("A single page shows its files directly and uses the configured unchecked default", t => {
    const f = fixture(t);
    f.open([0]);
    f.requests[0].respond({items: [], hiddenMirrors: false, pageFileCleanupAvailable: true, pageFileCleanupDefaultChecked: false});
    f.requests[1].respond([{fullPath: "/News/Story", files: ["/files/one.pdf", "/images/two.jpg"]}]);
    const choice = f.modal.find(".delete-page-files");
    assert.equal(choice.find("input").prop("checked"), false);
    assert.equal(choice.find("strong").length, 0);
    assert.deepEqual(choice.find("li").map((_, item) => item.textContent).get(), ["/files/one.pdf", "/images/two.jpg"]);
    f.editor.submit();
    assert.equal(f.submissions[0].data["101"].editorFields?.deleteUnusedPageFiles, undefined);
});

test("An editor can clear the default while the file preview is loading", t => {
    const f = fixture(t);
    f.open();
    f.requests[0].respond({items: [], hiddenMirrors: false, pageFileCleanupAvailable: true, pageFileCleanupDefaultChecked: true});
    f.modal.find(".delete-page-files input").prop("checked", false).trigger("change");
    assert.equal(f.submit().prop("disabled"), false);
    f.requests[1].respond([{fullPath: "/News/Story", files: ["/files/one.pdf"]}]);
    f.editor.submit();
    for (const row of Object.values(f.submissions[0].data)) assert.equal(row.editorFields?.deleteUnusedPageFiles, undefined);
});

for (const [result, message] of [[{error: "Scan failed"}, "error"], [[], "empty"]]) {
    test(`The ${message} file preview clears the default and permits ordinary deletion`, t => {
        const f = fixture(t);
        f.open();
        f.requests[0].respond({items: [], hiddenMirrors: false, pageFileCleanupAvailable: true, pageFileCleanupDefaultChecked: true});
        f.requests[1].respond(result);
        const choice = f.modal.find(".delete-page-files");
        assert.equal(choice.find("[role=status]").text(), `editor.delete_files.${message}.js`);
        assert.equal(choice.find("input").prop("disabled"), true);
        assert.equal(choice.find("input").prop("checked"), false);
        assert.equal(f.submit().prop("disabled"), false);
    });
}

// DataTable endpoints can report permission errors with HTTP 200.
for (const [response, status] of [[{}, 503], [{error: "Access denied"}, 200]]) {
    test(`A failed preview (HTTP ${status}) keeps deletion blocked`, t => {
        const f = fixture(t);
        f.open();
        f.requests[0].respond(response, status);
        assert.equal(f.output().hasClass("alert-danger"), true);
        assert.equal(f.output().text(), "editor.delete_mirrors.error.js");
        assertBlocked(f);
    });
}

