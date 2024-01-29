Feature('users.user-groups');

Before(({ I, login }) => {
     login('admin');
     I.amOnPage("/admin/v9/users/user-groups/");
});

Scenario('user-groups-zakladne testy', async ({ I, DataTables }) => {
     await DataTables.baseTest({
          dataTable: 'userGroupsDataTable',
          perms: 'user.admin.userGroups',
     });
});

Scenario('vybrany email docid', async ({ I, DataTables }) => {
     await DataTables.baseTest({
          dataTable: 'userGroupsDataTable',
          perms: 'user.admin.userGroups',
          createSteps: function(I, options) {
               //vyber stranku
               I.click("div.DTE_Field_Name_emailDoc button.btn-vue-jstree-item-edit");
               I.click(locate('.jstree-node.jstree-closed').withText("Jet portal 4").find('.jstree-icon.jstree-ocl')); //rozklikne adresar
               I.wait(1);
               I.click("Jet portal 4 - testovacia stranka");
               I.wait(1);
               I.seeInField("div.DTE_Field_Name_emailDoc input", "/Jet portal 4/Jet portal 4 - testovacia stranka");
          },
          editSteps: function(I, options) {
               //zmaz stranku
               I.click("div.DTE_Field_Name_emailDoc button.btn-vue-jstree-item-remove");
               I.seeInField("div.DTE_Field_Name_emailDoc input", "");
          }
     });
});

Scenario('test user tabu a linky na usera', ({ I, DT, DTE }) => {

     I.amOnPage("/admin/v9/users/user-groups/");
     DT.waitForLoader();
     DT.filter("userGroupName", "VIP Klienti");

     I.click("VIP Klienti");
     DTE.waitForEditor('userGroupsDataTable');

     I.click("#pills-dt-userGroupsDataTable-users-tab");
     I.see("vipklient")

     I.click("vipklient");
     I.wait(3);

     I.see("VIP");
     I.see("Klient");
});

Scenario('test karty adresare', ({ I, DT, DTE }) => {
     I.amOnPage("/admin/v9/users/user-groups/");
     DT.waitForLoader();
     I.click("Newsletter");
     DTE.waitForEditor("userGroupsDataTable");

     I.click("#pills-dt-userGroupsDataTable-folders-tab");
     DT.waitForLoader();
     I.see("Newsletter", ".DTE_Field_Name_groupDetailsList");
     I.see("Zaheslovaný", ".DTE_Field_Name_groupDetailsList");
     //tento je v domene test23
     I.dontSee("test23-newsletter", ".DTE_Field_Name_groupDetailsList");
     I.dontSee("Jet portal 4", ".DTE_Field_Name_groupDetailsList");

     DT.filter("groupName", "News");
     I.see("Newsletter", ".DTE_Field_Name_groupDetailsList");
     I.dontSee("Zaheslovaný", ".DTE_Field_Name_groupDetailsList");
});

Scenario('test karty web stranky', ({ I, DT, DTE }) => {
     I.amOnPage("/admin/v9/users/user-groups/");
     DT.waitForLoader();
     I.click("Newsletter");
     DTE.waitForEditor("userGroupsDataTable");

     I.click("#pills-dt-userGroupsDataTable-sites-tab");
     DT.waitForLoader();

     I.see("Testovaci newsletter", ".DTE_Field_Name_docDetailsList");
     I.see("Produktová stránka - B verzia", ".DTE_Field_Name_docDetailsList");
     I.dontSee("Jet portal 4 - testovacia stranka", ".DTE_Field_Name_docDetailsList");

     DT.filter("title", "Test");
     I.see("Testovaci newsletter", ".DTE_Field_Name_docDetailsList");
     I.dontSee("Produktová stránka - B verzia", ".DTE_Field_Name_docDetailsList");
});

Scenario('test karty web pouzivatelia', ({ I, DT, DTE }) => {
     I.amOnPage("/admin/v9/users/user-groups/");
     DT.waitForLoader();
     I.click("Newsletter");
     DTE.waitForEditor("userGroupsDataTable");

     I.click("#pills-dt-userGroupsDataTable-users-tab");
     DT.waitForLoader();

     I.see("vipklient@balat.sk", ".DTE_Field_Name_usersList");
     I.see("prihlasovaciemeno", ".DTE_Field_Name_usersList");
     I.dontSee("partner", ".DTE_Field_Name_usersList");

     DT.filter("login", "VIP");
     I.see("vipklient@balat.sk", ".DTE_Field_Name_usersList");
     I.dontSee("prihlasovaciemeno", ".DTE_Field_Name_usersList");
});