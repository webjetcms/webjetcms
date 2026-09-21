Feature('apps.blog.blog');

const WebjetDteJsTree = require("../../../pages/WebjetDteJsTree");

var randomNumber;
const tree = "#SomStromcek";
const allSections = tree + ' [id="-1_anchor"]';
const section = path => tree + ' a[title="' + path + '"]';

function expandSection(I, path) {
    I.waitForElement(section(path));
    I.clickCss(section(path));
    I.pressKey("ArrowRight");
}

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

    I.clickCss("button.btn-webjet-jstree-item-edit");
    within(WebjetDteJsTree.tree, () => {
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
    expandSection(I, "/Aplikácie/Blog/" + newBlogger);
    I.waitForElement(section("/Aplikácie/Blog/" + newBlogger + "/Nezaradené"));
    I.seeElement(allSections);
    I.seeElement(section("/Aplikácie/Blog/" + newBlogger));
    I.seeElement(section("/Aplikácie/Blog/" + newBlogger + "/Nezaradené"));

    I.clickCss(allSections);
    DT.waitForLoader("bloggerArticlesDataTable");
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

Scenario('Test folder tree permissions', async ({I, DT}) => {
    I.say("Admin must see all bloggers folders");
    I.relogin("tester");
    I.amOnPage("/apps/blog/admin/");

    I.seeElement(allSections);
    I.seeElement(section("/Aplikácie/Blog/blogger"));
    expandSection(I, "/Aplikácie/Blog/blogger");
    I.waitForElement(section("/Aplikácie/Blog/blogger/Nezaradené"));
    I.seeElement(section("/Aplikácie/Blog/bloggerPerm"));
    expandSection(I, "/Aplikácie/Blog/bloggerPerm");
    I.waitForElement(section("/Aplikácie/Blog/bloggerPerm/Nezaradené"));

    const foreignRootId = (await I.grabAttributeFrom(section("/Aplikácie/Blog/blogger"), "id")).replace("_anchor", "");

    I.say("Blogger can see only his folders");
    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/");
    I.seeElement(allSections);
    I.seeElement(section("/Aplikácie/Blog/bloggerPerm"));
    expandSection(I, "/Aplikácie/Blog/bloggerPerm");
    I.waitForElement(section("/Aplikácie/Blog/bloggerPerm/Nezaradené"));
    I.dontSeeElement(section("/Aplikácie/Blog/blogger/Nezaradené"));
    I.fillField("#tree-folder-search-input", "blogger");
    I.clickCss("#tree-folder-search-button");
    I.waitForInvisible("#SomStromcek .jstree-loading");
    I.dontSeeElement(section("/Aplikácie/Blog/blogger"));
    const foreignChildren = await I.executeScript(async foreignRootId => {
        const response = await fetch("/admin/rest/blog/tree", {
            method: "POST", headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken },
            body: JSON.stringify({ id: foreignRootId })
        });
        return (await response.json()).items;
    }, foreignRootId);
    I.assertEmpty(foreignChildren, "A blogger cannot expand another blogger's folder");
    const deniedSection = await I.executeScript(async foreignRootId => {
        const response = await fetch("/admin/rest/blog/sections/editor", {
            method: "POST", headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken },
            body: JSON.stringify({ action: "create", data: { 0: { parentGroupId: Number(foreignRootId), groupName: "forbidden-section-autotest" } } })
        });
        return response.json();
    }, foreignRootId);
    I.assertContain(deniedSection.error, "Na túto akciu nemáte právo.", "A blogger cannot create a section in another blogger's folder");
    I.relogin("tester");
    I.amOnPage("/apps/blog/admin/?removePerm=cmp_blog,cmp_blog_admin");
    const status = await I.executeScript(async () => (await fetch("/admin/rest/blog/tree", {
        method: "POST", headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken },
        body: JSON.stringify({ id: "0" })
    })).status);
    I.assertEqual(status, 403, "The tree endpoint requires Blog permission");
    const sectionStatus = await I.executeScript(async () => (await fetch("/admin/rest/blog/sections/all")).status);
    I.assertEqual(sectionStatus, 403, "The section editor requires Blog permission");
});

