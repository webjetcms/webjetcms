Feature('webpages.perex');

var doc_add_button = (locate("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success"));

Before(({ I, login }) => {
    login('admin');
});

Scenario('perex-zakladne testy @baseTest', async ({I, DataTables }) => {
    I.amOnPage("/admin/v9/webpages/perex/");
    await DataTables.baseTest({
        dataTable: 'perexDataTable',
        perms: 'editor_edit_perex',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
        skipSwitchDomain: true
    });
});

Scenario('verify all domains selection for available groups', ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/perex/");
    I.see("demotest.webjetcms.sk:/Newsletter", "#perexDataTable td.dt-style-json");
    DT.filter("perexGroupName", "PerexWithGroup_");
    I.see("demotest.webjetcms.sk:/Newsletter", "#perexDataTable td.dt-style-json");
    I.dontSee("/Test stavov/Zaheslovaný", "#perexDataTable td.dt-style-json");
    I.click("PerexWithGroup_B");
    DTE.waitForEditor("perexDataTable");
    I.click("button.btn-vue-jstree-add");
    I.waitForElement("#jsTree");
    I.waitForText("demotest.webjetcms.sk", 5, "#jsTree a.jstree-anchor");
    I.waitForText("mirroring.tau27.iway.sk", 5, "#jsTree a.jstree-anchor");
    I.click("a.close-custom-modal");
    DTE.cancel();
});

