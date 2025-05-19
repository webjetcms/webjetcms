Feature('webpages.clone-structure');

var randomNumber;

//CLASSIC cloning test variables
var srcGroupName = "clone-src-autotest-";
var srcGroupChildName = "Subfolder-autotest";
var destGroupName = "clone-dest-autotest-";
var newDocName = "New page-autotest-";
var doc_a_child_sk = '<p> auto strom </p>';
var doc_a_child_sk_perex = 'Toto je perex.';
var doc_b_sk = '<p> nový starý najstarší </p>';
var doc_b_sk_perex = 'Aj toto je perex.';

var fake_include = "!INCLUDE(Falošná aplikácia, vložená aplikácia sa nemôže prekladať)!";
var fake_include_html = "<p> Text pred " + fake_include + " text po</p>";

//NO TRANSLATE URL cloning test variables
var srcGroupName_noUrlTranslate = "clone-src-noUrlTranslate-autotest-";
var srcGroupChildName_noUrlTranslate = "Pod-priečinok";
var srcGroupChildName_virtualPath = "ppriecinok";
var destGroupName_noUrlTranslate = "clone-dest-noUrlTranslate-autotest-";
var destGroupChildName_noUrlTranslate = "Sub-folder";

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        srcGroupName += randomNumber;
        destGroupName += randomNumber;
        srcGroupName_noUrlTranslate += randomNumber;
        destGroupName_noUrlTranslate += randomNumber;
    }
});

function createGroup(I, DTE, DT, groupName, language, isRootGroup) {
    I.waitForElement(DT.btn.tree_add_button, 10);
    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor("groups-datatable");
    I.fillField("#DTE_Field_groupName", groupName);

    if (srcGroupChildName_noUrlTranslate==groupName) {
        I.fillField("#DTE_Field_urlDirName", srcGroupChildName_virtualPath);
    }

    if(isRootGroup) {
        I.groupSetRootParent();
        I.clickCss("#pills-dt-groups-datatable-template-tab");
        DTE.selectOption("lng", language);
    } else {
        I.click("#pills-dt-groups-datatable-fields-tab");
        I.fillField("#DTE_Field_fieldA", "field_a_value");
        I.fillField("#DTE_Field_fieldB", "field_b_value");
        I.fillField("#DTE_Field_fieldC", "field_c_value");
        I.fillField("#DTE_Field_fieldD", "field_d_value");
    }

    DTE.save();
}

async function fillDocBody(I, DTE, DT, body, perex) {
    I.clickCss("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dt-scroll > div.dt-scroll-head > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(DT.btn.edit_button);

    I.say("Filling perex");
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.fillField("#DTE_Field_htmlData", perex);

    I.say("Filling body");
    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(body);
    DTE.save();
}

async function editDoc(I, DT, DTE, title) {
    DT.filterContains("title", title);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.clickCss("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dt-scroll > div.dt-scroll-head > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(DT.btn.edit_button);
    DTE.waitForEditor();
}

async function checkBodyEN(I, DT, DTE, Apps, title, values, perex) {
    await editDoc(I, DT, DTE, title);

    I.say("Switch editor type to HTML");
    Apps.switchEditor('html');

    I.say("Check EN translated body");
    within('.CodeMirror-code', () => {
        for(var i = 0; i < values.length; i++)
            I.see( values[i] );
        
        //This include must be unchanged !!!
        I.see( fake_include );
    });

    I.say("Check EN translated perex");
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.seeInField("#DTE_Field_htmlData", perex);

    DTE.cancel();
}

function performCloning(I, srcGroupId, destGroupId, translateUrl = true) {
    I.amOnPage("/components/clone_structure/clone_structure.jsp");
    I.fillField("#srcGroupId1", srcGroupId);
    I.fillField("#destGroupId1", destGroupId);

    if(translateUrl == false) {
        //We dont want translate urls
        I.checkOption('input[name=keepVirtualPath]');
    }

    I.say("Save groupID");
    I.clickCss("#btnOk");

    I.waitForElement("#dialogCentralRow");
    I.waitForText("Klonujem adresár:", 100);
    if(translateUrl == false) {
        I.waitForText("/" + srcGroupName_noUrlTranslate + "/" + srcGroupChildName_noUrlTranslate, 20);
    } else {
        I.waitForText("/" + srcGroupName + "/" + srcGroupChildName, 20);
    }
    I.waitForText("Klonovanie štruktúry dokončené", 100);
}

async function hardDeleteFolder(I, DT, DTE, groupName) {
    I.say("SOFT DELETE");
    I.waitForElement(locate("a.jstree-anchor").withText(groupName), 10);
    I.jstreeClick(groupName);

    I.say("Hold id for hard delete");
    I.wait(1);
    const groupId = await I.grabValueFrom('#tree-folder-id');

    I.say("Perform soft delete");
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.see(groupName, "div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();

    I.say("Check taht group was deleted");
    I.waitForInvisible(  locate("a.jstree-anchor").withText(groupName), 10 );

    I.say("HARD DELETE");
    I.click("#pills-trash-tab");
    DT.waitForLoader();

    I.say("Sort by id");
    I.fillField("#tree-folder-id", groupId);
    I.pressKey("Enter");
    DT.waitForLoader();
    I.wait(1);

    I.say("Perform hard delete");
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();
    I.wait(2);

    I.say("Go back to folders");
    I.clickCss("#pills-folders-tab");
    DT.waitForLoader();
}

Scenario("Structure clonning with translate - classic", async ({ I, DTE, DT, Apps })  => {
    I.say("Preparing source folder");
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=9811');
        DT.waitForLoader();
        createGroup(I, DTE, DT, srcGroupName, "Slovenský", true);
        I.jstreeClick(srcGroupName);
        //Save groupID
        const srcGroupId = await I.grabValueFrom('#tree-folder-id');

        //
        I.say("Create new doc in root group");
            I.click(DT.btn.add_button);
            DTE.waitForEditor();
            //Add name
            I.waitForVisible("#DTE_Field_title");
            I.fillField("#DTE_Field_title", newDocName);
            //Add perex
            I.clickCss("#pills-dt-datatableInit-perex-tab");
            I.fillField("#DTE_Field_htmlData", doc_a_child_sk_perex);
            //Fill body
            I.clickCss("#pills-dt-datatableInit-content-tab");
            I.waitForElement("iframe.cke_wysiwyg_frame");
            await DTE.fillCkeditor(doc_a_child_sk + fake_include_html);
            DTE.save();

        //
        I.say("Create sub group");
        createGroup(I, DTE, DT, srcGroupChildName, "Slovenský", false);

        I.jstreeClick(srcGroupChildName);
        await fillDocBody(I, DTE, DT, doc_b_sk + fake_include_html, doc_b_sk_perex);

    I.say("Preparing dest folder");
        createGroup(I, DTE, DT, destGroupName, "Anglický", true);
        I.click( locate("a.jstree-anchor").withText(destGroupName) );
        const destGroupId = await I.grabValueFrom('#tree-folder-id');

    I.say("Perform cloning - Clasic one");
        performCloning(I, srcGroupId, destGroupId, true);

    I.say("Check cloning result");
        I.amOnPage("/admin/v9/webpages/web-pages-list/");
        I.click( locate("a.jstree-anchor").withText(destGroupName) );


        await checkBodyEN(I, DT, DTE, Apps, "New", ["car", "tree"], "This is a perex.");

        I.click( locate("a.jstree-anchor").withText("Subfolder") );

        await checkBodyEN(I, DT, DTE, Apps, "Subfolder", ["new", "old", "oldest"], "This is also a perex.");

        I.say("Check folder optional fields");
        I.jstreeNavigate([destGroupName, srcGroupChildName]);
        I.click(DT.btn.tree_edit_button);
        DTE.waitForEditor("groups-datatable");

        I.click("#pills-dt-groups-datatable-fields-tab");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldA"), "field_a_value");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldB"), "field_b_value");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldC"), "field_c_value");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldD"), "field_d_value");
});

