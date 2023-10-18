Feature('webpages.appstore');

var randomNumber, stringField, cookieTitle;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
        stringField = "stringField-autotest-"+randomNumber;
        cookieTitle = "Používanie cookies autotest-"+randomNumber;
    }
});

function testDemoComponent(I, testAllFields=false) {
    //prepni sa do okna
    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    //nastav hodnotu
    I.fillField("#DTE_Field_stringField", stringField);

    if (testAllFields) {
        I.fillField("#DTE_Field_primitiveIntegerField", 20);

        I.click("#pills-dt-component-datatable-advanced-tab");
        I.fillField("#DTE_Field_date", "15.2.2023 12:57:56");
    }

    I.click("#pills-dt-component-datatable-json-tab");
    I.click("#editorAppDTE_Field_groupDetails button.btn-vue-jstree-item-edit");
    I.waitForElement("div.jsTree-wrapper");
    I.click("Test stavov");

    I.switchTo();
    I.click("a.cke_dialog_ui_button_ok");
    I.wait(6);

    //over zobrazenie v nahlade
    I.switchTo();
    I.wait(2);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.wait(2);
    I.waitForElement("iframe.wj_component", 10);
    I.switchTo("iframe.wj_component");
    I.wait(2);
    I.see("stringField: "+stringField);
    I.see("groupDetails: /Test stavov, ID =");

    if (testAllFields) {
        I.see("primitiveBooleanField: false");
        I.see("primitiveIntegerField: 20");
        I.see("date: 15.02.2023 12:57:56");
    }

    //znova skus editovat a over, ci sa hodnota preniesla
    I.switchTo();
    I.wait(2);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.wait(2);
    I.click("iframe.wj_component");
    I.wait(6);

    I.switchTo();
    I.wait(2);
    I.switchTo(".cke_dialog_ui_iframe");
    I.wait(2);
    I.switchTo("#editorComponent");
    I.wait(2);
    I.seeInField("#DTE_Field_stringField", stringField);
}

Scenario('spring - zoznam aplikacii', ({ I, DTE, Browser }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=17321");
    DTE.waitForEditor();
    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.forceClick("#WebJETEditorBody p");
    I.switchTo();

    I.click("a.cke_button__components");
    I.waitForElement("div.cke_editor_data_dialog");
    I.wait(6);

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent")

    I.see("Fotogaléria", "div.promoApp");
    I.see("Demo komponenta", "div.menu-app");

    //klikni na demo komponentu
    I.click("#demo-komponenta");
    I.wait(3);

    I.see("Demo komponenta nejaky dlhy opis");

    I.click("Vložiť do stránky");
    I.wait(6);
    if (Browser.isFirefox()) I.wait(3);

    testDemoComponent(I, true);
});

Scenario('spring - nastavenie parametra', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=27030");
    DTE.waitForEditor();
    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.click("iframe.wj_component");
    I.wait(6);

    testDemoComponent(I);

    //zrus okno a uloz stranku
    I.switchTo();
    I.click("a.cke_dialog_ui_button_cancel");
    I.wait(2);

    DTE.save();

    I.amOnPage("/apps/spring-app/");
    I.see("stringField: "+stringField);
});

function testCookieComponent(I) {

    //prepni sa do okna
    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    //nastav hodnotu
    I.wait(2);
    I.fillField("#cookie_title", cookieTitle);

    I.switchTo();
    I.click("a.cke_dialog_ui_button_ok");
    I.wait(6);

    //over zobrazenie v nahlade
    I.switchTo();
    I.wait(2);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.wait(2);
    I.switchTo("iframe.wj_component");
    I.wait(2);
    I.see(cookieTitle);

    //znova skus editovat a over, ci sa hodnota preniesla
    I.switchTo();
    I.wait(2);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.wait(2);
    I.click("iframe.wj_component");
    I.wait(10);

    I.switchTo();
    I.wait(4);
    I.switchTo(".cke_dialog_ui_iframe");
    I.wait(4);
    I.switchTo("#editorComponent");
    I.wait(4);
    I.seeInField("#cookie_title", cookieTitle);

}

Scenario('jsp - zoznam aplikacii', ({ I, DTE, Browser }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=17321");
    DTE.waitForEditor();
    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.forceClick("#WebJETEditorBody p");
    I.switchTo();

    I.click("a.cke_button__components");
    I.waitForElement("div.cke_editor_data_dialog");
    I.wait(6);

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.wait(1);
    I.switchTo("#editorComponent");
    I.wait(1);

    I.see("Fotogaléria", "div.promoApp");
    I.see("Demo komponenta", "div.menu-app");

    if (Browser.isFirefox()) I.wait(3);
    I.waitForElement("#search", 10);
    I.fillField("#search", "Cookie");
    I.wait(4);

    I.dontSeeElement('div.promoApp');

    //klikni na demo komponentu
    I.click("#components-app-cookiebar-title");
    I.wait(3);

    I.see("Vložte si do stránky cookiebar");

    I.click("Vložiť do stránky");
    I.wait(6);

    testCookieComponent(I);
});

Scenario('jsp - cookies lista nastavenie parametra', ({ I, DTE, Browser }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25210");
    DTE.waitForEditor();
    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.forceClick("#WebJETEditorBody p");
    if (Browser.isFirefox()) I.wait(3);
    I.click("iframe.wj_component");
    I.wait(6);
    if (Browser.isFirefox()) I.wait(3);

    testCookieComponent(I);

    //zrus okno a uloz stranku
    I.switchTo();
    I.click("a.cke_dialog_ui_button_cancel");
    I.wait(2);

    DTE.save();

    I.amOnPage("/apps/cookies-lista/");
    I.see(cookieTitle);

    //vrat do povodneho stavu

});
