Feature('settings.stat-browser-migration');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/settings/stat-browser-migration/');
});

Scenario('shows migration preview without changing data @screenshot', ({ I, Document }) => {
    I.usePlaywrightTo('mock migration in progress', async ({ page }) => {
        await page.route('**/admin/rest/settings/stat-browser-migration/status', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    tableIndex: 73,
                    totalTables: 175,
                    cursor: 6400,
                    tableMaxId: 10000,
                    scanned: 524830,
                    updated: 118742,
                    tableUpdated: 1840,
                    tableDurationMillis: 12345,
                    running: true,
                    stopRequested: false,
                    paused: false,
                    done: false,
                    table: 'stat_error_2024_2',
                    error: null
                })
            });
        });

        await page.route('**/admin/rest/settings/stat-browser-migration', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    seoBots: [
                        { sourceId: 11, targetId: 1, source: 'Googlebot 2.1', target: 'Googlebot' },
                        { sourceId: 12, targetId: 2, source: 'Bingbot 2.0', target: 'Bingbot' }
                    ],
                    browserKeys: [
                        { sourceId: 101, targetId: 10, source: 'Chrome 127.0', target: 'Chrome' },
                        { sourceId: 102, targetId: 20, source: 'Mobile Safari 17.4', target: 'Mobile Safari' }
                    ],
                    tables: Array.from({ length: 175 }, (_, index) => `stat_table_${index + 1}`)
                })
            });
        });
    });
    I.refreshPage();
    I.waitForText('stat_error_2024_2', 10, '#migrationTable');
    I.waitForText('64%', 10, '#migrationProgress');
    I.waitForText('42%', 10, '#migrationOverallProgress');

    I.seeElement('#migrationAnalyze');
    I.seeElement('#migrationStart');
    I.seeElement('#migrationFinalize');
    I.click('#migrationAnalyze');
    I.waitForElement('#migrationPreview:not(.d-none)', 20);
    I.seeElement('#migrationMappings');

    Document.screenshot("/sysadmin/update/stat-browser-migration.png");
});

Scenario('checks permissions', ({ I }) => {
    I.amOnPage('/admin/v9/settings/stat-browser-migration/?removePerm=modUpdate');
    I.see('Na túto aplikáciu/funkciu nemáte prístupové práva');
});

Scenario('logout', ({ I }) => {
    I.logout();
});
