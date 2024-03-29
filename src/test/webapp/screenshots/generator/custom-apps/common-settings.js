Feature('custom-apps.common-settings');

Before(({ I, login }) => {
    login('admin');
});

Scenario('Screen', ({I, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=79733");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-content-tab");
    I.click("#cke_46");

    //1. iframe
    I.waitForElement("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe", 5);
    I.switchTo("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe");

    //2. iframe
    I.switchTo("iframe#editorComponent");

    I.seeElement("input#search");
    I.fillField("input#search", "Demo komponenta");
    I.seeElement("#demo-komponenta");
    I.clickCss("#demo-komponenta");

    I.clickCss("body > div > div.store > div.promo > div > div.content.app-info > div > div > div.app-buy > a");
    I.waitForElement("#component-datatable_modal", 10);

    I.click("#pills-dt-component-datatable-commonSettings-tab");

    I.switchTo();

    Document.screenshotElement("table.cke_dialog ", "/custom-apps/appstore/common-settings-tab.png");
});