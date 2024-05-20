Feature('apps.proxy');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('zakladne testy', async ({I, DataTables}) => {
    I.amOnPage("/apps/proxy/admin/");
    await DataTables.baseTest({
        dataTable: 'proxyDataTable',
        perms: 'cmp_proxy',
        requiredFields: ['name', 'localUrl', 'remoteServer', 'remoteUrl'],
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
        skipSwitchDomain: true
    });
});

Scenario('REST proxy test', async ({I, DataTables}) => {
    I.amOnPage("/todos/1/");
    I.see("\"title\"");
    I.see("\"id\": 1");
});