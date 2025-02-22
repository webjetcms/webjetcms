Feature('admin.searchall');

const TableWrappers = {
    WEB_PAGES: '#webPagesDataTable_wrapper',
    FILES: '#filesDataTable_wrapper',
    TRANSLATION_KEYS: '#translationKeysDataTable_wrapper'
};

const TabIds = {
    WEB_PAGES: '#pills-webPages-tab',
    FILES: '#pills-files-tab',
    TRANSLATION_KEYS: '#pills-translationKeys-tab'
  };

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
    }
});


Scenario('Verify Tabs and Data Table Headers in Admin Search', ({ I }) => {
    const pageHeaders = {
        webpages: [
            "ID",
            "Stav",
            "Názov web stránky",
            "Meno autora",
            "Posledná zmena"
        ],
        files: [
            "ID",
            "Názov web stránky",
            "Meno autora",
            "Posledná zmena"
        ],
        translationKeys: [
            "ID",
            "Kľúč",
            "Dátum zmeny",
            "Slovenský",
            "Český",
            "Anglický",
            "Nemecký",
            "Poľský",
            "Maďarský",
            "Chorvátsky",
            "Ruský",
            "Španielsky"
        ]
    };

    I.amOnPage('/admin/v9/search/index/');

    // Verify Web Pages tab
    I.clickCss(TabIds.WEB_PAGES);
    verifyHeaders(
        I,
        pageHeaders.webpages,
        TableWrappers.WEB_PAGES,
        [TableWrappers.FILES, TableWrappers.TRANSLATION_KEYS]
    );

    // Verify Files tab
    I.clickCss(TabIds.FILES);
    verifyHeaders(
        I,
        pageHeaders.files,
        TableWrappers.FILES,
        [TableWrappers.WEB_PAGES, TableWrappers.TRANSLATION_KEYS]
    );

    // Verify Translation Keys tab
    I.clickCss(TabIds.TRANSLATION_KEYS)
    verifyHeaders(
        I,
        pageHeaders.translationKeys,
        TableWrappers.TRANSLATION_KEYS,
        [TableWrappers.WEB_PAGES, TableWrappers.FILES]
    );
});

Scenario('SearchAll - webpages tab tests', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/search/index/');
    I.clickCss(TabIds.WEB_PAGES);
    filter(I, DT, 'Investičný vklad');
    I.waitForText('Záznamy 1 až 2 z 2');
    I.see('Investičný vklad', 'table tr');

    I.clickCss('#editorApprootDir button');
    I.click('English');
    I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10);

    I.clickCss('#editorApprootDir button');
    I.click('Jet portal 4');
    I.waitForText('Záznamy 1 až 2 z 2');

    filter(I, DT, 'test-podadresar');
    I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10);

    I.say('overiť že keď použijem ten externý filter tak súčasne funguje aj filtrovanie nad stĺpcami a že ak externý filter niečo vrátil, tak filter nad stĺpcom nemôže vratiť nič naviac');

    filter(I, DT, 'stranka');
    await verifyRecordCountChange(I, DT, 'authorName', 'tester', TableWrappers.WEB_PAGES);

    I.clickCss('#editorApprootDir button');
    I.click('Koreňový priečinok');
    I.say('Calling removeAllFilters...');
    await removeAllFilters(I);
    I.say('removeAllFilters executed.');

    filter(I, DT, 'naštartoval obchodnú stránku');
    await verifyRecordCountChange(I, DT, 'title', 'sales', TableWrappers.WEB_PAGES);
    I.see('McGregor sales force', 'table tr');
});

