Feature('changelog');

Before(({ I, login }) => {
    login('admin');
});

Scenario('mobilna-verzia', ({ I , DT, Document}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");

    DT.filter("title", "zob");
    DT.filter("authorName", "admin");

    Document.screenshot("/_media/changelog/2021q3/mobile-datatable-filter.png", 812, 375);
});

Scenario('redizajn-wj8', ({ I, Document }) => {
    I.amOnPage("/admin/elFinder/");

    I.resizeWindow(1090, 546);
    I.amOnPage("/admin/elFinder/");

    Document.screenshot("/_media/changelog/2021q4/redesign-wj8.png");

    I.wjSetDefaultWindowSize();
});

Scenario('webpages-fielda-d', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=7611");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-template-tab");
    I.scrollTo("div.DTE_Field_Name_htmlHead");
    I.click("div.DTE_Field_Name_tempFieldADocId button.dropdown-toggle");
    Document.screenshot("/_media/changelog/2021q4/editor-fielda-d.png");
});

Scenario('datatables select missing ID', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=63979");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-template-tab");
    Document.screenshot("/_media/changelog/2023-40/editor-tempid-missing.png");
});

Scenario('minimum required java version alert', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/");
    Document.screenshot("/_media/changelog/2023-40/minimal-java-version.png", 1260, 270);
});