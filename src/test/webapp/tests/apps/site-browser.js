Feature('apps.site-browser');

Before(({ I, login }) => {
    login('admin');
});

var container = "div.site_browse";

function testRootFolder(I) {
    I.waitForText("Adresár: / Test stavov / Site browser", 10, container);
    I.see("Subfolder1", container);
    I.see("Subfolder2", container);
    I.see("download.png", container);
    I.see("pb-responsive.png", container);
}

Scenario('site-browser-webpage', ({I}) => {
    I.logout();

    I.amOnPage('/apps/site-browser/');
    testRootFolder(I);

    I.click("Subfolder1", container);
    I.waitForText("Adresár: / Test stavov / Site browser / Subfolder1", 10, container);
    I.dontSee("Subfolder2", container);
    I.dontSee("download.png", container);
    I.see("browser.png", container);

    I.amOnPage("/apps/site-browser/?actualDir=/files/test-stavov/site-browser/subfolder2");
    I.waitForText("Adresár: / Test stavov / Site browser / Subfolder2", 10, container);
    I.dontSee("Subfolder1", container);
    I.dontSee("download.png", container);
    I.see("autotest.png", container);

    I.click(locate(container+" a.siteBrowserClick").withText(".."));
    testRootFolder(I);

    I.say("Cant't go up from root folder");
    I.amOnPage("/apps/site-browser/?actualDir=/files/");
    testRootFolder(I);

    I.say("Cant't spoof URL");
    I.amOnPage('/apps/site-browser/?actualDir=/files/test-stavov/site-browser/../../');
    testRootFolder(I);
});

Scenario('testovanie app - Zobrazenie súborov', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Zobrazenie súborov', '#components-site_browser-title');

    const defaultParams = {
        rootDir: '',
        target: '_blank',
        showActualDir: 'true'
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see('Adresár: /');
    I.see('Apps');

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        rootDir: '/files/archiv',
        target: '_self',
        showActualDir: 'false'
    };
    I.clickCss('button.btn-vue-jstree-item-edit');
    I.click(locate(".jstree-anchor").withText("archiv"));
    //I.click('apps','.jstree-anchor');
    DTE.fillField('target', changedParams.target);
    DTE.clickSwitch('showActualDir_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.dontSee('Adresár:');
    I.see('tarifa.pdf');
    I.see("zsd_faq_fakturacia-poplatkov-od-2014.pdf");
    I.see('Files');
});