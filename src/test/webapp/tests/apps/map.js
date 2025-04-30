Feature('apps.map');

Scenario("Map - test zobrazovania", async ({ I, Document }) => {
    I.amOnPage("/apps/map/");
    I.waitForElement("#map1");
    I.seeElement(locate("h1").withText(("Map")));
    I.waitForElement(".leaflet-control-zoom-in", 10);
    I.waitForElement(".leaflet-control-zoom-out", 10);
    I.wait(2);
    //await Document.compareScreenshotElement("#map1", "autotest-map0.png", 1280, 760, 20);
});