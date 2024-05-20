Feature('apps.abtesting');

var randomNumber, pageTitle, bvariantText;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        pageTitle = "webPage-autotest-"+randomNumber;
        bvariantText = "Toto je B variant stranky "+pageTitle;
    }
});

Scenario('vytvorenie stranok @singlethread', async ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=30452");
    DT.waitForLoader();

    I.createNewWebPage(randomNumber);
    DT.filter("title", pageTitle);

    I.clickCss('.dt-filter-id');
    I.waitForText('Záznamy 1 až 1 z 1', 10);

    //povol zobrazenie
    I.click(pageTitle);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
    I.dontSeeInField("#DTE_Field_virtualPath", "abtestvariantb.html");
    DTE.save();

    //vytvor B verziu
    I.clickCss("button.buttons-abtest");

    I.click("Potvrdiť", "div.toastr-buttons");
    I.waitForText("Záznamy 1 až 2 z 2", 10);
    I.see("Záznamy 1 až 2 z 2");

    DT.filterSelect("editorFields.statusIcons", "B variant stránky");

    I.waitForText('Záznamy 1 až 1 z 1', 10);

    I.click(pageTitle);
    DTE.waitForEditor();

    await DTE.fillCkeditor("<p>This is an autotest</p><p>"+bvariantText+"</p>");

    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "abtestvariantb.html");

    DTE.save();

});

Scenario('zobrazenie stranok @singlethread', async ({I, DT}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=30452");
    DT.waitForLoader();

    DT.filter("title", pageTitle);

    I.clickCss("tr.odd a.preview-page-link");
    I.wait(2);

    I.switchToNextTab();
    I.see("This is an autotest");

    //odhlas sa
    I.switchToPreviousTab();
    I.logout();

    //reloadni web stranku
    I.switchToNextTab();
    I.refreshPage();

    I.seeCookie("wjabtesting");
    let cookie = await I.grabCookie("wjabtesting");
    I.say("AB test variant: "+cookie.value);
    I.refreshPage();
    I.see("This is an autotest");

    //nastav A variantu
    I.say("A variant");
    cookie.value = "a";
    await I.setCookie(cookie);
    I.refreshPage();
    I.see("This is an autotest");
    I.dontSee(bvariantText);

    //nastav b variantu
    I.say("B variant");
    cookie.value = "b";
    await I.setCookie(cookie);
    I.refreshPage();
    I.see("This is an autotest");
    I.see(bvariantText);

    //nageneruj nejaku statistiku
    let counter = Math.floor(Math.random() * 10) + 10;
    for (let i=0; i<counter; i++) {
        I.clearCookie("wjabtesting");
        I.amOnPage("/test-stavov/ab-testovanie/");
        cookie = await I.grabCookie("wjabtesting");
        I.say(i+". AB test variant: "+cookie.value);
    }

    I.closeCurrentTab();
});

Scenario('zmazanie stranok @singlethread', ({I, DT}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=30452");
    DT.waitForLoader();
    DT.filter("title", pageTitle);

    I.clickCss('.dt-filter-id');

    I.see(pageTitle);

    I.wait(3);

    I.clickCss("#datatableInit_wrapper button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(pageTitle);
});

Scenario('Test ABtesting page with webpages and with right values', ({ I, DT, DTE }) => {
    I.amOnPage("/apps/abtesting/admin/");
    DT.filter("title", "AB testovanie");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    DT.filter("title", "Produktová stránka");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    DT.filter("title", "Úvodná stránka");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    DT.filter("title", "AB testovanie");
    I.clickCss("td.sorting_1");

    I.clickCss("button.buttons-history-preview");
    I.switchToNextTab();
    I.see("A variant");
    I.closeCurrentTab();
});

Scenario('Test abtesting config page', ({ I, DT, DTE }) => {
    I.say('Check that table shows right confs');
    I.amOnPage("/apps/abtesting/admin/config/");
    I.see("ABTesting");
    I.see("ABTestingRatio");
    I.see("ABTestingName");
    I.see("ABTestingCookieName");
    I.see("ABTestingCookieDays");

    I.say('Check editor');
    DT.filter("name", "ABTesting");
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor("abtestingConfDataTable");

    I.say("Check that right field's are disabled");
    I.seeElement('#DTE_Field_name[disabled]');
    I.seeElement('#DTE_Field_oldValue[disabled]');
    I.seeElement('#DTE_Field_description[disabled]');
    I.seeElement('#DTE_Field_dateChanged[disabled]');
    DTE.cancel();

    I.say('Check th we dont see action buttons like create ...');
    I.dontSeeElement("button.buttons-create");
    I.dontSeeElement("button.buttons-remove");
    I.dontSeeElement("button.buttons-celledit");
    I.dontSeeElement("button.buttons-import");
    I.dontSeeElement("button.buttons-duplicate");
});