Feature('templates');

Before(({ I, login }) => {
    login('admin');
});

Scenario('templates', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();
    Document.screenshot("/frontend/templates/templates.png");
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=7");
    DTE.waitForEditor();
    Document.screenshot("/frontend/templates/templates-edit.png");
});

Scenario('temps-groups', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    DT.waitForLoader();
    Document.screenshot("/frontend/templates/temps-groups.png");
    I.click("Demo JET");
    DTE.waitForEditor();
    Document.screenshot("/frontend/templates/temps-groups-edit.png");
});
