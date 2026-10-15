const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const {JSDOM} = require("jsdom");
const moment = require("moment");

const adminDirectory = path.resolve(__dirname, "../../../main/webapp/admin/v9");
const xlsx = require("xlsx");
const jquery = require("jquery");

/**
 * Runs the complete import dialog handler with a real XLSX file and browser FileReader.
 * Only the HTTP request and surrounding application services are replaced.
 */
async function importWorkbook(t, fields, rows, chunks = 25, optionsTable = {}) {
    const dom = new JSDOM(`<!doctype html><html><body>
        <div id="datatableImportModal">
            <input name="dt-settings-extend" value="all" type="radio" checked>
            <input name="dt-settings-import" value="update" type="radio" checked>
            <select id="dt-settings-update-by-column"><option value="id">ID</option></select>
            <input id="skip-wrong-data" type="checkbox">
            <label id="insert-file-label"></label><input id="insert-file-name">
            <input id="insert-file" type="file"><div id="import-status"></div>
            <button id="submit-import" class="btn-primary" type="button"><i></i></button>
        </div>
    </body></html>`);
    const window = dom.window;
    const $ = jquery(window);
    t.after(() => window.close());
    $.fn.modal = function () { return this; };
    const requests = [];
    $.ajax = async options => {
        requests.push(JSON.parse(options.data));
        return {};
    };
    const table = {
        DATA: {id: "importTest", fields, columns: [{data: "id"}]},
        getAjaxUrl: () => "/admin/rest/import-test",
        ajax: {reload() {}}
    };
    window.datatableImportModal = {tableId: table.DATA.id, TABLE: table};
    window.chunksQuantity = chunks;
    const context = vm.createContext({
        window,
        document: window.document,
        File: window.File,
        FileReader: window.FileReader,
        setTimeout: window.setTimeout.bind(window),
        require: name => name === "buffer/" ? require("node:buffer") : require(name),
        $,
        moment,
        XLSX: async () => xlsx,
        dtWJ: {getOptionsTableImport: () => optionsTable},
        WJ: {
            translate: key => key === "datatables.export.empty.js" ? "Not specified" : key,
            urlAddPath: (url, suffix) => url + suffix,
            setJsonProperty(object, property, value) {
                const keys = property.split(".");
                const last = keys.pop();
                for (const key of keys) object = object[key] ??= {};
                object[last] = value;
            },
            notifyError: message => { throw new Error(message); }
        }
    });
    const filename = path.join(adminDirectory, "npm_packages/webjetdatatables/export-import.js");
    const source = fs.readFileSync(filename, "utf8").replace(/^export /gm, "");
    vm.runInContext(source, context, {filename});
    context.bindImportButton(table, table.DATA);
    $("#datatableImportModal").trigger("shown.bs.modal");
    await Promise.resolve();

    const workbook = xlsx.utils.book_new();
    xlsx.utils.book_append_sheet(workbook, xlsx.utils.aoa_to_sheet(rows), "Import");
    const bytes = xlsx.write(workbook, {bookType: "xlsx", type: "buffer"});
    const file = new window.File([bytes], "import.xlsx");
    const input = window.document.querySelector("#insert-file");
    Object.defineProperty(input, "files", {value: [file]});
    const parsed = new Promise(resolve => $(window.document).one("file-reader-done", resolve));
    input.dispatchEvent(new window.Event("change"));
    await parsed;

    const submit = $.Event("click");
    $("#submit-import").trigger(submit);
    await submit.result;
    return requests;
}

test("Excel headers include blank first-row cells and clear empty numeric, date and time values", {timeout: 5000}, async t => {
    const fields = [
        {name: "id", data: "id", label: "ID"},
        {name: "price", data: "price", label: "Price", type: "text", attr: {type: "number"}},
        {name: "date", data: "date", label: "Date", type: "datetime", renderFormat: "dt-format-date"},
        {name: "dateTime", data: "dateTime", label: "Date and time", type: "datetime", renderFormat: "dt-format-date-time"},
        {name: "time", data: "time", label: "Time", type: "datetime", renderFormat: "dt-format-time-hm"},
        {name: "seconds", data: "seconds", label: "Seconds", type: "datetime", renderFormat: "dt-format-time-hms"},
        {name: "text", data: "text", label: "Text", type: "text"},
        {name: "omittedPrice", data: "omittedPrice", label: "Omitted price", type: "text", attr: {type: "number"}}
    ];
    const includedFields = fields.slice(0, -1);
    const requests = await importWorkbook(t, fields, [
        [...includedFields.map(field => `${field.label}|${field.data}`), "Unknown|unknown"],
        [1],
        [2, "", " ", "", " ", "", ""],
        [3, "NULL", "NULL", "NULL", "NULL", "NULL", "NULL"],
        [4, 0, "09/28/2026", "09/28/2026 12:30:00", "00:00", "00:00:00", "  Content  ", "Ignored"]
    ]);

    assert.equal(requests.length, 1);
    const request = requests[0];
    assert.deepEqual(request.importedColumns, includedFields.map(field => field.data));
    for (const row of Object.values(request.data)) {
        assert.equal(Object.hasOwn(row, "omittedPrice"), false, "A column absent from the workbook must stay absent from the payload");
        assert.equal(Object.hasOwn(row, "unknown"), false, "Unknown Excel columns must not become entity properties");
    }
    for (const index of [0, 1, 2]) {
        for (const name of ["price", "date", "dateTime", "time", "seconds"]) {
            assert.equal(request.data[index][name], null, `${name} must be null in empty or explicit NULL cells`);
        }
    }
    assert.equal(Object.hasOwn(request.data[0], "text"), false, "An omitted text cell must preserve the existing skip behavior");
    assert.equal(request.data[1].text, "", "An explicit empty text value must remain an empty string");
    assert.equal(request.data[2].text, null);
    assert.equal(request.data[3].price, 0, "Zero must not be treated as an empty number");
    assert.equal(request.data[3].date, new Date(2026, 8, 28).getTime());
    assert.equal(request.data[3].dateTime, new Date(2026, 8, 28, 12, 30).getTime());
    assert.equal(request.data[3].time, new Date(2000, 0, 1).getTime());
    assert.equal(request.data[3].seconds, new Date(2000, 0, 1).getTime());
    assert.equal(request.data[3].text, "Content");
});

