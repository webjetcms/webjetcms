Feature('apps.user');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Pouzivatelia @current', async ({ I, Apps }) => {
    Apps.insertApp('Používatelia', '#menu-users');

    const defaultParams = {
        type: "iframe",
        noPaging: "true",
        sortAscending : "false"
    };

    await Apps.assertParams(defaultParams);



    //TODO test

});