Feature('webpages.mirroring');

let randomNumber;
const groupTable = "groupsMirroringTable";
const docTable = "docsMirroringTable";
const confTable = "mirroringConfDataTable";

const groupsWrapperSelector = "#groupsMirroringTable_wrapper";
const docsWrapperSelector = "#docsMirroringTable_wrapper";


Before(({ I, login, DT }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

let sk_mirroring;
let sk_mirroring_child_A;
let sk_mirroring_child_B;
let cz_mirroring;
let sk_mirroring_copy;

let sk_mirroring_id;
let cz_mirroring_id;
let sk_mirroring_copy_id;

Scenario('Mirroring 1 - Structure prepare', async ({I, DT, DTE, Document}) => {
    sk_mirroring = "sk_mirroring_" + randomNumber;
    sk_mirroring_child_A = "sk_mirroring_child_A_" + randomNumber;
    sk_mirroring_child_B = "sk_mirroring_child_B_" + randomNumber;
    cz_mirroring = "cz_mirroring_" + randomNumber;
    sk_mirroring_copy = "sk_mirroring_copy_" + randomNumber;

    I.amOnPage("/admin/v9/webpages/web-pages-list");
    Document.switchDomain("mirroring.tau27.iway.sk");

    I.say("Create root groups");
    addFolder(I, DT, DTE, sk_mirroring, true, "Slovenský");
    I.jstreeNavigate([sk_mirroring]);
    sk_mirroring_id = await I.grabValueFrom('#tree-folder-id');

    addFolder(I, DT, DTE, cz_mirroring, true, "Český");
    I.jstreeNavigate([cz_mirroring]);
    cz_mirroring_id = await I.grabValueFrom('#tree-folder-id');

    addFolder(I, DT, DTE, sk_mirroring_copy, true, "Slovenský");
    I.jstreeNavigate([sk_mirroring_copy]);
    sk_mirroring_copy_id = await I.grabValueFrom('#tree-folder-id');

    I.say("Create subgroup of sk folder");
    I.jstreeNavigate([sk_mirroring]);
    addFolder(I, DT, DTE, sk_mirroring_child_A, false);
    addFolder(I, DT, DTE, sk_mirroring_child_B, false);

    I.say("Check in mirroring that folders are not there");
    I.amOnPage("/admin/v9/webpages/mirroring/");
    DT.filterContains("fieldA", "/" + sk_mirroring, groupsWrapperSelector);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    changeTabAndCheck(I, DT, "docs", false);

    DT.filterContains("fieldA", "/" + sk_mirroring, docsWrapperSelector);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Mirroring 2 - Perform valid clonning', ({I}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.amOnPage("/components/clone_structure/clone_structure.jsp");
    I.fillField("#srcGroupId1", sk_mirroring_id);
    I.fillField("#destGroupId1", cz_mirroring_id);
    I.checkOption("input[name='keepMirroring']");
    I.clickCss("#btnOk");
    I.waitForText("Klonovanie štruktúry dokončené", 50);
});

Scenario('Mirroring 3 - Check mirroring values + logic', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/mirroring/");

    /* GROUPS */

    checkTablesVisibility(I, true, false, false);

    I.say("Check groups values");
    DT.filterContains("fieldA", getAsPath([sk_mirroring]), groupsWrapperSelector);
    DT.filterContains("fieldB", getAsPath([cz_mirroring]), groupsWrapperSelector);
    DT.checkTableRow(groupTable, 1, ["", "", getAsPath([sk_mirroring]), getAsPath([cz_mirroring]), "", ""]);
    DT.checkTableRow(groupTable, 2, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_A]), getAsPath([cz_mirroring, sk_mirroring_child_A]), "", ""]);
    DT.checkTableRow(groupTable, 3, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_B]), getAsPath([cz_mirroring, sk_mirroring_child_B]), "", ""]);

    //Check generated classes
    checkRowClass(I, groupTable, [".mirror-err", ".mirror-err", ".mirror-err"], false);
    checkRowClass(I, groupTable, [".mirror-warn", ".mirror-warn", ".mirror-warn"], false);

    I.say("Check groups editor");
    DT.filterContains("fieldA", getAsPath([sk_mirroring, sk_mirroring_child_A]), groupsWrapperSelector);
    selectAndOpen(I, DTE, groupTable);

    I.say("Check group showed selectors");
    checkShowedSelectors(I, "A", "sk", getAsPath([sk_mirroring, sk_mirroring_child_A]));
    checkShowedSelectors(I, "B", "cz", getAsPath([cz_mirroring, sk_mirroring_child_A]));

    I.say("Check addButton selector logic");
    checkAddButtonLogic(I, 'C', 'H');

    /* DOCS */

    I.amOnPage("/admin/v9/webpages/mirroring/");
    changeTabAndCheck(I, DT, "docs");

    I.say("Check docs values");
    DT.filterContains("fieldA", getAsPath([sk_mirroring]), docsWrapperSelector);
    DT.filterContains("fieldB", getAsPath([cz_mirroring]), docsWrapperSelector);
    DT.checkTableRow(docTable, 1, ["", "", getAsPath([sk_mirroring], true), getAsPath([cz_mirroring], true), "", ""]);
    DT.checkTableRow(docTable, 2, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_A], true), getAsPath([cz_mirroring, sk_mirroring_child_A], true), "", ""]);
    DT.checkTableRow(docTable, 3, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_B], true), getAsPath([cz_mirroring, sk_mirroring_child_B], true), "", ""]);

    checkRowClass(I, groupTable, [".mirror-err", ".mirror-err", ".mirror-err"], false);
    checkRowClass(I, groupTable, [".mirror-warn", ".mirror-warn", ".mirror-warn"], false);

    I.say("Check docs editor");
    DT.filterContains("fieldB", getAsPath([cz_mirroring, sk_mirroring_child_B], true), docsWrapperSelector);
    selectAndOpen(I, DTE, docTable);

    I.say("Check group showed selectors");
    checkShowedSelectors(I, "A", "sk", getAsPath([sk_mirroring, sk_mirroring_child_B], true));
    checkShowedSelectors(I, "B", "cz", getAsPath([cz_mirroring, sk_mirroring_child_B], true));

    I.say("Check addButton selector logic");
    checkAddButtonLogic(I, 'C', 'H');

    /* CONF */

    I.amOnPage("/admin/v9/webpages/mirroring/");
    changeTabAndCheck(I, DT, "conf");

    I.say("Check mirroring configurations");
    DT.checkTableRow(confTable, 1, ["1", "structureMirroringConfig"]);
    DT.checkTableRow(confTable, 2, ["2", "structureMirroringAutoTranslatorLogin"]);
    DT.checkTableRow(confTable, 3, ["3", "structureMirroringAutoTranslatorLogin"]);

    I.say("Check that mirroring bind was set");
    DT.filterEquals("name", "structureMirroringConfig");
    selectAndOpen(I, DTE, confTable);

    I.see(sk_mirroring_id + "," + cz_mirroring_id + ":mirroring.tau27.iway.sk");
});

