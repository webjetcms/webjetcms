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

Scenario('Import/Export opens the current folder from More actions', async ({ I, DT }) => {
    for (const width of [1247, 1100, 767]) {
        I.resizeWindow(width, 958);
        I.dontSeeElement(`${wrapper} button.buttons-import-export`);
        I.clickCss(moreActions);
        I.waitForEnabled(`${moreMenu} button.buttons-import-export`);
        I.see('Import/Export', moreMenu);
        if (width === 1247) {
            I.clickCss(`${moreMenu} button.buttons-import-export`);
        } else {
            I.pressKey('ArrowDown');
            I.pressKey('End');
            I.pressKey('Enter');
        }
        I.switchToNextTab();
        I.seeInCurrentUrl('/components/import_web_pages/import_pages.jsp?groupId=67');
        I.waitForVisible('form.importExportTable');
        I.closeCurrentTab();
        I.dontSeeElement(moreMenu);
    }
    I.wjSetDefaultWindowSize();
    I.clickCss('#pills-waiting-tab');
    DT.waitForLoader();
    I.clickCss(moreActions);
    I.dontSeeElement(`${moreMenu} button.buttons-import-export`);
    I.pressKey('Escape');
    I.clickCss('#pills-folders-tab');
    DT.waitForLoader();
    I.clickCss(moreActions);
    I.seeElement(`${moreMenu} button.buttons-import-export`);
    I.pressKey('Escape');
});

Scenario('Switch between Settings and More actions without clicking outside', async ({ I }) => {
    for (let attempt = 0; attempt < 3; attempt++) {
        // Inspect in the same event loop, before the test runner's click delay can hide a stale overlay.
        const oldMenuVisible = await I.executeScript(({ settings, moreMenu }) => {
            document.querySelector(settings).click();
            const oldMenu = document.querySelector(moreMenu);
            return oldMenu !== null && oldMenu.getClientRects().length > 0;
        }, { settings, moreMenu });
        I.assertFalse(oldMenuVisible, 'The outgoing menu must not cover the new collection during its fade');
        I.waitForVisible(`${wrapper} button.buttons-colvis`);
        I.wait(1);
        I.seeElement(`${wrapper} button.buttons-colvis`);
        I.dontSeeElement(moreMenu);
        I.seeAttributesOnElements(settings, { 'aria-expanded': 'true' });
        I.clickCss(moreActions);
        // Wait beyond the old collection's fade-out to catch delayed removal of the new menu.
        I.wait(1);
        I.seeElement(moreMenu);
        I.dontSeeElement(`${wrapper} button.buttons-colvis`);
        I.seeAttributesOnElements(moreActions, { 'aria-expanded': 'true' });
        I.seeAttributesOnElements(settings, { 'aria-expanded': 'false' });
    }
    I.clickCss(moreActions);
    I.waitForInvisible(moreMenu);
    I.clickCss(moreActions);
    I.waitForVisible(moreMenu);
    I.pressKey('Escape');
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
            return { left: rect.left, right: rect.right, width: rect.width, viewport: window.innerWidth };
        }, dialog);
        I.assertTrue(bounds.left >= 0 && bounds.right <= bounds.viewport,
            `The entire settings dialog must be visible: ${JSON.stringify(bounds)}`);
        I.assertTrue(bounds.width > 300, 'Settings must retain their dialog layout');
        I.saveScreenshot(`more-actions-settings-${action}.png`);
        I.pressKey('Escape');
        I.waitForInvisible(dialog);
    });
}
