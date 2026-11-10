Feature('webpages.branch-p08-live').tag('@singlethread');

if (process.env.WEBJET_P08_REFERENCE === 'true') {
    const fs = require('fs');
    const path = require('path');
    const enabled = process.env.WEBJET_BRANCHES_ENABLED === 'true';
    const evidence = path.resolve('../../../build/test-results/p08/followup');
    const resultFile = path.join(evidence, 'live-' + enabled + '.json');
    const result = fs.existsSync(resultFile) ? JSON.parse(fs.readFileSync(resultFile, 'utf8')) : {enabled};
    const suffix = 'autotest-p08-live-' + enabled + '-' + Date.now();
    if (process.env.CODECEPT_URL !== 'http://iwcm.interway.sk:8086') throw new Error('P08 LIVE tests require the disposable local reference profile.');
    fs.mkdirSync(evidence, {recursive: true});
    const save = () => fs.writeFileSync(resultFile, JSON.stringify(result, null, 2));

    async function read(I, url) {
        return I.executeScript(async url => {
            const response = await fetch(url, {headers: {'X-CSRF-Token': window.csrfToken}});
            if (!response.ok) throw new Error('Reference read failed: ' + url + ': ' + response.status);
            return response.json();
        }, url);
    }

    async function publicPage(I, id) {
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        return I.executeScript(async id => {
            const page = await (await fetch('/admin/rest/web-pages/' + id, {headers: {'X-CSRF-Token': window.csrfToken}})).json();
            const response = await fetch(page.virtualPath, {credentials: 'omit', cache: 'no-store'});
            return {status: response.status, text: await response.text()};
        }, id);
    }

    async function pageApproval(I, DT, title, approve) {
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=5343');
        I.clickCss('#pills-waiting-tab');
        DT.waitForLoader();
        DT.filterContains('title', title);
        I.click(title, '#datatableInit_wrapper');
        I.switchToNextTab();
        I.waitForElement('#approveFormId', 20);
        I.switchTo('#approveFormId');
        I.waitForElement('textarea[name=notes]', 20);
        I.fillField('textarea[name=notes]', suffix);
        I.clickCss(approve ? '#tabMenu1 .btn.green' : '#tabMenu1 input.btn.red');
        I.waitForText(approve ? 'Web stránka bola schválená, je publikovaná na verejnej časti web stránky' : 'Pripomienky k web stránke boli zaslané', 20);
        I.switchTo();
        I.closeOtherTabs();
    }

    async function groupApproval(I, scheduleId, approve) {
        I.amOnPage('/admin/v9/webpages/approve-group/?scheduleId=' + scheduleId);
        I.waitForElement('#note', 20);
        I.fillField('#note', suffix);
        I.clickCss(approve ? 'button.btn-success' : 'button.btn-danger');
        I.waitForText('Schválenie/zamietnutie priečinka bolo úspešné', 20, '.alert-success');
    }

    Scenario('P08 configure explicit LIVE reference profile', async ({I, Document}) => {
        await I.relogin('admin');
        Document.setConfigValue('branchesEnabled', String(enabled), true);
        Document.setConfigValue('sendMailSaveEmailPath', '/WEB-INF/tmp/p08-regression-mail', true);
        Document.setConfigValue('translationEngineClasses', '', true);
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        const domain = await I.grabTextFrom('div.js-domain-toggler button');
        if (!domain.includes('demo.webjetcms.sk')) Document.switchDomain('demo.webjetcms.sk');
        const context = await I.executeScript(() => window.branchScreen);
        await I.assertEqual(context.enabled, enabled);
        await I.assertEqual(context.branchId, 0);
        // Legacy roles can use LIVE without the separate permission for managing packages.
        result.context = await I.executeScript(() => ({
            enabled: window.branchScreen.enabled,
            allowed: window.branchScreen.allowed,
            hasBranchSelection: document.querySelector('#branch-selection') != null,
            branchId: window.branchScreen?.branchId
        }));
        save();
    });

    Scenario('P08 LIVE page approval rejection and publication', async ({I, DT, DTE, Document}) => {
        const title = suffix + '-page';
        await I.relogin('tester2');
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=5343');
        DT.waitForLoader();
        I.click(DT.btn.add_button);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        DTE.fillField('title', title);
        DTE.fillField('navbar', title);
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + title + '</p>');
        I.executeScript(() => webpagesDatatable.EDITOR.one('submitSuccess', (event, json) => {window.p08Page = json.data[0].docId;}));
        DTE.save();
        Document.notifyCheckAndClose('Žiadosť o schválenie vytvorenia novej web stránky dostal: Tester Playwright');
        const id = Number(await I.executeScript(() => window.p08Page));
        await I.assertTrue(id > 0);
        const before = await publicPage(I, id);
        await I.assertContain(before.text, 'Chyba 404 - požadovaná stránka neexistuje');
        await I.assertNotContain(before.text, title);
        await I.relogin('admin');
        await pageApproval(I, DT, title, true);
        const published = await publicPage(I, id);
        await I.assertEqual(published.status, 200);
        await I.assertContain(published.text, title);

        await I.relogin('tester2');
        I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + id);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + title + ' rejected-edit</p>');
        DTE.save();
        Document.notifyCheckAndClose('Žiadosť o schválenie web stránky dostal: Tester Playwright');
        await I.relogin('admin');
        await pageApproval(I, DT, title, false);
        const rejected = await publicPage(I, id);
        await I.assertEqual(rejected.status, 200);
        await I.assertNotContain(rejected.text, 'rejected-edit');
        await I.relogin('tester2');
        I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + id);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + title + ' approved-edit</p>');
        DTE.save();
        Document.notifyCheckAndClose('Žiadosť o schválenie web stránky dostal: Tester Playwright');
        const pendingEdit = await publicPage(I, id);
        await I.assertNotContain(pendingEdit.text, 'approved-edit');
        await I.relogin('admin');
        await pageApproval(I, DT, title, true);
        const updated = await publicPage(I, id);
        await I.assertContain(updated.text, 'approved-edit');
        result.page = {id, title, before, published, rejected, pendingEdit, updated};
        save();
    });

    Scenario('P08 LIVE folder approval rejection and applied fields', async ({I, DT, DTE, Document}) => {
        const name = suffix + '-folder';
        await I.relogin('tester2');
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=114389');
        DT.waitForLoader();
        I.click(DT.btn.tree_add_button);
        DTE.waitForEditor('groups-datatable');
        DTE.fillField('groupName', name);
        I.executeScript(() => groupsDatatable.EDITOR.one('submitSuccess', (event, json) => {window.p08Group = json.data[0].groupId;}));
        DTE.save('groups-datatable');
        Document.notifyCheckAndClose('Žiadosť o schválenie pridania nového priečinka dostal: Publish Notification, Tester Playwright');
        const id = Number(await I.executeScript(() => window.p08Group));
        await I.relogin('admin');
        let queue = await read(I, '/admin/rest/groups/toapprove/all?size=1000');
        const pending = queue.content.find(group => group.groupId === id);
        await I.assertTrue(pending != null);
        const before = await read(I, '/admin/rest/groups/' + id);
        await I.assertTrue(before.internal);
        await groupApproval(I, pending.schedulerId, true);
        const approved = await read(I, '/admin/rest/groups/' + id);
        await I.assertFalse(approved.internal);
        const homepage = await publicPage(I, approved.defaultDocId);
        await I.assertEqual(homepage.status, 200);
        await I.assertContain(homepage.text, name);
        await I.assertNotContain(homepage.text, 'Chyba 404 - požadovaná stránka neexistuje');

        const edits = [];
        for (const approve of [false, true]) {
            await I.relogin('tester2');
            I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=' + id);
            DT.waitForLoader();
            I.click(DT.btn.tree_edit_button);
            DTE.waitForEditor('groups-datatable');
            I.clickCss('#pills-dt-groups-datatable-fields-tab');
            DTE.fillField('fieldA', suffix + '-approved-' + approve);
            DTE.save('groups-datatable');
            Document.notifyCheckAndClose('Žiadosť o schválenie priečinka dostal: Publish Notification, Tester Playwright');
            await I.relogin('admin');
            queue = await read(I, '/admin/rest/groups/toapprove/all?size=1000');
            const change = queue.content.find(group => group.groupId === id);
            await I.assertTrue(change != null);
            const unchanged = await read(I, '/admin/rest/groups/' + id);
            await I.assertEqual(unchanged.fieldA, '');
            await groupApproval(I, change.schedulerId, approve);
            const after = await read(I, '/admin/rest/groups/' + id);
            await I.assertEqual(after.fieldA, approve ? suffix + '-approved-true' : '');
            edits.push({approve, scheduleId: change.schedulerId, after});
        }
        result.group = {id, before, approved, homepage, edits};
        save();
    });

    Scenario('P08 LIVE multigroup content reaches the existing slave', async ({I, DTE}) => {
        await I.relogin('admin');
        const text = suffix + '-multigroup-body';
        I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=70837');
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + text + '</p>');
        DTE.save();
        const master = await publicPage(I, 70837);
        const slave = await publicPage(I, 70838);
        await I.assertEqual(master.status, 200);
        await I.assertEqual(slave.status, 200);
        await I.assertContain(master.text, text);
        await I.assertContain(slave.text, text);
        result.multigroup = {masterId: 70837, slaveId: 70838, text, master, slave};
        save();
    });

    Scenario('P08 LIVE mirroring creates a copy and propagates content and ordering', async ({I, DT, DTE, Document}) => {
        await I.relogin('admin');
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        Document.switchDomain('mirroring.tau27.iway.sk');
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=113666');
        DT.waitForLoader();
        I.click(DT.btn.tree_add_button);
        DTE.waitForEditor('groups-datatable');
        DTE.fillField('groupName', suffix + '-mirror');
        I.executeScript(() => groupsDatatable.EDITOR.one('submitSuccess', (event, json) => {window.p08MirrorGroup = json.data[0].groupId;}));
        DTE.save('groups-datatable');
        const id = Number(await I.executeScript(() => window.p08MirrorGroup));
        const source = await read(I, '/admin/rest/groups/' + id);
        const siblings = await read(I, '/admin/rest/groups/all?groupId=113667');
        const mirror = siblings.content.find(group => group.syncId === source.syncId);
        await I.assertTrue(mirror != null);
        await I.assertTrue(source.syncId > 1);
        await I.assertEqual(mirror.groupName, source.groupName);

        const text = suffix + '-mirrored-content';
        I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + source.defaultDocId);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + text + '</p>');
        I.clickCss('#pills-dt-datatableInit-menu-tab');
        DTE.fillField('sortPriority', '845');
        DTE.save();
        const sourcePage = await read(I, '/admin/rest/web-pages/' + source.defaultDocId);
        const mirrorPage = await read(I, '/admin/rest/web-pages/' + mirror.defaultDocId);
        await I.assertContain(sourcePage.data, text);
        await I.assertContain(mirrorPage.data, text);
        await I.assertEqual(sourcePage.syncId, mirrorPage.syncId);
        await I.assertEqual(sourcePage.sortPriority, 845);
        await I.assertEqual(mirrorPage.sortPriority, 845);
        result.mirroring = {source, mirror, sourcePage, mirrorPage};
        save();
        I.logout();
    });
}
