Feature('stat.top');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot pie chart', async ({I, DTE, Document}) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=Multistepform_screens");

    Document.scrollTo("#form-stats_radiogroup-1_container")
    Document.screenshotElement("#form-stats_radiogroup-1_container", "/developer/frameworks/charts/frontend/pie-chart-donut.png");

    Document.scrollTo("#form-stats_checkboxgroup-1_container")
    Document.screenshotElement("#form-stats_checkboxgroup-1_container", "/developer/frameworks/charts/frontend/pie-chart-classic.png");
});