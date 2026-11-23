Feature('webpages.ninja');

var docId = 266;
var pageUrl = "/novy-adresar-01/nevyhladatelna.html";
var defaultSeoValues;
var customSeoValues;
var originalTemplateGroupSeoValues;
var originalPageSeoValues;

Before(({ I, login }) => {
    login('admin');

    if (defaultSeoValues == null) {
        const randomText = I.getRandomText();
        defaultSeoValues = {
            description: `seo-default-description-autotest-${randomText}`,
            image: `/images/gallery/seo-default-image-autotest-${randomText}.png`,
            imageAlt: `seo-default-image-alt-autotest-${randomText}`
        };
        customSeoValues = {
            description: `seo-custom-description-autotest-${randomText}`,
            image: `/images/gallery/seo-custom-image-autotest-${randomText}.png`,
            imageAlt: `seo-custom-image-alt-autotest-${randomText}`
        };
    }
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
    I.amOnPage(pageUrl);
    I.seeInSource(`<link rel="canonical" href="${expected}"`);
    I.amOnPage(pageUrl + "?page=2");
    I.seeInSource(`<link rel="canonical" href="${expected}?page=2"`);
}

function openTemplateGroup(I, DT, DTE) {
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    DT.waitForLoader();
    I.click("Demo JET");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-seo-tab");
}

async function getTemplateGroupSeoValues(I, DT, DTE) {
    openTemplateGroup(I, DT, DTE);
    const values = {
        description: await I.grabValueFrom("#DTE_Field_description"),
        image: await I.grabValueFrom(locate(".DTE_Field_Name_seoImage").find("input.form-control")),
        imageAlt: await I.grabValueFrom("#DTE_Field_seoImageAlt")
    };
    DTE.cancel();
    return values;
}

function setTemplateGroupSeoValues(values, I, DT, DTE) {
    openTemplateGroup(I, DT, DTE);
    DTE.fillField("description", values.description);
    I.fillField(locate(".DTE_Field_Name_seoImage").find("input.form-control"), values.image);
    DTE.fillField("seoImageAlt", values.imageAlt);
    DTE.save();
}

function openPageEditor(I, DTE) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();
    DTE.waitForCkeditor();
}

async function getPageSeoValues(I, DTE) {
    openPageEditor(I, DTE);
    I.click("#pills-dt-datatableInit-fields-tab");
    const values = {
        description: await I.grabValueFrom("#DTE_Field_fieldS"),
        image: await I.grabValueFrom(locate(".DTE_Field_Name_fieldT").find("input.form-control")),
        imageAlt: await I.grabValueFrom("#DTE_Field_fieldP")
    };
    I.click("#pills-dt-datatableInit-perex-tab");
    values.perexDescription = await I.grabValueFrom("#DTE_Field_htmlData");
    values.perexImage = await I.grabValueFrom(locate(".DTE_Field_Name_perexImage").find("input.form-control"));
    DTE.cancel();
    return values;
}

function setPageSeoValues(values, I, DTE) {
    openPageEditor(I, DTE);
    I.click("#pills-dt-datatableInit-fields-tab");
    DTE.fillField("fieldS", values.description);
    I.fillField(locate(".DTE_Field_Name_fieldT").find("input.form-control"), values.image);
    DTE.fillField("fieldP", values.imageAlt);
    I.click("#pills-dt-datatableInit-perex-tab");
    DTE.fillField("htmlData", values.perexDescription);
    I.fillField(locate(".DTE_Field_Name_perexImage").find("input.form-control"), values.perexImage);
    DTE.save();
}

function checkSeoMetadata(expected, unexpected, I, Document) {
    const baseUrl = Document.getBaseUrl();
    I.amOnPage(`${pageUrl}?NO_WJTOOLBAR=true&v=${expected.description}`);
    I.seeInSource(`<meta name="description" content="${expected.description}"`);
    I.seeInSource(`<meta property="og:description" content="${expected.description}"`);
    I.seeInSource(`<meta property="og:image" content="${baseUrl}${expected.image}"`);
    I.seeInSource(`<meta property="og:image:alt" content="${expected.imageAlt}"`);

    if (unexpected != null) {
        I.dontSeeInSource(unexpected.description);
        I.dontSeeInSource(unexpected.image);
        I.dontSeeInSource(unexpected.imageAlt);
    }
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

Scenario('page SEO metadata - default VS custom values @singlethread', async ({I, DT, DTE, Document}) => {
    originalTemplateGroupSeoValues = await getTemplateGroupSeoValues(I, DT, DTE);
    originalPageSeoValues = await getPageSeoValues(I, DTE);

    setTemplateGroupSeoValues(defaultSeoValues, I, DT, DTE);
    setPageSeoValues({
        description: "",
        image: "",
        imageAlt: "",
        perexDescription: "",
        perexImage: ""
    }, I, DTE);
    checkSeoMetadata(defaultSeoValues, customSeoValues, I, Document);

    setPageSeoValues({
        description: customSeoValues.description,
        image: customSeoValues.image,
        imageAlt: customSeoValues.imageAlt,
        perexDescription: "",
        perexImage: ""
    }, I, DTE);
    checkSeoMetadata(customSeoValues, defaultSeoValues, I, Document);
});

Scenario('page SEO metadata - cleanup @singlethread', async ({I, DT, DTE}) => {
    if (originalTemplateGroupSeoValues != null) {
        setTemplateGroupSeoValues(originalTemplateGroupSeoValues, I, DT, DTE);
    }
    if (originalPageSeoValues != null) {
        setPageSeoValues(originalPageSeoValues, I, DTE);
    }
});
