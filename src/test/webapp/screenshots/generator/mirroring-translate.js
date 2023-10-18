Feature('wj9-webpage-mirroring-translate');

var add_webButton = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider'));
var remove_webButton = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-remove.btn-danger.buttons-divider'));
var docDataSK = '<p class="text-center"><strong>Názov stránky</strong></p><p>Test prekladu stránky s&nbsp;rôznymi slovami.</p><ol>	<li>dnes</li>	<li>zajtra</li>	<li>pozajtra</li></ol>';

Scenario('generovanie screenov', async ({I, DT, DTE, Document}) => {  
    I.relogin("admin");
    I.amOnPage("/admin/v9/webpages/web-pages-list");

    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("mirroring.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");

    I.click( locate("a.jstree-anchor").withText("preklad_sk") );
    I.click("button.buttons-edit");
    I.click("#pills-dt-groups-datatable-template-tab");
    Document.screenshot("/redactor/apps/docmirroring/language.png");
    DTE.cancel();

    I.click(add_webButton);
    I.waitForVisible("#DTE_Field_title");
    I.fillField("#DTE_Field_title", "dobré ráno");

    I.click("#pills-dt-datatableInit-content-tab");
    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(docDataSK);
    DTE.save();

    DT.filter("title", "dobré ráno");
    I.click("dobré ráno");
    DTE.waitForEditor();
    Document.screenshot("/redactor/apps/docmirroring/doc-sk.png");
    DTE.cancel();

    I.click( locate("a.jstree-anchor").withText("preklad_en") );
    DT.filter("title", "good morning");
    I.click("good morning");
    DTE.waitForEditor();

    Document.screenshot("/redactor/apps/docmirroring/doc-en.png");

    DTE.cancel();

    // Delete 
    I.click("td.dt-select-td.sorting_1");
    I.click(remove_webButton);
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.click("#pills-trash-tab");

    DT.filter("title", "good morning");
    I.click("td.dt-select-td.sorting_1");
    I.click(remove_webButton);

    DT.filter("title", "dobré ráno");
    I.click("td.dt-select-td.sorting_1");
    I.click(remove_webButton);
    I.wait(10);
});