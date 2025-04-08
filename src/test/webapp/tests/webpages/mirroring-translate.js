Feature('webpages.mirroring-translate');

var randomText;

// SK original
var docDataSK = '<p class="text-center"><strong>Názov stránky</strong></p><p>Test prekladu stránky s&nbsp;rôznymi slovami.</p><ol>	<li>dnes</li>	<li>zajtra</li>	<li>pozajtra</li></ol>';
var docDataSKupdate = '<p> nový starý najstarší </p>';
var docDataSKupdate2 = '<p> pes mačka kokos </p>';

// Translated EN vesrion
var docDataEN = ["Page", "different words", "oday", "omorrow", "he day after tomorrow"];
var docDataENupdate = ["new", "old", "oldest"];
var docDataENupdate2 = ["dog", "cat", "coconut"];

Before(({ I, DT }) => {
    if (typeof randomText=="undefined") {
        randomText = I.getRandomTextShort();
    }

    DT.addContext('configuration', '#configurationDatatable_wrapper');
});

/**
 * caseA while CONF structureMirroringAutoTranslatorLogin IS SET
 */
Scenario('Translation use autotranslator', async ({I, DT, DTE}) => {
    await testTranslation(I, DT, DTE, true);
});

/**
 * caseB while CONF structureMirroringAutoTranslatorLogin IS NOT SET
 */
Scenario('Translation use available', async ({I, DT, DTE}) => {
    await testTranslation(I, DT, DTE, false);
});

async function testTranslation(I, DT, DTE, useAutotranslator) {

    var docTitleSK;
    var docTitleEN;

    if(useAutotranslator) {
        I.say("Versioon WITH autoTranslator");
        // Set
        await setConfValues(I, DT, DTE, "autotranslator");

        docTitleSK = "Dobré ráno - A - " + randomText;
        docTitleEN = "Good morning - A - " + randomText;

        I.amOnPage("/admin/v9/webpages/web-pages-list/");
        DT.waitForLoader();
        DT.resetTable();
    } else {
        I.say("Versioon WITHout autoTranslator");
        // Set
        await setConfValues(I, DT, DTE, "");

        docTitleSK = "Dobré ráno - B - " + randomText;
        docTitleEN = "Good morning - B - " + randomText;

        I.amOnPage("/admin/v9/webpages/web-pages-list/");
        DT.waitForLoader();
        DT.resetTable();
    }

    //Select DOMAIN
    setDomain(I);

    // CREATE NEW DOC IN SK VERSION
    I.say("Creating initial SK version");
    I.jstreeClick("preklad_sk");
    I.click(DT.btn.add_button);
    I.waitForVisible("#DTE_Field_title");
    I.fillField("#DTE_Field_title", docTitleSK);

    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(docDataSK);
    DTE.save();

    // CHECK AUTO CREATED EN VERSION OF DOCS
    I.say("Checking autocreated initial EN version");
    checkBodyEN(I, DT, DTE, docTitleEN, docDataEN);
    if (useAutotranslator) I.see("Auto Translator", "#datatableInit");

    // UPDATE SK VERSION
    I.say("Updating initial SK version");
    await updateBody(I, DT, DTE, "SK", docTitleSK, docDataSKupdate);

    // CHECK UPDATED TRANSLATED EN DOC
    // EN version is still UNavailable (available = false)
    I.say("Checking auto updated EN version");
    checkBodyEN(I, DT, DTE, docTitleEN, docDataENupdate);
    if (useAutotranslator) I.see("Auto Translator", "#datatableInit");

    // SET EN version as AVAILABLE (available = true)
    I.say("Setting EN version as AVAILABLE");
    if(useAutotranslator) {
        I.relogin("autotranslator");
        I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=56846");
    }
    setDocAsAvailableEN(I, DT, DTE, docTitleEN);
    if(useAutotranslator) {
        I.relogin("admin");
        I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=56846");
        I.see("Auto Translator", "#datatableInit");
    }

    // UPDATE SK VERSION
    I.say("Updating initial SK version 2nd TIME");
    await updateBody(I, DT, DTE, "SK", docTitleSK, docDataSKupdate2);

    // CHECK UPDATED TRANSLATED EN DOC
    // EN version is NOW (available = true)
    if(useAutotranslator) {
        I.say("Checking auto updated EN version 2nd time - BECAUSE autoTranslator can change even available doc's");
        //IN CASE that we are using autoTranslator (is set in CONF), this update should be perform even when EN version has available = true
        checkBodyEN(I, DT, DTE, docTitleEN, docDataENupdate2);
    } else {
        I.say("Checking NON updated EN version 2nd time - BECAUSE autoTranslator is not set and available is true");
        //IN CASE that we are NOT using autoTranslator (is NOT set in CONF), this update should NOT be perform, because EN version has available = true
        checkBodyEN(I, DT, DTE, docTitleEN, docDataENupdate); // we are checking old value, that was set before setting EN version as available
    }

    if (useAutotranslator) {
        var enText = "This is manual update";
        //
        I.say("edit EN doc with current user");
        await updateBody(I, DT, DTE, "EN", docTitleEN, enText);
        I.dontSee("Auto Translator", "#datatableInit");

        //resave SK version
        await updateBody(I, DT, DTE, "SK", docTitleSK, docDataSKupdate2);

        //check EN version is not updated
        checkBodyEN(I, DT, DTE, docTitleEN, [enText]);
        I.dontSee("Auto Translator", "#datatableInit");
    }

    //Delete this doc's
    await deleteDocWithCheck(I, DT, DTE, docTitleSK, docTitleEN);
}

