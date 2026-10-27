Feature('admin.elfinder-upload-types');

let folderName;
const pdfName = 'archive_replace.pdf';
const imageName = 'mxjfiles-watermelon-23023.png';
const fileSelector = name => `.elfinder-cwd-filename[title="${name}"]`;

Before(({ login }) => {
    login('admin');
});

function openLinkDialog(I, DTE) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=16');
    DTE.waitForEditor();
    I.clickCss('#trEditor');
    I.clickCss('.cke_button_icon.cke_button__link_icon');
    I.waitForText('Informácie o odkaze', 10);
    I.switchTo('#wjLinkIframe');
    I.waitForElement('#finder .elfinder-navbar', 20);
    I.waitForText('Médiá tejto stránky/Súbory', 20, '.elfinder-path');
}

function attachUpload(I, fixture, accept) {
    I.clickCss('.elfinder-button-icon-upload');
    I.waitForElement('.elfinder-dialog input[type=file]:not([directory])', 10);
    if (accept !== undefined) I.seeElementInDOM(`.elfinder-dialog input[type=file]:not([directory])[accept="${accept}"]`);
    I.attachFile('.elfinder-dialog input[type=file]:not([directory])', fixture);
}

Scenario('image dialog rejects documents and accepts images in Files', async ({ I, DTE }) => {
    folderName = 'autotest-upload-types-' + I.getRandomTextShort();

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=16');
    DTE.waitForEditor();
    I.clickCss('#trEditor');
    I.clickCss('.cke_button_icon.cke_button__image_icon');
    I.waitForText('Vlastnosti obrázka', 10);
    I.switchTo('#wjImageIframeElement');
    I.waitForElement('#finder .elfinder-navbar', 20);
    I.waitForText('Médiá tejto stránky/Obrázky', 20, '.elfinder-path');
    I.click(locate('.elfinder-navbar-dir').withText('Súbory').first());
    I.waitForText('Médiá tejto stránky/Súbory', 15, '.elfinder-path');

    I.clickCss('.elfinder-button-icon-mkdir');
    I.waitForElement('.elfinder-cwd-filename textarea', 10);
    I.fillField('.elfinder-cwd-filename textarea', folderName);
    I.pressKey('Enter');
    I.waitForElement(fileSelector(folderName), 15);
    I.doubleClick(fileSelector(folderName));
    I.waitForText(folderName, 15, '.elfinder-path');

    I.startRecordingTraffic();
    attachUpload(I, 'tests/apps/file-archive/docs/archive_replace.pdf', 'image/*,video/*,audio/*');
    I.waitForText('Nepovolený typ súboru', 20, '.elfinder-dialog-error');
    I.see(pdfName, '.elfinder-dialog-error');
    I.dontSeeElement(fileSelector(pdfName));
    I.clickCss('.elfinder-dialog-error .ui-dialog-titlebar-close');

    // Dropping a document must also fail before any file data is sent.
    await I.executeScript(name => {
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(new File(['%PDF-1.4'], name, { type: 'application/pdf' }));
        document.querySelector('.elfinder-cwd-wrapper').dispatchEvent(new DragEvent('drop', { bubbles: true, dataTransfer }));
    }, pdfName);
    I.waitForText('Nepovolený typ súboru', 10, '.elfinder-dialog-error');
    I.clickCss('.elfinder-dialog-error .ui-dialog-titlebar-close');
    const traffic = await I.grabRecordedNetworkTraffics();
    I.stopRecordingTraffic();
    I.assertEqual(traffic.filter(request => String(request.requestPostData).includes('name="upload[]"')).length, 0,
        'Rejected documents must not send file data to the server');

    attachUpload(I, 'tests/webpages/mxjfiles-watermelon-23023.png');
    I.waitForElement(fileSelector(imageName), 30);
    I.waitForInvisible('.elfinder-notify-upload', 30);
    I.switchTo();
});

Scenario('link dialog still accepts documents in the same folder', ({ I, DTE }) => {
    openLinkDialog(I, DTE);
    I.waitForElement(fileSelector(folderName), 15);
    I.doubleClick(fileSelector(folderName));
    I.waitForText(folderName, 15, '.elfinder-path');
    // The unfiltered link picker confirms that the rejected document was never saved.
    I.seeElement(fileSelector(imageName));
    I.dontSeeElement(fileSelector(pdfName));

    attachUpload(I, 'tests/apps/file-archive/docs/archive_replace.pdf', '');
    I.waitForElement(fileSelector(pdfName), 30);
    I.waitForInvisible('.elfinder-notify-upload', 30);
    I.dontSeeElement('.elfinder-dialog-error');
    I.switchTo();
});

Scenario('cleanup media picker upload fixtures', async ({ I, DTE }) => {
    openLinkDialog(I, DTE);
    const folders = await I.grabNumberOfVisibleElements(fileSelector(folderName));
    if (folders > 0) {
        I.clickCss(fileSelector(folderName));
        I.pressKey('Delete');
        I.waitForVisible('.elfinder-confirm-accept', 10);
        I.clickCss('.elfinder-confirm-accept');
        I.waitForInvisible(fileSelector(folderName), 20);
    }
    I.switchTo();
});
