Feature('manual-frontend');

let confLng = "sk";

Before(({ I }) => {
    confLng = I.getConfLng();
});

Scenario('templates-bare', async({ I, DT, DTE, Document }) => {

    I.say("TOTO TREBA SPUSTIT Debug DemoDB - s poolman-local-webjet9demo.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoDB - s poolman-local-webjet9demo.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoDB - s poolman-local-webjet9demo.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoDB - s poolman-local-webjet9demo.xml databazou!!!");
    I.say("You also need /src/main/webapp/templates/bare");
    pause();

    I.relogin("admin");

    //bare.tau27.iway.sk/Slovensky
    if("sk" === confLng) {
        I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=383");
    } else if("en" === confLng) {
        I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=439");
    } else if("cs" === confLng) {
        I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=383");
    }


    DT.waitForLoader();
    DTE.waitForEditor();
    DTE.cancel();

    //korenovy priecinok
    I.jstreeClick("Slovensky");
    I.click(".ti.ti-pencil", "div.tree-col");
    DTE.waitForEditor("groups-datatable");
    Document.screenshot("/frontend/examples/template-bare/group-editor.png");
    I.clickCss("#pills-dt-groups-datatable-template-tab");
    Document.screenshot("/frontend/examples/template-bare/group-editor-temp.png");
    DTE.cancel();

    //stranka hlavicka
    I.clickCss("#pills-system-tab");
    DT.waitForLoader();
    I.jstreeClick("Hlavičky");
    I.click("Default Hlavička");
    DTE.waitForEditor();
    I.wait(10);

    //Need to switch to page builder
    I.clickCss("#DTE_Field_data-editorTypeSelector > div > button.dropdown-toggle");
    I.waitForElement("div.dropdown-menu");
    I.click( locate("a.dropdown-item > span").withText("Page Builder") );
    I.wait(2);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');

    I.say("Presun kurzor nad logo a drz focus");
    I.say("Presun kurzor nad logo a drz focus");
    I.say("Presun kurzor nad logo a drz focus");
    I.say("Presun kurzor nad logo a drz focus");
    I.say("Presun kurzor nad logo a drz focus");
    pause();
    I.say("Presun kurzor nad logo a drz focus");

    I.wait(10);

    //toto nepomaha, treba manualne dat kurzor ponad obrazovku
    I.moveCursorTo("a.navbar-brand");

    Document.screenshot("/frontend/examples/template-bare/header-editor.png");

    I.switchTo();

    //screenshot skupiny sablon
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    DT.waitForLoader();
    DT.filterContains("name", "Bare");
    I.click("Bare");
    DTE.waitForEditor();

    Document.screenshot("/frontend/examples/template-bare/tempgroup-editor.png");
    I.clickCss("#pills-dt-datatableInit-metadata-tab");
    Document.screenshot("/frontend/examples/template-bare/tempgroup-editor-metadata.png");

    //screenshot sablona
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContains("tempName", "Bare");
    I.click("Bare - Hlavná šablóna");
    DTE.waitForEditor();

    Document.screenshot("/frontend/examples/template-bare/temp-editor.png");
    I.clickCss("#pills-dt-datatableInit-style-tab");
    Document.screenshot("/frontend/examples/template-bare/temp-editor-style.png");

    //screenshot web stranka
    if("sk" === confLng) {
        I.amOnPage("/sk/?NO_WJTOOLBAR=true");
    } else if("en" === confLng) {
        I.amOnPage("/en/bare-en.html?NO_WJTOOLBAR=true");
    } else if("cs" === confLng) {
        //TODO prepare CS version
        I.amOnPage("/sk/?NO_WJTOOLBAR=true");
    }
    Document.screenshot("/frontend/examples/template-bare/barepage.png", 1000, 1190);

});

