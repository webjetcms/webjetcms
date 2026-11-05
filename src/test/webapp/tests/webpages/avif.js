Feature('webpages.avif');

const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { PNG } = require('pngjs');
const codecDirectory = process.env.AVIF_TEST_IMAGEMAGICK_DIR || '/usr/bin';
const filesPage = '/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9wcm9kdWt0b3ZhLXN0cmFua2E_E';
const galleryFilesPage = '/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9nYWxsZXJ5L3Rlc3Q_E';
const fileName = 'autotest-transparent.avif';
let folderName;
let originalConfig;
let pageId;
let pageName;
let originalEditorMode;
let createdFolders = [];
const imageUrl = () => '/images/produktova-stranka/' + folderName + '/' + fileName;

Before(({ I, login }) => {
    I.switchTo();
    login('admin');
    I.closeOtherTabs();
});

/** Read the actual response, decode its pixels in the browser and verify its container. */
async function inspectImage(I, url) {
    return I.executeScript(async (source) => {
        const response = await fetch(source, { cache: 'reload' });
        const data = await response.arrayBuffer();
        const bytes = new Uint8Array(data);
        const result = { status: response.status, mime: response.headers.get('content-type'),
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

/** Check rendered frames: canvas drawImage() only exposes the default frame of an animated image. */
async function assertAnimationPlayback(I, url) {
    await I.executeScript(async source => {
        const image = new Image();
        image.id = 'autotest-avif-playback';
        image.style.cssText = 'position:fixed;top:0;left:0;width:120px;height:80px;object-fit:contain;background:white;z-index:2147483647';
        document.body.appendChild(image);
        image.src = source;
        await image.decode();
    }, url);
    // Element screenshots are needed to inspect actual animation playback in Chromium and Firefox.
    const frames = await I.usePlaywrightTo('verify AVIF playback', async ({ page }) => {
        const image = page.locator('#autotest-avif-playback');
        const seen = { red: false, blue: false };
        const deadline = Date.now() + 5000;
        while (Date.now() < deadline && !(seen.red && seen.blue)) {
            const pixels = PNG.sync.read(await image.screenshot({ animations: 'allow' })).data;
            let hasRed = false, hasBlue = false;
            for (let i = 0; i < pixels.length; i += 4) {
                if (pixels[i] > 200 && pixels[i + 1] < 60 && pixels[i + 2] < 60) hasRed = true;
                if (pixels[i + 2] > 200 && pixels[i] < 60 && pixels[i + 1] < 60) hasBlue = true;
            }
            // Require separate frames; a static error icon may contain both colors at once.
            if (hasRed && !hasBlue) seen.red = true;
            if (hasBlue && !hasRed) seen.blue = true;
        }
        return seen;
    });
    await I.executeScript(() => document.getElementById('autotest-avif-playback').remove());
    I.assertTrue(frames.red && frames.blue, 'Both red and blue AVIF frames must play: ' + url);
}

async function upload(I, name) {
    I.clickCss('.elfinder-button-icon-upload');
    I.waitForElement('.elfinder-dialog input[type=file]:not([directory])', 10);
    I.attachFile('.elfinder-dialog input[type=file]:not([directory])', 'tests/webpages/' + name);
    I.waitForElement(`.elfinder-cwd-filename[title="${name}"]`, 30);
    I.waitForInvisible('.elfinder-notify-upload', 30);
}

/** Each workflow owns its files and captures the environment settings changed by that workflow. */
async function prepare({ I, Document }, name, withPage = false) {
    folderName = 'autotest-avif-' + name + '-' + I.getRandomTextShort();
    createdFolders = [];
    pageName = undefined;
    originalEditorMode = undefined;
    originalConfig = undefined;
    I.amOnPage('/admin/v9/');
    const settings = { imageMagickDir: codecDirectory, galleryStripExif: 'true', thumbServletAllowedSizeMode: 'allow' };
    if (withPage) {
        // Keep page-change notifications local during the test.
        Object.assign(settings, { sendMailSaveEmailPath: '/WEB-INF/tmp/' + folderName,
            sendMailSaveEmail: 'true', useSMTPServer: 'true' });
    }
    originalConfig = await I.executeScript(async keys => {
        const response = await fetch('/admin/rest/settings/configuration/all?view=all&size=10000', { headers: { 'X-CSRF-Token': window.csrfToken } });
        const result = await response.json();
        const rows = result.content || result.data;
        return Object.fromEntries(rows.filter(row => keys.includes(row.name)).map(row => [row.name, row.value]));
    }, Object.keys(settings));
    for (const key of Object.keys(settings)) I.assertTrue(Object.hasOwn(originalConfig, key));
    for (const [key, value] of Object.entries(settings)) Document.setConfigValue(key, value, true);
}

async function createFolder(I, page, existingFile) {
    createdFolders.push([page, folderName]);
    I.amOnPage(page);
    I.waitForElement(`.elfinder-cwd-filename[title="${existingFile}"]`, 20);
    I.click(`.elfinder-cwd-filename[title="${existingFile}"]`);
    I.clickCss('.elfinder-button-icon-mkdir');
    I.waitForVisible('.elfinder-cwd-filename textarea', 10);
    I.fillField('.elfinder-cwd-filename textarea', folderName);
    I.pressKey('Enter');
    I.waitForText(folderName, 10, '.elfinder-cwd-wrapper');
    I.doubleClick(`.elfinder-cwd-filename[title="${folderName}"]`);
    I.waitForElement(locate('.elfinder-navbar-dir.ui-state-active').withText(folderName), 10);
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
    I.seeElement('.DTE_Field_Name_perexImage .input-group-text');
    const preview = await I.grabCssPropertyFrom('.DTE_Field_Name_perexImage .input-group-text', 'background-image');
    I.assertContain(preview, imageUrl());
    const previewUrl = preview.match(/^url\(["']?(.*?)["']?\)$/)[1];
    assertImage(I, await inspectImage(I, previewUrl), 120, 80);
    DTE.cancel();
    I.amOnPage('/showdoc.do?docid=' + pageId);
    I.waitForElement(`img[src^="${imageUrl()}"]`, 20);
    I.waitForFunction((src) => Array.from(document.images).some(image => image.src.includes(src) && image.naturalWidth === 120), [imageUrl()], 20);
}

async function cropInEditor(I) {
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
}

/** Restore configuration before deleting test data so a deletion failure cannot leave the codec disabled. */
async function cleanup({ I, DT, DTE, Document }) {
    I.switchTo();
    I.closeOtherTabs();
    if (originalConfig) {
        for (const [key, value] of Object.entries(originalConfig)) Document.setConfigValue(key, value, true);
    }
    if (originalEditorMode !== undefined) {
        Document.setEditorMode(originalEditorMode);
    }
    if (pageName) {
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
        DT.waitForLoader();
        DT.filterEquals('title', pageName);
        if (await I.grabNumberOfVisibleElements('#datatableInit .dt-row-edit')) {
            DT.deleteAll();
            DTE.waitForLoader();
        }
    }
    for (const [page, name] of createdFolders) {
        I.amOnPage(page);
        I.refreshPage();
        I.waitForVisible('.elfinder-cwd', 20);
        const folder = `.elfinder-cwd-filename[title="${name}"]`;
        if (await I.grabNumberOfVisibleElements(folder)) {
            I.rightClick(folder);
            I.waitForVisible('.elfinder-contextmenu', 10);
            I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rm');
            I.waitForVisible('.elfinder-confirm-accept', 10);
            I.clickCss('.elfinder-confirm-accept');
            I.waitForInvisible(folder, 20);
        }
    }
}

Scenario('AVIF upload, thumbnail and page insertion @singlethread', async ({ I, DT, DTE, Document }) => {
    const helpers = { I, DT, DTE, Document };
    await prepare(helpers, 'page', true);
    await createFolder(I, filesPage, 'money.jpg');
    await upload(I, fileName);
    const original = await inspectImage(I, imageUrl());
    assertImage(I, original, 120, 80);
    assertImage(I, await inspectImage(I, '/thumb' + imageUrl() + '?w=60'), 60, 40);
    I.assertEqual((await inspectImage(I, imageUrl())).bytes, original.bytes, 'Thumbnails must preserve the original');
    await verifyPage(helpers);
});
Scenario('AVIF page cleanup @singlethread', cleanup);

Scenario('AVIF gallery upload preserves the animated original @singlethread', async ({ I, DT, Document }) => {
    await prepare({ I, Document }, 'gallery');
    await createFolder(I, galleryFilesPage, 'koala.jpg');
    const animatedFileName = 'autotest-animated-alpha.avif';
    await upload(I, animatedFileName);
    const base = '/images/gallery/test/' + folderName + '/';
    const original = await inspectImage(I, base + 'o_' + animatedFileName);
    I.assertEqual(original.status, 200);
    I.assertEqual(original.bytes, readFileSync(join(__dirname, animatedFileName)).join(','));
    for (const prefix of ['', 's_']) {
        const image = await inspectImage(I, base + prefix + animatedFileName);
        I.assertEqual(image.status, 200);
        I.assertContain(image.mime, 'image/avif');
        I.assertTrue(['ftypavif', 'ftypavis'].includes(image.signature));
        I.assertTrue(image.width > 0 && image.height > 0);
        await assertAnimationPlayback(I, base + prefix + animatedFileName);
    }
    for (const ip of [-1, 1, 2, 3, 4, 5, 6]) {
        const url = '/thumb' + base + animatedFileName + '?w=60&h=40' + (ip > 0 ? '&ip=' + ip : '');
        const thumbnail = await inspectImage(I, url);
        I.assertEqual(thumbnail.status, 200);
        I.assertContain(thumbnail.mime, 'image/avif');
        I.assertTrue(['ftypavif', 'ftypavis'].includes(thumbnail.signature));
        await assertAnimationPlayback(I, url);
    }
    I.amOnPage('/admin/v9/apps/gallery/?dir=' + base);
    DT.waitForLoader();
    I.waitForText(animatedFileName, 20, '#galleryTable');
});
Scenario('AVIF gallery cleanup @singlethread', cleanup);

Scenario('AVIF editor reports a missing codec and saves after recovery @singlethread', async ({ I, DTE, Document }) => {
    await prepare({ I, Document }, 'editor');
    await createFolder(I, filesPage, 'money.jpg');
    await upload(I, fileName);
    const original = await inspectImage(I, imageUrl());
    Document.setConfigValue('imageMagickDir', '/autotest-avif-missing-codec', true);
    await cropInEditor(I);
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.waitForText('Obrázok sa nepodarilo uložiť.', 30, '.toast-error');
    I.seeElement('#toast-container-upload [data-upload-status="error"]');
    I.assertEqual((await inspectImage(I, imageUrl())).bytes, original.bytes, 'A failed editor save must preserve the original');
    I.switchToPreviousTab();
    I.closeOtherTabs();
    Document.setConfigValue('imageMagickDir', codecDirectory, true);
    await cropInEditor(I);
    I.clickCss('.DTE_Form_Buttons > button.btn-primary');
    DTE.waitForLoader();
    I.waitForVisible('#toast-container-upload [data-upload-status="success"]', 60);
    I.switchToPreviousTab();
    I.closeOtherTabs();
    assertImage(I, await inspectImage(I, imageUrl()), 80, 80);
});
Scenario('AVIF editor cleanup @singlethread', cleanup);
