Feature('apps.htmlbox');

Before(({ login }) => {
    login('admin');
});

Scenario("Bloky - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/bloky/");
    I.waitForElement(locate("h1").withText(("Bloky")));
    I.see("zmením text");
});

Scenario('Blocks - Content should include gallery component not snippet', ({ I, DT, DTE }) => {
    navigateToWebPagesList(I);
    createNewPage(I, DT, DTE);
    openContentEditor(I);
    selectContent(I, "Text s aplikáciou");

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
    selectContent(I, "content15.html ", "Content");

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

function selectContent(I, contentName, folder) {
    I.say(`Selecting content: ${contentName}`);
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');

    if (folder) {
        I.say(`Navigating to folder: ${folder}`);
        I.clickCss("#tabLink2");
        I.selectOption("#DirNameSelect", folder);
        I.switchTo("#dirPreview");
    } else {
        I.switchTo('#previewWindow');
    }

    I.clickCss(`.thumbImage[data-name="${contentName}"]`);

    I.say('Confirming content selection');
    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");
}
