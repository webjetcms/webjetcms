Feature('webpages.editor-pastefromword');

var configName = "ckeditor_pasteFromWord_disallowedContent";
var configDetailUrl = "/admin/rest/settings/configuration/autocomplete/detail?name=" + encodeURIComponent(configName);
var originalConfigState;

var customPasteFromWordDisallowedContent = " table[width,height,border];\n td(*) ; invalid[ ; td[align,valign] ; th(*) ; th[valign] ; p[align] ; span ; col[width] ; ";

var pasteFromWordHtml = '<table width="640" height="120" border="2"><tbody><tr><th class="word-header" align="center" valign="bottom"><span>Header</span></th><td class="word-cell" align="right" valign="middle">Cell</td></tr></tbody></table><p align="justify"><span>Text</span></p>';

Before(({ login }) => {
    login('admin');
});

function setPasteFromWordConfig(I, Document, value) {
    I.say("Setting CKEditor paste from Word filter config");
    Document.setConfigValue(configName, value);
}

async function getPasteFromWordFilterResult(I, DTE, pageBuilder) {
    var docId = pageBuilder === true ? 57 : 266;
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();

    if (pageBuilder === true) {
        I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);
        I.switchTo("#DTE_Field_data-pageBuilderIframe");
        I.waitForElement("div.cke_inner", 10);
        I.waitForText("Odstavec a zarovnanie", 10);
        I.waitForElement("#wjInline-docdata [data-ckeditor-instance]", 10);
    } else {
        DTE.waitForCkeditor();
    }

    var result = await I.executeScript(function(htmlOrRootElement, iframeHtml) {
        var html = typeof iframeHtml === "string" ? iframeHtml : htmlOrRootElement;
        var editor = window.ckEditorInstance;
        var pageBuilderEditorElement = document.querySelector("#wjInline-docdata [data-ckeditor-instance]");
        if (pageBuilderEditorElement !== null) {
            var editorName = pageBuilderEditorElement.getAttribute("data-ckeditor-instance");
            editor = window.CKEDITOR.instances[editorName];
        }
        if (editor == null) {
            throw new Error("CKEditor instance is not available");
        }

        var eventData = { dataValue: html };
        editor.fire("afterPasteFromWord", eventData);

        var container = document.createElement("div");
        container.innerHTML = eventData.dataValue;

        var table = container.querySelector("table");
        var td = container.querySelector("td");
        var th = container.querySelector("th");
        var paragraph = container.querySelector("p");

        if (table === null || td === null || th === null || paragraph === null) {
            throw new Error("Unexpected pasteFromWord HTML: " + eventData.dataValue);
        }

        return {
            config: editor.config.pasteFromWordDisallowedContent,
            tableWidth: table.getAttribute("width"),
            tableHeight: table.getAttribute("height"),
            tableBorder: table.getAttribute("border"),
            tdClass: td.getAttribute("class"),
            tdAlign: td.getAttribute("align"),
            tdValign: td.getAttribute("valign"),
            thClass: th.getAttribute("class"),
            thAlign: th.getAttribute("align"),
            thValign: th.getAttribute("valign"),
            paragraphAlign: paragraph.getAttribute("align"),
            hasSpan: container.querySelector("span") !== null
        };
    }, pasteFromWordHtml);

    if (pageBuilder === true) I.switchTo();
    return result;
}

function assertPasteFromWordFilterResult(I, result, expected) {
    Object.keys(expected).forEach(function(key) {
        I.assertEqual(result[key], expected[key], "Unexpected paste from Word filter result for " + key);
    });
}

Scenario('pasteFromWord filter-custom @singlethread', async ({ I, DTE, Document }) => {
    setPasteFromWordConfig(I, Document, customPasteFromWordDisallowedContent);

    var result = await getPasteFromWordFilterResult(I, DTE);

    assertPasteFromWordFilterResult(I, result, {
        config: customPasteFromWordDisallowedContent,
        tableWidth: null,
        tableHeight: null,
        tableBorder: null,
        tdClass: null,
        tdAlign: null,
        tdValign: null,
        thClass: null,
        thAlign: "center",
        thValign: null,
        paragraphAlign: null,
        hasSpan: false
    });
});

Scenario('pasteFromWord filter-pageBuilder-custom @singlethread', async ({ I, DTE, Document }) => {
    setPasteFromWordConfig(I, Document, customPasteFromWordDisallowedContent);
    Document.resetPageBuilderMode();

    var result = await getPasteFromWordFilterResult(I, DTE, true);

    assertPasteFromWordFilterResult(I, result, {
        config: customPasteFromWordDisallowedContent,
        tableWidth: null,
        tableHeight: null,
        tableBorder: null,
        tdClass: null,
        tdAlign: null,
        tdValign: null,
        thClass: null,
        thAlign: "center",
        thValign: null,
        paragraphAlign: null,
        hasSpan: false
    });
});

Scenario('pasteFromWord filter-pageBuilder-empty @singlethread', async ({ I, DTE, Document }) => {
    setPasteFromWordConfig(I, Document, "");
    Document.resetPageBuilderMode();

    var result = await getPasteFromWordFilterResult(I, DTE, true);

    assertPasteFromWordFilterResult(I, result, {
        config: "",
        tableWidth: "640",
        tableHeight: "120",
        tableBorder: "2",
        tdClass: "word-cell",
        tdAlign: "right",
        tdValign: "middle",
        thClass: "word-header",
        thAlign: "center",
        thValign: "bottom",
        paragraphAlign: "justify",
        hasSpan: true
    });
});

Scenario('pasteFromWord filter-empty @singlethread', async ({ I, DTE, Document }) => {
    setPasteFromWordConfig(I, Document, "");

    var result = await getPasteFromWordFilterResult(I, DTE);

    assertPasteFromWordFilterResult(I, result, {
        config: "",
        tableWidth: "640",
        tableHeight: "120",
        tableBorder: "2",
        tdClass: "word-cell",
        tdAlign: "right",
        tdValign: "middle",
        thClass: "word-header",
        thAlign: "center",
        thValign: "bottom",
        paragraphAlign: "justify",
        hasSpan: true
    });
});

Scenario('reset', async ({ I, Document }) => {
    //reset back to default value
    setPasteFromWordConfig(I, Document, "table[width,height,border];td(*);td[align,valign,nowrap];th(*);th[align,valign];p[align];span;col[width]");
});