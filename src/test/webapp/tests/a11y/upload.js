Feature('a11y.upload');

Before(({ I, login }) => {
    login('admin');
});

Scenario('p32: file upload', async ({ I, a11y }) => {
    I.amOnPage("/apps/contact/admin/upload/");
    await a11y.check();
});

Scenario('p32: dropzone', async ({ I, DT, DTE, a11y }) => {
    I.amOnPage("/apps/file-archive/admin/");
    DT.waitForLoader();
    DT.clickAddButton("fileArchiveDataTable");
    DTE.waitForEditor("fileArchiveDataTable");

    await a11y.check();
});