Feature('admin.rest');

Before(({ login }) => {
    login('admin');
});

Scenario("Allow all IP addresses", ({ I, Document }) => {
    Document.setConfigValue('restAllowedIpAddresses', "*");
    Document.setConfigValue('restAllowedIpAddresses-DocRestController', '');
    Document.setConfigValue('restAllowedIpAddresses-PropertiesRestController', '');
    Document.setConfigValue('propertiesRestControllerAllowedKeysPrefixes', "*");
});

Scenario("Test retrieving document details by docId ", ({ I }) => {
    I.sendGetRequest('/rest/documents/141');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsKeys([
        'groupId', 'title', 'navbar', 'virtualPath', 'editorVirtualPath',
        'generateUrlFromTitle', 'urlInheritGroup', 'sortPriority', 'externalLink',
        'available', 'searchable', 'cacheable', 'tempId', 'headerDocId', 'footerDocId',
        'menuDocId', 'rightMenuDocId', 'perexGroups', 'dateCreated', 'passwordProtected',
        'disableAfterEnd', 'publishAfterStart', 'lazyLoaded', 'requireSsl', 'fieldA', 'fieldB',
        'fieldC', 'fieldD', 'fieldE', 'fieldF', 'fieldG', 'fieldH', 'fieldI', 'fieldJ',
        'fieldK', 'fieldL', 'fieldM', 'fieldN', 'fieldO', 'fieldP', 'fieldQ', 'fieldR',
        'fieldS', 'fieldT', 'tempFieldADocId', 'tempFieldBDocId', 'tempFieldCDocId',
        'tempFieldDDocId', 'showInMenu', 'showInNavbar', 'showInSitemap', 'loggedShowInMenu',
        'loggedShowInNavbar', 'loggedShowInSitemap', 'htmlHead', 'publishStartDate',
        'publishEndDate', 'eventDateDate', 'htmlData', 'perexPlace', 'perexImage',
        'authorName', 'docLink', 'data', 'authorId', 'tempName', 'logonPageDocId',
        'fileName', 'authorEmail', 'authorPhoto', 'fullPath', 'id', 'rootGroupL1', 'rootGroupL2',
        'rootGroupL3', 'publicable', 'historyActual', 'historyApproveDate', 'historyApprovedByName',
        'historyDisapprovedByName', 'historySaveDate', 'historyId', 'historyApprovedBy',
        'historyDisapprovedBy', 'docId', 'publishStart', 'publishEnd', 'eventDate', 'editorFields'
    ]);

    I.seeResponseContainsJson({
        "title": "Jet portal 4 - testovacia stranka",
        "navbar": "Jet portal 4 - testovacia stranka"
    });
    I.seeResponseContainsJson({
        "available": true
    });
});


Scenario("Test retrieving AB testing translation keys with set configuration value", ({ I, Document }) => {
    Document.setConfigValue('propertiesRestControllerAllowedKeysPrefixes', 'components.abtesting\nwebpages.modal');
    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson({
        "components.abtesting.dialog_title": "AB testovanie",
        "components.abtesting.ab_page_list": "Zoznam AB stránok",
        "components.abtesting.variantName": "Názov varianty",
        "components.abtesting.example": "Príklad",
        "components.abtesting.cookieDays": "Platnosť cookie (v dňoch)",
        "components.abtesting.allowed": "AB testovanie povolené",
        "components.abtesting.ratio": "Pomer",
        "components.abtesting.cookieName": "Názov cookie"
    });

    I.sendGetRequest('/rest/properties/sk/webpages.modal');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson({
        "webpages.modal.save": "Uložiť zmeny",
        "webpages.modal.title": "Nastavanie zobrazenia",
        "webpages.modal.close": "Zavrieť",
        "webpages.modal.dateCreated": "Dátum vytvorenia",
        "webpages.modal.docId": "ID web stránky (Doc ID)",
        "webpages.modal.sortPriority": "Poradie"
    });

    Document.setConfigValue('propertiesRestControllerAllowedKeysPrefixes', "components.abtesting");
    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson({
        "components.abtesting.dialog_title": "AB testovanie"
    });

    I.sendGetRequest('/rest/properties/sk/webpages.modal');
    I.seeResponseContainsJson({});
    I.dontSeeResponseContainsJson({
        "webpages.modal.save": "Uložiť zmeny",
    });
});

