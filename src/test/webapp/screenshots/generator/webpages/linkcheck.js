Feature('webpages.link-check');

Before(({ I, login }) => {
    login('admin');
});

let groupId = 67;

Scenario('generovanie screenov',({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/linkcheck?groupId=" + groupId);

    Document.screenshot("/redactor/webpages/linkcheck-datatable.png");

    I.amOnPage('/admin/v9/webpages/web-pages-list/');

    Document.screenshotElement("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.btn-outline-secondary.buttons-linkcheck", "/redactor/webpages/linkcheck-href-button.png");
});