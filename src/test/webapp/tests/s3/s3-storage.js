const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

Feature('s3.s3-storage');

let folderName;
let originalUseForDirs;
// elFinder adds zero-width spaces for wrapping; hovering also removes the title attribute.
const fileSelector = name => locate({ xpath: `//div[contains(@class, 'elfinder-cwd-filename') and translate(., '\u200b', '')="${name}"]` });
const filesPage = '/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVz';

Before(({ login }) => login('admin'));

Scenario('configure isolated S3 paths @singlethread', async ({ I, Document }) => {
    folderName = 'autotest-s3-' + I.getRandomTextShort();
    I.amOnPage('/admin/v9/');
    originalUseForDirs = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/settings/configuration/autocomplete/detail?name=s3UseForDirs', {
            headers: { 'X-CSRF-Token': window.csrfToken }
        });
        if (!response.ok) throw new Error('Cannot preserve S3 routing: HTTP ' + response.status);
        const configuration = await response.json();
        const value = configuration.displayValue ?? configuration.value;
        if (configuration.name !== 's3UseForDirs' || typeof value !== 'string') {
            throw new Error('Cannot preserve the runtime s3UseForDirs configuration');
        }
        return value;
    });
    Document.setConfigValue('s3UseForDirs', `/files/${folderName},/images/gallery/${folderName},/WEB-INF/imgcache/images/gallery/${folderName}`, true);
});

Scenario('explorer multiple upload, Unicode rename and HTTP download @singlethread', async ({ I }) => {
    I.amOnPage(filesPage);
    I.waitForVisible('#nav-iwcm_2_L2ZpbGVz', 20);
    I.clickCss('#nav-iwcm_2_L2ZpbGVz');
    I.clickCss('.elfinder-button-icon-mkdir');
    I.waitForElement('.elfinder-cwd-filename textarea', 10);
    I.fillField('.elfinder-cwd-filename textarea', folderName);
    I.pressKey('Enter');
    I.waitForElement(fileSelector(folderName), 15);
    I.doubleClick(fileSelector(folderName));
    I.waitForText(folderName, 15, '.elfinder-path');
    I.clickCss('.elfinder-button-icon-upload');
    I.waitForElement('.elfinder-dialog input[type=file]:not([directory])', 10);
    // CodeceptJS 3.6 attachFile accepts only one path; use the browser API for a single multi-file selection.
    I.usePlaywrightTo('select two files in the upload dialog', async ({ page }) => {
        await page.locator('.elfinder-dialog input[type=file]:not([directory])').setInputFiles([
            { name: 'ľščťú žýáíéô.png', mimeType: 'image/png', buffer: fs.readFileSync('tests/admin/ľščťú žýáíéô.png') },
            { name: 'autotest-second.txt', mimeType: 'text/plain', buffer: Buffer.from('autotest-multiple-upload') }
        ]);
    });
    I.waitForElement(fileSelector('ľščťú žýáíéô.png'), 30);
    I.waitForElement(fileSelector('autotest-second.txt'), 30);
    I.waitForInvisible('.elfinder-notify-upload', 30);
    I.click(fileSelector('ľščťú žýáíéô.png'));
    I.pressKey('F2');
    I.waitForElement('.elfinder-cwd-filename textarea', 10);
    I.fillField('.elfinder-cwd-filename textarea', 'autotest-žltý obrázok.png');
    I.pressKey('Enter');
    I.waitForElement(fileSelector('autotest-žltý obrázok.png'), 20);
    const downloaded = await I.executeScript(async url => {
        const response = await fetch(url);
        return { status: response.status, bytes: Array.from(new Uint8Array(await response.arrayBuffer())), etag: response.headers.get('etag') };
    }, `/files/${folderName}/autotest-žltý obrázok.png`);
    assert.equal(downloaded.status, 200);
    assert.deepEqual(Buffer.from(downloaded.bytes), fs.readFileSync('tests/admin/ľščťú žýáíéô.png'));
    assert.ok(downloaded.etag);
    const second = await I.executeScript(async url => {
        const response = await fetch(url);
        return { status: response.status, text: await response.text() };
    }, `/files/${folderName}/autotest-second.txt`);
    assert.deepEqual(second, { status: 200, text: 'autotest-multiple-upload' });
    assert.equal(fs.existsSync(path.resolve('../../main/webapp/files', folderName, 'autotest-žltý obrázok.png')), false);
});

