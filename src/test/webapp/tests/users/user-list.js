Feature('users.user-list');

var randomText = null;
var url = "/admin/v9/users/user-list/";
var permEditAdminUser = "users.edit_admins";
var permEditPublicUser = "users.edit_public_users";
const DUPLICATE_TEXT = "-dupli.cate";

Before(({ I, login }) => {
     login('admin');
     if (randomText == null) {
          randomText = I.getRandomTextShort();
          I.say("randomText: "+randomText);
     }
     I.amOnPage("/admin/v9/users/user-list/");
});

Scenario('user-list-zakladne testy @baseTest', async ({ I, DataTables, DTE }) => {
     await DataTables.baseTest({
          dataTable: 'usersDatatable',
          perms: 'menuUsers',
          createSteps: function(I, options) {
               DTE.save();
               I.see("Zadané heslo nespĺňa bezpečnostné nastavenia aplikácie", "div.DTE_Field_Name_password");
               I.fillField("#DTE_Field_password", "password");
               I.waitForText("Toto je veľmi často používané heslo.", 10, "div.DTE_Field_Name_password");
               I.fillField("#DTE_Field_password", "Heslo"+randomText);

               //tieto polia musia byt zadane a nezbehne ich autodetekcia, kedze su len v editore
               I.see("Povinné pole. Zadajte aspoň jeden znak.", "div.DTE_Field_Name_editorFields\\.login")
               I.fillField("#DTE_Field_editorFields-login", "login-"+randomText);
          },
          duplicateSteps: function(I, options) {
               I.fillField('#DTE_Field_editorFields-login', "login-"+randomText+DUPLICATE_TEXT);
           },
          skipSwitchDomain: true
     });
});

Scenario('user-list-email-validacia', ({ I, DTE }) => {
     I.click("button.buttons-create");
     DTE.waitForEditor();
     I.fillField("#DTE_Field_email", "zly.email@balat.sk. ");
     DTE.save();
     I.see("Nesprávna emailová adresa.", ".DTE_Field_Name_email");
});

/**
 * Existoval bug, kedy padalo na NPE ked sa zmazali nastavene skupiny prav
 */
Scenario('user-list-test pridania a odobratia skupin prav @baseTest', async ({ I, DataTables }) => {
     randomText = I.getRandomTextShort();

     await DataTables.baseTest({
          dataTable: 'usersDatatable',
          perms: 'menuUsers',
          createSteps: function(I, options) {
               //tieto polia musia byt zadane a nezbehne ich autodetekcia, kedze su len v editore
               I.fillField("#DTE_Field_editorFields-login", "login-"+randomText);
               //I.fillField("#DTE_Field_email", randomText+"@balat.sk");
               I.fillField("#DTE_Field_password", "Heslo"+randomText);

               I.click("#pills-dt-datatableInit-rightsTab-tab");
               I.click("Áno", "div.DTE_Field_Name_admin");
               I.forceClick("forTestA");
               I.forceClick("forTestB");
               I.seeElement("#DTE_Field_editorFields-permGroups_1:checked");
               I.seeElement("#DTE_Field_editorFields-permGroups_2:checked");
               I.click("#pills-dt-datatableInit-personalInfo-tab");
          },
          editSteps: function(I, options) {
               I.click("#pills-dt-datatableInit-rightsTab-tab");
               I.seeElement("#DTE_Field_editorFields-permGroups_1:checked");
               I.seeElement("#DTE_Field_editorFields-permGroups_2:checked");
               I.forceClick("forTestA");
               I.forceClick("forTestB");
               I.dontSeeElement("#DTE_Field_editorFields-permGroups_1:checked");
               I.dontSeeElement("#DTE_Field_editorFields-permGroups_2:checked");
               I.click("#pills-dt-datatableInit-personalInfo-tab");
          },
          duplicateSteps: function(I, options) {
               I.fillField('#DTE_Field_editorFields-login', "login-"+randomText+DUPLICATE_TEXT);
          },
          skipSwitchDomain: true
     });
});