Scenario('Test retrieving AB testing translation keys without set configuration value', ({ I, Document }) => {
    Document.setConfigValue('propertiesRestControllerAllowedKeysPrefixes', '');
    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson({});
    I.dontSeeResponseContainsJson({
        "components.abtesting.dialog_title": "AB testovanie",
        "components.abtesting.ab_page_list": "Zoznam AB stránok",
        "components.abtesting.variantName": "Názov varianty",
        "components.abtesting.example": "Príklad",
        "components.abtesting.cookieDays": "Platnosť cookie (v dňoch)",
        "components.abtesting.allowed": "AB testovanie povolené",
        "components.abtesting.ratio": "Pomer",
        "components.abtesting.cookieName": "Názov cookie"
    });

    I.sendGetRequest('/rest/properties/sk/webpages.modal');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson({});
    I.dontSeeResponseContainsJson({
        "webpages.modal.save": "Uložiť zmeny",
        "webpages.modal.title": "Nastavanie zobrazenia",
        "webpages.modal.close": "Zavrieť",
        "webpages.modal.dateCreated": "Dátum vytvorenia",
        "webpages.modal.docId": "ID web stránky (Doc ID)",
        "webpages.modal.sortPriority": "Poradie"
    });
});

Scenario('Test retrieving translation keys for multiple languages', ({ I, Document }) => {
    Document.setConfigValue('propertiesRestControllerAllowedKeysPrefixes', 'components.basket');

    const languages = ['sk', 'cs', 'en'];
    const expectedTranslations = {
        "sk": "Pridať do košíka",
        "cs": "Přidat do košíku",
        "en": "Add to basket"
    };

    languages.forEach(language => {
        I.sendGetRequest(`/rest/property/${language}/components.basket.addToBasket`);
        I.seeResponseCodeIs(200);
        I.seeResponseContainsJson({
            "components.basket.addToBasket": expectedTranslations[language]
        });
    });
});

Scenario('Test retrieving translation with randomly generated bonus parameters', ({ I, Document }) => {

    Document.setConfigValue('propertiesRestControllerAllowedKeysPrefixes', 'converter.number.invalidNumber');

    const param1 = I.getRandomText();
    const param2 = I.getRandomText();

    const expectedTranslation = `Hodnota (${param2}) v poli ${param1} musí byť číslo`;
    I.sendGetRequest(`/rest/property/sk/converter.number.invalidNumber/${param1}/${param2}`)
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson({
        "converter.number.invalidNumber": expectedTranslation
    });
});

Scenario('Test IP address allowed for services', ({ I, Document }) => {
    Document.setConfigValue('restAllowedIpAddresses', "");
    I.sendGetRequest('/rest/documents/141');
    I.seeResponseCodeIs(401);

    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(401);

    //
    I.say("Allowing DocRestController");
    Document.setConfigValue('restAllowedIpAddresses-DocRestController', "*");
    I.sendGetRequest('/rest/documents/141');
    I.seeResponseCodeIs(200);

    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(401);

    //
    I.say("Allowing also properties");
    Document.setConfigValue('restAllowedIpAddresses-PropertiesRestController', "*");
    I.sendGetRequest('/rest/documents/141');
    I.seeResponseCodeIs(200);

    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(200);

    //
    I.say("Disabling documents");
    Document.setConfigValue('restAllowedIpAddresses-DocRestController', '');
    I.sendGetRequest('/rest/documents/141');
    I.seeResponseCodeIs(401);

    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(200);
});

Scenario('Reset configuration value', ({ I, Document }) => {
    Document.setConfigValue('propertiesRestControllerAllowedKeysPrefixes', '');
    Document.setConfigValue('restAllowedIpAddresses-DocRestController', '');
    Document.setConfigValue('restAllowedIpAddresses-PropertiesRestController', '');

    Document.setConfigValue('restAllowedIpAddresses', "");

    I.sendGetRequest('/rest/documents/141');
    I.seeResponseCodeIs(401);

    I.sendGetRequest('/rest/properties/sk/components.abtesting');
    I.seeResponseCodeIs(401);
});
