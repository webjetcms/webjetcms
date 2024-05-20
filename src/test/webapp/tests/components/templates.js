Feature('components.templates');

var randomNumber;
var containerTree = "div.tree-col";
var containerTable = "#datatableInit_wrapper";

let testOptions = {
    dataTable: 'tempsTable',
    requiredFields: ['tempName', 'forward'],
    perms: 'menuTemplates',
    editSteps: function(I, options, DT, DTE) {
        I.click("#pills-dt-datatableInit-accessTab-tab");
        I.click("Pridať");
        I.waitForVisible("div.jsTree-wrapper");
        I.wait(1);
        I.click("Newsletter", "#jsTree");
        I.click("#pills-dt-datatableInit-basic-tab");
    },
    editSearchSteps: function(I, options, DT, DTE) {
        I.fillField("input.dt-filter-availableGrooupsList", "News");
        I.pressKey('Enter', "input.dt-filter-availableGrooupsList");
        DT.waitForLoader();
        I.see("Newsletter", "div.dataTables_scrollBody");
        I.see(`${options.testingData[0]}-chan.ge`, "div.dataTables_scrollBody");
    }
};

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/templates/temps-list");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('templates-zakladne testy', async ({ I, DataTables }) => {
    I.see("Skupina šablón");
    await DataTables.baseTest(testOptions);
});

Scenario('overenie prepinania zoznamu JSP forwardov', ({ I, DTE }) => {
    I.click("Subpage");
    DTE.waitForEditor();
    I.dontSee("default/subpage.jsp", "div.DTE_Field_Name_forward");
    //tento select pre ukazku menime zadanim textu a potvrdenim enterom
    DTE.selectOption("templatesGroupId", "default");
    //I.pressKey("Enter", "div.bs-searchbox input");

    //pockaj na dobehnutie ajaxu na zoznam forwardov
    I.wait(3);
    I.fillField("#DTE_Field_forward", "*");
    I.waitForVisible("ul.dt-autocomplete-select");
    //over, ze sa nacitali
    I.see("default/subpage.jsp", "ul.dt-autocomplete-select")
    I.see("default/homepage.jsp", "ul.dt-autocomplete-select")

    //skus selectnut
    I.click(locate("ul.dt-autocomplete-select li").withText("subpage.jsp"));
    //over, ze to je nastavene priamo v bs-selecte
    I.seeInField("#DTE_Field_forward", "default/subpage.jsp");
});

