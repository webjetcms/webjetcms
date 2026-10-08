Feature('admin.file-archive-picker').tag('@singlethread');

const SL = require('../apps/file-archive/SL.js');

const fileName = 'archive_picker_test.pdf';
let virtualFileName;

Before(({ I, login }) => {
    I.switchTo();
    login('admin');
    if (!virtualFileName) {
        virtualFileName = SL.randomName('file-archive-picker');
    }
});

Scenario('File archive picker in dialog.pug after maximalize, test vizual', async ({ I, DT, DTE }) => {
    I.resizeWindow(1920, 1200);
    openArchivePicker(I, DT, DTE);
    I.switchTo();

    const layoutFits = () => {
        const picker = document.querySelector('#modalIframeIframeElement').contentWindow;
        const archive = picker.document.querySelector('#fileArchiveIframe').contentWindow;
        const content = archive.document.querySelector('.ly-content').getBoundingClientRect();
        const toolbar = archive.document.querySelector('#fileArchiveDataTable_wrapper .dt-header-row').getBoundingClientRect();
        const footer = archive.document.querySelector('#fileArchiveDataTable_wrapper .dt-footer-row').getBoundingClientRect();
        const link = picker.document.querySelector('#linkInput').getBoundingClientRect();
        return toolbar.top >= 0 && footer.bottom <= content.bottom + 1
            && footer.bottom <= archive.innerHeight + 1
            && footer.bottom >= archive.innerHeight - 2
            && Math.abs(content.height - archive.innerHeight) <= 1
            && link.bottom <= picker.innerHeight + 1;
    };

    I.waitForFunction(layoutFits, 15);
    I.clickCss('#modalIframe .dialog-buttons .maximize');
    I.waitForElement('#modalIframe .modal-fullscreen', 10);
    I.waitForFunction(layoutFits, 15);
    I.saveScreenshot('file-archive-picker-maximized.png');

    I.clickCss('#modalIframe .dialog-buttons .minimize');
    I.waitForInvisible('#modalIframe .modal-fullscreen', 10);
    I.waitForFunction(layoutFits, 15);
    I.saveScreenshot('file-archive-picker-restored.png');
    I.clickCss('#modalIframe .dialog-buttons .btn-close');
    I.waitForInvisible('#modalIframe', 10);
    DTE.cancel('datatableInit');
});

Scenario('Upload an archive file and insert its link into a new web page', async ({ I, DT, DTE }) => {
    const expectedUrl = '/files/archiv/' + fileName;

    openArchivePicker(I, DT, DTE);
    SL.uploadFile(virtualFileName, '../../../admin/' + fileName);
    DTE.save('fileArchiveDataTable');
    I.waitForInvisible('#fileArchiveDataTable_modal', 20);
    DT.filterEquals('virtualFileName', virtualFileName);
    I.see(fileName, '#fileArchiveDataTable tbody');
    I.click(locate('#fileArchiveDataTable tbody .dt-row-edit a').withText(virtualFileName));

    I.switchTo();
    I.switchTo('#modalIframeIframeElement');
    I.seeInField('#file', expectedUrl);
    I.switchTo();
    I.clickCss('#modalIframe .modal-footer button.btn-primary');
    I.waitForInvisible('#modalIframe', 10);
    I.assertEqual(await I.grabValueFrom('#DTE_Field_externalLink'), expectedUrl, 'The page field must receive the selected archive URL.');
    const uploadStatus = await I.executeScript(async url => (await fetch(url, { cache: 'no-store' })).status, expectedUrl);
    I.assertEqual(uploadStatus, 200, 'The uploaded file must be accessible.');
    DTE.cancel('datatableInit');
});

Scenario('Delete the uploaded archive picker file', async ({ I, DT, DTE }) => {
    openArchivePicker(I, DT, DTE);
    DT.filterEquals('virtualFileName', virtualFileName);
    if (await I.grabNumberOfVisibleElements('#fileArchiveDataTable tbody .dt-row-edit a') > 0) {
        I.see(fileName, '#fileArchiveDataTable tbody');
        DT.deleteAll('fileArchiveDataTable');
        DT.waitForLoader('fileArchiveDataTable');
    }
    I.waitForElement('#fileArchiveDataTable tbody td.dt-empty', 10);
    I.dontSee(virtualFileName, '#fileArchiveDataTable tbody');
    I.switchTo();
    I.clickCss('#modalIframe .dialog-buttons .btn-close');
    I.waitForInvisible('#modalIframe', 10);
    DTE.cancel('datatableInit');

    I.amOnPage(SL.elfinder);
    I.waitForElement('#finder .elfinder-navbar', 20);
    I.waitForFunction(() => {
        const finder = $('#finder').elfinder('instance');
        return finder && finder.cwd().hash === 'iwcm_2_L2ZpbGVzL2FyY2hpdg_E_E';
    }, 20);
    I.dontSeeElement('.elfinder-cwd-filename[title="' + fileName + '"]');
});

/** Opens the archive tab from a new page's Basic tab and switches into its iframe. */
function openArchivePicker(I, DT, DTE) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.clickCss('#panel-body-dt-datatableInit-basic .DTE_Field_Name_externalLink .DTE_Field_InputControl button');
    I.waitForVisible('#modalIframe', 10);
    I.switchTo('#modalIframeIframeElement');
    I.waitForElement('#finder .elfinder-navbar', 20);
    I.clickCss('#fileArchiveTab');
    I.switchTo('#fileArchiveIframe');
    I.waitForVisible('#fileArchiveDataTable_wrapper .dt-footer-row', 20);
    DT.waitForLoader('fileArchiveDataTable');
}
