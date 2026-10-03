const { showWidget, waitForWidgets, mockDashboardBootstrap, dashboardPageRoute } = require('../../helpers/dashboard-browser');

Feature('a11y.dashboard');

Before(({ I, login }) => {
    login('admin');
});

const settingsRoute = '**/admin/rest/dashboard/settings';
const formsRoute = '**/admin/rest/forms-list/all';
const editOverview = '.md-dashboard__toolbar-actions > button[aria-pressed]';
const editShortcuts = '.md-dashboard__shortcut-actions > button[aria-pressed]';
const modal = '.md-dashboard-modal';
const variants = {
    'recent-pages': ['2x3', '3x2', '3x3'], approvals: ['1x1', '3x3'], publishing: ['2x2', '2x3'],
    forms: ['1x1', '3x3'], traffic: ['1x1', '3x3'], 'top-pages': ['2x3', '3x3'],
    'search-terms': ['2x3', '3x3'], referrers: ['2x2', '2x3', '3x3'], errors: ['1x1', '3x3'],
    newsletter: ['2x2', '3x3'], 'changed-pages': ['3x2', '3x3'], audit: ['3x2', '3x3'],
    'logged-admins': ['2x2', '2x3'], 'server-memory': ['3x2', '3x3'], 'server-cpu': ['3x2', '3x3']
};
const widgetItems = Object.entries(variants).flatMap(([type, sizes]) => sizes.map(size => ({
    id: `a11y-autotest-${type}-${size}`, type, size, options: {}
})));

/** Uses every supported grid variant without overwriting the account's personal layout. */
async function openAuditDashboard(I, items = widgetItems) {
    let settings = {
        version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true,
        acknowledgedNewsVersion: null, domainOptions: {}, items: [
            ...items,
            { id: 'a11y-autotest-shortcut', type: 'shortcut', size: '1x1', options: { source: 'url', href: '/admin/v9/', title: 'Dashboard autotest' } }
        ]
    };
    await I.mockRoute(settingsRoute, route => {
        settings = { ...route.request().postDataJSON(), configured: true };
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(settings) });
    });
    await mockDashboardBootstrap(I, () => ({ settings, notices: ['info', 'warning', 'error'].map(severity => ({
        id: `a11y-autotest-${severity}`, severity, icon: 'ti-info-circle', title: `${severity} notice autotest`,
        bodyHtml: '<p>Expanded notice details autotest.</p>', action: { type: 'link', url: '/admin/v9/', label: 'Notice action autotest' }
    })) }));
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.waitForElement('.md-dashboard__notice-list[aria-busy="false"]', 20);
}

/** Enters a control by keyboard after positioning it in the transformed administration scroller. */
function focusControl(I, selector) {
    I.executeScript(selector => {
        const control = document.querySelector(selector);
        if (control.closest('.modal')) { control.focus(); return; }
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        if (scrollbar.limit.y > 0) scrollbar.setPosition(0, scrollbar.offset.y + control.getBoundingClientRect().top - 100);
        else control.scrollIntoView({ block: 'center' });
        control.focus({ preventScroll: true });
    }, selector);
}

function waitForDialog(I, selector = modal) {
    I.waitForFunction(selector => {
        const dialog = document.querySelector(selector);
        return dialog?.classList.contains('show') && getComputedStyle(dialog).opacity === '1' && dialog.contains(document.activeElement);
    }, [selector], 10);
}

function closeDialog(I, trigger) {
    I.pressKey('Escape');
    I.waitForFunction(() => !document.querySelector('.md-dashboard-modal'), 10);
    I.waitForFunction(selector => document.activeElement === document.querySelector(selector), [trigger], 10);
}

After(async ({ I }) => {
    for (const route of [dashboardPageRoute, settingsRoute, formsRoute]) await I.stopMockingRoute(route);
    I.wjSetDefaultWindowSize();
});

Scenario('dashboard', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await waitForWidgets(I);
    await a11y.check();
});

for (const width of [1337, 320]) {
    Scenario(`All widget variants are audited in the viewport at ${width}px`, async ({ I, a11y }) => {
        I.resizeWindow(width, 1000);
        await openAuditDashboard(I);
        await a11y.check();
        // Scanning only after returning to the top can miss contrast in clipped, offscreen cards.
        for (const item of widgetItems) {
            const card = `[data-instance-id="${item.id}"]`;
            await showWidget(I, item.id);
            I.dontSeeElement(`${card} .md-dashboard__widget-content > .text-danger`);
            await a11y.check(card);
        }
        I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__hero, .md-dashboard__search, .md-dashboard__toolbar, .md-dashboard__widget')]
            .filter(element => {
                const bounds = element.getBoundingClientRect();
                return bounds.left < -1 || bounds.right > window.innerWidth + 1 || element.scrollWidth > element.clientWidth + 1;
            }).map(element => element.dataset.instanceId || element.className)), [], `Dashboard content must reflow at ${width}px.`);
        await a11y.check();
    });
}

