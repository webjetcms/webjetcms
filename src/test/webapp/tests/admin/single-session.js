Feature("apps.user.single-session");

var logoffLocator = "#toast-container-logoff";

Before(({ login }) => {
    login('admin');
});

/**
 * Check multiple sessions for same user logon
 * @param {*} I
 * @param {*} sessionSingleLogon
 */
function checkMultiUserLogon(I, sessionSingleLogon=false, secondUserName="tester2") {
    var introText = "Vitajte, Tester2 Playwright2";

    session('first user', () => {
        I.amOnPage("/admin/v9/");
        I.relogin("tester2");
        I.see(introText, ".overview__dashboard__title h2");
    });

    session('second user', () => {
        if ("tester3"===secondUserName) introText = "Vitajte, Tester_L2 Playwright";
        I.relogin(secondUserName);
        I.amOnPage("/admin/v9/");
        I.see(introText, ".overview__dashboard__title h2");

        I.wait(10);
        I.dontSeeElement(logoffLocator);
    });

    session('first user', () => {
        if (sessionSingleLogon===true) I.seeElement(logoffLocator);
        else I.dontSeeElement(logoffLocator);

        I.logout();
    });

    session('second user', () => {
        //logoff second user session
        I.logout();
    });

    I.switchTo();
}

Scenario("overenie single session @singlethread", async ({ I, Document }) => {
    //
    I.say("test 2 same users - first one should be logged off");
    Document.setConfigValue("sessionSingleLogon", true);
    checkMultiUserLogon(I, true);

    //
    I.say("test 2 different users - both should stay logged in");
    checkMultiUserLogon(I, false, "tester3");

    //
    I.say("disable single session - both should stay logged in");
    Document.setConfigValue("sessionSingleLogon", false);
    checkMultiUserLogon(I, false);

    //
    I.say("main user should still be logged in");
    I.amOnPage("/admin/v9/");
    I.see("Vitajte, Tester Playwright", ".overview__dashboard__title h2");
});

Scenario("reset settings @singlethread", async ({ I, Document }) => {
    Document.setConfigValue("sessionSingleLogon", false);
});

Scenario("active session list on dashboard @singlethread", async ({ I, Document }) => {

    var introText = "Vitajte, Tester_L2 Playwright";
    session('first user', () => {
        I.amOnPage("/admin/v9/");
        I.relogin("tester3");
        I.see(introText, ".overview__dashboard__title h2");
    });

    session('second user', () => {
        I.relogin("tester3");
        I.amOnPage("/admin/v9/");
        I.see(introText, ".overview__dashboard__title h2");

        //logout first user session
        I.click(locate("#webjet-overview-dashboard div.active-sessions li button"));
    });

    session('first user', () => {
        I.waitForElement(logoffLocator, 30);
        I.logout();
    });

    session('second user', () => {
        I.dontSeeElement(logoffLocator);
        I.logout();
    });

});