Feature('stat.serach-engines');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot bar chart', async ({I, DTE, Document}) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=Multistepform_screens");

    Document.scrollTo("#form-stats_select-1_container");
    Document.screenshotElement("#form-stats_select-1_container", "/developer/frameworks/charts/frontend/bar-chart-horizontal.png");

    Document.scrollTo("#form-stats_radiogroup-2_container");
    Document.screenshotElement("#form-stats_radiogroup-2_container", "/developer/frameworks/charts/frontend/bar-chart-vertical.png");
});