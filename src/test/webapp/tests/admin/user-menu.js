Feature('admin.user-menu').tag('@singlethread');

const trigger = '#dropdownMenuUser';
const panel = '#admin-user-menu';
const badge = '.md-user-menu__two-factor-badge';
const statusRoute = '**/admin/users/2factorauth';
const menuDataRoutes = /\/admin\/(users\/2factorauth|rest\/(sessions|security\/login-events))(?:[/?]|$)/;

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

Scenario('User menu renders identity and sessions without requesting account data', async ({ I }) => {
    const requests = [];
    await I.mockRoute(menuDataRoutes, route => {
        requests.push(new URL(route.request().url()).pathname);
        return route.continue();
    });
    I.amOnPage('/admin/v9/search/');
    I.resizeWindow(1280, 850);
    openMenu(I);
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
    I.assertFalse(/Nové prihlásenie do vášho účtu|zariaden/i.test(menuText), 'The menu must omit new-login actions and device counts.');
    I.assertTrue(['true', 'false'].includes(await I.grabAttributeFrom(badge, 'data-enabled')), 'The 2FA badge must expose its rendered boolean state.');

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
    I.assertDeepEqual(requests, [], 'Rendering and repeatedly opening the menu must not fetch 2FA, sessions or devices.');
    I.wjSetDefaultWindowSize();
    await I.stopMockingRoute(menuDataRoutes);
});

Scenario('2FA badge refreshes once after the settings popup closes and retries a failed refresh', async ({ I }) => {
    I.amOnPage('/admin/v9/search/');
    openMenu(I);
    const initiallyEnabled = (await I.grabAttributeFrom(badge, 'data-enabled')) === 'true';
    const initialLabel = (await I.grabTextFrom(badge)).trim();
    const responses = [
        { status: 200, body: initiallyEnabled ? '' : 'true' },
        { status: 500, body: 'Refresh failed' },
        { status: 200, body: initiallyEnabled ? 'true' : '' }
    ];
    let reads = 0;
    await I.mockRoute(statusRoute, route => {
        const response = responses[reads++];
        return route.fulfill({ contentType: 'text/plain', ...response });
    });
    I.executeScript(() => {
        window.userMenuOriginalPopup = WJ.openPopupDialog;
        WJ.openPopupDialog = url => {
            window.userMenuPopup = { url, closed: false };
            return window.userMenuPopup;
        };
    });
    I.clickCss('.md-user-menu__two-factor');
    I.waitForInvisible(panel, 5);
    I.assertEqual(await I.executeScript(() => window.userMenuPopup.url), '/admin/2factorauth.jsp');
    openMenu(I);
    I.assertEqual(reads, 0, 'Opening the menu while the settings popup is open must not refresh its status.');
    closeMenu(I);
    I.executeScript(() => { window.userMenuPopup.closed = true; });
    openMenu(I);
    I.waitForElement(`${badge}[data-enabled="${!initiallyEnabled}"]`, 5);
    const changedLabel = (await I.grabTextFrom(badge)).trim();
    I.assertNotEqual(changedLabel, initialLabel, 'Refreshing 2FA must update the visible state label.');
    I.assertEqual(reads, 1, 'Closing the settings popup must cause one refresh at the next menu opening.');
    closeMenu(I);
    openMenu(I);
    I.assertEqual(reads, 1, 'Ordinary later openings must reuse the refreshed badge.');

    I.clickCss('.md-user-menu__two-factor');
    I.waitForInvisible(panel, 5);
    I.executeScript(() => { window.userMenuPopup.closed = true; });
    openMenu(I);
    await I.waitForFunction(() => document.querySelector('#admin-user-menu').getAttribute('aria-busy') === 'false', 5);
    I.assertEqual(reads, 2);
    I.assertEqual((await I.grabTextFrom(badge)).trim(), changedLabel, 'A failed refresh must preserve the last known badge.');
    closeMenu(I);
    openMenu(I);
    I.waitForElement(`${badge}[data-enabled="${initiallyEnabled}"]`, 5);
    I.assertEqual((await I.grabTextFrom(badge)).trim(), initialLabel);
    I.assertEqual(reads, 3, 'The next opening must retry the failed refresh.');
    closeMenu(I);
    I.executeScript(() => { WJ.openPopupDialog = window.userMenuOriginalPopup; });
    await I.stopMockingRoute(statusRoute);
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
