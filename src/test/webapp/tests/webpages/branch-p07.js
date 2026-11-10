Feature('webpages.branch-p07').tag('@singlethread');

if (process.env.WEBJET_P07_BROWSER === 'true') {
    const fs = require('fs');
    const path = require('path');
    const read = file => Object.fromEntries(fs.readFileSync(file, 'utf8').trim().split('\n').filter(line => line && !line.startsWith('#')).map(line => {
        const separator = line.indexOf('=');
        return [line.slice(0, separator), line.slice(separator + 1)];
    }));
    const fixture = role => read(`/tmp/webjet-p07-${role}.properties`);
    const credentials = read('/tmp/webjet-p05-login.properties');
    const evidence = path.resolve(process.env.WEBJET_P07_EVIDENCE || '../../../build/test-results/p07');
    const dialog = '#branch-merge-dialog';
    const busy = `${dialog} .pg-branch-merge__content[aria-busy=true]`;
    const workTitle = 'autotest P06 WORK';
    if (!/^http:\/\/(iwcm\.interway\.sk|127\.0\.0\.1|localhost):8086\/?$/.test(process.env.CODECEPT_URL || '')) {
        throw new Error('P07 publishing tests require the isolated local POC on port 8086.');
    }

    function save(name, value) {
        fs.mkdirSync(evidence, {recursive: true});
        fs.writeFileSync(path.join(evidence, name+'.json'), JSON.stringify(value, null, 2));
    }

    async function login(I, root) {
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForEnabled('#branch-selection', 30);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${root}`);
        I.waitForEnabled('#branch-selection', 30);
    }

    async function report(I, f) {
        return I.executeScript(async id => {
            const response = await fetch(`/admin/rest/branches/${id}/progress`, {cache: 'no-store'});
            if (!response.ok) throw new Error('P07 report: '+response.status+' '+await response.text());
            return response.json();
        }, f.branchId);
    }

    async function open(I, f) {
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${f.groupId}#branchCompare=${f.branchId}`);
        // A fragment-only navigation does not initialize the screen again.
        I.refreshPage();
        I.waitForVisible(dialog, 30);
        I.waitForElement(`${dialog} [data-branch-compare="${f.pageId}"]`, 30);
        I.waitForInvisible(busy, 30);
    }

    async function render(I, f, preview) {
        return I.executeScript(async ({fixture: f, preview}) => {
            const prefix = preview ? `/admin/webpages/branch-preview/${f.branchId}` : '';
            const response = await fetch(prefix+f.url, {cache: 'no-store'});
            if (!response.ok) throw new Error('P07 render: '+response.status+' '+await response.text());
            const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
            const news = doc.querySelector('ol.p06-news');
            const menu = doc.querySelector('#p06-menu');
            if (!news || !menu) throw new Error('The actual NewsActionBean/MenuULLI fixture render is missing.');
            const normalize = value => String(value || '').replaceAll(location.origin, '').replace(/\/admin\/webpages\/branch-preview\/\d+/g, '');
            return {
                news: {markup: normalize(news.innerHTML), ids: [...news.querySelectorAll('li[data-id]')].map(item => Number(item.dataset.id)),
                    items: [...news.querySelectorAll('li[data-id]')].map(item => ({id: Number(item.dataset.id), author: item.dataset.author, saveDate: item.dataset.date,
                        title: item.querySelector('a')?.textContent.trim(), perex: item.querySelector('span')?.textContent.trim(),
                        links: [...item.querySelectorAll('a')].map(link => ({text: link.textContent.trim(), href: normalize(link.getAttribute('href'))})),
                        media: [...item.querySelectorAll('.p06-media')].map(media => ({id: Number(media.dataset.media), title: media.textContent.trim(), href: normalize(media.getAttribute('href'))}))}))},
                menu: {markup: normalize(menu.innerHTML), links: [...menu.querySelectorAll('a')].map(link => ({text: link.textContent.trim(), href: normalize(link.getAttribute('href')), classes: link.className, itemClasses: link.closest('li')?.className || ''}))},
                banner: doc.querySelector('#branch-preview-status')?.textContent.trim() || null
            };
        }, {fixture: f, preview});
    }

    async function schedule(I, f, instant, keyboard = false) {
        I.waitForEnabled('#branch-merge-schedule', 30);
        I.click('#branch-merge-schedule');
        I.waitForVisible('#branch-merge-schedule-form', 10);
        const value = await I.executeScript(at => {
            const parts = new Intl.DateTimeFormat('en-CA', {timeZone: 'Europe/Bratislava', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23'}).formatToParts(new Date(at));
            const part = type => parts.find(item => item.type === type).value;
            return `${part('year')}-${part('month')}-${part('day')}T${part('hour')}:${part('minute')}`;
        }, instant);
        // CodeceptJS fillField types by segments into native datetime-local; Playwright fill uses its ISO value.
        await I.usePlaywrightTo('fill native scheduling date and time', async ({page}) => { await page.locator('#branch-merge-publish-at').fill(value); });
        if (keyboard) { I.focus('#branch-merge-schedule-confirm'); I.pressKey('Enter'); }
        else I.click('#branch-merge-schedule-confirm');
        I.waitForInvisible(busy, 30);
        I.waitForInvisible('#branch-merge-schedule-form', 30);
        I.waitForVisible('#branch-merge-scheduled', 30);
        I.waitForEnabled('#branch-merge-schedule', 30);
        const result = await report(I, f);
        I.assertEqual(result.state, 'SCHEDULED');
        I.assertEqual(result.publishAt, Math.floor(instant / 60000) * 60000);
        I.assertEqual(result.timeZone, 'Europe/Bratislava');
        return result;
    }

    async function publicFile(I, f) {
        return I.executeScript(async file => {
            const response = await fetch(file, {cache: 'no-store'});
            return {status: response.status, text: await response.text()};
        }, f.file);
    }

    function semantic(rendered, f) {
        const root = f.url.replace(/\/$/, '');
        const normalize = value => String(value || '').replaceAll(root, '/fixture').replaceAll(f.file, '/fixture-file')
            .replace(/autotest P06 (?:p07-)?(?:manual-|attention-|preview-)?(?:test-)?[a-f0-9]{8}/g, 'autotest P06 fixture');
        return {
            news: rendered.news.items.map(item => ({author: item.author, saveDate: item.saveDate, title: item.title, perex: item.perex,
                links: item.links.map(link => ({text: link.text, href: normalize(link.href)})),
                media: item.media.map(media => ({title: media.title, href: normalize(media.href)}))})),
            menu: rendered.menu.links.map(link => ({...link, text: normalize(link.text), href: normalize(link.href), itemClasses: link.itemClasses.replace(/(?:group|page)\d+/g, 'fixtureId')}))
        };
    }

    Before(async ({ I }) => {
        I.switchTo(); I.wjSetDefaultWindowSize();
        await login(I, fixture('scheduled').groupId);
        const translations = await I.executeScript(() => Object.fromEntries(['schedule', 'reschedule', 'unschedule', 'reopen', 'scheduledNote', 'attentionNote'].map(name => [name, WJ.translate(`branches.merge.${name}.js`)])));
        for (const [name, value] of Object.entries(translations)) I.assertNotEqual(value, `branches.merge.${name}.js`);
    });

    Scenario('P07 moving and cancelling a term changes actual News preview time; planning works on mobile and with Enter', async ({ I }) => {
        const f = fixture('preview');
        I.assertAbove(Number(f.publishCutoff), Date.now()+120000, 'Prepare a fresh future News cutoff before running P07.');
        const before = await report(I, f);
        I.assertEqual(before.state, 'DRAFT');
        const current = await render(I, f, true);
        I.assertFalse(current.news.ids.includes(Number(f.pageId)));
        I.assertTrue(current.news.ids.includes(Number(f.secondPageId)));
        await open(I, f);
        I.resizeWindow(390, 844);
        const firstTerm = await schedule(I, f, Number(f.publishCutoff)-60000, true);
        I.dontSee('Upravovať web', dialog);
        I.see('Balík je uzavretý pre editáciu.', '#branch-merge-scheduled');
        const first = await render(I, f, true);
        I.assertFalse(first.news.ids.includes(Number(f.pageId)));
        I.assertTrue(first.news.ids.includes(Number(f.secondPageId)));
        I.assertContain(first.banner, 'Europe/Bratislava');
        I.assertContain(first.banner, 'Novinky vyhodnotené k');
        const shiftedTerm = await schedule(I, f, Number(f.publishCutoff)+60000, true);
        const shifted = await render(I, f, true);
        I.assertTrue(shifted.news.ids.includes(Number(f.pageId)));
        I.assertFalse(shifted.news.ids.includes(Number(f.secondPageId)));
        I.assertNotEqual(shifted.banner, first.banner);
        I.assertNotEqual(shifted.news.markup, first.news.markup);
        I.click('#branch-merge-schedule');
        I.waitForVisible('#branch-merge-schedule-form', 10);
        const mobile = await I.executeScript(() => {
            const dialog = document.querySelector('#branch-merge-dialog').getBoundingClientRect();
            const form = document.querySelector('#branch-merge-schedule-form').getBoundingClientRect();
            const input = document.querySelector('#branch-merge-publish-at');
            const bounds = input.getBoundingClientRect();
            return {fits: dialog.left >= 0 && dialog.right <= innerWidth && form.right <= dialog.right && bounds.right <= form.right,
                label: document.querySelector(`label[for="${input.id}"]`)?.textContent, describedBy: input.getAttribute('aria-describedby'), focused: document.activeElement === input};
        });
        I.assertTrue(mobile.fits); I.assertTrue(mobile.focused); I.assertTrue(Boolean(mobile.label)); I.assertTrue(Boolean(mobile.describedBy));
        I.saveScreenshot('branch-p07-mobile-schedule.png');
        I.click('Zrušiť', '#branch-merge-schedule-form');
        I.click('#branch-merge-unschedule'); I.waitForVisible('#branch-merge-unschedule-confirm', 10);
        I.focus('#branch-merge-unschedule-confirm'); I.pressKey('Enter');
        I.waitForInvisible(busy, 30); I.waitForEnabled('#branch-merge-publish', 30);
        const cancelled = await report(I, f);
        I.assertEqual(cancelled.state, 'DRAFT'); I.assertEqual(cancelled.publishAt, null); I.assertEqual(cancelled.scheduledBy, null);
        I.assertDeepEqual(cancelled.items.map(item => item.work), before.items.map(item => item.work), 'Cancelling preserves every WORK snapshot.');
        const afterCancel = await render(I, f, true);
        I.assertDeepEqual(afterCancel.news, current.news, 'N12: cancelled schedule evaluates News at current time again.');
        I.assertNotEqual(afterCancel.banner, shifted.banner);
        I.assertNotContain(afterCancel.banner, 'Budúci termín');
        save('browser-preview-time', {f, firstTerm, shiftedTerm, current, first, shifted, mobile, cancelled, afterCancel});
        I.pressKey('Escape'); I.waitForInvisible(dialog, 10); I.wjSetDefaultWindowSize();
    });

    Scenario('P07 the real cron honors a moved term after logout and stops a new conflict before publishing any item', async ({ I, DTE }) => {
        const scheduled = fixture('scheduled'), manual = fixture('manual'), attention = fixture('attention');
        const scheduledBefore = await report(I, scheduled);
        let scheduledPreview, originalTerm, newTerm, manualAfter, manualPublic;
        if (process.env.WEBJET_P07_RESUME === 'true') {
            const saved = JSON.parse(fs.readFileSync(path.join(evidence, 'browser-scheduled-before-wait.json'), 'utf8'));
            ({scheduledPreview, originalTerm, newTerm, manualAfter, manualPublic} = saved);
            I.assertTrue(['SCHEDULED', 'MERGED'].includes(scheduledBefore.state));
        } else {
            I.assertEqual(scheduledBefore.state, 'DRAFT'); I.assertEqual((await report(I, attention)).state, 'DRAFT');
            scheduledPreview = await render(I, scheduled, true);
            const manualPreview = await render(I, manual, true);
            originalTerm = Math.ceil(Date.now()/60000)*60000+120000;
            newTerm = originalTerm+120000;
            await open(I, scheduled);
            const first = await schedule(I, scheduled, originalTerm, true);
            const shifted = await schedule(I, scheduled, newTerm, true);
            I.assertAbove(shifted.revision, first.revision);
            I.saveScreenshot('branch-p07-moved-term.png');
            await open(I, attention);
            const plannedAttention = await schedule(I, attention, originalTerm, true);
            I.pressKey('Escape'); I.waitForInvisible(dialog, 10);
            // Edit the actual LIVE page through the normal editor after the package has been scheduled.
            I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${attention.pageId}`);
            I.waitForEnabled('#branch-selection', 30); I.seeInField('#branch-selection', '0');
            DTE.waitForEditor();
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            I.fillField('#DTE_Field_title', 'autotest P07 LIVE conflict after scheduling');
            DTE.save(undefined, true); DTE.waitForModalClose();
            const conflictBeforeTerm = await report(I, attention);
            I.assertEqual(conflictBeforeTerm.state, 'SCHEDULED');
            I.assertTrue(conflictBeforeTerm.items.some(item => item.type === 'DOCUMENT' && item.objectKey === attention.pageId && item.conflict));
            // The same target fixture is published manually with the existing P06 controls.
            await open(I, manual); I.waitForEnabled('#branch-merge-publish', 30);
            I.click('#branch-merge-publish'); I.waitForVisible('#branch-merge-confirm', 10); I.click('#branch-merge-confirm');
            I.waitForText('Publikovaná', 90, '#branch-merge-title');
            manualAfter = await report(I, manual); manualPublic = await render(I, manual, false);
            I.assertEqual(manualAfter.state, 'MERGED'); I.assertDeepEqual(manualPublic.news, manualPreview.news); I.assertDeepEqual(manualPublic.menu, manualPreview.menu);
            save('browser-scheduled-before-wait', {scheduled, manual, attention, originalTerm, newTerm, first, shifted, plannedAttention, conflictBeforeTerm, scheduledPreview, manualPreview, manualAfter, manualPublic});
        }
        // Logout is deliberate: only the existing background cron can publish the pending package.
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        const anonymous = await I.executeScript(async id => (await fetch(`/admin/rest/branches/${id}`)).status, scheduled.branchId);
        I.assertEqual(anonymous, 403);
        I.waitForFunction(at => Date.now() >= at, [originalTerm+15000], 220);
        if (Date.now() < newTerm) {
            const originalFiles = {scheduled: await publicFile(I, scheduled), attention: await publicFile(I, attention)};
            I.assertEqual(originalFiles.scheduled.text, 'autotest P06 BASE attachment', 'The package must not publish at its original term.');
            I.assertEqual(originalFiles.attention.text, 'autotest P06 BASE attachment', 'A new conflict must stop all WORK items.');
            save('browser-original-term-not-published', {checkedAt: Date.now(), originalTerm, newTerm, anonymous, originalFiles});
        } else {
            // A resumed verification may start after publication; keep the separately captured original-term evidence.
            I.assertTrue(fs.existsSync(path.join(evidence, 'original-term-db.txt')));
        }
        I.waitForFunction(async ({at, file}) => {
            if (Date.now() < at || Date.now() < (window.p07NextPoll || 0)) return false;
            window.p07NextPoll = Date.now()+1000;
            return (await (await fetch(file, {cache: 'no-store'})).text()) === 'autotest P06 WORK attachment';
        }, [{at: newTerm, file: scheduled.file}], 260);
        await login(I, scheduled.groupId);
        await open(I, scheduled);
        I.waitForText('Publikovaná', 90, '#branch-merge-title');
        const scheduledPublic = await render(I, scheduled, false);
        I.assertDeepEqual(scheduledPublic.news, scheduledPreview.news, 'Actual cron output equals the reviewed News preview, including author and SAVE_DATE.');
        I.assertDeepEqual(scheduledPublic.menu, scheduledPreview.menu, 'Actual cron output equals the reviewed MenuULLI output.');
        I.assertDeepEqual(semantic(scheduledPublic, scheduled), semantic(manualPublic, manual), 'Scheduled and manual publication have equivalent News, media and menu behavior.');
        I.assertEqual((await publicFile(I, scheduled)).text, 'autotest P06 WORK attachment');
        const completed = await report(I, scheduled);
        I.assertEqual(completed.state, 'MERGED'); I.assertTrue(completed.items.every(item => item.applyState === 'DONE'));
        for (const item of completed.items.filter(item => item.type === 'DOCUMENT')) {
            const original = scheduledBefore.items.find(candidate => candidate.type === 'DOCUMENT' && candidate.objectKey === item.objectKey);
            I.assertEqual(item.live.fields.author_id, original.work.fields.author_id);
            I.assertEqual(item.live.fields.date_created, original.work.fields.date_created);
        }
        I.saveScreenshot('branch-p07-cron-published.png');
        await open(I, attention);
        I.waitForText('Vyžaduje zásah', 30, '#branch-merge-title');
        const stopped = await report(I, attention);
        I.assertEqual(stopped.state, 'NEEDS_ATTENTION'); I.assertContain(stopped.lastError.toLowerCase(), `document:${attention.pageId}`);
        I.assertFalse(stopped.items.some(item => item.applyState === 'DONE' || item.applyPhase || item.live.fields.title?.startsWith(workTitle)));
        I.see(stopped.lastError, dialog); I.see('Žiadna položka sa nezverejnila.', dialog);
        I.seeElement('#branch-merge-reopen'); I.dontSeeElement('#branch-merge-publish');
        const attentionAfter = await render(I, attention, false);
        I.assertFalse(attentionAfter.news.items.some(item => item.title.startsWith(workTitle)));
        I.assertTrue(attentionAfter.news.items.some(item => item.title === 'autotest P07 LIVE conflict after scheduling'));
        I.assertEqual((await publicFile(I, attention)).text, 'autotest P06 BASE attachment');
        I.saveScreenshot('branch-p07-needs-attention.png');
        const retained = await report(I, {branchId: 1315});
        I.assertEqual(retained.state, 'DRAFT'); I.assertEqual(retained.revision, 25);
        save('browser-scheduler-results', {checkedAt: Date.now(), originalTerm, newTerm, completed, scheduledPublic, manualAfter, manualPublic, stopped, attentionAfter, retainedDemo: {id: retained.id, state: retained.state, revision: retained.revision}});
    });
}