Scenario('System notice actions support keyboard dismissal and undo', async ({ I, a11y }) => {
    await openAuditDashboard(I, []);
    await I.mockRoute('**/admin/rest/admin-settings/', route => route.fulfill({ status: 200, contentType: 'application/json', body: 'true' }));
    focusControl(I, '[data-notice-id="a11y-autotest-warning"] .md-dashboard__notice-dismiss');
    I.pressKey('Space');
    I.waitForVisible('.md-dashboard__notice-toast', 5);
    I.dontSeeElement('[data-notice-id="a11y-autotest-warning"]');
    I.seeElement('[data-notice-id="a11y-autotest-error"] .md-dashboard__notice-action');
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard__notice-toast button'), 5);
    await a11y.check('.md-dashboard__notice-toast');
    I.pressKey('Space');
    I.waitToHide('.md-dashboard__notice-toast', 5);
    I.seeElement('[data-notice-id="a11y-autotest-warning"] .md-dashboard__notice-action');
    I.waitForFunction(() => document.activeElement === document.querySelector('[data-notice-id="a11y-autotest-warning"] .md-dashboard__notice-action'), 5);
    await I.stopMockingRoute('**/admin/rest/admin-settings/');
    await a11y.check('.md-dashboard__notices');
});

Scenario('Dashboard controls support Space and keyboard search scope', async ({ I, a11y }) => {
    await openAuditDashboard(I);
    for (const selector of [editShortcuts, editOverview]) {
        focusControl(I, selector);
        I.pressKey('Space');
        I.waitForElement(`${selector}[aria-pressed="true"]`, 5);
        I.pressKey('Space');
        I.waitForElement(`${selector}[aria-pressed="false"]`, 5);
    }
    const newsToggle = '.md-dashboard-widget__news-toggle';
    focusControl(I, newsToggle);
    I.pressKey('Space');
    I.waitForElement(`${newsToggle}[aria-expanded="false"]`, 10);
    I.waitForFunction(selector => document.activeElement === document.querySelector(selector), [newsToggle], 10);
    await a11y.check('.md-dashboard__hero');
    I.pressKey('Space');
    I.waitForElement(`${newsToggle}[aria-expanded="true"]`, 10);
    focusControl(I, '.md-dashboard__search input[type="radio"]:checked');
    I.pressKey('ArrowRight');
    I.waitForFunction(() => document.querySelector('.md-dashboard__search input[value="docs"]').checked, 5);
    I.assertTrue(await I.executeScript(() => {
        const search = document.querySelector('.md-dashboard__search input[type="search"]');
        return search.getAttribute('aria-label') === search.placeholder && search.placeholder.length > 0;
    }), 'The search field must retain an accessible name when switching scope.');
    await a11y.check('.md-dashboard__search');
});

Scenario('Shortcut settings, color choices and movement dialogs are accessible', async ({ I, a11y }) => {
    await openAuditDashboard(I);
    I.clickCss(editShortcuts);
    const addShortcut = '.md-dashboard__shortcut-actions > button:first-child';
    focusControl(I, addShortcut);
    I.pressKey('Enter');
    waitForDialog(I);
    await a11y.check(modal);
    I.clickCss(`${modal} .bootstrap-select:has(select[name="dashboardShortcutGroup"]) > button`);
    I.waitForVisible(`${modal} .bootstrap-select .dropdown-menu.show .bs-searchbox input`, 5);
    await a11y.check(modal);
    I.pressKey('Escape');
    I.waitForInvisible(`${modal} .bootstrap-select .dropdown-menu.show`, 5);
    I.seeElement(modal);
    I.seeElement(`${modal} .bootstrap-select > button[aria-controls][aria-expanded="false"]`);
    I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard-modal .bs-searchbox input')]
        .every(input => input.getAttribute('aria-expanded') === 'false')), 'Closing a picker must update its search combobox state.');
    I.selectOption(`${modal} select[name="dashboardShortcutSource"]`, 'url');
    I.waitForVisible(`${modal} input[name="dashboardShortcutUrl"]`, 5);
    await a11y.check(modal);
    focusControl(I, `${modal} input[type="radio"]:checked`);
    I.pressKey('ArrowRight');
    I.waitForFunction(() => document.querySelector('.md-dashboard-modal input[type="radio"]:checked').value === 'mint', 5);
    I.assertTrue(await I.executeScript(() => {
        const swatch = document.activeElement.nextElementSibling;
        return parseFloat(getComputedStyle(swatch).outlineWidth) >= 2;
    }), 'The selected color must have a visible keyboard focus indicator.');
    closeDialog(I, addShortcut);
    const move = '[data-instance-id="a11y-autotest-shortcut"] .md-dashboard__drag';
    focusControl(I, move);
    I.pressKey('Enter');
    waitForDialog(I);
    await a11y.check(modal);
    closeDialog(I, move);
});

