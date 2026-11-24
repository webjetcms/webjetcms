Feature('webpages.virtual-path').tag('@singlethread');

const testGroupUrl = '/admin/v9/webpages/web-pages-list/?groupid=67';
let urlCleanupPageTitle;

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
});

function testVirtualPathChange(I, DTE, urlDirName) {
    //nastav hodnotu virtualpath do urlDirName
    I.jstreeNavigate(['Test stavov', 'VirtualPath']);
    I.click("div.tree-col button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    DTE.fillField("urlDirName", urlDirName);
    DTE.save();

    //over, ze sa korektne nastavili URL adresy v strankach
    //default stranka
    I.click("VirtualPath", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");

    I.seeElement("div.DTE_Field_Name_virtualPath");
    I.dontSeeElement("div.DTE_Field_Name_editorVirtualPath");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/virtualpath/"); //tato URL sa nemeni, je fixna
    I.dontSeeElement("div.DTE_Field_Name_generateUrlFromTitle .form-switch .form-check-input:checked");
    I.dontSeeElement("div.DTE_Field_Name_urlInheritGroup .form-switch .form-check-input:checked");
    DTE.cancel();

    //Podľa title
    //najskor resetni URL
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25861");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    fillTitle(I, "Podľa title");
    DTE.save();

    I.click("Podľa title", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");

    I.seeElement("div.DTE_Field_Name_virtualPath");
    I.dontSeeElement("div.DTE_Field_Name_editorVirtualPath");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/"+urlDirName+"/podla-title.html");
    I.seeElement("div.DTE_Field_Name_generateUrlFromTitle .form-switch .form-check-input:checked");
    I.dontSeeElement("div.DTE_Field_Name_urlInheritGroup .form-switch .form-check-input:checked");

    //zmen title stranky
    fillTitle(I, "Podľa zmena title");
    DTE.save();
    I.click("Podľa zmena title", "#datatableInit_wrapper");

    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/"+urlDirName+"/podla-zmena-title.html");
    DTE.cancel();

    //podla url dirDirName
    I.click("Podľa urlDirName", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");

    I.dontSeeElement("div.DTE_Field_Name_virtualPath");
    I.seeElement("div.DTE_Field_Name_editorVirtualPath");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/"+urlDirName+"/podla-url-dir-name.html");
    I.seeInField("#DTE_Field_editorVirtualPath", "podla-url-dir-name.html");
    I.see("/test-stavov/"+urlDirName+"/", "div.DTE_Field_Name_editorVirtualPath span.baseurl");
    I.dontSeeElement("div.DTE_Field_Name_generateUrlFromTitle .form-switch .form-check-input:checked");
    I.seeElement("div.DTE_Field_Name_urlInheritGroup .form-switch .form-check-input:checked");
    DTE.cancel();

}

/** Blurs the title before filling navbar so its change handler cannot overwrite typed text. */
function fillTitle(I, title) {
    I.fillField("#DTE_Field_title", title);
    I.clickCss("#DTE_Field_navbar");
    I.fillField("#DTE_Field_navbar", title);
    I.seeInField("#DTE_Field_title", title);
    I.seeInField("#DTE_Field_navbar", title);
}

Scenario('prepinanie poli a zmena URL adries', ({ I, DTE }) => {

    testVirtualPathChange(I, DTE, "virtualpath");

    testVirtualPathChange(I, DTE, "virtualzmena");
});

Scenario('Cleans pasted text in both URL fields', async ({ I, DT, DTE }) => {
    const uniqueSuffix = I.getRandomText();
    urlCleanupPageTitle = `url-autotest-${uniqueSuffix}`;
    const pastedText = 'Operátor vypína 3G sieť, aby mohol ďalej vylepšovať parametre modernejších sietí, vrátane 5G';
    const slug = 'operator-vypina-3g-siet-aby-mohol-dalej-vylepsovat-parametre-modernejsich-sieti-vratane-5g';
    const manualPrefix = `/test-stavov/${urlCleanupPageTitle}/`;
    const inheritedSuffix = `${slug}-${uniqueSuffix}`;

    I.amOnPage(testGroupUrl);
    DT.waitForLoader();
    DT.clickAddButton();
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    fillTitle(I, urlCleanupPageTitle);
    I.uncheckOption('#DTE_Field_generateUrlFromTitle_0');
    I.uncheckOption('#DTE_Field_urlInheritGroup_0');
    DTE.fillField('virtualPath', `${manualPrefix}${pastedText}.html`);
    DTE.save('datatableInit', true);

    DT.filterEquals('title', urlCleanupPageTitle);
    I.click(urlCleanupPageTitle, '#datatableInit tbody');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.assertEqual(await I.grabValueFrom('#DTE_Field_virtualPath'), `${manualPrefix}${slug}.html`);

    // A suffix longer than 64 characters also exercises the expanded history column during the save.
    I.checkOption('#DTE_Field_urlInheritGroup_0');
    DTE.fillField('editorVirtualPath', `${pastedText} ${uniqueSuffix}`);
    DTE.save('datatableInit', true);

    I.click(urlCleanupPageTitle, '#datatableInit tbody');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.seeCheckboxIsChecked('#DTE_Field_urlInheritGroup_0');
    I.assertEqual(await I.grabValueFrom('#DTE_Field_editorVirtualPath'), inheritedSuffix);
    I.assertEqual(await I.grabValueFrom('#DTE_Field_virtualPath'), `/test-stavov/${inheritedSuffix}.html`);
    DTE.cancel();
});

Scenario('Cleans pasted text in both URL fields - delete test page', async ({ I, DT }) => {
    if (!urlCleanupPageTitle) return;

    I.amOnPage(testGroupUrl);
    DT.filterEquals('title', urlCleanupPageTitle);
    if (await I.grabNumberOfVisibleElements('#datatableInit tbody tr[id]') > 0) DT.deleteAll();
});
