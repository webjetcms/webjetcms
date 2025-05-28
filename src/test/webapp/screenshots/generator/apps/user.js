Feature('apps.user');
Before(({ login }) => {
    login('admin');
});

Scenario('Používatelia', ({ I, DT, Document }) => {

    const lng = I.getConfLng();

    I.amOnPage("/apps/pouzivatelia/?language=" + lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/user.png");

    I.amOnPage("/apps/pouzivatelia/linka-autorizaciu-mailovej-adresy.html?language=" + lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/email.png");

    I.amOnPage("/apps/pouzivatelia/zabudnute-heslo.html?language=" + lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/password.png");

    I.amOnPage("/apps/pouzivatelia/meno.html?language=" + lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/name.png");

    I.amOnPage("/apps/pouzivatelia/prihlasovaci-formular.html?language=" + lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/user/signin.png");

    Document.screenshotAppEditor(106434, "/redactor/apps/user/editor.png");
});