Scenario("Structure cloning with translate - NO URL TRANSLATE", async ({ I, DTE, DT })  => {
    I.say("Preparing source folder");
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=9811');
        DT.waitForLoader();
        createGroup(I, DTE, DT, srcGroupName_noUrlTranslate, "Slovenský", true);
        I.jstreeClick(srcGroupName_noUrlTranslate);
        //Save groupID
        const srcGroupId = await I.grabValueFrom('#tree-folder-id');

        I.say("Create sub group");
            createGroup(I, DTE, DT, srcGroupChildName_noUrlTranslate, "Slovenský", false);

        I.say("Preparing dest folder");
            createGroup(I, DTE, DT, destGroupName_noUrlTranslate, "Anglický", true);
            I.click( locate("a.jstree-anchor").withText(destGroupName_noUrlTranslate) );
            const destGroupId = await I.grabValueFrom('#tree-folder-id');

        I.say("Perform cloning - NO URL translate");
            performCloning(I, srcGroupId, destGroupId, false);

        I.say("Check cloning result");
            I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + destGroupId);
            DT.waitForLoader();
            I.click( locate("a.jstree-anchor").withText(destGroupChildName_noUrlTranslate) );
            I.click(DT.btn.tree_edit_button);
            DTE.waitForEditor("groups-datatable");

            //Its not translated good
            I.seeInField("#DTE_Field_urlDirName", srcGroupChildName_virtualPath);
            DTE.cancel();

            await editDoc(I, DT, DTE, destGroupChildName_noUrlTranslate);
            I.click("#pills-dt-datatableInit-basic-tab");
            I.seeInField("#DTE_Field_title", destGroupChildName_noUrlTranslate);
            I.seeInField("#DTE_Field_virtualPath", "/" + destGroupName_noUrlTranslate.toLowerCase() + "/"+srcGroupChildName_virtualPath+"/"); //SRC child name no translated URL, just prefix is DIFF

    I.say('Check that even LINKs work fine');
        //Same link WIth different prefix AND different showed pages
        I.amOnPage("/" + srcGroupName_noUrlTranslate.toLowerCase() + "/"+srcGroupChildName_virtualPath+"/");
        I.waitForText("POD-PRIEČINOK", 10);

        I.amOnPage("/" + destGroupName_noUrlTranslate.toLowerCase() + "/"+srcGroupChildName_virtualPath+"/");
        I.waitForText("SUB-FOLDER", 10);
});

Scenario('delete data', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=9811');
    DT.waitForLoader();

    await hardDeleteFolder(I, DT, DTE, srcGroupName);
    await hardDeleteFolder(I, DT, DTE, destGroupName);
    await hardDeleteFolder(I, DT, DTE, srcGroupName_noUrlTranslate);
    await hardDeleteFolder(I, DT, DTE, destGroupName_noUrlTranslate);
});

Scenario('odhlasenie', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});