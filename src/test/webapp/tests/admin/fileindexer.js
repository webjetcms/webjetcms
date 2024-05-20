Feature('elFinder.fileIndexer');

var randomNumber = (new Date()).getTime();

Before(({ I, login }) => {
    login('admin');
});

Scenario('test indexovania suborov', ({I}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_");

    I.click("#finder > div.elfinder-workzone > div.ui-state-default.elfinder-navbar.ui-resizable > div.elfinder-tree > div:nth-child(2)");

    I.rightClick('#iwcm_2_L2ZpbGVz');

    I.click('#finder > div.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle > div:nth-child(16)');

    I.switchTo("#elfinderIframe");

    I.switchTo();

    I.selectOption('#elfinder-modal > div > div > div.modal-footer > select', 'Indexuj');

    I.click("#elfinder-modal > div > div > div.modal-footer > button.btn.btn-primary.submit");

    I.switchTo("#elfinderIframe");

    I.waitForText("Indexovanie súborov dokončené", 300);

    I.see("Indexovanie súborov dokončené");

    I.see("/files/archiv/zsd_faq_fakturacia-poplatkov-od-2014.pdf");
    I.see("/files/jurko.jpg");
    I.see("/files/zaheslovane/barepage.png");
    //folder /files/zaheslovane/peknylogin should not be indexed
    I.dontSee("/files/zaheslovane/peknylogin/");
    //protected files should not be indexed
    I.dontSee("/files/protected/");
});

Scenario('access protected full text index', ({I}) => {
    I.amOnPage("/files/protected/bankari/test-forward.txt.html?v=1"+randomNumber);
    I.see("požadovaná stránka neexistuje");
    I.logout();
    I.wait(2);
    I.amOnPage("/files/protected/bankari/test-forward.txt.html?v=2"+randomNumber);
    I.see("Zadajte vaše prihlasovacie údaje");
});

function accessProtectedFile(I) {
    I.say("I'm logged, file should be ACCESSIBLE");
    I.amOnPage("/files/protected/bankari/test-forward.txt?v=3"+randomNumber);
    I.waitForText("This is test file for fileforward after logon", 10);
    I.amOnPage("/files/zaheslovane/for-bankers.txt?v=3"+randomNumber);
    I.waitForText("This file is accessible only for users in Bankari group", 10);

    I.logout();

    I.say("I'm NOT logged, file should NOT be ACCESSIBLE");
    I.amOnPage("/files/protected/bankari/test-forward.txt?v=4"+randomNumber);
    I.waitForText("Zadajte vaše prihlasovacie údaje", 10);
    I.amOnPage("/files/zaheslovane/for-bankers.txt?v=4"+randomNumber);
    I.waitForText("Zadajte vaše prihlasovacie údaje", 10);
}

Scenario('access protected file @singlethread', ({I, Document}) => {
    Document.setConfigValue("cloudStaticFilesDir", "")
    accessProtectedFile(I);
    I.relogin("admin");
    Document.setConfigValue("cloudStaticFilesDir", "{FILE_ROOT}static-files/");
    accessProtectedFile(I);
});

Scenario('access protected file-reset @singlethread', ({I, Document}) => {
    Document.setConfigValue("cloudStaticFilesDir", "")
});

function searchProtectedFile(I) {
    I.amOnPage("/apps/vyhladavanie/");
    I.fillField('#searchWords', 'fileforward');
    I.click(".smallSearchSubmit");
    I.waitForElement("h1.searchResultsH1");
    I.see("Neboli nájdené žiadne stránky vyhovujúce zadaným kritériám.");
}

Scenario('search', ({I}) => {
    searchProtectedFile(I);
    I.logout();
    I.wait(10);
    searchProtectedFile(I);
});

Scenario('Lucene indexing', ({I}) => {

    //
    I.say("Reindex data");
    I.amOnPage("/components/search/lucene_console.jsp");
    I.selectOption("#indexed", "documents");
    I.click("input[name=index]");
    I.waitForText("Closing index.", 300);

    //
    I.say("Testing search");
    I.amOnPage("/components/search/lucene_console.jsp");
    I.selectOption("#indexed", "documents");
    I.fillField("#query", "fileforward");
    I.click("input[name=search]");
    I.waitForText("Testing query: fileforward");
    I.see("Size: 0");
    I.dontSee("title=test-forward.txt");
});