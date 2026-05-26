Feature('a11y.restaurant-menu');

Before(({ I, login }) => {
    login('admin');
});

Scenario('p33: restaurant-menu - week selection', async ({ I, a11y }) => {
    I.amOnPage('/apps/restaurant-menu/admin/');
    await a11y.check();
});
