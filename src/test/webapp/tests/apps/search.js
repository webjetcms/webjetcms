Feature('apps.vyhladavanie');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Vyhľadávanie - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/vyhladavanie/");

    I.waitForElement("#searchWords");
    I.fillField('#searchWords', 'Kontakt');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Približný počet výsledkov", 10);

    I.fillField('#searchWords', 'Nothing');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Časový limit medzi dvoma hľadaniami je 10 sekúnd. Počkajte a skúste vyhľadanie neskôr.", 10);

    I.wait(11);
    I.fillField('#searchWords', 'Nothing');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Neboli nájdené žiadne stránky vyhovujúce zadaným kritériám.", 10);
});

Scenario('testovanie app - Vyhladavanie', async ({ I, DTE, Apps }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=77875');
    DTE.waitForEditor();
    I.wait(5);

    const defaultParams = {
        rootGroup: '1+15257',
        perpage: '10',
        orderType: 'sortPriority',
        order: 'asc',
        sForm: 'complete'
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.wait(5);
    I.switchToNextTab();

    I.waitForElement('#searchWords', 10);
    I.fillField('#searchWords', 'Kontakt');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Približný počet výsledkov: 1", 10);

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        rootGroup: '15257',
        perpage: '2',
        orderType: 'title',
        order: 'desc',
        sForm: 'form'
    };

    I.click(locate('.btn-vue-jstree-item-remove').first());
    DTE.fillField('perpage', changedParams.perpage);
    DTE.clickSwitch('checkDuplicity_0');
    DTE.selectOption('orderType','Názvu');
    DTE.clickSwitch('order_1');
    DTE.clickSwitch('sForm_0');
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    DTE.save()
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=77875');
    DTE.waitForEditor();

    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.waitForElement('#searchWords', 10);
    I.fillField('#searchWords', 'Kontakt');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForElement('#searchWords', 10);
    I.dontSee("Približný počet výsledkov: 1");

});

Scenario('Revert changes', ({ I, DTE, Apps }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=77875');
    DTE.waitForEditor();
    Apps.clearPageContent();
    Apps.switchEditor('html');
    const snippet = '!INCLUDE(/components/search/search.jsp, rootGroup=1+15257, perpage=10, checkDuplicity=false, orderType=sortPriority, order=asc, sForm=complete)!'
    I.clickCss(".CodeMirror-code");
    I.fillField(".CodeMirror-code", snippet);
    DTE.save();
});