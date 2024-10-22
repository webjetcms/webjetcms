Feature('apps.insert-script');

Before(({ I, login }) => {
    login('admin');
});

Scenario('insert-script screeny', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/admin/v9/apps/insert-script/");
    Document.screenshot("/redactor/apps/insert-script/editor.png", 1500, 800);

    I.amOnPage("/admin/v9/apps/insert-script/?id=58");
    DTE.waitForEditor('insertScriptTable');

    I.clickCss('a#pills-dt-insertScriptTable-main-tab');
    Document.screenshotElement("div.DTE.modal-content","/redactor/apps/insert-script/main.png");

    I.clickCss('a#pills-dt-insertScriptTable-scriptPerms-tab');
    Document.screenshotElement("div.DTE.modal-content","/redactor/apps/insert-script/perms.png");

    I.clickCss('a#pills-dt-insertScriptTable-scriptBody-tab');
    Document.screenshotElement("div.DTE.modal-content","/redactor/apps/insert-script/body.png");
});