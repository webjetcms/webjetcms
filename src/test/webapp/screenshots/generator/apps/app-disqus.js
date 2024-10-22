Feature('apps.app-disqus');
Before(({ login }) => {
    login('admin');
});

Scenario('Disqus komentare', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/disqus-komentare");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-disqus/app-disqus.png");
    Document.screenshotAppEditor(106333, "/redactor/apps/app-disqus/editor.png");
});
