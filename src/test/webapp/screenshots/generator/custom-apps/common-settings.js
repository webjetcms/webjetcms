Feature('custom-apps.common-settings');

Before(({ I, login }) => {
    login('admin');
});

Scenario('Screen', ({I, DTE, Document, Apps}) => {

    Apps.openAppEditor(30226);

    I.wait(2);
    I.clickCss("#pills-dt-component-datatable-commonSettings-tab");

    I.switchTo();

    Document.screenshotElement("table.cke_dialog ", "/custom-apps/appstore/common-settings-tab.png");
});