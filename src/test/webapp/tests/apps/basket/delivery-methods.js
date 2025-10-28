Feature('apps.basket.deliver-methods');

const SL = require("./SL");

Before(({ login }) => {
    login('admin');
});

Scenario('Delivery methods datatable tests', ({I, DT, DTE}) => {
    I.amOnPage("/apps/basket/admin/delivery-methods/");

    selectDeliveryMethod(I, DT, SL.DeliveryMethods.inStoreDelivery);
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("deliveryMethodsDataTable");

    //Check that country is pre-set as it should be
    I.seeElement( locate("div.DTE_Field_Name_deliveryMethodName").find( locate("div.filter-option-inner-inner").withText(SL.DeliveryMethods.inStoreDelivery) ) );

    I.say("Test err - no country");
        DTE.save();
        I.waitForText("Musíte zadať aspoň jednu podporovanú krajinu.", 5);

    I.say("Test err - redundant country for delivery");
        DTE.selectOption("supportedCountries", SL.Countries.sk);
        I.pressKey('Escape');
        DTE.selectOption("supportedCountries", "Česká republika");
        I.pressKey('Escape');
        DTE.selectOption("supportedCountries", "Poľsko");

        DTE.save();

        I.waitForText("Tento spôsob doručenia už pokrýva krajiny Česká republika, Poľsko", 5);

    I.say("Do valid save");
        DTE.selectOption("supportedCountries", "Česká republika");
        I.pressKey('Escape');
        DTE.selectOption("supportedCountries", "Poľsko");
        DTE.save();

    I.say("Check created entity");
        DT.filterSelect("deliveryMethodName", SL.DeliveryMethods.inStoreDelivery);
        DT.filterSelect("supportedCountries", SL.Countries.sk);
        I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check vaklues and do edit");
        I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric").first());
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor("deliveryMethodsDataTable");

        I.seeElement( locate("div.DTE_Field_Name_deliveryMethodName").find( locate("div.filter-option-inner-inner").withText(SL.DeliveryMethods.inStoreDelivery) ) );
        I.seeInField("#DTE_Field_price", "0");
        I.seeInField("#DTE_Field_vat", "0");
        I.seeInField("#DTE_Field_priceVat", "0");

        I.fillField("#DTE_Field_price", 15);
        I.fillField("#DTE_Field_vat", 23);

        DTE.save();

    I.say('Check values');
        DT.checkTableRow("deliveryMethodsDataTable", 1, ["", SL.DeliveryMethods.inStoreDelivery, SL.Countries.sk, "15,00", "23,00", "18,45"]);

    I.say("Delete record");
        I.clickCss("button.buttons-remove");
        I.waitForElement(".DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Post tets check', async ({I, DT}) => {
    I.amOnPage("/apps/basket/admin/delivery-methods/");
    DT.filterSelect("deliveryMethodName", SL.DeliveryMethods.inStoreDelivery);
    DT.filterSelect("supportedCountries", SL.Countries.sk);

    const isThereRecord = await I.grabNumberOfVisibleElements( locate("td.dt-select-td.cell-not-editable.dt-type-numeric") );
    if(isThereRecord > 0) {
        I.say("Record found - try delete it");
            I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric").first());
            I.clickCss("button.buttons-remove");
            I.waitForElement(".DTE_Action_Remove");
            I.click("Zmazať", "div.DTE_Action_Remove");
            I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
});

function selectDeliveryMethod(I, DT, name) {
    I.click( locate("#select_wrapper").find("button.dropdown-toggle.dropdown") );
    I.waitForVisible("div.dropdown-menu.show");
    I.click( locate("a.dropdown-item > span").withText(name) );
    DT.waitForLoader();
}