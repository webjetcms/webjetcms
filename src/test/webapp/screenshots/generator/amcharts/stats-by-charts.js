Feature('apps.form-stats');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot stat section', ({I, Document}) => {
    I.resizeWindow(1280, 4900);
    I.amOnPage("/apps/form/admin/form-stats/?formName=Multistepform_screens");
    Document.screenshot("/developer/frameworks/charts/stats-by-charts/example-1.png");
});

Scenario('screenshot stat section 2', ({I, Document}) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=Multistepform_screens");

    Document.screenshot("/redactor/apps/multistep-form/stat-section-header.png");

    I.resizeWindow(1280, 1800);

    Document.scrollTo("#form-stats_pohlavie-false_container");
    Document.screenshot("/redactor/apps/multistep-form/stat-section.png");

    I.wjSetDefaultWindowSize();
});

Scenario('screenshot stat section 3', ({I, Document}) => {
    // Advanced stats
    I.amOnPage("/apps/form/admin/form-stats/?formName=mutistepform_advanced_error_stats");

    I.clickCss("#expandBonusChartButton");

    Document.screenshot("/redactor/apps/multistep-form/stat-section-header-advanced.png");

    I.resizeWindow(1280, 1950);

    Document.screenshot("/redactor/apps/multistep-form/stat-section-advanced.png");
});