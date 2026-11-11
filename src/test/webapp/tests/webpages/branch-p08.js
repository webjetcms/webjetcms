Feature('webpages.branch-p08').tag('@singlethread');

if (process.env.WEBJET_P08_BROWSER === 'true') {
    const branchPicker = require('../../helpers/branch-picker');
    const fs = require('fs');
    const path = require('path');
    const read = file => Object.fromEntries(fs.readFileSync(file, 'utf8').trim().split('\n').filter(line => line && !line.startsWith('#')).map(line => {
        const index = line.indexOf('=');
        return [line.slice(0, index), line.slice(index + 1)];
    }));
    const fixture = read('/tmp/webjet-p08-pilot.properties');
    const credentials = read('/tmp/webjet-p08-login.properties');
    const admin = read('/tmp/webjet-p05-login.properties');
    const evidence = path.resolve('../../../build/test-results/p08/browser');
    const pages = fixture.pages.split(',').map(Number);
    const dialog = '#branch-merge-dialog';
    const busy = dialog+' .pg-branch-merge__content[aria-busy=true]';
    if (process.env.CODECEPT_URL !== 'http://127.0.0.1:8086') throw new Error('P08 requires its isolated public loopback domain on port 8086.');

    function save(name, data) {
        fs.mkdirSync(evidence, {recursive: true});
        fs.writeFileSync(path.join(evidence, name+'.json'), JSON.stringify(data, null, 2));
    }

    function logon(I, editor) {
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.wjSetDefaultWindowSize();
        I.fillField('username', editor ? credentials[editor+'.login'] : admin.login);
        I.fillField('password', secret(editor ? credentials[editor+'.password'] : admin.password));
        I.click('Prihlásiť sa');
        I.waitForVisible('button.js-profile-toggler', 30);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        I.waitForEnabled('#branch-switcher', 30);
    }

    async function report(I) {
        return I.executeScript(async id => {
            const response = await fetch(`/admin/rest/branches/${id}/progress`, {cache: 'no-store'});
            if (!response.ok) throw new Error('P08 report '+response.status);
            return response.json();
        }, fixture.branchId);
    }

    async function render(I, preview = false) {
        return I.executeScript(async ({f, preview}) => {
            const prefix = preview ? `/admin/webpages/branch-preview/${f.branchId}` : '';
            const response = await fetch(prefix+f.url, {credentials: preview ? 'same-origin' : 'omit', cache: 'no-store'});
            const source = await response.text();
            const document = new DOMParser().parseFromString(source, 'text/html');
            const normalize = value => String(value || '').replaceAll(location.origin, '').replace(/\/admin\/webpages\/branch-preview\/\d+/g, '');
            const news = document.querySelector('ol.p06-news');
            const menu = document.querySelector('#p06-menu');
            return {status: response.status, news: normalize(news?.innerHTML), menu: normalize(menu?.innerHTML),
                ids: [...(news?.querySelectorAll('li[data-id]') || [])].map(item => Number(item.dataset.id)),
                links: [...(menu?.querySelectorAll('a') || [])].map(link => ({text: link.textContent.trim(), href: normalize(link.getAttribute('href'))})),
                privatePathLeaked: source.includes('/tmp/webjet-')};
        }, {f: fixture, preview});
    }

    Scenario('P08 public baseline with branches disabled', async ({I, Document}) => {
        logon(I);
        Document.setConfigValue('branchesEnabled', 'false', true);
        const baseline = await render(I);
        I.assertEqual(baseline.status, 200, 'The pilot must be publicly available before publication.');
        I.assertEqual(baseline.ids.length, 19);
        I.assertTrue(baseline.menu.length > 0);
        save('public-off', baseline);
    });

    Scenario('P08 restore enabled POC configuration', ({I, Document}) => {
        I.amOnPage('/admin/');
        Document.setConfigValue('branchesEnabled', 'true', true);
    });

    Scenario('P08 unchanged public News and Menu with branches enabled', async ({I}) => {
        logon(I);
        const before = JSON.parse(fs.readFileSync(path.join(evidence, 'public-off.json')));
        const enabled = await render(I);
        I.assertDeepEqual(enabled, before);
        save('public-on', enabled);
    });

    for (const [index, editor] of ['a', 'b', 'c'].entries()) {
        Scenario(`P08 editor ${editor} prepares assigned campaign pages through the normal editor`, async ({I, DTE}) => {
            logon(I, editor);
            branchPicker.select(I, fixture.branchId);
            I.waitForText('autotest P08 pilot', 20, '#branch-active-label');
            const assigned = pages.filter((id, pageIndex) => pageIndex % 3 === index);
            for (const id of assigned) {
                I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${id}`);
                DTE.waitForEditor();
                I.clickCss('#pills-dt-datatableInit-basic-tab');
                I.fillField('#DTE_Field_title', `autotest P08 editor ${editor} page ${id}`);
                I.fillField('#DTE_Field_navbar', `autotest P08 editor ${editor} page ${id}`);
                I.clickCss('#pills-dt-datatableInit-content-tab');
                await DTE.fillCkeditor(`<h1>autotest P08 editor ${editor} page ${id}</h1><p>Campaign contribution by editor ${editor}.</p>`);
                DTE.save();
                DTE.waitForModalClose();
            }
            const current = await report(I);
            for (const id of assigned) I.assertEqual(current.items.find(item => item.type === 'DOCUMENT' && item.objectKey === String(id)).work.fields.title, `autotest P08 editor ${editor} page ${id}`);
            const preview = await render(I, true);
            I.assertEqual(preview.status, 200);
            I.assertEqual(preview.ids.length, 21);
            I.assertFalse(preview.privatePathLeaked);
            save('editor-'+editor, {login: credentials[editor+'.login'], assigned, current, preview});
            I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}${fixture.url}`);
            I.waitForText(`autotest P08 editor ${editor}`, 20, '#p06-menu');
            I.click(locate('#p06-menu a').withText(`autotest P08 editor ${editor}`).first());
            I.waitForText(`Campaign contribution by editor ${editor}.`, 20);
            I.seeInCurrentUrl(`/admin/webpages/branch-preview/${fixture.branchId}/`);
        });
    }

    Scenario('P08 concurrent production edit, unknown type rejection and UI conflict publication', async ({I, DTE}) => {
        logon(I, 'c');
        branchPicker.select(I, fixture.branchId);
        I.waitForText('autotest P08 pilot', 20, '#branch-active-label');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.pageId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.fillField('#DTE_Field_title', `autotest P08 editor a page ${fixture.pageId}`);
        DTE.save();
        DTE.waitForModalClose();
        branchPicker.select(I, 0);
        I.waitForElement('#branch-switcher[aria-label*="Hlavná verzia"]', 20);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.pageId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.assertEqual(await I.executeScript(() => window.branchScreen.branchId), 0);
        I.fillField('#DTE_Field_title', 'autotest P08 concurrent LIVE');
        DTE.save();
        DTE.waitForModalClose();
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}#branchCompare=${fixture.branchId}`);
        I.waitForVisible(dialog, 30);
        I.waitForInvisible(busy, 30);
        const before = await report(I);
        I.assertDeepEqual([...new Set(before.items.map(item => item.type))].sort(), ['DOCUMENT', 'FILE', 'GROUP', 'MEDIA']);
        I.assertTrue(before.items.some(item => item.conflict));
        const unknown = await I.executeScript(async ({id, revision}) => {
            const response = await fetch(`/admin/rest/branches/${id}/resolve`, {method: 'POST', headers: {'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken}, body: JSON.stringify({expectedRevision: revision, type: 'BANNER', objectKey: '1', choice: 'WORK', liveHash: 'invalid', workHash: 'invalid'})});
            return response.status;
        }, {id: fixture.branchId, revision: before.revision});
        I.assertEqual(unknown, 400);
        I.assertEqual((await report(I)).revision, before.revision);
        for (const item of before.items.filter(item => item.conflict)) {
            I.click(`${dialog} [data-branch-compare="${item.objectKey}"]`);
            I.waitForVisible(dialog+' .pg-branch-merge__detail', 10);
            I.checkOption(`${dialog} input[name="branch-merge-choice"][value="WORK"]`);
            I.click('#branch-merge-save-choice');
            I.waitForInvisible(busy, 30);
        }
        I.waitForEnabled('#branch-merge-publish', 30);
        const preview = await render(I, true);
        I.assertEqual(preview.status, 200);
        const fileBefore = await I.executeScript(async f => {
            const publicResponse = await fetch(f.file, {credentials: 'omit', cache: 'no-store'});
            const previewResponse = await fetch(`/admin/webpages/branch-preview/${f.branchId}${f.file}`, {cache: 'no-store'});
            return {publicStatus: publicResponse.status, publicBytes: await publicResponse.text(), previewStatus: previewResponse.status, previewBytes: await previewResponse.text()};
        }, fixture);
        I.assertEqual(fileBefore.publicStatus, 200);
        I.assertEqual(fileBefore.previewStatus, 200);
        I.assertNotEqual(fileBefore.publicBytes, fileBefore.previewBytes);
        save('before-merge', {before, unknown, preview, fileBefore});
        I.saveScreenshot('branch-p08-comparison.png');
        I.click('#branch-merge-publish');
        I.waitForVisible('#branch-merge-confirmation', 10);
        I.click('#branch-merge-confirm');
        I.waitForText('Publikovaná', 90, '#branch-merge-title');
        I.waitForText('Publikovanie je dokončené.', 30, '#branch-merge-progress');
        const after = await report(I);
        I.assertEqual(after.state, 'MERGED');
        I.assertTrue(after.items.every(item => item.applyState === 'DONE'));
        save('published', after);
    });

    Scenario('P08 anonymous public HTML menu News detail and attachment equal the selected preview', async ({I}) => {
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        const before = JSON.parse(fs.readFileSync(path.join(evidence, 'before-merge.json')));
        const after = await render(I);
        I.assertEqual(after.status, 200);
        I.assertDeepEqual(after, before.preview);
        I.amOnPage(fixture.url);
        I.waitForText('autotest P08 editor a', 20, '#p06-menu');
        I.click(locate('#p06-menu a').withText('autotest P08 editor a').first());
        I.waitForText('Campaign contribution by editor a.', 20);
        I.dontSeeInCurrentUrl('/admin/');
        I.amOnPage(fixture.url);
        I.click(locate('ol.p06-news a').withText('autotest P08 editor b').first());
        I.waitForText('Campaign contribution by editor b.', 20);
        const attachment = await I.executeScript(async f => {
            const response = await fetch(f.file, {credentials: 'omit', cache: 'no-store'});
            const preview = await fetch(`/admin/webpages/branch-preview/${f.branchId}${f.url}`, {credentials: 'omit', cache: 'no-store'});
            return {status: response.status, bytes: await response.text(), anonymousPreviewStatus: preview.status};
        }, fixture);
        I.assertEqual(attachment.status, 200);
        I.assertEqual(attachment.bytes, before.fileBefore.previewBytes);
        I.assertEqual(attachment.anonymousPreviewStatus, 403);
        save('public-after', {after, attachment});
        I.saveScreenshot('branch-p08-public-result.png');
    });
}
