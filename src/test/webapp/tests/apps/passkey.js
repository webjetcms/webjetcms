Feature('apps.passkey');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/apps/passkey/admin/");
});

Scenario('passkey-zakladne testy', async ({ I, DataTables, DT, DTE }) => {
    // Verify the page loads with the passkey datatable
    DT.waitForLoader("passkeyDataTable");

    I.seeElement({css: ".buttons-create"});

    // Verify the import button is hidden
    I.dontSeeElement({css: ".buttons-import"});

    //TODO: test passkeys for different users - check that user can see only their passkeys

});
