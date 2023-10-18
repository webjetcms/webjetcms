Feature('settings.missing-keys');

Before(({ I, login }) =>{
    login('admin');
});

Scenario('missing-keys', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=11");
    DTE.waitForEditor();

    //Campaings data table
    I.amOnPage("/admin/v9/settings/missing-keys/");
    Document.screenshot("/admin/settings/missing-keys/dataTable.png");

    I.click("Zoznam kontaktov");
    DTE.waitForEditor();

    Document.screenshot("/admin/settings/missing-keys/editor.png");
});