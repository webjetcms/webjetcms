Feature('manual-custom-apps');

Before(({ I, login }) => {
    login('admin');
});

Scenario('backend', ({ I, Document }) => {
    I.amOnPage("/apps/contact/admin/");

    //cela stranka
    Document.screenshot("/custom-apps/admin-menu-item/contact.png");

    //breadcrumb
    Document.screenshotElement("div.ly-container div.md-breadcrumb", "/custom-apps/admin-menu-item/breadcrumb.png");

    //datatable
    Document.screenshotElement("#dataTable_wrapper", "/custom-apps/admin-menu-item/datatable.png");

    //breadcrumb s vyberom jazyka
    I.amOnPage("/apps/gdpr/admin/");
    Document.screenshotElement("div.ly-container div.md-breadcrumb", "/developer/frameworks/breadcrumb-language.png");
});

Scenario('frontend', ({ I, Document }) => {
    I.amOnPage("/apps/spring-app/kontakty/?NO_WJTOOLBAR=true");

    //cela stranka
    Document.screenshot("/custom-apps/spring-mvc/list.png");

    //editacia
    I.click("Upraviť", "div.container table.table tbody tr:nth-child(2)");
    Document.screenshot("/custom-apps/spring-mvc/edit.png");

    I.amOnPage("/apps/spring-app/kontakty/?NO_WJTOOLBAR=true");
    I.click("Nový kontakt");
    I.click("Potvrdiť");
    Document.screenshot("/custom-apps/spring-mvc/validation.png");
});

Scenario('admin-with-upload', ({ I, Document }) => {
    I.amOnPage("/apps/contact/admin/upload/");

    I.click("Potvrdiť");

    //cela stranka
    Document.screenshot("/custom-apps/spring-mvc/admin-upload.png");
});