Feature('apps.user');

Before(({ login }) => {
    login('admin');
});

Scenario('User app screens', ({ I, DT, Document, i18n }) => {

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

    Document.screenshotAppEditor(106434, "/redactor/apps/user/editor.png", function(Document, I, DT, DTE) {
        I.clickCss("#pills-dt-component-datatable-showed-tab");
        Document.screenshot("/redactor/apps/user/editor-showed.png");

        I.clickCss("#pills-dt-component-datatable-required-tab");
        Document.screenshot("/redactor/apps/user/editor-required.png");

        I.clickCss("#pills-dt-component-datatable-basic-tab");
        DTE.selectOption("field", i18n.get("Logon form"));
        Document.screenshot("/redactor/apps/user/editor-login_form.png");

        DTE.selectOption("field", i18n.get("Registration form"));
    });
});