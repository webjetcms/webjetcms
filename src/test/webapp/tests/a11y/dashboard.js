Feature('a11y.dashboard');

Before(({ I, login }) => {
    login('admin');
});

Scenario('positive scenario', async ({ I, a11y }) => {
    I.amOnPage("/components/aceintegration/admin/a11y.html");
    await a11y.check();
});

Scenario('dashboard', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');
    await a11y.check();
});