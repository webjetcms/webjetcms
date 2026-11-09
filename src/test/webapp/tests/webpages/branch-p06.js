Feature('webpages.branch-p06').tag('@singlethread');

if (process.env.WEBJET_P06_BROWSER === 'true') {
    const fs = require('fs');
    const path = require('path');
    const crypto = require('crypto');
    const read = file => Object.fromEntries(fs.readFileSync(file, 'utf8').trim().split('\n').filter(line => line && !line.startsWith('#')).map(line => {
        const separator = line.indexOf('=');
        return [line.slice(0, separator), line.slice(separator + 1)];
    }));
    const credentials = read('/tmp/webjet-p05-login.properties');
    const evidence = path.resolve(process.env.WEBJET_P06_EVIDENCE || '../../../build/test-results/p06');
    const digest = bytes => crypto.createHash('sha256').update(Buffer.from(bytes)).digest('hex');
    const dialog = '#branch-merge-dialog';
    const detail = dialog+' .pg-branch-merge__detail';
    const busy = dialog+' .pg-branch-merge__content[aria-busy=true]';
    const itemButton = id => `${dialog} [data-branch-compare="${id}"]`;
    const retain = () => read('/tmp/webjet-p06-manual.properties');
    const disposable = () => read('/tmp/webjet-p06-publish.properties');

    if (!/^http:\/\/(iwcm\.interway\.sk|127\.0\.0\.1|localhost):8086\/?$/.test(process.env.CODECEPT_URL || '')) {
        throw new Error('P06 browser publishing requires the isolated local POC on port 8086.');
    }

    function save(name, value) {
        fs.mkdirSync(evidence, {recursive: true});
        fs.writeFileSync(path.join(evidence, name+'.json'), JSON.stringify(value, null, 2));
    }

    async function report(I, fixture) {
        return I.executeScript(async id => {
            const response = await fetch(`/admin/rest/branches/${id}/progress`, {cache: 'no-store'});
            if (!response.ok) throw new Error('P06 report: '+response.status+' '+await response.text());
            return response.json();
        }, fixture.branchId);
    }

    function pageItem(current, fixture) {
        return current.items.find(item => item.type === 'DOCUMENT' && item.objectKey === String(fixture.pageId));
    }

    async function openComparison(I, fixture, management = false) {
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}${management ? '' : '#branchCompare='+fixture.branchId}`);
        if (management) {
            I.waitForEnabled('#branch-selection', 30);
            I.selectOption('#branch-selection', 'manage');
            I.waitForVisible('dialog[open] .branch-list', 20);
            const current = await report(I, fixture);
            I.click(locate('dialog[open] .branch-list p').withText(current.name).find('button').withText('Skontrolovať zmeny'));
        }
        I.waitForVisible(dialog, 30);
        I.waitForElement(itemButton(fixture.pageId), 30);
        I.waitForInvisible(busy, 30);
        I.click(itemButton(fixture.pageId));
        I.waitForVisible(detail, 10);
        I.seeElement(`${detail} [data-branch-version="BASE"]`);
        I.seeElement(`${detail} [data-branch-version="LIVE"]`);
        I.seeElement(`${detail} [data-branch-version="WORK"]`);
    }

    async function choose(I, choice, ready = true) {
        I.checkOption(`${detail} input[name="branch-merge-choice"][value="${choice}"]`);
        I.click('#branch-merge-save-choice');
        I.waitForInvisible(busy, 30);
        if (ready) I.waitForEnabled('#branch-merge-publish', 30);
        I.seeCheckboxIsChecked(`${detail} input[name="branch-merge-choice"][value="${choice}"]`);
    }

    async function render(I, fixture, preview) {
        return I.executeScript(async ({fixture: f, preview: isPreview}) => {
            const prefix = isPreview ? `/admin/webpages/branch-preview/${f.branchId}` : '';
            const response = await fetch(prefix+f.url, {cache: 'no-store'});
            if (!response.ok) throw new Error('P06 render: '+response.status+' '+await response.text());
            const source = await response.text();
            const doc = new DOMParser().parseFromString(source, 'text/html');
            const news = doc.querySelector('ol.p06-news');
            const menu = doc.querySelector('#p06-menu');
            if (!news || !menu) throw new Error('The actual P06 News and Menu render is missing.');
            const normalize = value => String(value || '').replaceAll(location.origin, '').replace(/\/admin\/webpages\/branch-preview\/\d+/g, '');
            return {
                url: normalize(response.url),
                news: {
                    markup: normalize(news.innerHTML),
                    ids: [...news.querySelectorAll('li[data-id]')].map(item => Number(item.dataset.id)),
                    items: [...news.querySelectorAll('li[data-id]')].map(item => ({
                        id: Number(item.dataset.id), author: item.dataset.author, saveDate: item.dataset.date,
                        title: item.querySelector('a')?.textContent.trim(), perex: item.querySelector('span')?.textContent.trim(),
                        links: [...item.querySelectorAll('a')].map(link => ({text: link.textContent.trim(), href: normalize(link.getAttribute('href'))})),
                        media: [...item.querySelectorAll('.p06-media')].map(media => ({id: Number(media.dataset.media), title: media.textContent.trim(), href: normalize(media.getAttribute('href'))}))
                    }))
                },
                menu: {
                    markup: normalize(menu.innerHTML),
                    links: [...menu.querySelectorAll('a')].map(link => ({text: link.textContent.trim(), href: normalize(link.getAttribute('href')), classes: link.className, itemClasses: link.closest('li')?.className || ''}))
                }
            };
        }, {fixture, preview});
    }

    async function binaries(I, fixture) {
        return I.executeScript(async f => {
            const output = {};
            for (const version of ['BASE', 'LIVE', 'WORK']) {
                const response = await fetch(`/admin/rest/branches/${f.branchId}/file-version?path=${encodeURIComponent(f.file)}&version=${version}`, {cache: 'no-store'});
                output[version] = {status: response.status, cache: response.headers.get('cache-control'), bytes: Array.from(new Uint8Array(await response.arrayBuffer()))};
            }
            return output;
        }, fixture);
    }

    async function homeIdentity(I, fixture) {
        return I.executeScript(async id => {
            const response = await fetch(`/admin/rest/web-pages/${id}`, {cache: 'no-store'});
            if (!response.ok) throw new Error('P06 owner page: '+response.status);
            const page = await response.json();
            return {docId: page.docId, authorId: page.authorId, dateCreated: page.dateCreated, perexGroup: page.perexGroup};
        }, fixture.homeId);
    }

    async function pageDetails(I, id) {
        return I.executeScript(async id => {
            const response = await fetch(`/admin/rest/web-pages/${id}`, {cache: 'no-store'});
            if (!response.ok) throw new Error('P06 page repository: '+response.status);
            const page = await response.json();
            return {docId: page.docId, title: page.title, navbar: page.navbar, htmlData: page.htmlData, data: page.data, groupId: page.groupId, available: page.available, authorId: page.authorId, dateCreated: page.dateCreated, perexGroup: page.perexGroup, virtualPath: page.virtualPath};
        }, id);
    }

    async function trashDetails(I, fixture) {
        const folders = await I.executeScript(async f => {
            const read = async id => {
                const response = await fetch(`/admin/rest/groups/${id}`, {cache: 'no-store'});
                if (!response.ok) throw new Error('P06 folder repository: '+response.status);
                const group = await response.json();
                return {groupId: group.groupId, parentGroupId: group.parentGroupId, groupName: group.groupName};
            };
            return {section: await read(f.section), nested: await read(f.nested)};
        }, fixture);
        return {...folders, page: await pageDetails(I, fixture.page)};
    }

    Before(async ({ I }) => {
        I.switchTo();
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.wjSetDefaultWindowSize();
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForEnabled('#branch-selection', 30);
        // Loading the fixture root selects its logical domain; the initial dashboard can use another domain.
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${retain().groupId}`);
        I.waitForEnabled('#branch-selection', 30);
        const translations = await I.executeScript(() => Object.fromEntries(['contentWrittenError', 'contentWrittenNote', 'trashWaitNote', 'fileStateNote', 'liveMoveNote'].map(name => [name, WJ.translate(`branches.merge.${name}.js`)])));
        for (const [name, value] of Object.entries(translations)) I.assertNotEqual(value, `branches.merge.${name}.js`, 'P06 completion and recovery messages must be loaded before changing any fixture.');
    });

    Scenario('P06 retained conflict offers real three-way content and authenticated files without publishing the demo', async ({ I }) => {
        const fixture = retain();
        const before = await report(I, fixture);
        I.assertEqual(before.state, 'DRAFT');
        I.assertTrue(pageItem(before, fixture).conflict);
        I.assertEqual(pageItem(before, fixture).choice, null);
        await openComparison(I, fixture, true);
        const item = pageItem(before, fixture);
        for (const version of ['BASE', 'LIVE', 'WORK']) I.see(item[version.toLowerCase()].fields.title, `${detail} [data-branch-version="${version}"]`);
        I.click('#branch-merge-all-fields');
        I.seeCheckboxIsChecked('#branch-merge-all-fields');
        const editorLabels = await I.executeScript(() => Object.fromEntries(
            ['htmlHead', 'fieldA'].map(name => {
                const column = window.webpagesDatatable.settings()[0].aoColumns.find(column => column.mData === name);
                return [name, new DOMParser().parseFromString(column.sTitle, 'text/html').body.textContent.trim()];
            })
        ));
        I.assertEqual(before.fieldLabels.DOCUMENT.html_head, editorLabels.htmlHead);
        I.assertEqual(before.fieldLabels.DOCUMENT.field_a, editorLabels.fieldA);
        I.see(editorLabels.htmlHead, `${detail} [data-branch-version="BASE"] dt`);
        I.see(editorLabels.fieldA, `${detail} [data-branch-version="BASE"] dt`);
        I.seeElement('#branch-merge-publish:disabled');
        const files = await binaries(I, fixture);
        for (const version of Object.values(files)) {
            I.assertEqual(version.status, 200);
            I.assertContain(version.cache, 'no-store');
        }
        I.assertEqual(digest(files.BASE.bytes), digest(files.LIVE.bytes));
        I.assertNotEqual(digest(files.BASE.bytes), digest(files.WORK.bytes));
        I.resizeWindow(700, 900);
        const mobile = await I.executeScript(() => {
            const box = document.querySelector('#branch-merge-dialog').getBoundingClientRect();
            const base = document.querySelector('[data-branch-version="BASE"]').getBoundingClientRect();
            const live = document.querySelector('[data-branch-version="LIVE"]').getBoundingClientRect();
            return {fits: box.left >= 0 && box.right <= innerWidth, stacked: live.top >= base.bottom, closeLabel: document.querySelector('#branch-merge-dialog header button').textContent, expectedCloseLabel: WJ.translate('button.close')};
        });
        I.assertTrue(mobile.fits); I.assertTrue(mobile.stacked); I.assertEqual(mobile.closeLabel, mobile.expectedCloseLabel);
        I.saveScreenshot('branch-p06-mobile.png');
        I.pressKey('Escape');
        I.waitForInvisible(dialog, 10);
        I.wjSetDefaultWindowSize();
        const after = await report(I, fixture);
        I.assertEqual(after.state, 'DRAFT');
        I.assertEqual(after.revision, before.revision);
        I.assertTrue(pageItem(after, fixture).conflict);
        I.assertEqual(pageItem(after, fixture).choice, null);
        save('browser-retained-demo', {before, editorLabels, files, mobile, after});
    });

    Scenario('P06 LIVE and WORK immediately affect actual News and Menu, then the disposable package publishes through UI', async ({ I }) => {
        const fixture = disposable();
        const before = await report(I, fixture);
        I.assertEqual(before.state, 'DRAFT', 'Use a fresh P06 disposable publishing fixture for each run.');
        I.assertTrue(pageItem(before, fixture).conflict);
        I.assertTrue(Boolean(fixture.livePageId), 'The disposable fixture must include a second conflict retained as LIVE.');
        const keepLive = before.items.find(item => item.type === 'DOCUMENT' && item.objectKey === String(fixture.livePageId));
        I.assertTrue(keepLive.conflict);
        I.assertEqual(before.items.filter(item => item.type === 'DOCUMENT').length, 20);
        const ownerBefore = await homeIdentity(I, fixture);
        // This real administration detail warms the production DocDetails repository before JDBC publication.
        const pageBefore = await pageDetails(I, fixture.pageId);
        const livePageBefore = await pageDetails(I, fixture.livePageId);
        I.assertEqual(pageBefore.title, pageItem(before, fixture).live.fields.title);
        I.assertEqual(ownerBefore.docId, Number(fixture.homeId));
        I.assertTrue(Boolean(ownerBefore.dateCreated));
        I.assertFalse(before.items.some(item => item.type === 'DOCUMENT' && item.objectKey === String(fixture.homeId)), 'The old owner page must have no WORK row during the media-only change.');
        const publicBefore = await render(I, fixture, false);
        I.assertEqual(publicBefore.news.ids.length, 19);
        await openComparison(I, fixture);
        await choose(I, 'LIVE', false);
        const liveChoice = await report(I, fixture);
        I.assertEqual(pageItem(liveChoice, fixture).choice, 'LIVE');
        const livePreview = await render(I, fixture, true);
        const liveNews = livePreview.news.items.find(item => item.id === Number(fixture.pageId));
        I.assertEqual(liveNews.title, pageItem(before, fixture).live.fields.title);
        I.assertTrue(livePreview.menu.links.some(link => link.href === pageItem(before, fixture).live.fields.virtual_path), 'MenuULLI must use the selected LIVE URL of the folder default page.');
        await choose(I, 'WORK', false);
        I.click(itemButton(fixture.livePageId));
        I.waitForVisible(detail, 10);
        I.see(keepLive.live.fields.title, `${detail} [data-branch-version="LIVE"]`);
        await choose(I, 'LIVE');
        I.see('Produkčný obsah zostáva zachovaný.', detail);
        const selected = await report(I, fixture);
        I.assertEqual(pageItem(selected, fixture).choice, 'WORK');
        I.assertEqual(selected.items.find(item => item.type === 'DOCUMENT' && item.objectKey === String(fixture.livePageId)).choice, 'LIVE');
        const workPreview = await render(I, fixture, true);
        const workNews = workPreview.news.items.find(item => item.id === Number(fixture.pageId));
        I.assertEqual(workNews.title, pageItem(before, fixture).work.fields.title);
        I.assertEqual(workPreview.news.ids.length, 21);
        I.assertTrue(workPreview.menu.links.some(link => link.href === pageItem(before, fixture).work.fields.virtual_path), 'MenuULLI must use the selected WORK URL of the folder default page.');
        const liveNewsRetained = workPreview.news.items.find(item => item.id === Number(fixture.livePageId));
        I.assertEqual(liveNewsRetained.title, keepLive.live.fields.title);
        I.assertEqual(liveNewsRetained.perex, keepLive.live.fields.html_data);
        I.assertTrue(liveNewsRetained.links.some(link => link.href === keepLive.live.fields.virtual_path));
        I.assertTrue(workPreview.menu.links.some(link => link.text === keepLive.live.fields.navbar && link.href === keepLive.live.fields.virtual_path), 'The moved tree must retain the second page\'s chosen LIVE navigation and URL.');
        I.assertNotEqual(livePreview.menu.markup, workPreview.menu.markup);
        const oldOwner = workPreview.news.items.find(item => item.id === Number(fixture.homeId));
        I.assertTrue(oldOwner.media.some(media => media.id === Number(fixture.mediaId) && media.title === 'autotest P06 WORK media'));
        I.assertTrue(workPreview.news.items.some(item => item.media.some(media => media.id === Number(fixture.newMediaId))));
        const files = await binaries(I, fixture);
        I.assertEqual(files.WORK.status, 200);
        save('browser-before-publish', {fixture, before, selected, ownerBefore, pageBefore, livePageBefore, publicBefore, livePreview, workPreview, files});
        I.saveScreenshot('branch-p06-comparison.png');
        I.click('#branch-merge-publish');
        I.waitForVisible('#branch-merge-confirmation', 10);
        I.see('Publikujú sa iba zmeny uvedené v tomto prehľade.', '#branch-merge-confirmation');
        I.click('#branch-merge-confirm');
        I.waitForText('Publikovaná', 90, '#branch-merge-title');
        I.waitForText('Publikovanie je dokončené.', 30, '#branch-merge-progress');
        const published = await report(I, fixture);
        I.assertEqual(published.state, 'MERGED');
        I.assertTrue(published.items.every(item => item.applyState === 'DONE'));
        I.assertEqual(published.lastError, null);
        const publicAfter = await render(I, fixture, false);
        I.assertDeepEqual(publicAfter.news, workPreview.news, 'N10/N11: real NewsApp/NewsActionBean IDs, order, count, content, author, dates and media must equal the preview.');
        I.assertDeepEqual(publicAfter.menu, workPreview.menu, 'M09/M11: real menu_ul_li.jsp/MenuULLI markup, active classes, order and links must equal the preview.');
        const ownerAfter = await homeIdentity(I, fixture);
        const pageAfter = await pageDetails(I, fixture.pageId);
        const livePageAfter = await pageDetails(I, fixture.livePageId);
        I.assertDeepEqual(ownerAfter, ownerBefore, 'Publishing only media on the old page must preserve its SAVE_DATE, author and perex groups.');
        I.assertEqual(pageAfter.docId, pageBefore.docId);
        I.assertEqual(pageAfter.title, pageItem(before, fixture).work.fields.title, 'The warmed DocDetails repository must return published WORK content.');
        I.assertEqual(pageAfter.data, pageItem(before, fixture).work.fields.data);
        I.assertEqual(pageAfter.authorId, pageBefore.authorId);
        I.assertEqual(pageAfter.dateCreated, pageBefore.dateCreated);
        I.assertDeepEqual(livePageAfter, livePageBefore, 'A page retained as LIVE keeps its production content, URL, author and SAVE_DATE even when its folder moves.');
        for (const item of published.items.filter(item => item.type === 'DOCUMENT')) {
            const reviewed = selected.items.find(candidate => candidate.type === 'DOCUMENT' && candidate.objectKey === item.objectKey);
            const target = reviewed.choice === 'LIVE' ? reviewed.live : reviewed.work;
            I.assertEqual(item.live.fields.date_created, target.fields.date_created);
            I.assertEqual(item.live.fields.author_id, target.fields.author_id);
            I.assertDeepEqual(item.live.fields.perex_group, target.fields.perex_group);
        }
        const publicFile = await I.executeScript(async file => {
            const response = await fetch(file, {cache: 'no-store'});
            return {status: response.status, bytes: Array.from(new Uint8Array(await response.arrayBuffer()))};
        }, fixture.file);
        I.assertEqual(publicFile.status, 200);
        I.assertEqual(digest(publicFile.bytes), digest(files.WORK.bytes));
        save('browser-after-publish', {published, publicAfter, ownerAfter, pageAfter, livePageAfter, publicFile});
        I.scrollTo('#branch-merge-progress');
        I.saveScreenshot('branch-p06-published.png');
        const retained = await report(I, retain());
        I.assertEqual(retained.state, 'DRAFT');
        I.assertTrue(pageItem(retained, retain()).conflict);
        I.assertEqual(pageItem(retained, retain()).choice, null);
        I.amOnPage(fixture.url);
        I.waitForVisible('ol.p06-news', 30);
        I.see('autotest P06 WORK media', '.p06-news');
        I.seeElement(`.p06-media[data-media="${fixture.newMediaId}"]`);
        I.dontSeeElement('#branch-preview-status');
        I.saveScreenshot('branch-p06-public.png');
    });

    Scenario('P06 the existing trash recovery restores the deleted subtree with its original nesting, IDs and full page', async ({ I, DT, Document }) => {
        const fixture = read('/tmp/webjet-p06-trash.properties');
        Document.setConfigValue('editorDisableHistory', 'true', true);
        const before = await trashDetails(I, fixture);
        I.assertNotEqual(before.section.parentGroupId, Number(fixture.root), 'The prepared subtree must start under the existing trash.');
        I.assertEqual(before.nested.parentGroupId, Number(fixture.section));
        I.assertEqual(before.page.groupId, Number(fixture.nested));
        I.assertFalse(before.page.available);
        I.assertTrue(Boolean(before.page.title)); I.assertTrue(Boolean(before.page.data));
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.section}`);
        I.waitForEnabled('#branch-selection', 30);
        I.seeInField('#branch-selection', '0');
        DT.waitForLoader();
        I.waitForElement('#pills-trash-tab.active', 20);
        I.waitForVisible('.tree-col .dt-header-row .buttons-recover', 20);
        I.click('.tree-col .dt-header-row .buttons-recover');
        I.waitForVisible('button[id^="confirmationYes"]', 10);
        I.see('Obnovenie priečinka sa týka aj všetkých pod-priečinkov');
        I.click('button[id^="confirmationYes"]');
        I.waitForText('Priečinok bol úspešne obnovený', 30);
        I.waitForInvisible('.tree-col .buttons-recover.processing', 30);
        DT.waitForLoader();
        const after = await trashDetails(I, fixture);
        I.assertEqual(after.section.groupId, Number(fixture.section));
        I.assertEqual(after.section.parentGroupId, Number(fixture.root));
        I.assertEqual(after.nested.groupId, Number(fixture.nested));
        I.assertEqual(after.nested.parentGroupId, Number(fixture.section));
        I.assertEqual(after.page.docId, Number(fixture.page));
        I.assertEqual(after.page.groupId, Number(fixture.nested));
        I.assertTrue(after.page.available);
        I.assertEqual(after.page.title, before.page.title);
        I.assertEqual(after.page.data, before.page.data);
        I.assertEqual(after.page.authorId, before.page.authorId);
        I.assertEqual(after.page.dateCreated, before.page.dateCreated);
        save('browser-subtree-recovery', {fixture, before, after, historyDisabledDuringRecovery: true});
        I.amOnPage(`/showdoc.do?docid=${fixture.page}`);
        I.waitForText(before.page.title, 30, 'h1');
        I.saveScreenshot('branch-p06-subtree-restored.png');
        Document.setConfigValue('editorDisableHistory', 'false', true);
    });

    Scenario('P06 a persisted partial failure shows completed and pending items and resumes without rewriting a completed page', async ({ I }) => {
        const fixture = read('/tmp/webjet-p06-partial.properties');
        if (!fixture.groupId) fixture.groupId = fixture.root;
        const before = await report(I, fixture);
        I.assertEqual(before.state, 'MERGE_FAILED', 'Prepare a fresh retained partial fixture before this scenario.');
        const completed = before.items.filter(item => item.applyState === 'DONE');
        const errors = before.items.filter(item => item.applyState === 'ERROR' || item.error);
        const pending = before.items.length - completed.length - errors.length;
        I.assertTrue(completed.length > 0);
        I.assertTrue(pending > 0);
        const first = completed.find(item => item.type === 'DOCUMENT');
        I.assertTrue(Boolean(first), 'The persisted failure must follow at least one completed document write.');
        const firstBefore = await pageDetails(I, first.objectKey);
        const compareFixture = {...fixture, pageId: first.objectKey};
        await openComparison(I, compareFixture);
        I.see('Publikovanie zlyhalo', '#branch-merge-title');
        I.see(`Dokončené: ${completed.length} z ${before.items.length} · chyby: ${errors.length} · zostáva: ${pending}`, '#branch-merge-progress');
        I.see('Už dokončené zmeny sú verejné.', '#branch-merge-progress');
        I.dontSee('Otvoriť pracovnú verziu v editore', detail);
        const writtenError = errors.find(item => ['DATA', 'REFERENCES', 'TRASH_WAIT', 'TRASH_ROOT', 'EFFECTS'].includes(item.applyPhase));
        I.assertTrue(Boolean(writtenError), 'The prepared failure must identify its already-written content phase.');
        const errorRow = `${dialog} tr[data-branch-type="${writtenError.type}"][data-branch-key="${writtenError.objectKey}"]`;
        I.see('Obsah zapísaný, chyba dokončenia', errorRow);
        I.click(locate(errorRow).find('button'));
        I.waitForVisible(detail, 10);
        I.see(writtenError.applyPhase === 'TRASH_WAIT' ? 'Stránka je už nedostupná.' : 'Obsah tejto položky už môže byť verejný.', detail);
        I.scrollTo('#branch-merge-progress');
        I.saveScreenshot('branch-p06-partial-failure.png');
        I.click('Skontrolovať zmeny', dialog);
        I.waitForInvisible(busy, 30);
        I.waitForEnabled('#branch-merge-publish', 30);
        I.click('#branch-merge-publish');
        I.waitForVisible('#branch-merge-confirmation', 10);
        I.see('Znova overiť a pokračovať', '#branch-merge-confirmation');
        I.click('#branch-merge-confirm');
        I.waitForText('Publikovaná', 90, '#branch-merge-title');
        I.waitForText('Publikovanie je dokončené.', 30, '#branch-merge-progress');
        const after = await report(I, fixture);
        I.assertEqual(after.state, 'MERGED');
        I.assertTrue(after.items.every(item => item.applyState === 'DONE'));
        const firstAfter = await pageDetails(I, first.objectKey);
        I.assertDeepEqual(firstAfter, firstBefore, 'Resume must preserve the already-published document, author and SAVE_DATE.');
        for (const old of completed) {
            const current = after.items.find(item => item.type === old.type && item.objectKey === old.objectKey);
            I.assertEqual(current.applyState, 'DONE');
            I.assertEqual(current.applyPhase, old.applyPhase);
        }
        save('browser-partial-resume', {fixture, before, firstBefore, after, firstAfter});
        I.scrollTo('#branch-merge-progress');
        I.saveScreenshot('branch-p06-resumed.png');
    });

    Scenario('P06 comparison, original binaries and publishing require an authenticated administrator', async ({ I }) => {
        const fixture = retain();
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        const denied = await I.executeScript(async f => {
            const detail = await fetch(`/admin/rest/branches/${f.branchId}`);
            const file = await fetch(`/admin/rest/branches/${f.branchId}/file-version?path=${encodeURIComponent(f.file)}&version=BASE`);
            const publish = await fetch(`/admin/rest/branches/${f.branchId}/merge`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({expectedRevision: 0, preparationHash: 'unauthorized-autotest'})});
            return {detail: detail.status, file: file.status, publish: publish.status};
        }, fixture);
        I.assertEqual(denied.detail, 403);
        I.assertEqual(denied.file, 403);
        I.assertEqual(denied.publish, 403);
        save('browser-auth-boundary', denied);
    });
}
