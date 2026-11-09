Feature('webpages.branch-p03');

if (process.env.WEBJET_P03_BROWSER === 'true') {
    const fs = require('fs');
    const read = path => Object.fromEntries(fs.readFileSync(path, 'utf8').trim().split('\n').map(line => { const i = line.indexOf('='); return [line.slice(0, i), line.slice(i + 1)]; }));
    const fixture = read('/tmp/webjet-p03-manual.properties');
    const credentials = read('/tmp/webjet-p03-login.properties');

    Before(({ I }) => {
        // This disposable database has its own generated account; the shared tester is untouched.
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.wjSetDefaultWindowSize();
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForElement('.header-title', 30);
    });

    Scenario('P03 real administration selection, protected preview and read-only boundary', async ({ I, DT }) => {
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        I.waitForElement('#branch-selection', 20);
        I.waitForEnabled('#branch-selection', 20);
        I.selectOption('#branch-selection', fixture.branchA);
        I.waitForText('P03 manual A', 20, '#branch-active-label');
        I.seeTextEquals(`P03 manual A · DRAFT (#${fixture.branchA})`, '#branch-selection option:checked');
        DT.waitForLoader();
        I.saveScreenshot('branch-p03-admin.png');

        const statuses = await I.executeScript(async ({ group, branch }) => {
            const headers = { 'X-CSRF-Token': window.csrfToken, 'X-WebJET-Branch': String(branch), 'X-WebJET-Branch-Domain': 'iwcm.interway.sk', 'Content-Type': 'application/json' };
            const write = await fetch('/admin/rest/web-pages', { method: 'POST', headers, body: '{}' });
            const stale = await fetch(`/admin/rest/web-pages?groupId=${group}`, { headers: { ...headers, 'X-WebJET-Branch': '0' } });
            const context = await fetch('/admin/rest/branches/context').then(response => response.json());
            return { write: write.status, stale: stale.status, selected: context.branchId };
        }, { group: fixture.groupId, branch: fixture.branchA });
        I.assertEqual(statuses.write, 409); I.assertEqual(statuses.stale, 409); I.assertEqual(statuses.selected, Number(fixture.branchA));

        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchA}/p03-manual/`);
        I.waitForText('P03 WORK A', 20);
        I.seeElement('#branch-preview-status');
        I.seeElement('ol.news a[href*="/admin/webpages/branch-preview/"]');
        // Decoded dimensions prove that protected image requests succeeded, beyond DOM presence.
        I.waitForFunction(() => [...document.images].some(image => image.complete && image.naturalWidth === 16), 20);
        I.saveScreenshot('branch-p03-preview-a.png');
        const paging = await I.grabAttributeFrom('nav a[href*="page=2"]', 'href');
        I.amOnPage(paging); I.waitForElement('ol.news', 20); I.seeInCurrentUrl(`/admin/webpages/branch-preview/${fixture.branchA}/`);
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchB}/p03-manual/`); I.waitForText('P03 WORK B', 20); I.dontSee('P03 WORK A');
        I.saveScreenshot('branch-p03-preview-b.png');
        I.amOnPage('/p03-manual/'); I.waitForText('P03 LIVE home', 20); I.dontSee('P03 WORK A');
    });

    Scenario('P03 create dialog keeps selection and rejects stale domain', async ({ I }) => {
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        I.waitForElement('#branch-selection', 20);
        I.waitForEnabled('#branch-selection', 20);
        I.selectOption('#branch-selection', '+ Nová pracovná verzia');
        I.waitForVisible('dialog[open]', 10);
        const name = `autotest-p03-created-${Date.now()}`;
        I.fillField('dialog input[name=name]', name);
        I.fillField('dialog textarea[name=description]', 'P03 local browser fixture');
        I.click('Vytvoriť', 'dialog');
        I.waitForText(name, 15, 'dialog .branch-result');
        I.see('Upravovať web', 'dialog .branch-result');
        const result = await I.executeScript(async () => {
            const context = await fetch('/admin/rest/branches/context').then(response => response.json());
            const stale = await fetch('/admin/rest/branches/context', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ branchId: 0, expectedDomainName: 'foreign.example' }) });
            return { branchId: context.branchId, stale: stale.status };
        });
        I.assertEqual(result.branchId, 0); I.assertEqual(result.stale, 409);
        I.saveScreenshot('branch-p03-create.png');
        I.click('Zavrieť', 'dialog');
        I.waitForInvisible('dialog[open]', 10);
    });

    Scenario('P03 first administration screen on a separate local transport host', async ({ I }) => {
        I.amOnPage('http://127.0.0.1:8086/logoff.do?forward=/admin/logon/');
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForElement('#branch-selection', 30);
        I.waitForEnabled('#branch-selection', 20);
        const context = await I.executeScript(() => ({ host: location.hostname, domain: window.branchScreen.domainName, branchId: window.branchScreen.branchId }));
        I.assertEqual(context.host, '127.0.0.1');
        I.assertEqual(context.domain, 'iwcm.interway.sk');
        I.assertEqual(context.branchId, 0);
        I.selectOption('#branch-selection', fixture.branchA);
        I.waitForText('P03 manual A', 20, '#branch-active-label');
        I.amOnPage(`http://127.0.0.1:8086/admin/webpages/branch-preview/${fixture.branchA}/p03-manual/`);
        I.waitForText('P03 WORK A', 20);
        I.seeInCurrentUrl('http://127.0.0.1:8086/admin/webpages/branch-preview/');
        // The decoded thumbnail also checks authenticated asset requests on this transport host.
        I.waitForFunction(() => [...document.images].some(image => image.complete && image.naturalWidth === 16), 20);
        I.saveScreenshot('branch-p03-admin-host.png');
    });
}
