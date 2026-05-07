Feature('a11y.layout');

Before(({ I, login }) => {
    login('admin');
});

Scenario('p35: headings', async ({ I, a11y }) => {
    I.amOnPage('/apps/stat/admin/');
    I.wait(1);
    //TODO: div.header-title should be H1
    await a11y.check();
});

Scenario('p37: ly-content-wrapper section should be in main tag', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');
    //TODO: main section should be in main tag
    await a11y.check();
});

Scenario('p39: contrast between default vs hover vs focus', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/settings/redirect/');
    //TODO: check contrast between default vs hover vs focus on buttons and links
    await a11y.check();
});
