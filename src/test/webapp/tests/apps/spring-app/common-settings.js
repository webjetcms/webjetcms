Feature('spring-app.common-settings');

Before(({ I, login }) => {
    login('admin');
});

Scenario('Hiding/showing CommonSetting tab in Spring based application', ({I, DTE, Document}) => {

    I.say("Check that CommonSettings tab is visible for Spring based application Demo komponenta");
    checkCommonSettingsTab(I, DTE, "Demo komponenta", "#demo-komponenta", true);

    I.say("Check that CommonSettings tab is MOT visible for Spring based application Kontakty");
    checkCommonSettingsTab(I, DTE, "Kontakty", "#kontakty", false);
});

function checkCommonSettingsTab(I, DTE, searchText, appId, shouldBeVisible) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=79733");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-content-tab");
    I.click("a.cke_button__components");

    //1. iframe
    I.waitForElement("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe", 5);
    I.switchTo("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe");

    //2. iframe
    I.switchTo("iframe#editorComponent");

    I.seeElement("input#search");
    I.fillField("input#search", searchText);
    I.seeElement(appId);
    I.clickCss(appId);

    I.clickCss("body > div > div.store > div.promo > div > div.content.app-info > div > div > div.app-buy > a");
    I.waitForElement("#component-datatable_modal", 10);

    if(shouldBeVisible) {
        I.seeElement("#pills-dt-component-datatable-commonSettings-tab");
        I.click("#pills-dt-component-datatable-commonSettings-tab");

    } else {
        I.dontSeeElement("#pills-dt-component-datatable-commonSettings-tab");
    }

    I.switchTo();
}

Scenario('Hiding/showing spring based app based on device', ({I}) => {
    I.say("Check PC version");
    I.amOnPage("/apps/podmienene-zobrazenie/?forceBrowserDetector=pc");
    I.waitForElement(locate("h1").withText("Podmienene zobrazenie"));
    I.seeElement( locate("span").withText("Verzia pre PC") );
    I.dontSeeElement( locate("span").withText("Verzia pre Tablet a Telefon") );

    I.say("Check phone/table version as TABLET");
    I.amOnPage("/apps/podmienene-zobrazenie/?forceBrowserDetector=tablet");
    I.waitForElement(locate("h1").withText("Podmienene zobrazenie"));
    I.dontSeeElement( locate("span").withText("Verzia pre PC") );
    I.seeElement( locate("span").withText("Verzia pre Tablet a Telefon") );

    I.say("Check phone/table version as PHONE");
    I.amOnPage("/apps/podmienene-zobrazenie/?forceBrowserDetector=tablet");
    I.waitForElement(locate("h1").withText("Podmienene zobrazenie"));
    I.dontSeeElement( locate("span").withText("Verzia pre PC") );
    I.seeElement( locate("span").withText("Verzia pre Tablet a Telefon") );
});

Scenario("Device type checkbox save", ({I, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=80009");
    DTE.waitForEditor();

    I.waitForElement(".cke_wysiwyg_frame.cke_reset", 10);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForElement("iframe.wj_component", 10);
    I.switchTo("iframe.wj_component");
    I.waitForText("Demo component view, params", 10);
    I.dontSee("Zobrazenie na zariadeniach");

    I.switchTo();
    Document.editorComponentOpen();

    I.waitForElement("#pills-dt-component-datatable-commonSettings-tab", 10);
    I.clickCss("#pills-dt-component-datatable-commonSettings-tab");
    I.waitForElement("#pills-dt-component-datatable-commonSettings", 10);
    I.clickCss("label[for=DTE_Field_device_1]")
    I.switchTo();
    I.clickCss("table.cke_dialog table.cke_dialog_contents td.cke_dialog_footer a.cke_dialog_ui_button_ok");
    I.wait(2);

    //
    I.say("Checking device type mode");
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForElement("iframe.wj_component", 10);
    I.switchTo("iframe.wj_component");
    I.waitForText("Zobrazenie na zariadeniach:", 10, ".deviceInfoTitle");
    I.waitForText("Tablet", 10, ".deviceInfoTypes");
    I.switchTo();

    //
    I.say("Unselecting device type");
    Document.editorComponentOpen();

    I.waitForElement("#pills-dt-component-datatable-commonSettings-tab", 10);
    I.clickCss("#pills-dt-component-datatable-commonSettings-tab");
    I.waitForElement("#pills-dt-component-datatable-commonSettings", 10);
    I.clickCss("label[for=DTE_Field_device_1]")
    I.switchTo();
    I.clickCss("table.cke_dialog table.cke_dialog_contents td.cke_dialog_footer a.cke_dialog_ui_button_ok");
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForElement("iframe.wj_component", 10);
    I.switchTo("iframe.wj_component");
    I.waitForText("Demo component view, params", 10);
    I.dontSee("Zobrazenie na zariadeniach");
    I.switchTo();

    DTE.cancel();
});

Scenario("Cache", async ({I}) => {
    I.amOnPage("/apps/spring-app/cache-test.html?NO_WJTOOLBAR=true");
    I.waitForElement("p.currentDate", 10);
    var date = await I.grabTextFrom("p.currentDate");

    //
    I.say("Date must be changed after refresh for admin - not cached");
    I.amOnPage("/apps/spring-app/cache-test.html?NO_WJTOOLBAR=true");
    I.waitForElement("p.currentDate", 10);
    I.dontSee(date, "p.currentDate");

    //
    I.logout();
    I.amOnPage("/apps/spring-app/cache-test.html");
    I.waitForElement("p.currentDate", 10);
    var date = await I.grabTextFrom("p.currentDate");
    I.wait(5);

    //
    I.say("Date must be same after refresh - cached");
    I.amOnPage("/apps/spring-app/cache-test.html");
    I.waitForElement("p.currentDate", 10);
    I.seeTextEquals(date, "p.currentDate");

    //
    I.say("Date must be changed after refresh with URL parameter - not cached");
    I.amOnPage("/apps/spring-app/cache-test.html?_disableCache=true");
    I.waitForElement("p.currentDate", 10);
    I.dontSee(date, "p.currentDate");
});