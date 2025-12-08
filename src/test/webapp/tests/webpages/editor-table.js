Feature('webpages.editor-table');

var defaultConfig = {
    class: "table table-sm tabulkaStandard",
    cols: "5",
    rows: "2",
    width: "100%",
    height: "",
    border: "1",
    cellpadding: "1",
    cellspacing: "1"
};

var customConfig = {
    class: "customTableClass",
    cols: "3",
    rows: "4",
    width: "80%",
    height: "200px",
    border: "2",
    cellpadding: "5",
    cellspacing: "3"
};

var customConfigEmpty = {
    class: "customTableClass",
    cols: "3",
    rows: "4",
    width: "",
    height: "",
    border: "",
    cellpadding: "",
    cellspacing: ""
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
    I.wait(5);

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