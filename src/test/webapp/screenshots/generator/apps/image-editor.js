Feature('apps.image-editor');

Before(({ login }) => {
    login('admin');
});

Scenario('image-editor', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/apps/gallery/?id=164");
    DTE.waitForEditor("galleryTable");
    I.click("#pills-dt-galleryTable-photoeditor-tab");
    I.waitForElement(".tie-btn-resize", 10);

    var buttons = ['tie-btn-resize', 'tie-btn-crop', 'tie-btn-rotate', 'tie-btn-draw', 'tie-btn-shape', 'tie-btn-icon', 'tie-btn-text', 'tie-btn-mask', 'tie-btn-filter'];
    buttons.forEach(button => {
        Document.screenshotElement(`li.${button}.tui-image-editor-item.normal`, `/redactor/image-editor/${button}.png`);
    });

    I.clickCss(".tie-btn-resize");
    Document.screenshot("/redactor/image-editor/editor-preview.png");
});