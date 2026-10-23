Feature('webpages.avif');

const codecDirectory = process.env.AVIF_TEST_IMAGEMAGICK_DIR || '/usr/bin';
const filesPage = '/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9wcm9kdWt0b3ZhLXN0cmFua2E_E';
let folderName;
let originalConfig;
let pageId;
let pageName;
let originalEditorMode;
let galleryFolder;
const galleryFilesPage = '/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9nYWxsZXJ5L3Rlc3Q_E';
const fileName = 'autotest-transparent.avif';
const imageUrl = () => '/images/produktova-stranka/' + folderName + '/' + fileName;

Before(({ I, login }) => {
    I.switchTo();
    login('admin');
    I.closeOtherTabs();
    if (!folderName) folderName = 'autotest-avif-' + I.getRandomTextShort();
});

/** Read the actual response, decode its pixels in the browser and verify its container. */
async function inspectImage(I, url) {
    return I.executeScript(async (source) => {
        const response = await fetch(source, { cache: 'reload' });
        const data = await response.arrayBuffer();
        const bytes = new Uint8Array(data);
        const result = { status: response.status, mime: response.headers.get('content-type'),
            cacheControl: response.headers.get('cache-control'),
            signature: String.fromCharCode(...bytes.slice(4, 12)), bytes: Array.from(bytes).join(',') };
        if (!response.ok) return result;
        const image = new Image();
        const objectUrl = URL.createObjectURL(new Blob([data], { type: result.mime }));
        try {
            image.src = objectUrl;
            await image.decode();
            const canvas = document.createElement('canvas');
            canvas.width = image.naturalWidth;
            canvas.height = image.naturalHeight;
            const context = canvas.getContext('2d');
            context.drawImage(image, 0, 0);
            return { ...result, width: canvas.width, height: canvas.height,
                alpha: context.getImageData(0, 0, 1, 1).data[3] };
        } finally {
            URL.revokeObjectURL(objectUrl);
        }
    }, url);
}

function assertImage(I, image, width, height, transparent = true) {
    I.assertEqual(image.status, 200);
    I.assertContain(image.mime, 'image/avif');
    I.assertEqual(image.signature, 'ftypavif');
    I.assertEqual(image.width, width);
    I.assertEqual(image.height, height);
    if (transparent) I.assertTrue(image.alpha < 16, 'The transparent corner must survive AVIF processing');
}

async function upload(I, name) {
    I.clickCss('.elfinder-button-icon-upload');
    I.waitForElement('.elfinder-dialog input[type=file]:not([directory])', 10);
    I.attachFile('.elfinder-dialog input[type=file]:not([directory])', 'tests/webpages/' + name);
    I.waitForElement(`.elfinder-cwd-filename[title="${name}"]`, 30);
    I.waitForInvisible('.elfinder-notify-upload', 30);
}

async function verifyUpload({ I, Document }) {
    I.amOnPage('/admin/v9/');
    originalConfig = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/settings/configuration/all?view=all&size=10000', { headers: { 'X-CSRF-Token': window.csrfToken } });
        const result = await response.json();
        const rows = result.content || result.data;
        return Object.fromEntries(rows.filter(row => ['imageMagickDir', 'galleryStripExif', 'thumbServletAllowedSizeMode', 'sendMailSaveEmail', 'sendMailSaveEmailPath', 'useSMTPServer'].includes(row.name))
            .map(row => [row.name, row.value]));
    });
    I.assertTrue(Object.hasOwn(originalConfig, 'imageMagickDir'));
    I.assertTrue(Object.hasOwn(originalConfig, 'galleryStripExif'));
    Document.setConfigValue('imageMagickDir', codecDirectory, true);
    Document.setConfigValue('galleryStripExif', 'false', true);
    Document.setConfigValue('thumbServletAllowedSizeMode', 'allow', true);
    // Keep page-change notifications local during the test.
    Document.setConfigValue('sendMailSaveEmailPath', '/WEB-INF/tmp/' + folderName, true);
    Document.setConfigValue('sendMailSaveEmail', 'true', true);
    Document.setConfigValue('useSMTPServer', 'true', true);
    I.amOnPage(filesPage);
    I.waitForElement('.elfinder-cwd-filename[title="money.jpg"]', 20);
    I.click('.elfinder-cwd-filename[title="money.jpg"]');
    I.clickCss('.elfinder-button-icon-mkdir');
    I.fillField('.elfinder-cwd-filename[title="Nový priečinok"] textarea', folderName);
    I.pressKey('Enter');
    I.waitForText(folderName, 10, '.elfinder-cwd-wrapper');
    I.doubleClick(`.elfinder-cwd-filename[title="${folderName}"]`);
    I.waitForElement(locate('.elfinder-navbar-dir.ui-state-active').withText(folderName), 10);
    await upload(I, fileName);
    await upload(I, 'autotest-animated.avif');

    const original = await inspectImage(I, imageUrl());
    assertImage(I, original, 120, 80);
    assertImage(I, await inspectImage(I, '/thumb' + imageUrl() + '?w=60'), 60, 40);
    assertImage(I, await inspectImage(I, '/thumb' + imageUrl() + '?w=60&h=40&ip=5'), 60, 40);
    I.assertEqual((await inspectImage(I, imageUrl())).bytes, original.bytes, 'Thumbnails must preserve the original');
    I.rightClick(`.elfinder-cwd-filename[title="${fileName}"]`);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-info');
    I.waitForText('120x80', 10, '.elfinder-dialog');
    I.pressKey('Escape');

    const animated = imageUrl().replace(fileName, 'autotest-animated.avif');
    const animation = await inspectImage(I, animated);
    I.assertEqual(animation.signature, 'ftypavis');
    const unchangedThumbnail = await inspectImage(I, '/thumb' + animated + '?w=240&h=160');
    I.assertEqual(unchangedThumbnail.status, 200);
    I.assertContain(unchangedThumbnail.mime, 'image/avif');
    I.assertEqual(unchangedThumbnail.bytes, animation.bytes, 'A copy must preserve the animation when metadata stripping is disabled');
    Document.setConfigValue('galleryStripExif', 'true', true);
    // Use different bounds to avoid reusing the thumbnail cached before metadata stripping was enabled.
    assertImage(I, await inspectImage(I, '/thumb' + animated + '?w=241&h=161'), 120, 80, false);
    assertImage(I, await inspectImage(I, '/thumb' + animated + '?w=60&h=40'), 60, 40, false);
    I.assertEqual((await inspectImage(I, animated)).bytes, animation.bytes);

}

