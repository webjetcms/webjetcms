Feature('a11y.dashboard');

Before(({ I, login }) => {
    login('admin');
});

Scenario('dashboard', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/');
    await a11y.check();
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
                title: "Editovať poslednú verziu", //button title
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

    //wait for notifications to appear
    I.wait(2);

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

    I.wait(2); //wait for toast to be fully visible before checking a11y
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
