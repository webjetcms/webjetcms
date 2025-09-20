Feature('webpages.fontawesome');

Before(({ login }) => {
    login('admin');
});

const defaultEditorFontAwesomeCustomIcons = 'fa-wand-magic-sparkles:Super Magic Wand\nfa-wheelchair-move:Wheelchair Move';
const defaultEditorFontAwesomeCssPath = '/templates/aceintegration/jet/assets/fontawesome/css/fontawesome.css\n/templates/aceintegration/jet/assets/fontawesome/css/solid.css';

function navigateToEditor(I, DTE) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=21');
    DTE.waitForLoader();
    I.waitForElement(".cke_wysiwyg_frame.cke_reset");
    I.waitForElement(".cke_button__image");
    I.wait(0.3);
}

function openFontAwesomeDialog(I) {
    I.clickCss('.cke_button.cke_button__fontawesome');
    I.waitForElement('.cke_editor_data_dialog > table', 10);
}

Scenario('Verify absence of FontAwesome button when no editorFontAwesomeCssPath is set', ({ I, DTE, Document }) => {
    Document.setConfigValue('editorFontAwesomeCssPath', '');
    navigateToEditor(I, DTE);
    I.dontSeeElement('.cke_button.cke_button__fontawesome');
});

Scenario('Validate FontAwesome dialog and screenshot comparison for taxi icon', async ({ I, DTE, Document }) => {
    Document.setConfigValue('editorFontAwesomeCssPath', defaultEditorFontAwesomeCssPath);
    Document.setConfigValue('editorFontAwesomeCustomIcons', '');
    navigateToEditor(I, DTE);
    openFontAwesomeDialog(I);
    I.fillField('#cke_146_textInput', 'Super Magic Wand');
    I.dontSeeElement('#fontawesome-data > a > .fa.fa-wand-magic-sparkles');
    I.fillField('#cke_146_textInput', 'taxi');
    await Document.compareScreenshotElement('#fontawesome-data > a > .fa.fa.taxi', 'fontawesome-taxi.png', null, null, 12);
});

Scenario('Verify FontAwesome dialog and icon presence when defaultEditorFontAwesomeCustomIcons is set', ({ I, DTE, Document }) => {
    Document.setConfigValue('editorFontAwesomeCustomIcons', defaultEditorFontAwesomeCustomIcons);
    navigateToEditor(I, DTE);
    openFontAwesomeDialog(I);
    I.fillField('#cke_146_textInput', 'Super Magic Wand');
    I.seeElement('#fontawesome-data > a > .fa.fa-wand-magic-sparkles');
});