Scenario('Mirroring 4 - Wrong clonning', async ({I}) => {
    I.say("Clonne group where SRC and DEST group have same LNG - logical err");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.amOnPage("/components/clone_structure/clone_structure.jsp");
    I.fillField("#srcGroupId1", sk_mirroring_id);
    I.fillField("#destGroupId1", sk_mirroring_copy_id);
    I.checkOption("input[name='keepMirroring']");
    I.clickCss("#btnOk");
    I.waitForText("Klonovanie štruktúry dokončené", 50);
});

Scenario('Mirroring 5 - Check mirroring result of wrong clonning + logic', ({I, DT, Document}) => {
    I.amOnPage("/admin/v9/webpages/mirroring/");

    checkTablesVisibility(I, true, false, false);

    I.say("Check groups values");
    DT.filterContains("fieldA", getAsPath([sk_mirroring]), groupsWrapperSelector);
    DT.filterContains("fieldB", getAsPath([cz_mirroring]), groupsWrapperSelector);

    DT.checkTableRow(groupTable, 1, ["", "", getAsPath([sk_mirroring]) + "\n" + getAsPath([sk_mirroring_copy]), getAsPath([cz_mirroring]), "", ""]);
    DT.checkTableRow(groupTable, 2, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_A]) + "\n" + getAsPath([sk_mirroring_copy, sk_mirroring_child_A]), getAsPath([cz_mirroring, sk_mirroring_child_A]), "", ""]);
    DT.checkTableRow(groupTable, 3, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_B]) + "\n" + getAsPath([sk_mirroring_copy, sk_mirroring_child_B]), getAsPath([cz_mirroring, sk_mirroring_child_B]), "", ""]);

    checkRowClass(I, groupTable, [".mirror-err", ".mirror-err", ".mirror-err"], true);

    I.say("Check docs values");
    changeTabAndCheck(I, DT, "docs");
    checkTablesVisibility(I, false, true, false);

    DT.filterContains("fieldA", getAsPath([sk_mirroring]), docsWrapperSelector);
    DT.filterContains("fieldB", getAsPath([cz_mirroring]), docsWrapperSelector);

    DT.checkTableRow(docTable, 1, ["", "", getAsPath([sk_mirroring], true) + "\n" + getAsPath([sk_mirroring_copy], true), getAsPath([cz_mirroring], true), "", ""]);
    DT.checkTableRow(docTable, 2, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_A], true) + "\n" + getAsPath([sk_mirroring_copy, sk_mirroring_child_A], true), getAsPath([cz_mirroring, sk_mirroring_child_A], true), "", ""]);
    DT.checkTableRow(docTable, 3, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_B], true) + "\n" + getAsPath([sk_mirroring_copy, sk_mirroring_child_B], true), getAsPath([cz_mirroring, sk_mirroring_child_B], true), "", ""]);

    checkRowClass(I, docTable, [".mirror-err", ".mirror-err", ".mirror-err"], true);
});