Scenario('test-prava-admin-user', ({ I }) => {

     I.fillField({css: "input.dt-filter-lastName"}, "Admin");
     I.pressKey('Enter', "input.dt-filter-key");
     I.see("Admin", "div.dataTables_scrollBody tbody");
     I.wait(1);

     I.amOnPage(url + "?removePerm=" + permEditAdminUser);

     I.fillField({css: "input.dt-filter-lastName"}, "Admin");
     I.pressKey('Enter', "input.dt-filter-key");
     I.dontSee("Admin", "div.dataTables_scrollBody tbody");

     I.logout();
 });

 Scenario('test-prava-public-user', ({ I }) => {

     I.fillField({css: "input.dt-filter-lastName"}, "Balážová");
     I.pressKey('Enter', "input.dt-filter-key");
     I.see("Balážová", "div.dataTables_scrollBody tbody");
     I.wait(1);

     I.amOnPage(url + "?removePerm=" + permEditPublicUser);

     I.fillField({css: "input.dt-filter-lastName"}, "Balážová");
     I.pressKey('Enter', "input.dt-filter-key");
     I.dontSee("Balážová", "div.dataTables_scrollBody tbody");

     I.logout();
 });

 Scenario('test-prava-admin-and-public-user', ({ I }) => {

     I.fillField({css: "input.dt-filter-lastName"}, "Admin");
     I.pressKey('Enter', "input.dt-filter-key");
     I.see("Admin", "div.dataTables_scrollBody tbody");
     I.wait(1);

     I.fillField({css: "input.dt-filter-lastName"}, "Balážová");
     I.pressKey('Enter', "input.dt-filter-key");
     I.see("Balážová", "div.dataTables_scrollBody tbody");
     I.wait(1);

     I.amOnPage(url + "?removePerm=" + permEditAdminUser + "," + permEditPublicUser);

     I.fillField({css: "input.dt-filter-lastName"}, "Admin");
     I.pressKey('Enter', "input.dt-filter-key");
     I.dontSee("Admin", "div.dataTables_scrollBody tbody");

     I.fillField({css: "input.dt-filter-lastName"}, "Balážová");
     I.pressKey('Enter', "input.dt-filter-key");
     I.dontSee("Balážová", "div.dataTables_scrollBody tbody");

     I.logout();
 });


 Scenario('test-zmeny-pristupu-usera-do-admin-sekcie', ({ I, DTE }) => {

     I.amOnPage(url + "?removePerm=" + permEditAdminUser);

     I.fillField({css: "input.dt-filter-lastName"}, "Balážová");
     I.pressKey('Enter', "input.dt-filter-key");
     I.see("Balážová", "div.dataTables_scrollBody tbody");
     I.wait(1);

     I.click("td.dt-select-td.sorting_1");
     I.click("button.buttons-edit");
     DTE.waitForEditor();

     //I.click("#pills-dt-datatableInit-rightsTab-tab");
     I.forceClick('#DTE_Field_admin_0');
     DTE.save();
     I.see("Nemáte právo na túto editáciu");

     I.logout();
 });

 Scenario('users-list-import', async ({ I, DataTables }) => {
     await DataTables.importTest({
          dataTable: 'usersDatatable',
          file: 'tests/users/user-list.xlsx',
          updateBy: 'Prihlasovacie meno - editorFields.login',
          rows: [
               {
                    login: "login-test-import"
               }
          ],
          preserveColumns: [
               'title',
               'deliveryFirstName',
               'deliveryLastName'
          ]
     });
});


Scenario('filter by user group', ({ I, DT }) => {
     DT.resetTable();
     DT.showColumn("Prístupové práva k neverejným sekciám web sídla");

     DT.filter("editorFields.permisions", "Bankári");
     I.see("Bankári, Obchodní partneri");

     //
     I.say("Filter equals show only users with this one user group");
     DT.filter("editorFields.permisions", "Bankári", "Rovná sa");
     I.dontSee("Bankári, Obchodní partneri");

     DT.resetTable();
});

