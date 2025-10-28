Feature('apps.response-header');

Before(({ I, login }) => {
    login('admin');
});

Scenario('response headers screenshots', ({ I, DT, DTE, Document }) => {
    var cspLongHeader = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://code.jquery.com https://stackpath.bootstrapcdn.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:;";

    I.amOnPage("/apps/response-header/admin/");

    Document.screenshot("/admin/settings/response-header/dataTable.png");

    DT.filterContains("url", "/apps/http-hlavicky/");
    I.click("/apps/http-hlavicky/");

    DTE.waitForEditor("responseHeadersDataTable");
    DTE.fillField("headerName", "Content-Security-Policy");
    DTE.fillField("headerValue", cspLongHeader);

    Document.screenshot("/admin/settings/response-header/editor.png");

    DTE.cancel();

    I.amOnPage("/apps/response-header/admin/?id=407");
    DTE.waitForEditor("responseHeadersDataTable");
    DTE.fillField("url", "/apps/http-hlavicky/*.html,*.jpg,*.png");
    Document.screenshot("/admin/settings/response-header/editor-wildcard.png");
});