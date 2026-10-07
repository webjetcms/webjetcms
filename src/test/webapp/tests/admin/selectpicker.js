Feature('admin.selectpicker');

/**
 * Otestovanie problemov so selectpickerom, ktory sa nedal zmenit ked sa:
 * - preslo na dalsiu stranku v zozname
 * - nastavil sa filter a nasledne sa zmazal
 * - otvorilo sa popup okno DT editora, zatvorilo a otvorilo sa znova
 */

Before(({ I, login }) => {
    login('admin');
});

function clearFilter(I, name) {
    I.clickCss("#dt-filter-labels-link-"+name);
    I.wait(2);
}

Scenario('datatables header select', ({ I, DT, DTE }) => {
    var key = "Demo JET";

    I.say("odfiltruj data");
    I.amOnPage("/admin/v9/templates/temps-groups-list/");

    DT.filterContains("name", key);

    I.see('InterWay Developer SK');
    I.dontSee('Developer CZ');

    I.say("otvor popup okno");
    I.click(key);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-metadata-tab");

    I.seeInField("#DTE_Field_projectDeveloper", 'InterWay Developer SK');
    DTE.cancel();

    I.say("prepni jazyk v hlavicke");

    I.click({css: "div.breadcrumb-language-select"});
    I.click(locate('.dropdown-item').withText("Český jazyk"));
    DT.waitForLoader();

    I.see('Developer CZ');
    I.dontSee('InterWay Developer SK');

    I.click(key);
    DTE.waitForEditor();

    I.seeInField("#DTE_Field_projectDeveloper", 'Developer CZ');
    DTE.cancel();
});

Scenario('nastavenie filtra, zmazanie filtra', ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-search/");

    I.say("vyber moznost INIT a odfiltruj data");
    DT.filterSelect("logType", "INIT");
    clearFilter(I, "logType");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("logType", "Helpdesk");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    I.say("skus prejst na stranku 5 a vyskusaj zmenit selector");
    I.click({css: "ul.pagination li:nth-child(6) button"});
    I.wait(2);
    DT.filterSelect("logType", "Helpdesk");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("logType", "Helpdesk");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    //
    I.say("Check set filter not clicking on search button");
    I.amOnPage("/admin/v9/apps/audit-search/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-logType button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("USER_LOGON"));
    DT.filterContains("description", "node");
    I.see("USER_LOGON", "#datatableInit tbody");
    I.dontSee("CRON", "#datatableInit tbody");

    I.say("Clear node value, check logType is applyed");
    DT.filterContains("description", "");
    I.see("USER_LOGON", "#datatableInit tbody");
    I.dontSee("CRON", "#datatableInit tbody");

    //
    I.say("Check boolean filter");
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-mustAccepted button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Áno"));
    DT.filterContains("name", "test");
    I.see("Test", "#reservationObjectDataTable tbody");
    I.dontSee("testB", "#reservationObjectDataTable tbody");
    I.dontSee("Zasadačka veľká", "#reservationObjectDataTable tbody");

    I.say("Clear name value, check mustAccepted is applyed");
    DT.filterContains("name", "");
    I.see("Test", "#reservationObjectDataTable tbody");
    I.dontSee("testB", "#reservationObjectDataTable tbody");
    I.dontSee("Zasadačka veľká", "#reservationObjectDataTable tbody");
});

Scenario('nastavenie filtra LOCAL, zmazanie filtra LOCAL', ({ I, DT }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");

    I.say("vyber moznost Anglicky a odfiltruj data");
    DT.filterSelect("lng", "Anglický");
    I.see("Záznamy 1 až 2 z 2");
    clearFilter(I, "lng");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("lng", "Španielsky");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "lng");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("lng", "Ruský");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "lng");

    //
    I.say("Check set filter not clicking on search button");
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-templatesGroupId button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Demo JET"));
    DT.filterContains("availableGrooupsList", "jet");
    I.see("Demo JET", "#datatableInit tbody");
    I.see("Microsite - blue", "#datatableInit tbody");

    I.say("Clear jet value, check templatesGroupId is applyed");
    DT.filterContains("availableGrooupsList", "");
    I.see("Demo JET", "#datatableInit tbody");
    I.dontSee("nepriradené", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");

    //
    I.say("Check boolean filter");
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-disableSpamProtection button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Áno"));
    DT.filterContains("tempName", "yellow");
    I.see("Microsite - yellow", "#datatableInit tbody");

    I.say("Clear tempName value, check disableSpamProtection is applyed");
    DT.filterContains("tempName", "");
    I.see("Microsite - yellow", "#datatableInit tbody");
    I.dontSee("nepriradené", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");
});

