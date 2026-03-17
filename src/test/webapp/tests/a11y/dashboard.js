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

    I.executeScript(() => {
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

    await a11y.check();
});