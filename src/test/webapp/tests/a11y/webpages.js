Feature('a11y.webpages');

Before(({ I, login }) => {
    login('admin');
});

Scenario('jstree settings', async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.click("button.buttons-jstree-settings");
    I.waitForElement("#jstreeSettingsModal", 5);
    await a11y.check();
});