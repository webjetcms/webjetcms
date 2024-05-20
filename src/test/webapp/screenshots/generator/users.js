Feature('users');

var url = "/admin/v9/users/user-list/";
var permEditAdminUser = "users.edit_admins";
var permEditPublicUser = "users.edit_public_users";

Before(({ I, login }) => {
    login('admin');
});

Scenario('users', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/users/user-list");

    DT.waitForLoader();

    //TODO - need to open, close and then reopen -> problem with loading icons of permission in first open 
    I.clickCss("button.buttons-create");
    DTE.waitForEditor();
    DTE.cancel();
    I.clickCss("button.buttons-create");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-rightsTab-tab");
    I.checkOption("#DTE_Field_admin_0");

    I.scrollTo("#perms_welcome-leaf_anchor");
    I.pressKey("ArrowUp");
    I.click("label[for=DTE_Field_editorFields-permGroups_3]");
    Document.screenshot("/developer/datatables-editor/field-type-jstree.png", 1280, 700);

    I.click("#pills-dt-datatableInit-personalInfo-tab");
    I.scrollTo("#DTE_Field_password");
    I.fillField("#DTE_Field_password", "admin");
    I.executeScript(function() {
        $('div.DTE_Field_Name_password').css("padding-top", "10px");
        $('div.DTE_Field_Name_password').css("padding-bottom", "10px");
    });
    Document.screenshotElement("div.DTE_Field_Name_password", "/developer/libraries/password-strength.png");
});

Scenario('password-strength-login', ({ I, Document }) => {
    I.amOnPage('/logoff.do?forward=/admin/');

    let language = I.getConfLng();
    if("sk" !== language) {
        if("en" == language) { 
            I.selectOption("language", "English");
        }
    }

    I.fillField("#username", "admin");
    I.fillField("#password", "admin");
    I.wait(1);

    Document.screenshotElement("div.content", "/_media/changelog/2021q2/2021-26-password-strength.png");
});

Scenario('users-docs-screens', ({ I, Document }) => {
    I.amOnPage("/admin/v9/users/user-list");

    //Users data table
    Document.screenshot("/admin/users/users-dataTable.png");

    //Tab personal info
    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/admin/users/users-tab-personalInfo.png");

    //Tab contact
    I.click("#pills-dt-datatableInit-contactTab-tab");
    Document.screenshotElement("div.DTE_Action_Create", "/admin/users/users-tab-contact.png");

    //Tab groups
    I.click("#pills-dt-datatableInit-groupsTab-tab");
    Document.screenshotElement("div.DTE_Action_Create", "/admin/users/users-tab-groups.png");

    //Tab rights - create new user (without admin section)
    I.click("#pills-dt-datatableInit-rightsTab-tab");
    Document.screenshotElement("div.DTE_Action_Create", "/admin/users/users-tab-right-without-admin-section.png");
    I.click("button.btn-close-editor", "div.DTE_Footer");
    I.wait(1);

    //Tab rights - edit admin user (with admin section)
    I.fillField({css: "input.dt-filter-lastName"}, "Admin");
    I.pressKey('Enter', "input.dt-filter-key");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-edit");
    I.wait(1);

    I.click("#pills-dt-datatableInit-rightsTab-tab"); //rights with admin section
    Document.screenshotElement("div.modal.show div.DTE_Action_Edit", "/admin/users/users-tab-right-with-admin-section.png");

    //Saving admin - withou perms - error
    I.amOnPage(url + "?removePerm=" + permEditAdminUser);

    I.fillField({css: "input.dt-filter-lastName"}, "Balážová");
    I.pressKey('Enter', "input.dt-filter-key");
    I.see("Balážová");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-edit");
    I.wait(1);

    Document.screenshotElement("div.DTE_Header", "/admin/users/users-tab-right-hidden-tab.png");

    I.amOnPage('/logoff.do?forward=/admin/');
});

Scenario('users-docs-screens-adminuser', ({ I , DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/users/user-list");
    DT.filter("login", "admin");
    I.click("admin");
    DTE.waitForEditor();

    //Tab free items
    I.click("#pills-dt-datatableInit-freeItems-tab");
    Document.screenshotElement("div.modal.show div.DTE_Action_Edit", "/admin/users/users-tab-freeItems.png");

    //Tab approving
    I.click("#pills-dt-datatableInit-approvingTab-tab");
    Document.screenshotElement("div.modal.show div.DTE_Action_Edit", "/admin/users/users-tab-approving.png");
});

Scenario('users-docs-existing-user', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/users/user-list");
    I.click("dwaynejohnson");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-rightsTab-tab"); //rights with admin section
    I.checkOption("#DTE_Field_admin_0");
    I.pressKey("ArrowDown");
    I.pressKey("ArrowDown");
    I.pressKey("ArrowDown");
    Document.screenshotElement("div.modal.show div.DTE_Action_Edit", "/admin/users/users-tab-right-existing.png");
});

Scenario('users-docs-permsfilter', ({ I , DTE, Document}) => {
    I.amOnPage("/admin/v9/users/user-list");
    I.click({css: "button.buttons-create"});
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-rightsTab-tab"); //rights with admin section
    I.checkOption("#DTE_Field_admin_0");

    I.scrollTo("#perms_welcome-leaf_anchor");
    I.pressKey("ArrowDown");
    I.pressKey("ArrowDown");
    I.pressKey("ArrowDown");

    if("sk" === I.getConfLng()) {
        I.fillField("div.DTE_Field_Type_jsTree div.input-group input.form-control", "adresár");
    } else if("en" === I.getConfLng()) { 
        I.fillField("div.DTE_Field_Type_jsTree div.input-group input.form-control", "director");
    }

    I.click("div.DTE_Field_Type_jsTree div.input-group button.btn-search")
    Document.screenshotElement("div.DTE_Action_Create", "/admin/users/users-tab-right-search.png");
});