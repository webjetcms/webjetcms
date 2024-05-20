Feature('webpages.clone-structure');

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

function createGroup(I, DTE, groupName, language, isRootGroup) {
    I.waitForElement(add_button, 10);
    I.click(add_button);
    DTE.waitForEditor("groups-datatable");
    I.fillField("#DTE_Field_groupName", groupName);

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

async function fillDocBody(I, DTE, body) {
    I.clickCss("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(doc_edit_button);

    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(body);
    DTE.save();
}

async function checkBodyEN(I, DT, DTE, title, values) {

    DT.filter("title", title);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.clickCss("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(doc_edit_button);
    DTE.waitForEditor();

    I.say("Check EN translated body");
    I.switchTo("iframe.cke_wysiwyg_frame");
    for(var i = 0; i < values.length; i++)
        I.see( values[i] );
    I.switchTo();

    DTE.cancel();
}

Scenario("Structure clonning with translate", async ({ I, DTE, DT })  => {

    //
    I.say("Preparing source folder");
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=9811');
        DT.waitForLoader();
        createGroup(I, DTE, srcGroupName, "Slovenský", true);
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
        createGroup(I, DTE, srcGroupChildName, "Slovenský", false);

        I.jstreeClick(srcGroupChildName);
        await fillDocBody(I, DTE, doc_b_sk);

    //
    I.say("Preparing dest folder");
        createGroup(I, DTE, destGroupName, "Anglický", true);
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

        I.waitForElement("#dialogCentralRow");
        I.waitForText("Klonujem adresár:", 100);
        I.waitForText("/clone-src-autotest"+randomNumber+"/Subfolder-autotest")
        I.waitForText("Klonovanie štruktúry dokončené", 100);

    //
    I.say("Check clonning result");
        I.amOnPage("/admin/v9/webpages/web-pages-list/");
        I.click( locate("a.jstree-anchor").withText(destGroupName) );

        await checkBodyEN(I, DT, DTE, "New", ["car", "tree"]);

        I.click( locate("a.jstree-anchor").withText("Subfolder") );

        await checkBodyEN(I, DT, DTE, "Subfolder", ["new", "old", "oldest"]);

        I.say("Check folder optional fields");
        I.jstreeNavigate([destGroupName, srcGroupChildName]);
        I.click(edit_button);
        DTE.waitForEditor("groups-datatable");
        I.click("#pills-dt-groups-datatable-fields-tab");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldA"), "field_a_value");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldB"), "field_b_value");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldC"), "field_c_value");
        I.seeInField(locate("#groups-datatable_modal").find("#DTE_Field_fieldD"), "field_d_value");
});

Scenario('delete data', ({ I, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    //
    I.say("Delete and check");
        I.waitForElement(locate("a.jstree-anchor").withText(srcGroupName), 10);
        I.jstreeClick(srcGroupName);
        //delete
        I.click(delete_button);
        DTE.waitForEditor("groups-datatable");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader();
        //Check that both folders are gone
        I.waitForInvisible(  locate("a.jstree-anchor").withText(srcGroupName), 10 );

        I.jstreeClick(destGroupName);
        //delete
        I.click(delete_button);
        DTE.waitForEditor("groups-datatable");
        I.click("Zmazať", "div.DTE_Action_Remove");
        //Check that both folders are gone
        DTE.waitForLoader();
});

Scenario('odhlasenie', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});