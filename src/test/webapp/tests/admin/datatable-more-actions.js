Feature('admin.datatable-more-actions');

const wrapper = '#datatableInit_wrapper';
const moreActions = `${wrapper} button.buttons-more-actions`;
const settings = `${wrapper} button.buttons-settings`;
const moreMenu = `${wrapper} .dt-more-actions-menu`;

Before(({ I, login, DT }) => {
    login('admin');
    I.resizeWindow(1247, 1231);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
});

Scenario('Import/Export opens the current folder and actions follow the selected tab', ({ I, DT }) => {
    I.dontSeeElement(`${wrapper} button.buttons-import-export`);
    I.clickCss(moreActions);
    I.waitForEnabled(`${moreMenu} button.buttons-import-export`);
    I.clickCss(`${moreMenu} button.buttons-import-export`);
    I.switchToNextTab();
    I.seeInCurrentUrl('/components/import_web_pages/import_pages.jsp?groupId=67');
    I.waitForVisible('form.importExportTable');
    I.closeCurrentTab();
    I.dontSeeElement(moreMenu);

    I.clickCss('#pills-waiting-tab');
    DT.waitForLoader();
    I.clickCss(moreActions);
    I.seeElement(`${moreMenu} button.buttons-stat`);
    for (const action of ['import-export', 'abtest', 'linkcheck', 'addPerexGroup', 'removePerexGroup', 'addUserGroup', 'removeUserGroup']) {
        I.dontSeeElement(`${moreMenu} button.buttons-${action}`);
    }
    I.pressKey('Escape');
    I.executeScript(() => webpagesDatatable.hideButton('stat'));
    I.dontSeeElement(moreActions);
    I.executeScript(() => webpagesDatatable.showButton('stat'));
    I.clickCss('#pills-folders-tab');
    DT.waitForLoader();
    I.clickCss(moreActions);
    I.seeElement(`${moreMenu} button.buttons-import-export`);
    I.pressKey('Escape');
});

Scenario('Keyboard navigation opens page statistics and allows leaving the menu', async ({ I }) => {
    I.clickCss('#datatableInit tbody tr:first-child td.dt-select-td');
    const docId = await I.executeScript(() => webpagesDatatable.rows({ selected: true }).data()[0].docId);
    I.clickCss(moreActions);
    I.pressKey('Escape');
    I.pressKey('Enter');
    const actions = await I.executeScript(selector => $(selector).find('button:not(:disabled):visible')
        .toArray().map(button => button.getAttribute('data-dtbtn')), moreMenu);
    I.assertAbove(actions.length, 1);
    I.pressKey('Home');
    I.assertEqual(await I.executeScript(() => document.activeElement.getAttribute('data-dtbtn')), actions[0]);
    I.pressKey(['Shift', 'Tab']);
    I.waitForInvisible(moreMenu);
    I.assertTrue(await I.executeScript(selector => document.activeElement.matches(selector), moreActions));
    I.pressKey('Enter');
    I.pressKey('End');
    I.assertEqual(await I.executeScript(() => document.activeElement.getAttribute('data-dtbtn')), actions[actions.length - 1]);
    I.pressKey('Tab');
    I.waitForInvisible(moreMenu);
    I.assertFalse(await I.executeScript(selector => document.activeElement.matches(selector), moreActions));

    I.clickCss(moreActions);
    I.pressKey('ArrowDown');
    I.pressKey('Home');
    const statisticsIndex = actions.indexOf('stat');
    I.assertAbove(statisticsIndex, -1);
    for (let index = 0; index < statisticsIndex; index++) I.pressKey('ArrowDown');
    I.assertEqual(await I.executeScript(() => document.activeElement.getAttribute('data-dtbtn')), 'stat');
    I.pressKey('Enter');
    I.switchToNextTab();
    I.seeInCurrentUrl('/apps/stat/admin/top-details/?docId=' + docId);
    I.waitForElement('div#topDetails-lineVariantA');
    I.closeCurrentTab();
});

Scenario('Menu stays accessible around the sidebar breakpoint and on narrow screens', async ({ I }) => {
    for (const width of [1202, 1190, 700]) {
        I.resizeWindow(width, 900);
        I.clickCss(moreActions);
        const bounds = await I.executeScript(selector => {
            const rect = document.querySelector(selector).getBoundingClientRect();
            return { left: rect.left, right: rect.right, viewport: window.innerWidth };
        }, moreMenu);
        I.assertTrue(bounds.left >= 0 && bounds.right <= bounds.viewport, 'The menu must fit the viewport');
        I.pressKey('Tab');
        I.pressKey('Escape');
        I.waitForInvisible(moreMenu);
        I.assertTrue(await I.executeScript(selector => document.activeElement.matches(selector), moreActions));
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Switch between Settings and More actions without clicking outside', async ({ I }) => {
    I.clickCss(moreActions);
    // Inspect immediately so the runner's click delay cannot conceal an outgoing overlay.
    const oldMenuVisible = await I.executeScript(({ settings, moreMenu }) => {
        document.querySelector(settings).click();
        const oldMenu = document.querySelector(moreMenu);
        return oldMenu !== null && oldMenu.getClientRects().length > 0;
    }, { settings, moreMenu });
    I.assertFalse(oldMenuVisible, 'The outgoing menu must not cover the new collection during its fade');
    I.waitForVisible(`${wrapper} button.buttons-colvis`);
    I.waitForFunction(() => $('.dt-button-collection:animated').length === 0);
    I.dontSeeElement(moreMenu);
    I.seeAttributesOnElements(settings, { 'aria-expanded': 'true' });
    I.clickCss(moreActions);
    // Observe the final state after queued collection animations have finished.
    I.waitForFunction(() => $('.dt-button-collection:animated').length === 0);
    I.seeElement(moreMenu);
    I.dontSeeElement(`${wrapper} button.buttons-colvis`);
    I.seeAttributesOnElements(moreActions, { 'aria-expanded': 'true' });
    I.seeAttributesOnElements(settings, { 'aria-expanded': 'false' });
    I.clickCss(moreActions);
    I.waitForInvisible(moreMenu);
});

for (const action of ['colvis', 'page-length']) {
    Scenario(`Settings ${action} dialog stays inside the viewport`, async ({ I }) => {
        I.clickCss(settings);
        I.clickCss(`${wrapper} button.buttons-${action}`);
        const dialog = `${wrapper} button.buttons-${action} ~ .dt-button-collection`;
        I.waitForVisible(dialog);
        const bounds = await I.executeScript(selector => {
            const rect = document.querySelector(selector).getBoundingClientRect();
            return { left: rect.left, right: rect.right, viewport: window.innerWidth };
        }, dialog);
        I.assertTrue(bounds.left >= 0 && bounds.right <= bounds.viewport,
            `The entire settings dialog must be visible: ${JSON.stringify(bounds)}`);
        I.pressKey('Escape');
        I.waitForInvisible(dialog);
    });
}
