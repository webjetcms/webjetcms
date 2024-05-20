Feature('apps.clone-structure');

var randomNumber;
var add_button = (locate('.tree-col').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider'));
var edit_button = (locate('.tree-col').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning'));
var delete_button = (locate('.tree-col').find('.btn.btn-sm.buttons-selected.buttons-remove.noperms-deleteDir.btn-danger'));
var doc_edit_button = (locate("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.buttons-selected.buttons-edit.btn-warning"));
var doc_add_button = (locate("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success"));

var srcGroupName = "clone-src-autotest";
var srcGroupChildName = "Subfolder-autotest";
var destGroupName = "clone-dest-autotest";
var newDocName = "New page-autotest";

var doc_a_child_sk = '<p> auto strom </p>';
var doc_b_sk = '<p> nový starý najstarší </p>';


Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();

        srcGroupName += randomNumber;
        destGroupName += randomNumber;
    }
});

Scenario("Structure clone screenshots", async ({ I, Document })  => {
    I.amOnPage("/components/clone_structure/clone_structure.jsp");
    I.resizeWindow(600, 500); //Need to by first resize and than screenshot)
    Document.screenshot("/redactor/apps/clone-structure/clone_structure.png");
    //I.click("#dialogCentralRow > div > form > table > tbody > tr:nth-child(1) > td:nth-child(2) > input.button50");
    //Document.screenshot("/redactor/apps/clone-structure/clone_structure_2.png");
});

//COPYED from test, only added screen shot

function createGroup(I, DTE, groupName, language, isRootGroup) {
    I.waitForElement(add_button, 10);
    I.click(add_button);
    DTE.waitForEditor("groups-datatable");
    I.fillField("#DTE_Field_groupName", groupName);

    if(isRootGroup) {
        I.groupSetRootParent();
        I.clickCss("#pills-dt-groups-datatable-template-tab");
        DTE.selectOption("lng", language);
    }

    DTE.save();
}

async function fillDocBody(I, DTE, body) {
    I.clickCss("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(doc_edit_button);

    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(body);
    DTE.save();
}

Scenario("Structure clonning with translate", async ({ I, DTE, DT, Document })  => {
    let confLng = I.getConfLng();

    //
    I.say("Preparing source folder");
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=9811');
        DT.waitForLoader();

        if("sk" === confLng) {
            createGroup(I, DTE, srcGroupName, "Slovenský", true);
        } else if("en" === confLng) {
            createGroup(I, DTE, srcGroupName, "Slovak", true);
        }

        I.jstreeClick(srcGroupName);
        //Save groupID
        const srcGroupId = await I.grabValueFrom('#tree-folder-id');

        //
        I.say("Create new doc in root group");
            I.click(doc_add_button);
            DTE.waitForEditor();
            I.waitForVisible("#DTE_Field_title");
            I.fillField("#DTE_Field_title", newDocName);
            I.clickCss("#pills-dt-datatableInit-content-tab");
            I.waitForElement("iframe.cke_wysiwyg_frame");
            await DTE.fillCkeditor(doc_a_child_sk);
            DTE.save();

        //
        I.say("Create sub group");

        if("sk" === confLng) {
            createGroup(I, DTE, srcGroupChildName, "Slovenský", false);
        } else if("en" === confLng) {
            createGroup(I, DTE, srcGroupChildName, "Slovak", false);
        }

        I.jstreeClick(srcGroupChildName);
        await fillDocBody(I, DTE, doc_b_sk);

    //
    I.say("Preparing dest folder");

        if("sk" === confLng) {
            createGroup(I, DTE, destGroupName, "Anglický", true);
        } else if("en" === confLng) {
            createGroup(I, DTE, destGroupName, "English", true);
        }
        
        I.click( locate("a.jstree-anchor").withText(destGroupName) );
        const destGroupId = await I.grabValueFrom('#tree-folder-id');

    //
    I.say("Perform cloning");
        I.amOnPage("/components/clone_structure/clone_structure.jsp");
        I.fillField("#srcGroupId1", srcGroupId);
        I.fillField("#destGroupId1", destGroupId);

        //
        I.say("Save groupID");
        I.clickCss("#btnOk");

        I.waitForElement(".wjDialogHeaderTable");

        if("sk" === confLng) {
            I.waitForText("Klonujem adresár:", 100);
            I.waitForText("/clone-src-autotest" + randomNumber + "/Subfolder-autotest")
            I.waitForText("Klonovanie štruktúry dokončené", 100);
        } else if("en" === confLng) {
            I.waitForText("Cloning directory:", 100);
            I.waitForText("/clone-src-autotest" + randomNumber + "/Subfolder-autotest")
            I.waitForText("Clone structure done", 100);
        }

    Document.screenshot("/redactor/apps/clone-structure/clone_structure_result.png", 500, 500);
});

Scenario('delete data', ({ I, DTE }) => {
    let confLng = I.getConfLng();
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    //
    I.say("Delete and check");
        I.waitForElement(locate("a.jstree-anchor").withText(srcGroupName), 10);
        I.jstreeClick(srcGroupName);
        //delete
        I.click(delete_button);
        DTE.waitForEditor("groups-datatable");

        if("sk" === confLng) {
            I.click("Zmazať", "div.DTE_Action_Remove");
        } else if("en" === confLng) { 
            I.click("Delete", "div.DTE_Action_Remove");
        };

        DTE.waitForLoader();
        //Check that both folders are gone
        I.waitForInvisible(  locate("a.jstree-anchor").withText(srcGroupName), 10 );

        I.jstreeClick(destGroupName);
        //delete
        I.click(delete_button);
        DTE.waitForEditor("groups-datatable");

        if("sk" === confLng) {
            I.click("Zmazať", "div.DTE_Action_Remove");
        } else if("en" === confLng) { 
            I.click("Delete", "div.DTE_Action_Remove");
        }

        //Check that both folders are gone
        DTE.waitForLoader();
});

Scenario('odhlasenie', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});