test("Header matching accepts changed labels, bare names and lowercase headers after a blank row", {timeout: 5000}, async t => {
    const fields = [
        {name: "id", data: "id", label: "ID"},
        {name: "price", data: "price", label: "Current price", attr: {type: "number"}},
        {name: "quantity", data: "quantity", label: "Quantity", attr: {type: "number"}},
        {name: "lowerCasePrice", data: "lowerCasePrice", label: "Another price", attr: {type: "number"}}
    ];
    const requests = await importWorkbook(t, fields, [
        ["quantity", "Previous label|price", "another price|lowercaseprice", "ID|id"],
        [],
        [0, 12, 34, 5]
    ]);

    assert.deepEqual(requests[0].importedColumns, fields.map(field => field.data));
    assert.deepEqual(requests[0].data, {0: {__rowNum__: 2, id: 5, price: 12, quantity: 0, lowerCasePrice: 34}});
});

test("Every import chunk shares the header columns even when its numeric cells are empty", {timeout: 5000}, async t => {
    const fields = [
        {name: "id", data: "id", label: "ID"},
        {name: "price", data: "price", label: "Price", attr: {type: "number"}}
    ];
    const requests = await importWorkbook(t, fields, [
        ["ID|id", "Price|price"],
        [1], [2, 0], [3, "NULL"], [4, 10], [5]
    ], 2);

    assert.deepEqual(requests.map(request => Object.keys(request.data).length), [2, 2, 1]);
    assert.deepEqual(requests.map(request => request.dzchunkindex), [0, 1, 2]);
    for (const request of requests) {
        assert.deepEqual(request.importedColumns, ["id", "price"]);
        assert.equal(Object.hasOwn(request, "importedColumnsByRow"), false);
    }
    const rows = requests.flatMap(request => Object.values(request.data));
    assert.deepEqual(rows.map(row => row.id), [1, 2, 3, 4, 5]);
    assert.deepEqual(rows.map(row => row.price), [null, 0, null, 10, null]);
});

test("Exported empty values clear numbers and dates without bypassing select option lookup", {timeout: 5000}, async t => {
    const fields = [
        {name: "id", data: "id", label: "ID"},
        {name: "price", data: "price", label: "Price", attr: {type: "number"}},
        {name: "date", data: "date", label: "Date", type: "datetime", renderFormat: "dt-format-date"},
        {name: "status", data: "status", label: "Status", type: "select", renderFormat: "dt-format-select"}
    ];
    const requests = await importWorkbook(t, fields, [
        ["ID|id", "Price|price", "Date|date", "Status|status"],
        [1, "Not specified", "Not specified", "Not specified"],
        [2, 12, "09/28/2026", "Active"]
    ], 25, {"status-Not specified": 7, "status-Active": 1});

    assert.equal(requests[0].data[0].price, null);
    assert.equal(requests[0].data[0].date, null);
    assert.equal(requests[0].data[0].status, 7, "A select label matching the empty sentinel must still resolve to its option ID");
    assert.equal(requests[0].data[1].status, 1);
});

test("Missing multi-select cells stay omitted while supplied options and explicit empty strings are imported", {timeout: 5000}, async t => {
    const fields = [
        {name: "id", data: "id", label: "ID"},
        {name: "tags", data: "tags", label: "Tags", type: "select", array: true}
    ];
    const requests = await importWorkbook(t, fields, [
        ["ID|id", "Tags|tags"],
        [1],
        [2, "First,Second"],
        [3, ""]
    ], 25, {"tags-First": 10, "tags-Second": 20});

    assert.deepEqual(requests[0].importedColumns, ["id", "tags"]);
    assert.equal(Object.hasOwn(requests[0].data[0], "tags"), false, "A missing multi-select cell must not clear existing associations");
    assert.deepEqual(requests[0].data[1].tags, [10, 20]);
    assert.deepEqual(requests[0].data[2].tags, []);
});
