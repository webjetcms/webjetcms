Feature('permission-groups');

Before(({ I, login }) => {
    login('admin');
});

Scenario('permission-groups', ({ I, Document }) => {
    I.amOnPage("/admin/v9/users/permission-groups/");

    Document.screenshot("/admin/users/permissiongroups-datatable.png", 1280, 700);

    I.click("button.buttons-create");
    I.clickCss("#pills-dt-permissionGroupsDataTable-perms-tab");
    Document.screenshotElement("div.DTE_Action_Create", "/admin/users/permissiongroups-editor.png");
});