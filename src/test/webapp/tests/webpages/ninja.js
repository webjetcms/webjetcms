Feature('webpages.ninja');

var docId = 266;

Before(({ I, login }) => {
    login('admin');
});

function setField(field, value, I, DTE) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.click("#pills-dt-datatableInit-fields-tab");
    DTE.fillField("field"+field, value);
    DTE.save();
}

function checkCanonical(expected, I) {
    I.amOnPage("/novy-adresar-01/nevyhladatelna.html");
    I.seeInSource(`<link rel="canonical" href="${expected}"`);
    I.amOnPage("/novy-adresar-01/nevyhladatelna.html?page=2");
    I.seeInSource(`<link rel="canonical" href="${expected}?page=2"`);
}

Scenario('page-canonical', async ({I, DT, DTE, Document}) => {

    setField("Q", "", I, DTE);

    let protocol = Document.getProtocol();

    checkCanonical(`${protocol}://demo.webjetcms.sk/novy-adresar-01/nevyhladatelna.html`, I);

    setField("Q", "/novy-adresar-01/nevyhladatelna-canonical.html", I, DTE);
    checkCanonical(`${protocol}://demo.webjetcms.sk/novy-adresar-01/nevyhladatelna-canonical.html`, I);

    setField("Q", "https://www.webjetcms.sk/canonical.html", I, DTE);
    checkCanonical(`https://www.webjetcms.sk/canonical.html`, I);
});

Scenario('page-canonical-cleanup', async ({I, DT, DTE, Document}) => {
    setField("Q", "", I, DTE);
});
