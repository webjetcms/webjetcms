Feature('apps.image-editor');

const fs = require('node:fs');
const path = require('node:path');
var randomNumber;
var autoName;
var remasterFileName;
const imageFolder = '/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy90ZXN0LXN0YXZvdi90ZXN0aW1wb3J0cGl4YWJheQ_E_E';

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        autoName = 'cervene-auto-autotest-' + randomNumber;
        remasterFileName = 'autotest-remaster-' + randomNumber + '.jpg';
    }
    I.closeOtherTabs();
});

Scenario('Area of interest uses the original gallery image', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    await I.executeScript(function() {
        window.location.href="/admin/v9/apps/image-editor/?id=-1&dir=/images/gallery/test&name=koala.jpg&showOnlyEditor=true";
    });
    I.waitForVisible("#galleryTable_modal", 20);
    I.clickCss("#pills-dt-galleryTable-areaOfInterest-tab");
    I.waitForElement("webjet-image-area-selector[data-ready='true'] cropper-canvas", 20);

    const imageSource = await I.grabAttributeFrom("webjet-image-area-selector img", "src");
    I.assertContain(imageSource, "/images/gallery/test/o_koala.jpg");
});

Scenario('Pixabay - test custom filename and image source after adding', async ({ I, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=108022');
    I.closeOtherTabs();
    var testFileName = autoName + ".jpg";
    var searchTerm = 'letters, alphabet, animals, nature, abcd';
    var expectedSearchFileName = 'letters-alphabet-animals-nature-abcd';
    var resultSelector = locate('.pixabayBox .col-xs-3 a').first();

    DTE.waitForEditor();
    I.waitForElement(".cke_wysiwyg_frame.cke_reset");
    I.clickCss('.cke_button__image');
    I.click(locate("a.cke_dialog_tab").withText("Fotobanka"));
    I.switchTo('#wjImagePixabayIframeElement');
    I.waitForElement('#search', 10);
    waitForPixabaySearch(I);
    I.fillField('#search', "***");
    I.fillField('#search', searchTerm);
    I.click('button[type="submit"]');
    waitForPixabaySearch(I);
    I.waitForElement(resultSelector, 40);

    var imageSource = await I.grabAttributeFrom(resultSelector, 'href');
    const sourceWidth = Number(await I.grabAttributeFrom(resultSelector, 'data-width'));
    const sourceHeight = Number(await I.grabAttributeFrom(resultSelector, 'data-height'));
    I.assertTrue(sourceWidth > 0, 'The search result must provide its width');
    I.assertTrue(sourceHeight > 0, 'The search result must provide its height');
    I.fillField('#search', 'this value was not searched');
    I.click(resultSelector);
    DTE.waitForModal('imageModal');
    I.seeInField('#imageName', expectedSearchFileName);
    I.seeInField("#imageWidth", "1280");
    I.seeInField("#imageHeight", String(Math.ceil(1280 * sourceHeight / sourceWidth)));

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

    I.amOnPage(imageFolder);

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

});

Scenario('Image editor - remaster, test of functionality', async ({ I, DTE, Document }) => {

    I.amOnPage(imageFolder);
    I.switchTo();

    // Use a stable fixture independently of the live Pixabay search and its upload test.
    var testFileName = remasterFileName;
    const fixturePath = path.join(global.output_dir, testFileName);
    fs.copyFileSync(path.join(__dirname, 'gallery/puppy-2785074.jpg'), fixturePath);
    I.waitForVisible('.elfinder-navbar-wrapper span[title$="/testimportpixabay"]', 20);
    I.clickCss('.elfinder-navbar-wrapper span[title$="/testimportpixabay"]');
    I.waitForVisible('.elfinder-button:not(.ui-state-disabled) .elfinder-button-icon-upload', 20);
    I.clickCss('.elfinder-button-icon-upload');
    I.waitForElement('.elfinder-dialog input[type=file]:not([directory])', 10);
    I.attachFile('.elfinder-dialog input[type=file]:not([directory])', path.relative(global.codecept_dir, fixturePath));
    I.waitForElement(`.elfinder-cwd-filename[title="${testFileName}"]`, 60);
    I.waitForInvisible('.elfinder-notify-upload', 60);

    openImageEditor(I, testFileName);

    I.say("CROP the image")
    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.waitForVisible("#galleryTable_modal");

    waitForImageCanvas(I, 1280, 853);
    I.clickCss("li.tie-btn-crop.tui-image-editor-item");
    I.waitForVisible("div.tui-image-editor-menu-crop");

    I.clickCss("div.tui-image-editor-button.preset-square");
    I.clickCss("div.tui-image-editor-button.apply");
    I.waitForInvisible("div.tui-image-editor-menu-crop");
    waitForImageCanvas(I, 853, 853);

    await Document.compareScreenshotElement('div.tui-image-editor-canvas-container', "autotest-cropped-puppy.png", 1280, 904, 20);

    I.say("Save Change");
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.waitForVisible("#toast-container-upload div.toast-message .ti-circle-check", 120);
    I.switchToPreviousTab();
    I.closeOtherTabs();
    I.switchTo();

    I.say("Open and test it");
    openImageEditor(I, testFileName);

    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.waitForVisible("#galleryTable_modal");

    waitForImageCanvas(I, 853, 853);

    await Document.compareScreenshotElement('div.tui-image-editor-canvas-container', "autotest-cropped-puppy.png", 1280, 904, 20);

    I.say("Change are of interest");
    I.clickCss("#pills-dt-galleryTable-areaOfInterest-tab");
    I.waitForElement("webjet-image-area-selector[data-ready='true']", 20);
    for (const [input, coordinate, value] of [['w', 'width', 240], ['h', 'height', 180], ['x', 'left', 80], ['y', 'top', 550]]) {
        I.fillField('#' + input, String(value));
        I.waitForFunction(([key, expected]) => {
            const selector = document.querySelector('webjet-image-area-selector');
            return selector.coordinates[key] === expected && !selector._applyingCoordinates;
        }, [coordinate, value], 10);
    }

    I.say("Save Change");
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.closeCurrentTab();

    I.say("Open and test it");
    openImageEditor(I, testFileName);

    I.clickCss("#pills-dt-galleryTable-areaOfInterest-tab");
    I.waitForElement("webjet-image-area-selector[data-ready='true']", 20);

    I.seeInField("#w", "240");
    I.seeInField("#h", "180");
    I.seeInField("#x", "80");
    I.seeInField("#y", "550");

    I.say("Close editor and delete the image");
    I.closeCurrentTab();
    I.switchTo();
});

function waitForPixabaySearch(I) {
    // CodeceptJS 3.6 cannot waitForFunction on its iframe FrameLocator.
    I.switchTo();
    I.waitForFunction(() => document.querySelector('#wjImagePixabayIframeElement').contentWindow.jQuery?.active === 0, 40);
    I.switchTo('#wjImagePixabayIframeElement');
}

function waitForImageCanvas(I, width, height) {
    I.waitForFunction(([expectedWidth, expectedHeight]) => {
        const canvas = document.querySelector('#photoEditorContainer canvas.lower-canvas');
        return canvas?.width === expectedWidth && canvas?.height === expectedHeight;
    }, [width, height], 30);
}

function openImageEditor(I, imageName) {
    I.waitForElement(`.elfinder-cwd-filename[title="${imageName}"]`);
    I.rightClick(`.elfinder-cwd-filename[title="${imageName}"]`);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjeditswitch');

    I.switchToNextTab();
    I.waitForVisible("#galleryTable_modal");
}

Scenario('Image editor - cleanup uploaded images', async ({ I }) => {
    I.switchTo();
    I.closeOtherTabs();

    I.amOnPage(imageFolder);
    I.waitForVisible('.elfinder-cwd', 20);
    I.waitForInvisible('.elfinder-notify', 30);
    for (const testFileName of [autoName + '.jpg', remasterFileName]) {
        const file = `.elfinder-cwd-filename[title="${testFileName}"]`;
        if (await I.grabNumberOfVisibleElements(file) === 0) continue;
        I.rightClick(file);
        I.waitForVisible('.elfinder-contextmenu', 10);
        I.clickCss('.elfinder-contextmenu .elfinder-button-icon-rm');
        I.waitForVisible('.elfinder-dialog-confirm', 10);
        I.clickCss('.elfinder-dialog-confirm .elfinder-confirm-accept');
        I.waitForInvisible(file);
    }
    fs.rmSync(path.join(global.output_dir, remasterFileName), { force: true });
});
