Feature('admin.filebrowser');

Before(({ login }) => {
    login('admin');
});

Scenario('dir properties', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_");

    //Find our file in tree
    I.click( locate("span.elfinder-navbar-dir").withText("Súbory"), null, { position: { x: 20, y: 5 } });
    I.click( locate("span.elfinder-navbar-dir").withText("protected"), null, { position: { x: 20, y: 5 } } );

    //Open dir editor + check dir path
    openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");

    //
    I.say("Edit entity");

        I.see("Indexovať súbory pre vyhľadávanie");
        I.see("Prístupové práva k neverejným sekciám web sídla");

        I.uncheckOption("#DTE_Field_indexFullText_0");
        I.uncheckOption("#DTE_Field_editorFields-permisions_1");

        //Save
        I.switchTo();
        I.click( locate("div.modal-footer").find("button.btn.btn-primary") );

        // TODO - later when save return some message - do check
    //
    I.say("Test working save");
        openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");

        I.dontSeeCheckboxIsChecked("#DTE_Field_indexFullText_0");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_2");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_3");

        //Do change
        I.checkOption("#DTE_Field_indexFullText_0");
        I.checkOption("#DTE_Field_editorFields-permisions_1");
        I.seeCheckboxIsChecked("#DTE_Field_indexFullText_0");
        I.seeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");

        //Save
        I.switchTo();
        I.click( locate("div.modal-footer").find("button.btn.btn-primary") );

        //TestChange
        openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");

        I.seeCheckboxIsChecked("#DTE_Field_indexFullText_0");
        I.seeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_2");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_3");

        //Return it to former state and save
        I.uncheckOption("#DTE_Field_indexFullText_0");
        I.uncheckOption("#DTE_Field_editorFields-permisions_1");

        //Save
        I.switchTo();
        I.click( locate("div.modal-footer").find("button.btn.btn-primary") );

    I.say("Test indexing");
        openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");
        I.clickCss("#pills-dt-datatableInit-index-tab");

        I.waitForElement("#indexMenu");
        I.see("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        I.click("button#start-index-button");
        I.dontSee("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        //I.see("Indexujú sa súbory, prosím čakajte (môže to trvať niekoľko minút).");

        I.waitForText("Indexovanie súborov dokončené", 100);

    //
    I.say("Test usage");
        I.clickCss("#pills-dt-datatableInit-usage-tab");
        DT.waitForLoader("#datatableFieldDTE_Field_editorFields-docDetailsList_processing", 200);
        I.see("Použitie", ".nav-link.active");
});

function openDirEditorAndCheck(I, dirPath) {
    //Open dir editor for /files/protected/dir-edit-form-test
    I.say("Opening /files/protected/dir-edit-form-test");
    I.click("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E");
    I.rightClick('#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E');
    I.click( locate('div.elfinder-contextmenu-item').withChild( locate("span").withText("Nastavenie priečinka") ) );
    I.switchTo("#modalIframeIframeElement");
    I.seeElement( locate("h5.modal-title").withText(dirPath) );
}

function openFileEditorAndCheck(I, fileName) {
    //Open properties of the file
    I.click("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3QvdGVzdGpwZ2F0dGFjaG1lbnRmaWxlLmpwZw_E_E");
    I.rightClick("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3QvdGVzdGpwZ2F0dGFjaG1lbnRmaWxlLmpwZw_E_E");
    I.click( locate('div.elfinder-contextmenu-item').withChild( locate("span").withText("Nastavenie súboru") ) );
    I.switchTo("#modalIframeIframeElement");
    I.seeElement( locate("h5.modal-title").withText(fileName) );
}

Scenario('file properties', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E");

    openFileEditorAndCheck(I, "testjpgattachmentfile.jpg");

    //
    I.say("Test indexing");
        I.clickCss("#pills-dt-datatableInit-index-tab");

        I.waitForElement("#indexMenu");
        I.see("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        I.click("button#start-index-button");
        I.dontSee("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        I.see("Indexujú sa súbory, prosím čakajte (môže to trvať niekoľko minút).");

        I.waitForText("Indexovanie súborov dokončené", 100);

    //
    I.say("Test usage");
        I.clickCss("#pills-dt-datatableInit-usage-tab");
        DT.waitForLoader("#datatableFieldDTE_Field_docDetailsList_processing", 200);
        I.see("Použitie", ".nav-link.active");
});