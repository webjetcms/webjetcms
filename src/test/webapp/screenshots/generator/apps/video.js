Feature('apps.video');

Before(({ login }) => {
    login('admin');
});

Scenario('video app screens', ({ I, DT, Document, Apps }) => {

    Apps.insertApp('Video', '#components-video-title', null, false);

    I.switchTo('iframe[src$="webjetcomponet.jsp"]');
    I.switchTo('#editorComponent');

    Document.screenshot("/redactor/apps/video/editor-source.png");


    I.clickCss(".image_radio_item > label[for=DTE_Field_field_0]");
    Document.screenshot("/redactor/apps/video/editor-youtube.png");

    I.clickCss(".image_radio_item > label[for=DTE_Field_field_1]");
    Document.screenshot("/redactor/apps/video/editor-vimeo.png");

    I.clickCss(".image_radio_item > label[for=DTE_Field_field_2]");
    Document.screenshot("/redactor/apps/video/editor-facebook.png");

    I.clickCss(".image_radio_item > label[for=DTE_Field_field_3]");
    Document.screenshot("/redactor/apps/video/editor-video.png");

    I.amOnPage("/apps/video/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/video/video.png");
});
