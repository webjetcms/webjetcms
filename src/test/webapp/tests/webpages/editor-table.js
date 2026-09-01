Feature('webpages.editor-table');

var defaultConfig = {
    class: "table table-sm tabulkaStandard",
    cols: "5",
    rows: "2",
    width: "100%",
    height: "",
    border: "1",
    cellpadding: "1",
    cellspacing: "1",
    wrapperClass: "table-responsive"
};

var defaultPasteFromWordDisallowedContent = "table[width],table[height],table[border],td(*),td[valign],td[align],th(*),th[valign],th[align],p[align],span,col[width]";

var customPasteFromWordDisallowedContent = " table[width],\n table[height] , table[border], td(*) , td[valign], th(*), th[valign], p[align], span, col[width], , ";

var pasteFromWordHtml = '<table width="640" height="120" border="2"><tbody><tr><th class="word-header" align="center" valign="bottom"><span>Header</span></th><td class="word-cell" align="right" valign="middle">Cell</td></tr></tbody></table><p align="justify"><span>Text</span></p>';

var customConfig = {
    class: "customTableClass",
    cols: "3",
    rows: "4",
    width: "80%",
    height: "200px",
    border: "2",
    cellpadding: "5",
    cellspacing: "3",
    wrapperClass: "customWrapperClass"
};

var customConfigEmpty = {
    class: "",
    cols: "3",
    rows: "4",
    width: "",
    height: "",
    border: "",
    cellpadding: "",
    cellspacing: "",
    wrapperClass: ""
};

Before(({ login }) => {
    login('admin');
});

function setCustomTableConfig(I, Document, config) {
    I.say("Setting custom CKEditor table config");
    Document.setConfigValue("ckeditor_table_class", config.class);
    Document.setConfigValue("ckeditor_table_cols", config.cols);
    Document.setConfigValue("ckeditor_table_rows", config.rows);
    Document.setConfigValue("ckeditor_table_width", config.width);
    Document.setConfigValue("ckeditor_table_height", config.height);
    Document.setConfigValue("ckeditor_table_border", config.border);
    Document.setConfigValue("ckeditor_table_cellpadding", config.cellpadding);
    Document.setConfigValue("ckeditor_table_cellspacing", config.cellspacing);
    Document.setConfigValue("ckeditor_table_wrapper_class", config.wrapperClass);
}

