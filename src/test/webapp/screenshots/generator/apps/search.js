Feature('apps.search');

Before(({ login }) => {
    login('admin');
});

Scenario('vyhladavanie', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/vyhladavanie/?words=Kontakt&language="+I.getConfLng());
    Document.screenshot("/redactor/apps/search/search.png");

    Document.screenshotAppEditor(77875, "/redactor/apps/search/editor.png");
});

Scenario('Administration quick search', ({ I, Document }) => {
    const modal = '#admin-quick-search';
    const query = I.getConfLng() === 'en' ? 'banner system' : 'bannerový systém';

    I.amOnPage('/admin/v9/');
    I.resizeWindow(1440, 1100);
    I.waitForVisible('button.js-search-toggler', 10);
    Document.screenshotElement('.ly-header', '/redactor/admin/search/quick-search-trigger.png');

    I.clickCss('button.js-search-toggler');
    I.waitForVisible(modal, 10);
    I.waitForFunction(() => document.activeElement.id === 'admin-quick-search-input', 10);
    I.fillField('#admin-quick-search-input', query);
    I.pressKey('Enter');
    // Capture real results only after both the administration and documentation requests finish.
    I.waitForElement('#admin-quick-search-results[aria-busy="false"]', 90);
    I.dontSeeElement(`${modal} .md-quick-search__error`);
    // A result count also confirms a successful response when the localized index returns no matches.
    I.seeElement(`${modal} [data-scope="documentation"] .md-quick-search__count`);
    I.moveCursorTo(`${modal} .md-quick-search__input-row`);
    Document.screenshotElement(`${modal} .modal-content`, '/redactor/admin/search/quick-search.png');

    I.clickCss(`${modal} [data-scope="documentation"]`);
    I.waitForFunction(() => document.querySelector('#admin-quick-search [data-source="documentation"] .md-quick-search__documentation-result')
        || document.querySelector('#admin-quick-search .md-quick-search__empty'), 10);
    I.moveCursorTo(`${modal} .md-quick-search__input-row`);
    Document.screenshotElement(`${modal} .modal-content`, '/redactor/admin/search/quick-search-documentation.png');

    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    I.wjSetDefaultWindowSize();
});