Scenario('editor style combo', ({ I, DTE, Document }) => {
    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("POZOR: musí existovať ")
    pause();

    I.relogin("admin");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=63");
    DTE.waitForEditor();

    I.selectOption("#DTE_Field_data-editorTypeSelector select", "");
    I.wait(2);

    I.resizeWindow(1280, 700);
    I.wait(6);

    //sprav screenshot standardneho selectu
    I.waitForElement('#trEditor', 10);
    I.clickCss('#trEditor');
    I.click('p');

    I.click(".cke_combo__styles .cke_combo_button");
    I.wait(2);

    Document.screenshot("/frontend/examples/template-bare/editor-stylecombo.png");

    //I.resizeWindow(1280, 700);

    //sprav screenshot ked je kurzor v tabulke
    I.clickCss('#trEditor');
    I.click('.cke_button.cke_button__table.cke_button_.cke_button_off');
    I.waitForElement('.cke_button.cke_button__table.cke_button_.cke_button_on', 10);
    // volba velkosti tabulky
    for (let i = 0; i <= 3; i++) {
        I.pressKey('ArrowDown');
    }
    for (let j = 0; j <= 2; j++) {
        I.pressKey('ArrowRight');
    }
    I.pressKey('Enter');
    // vidim tabulku v editore
    I.click('p');
    I.pressKey('ArrowUp');
    I.waitForElement(locate('.cke_path_item').withText('table'), 10);
    I.wait(1);

    I.switchTo();
    I.click(".cke_combo__styles .cke_combo_button");
    Document.screenshot("/frontend/examples/template-bare/editor-stylecombo-table.png");

    I.wjSetDefaultWindowSize();

    Document.resetPageBuilderMode();
});

Scenario('template-creative', async({ I, DT, DTE, Document }) => {

    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("TOTO TREBA SPUSTIT Debug DemoCMS DB - s poolman-local-democms.xml databazou!!!");
    I.say("You also need /src/main/webapp/templates/creative");
    pause();
    //I.sleep(5);

    I.relogin("admin");

    //creative.webjetcms.sk/Slovensky
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=42");
    DT.waitForLoader();
    I.say("Kllikni aby bolo vidno PageBuilder ciary");
    I.say("Kllikni aby bolo vidno PageBuilder ciary");
    I.say("Kllikni aby bolo vidno PageBuilder ciary");
    pause();
    I.say("Kllikni aby bolo vidno PageBuilder ciary");
    I.wait(10);
    Document.screenshot("/frontend/examples/templates-creative/editor-webpage.png");
    DTE.cancel();

    //korenovy priecinok
    I.jstreeClick("Slovensky");
    I.click("button.buttons-edit", "div.tree-col");
    DTE.waitForEditor("groups-datatable");
    Document.screenshot("/frontend/examples/templates-creative/group-editor.png");
    I.clickCss("#pills-dt-groups-datatable-template-tab");
    Document.screenshot("/frontend/examples/templates-creative/group-editor-temp.png");
    DTE.cancel();

    //screenshot skupiny sablon
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    DT.filterContains("name", "Creative");
    I.click("Creative");
    DTE.waitForEditor();

    Document.screenshot("/frontend/examples/templates-creative/tempgroup-editor.png");

    //screenshot sablona
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContains("tempName", "Creative");
    I.click("Creative - Hlavná šablóna");
    DTE.waitForEditor();

    Document.screenshot("/frontend/examples/templates-creative/temp-editor.png");
    I.clickCss("#pills-dt-datatableInit-style-tab");
    Document.screenshot("/frontend/examples/templates-creative/temp-editor-style.png");

    //web stranka
    if("sk" === confLng) {
        I.amOnPage("/sk/?NO_WJTOOLBAR=true");
    } else if("en" === confLng) {
        I.amOnPage("/sk/?NO_WJTOOLBAR=true&language=en");
    } else if("cs" === confLng) {
        I.amOnPage("/sk/?NO_WJTOOLBAR=true");
    }

    I.pressKey("ArrowDown");

    Document.screenshot("/frontend/examples/templates-creative/creativepage.png", 1000, 700);
});

Scenario('reset DB', async({ I, DT, DTE, Document }) => {
    I.say("Spusti nazad standard DB");
    I.say("Spusti nazad standard DB");
    I.say("Spusti nazad standard DB");
    I.say("Spusti nazad standard DB");
    I.say("Spusti nazad standard DB");
    pause();
});

Scenario('picture element', async({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=149928");
    DTE.waitForEditor();
    I.wait(5);
    I.switchTo(".cke_wysiwyg_frame");
    I.clickCss("picture");
    I.wait(2);
    Document.screenshot("/frontend/setup/picture-element.png");
    I.switchTo();
});