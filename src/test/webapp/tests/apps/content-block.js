Feature('apps.content_block');

Scenario("Content block - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/content-block/");
    I.waitForElement(locate("h2").withText(("Najlepší elektromobil")), 10);
    I.seeElement({ css: 'div.blueBox div.container img[src="/images/gallery/test-vela-foto/dsc04089.jpeg"]' });
});