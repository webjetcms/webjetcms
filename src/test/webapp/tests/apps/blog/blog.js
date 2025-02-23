Feature('apps.blog');

var randomNumber;
var sectionSelector = "#groupSelect_wrapper button.dropdown-toggle";

Before(({ I }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Create blogger and test his logic', async ({ I, DT, DTE }) => {
    let newBlogger = "newBlogger" + randomNumber + "_autotest";

    I.say("CREATE NEW BLOGGER");
    I.relogin("tester");
    I.amOnPage("/apps/blog/admin/bloggers/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("bloggerDataTable");

    I.say("Test editor validation");
    DTE.save();
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    I.fillField("#DTE_Field_email", "invalid_email");
    DTE.save();
    I.see("Nesprávna emailová adresa. Zadajte email vo formáte meno@domena.");
    I.fillField("#DTE_Field_email", "bloggerTestInsert@balat.sk");

    I.say("Put already existing login");
    I.fillField("#DTE_Field_login", "tester");
    DTE.save();
    I.see("zadané prihlasovacie meno je už použité, zvoľte iné");
    I.fillField("#DTE_Field_login", newBlogger);

    I.clickCss("button.btn-vue-jstree-item-edit");
    within("div#jsTree", () => {
        I.click(locate('.jstree-node.jstree-closed').withText('Aplikácie').find('.jstree-icon.jstree-ocl'));
        I.click(locate('.jstree-node.jstree-closed').withText('Blog').find('.jstree-anchor'));
    });
    I.fillField("#DTE_Field_firstName", "InsertBlogger");
    I.fillField("#DTE_Field_lastName", "Autotest");
    I.fillField("#DTE_Field_password", secret(I.getDefaultPassword()));

    DTE.save();

    I.waitForInvisible("#bloggerDataTable_modal", 5);
    I.dontSee("Nastala neočakávaná chyba, skúste požiadavku opakovať neskôr.", "div.DTE_Form_Error");

    I.say("Check created blogger structure");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=63517");
    DT.waitForLoader();
    I.jstreeClick(newBlogger);
    I.jstreeClick("Nezaradené");
    I.see("Nezaradené");
    I.see("Váš prvý článok");

    I.say("Check perm - even blog_admin cant see article without perm cmp_blog");
    I.amOnPage("/apps/blog/admin?removePerm=cmp_blog");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
    I.amOnPage("/apps/blog/admin/bloggers/");
    I.dontSee("Na túto aplikáciu/funkciu nemáte prístupové práva");

    I.say("LOG as new blogger - do checks");
    I.relogin(newBlogger);

    I.say("Check perm");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
    I.amOnPage("/apps/blog/admin/bloggers/");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
    I.amOnPage("/apps/forum/admin/");
    I.dontSee("Na túto aplikáciu/funkciu nemáte prístupové práva");

    I.say("Check pages");
    I.amOnPage("/apps/blog/admin/");
    I.see(newBlogger);
    I.see("Nezaradené");
    I.see("Váš prvý článok");

    I.say("Check folders");
    I.clickCss(sectionSelector);
    I.seeElement( locate("a.dropdown-item > span").withText("Všetky sekcie") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/" + newBlogger) );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/" + newBlogger + "/Nezaradené") );

    I.say("Check main functionality");
    I.click("Váš prvý článok");
    DTE.waitForEditor("bloggerArticlesDataTable");
    I.clickCss("#pills-dt-bloggerArticlesDataTable-basic-tab");
    I.fillField("#DTE_Field_title", "Váš prvý článok CHANGE");
    DTE.save();
    DT.filterContains("title", "Váš prvý článok CHANGE");
    I.see("Váš prvý článok CHANGE");

    I.say("Remove blogger Structure");
    I.relogin("tester");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=63517");
    I.jstreeClick(newBlogger);
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.dontSeeElement(locate("a.jstree-anchor").withText(newBlogger));

    I.say("Remove user/blogger from system");
    I.amOnPage("/admin/v9/users/user-list/");
    DT.filterContains("login", newBlogger);
    DT.filterContains("firstName", "InsertBlogger");
    DT.filterContains("lastName", 'Autotest');
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.dontSee(newBlogger);
});