Scenario('chunk upload and conditional download @singlethread', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    // Fetch exercises the real authenticated servlet with deterministic chunk boundaries.
    const result = await I.executeScript(async folder => {
        const name = 'autotest-chunks.txt';
        const statuses = [];
        for (let chunk = 0; chunk < 3; chunk++) {
            const data = new FormData();
            data.set('name', name);
            data.set('destinationFolder', `/files/${folder}/`);
            data.set('writeDirectlyToDestination', 'true');
            data.set('overwriteMode', 'overwrite');
            data.set('uploadType', 'file');
            data.set('dztotalfilesize', '6');
            data.set('chunk', chunk);
            data.set('chunks', '3');
            data.set('file', new File([new Uint8Array([65 + chunk * 2, 66 + chunk * 2])], name, { type: 'text/plain' }));
            const response = await fetch('/admin/upload/chunk', { method: 'POST', headers: { 'X-CSRF-TOKEN': window.csrfToken }, body: data });
            statuses.push(await response.json());
        }
        const url = `/files/${folder}/${name}`;
        const file = await fetch(url);
        const etag = file.headers.get('etag');
        const bytes = Array.from(new Uint8Array(await file.arrayBuffer()));
        const cached = await fetch(url, { headers: { 'If-None-Match': etag } });
        const range = await fetch(url, { headers: { Range: 'bytes=2-4' } });
        return { statuses, bytes, cached: cached.status, range: range.status, rangeBytes: Array.from(new Uint8Array(await range.arrayBuffer())) };
    }, folderName);
    for (const response of result.statuses) assert.equal(response.success, true, JSON.stringify(response));
    assert.deepEqual(result.bytes, [65, 66, 67, 68, 69, 70]);
    assert.equal(result.cached, 304);
    assert.equal(result.range, 206);
    assert.deepEqual(result.rangeBytes, [67, 68, 69]);
});

Scenario('gallery upload and cached thumbnails @singlethread', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/');
    const files = ['gallery.png', 'puppy-2785074.jpg'].map(name => ({
        name: 'autotest-' + name,
        base64: fs.readFileSync(`tests/apps/gallery/${name}`).toString('base64')
    }));
    const uploaded = await I.executeScript(async ({ folder, files }) => {
        const results = [];
        for (const file of files) {
            const bytes = Uint8Array.from(atob(file.base64), c => c.charCodeAt(0));
            const data = new FormData();
            data.set('name', file.name);
            data.set('destinationFolder', `/images/gallery/${folder}/`);
            data.set('writeDirectlyToDestination', 'true');
            data.set('overwriteMode', 'overwrite');
            data.set('uploadType', 'image');
            data.set('dztotalfilesize', bytes.length);
            data.set('file', new File([bytes], file.name));
            const response = await fetch('/admin/upload/chunk', { method: 'POST', headers: { 'X-CSRF-TOKEN': window.csrfToken }, body: data });
            results.push(await response.json());
        }
        return results;
    }, { folder: folderName, files });
    uploaded.forEach(response => assert.equal(response.success, true, JSON.stringify(response)));
    I.amOnPage(`/admin/v9/apps/gallery/?dir=/images/gallery/${folderName}`);
    DT.waitForLoader();
    I.waitForText('autotest-gallery.png', 20, '#galleryTable');
    I.waitForText('autotest-puppy-2785074.jpg', 20, '#galleryTable');
    // Gallery cards use CSS backgrounds; verify that the displayed thumbnail URLs decode.
    I.waitForFunction(async () => {
        const cards = Array.from(document.querySelectorAll('#galleryTable tbody .img'));
        if (cards.length !== 2) return false;
        const decoded = await Promise.all(cards.map(async card => {
            const url = getComputedStyle(card).backgroundImage.match(/^url\(["']?(.*?)["']?\)$/)?.[1];
            if (!url) return false;
            const image = new Image();
            image.src = url;
            try { await image.decode(); } catch { return false; }
            return image.naturalWidth > 0;
        }));
        return decoded.every(Boolean);
    }, [], 30);
    const thumbs = await I.executeScript(async folder => {
        const statuses = [];
        for (let i = 0; i < 2; i++) {
            const response = await fetch(`/thumb/images/gallery/${folder}/autotest-gallery.png?w=100&h=100`);
            statuses.push({ status: response.status, size: (await response.arrayBuffer()).byteLength });
        }
        return statuses;
    }, folderName);
    thumbs.forEach(thumb => { assert.equal(thumb.status, 200); assert.ok(thumb.size > 0); });
});

Scenario('cleanup gallery metadata and files @singlethread', async ({ I, DT, DTE }) => {
    I.amOnPage(`/admin/v9/apps/gallery/?dir=/images/gallery/${folderName}`);
    DT.waitForLoader();
    if (await I.grabNumberOfVisibleElements(locate('#SomStromcek a').withText(folderName))) {
        I.click(DT.btn.tree_delete_button);
        DTE.waitForModal('galleryDimensionDatatable_modal');
        DTE.save();
    }
});

Scenario('cleanup S3 fixtures and restore routing @singlethread', async ({ I, Document }) => {
    I.amOnPage(filesPage);
    I.waitForVisible('#nav-iwcm_2_L2ZpbGVz', 20);
    I.clickCss('#nav-iwcm_2_L2ZpbGVz');
    if (folderName && await I.grabNumberOfVisibleElements(fileSelector(folderName))) {
        I.click(fileSelector(folderName));
        I.pressKey('Delete');
        I.waitForVisible('.elfinder-confirm-accept', 10);
        I.clickCss('.elfinder-confirm-accept');
        I.waitForInvisible(fileSelector(folderName), 20);
    }
    if (originalUseForDirs !== undefined) Document.setConfigValue('s3UseForDirs', originalUseForDirs, true);
});
