Feature('users.permission-groups');

Before(({ I, login }) => {
     login('admin');
});

Scenario('permission-groups', async ({I, DataTables }) => {
    I.amOnPage("/admin/v9/users/permission-groups/");
    await DataTables.baseTest({
        dataTable: 'permissionGroupsDataTable',
        perms: 'users.perm_groups'
    });
});