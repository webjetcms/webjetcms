Feature('webpages.branch-switcher');

const branchPicker = require('../../helpers/branch-picker');
const trigger = '#branch-switcher';
const picker = '#branch-picker';
const listRoute = /\/admin\/rest\/branches\/?(?:\?.*)?$/;
const loadError = 'Pracovné verzie sa nepodarilo načítať. Skúste to znova alebo kontaktujte správcu.';

Before(({ login }) => login('admin'));

// Set this to an open package on the Slovak demo site, where the first root folder has a different landing page.
const previewBranchId = process.env.WEBJET_PREVIEW_HOME_BRANCH_ID;
if (previewBranchId) {
    Scenario('Header preview and Úvod open the same demo homepage', async ({ I }) => {
        const home = `/admin/webpages/branch-preview/${previewBranchId}/`;
        const checkHomepage = async () => {
            I.waitForVisible('#branch-preview-status', 20);
            I.assertEqual(new URL(await I.grabCurrentUrl()).pathname, home);
            I.see('ZAUJÍMAVÉ NOVÉ PRODUKTY');
            I.see('NAŠI BANKÁRI');
        };
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        branchPicker.select(I, previewBranchId);
        I.waitForVisible('#branch-preview-link', 20);
        I.click('#branch-preview-link');
        I.switchToNextTab();
        await checkHomepage();
        I.click(locate('a').withText('Úvod').withAttr({ href: home }).first());
        await checkHomepage();
        I.closeCurrentTab();
    });
}

Scenario('Empty working version list opens the picker', async ({ I }) => {
    await I.mockRoute(listRoute, route => route.fulfill({json: []}));
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.waitForEnabled(trigger, 20);
    I.clickCss(trigger);
    I.waitForVisible(picker, 10);
    I.see('Hlavná verzia', picker);
    I.seeElement(`${picker} [data-branch-action="manage"]`);
    I.pressKey('Escape');
    I.waitForInvisible(picker, 5);
    await I.stopMockingRoute(listRoute);
});

for (const status of [200, 500]) {
    Scenario(`Failed working version list shows a localized error and can be retried (HTTP ${status})`, async ({ I }) => {
        let failed = true;
        await I.mockRoute(listRoute, route => route.fulfill({
            status: failed ? status : 200,
            json: failed ? {error: 'autotest database error'} : []
        }));
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        I.waitForEnabled(trigger, 20);
        I.clickCss(trigger);
        await I.waitForText(loadError, 10, '#toast-container-webjet');
        I.dontSee('autotest database error', '#toast-container-webjet');
        I.dontSeeElement(picker);
        I.seeElement(`${trigger}:enabled`);
        failed = false;
        I.clickCss(trigger);
        I.waitForVisible(picker, 10);
        I.see('Hlavná verzia', picker);
        I.pressKey('Escape');
        await I.stopMockingRoute(listRoute);
    });
}
