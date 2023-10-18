Feature('elFinder.fileIndexer');

Before(({ I, login }) => {
    login('admin');
});

Scenario('test indexovania suborov', async ({I, DataTables}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_");

    I.click("#finder > div.elfinder-workzone > div.ui-state-default.elfinder-navbar.ui-resizable > div.elfinder-tree > div:nth-child(2)");

    I.rightClick('#iwcm_2_L2ZpbGVz');

    I.click('#finder > div.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle > div:nth-child(16)');

    I.switchTo("#elfinderIframe");

    I.click('div.checker');

    I.switchTo();

    I.selectOption('#elfinder-modal > div > div > div.modal-footer > select', 'Indexuj');

    I.click("#elfinder-modal > div > div > div.modal-footer > button.btn.btn-primary.submit");

    I.switchTo("#elfinderIframe");

    I.waitForText("Indexovanie súborov dokončené", 20);

    I.see("Indexovanie súborov dokončené");

    I.see("/files/jurko.jpg");
    I.see("/files/zaheslovane/barepage.png");
    I.see("/files/zaheslovane/peknylogin/barepage.png");
});