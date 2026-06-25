Feature('apps.response-header');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }

    I.amOnPage("/apps/response-header/admin/");
});

Scenario('zakladne testy @baseTest', async ({I, DataTables}) => {
    await DataTables.baseTest({
        dataTable: 'responseHeadersDataTable',
        perms: 'cmp_response-header',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('zakladne testy-manual', ({I, DT, DTE}) => {

    var cspLongHeader = "Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://code.jquery.com https://stackpath.bootstrapcdn.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; connect-src 'self' https://api.example.com; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'; upgrade-insecure-requests; block-all-mixed-content;";

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("responseHeadersDataTable");

    DTE.save();

    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    I.clickCss("#DTE_Field_url");
    I.fillField("#DTE_Field_url", "/test");

    I.clickCss("#DTE_Field_headerName");
    I.fillField("#DTE_Field_headerName", "TestHeader_" + randomNumber);

    I.clickCss("#DTE_Field_headerValue");
    I.fillField("#DTE_Field_headerValue", cspLongHeader);

    DTE.save();

    DT.filterContains("headerName", "TestHeader_" + randomNumber);
    I.click("/test");
    DTE.waitForEditor("responseHeadersDataTable");

    I.see("/test");
    I.see("TestHeader_" + randomNumber);
    I.see(cspLongHeader);

    I.clickCss("#DTE_Field_headerName");
    I.fillField("#DTE_Field_headerName", "TestHeader_" + randomNumber + "_changed");

    DTE.save();

    DT.filterContains("headerName", "TestHeader_" + randomNumber + "_changed");

    I.see("TestHeader_" + randomNumber + "_changed");
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

/**
<p>Zoznam HTTP response hlavičiek:</p>

<pre id="response">
headers</pre>

<script>
    $(document).ready(function() {
    var req = new XMLHttpRequest();
    req.open('GET', document.location, false);
    req.send(null);
    console.log("headers: ", req.getAllResponseHeaders());
    $("#response").text(req.getAllResponseHeaders());
    });
</script>
 */

Scenario('test-odpovede', ({I, DT, DTE}) => {
    I.amOnPage("/apps/http-hlavicky/?NO_WJTOOLBAR=true");
    I.waitForText("content-language", 10, "#response");
    I.see("content-language: sk-SK");
    I.see("x-frame-options: SAMEORIGIN");
    I.see("x-webjet-header: root-folder");
    I.dontSee("x-frame-options: sub-folder");
    I.dontSee("x-webjet-header: sub-folder");
    I.see("x-macro-test: aceintegration-"+I.getDefaultDomainName());
    //^/apps/http-hlavicky/$
    I.see("x-webjet-equals: true");
    // /apps/http-hlavicky/*.html
    I.dontSee("x-webjet-suffix");

    I.amOnPage("/apps/http-hlavicky/podpriecinok/?NO_WJTOOLBAR=true");
    I.waitForText("content-language", 10, "#response");
    I.dontSee("x-frame-options: SAMEORIGIN");
    I.dontSee("x-webjet-header: root-folder");
    I.see("x-frame-options: sub-folder");
    I.see("x-webjet-header: sub-folder");
    I.see("x-macro-test: aceintegration-"+I.getDefaultDomainName());
    //^/apps/http-hlavicky/$
    I.dontSee("x-webjet-equals");
    // /apps/http-hlavicky/*.html
    I.dontSee("x-webjet-suffix");
    //xRobotsTag conf. value - NOT_SEARCHABLE_PAGE
    I.see("x-robots-tag: noindex, nofollow");

    I.amOnPage("/apps/http-hlavicky/podpriecinok/podstranka.html?NO_WJTOOLBAR=true");
    I.waitForText("content-language", 10, "#response");
    //
    I.say("owerwrite template value by response-header app");
    I.see("content-language: cs-CZ");
    I.dontSee("x-frame-options: SAMEORIGIN");
    I.dontSee("x-webjet-header: root-folder");
    I.see("x-frame-options: sub-folder");
    I.see("x-webjet-header: sub-folder");
    I.see("x-macro-test: aceintegration-"+I.getDefaultDomainName());
    //test header with new line, it should be replaced by space
    I.see("x-multiline-test: aceintegration "+I.getDefaultDomainName());
    //^/apps/http-hlavicky/$
    I.dontSee("x-webjet-equals");
    // /apps/http-hlavicky/*.html
    I.see("x-webjet-suffix: true");
    //custom xRobotsTag value
    I.dontSee("x-robots-tag: noindex, nofollow");
    I.see("x-robots-tag: all");

    I.amOnPage("/apps/http-hlavicky/rest-volanie.html");
    I.waitForText("content-language", 10, "#response");
    I.see("x-webjet-header: rest");

    //
    I.say("Test autoset from files folder");
    I.amOnPage("/files/en/http-headers.html");
    I.waitForText("content-language", 10, "#response");
    I.see("content-language: en-GB");

    I.amOnPage("/files/cz/http-headers.html");
    I.waitForText("content-language", 10, "#response");
    I.see("content-language: cs-CZ");
});

/* meta robots CHECK */

const robotsTestDocId = 166410;
const robotsTestPage = "/apps/http-hlavicky/index_folow_behaviour.html";
const followLinksOptions = {
    searchable: "Podľa nastavenia Prehľadávať",
    follow: "Povoliť sledovanie odkazov",
    nofollow: "Zakázať sledovanie odkazov"
};

async function setRobotsSettings(I, DTE, searchable, followLinks) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + robotsTestDocId);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.waitForVisible("#DTE_Field_searchable_0", 10);

    if(searchable === true) {
        I.checkOption('#DTE_Field_searchable_0');
    } else {
        I.uncheckOption('#DTE_Field_searchable_0');
    }

    DTE.selectOption("followLinks", followLinks);
    DTE.save();
}

async function checkRobotsHeader(I, expected) {
    I.amOnPage(robotsTestPage + "?NO_WJTOOLBAR=true");
    I.waitForText("index_folow_behaviour", 10);

    const robots = await I.grabAttributeFrom(
        'meta[name="robots"]',
        'content'
    );

    I.assertEqual(robots, expected, "Expected meta robots do not match real value;");
}

Scenario("test meta robots by page settings", async ({I, DTE}) => {
    const settings = [
        { searchable: false, followLinks: followLinksOptions.searchable, expected: "noindex, nofollow" },
        { searchable: false, followLinks: followLinksOptions.follow, expected: "noindex" },
        { searchable: false, followLinks: followLinksOptions.nofollow, expected: "noindex, nofollow" },
        { searchable: true, followLinks: followLinksOptions.nofollow, expected: "nofollow" },
        { searchable: true, followLinks: followLinksOptions.follow, expected: "all" },
        { searchable: true, followLinks: followLinksOptions.searchable, expected: "all" }
    ];

    for (const setting of settings) {
        I.say("Searchable: " + setting.searchable + ", follow links: " + setting.followLinks);
        await setRobotsSettings(I, DTE, setting.searchable, setting.followLinks);
        await checkRobotsHeader(I, setting.expected);
    }
});

Scenario("test SEO image dimensions", async ({I}) => {
    I.amOnPage("/showdoc.do?docid=141");

    const widthA = await I.grabAttributeFrom('meta[property="og:image:width"]', "content");
    const heightA = await I.grabAttributeFrom('meta[property="og:image:height"]', "content");

    I.assertEqual(widthA, "265");
    I.assertEqual(heightA, "225");

    I.amOnPage("/showdoc.do?docid=92");

    const widthB = await I.grabAttributeFrom('meta[property="og:image:width"]', "content");
    const heightB = await I.grabAttributeFrom('meta[property="og:image:height"]', "content");

    I.assertEqual(widthB, "0");
    I.assertEqual(heightB, "0");
});