//v suboroch viem hladat "txt" a potom v Nazov suboru odfiltrovat len bankers
Scenario('SearchAll - files tab tests', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/search/index/');
    I.clickCss(TabIds.FILES);
    DT.waitForLoader("filesDataTable");

    filter(I, DT, 'txt');
    const tableInfo = TableWrappers.FILES.replace('wrapper', 'info');
    const recordsBefore = await getTotalRecords(I, tableInfo);
    // unable to use DT.filter('title', 'bankers);
    I.fillField(`${TableWrappers.FILES} th.dt-format-text.dt-th-title > form > div > input`, 'bankers');
    I.clickCss(`${TableWrappers.FILES} th.dt-format-text.dt-th-title > form > div > button`);
    DT.waitForLoader();
    const recordsAfter = await getTotalRecords(I, tableInfo);
    I.assertAbove(recordsBefore, recordsAfter, `Target data after filter on is greater than the original value`);
    I.see('for-bankers.txt', 'table tr');
    await removeAllFilters(I);

    filter(I, DT, 'jurko.jpg');
    I.waitForText('Záznamy 1 až 1 z 1');
    I.see('jurko.jpg', 'table tr');

    filter(I, DT, 'only for users in Bankari group');
    I.waitForText('Záznamy 1 až 1 z 1');
    I.see('for-bankers.txt', 'table tr');

});

Scenario('SearchAll - translation keys tab tests', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/search/index/');
    I.clickCss(TabIds.TRANSLATION_KEYS);
    DT.waitForLoader("translationKeysDataTable");

    filter(I, DT, 'Toto je bežne používané heslo.');
    I.waitForText('Záznamy 1 až 1 z 1');
    I.see('wj-password-strength.warnings.common.js', 'table tr');

    filter(I, DT, 'Pridať adresár');
    await verifyRecordCountChange(I, DT, 'key', 'addGroup', TableWrappers.TRANSLATION_KEYS);

});

Scenario('SearchAll - user with limited permission', ({ I, DT }) => {
    I.relogin('ftester');
    I.amOnPage('/admin/v9/search/index/');
    I.clickCss(TabIds.WEB_PAGES);
    DT.waitForLoader();
    filter(I, DT, 'McGregor sales force');

    I.waitForText('Záznamy 1 až 1 z 1');
    I.see('McGregor sales force', 'table tr');

    filter(I, DT, 'Investičný vklad');
    I.waitForText('Nenašli sa žiadne vyhovujúce záznamy');
});

Scenario('SearchAll - domains', ({ I, DT }) => {
    I.amOnPage('/admin/v9/search/index/');
    DT.waitForLoader();

    switchDomainTo(I, DT, "mirroring.tau27.iway.sk");

    I.say('Testing webpages in another domain');
    I.click('#pills-webPages-tab');
    filter(I, DT, 'Investičný vklad');
    I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10);
    filter(I, DT, 'Vstup');
    I.see('Vstup', 'table tr');

    I.say('Testing files in another domain');
    I.clickCss('#pills-files-tab');
    filter(I, DT, 'jurko.jpg');
    I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10);

    switchDomainTo(I, DT, I.getDefaultDomainName());
    filter(I, DT, 'Vstup');
    I.dontSee('Vstup', 'table tr');
});

Scenario('Searchall - file link and the clickable URL redirection', async ({ I, DT }) => {
    const bankersUrl = '/files/zaheslovane/for-bankers.txt';

    I.amOnPage('/admin/v9/search/index/');
    I.click(TabIds.FILES);
    filter(I, DT, 'for-bankers.txt');

    I.click('//a[contains(text(), "for-bankers.txt")]');

    I.switchToNextTab();
    const currentUrl = await I.grabCurrentUrl();
    I.assertContain(currentUrl, bankersUrl, `Redirected url ${currentUrl} is not correct`);
    I.see('This file is accessible only for users in Bankari group');
    I.switchToPreviousTab();
    I.closeOtherTabs();

    I.click('//a[text()="/files/zaheslovane/for-bankers.txt"]');
    I.switchToNextTab();

    I.waitForElement('.elfinder-cwd-wrapper', 20);
    within('.elfinder-cwd-wrapper', () => {
        I.waitForElement('#iwcm_1_L2ZpbGVzL3phaGVzbG92YW5lL2Zvci1iYW5rZXJzLnR4dA_E_E', 10);
    });
});