/** Gallery upload creates derivatives and retains the original animation. */
async function verifyGallery({ I, DT }) {
    galleryFolder = folderName;
    I.amOnPage(galleryFilesPage);
    I.refreshPage();
    I.waitForElement('.elfinder-cwd-filename[title="koala.jpg"]', 20);
    I.click('.elfinder-cwd-filename[title="koala.jpg"]');
    I.clickCss('.elfinder-button-icon-mkdir');
    I.fillField('.elfinder-cwd-filename[title="Nový priečinok"] textarea', galleryFolder);
    I.pressKey('Enter');
    I.waitForText(galleryFolder, 10, '.elfinder-cwd-wrapper');
    I.doubleClick(`.elfinder-cwd-filename[title="${galleryFolder}"]`);
    I.waitForElement(locate('.elfinder-navbar-dir.ui-state-active').withText(galleryFolder), 10);
    await upload(I, 'autotest-animated.avif');
    const base = '/images/gallery/test/' + galleryFolder + '/';
    const original = await inspectImage(I, base + 'o_autotest-animated.avif');
    I.assertEqual(original.signature, 'ftypavis');
    for (const prefix of ['', 's_']) {
        const image = await inspectImage(I, base + prefix + 'autotest-animated.avif');
        I.assertEqual(image.status, 200);
        I.assertContain(image.mime, 'image/avif');
        // Metadata stripping produces a static image even when inherited gallery settings only copy it.
        I.assertEqual(image.signature, 'ftypavif');
        I.assertTrue(image.width > 0 && image.height > 0);
    }
    const uploaded = await inspectImage(I, imageUrl().replace(fileName, 'autotest-animated.avif'));
    I.assertEqual(original.bytes, uploaded.bytes);
    I.amOnPage('/admin/v9/apps/gallery/?dir=' + base);
    DT.waitForLoader();
    I.waitForText('autotest-animated.avif', 20, '#galleryTable');
}

async function verifyPage({ I, DTE, DT, Document }) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    originalEditorMode = await I.executeScript(() => WJ.getAdminSetting('editorTypeForced'));
    Document.setEditorMode('standard');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    pageName = 'autotest-avif-page-' + I.getRandomTextShort();
    I.clickCss('#datatableInit_wrapper button.buttons-create');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.fillField('#DTE_Field_title', pageName);
    I.fillField('#DTE_Field_navbar', pageName);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.waitForFunction(() => window.ckEditorInstance?.status === 'ready', [], 20);
    I.clickCss('.cke_button__image');
    I.waitForVisible('.cke_dialog', 10);
    I.switchTo('#wjImageIframeElement');
    I.waitForVisible('#txtUrl', 10);
    I.fillField('#txtUrl', imageUrl());
    I.pressKey('Tab');
    I.switchTo();
    I.waitForFunction(() => CKEDITOR.dialog.getCurrent().getValueOf('info', 'txtWidth') == '120', [], 20);
    I.clickCss('.cke_dialog_ui_button_ok');
    I.waitForInvisible('.cke_dialog', 10);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    I.fillField('#DTE_Field_perexImage', imageUrl());
    I.pressKey('Tab');
    DTE.save();
    DT.filterEquals('title', pageName);
    pageId = await I.executeScript(name => webpagesDatatable.rows().data().toArray().find(row => row.title === name).docId, pageName);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + pageId);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    I.seeInField('#DTE_Field_perexImage', imageUrl());
    I.seeElement('.DTE_Field_Name_perexImage .has-image');
    DTE.cancel();
    I.amOnPage('/showdoc.do?docid=' + pageId);
    I.waitForElement(`img[src^="${imageUrl()}"]`, 20);
    I.waitForFunction((src) => Array.from(document.images).some(image => image.src.includes(src) && image.naturalWidth === 120), [imageUrl()], 20);
}

