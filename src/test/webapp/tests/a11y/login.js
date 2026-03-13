Feature('a11y.login');

Scenario('logon page check', async ({ I, a11y }) => {
    I.logout();
    I.amOnPage('/admin/');
    await a11y.check();
});
