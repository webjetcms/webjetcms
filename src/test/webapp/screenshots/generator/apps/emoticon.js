Feature('apps.emoticon');

Before(({ login }) => {
    login('admin');
});

Scenario('Emotikon app screens', ({ I, Document, Apps }) => {
    Apps.insertApp('Emotikony', '#components-emoticon-title', null, false);

    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    I.moveCursorTo("#panel-body-dt-component-datatable-basic img[src*=biggrin]");

    Document.screenshot("/redactor/apps/emoticon/editor.png");

    I.clickCss("#panel-body-dt-component-datatable-basic img[src*=biggrin]");
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    Document.screenshot("/redactor/apps/emoticon/emoticon.png");
});