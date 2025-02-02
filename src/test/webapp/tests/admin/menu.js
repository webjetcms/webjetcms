Feature('admin.menu');

Before(({ login }) => {
    login('admin');
});

Scenario('menu items highlighting', ({I}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.waitForElement(locate(".md-main-menu__item.md-main-menu__item--active.md-main-menu__item--open").withText("Zoznam web stránok"), 10);

    I.amOnPage("/components/aceintegration/admin-example.jsp#education-dashboard");
    I.waitForElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Dashboard"), 10);

    I.click("Courses", ".md-main-menu__item--open");
    I.waitForElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Courses"), 10);
    I.dontSeeElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Dashboard"));

    I.click("Overview top", ".md-main-menu__item--open");
    I.waitForElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Overview top"), 10);
    I.dontSeeElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Overview bottom"));

    I.click("Overview bottom", ".md-main-menu__item--open");
    I.waitForElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Overview bottom"), 10);
    I.dontSeeElement(locate(".md-main-menu__item__sub-menu__item--active a").withText("Overview top"));
});