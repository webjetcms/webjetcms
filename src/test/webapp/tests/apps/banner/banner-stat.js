Feature('apps.banner-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('kontrola banner-stat a banner-detail', ({I, DTE}) => {
    I.amOnPage("/apps/banner/admin/banner-stat/");

    I.click("div.md-breadcrumb input.dt-filter-from-dayDate");
    I.fillField("div.md-breadcrumb input.dt-filter-from-dayDate", "01.05.2022");
    I.click("#bannerStatDataTable_extfilter > div > div.col-auto.dt-extfilter.dt-extfilter-dayDate > form > div > button");

    I.wait(2);

    //Values
    I.see("Obsahový banner");
    I.see("Obrázkový banner");
    I.see("HTML Banner");

    //Graphs names
    I.see("Top štatistika videní bannerov");
    I.see("Top štatistika kliknutí bannerov");

    //Check link to another page
    I.click("Obsahový banner");
    I.wait(2);

    I.see("Obsahový banner");
    I.see("Štatistika videní bannera");
    I.see("Štatistika kliknutí na banner");
})