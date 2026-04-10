Feature('apps.form-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot bar chart from search engines', async ({I, Document}) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=Multistepform_screens");


    Document.scrollTo("#form-stats_wysiwyg-1_container");
    Document.screenshotElement("#form-stats_wysiwyg-1_container", "/developer/frameworks/charts/frontend/table-chart.png");
});