Scenario('Test group select logic', async ({I}) => {
    I.say("Admin must see all bloggers folders");
    I.relogin("tester");
    I.amOnPage("/apps/blog/admin/");

    I.clickCss(sectionSelector);
    I.seeElement( locate("a.dropdown-item > span").withText("Všetky sekcie") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/blogger") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/blogger/Nezaradené") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm/Nezaradené") );

    I.say("Blogger can see only his folders");
    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/");
    I.clickCss(sectionSelector);
    I.seeElement( locate("a.dropdown-item > span").withText("Všetky sekcie") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm/Nezaradené") );
    I.dontSeeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/blogger/Nezaradené") );
});

Scenario('Test create subgroup logic', async ({I, DTE, DT}) => {
    let subFolder = "section-" + randomNumber;

    I.say("Check that parent group must be selected");
    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/");

    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-warning");
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-title").withText("Pridanie sekcie") );
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-message").withText("Pred pridaním novej sekcie, musí byť zvolená sekcia vo výberovom poli.") );
    I.toastrClose();

    I.say("Check, that name of new subgroup must be set (or error toad)");
    I.clickCss(sectionSelector);
    I.click( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm") );
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-info");
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-title").withText("Pridanie sekcie") );
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-message > div.toastr-message").withText("Zadajte názov novej sekcie, ktorá sa má vytvoriť v priečinku: /Aplikácie/Blog/bloggerPerm") );

    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.waitForElement("#toast-container-webjet > .toast-error");
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-title").withText("Pridanie sekcie") );
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-message").withText("Pridanie novej sekcie bolo neúspešné.") );
    I.toastrClose();

    I.say("Add subfolder and test it");
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-info");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", subFolder);
    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.waitForElement("#toast-container-webjet > .toast-success");
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-message").withText("Pridanie novej sekcie bolo úspešné.") );
    I.toastrClose();

    I.say("Verify that folder select was updated");
    I.clickCss(sectionSelector);
    I.seeElement( locate("a.dropdown-item > span").withText("Všetky sekcie") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm") );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm/" + subFolder) );
    I.seeElement( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm/Nezaradené") );

    I.relogin("tester");

    I.say("Check created sub folder");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=63847");
    I.jstreeClick(subFolder);

    I.say("Delete sub folder");
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.dontSeeElement(locate("a.jstree-anchor").withText(subFolder));
});

Scenario('Test webpage logic', ({I, DT, DTE}) => {
    let newPageName = "newbloggerPage" + randomNumber + "_autotest";

    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/");
    DT.waitForLoader();

    I.say("Test, that we can add webpage without selected folder");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("bloggerArticlesDataTable");
    I.wait(3);
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input.form-control", "/Aplikácie/Blog/bloggerPerm/Nezaradené");
    DTE.cancel();

    I.say("Select folder");
    I.clickCss(sectionSelector);
    I.click( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm") );

    //Too fast for DTE, wait for a while
    I.say("Wait for a second.");
    I.wait(1);

    I.say("Now create new page");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("bloggerArticlesDataTable");
    I.clickCss("#pills-dt-bloggerArticlesDataTable-basic-tab");
    I.fillField("#DTE_Field_title", newPageName);

    I.say("I cant see folder tree settings");
    I.dontSeeElement("#panel-body-dt-bloggerArticlesDataTable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_json.DTE_Field_Name_editorFields\.groupDetails.dt-style-json.dt-tree-group");
    I.dontSeeElement("#panel-body-dt-bloggerArticlesDataTable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_json.DTE_Field_Name_editorFields\.groupCopyDetails.dt-style-json.dt-tree-group-array");
    DTE.save();

    I.say("Check as blogger, that page was created");
    I.see(newPageName);

    I.say("Try page edit");
    I.click(newPageName);
    DTE.waitForEditor("bloggerArticlesDataTable");
    I.clickCss("#pills-dt-bloggerArticlesDataTable-basic-tab");
    I.fillField("#DTE_Field_title", "edit_" + newPageName);
    DTE.save();
    I.see("edit_" + newPageName);

    I.say("Try page delete");
    DT.filterContains("title", "edit_" + newPageName);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.dontSee("edit_" + newPageName);

    I.say("Go to all folder and verify that page was deleted");
    I.clickCss(sectionSelector);
    I.click( locate("a.dropdown-item > span").withText("Všetky sekcie") );
    I.dontSee(newPageName);
    I.dontSee("edit_" + newPageName);
});

Scenario('Otestuj Diskusiu pre Blogera', ({I, DT, DTE}) => {
    I.relogin("blogger");
    I.amOnPage("/apps/forum/admin/");
    I.dontSee("Na túto aplikáciu/funkciu nemáte prístupové práva");

    DT.filterContains("subject", "Blogger");
    I.click("Blogger");
    DTE.waitForEditor("forumDataTable");
    I.see("Titulok", "div.DTE_Field_Name_subject")
    I.seeInField("#DTE_Field_subject", "Blogger");
    DTE.cancel();
});