Feature('apps.export-dat');

Before(({ I, login }) => {
    login('admin');
});

// Plus overenie pridania adresara
Scenario('Export dat zakladne testy', async ({I, DataTables}) => {
    I.amOnPage("/apps/export-dat/admin/");
    await DataTables.baseTest({
        dataTable: 'exportDatDataTable',
        perms: 'cmp_export',
        createSteps: function(I, options) {
            I.click("#pills-dt-exportDatDataTable-filter-tab");
            I.click(".btn-vue-jstree-add");
            I.click("test");
            I.click("#pills-dt-exportDatDataTable-basic-tab");
        },
        editSteps: function(I, options) {
            I.click("#pills-dt-exportDatDataTable-filter-tab");
            I.see("/test");
            I.click("#pills-dt-exportDatDataTable-basic-tab");
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('Export dat json a XML', ({I, Browser}) => {

    if (Browser.isFirefox()) {
        I.say("Firefox, skipping test");
        return;
    }

    I.amOnPage("/export-dat/json/");
    I.see('"docLink": "/en/zo-sveta-financii/mcgregorov-obchodny-uder.html"');
    I.see('"fileName": "/English/News"');

    I.amOnPage("/export-dat/xml/");
    I.see("<title>RSS Feed</title>");
    I.see("<link>http://demotest.webjetcms.sk/en/zo-sveta-financii/mcgregorov-obchodny-uder.html</link>");
});

Scenario('Overenie filtrovania primitivneho boolean', ({I, DT}) => {
    I.amOnPage("/apps/export-dat/admin/");
    DT.filterSelect("expandGroupIds", "Áno");
    I.see("/export-dat/xml/")
    I.dontSee("/export-dat/json/")
});


function checkExportRowsAlert(I, maxRows) {
    I.amOnPage("/admin/v9/apps/audit-search/");
    I.click("button.btn-export-dialog");
    I.waitForElement("div.modal.DTED.show", 10);
    I.see("exportovaných je maximálne "+maxRows+" záznamov", "div.modal.show div.alert-info");
}

Scenario('Overenie nastavenia poctu riadkov na export', ({I, Document}) => {
    Document.setConfigValue("datatablesExportMaxRows", "80000");
    checkExportRowsAlert(I, 80000);

    Document.setConfigValue("datatablesExportMaxRows", "50000");
    checkExportRowsAlert(I, 50000);
});