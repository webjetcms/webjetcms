Feature('apps.user');
Before(({ login }) => {
    login('admin');
});

Scenario('Používatelia', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/pouzivatelia");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/user.png");

    I.amOnPage("/apps/pouzivatelia/linka-autorizaciu-mailovej-adresy.html");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/email.png");

    I.amOnPage("/apps/pouzivatelia/zabudnute-heslo.html");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/password.png");

    I.amOnPage("/apps/pouzivatelia/meno.html");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/name.png");

    I.amOnPage("/apps/pouzivatelia/prihlasovaci-formular.html");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/signin.png");

    Document.screenshotAppEditor(106434, "/redactor/apps/user/editor.png");
});
