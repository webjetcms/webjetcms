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
const pageRoute = new RegExp(`/admin/rest/branches/${id}/page-content\\?`);

Before(({ login }) => login('admin'));
After(({ I }) => {
    I.stopMockingRoute(reportRoute);
    I.stopMockingRoute(changesRoute);
    I.stopMockingRoute(rowRoute);
    I.stopMockingRoute(listRoute);
    I.stopMockingRoute(pageRoute);
});

Scenario('Working version editor filters conflicts and keeps scheduling separate from publishing', async ({ I, DT }) => {
    const snapshot = (title, fields = {}) => ({state: 'ACTIVE', fields: {title, navbar: 'autotest unchanged navigation', data: `<p>${title} HTML</p>`, ...fields}});
    const sharedFields = {author_id: 18, field_o: false, login_doc_id: 0, html_head: '<p>autotest shared HTML</p>'};
    const absent = {state: 'ABSENT', fields: {}};
    const sectionPath = '/Autotest & redakcia';
    const branch = {id, name, color: 'mint', state: 'DRAFT', revision: 1, description: 'autotest'};
    const report = {
        ...branch, ready: false, preparationHash: 'autotest-plan', errors: [], timeZone: 'Europe/Bratislava',
        items: [
            {type: 'DOCUMENT', objectKey: '12', label: 'autotest conflict', operation: 'UPSERT', conflict: true,
                base: snapshot('autotest original', {author_id: 1, login_doc_id: -1, base_only: 'autotest baseline context', html_head: '<p>autotest baseline HTML</p>'}),
                live: snapshot('autotest live', {...sharedFields, external_link: '', empty_list: [], blank_text: ' \t', available: false, sort_priority: 0, live_only: 'autotest main-only value'}),
                work: snapshot('autotest draft', {...sharedFields, external_link: null, blank_text: null, available: null, sort_priority: null, work_only: 'autotest work-only value'}),
                liveHash: 'autotest-live', workHash: 'autotest-work', writeRequired: true, resolvable: true, editable: true},
            {type: 'DOCUMENT', objectKey: '18', label: 'autotest new page', operation: 'UPSERT',
                base: absent, live: absent, work: snapshot('autotest new page'), workOnly: true, writeRequired: true, resolvable: true,
                folderPath: sectionPath + '/autotest folder'},
            {type: 'DOCUMENT', objectKey: '21', label: 'autotest unchanged main with a long title that keeps comparison actions visible on narrow screens', operation: 'UPSERT', conflict: false,
                base: snapshot('autotest original'), live: snapshot('autotest original'), work: snapshot('autotest draft'),
                liveHash: 'autotest-original', workHash: 'autotest-draft',
                writeRequired: true, resolvable: true, editable: true},
            {type: 'DOCUMENT', objectKey: '22', label: 'autotest matching deletion', operation: 'DELETE', conflict: false,
                base: snapshot('autotest removed'), live: {...snapshot('autotest removed'), state: 'TRASH'},
                work: {...snapshot('autotest removed'), state: 'TRASH'}, writeRequired: false},
            {type: 'GROUP', objectKey: '15', label: 'autotest folder', operation: 'UPSERT', workHash: 'autotest-folder', writeRequired: true,
                base: absent, live: absent, work: {state: 'ACTIVE', fields: {group_name: 'autotest folder', html_head: '<meta name="autotest">'}},
                folderPath: sectionPath}
        ]
    };
    const requests = [];
    const pageRequests = [];
    let rejectFirstPage = true;
    await I.mockRoute(pageRoute, route => {
        const query = new URL(route.request().url()).searchParams;
        pageRequests.push(Object.fromEntries(query));
        if (rejectFirstPage) {
            rejectFirstPage = false;
            return route.fulfill({status: 503, json: {message: 'autotest temporary render failure'}});
        }
        const version = query.get('version');
        const diff = query.get('highlight') === 'true';
        return route.fulfill({contentType: 'text/html', body: `<!doctype html><html><head><style>html {scroll-behavior: smooth} body {margin:0} main {height:${version === 'LIVE' ? 4200 : 3000}px}</style></head><body><main><h1>autotest ${version} rendered page</h1>${diff ? '<ins>autotest highlighted difference</ins>' : ''}<a href="/autotest-away">autotest link</a><script>parent.autotestPreviewScriptExecuted=true</script></main></body></html>`});
    });
    let rejectFirstResolution = true;
    const resolutionError = 'autotest resolution failed';
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
        const options = {type: [{label: 'Stránky', value: 'DOCUMENT'}, {label: 'Priečinky', value: 'GROUP'}, {label: 'Médiá', value: 'MEDIA'}, {label: 'Súbory', value: 'FILE'}]};
        return route.fulfill({json: {content, options, totalElements: content.length, report}});
    });
    await I.mockRoute(reportRoute, route => {
        const request = route.request();
        const action = new URL(request.url()).pathname.split('/').pop();
        if (request.method() === 'POST') {
            const payload = request.postDataJSON();
            requests.push({action, payload});
            if (action === 'resolve' && rejectFirstResolution) {
                rejectFirstResolution = false;
                return route.fulfill({status: 409, json: {message: resolutionError}});
            }
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
    I.resizeWindow(390, 844);
    // Column sizing after a resize is asynchronous; visibility alone does not check clipping.
    I.waitForFunction(() => {
        const body = document.querySelector('#datatableFieldDTE_Field_changes_wrapper .dt-scroll-body');
        const bounds = body.getBoundingClientRect();
        return body.scrollLeft === 0 && [...body.querySelectorAll('[data-branch-compare]')].every(action => {
            const rect = action.getBoundingClientRect();
            return rect.left >= bounds.left && rect.right <= bounds.right;
        });
    }, 5);
    I.wjSetDefaultWindowSize();
    const conflictWarning = 'Pred publikovaním vyriešte konflikty na karte Zmeny.';
    I.waitForText(conflictWarning, 5, '#toast-container-webjet .toast-warning');
    I.dontSee(conflictWarning, modal);
    I.see('Zobraziť', `${changesTable} [data-branch-compare="18"]`);
    I.clickCss(`${changesTable} [data-branch-compare="18"]`);
    const detail = 'dialog.pg-branch-merge[open]';
    I.waitForVisible(detail, 10);
    I.see('autotest new page', `${detail} [data-branch-version="WORK"]`);
    I.see('Zobraziť stránku', '#branch-merge-fields [data-branch-version="WORK"] .pg-branch-merge__actions');
    I.dontSeeElement(`${detail} [data-branch-version="BASE"]`);
    I.dontSeeElement(`${detail} [data-branch-version="LIVE"]`);
    I.clickCss('#branch-merge-content-tab');
    I.waitForText('Obsah stránky sa nepodarilo načítať.', 5, '.pg-branch-page-content__status');
    I.click('Obnoviť zmeny', '.pg-branch-page-content__status');
    I.waitForVisible('#branch-merge-content iframe', 5);
    I.dontSeeElement('#branch-merge-content [data-branch-page-version="LIVE"]');
    I.dontSeeElement('#branch-merge-highlight');
    I.clickCss('#branch-merge-fields-tab');
    I.see('Vynechať položku z publikovania', detail);
    I.pressKey('Escape');
    I.waitForInvisible(detail, 10);
    I.clickCss(`${changesTable} [data-branch-compare="21"]`);
    I.waitForVisible(detail, 10);
    I.dontSeeElement(`${detail} [data-branch-version="BASE"]`);
    I.see('autotest original', '#branch-merge-fields [data-branch-version="LIVE"]');
    I.see('autotest draft', '#branch-merge-fields [data-branch-version="WORK"]');
    const twoColumnsFillWidth = await I.executeScript(() => {
        const grid = document.querySelector('#branch-merge-fields .pg-branch-merge__versions').getBoundingClientRect();
        const panels = [...document.querySelectorAll('#branch-merge-fields [data-branch-version]')].map(panel => panel.getBoundingClientRect());
        return panels.length === 2 && Math.abs(panels[0].width - panels[1].width) <= 1
            && Math.abs(panels[0].left - grid.left) <= 1 && Math.abs(panels[1].right - grid.right) <= 1;
    });
    I.assertTrue(twoColumnsFillWidth);
    I.assertEqual(pageRequests.length, 2);
    I.checkOption('#branch-merge-choice-live');
    I.clickCss('#branch-merge-content-tab');
    I.waitForVisible('#branch-merge-content.active', 5);
    // Frame readiness and scroll coordinates have no standard CodeceptJS text/visibility assertion.
    I.waitForFunction(() => [...document.querySelectorAll('#branch-merge-content iframe')].every(frame =>
        frame.contentDocument?.querySelector('h1')?.textContent.includes('rendered page')), 5);
    I.dontSeeElement('#branch-merge-all-fields');
    const rendered = await I.executeScript(() => [...document.querySelectorAll('#branch-merge-content iframe')].map(frame => ({
        title: frame.title, text: frame.contentDocument.querySelector('h1').textContent, sandbox: frame.getAttribute('sandbox')
    })));
    I.assertEqual(rendered.length, 2);
    I.assertEqual(rendered[0].text, 'autotest LIVE rendered page');
    I.assertEqual(rendered[1].text, 'autotest WORK rendered page');
    I.assertTrue(rendered.every(frame => frame.title && frame.sandbox === 'allow-same-origin'));
    I.assertFalse(Boolean(await I.executeScript(() => window.autotestPreviewScriptExecuted)));
    I.executeScript(() => document.querySelector('#branch-merge-content iframe').contentWindow.scrollTo({top: 900, behavior: 'instant'}));
    I.waitForFunction(() => [...document.querySelectorAll('#branch-merge-content iframe')].every(frame => frame.contentWindow.scrollY === 900), 5);
    I.executeScript(() => document.querySelectorAll('#branch-merge-content iframe')[1].contentWindow.scrollTo({top: 500, behavior: 'instant'}));
    I.waitForFunction(() => [...document.querySelectorAll('#branch-merge-content iframe')].every(frame => frame.contentWindow.scrollY === 500), 5);
    I.checkOption('#branch-merge-highlight');
    await I.waitForFunction(() => document.querySelectorAll('#branch-merge-content iframe')[1].contentDocument?.querySelector('ins'), 5);
    I.seeCheckboxIsChecked('#branch-merge-choice-live');
    I.assertEqual(pageRequests.filter(query => query.version === 'LIVE').length, 1);
    I.assertEqual(pageRequests.filter(query => query.version === 'WORK' && query.highlight === 'true').length, 1);
    I.executeScript(() => document.querySelector('#branch-merge-content iframe').contentWindow.scrollTo({top: 10000, behavior: 'instant'}));
    I.waitForFunction(() => {
        const [live, work] = [...document.querySelectorAll('#branch-merge-content iframe')];
        return live.contentWindow.scrollY === live.contentDocument.scrollingElement.scrollHeight - live.contentDocument.scrollingElement.clientHeight
            && work.contentWindow.scrollY === work.contentDocument.scrollingElement.scrollHeight - work.contentDocument.scrollingElement.clientHeight;
    }, 5);
    I.resizeWindow(390, 844);
    const previewFits = await I.executeScript(() => {
        const panes = [...document.querySelectorAll('.pg-branch-page-content__version')].map(panel => panel.getBoundingClientRect());
        return panes.every(panel => panel.left >= 0 && panel.right <= innerWidth) && panes[1].top >= panes[0].bottom;
    });
    I.assertTrue(previewFits);
    I.wjSetDefaultWindowSize();
    I.clickCss('#branch-merge-fields-tab');
    I.checkOption('#branch-merge-choice-work');
    I.clickCss('#branch-merge-html-tab');
    I.waitForVisible('#branch-merge-html.active', 5);
    I.dontSeeElement('#branch-merge-html [data-branch-version="BASE"]');
    I.see('autotest original HTML', '#branch-merge-html [data-branch-version="LIVE"]');
    I.see('autotest original HTML', '#branch-merge-html [data-branch-version="LIVE"] .pg-branch-merge__changed');
    I.see('autotest draft HTML', '#branch-merge-html [data-branch-version="WORK"] .pg-branch-merge__changed');
    I.checkOption('#branch-merge-all-fields');
    I.seeCheckboxIsChecked('#branch-merge-choice-work');
    I.dontSeeElement('#branch-merge-html [data-branch-version="BASE"]');
    I.clickCss('#branch-merge-save-choice');
    I.waitForText(resolutionError, 5, `${detail} [role="alert"]`);
    I.seeElement(detail);
    I.seeCheckboxIsChecked('#branch-merge-choice-work');
    I.seeElement('#branch-merge-save-choice:disabled');
    I.clickCss(`${detail} [data-branch-retry]`);
    I.waitForEnabled('#branch-merge-save-choice', 10);
    I.dontSee(resolutionError, detail);
    I.seeCheckboxIsChecked('#branch-merge-choice-work');
    I.click('Zatvoriť', `${detail} footer`);
    I.waitForInvisible(detail, 10);
    I.clickCss(`${changesTable} [data-branch-compare="22"]`);
    I.waitForVisible(detail, 10);
    // A state-only difference still needs the baseline, even when LIVE and WORK agree.
    I.see('Východiskový stav', '#branch-merge-fields [data-branch-version="BASE"]');
    I.see('Položka existuje', '#branch-merge-fields [data-branch-version="BASE"]');
    I.see('Hlavná verzia', '#branch-merge-fields [data-branch-version="LIVE"]');
    I.see('Pracovná verzia', '#branch-merge-fields [data-branch-version="WORK"]');
    I.dontSee('autotest removed', '#branch-merge-fields .pg-branch-merge__version-body');
    I.dontSeeElement('#branch-merge-fields .pg-branch-merge__changed');
    I.clickCss('#branch-merge-html-tab');
    I.waitForVisible('#branch-merge-html.active', 5);
    I.see('Východiskový stav', '#branch-merge-html [data-branch-version="BASE"]');
    I.dontSee('autotest removed HTML', '#branch-merge-html .pg-branch-merge__version-body');
    I.dontSeeElement('#branch-merge-html .pg-branch-merge__changed');
    I.click('Zatvoriť', `${detail} footer`);
    I.waitForInvisible(detail, 10);
    DT.filterSelect('type', 'Priečinky', changesWrapper);
    I.see('autotest folder', changesTable);
    I.clickCss(`${changesTable} [data-branch-compare="15"]`);
    I.waitForVisible(detail, 5);
    I.dontSeeElement('#branch-merge-html-tab');
    I.dontSeeElement('#branch-merge-content-tab');
    I.see('<meta name="autotest">', '#branch-merge-fields');
    I.click('Zatvoriť', `${detail} footer`);
    I.waitForInvisible(detail, 5);
    I.dontSee('autotest conflict', changesTable);
    DT.clearFilter('type');
    I.clickCss('label[for="branch-merge-problems"]');
    await I.dontSee('autotest folder', changesTable);
    I.assertEqual(loads.length, loaded + 2);
    I.clickCss('[data-branch-compare="12"]');
    I.waitForVisible('dialog.pg-branch-merge', 10);
    const editHint = 'Po uložení v editore sa sem vráťte a znova skontrolujte zmeny. Uloženie zruší starú voľbu výsledku.';
    const saveHint = 'Zvolená verzia sa použije pre celú položku.';
    const baseHint = 'Stav hlavnej verzie zachytený pri prvom uložení úpravy tejto položky v pracovnej verzii. Pri ďalších úpravách sa nemení.';
    I.see('Východiskový stav', '#branch-merge-fields [data-branch-version="BASE"]');
    I.focus('#branch-merge-fields-base-title');
    I.waitForText(baseHint, 5, `${detail} .tooltip.show`);
    I.pressKey('Escape');
    I.waitForInvisible(`${detail} .tooltip.show`, 5);
    I.seeElement(detail);
    I.dontSee(editHint, detail);
    I.dontSee(saveHint, detail);
    const fieldsWork = '#branch-merge-fields [data-branch-version="WORK"]';
    const fieldsLive = '#branch-merge-fields [data-branch-version="LIVE"]';
    const sharedLabels = ['author id', 'field o', 'login doc id', 'external link', 'empty list', 'blank text', 'base only'];
    for (const panel of [fieldsLive, fieldsWork]) {
        for (const label of sharedLabels) I.dontSee(label, `${panel} dl`);
        for (const label of ['available', 'sort priority', 'live only', 'work only']) I.see(`${label} · zmenené`, `${panel} dl`);
    }
    I.dontSee('autotest baseline context', '#branch-merge-fields');
    I.see('Upraviť', `${fieldsWork} .pg-branch-merge__actions`);
    I.see('Zobraziť stránku', `${fieldsWork} .pg-branch-merge__actions`);
    I.see('Zobraziť stránku', `${fieldsLive} .pg-branch-merge__actions`);
    I.seeElement(`${fieldsLive} a[href="/showdoc.do?docid=12"]`);
    I.dontSeeElement(`${fieldsLive} [data-branch-edit]`);
    I.dontSeeElement('#branch-merge-fields [data-branch-version="BASE"] .pg-branch-merge__actions');
    const aligned = await I.executeScript(() => {
        const tops = [...document.querySelectorAll('#branch-merge-fields .pg-branch-merge__version-body')].map(body => body.getBoundingClientRect().top);
        return Math.max(...tops) - Math.min(...tops) <= 1;
    });
    I.assertTrue(aligned);
    I.focus(`${fieldsWork} [data-branch-edit]`);
    I.waitForText(editHint, 5, `${detail} .tooltip.show`);
    I.pressKey('Escape');
    I.waitForInvisible(`${detail} .tooltip.show`, 5);
    I.seeElement(detail);
    I.moveCursorTo('#branch-merge-save-hint');
    I.waitForText(saveHint, 5, `${detail} .tooltip.show`);
    I.seeElement('#branch-merge-save-choice:disabled');
    I.pressKey('Escape');
    I.waitForInvisible(`${detail} .tooltip.show`, 5);
    I.seeElement(detail);
    I.seeElement('#branch-merge-fields.active');
    I.dontSeeElement('#branch-merge-html');
    I.dontSee('autotest unchanged navigation', detail);
    I.see('Použiť hlavnú verziu', `${detail} footer`);
    I.see('Použiť pracovnú verziu', `${detail} footer`);
    I.seeElement('#branch-merge-save-choice:disabled');
    I.checkOption(`${detail} footer input[name="branch-merge-choice"][value="WORK"]`);
    I.clickCss('#branch-merge-html-tab');
    I.waitForVisible('#branch-merge-html.active', 5);
    I.see('Východiskový stav', '#branch-merge-html [data-branch-version="BASE"]');
    I.focus('#branch-merge-html-base-title');
    I.waitForText(baseHint, 5, `${detail} .tooltip.show`);
    I.pressKey('Escape');
    I.waitForInvisible(`${detail} .tooltip.show`, 5);
    I.seeElement(detail);
    I.see('Upraviť', '#branch-merge-html [data-branch-version="WORK"] .pg-branch-merge__actions');
    I.see('Zobraziť stránku', '#branch-merge-html [data-branch-version="LIVE"] .pg-branch-merge__actions');
    I.see('autotest draft HTML', '#branch-merge-html [data-branch-version="WORK"]');
    I.see('autotest live HTML', '#branch-merge-html [data-branch-version="LIVE"] .pg-branch-merge__changed');
    I.dontSee('html head', '#branch-merge-html');
    I.seeCheckboxIsChecked('#branch-merge-choice-work');
    I.checkOption('#branch-merge-all-fields');
    I.seeElement('#branch-merge-html.active');
    for (const version of ['LIVE', 'WORK']) {
        const panel = `#branch-merge-html [data-branch-version="${version}"]`;
        I.see('autotest shared HTML', panel);
        I.dontSee('html head · zmenené', panel);
    }
    I.seeCheckboxIsChecked('#branch-merge-choice-work');
    I.dontSeeElement(`${detail} .tooltip`);
    I.clickCss('#branch-merge-fields-tab');
    I.see('autotest unchanged navigation', '#branch-merge-fields');
    I.see('autotest baseline context', '#branch-merge-fields [data-branch-version="BASE"]');
    for (const panel of [fieldsLive, fieldsWork]) {
        for (const label of sharedLabels) {
            I.see(label, `${panel} dl`);
            I.dontSee(`${label} · zmenené`, `${panel} dl`);
        }
    }
    const normalizedValues = await I.executeScript(() => Object.fromEntries(['LIVE', 'WORK'].map(version => {
        const panel = document.querySelector(`#branch-merge-fields [data-branch-version="${version}"]`);
        const labels = ['author id', 'field o', 'login doc id', 'external link', 'empty list', 'blank text'];
        return [version, Object.fromEntries([...panel.querySelectorAll('dt')].filter(dt => labels.includes(dt.textContent)).map(dt => [dt.textContent, dt.nextElementSibling.textContent]))];
    })));
    const expectedSharedValues = {'author id': '18', 'field o': 'Nie', 'login doc id': '0', 'external link': '—', 'empty list': '—', 'blank text': '—'};
    I.assertDeepEqual(normalizedValues, {LIVE: expectedSharedValues, WORK: expectedSharedValues});
    I.clickCss(`${detail} header button[aria-label="Maximalizovať okno"]`);
    I.seeElement('dialog.pg-branch-merge--maximized');
    I.clickCss(`${detail} header button[aria-label="Zmenšiť okno"]`);
    I.dontSeeElement('dialog.pg-branch-merge--maximized');
    I.resizeWindow(390, 844);
    const footerFits = await I.executeScript(() => {
        const dialog = document.querySelector('dialog.pg-branch-merge');
        const footer = dialog.querySelector('footer').getBoundingClientRect();
        const save = dialog.querySelector('#branch-merge-save-choice').getBoundingClientRect();
        const versions = [...dialog.querySelectorAll('#branch-merge-fields [data-branch-version]')];
        const actionsFit = versions.every(version => {
            const bounds = version.getBoundingClientRect();
            return [...version.querySelectorAll('.pg-branch-merge__actions a, .pg-branch-merge__actions button')].every(action => {
                const rect = action.getBoundingClientRect();
                return rect.left >= bounds.left && rect.right <= bounds.right;
            });
        });
        const stacked = versions.every((version, index) => index === 0 || versions[index - 1].getBoundingClientRect().bottom <= version.getBoundingClientRect().top);
        return footer.top > 0 && footer.bottom <= innerHeight && save.left >= 0 && save.right <= innerWidth
            && dialog.scrollWidth <= dialog.clientWidth && actionsFit && stacked;
    });
    I.assertTrue(footerFits);
    I.seeCheckboxIsChecked('#branch-merge-choice-work');
    I.wjSetDefaultWindowSize();
    I.clickCss('#branch-merge-save-choice');
    I.waitForEnabled('#branch-merge-publish', 10);
    I.saveScreenshot('branch-save-result.png');
    I.waitForInvisible(detail, 5);
    I.seeElement(modal);
    I.dontSee('autotest conflict', changesTable);
    I.seeCheckboxIsChecked('#branch-merge-problems');
    I.clickCss('label[for="branch-merge-problems"]');
    I.waitForText('autotest conflict', 10, changesTable);
    I.see('Pripravené na publikovanie', `${changesTable} tr[data-branch-key="12"]`);
    I.see('Porovnať', `${changesTable} [data-branch-compare="12"]`);
    I.clickCss(`${changesTable} [data-branch-compare="12"]`);
    I.waitForVisible(detail, 10);
    I.seeCheckboxIsChecked('#branch-merge-choice-work');
    I.see('Východiskový stav', '#branch-merge-fields [data-branch-version="BASE"]');
    I.clickCss('#branch-merge-html-tab');
    I.waitForVisible('#branch-merge-html.active', 5);
    I.see('Východiskový stav', '#branch-merge-html [data-branch-version="BASE"]');
    I.click('Zatvoriť', `${detail} footer`);
    I.waitForInvisible('dialog.pg-branch-merge', 10);
    I.seeElement(modal);
    I.clickCss(`${changesWrapper} [data-dtbtn="reload"]`);
    I.waitForInvisible('#panel-body-dt-branchesDataTable-changes[aria-busy=true]', 10);
    I.waitForText('autotest folder', 10, changesTable);
    I.clickCss(`${changesTable} tr[data-branch-key="15"] td.dt-select-td`);
    I.waitForEnabled(`${changesWrapper} .buttons-branch-revert`, 10);
    I.clickCss(`${changesWrapper} .buttons-branch-revert`);
    await I.waitForText('autotest folder', 10, '#branch-merge-confirmation');
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'resolve']);
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
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'resolve', 'revert', 'schedule', 'unschedule']);
    I.click('Zrušiť', '#branch-merge-confirmation');
    I.waitForInvisible('#branch-merge-confirmation', 10);
    I.waitForEnabled('#branch-merge-publish', 10);
    I.clickCss('#branch-merge-publish');
    await I.waitForVisible('#branch-merge-confirmation', 10);
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'resolve', 'revert', 'schedule', 'unschedule']);
    I.clickCss('#branch-merge-confirm');
    await I.waitForVisible('#branch-merge-progress', 10);
    I.assertDeepEqual(requests.map(({action}) => action), ['resolve', 'resolve', 'revert', 'schedule', 'unschedule', 'merge']);
    I.assertDeepEqual(requests[2].payload, {type: 'GROUP', id: 15, hash: 'autotest-folder'});
    I.assertEqual(requests[3].payload.publishAt, Date.parse('2036-06-04T07:30:00Z'));
    I.assertEqual(requests[5].payload.preparationHash, 'autotest-plan');
});
