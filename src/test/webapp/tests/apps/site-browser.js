Feature('apps.site-browser');

var container = "div.site_browse";

function testRootFolder(I) {
    I.waitForText("Adresár: / Test stavov / Site browser", 10, container);
    I.see("Subfolder1", container);
    I.see("Subfolder2", container);
    I.see("download.png", container);
    I.see("pb-responsive.png", container);
}

Scenario('site-browser-webpage', ({I}) => {
    I.logout();

    I.amOnPage('/apps/site-browser/');
    testRootFolder(I);

    I.click("Subfolder1", container);
    I.waitForText("Adresár: / Test stavov / Site browser / Subfolder1", 10, container);
    I.dontSee("Subfolder2", container);
    I.dontSee("download.png", container);
    I.see("browser.png", container);

    I.amOnPage("/apps/site-browser/?actualDir=/files/test-stavov/site-browser/subfolder2");
    I.waitForText("Adresár: / Test stavov / Site browser / Subfolder2", 10, container);
    I.dontSee("Subfolder1", container);
    I.dontSee("download.png", container);
    I.see("autotest.png", container);

    I.click(locate(container+" a.siteBrowserClick").withText(".."));
    testRootFolder(I);

    I.say("Cant't go up from root folder");
    I.amOnPage("/apps/site-browser/?actualDir=/files/");
    testRootFolder(I);

    I.say("Cant't spoof URL");
    I.amOnPage('/apps/site-browser/?actualDir=/files/test-stavov/site-browser/../../');
    testRootFolder(I);
});