Feature('webpages.editor-http-headers @singlethread');

const output = '#datatableInit_modal .webpage-http-headers pre';
let suffix;
let originalRemove;
let folderCreated = false;

Before(({ I, login }) => {
    login('admin');
    if (!suffix) suffix = I.getRandomText();
});

Scenario('preview loads on Basic and follows unsaved settings', async ({ I, DTE }) => {
    I.resizeWindow(1400, 1200);
    I.executeScript(() => localStorage.removeItem('translate'));
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=25');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-content-tab');
    DTE.cancel();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=25');
    DTE.waitForEditor();
    I.assertEqual(await I.executeScript(() => performance.getEntriesByType('resource').filter(item => item.name.includes('/http-headers')).length), 0, 'Content must not request headers');
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.waitForVisible('#DTE_Field_searchable_1', 10);
    I.checkOption('#DTE_Field_searchable_0');
    I.selectOption('#DTE_Field_followLinks', 'Podľa nastavenia Prehľadávať (áno = follow, nie = nofollow)');
    I.waitForText('X-Robots-Tag: all', 10, output);
    I.checkOption('#DTE_Field_searchable_1');
    I.waitForText('X-Robots-Tag: noindex, nofollow', 10, output);
    I.selectOption('#DTE_Field_followLinks', 'Povoliť nasledovanie odkazov (follow)');
    I.waitForText('X-Robots-Tag: noindex', 10, output);
    I.assertEqual((await I.grabTextFrom(output)).split('\n')[0], 'X-Robots-Tag: noindex', 'Follow must remove nofollow');
    I.selectOption('#DTE_Field_followLinks', 'Zakázať nasledovanie odkazov (nofollow)');
    I.waitForText('X-Robots-Tag: noindex, nofollow', 10, output);
    I.checkOption('#DTE_Field_searchable_0');
    I.selectOption('#DTE_Field_followLinks', 'Zakázať nasledovanie odkazov (nofollow)');
    I.waitForText('X-Robots-Tag: nofollow', 10, output);
    I.saveScreenshot('autotest-http-headers.png');
    I.selectOption('#DTE_Field_followLinks', 'Povoliť nasledovanie odkazov (follow)');
    I.waitForText('X-Robots-Tag: all', 10, output);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.waitForText('X-Robots-Tag: all', 10, output);
    I.seeNumberOfElements('#datatableInit_modal .webpage-http-headers', 1);
    I.see('Referrer-Policy:', output);
    I.dontSee('Location:', output);
    I.fillField('#DTE_Field_externalLink', '/autotest-redirect?source=test#section');
    I.clickCss('#DTE_Field_navbar');
    I.waitForText('Location: http://demo.webjetcms.sk/autotest-redirect?source=test#section', 10, output);
    I.fillField('#DTE_Field_externalLink', 'https://example.com/autotest-redirect');
    I.clickCss('#DTE_Field_navbar');
    I.waitForText('Location: https://example.com/autotest-redirect', 10, output);
    I.waitForText('X-Robots-Tag: neodosiela sa', 10, output);
    I.saveScreenshot('autotest-http-headers-redirect.png');
    I.fillField('#DTE_Field_externalLink', '');
    I.clickCss('#DTE_Field_navbar');
    I.waitForText('X-Robots-Tag: all', 10, output);
    I.dontSee('Location:', output);
    I.amAcceptingPopups();
    DTE.cancel();
});

Scenario('create template and exact-path header fixtures', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/templates/temps-list/');
    DT.waitForLoader();
    originalRemove = await I.executeScript(() => window.Constants.urlRemoveSpojky);
    for (const [name, mode] of [['keep', 'Ponechať'], ['remove', 'Odstrániť']]) {
        I.clickCss('#datatableInit_wrapper button.buttons-create');
        DTE.waitForEditor();
        DTE.fillField('tempName', 'autotest-http-headers-' + suffix + '-' + name);
        DTE.fillField('forward', 'default/homepage.jsp');
        I.clickCss('#pills-dt-datatableInit-advanced-tab');
        DTE.selectOption('urlRemoveSpojkyMode', mode);
        DTE.save();
    }

    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
    I.createFolderStructure(suffix, false);
    folderCreated = true;

    I.amOnPage('/apps/response-header/admin/');
    DT.waitForLoader('responseHeadersDataTable');
    const path = '/name-autotest-' + suffix + '/autotest-http-headers-' + suffix;
    for (const [ending, policy] of [['-news-a-events.html', 'no-referrer'], ['-news-events.html', 'same-origin']]) {
        I.clickCss('#responseHeadersDataTable_wrapper button.buttons-create');
        DTE.waitForEditor('responseHeadersDataTable');
        DTE.fillField('url', '^' + path + ending + '$');
        DTE.fillField('headerName', 'Referrer-Policy');
        DTE.fillField('headerValue', policy);
        DTE.save('responseHeadersDataTable');
    }
});

for (const globalRemove of [true, false]) {
    Scenario('preview uses unsaved template with global removal ' + globalRemove, ({ I, DTE, Document }) => {
        Document.setConfigValue('urlRemoveSpojky', String(globalRemove), true);
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
        I.jstreeNavigate(['name-autotest-' + suffix]);
        I.clickCss('#datatableInit_wrapper button.buttons-create');
        DTE.waitForEditor();
        DTE.waitForCkeditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        const title = 'autotest-http-headers-' + suffix + ' news a events';
        DTE.fillField('title', title);
        DTE.fillField('navbar', title);
        I.checkOption('#DTE_Field_generateUrlFromTitle_0');

        for (const [name, policy] of [['remove', 'same-origin'], ['keep', 'no-referrer'], ['remove', 'same-origin']]) {
            I.clickCss('#pills-dt-datatableInit-template-tab');
            DTE.selectOption('tempId', 'autotest-http-headers-' + suffix + '-' + name);
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            I.waitForText('Referrer-Policy: ' + policy, 10, output);
        }
        I.amAcceptingPopups();
        DTE.cancel();
    });
}

Scenario('restore configuration and remove header preview fixtures', ({ I, DT, Document }) => {
    if (originalRemove !== undefined) Document.setConfigValue('urlRemoveSpojky', String(originalRemove), true);
    if (folderCreated) I.deleteFolderStructure(suffix);
    I.amOnPage('/apps/response-header/admin/');
    DT.waitForLoader('responseHeadersDataTable');
    DT.filterContains('url', 'autotest-http-headers-' + suffix);
    DT.deleteAll('responseHeadersDataTable');
    I.amOnPage('/admin/v9/templates/temps-list/');
    DT.waitForLoader();
    DT.filterContains('tempName', 'autotest-http-headers-' + suffix);
    DT.deleteAll();
});