Scenario('Every widget settings form and action menu is accessible', async ({ I, a11y }) => {
    await openAuditDashboard(I);
    I.clickCss(editOverview);
    for (const [type, sizes] of Object.entries(variants)) {
        const id = `a11y-autotest-${type}-${sizes[0]}`;
        const card = `[data-instance-id="${id}"]`;
        const trigger = `${card} .dropdown > button`;
        await showWidget(I, id);
        focusControl(I, trigger);
        I.pressKey('Enter');
        I.waitForVisible(`${card} .dropdown-menu.show`, 5);
        // Audit the open popup; the underlying card was checked without the overlay above it.
        await a11y.check(`${card} .dropdown-menu.show`);
        I.clickCss(`${card} [data-dashboard-action="settings"]`);
        waitForDialog(I);
        I.waitForEnabled(`${modal} .modal-footer button`, 20);
        await a11y.check(modal);
        closeDialog(I, trigger);
    }
});

Scenario('Feedback has a named dialog, required field, accessible validation and focus restoration', async ({ I, a11y }) => {
    await openAuditDashboard(I);
    const trigger = '.md-dashboard__feedback';
    focusControl(I, trigger);
    I.pressKey('Enter');
    waitForDialog(I, '#feedback_modal');
    await a11y.check('#feedback_modal');
    I.assertTrue(await I.executeScript(() => {
        const dialog = document.querySelector('#feedback_modal');
        const title = document.getElementById(dialog.getAttribute('aria-labelledby'));
        return Boolean(title?.textContent.trim());
    }), 'The feedback dialog must expose its visible title as its accessible name.');
    I.seeElement('#feedback-group-text[aria-required="true"]');
    // An empty submission exercises local validation without sending a message or uploading a file.
    I.clickCss('#feedback_modal button[type="submit"]');
    I.waitForVisible('#feedback-text-error:not(.invisible)', 5);
    I.seeElement('#feedback-group-text[aria-invalid="true"]');
    I.waitForFunction(() => document.activeElement?.id === 'feedback-group-text', 5);
    await a11y.check('#feedback_modal');
    I.fillField('#feedback-group-text', 'Feedback draft autotest');
    I.dontSeeElement('#feedback-group-text[aria-invalid="true"]');
    I.dontSeeElement('#feedback-text-error:not(.invisible)');
    I.pressKey('Escape');
    I.waitForFunction(() => !document.querySelector('#feedback_modal'), 10);
    I.waitForFunction(selector => document.activeElement === document.querySelector(selector), [trigger], 10);
});

Scenario('Empty and failed widget content is audited after loading completes', async ({ I, a11y }) => {
    let failed = false;
    await I.mockRoute(formsRoute, route => route.fulfill({ status: failed ? 503 : 200, contentType: 'application/json', body: JSON.stringify(failed
        ? { reason: 'domain-unavailable' }
        : { content: [] }) }));
    const item = widgetItems.find(item => item.type === 'forms' && item.size === '3x3');
    await openAuditDashboard(I, [item]);
    await showWidget(I, item.id);
    await a11y.check();
    failed = true;
    I.refreshPage();
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await showWidget(I, item.id);
    const card = `[data-instance-id="${item.id}"]`;
    I.seeElement(`${card} .md-dashboard__widget-content > .text-danger`);
    await a11y.check();
    failed = false;
    focusControl(I, `${card} .md-dashboard__widget-content > button`);
    I.pressKey('Space');
    I.waitForInvisible(`${card} .md-dashboard__widget-content > .text-danger`, 10);
    await showWidget(I, item.id);
    await a11y.check(card);
});

