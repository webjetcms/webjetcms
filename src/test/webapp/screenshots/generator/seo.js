Feature('apps.seo');

let confLng = "sk";

Before(({ I, login }) => {
    login('admin');

    confLng = I.getConfLng();
});

function setDates(I, filterName) {

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    if("sk" === confLng || "cs" === confLng) {
        within(filterName, () => {
            I.fillField({css: "input.dt-filter-from-dayDate"}, "01.07.2021");
            I.fillField({css: "input.dt-filter-to-dayDate"}, "30.07.2021");
            I.click({css: "button.dt-filtrujem-dayDate"});
        });
    } else if("en" === confLng) { 
        within(filterName, () => {
            I.fillField({css: "input.dt-filter-from-dayDate"}, "07/01/2021");
            I.fillField({css: "input.dt-filter-to-dayDate"}, "07/30/2021");
            I.click({css: "button.dt-filtrujem-dayDate"});
        });
    }

    I.dtWaitForLoader();
}

Scenario("seo - screenshots", ({ I, Document, DT }) => {
    I.amOnPage("/apps/seo/admin/");

    setDates(I, "#botsDataTable_extfilter");
    I.waitForInvisible("#webjetAnimatedLoader");
    Document.screenshot("/redactor/apps/seo/seo-admin-page.png", 1500, 800);

    DT.filter("name", "YandexBot 3.0");
    I.click("YandexBot 3.0");
    DT.waitForLoader();
    I.waitForInvisible("#webjetAnimatedLoader");
    Document.screenshot("/redactor/apps/seo/seo-admin-details-page.png", 1500, 800);

    I.amOnPage("/apps/seo/admin/management-keywords/");
    Document.screenshot("/redactor/apps/seo/seo-management-keywords-page.png", 1500, 800);
    DT.filter("name", "Redakčný systém WebJET");
    DT.filter("domain", "www.webjetcms.sk");
    I.click("Redakčný systém WebJET");
    DT.waitForLoader();
    I.waitForInvisible("#webjetAnimatedLoader");
    Document.screenshotElement("#managementKeywordsDataTable_modal > div > div", "/redactor/apps/seo/seo-management-keywords-editor.png");

    I.amOnPage("/apps/seo/admin/stat-keywords/");
    Document.screenshot("/redactor/apps/seo/seo-stat-keywords-page.png", 1500, 800);
    DT.filter("queryName", "archiv");
    I.click("archiv");
    DT.waitForLoader();
    I.waitForInvisible("#webjetAnimatedLoader");
    Document.screenshot("/redactor/apps/seo/seo-stat-keywords-detail-page.png", 1500, 800);

    I.amOnPage("/apps/seo/admin/positions/");
    Document.screenshot("/redactor/apps/seo/seo-positions-page.png", 1500, 800);
    DT.filter("name", "Redakčný systém WebJET");
    DT.filter("domain", "www.webjetcms.sk");
    I.click("Redakčný systém WebJET");
    DT.waitForLoader();
    I.waitForInvisible("#webjetAnimatedLoader");
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

