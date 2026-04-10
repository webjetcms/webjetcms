Feature('a11y.elfinder');

Before(({ I, login }) => {
    login('admin');
});

Scenario('p26: elfinder dialog', async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/files/index/");
    I.waitForElement("#nav-iwcm_2_", 20);

    await a11y.check();
});