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

Scenario('test allEditableGroups and allWritableFolders checkboxes', ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/users/permission-groups/");
    DT.waitForLoader();

    I.say("Create new permission group");
    DT.clickAddButton("permissionGroupsDataTable");
    DTE.waitForEditor("permissionGroupsDataTable");

    I.say("Switch to dirs tab and verify checkboxes exist");
    I.clickCss("#pills-dt-permissionGroupsDataTable-dirs-tab");
    I.waitForElement(".DTE_Field_Name_allEditableGroups", 5);
    I.seeElement(".DTE_Field_Name_allWritableFolders");

    I.say("Verify tree selectors are visible initially");
    I.seeElement(".DTE_Field_Name_editorFields\\.editableGroups");
    I.seeElement(".DTE_Field_Name_editorFields\\.writableFolders");

    I.say("Check allEditableGroups and verify tree selectors are hidden");
    DTE.clickSwitch("allEditableGroups_0");
    I.dontSeeElement(".DTE_Field_Name_editorFields\\.editableGroups:visible");
    I.dontSeeElement(".DTE_Field_Name_editorFields\\.editablePages:visible");

    I.say("Check allWritableFolders and verify writable folders tree is hidden");
    DTE.clickSwitch("allWritableFolders_0");
    I.dontSeeElement(".DTE_Field_Name_editorFields\\.writableFolders:visible");

    I.say("Uncheck allEditableGroups and verify tree selectors are visible again");
    DTE.clickSwitch("allEditableGroups_0");
    I.seeElement(".DTE_Field_Name_editorFields\\.editableGroups");

    DTE.cancel("permissionGroupsDataTable");
});

function setEditableGroups(enabled, I, DTE) {
    I.amOnPage("/admin/v9/users/permission-groups/?id=4");
    DTE.waitForEditor("permissionGroupsDataTable");
    I.say("Disabling allEditableGroups");

    I.clickCss("#pills-dt-permissionGroupsDataTable-dirs-tab");
    if (enabled) {
        I.checkOption("#DTE_Field_allEditableGroups_0");
        I.checkOption("#DTE_Field_allWritableFolders_0");
    } else {
        I.uncheckOption("#DTE_Field_allEditableGroups_0");
        I.uncheckOption("#DTE_Field_allWritableFolders_0");
    }
    DTE.save();
}

Scenario('test allEditableGroups and allWritableFolders user', ({I, DTE, DT}) => {
    I.relogin("admin");
    setEditableGroups(false, I, DTE);

    I.relogin("tester.allgroups");
    I.say("verify normal permissions without ALL set");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.waitForElement(locate("a.jstree-anchor").withText("Jet portal 4"), 10);
    I.dontSeeElement(locate("a.jstree-anchor").withText("Test stavov"));

    I.amOnPage("/admin/v9/files/index/#elf_iwcm_2_");
    I.waitForElement("#iwcm_2_L2FwcHM_E.elfinder-ro", 10); //apps RO

    I.relogin("admin");
    setEditableGroups(true, I, DTE);

    I.relogin("tester.allgroups");
    I.say("verify permissions with ALL set");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.waitForElement(locate("a.jstree-anchor").withText("Jet portal 4"), 10);
    I.waitForElement(locate("a.jstree-anchor").withText("Test stavov"), 10);

    I.amOnPage("/admin/v9/files/index/#elf_iwcm_2_");
    I.waitForElement("#iwcm_2_L2FwcHM_E:not(.elfinder-ro)", 10); //apps RW
});

Scenario('logout 2', ({ I, DTE }) => {
    I.logout();
    I.relogin("admin");
    setEditableGroups(false, I, DTE);
});