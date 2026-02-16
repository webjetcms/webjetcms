Feature('apps.htmlbox');

Before(({ login }) => {
    login('admin');
});

/* REMATERED old tests */

Scenario("Bloky - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/bloky/");
    I.waitForElement(locate("h1").withText(("Bloky")));
    I.see("zmením text");
});

Scenario('Blocks - Content should include gallery component not snippet', ({ I, DT, DTE }) => {
    navigateToWebPagesList(I);
    createNewPage(I, DT, DTE);
    openContentEditor(I);
    selectContent(I, "Text s aplikáciou", "Hlavná šablona: Šablóny");

    I.switchTo("#cke_1_contents > iframe");
    I.dontSee("!INCLUDE(/components/gallery/gallery.jsp");
    I.switchTo();
    I.clickCss(".btn.btn-preview");
    I.wait(2);
    I.switchToNextTab();
    I.dontSee("!INCLUDE(/components/gallery/gallery.jsp");
    I.closeCurrentTab();
});

Scenario('Blocks - Editing remote image should show a warning', ({ I, DT, DTE }) => {
    navigateToWebPagesList(I);
    createNewPage(I, DT, DTE);
    openContentEditor(I);
    selectContent(I, "content15.html ", "Predpripravený blok", "Content");

    I.say('Clicking on the inserted image');
    I.switchTo("#cke_1_contents > iframe");
    I.clickCss("#WebJETEditorBody div.image");

    I.say('Trying to edit the image');
    I.switchTo();
    I.waitForElement(".cke_button.cke_button__floatimageedit.cke_button_off", 10);
    I.clickCss(".cke_button.cke_button__floatimageedit.cke_button_off");

    I.say('Verifying that a warning appears about the remote image');
    I.switchToNextTab();
    I.seeElement(locate(".toast-title").withText("Chyba"));
    I.seeElement(locate(".toast-message").withText("Obrázok je na vzdialenom serveri, na ktorom nie je možné obrázok priamo upraviť. Pre jeho úpravu ho najskôr stiahnite a uložte na váš server."));
});

function navigateToWebPagesList(I) {
    I.say('Navigating to the web pages list in the admin panel');
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
}

function createNewPage(I, DT, DTE) {
    I.say('Clicking the add button to create a new page');
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.wait(2);
}

function openContentEditor(I) {
    I.say('Switching to the content tab');
    I.clickCss('#pills-dt-datatableInit-content-tab');

    I.say('Opening the HTML box editor');
    I.waitForElement('.cke_button.cke_button__htmlbox.cke_button_off');
    I.clickCss('.cke_button.cke_button__htmlbox.cke_button_off');
}

function selectContent(I, contentName, blockType, blockName) {
    I.say(`Selecting content: ${contentName}`);
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');

    I.dtEditorSelectOption("codeType", blockType);

    if (blockName) { I.dtEditorSelectOption("blockType", blockName); }

    I.switchTo('#previewIframe');

    I.clickCss(`.thumbImage[data-name="${contentName}"]`);

    I.say('Confirming content selection');
    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");
}

/* NEW tests */

Scenario('Test app visual and logic', ({ I, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=64361");

    Apps.insertApp("Predpripravené bloky", '#components-htmlbox-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    var codeTypeOptions = ["Predpripravený blok", "Web stránka", "Hlavná šablona: Šablóny"];
    I.say("Check code type options");
    checkAndSelect(I, "codeType", codeTypeOptions, "Predpripravený blok");

    var blockTypeOptions = ["Columns", "Contact", "Content", "Download", "Header"];
    I.say("Check block type options");
    checkAndSelect(I, "blockType", blockTypeOptions, "Content");

    I.say("Check and select generated blockType");
    I.switchTo('#previewIframe');
    I.forceClick(`.thumbImage[data-name="content01.html "]`);

    I.say("Save app and check inserted value");
    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");

    Apps.switchEditor('html');
    I.see('<section class="content1"');
    I.see('<h2 class="text-uppercase editContent"');
    I.see('url(/images/template/common/ublocks/content1.jpg);');
});

Scenario('Test app visual and logic 2', ({ I, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=64361");

    Apps.insertApp("Predpripravené bloky", '#components-htmlbox-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.dtEditorSelectOption("codeType", "Hlavná šablona: Šablóny");

    I.say("Chekc templates and choose one - as STATIC");
    I.switchTo('#previewIframe');
    I.forceClick(`.thumbImage[data-name="Normálna stránka"]`);

    I.say("Save app and check inserted value");
    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");

    Apps.switchEditor('html');
    I.see("<h1>Toto je nadpis stránky</h1>");
    I.see("<p>Lorem ipsum dolor sit amet, consectetuer");
});

Scenario('Test app visual and logic 3', ({ I, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=64361");

    Apps.insertApp("Predpripravené bloky", '#components-htmlbox-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.dtEditorSelectOption("codeType", "Hlavná šablona: Šablóny");

    I.say("Chekc templates and choose one - as DYNAMIC link");
    I.clickCss("#DTE_Field_docStyle_1");
    I.switchTo('#previewIframe');
    I.forceClick(`.thumbImage[data-name="Normálna stránka"]`);

    I.say("Save app and check inserted value");
    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");

    Apps.switchEditor('html');
    I.see("!INCLUDE(/components/htmlbox/showdoc.jsp, docid=317)!");
    Apps.switchEditor('standard');

    I.say("Reopen app and test that DOC type of block is set, and page is pre-set too");
    Apps.openAppEditor();

    I.seeElement( locate("#editorAppDTE_Field_docDetails input.form-control[value='/System/Šablóny/Normálna stránka']") );
    I.switchTo("#previewIframe");
    I.seeElement("#WebJETEditor3Body");
    I.see("Toto je nadpis stránky");

    I.switchTo();
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    I.clickCss("button.btn-vue-jstree-item-remove");
    I.switchTo("#previewIframe");
    I.dontSeeElement("#WebJETEditor3Body");
    I.dontSee("Toto je nadpis stránky");
});

function checkAndSelect(I, selector, seeOptions, selectOption) {
    I.click(".DTE_Field_Name_" + selector + " button.dropdown-toggle");
    I.waitForVisible("div.dropdown-menu.show");
    seeOptions.forEach(option => {
        I.seeElement( locate("div.dropdown-menu.show").find( locate("a.dropdown-item > span").withText(option) ) );
    });
    if (selectOption) { I.click( locate("div.dropdown-menu.show").find( locate("a.dropdown-item > span").withText(selectOption) ) ); }
    I.waitForInvisible("div.dropdown-menu.show");
}