Feature('components.template-groups');

var seoGroupName;
var seoDescriptionSk;
var seoDescriptionCz;
var seoImage;
var seoImageAltSk;
var seoImageAltCz;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/templates/temps-groups-list/");

    if (typeof seoGroupName === "undefined") {
        const randomText = I.getRandomText();
        seoGroupName = `seo-defaults-autotest-${randomText}`;
        seoDescriptionSk = `seo-description-sk-autotest-${randomText}`;
        seoDescriptionCz = `seo-description-cz-autotest-${randomText}`;
        seoImage = `/images/gallery/seo-default-autotest-${randomText}.png`;
        seoImageAltSk = `seo-image-alt-sk-autotest-${randomText}`;
        seoImageAltCz = `seo-image-alt-cz-autotest-${randomText}`;
    }
});

function openSeoTab(I) {
    I.clickCss("#pills-dt-datatableInit-seo-tab");
}

function switchBreadcrumbLanguage(language, I, DT) {
    I.click({css: "div.breadcrumb-language-select"});
    I.click(locate('.dropdown-item').withText(language));
    DT.waitForLoader();
}

Scenario('template-groups-zakladne testy @baseTest', async ({ I, DataTables }) => {
    I.see("Názov skupiny");
    await DataTables.baseTest({
        dataTable: 'tempsGroupsListTable',
        requiredFields: ['name'],
        perms: 'menuTemplatesGroup',
        skipSwitchDomain: true
    });
});

Scenario('lokalizacia a persistencia SEO predvolenych hodnot', ({ I, DT, DTE }) => {
    I.clickCss("#datatableInit_wrapper button.buttons-create");
    DTE.waitForEditor();
    DTE.fillField("name", seoGroupName);

    openSeoTab(I);
    I.see("Predvolený SEO popis", "div.DTE_Field_Name_description");
    I.see("Predvolený SEO obrázok", "div.DTE_Field_Name_seoImage");
    I.see("Predvolený alternatívny text SEO obrázka", "div.DTE_Field_Name_seoImageAlt");
    DTE.fillField("description", seoDescriptionSk);
    I.fillField(locate(".DTE_Field_Name_seoImage").find("input.form-control"), seoImage);
    DTE.fillField("seoImageAlt", seoImageAltSk);
    DTE.save();

    DT.filterContains("name", seoGroupName);
    I.click(seoGroupName);
    DTE.waitForEditor();
    openSeoTab(I);
    I.seeInField("#DTE_Field_description", seoDescriptionSk);
    I.seeInField(locate(".DTE_Field_Name_seoImage").find("input.form-control"), seoImage);
    I.seeInField("#DTE_Field_seoImageAlt", seoImageAltSk);
    DTE.cancel();

    switchBreadcrumbLanguage("Český jazyk", I, DT);
    DT.filterContains("name", seoGroupName);
    I.click(seoGroupName);
    DTE.waitForEditor();
    openSeoTab(I);
    I.seeElement("div.DTE_Field_Name_description");
    I.dontSeeInField("#DTE_Field_description", seoDescriptionSk);
    I.seeInField(locate(".DTE_Field_Name_seoImage").find("input.form-control"), seoImage);
    I.dontSeeInField("#DTE_Field_seoImageAlt", seoImageAltSk);
    DTE.fillField("description", seoDescriptionCz);
    DTE.fillField("seoImageAlt", seoImageAltCz);
    DTE.save();

    I.click(seoGroupName);
    DTE.waitForEditor();
    openSeoTab(I);
    I.seeInField("#DTE_Field_description", seoDescriptionCz);
    I.seeInField(locate(".DTE_Field_Name_seoImage").find("input.form-control"), seoImage);
    I.seeInField("#DTE_Field_seoImageAlt", seoImageAltCz);
    DTE.cancel();

    switchBreadcrumbLanguage("Slovenský jazyk", I, DT);
    DT.filterContains("name", seoGroupName);
    I.click(seoGroupName);
    DTE.waitForEditor();
    openSeoTab(I);
    I.seeInField("#DTE_Field_description", seoDescriptionSk);
    I.seeInField(locate(".DTE_Field_Name_seoImage").find("input.form-control"), seoImage);
    I.seeInField("#DTE_Field_seoImageAlt", seoImageAltSk);
    DTE.cancel();
});

Scenario('zmazanie SEO testovacej skupiny', async ({ I, DT }) => {
    DT.filterContains("name", seoGroupName);
    const visibleRows = await I.grabNumberOfVisibleElements(locate("#datatableInit_wrapper tbody tr").withText(seoGroupName));
    if (visibleRows > 0) {
        DT.deleteAll();
        I.dontSee(seoGroupName, "#datatableInit_wrapper");
    }
});

Scenario('ukladanie metadat', ({ I, DT, DTE }) => {
    I.click("Demo JET");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-metadata-tab");
    I.seeInField("#DTE_Field_projectDeveloper", "InterWay Developer SK");
    I.seeInField("#DTE_Field_projectGenerator", "WebJET CMS Generator SK");

    DTE.cancel();

    //prepni jazyk na en
    I.click("Slovenský jazyk", "div.breadcrumb-language-select");
    I.clickCss("#bs-select-2-2");
    DT.waitForLoader();

    I.click("Demo JET");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-metadata-tab");
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
    I.fillField("password", secret(I.getDefaultPassword()));
    I.forceClick("Přihlásit se");

    I.amOnPage("/admin/v9/templates/temps-groups-list/");

    I.click("Demo JET");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-metadata-tab");
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
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    I.click("Demo JET");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-metadata-tab");
    I.fillField("#DTE_Field_projectAuthor", author);

    DTE.save();

    let v = (new Date()).getTime();
    I.amOnPage("/?v="+v);
    I.seeInSource('<meta name="author" content="'+author+'"');

    //check thymeleaf version
    I.amOnPage("/uvodna-stranka-thymeleaf.html?v="+v);
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
