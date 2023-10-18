Feature('webpages.pagebuilder');

Before(({ I, login }) => {
    login('admin');
});

Scenario('overenie zobrazenia podla sablony', ({I, DTE, Document}) => {

    //reset PB settings
    Document.resetPageBuilderMode();

    //stranka s PB
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(20);

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.seeElement("div.exit-inline-editor");
    I.switchTo();
    I.seeElement("#trEditor div.wysiwyg");
    I.dontSeeElement("#trEditor div.wysiwyg_textarea");

    I.switchTo('#DTE_Field_data-pageBuilderIframe');

    I.seeElement("div.cke_inner");
    I.see("Odstavec a zarovnanie");

    I.switchTo();

    //over moznost prepnutia editora
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.selectOption("#DTE_Field_data-editorTypeSelector select", "");
    I.wait(2);
    I.switchTo();
    I.dontSeeElement("#trEditor div.wysiwyg");
    I.seeElement("#trEditor div.wysiwyg_textarea");

    //otvor stranku kde nie je PB
    DTE.cancel();
    I.click("Produktová stránka - B verzia");
    DTE.waitForEditor();
    I.wait(5);

    I.dontSeeElement("div.exit-inline-editor");
    I.dontSeeElement("#trEditor div.wysiwyg");
    I.seeElement("#trEditor div.wysiwyg_textarea");

    //
    I.say("reload page and check if PB is still NOT active");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(5);
    I.dontSeeElement("#trEditor div.wysiwyg");
    I.seeElement("#trEditor div.wysiwyg_textarea");

    //reset PB settings
    Document.resetPageBuilderMode();
});

Scenario('bug - prepnutie editora', ({I, DTE, Document}) => {
    //bug: nacitam do editora stranku, prepnem na Standardny editor, prepnem do HTML kodu, ulozim
    //otvorim inu stranku, prepnem editor na Standardny a vidim stary text

    //reset PB settings
    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.selectOption("#DTE_Field_data-editorTypeSelector select", "");
    I.wait(2);
    I.switchTo();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.click('#trEditor', null, { position: { x: 177, y: 400 } });
    I.pressKey(['Ctrl', 'Q']);
    I.see("Suspendisse interdum dolor justo, ac venenatis massa");

    I.wait(1);
    I.switchTo();
    DTE.cancel();

    //otvor druhu stranku a over zobrazeny kod
    I.click("Produktová stránka - multi");
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.dontSee("This is old page");
    I.switchTo();

    I.click('#trEditor', null, { position: { x: 177, y: 400 } });
    I.pressKey(['Ctrl', 'Q']);
    I.dontSee("Suspendisse interdum dolor justo, ac venenatis massa");

    //reset PB settings
    Document.resetPageBuilderMode();
});

Scenario('bug - zobrazenie standardny po prepnuti a zatvoreni okna', ({I, DTE, Document}) => {
    //bug: ked prepnem z PB na standardny, zatvorim okno, otvorim, tak sa prepinac nezobrazi
    //overit aj to, ze sa nezobrazi na stranke, kde nie je PB zapnute

    //reset PB settings
    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(10);

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.selectOption("#DTE_Field_data-editorTypeSelector select", "");
    I.wait(2);
    I.switchTo();

    DTE.cancel();

    I.click("Produktová stránka - PageBuilder");
    DTE.waitForEditor();
    I.wait(10);
    I.seeElement("div.exit-inline-editor");

    DTE.cancel();

    //stranka bez pagebuildera
    I.click("Produktová stránka - B verzia");
    DTE.waitForEditor();
    I.wait(10);
    I.dontSeeElement("div.exit-inline-editor");

    DTE.cancel();

    //otvor znova PB a over, ze mame selector
    I.click("Produktová stránka - PageBuilder");
    DTE.waitForEditor();
    I.wait(10);
    I.seeElement("div.exit-inline-editor");

    //reset PB settings
    Document.resetPageBuilderMode();
});

Scenario('bug - nova stranka sablona podla priecinka', ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.waitForLoader();
    I.jstreeNavigate(["Test stavov", "Page Builder"]);
    I.click("Page Builder", "#datatableInit_wrapper");

    //tato stranka nema PB
    DTE.waitForEditor();
    I.wait(5);
    I.dontSeeElement("#DTE_Field_data-pageBuilderIframe");
    I.dontSeeElement("div.exit-inline-editor");
    DTE.cancel();

    //skusim novu stranku, ta musi mat PB
    I.click("button.buttons-create", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-content-tab");
    I.waitForVisible("#DTE_Field_data-pageBuilderIframe", 5);
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.seeElement("div.exit-inline-editor");

    I.switchTo();
    DTE.cancel();
});

Scenario('reset PB settings', ({Document}) => {
    //reset PB settings
    Document.resetPageBuilderMode();
});