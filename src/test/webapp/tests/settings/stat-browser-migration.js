Feature('settings.stat-browser-migration');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/settings/stat-browser-migration/');
});

Scenario('shows migration preview without changing data', ({ I }) => {
    I.seeElement('#migrationAnalyze');
    I.seeElement('#migrationStart');
    I.click('#migrationAnalyze');
    I.waitForElement('#migrationPreview:not(.d-none)', 20);
    I.seeElement('#migrationMappings');
});

Scenario('checks permissions', ({ I }) => {
    I.amOnPage('/admin/v9/settings/stat-browser-migration/?removePerm=modUpdate');
    I.see('Na túto aplikáciu/funkciu nemáte prístupové práva');
});

Scenario('logout', ({ I }) => {
    I.logout();
});
