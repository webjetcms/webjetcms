// Isolate request interception from stale workers and dispose routes after every scenario.
Feature('settings.stat-browser-migration', { timeout: 180 })
    .config('Playwright', { restart: 'context' });

AfterSuite(({ I }) => {
    I.limitTime(30).usePlaywrightTo('release the closed context before restoring session mode', async helper => {
        // CodeceptJS 3 retains the closed context, which session mode would otherwise reuse.
        helper.browserContext = null;
    });
});

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/settings/stat-browser-migration/');
});

Scenario('shows migration preview without changing data @screenshot', ({ I, Document }) => {
    I.mockRoute('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
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
    }));

    I.mockRoute('**/admin/rest/settings/stat-browser-migration', route => route.fulfill({
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
    }));
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

Scenario('shows reference scan progress and retained identifiers', async ({ I }) => {
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
    await I.mockRoute('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(state)
    }));
    await I.amOnPage('/admin/v9/settings/stat-browser-migration/?userlngr=true');
    await I.waitForText('stat_views_2024_2', 10, '#migrationTable');
    await I.waitForText('33%', 10, '#migrationOverallProgress');
    await I.see('Kontrola používaných identifikátorov', '#migrationProgress');
    await I.seeElement('#migrationProgress.progress-bar-striped.progress-bar-animated');
    await I.dontSeeElement('#migrationProgress[aria-valuenow]');
    await I.verifyDisabled('#migrationFinalize');
    await I.saveScreenshot('stat-browser-reference-progress.png');

    Object.assign(state, {
        running: false,
        finalizing: false,
        finalized: true,
        done: true,
        table: 'done',
        deletedStatKeys: 1,
        retainedStatKeys: 5
    });
    await I.waitForText('100%', 10, '#migrationOverallProgress');
    await I.seeElement('#migrationProgress[aria-valuenow="100"]:not(.progress-bar-animated)');
    await I.see('1', '#migrationDeletedKeys');
    await I.see('5', '#migrationRetainedKeys');
});

Data([{ seoBotsIndexReady: false }, { seoBotsIndexReady: true }]).Scenario('finalization with empty mappings follows index readiness', ({ I, current }) => {
    I.mockRoute('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ running: false, done: true })
    }));
    I.mockRoute('**/admin/rest/settings/stat-browser-migration', route => route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ seoBots: [], browserKeys: [], tables: [], migrationCompleted: true, seoBotsIndexReady: current.seoBotsIndexReady })
    }));
    I.refreshPage();
    I.waitForElement('#migrationStart:disabled', 10);
    I.click('#migrationAnalyze');
    I.waitForElement('#migrationPreview:not(.d-none)', 10);
    if (current.seoBotsIndexReady) I.verifyDisabled('#migrationFinalize');
    else I.seeElement('#migrationFinalize:not(:disabled)');
});

Data([{ finalized: false }, { finalized: true }]).Scenario('analysis separates retained OS values from completed browser migration', ({ I, current }) => {
    I.mockRoute('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ running: false, done: true, finalized: current.finalized, deletedStatKeys: 991, retainedStatKeys: 6 })
    }));
    I.mockRoute('**/admin/rest/settings/stat-browser-migration', route => route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
            seoBots: [], browserKeys: [], tables: ['stat_views_2021_7'], migrationCompleted: true, seoBotsIndexReady: true,
            retainedKeys: ['', 'Safari 4.0', 'Chrome 110.0', 'Chrome 33', 'Edge Mobile 118', 'Chrome 137']
                .map(source => ({ source }))
        })
    }));
    I.amOnPage('/admin/v9/settings/stat-browser-migration/?userlngr=true');
    I.waitForElement('#migrationStart:disabled', 10);
    I.click('#migrationAnalyze');
    I.waitForVisible('#migrationRetainedPreview', 10);
    I.see('0', '#keyMappingCount');
    I.see('6', '#retainedKeyCount');
    I.see('nevyžadujú ďalšiu migráciu', '#migrationRetainedPreview');
    I.see('(prázdna hodnota)', '#migrationRetainedMappings');
    I.see('Safari 4.0', '#migrationRetainedMappings');
    I.see('Chrome 137', '#migrationRetainedMappings');
    I.dontSeeElement('#migrationMappings');
    I.dontSeeElement('#migrationRetainedBrowserPreview');
    I.seeElement('#migrationAnalysisComplete');
    I.verifyDisabled('#migrationStart');
    I.verifyDisabled('#migrationFinalize');
    I.saveScreenshot(`stat-browser-retained-analysis-${current.finalized}.png`);
});

