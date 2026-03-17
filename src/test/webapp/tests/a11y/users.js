Feature('a11y.users');

Before(({ I, login }) => {
    login('admin');
});

Scenario('user groups tab', async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/users/user-list/?id=7");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-groupsTab-tab");

    await a11y.check();
});

Scenario('permissions tab', async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/users/user-list/?id=7");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-rightsTab-tab");

    await a11y.check();
});

Scenario('writable folders modal', async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/users/user-list/?id=7");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-rightsTab-tab");

    I.click(".DTE_Field_Name_editorFields\\.writableFolders button.btn-vue-jstree-add");
    I.waitForElement(".custom-modal.open-custom-modal .jsTree-wrapper", 10);
    I.clickCss('li[id="/images"] i');

    await a11y.check();
});

Scenario('editable pages modal', async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/users/user-list/?id=7");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-rightsTab-tab");

    I.click(".DTE_Field_Name_editorFields\\.editablePages button.btn-vue-jstree-add");
    I.waitForElement(".custom-modal.open-custom-modal .jsTree-wrapper", 10);
    I.clickCss('li[id="domain:test23.tau27.iway.sk"] i');
    I.waitForElement('li[id="83"] i', 10);
    I.clickCss('li[id="83"] i');

    await a11y.check();
});