Scenario('Widget catalogue supports keyboard entry, a focus trap and focus restoration', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    await I.executeScript(() => document.querySelector('.md-dashboard__toolbar-actions .md-dashboard__edit-control:not(.md-dashboard__reset)').focus());
    I.pressKey('Enter');
    I.waitForVisible('.md-dashboard-modal input[type="search"]', 10);
    I.seeElement('.md-dashboard-modal--catalogue .modal-dialog-centered');
    I.seeElement('.md-dashboard__catalogue-filter[data-category="all"][aria-pressed="true"]');
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard-modal input[type="search"]'), 10);
    await a11y.check('.md-dashboard-modal');
    I.pressKey(['Shift', 'Tab']);
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard-modal .modal-header button'), 10);
    I.pressKey(['Shift', 'Tab']);
    I.waitForFunction(() => document.querySelector('.md-dashboard-modal').contains(document.activeElement) && document.activeElement !== document.querySelector('.md-dashboard-modal .modal-header button'), 10);
    I.pressKey('Escape');
    I.waitForInvisible('.md-dashboard-modal', 10);
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard__toolbar-actions .md-dashboard__edit-control:not(.md-dashboard__reset)'), 10);
    I.pressKey(['Shift', 'Tab']);
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard__toolbar-actions .md-dashboard__reset'), 10);
    I.pressKey('Enter');
    const resetDialog = '#toast-container-webjet .toast[role="dialog"]';
    I.waitForVisible(`${resetDialog} button[id^="confirmationYes"]`, 10);
    I.waitForFunction(() => {
        const modal = document.querySelector('#toast-container-webjet .toast[role="dialog"]');
        // Check contrast after the focused cancel button finishes its color transition.
        return modal && getComputedStyle(modal).opacity === '1' && modal.contains(document.activeElement)
            && modal.getAnimations({ subtree: true }).every(animation => animation.playState === 'finished');
    }, 10);
    I.seeElement(`${resetDialog}[aria-modal="true"][aria-labelledby][aria-describedby]`);
    I.assertTrue(await I.executeScript(() => document.activeElement?.id.startsWith('confirmationNo')), 'The standard confirmation must initially focus its safe cancel action.');
    await a11y.check(resetDialog);
    I.pressKey('Escape');
    I.waitForInvisible(resetDialog, 10);
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard__toolbar-actions .md-dashboard__reset'), 10);
});

Scenario("show all notification types", async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');

    await I.executeScript(() => {
        const notificationTypes = [
            'success',
            'error',
            'warning',
            'info',
        ];

        const buttons = [
            {
                title: "Edit latest version", //button title
                cssClass: "btn btn-primary", //button CSS class
                icon: "ti ti-pencil", //optional: Tabler icon
                click: "editFromHistory(38, 33464)", //onclick function
                closeOnClick: true //close toastr on button click, default true
            }
        ];

        notificationTypes.forEach(type => {
            WJ.notify(type, `This is a ${type} notification`, `This is sample text for a ${type} notification`, 0, buttons);
        });
    });

    I.waitForFunction(() => {
        const notifications = [...document.querySelectorAll('#toast-container-webjet .toast')];
        return notifications.length === 4 && notifications.every(toast => getComputedStyle(toast).opacity === '1');
    }, 10);

    await a11y.check();
});

Scenario("p44: confirm notification focus", async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');

    await I.executeScript(() => {
        const trigger = document.createElement('button');
        trigger.id = 'a11y-confirm-trigger';
        trigger.textContent = 'Open confirmation';
        document.body.appendChild(trigger);
        trigger.focus();

        WJ.confirm({
            title: 'Confirmation title',
            message: 'Confirmation message'
        });
    });

    const dialogSelector = '#toast-container-webjet .toast';

    I.waitForElement(dialogSelector, 10);
    I.waitForFunction(() => document.activeElement?.id.startsWith('confirmationNo'));
    I.seeElement(`${dialogSelector}[aria-modal="true"][aria-labelledby][aria-describedby]`);

    I.waitForFunction(() => getComputedStyle(document.querySelector('#toast-container-webjet .toast')).opacity === '1', 10);
    await a11y.check(dialogSelector);

    I.pressKey('Tab');
    I.waitForFunction(() => document.activeElement?.id.startsWith('confirmationYes'));
    I.assertTrue(await I.executeScript(() => {
        const style = getComputedStyle(document.activeElement);
        return style.outlineStyle === 'solid' && parseFloat(style.outlineWidth) >= 2;
    }), 'Primary button must have a visible keyboard focus indicator');

    I.pressKey('Tab');
    I.waitForFunction(() => document.activeElement?.classList.contains('toast-close-button'));
    I.assertTrue(await I.executeScript(() => {
        const style = getComputedStyle(document.activeElement);
        return style.outlineStyle === 'solid' && parseFloat(style.outlineWidth) >= 2;
    }), 'Close button must have a visible keyboard focus indicator');

    I.pressKey(['Shift', 'Tab']);
    I.waitForFunction(() => document.activeElement?.id.startsWith('confirmationYes'));

    I.pressKey('Escape');
    I.waitForInvisible(dialogSelector, 10);
    I.waitForFunction(() => document.activeElement?.id === 'a11y-confirm-trigger');
});
