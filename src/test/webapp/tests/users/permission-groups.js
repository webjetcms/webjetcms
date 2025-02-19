Feature('users.permission-groups');

Before(({ I, login }) => {
     login('admin');
});

Scenario('permission-groups @baseTest', async ({I, DataTables }) => {
    I.amOnPage("/admin/v9/users/permission-groups/");
    await DataTables.baseTest({
        dataTable: 'permissionGroupsDataTable',
        perms: 'users.perm_groups',
        skipSwitchDomain: true
    });
});

Scenario('test user perms', ({I}) => {
    I.relogin("tester2");
    I.amOnPage("/admin/v9/settings/update/");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");

    I.amOnPage("/admin/v9/webpages/media/");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");

    //
    I.relogin("tester3");
    I.say("User has this perms from group Admini");
    I.amOnPage("/admin/v9/settings/update/");
    I.see("Aktualizácia WebJETu", ".header-title");
    I.amOnPage("/admin/v9/webpages/media/");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario('logout', ({ I }) => {
    I.logout();
});