async function verifyImageEditor({ I, DTE, Document }) {
    I.amOnPage(filesPage);
    I.waitForElement(`.elfinder-cwd-filename[title="${folderName}"]`, 20);
    I.doubleClick(`.elfinder-cwd-filename[title="${folderName}"]`);
    I.waitForElement(`.elfinder-cwd-filename[title="${fileName}"]`, 10);
    I.rightClick(`.elfinder-cwd-filename[title="${fileName}"]`);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjeditswitch');
    I.switchToNextTab();
    I.waitForVisible('#galleryTable_modal', 20);
    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.waitForFunction(() => document.querySelector('#photoEditorContainer canvas.lower-canvas')?.width === 120, [], 30);
    I.clickCss('li.tie-btn-crop.tui-image-editor-item');
    I.waitForVisible('div.tui-image-editor-menu-crop');
    I.clickCss('div.tui-image-editor-button.preset-square');
    I.clickCss('div.tui-image-editor-button.apply');
    I.waitForFunction(() => document.querySelector('#photoEditorContainer canvas.lower-canvas')?.width === 80, [], 20);
    I.saveScreenshot('autotest-avif-editor.png');
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.waitForVisible('#toast-container-upload .ti-circle-check', 60);
    I.switchToPreviousTab();
    I.closeOtherTabs();
    assertImage(I, await inspectImage(I, imageUrl()), 80, 80);

    const original = await inspectImage(I, imageUrl());
    Document.setConfigValue('imageMagickDir', '/autotest-avif-missing-codec', true);
    I.amOnPage('/admin/v9/');
    // A new size prevents a previously generated thumbnail from hiding the unavailable codec.
    const failed = await inspectImage(I, '/thumb' + imageUrl() + '?w=37&h=29&ip=5');
    I.assertEqual(failed.status, 200);
    I.assertContain(failed.mime, 'image/gif');
    I.assertContain(failed.cacheControl, 'no-store');
    const placeholder = await inspectImage(I, '/components/_common/mime/big_unknown.gif');
    I.assertEqual(failed.bytes, placeholder.bytes);
    I.assertEqual(failed.width, placeholder.width);
    I.assertEqual(failed.height, placeholder.height);
    I.assertEqual((await inspectImage(I, imageUrl())).bytes, original.bytes);
    Document.setConfigValue('imageMagickDir', codecDirectory, true);
    // This crop changes the aspect ratio and can remove the transparent border.
    assertImage(I, await inspectImage(I, '/thumb' + imageUrl() + '?w=37&h=29&ip=5'), 37, 29, false);
}

Scenario('AVIF upload, page insertion, thumbnails, image editor and missing codec @singlethread', async ({ I, DT, DTE, Document }) => {
    const helpers = { I, DT, DTE, Document };
    await verifyUpload(helpers);
    await verifyGallery(helpers);
    await verifyPage(helpers);
    await verifyImageEditor(helpers);
});

After(async ({ I, DT, DTE, Document }) => {
    I.switchTo();
    I.closeOtherTabs();
    if (pageName) {
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
        DT.waitForLoader();
        DT.filterContains('title', 'autotest-avif-page-');
        if (await I.grabNumberOfVisibleElements('#datatableInit .dt-row-edit')) {
            DT.deleteAll();
            DTE.waitForLoader();
        }
    }
    if (originalConfig) {
        for (const [key, value] of Object.entries(originalConfig)) Document.setConfigValue(key, value, true);
    }
    if (originalEditorMode !== undefined) await I.executeScript(value => WJ.setAdminSetting('editorTypeForced', value), originalEditorMode);
    for (const [page, name] of [[filesPage, folderName], [galleryFilesPage, galleryFolder]]) {
        if (!name) continue;
        I.amOnPage(page);
        I.refreshPage();
        I.waitForVisible('.elfinder-cwd', 20);
        const folder = '.elfinder-cwd-filename[title^="autotest-avif-"]';
        let count = await I.grabNumberOfVisibleElements(folder);
        while (count > 0) {
            I.rightClick(folder);
            I.waitForVisible('.elfinder-contextmenu', 10);
            I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rm');
            I.waitForVisible('.elfinder-confirm-accept', 10);
            I.clickCss('.elfinder-confirm-accept');
            I.waitForFunction(([selector, previousCount]) => document.querySelectorAll(selector).length < previousCount,
                [folder, count], 20);
            count = await I.grabNumberOfVisibleElements(folder);
        }
    }
});
