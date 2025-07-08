Feature('apps.clone-structure');

var randomNumber;
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
    I.say("Without key, no set translators");
    Document.setConfigValue("deepl_auth_key", "");

    I.amOnPage("/components/clone_structure/clone_structure.jsp");
    Document.screenshot("/redactor/apps/clone-structure/clone_structure_no_set_translator.png", 750, 600);

    I.say("Set key, now with set translator");
    Document.setConfigValue("deepl_auth_key", process.env.DEEPL_AUTH_KEY);

    I.amOnPage("/components/clone_structure/clone_structure.jsp");
    Document.screenshot("/redactor/apps/clone-structure/clone_structure_set_translator.png", 750, 600);

    I.fillField("#undoSyncGroupId", 21423);
    I.clickCss("#btnUndoSync");
    I.waitForVisible("#undo_succ", 10);
    Document.screenshot("/redactor/apps/clone-structure/clone_structure_undo_sync.png", 750, 600);
});

//COPIED from test, only added screen shot

function createGroup(I, DTE, DT, groupName, language, isRootGroup) {
    I.waitForElement(DT.btn.tree_add_button, 10);
    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor("groups-datatable");
    I.fillField("#DTE_Field_groupName", groupName);

    if(isRootGroup) {
        I.groupSetRootParent();
        I.clickCss("#pills-dt-groups-datatable-template-tab");
        DTE.selectOption("lng", language);
    }

    DTE.save();
}

async function fillDocBody(I, DTE, DT, body) {
    I.clickCss("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dt-scroll > div.dt-scroll-head > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(DT.btn.edit_button);

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

        switch (confLng) {
            case 'sk':
                createGroup(I, DTE, DT, srcGroupName, "Slovenský", true);
                break;
            case 'en':
                createGroup(I, DTE, DT, srcGroupName, "Slovak", true);
                break;
            case 'cs':
                createGroup(I, DTE, DT, srcGroupName, "Slovensky", true);
                break;
            default:
                throw new Error(`Unsupported language code: ${confLng}`);
        }


        I.jstreeClick(srcGroupName);
        //Save groupID
        const srcGroupId = await I.grabValueFrom('#tree-folder-id');

        //
        I.say("Create new doc in root group");
            I.click(DT.btn.add_button);
            DTE.waitForEditor();
            I.waitForVisible("#DTE_Field_title");
            I.fillField("#DTE_Field_title", newDocName);
            I.clickCss("#pills-dt-datatableInit-content-tab");
            I.waitForElement("iframe.cke_wysiwyg_frame");
            await DTE.fillCkeditor(doc_a_child_sk);
            DTE.save();

        //
        I.say("Create sub group");

        switch (confLng) {
            case 'sk':
                createGroup(I, DTE, DT, srcGroupChildName, "Slovenský", false);
                break;
            case 'en':
                createGroup(I, DTE, DT, srcGroupChildName, "Slovak", false);
                break;
            case 'cs':
                createGroup(I, DTE, DT, srcGroupChildName, "Slovensky", false);
                break;
            default:
                throw new Error(`Unsupported language code: ${confLng}`);
        }

        I.jstreeClick(srcGroupChildName);
        await fillDocBody(I, DTE, DT, doc_b_sk);

    //
    I.say("Preparing dest folder");

    switch (confLng) {
        case 'sk':
            createGroup(I, DTE, DT, destGroupName, "Anglický", true);
            break;
        case 'en':
            createGroup(I, DTE, DT, destGroupName, "English", true);
            break;
        case 'cs':
            createGroup(I, DTE, DT, destGroupName, "Česky", true);
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
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
        } else if("cs" === confLng) {
            I.waitForText("Klonuji adresář:", 100);
            I.waitForText("/clone-src-autotest" + randomNumber + "/Subfolder-autotest")
            I.waitForText("Klonování struktury dokončeno", 100);
        }

    Document.screenshot("/redactor/apps/clone-structure/clone_structure_result.png", 500, 500);
});

Scenario('delete data', ({ I, DTE, DT }) => {
    let confLng = I.getConfLng();
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    //
    I.say("Delete and check");
        I.waitForElement(locate("a.jstree-anchor").withText(srcGroupName), 10);
        I.jstreeClick(srcGroupName);
        //delete
        I.click(DT.btn.tree_delete_button);
        DTE.waitForEditor("groups-datatable");

        switch (confLng) {
            case 'sk':
                I.click("Zmazať", "div.DTE_Action_Remove");
                break;
            case 'en':
                I.click("Delete", "div.DTE_Action_Remove");
                break;
            case 'cs':
                I.click("Smazat", "div.DTE_Action_Remove");
                break;
            default:
                throw new Error(`Unsupported language code: ${confLng}`);
        }

        DTE.waitForLoader();
        //Check that both folders are gone
        I.waitForInvisible(  locate("a.jstree-anchor").withText(srcGroupName), 10 );

        I.jstreeClick(destGroupName);
        //delete
        I.click(DT.btn.tree_delete_button);
        DTE.waitForEditor("groups-datatable");


        switch (confLng) {
            case 'sk':
                I.click("Zmazať", "div.DTE_Action_Remove");
                break;
            case 'en':
                I.click("Delete", "div.DTE_Action_Remove");
                break;
            case 'cs':
                I.click("Smazat", "div.DTE_Action_Remove");
                break;
            default:
                throw new Error(`Unsupported language code: ${confLng}`);
        }

        //Check that both folders are gone
        DTE.waitForLoader();
});

Scenario('odhlasenie', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});