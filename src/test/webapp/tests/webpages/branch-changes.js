Feature('webpages.branch-changes');

const id = 900001;
const name = 'autotest editorial version';
const modal = '#branchesDataTable_modal';
const basicTab = '#pills-dt-branchesDataTable-basic-tab';
const changesTab = '#pills-dt-branchesDataTable-changes-tab';
const scheduleTab = '#pills-dt-branchesDataTable-publishing-tab';
const reportRoute = new RegExp(`/admin/rest/branches/${id}/(?:progress|resolve|schedule|unschedule|merge)$`);
const rowRoute = new RegExp(`/admin/rest/branches/${id}$`);
const listRoute = /\/admin\/rest\/branches\/all\?/;

Before(({ login }) => login('admin'));
After(({ I }) => {
    I.stopMockingRoute(reportRoute);
    I.stopMockingRoute(rowRoute);
    I.stopMockingRoute(listRoute);
});

Scenario('Working version editor filters conflicts and keeps scheduling separate from publishing', async ({ I }) => {
    const snapshot = title => ({state: 'ACTIVE', fields: {title}});
    const branch = {id, name, color: 'mint', state: 'DRAFT', revision: 1, description: 'autotest'};
    const report = {
        ...branch, ready: false, preparationHash: 'autotest-plan', errors: [], timeZone: 'Europe/Bratislava',
        items: [
            {type: 'DOCUMENT', objectKey: '12', label: 'autotest conflict', operation: 'UPSERT', conflict: true,
                base: snapshot('autotest original'), live: snapshot('autotest live'), work: snapshot('autotest draft'),
                liveHash: 'autotest-live', workHash: 'autotest-work', writeRequired: true, resolvable: true},
            {type: 'GROUP', objectKey: '15', label: 'autotest folder', operation: 'UPSERT', writeRequired: true}
        ]
    };
    const requests = [];
    await I.mockRoute(listRoute, route => route.fulfill({json: {content: [branch], totalElements: 1}}));
    await I.mockRoute(rowRoute, route => route.fulfill({json: branch}));
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
    I.waitForText('autotest conflict', 10, '#branchChangesDataTable');
    I.seeElement('#branch-merge-publish:disabled');
    I.selectOption('#branch-merge-type', 'GROUP');
    I.see('autotest folder', '#branchChangesDataTable');
    I.dontSee('autotest conflict', '#branchChangesDataTable');
    I.selectOption('#branch-merge-type', '');
    I.checkOption('#branch-merge-problems');
    I.dontSee('autotest folder', '#branchChangesDataTable');
    I.clickCss('[data-branch-compare="12"]');
    I.waitForVisible('dialog.pg-branch-merge', 10);
    I.checkOption('input[name="branch-merge-choice"][value="WORK"]');
    I.clickCss('#branch-merge-save-choice');
    I.waitForEnabled('#branch-merge-publish', 10);
    I.pressKey('Escape');
    I.waitForInvisible('dialog.pg-branch-merge', 10);
    I.seeElement(modal);

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
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'schedule', 'unschedule']);
    I.click('Zrušiť', '#branch-merge-confirmation');
    I.waitForInvisible('#branch-merge-confirmation', 10);
    I.waitForEnabled('#branch-merge-publish', 10);
    I.clickCss('#branch-merge-publish');
    await I.waitForVisible('#branch-merge-confirmation', 10);
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'schedule', 'unschedule']);
    I.clickCss('#branch-merge-confirm');
    await I.waitForVisible('#branch-merge-progress', 10);
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'schedule', 'unschedule', 'merge']);
    I.assertEqual(requests[1].payload.publishAt, Date.parse('2036-06-04T07:30:00Z'));
    I.assertEqual(requests[3].payload.preparationHash, 'autotest-plan');
});
