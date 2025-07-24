Feature('webpages.mirroring');

const groupTable = "groupsMirroringTable";
const docTable = "docsMirroringTable";
const confTable = "mirrroringConfDataTable";

const groupsWrapperSelector = "#groupsMirroringTable_wrapper";
const docsWrapperSelector = "#docsMirroringTable_wrapper";

Before(({ login }) => {
    login('admin');
});

Scenario('Mirroring screens', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/mirroring/");
    Document.switchDomain("mirroring.tau27.iway.sk");

    I.resizeWindow(1550, 700);

    DT.filterContains("fieldA", "mirroring_scr", groupsWrapperSelector);

    Document.screenshot("/redactor/webpages/mirroring/groups_datatable.png");

    DT.filterEquals("fieldA", "/sk_mirroring_scr/child_A /sk_mirroring_scr_2", groupsWrapperSelector);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor(groupTable);

    Document.screenshotElement(".DTE.DTE_Action_Edit.modal-content", "/redactor/webpages/mirroring/groups_editor_A.png");

    I.clickCss("#add-sync-btn");
    I.clickCss("#add-sync-btn");
    I.clickCss("#add-sync-btn");

    Document.screenshotElement(".DTE.DTE_Action_Edit.modal-content", "/redactor/webpages/mirroring/groups_editor_B.png");

    I.amOnPage("/admin/v9/webpages/mirroring/");
    I.clickCss("#pills-docs-tab");
    DT.waitForLoader();

    DT.filterContains("fieldA", "mirroring_scr", docsWrapperSelector);

    Document.screenshot("/redactor/webpages/mirroring/docs_datatable.png");

    DT.filterEquals("fieldA", "/sk_mirroring_scr/child_A/child_A /sk_mirroring_scr/child_A/child_A_2", docsWrapperSelector);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor(docTable);

    Document.screenshotElement(".DTE.DTE_Action_Edit.modal-content", "/redactor/webpages/mirroring/doc_editor_A.png");

    I.clickCss("#add-sync-btn");
    I.clickCss("#add-sync-btn");
    I.clickCss("#add-sync-btn");

    Document.screenshotElement(".DTE.DTE_Action_Edit.modal-content", "/redactor/webpages/mirroring/doc_editor_B.png");

    I.amOnPage("/admin/v9/webpages/mirroring/");
    I.clickCss("#pills-settings-tab");
    DT.waitForLoader();

    Document.screenshot("/redactor/webpages/mirroring/settings_datatable.png");
});