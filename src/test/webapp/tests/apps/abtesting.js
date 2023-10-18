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

    I.click('.dt-filter-id');
    I.waitForText('Záznamy 1 až 1 z 1', 10);

    //povol zobrazenie
    I.click(pageTitle);
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
    I.dontSeeInField("#DTE_Field_virtualPath", "abtestvariantb.html");
    DTE.save();

    //vytvor B verziu
    I.click("button.buttons-abtest");

    I.click("Potvrdiť", "div.toastr-buttons");
    I.waitForText("Záznamy 1 až 2 z 2", 10);
    I.see("Záznamy 1 až 2 z 2");

    DT.filterSelect("editorFields.statusIcons", "B variant stránky");

    I.waitForText('Záznamy 1 až 1 z 1', 10);

    I.click(pageTitle);
    DTE.waitForEditor();

    await DTE.fillCkeditor("<p>This is an autotest</p><p>"+bvariantText+"</p>");

    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "abtestvariantb.html");

    DTE.save();

});

Scenario('zobrazenie stranok @singlethread', async ({I, DT}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=30452");
    DT.waitForLoader();

    DT.filter("title", pageTitle);

    I.click("tr.odd a.preview-page-link");
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

    I.click('.dt-filter-id');

    I.see(pageTitle);

    I.wait(3);

    I.click("#datatableInit_wrapper button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(pageTitle);
});