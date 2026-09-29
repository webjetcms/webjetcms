Feature("admin.single-session");

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
    var introText = "Vitajte späť, Tester2 Playwright2";

    session('first user', () => {
        I.amOnPage("/admin/v9/");
        I.relogin("tester2");
        I.see(introText, ".md-dashboard__greeting");
    });

    session('second user', () => {
        if ("tester3"===secondUserName) introText = "Vitajte späť, Tester_L2 Playwright";
        I.relogin(secondUserName);
        I.amOnPage("/admin/v9/");
        I.see(introText, ".md-dashboard__greeting");

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
    I.see("Vitajte späť, Tester Playwright", ".md-dashboard__greeting");
});

Scenario("reset settings @singlethread", async ({ I, Document }) => {
    Document.setConfigValue("sessionSingleLogon", false);
});

Scenario("active session list on dashboard @singlethread", async ({ I }) => {

    var introText = "Vitajte späť, Tester_L2 Playwright";
    let createdLogonTime;
    session('first user', async () => {
        I.amOnPage("/admin/v9/");
        I.relogin("tester3");
        I.see(introText, ".md-dashboard__greeting");
        I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
        createdLogonTime = await I.executeScript(() => {
            const { currentSessions } = document.querySelector('webjet-overview-dashboard').data;
            return currentSessions.userSessions.flatMap(cluster => cluster.userSessions)
                .find(item => item.sessionId === currentSessions.currentSessionId)?.logonTime;
        });
        I.assertTrue(Number(createdLogonTime) > 0, 'The first test context must identify its own session.');
    });

    session('second user', async () => {
        I.relogin("tester3");
        I.amOnPage("/admin/v9/");
        I.see(introText, ".md-dashboard__greeting");

        // Remove only the session created by the first test context.
        const target = `.md-dashboard__sessions [data-session-logon="${createdLogonTime}"] .md-dashboard-widget__session-logout`;
        I.waitForVisible(target, 20);
        I.assertEqual(await I.grabNumberOfVisibleElements(target), 1, 'The session action must target only the context created by this test.');
        I.clickCss(target);
        I.waitForInvisible(target, 10);
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
