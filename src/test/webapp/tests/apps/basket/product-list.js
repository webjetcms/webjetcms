Feature('apps.basket.product-list');

let subcategory = "section.md-subcategory-selector > ul > li > a";

Before(({ login }) => {
    login('admin');
});

Scenario('Eshop CHECK subcategory-selector AND product-list', async ({I, Document}) => {
    let item = "section.md-product-list article.item > div.item-header > h3";

    await changeDomain(I, Document, null);

    I.amOnPage("/produkty/");

    I.waitForElement("section.md-subcategory-selector", 10);
        I.seeElement( locate(subcategory).withText("Wearables") );
        I.seeElement( locate(subcategory).withText("Mobily") );

        I.say("Cant see level 2 subcategories");
        I.dontSeeElement( locate(subcategory).withText("Iphone") );
        I.dontSeeElement( locate(subcategory).withText("Android") );

    I.waitForElement("section.md-product-list", 10);
        I.seeElement( locate(item).withText("Smasung Galaxy S9 128GB") );
        I.seeElement( locate(item).withText("iPhone X 256GB") );
        I.seeElement( locate(item).withText("Apple Watch Series 4 40mm Vesmírne čierny hliník s čiernym špo...") );

        I.say("Cant see DOC of group (default_doc_id)");
        I.dontSeeElement( locate(item).withText("Mobily") );
        I.dontSeeElement( locate(item).withText("Wearables") );

    I.say("Check next subcategory - level 2");
        I.click( locate(subcategory).withText("Mobily") );

    I.waitForElement("section.md-subcategory-selector", 10);
        I.dontSeeElement( locate(subcategory).withText("Wearables") );
        I.dontSeeElement( locate(subcategory).withText("Mobily") );
        I.dontSeeElement( locate(subcategory).withText("Smasung Galaxy S9 128GB") );
        I.dontSeeElement( locate(subcategory).withText("iPhone X 256GB") );

        I.say("Cant see level 2 subcategories");
        I.seeElement( locate(subcategory).withText("Iphone") );
        I.seeElement( locate(subcategory).withText("Android") );

    I.waitForElement("section.md-product-list", 10);
        I.seeElement( locate(item).withText("Smasung Galaxy S9 128GB") );
        I.seeElement( locate(item).withText("iPhone X 256GB") );
        I.dontSeeElement( locate(item).withText("Apple Watch Series 4 40mm Vesmírne čierny hliník s čiernym špo...") );


    I.say("Check data level 3");
        I.click( locate(subcategory).withText("Android") );

    I.waitForElement("section.md-product-list", 10);
        I.seeElement( locate(item).withText("Smasung Galaxy S9 128GB") );
        I.seeElement( locate(item).withText("Smasung Galaxy S9 64GB") );

        I.dontSeeElement( locate(item).withText("iPhone X 256GB") );
        I.dontSeeElement( locate(item).withText("iPhone X 128GB") );
});

//TODO maybe later when we will have more items (with different perex/price)

Scenario('Check ext filter plus filter by column', async ({I, DT, Document}) => {

    await changeDomain(I, Document, "/apps/basket/admin/product-list/");

    I.seeElement(locate(".nav-link.active").withText("Zoznam produktov"));

    DT.filterEquals("title", "iPhone X 256GB");
    DT.checkTableRow("productListDataTable", 1, ["92895", "", "iPhone X 256GB", "Tester Playwright"]);
    DT.filterContainsForce("title", "Apple Watch Series 4 44mm");
    DT.checkTableRow("productListDataTable", 1, ["92897", "", "Apple Watch Series 4 44mm Vesmírne čierny hliník s čiernym športovým remi", "Tester Playwright"]);

    changeFolder(I, "/sk/Produkty/Mobily");
    DT.filterContainsForce("title", "Apple Watch Series 4 44mm");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    DT.filterEquals("title", "iPhone X 256GB");
    DT.checkTableRow("productListDataTable", 1, ["92895", "", "iPhone X 256GB", "Tester Playwright"]);
    DT.filterEquals("title", "Smasung Galaxy S9 128GB");
    DT.checkTableRow("productListDataTable", 1, ["92892", "", "Smasung Galaxy S9 128GB", "Tester Playwright"]);

    changeFolder(I, "/sk/Produkty/Mobily/Android");
    DT.filterEquals("title", "iPhone X 256GB");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filterEquals("title", "Smasung Galaxy S9 128GB");
    DT.checkTableRow("productListDataTable", 1, ["92892", "", "Smasung Galaxy S9 128GB", "Tester Playwright"]);
});

