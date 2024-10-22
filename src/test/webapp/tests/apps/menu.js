Feature('apps.menu');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Menu', async ({ I, DTE, Apps }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=25209');
    DTE.waitForEditor();
    I.wait(5);
    
    const defaultParams = {
        rootGroupId: '15257',
        startOffset: '0',
        maxLevel: '-1',
        menuIncludePerex: 'false',
        classes: 'basic',
        generateEmptySpan: 'false',
        openAllItems: 'false',
        onlySetVariables: 'false',
        rootUlId: 'menutest',
        menuInfoDirName: ''

    };

    await Apps.assertParams(defaultParams);
    
    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.wait(5);
    I.switchToNextTab();

    I.see('Anketa');

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        rootGroupId: '1',
        startOffset: '-1',
        maxLevel: '-2',
        menuIncludePerex: 'true',
        classes: 'full',
        generateEmptySpan: 'true',
        openAllItems: 'true',
        onlySetVariables: 'false',
        rootUlId: 'menutest1',
        menuInfoDirName: 'name',
        menuIncludePerexLevel: '1'
    };
    
    I.clickCss('.btn-vue-jstree-item-edit');
    I.click('Jet portal 4','.jstree-anchor');
    DTE.fillField('startOffset', changedParams.startOffset);
    DTE.fillField('maxLevel', changedParams.maxLevel);
    Apps.switchToTabByIndex(1);
    DTE.selectOption('classes','Všetky');
    DTE.clickSwitch('generateEmptySpan_0');
    DTE.clickSwitch('openAllItems_0');
    DTE.fillField('rootUlId', changedParams.rootUlId);
    DTE.fillField('menuInfoDirName', changedParams.menuInfoDirName);
    DTE.clickSwitch('menuIncludePerex_0');
    DTE.fillField('menuIncludePerexLevel',changedParams.menuIncludePerexLevel);
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    
    await Apps.assertParams(changedParams);
    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.wait(2);
    I.switchToNextTab();
    I.dontSee('Anketa');
    I.see('Úvod');  
});
