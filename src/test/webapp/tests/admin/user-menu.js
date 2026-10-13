Feature('admin.user-menu').tag('@singlethread');

const trigger = '#dropdownMenuUser';
const panel = '#admin-user-menu';
const badge = '.md-user-menu__two-factor-badge';
const statusRoute = '**/admin/rest/user-menu';
const menuDataRoutes = /\/admin\/(users\/2factorauth|rest\/(user-menu|sessions|security\/login-events))(?:[/?]|$)/;

Before(({ login }) => login('admin'));

function openMenu(I) {
    I.waitForElement(`${trigger}[data-user-initialized="true"]`, 15);
    I.clickCss(trigger);
    I.waitForVisible(`${panel}.show`, 5);
}

function closeMenu(I) {
    I.pressKey('Escape');
    I.waitForInvisible(panel, 5);
}

Scenario('User menu loads account data only when opened', async ({ I, Document }) => {
    const requests = [];
    await I.mockRoute(menuDataRoutes, route => {
        requests.push(new URL(route.request().url()).pathname);
        return route.continue();
    });
    I.amOnPage('/admin/v9/search/');
    I.resizeWindow(1280, 850);
    I.waitForElement(`${trigger}[data-user-initialized="true"]`, 15);
    I.assertDeepEqual(requests, [], 'Rendering the page must not fetch menu data.');
    openMenu(I);
    I.waitForElement(`${badge}[data-enabled]`, 10);
    const user = await I.executeScript(() => ({
        fullName: window.currentUser.fullName, login: window.currentUser.login,
        email: window.currentUser.email, userId: window.currentUser.userId
    }));
    const identity = await I.grabTextFrom('.md-user-menu__identity');
    I.assertContain(identity, user.fullName, 'The menu must identify the signed-in user.');
    I.assertContain(identity, user.login);
    I.assertContain(identity, user.email);
    I.assertTrue((await I.grabTextFrom('.md-user-menu__avatar')).trim().length > 0, 'The avatar must display initials.');
    I.assertEqual(await I.grabAttributeFrom('.md-user-menu__profile', 'data-userid'), String(user.userId));
    I.assertEqual(await I.grabAttributeFrom('.md-user-menu__sessions', 'href'), '/admin/v9/?activeSessions=true');
    I.assertTrue(await I.executeScript(() => /^\d+$/.test(document.querySelector('.md-user-menu__session-count').textContent.trim())),
        'The active-sessions badge must show a session count.');
    I.seeElement('.md-user-menu__logout');
    I.seeElement('.ly-header .js-logout-toggler');
    const menuText = await I.grabTextFrom(panel);
    I.assertFalse(/Bol som to ja|Nebol som to ja/.test(menuText), 'Account verification actions must remain in the dashboard.');
    I.assertTrue(['true', 'false'].includes(await I.grabAttributeFrom(badge, 'data-enabled')), 'The 2FA badge must expose its rendered boolean state.');
    I.moveCursorTo('.md-user-menu__avatar');
    Document.screenshotElement(panel, '/redactor/admin/user-menu.png');

    closeMenu(I);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'dropdownMenuUser');
    I.pressKey('ArrowDown');
    I.waitForVisible(`${panel}.show`, 5);
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-user-menu__profile')), 'ArrowDown must focus the profile action.');
    I.pressKey('ArrowDown');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-user-menu__sessions')), 'ArrowDown must reach the sessions link without navigating.');
    closeMenu(I);

    for (const width of [1280, 375]) {
        I.resizeWindow(width, 850);
        if (width < 1200) {
            I.clickCss('.js-sidebar-toggler');
            I.waitForVisible('.ly-page-wrapper.active', 5);
        }
        openMenu(I);
        I.assertTrue(await I.executeScript(() => {
            const menu = document.querySelector('#admin-user-menu');
            const bounds = menu.getBoundingClientRect();
            return bounds.left >= 0 && bounds.right <= innerWidth + 1 && bounds.bottom <= innerHeight + 1 && menu.scrollWidth <= menu.clientWidth + 1;
        }), `The menu must fit the ${width}px viewport without horizontal overflow.`);
        I.saveScreenshot(`user-menu-${width}.png`);
        closeMenu(I);
        if (width < 1200) {
            I.clickCss('.js-sidebar-toggler');
            I.waitForInvisible('.ly-page-wrapper.active', 5);
        }
    }
    I.assertTrue(requests.length > 0 && requests.every(url => url === '/admin/rest/user-menu'), 'Menu openings must fetch only the account summary.');
    I.wjSetDefaultWindowSize();
    await I.stopMockingRoute(menuDataRoutes);
});

