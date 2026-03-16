Feature('a11y.datatable');

Before(({ I, login }) => {
    login('admin');
});

Scenario('basic datatable', async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    await a11y.check();
    I.amOnPage("/apps/news/admin/");
    await a11y.check();
    I.amOnPage("/admin/v9/users/user-list/");
    await a11y.check();
    I.amOnPage("/apps/basket/admin/");
    await a11y.check();
});

Scenario('filter', async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContains("tempName", "page");
    await a11y.check();
});

Scenario('editor - with error messages', async ({ I, DT, DTE, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.save();
    I.pressKey("Escape");
    await a11y.check();
});