Scenario('Mirroring 6 - DOCS edit logic', ({I, DT, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/mirroring/");

    I.say("Test page edit");
    changeTabAndCheck(I, DT, "docs");

    DT.filterContains("fieldB", getAsPath([cz_mirroring, sk_mirroring_child_B], true), docsWrapperSelector);

    selectAndOpen(I, DTE, docTable);

    I.say("remove error sync and test");
    I.click( locate(".DTE_Field_Name_selectorB button.btn-vue-jstree-item-remove") );
    DTE.save();

    DT.checkTableRow(docTable, 1, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_B], true), getAsPath([cz_mirroring, sk_mirroring_child_B], true), "", ""]);
    checkRowClass(I, docTable, [".mirror-err.mirror-warn"], false);

    I.say("Now try add sync");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor(docTable);

    I.clickCss("button#add-sync-btn");
    I.click( locate(".DTE_Field_Name_selectorC button.btn-vue-jstree-item-edit") );
    I.waitForVisible("#jsTree");
    I.click(locate('.jstree-node.jstree-closed').withText(sk_mirroring_copy).find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jstree-node.jstree-closed').withText(sk_mirroring_child_B).find('.jstree-icon.jstree-ocl'));
    I.click( locate('.jstree-anchor').withText(sk_mirroring_child_B).withChild(".ti-star-filled") );
    I.checkOption("#DTE_Field_ignoreProblems_0");
    DTE.save();

    DT.checkTableRow(docTable, 1, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_B], true) + "\n" + getAsPath([sk_mirroring_copy, sk_mirroring_child_B], true), getAsPath([cz_mirroring, sk_mirroring_child_B], true), "", ""]);
    checkRowClass(I, docTable, [".mirror-err"], true);

    I.say("Now remove all sync and test that record is gone - like deleted");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor(docTable);

    I.click( locate(".DTE_Field_Name_selectorA button.btn-vue-jstree-item-remove") );
    I.click( locate(".DTE_Field_Name_selectorB button.btn-vue-jstree-item-remove") );
    I.click( locate(".DTE_Field_Name_selectorC button.btn-vue-jstree-item-remove") );
    DTE.save();

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Mirroring 7 - GROUPS edit logic', ({I, DT, DTE}) => {
    //Little complicated, because group change allso affect doc

    I.amOnPage("/admin/v9/webpages/mirroring/");

    DT.filterContains("fieldB", getAsPath([cz_mirroring, sk_mirroring_child_A]), groupsWrapperSelector);
    DT.checkTableRow(groupTable, 1, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_A]) + "\n" + getAsPath([sk_mirroring_copy, sk_mirroring_child_A]), getAsPath([cz_mirroring, sk_mirroring_child_A]), "", ""]);
    checkRowClass(I, groupTable, [".mirror-err"], true);

    selectAndOpen(I, DTE, groupTable);

    I.say("Remove bad sync and check status of group and doc");
    I.click( locate(".DTE_Field_Name_selectorB button.btn-vue-jstree-item-remove") );
    DTE.save();

    DT.checkTableRow(groupTable, 1, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_A]), getAsPath([cz_mirroring, sk_mirroring_child_A]), "", ""]);
    checkRowClass(I, groupTable, [".mirror-err.mirror-warn"], false);

    changeTabAndCheck(I, DT, "docs", false);

    DT.filterContains("fieldB", getAsPath([cz_mirroring, sk_mirroring_child_A]), docsWrapperSelector);
    DT.checkTableRow(docTable, 1, ["", "", getAsPath([sk_mirroring, sk_mirroring_child_A], true), getAsPath([cz_mirroring, sk_mirroring_child_A], true), "", ""]);
    checkRowClass(I, docTable, [".mirror-err.mirror-warn"], false);

    I.say("Remove all sync from group and check - delete like");
    changeTabAndCheck(I, DT, "groups", false);

    I.clickCss("button.buttons-edit");
    DTE.waitForEditor(groupTable);

    I.click( locate(".DTE_Field_Name_selectorA button.btn-vue-jstree-item-remove") );
    I.click( locate(".DTE_Field_Name_selectorB button.btn-vue-jstree-item-remove") );
    DTE.save();
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    changeTabAndCheck(I, DT, "docs", false);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Mirroring 8 - test delete logic', ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/mirroring/");

    DT.filterEquals("fieldA", getAsPath([sk_mirroring]) + " " + getAsPath([sk_mirroring_copy]), groupsWrapperSelector);

    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor(groupTable);
    I.click("Zmazať", "div.DTE_Action_Remove");

    DT.filterContains("fieldB", getAsPath([sk_mirroring]), groupsWrapperSelector);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    changeTabAndCheck(I, DT, "docs");
    DT.filterEquals("fieldA", getAsPath([sk_mirroring]), docsWrapperSelector);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Mirroring 9 - DELETE structure', ({I, DT, DTE}) => {
    deleteGroup(I, DT, DTE, sk_mirroring, sk_mirroring_id);
    deleteGroup(I, DT, DTE, cz_mirroring, cz_mirroring_id);
    deleteGroup(I, DT, DTE, sk_mirroring_copy, sk_mirroring_copy_id);
});

