Feature('apps.map');

Scenario("Map - test zobrazovania", ({ I, Document }) => {
    I.amOnPage("/apps/map/");
    I.waitForElement("#map1");
    I.seeElement(locate("h1").withText(("Map")));
    I.seeElement(".leaflet-control-zoom-in");
    I.seeElement(".leaflet-control-zoom-out");
    Document.compareScreenshotElement("#map1", "autotest-map0.png", 1280, 760);
});