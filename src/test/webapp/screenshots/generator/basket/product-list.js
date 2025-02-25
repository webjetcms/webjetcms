Feature('apps.product-list');

Before(({ login }) => {
    login('admin');
});

Scenario('Product list screens', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/eshop/admin/product-list/");

    Document.screenshot("/redactor/apps/eshop/product-list/datatable.png");

    Document.screenshotElement("#pills-dateRange-tab", "/redactor/apps/eshop/product-list/select.png");

    I.clickCss("#productListDataTable_extfilter > div > div > div > button.dropdown-toggle");
    I.waitForElement("body > div.dropdown.bootstrap-select > div.dropdown-menu", 5);

    Document.screenshotElement("body > div.dropdown.bootstrap-select > div.dropdown-menu", "/redactor/apps/eshop/product-list/select-options.png");

    I.amOnPage("/apps/eshop/admin/product-list/");
    changeFolder(I, "/shop.tau27.iway.sk/Produkty/Mobily");
    Document.screenshotElement("#pills-dateRange-tab", "/redactor/apps/eshop/product-list/select-phones.png");
    changeFolder(I, "/shop.tau27.iway.sk/Produkty/Mobily/Android");
    Document.screenshotElement("#pills-dateRange-tab", "/redactor/apps/eshop/product-list/select-phones-android.png");

    I.amOnPage("/apps/eshop/admin/product-list/");
    Document.screenshotElement("button.buttons-add-folder", "/redactor/apps/eshop/product-list/add-folder-button.png");
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet");
    I.moveCursorTo("#toast-container-webjet");
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/eshop/product-list/toaster-new-folder.png");
    Document.screenshotElement( locate("#toast-container-webjet").find("button.btn-primary"), "/redactor/apps/eshop/product-list/toaster-new-folder-button.png");

    I.amOnPage("/apps/eshop/admin/product-list/");
    doToasterScreen(I, Document, "", "A");
    doToasterScreen(I, Document, "Mobily", "B");
    doToasterScreen(I, Document, "Insert-autotest", "C");

    changeFolder(I, "/shop.tau27.iway.sk/Produkty/Mobily");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("productListDataTable");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/eshop/product-list/new-product.png");
    DTE.cancel();

    DT.filter("title", "iPhone X 256GB");
    I.click("iPhone X 256GB");
    DTE.waitForEditor("productListDataTable");
    I.clickCss("#pills-dt-productListDataTable-perex-tab");
    I.waitForElement("div.DTE_Field_Name_perexGroups");
    Document.screenshotElement("div.DTE_Field_Name_perexImage", "/redactor/apps/eshop/product-list/new-product-image.png");
    Document.screenshotElement("div.DTE_Field_Name_perexGroups", "/redactor/apps/eshop/product-list/new-product-perex.png");

    I.clickCss("#pills-dt-productListDataTable-attributes-tab");
    Document.screenshotElement("#productListDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/product-list/new-product-attr.png");
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
    I.clickCss("#productListDataTable_extfilter > div > div > div > button.dropdown-toggle");
    I.waitForElement("body > div.dropdown.bootstrap-select > div.dropdown-menu", 5);
    I.click( locate("a.dropdown-item > span").withText(folderName) );
}