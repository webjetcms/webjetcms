Feature('admin.filebrowser');

Before(({ login }) => {
    login('admin');
});

Scenario('dir properties', async ({I}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_");

    //Find our file in tree
    I.click( locate("span.elfinder-navbar-dir").withText("Súbory") );
    I.click( locate("span.elfinder-navbar-dir").withText("protected") );

    //Open dir editor
    openDirEditor(I);

    //
    I.say("Prepare form");
        I.switchTo("#elfinderIframe");
        I.switchTo("#fbrowserEditForm");

        I.see("Prístupové práva");

        //Test set name
        let dirNme = await I.grabValueFrom("#dir");
        I.assertEqual(dirNme, "dir-edit-form-test");

        I.uncheckOption("#indexFulltext1");
        I.uncheckOption("#passwordProtected1");

        //Save
        I.switchTo();
        I.switchTo();
        I.click("#elfinder-modal button.btn-primary.submit");

        I.switchTo("#elfinderIframe");
        I.waitForText("Údaje boli úspešne uložené.", 20);

    //
    I.say("Test working save");
        I.dontSeeCheckboxIsChecked("#indexFulltext1");
        I.dontSeeCheckboxIsChecked("#passwordProtected1");
        I.dontSeeCheckboxIsChecked("#passwordProtected2");
        I.dontSeeCheckboxIsChecked("#passwordProtected3");

        //Do change
        I.checkOption("#indexFulltext1");
        I.checkOption("#passwordProtected1");
        I.seeCheckboxIsChecked("#indexFulltext1");
        I.seeCheckboxIsChecked("#passwordProtected1");

        //Save
        I.switchTo();
        I.click("#elfinder-modal button.btn-primary.submit");

        //TestChange
        I.switchTo("#elfinderIframe");
        I.waitForText("Údaje boli úspešne uložené.", 20);

        I.seeCheckboxIsChecked("#indexFulltext1");
        I.seeCheckboxIsChecked("#passwordProtected1");
        I.dontSeeCheckboxIsChecked("#passwordProtected2");
        I.dontSeeCheckboxIsChecked("#passwordProtected3");

        //Test set name
        dirNme = await I.grabValueFrom("#dir");
        I.assertEqual(dirNme, "dir-edit-form-test");

        //Return it to former state and save
        I.checkOption("#indexFulltext1");
        I.checkOption("#passwordProtected1");
        I.switchTo();
        I.click("#elfinder-modal button.btn-primary.submit");

        I.switchTo("#elfinderIframe");
        I.waitForText("Údaje boli úspešne uložené.", 20);
    //
    I.say("Test indexing");
        I.switchTo();
        I.selectOption('select.btn.form-control.action', 'Indexuj');
        I.click("#elfinder-modal button.btn-primary.submit");
        I.switchTo("#elfinderIframe");
        I.waitForText("Indexujú sa súbory, prosím čakajte (môže to trvať niekoľko minút).", 200);
        I.dontSee("/files/protected/dir-edit-form-test/");
        I.waitForText("Indexovanie súborov dokončené", 100);
        I.switchTo();
        //We need close windows before further testing
        I.click("button.btn-default.closeModal");
        I.wait(1);

    //
    I.say("Test usage");
        //Open dir editor
        openDirEditor(I);
        I.selectOption('select.btn.form-control.action', 'Použitie');
        I.click("#elfinder-modal button.btn-primary.submit");
        I.switchTo("#elfinderIframe");
        I.waitForText("Použitie súboru: /files/protected/dir-edit-form-test/", 200);
});

function openDirEditor(I) {
    //Open dir editor for /files/protected/dir-edit-form-test
    I.say("Opening /files/protected/dir-edit-form-test");
    I.click("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E");
    I.rightClick('#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E');
    I.click( locate('div.elfinder-contextmenu-item').withChild( locate("span").withText("Nastavenie adresáru") ) );
    I.see("Nastavenie adresáru");
}

function openFileEditor(I) {
    //Open properties of the file
    I.click("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3QvdGVzdGpwZ2F0dGFjaG1lbnRmaWxlLmpwZw_E_E");
    I.rightClick("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3QvdGVzdGpwZ2F0dGFjaG1lbnRmaWxlLmpwZw_E_E");
    I.click( locate('div.elfinder-contextmenu-item').withChild( locate("span").withText("Nastavenie súboru") ) );
    I.waitForElement("div.modal.in");
    I.see("Nastavenie súboru");
}

Scenario('file properties', async ({I}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E");

    openFileEditor(I);

    I.switchTo("#elfinderIframe");

    I.see("Názov súboru");
    I.see("Zmeniť názov vo všetkých súboroch");
    I.seeInField("#file", "testjpgattachmentfile.jpg");

    //
    I.say("Test indexing");
        I.switchTo();
        I.selectOption('select.btn.form-control.action', 'Indexuj');
        I.click("#elfinder-modal button.btn-primary.submit");
        I.switchTo("#elfinderIframe");
        I.waitForText("Indexujú sa súbory, prosím čakajte (môže to trvať niekoľko minút).", 200);
        I.see("/files/protected/dir-edit-form-test/testjpgattachmentfile.jpg");
        I.waitForText("Indexovanie súborov dokončené", 100);
        I.switchTo();
        //We need close windows before further testing
        I.click("button.btn-default.closeModal");
        I.wait(1);

    //
    I.say("Test usage");
        //Open dir editor
        openFileEditor(I);
        I.selectOption('select.btn.form-control.action', 'Použitie');
        I.click("#elfinder-modal button.btn-primary.submit");
        I.switchTo("#elfinderIframe");
        I.waitForText("Použitie súboru: /files/protected/dir-edit-form-test/testjpgattachmentfile.jpg", 200);
});