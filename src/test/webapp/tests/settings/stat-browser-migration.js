Feature('settings.stat-browser-migration');

Before(({ I, login }) => {
    login('admin');
    I.usePlaywrightTo('clear migration response mocks', async ({ page }) => {
        await page.unroute('**/admin/rest/settings/stat-browser-migration/status');
        await page.unroute('**/admin/rest/settings/stat-browser-migration');
    });
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

Scenario('shows reference scan progress and retained identifiers', ({ I }) => {
    const state = {
        tableIndex: 1,
        totalTables: 3,
        cursor: 0,
        tableMaxId: 0,
        running: true,
        finalizing: true,
        done: false,
        table: 'stat_views_2024_2'
    };
    I.usePlaywrightTo('mock the reference scan', async ({ page }) => {
        await page.route('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(state)
        }));
    });
    I.amOnPage('/admin/v9/settings/stat-browser-migration/?userlngr=true');
    I.waitForText('stat_views_2024_2', 10, '#migrationTable');
    I.waitForText('33%', 10, '#migrationOverallProgress');
    I.see('Kontrola používaných identifikátorov', '#migrationProgress');
    I.seeElement('#migrationProgress.progress-bar-striped.progress-bar-animated');
    I.dontSeeElement('#migrationProgress[aria-valuenow]');
    I.verifyDisabled('#migrationFinalize');
    I.saveScreenshot('stat-browser-reference-progress.png');

    I.usePlaywrightTo('complete the mocked finalization', async () => {
        Object.assign(state, {
            running: false,
            finalizing: false,
            finalized: true,
            done: true,
            table: 'done',
            deletedStatKeys: 1,
            retainedStatKeys: 5
        });
    });
    I.waitForText('100%', 10, '#migrationOverallProgress');
    I.seeElement('#migrationProgress[aria-valuenow="100"]:not(.progress-bar-animated)');
    I.see('1', '#migrationDeletedKeys');
    I.see('5', '#migrationRetainedKeys');
});

Scenario('checks permissions', ({ I }) => {
    I.amOnPage('/admin/v9/settings/stat-browser-migration/?removePerm=modUpdate');
    I.see('Na túto aplikáciu/funkciu nemáte prístupové práva');
});

Scenario('logout', ({ I }) => {
    I.logout();
});
