Feature('webpages.perex');

Before(({ login, DT }) => {
    login('admin');

    DT.addContext('perex', '#perexDataTable_wrapper');
});

Scenario('perex-zakladne testy @baseTest', async ({I, DataTables }) => {
    I.amOnPage("/admin/v9/webpages/perex/");
    await DataTables.baseTest({
        dataTable: 'perexDataTable',
        perms: 'editor_edit_perex',
        createSteps: function(I, options) {
            I.clickCss("#pills-dt-perexDataTable-fields-tab");

            I.click("input#DTE_Field_fieldA");
            I.fillField("#DTE_Field_fieldA", "auto");
            I.waitForVisible( locate(".ui-menu-item-wrapper").withText("Autocomplete Iná možnosť") );
            I.click( locate(".ui-menu-item-wrapper").withText("Autocomplete Iná možnosť") );
            I.checkOption("#DTE_Field_fieldB");
            I.fillField("#DTE_Field_fieldC", "test fieldC");
            I.fillField("#DTE_Field_fieldD", "test fieldD");
            I.fillField("#DTE_Field_fieldE", "test fieldE");
            I.fillField("#DTE_Field_fieldF", "test fieldF");

            I.clickCss("#pills-dt-perexDataTable-basic-tab");
        },
        editSteps: function(I, options) {
            I.clickCss("#pills-dt-perexDataTable-fields-tab");

            I.seeInField("#DTE_Field_fieldA", "Autocomplete Iná možnosť");
            I.seeCheckboxIsChecked("#DTE_Field_fieldB");
            I.seeInField("#DTE_Field_fieldC", "test fieldC");
            I.seeInField("#DTE_Field_fieldD", "test fieldD");
            I.seeInField("#DTE_Field_fieldE", "test fieldE");
            I.seeInField("#DTE_Field_fieldF", "test fieldF");

            I.clickCss("#pills-dt-perexDataTable-basic-tab");
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        }
    });
});

