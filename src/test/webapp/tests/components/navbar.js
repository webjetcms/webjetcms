Feature('components.navbar');

Before(({ I, login }) => {
    login('admin');
});

Scenario('zobrazenie navbaru', ({ I }) => {
    I.amOnPage("/apps/navbar/podadresar-1/stranka-zobrazi-navigacnej-liste.html");
    I.see("Stránka sa zobrazí v navigačnej lište", "div.navbartest");

    I.amOnPage("/apps/navbar/podadresar-1/stranka-nezobrazi-navigacnej-liste.html");
    I.dontSee("Stránka sa nezobrazí v navigačnej lište", "div.navbartest");

    I.amOnPage("/apps/navbar/podadresar-1/stranka-zobrazi-navigacnej-liste-1.html");
    I.see("Stránka sa zobrazí v navigačnej lište podľa menu", "div.navbartest");

    I.amOnPage("/apps/navbar/podadresar-1/stranka-zobrazi-navigacnej-liste-1-1.html");
    I.dontSee("Stránka sa nezobrazí v navigačnej lište podľa menu", "div.navbartest");
});

function verifyNavbarHtml(unescapedName, I) {
    I.amOnPage("/test-stavov/testhtml-nazve/html-subpage.html");

    //
    I.see("subpage".toUpperCase(), "div.container h1 sub");

    //
    I.waitForElement("div.navbar-test", 10);
    I.dontSee(unescapedName, "div.navbar-test");

    //folder
    I.see("HTML", "div.navbar-test a sub");

    //last page
    I.see("subpage", "div.navbar-test sub");
}

Scenario('zobrazenie navbaru - HTML escaping', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");

    var unescapedName = "Test<sub>HTML</sub> v názve";

    //
    I.say("Verify HTML is escaped in jstree");
    I.jstreeWaitForLoader();
    I.waitForElement(locate("a.jstree-anchor").withText(unescapedName), 10);
    I.jstreeClick(unescapedName);
    I.click(".tree-col .dt-buttons button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.waitForElement("#DTE_Field_groupName", 10);
    I.seeInField("#DTE_Field_groupName", unescapedName);
    I.seeInField("#DTE_Field_navbarName", unescapedName);
    I.seeInField("#DTE_Field_urlDirName", "testhtml-nazve");
    I.wait(2);
    DTE.save("groups-datatable");
    DTE.waitForModalClose("groups-datatable_modal");

    //
    I.say("Verify HTML escape in webpabe");
    var subpageName = "HTML <sub>subpage</sub>";
    I.click(locate("#datatableInit td.dt-row-edit a").withText(subpageName));
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_title", subpageName);
    I.seeInField("#DTE_Field_navbar", subpageName);
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/testhtml-nazve/html-subpage.html");
    I.wait(2);
    DTE.save();
    DTE.waitForModalClose();

    //
    I.say("Verify HTML is NOT escaped in navbar");
    Document.setConfigValue("navbarDefaultType", "normal");
    verifyNavbarHtml(unescapedName, I);
    I.seeInSource("Test stavov</a> &gt; <a href=\"/test-stavov/testhtml-nazve/\">"+unescapedName+"</a> &gt; HTML <sub>subpage</sub>");

    Document.setConfigValue("navbarDefaultType", "rdf");
    verifyNavbarHtml(unescapedName, I);

    Document.setConfigValue("navbarDefaultType", "schema.org");
    verifyNavbarHtml(unescapedName, I);

    //
    I.say("Verify HTML is NOT escaped in menu");
    I.dontSee(unescapedName, "#menu-test");
    I.see("HTML", "#menu-test li a sub");
    I.seeInSource("<li class=\"open\"><a href=\"/test-stavov/testhtml-nazve/\">"+unescapedName+"</a></li>");
});

Scenario('custom navbar @current', ({ I, DTE, Document }) => {
    Document.setConfigValue("navbarDefaultType", "sk.iway.aceintegration.CustomNavbar");

    I.amOnPage("/test-stavov/testhtml-nazve/html-subpage.html");
    I.waitForText("Custom navbar for group 97708 doc 131101, Custom navbar for non-default doc 131101", 10, "div.navbar-test");

    Document.setConfigValue("navbarDefaultType", "normal");
});

Scenario('zobrazenie navbaru - HTML escaping - reset', ({ I, DTE, Document }) => {
    Document.setConfigValue("navbarDefaultType", "normal");
});