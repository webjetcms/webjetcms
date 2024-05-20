Feature('wj9-webpage-mirroring-translate');

var add_webButton = (locate('#datatableInit_wrapper').find('.buttons-create.btn-success'));
var remove_webButton = (locate('#datatableInit_wrapper').find('.buttons-remove.btn-danger'));
var docDataSK = '<p class="text-center"><strong>Názov stránky</strong></p><p>Test prekladu stránky s&nbsp;rôznymi slovami.</p><ol>	<li>dnes</li>	<li>zajtra</li>	<li>pozajtra</li></ol>';

var skVersionName = "dobré nové ráno";
var enVersionName = "good new morning";

Before(({ I, login }) => {
    login('admin');
});

Scenario('generovanie screenov', async ({I, DT, DTE, Document}) => {  
    let confLng = I.getConfLng();
    I.say("Set configuration value");
    Document.setConfigValue("structureMirroringConfig", "56845,56846:mirroring.tau27.iway.sk");

    I.amOnPage("/admin/v9/webpages/web-pages-list");
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("mirroring.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");

    I.click( locate("a.jstree-anchor").withText("preklad_sk") );
    I.clickCss("button.buttons-edit");
    I.clickCss("#pills-dt-groups-datatable-template-tab");
    Document.screenshot("/redactor/apps/docmirroring/language.png");
    DTE.cancel();

    I.click(add_webButton);
    DTE.waitForEditor();
    I.waitForVisible("#DTE_Field_title");
    I.fillField("#DTE_Field_title", skVersionName);

    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(docDataSK);
    DTE.save();

    //TODO redo this part
    DT.filter("title", skVersionName);
    I.click(skVersionName);
    DTE.waitForEditor();
    Document.screenshot("/redactor/apps/docmirroring/doc-sk.png");
    DTE.cancel();

    I.click( locate("a.jstree-anchor").withText("preklad_en") );
    DT.filter("title", enVersionName);
    I.click(enVersionName);
    DTE.waitForEditor();

    Document.screenshot("/redactor/apps/docmirroring/doc-en.png");

    DTE.cancel();
    I.wait(1);

    // Delete 
    removePage(I, DT, enVersionName, confLng);

    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();

    //remove created pages
    removePage(I, DT, skVersionName, confLng, true);
    removePage(I, DT, enVersionName, confLng, true);
});

function removePage(I, DT, pageName, confLng, all = false) {
    DT.filter("title", pageName);

    if(all === true) {
        I.clickCss("button.buttons-select-all");
    } else {
        I.forceClick("#datatableInit tbody tr:nth-child(1) td.dt-select-td");
    }
    
    I.click(remove_webButton);

    if("sk" === confLng) {
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    } else if("en" === confLng) { 
        I.click("Delete", "div.DTE_Action_Remove");
        I.see("No matching records found");
    }
}