Feature('webpages.svgicon');

Before(({ I, login }) => {
     login('admin');
});

Scenario('SVG Icon Dialog @screenshot', async ({ I, DT, DTE, Document, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.dontSeeElement('.cke_button__webjetsvgicon');

    Document.setConfigValue("ckeditor_svgIcon_path", "/templates/aceintegration/jet/assets/images/sprite.svg");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.waitForElement('.cke_button__webjetsvgicon', 10);
    I.clickCss('.cke_button__webjetsvgicon');
    I.waitForElement('.wj-icon-grid', 10);
    I.waitForElement('.wj-icon-item[title=birth]', 10);
    I.seeElement('.wj-icon-item[title=bluetooth]');

    Document.screenshot("/frontend/setup/svgicon.png");

    I.type("birth");
    I.waitForInvisible('.wj-icon-item[title=bluetooth]', 10);
    I.seeElement('.wj-icon-item[title=birth]');
    I.seeElement('.wj-icon-item[title=birthday]');
    I.clickCss('.wj-icon-item[title=birth]');

    I.clickCss(".cke_dialog_ui_button.cke_dialog_ui_button_ok");

    Apps.switchEditor("html");
    const inputString = await I.grabTextFrom('.CodeMirror-code');

    I.assertContain(inputString, '<use xlink:href="/templates/aceintegration/jet/assets/images/sprite.svg#birth"', 'SVG icon was not inserted correctly');
});

Scenario("reset settings", ({ Document }) => {
    Document.setConfigValue("ckeditor_svgIcon_path", "");
});