Scenario('overenie filtrovania perex skupin podla adresara', ({I, DTE}) => {
    var editorContainer = "#datatableInit_modal .DTE_Field_Name_perexGroups";

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.say("Before navigate");
    I.jstreeNavigate(["Jet portal 4", "Kontakt"]);
    I.say("After navigate");
    I.click("Kontakt", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible(editorContainer);
    I.scrollTo(editorContainer, 5, 5);
    I.wait(1);
    I.see("podnikanie", editorContainer);
    I.see("investícia", editorContainer);
    I.dontSee("Newsletter perex skupina", editorContainer);
    I.clickCss("div.DTE_Footer button.btn-close-editor", "#datatableInit_modal");
    DTE.waitForLoader();

    I.jstreeNavigate(["Newsletter"]);
    I.click("Newsletter", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible(editorContainer);
    I.scrollTo(editorContainer, 5, 5);
    I.wait(1);
    I.see("podnikanie", editorContainer);
    I.see("investícia", editorContainer);
    I.see("Newsletter perex skupina", editorContainer);
});

function openEditorOnPerexTab(I, DTE) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=30319");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible("#pills-dt-datatableInit-perex");
}

Scenario('overenie nastavenia perex skupiny web stranky', ({I, DTE}) => {
    //nastav perex skupinu
    openEditorOnPerexTab(I, DTE);
    I.checkOption("#DTE_Field_perexGroups_3");
    I.wait(1);
    DTE.save();

    //over perex skupinu
    openEditorOnPerexTab(I, DTE);
    I.dontSeeElement("#DTE_Field_perexGroups_1:checked");
    I.seeElement("#DTE_Field_perexGroups_3:checked");

    //odskrtni a uloz
    I.uncheckOption("#DTE_Field_perexGroups_3");
    I.wait(1);
    DTE.save();
    openEditorOnPerexTab(I, DTE);
    I.dontSeeElement("#DTE_Field_perexGroups_3:checked");
});

Scenario('overenie filtrovania perexov podla prava', ({I, DT}) => {
    I.relogin("tester_perex");
    I.amOnPage("/admin/v9/webpages/perex/");

    //With perm for folder /Test stavov
    checkPerex(I, DT, "PerexWithoutGroup", true);
    checkPerex(I, DT, "PerexWithGroup_A", false);
    checkPerex(I, DT, "PerexWithGroup_B", true);

    //No perm
    I.relogin("tester");
    I.amOnPage("/admin/v9/webpages/perex/");
    checkPerex(I, DT, "PerexWithoutGroup", true);
    checkPerex(I, DT, "PerexWithGroup_A", true);
    checkPerex(I, DT, "PerexWithGroup_B", true);

    //With perm for folder /Newsletter
    I.relogin("tester2");
    I.amOnPage("/admin/v9/webpages/perex/");
    checkPerex(I, DT, "PerexWithoutGroup", true);
    checkPerex(I, DT, "PerexWithGroup_A", true);
    checkPerex(I, DT, "PerexWithGroup_B", false);

});

Scenario('Check Perex Groups Rendering Behavior in Editor', ({ I, DT, DTE , Document }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    DT.resetTable();
    DT.showColumn('Značky');

    checkElements(I, DTE, DT, true);

    Document.setConfigValue('perexGroupsRenderAsSelect', 3);

    checkElements(I, DTE, DT, false);

});

Scenario('Restore Default Perex Groups Rendering Configuration', ({ I, DT, Document }) => {
    Document.setConfigValue('perexGroupsRenderAsSelect', 30);
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    DT.resetTable();
});

/**
 * Checks the visibility of checkbox or multiselect elements in the perex tab
 * of the web pages list editor.
 *
 * @param {Object} I
 * @param {Object} DTE
 * @param {boolean} shouldSeeCheckbox - A flag indicating whether to check for
 *        checkboxes (true) or multiselect elements (false).
 */
async function checkElements(I, DTE, DT, shouldSeeCheckbox) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=24');

    I.say('Try to fill search field column in datatable');
    I.waitForVisible('#datatableInit_wrapper th.dt-th-perexGroups', 10);

    I.waitForText("McGregorov obchodný úder", 10, "#datatableInit td.dt-row-edit");
    I.see("Trhy sú naďalej vydesené", "#datatableInit td.dt-row-edit");

    DT.filter('perexGroups','odni'); //PODNIKANIE

    I.waitForText("McGregorov obchodný úder", 10, "#datatableInit td.dt-row-edit");
    I.dontSee("Trhy sú naďalej vydesené", "#datatableInit td.dt-row-edit");

    DT.clearFilter('perexGroups');

    I.click("McGregorov obchodný údera");
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-perex-tab');

    if (shouldSeeCheckbox) {
        I.say('Check if I see checkbox in perex tab');
        I.dontSeeElement('#DTE_Field_perexGroups[multiple]');
        I.dontSeeElement('#DTE_Field_perexGroups');
        I.seeElement('#DTE_Field_perexGroups_0');
        I.seeCheckboxIsChecked(locate(".DTE_Field_Name_perexGroups div.form-switch").withText("podnikanie").find("input"));
        I.dontSeeCheckboxIsChecked(locate(".DTE_Field_Name_perexGroups div.form-switch").withText("investícia").find("input"));
    } else {
        I.say('Check if I see multiselect in perex tab');
        I.seeElement('#DTE_Field_perexGroups[multiple]');
        I.seeElement('#DTE_Field_perexGroups');
        I.dontSeeElement('#DTE_Field_perexGroups_0');
        I.see("podnikanie", ".DTE_Field_Name_perexGroups div.bootstrap-select .filter-option .filter-option-inner-inner");
        I.dontSee("investícia", ".DTE_Field_Name_perexGroups div.bootstrap-select .filter-option .filter-option-inner-inner");
    }

    DTE.cancel();
}

/**
 * Checks the visibility of a specified perex group name in the filtered results.
 *
 * @param {Object} I
 * @param {Object} DT
 * @param {string} name - The name of the perex group to check for in the filtered results.
 * @param {boolean} [shouldSee=true] - A flag indicating whether the specified name should
 *        be visible (true) or not (false). Defaults to true.
 */
function checkPerex(I, DT,  name, shouldSee = true) {
    DT.filter("perexGroupName", name);

    if(shouldSee) {
        I.see(name);
        I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    } else {
        I.dontSee(name);
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
}