Scenario('Creating new folder (cathegory)', async ({I, DT, DTE, Document}) => {

    await changeDomain(I, Document, "/apps/basket/admin/product-list/");

    let random = I.getRandomText();

    I.say('Try add new category without name - wait for err');
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet");
    I.seeElement( locate("div.toast-title").withText("Pridanie kategórie produktov") );
    I.seeElement( locate("div.toastr-message").withText("Zadajte unikátny názov novej kategórie produktov, ktorá sa má vytvoriť v priečinku: /sk/Produkty") );

    I.say("Wait for err if no name is set");
    I.clickCss("div.toastr-buttons > button.btn-primary");
    checkToaster(I, false);

    I.say("And name and test create");
    let newGroupA = "TestGroupA_" + random + "_autotest";
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet");
    I.fillField("div.toastr-input > input", newGroupA);
    I.clickCss("div.toastr-buttons > button.btn-primary");

    I.say("Check succesfull toast message");
    checkToaster(I, true);

    I.say("That folder is in select now");
    I.click ( locate("#groupSelect_wrapper").find("button.dropdown-toggle") );
    I.waitForElement("div.dropdown-menu.show");
    I.seeElement( locate("div.dropdown-menu.show").find( locate("a.dropdown-item > span").withText("/sk/Produkty/" + newGroupA) ) );
    I.click ( locate("#groupSelect_wrapper").find("button.dropdown-toggle") );

    I.say("Try set same name in same folder - wait for err");
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet");
    I.fillField("div.toastr-input > input", newGroupA);
    I.clickCss("div.toastr-buttons > button.btn-primary");
    checkToaster(I, false);

    I.say("Create sub group");
    let newGroupB = "TestGroupA_child_" + random + "_autotest";
    changeFolder(I, "/sk/Produkty/" + newGroupA);

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet");
    I.fillField("div.toastr-input > input", newGroupB);
    I.clickCss("div.toastr-buttons > button.btn-primary");
    checkToaster(I, true);

    I.say("Check it in ESHOP");
    I.amOnPage("/produkty/");
    I.waitForElement("section.md-subcategory-selector", 10);
    I.seeElement( locate(subcategory).withText(newGroupA) );
    I.dontSeeElement( locate(subcategory).withText(newGroupB) );

    I.click( locate(subcategory).withText(newGroupA) );
    I.waitForElement("div.productsTags", 10);
    I.dontSeeElement( locate("div.productsTags").withText(newGroupA) );
    I.seeElement( locate("div.productsTags").withText(newGroupB) );

    I.say("Delete structure");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=72080");
    I.waitForElement(locate(".jstree-anchor").withText(newGroupA), 10);
    I.click(locate(".jstree-anchor").withText(newGroupA));
    DT.waitForLoader();

    I.click( locate("div.wp-header-tree").find("button.buttons-remove.noperms-deleteDir") );
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose("groups-datatable_modal");
    DT.waitForLoader();

    I.waitForInvisible(newGroupA, 10);
    I.waitForInvisible(newGroupB, 10);
});

Scenario('Logout to refresh selected domain', ({I}) => {
    I.logout();
});

async function changeDomain(I, Document, url) {
    I.say("changeDomain FN");

    if(url == null) {
        url = "/admin/v9/webpages/web-pages-list/";
    }

    I.amOnPage(url);

    const selectedDomain = (await I.grabTextFrom('button[data-id="header.currentDomain"] > div > div > div')).trim();

    if(selectedDomain != "shop.tau27.iway.sk") {
        I.say("Switching domain to shop.tau27.iway.sk")
        Document.switchDomain("shop.tau27.iway.sk");
    }
}

function changeFolder(I, folderName) {
    I.clickCss("#groupSelect_wrapper button.dropdown-toggle");
    I.waitForElement("#groupSelect_wrapper > div.dropdown.bootstrap-select > div.dropdown-menu", 5);
    I.click( locate("#groupSelect_wrapper a.dropdown-item > span").withText(folderName) );
}

function checkToaster(I, success) {
    let toastrClass = success ? "toast-success" : "toast-error";
    let msgStatus = success ? "úspešné" : "neúspešné";

    I.waitForElement("#toast-container-webjet > div." + toastrClass);
    I.seeElement( locate("div.toast-title").withText("Pridanie kategórie produktov") );
    I.seeElement( locate("div.toast-message").withText("Pridanie novej kategórie bolo " + msgStatus) );
    I.clickCss("button.toast-close-button");
    I.waitForInvisible("#toast-container-webjet");
}