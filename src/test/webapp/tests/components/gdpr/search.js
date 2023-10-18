Feature('components.gdpr.search');

Before(({ I, login }) => {
    login('admin');
});

Scenario('test vyhladavania', ({I, DT}) => {
    I.amOnPage("/apps/gdpr/admin/search/");
    DT.waitForLoader("searchDataTable");

    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.fillField({css: "input.dt-filter-value"}, "test");
    I.pressKey('Enter', "input.dt-filter-key");
    DT.waitForLoader("searchDataTable");

    DT.filterSelect("modul", "Formuláre");
    DT.waitForLoader("searchDataTable");

    DT.filter("url", "321");
    DT.waitForLoader("searchDataTable");

    I.see("test-form");
});
