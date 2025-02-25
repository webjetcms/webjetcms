Feature('apps.seo');

let confLng = "sk";

Before(({ I, login }) => {
    login('admin');

    confLng = I.getConfLng();
});

Scenario("seo - screenshots", ({ I, Document, DT, i18n }) => {
    I.amOnPage("/apps/seo/admin/");
    I.waitForInvisible("#loader", 20);
    DT.setDates(i18n.getDate("07/01/2021"), i18n.getDate("07/30/2021"), "#botsDataTable_extfilter");
    DT.waitForLoader();
    I.waitForInvisible("#webjetAnimatedLoader");
    Document.screenshot("/redactor/apps/seo/seo-admin-page.png", 1500, 800);

    DT.filterContains("name", "Googlebot 2.0");
    I.click("Googlebot 2.0");
    DT.waitForLoader();
    I.waitForInvisible(".webjetAnimatedLoader");
    Document.screenshot("/redactor/apps/seo/seo-admin-details-page.png", 1500, 800);

    I.amOnPage("/apps/seo/admin/management-keywords/");
    Document.screenshot("/redactor/apps/seo/seo-management-keywords-page.png", 1500, 800);
    DT.filterContains("name", "Redakčný systém WebJET");
    DT.filterContains("domain", "www.webjetcms.sk");
    I.click("Redakčný systém WebJET");
    DT.waitForLoader();
    I.waitForInvisible(".webjetAnimatedLoader");
    Document.screenshotElement("#managementKeywordsDataTable_modal > div > div", "/redactor/apps/seo/seo-management-keywords-editor.png");

    I.amOnPage("/apps/seo/admin/stat-keywords/");
    Document.screenshot("/redactor/apps/seo/seo-stat-keywords-page.png", 1500, 800);
    DT.filterContains("queryName", "archiv");
    I.click("archiv");
    DT.waitForLoader();
    I.waitForInvisible(".webjetAnimatedLoader");
    Document.screenshot("/redactor/apps/seo/seo-stat-keywords-detail-page.png", 1500, 800);

    I.amOnPage("/apps/seo/admin/positions/");
    Document.screenshot("/redactor/apps/seo/seo-positions-page.png", 1500, 800);
    DT.filterContains("name", "Redakčný systém WebJET");
    DT.filterContains("domain", "www.webjetcms.sk");
    I.click("Redakčný systém WebJET");
    DT.waitForLoader();
    I.waitForInvisible(".webjetAnimatedLoader");
    Document.screenshot("/redactor/apps/seo/seo-positions-details-page.png", 1500, 800);

    I.amOnPage("/apps/seo/admin/number-keywords/");
    Document.screenshot("/redactor/apps/seo/seo-number-keywords-page.png", 1500, 800);
});

Scenario("seo - screenshots webpage", ({ I, Document, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(8);

    I.switchTo("#DTE_Field_data-pageBuilderIframe");

    I.clickCss("a.cke_button__components");
    I.wait(6);

    I.switchTo();
    I.switchTo("#DTE_Field_data-pageBuilderIframe");

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent")

    I.forceClickCss("#components-seo-title");
    I.wait(3);
    switch (confLng) {
        case 'sk':
            I.click("Vložiť do stránky");
            break;
        case 'en':
            I.click("Add to page");
            break;
        case 'cs':
            I.click("Vložit do stránky");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    I.wait(10);

    Document.screenshot("/redactor/apps/seo/seo-app.png");
});