Scenario("approve user - show user by id in DT", ({I}) => {
     var url="/admin/v9/users/user-list/#dt-filter-id=3";
     I.logout();
     I.amOnPage(url);
     I.relogin("admin", false);

     I.seeInCurrentUrl(url);
     I.waitForText("Záznamy 1 až 1 z 1", 10, "div.dataTables_info");
     I.see("vipklient", "#datatableInit");
     I.dontSee("arnoldschwarzenegger", "#datatableInit");

     url = url + "&dt-select=true";

     I.logout();
     I.amOnPage(url);
     I.relogin("admin", false);

     I.seeInCurrentUrl(url);
     I.waitForText("Záznamy 1 až 1 z 1", 10);
     I.see("vipklient", "#datatableInit");
     I.dontSee("arnoldschwarzenegger", "#datatableInit");
     I.see("1 riadok označený", "div.dataTables_info");
});

Scenario("BUG set user perms from first page", ({I, DTE}) => {

     I.amOnPage("/admin/v9/users/user-list/?id=1");

     //
     I.say("Checking clicked perms, it was empty before fix")
     DTE.waitForEditor();
     I.click("#pills-dt-datatableInit-rightsTab-tab");
     I.seeElement("#perms_cmp_adminlog a.jstree-clicked");

     DTE.cancel();

     //
     I.say("open another user, check perms are different");
     I.click("sylvesterstallone");
     DTE.waitForEditor();
     I.click("#pills-dt-datatableInit-rightsTab-tab");
     I.dontSeeElement("#perms_cmp_adminlog a.jstree-clicked");

     DTE.cancel();

});

Scenario("BUG permgroups icons not shown", ({I, DTE}) => {
     I.amOnPage("/admin/v9/users/user-list/?id=1");
     DTE.waitForEditor();
     I.click("#pills-dt-datatableInit-rightsTab-tab");
     I.seeElement("li#perms_cmp_adminlog span.taggroup span.tag.permgroup-62");
     I.dontSeeElement("li#perms_cmp_adminlog_logging span.taggroup span.tag.permgroup-63");

     DTE.cancel();
});

function userEditDialog(I, login, isSelf) {
     if ("tester"!==login) I.relogin(login);

     I.clickCss("button.js-profile-toggler");
     I.click("Profil", "a.dropdown-item");
     I.waitForElement("#modalIframe");

     I.switchTo("#modalIframeIframeElement");

     I.waitForText(login, 15, "#datatableInit_modal div.DTE_Header_Content h5");
     I.waitForText("E-mailová adresa", 10);

     //
     I.say("Check tabs");
     I.seeElement("#pills-dt-datatableInit-personalInfo-tab");
     I.seeElement("#pills-dt-datatableInit-contactTab-tab");
     if (isSelf) {
          I.dontSeeElement("#pills-dt-datatableInit-freeItems-tab");
          I.dontSeeElement("#pills-dt-datatableInit-groupsTab-tab");
          I.dontSeeElement("#pills-dt-datatableInit-rightsTab-tab");
          I.dontSeeElement("#pills-dt-datatableInit-approvingTab-tab");
     } else {
          I.seeElement("#pills-dt-datatableInit-freeItems-tab");
          I.seeElement("#pills-dt-datatableInit-groupsTab-tab");
          I.seeElement("#pills-dt-datatableInit-rightsTab-tab");
          I.seeElement("#pills-dt-datatableInit-approvingTab-tab");
     }

     //
     I.say("Check fields")
     if (isSelf) {
          I.dontSee("Schválený používateľ");
          I.dontSee("Začiatok platnosti");
          I.dontSeeElement("#DTE_Field_login");
          I.dontSeeElement("#DTE_Field_editorFields-login");
          I.dontSeeElement("#DTE_Field_authorized_0");
          I.dontSeeElement("#DTE_Field_allowLoginStart");
          I.dontSeeElement("#DTE_Field_allowLoginEnd");
     } else {
          I.see("Schválený používateľ");
          I.see("Začiatok platnosti");

          I.seeElement("#DTE_Field_authorized_0");
          I.seeElement("#DTE_Field_allowLoginStart");
          I.seeElement("#DTE_Field_allowLoginEnd");
     }

     //
     I.say("Fill data");
     var address = "autotest-address-"+randomText;
     I.click("#pills-dt-datatableInit-contactTab-tab");
     I.fillField("#DTE_Field_address", address);

     I.switchTo();
     I.click("#modalIframe div.modal-footer button.btn-primary");

     I.waitForText("Profil bol aktualizovaný", 20, ".toast-message");
     I.dontSeeElement("#modalIframe");
     I.toastrClose();

     //
     I.say("Check data");

     I.clickCss("button.js-profile-toggler");
     I.click("Profil", "a.dropdown-item");
     I.waitForElement("#modalIframe");

     I.switchTo("#modalIframeIframeElement");

     I.waitForText(login, 15, "#datatableInit_modal div.DTE_Header_Content h5");
     I.waitForText("E-mailová adresa", 10);

     I.click("#pills-dt-datatableInit-contactTab-tab");
     I.seeInField("#DTE_Field_address", address);

     I.switchTo();
     I.click("#modalIframe div.modal-header button.btn-close");
}