Scenario('Menu opens before its response and refreshes on every opening', async ({ I }) => {
    let release;
    const pending = new Promise(resolve => { release = resolve; });
    let reads = 0;
    let enabled = true;
    await I.mockRoute(statusRoute, async route => {
        reads++;
        await pending;
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
            sessionCount: 4, deviceCount: 25, twoFactorEnabled: enabled, newLoginCount: 2
        }) });
    });
    I.amOnPage('/admin/v9/search/');
    openMenu(I);
    I.seeElement(`${panel}[aria-busy="true"]`);
    I.assertEqual(await I.executeScript(() => document.querySelector('.md-user-menu__two-factor-badge').textContent.trim()), '', 'Pending status must not suggest disabled 2FA.');
    I.assertEqual(await I.executeScript(() => document.querySelector('.md-user-menu__session-count').textContent.trim()), '');
    release();
    await I.waitForElement(`${badge}[data-enabled="true"]`, 10);
    I.see('4', '.md-user-menu__session-count');
    I.see('25', '.md-user-menu__session-hint');
    I.see('2', '.md-user-menu__new-logins');
    I.assertEqual(reads, 1);
    closeMenu(I);
    enabled = false;
    openMenu(I);
    await I.waitForElement(`${badge}[data-enabled="false"]`, 10);
    I.assertEqual(reads, 2, 'Every opening must refresh status without tracking a popup.');
    closeMenu(I);
    await I.stopMockingRoute(statusRoute);
});

Scenario('Unavailable status leaves the menu usable and retries on reopening', async ({ I }) => {
    await I.mockRoute(statusRoute, route => route.fulfill({ status: 503 }));
    I.amOnPage('/admin/v9/search/');
    openMenu(I);
    I.waitForText('—', 5, '.md-user-menu__session-count');
    I.seeElement('.md-user-menu__profile');
    I.seeElement('.md-user-menu__logout');
    I.dontSeeElement(`${badge}.is-disabled`);
    closeMenu(I);
    await I.stopMockingRoute(statusRoute);
    openMenu(I);
    I.waitForElement(`${badge}[data-enabled]`, 10);
    closeMenu(I);
});

Scenario('Menu counts match both personal dashboard tabs', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    openMenu(I);
    I.waitForElement(`${badge}[data-enabled]`, 10);
    const sessions = (await I.grabTextFrom('.md-user-menu__session-count')).trim();
    const devices = (await I.grabTextFrom('.md-user-menu__session-hint')).match(/\d+/)?.[0];
    I.clickCss('.md-user-menu__sessions');
    const modal = '.md-dashboard-modal--sessions';
    I.waitForVisible(modal, 20);
    I.see(`(${sessions})`, `${modal} [role="tab"][id$="-mine"]`);
    I.clickCss(`${modal} [role="tab"][id$="-devices"]`);
    I.waitForText(`(${devices})`, 10, `${modal} [role="tab"][id$="-devices"]`);
    I.clickCss(`${modal} .modal-footer > button`);
    I.waitForInvisible(modal, 10);
});

Scenario('Sessions link opens the personal dashboard tab and consumes the URL parameter', async ({ I }) => {
    let deviceReads = 0;
    const devicesRoute = '**/admin/rest/security/login-events?page=*';
    await I.mockRoute(devicesRoute, route => {
        deviceReads++;
        return route.continue();
    });
    I.amOnPage('/admin/v9/search/');
    openMenu(I);
    I.clickCss('.md-user-menu__sessions');
    const modal = '.md-dashboard-modal--sessions';
    I.waitForVisible(modal, 20);
    I.seeElement(`${modal} [role="tab"][id$="-mine"][aria-selected="true"]`);
    I.seeElement(`${modal} .md-dashboard-sessions__mine`);
    I.assertFalse(await I.executeScript(() => new URL(location.href).searchParams.has('activeSessions')), 'The sessions parameter must be consumed after opening the dialog.');
    I.assertEqual(deviceReads, 0, 'Opening the personal sessions tab must not fetch registered devices.');
    I.clickCss(`${modal} .modal-footer > button`);
    I.waitForInvisible(modal, 10);
    I.waitForFunction(() => document.activeElement.id === 'dropdownMenuUser', 5);
    I.refreshPage();
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.dontSeeElement(modal);
    await I.stopMockingRoute(devicesRoute);
});

Scenario('Profile and encryption-key actions keep their permission rules', async ({ I }) => {
    I.amOnPage('/admin/v9/search/?removePerm=users.edit_admins,cmp_form');
    openMenu(I);
    I.assertEqual(await I.grabAttributeFrom('.md-user-menu__profile', 'data-onlyself'), 'true', 'Users without administrator-edit permission must only edit their own profile.');
    I.dontSeeElement(`${panel} button[onclick*="keymanagement"]`);
    I.seeElement('.md-user-menu__sessions');
    I.seeElement('.md-user-menu__two-factor');
    I.seeElement('.md-user-menu__logout');
    closeMenu(I);
    I.logout();
});
