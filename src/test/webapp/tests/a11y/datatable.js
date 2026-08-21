Feature('a11y.datatable');

Before(({ I, login }) => {
    login('admin');
});

Scenario('basic datatable', async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    //mark first row as selected to reveal buttons
    I.forceClick(".dt-scroll-body tbody tr:nth-child(1) td.dt-select-td");
    I.waitForElement(".dt-buttons button.btn-danger:not(.disabled)", 5);

    await a11y.check();
    I.amOnPage("/apps/news/admin/");
    await a11y.check();
    I.amOnPage("/admin/v9/users/user-list/");
    await a11y.check();
    I.amOnPage("/apps/basket/admin/");
    await a11y.check();
});

Scenario('filter', async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContains("tempName", "page");
    await a11y.check();
});

Scenario('editor - with error messages', async ({ I, DT, DTE, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    //save empty form to trigger error messages
    DTE.save();
    I.pressKey("Escape");
    await a11y.check();
});

Scenario("toggle buttons", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    await a11y.check();
});

Scenario("export modal", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click(DT.btn.export_button);
    I.waitForElement("#datatableExportModal", 5);
    await a11y.check();
});

Scenario("import modal", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click(DT.btn.import_button);
    I.waitForElement("#datatableImportModal", 5);
    await a11y.check();
});

Scenario("p27: input with buttons", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/banner/admin/?id=2");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");
    await a11y.check();
});

Scenario("p27: input with buttons-top-filter", async ({ I, a11y }) => {
    I.amOnPage("/apps/contact/admin/");
    await a11y.check();

    I.amOnPage("/admin/v9/search/index/");
    I.wait(1); //wait for datatable to initialize
    await a11y.check();

    I.amOnPage("/apps/form/admin/detail/?formName=Form-with-redirection-to-Spring-App");
    await a11y.check();
});

Scenario("p28: select", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/?id=2");
    DTE.waitForEditor("enumerationTypeDataTable");
    await a11y.check();
});

Scenario("p28: select-top-filter", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/news/admin/");
    await a11y.check();
});

Scenario("p29: checkbox-access, p30: fieldset/legend", async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=92");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.clickCss("#pills-dt-datatableInit-access-tab")
    await a11y.check();
    //TODO: fieldset/legend
});

Scenario("p29: checkbox-main", async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=92");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab")
    await a11y.check();
});

Scenario("p31: wysiwyg field", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/inquiry/admin/?id=1");
    DTE.waitForEditor("inquiryDataTable");
    await a11y.check();
    //TODO: tab inside wysiwyg field - it's not possible to get out
});

Scenario("p34: help text", async ({ I, DTE, Apps, a11y }) => {
    Apps.openAppEditor(77667);
    //I.switchTo('#editorComponent');
    I.waitForText("Povoliť viacero odpovedí", 10, ".col-form-label");
    await a11y.check("iframe.cke_dialog_ui_iframe #editorComponent");
});

Scenario("p40: is-not-public contrast", async ({ I, DTE, Apps, a11y }) => {
    I.amOnPage("/apps/forum/admin/");
    await a11y.check();
});

Scenario("p41: column visibility settings", async ({ I, DTE, Apps, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");

    var tableId = "datatableInit";
    var container = "#"+tableId+"_wrapper";
    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection ul[role=menu] div.dt-button-collection ul[role=menu]");

    await a11y.check("ul.dropdown-menu.show div.dt-button-collection ul.dropdown-menu.show");
});

Scenario('p42: Overenie zobrazenia tooltipov', async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-advanced-tab");

    const titleTooltipButton = "div.DTE_Field_Name_moveStyleToHead button.btn-tooltip";
    const tooltipLabel = await I.grabAttributeFrom(titleTooltipButton, "aria-label");
    I.assertTrue(tooltipLabel.length > 0, "Tooltip button must have an accessible name");

    I.executeScript((selector) => document.querySelector(selector).focus(), titleTooltipButton);
    I.waitForVisible("div.tooltip.show", 5);
    const tooltipId = await I.grabAttributeFrom(titleTooltipButton, "aria-describedby");
    I.assertTrue(tooltipId.length > 0, "Tooltip button must reference the visible tooltip");

    await a11y.check("#datatableInit_modal");

    I.pressKey("Escape");
    I.waitForInvisible("div.tooltip.show", 5);
    I.seeElement("#datatableInit_modal");
});

Scenario("p43: contrast", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=269");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    await a11y.check("#SomStromcek");
});