Feature('webpages.branch-changes');

const id = 900001;
const name = 'autotest editorial version';
const modal = '#branchesDataTable_modal';
const basicTab = '#pills-dt-branchesDataTable-basic-tab';
const changesTab = '#pills-dt-branchesDataTable-changes-tab';
const changesTable = '#datatableFieldDTE_Field_changes';
const changesWrapper = changesTable + '_wrapper';
const scheduleTab = '#pills-dt-branchesDataTable-publishing-tab';
const reportRoute = new RegExp(`/admin/rest/branches/${id}/(?:progress|resolve|schedule|unschedule|merge|changes/revert)$`);
const rowRoute = new RegExp(`/admin/rest/branches/${id}$`);
const changesRoute = new RegExp(`/admin/rest/branches/${id}/changes/all(?:\\?.*)?$`);
const listRoute = /\/admin\/rest\/branches\/all\?/;

Before(({ login }) => login('admin'));
After(({ I }) => {
    I.stopMockingRoute(reportRoute);
    I.stopMockingRoute(changesRoute);
    I.stopMockingRoute(rowRoute);
    I.stopMockingRoute(listRoute);
});

Scenario('Working version editor filters conflicts and keeps scheduling separate from publishing', async ({ I, DT }) => {
    const snapshot = title => ({state: 'ACTIVE', fields: {title}});
    const absent = {state: 'ABSENT', fields: {}};
    const sectionPath = '/Autotest & redakcia';
    const branch = {id, name, color: 'mint', state: 'DRAFT', revision: 1, description: 'autotest'};
    const report = {
        ...branch, ready: false, preparationHash: 'autotest-plan', errors: [], timeZone: 'Europe/Bratislava',
        items: [
            {type: 'DOCUMENT', objectKey: '12', label: 'autotest conflict', operation: 'UPSERT', conflict: true,
                base: snapshot('autotest original'), live: snapshot('autotest live'), work: snapshot('autotest draft'),
                liveHash: 'autotest-live', workHash: 'autotest-work', writeRequired: true, resolvable: true},
            {type: 'DOCUMENT', objectKey: '18', label: 'autotest new page', operation: 'UPSERT',
                base: absent, live: absent, work: snapshot('autotest new page'), workOnly: true, writeRequired: true, resolvable: true,
                folderPath: sectionPath + '/autotest folder'},
            {type: 'GROUP', objectKey: '15', label: 'autotest folder', operation: 'UPSERT', workHash: 'autotest-folder', writeRequired: true,
                folderPath: sectionPath}
        ]
    };
    const requests = [];
    await I.mockRoute(listRoute, route => route.fulfill({json: {content: [branch], totalElements: 1}}));
    await I.mockRoute(rowRoute, route => route.fulfill({json: branch}));
    const loads = [];
    await I.mockRoute(changesRoute, route => {
        loads.push(new URL(route.request().url()).search);
        const content = report.items.map(item => ({
            id: `${item.type}-${item.objectKey}`, type: item.type, objectKey: item.objectKey, label: item.label,
            folderPath: item.folderPath,
            operation: item.workOnly ? 'new' : 'updated', status: item.conflict ? 'conflict' : 'willPublish',
            comparison: item.workOnly ? 'viewItem' : item.conflict ? 'resolve' : 'compareItem', problem: Boolean(item.conflict),
            revertible: report.state === 'DRAFT'
        }));
        const options = {type: [{label: 'Stránky', value: 'DOCUMENT'}, {label: 'Priečinky', value: 'GROUP'}]};
        return route.fulfill({json: {content, options, totalElements: content.length, report}});
    });
    await I.mockRoute(reportRoute, route => {
        const request = route.request();
        const action = new URL(request.url()).pathname.split('/').pop();
        if (request.method() === 'POST') {
            const payload = request.postDataJSON();
            requests.push({action, payload});
            report.revision++;
            if (action === 'resolve') {
                Object.assign(report.items[0], {conflict: false, choice: payload.choice});
                report.ready = true;
            }
            if (action === 'revert') report.items = report.items.filter(item => item.objectKey !== String(payload.id));
            if (action === 'schedule') Object.assign(report, {state: 'SCHEDULED', publishAt: payload.publishAt});
            if (action === 'unschedule') Object.assign(report, {state: 'DRAFT', publishAt: null});
            if (action === 'merge') report.state = 'MERGING';
        }
        return route.fulfill({json: report});
    });

    I.amOnPage('/admin/v9/webpages/branches/');
    I.waitForText(name, 20, '#branchesDataTable');
    I.clickCss('#branchesDataTable tbody td.dt-select-td');
    I.clickCss('.buttons-branch-compare');
    I.waitForVisible(`${changesTab}[aria-selected="true"]`, 10);
    await I.waitForText('autotest conflict', 10, changesTable);
    const loaded = loads.length;
    I.assertAbove(loaded, 0);
    I.assertTrue(loads.every(query => !new URLSearchParams(query).has('size')));
    I.dontSeeElement(`${changesWrapper} [data-dtbtn="create"]`);
    I.dontSeeElement(`${changesWrapper} [data-dtbtn="remove"]`);
    I.seeElement('#branch-merge-publish:disabled');
    I.see(sectionPath + '/autotest folder', `${changesTable} tr[data-branch-key="18"] .pg-branch-change-path`);
    I.see(sectionPath, `${changesTable} tr[data-branch-key="15"] .pg-branch-change-path`);
    const conflictWarning = 'Pred publikovaním vyriešte konflikty na karte Zmeny.';
    I.waitForText(conflictWarning, 5, '#toast-container-webjet .toast-warning');
    I.dontSee(conflictWarning, modal);
    I.see('Zobraziť', `${changesTable} [data-branch-compare="18"]`);
    I.clickCss(`${changesTable} [data-branch-compare="18"]`);
    const detail = 'dialog.pg-branch-merge[open]';
    I.waitForVisible(detail, 10);
    I.see('autotest new page', `${detail} [data-branch-version="WORK"]`);
    I.dontSeeElement(`${detail} [data-branch-version="BASE"]`);
    I.dontSeeElement(`${detail} [data-branch-version="LIVE"]`);
    I.see('Vynechať položku z publikovania', detail);
    I.pressKey('Escape');
    I.waitForInvisible(detail, 10);
    DT.filterSelect('type', 'Priečinky', changesWrapper);
    I.see('autotest folder', changesTable);
    I.dontSee('autotest conflict', changesTable);
    DT.clearFilter('type');
    I.clickCss('label[for="branch-merge-problems"]');
    await I.dontSee('autotest folder', changesTable);
    I.assertEqual(loads.length, loaded);
    I.clickCss('[data-branch-compare="12"]');
    I.waitForVisible('dialog.pg-branch-merge', 10);
    I.checkOption('input[name="branch-merge-choice"][value="WORK"]');
    I.clickCss('#branch-merge-save-choice');
    I.waitForEnabled('#branch-merge-publish', 10);
    I.pressKey('Escape');
    I.waitForInvisible('dialog.pg-branch-merge', 10);
    I.seeElement(modal);
    I.clickCss('label[for="branch-merge-problems"]');
    I.clickCss(`${changesWrapper} [data-dtbtn="reload"]`);
    I.waitForInvisible('#panel-body-dt-branchesDataTable-changes[aria-busy=true]', 10);
    I.waitForText('autotest folder', 10, changesTable);
    I.clickCss(`${changesTable} tr[data-branch-key="15"] td.dt-select-td`);
    I.waitForEnabled(`${changesWrapper} .buttons-branch-revert`, 10);
    I.clickCss(`${changesWrapper} .buttons-branch-revert`);
    await I.waitForText('autotest folder', 10, '#branch-merge-confirmation');
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve']);
    I.clickCss('#branch-merge-revert-confirm');
    I.waitForInvisible(`${changesTable} tr[data-branch-key="15"]`, 10);

    I.clickCss(basicTab);
    I.fillField(`${modal} #DTE_Field_name`, 'autotest unsaved name');
    I.seeElement('#branch-merge-publish:disabled');
    I.see('Uložiť', `${modal} .DTE_Footer`);
    I.fillField(`${modal} #DTE_Field_name`, name);
    I.clickCss(scheduleTab);
    I.waitForEnabled('#branch-merge-schedule-confirm', 10);
    I.resizeWindow(390, 844);
    I.see(name, `${modal} .modal-title`);
    const inputFits = await I.executeScript(() => {
        const dialog = document.querySelector('#branchesDataTable_modal .modal-dialog').getBoundingClientRect();
        const input = document.querySelector('#branch-merge-publish-at').getBoundingClientRect();
        return innerWidth === 390 && input.left >= dialog.left && input.right <= dialog.right;
    });
    I.assertTrue(inputFits);
    // Native datetime-local values need an ISO wall time to fill all segments together.
    await I.usePlaywrightTo('enter a publication date', async ({page}) => {
        await page.locator('#branch-merge-publish-at').fill('2036-06-04T09:30');
    });
    I.saveScreenshot('branch-changes-mobile-schedule.png');
    I.clickCss('#branch-merge-schedule-confirm');
    I.waitForVisible('#branch-merge-scheduled', 10);
    I.dontSeeElement('#branch-merge-publish');
    I.clickCss('#branch-merge-unschedule');
    I.clickCss('#branch-merge-unschedule-confirm');
    I.waitForInvisible('#branch-merge-scheduled', 10);

    I.wjSetDefaultWindowSize();
    I.clickCss(`${modal} .DTE_Footer .btn-close-editor`);
    I.waitForInvisible(modal, 10);
    I.amOnPage('/admin/v9/webpages/branches/');
    I.waitForText(name, 20, '#branchesDataTable');
    I.clickCss('#branchesDataTable tbody td.dt-select-td');
    I.clickCss('.buttons-branch-publish');
    I.waitForVisible(`${changesTab}[aria-selected="true"]`, 10);
    await I.waitForVisible('#branch-merge-confirmation', 10);
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'revert', 'schedule', 'unschedule']);
    I.click('Zrušiť', '#branch-merge-confirmation');
    I.waitForInvisible('#branch-merge-confirmation', 10);
    I.waitForEnabled('#branch-merge-publish', 10);
    I.clickCss('#branch-merge-publish');
    await I.waitForVisible('#branch-merge-confirmation', 10);
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'revert', 'schedule', 'unschedule']);
    I.clickCss('#branch-merge-confirm');
    await I.waitForVisible('#branch-merge-progress', 10);
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'revert', 'schedule', 'unschedule', 'merge']);
    I.assertDeepEqual(requests[1].payload, {type: 'GROUP', id: 15, hash: 'autotest-folder'});
    I.assertEqual(requests[2].payload.publishAt, Date.parse('2036-06-04T07:30:00Z'));
    I.assertEqual(requests[4].payload.preparationHash, 'autotest-plan');
});
