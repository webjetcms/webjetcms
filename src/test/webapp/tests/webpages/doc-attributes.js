Feature('webpages.doc-attributes');

var table = "table.atrTable";

Before(({ I, login }) => {
    login('admin');
});

Scenario('docatr-zakladne testy @baseTest', async ({I, DataTables}) => {
    I.amOnPage("/admin/v9/webpages/attributes/");
    await DataTables.baseTest({
        dataTable: 'attributesDataTable',
        perms: 'cmp_attributes',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {

        },
    });
});

Scenario('atributy v stranke', async ({I, DT, DTE}) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Aplikácie", "Atribúty stránky", "Monitory"]);

    I.click("Dell P2772");
    DTE.waitForEditor();
    I.seeElement("#pills-dt-datatableInit-attributes-tab");
    I.clickCss("#pills-dt-datatableInit-attributes-tab");

    var container = "#panel-body-dt-datatableInit-attributes";
    within(container, () => {
        I.see("Uhlopriečka")
        I.see("Power Delivery (W)");
        I.dontSee("Popis");
    });

    I.seeInField("#atr_1", "27");
    I.dontSeeCheckboxIsChecked('#atr_2_false');
    I.seeCheckboxIsChecked('#atr_2_true');
    I.seeInField("#atr_3", "60");
    I.seeInField("#atr_4", "430.65");

    DTE.save();

    I.click("Apple 5k");
    DTE.waitForEditor();
    I.seeElement("#pills-dt-datatableInit-attributes-tab");
    I.clickCss("#pills-dt-datatableInit-attributes-tab");

    I.seeInField("#atr_1", "27");
    I.seeCheckboxIsChecked('#atr_2_false');
    I.dontSeeCheckboxIsChecked('#atr_2_true');
    I.seeInField("#atr_3", "120");
    I.seeInField("#atr_4", "2240.33");

    DTE.save();

    //
    I.say("Opening multiedit and saving");
    I.forceClick(locate("table.datatableInit tr").withText("Dell P2772").find("td.dt-select-td"));
    I.forceClick(locate("table.datatableInit tr").withText("Apple 5k").find("td.dt-select-td"));
    I.click(DT.btn.edit_button)
    DTE.waitForEditor();
    I.dontSeeElement("#pills-dt-datatableInit-attributes-tab");

    DTE.save();

});

function checkDell(table, I) {
    within(table, () => {
        I.see("Uhlopriečka");
        I.see("Áno");
        I.see("60");
        I.see("430,65");

        I.dontSee("120");
        I.dontSee("Nie");
        I.dontSee("2240,33");
    });
}

function checkApple(table, I) {
    within(table, () => {
        I.see("Uhlopriečka");
        I.see("120");
        I.see("Nie");
        I.see("2240,33");

        I.dontSee("Áno");
        I.dontSee("60");
        I.dontSee("430,65");
    });
}

function checkAppleDell(table, I) {
    within(table, () => {
        I.see("Apple 5k");
        I.see("Nie");
        I.see("120");
        I.see("2240,33");

        I.see("Dell P2772");
        I.see("Áno");
        I.see("60");
        I.see("430,65");
    });
}

Scenario('zobrazenie atributov na web stranke', ({I}) => {

    I.amOnPage("/apps/atributy-stranky/monitory/");
    checkAppleDell(table, I);

    I.click("Dell P2772");
    I.wait(1);
    I.waitForElement(table, 5);
    checkDell(table, I);

    I.amOnPage("/apps/atributy-stranky/monitory/apple-5k.html");
    I.waitForElement(table, 5);
    checkApple(table, I);
});

