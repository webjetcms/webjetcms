Feature('webpages.group-showin');

Before(({ I, login }) => {
    login('admin');
});

function setCheckValueRecursive(I, DTE, folder, subfolder, fieldName, value, forceFieldName) {
    I.say("---->setCheckValueRecursive, folder="+folder+" subfolder="+subfolder+" fieldName="+fieldName+" value="+value+" forceFieldName="+forceFieldName)

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(folder);

    //nastav hodnotu a zaskrtni force subgroup
    I.click("div.tree-col div.dt-buttons button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-menu-tab");
    DTE.selectOption(fieldName, value);
    I.wait(1);
    I.see(value, "div.DTE_Field_Name_"+fieldName+" button.dropdown-toggle div.filter-option");
    I.clickCss("#DTE_Field_editorFields-"+forceFieldName+"_0");
    I.wait(1);
    DTE.save();

    //over v adresari
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(folder);
    I.click("div.tree-col div.dt-buttons button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-menu-tab");
    I.see(value, "div.DTE_Field_Name_"+fieldName+" button.dropdown-toggle div.filter-option");
    DTE.cancel();

    //over v subgroup
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(folder);
    I.jstreeClick(subfolder);
    I.click("div.tree-col div.dt-buttons button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-menu-tab");
    I.see(value, "div.DTE_Field_Name_"+fieldName+" button.dropdown-toggle div.filter-option");
    DTE.cancel();
}

Scenario('editor-nastavenie navbaru', ({ I, DTE }) => {
    setCheckValueRecursive(I, DTE, ["Aplikácie", "Navbar", "Podadresár 2"], "Pod-podadresár 1", "showInNavbar", "Zobraziť", "forceNavbarSubfolders");
    setCheckValueRecursive(I, DTE, ["Aplikácie", "Navbar", "Podadresár 2"], "Pod-podadresár 1", "showInNavbar", "Rovnako ako menu", "forceNavbarSubfolders");

    setCheckValueRecursive(I, DTE, ["Aplikácie", "Navbar", "Podadresár 2"], "Pod-podadresár 1", "loggedShowInNavbar", "Nezobraziť", "forceLoggedNavbarSubfolders");
    setCheckValueRecursive(I, DTE, ["Aplikácie", "Navbar", "Podadresár 2"], "Pod-podadresár 1", "loggedShowInNavbar", "Rovnako ako pre neprihláseného", "forceLoggedNavbarSubfolders");
});

Scenario('editor-nastavenie sitemap', ({ I, DTE }) => {
    setCheckValueRecursive(I, DTE, ["Aplikácie", "Mapa stránok", "Podadresár 2"], "Pod-podadresár 1", "showInSitemap", "Zobraziť bez podpriečinkov", "forceSitemapSubfolders");
    setCheckValueRecursive(I, DTE, ["Aplikácie", "Mapa stránok", "Podadresár 2"], "Pod-podadresár 1", "showInSitemap", "Rovnako ako menu", "forceSitemapSubfolders");

    setCheckValueRecursive(I, DTE, ["Aplikácie", "Mapa stránok", "Podadresár 2"], "Pod-podadresár 1", "loggedShowInSitemap", "Zobraziť vrátane web stránok", "forceLoggedSitemapSubfolders");
    setCheckValueRecursive(I, DTE, ["Aplikácie", "Mapa stránok", "Podadresár 2"], "Pod-podadresár 1", "loggedShowInSitemap", "Rovnako ako pre neprihláseného", "forceLoggedSitemapSubfolders");
});

//TODO skontrolovat casove publikovanie
