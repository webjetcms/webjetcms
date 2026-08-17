Feature('apps.image-editor');

var randomNumber;
var autoName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        autoName = 'cervene-auto-autotest-' + randomNumber;
    }
    I.closeOtherTabs();
});

Scenario('Pixabay - test custom filename and image source after adding', async ({ I, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=108022');
    I.closeOtherTabs();
    var testFileName = autoName + ".jpg";
    var searchTerm = 'letters, alphabet, animals, nature, abcd';
    var expectedSearchFileName = 'letters-alphabet-animals-nature-abcd';
    var resultSelector = '.pixabayBox .col-xs-3 a:first-child';

    DTE.waitForEditor();
    I.waitForElement(".cke_wysiwyg_frame.cke_reset");
    I.clickCss('.cke_button__image');
    I.click(locate("a.cke_dialog_tab").withText("Fotobanka"));
    I.switchTo('#wjImagePixabayIframeElement');
    I.waitForElement('#search', 10);
    I.wait(1);
    I.fillField('#search', "***");
    I.fillField('#search', searchTerm);
    I.click('button[type="submit"]');
    I.waitForElement(resultSelector, 40);

    var imageSource = await I.grabAttributeFrom(resultSelector, 'href');
    I.fillField('#search', 'this value was not searched');
    I.click(resultSelector);
    DTE.waitForModal('imageModal');
    I.seeInField('#imageName', expectedSearchFileName);
    I.seeInField("#imageWidth", "1280");
    I.seeInField("#imageHeight", "904");

    var extension = await I.grabAttributeFrom('#imageModal .imageExtension', 'title');
    var extensionIcon = await I.grabAttributeFrom('#imageModal .imageExtension i', 'class');
    I.assertEqual(extension, 'JPG', 'The detected image extension is incorrect');
    I.assertContain(extensionIcon, 'ti-jpg', 'The image extension icon is incorrect');

    I.fillField('#imageName', '');
    I.clickCss('button.btn.btn-primary.saveImage');
    I.waitForVisible('#imageNameRequiredError', 10);
    I.see('Zadajte názov súboru', '#imageNameRequiredError');
    I.seeElement('#imageName.is-invalid');

    I.fillField('#imageName', 'Červené auto! autotest-' + randomNumber);
    I.pressKey('Tab');
    I.seeInField('#imageName', autoName);
    I.dontSeeElement('#imageNameRequiredError:visible');

    I.clickCss('button.btn.btn-primary.saveImage');
    DTE.waitForModalClose('imageModal');
    I.switchTo();
    I.switchTo('#wjImageIframeElement');
    I.waitForElement(`.elfinder-cwd-filename[title="${testFileName}"]`, 20);

    I.switchTo();
    I.click(locate("a.cke_dialog_tab").withText("Fotobanka"));
    I.switchTo('#wjImagePixabayIframeElement');
    I.waitForElement(resultSelector, 10);
    I.click(resultSelector);
    DTE.waitForModal('imageModal');
    I.fillField('#imageName', autoName);
    I.clickCss('button.btn.btn-primary.saveImage');
    I.waitForVisible('#imageNameDuplicateError', 10);
    I.seeElement('#imageName.is-invalid');
    I.seeElement('#imageModal.show');
    I.dontSeeElement('#imageModal .errors:visible');
    I.clickCss('#imageModal .modal-footer button[data-bs-dismiss="modal"]');
    DTE.waitForModalClose('imageModal');

    I.switchTo();

    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy90ZXN0LXN0YXZvdi90ZXN0aW1wb3J0cGl4YWJheQ_E_E');

    I.waitForElement(`.elfinder-cwd-filename[title="${testFileName}"]`, 20);
    I.rightClick(`.elfinder-cwd-filename[title="${testFileName}"]`);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjeditswitch');

    I.switchToNextTab();
    I.waitForVisible("#galleryTable_modal");
    I.clickCss('#pills-dt-galleryTable-metadata-tab');

    let imageName = await I.grabValueFrom('#DTE_Field_imageName');
    I.assertEqual(imageName, testFileName, 'The custom filename was not preserved');
    I.assertNotContain(imageName, '_1280_', 'The filename must not contain image dimensions');
    let imageSourceField = await I.grabValueFrom("#DTE_Field_imageSource");
    I.assertEqual(imageSource, imageSourceField, "Image source does not match expected URL");

    I.closeCurrentTab();
    I.switchTo();

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

    await Document.compareScreenshotElement('div.tui-image-editor-canvas-container', "autotest-croped_image.png", 1280, 904, 20);

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

    await Document.compareScreenshotElement('div.tui-image-editor-canvas-container', "autotest-croped_image.png", 1280, 904, 20);

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
