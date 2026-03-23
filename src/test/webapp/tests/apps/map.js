Feature('apps.map');

Scenario("Map - test zobrazovania", async ({ I, Document }) => {
    I.amOnPage("/apps/map/");
    I.waitForElement("#map1");
    I.seeElement(locate("h1").withText(("Map")));
    I.waitForElement(".leaflet-control-zoom-in", 10);
    I.waitForElement(".leaflet-control-zoom-out", 10);
    I.wait(2);

   await Document.compareScreenshotElement("#map1", "autotest-map0.png", 1280, 760, 20);
});

Scenario("Map - test editor", async ({ I, login, Apps }) => {
    login('admin');

    Apps.openAppEditor(59889);

    switchToMapIframe(I);
    await checkMap(I, '1220px', '123px');
    await checkMapPin(I, true, '<b>WebJET CMS Headquarter</b><br>Zem. šírka:: 48.1454687<br>Zem. dĺžka:: 17.1549734');

    I.say('Change MAP settings and test map again');
    switchBackToEditor(I);
    I.clickCss("#pills-dt-component-datatable-mapSettings-tab");
    I.uncheckOption("#DTE_Field_sizeInPercent_0");
    I.checkOption("#DTE_Field_showControls_0");
    I.clickCss("#pills-dt-component-datatable-mapSettings button.btn-primary");

    switchToMapIframe(I);
    await checkMap(I, '400px', '400px', true);

    I.say('Change map PIN settings and test map again');
    switchBackToEditor(I);
    I.clickCss("#pills-dt-component-datatable-pinSettings-tab");
    I.uncheckOption("#DTE_Field_labelAddress_0");
    I.fillField("#DTE_Field_label", "This is custom label");
    I.clickCss("#pills-dt-component-datatable-pinSettings button.btn-primary");

    switchToMapIframe(I);
    await checkMapPin(I, true, "<b>This is custom label</b>");
});

function switchToMapIframe(I) {
    I.seeElement("iframe#previewIframe");
    I.switchTo("#previewIframe")
    I.seeElement("div#map");
}

function switchBackToEditor(I) {
    I.switchTo();
    I.switchTo('.cke_dialog_ui_iframe');
    I.wait(1);
    I.switchTo('#editorComponent');
}

async function checkMap(I, widthText, heightText, seeControls = false) {
    I.say('Check map size');
    const size = await I.executeScript(() => {
        const el = document.querySelector('#map');
        const style = window.getComputedStyle(el);
        return {
            width: style.width,
            height: style.height
        };
    });
    I.assertEqual(size.width, widthText);
    I.assertEqual(size.height, heightText);

    if(seeControls === true) {
        I.seeElement("#map .leaflet-control-zoom a.leaflet-control-zoom-in");
        I.seeElement("#map .leaflet-control-zoom a.leaflet-control-zoom-out");
    } else {
        I.dontSeeElement("#map .leaflet-control-zoom a.leaflet-control-zoom-in");
        I.dontSeeElement("#map .leaflet-control-zoom a.leaflet-control-zoom-out");
    }
}

async function checkMapPin(I, isPinShowed, pinContext = null) {
    I.say("Check pin label");

    if(isPinShowed === true) {
        I.seeElement("#map .leaflet-popup-content");

        if(pinContext !== null) {
            const html = await I.grabHTMLFrom('#map .leaflet-popup-content');
            I.assertEqual(html, pinContext);
        }

    } else {
        I.dontSeeElement("#map .leaflet-popup-content");
    }
}