Scenario('verify only current domain selection for available groups', ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/perex/");
    I.dontSee(I.getDefaultDomainName()+":/Newsletter", "#perexDataTable td.dt-style-json");
    I.see("/Newsletter", "#perexDataTable td.dt-style-json");
    DT.filterContains("perexGroupName", "PerexWithGroup_");
    I.dontSee(I.getDefaultDomainName()+":/Newsletter", "#perexDataTable td.dt-style-json");
    I.see("/Newsletter", "#perexDataTable td.dt-style-json");
    I.dontSee("/Test stavov/Zaheslovaný", "#perexDataTable td.dt-style-json");
    I.click("PerexWithGroup_B");
    DTE.waitForEditor("perexDataTable");
    I.click("button.btn-vue-jstree-add");
    I.waitForElement("#jsTree");
    I.waitForText("Jet portal 4", 5, "#jsTree a.jstree-anchor");
    I.dontSee("mirroring.tau27.iway.sk", "#jsTree a.jstree-anchor");
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

Scenario('Check Perex Groups Rendering Behavior in Editor', async ({ I, DT, DTE , Document }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    DT.resetTable();
    await DT.showColumn('Značky');

    checkElements(I, DTE, DT, true);

    Document.setConfigValue('perexGroupsRenderAsSelect', 3);

    checkElements(I, DTE, DT, false);
});

Scenario('Check Perex Groups Rendering Behavior in Gallery', async ({ I, DT, DTE , Document }) => {

    Document.setConfigValue('perexGroupsRenderAsSelect', 30);

    I.amOnPage('/admin/v9/apps/gallery/?dir=/images/gallery/test-vela-foto/&id=236');
    DTE.waitForEditor("galleryTable");
    checkGalleryElements(I, DTE, DT, true);
    DTE.cancel();

    await I.executeScript(function() {
        window.location.href="/admin/v9/apps/image-editor/?id=-1&dir=/images/apps&name=apps-monitor.jpg&showOnlyEditor=true";
    });
    DTE.waitForEditor("galleryTable");
    checkGalleryElements(I, DTE, DT, true);

    Document.setConfigValue('perexGroupsRenderAsSelect', 3);

    I.amOnPage('/admin/v9/apps/gallery/?dir=/images/gallery/test-vela-foto/&id=236');
    DTE.waitForEditor("galleryTable");
    checkGalleryElements(I, DTE, DT, false);
    DTE.cancel();

    await I.executeScript(function() {
        window.location.href="/admin/v9/apps/image-editor/?id=-1&dir=/images/apps&name=apps-monitor.jpg&showOnlyEditor=true";
    });
    DTE.waitForEditor("galleryTable");
    checkGalleryElements(I, DTE, DT, false);

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

    DT.filterContains('perexGroups','odni'); //PODNIKANIE

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

function checkGalleryElements(I, DTE, DT, shouldSeeCheckbox) {
    I.clickCss("#pills-dt-galleryTable-metadata-tab");

    if (shouldSeeCheckbox) {
        I.say('Check if I see checkbox in perex tab');
        I.dontSeeElement('#DTE_Field_editorFields-perexGroupsIds[multiple]');
        I.dontSeeElement('#DTE_Field_editorFields-perexGroupsIds');

        I.seeCheckboxIsChecked(locate(".DTE_Field_Name_editorFields\\.perexGroupsIds div.form-switch").withText("podnikanie").find("input"));
        I.seeCheckboxIsChecked(locate(".DTE_Field_Name_editorFields\\.perexGroupsIds div.form-switch").withText("investícia").find("input"));
        I.dontSeeCheckboxIsChecked(locate(".DTE_Field_Name_editorFields\\.perexGroupsIds div.form-switch").withText("Newsletter perex skupina").find("input"));

        I.seeElement('#DTE_Field_editorFields-perexGroupsIds_0');
    } else {
        I.say('Check if I see multiselect in perex tab');
        I.seeElement('#DTE_Field_editorFields-perexGroupsIds[multiple]');
        I.seeElement('#DTE_Field_editorFields-perexGroupsIds');

        I.see("podnikanie", ".DTE_Field_Name_editorFields\\.perexGroupsIds div.bootstrap-select .filter-option .filter-option-inner-inner");
        I.see("investícia", ".DTE_Field_Name_editorFields\\.perexGroupsIds div.bootstrap-select .filter-option .filter-option-inner-inner");
        I.dontSee("Newsletter perex skupina", ".DTE_Field_Name_editorFields\\.perexGroupsIds div.bootstrap-select .filter-option .filter-option-inner-inner");

        I.dontSeeElement('#DTE_Field_editorFields-perexGroupsIds_0');
    }
}

Scenario('Delete language mutation perex', async ({ I, DT, DTE }) => {
    I.relogin('admin', true, true, 'sk');
    await deletePerex(I, DT, DTE, 'Skupina jazyková mutácia');
});

Scenario('Testing language mutation perex', ({ I, DT, DTE }) => {
    const perexGroupName = 'Skupina jazyková mutácia';
    const perexGroupNameSk = 'Jazyková mutácia';
    const perexGroupNameEn = 'Language mutation';

    I.amOnPage('/admin/v9/webpages/perex/');
    I.click(DT.btn.perex_add_button);
    I.waitForVisible('.DTE.modal-content');
    I.clickCss('#perexDataTable_modal button.btn.btn-primary');
    I.see('Chyba: niektoré polia neobsahujú správne hodnoty.');
    DTE.fillField('perexGroupName', perexGroupName);
    I.clickCss('#pills-dt-perexDataTable-translates-tab');
    DTE.fillField('perexGroupNameSk', perexGroupNameSk);
    DTE.fillField('perexGroupNameEn', perexGroupNameEn);
    I.clickCss('#perexDataTable_modal button.btn.btn-primary');
    DTE.waitForLoader();

    checkPerex(I, DT, perexGroupNameSk, true, 'Nenašli sa žiadne vyhovujúce záznamy');

    //
    I.say("Verify on admin it's still main title not translated");
    I.relogin('admin', true, true, 'en');
    I.amOnPage('/admin/v9/webpages/perex/');
    checkPerex(I, DT, perexGroupName, true, 'Nenašli sa žiadne vyhovujúce záznamy');

    I.relogin('admin', true, true, 'cs');
    I.amOnPage('/admin/v9/webpages/perex/');
    checkPerex(I, DT, perexGroupName, true, 'Nenašli sa žiadne vyhovujúce záznamy');
});

Scenario('Testing language mutation perex-logout', ({ I }) => {
    //logout because of the language mutation in previous test
    I.logout();
});

const perexName = 'Import perex';
Scenario('Before - Web Page Import and Perex validation', async ({ I, DT, DTE }) => {
    I.relogin('admin', true, true, 'sk');
    await deletePerex(I, DT, DTE, perexName);
});

Scenario('Web Page Import and Perex validation', async ({ I, DT, DTE }) => {
    const webPageName = 'Test import perex';

    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.jstreeNavigate(['Test stavov', webPageName]);
    I.forceClick(DT.btn.import_export_button)

    I.switchToNextTab();
    I.waitForVisible('.wjDialogHeaderTable');
    I.clickCss('#type3');
    I.clickCss('#btnOk');
    I.attachFile('input[name="archive"]', 'tests/webpages/'+I.getDefaultDomainName()+'-test_import_perex.zip');

    //SIVAN - without resize window, the checkOption is not working
    I.resizeWindow(1920, 1080);

    I.wait(1);
    I.clickCss('#btnOk');

    I.waitForText("Vybrané priečinky", 10);

    //I.checkOption crashed the window...
    I.forceClick(`//tr[td/strong[contains(text(), "/Test stavov/${webPageName}")]]//input[@type="checkbox"]`);
    I.forceClick(`//tr[td[contains(text(), "${webPageName}")]]//input[@type="checkbox"]`);

    I.clickCss('#btnOk');
    I.waitForVisible('#btnCancel', 60);
    I.clickCss('#btnCancel');
    I.switchToNextTab()

    I.say("Verify that perex was created");
    I.amOnPage('/admin/v9/webpages/perex/');
    checkPerex(I, DT, perexName, true, 'Nenašli sa žiadne vyhovujúce záznamy');

    I.say("Verify that perex is in web page and is checked");
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.jstreeNavigate(['Test stavov', webPageName]);

    I.clickCss("button.buttons-select-all");
    I.click(DT.btn.edit_button);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");

    I.seeElement( locate(".DTE_Field_Name_perexGroups").find( locate("label").withText("Import perex") ) );
    I.seeCheckboxIsChecked(locate(".DTE_Field_Name_perexGroups div.form-switch").withText("Import perex").find("input"));
});

/**
 * Deletes a specified perex entry from the perex administration page if it exists.
 *
 * @param {Object} I
 * @param {Object} DT
 * @param {Object} DTE
 * @param {string} perexName - The name of the perex to be deleted.
 */
async function deletePerex(I, DT, DTE, perexName) {
    I.amOnPage('/admin/v9/webpages/perex/');

    DT.filterEquals('perexGroupName', perexName);
    const isPerexDeleted = await I.grabNumberOfVisibleElements('td.dt-empty');
    I.say("isPerexDeleted: " + isPerexDeleted);

    if (isPerexDeleted === 0) {
        I.clickCss('button.buttons-select-all');
        I.click(DT.btn.perex_delete_button);
        I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
        I.see(perexName, '.DTE.modal-content.DTE_Action_Remove');
        I.click('Zmazať', 'div.DTE_Action_Remove');
        DTE.waitForLoader();
    }
    I.see('Nenašli sa žiadne vyhovujúce záznamy');
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
function checkPerex(I, DT, name, shouldSee = true, message = 'Nenašli sa žiadne vyhovujúce záznamy') {
    DT.filterContains("perexGroupName", name);

    if(shouldSee) {
        I.see(name);
        I.dontSee(message);
    } else {
        I.dontSee(name);
        I.see(message);
    }
}

Scenario('Check language variants in news', ({ I, DTE }) => {
    I.amOnPage("/zo-sveta-financii/");
    I.see("PODNIKANIE", ".portfolio-item .tag");
    I.see("INVESTÍCIA", ".portfolio-item .tag");

    I.say("Verify custom fields - F = background color");
    I.seeCssPropertiesOnElements(".portfolio-item span.tag.c2", {
        "background-color": "#01A3E0"
    });
    I.seeInSource(    '<span class="tag c2" style="background-color: #01A3E0">podnikanie</span>');
    I.dontSeeInSource('<span class="tag c1" style="background-color: $perexGroup.fieldF">')

    //
    I.say("CZ version")
    I.amOnPage("/zo-sveta-financii/?language=cz");
    I.dontSee("PODNIKANIE", ".portfolio-item .tag");
    I.dontSee("INVESTÍCIA", ".portfolio-item .tag");
    I.see("PODNIKÁNÍ", ".portfolio-item .tag");
    I.see("INVESTICE", ".portfolio-item .tag");

    //
    I.say("HU version not set, show default")
    I.amOnPage("/zo-sveta-financii/?language=hu");
    I.see("PODNIKANIE", ".portfolio-item .tag");
    I.see("INVESTÍCIA", ".portfolio-item .tag");
});

Scenario('Verify domain_id groups', ({ I, DT, Document }) => {
    /*
    INSERT INTO `perex_groups` (`perex_group_name`, `available_groups`, `perex_group_name_sk`, `perex_group_name_cz`, `perex_group_name_en`, `perex_group_name_de`, `perex_group_name_pl`, `perex_group_name_ru`, `perex_group_name_hu`, `perex_group_name_cho`, `perex_group_name_esp`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `domain_id`)
    VALUES
        ('DOMAIN: test23', '', '', '', '', '', '', '', '', '', '', '', 'false', '', '', '', '', 83),
        ('DOMAIN: demo.webjetcms.sk', '', '', '', '', '', '', '', '', '', '', '', 'false', '', '', '', '', 1);

    INSERT INTO `perex_groups` (`perex_group_name`, `available_groups`, `perex_group_name_sk`, `perex_group_name_cz`, `perex_group_name_en`, `perex_group_name_de`, `perex_group_name_pl`, `perex_group_name_ru`, `perex_group_name_hu`, `perex_group_name_cho`, `perex_group_name_esp`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`)
    VALUES
        ('DOMAIN: test23', '83', '', '', '', '', '', '', '', '', '', '', 'false', '', '', '', ''),
        ('DOMAIN: demo.webjetcms.sk', '1', '', '', '', '', '', '', '', '', '', '', 'false', '', '', '', '');
    */
    var demoName = "DOMAIN: demo.webjetcms.sk";
    var test23Name = "DOMAIN: test23";
    var selector = "#perexDataTable td.dt-row-edit";

    I.amOnPage("/admin/v9/webpages/perex/");
    DT.filterContains("perexGroupName", "DOMAIN:");
    I.dontSee(test23Name, selector);
    I.see(demoName, selector);

    Document.switchDomain('test23.tau27.iway.sk');
    DT.filterContains("perexGroupName", "DOMAIN:");
    I.see(test23Name, selector);
    I.dontSee(demoName, selector);
});

Scenario('Verify domain_id groups-logout', ({ I }) => {
    I.logout();
});

Scenario("BUG: duplicate available groups in perex group on save", async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/perex/");

    var perexGroupName = "PerexWithGroup_A";

    DT.filterContains("perexGroupName", perexGroupName);
    I.click(perexGroupName);
    DTE.waitForEditor("perexDataTable");
    I.click("button.btn-vue-jstree-add");
    I.waitForElement("#jsTree");
    I.waitForText("Jet portal 4", 5, "#jsTree a.jstree-anchor");
    I.click("a.close-custom-modal");
    DTE.save();

    //verify no duplicates
    I.click(perexGroupName);
    DTE.waitForEditor("perexDataTable");
    var rows = await I.grabNumberOfVisibleElements("#editorAppDTE_Field_editorFields-availableGroups div.dt-tree-container div.form-group button.btn-vue-jstree-item-edit");
    I.assertEqual(rows, 1, "Expected 1 available group, but found " + rows);
});

Scenario("Feature: perexGroups with same names are distinquised by id perexGroupName", ({ I, DTE, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=150903");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible(".DTE_Field_Name_perexGroups");

    checkPerexGroups(true, I);

    //
    I.say("Checking perex groups in news app");
    I.clickCss("#pills-dt-datatableInit-content-tab");
    Apps.openAppEditor();
    I.waitForElement("#pills-dt-component-datatable-perex-tab", 10);
    I.clickCss("#pills-dt-component-datatable-perex-tab");

    checkPerexGroups(false, I, ".DTE_Field_Name_perexGroup");
    checkPerexGroups(false, I, ".DTE_Field_Name_perexGroupNot");

    Apps.cancel();
    I.switchTo();
    DTE.cancel();

    //
    I.say("Checking perex groups in gallery app");
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test-vela-foto/&id=236");
    DTE.waitForEditor("galleryTable");
    I.clickCss("#pills-dt-galleryTable-metadata-tab");
    I.waitForVisible(".DTE_Field_Name_editorFields\\.perexGroupsIds");

    checkPerexGroups(false, I, ".DTE_Field_Name_editorFields\\.perexGroupsIds");

    DTE.cancel();
});

function checkPerexGroups(isGroupIdFiltered, I, parentSelector = ".DTE_Field_Name_perexGroups") {
    checkPerexEditor(I, 624, "PerexWithoutGroup", parentSelector);
    if (isGroupIdFiltered) I.dontSee("Newsletter perex skupina", parentSelector);
    else checkPerexEditor(I, 10, "Newsletter perex skupina", parentSelector);

    I.say("Z_Duplicity-Unique show no id NOR perexGroupName, because returned string is unique");
    checkPerexEditor(I, 1968, "Z_Duplicity-Unique", parentSelector);

    I.say("Z_DUPLICITY show only id, because lng text and perexGroupName are same, so not logic to add it");
    checkPerexEditor(I, 1969, "Z_DUPLICITY (1969)", parentSelector);

    I.say("Z_Duplicity-a show id AND perexGroupName, because this values are different, so for better distinquise.");
    checkPerexEditor(I, 1970, "Z_Dupľičity (1970:Z_Duplicity-A)", parentSelector);

    I.say("Z_Duplicity-a same as Z_Duplicity-b");
    checkPerexEditor(I, 1971, "Z_Duplicity (1971:Z_Duplicity-b)", parentSelector);
}

function checkPerexEditor(I, id, name, parentSelector) {
    I.seeElement( locate( locate(parentSelector + " div.custom-control.form-switch").withChild( locate("input.form-check-input[value='" + id + "']") ).find( locate("label.form-check-label").withText(name) ) ) );
}
