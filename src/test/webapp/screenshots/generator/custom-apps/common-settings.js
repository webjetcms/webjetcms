Feature('custom-apps.common-settings');

Before(({ I, login }) => {
    login('admin');
});

Scenario('Screen', async ({I, DTE, Document, Apps}) => {

    Apps.openAppEditor(30226);

    I.wait(2);
    I.clickCss("#pills-dt-component-datatable-commonSettings-tab");

    I.clickCss("label[for=DTE_Field_device_1]")

    await DTE.selectOptionMulti("wrapperClass", ["mt-3","w-50"]);
    // Fill wrapper text fields
    I.fillField("#DTE_Field_wrapperId", "banner-wrapper-id");

    I.switchTo();

    Document.screenshotElement("table.cke_dialog ", "/custom-apps/appstore/common-settings-tab.png");
});