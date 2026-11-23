Feature('webpages.template-url-stop-words @singlethread');

let suffix;
let templateName;
let originalRemove;
let folderCreated = false;
let templateCreated = false;

Before(({ I, login }) => {
    login('admin');
    if (!suffix) {
        suffix = I.getRandomText();
        templateName = 'autotest-url-template-' + suffix;
    }
});

Scenario('Create isolated template and folder', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/templates/temps-list/');
    DT.waitForLoader();
    originalRemove = await I.executeScript(() => window.Constants.urlRemoveSpojky);
    I.clickCss('#datatableInit_wrapper button.buttons-create');
    DTE.waitForEditor();
    DTE.fillField('tempName', templateName);
    DTE.fillField('forward', 'default/homepage.jsp');
    I.clickCss('#pills-dt-datatableInit-advanced-tab');
    I.seeInField('#DTE_Field_urlRemoveSpojkyMode', '0');
    DTE.save();
    templateCreated = true;

    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
    I.createFolderStructure(suffix, false);
    folderCreated = true;
    I.clickCss('div.tree-col button.buttons-edit');
    DTE.waitForEditor('groups-datatable');
    I.clickCss('#pills-dt-groups-datatable-template-tab');
    DTE.selectOption('tempId', templateName);
    DTE.save();
});

for (const globalRemove of [false, true]) {
    Scenario('Generate page URLs with global removal ' + globalRemove, async ({ I, DT, DTE, Document }) => {
        Document.setConfigValue('urlRemoveSpojky', String(globalRemove), true);
        const modes = ['Podľa konfiguračnej premennej urlRemoveSpojky', 'Odstrániť', 'Ponechať'];
        for (let mode = 0; mode < modes.length; mode++) {
            I.amOnPage('/admin/v9/templates/temps-list/');
            DT.filterEquals('tempName', templateName);
            I.click(templateName, '#datatableInit_wrapper');
            DTE.waitForEditor();
            I.clickCss('#pills-dt-datatableInit-advanced-tab');
            DTE.selectOption('urlRemoveSpojkyMode', modes[mode]);
            DTE.save();

            // Reload from persistence before using the template to generate a URL.
            I.amOnPage('/admin/v9/templates/temps-list/');
            DT.filterEquals('tempName', templateName);
            I.click(templateName, '#datatableInit_wrapper');
            DTE.waitForEditor();
            I.clickCss('#pills-dt-datatableInit-advanced-tab');
            I.seeInField('#DTE_Field_urlRemoveSpojkyMode', String(mode));
            if (globalRemove && mode === 2) I.saveScreenshot('autotest-template-url-stop-words.png');
            DTE.cancel();

            const pageTitle = 'autotest-url-' + suffix + '-' + globalRemove + '-' + mode + ' news a events';
            I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
            I.jstreeNavigate(['name-autotest-' + suffix]);
            I.clickCss('#datatableInit_wrapper button.buttons-create');
            DTE.waitForEditor();
            DTE.waitForCkeditor();
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            DTE.fillField('title', pageTitle);
            DTE.fillField('navbar', pageTitle);
            I.checkOption('#DTE_Field_generateUrlFromTitle_0');
            I.clickCss('#pills-dt-datatableInit-template-tab');
            I.see(templateName, '.DTE_Field_Name_tempId');
            DTE.save();

            I.click(pageTitle, '#datatableInit_wrapper');
            DTE.waitForEditor();
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            const url = await I.grabValueFrom('#DTE_Field_virtualPath');
            const remove = mode === 1 || (mode === 0 && globalRemove);
            const expected = pageTitle.replaceAll(' ', '-').toLowerCase().replace('-a-', remove ? '-' : '-a-') + '.html';
            I.assertEndsWith(url, '/' + expected, 'The saved URL must respect the template override.');
            DTE.cancel();
        }
    });
}

Scenario('Restore configuration and remove test records', ({ I, DT, Document }) => {
    if (originalRemove !== undefined) Document.setConfigValue('urlRemoveSpojky', String(originalRemove), true);
    if (folderCreated) I.deleteFolderStructure(suffix);
    if (templateCreated) {
        I.amOnPage('/admin/v9/templates/temps-list/');
        DT.filterEquals('tempName', templateName);
        DT.deleteAll();
    }
});

Scenario('Template settings require template permission', ({ I, DT }) => {
    I.amOnPage('/admin/v9/templates/temps-list/');
    DT.checkPerms('menuTemplates', '/admin/v9/templates/temps-list/');
});