Scenario("Useredit-dialog", ({I}) => {

     I.say("Checking standard user with all perms");
     userEditDialog(I, "tester", false);

     I.say("Checking user without useredit perms");
     userEditDialog(I, "tester2", true);

});

Scenario("logout", ({I}) => {
     I.logout();
});

Scenario("New select filter using permGroups", ({I, DT}) => {

     DT.showColumn("Skupiny práv");

     DT.filter("login", "admin");

     DT.filterSelect("editorFields.permGroups", "Admini");

     I.see("Admini", "div.datatable-column-width");
     I.dontSee("forTestA", "div.datatable-column-width");
     I.dontSee("forTestB", "div.datatable-column-width");

     DT.filter("login", "tester");
     I.see("tester", "td.dt-row-edit");
     I.see("tester3", "td.dt-row-edit");
     I.dontSee("tester2", "td.dt-row-edit");

     DT.filterSelect("editorFields.permGroups", "forTestA");

     I.see("Nenašli sa žiadne vyhovujúce záznamy", "div.dataTables_scrollBody");

     DT.filter("login", "testLogin");

     I.see("testLogin", "td.dt-row-edit");
     I.see("forTestA, forTestB", "div.datatable-column-width");

     DT.resetTable();
});

Scenario('Test self_delete error', ({ I, DT, DTE}) => {
     I.relogin("tester_self_delete");
     I.amOnPage("/admin/v9/users/user-list/");
     DT.filter("login", "tester_self_delete");
     I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

     I.say("try delete myself");
     I.click("button.dt-filter-id");
     I.click("button.btn.btn-sm.buttons-selected.buttons-remove");
     DTE.waitForEditor();
     I.click("Zmazať", "div.DTE_Action_Remove");
     I.see("Nemôžete vymazať používateľa, s ktorým ste aktuálne prihlásený.", "div.DTE_Form_Error");
     DTE.cancel();
});

/* XLSX si prepare so it's trigger both error handling in V2-initBinder and V2-handleEditor */