Scenario('Searchall - permissions', async ({ I, DT, DTE }) => {
    openUserPermissions(I, DT, DTE, 'ftester');
    await togglePermission(I, '#perms_menuWebpages_anchor', false);
    await togglePermission(I, '#perms_edit_text_anchor', false);
    DTE.save();

    I.relogin('ftester');
    I.dontSeeElement('.btn.btn-sm.btn-outline-secondary.js-search-toggler');

    I.relogin('admin');
    openUserPermissions(I, DT, DTE, 'ftester');
    await togglePermission(I, '#perms_menuWebpages_anchor', true);
    await togglePermission(I, '#perms_edit_text_anchor', false);
    DTE.save();

    I.relogin('ftester');
    I.seeElement('.btn.btn-sm.btn-outline-secondary.js-search-toggler');
    I.amOnPage('/admin/v9/search/index/');
    I.seeElement('#pills-webPages-tab');
    I.seeElement('#pills-files-tab');
    I.dontSeeElement('#pills-translationKeys-tab');

    I.relogin('admin');
    openUserPermissions(I, DT, DTE, 'ftester');
    await togglePermission(I, '#perms_menuWebpages_anchor', false);
    await togglePermission(I, '#perms_edit_text_anchor', true);
    DTE.save();

    I.relogin('ftester');
    I.seeElement('.btn.btn-sm.btn-outline-secondary.js-search-toggler');
    I.amOnPage('/admin/v9/search/index/');
    I.dontSeeElement('#pills-webPages-tab');
    I.dontSeeElement('#pills-files-tab');
    I.seeElement('#pills-translationKeys-tab');

});

Scenario('Revert permissions of ftester', async ({ I, DT, DTE }) => {
    openUserPermissions(I, DT, DTE, 'ftester');
    await togglePermission(I, '#perms_menuWebpages_anchor', true);
    await togglePermission(I, '#perms_edit_text_anchor', true);
    DTE.save();
});

function switchDomainTo(I, DT, domain) {
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText(domain));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");
    I.wait(2);
    DT.waitForLoader();
}

function openUserPermissions(I, DT, DTE, user) {
    I.amOnPage('/admin/v9/users/user-list');
    DT.waitForLoader();
    DT.filterContains('login', user);
    I.click('.buttons-select-all');
    I.clickCss('#datatableInit_wrapper button.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
}

function filter(I, DT, value){
    DT.waitForLoader();
    DT.waitForLoader("filesDataTable");
    DT.waitForLoader("translationKeysDataTable");

    I.fillField('#searchText', value);
    I.clickCss('#searchDataTable_extfilter .filtrujem');

    DT.waitForLoader();
    DT.waitForLoader("filesDataTable");
    DT.waitForLoader("translationKeysDataTable");
}

async function getTotalRecords(I, selector) {
    const text = await I.grabTextFrom(selector);
    I.say("getTotalRecords: text="+text+" selector="+selector);
    const match = text.match(/z (\d+)$/);
    return match ? parseInt(match[1], 10) : 0;
}

async function togglePermission(I, selector, shouldCheck) {
    const classes = await I.grabAttributeFrom(selector, 'class');
    const isChecked = classes.includes('jstree-clicked');

    if (shouldCheck && !isChecked) {
        await I.click(selector);
    } else if (!shouldCheck && isChecked) {
        await I.click(selector);
    }
}



async function verifyRecordCountChange(I, DT, column, value, tableWrapper) {
    const tableInfo = tableWrapper.replace('wrapper', 'info');
    const recordsBefore = await getTotalRecords(I, tableInfo);
    DT.filterContains(column, value);

    DT.waitForLoader();
    DT.waitForLoader("filesDataTable");
    DT.waitForLoader("translationKeysDataTable");

    const recordsAfter = await getTotalRecords(I, tableInfo);
    I.assertAbove(recordsBefore, recordsAfter, `Target data after filter on ${column} is not less than the original value`);
}

function verifyHeaders(I, headers, visibleWrapper, hiddenWrappers) {
    for (const header of headers) {
        I.waitForText(header, 'tr > th');
    }
    I.seeElement(visibleWrapper);
    for (const wrapper of hiddenWrappers) {
        I.dontSeeElement(wrapper);
    }
}

async function removeAllFilters(I) {
    const filterSelector = '[id^="dt-filter-labels-link-"] > span';

    while (await I.grabNumberOfVisibleElements(filterSelector) > 0) {
      I.click(filterSelector);
      I.waitForInvisible(filterSelector, 5);
    }

    I.say('All filters have been removed.');
  }