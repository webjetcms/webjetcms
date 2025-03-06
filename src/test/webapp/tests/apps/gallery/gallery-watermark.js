Feature('apps.gallery.gallery-watermark');

Before(({ login }) => {
    login('admin');
});

Scenario('Watermark - Visual Test', async ({ I, DT, DTE, Document }) => {
    const watermarkFile = "/images/watermark.svg";
    Document.setConfigValue('galleryWatermarkSvgSizePercent', 40);

    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();

    I.jstreeNavigate(['watermark', 'subfolder3']);
    I.click(DT.btn.tree_edit_button);

    I.waitForElement('#pills-dt-galleryDimensionDatatable-watermark-tab', 10);
    I.clickCss('#pills-dt-galleryDimensionDatatable-watermark-tab');
    I.waitForVisible('#panel-body-dt-galleryDimensionDatatable-watermark', 15);

    I.fillField(
        locate('.DTE_Field.form-group.row.DTE_Field_Type_elfinder.DTE_Field_Name_watermark')
            .find('input.form-control'),
        watermarkFile
    );
    DTE.clickSwitch('editorFields-regenerateWatermark_0');
    DTE.save();

    //https://pixabay.com/sk/photos/kopce-hmla-ve%C4%8Dern%C3%A9-atmosf%C3%A9ra-9352436/
    I.amOnPage('/images/gallery/watermark/subfolder3/hills.jpg');
    await Document.compareScreenshotElement('img', 'watermark.png', null, null, 10);
});

Scenario('Revert config value', ({ Document }) => {
    Document.setConfigValue('galleryWatermarkSvgSizePercent', 5);
});