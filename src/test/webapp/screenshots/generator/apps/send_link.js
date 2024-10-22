Feature('apps.send_link');

Before(({ login }) => {
    login('admin');
});

Scenario('poslat stranku emailom', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/poslat-stranku-emailom/?NO_WJTOOLBAR=true&language="+I.getConfLng());
    DT.waitForLoader();
    I.clickCss(".sendLink");
    Document.screenshot("/redactor/apps/send_link/send_link.png");

    Document.screenshotAppEditor(77774, "/redactor/apps/send_link/editor.png");

});
