Feature('apps.banner-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('kontrola banner-stat a banner-detail', ({I, DT}) => {
    I.amOnPage("/apps/banner/admin/banner-stat/");

    I.click("div.md-breadcrumb input.dt-filter-from-dayDate");
    I.fillField("div.md-breadcrumb input.dt-filter-from-dayDate", "01.05.2022");
    I.clickCss("#bannerStatDataTable_extfilter > div > div.col-auto.dt-extfilter.dt-extfilter-dayDate > form > div > button");
    DT.waitForLoader();

    //Values
    I.see("Obsahový banner");
    I.see("Obrázkový banner");
    I.see("HTML Banner");

    //Graphs names
    I.see("Top štatistika videní bannerov");
    I.see("Top štatistika kliknutí bannerov");

    //Check link to another page
    I.forceClick(locate("#bannerStatDataTable td div.datatable-column-width a").withText("Obsahový banner"));

    I.waitForText("Obsahový banner", 10, "#pills-banner-stat-detail");
    I.waitForText("Štatistika videní bannera", 10);
    I.see("Štatistika kliknutí na banner");
})