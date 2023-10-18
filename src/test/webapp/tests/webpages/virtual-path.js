Feature('webpages.virtual-path');

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
    I.click("#pills-dt-datatableInit-basic-tab");

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
    I.wait(5); //ckeditor
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("#DTE_Field_title", "Podľa title");
    I.wait(1);
    I.fillField("#DTE_Field_navbar", "Podľa title");
    I.wait(1);
    I.fillField("#DTE_Field_navbar", "Podľa title"); //este raz, lebo blur na title to sem doplni a schaosi vyplnanie
    DTE.save();

    I.click("Podľa title", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");

    I.seeElement("div.DTE_Field_Name_virtualPath");
    I.dontSeeElement("div.DTE_Field_Name_editorVirtualPath");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/"+urlDirName+"/podla-title.html");
    I.seeElement("div.DTE_Field_Name_generateUrlFromTitle .form-switch .form-check-input:checked");
    I.dontSeeElement("div.DTE_Field_Name_urlInheritGroup .form-switch .form-check-input:checked");

    //zmen title stranky
    I.fillField("#DTE_Field_title", "Podľa zmena title");
    I.wait(1);
    I.fillField("#DTE_Field_navbar", "Podľa zmena title");
    I.wait(1);
    I.fillField("#DTE_Field_navbar", "Podľa zmena title");
    DTE.save();
    I.click("Podľa zmena title", "#datatableInit_wrapper");

    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/"+urlDirName+"/podla-zmena-title.html");
    DTE.cancel();

    //podla url dirDirName
    I.click("Podľa urlDirName", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");

    I.dontSeeElement("div.DTE_Field_Name_virtualPath");
    I.seeElement("div.DTE_Field_Name_editorVirtualPath");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/"+urlDirName+"/podla-url-dir-name.html");
    I.seeInField("#DTE_Field_editorVirtualPath", "podla-url-dir-name.html");
    I.see("/test-stavov/"+urlDirName+"/", "div.DTE_Field_Name_editorVirtualPath span.baseurl");
    I.dontSeeElement("div.DTE_Field_Name_generateUrlFromTitle .form-switch .form-check-input:checked");
    I.seeElement("div.DTE_Field_Name_urlInheritGroup .form-switch .form-check-input:checked");
    DTE.cancel();

}

Scenario('prepinanie poli a zmena URL adries', ({ I, DTE }) => {

    testVirtualPathChange(I, DTE, "virtualpath");

    testVirtualPathChange(I, DTE, "virtualzmena");
});
