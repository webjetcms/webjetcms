Feature('a11y.positive');

Before(({ I, login }) => {
    login('admin');
});

/**
 * check scenario without any accessibility issues, to verify the test setup is correct and not reporting false positives
 */
Scenario('positive scenario', async ({ I, a11y }) => {
    I.amOnPage("/components/aceintegration/admin/a11y.html");
    await a11y.check();
});

Scenario("verify a11y reports not attached to basic bug", async ({ I }) => {
    I.amOnPage("/admin/v9/");
    //I.see("hahaha");
});
