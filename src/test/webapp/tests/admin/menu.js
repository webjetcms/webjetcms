Feature('admin.menu');

Before(({ login }) => {
    login('admin');
});

Scenario('menu items highlighting v8', ({I}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.waitForElement(locate(".md-main-menu__item.md-main-menu__item--active .md-main-menu__item__link").withText("Webové stránky"), 10);
    I.amOnPage("/components/aceintegration/admin-example.jsp#education-dashboard");
    I.waitForElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Dashboard"), 10);

    I.click("Courses", ".ly-submenu .md-tabs");
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Courses"), 10);
    I.dontSeeElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Dashboard"));

    I.click("Overview top", ".ly-submenu .md-tabs");
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Overview top"), 10);
    I.dontSeeElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Overview bottom"));

    I.click(locate('.md-main-menu__item__link').withText('Menu HASH test'));
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link").withText("Overview bottom"));
    I.click("Overview bottom", ".ly-submenu .md-tabs");
    I.waitForElement(locate(".nav-link.active").withText("Overview bottom"), 10);
    I.dontSeeElement(locate(".nav-link.active").withText("Overview top"));

    I.seeInCurrentUrl('overview/#overview-dashboard-b');
    I.click(locate("a.nav-link").withText("Overview top"));
    I.seeInCurrentUrl('overview/#overview-dashboard-a');
    //I.refreshPage();
    I.waitForElement(locate(".nav-link.active").withText("Overview top"));
    I.dontSeeElement(locate(".nav-link.active").withText("Overview bottom"));
});

Scenario('menu items highlighting audit', ({I}) => {
    I.amOnPage("/admin/v9/apps/audit-search/");
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Vyhľadávanie"), 10);

    I.click("Úrovne logovania", ".ly-submenu .md-tabs");
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Úrovne logovania"), 10);
    I.seeInCurrentUrl('/admin/v9/apps/audit-log-levels/');
});

Scenario('menu items highlighting webpages', ({I}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.waitForElement(locate(".md-main-menu__item.md-main-menu__item--active .md-main-menu__item__link").withText("Webové stránky"), 10);
    I.seeElement("div.tree-col");

    I.click("Naposledy upravené", ".ly-submenu .md-tabs");
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Naposledy upravené"), 10);
    I.dontSeeElement("div.tree-col");

    I.click("Systémové", ".ly-submenu .md-tabs");
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Systémové"), 10);
    I.waitForText("System", 10, "#datatableInit td.dt-row-edit");
    I.seeElement("div.tree-col");

    I.click("Aktívne", ".ly-submenu .md-tabs");
    I.waitForElement(locate(".ly-submenu .md-tabs a.nav-link.active").withText("Aktívne"), 10);
    I.waitForText("Jet portal 4 - testovacia stranka", 10, "#datatableInit td.dt-row-edit");
    I.seeElement("div.tree-col");
});