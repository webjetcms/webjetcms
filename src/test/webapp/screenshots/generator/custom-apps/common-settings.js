Feature('custom-apps.common-settings');

Before(({ I, login }) => {
    login('admin');
});

Scenario('Screen', ({I, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=79733");
    DTE.waitForEditor();
    I.wait(3);

    //I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForElement("#cke_46", 10);
    I.clickCss("#cke_46");

    //1. iframe
    I.waitForElement("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe", 5);
    I.switchTo("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe");

    //2. iframe
    I.switchTo("iframe#editorComponent");

    I.waitForElement("input#search", 10);
    I.wait(2);
    I.fillField("input#search", "Demo komponenta");
    I.seeElement("#demo-komponenta");
    I.wait(2);
    I.clickCss("#demo-komponenta");

    I.wait(2);
    I.clickCss("body > div > div.store > div.promo > div > div.content.app-info > div > div > div.app-buy > a");
    I.wait(2);
    I.waitForElement("#component-datatable_modal", 10);

    I.wait(2);
    I.clickCss("#pills-dt-component-datatable-commonSettings-tab");

    I.switchTo();

    Document.screenshotElement("table.cke_dialog ", "/custom-apps/appstore/common-settings-tab.png");
});