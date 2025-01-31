Feature('apps.user-groups');

Before(({ login }) => {
    login("admin");
});

Scenario('User groups screenshots', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/users/user-groups/");
    DT.waitForLoader();

    Document.screenshot("/admin/users/user-groups-datatable.png");

    DT.filterContains("userGroupName", "ReservationDiscount");
    I.clickCss("td.dt-select-td.sorting_1");

    Document.screenshotElement("button.buttons-addGroupToAll", "/admin/users/user-groups-addGroupToAll.png");
    Document.screenshotElement("button.buttons-removeGroupFromAll", "/admin/users/user-groups-removeGroupFromAll.png");
    Document.screenshotElement("button.buttons-removeUsersFromGroup", "/admin/users/user-groups-removeUsersFromGroup.png");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("userGroupsDataTable");

    Document.screenshotElement("div.DTE_Action_Create", "/admin/users/user-groups-editor.png");
});