function setPasteFromWordConfig(I, Document, value) {
    I.say("Setting CKEditor paste from Word filter config");
    Document.setConfigValue("ckeditor_pasteFromWord_disallowedContent", value);
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

async function checkCkEditorValue(I, label, expectedValue) {
    I.say("Checking CKEditor dialog field " + label + " for expected value: " + expectedValue);
    var requiredAppend = "";
    if ("Stĺpce" == label || "Riadky" == label) requiredAppend = "*";
    var baseId = await I.grabAttributeFrom(locate(".cke_dialog_container label.cke_dialog_ui_labeled_label").withTextEquals(label+requiredAppend), 'for');
    var value = await I.grabValueFrom("#" + baseId);
    I.assertEqual(value, expectedValue, label + " value " + value + " doesn't match " + expectedValue);
};

async function verifyTableConfig(I, DTE, Browser, tableConfig, quickTable) {

    I.say("Verifying CKEditor table config in dialog");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=266");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.wait(2);

    I.clickCss('.cke_button.cke_button__table.cke_button_.cke_button_off');
    I.waitForElement('.cke_button.cke_button__table.cke_button_.cke_button_on', 10);

    if (quickTable===true) {
        // volba velkosti tabulky
        for (let i = 1; i < tableConfig.rows; i++) {
            I.pressKey('ArrowDown');
        }
        for (let j = 1; j < tableConfig.cols; j++) {
            I.pressKey('ArrowRight');
        }
        I.pressKey('Enter');
        if (Browser.isFirefox()) I.wait(1);
        // vidim tabulku v editore
        I.clickCss(".cke_path a.cke_path_item:nth-child(2)"); //klikni na body p v navbare dole v ckeditore
        if (Browser.isFirefox()) I.wait(3);
        I.pressKey('ArrowUp');
        if (Browser.isFirefox()) {
            //ff ma nejak inak kurzor a je potrebne este ist hore
            I.pressKey('ArrowUp');
        }
        I.waitForElement(locate('.cke_path_item').withText('table'), 10);
        I.wait(1);
        I.switchTo(".cke_wysiwyg_frame.cke_reset");
        I.rightClick("table tr th:nth-child(1)");
        I.wait(1);

        I.switchTo();
        I.switchTo(".cke_menu_panel iframe.cke_panel_frame");
        I.click(locate("a.cke_menubutton").withText("Vlastnosti tabuľky"));
        I.wait(1);

        I.switchTo();
    } else {
        I.switchTo(".cke_panel_frame");
        I.wait(3);
        I.clickCss(".cke_colormore");
        I.switchTo();
    }

    I.waitForElement(".cke_dialog_container", 10);

    await checkCkEditorValue(I, "Stĺpce", tableConfig.cols);
    await checkCkEditorValue(I, "Riadky", tableConfig.rows);
    await checkCkEditorValue(I, "Šírka", tableConfig.width);
    await checkCkEditorValue(I, "Výška", tableConfig.height);
    await checkCkEditorValue(I, "Šírka orámovania", tableConfig.border);
    await checkCkEditorValue(I, "Vzdialenosť buniek (cell spacing)", tableConfig.cellspacing);
    await checkCkEditorValue(I, "Odsadenie obsahu (cell padding)", tableConfig.cellpadding);

    I.click(locate(".cke_dialog_tab").withText("Rozšírené"));

    await checkCkEditorValue(I, "Triedy štýlu", tableConfig.class);

    I.clickCss(".cke_dialog_ui_button.cke_dialog_ui_button_ok");

    //
    I.say("Verifying wrapper class around table in source code");
    let htmlCode = await DTE.getCkeditor();
    let wrapperClass = tableConfig.wrapperClass;

    I.say("wrapperClass='" + wrapperClass + "' htmlCode='" + htmlCode + "'");

    if (wrapperClass==="") {
        I.assertFalse(htmlCode.includes('<div'), "Wrapper class div should not be present");
    } else {
        I.assertTrue(htmlCode.includes('class="' + wrapperClass + '"'), "Wrapper class div not found");
    }
}

Scenario('table config-custom', async ({ I, DT, DTE, Document, Browser }) => {
    setCustomTableConfig(I, Document, customConfig);

    await verifyTableConfig(I, DTE, Browser, customConfig, false);
    await verifyTableConfig(I, DTE, Browser, customConfig, true);
});

Scenario('table config-custom-empty', async ({ I, DT, DTE, Document, Browser }) => {
    setCustomTableConfig(I, Document, customConfigEmpty);

    await verifyTableConfig(I, DTE, Browser, customConfigEmpty, false);
    await verifyTableConfig(I, DTE, Browser, customConfigEmpty, true);
});

Scenario('table config-default', async ({ I, DT, DTE, Document, Browser }) => {
    setCustomTableConfig(I, Document, defaultConfig);

    await verifyTableConfig(I, DTE, Browser, defaultConfig, false);
    await verifyTableConfig(I, DTE, Browser, defaultConfig, true);
});

Scenario('pasteFromWord filter-custom', async ({ I, DTE, Document }) => {
    setPasteFromWordConfig(I, Document, customPasteFromWordDisallowedContent);

    var result = await getPasteFromWordFilterResult(I, DTE);

    assertPasteFromWordFilterResult(I, result, {
        config: customPasteFromWordDisallowedContent,
        tableWidth: null,
        tableHeight: null,
        tableBorder: null,
        tdClass: null,
        tdAlign: "right",
        tdValign: null,
        thClass: null,
        thAlign: "center",
        thValign: null,
        paragraphAlign: null,
        hasSpan: false
    });
});

Scenario('pasteFromWord filter-pageBuilder-custom', async ({ I, DTE, Document }) => {
    setPasteFromWordConfig(I, Document, customPasteFromWordDisallowedContent);
    Document.resetPageBuilderMode();

    var result = await getPasteFromWordFilterResult(I, DTE, true);

    assertPasteFromWordFilterResult(I, result, {
        config: customPasteFromWordDisallowedContent,
        tableWidth: null,
        tableHeight: null,
        tableBorder: null,
        tdClass: null,
        tdAlign: "right",
        tdValign: null,
        thClass: null,
        thAlign: "center",
        thValign: null,
        paragraphAlign: null,
        hasSpan: false
    });
});

Scenario('pasteFromWord filter-pageBuilder-empty', async ({ I, DTE, Document }) => {
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

Scenario('pasteFromWord filter-empty', async ({ I, DTE, Document }) => {
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

Scenario('pasteFromWord filter-default', async ({ I, DTE, Document }) => {
    setPasteFromWordConfig(I, Document, defaultPasteFromWordDisallowedContent);

    var result = await getPasteFromWordFilterResult(I, DTE);

    assertPasteFromWordFilterResult(I, result, {
        config: defaultPasteFromWordDisallowedContent,
        tableWidth: null,
        tableHeight: null,
        tableBorder: null,
        tdClass: null,
        tdAlign: null,
        tdValign: null,
        thClass: null,
        thAlign: null,
        thValign: null,
        paragraphAlign: null,
        hasSpan: false
    });
});
