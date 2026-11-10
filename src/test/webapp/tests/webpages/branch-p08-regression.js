Feature('webpages.branch-p08-regression').tag('@singlethread');

if (process.env.WEBJET_P08_BROWSER === 'true') {
    const fs = require('fs');
    const path = require('path');
    const read = file => Object.fromEntries(fs.readFileSync(file, 'utf8').trim().split('\n').map(line => {
        const index = line.indexOf('='); return [line.slice(0, index), line.slice(index + 1)];
    }));
    const credentials = read('/tmp/webjet-p05-login.properties');
    const pilot = read('/tmp/webjet-p08-pilot.properties');
    const manifest = '/tmp/webjet-p08-regression.json';
    const fixture = fs.existsSync(manifest) ? JSON.parse(fs.readFileSync(manifest)) : {};
    const evidence = path.resolve('../../../build/test-results/p08/regression');
    const mediaTable = '#datatableFieldDTE_Field_editorFields-media';
    if (process.env.CODECEPT_URL !== 'http://127.0.0.1:8086') throw new Error('P08 regression requires the isolated loopback POC.');
    const save = (name, data) => {fs.mkdirSync(evidence, {recursive: true});fs.writeFileSync(path.join(evidence, name+'.json'), JSON.stringify(data, null, 2));};
    const retain = () => fs.writeFileSync(manifest, JSON.stringify(fixture, null, 2));

    function logon(I) {
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.wjSetDefaultWindowSize();
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForVisible('button.js-profile-toggler', 30);
    }

    async function anonymous(I, url) {
        return I.executeScript(async url => {
            const response = await fetch(url, {credentials: 'omit', cache: 'no-store'});
            return {status: response.status, text: await response.text()};
        }, url);
    }

    Scenario('P08 prepare a separate production regression folder', async ({I, DT, DTE}) => {
        logon(I);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${pilot.groupId}`);
        I.waitForEnabled('#branch-selection', 30);
        I.selectOption('#branch-selection', 'Produkcia');
        // Context is server-owned; a default label alone cannot establish which screen was loaded.
        I.waitForFunction(() => window.branchScreen?.branchId === 0, [], 30);
        if (!fixture.groupId) {
            I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
            DT.waitForLoader();
            I.click(DT.btn.tree_add_button);
            DTE.waitForEditor('groups-datatable');
            I.fillField('#DTE_Field_groupName', 'autotest P08 production regression');
            I.fillField('#DTE_Field_navbarName', 'autotest P08 production regression');
            I.fillField('#DTE_Field_urlDirName', 'autotest-p08-regression');
            // groupid=0 navigation may restore the previous tree selection; this fixture needs its own top-level scope.
            I.executeScript(() => groupsDatatable.EDITOR.field('editorFields.parentGroupDetails').set({groupId: 0}));
            I.executeScript(() => groupsDatatable.EDITOR.one('submitSuccess', (event, json) => {window.p08CreatedGroup = json.data[0].groupId;}));
            DTE.save('groups-datatable');
            fixture.groupId = Number(await I.executeScript(() => window.p08CreatedGroup));
            I.assertTrue(fixture.groupId > 0);
            retain();
        }
        save('fixture', fixture);
    });

    for (const enabled of [false, true]) {
        Scenario(`P08 production editing URL history media and individual publication with flag ${enabled}`, async ({I, DT, DTE, Document}) => {
            logon(I);
            Document.setConfigValue('branchesEnabled', String(enabled), true);
            I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
            DT.waitForLoader();
            const title = 'autotest P08 production '+enabled;
            if (fixture[String(enabled)]) I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture[String(enabled)].id}`);
            else I.click(DT.btn.add_button);
            DTE.waitForEditor();
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            I.fillField('#DTE_Field_title', title);
            I.fillField('#DTE_Field_navbar', title);
            I.checkOption('#DTE_Field_available_0');
            I.clickCss('#pills-dt-datatableInit-template-tab');
            I.selectOption('#DTE_Field_tempId', 'P03 manual');
            I.clickCss('#pills-dt-datatableInit-content-tab');
            await DTE.fillCkeditor(`<p>${title} live</p>`);
            DTE.save();
            DTE.waitForModalClose();
            const page = await I.executeScript(title => {
                const page = webpagesDatatable.rows().data().toArray().find(item => item.title === title);
                return {id: page.docId, url: page.virtualPath};
            }, title);
            fixture[String(enabled)] = page;
            retain();
            const initial = await anonymous(I, page.url);
            I.assertEqual(initial.status, 200);
            I.assertContain(initial.text, title+' live');

            I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${page.id}`);
            DTE.waitForEditor();
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            I.fillField('#DTE_Field_title', title+' updated');
            I.clickCss('#pills-dt-datatableInit-content-tab');
            await DTE.fillCkeditor(`<p>${title} updated live</p>`);
            I.clickCss('#pills-dt-datatableInit-media-tab');
            I.clickCss(`${mediaTable}_wrapper button.buttons-create`);
            DTE.waitForEditor('datatableFieldDTE_Field_editorFields-media');
            I.fillField('#DTE_Field_mediaTitleSk', title+' attachment');
            I.fillField('#DTE_Field_mediaLink', pilot.file);
            I.clickCss(`${mediaTable}_modal button.btn-primary`);
            DTE.waitForLoader('datatableFieldDTE_Field_editorFields-media');
            DT.waitForLoader();
            I.waitForText(title+' attachment', 20, mediaTable);
            DTE.save();
            DTE.waitForModalClose();
            const updated = await anonymous(I, page.url);
            I.assertEqual(updated.status, 200);
            I.assertContain(updated.text, title+' updated live');

            I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${page.id}`);
            DTE.waitForEditor();
            I.clickCss('#pills-dt-datatableInit-history-tab');
            I.waitForElement('#datatableFieldDTE_Field_editorFields-history tbody tr', 20);
            const historyCount = await I.grabNumberOfVisibleElements('#datatableFieldDTE_Field_editorFields-history tbody tr');
            I.assertTrue(historyCount >= 2);
            I.clickCss('#pills-dt-datatableInit-content-tab');
            await DTE.fillCkeditor(`<p>${title} scheduled</p>`);
            I.clickCss('#pills-dt-datatableInit-perex-tab');
            const term = Date.now()+20000;
            DTE.fillField('publishStartDate', I.formatDateTime(term));
            I.pressKey('Tab');
            I.checkOption('#DTE_Field_editorFields-publishAfterStart_0');
            DTE.save();
            DTE.waitForModalClose();
            const pending = await anonymous(I, page.url);
            I.assertContain(pending.text, title+' updated live');
            I.assertNotContain(pending.text, title+' scheduled');
            // Retry only a public read/assertion; the individual plan is saved once and published by DocPublishService.
            const published = await I.retry({retries: 45, minTimeout: 1000, maxTimeout: 1000}).executeScript(async ({url, title}) => {
                const response = await fetch(url, {credentials: 'omit', cache: 'no-store'});
                const text = await response.text();
                if (response.status !== 200 || !text.includes(title+' scheduled')) throw new Error('P08 individual plan is not public yet');
                return {status: response.status, text};
            }, {url: page.url, title});
            I.assertEqual(published.status, 200);
            await I.assertContain(published.text, title+' scheduled');
            save('production-'+enabled, {page, historyCount, term, initial, updated, pending, published});
        });

        Scenario(`P08 restore enabled POC flag after production ${enabled}`, ({I, Document}) => {
            logon(I);
            Document.setConfigValue('branchesEnabled', 'true', true);
        });
    }

    Scenario('P08 restricted editor cannot save outside its permitted production folder', async ({I}) => {
        const editors = read('/tmp/webjet-p08-login.properties');
        logon(I);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        I.waitForEnabled('#branch-selection', 30);
        const record = await I.executeScript(async id => (await fetch(`/admin/rest/web-pages/${id}`, {cache: 'no-store'})).json(), fixture.false.id);
        await I.assertTrue(typeof record.title === 'string' && record.title.length > 0);
        const publicBefore = await anonymous(I, fixture.false.url);
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.fillField('username', editors['a.login']);
        I.fillField('password', secret(editors['a.password']));
        I.click('Prihlásiť sa');
        I.waitForVisible('button.js-profile-toggler', 30);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${pilot.groupId}`);
        I.waitForEnabled('#branch-selection', 30);
        const denied = await I.executeScript(async ({id, record, group}) => {
            const response = await fetch(`/admin/rest/web-pages/${id}`, {cache: 'no-store'});
            const read = await response.json();
            const page = {...record};
            page.title = 'autotest P08 unauthorized change';
            const write = await fetch(`/admin/rest/web-pages/editor?groupId=${group}`, {method: 'POST', headers: {'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken}, body: JSON.stringify({action: 'edit', data: {[id]: page}})});
            const body = await write.json();
            return {readStatus: response.status, readError: read.error, writeStatus: write.status, error: body.error, fieldErrors: body.fieldErrors};
        }, {id: fixture.false.id, record, group: fixture.groupId});
        await I.assertTrue(denied.writeStatus === 403 || Boolean(denied.error) || denied.fieldErrors?.length > 0);
        await I.assertNotContain(JSON.stringify(denied.fieldErrors || []), 'navbar');
        await I.assertDeepEqual(await anonymous(I, fixture.false.url), publicBefore);
        save('permissions', {page: fixture.false.id, ...denied});
    });

    for (const enabled of [false, true]) {
        Scenario(`P08 ordinary public load sample with flag ${enabled}`, async ({I, Document}) => {
            logon(I);
            Document.setConfigValue('branchesEnabled', String(enabled), true);
            const measurements = await I.executeScript(async url => {
                const values = [];
                for (let index=0; index<23; index++) {
                    const start = performance.now();
                    const response = await fetch(url, {credentials: 'omit', cache: 'no-store'});
                    const source = await response.text();
                    if (response.status !== 200 || !source.includes('p06-news') || !source.includes('p06-menu')) throw new Error('P08 public render failed during sampling');
                    if (index>=3) values.push(performance.now()-start);
                }
                return values;
            }, pilot.url);
            save('load-'+enabled, {warmup: 3, samples: measurements, unit: 'ms', note: 'Sequential local HTTP render, no external assets or concurrency.'});
        });

        Scenario(`P08 restore enabled POC flag after load ${enabled}`, ({I, Document}) => {
            logon(I);
            Document.setConfigValue('branchesEnabled', 'true', true);
        });
    }
}
