Feature('a11y.dashboard');

Before(({ I, login }) => {
    login('admin');
});

Scenario('dashboard', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');
    await a11y.check();
});