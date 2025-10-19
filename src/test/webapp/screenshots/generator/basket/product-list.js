Feature('apps.product-list');

Before(({ login }) => {
    login('admin');
});

Scenario('Product list screens', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/basket/admin/product-list/");

    Document.switchDomain("shop.tau27.iway.sk");

    Document.screenshot("/redactor/apps/eshop/product-list/datatable.png");

    Document.screenshotElement(".dt-header-row", "/redactor/apps/eshop/product-list/select.png");

    I.clickCss("#groupSelect_wrapper button.dropdown-toggle");
    I.waitForElement("#groupSelect_wrapper > div.dropdown.bootstrap-select > div.dropdown-menu", 5);

    Document.screenshotElement("#groupSelect_wrapper > div.dropdown.bootstrap-select > div.dropdown-menu", "/redactor/apps/eshop/product-list/select-options.png");

    I.amOnPage("/apps/basket/admin/product-list/");
    changeFolder(I, "/sk/Produkty/Mobily");
    Document.screenshotElement("#groupSelect_wrapper", "/redactor/apps/eshop/product-list/select-phones.png");
    changeFolder(I, "/sk/Produkty/Mobily/Android");
    Document.screenshotElement("#groupSelect_wrapper", "/redactor/apps/eshop/product-list/select-phones-android.png");

    I.amOnPage("/apps/basket/admin/product-list/");
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet");
    I.moveCursorTo("#toast-container-webjet");
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/eshop/product-list/toaster-new-folder.png");

    I.amOnPage("/apps/basket/admin/product-list/");
    doToasterScreen(I, Document, "", "A");
    doToasterScreen(I, Document, "Mobily", "B");
    doToasterScreen(I, Document, "Insert-autotest", "C");

    changeFolder(I, "/sk/Produkty/Mobily");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("productListDataTable");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/eshop/product-list/new-product.png");
    DTE.cancel();

    DT.filterEquals("title", "iPhone X 256GB");
    I.click("iPhone X 256GB");
    DTE.waitForEditor("productListDataTable");
    I.clickCss("#pills-dt-productListDataTable-perex-tab");
    I.waitForElement("div.DTE_Field_Name_perexGroups");
    I.switchTo();
    Document.screenshot("/redactor/apps/eshop/product-list/new-product-image.png", null, null, "div.DTE_Field_Name_perexImage div.input-group");
    I.scrollTo("div.DTE_Field_Name_perexGroups");
    Document.screenshot("/redactor/apps/eshop/product-list/new-product-perex.png", null, null, "div.DTE_Field_Name_perexGroups div.DTE_Field_InputControl > div");

    I.clickCss("#pills-dt-productListDataTable-attributes-tab");
    DTE.selectOption("editorFields\\.attrGroup", "Monitor");
    Document.screenshotElement("#productListDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/product-list/new-product-attr.png");
});

Scenario("delete created folder", async ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=72080");
    I.jstreeClick("Insert-autotest");
    DT.waitForLoader();
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.waitForText("Insert-autotest", 10, "#groups-datatable_modal .DTE_Form_Info");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose("groups-datatable_modal");
    DT.waitForLoader();
    I.waitForInvisible("Insert-autotest", 10);
});

Scenario('Logout to refresh domain', ({I}) => {
    I.logout();
});

function doToasterScreen(I, Document, name, id) {
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet");
    I.fillField("div.toastr-input > input", name);
    I.clickCss("div.toastr-buttons > button.btn-primary");
    I.waitForElement("#toast-container-webjet");
    I.moveCursorTo("#toast-container-webjet > div.toast");
    I.wait(1);
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/eshop/product-list/toaster-new-folder-" + id + ".png");
    I.clickCss("button.toast-close-button");
    I.dontSeeElement("#toast-container-webjet");
    I.wait(1);
}

function changeFolder(I, folderName) {
    I.clickCss("#groupSelect_wrapper button.dropdown-toggle");
    I.waitForElement("#groupSelect_wrapper > div.dropdown.bootstrap-select > div.dropdown-menu", 5);
    I.click( locate("#groupSelect_wrapper a.dropdown-item > span").withText(folderName) );
}