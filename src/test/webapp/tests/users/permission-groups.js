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

Scenario('test user perms', ({I, DT}) => {
    I.relogin("tester2");
    I.amOnPage("/admin/v9/settings/update/");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");

    I.amOnPage("/admin/v9/webpages/media/");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");

    //
    let mediaManuSelector = locate(".ly-sidebar .md-main-menu--open .md-main-menu__item > a").withText("Média");
    I.say("verify media menu item is linked to media groups subitem, because user doesnt have perms editor_edit_media_all");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.waitForElement(mediaManuSelector, 10);
    I.click(mediaManuSelector);
    I.waitForElement(locate(".ly-submenu li.nav-item a.nav-link.active").withText("Správa skupín"), 5);
    I.seeInCurrentUrl("/admin/v9/webpages/media-groups/");
    I.see("Média", ".header-title");

    //
    I.say("Verify also v8 menu");
    I.amOnPage("/components/aceintegration/admin-example.jsp#education-dashboard");
    I.click("a.md-large-menu__item__link[data-menu-id=website]");
    I.click(mediaManuSelector);
    I.waitForElement(locate(".ly-submenu li.nav-item a.nav-link.active").withText("Správa skupín"), 5);
    I.seeInCurrentUrl("/admin/v9/webpages/media-groups/");
    I.see("Média", ".header-title");

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