Scenario('Test create subgroup logic', async ({I, DTE, DT}) => {
    let subFolder = "section-autotest-" + randomNumber;

    I.say("Check that parent group must be selected");
    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/");

    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-warning");
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-title").withText("Pridanie sekcie") );
    I.waitForElement( locate("#toast-container-webjet > div > div.toast-message").withText("Pred pridaním novej sekcie vyberte nadradený priečinok v strome.") );
    I.toastrClose();

    const modal = "#blogSectionTable_modal";
    I.say("The standard section editor validates the name without closing");
    I.clickCss(section("/Aplikácie/Blog/bloggerPerm"));
    I.seeElement("button.buttons-add-folder.btn-success .ti-plus");
    I.clickCss("button.buttons-add-folder");
    DTE.waitForEditor("blogSectionTable");
    I.waitForFunction(() => document.activeElement.id === "DTE_Field_groupName");
    I.see("Pridanie sekcie", modal + " .modal-title");
    I.seeInField(modal + " #DTE_Field_parentPath", "/Aplikácie/Blog/bloggerPerm");
    I.seeElement(modal + " #DTE_Field_parentPath:disabled");
    DTE.save("blogSectionTable");
    I.seeElement(modal + " .DTE_Field_Name_groupName.is-invalid");

    I.fillField(modal + " #DTE_Field_groupName", "   ");
    DTE.save("blogSectionTable");
    I.seeElement(modal + " .DTE_Field_Name_groupName.is-invalid");

    I.fillField(modal + " #DTE_Field_groupName", "Nezaradené");
    DTE.save("blogSectionTable");
    I.see("Rubrika s takýmto menom už existuje", modal);
    I.seeInField(modal + " #DTE_Field_groupName", "Nezaradené");
    DTE.cancel("blogSectionTable");
    I.waitForFunction(() => document.activeElement.classList.contains("buttons-add-folder"));

    I.say("Cancel preserves the tree; a new dialog starts with an empty name");
    I.clickCss("button.buttons-add-folder");
    DTE.waitForEditor("blogSectionTable");
    I.seeInField(modal + " #DTE_Field_groupName", "");
    I.fillField(modal + " #DTE_Field_groupName", subFolder);
    DTE.save("blogSectionTable", true);
    I.waitForElement("#toast-container-webjet > .toast-success");
    I.see("Pridanie novej sekcie bolo úspešné.", "#toast-container-webjet");
    I.toastrClose();

    I.say("Verify that the tree contains the new section");
    expandSection(I, "/Aplikácie/Blog/bloggerPerm");
    I.waitForElement(section("/Aplikácie/Blog/bloggerPerm/" + subFolder));
    I.seeElement(allSections);
    I.seeElement(section("/Aplikácie/Blog/bloggerPerm"));
    I.seeElement(section("/Aplikácie/Blog/bloggerPerm/" + subFolder));
    expandSection(I, "/Aplikácie/Blog/bloggerPerm");
    I.waitForElement(section("/Aplikácie/Blog/bloggerPerm/Nezaradené"));

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
    I.clickCss("#bloggerArticlesDataTable_wrapper button.buttons-create");
    DTE.waitForEditor("bloggerArticlesDataTable");
    I.clickCss("#pills-dt-bloggerArticlesDataTable-basic-tab");
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input.form-control", "/Aplikácie/Blog/bloggerPerm/Nezaradené");
    DTE.cancel();

    I.say("Select folder");
    I.clickCss(section("/Aplikácie/Blog/bloggerPerm"));

    DT.waitForLoader("bloggerArticlesDataTable");

    I.say("Now create new page");
    I.clickCss("#bloggerArticlesDataTable_wrapper button.buttons-create");
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
    I.clickCss(allSections);
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

Scenario('Folder navigation, search and responsive layout', async ({ I, DT }) => {
    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/#63847");
    I.waitForElement(section("/Aplikácie/Blog/bloggerPerm") + ".jstree-clicked");
    DT.waitForLoader("bloggerArticlesDataTable");
    I.dontSeeElement("#groupSelect");
    I.fillField("#tree-folder-search-input", "Nezaradené");
    I.pressKey("Enter");
    const child = section("/Aplikácie/Blog/bloggerPerm/Nezaradené");
    I.waitForVisible(child);
    I.clickCss(child);
    DT.waitForLoader("bloggerArticlesDataTable");
    const childId = await I.executeScript(() => $("#SomStromcek").jstree(true).get_selected()[0]);
    I.clickCss("#tree-folder-search-clear-button");
    I.waitForElement(child + ".jstree-clicked");
    I.clickCss(".tree-col .buttons-refresh");
    I.waitForElement(child + ".jstree-clicked");
    I.assertEqual(await I.executeScript(() => new URL(bloggerArticlesDataTable.getAjaxUrl(), location.origin).searchParams.get("groupId")), childId);
    I.amOnPage("/apps/blog/admin/#999999999");
    I.waitForElement(allSections + ".jstree-clicked");
    DT.waitForLoader("bloggerArticlesDataTable");
    I.assertEqual(await I.executeScript(() => location.hash), "#-1");

    expandSection(I, "/Aplikácie/Blog/bloggerPerm");
    I.waitForVisible(child);
    I.pressKey("ArrowDown");
    I.pressKey("Enter");
    I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains("jstree-clicked")), "Keyboard navigation selects the focused folder");
    I.assertNotEqual(await I.executeScript(() => getComputedStyle(document.activeElement).outlineStyle), "none");
    DT.waitForLoader("bloggerArticlesDataTable");
    for (const width of [1280, 1100, 640]) {
        I.resizeWindow(width, 800);
        I.waitForFunction(width => {
            const tree = document.querySelector(".tree-col").getBoundingClientRect();
            const table = document.querySelector(".datatable-col").getBoundingClientRect();
            return width < 768 ? table.top >= tree.bottom - 2 : table.left >= tree.right - 2;
        }, [width]);
        I.saveScreenshot("autotest-blog-tree-" + width + ".png");
    }
    I.wjSetDefaultWindowSize();
});

Scenario('An empty domain cannot show unrelated Blog articles', async ({ I, DT, Document }) => {
    I.relogin("tester");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    Document.switchDomain("test23.tau27.iway.sk");
    I.amOnPage("/apps/blog/admin/");
    I.waitForVisible("#blog-folders-empty");
    I.waitForElement("#bloggerArticlesDataTable_wrapper .buttons-create.disabled");
    I.seeElement(".tree-col .buttons-add-folder[disabled]");
    I.dontSeeElement("#SomStromcek .jstree-anchor");
    DT.waitForLoader("bloggerArticlesDataTable");
    DT.filterContains("title", "blogger");
    I.assertEqual(await I.executeScript(() => bloggerArticlesDataTable.page.info().recordsTotal), 0);
    I.logout();
});
