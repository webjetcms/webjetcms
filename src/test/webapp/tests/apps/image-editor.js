Feature('apps.image-editor');

var randomNumber;
var autoName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        autoName = 'autotest-' + randomNumber;
    }
    I.closeOtherTabs();
});

Scenario('Pixabay - test image source after adding', async ({ I, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=108022');
    var testFileName = autoName + ".jpg";

    DTE.waitForEditor();
    I.waitForElement(".cke_wysiwyg_frame.cke_reset");
    I.clickCss('.cke_button__image');
    I.clickCss('#cke_wjImagePixabay_131');
    I.switchTo('#wjImagePixabayIframeElement');
    I.waitForElement('#search', 10);
    I.wait(1);
    I.fillField('#search', 'abcd');
    I.wait(1);
    I.click('button[type="submit"]');
    I.waitForElement('.pixabayBox .col-xs-3 a:first-child', 10);
    I.click('.pixabayBox .col-xs-3 a:first-child');
    DTE.waitForModal('imageModal');
    I.clickCss('button.btn.btn-primary.saveImage');
    DTE.waitForModalClose('imageModal');
    I.switchTo();
    I.switchTo('#wjImageIframeElement')
    I.waitForElement('.elfinder-cwd-filename', 20);
    //There will allways be only one file when added
    I.waitForElement('.elfinder-cwd-file');
    I.switchTo();

    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy90ZXN0LXN0YXZvdi90ZXN0aW1wb3J0cGl4YWJheQ_E_E');

    I.waitForElement('.elfinder-cwd-filename');
    I.rightClick(locate('.elfinder-cwd-filename').last());
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjeditswitch');

    I.switchToNextTab();
    I.waitForVisible("#galleryTable_modal");
    I.clickCss('#pills-dt-galleryTable-metadata-tab');

    let imageName = await I.grabValueFrom('#DTE_Field_imageName');
    I.assertContain(imageName, "_1280_", "Picture has wrong dimensions");
    let imageSource = "https://pixabay.com/get/" + imageName.substring(0, imageName.indexOf("_1280_")+5) + ".jpg";

    DTE.seeInField('imageSource', imageSource);
    DTE.fillField('imageName', testFileName);
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.wait(4);
    I.closeCurrentTab();

    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy90ZXN0LXN0YXZvdi90ZXN0aW1wb3J0cGl4YWJheQ_E_E');
    I.waitForElement('.elfinder-cwd-filename');
    //We need to wait for new anme, because after change will be fired reload
    I.waitForElement(`.elfinder-cwd-filename[title="${testFileName}"]`, 15);

    I.say("Use the picture for NEXT test");
});

Scenario('Image editor - remaster, test of functionality', async ({ I, DTE, Document }) => {

    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy90ZXN0LXN0YXZvdi90ZXN0aW1wb3J0cGl4YWJheQ_E_E');
    I.switchTo();

    var testFileName = autoName + ".jpg";

    openImageEditor(I, testFileName);

    I.say("CROP the image")
    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.waitForVisible("#galleryTable_modal");

    I.waitForElement("#photoEditorContainer");
    I.clickCss("li.tie-btn-crop.tui-image-editor-item");
    I.waitForVisible("div.tui-image-editor-menu-crop");

    I.clickCss("div.tui-image-editor-button.preset-square");
    I.wait(1);
    I.clickCss("div.tui-image-editor-button.apply");
    I.waitForInvisible("div.tui-image-editor-menu-crop");

    await Document.compareScreenshotElement('div.tui-image-editor-canvas-container', "autotest-croped_image.png", null, null, 10);

    I.say("Save Change");
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.wait(2);
    I.waitForVisible("#toast-container-upload div.toast-message .ti-circle-check", 120);
    I.switchToPreviousTab();

    //
    I.say("Wait for editor window to close");
    I.wait(4);
    I.switchTo();

    I.say("Open and test it");
    openImageEditor(I, testFileName);

    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.waitForVisible("#galleryTable_modal");

    I.waitForElement("#photoEditorContainer");

    I.wait(2);

    await Document.compareScreenshotElement('div.tui-image-editor-canvas-container', "autotest-croped_image.png", null, null, 20);

    I.say("Change are of interest");
    I.clickCss("#pills-dt-galleryTable-areaOfInterest-tab");
    I.waitForVisible("div.ready");
    I.fillField("#w", "240");
    I.wait(2);
    I.fillField("#h", "180");
    I.wait(2);
    I.fillField("#x", "80");
    I.wait(2);
    I.fillField("#y", "550");
    I.wait(5);

    I.say("Save Change");
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.closeCurrentTab();

    I.say("Open and test it");
    openImageEditor(I, testFileName);

    I.clickCss("#pills-dt-galleryTable-areaOfInterest-tab");
    I.waitForVisible("div.ready");

    I.seeInField("#w", "240");
    I.seeInField("#h", "180");
    I.seeInField("#x", "80");
    I.seeInField("#y", "550");

    I.say("Close editor and delete the image");
    I.closeCurrentTab();
    I.switchTo();
});

function openImageEditor(I, imageName) {
    I.waitForElement(`.elfinder-cwd-filename[title="${imageName}"]`);
    I.rightClick(`.elfinder-cwd-filename[title="${imageName}"]`);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjeditswitch');

    I.switchToNextTab();
    I.waitForVisible("#galleryTable_modal");
}

Scenario('Image editor - remaster, test of functionality - cleanup', ({I,DT, DTE}) => {
    I.switchTo();
    I.closeOtherTabs();

    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy90ZXN0LXN0YXZvdi90ZXN0aW1wb3J0cGl4YWJheQ_E_E');
    var testFileName = autoName + ".jpg";

    I.rightClick(`.elfinder-cwd-filename[title="${testFileName}"]`);
    I.waitForVisible('.elfinder-contextmenu', 10);

    I.clickCss("span.elfinder-button-icon.elfinder-button-icon-rm");
    I.waitForVisible( locate("div.ui-dialog-titlebar > span.elfinder-dialog-title").withText("Vymazať") );
    I.click( locate("button.ui-button > span.ui-button-text").withText("Vymazať") );

    I.waitForInvisible(`.elfinder-cwd-filename[title="${testFileName}"]`);
});