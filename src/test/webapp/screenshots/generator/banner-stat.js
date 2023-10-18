Feature('apps.banner-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('banner-stat a banner-detail', ({I, DTE, Document}) => {
    I.amOnPage("/apps/banner/admin/banner-stat");

    I.click("div.md-breadcrumb input.dt-filter-from-dayDate");
    I.fillField("div.md-breadcrumb input.dt-filter-from-dayDate", "01.05.2022");
    I.click("#bannerStatDataTable_extfilter > div > div.col-auto.dt-extfilter.dt-extfilter-dayDate > form > div > button");
    I.wait(2);

    //Header
    Document.screenshotElement("#pills-banner-stat", "/redactor/apps/banner/banner-stat/header.png");

    //Table
    Document.screenshot("/redactor/apps/banner/banner-stat/stat-table.png");

    I.click("Investičný vklad");
    I.wait(2);

    //Table
    Document.screenshot("/redactor/apps/banner/banner-stat/detail-table.png");

    I.click("Investičný vklad");
    I.wait(2);
    //Table
    Document.screenshot("/redactor/apps/banner/banner-stat/editor.png");
});