async function deleteDocWithCheck(I, DT, DTE, docNameA, docNameB) {
    DT.waitForLoader();
    I.wait(0.5);
    I.jstreeClick("preklad_sk");
    DT.filterContains("title", docNameA);
    I.waitForText("Záznamy 1 až 1 z 1", 10, "#datatableInit_wrapper .dt-info");
    I.see(docNameA);
    I.wait(1);

    // Delete SK version
    I.clickCss("td.dt-select-td");
    I.click(DT.btn.delete_button);
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy");

    // Check that EN version i gone too
    I.jstreeClick("preklad_en");
    DT.filterContains("title", docNameB);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
}

function setDomain(I) {
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("mirroring.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");
}

async function setConfValues(I, DT, DTE, autotranslator) {
    I.relogin("admin");

    // Set needed configurations
    I.amOnPage("/admin/v9/settings/configuration/");

    //Set structureMirroringConfig
    I.click(DT.btn.configuration_add_button);
    DTE.waitForEditor("configurationDatatable");
    I.fillField("#DTE_Field_name", "structureMirroringConfig");
    I.fillField("#DTE_Field_value", "56845,56846:mirroring.tau27.iway.sk");
    DTE.save();
    //Check it
    DT.filterContains("name", "structureMirroringConfig");
    I.see("56845,56846:mirroring.tau27.iway.sk");

    //Set structureMirroringAutoTranslatorLogin
    I.click(DT.btn.configuration_add_button);
    DTE.waitForEditor("configurationDatatable");
    I.fillField("#DTE_Field_name", "structureMirroringAutoTranslatorLogin");
    I.fillField("#DTE_Field_value", autotranslator);
    DTE.save();
    //Check it
    DT.filterContains("name", "structureMirroringAutoTranslatorLogin");
    I.see(autotranslator);

    I.say("autoTranslator set as : ", autotranslator);
}

function setDocAsAvailableEN(I, DT, DTE, title) {
    openDoc(I, DT, DTE, title, "EN");

    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.checkOption("#DTE_Field_available_0");
    DTE.save();
}

async function updateBody(I, DT, DTE, lang, title, body) {
    openDoc(I, DT, DTE, title, lang);

    //Change doc body (data)
    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(body);
    DTE.save();
}

function checkBodyEN(I, DT, DTE, title, values, skipFirst2 = false) {
    openDoc(I, DT, DTE, title, "EN");

    var i = 0;
    if(skipFirst2) i = 2;

    //Check EN translated body
    I.switchTo("iframe.cke_wysiwyg_frame");
    for(; i < values.length; i++)
        I.see( values[i] );
    I.switchTo();

    DTE.cancel();
}

function openDoc(I, DT, DTE, docName, version) {

    DT.waitForLoader();
    I.wait(0.5);
    if(version == "SK") {
        I.jstreeClick("preklad_sk");
    } else if(version == "EN") {
        I.jstreeClick("preklad_en");
    } else return; //uknown

    DT.filterContains("title", docName);
    I.see(docName);
    I.click(docName);
    DTE.waitForEditor();
}