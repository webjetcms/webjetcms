Feature('apps.send-message');

Before(({ login }) => {
    login('admin');
});

const TabIds = {
    WEB_PAGES: '#pills-webPages-tab',
    FILES: '#pills-files-tab',
    TRANSLATION_KEYS: '#pills-translationKeys-tab'
  };

Scenario('SearchAll - screenshots', ({ I, DT, Document }) => {
    I.amOnPage('/admin/v9/search/index/');
    filter(I, DT, 'naštartoval obchodnú stránku');
    DT.filterContains('title', 'sales');
    Document.screenshot("/redactor/admin/search/search.png");
    Document.screenshotElement("#editorApprootDir", "/redactor/admin/search/tree-filter.png");
    I.clickCss(TabIds.FILES);
    filter(I, DT, 'for-bankers.txt');
    Document.screenshotElement('//a[contains(text(), "for-bankers.txt")]',"/redactor/admin/search/icon-eye.png");
    Document.screenshotElement('//a[text()="/files/zaheslovane/for-bankers.txt"]',"/redactor/admin/search/link.png");

});

function filter(I, DT, value){
    I.fillField('#searchText', value);
    I.clickCss('#searchDataTable_extfilter .filtrujem');
    DT.waitForLoader();
}