Scenario('Check contains,startswith,endswith,equals', ({ I, DT }) => {
    //
    I.say("LOCAL search");
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContainsForce("tempName", "Newsletter");
    I.see("Newsletter", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filterStartsWith("tempName", "Newsletter");
    I.see("Newsletter", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filterEndsWith("tempName", "EN");
    I.see("Subpage EN", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filterEquals("tempName", "Newsletter");
    I.see("Newsletter", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    //
    I.say("SERVER side search");
    I.amOnPage("/admin/v9/webpages/media/");
    DT.filterContainsForce("mediaLink", ".sk");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.see("www.pluska.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filterStartsWith("mediaLink", "s");
    I.see("sdasdfasdf", "#mediaTable tbody");
    I.dontSee("www.sme.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filterEndsWith("mediaLink", ".sk");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.see("www.pluska.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filterEquals("mediaLink", "www.sme.sk");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.dontSee("www.sme.sk/en", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");
});

Scenario('BUG-zobrazenie selectov vo vnorenej DT', async ({ I, DTE }) => {
    const filter = '#datatableFieldDTE_Field_docDetailsList_wrapper th.dt-th-title div.input-group';
    // Reopening the editor used to stretch the nested filter's selectpicker and clip its icon.
    async function checkNestedFilter() {
        I.clickCss('#pills-dt-userGroupsDataTable-sites-tab');
        I.waitForVisible(`${filter} button.dropdown-toggle`, 10);
        I.waitForFunction(selector => {
            const group = document.querySelector(selector);
            const button = group.querySelector('button.dropdown-toggle').getBoundingClientRect();
            const input = group.querySelector('input.filter-input').getBoundingClientRect();
            return button.width >= 24 && button.width <= 40 && Math.abs(button.height - input.height) <= 1;
        }, [filter], 10);
        I.assertTrue(await I.executeScript(selector => {
            const group = document.querySelector(selector);
            const button = group.querySelector('button.dropdown-toggle').getBoundingClientRect();
            const icon = group.querySelector('button .filter-option i');
            const bounds = icon.getBoundingClientRect();
            return !['none', 'normal', '""'].includes(getComputedStyle(icon, '::before').content)
                && bounds.width > 0 && bounds.left >= button.left && bounds.right <= button.right
                && bounds.top >= button.top && bounds.bottom <= button.bottom;
        }, filter), 'The nested filter icon must render completely inside its compact button.');
        I.clickCss(`${filter} button.dropdown-toggle`);
        I.waitForVisible('div.dropdown-menu.show', 10);
        I.click(locate('div.dropdown-menu.show .dropdown-item').withChild('span > i.ti-arrow-right-bar'));
        I.seeInField(`${filter} select.filter-input-prepend`, 'startwith');
        I.seeElement(`${filter} button .ti-arrow-right-bar`);
        I.clickCss(`${filter} button.dropdown-toggle`);
        I.click(locate('div.dropdown-menu.show .dropdown-item').withChild('span > i.ti-arrows-horizontal'));
        I.seeInField(`${filter} select.filter-input-prepend`, 'contains');
        I.seeElement(`${filter} button .ti-arrows-horizontal`);
    }

    I.amOnPage("/admin/v9/users/user-groups/?id=2");
    DTE.waitForEditor("userGroupsDataTable");
    await checkNestedFilter();

    DTE.cancel();
    I.click("Obchodní partneri");
    DTE.waitForEditor("userGroupsDataTable");
    await checkNestedFilter();
    DTE.cancel();
});

Scenario('BUG-set selectpickerbinded after fields visibility change', async ({ I, DT }) => {
    var selector = "#datatableInit_wrapper .dt-scroll-headInner th.dt-th-fieldA select.filter-input-prepend.selectpickerbinded";

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.resetTable();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=21686");
    DT.waitForLoader();
    I.jstreeClick("Voliteľné polia");
    await DT.showColumn("text - A");

    I.waitForElement(selector, 10);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=7625");

    I.waitForElement(selector, 10);
});

Scenario("reset table", ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.resetTable();
});

Scenario("BUG selectpicker BS 5.3 duplicate value", ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/users/user-groups/?id=1");
    DTE.waitForEditor("userGroupsDataTable");
    const field = ".DTE_Field_Name_userGroupType button.dropdown-toggle div.filter-option-inner-inner";
    const value1 = "Prístupov k zaheslovanej sekcii web sídla";
    const value2 = "Prihlásenie k hromadnému e-mailu"
    I.see(value1, field);
    I.dontSee(value2, field);
    DTE.cancel();
    I.click("Newsletter");
    DTE.waitForEditor("userGroupsDataTable");
    I.dontSee(value1, field);
    I.see(value2, field);
    DTE.cancel();
});

Scenario('Test add template in select-editable', ({ I, DT, DTE }) => {
    var randomNumber = I.getRandomTextShort();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    DT.waitForLoader();
    DT.filterContains("title", "Test pridania zaznamu v select-editable");
    DT.waitForLoader();

    I.click("Test pridania zaznamu v select-editable");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-template-tab");
    I.waitForElement("div.DTE_Field_Name_tempId button.btn-add", 10);
    I.clickCss("div.DTE_Field_Name_tempId button.btn-add");

    I.waitForElement("#modalIframe");
    I.wait(4);
    I.switchTo("#modalIframeIframeElement");

    var newTempName = "autotest-template-"+randomNumber;
    DTE.fillField("tempName", newTempName);
    DTE.fillField("forward", "jet/blank.jsp");
    I.pressKey("Enter");
    I.switchTo();
    I.wait("#modalIframe div.modal-footer button.btn-primary", 10);
    I.clickCss("#modalIframe div.modal-footer button.btn-primary");
    I.waitForText(newTempName, 10, "div.DTE_Field_Name_tempId div.filter-option div.filter-option-inner-inner");
});

Scenario('Cleanup template', ({ I, DT }) => {
    I.say("Deteting autotest templates");
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContains("tempName", "autotest-template-");
    DT.deleteAll()
});
