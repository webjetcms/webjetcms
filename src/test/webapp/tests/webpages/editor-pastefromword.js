Feature('webpages.editor-pastefromword');

var configName = "ckeditor_pasteFromWord_disallowedContent";
var configDetailUrl = "/admin/rest/settings/configuration/autocomplete/detail?name=" + encodeURIComponent(configName);
var originalConfigState;

var customPasteFromWordDisallowedContent = " table[width,height,border];\n td(*) ; invalid[ ; td[align,valign] ; th(*) ; th[valign] ; p[align] ; span ; col[width] ; ";

var pasteFromWordHtml = '<table width="640" height="120" border="2"><tbody><tr><th class="word-header" align="center" valign="bottom"><span>Header</span></th><td class="word-cell" align="right" valign="middle">Cell</td></tr></tbody></table><p align="justify"><span>Text</span></p>';

async function getPasteFromWordConfigState(I) {
    var response = await I.sendGetRequest(configDetailUrl);
    if (response.status !== 200 || response.data.name !== configName) {
        throw new Error("Unable to read configuration value " + configName);
    }

    return {
        exists: response.data.id != null,
        id: response.data.id,
        value: response.data.value
    };
}

async function restorePasteFromWordConfig(I) {
    if (originalConfigState == null) return;

    var currentConfigState = await getPasteFromWordConfigState(I);

    if (originalConfigState.exists === true) {
        if (currentConfigState.exists === true && currentConfigState.value === originalConfigState.value) return;

        var saveResponse = await I.sendPostRequest("/admin/rest/settings/configuration/add", {
            name: configName,
            value: originalConfigState.value
        });
        if (saveResponse.status !== 201 || saveResponse.data.name !== configName || saveResponse.data.value !== originalConfigState.value) {
            throw new Error("Unable to restore configuration value " + configName);
        }
        return;
    }

    if (currentConfigState.exists === false) return;

    var deleteResponse = await I.sendDeleteRequestWithPayload(
        "/admin/rest/settings/configuration/" + currentConfigState.id,
        { name: configName }
    );
    if (deleteResponse.status !== 200 || deleteResponse.data.result !== true) {
        throw new Error("Unable to remove temporary configuration value " + configName);
    }
}

BeforeSuite(async ({ I }) => {
    originalConfigState = await getPasteFromWordConfigState(I);
});

AfterSuite(async ({ I }) => {
    await restorePasteFromWordConfig(I);
});

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
    } else {
        DTE.waitForCkeditor();
    }

    var result = await I.executeScript(function(html) {
        var eventData = { dataValue: html };
        window.ckEditorInstance.fire("afterPasteFromWord", eventData);

        var container = document.createElement("div");
        container.innerHTML = eventData.dataValue;

        var table = container.querySelector("table");
        var td = container.querySelector("td");
        var th = container.querySelector("th");
        var paragraph = container.querySelector("p");

        return {
            config: window.ckEditorInstance.config.pasteFromWordDisallowedContent,
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
    Document.setConfigValue("ckeditor_pasteFromWord_disallowedContent", "table[width,height,border];td(*);td[align,valign,nowrap];th(*);th[align,valign];p[align];span;col[width]");
});