Scenario('filtrovanie tabulky podla URL parametra', async ({I}) => {
    I.amOnPage("/apps/atributy-stranky/monitory/");
    I.logout();
    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_GT_Power+Delivery+(W)=90");
    I.waitForElement(table, 20);
    checkApple(table, I);

    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_LT_Power+Delivery+(W)=90");
    I.waitForElement(table, 20);
    checkDell(table, I);

    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_GTLT_Power+Delivery+(W)=50:200");
    I.waitForElement(table, 20);
    checkAppleDell(table, I);

    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_GTLT_Power+Delivery+(W)=50:80");
    I.waitForElement(table, 20);
    checkDell(table, I);

    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_GTLT_Power+Delivery+(W)=50:59");
    I.waitForElement(table, 20);
    I.see("Neboli nájdené žiadne stránky vyhovujúce zadaným kritériám");

    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_SS_vyrobca=pple");
    I.waitForElement(table, 20);
    checkApple(table, I);

    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_EQ_vyrobca=Apple");
    I.waitForElement(table, 20);
    checkApple(table, I);

    I.amOnPage("/apps/atributy-stranky/monitory/?atrs_EQ_vyrobca=pple");
    I.waitForElement(table, 20);
    I.see("Neboli nájdené žiadne stránky vyhovujúce zadaným kritériám");
});

Scenario('overenie zoznamu v inej domene', ({I, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    Document.switchDomain("rozbalenie.tau27.iway.sk");
    I.click("Test rozbalenia podadresarov", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.wait(3);
    I.dontSeeElement("#pills-dt-datatableInit-attributes-tab");
    DTE.cancel();
});

Scenario('logout', ({I}) => {
    I.logout();
});

function setAttr(title, I, Browser) {
    I.fillField({css: "div[data-atr-group=Monitor] input.ui-autocomplete-input"}, title);
    if (Browser.isFirefox()) I.fillField({css: "div[data-atr-group=Monitor] input.ui-autocomplete-input"}, title);
}

async function verifyAttrInWebpage(value, webpageUrl, I, see=true) {
    I.amOnPage(webpageUrl+"?NO_WJTOOLBAR=true");
    I.say("Verify attr "+value+" in webpage "+webpageUrl+" see="+see);

    var container = "article.ly-content div.container div.attributes";
    var containerCompare = "article.ly-content div.container div.comparetable table.atrTable tr td";

    if (see) {

        I.see(value, container);
        var count = await I.grabNumberOfVisibleElements(locate(container).withText(value));
        I.assertEqual(count, 1);


        I.see(value, containerCompare);
        count = await I.grabNumberOfVisibleElements(locate(containerCompare).withText(value));
        I.assertEqual(count, 2);
    } else {
        I.dontSee(value, container);
        I.dontSee(value, containerCompare);
    }
}

Scenario('attrs v stranke-multigroup', async ({I, DataTables, DT, DTE, Browser}) => {
    var randomText = I.getRandomText();
    var attrMaster = "autotest-master-" + randomText;
    var attrSlave = "autotest-slave-" + randomText;
    var container = "#pills-dt-datatableInit-attributes";

    //
    I.say("Changing attrs in master page");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=70950");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-attributes-tab");
    I.see("Výrobca", container);
    I.see("Monitor", container+" div.DTE_Field_Name_editorFields\\.attrGroup div.filter-option-inner-inner");

    setAttr(attrMaster, I, Browser);
    DTE.save();

    await verifyAttrInWebpage(attrMaster, "/test-stavov/multigroup/master/multi-attrs.html", I);
    await verifyAttrInWebpage(attrMaster, "/test-stavov/multigroup/slave/multi-attrs.html", I);

    //
    I.say("Changing attrs in slave page");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=70951");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-attributes-tab");
    I.see("Výrobca", container);
    I.see("Monitor", container+" div.DTE_Field_Name_editorFields\\.attrGroup div.filter-option-inner-inner");

    setAttr(attrSlave, I, Browser);
    DTE.save();

    await verifyAttrInWebpage(attrSlave, "/test-stavov/multigroup/master/multi-attrs.html", I);
    await verifyAttrInWebpage(attrSlave, "/test-stavov/multigroup/slave/multi-attrs.html", I);

    await verifyAttrInWebpage(attrMaster, "/test-stavov/multigroup/master/multi-attrs.html", I, false);
    await verifyAttrInWebpage(attrMaster, "/test-stavov/multigroup/slave/multi-attrs.html", I, false);
});