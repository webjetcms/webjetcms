Feature('apps.response-header');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }

    I.amOnPage("/apps/response-header/admin/");
});

Scenario('zakladne testy', async ({I, DataTables}) => {
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

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("responseHeadersDataTable");

    DTE.save();

    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    I.click("#DTE_Field_url");
    I.fillField("#DTE_Field_url", "/test");

    I.click("#DTE_Field_headerName");
    I.fillField("#DTE_Field_headerName", "TestHeader_" + randomNumber);

    I.click("#DTE_Field_headerValue");
    I.fillField("#DTE_Field_headerValue", "Test_value");

    DTE.save();

    DT.filter("headerName", "TestHeader_" + randomNumber);
    I.click("/test");
    DTE.waitForEditor("responseHeadersDataTable");

    I.see("/test");
    I.see("TestHeader_" + randomNumber);
    I.see("Test_value");

    I.click("#DTE_Field_headerName");
    I.fillField("#DTE_Field_headerName", "TestHeader_" + randomNumber + "_changed");

    DTE.save();

    DT.filter("headerName", "TestHeader_" + randomNumber + "_changed");

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
    I.waitForText("content-language", "#response");
    I.see("content-language: sk-SK");
    I.see("x-frame-options: SAMEORIGIN");
    I.see("x-webjet-header: root-folder");
    I.dontSee("x-frame-options: sub-folder");
    I.dontSee("x-webjet-header: sub-folder");
    I.see("x-macro-test: aceintegration-demotest.webjetcms.sk");
    //^/apps/http-hlavicky/$
    I.see("x-webjet-equals: true");
    // /apps/http-hlavicky/*.html
    I.dontSee("x-webjet-suffix");

    I.amOnPage("/apps/http-hlavicky/podpriecinok/?NO_WJTOOLBAR=true");
    I.waitForText("content-language", "#response");
    I.dontSee("x-frame-options: SAMEORIGIN");
    I.dontSee("x-webjet-header: root-folder");
    I.see("x-frame-options: sub-folder");
    I.see("x-webjet-header: sub-folder");
    I.see("x-macro-test: aceintegration-demotest.webjetcms.sk");
    //^/apps/http-hlavicky/$
    I.dontSee("x-webjet-equals");
    // /apps/http-hlavicky/*.html
    I.dontSee("x-webjet-suffix");
    //xRobotsTag conf. value - NOT_SEARCHABLE_PAGE
    I.see("x-robots-tag: noindex, nofollow");

    I.amOnPage("/apps/http-hlavicky/podpriecinok/podstranka.html?NO_WJTOOLBAR=true");
    I.waitForText("content-language", "#response");
    //
    I.say("owerwrite template value by response-header app");
    I.see("content-language: cs-CZ");
    I.dontSee("x-frame-options: SAMEORIGIN");
    I.dontSee("x-webjet-header: root-folder");
    I.see("x-frame-options: sub-folder");
    I.see("x-webjet-header: sub-folder");
    I.see("x-macro-test: aceintegration-demotest.webjetcms.sk");
    //^/apps/http-hlavicky/$
    I.dontSee("x-webjet-equals");
    // /apps/http-hlavicky/*.html
    I.see("x-webjet-suffix: true");
    //custom xRobotsTag value
    I.dontSee("x-robots-tag: noindex, nofollow");
    I.see("x-robots-tag: index, follow");

    I.amOnPage("/apps/http-hlavicky/rest-volanie.html");
    I.waitForText("content-language", "#response");
    I.see("x-webjet-header: rest");

    //
    I.say("Test autoset from files folder");
    I.amOnPage("/files/en/http-headers.html");
    I.waitForText("content-language", "#response");
    I.see("content-language: en-GB");

    I.amOnPage("/files/cz/http-headers.html");
    I.waitForText("content-language", "#response");
    I.see("content-language: cs-CZ");
});