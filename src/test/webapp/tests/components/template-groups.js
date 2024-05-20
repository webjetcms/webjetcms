Feature('components.template-groups');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/templates/temps-groups-list");
});

Scenario('template-groups-zakladne testy', async ({ I, DataTables }) => {
    I.see("Názov skupiny");
    await DataTables.baseTest({
        dataTable: 'tempsGroupsListTable',
        requiredFields: ['name'],
        perms: 'menuTemplatesGroup',
        skipSwitchDomain: true
    });
});

Scenario('ukladanie metadat', ({ I, DT, DTE }) => {
    I.click("Demo JET");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-metadata-tab");
    I.seeInField("#DTE_Field_projectDeveloper", "InterWay Developer SK");
    I.seeInField("#DTE_Field_projectGenerator", "WebJET CMS Generator SK");

    DTE.cancel();

    //prepni jazyk na en
    I.click("Slovenský jazyk", "div.breadcrumb-language-select");
    I.click("#bs-select-2-2");
    DT.waitForLoader();

    I.click("Demo JET");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-metadata-tab");
    I.seeInField("#DTE_Field_projectDeveloper", "Developer EN");
    I.seeInField("#DTE_Field_projectGenerator", "Generator EN");

    DTE.cancel();
});

function testPageSource(I) {
    I.amOnPage("/?NO_WJTOOLBAR=true");
    I.seeInSource('<meta name="generator" content="WebJET CMS Generator SK"');
    I.seeInSource('<meta name="copyright" content="© 2018 WebJET CMS"');
    I.dontSeeInSource('<meta name="generator" content="Generator EN"');
    I.dontSeeInSource('<meta name="copyright" content="Copyright EN"');

    I.amOnPage("/en/?NO_WJTOOLBAR=true");
    I.dontSeeInSource('<meta name="generator" content="WebJET CMS Generator SK"');
    I.dontSeeInSource('<meta name="copyright" content="© 2018 WebJET CMS"');
    I.seeInSource('<meta name="generator" content="Generator EN"');
    I.seeInSource('<meta name="copyright" content="Copyright EN"');

    I.amOnPage("/test-stavov/czech-language/?NO_WJTOOLBAR=true");
    I.dontSeeInSource('<meta name="generator" content="WebJET CMS Generator SK"');
    I.dontSeeInSource('<meta name="copyright" content="© 2018 WebJET CMS"');
    I.dontSeeInSource('<meta name="generator" content="Generator EN"');
    I.dontSeeInSource('<meta name="copyright" content="Copyright EN"');
    I.seeInSource('<meta name="generator" content="Generator CZ"');
    I.seeInSource('<meta name="copyright" content="Copyright CZ"');

}

Scenario('zobrazenie metadat na stranke', ({ I }) => {
    testPageSource(I);
});

Scenario('overenie nacitania podla prihlaseneho jazyka', ({ I, DTE }) => {
    I.logout();

    I.amOnPage("/admin/logon/?language=cz");

    I.fillField("username", "tester");
    I.fillField("password", secret("*********"));
    I.forceClick("Přihlásit se");
    I.wait(3);

    I.amOnPage("/admin/v9/templates/temps-groups-list");

    I.click("Demo JET");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-metadata-tab");
    I.dontSeeInField("#DTE_Field_projectDeveloper", "Developer SK");
    I.dontSeeInField("#DTE_Field_projectGenerator", "Generator SK");
    I.seeInField("#DTE_Field_projectDeveloper", "Developer CZ");
    I.seeInField("#DTE_Field_projectGenerator", "Generator CZ");

    DTE.cancel();

    testPageSource(I);
});

Scenario('odhlasenie', ({ I }) => {
    I.logout();
});

function checkMetadataAuthor(author, I, DTE) {
    I.amOnPage("/admin/v9/templates/temps-groups-list");
    I.click("Demo JET");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-metadata-tab");
    I.fillField("#DTE_Field_projectAuthor", author);

    DTE.save();

    I.amOnPage("/?NO_WJTOOLBAR=true");
    I.seeInSource('<meta name="author" content="'+author+'"');

    //check thymeleaf version
    I.amOnPage("/uvodna-stranka-thymeleaf.html");
    if (author=="") {
        //in thymeleaf we use data-th-id to hide empty values
        I.dontSeeInSource('<meta name="author"');
    } else {
        I.seeInSource('<meta name="author" content="'+author+'"');
    }

}

Scenario('set empty value', ({ I, DTE }) => {

    //BUG: set empty value
    checkMetadataAuthor("", I, DTE);

    //set standard value
    checkMetadataAuthor("InterWay", I, DTE);

});