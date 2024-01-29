Feature('admin.elFinder');

Before(({ I, login }) => {
    login('admin');
});

Scenario('search files', ({I, Document}) => {

    //all media/images/apps/
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_L2ltYWdlcy9hcHBz");
    //atributy-stranky
    I.waitForText("atributy-stranky", 10, "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

    //
    I.say("Searching in current directory and subdirectories");
    I.fillField("div.elfinder-button-search input", "monitor");
    I.waitForElement("div.elfinder-button-search div.elfinder-button-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwdRecursive]");
    I.waitForLoader(".WJLoaderDiv");

    Document.screenshot("/redactor/elfinder/search.png", 1010, 480);

});