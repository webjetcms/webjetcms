Feature('admin.fileIndexer');

var randomNumber = (new Date()).getTime();

Before(({ I, login }) => {
    login('admin');
});

Scenario('test indexovania suborov', ({I}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_");

    I.clickCss("#finder > div.elfinder-workzone > div.ui-state-default.elfinder-navbar.ui-resizable > div.elfinder-tree > div:nth-child(2)");
    I.rightClick('#iwcm_2_L2ZpbGVz');
    I.click( locate('div.elfinder-contextmenu-item').withChild( locate("span").withText("Nastavenie priečinka") ) );
    I.switchTo("#modalIframeIframeElement");
    I.seeElement( locate("h5.modal-title").withText("/files") );

    I.clickCss("#pills-dt-datatableInit-index-tab");

    I.waitForElement("#indexMenu");
    I.see("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
    I.click("button#start-index-button");
    I.dontSee("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
    //I.see("Indexujú sa súbory, prosím čakajte (môže to trvať niekoľko minút).");

    I.waitForText("/files/jurko.jpg", 200);
    I.dontSee("/files/archiv/zsd_faq_fakturacia-poplatkov-od-2014.pdf");
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
    I.waitForElement("h1.searchResultsH1", 20);
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
    I.selectOption({css: "#indexed"}, "documents");
    I.click("input[name=index]");
    I.waitForText("Closing index.", 300);

    //
    I.say("Testing search");
    I.amOnPage("/components/search/lucene_console.jsp");
    I.selectOption({css: "#indexed"}, "documents");
    I.fillField("#query", "fileforward");
    I.clickCss("input[name=search]");
    I.waitForText("Testing query: fileforward");
    I.see("Size: 0");
    I.dontSee("title=test-forward.txt");
});