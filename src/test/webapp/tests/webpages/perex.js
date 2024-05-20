Feature('webpages.perex');

Before(({ I, login }) => {
    login('admin');
});

Scenario('perex-zakladne testy', async ({I, DataTables }) => {
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