Scenario('Test import incorrect data', ({ I, DT}) => {
     let fileName = 'tests/users/wrong-empty-data-user-list.xlsx';

     I.relogin("tester");
     I.amOnPage("/admin/v9/users/user-list/");

     checkWrongEmails(I, DT);

     I.say("IF SkipWrong IS NOT checked, import will fail");
     insertFile(I, fileName, false);
     I.waitForText("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");
     I.waitForText("email - Nesprávna emailová adresa. Zadajte email vo formáte meno@domena.");
     I.clickCss("#datatableImportModal > div > div > div.modal-footer > button.btn-outline-secondary");

     checkWrongEmails(I, DT);

     I.say("IF SkipWrong IS checked, import will NOT fail");
     insertFile(I, fileName, true);
     I.dontSee("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");
     I.dontSee("email - Nesprávna emailová adresa. Zadajte email vo formáte meno@domena.");
     I.waitForInvisible("#datatableImportModal");

     I.say("CHECK returned error");
     I.waitForElement("#toast-container-webjet");
     I.seeElement( locate("div.toast-title").withText("Chyba") );
     I.seeElement( locate("div.toast-message").withText("Riadok 1: firstName - EMPTY - Povinné pole. Zadajte aspoň jeden znak.") );
     I.seeElement( locate("div.toast-message").withText("Riadok 2: email - testIsnai123@interway - Nesprávna emailová adresa. Zadajte email vo formáte meno@domena.") );
     I.seeElement( locate("div.toast-message").withText("Riadok 3: email - tes125*$as. - Nesprávna emailová adresa. Zadajte email vo formáte meno@domena.") );
     I.seeElement( locate("div.toast-message").withText("Riadok 4: email -  - Povinné pole. Zadajte aspoň jeden znak.") );

     checkWrongEmails(I, DT);
});

function checkWrongEmails(I, DT) {
     I.say("Checking that wrong emails are NOT here");
     DT.filter("login", "testWrongMail");
     I.see("Nenašli sa žiadne vyhovujúce záznamy");
}

function insertFile(I, fileName, skipWrong) {
     I.clickCss("button.btn-import-dialog");
     I.waitForElement("#datatableImportModal");

     I.wait(1);
     I.attachFile('#insert-file', fileName);
     I.waitForEnabled("#submit-import", 5);

     if(skipWrong === true) {
          I.checkOption( locate("#datatableImportModal").find("#skip-wrong-data") );
     } else {
          I.uncheckOption( locate("#datatableImportModal").find("#skip-wrong-data") );
     }

     I.click("#submit-import");
}

Scenario("logout 2", ({I}) => {
     I.logout();
});

Scenario('user-list-multiweb testy @singlethread @baseTest', async ({ I, DataTables, DT, DTE, Document }) => {
     //test users in MultiWeb configuration - split by domains

     Document.setConfigValue("usersSplitByDomain", "true");
     I.amOnPage("/admin/v9/users/user-list/");
     Document.switchDomain("test23.tau27.iway.sk");
     DT.waitForLoader();
     I.dontSee("arnoldschwarzenegger", "div.dataTables_scrollBody tbody");
     DT.filter("login", "admin");
     I.waitForText("Záznamy 0 až 0 z 0", 10, "div.dataTables_info");
     I.amOnPage("/admin/v9/users/user-list/");

     await DataTables.baseTest({
          dataTable: 'usersDatatable',
          perms: 'menuUsers',
          createSteps: function(I, options) {
               DTE.save();
               I.see("Zadané heslo nespĺňa bezpečnostné nastavenia aplikácie", "div.DTE_Field_Name_password");
               I.fillField("#DTE_Field_password", "password");
               I.waitForText("Toto je veľmi často používané heslo.", 10, "div.DTE_Field_Name_password");
               I.fillField("#DTE_Field_password", "Heslo"+randomText);

               //tieto polia musia byt zadane a nezbehne ich autodetekcia, kedze su len v editore
               I.see("Povinné pole. Zadajte aspoň jeden znak.", "div.DTE_Field_Name_editorFields\\.login")
               I.fillField("#DTE_Field_editorFields-login", "login-"+randomText);
          },
          duplicateSteps: function(I, options) {
               I.fillField('#DTE_Field_editorFields-login', "login-"+randomText+DUPLICATE_TEXT);
          }
     });
     I.relogin("admin");
     Document.setConfigValue("usersSplitByDomain", "false");
});

Scenario("reset wjVersion @singlethread", ({Document}) => {
     Document.setConfigValue("usersSplitByDomain", "false");
});


//TODO: dalsie testy, overenie jednotlivych kariet, overenie prihlasenia vytvoreneho pouzivatela

//TODO: test vnorenej datatabulky schvalovania