Data([{ finalized: false }, { finalized: true }]).Scenario('completed analysis separates retained browser and OS identifiers', async ({ I, current }) => {
    const markupSource = '<img data-autotest-retained-browser src="x" onerror="window.autotestRetainedBrowserExecuted=true"> 1.0';
    if (current.finalized) I.resizeWindow(760, 900);
    I.usePlaywrightTo('mock completed migration with referenced identifiers', async ({ page }) => {
        await page.route('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ running: false, done: true, finalized: current.finalized })
        }));
        await page.route('**/admin/rest/settings/stat-browser-migration', route => route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                seoBots: [], browserKeys: [], tables: ['stat_views_2021_7'], migrationCompleted: true, seoBotsIndexReady: true,
                retainedBrowserKeys: [
                    { sourceId: 10, targetId: 11, source: 'MSIE 8.0', target: 'MSIE' },
                    { sourceId: 29539, targetId: 29540, source: 'Yanga WorldSearch Bot v1.1', target: 'Yanga WorldSearch Bot' },
                    { sourceId: 30, targetId: 31, source: markupSource, target: 'autotest markup browser' }
                ],
                retainedKeys: [{ source: 'Safari 4.0' }]
            })
        }));
    });
    I.amOnPage('/admin/v9/settings/stat-browser-migration/?userlngr=true');
    I.waitForElement('#migrationStart:disabled', 10);
    I.click('#migrationAnalyze');
    I.waitForVisible('#migrationRetainedBrowserPreview', 10);
    I.seeElement('#migrationAnalysisComplete');
    I.see('0', '#keyMappingCount');
    I.see('3', '#retainedBrowserKeyCount');
    I.see('1', '#retainedKeyCount');
    I.see('Opakovaná finalizácia ich nezlúči ani neodstráni.', '#migrationRetainedBrowserPreview');
    I.see('MSIE 8.0', '#migrationRetainedBrowserMappings');
    I.see('Yanga WorldSearch Bot v1.1', '#migrationRetainedBrowserMappings');
    I.see(markupSource, '#migrationRetainedBrowserMappings');
    I.see('Safari 4.0', '#migrationRetainedMappings');
    I.dontSee('Safari 4.0', '#migrationRetainedBrowserMappings');
    I.dontSee('MSIE 8.0', '#migrationRetainedMappings');
    I.dontSee('autotest markup browser', '#migrationRetainedBrowserMappings');
    I.assertEqual(await I.grabNumberOfVisibleElements('#migrationRetainedBrowserMappings tbody td'), 3, 'Retained browser rows show only the stored source value');
    I.assertEqual(await I.executeScript(() => document.querySelectorAll('#migrationRetainedBrowserMappings img').length), 0, 'Retained browser values must be rendered as text');
    I.dontSeeElement('#migrationMappings');
    I.verifyDisabled('#migrationStart');
    I.verifyDisabled('#migrationFinalize');
    I.saveScreenshot(`stat-browser-retained-browser-analysis-${current.finalized}.png`, true);
    if (current.finalized) I.wjSetDefaultWindowSize();
});

Scenario('completed analysis enables finalization for remaining unused identifiers', ({ I }) => {
    I.usePlaywrightTo('mock previously finalized migration with unused identifiers', async ({ page }) => {
        await page.route('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ running: false, done: true, finalized: true })
        }));
        await page.route('**/admin/rest/settings/stat-browser-migration', route => route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                seoBots: [], tables: ['stat_views_2021_7'], migrationCompleted: true, seoBotsIndexReady: true,
                browserKeys: [{ sourceId: 101, targetId: 10, source: 'Chrome 127.0', target: 'Chrome' }],
                retainedBrowserKeys: [{ sourceId: 20, targetId: 21, source: 'MSIE 8.0', target: 'MSIE' }],
                retainedKeys: []
            })
        }));
    });
    I.refreshPage();
    I.waitForElement('#migrationStart:disabled', 10);
    I.click('#migrationAnalyze');
    I.waitForVisible('#migrationMappings', 10);
    I.see('1', '#keyMappingCount');
    I.see('nepoužívaných identifikátorov na odstránenie', '#keyMappingLabel');
    I.see('Chrome 127.0', '#migrationMappings');
    I.see('MSIE 8.0', '#migrationRetainedBrowserMappings');
    I.dontSeeElement('#migrationAnalysisComplete');
    I.seeElement('#migrationFinalize:not(:disabled)');
    I.verifyDisabled('#migrationStart');
});

Data(['modUpdate', 'users.edit_admins']).Scenario('allows access with either migration permission', ({ I, current }) => {
    I.amOnPage(`/admin/v9/settings/stat-browser-migration/?removePerm=${current}`);
    I.waitForElement('#migrationAnalyze', 10);
    I.logout();
});

Scenario('checks permissions', ({ I }) => {
    I.amOnPage('/admin/v9/settings/stat-browser-migration/?removePerm=modUpdate,users.edit_admins');
    I.waitUrlEquals('/admin/403.jsp', 10);
    I.see('Na túto aplikáciu/funkciu nemáte prístupové práva');
    I.dontSeeElement('#migrationAnalyze');
    I.logout();
});

Scenario('shows error merge progress before completing the table', async ({ I }) => {
    const state = {
        tableIndex: 1,
        totalTables: 3,
        cursor: 500,
        tableMaxId: 1000,
        running: true,
        mergingStatErrors: true,
        table: 'stat_error_2024_2'
    };
    await I.mockRoute('**/admin/rest/settings/stat-browser-migration/status', route => route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(state)
    }));
    await I.amOnPage('/admin/v9/settings/stat-browser-migration/?userlngr=true');
    await I.waitForText('Zlučovanie záznamov chýb', 10, '#migrationProgress');
    await I.see('stat_error_2024_2', '#migrationTable');
    await I.see('33%', '#migrationOverallProgress');
    await I.seeElement('#migrationProgress.progress-bar-striped.progress-bar-animated');
    await I.dontSeeElement('#migrationProgress[aria-valuenow]');
    await I.verifyDisabled('#migrationStart');

    Object.assign(state, { mergingStatErrors: false, tableIndex: 2, cursor: 0, tableMaxId: 0 });
    await I.waitForText('67%', 10, '#migrationOverallProgress');
    await I.seeElement('#migrationProgress[aria-valuenow="0"]:not(.progress-bar-animated)');
});

Scenario('logout', ({ I }) => {
    I.logout();
});
