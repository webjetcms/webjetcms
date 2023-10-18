Feature('admin.elFinder');

Before(({ I, login }) => {
    login('admin');
});

function getFileUsage(I) {

    I.switchTo();

    I.selectOption('#elfinder-modal > div > div > div.modal-footer > select', 'Použitie');

    I.click("#elfinder-modal > div > div > div.modal-footer > button.btn.btn-primary.submit");

    I.switchTo("#elfinderIframe");

    I.waitForText("Použitie súboru:", 300);

    I.see("Použitie súboru:");

    I.see("Test file usage");

    I.switchTo();
}

Scenario('dir usage', async ({I, DataTables}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcw_E_E");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5');

    I.click('#finder > div.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle > div:nth-child(16)');

    I.switchTo("#elfinderIframe");

    I.see("Indexovať súbory pre vyhľadávanie");
    I.see("Obchodní partneri");


    getFileUsage(I);
});

Scenario('file usage', async ({I, DataTables}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E');

    I.click('#finder > div.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle > div:nth-child(20)');

    I.switchTo("#elfinderIframe");

    I.see("Názov súboru");
    I.see("Zmeniť názov vo všetkých súboroch");

    getFileUsage(I);
});