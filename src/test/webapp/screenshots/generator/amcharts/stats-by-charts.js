Feature('apps.form-stats');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot stat section', ({I, Document}) => {
    I.resizeWindow(900, 4100);
    I.amOnPage("/apps/form/admin/form-stats/?formName=stattestform");
    Document.screenshotElement("#form-stats", "/developer/frameworks/charts/stats-by-charts/example-1.png");
});

Scenario('screenshot stat section 2', ({I, Document}) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=stattestform");
    I.resizeWindow(900, 1800);

    Document.screenshot("/redactor/apps/multistep-form/stat-section.png");
    Document.screenshotElement(".header-container", "/redactor/apps/multistep-form/stat-section-header.png");
});