Scenario('Template merge', ({I, DT, DTE}) => {
    var oldTemplateName = "auto-test-old-" + randomNumber;
    var newTemplateName = "auto-test-new-" + randomNumber;
    var groupDocName = "name-autotest-tr-" + randomNumber;

    //Create OLD template (template we want to replace)
        I.say("Create OLD template (template we want to replace)");
        I.click({css: "div.dt-buttons button.buttons-create"});
        DTE.waitForLoader();
        I.click("#DTE_Field_tempName");
        I.fillField("#DTE_Field_tempName", oldTemplateName);
        I.fillField("#DTE_Field_forward", "default/homepage.jsp");

        //Save
        DTE.save();
        DTE.waitForLoader();

        //Check that template was created
        DT.filter("tempName", oldTemplateName);
        I.see(oldTemplateName);

    //Create NEW template (template that will replace OLD template)
        I.say("Create NEW template (template that will replace OLD template)");
        I.click({css: "div.dt-buttons button.buttons-create"});
        DTE.waitForLoader();
        I.click("#DTE_Field_tempName");
        I.fillField("#DTE_Field_tempName", newTemplateName);
        I.fillField("#DTE_Field_forward", "default/homepage.jsp");
        //check that new template hasn't option to merge
        I.click("#pills-dt-datatableInit-templatesTab-tab");
        I.dontSee("Zlúčiť túto šablónu do zvolenej šablóny");

        //Save
        DTE.save();

        //Check that template was created
        DT.filter("tempName", newTemplateName);
        I.see(newTemplateName);

    //Create group with doc and use oldTemplate
        I.say("Create group with doc and use oldTemplate");
        I.amOnPage("/admin/v9/webpages/web-pages-list/?groupId=0");
        I.jstreeReset();

        I.waitForElement("button.buttons-create", 10, containerTree);
        I.click("button.buttons-create", containerTree);
        DTE.waitForEditor("groups-datatable");
        I.dontSeeElement("div.toast-message");
        I.dontSee("Pole Menu (loggedMenuType) neobsahuje možnosť s ID -1");

        I.say("Setting group name");
        I.click("#DTE_Field_groupName");
        I.fillField("#DTE_Field_groupName", groupDocName);
        I.click("#DTE_Field_navbarName");
        I.groupSetRootParent();
        I.wait(1);

        I.say("Setting template");
        I.click("#pills-dt-groups-datatable-template-tab");
        I.wait(2);
        DTE.selectOption("tempId", oldTemplateName);

        I.click("Pridať");
        I.waitForText(groupDocName, 10, containerTree);

    //Go replace template + some checks
        I.say("Go replace oldTemplate with newTemplate");
        I.amOnPage("/admin/v9/templates/temps-list/");
        DT.filter("tempName", oldTemplateName);
        I.click(oldTemplateName);
        DTE.waitForLoader();

        //Check if we see fields (edit only)
        I.say("Check if we see fields (edit only)");
        I.click("#pills-dt-datatableInit-templatesTab-tab");
        I.see("Zlúčiť túto šablónu do zvolenej šablóny");

        //Check if nothing happend (Because toggle is false)
        I.say("Check if nothing happend (Because toggle is false)");
        I.click("#panel-body-dt-datatableInit-templatesTab > div.DTE_Field.form-group.row.DTE_Field_Type_select.DTE_Field_Name_editorFields\\.mergeToTempId.hide-on-create > div.col-sm-7 > div.DTE_Field_InputControl > div > button");
        I.fillField("body > div.bs-container.dropdown.bootstrap-select.form-select.dropup > div > div.bs-searchbox > input", newTemplateName);
        I.wait(2);
        I.click(locate("a.dropdown-item").withChild("span.text").withText(newTemplateName));
        DTE.save();

        //Check loop error
        I.say("Check loop error");
        I.click(oldTemplateName);
        DTE.waitForLoader();
        I.click("#pills-dt-datatableInit-templatesTab-tab");
        DTE.selectOption("editorFields\\.mergeToTempId", oldTemplateName);
        I.click("#DTE_Field_editorFields-mergeTemplates_0");
        DTE.save();
        DTE.waitForLoader();
        I.see("Nemôžete nahradiť šablónu tou istou šablónou.");

        //Check if old vanish after replace
        I.say("Check if old vanish after replace");
        DTE.selectOption("editorFields\\.mergeToTempId", newTemplateName);
        DTE.save();
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
        I.wait(1);

    //Go back to doc/groups (webpages) and check changes then remove support group with doc
        I.say("Go back to doc/groups (webpages) and check changes then remove support group with doc");
        I.amOnPage("/admin/v9/webpages/web-pages-list/");
        I.wait(2);
        I.click(groupDocName);

        //Check template in group
        I.say("Check if replaced template is replaced in GROUPS");
        I.click("button.buttons-edit", containerTree);
        DTE.waitForEditor("groups-datatable");
        I.click("#pills-dt-groups-datatable-template-tab");
        I.see(newTemplateName);
        I.click("Zrušiť");
        DTE.waitForLoader();

        //Check template in doc
        I.say("Check if replaced template is replaced in  DOCS");
        I.click("td.dt-select-td.sorting_1");
        I.click(containerTable + " button.buttons-edit");
        DTE.waitForLoader("webpagesDatatable");
        I.click("#pills-dt-datatableInit-template-tab");
        I.see(newTemplateName);
        I.click("Zrušiť")
        DTE.waitForLoader();

        //Remove group/doc
        I.say("Remove test group/doc");
        I.click("button.buttons-remove", containerTree);
        I.click("Zmazať");
        DTE.waitForLoader();

    //Remove left newTemplate and check that old one is gone too
        I.amOnPage("/admin/v9/templates/temps-list/");

        I.say("Check taht olTemplate is gone");
        DT.filter("tempName", oldTemplateName);
        I.see("Nenašli sa žiadne vyhovujúce záznamy");

        I.say("Delete newTemplate and check if is gone");
        DT.filter("tempName", newTemplateName);
        I.see(newTemplateName);
        I.click("td.dt-select-td.sorting_1");
        I.click("button.buttons-remove");
        I.click("Zmazať");
        DTE.waitForLoader();
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Forwards by installName', ({I, DTE}) => {

    I.click("button.buttons-create");
    DTE.waitForEditor();

    I.fillField("#DTE_Field_forward", "*");
    I.waitForVisible("ul.dt-autocomplete-select");
    I.see("default/subpage.jsp", "ul.dt-autocomplete-select li");
    I.see("default/homepage.jsp", "ul.dt-autocomplete-select li");
    I.dontSee("another/another.jsp", "ul.dt-autocomplete-select li");

    //
    I.say("Setting another install name");
    I.fillField("#DTE_Field_templateInstallName", "another-install-name");

    I.fillField("#DTE_Field_forward", "*");
    I.waitForVisible("ul.dt-autocomplete-select");
    I.dontSee("default/subpage.jsp", "ul.dt-autocomplete-select li");
    I.dontSee("default/homepage.jsp", "ul.dt-autocomplete-select li");
    I.see("another/another.jsp", "ul.dt-autocomplete-select li");
});

Scenario('Filter templates by domainName/groups', ({I, DT, Document}) => {
    DT.filter("tempName", "Microsite");
    I.see("Microsite - yellow");
    I.see("Microsite - blue");
    DT.filter("tempName", "Newsletter");
    I.see("Newsletter");
    I.see("Newsletter EN");

    Document.switchDomain("test23.tau27.iway.sk");

    DT.filter("tempName", "Microsite");
    I.see("Microsite - yellow");
    I.dontSee("Microsite - blue");
    DT.filter("tempName", "Newsletter");
    I.see("Newsletter EN");
    I.see("Záznamy 1 až 1 z 1");

    Document.switchDomain("mirroring.tau27.iway.sk");

    DT.filter("tempName", "Microsite");
    I.see("Microsite - yellow");
    I.dontSee("Microsite - blue");
    DT.filter("tempName", "Newsletter");
    I.dontSee("Newsletter EN");
    I.see("Záznamy 0 až 0 z 0");
});

Scenario('odhlasenie', ({ I }) => {
    I.logout();
});