Scenario('odhlasenie', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});

function deleteGroup(I, DT, DTE, groupName, groupId) {
    I.amOnPage("/admin/v9/webpages/web-pages-list");

    I.say("Select group to delete");
    I.waitForElement(locate("a.jstree-anchor").withText(groupName), 10);
    I.jstreeClick(groupName);

    I.say("Perform soft delete");
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.see(groupName, "div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();

    I.say("Check taht group was deleted");
    I.waitForInvisible(  locate("a.jstree-anchor").withText(groupName), 10 );

    I.say("HARD DELETE");
    I.clickCss("#pills-trash-tab");
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
}

function changeTabAndCheck(I, DT, tab, doTest = true) {
    if("groups" == tab) {
        I.clickCss("#pills-groups-tab");
        DT.waitForLoader();
        if(doTest == true) checkTablesVisibility(I, true, false, false);
    } else if("docs" == tab) {
        I.clickCss("#pills-docs-tab");
        DT.waitForLoader();
        if(doTest == true) checkTablesVisibility(I, false, true, false);
    } else if("conf" == tab) {
        I.clickCss("#pills-settings-tab");
        DT.waitForLoader();
        if(doTest == true) checkTablesVisibility(I, false, false, true);
    }
}

function selectAndOpen(I, DTE, table) {
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor(table);
}

function checkRowClass(I, table, classes, shouldSee) {
    if(shouldSee == true) {
        for(let i = 0; i < classes.length; i++) {
            I.seeElement( locate("#" + table + " > tbody > tr:nth-child(" + (i + 1) + ")" + classes[i]) );
        }
    } else {
        for(let i = 0; i < classes.length; i++) {
            I.dontSeeElement( locate("#" + table + " > tbody > tr:nth-child(" + (i + 1) + ")" + classes[i]) );
        }
    }
}

function getAsPath(namesArr, doubleLastOne = false) {
    let path = "";
    for(let i = 0; i < namesArr.length; i++) {
        path += "/" + namesArr[i];
    }

    if(doubleLastOne == true) {
        path += "/" + namesArr[namesArr.length - 1];
    }

    return path;
}

function addFolder(I, DT, DTE, name, isRoot, language = null) {
    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor("groups-datatable");

    //set name
    I.fillField("#DTE_Field_groupName", name);

    if(isRoot == true) {
        I.groupSetRootParent();
        I.wait(1);
    }

    if(language != null) {
        I.clickCss("#pills-dt-groups-datatable-template-tab");
        I.waitForVisible("#DTE_Field_lng");
        DTE.selectOption('lng', language);
    }

    DTE.save();
}

function checkTablesVisibility(I, groups, docs, conf) {
    I.say("Check visible tables");
    groups == true ? I.seeElement("#groupsMirroringTable_wrapper") : I.dontSeeElement("#groupsMirroringTable_wrapper");
    docs == true ? I.seeElement("#docsMirroringTable_wrapper") : I.dontSeeElement("#docsMirroringTable_wrapper");
    conf == true ? I.seeElement("#mirroringConfDataTable_wrapper") : I.dontSeeElement("#mirroringConfDataTable_wrapper");
}

function checkShowedSelectors(I, alphabet, label, value) {
    I.seeElement("div#DTE_Field_selector" + alphabet + ".vueComponent");
    I.seeElement( locate(".DTE_Field_Name_selector" + alphabet + " label").withText(label) );
    I.seeElement("input[value='" + value + "']");
    I.seeElement( locate(".DTE_Field_Name_selector" + alphabet + " button.btn-vue-jstree-item-edit") );
    I.seeElement( locate(".DTE_Field_Name_selector" + alphabet + " button.btn-vue-jstree-item-remove") );
}

function checkAddButtonLogic(I, startAlphabet, endAlphabet) {
    I.seeElement("button#add-sync-btn");

    for(let alphabet = startAlphabet; alphabet.charCodeAt(0) <= endAlphabet.charCodeAt(0); alphabet = String.fromCharCode(alphabet.charCodeAt(0) + 1)) {
        I.dontSeeElement(".DTE_Field_Name_selector" + alphabet);
        I.clickCss("button#add-sync-btn");
        I.seeElement(".DTE_Field_Name_selector" + alphabet);

        if(alphabet != endAlphabet) {
            I.seeElement("button#add-sync-btn");
        } else {
            I.dontSeeElement("button#add-sync-btn");
        }
    }
}
