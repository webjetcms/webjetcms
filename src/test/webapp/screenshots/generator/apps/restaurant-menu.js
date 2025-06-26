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

    const confLng = I.getConfLng();
    const allergenField = 'editorFields\\.alergensArr';
    const dropdownSelector = 'div.dropdown-menu.show .dropdown-item';

    switch (confLng) {
        case 'sk':
            DTE.selectOption(allergenField, '1 - Obilniny');
            I.click(locate(dropdownSelector).withText("7 - Mlieko"));
            break;
        case 'en':
            DTE.selectOption(allergenField, '1 - Cereals');
            I.click(locate(dropdownSelector).withText("7 - Milk"));
            break;
        case 'cs':
            DTE.selectOption(allergenField, '1 - Obilniny');
            I.click(locate(dropdownSelector).withText("7 - Mléko"));
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/restaurant-menu/meals-allergens-list.png");
});

Scenario('menu screens', ({ I, DT, Document, i18n }) => {
    I.amOnPage("/apps/restaurant-menu/admin/");
    DT.setExtfilterDate(i18n.getDate('11/28/2023'));
    Document.screenshot("/redactor/apps/restaurant-menu/menu-data-table.png", 1400, 400);
    Document.screenshotElement("#pills-meals", "/redactor/apps/restaurant-menu/menu-external-filter.png");
    Document.screenshotElement("div.dt-extfilter-dayDate", "/redactor/apps/restaurant-menu/menu-external-filter-date.png");
    Document.screenshotElement("#menuType", "/redactor/apps/restaurant-menu/menu-external-filter-type.png");
    Document.screenshotElement("div.status-info", "/redactor/apps/restaurant-menu/menu-external-filter-status-a.png");

    I.clickCss("#menuType > button[data-menu-type=weeks]");

    Document.screenshot("/redactor/apps/restaurant-menu/menu-data-table-weeks.png", 1650, 1000);
    Document.screenshotElement("div.status-info", "/redactor/apps/restaurant-menu/menu-external-filter-status-b.png");
});

Scenario('menu app screens', async ({ I, Document }) => {
    I.amOnPage("/apps/restaurant-menu/?NO_WJTOOLBAR=true&week=2023-W48");
    Document.screenshot("/redactor/apps/restaurant-menu/menu-app-frontend.png", 1000, 1100);

    Document.screenshotAppEditor(74621, "/redactor/apps/restaurant-menu/menu-app-dialog.png", function(Document, I) {
        I.clickCss("#pills-dt-component-datatable-listMealsIframeWindowTab-tab");
        Document.screenshot("/redactor/apps/restaurant-menu/menu-app-dialog-meals.png");

        I.clickCss("#pills-dt-component-datatable-newMenuIframeWindowTab-tab");
        I.switchTo("#DTE_Field_iframe2");
        I.fillField("input.dt-filter-from-dayDate", "28.11.2023");
        I.clickCss("button.dt-filtrujem-dayDate");

        Document.screenshot("/redactor/apps/restaurant-menu/menu-app-dialog-menu.png");

        I.switchTo();
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });
});