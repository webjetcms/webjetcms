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

    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);
    I.waitForElement("div.exit-inline-editor", 10);
    I.seeElement("div.exit-inline-editor");
    I.waitForText("Page Builder", 10, "div.exit-inline-editor button .filter-option-inner-inner");
    I.seeElement("#trEditor div.wysiwyg");
    I.dontSeeElement("#trEditor div.wysiwyg_textarea");

    I.switchTo('#DTE_Field_data-pageBuilderIframe');

    I.seeElement("div.cke_inner");
    I.see("Odstavec a zarovnanie");
    I.waitForElement("#wjInline-docdata.pb-wrapper", 10);

    I.switchTo();

    //over moznost prepnutia editora
    I.selectOption("#DTE_Field_data-editorTypeSelector select", "");
    I.wait(2);
    I.dontSeeElement("#trEditor div.wysiwyg");
    I.seeElement("#trEditor div.wysiwyg_textarea");

    //otvor stranku kde nie je PB
    DTE.cancel();
    I.click("Produktová stránka - B verzia");
    DTE.waitForEditor();
    I.wait(5);

    I.waitForText("Štandardný", 10, "div.exit-inline-editor button .filter-option-inner-inner");
    I.dontSee("Page Builder", "div.exit-inline-editor button .filter-option-inner-inner");
    I.clickCss("div.exit-inline-editor button");
    I.dontSee("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);
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

    I.selectOption("#DTE_Field_data-editorTypeSelector select", "");
    I.wait(2);

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.clickCss('#trEditor', null, { position: { x: 177, y: 400 } });
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

    I.clickCss('#trEditor', null, { position: { x: 177, y: 400 } });
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

    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);
    I.waitForElement("#DTE_Field_data-editorTypeSelector select", 10);
    I.selectOption("#DTE_Field_data-editorTypeSelector select", "");
    I.wait(2);

    DTE.cancel();

    I.click("Produktová stránka - PageBuilder");
    DTE.waitForEditor();
    I.waitForElement("div.exit-inline-editor", 10);

    DTE.cancel();

    //stranka bez pagebuildera
    I.click("Produktová stránka - B verzia");
    DTE.waitForEditor();
    I.wait(2);
    I.clickCss("div.exit-inline-editor button");
    I.dontSee("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);

    DTE.cancel();

    //otvor znova PB a over, ze mame selector
    I.click("Produktová stránka - PageBuilder");
    DTE.waitForEditor();
    I.waitForElement("div.exit-inline-editor", 10);
    I.clickCss("div.exit-inline-editor button");
    I.see("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);

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
    I.waitForElement("div.exit-inline-editor", 10);
    I.clickCss("div.exit-inline-editor button");
    I.dontSee("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);
    DTE.cancel();

    //skusim novu stranku, ta musi mat PB
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForVisible("#DTE_Field_data-pageBuilderIframe", 5);
    I.waitForElement("div.exit-inline-editor", 10);
    I.waitForText("Page Builder", 10, "div.exit-inline-editor button .filter-option-inner-inner");
    I.clickCss("div.exit-inline-editor button");
    I.see("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);

    DTE.cancel();
});

Scenario('check toolbar elements', ({I, DTE, Document}) => {
    //reset PB settings
    //Document.resetPageBuilderMode();

    //stranka s PB
    I.resizeWindow(1280, 960);
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);

    I.waitForElement("div.exit-inline-editor", 10);
    I.seeElement("div.exit-inline-editor");
    I.seeElement("#trEditor div.wysiwyg");
    I.waitForInvisible("#trEditor > div.wysiwyg_textarea", 10);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');

    I.waitForElement("div.cke_inner", 10);
    I.seeElement("div.cke_inner");
    I.waitForText("Odstavec a zarovnanie", 10);
    I.see("Odstavec a zarovnanie");

    //
    I.waitForElement("#wjInline-docdata.pb-wrapper", 10);
    I.say("Click on col toolbar");
    I.seeElementInDOM("section:nth-child(1) aside.pb-toolbar");
    I.forceClick({css: "section:nth-child(1) .container .row .col-3:nth-child(1) aside.pb-toolbar"});
    I.seeElement("section:nth-child(1) .container .row .col-3:nth-child(1) aside.pb-highlighter__top");

    //
    I.say("Open style modal");
    I.forceClick({css: "aside.pb-is-toolbar-active span.pb-toolbar-button__style"});
    I.waitForElement("#wjInline-docdata.pb-is-modal-open div.pb-modal", 10);

    I.forceClick({css: "#wjInline-docdata.pb-is-modal-open div.pb-modal .pb-modal__footer .pb-modal__footer__button-close"});
    I.dontSeeElement("#wjInline-docdata div.pb-modal");

    //
    I.say("check styleCombo options");
    I.forceClick({css: "span.cke_combo__styles a.cke_combo_button"});
    I.waitForElement("iframe.cke_panel_frame", 5);
    I.switchTo("iframe.cke_panel_frame");
    I.see("Nadpis 1");
    I.see("baretest1");
    I.switchTo();
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.pressKey(['Escape']);
    I.dontSeeElement("iframe.cke_panel_frame");

    I.switchTo();
});

Scenario('reset PB settings', ({Document}) => {
    //reset PB settings
    Document.resetPageBuilderMode();
});