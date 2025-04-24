Feature('apps.pribuzne-stranky');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Príbuzné stránky - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/pribuzne-stranky/");
    I.waitForElement(locate('b').withText('Prečítajte si aj'));
    I.click(locate('a').withText('KONSOLIDÁCIA NAPRIEČ TRHMI'));
    I.seeElement(locate('h1').withText('Konsolidácia naprieč trhmi'));
    I.amOnPage("/apps/pribuzne-stranky/");
    I.click(locate('a').withText('MCGREGOROV OBCHODNÝ ÚDERA'));
    I.seeElement(locate('h1').withText('McGregorov obchodný údera'));
});

Scenario('testovanie app - Príbuzné stránky', async ({ I, DTE, Apps, Document }) => {
    Apps.insertApp('Príbuzné stránky', '#components-related-pages-title', null, false);

    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    I.clickCss("button.btn-vue-jstree-add");
    I.click(locate('.jstree-node.jstree-closed').withText('Jet portal 4').find('.jstree-icon.jstree-ocl'));
    I.click(locate('a').withText('Zo sveta financií').withAttr({ role: 'treeitem' }));
    I.waitForElement('input[value="/Jet portal 4/Zo sveta financií"]', 10);
    DTE.clickSwitchLabel("ďalšia perex skupina");
    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const defaultParams = {
        rootGroups: "24",
        rGroupsRecursive: "false",
        titleName: "groupName",
        pagesInGroup: "10",
        groups: "3"
    };
    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.seeElement(locate('b').withText('Zo sveta financií'));
    I.seeElement(locate('a').withText('ČÍM JE ČLOVEK BOHATŠÍ, TÝM MÁ MENEJ HOTOVOSTI'));
    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        rootGroups: "1",
        rGroupsRecursive: "true",
        titleName: "Moje stránky",
        pagesInGroup: "5",
        groups: "3+1"
    };
    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('a').withText('Jet portal 4').withAttr({ role: 'treeitem' }));
    I.waitForElement('input[value="/Jet portal 4"]', 10);
    DTE.clickSwitchLabel("Prehľadať aj podadresáre");
    I.clickCss("#DTE_Field_titleType_2");
    DTE.fillField("titleName", "Moje stránky");
    DTE.fillField("pagesInGroup", "5");
    DTE.clickSwitchLabel("investícia");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.seeElement(locate('b').withText('Moje stránky'));
    I.seeElement(locate('a').withText('ČÍM JE ČLOVEK BOHATŠÍ, TÝM MÁ MENEJ HOTOVOSTI'));
    I.seeElement(locate('a').withText('GRAF TÝŽDŇA: SVETOVÉ AKCIOVÉ INDEXY MAJÚ VYŠŠÍ PODIEL KLESAJÚCICH AKTÍV'));
});