Feature('apps.spring-app');

Scenario('zakladne testy zobrazenia', async ({I}) => {
    I.amOnPage("/apps/spring-app/");

    I.dontSee("Ospravedlňujeme sa, ale nastala chyba pri vykonávaní komponenty");

    I.see("Demo component view, params:");
    I.see("test1: Toto je test");

    I.see("primitiveBooleanField: true");
    I.see("primitiveIntegerField: 10");
    I.see("primitiveDoubleField: 11.0");
    I.see("primitiveFloatField: 12.0");

    I.see("date: 20.05.2023 20:20:20");

    I.see("dirSimple: /images/bannery");

    I.see("groupDetails: /Test stavov, ID => 67");
    I.see("docDetails: /Jet portal 4/Zo sveta financií/Trhy sú naďalej vydesené, ID => 14");

    I.see("/System/Hlavičky-pätičky, ID => 4");
    I.see("/Aplikácie/Spring app, ID => 23425");

    I.see("/Jet portal 4/Úvodná stránka/Úvodná stránka, ID => 4");
    I.see("/English/Home/Home, ID => 31");
});

Scenario('volanie podla URL', async ({I}) => {
    I.amOnPage("/apps/spring-app/");
    await I.executeScript(function() {
        window.location.href="/apps/spring-app/?save";
    });

    I.dontSee("Ospravedlňujeme sa, ale nastala chyba pri vykonávaní komponenty");

    I.see("Demo component view, params:");
    I.see("test1: This is save method");
});

/**
 * Test pageParams binding in demoComponent
 */
Scenario('demo component page params', ({I, DTE}) => {

    I.relogin('admin');

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=27030");
    DTE.waitForEditor();
    I.wait(6);

    //prepni sa do okna
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

    I.say("Testing JSON fields");
    I.clickCss("#pills-dt-component-datatable-json-tab");
    I.waitForElement("#editorAppDTE_Field_groupDetails input.form-control", 10);
    I.seeInField("#editorAppDTE_Field_groupDetails input.form-control", "/Test stavov");
    I.seeInField("#editorAppDTE_Field_docDetails input.form-control", "/Jet portal 4/Zo sveta financií/Trhy sú naďalej vydesené");
    I.seeInField("#editorAppDTE_Field_dirSimple input.form-control", "/images/bannery");

    I.switchTo();
});

Scenario('mvc aplikacia', async ({I}) => {
    I.amOnPage("/apps/spring-app/kontakty/");
    I.switchTo();

    I.dontSee("Ospravedlňujeme sa, ale nastala chyba pri vykonávaní komponenty");

    I.see("InterWay, a. s.");
    I.see("SK2020268294");

    I.click("Nový kontakt");
    I.waitForElement(locate('button').withAttr({"name" : "saveForm"}));
    I.click(locate('button').withAttr({"name" : "saveForm"}));
    I.see("PSČ - veľkosť musí byť medzi 5 a 8");
    I.see("Názov - nemôže byť prázdne");

    I.amOnPage("/apps/spring-app/kontakty/");
    I.waitForElement("div.container table.table tr:nth-child(2)", 10);
    I.click("Upraviť", "div.container table.table tr:nth-child(2)");
    I.seeInField("name", "InterWay, a. s.");
});

function checkSpringAppTexts(I, lng) {
    if ("sk"===lng) {
        I.waitForText("Názov", 10, "table.table-contact-app tr th");
        I.see("DIČ", "table.table-contact-app tr th")
        I.see("Mesto", "table.table-contact-app tr th")
        I.see("Nový kontakt", "a.btn.btn-primary");
    } else if ("en"===lng) {
        I.waitForText("Name", 10, "table.table-contact-app tr th");
        I.see("VAT ID", "table.table-contact-app tr th")
        I.see("City", "table.table-contact-app tr th")
        I.see("New contact", "a.btn.btn-primary");
    }
}

/**
 * BUG: WebJET did not correctly set the language for the embedded Spring application when redirecting the form
 */
Scenario("Form with spring app forward", ({I}) => {
    I.logout();

    //SK
    I.say("Testing SLOVAK version");
    I.amOnPage("/apps/spring-app/kontakty/form-presmerovanim-spring-app.html");
    I.click("Odoslať");
    checkSpringAppTexts(I, "sk");

    //spam bot wait
    I.wait(30);

    //EN
    I.say("Testing ENGLISH version");
    I.amOnPage("/en/apps/spring-app/contact/form-redirection-spring-app.html");
    I.click("Submit");
    checkSpringAppTexts(I, "en");
});

function checkSpringAppTextsInEditor(docId, lng, I, DTE) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
    DTE.waitForEditor();
    I.wait(6);

    //prepni sa do okna
    I.switchTo();
    I.wait(2);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.wait(2);
    I.switchTo("iframe.wj_component");

    checkSpringAppTexts(I, lng);

    I.switchTo();
    DTE.cancel();
}

/**
 * Verify spring app preview in webpages editor to have correct language
 */
Scenario("Spring app language in editor", ({I, DTE}) => {
    I.relogin('admin');

    checkSpringAppTextsInEditor(27032, "sk", I, DTE);
    checkSpringAppTextsInEditor(105607, "en", I, DTE);
});