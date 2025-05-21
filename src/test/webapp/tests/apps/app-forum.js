Feature('apps.app-forum');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Jednoducha diskusia @current', async ({ I, Apps }) => {
    Apps.insertApp('Diskusia', '#components-forum-title');

    const defaultParams = {
        type: "iframe",
        noPaging: "true",
        sortAscending : "false"
    };

    await Apps.assertParams(defaultParams);



    //TODO test

});