Feature('webpages.appstore');

var randomNumber, stringField, cookieTitle;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
        stringField = "stringField-autotest-"+randomNumber+", auto, \"test\", pokus\"";
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

        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        I.fillField("#DTE_Field_date", "15.2.2023 12:57:56");
        I.pressKey("Escape");
    }

    I.clickCss("#pills-dt-component-datatable-json-tab");
    I.clickCss("#editorAppDTE_Field_groupDetails button.btn-vue-jstree-item-edit");
    I.waitForElement("div.jsTree-wrapper");
    I.click("Test stavov");

    I.switchTo();
    I.clickCss("a.cke_dialog_ui_button_ok");
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

Scenario('spring - zoznam aplikacii', ({ I, DT, DTE, Browser }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=15257");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    I.clickCss("#pills-dt-datatableInit-content-tab");

    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.forceClick("#WebJETEditorBody p");
    I.switchTo();

    I.clickCss("a.cke_button__components");
    I.waitForElement("div.cke_editor_data_dialog");
    I.wait(6);

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent")

    I.see("Fotogaléria", "div.promoApp");
    I.see("Demo komponenta", "div.menu-app");

    //klikni na demo komponentu
    I.clickCss("#demo-komponenta");
    I.wait(3);

    I.see("Demo komponenta nejaky dlhy opis");

    I.click("Vložiť do stránky");
    I.wait(6);
    if (Browser.isFirefox()) I.wait(3);

    testDemoComponent(I, true);
});

Scenario('spring - nastavenie parametra', ({ I, DTE, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=27030");
    DTE.waitForEditor();
    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.click("iframe.wj_component");
    I.wait(6);

    testDemoComponent(I);

    //zrus okno a uloz stranku
    I.switchTo();
    I.clickCss("a.cke_dialog_ui_button_cancel");
    I.wait(2);

    //check params inside editor
    I.switchTo();
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.switchTo('iframe.wj_component');

    I.see("Demo component view, params:");
    I.see("stringField: " + stringField);

    I.switchTo();

    //check param in HTML view
    Apps.switchEditor('html');
    I.see('stringField=&quot;' + stringField.replace(/"/g, '\\\&quot;') + '&quot;,');
    Apps.switchEditor();

    DTE.save();

    I.amOnPage("/apps/spring-app/");
    I.see("stringField: "+stringField);
});

function testCookieComponent(I, DTE) {

    //prepni sa do okna
    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    //nastav hodnotu
    I.wait(2);
    DTE.fillField("cookie_title", cookieTitle);

    I.switchTo();
    I.clickCss("a.cke_dialog_ui_button_ok");
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
    DTE.seeInField("cookie_title", cookieTitle);

}

Scenario('jsp - zoznam aplikacii', ({ I, DTE, Browser }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=17321");
    DTE.waitForEditor();
    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.forceClick("#WebJETEditorBody p");
    I.switchTo();

    I.clickCss("a.cke_button__components");
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
    I.clickCss("#components-app-cookiebar-title");
    I.wait(3);

    I.see("panel súhlasu s cookies");

    I.click("Vložiť do stránky");
    I.wait(6);

    testCookieComponent(I, DTE);
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

    testCookieComponent(I, DTE);

    //zrus okno a uloz stranku
    I.switchTo();
    I.clickCss("a.cke_dialog_ui_button_cancel");
    I.wait(2);

    DTE.save();

    I.amOnPage("/apps/cookies-lista/");
    I.see(cookieTitle);

    //vrat do povodneho stavu

});

Scenario('reset text', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25210");
    DTE.waitForEditor();
    I.wait(3);

    Document.editorComponentOpen();
    I.fillField("#DTE_Field_cookie_title", "Používanie cookies");
    I.fillField("#DTE_Field_cookie_text", "");
    Document.editorComponentOk();

    //zrus okno a uloz stranku
    I.switchTo();
    DTE.save();
});

Scenario('Should not cache SK gallery images after logout and language switch to CZ for annotated applications', ({ I, DT, DTE }) => {
    openAppstore(I, DT, DTE);

    I.seeElementInDOM('.app-components-app-cookiebar-title img[src="/components/app-cookiebar/screenshot-1.jpg"]');
    I.seeElementInDOM('.app-components-app-cookiebar-title img[src="/components/app-cookiebar/screenshot-2.jpg"]');
    I.seeElementInDOM('.app-demo-komponenta img[src="/components/app-cookiebar/screenshot-2.jpg"]');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_cancel');
    DTE.cancel("datatableInit");
    I.relogin("admin", true, true, language = "cs");

    openAppstore(I, DT, DTE);

    I.seeElementInDOM('.app-components-app-cookiebar-title img[src="/components/app-cookiebar/screenshot-1-cs.jpg"]');
    I.seeElementInDOM('.app-components-app-cookiebar-title img[src="/components/app-cookiebar/screenshot-2-cs.jpg"]');
    I.seeElementInDOM('.app-demo-komponenta img[src="/components/app-cookiebar/screenshot-2-cs.jpg"]');
});

function openAppstore(I, DT, DTE){
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.waitForElement(".cke_wysiwyg_frame.cke_reset");
    I.wait(1);

    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.clickCss('.cke_button.cke_button__components.cke_button_off');
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');
    I.waitForElement('#search', 10);
}