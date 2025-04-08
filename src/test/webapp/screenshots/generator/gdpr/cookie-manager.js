Feature('cookie-manger');

Before(({ I, login }) => {
    login('admin');
});

Scenario('gdpr-cookie-manager', ({ I, Document }) => {
    I.amOnPage("/apps/gdpr/admin/");

    //Cookies data table
    Document.screenshot("/redactor/apps/gdpr/cookiemanager-datatable.png");

    //Jazykovy selektor
    Document.screenshotElement("#cookiesDataTable_wrapper  div.breadcrumb-language-select", "/redactor/apps/gdpr/cookiemanager-jazykovy-selector.png");

    //Domenovy selektor
    Document.screenshotElement("div.header-link-group", "/redactor/apps/gdpr/cookiemanager-domenovy-selector.png");

});