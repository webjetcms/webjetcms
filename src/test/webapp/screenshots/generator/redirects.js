Feature('apps.redirects');

var edit_button = (locate("button.btn.btn-sm.buttons-selected.buttons-edit.btn-warning"));
var add_button = (locate('button.btn.btn-sm.buttons-create.btn-success.buttons-divider'));
Before(({login }) => {
    login('admin');
});

Scenario('redirects screenshots', ({ I, DT, DTE,  Document }) => {
    I.amOnPage("/admin/v9/settings/redirect/");

    Document.screenshot("/redactor/webpages/redirects/redirect-path.png");
    I.click(add_button);
    DTE.waitForEditor();
    DTE.fillField('oldUrl', '/test-stavov/virtualpath/podla-title.html');
    DTE.fillField('newUrl', '/test-stavov/virtualpath/podla-zmena-title.html');
    DTE.fillField('redirectCode', '301');
    DTE.fillField('domainName', 'demotest.webjetcms.sk');
    Document.screenshotElement('.DTE.modal-content.DTE_Action_Create',"/redactor/webpages/redirects/path-editor.png");


    I.amOnPage("/admin/v9/settings/domain-redirect/");
    DT.filter("redirectTo", "iway.sk");

    Document.screenshot("/redactor/webpages/redirects/redirect-domain.png");
    I.click(add_button);
    DTE.waitForEditor();
    DTE.fillField('redirectFrom', 'demo.webjetcms.sk');
    DTE.selectOption('protocol', 'alias');

    I.checkOption('#DTE_Field_active_0');
    I.checkOption('#DTE_Field_redirectParams_0');
    I.checkOption('#DTE_Field_redirectPath_0');
    Document.screenshotElement('.DTE.modal-content.DTE_Action_Create',"/redactor/webpages/redirects/domain-editor.png");

});

