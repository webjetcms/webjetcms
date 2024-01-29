Feature('apps.restaurant-menu.meals');

Before(({ login }) => {
    login('admin');
});

Scenario('meals screens', ({ I, DTE, Document }) => { 
    I.amOnPage("/apps/restaurant-menu/admin/meals/");
    Document.screenshot("/redactor/apps/restaurant-menu/meals-data-table.png", 1600, 600);

    I.click("button.buttons-create");
    DTE.waitForEditor("mealsDataTable");

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/restaurant-menu/meals-editor.png");

    DTE.selectOption('editorFields\\.alergensArr', '1 - Obilniny');
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("7 - Mlieko"));

    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/restaurant-menu/meals-allergens-list.png");
});

Scenario('menu screens', async ({ I, DT, Document }) => {  
    I.amOnPage("/apps/restaurant-menu/admin/");

    await setDate(I, DT, "28.11.2023");

    Document.screenshot("/redactor/apps/restaurant-menu/menu-data-table.png", 1400, 400);
    Document.screenshotElement("#pills-meals", "/redactor/apps/restaurant-menu/menu-external-filter.png");
    Document.screenshotElement("div.dt-extfilter-dayDate", "/redactor/apps/restaurant-menu/menu-external-filter-date.png");
    Document.screenshotElement("#menuType", "/redactor/apps/restaurant-menu/menu-external-filter-type.png");
    Document.screenshotElement("div.status-info", "/redactor/apps/restaurant-menu/menu-external-filter-status-a.png");

    I.click("#menuType > button[data-menu-type=weeks]");

    Document.screenshot("/redactor/apps/restaurant-menu/menu-data-table-weeks.png", 1650, 1000);
    Document.screenshotElement("div.status-info", "/redactor/apps/restaurant-menu/menu-external-filter-status-b.png");
});

Scenario('menu app screens', async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=74621");
    DTE.waitForEditor();

    I.waitForElement(".cke_wysiwyg_frame.cke_reset", 30);
    I.switchTo("iframe.cke_reset");

    I.waitForElement("iframe.wj_component", 10);
    I.switchTo("iframe.wj_component");

    I.forceClick("div.inlineComponentButtons > a:nth-child(1)");
    I.switchTo("");
    I.switchTo("");

    I.waitForElement("div.cke_dialog_body");

    Document.screenshotElement("div.cke_dialog_body", "/redactor/apps/restaurant-menu/menu-app-dialog.png");

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("iframe#editorComponent");
        I.click("a#tabLink2");
    I.switchTo("");
    I.switchTo("");

    DT.waitForLoader();

    Document.screenshotElement("div.cke_dialog_body", "/redactor/apps/restaurant-menu/menu-app-dialog-meals.png");

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("iframe#editorComponent");
        I.click("a#tabLink4");
    I.switchTo("");
    I.switchTo("");

    Document.screenshotElement("div.cke_dialog_body", "/redactor/apps/restaurant-menu/menu-app-dialog-menu.png");

    I.amOnPage("/apps/restaurant-menu/");
    I.waitForText("RESTAURANT MENU", 30);
    I.waitForElement("div.restaurant-menu", 30);
    I.fillField("#datepicker2_input", "48-2023");
    I.click("#wjInline-docdata > section > div > input.button50");

    I.waitForElement(locate("h2.menu").withText("Pondelok (27.11.2023)"));
    Document.screenshot("/redactor/apps/restaurant-menu/menu-app-frontend.png", 1000, 1100);
});

async function setDate(I, DT, value) {
    I.fillField("div.dt-extfilter-dayDate > form > div.input-group > input.datepicker.min", value);
    I.click("div.dt-extfilter-dayDate > form > div.input-group > button.filtrujem");
    DT.waitForLoader();
}