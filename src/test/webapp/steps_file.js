const moment = require("moment");
const buttons = require('./pages/buttons.js')

module.exports = function () {
  return actor({

    getConfLng() {
      var lng = process.env.CODECEPT_LNG;
      if (typeof lng == "undefined" || lng == null || lng == "") lng = "sk";
      return lng;
    },

    getDefaultPassword() {
      return process.env.CODECEPT_DEFAULT_PASSWORD;
    },

    getDefaultAuthToken() {
      return process.env.CODECEPT_DEFAULT_AUTH_TOKEN;
    },

    getDefaultDomainName() {
      return process.env.CODECEPT_DEFAULT_DOMAIN_NAME;
    },

    // Define custom steps here, use 'this' to access default methods of I.
    // It is recommended to place a general 'login' function here.
    fillAreaField(area, generateRandomNum) {
      area.w = generateRandomNum(600);
      area.h = generateRandomNum(600);
      area.x = generateRandomNum(100);
      area.y = generateRandomNum(50);
      this.fillField({ id: "w" }, area.w);
      this.wait(2);
      this.fillField({ id: "h" }, area.h);
      this.wait(2);
      this.fillField({ id: 'x' }, area.x);
      this.wait(2);
      this.fillField({ id: 'y' }, area.y);
      this.wait(2);
    },

    seeAndClick(name) {
      this.see(name);
      this.click(name);
    },

    /**
     * Naformatuje zadany timestamp do datumu a casu podla nastavenia moment v stranke
     * @param timestamp
     */
    formatDateTime(timestamp) {
      //const I = this.helpers['Playwright'];
      /*var formatted = I.executeScript((timestamp) => {
            //console.log("execScript, timestamp=", timestamp);
            return window.moment(timestamp).format("L HH:mm:ss");
        }, timestamp
      );*/
      //TODO: nefunguje hore uvedeny kod (dropne session/cookies) takze treba fixnut tento problem
      var userLng = "sk";
      moment.updateLocale(userLng, { invalidDate: "" });
      moment.locale(userLng);
      return moment(timestamp).format("L HH:mm:ss");
      //return "15.10.2020 17:40:30";
    },

    /**
     * Naformatuje zadany timestamp do datumu a casu podla nastavenia moment v stranke
     * @param timestamp
     */
    formatDate(timestamp) {
      var userLng = "sk";
      moment.updateLocale(userLng, { invalidDate: "" });
      moment.locale(userLng);
      return moment(timestamp).format("L");
      //return "15.10.2020 17:40:30";
    },

    /**
     * Parses a date/time string using moment and returns a Date object.
     * @param {string} dateTimeStr - The date/time string to parse.
     * @returns {Date|null} - The parsed Date object, or null if invalid.
     */
    parseDateTime(dateTimeStr) {
      var userLng = "sk";
      moment.updateLocale(userLng, { invalidDate: "" });
      moment.locale(userLng);
      // Try to parse using the same format as formatDateTime
      var m = moment(dateTimeStr, "L HH:mm:ss", true);
      if (m.isValid()) {
        return m.toDate();
      }
      // Try parsing without seconds if needed
      m = moment(dateTimeStr, "L HH:mm", true);
      if (m.isValid()) {
        return m.toDate();
      }
      // Return null if parsing failed
      return null;
    },

    wjSetDefaultWindowSize() {
      this.resizeWindow(1280, 760);
    },

    // odhlasenie z aplikacie
    logout() {
      //this.refreshPage();
      this.amOnPage('/logoff.do?forward=/admin/logon/');
      this.waitUrlEquals('/admin/logon/', 10);
    },

    //prihlasenie usera, z nejakeho dovodu vramci scenarov nefunguje dobre autologin plugin
    relogin(user, autologoff=true, waitForText=true, language = undefined) {
      //lebo v codecept_conf mame admina, ale realne je to user tester
      if ("admin"===user) user = "tester";
      if (autologoff) {
        //start in new fresh tab
        this.openNewTab();
        this.closeOtherTabs();
        this.amOnPage('/logoff.do?forward=/admin/logon/');
      }

      this.switchTo();

      //Select language if not default
      let activeLanguage = language !== undefined ? language : this.getConfLng();
      let helper = "Pomocník";

      if ("sk" !== activeLanguage) {
        // Different language detected, selecting language
        if ("en" === activeLanguage) {
          this.selectOption("language", "English");
          helper = "Help";
        } else if ("cs" === activeLanguage) {
          this.selectOption("language", "Česky");
          helper = "Nápověda";
        }
      }

      //aby sme vzdy v kazdom scenari mali prednastavenu velkost okna
      //odosli prihlasenie
      this.fillField("username", user);
      this.fillField("password", secret(this.getDefaultPassword()));
      this.click("login-submit");
      if (waitForText===true) this.waitForText(helper, 10);
    },

    amOnPageLng(url) {
      var lng = this.getConfLng();
      if (lng != "sk") {
        if (url.indexOf("?")!=-1) {
          url += "&language="+lng;
        } else {
          url += "?language="+lng;
        }
      }
      this.amOnPage(url);
    },

    //vygenerovanie nahodneho retazca
    getRandomText() {
      const startDate = new Date();
      //let randomText = startDate.toISOString().replace(/T/gi, ' ').replace(/\..+/gi, '').replace(/:/gi, '').replace(/ /gi, '-');
      var userLng = "sk";
      moment.updateLocale(userLng, { invalidDate: "" });
      moment.locale(userLng);
      let randomText = moment(startDate).format("YY-MM-DD-HHmmss");
      randomText += "-" + Math.floor(Math.random() * 1000);
      return randomText;
    },

    getRandomTextShort() {
      const startDate = new Date();
      //let randomText = startDate.toISOString().replace(/T/gi, ' ').replace(/\..+/gi, '').replace(/:/gi, '').replace(/ /gi, '-');
      var userLng = "sk";
      moment.updateLocale(userLng, { invalidDate: "" });
      moment.locale(userLng);
      let randomText = moment(startDate).format("MMDD");
      randomText += Math.floor(Math.random() * 100);
      return randomText;
    },

    waitForLoader(selector) {
      this.waitForInvisible(selector, 20);
      //ak by sa este nestihol zobrazit kym sa vykona prva podmienka, pre istotu pockame a potom skusime znova
      this.wait(1);
      this.waitForInvisible(selector, 20);
      this.wait(1);
    },

    async waitForTime(time) {
      var failsafe = 0;
      var userLng = "sk";
      moment.updateLocale(userLng, { invalidDate: "" });
      moment.locale(userLng);
      while (failsafe++ < 300) {
        this.wait(1);
        var now = await this.executeScript(function(){ return new Date().getTime(); });
        if (now > time) return;
        this.say("Waiting until "+moment(time).format("HH:mm:ss")+" now="+moment(now).format("HH:mm:ss"));
      }
    },

    toastrClose() {
      var selector = "#toast-container-webjet button.toast-close-button";
      this.waitForElement(selector, 10);
      this.forceClick({css: selector});
    },

    clickCss(name, parent=null) {
      if (parent == null) {
        this.click({css: name});
      } else {
        this.click({css: parent + " " + name});
      }
    },

    forceClickCss(name, parent=null) {
      if (parent == null) {
        this.forceClick({css: name});
      } else {
        this.forceClick({css: parent + " " + name});
      }
    },

    //-------------- FUNKCIE PRE DATATABLE --------------

    dtWaitForLoader(name) {
      if (typeof name == "undefined") { name = "div.dt-processing"; }
      this.waitForInvisible(name, 40);
      //ak by sa este nestihol zobrazit kym sa vykona prva podmienka, pre istotu pockame a potom skusime znova
      this.wait(0.5);
      this.waitForInvisible(name, 40);
      this.wait(0.5);
    },

    dtEditorWaitForLoader() {
      let name = "div.DTE_Processing_Indicator";
      this.waitForInvisible(name, 200);
      this.wait(0.5);
    },

    dtWaitForEditor(name) {
      if (typeof name == "undefined") { name = "datatableInit"; }
      this.waitForVisible("#" + name + "_modal", 60);
      this.wait(1);
    },

    waitForModal(id) {
      this.waitForVisible("#" + id);
      this.wait(1);
    },

    waitForModalClose(id) {
      this.waitForInvisible("#" + id);
      this.wait(1);
    },

    dtFilter(name, value) {
      if (name.indexOf(".")!=-1) name = name.replace(/\./gi, "\\.");
      this.fillField({ css: "div.dt-scroll-headInner input.dt-filter-" + name }, value);
      this.click({ css: "div.dt-scroll-headInner button.dt-filtrujem-" + name });
      this.dtWaitForLoader();
    },

    dtFilterSelect(name, value) {
      if (name.indexOf(".")!=-1) name = name.replace(/\./gi, "\\.");
      this.click({ css: "div.dt-scroll-headInner div.dt-filter-" + name + " button.btn-outline-secondary" });
      this.click(locate('div.dropdown-menu.show .dropdown-item').withText(value));
      this.click({ css: "div.dt-scroll-headInner button.dt-filtrujem-" + name });
      this.dtWaitForLoader();
    },

    dtFillField(name, value) {
      this.fillField("#DTE_Field_"+name, value);
    },

    /**
     * Vyplni hodnotu do WYSIWYG editora Quill
     * @param {*} name - meno pola
     * @param {*} value - hodnota na vyplnenie, POZOR, nevie to zatial diakritiku z dovodu pouzitia type prikazu
     */
    dtFillQuill(name, value) {
      this.click(locate('p').inside("#DTE_Field_"+name));
      this.wait(0.3);
      this.pressKey(['CommandOrControl', 'A']);
      this.wait(0.3);
      this.pressKey('Backspace');
      this.wait(0.3);
      /*var i;
      for (i=0; i<value.length; i++) {
        this.pressKey(value.charAt(i));
      }*/
      //this.fillField(locate('p').inside("#DTE_Field_"+name), value);
      this.type(value, 50)
    },

    /**
     * Vyplni hodnotu do WYSIWYG editora Cleditor
     * @param {*} parentSelector - selector parent elementu
     * @param {*} value - hodnota na vyplnenie, POZOR, nevie to zatial diakritiku z dovodu pouzitia type prikazu
     */
    dtFillCleditor(parentSelector, value) {
      this.click(parentSelector+" div.cleditorMain", null, { position: { x: 10, y: 30 } });
      this.wait(0.3);
      this.pressKey(['CommandOrControl', 'A']);
      this.wait(0.3);
      this.pressKey('Backspace');
      this.wait(0.3);
      this.type(value, 50);
    },

    //vybere hodnotu v selecte v DT editore
    dtEditorSelectOption(name, text) {
      this.click({ css: "div.modal-dialog div.DTE_Field_Name_" + name + " button.dropdown-toggle" });
      this.waitForElement(locate('div.dropdown-menu.show .dropdown-item').withText(text), 5);
      this.click(locate('div.dropdown-menu.show .dropdown-item').withText(text));
      this.wait(0.3);
    },

    //ulozi editor a pocka na vysledok
    dtEditorSave() {
      //ideme cez slector, pretoze to moze byt Pridat (novy zaznam) alebo Ulozit (existujuci)
      this.click({ css: "div.DTED.show div.DTE_Footer.modal-footer button.btn.btn-primary" });
      this.dtEditorWaitForLoader();
      //cakaj na reload datatabulky
      this.dtWaitForLoader();
    },

    dtEditorCancel() {
      this.click({ css: "div.DTED.show div.DTE_Footer.modal-footer button.btn-close-editor" });
      this.dtEditorWaitForLoader();
    },

    //zresetuje nastavenie datatabulky (poradie stlpcov, usporiadanie, zoznam stlpcov)
    dtResetTable(name = "datatableInit") {
      var container = "#"+name+"_wrapper";
      this.wait(1); //must be here otherwise on webpages it will not reset sorting correctly after load
      this.dtWaitForLoader();
      this.click(container+" button.buttons-settings");
      this.click(container+" button.buttons-colvis");
      this.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
      this.clickCss(container+" div.colvispostfix_wrapper button.buttons-colvisRestore");
      this.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
      this.dtWaitForLoader();
    },

    //-------------- FUNKCIE PRE JSTREE --------------
    jstreeClick(name, strict = false) {
      //pockaj na jstreeLoader
      this.jstreeWaitForLoader();

      if (strict) {
        const exactMatchXPath = `.//*[@id='SomStromcek']//a[contains(concat(' ', normalize-space(@class), ' '), ' jstree-anchor ') and normalize-space(text())='${name}']`;
        this.click(locate({ xpath: exactMatchXPath }));
      } else {
        this.click(locate("#SomStromcek a.jstree-anchor").withText(name));
      }

      //pockaj na nacitanie datatable
      this.dtWaitForLoader();
    },

    //postupne otvori stromovu strukturu zadanej cesty, napr. ["Jet portal 4", "Kontakt"]
    jstreeNavigate(pathArray) {
      var that = this;
      pathArray.forEach(function (name, index) {
        that.say("clicking on jstreitem: name=" + name + " index=" + index);
        that.jstreeClick(name);
      });
    },

    jstreeWaitForLoader() {
      let name = "li.jstree-loading";
      this.waitForInvisible(name, 40);
      this.wait(0.5);
      this.waitForInvisible(name, 40);
      this.wait(0.3);
    },

    jstreeReset() {
      this.click("button.buttons-jstree-settings");
      this.waitForElement("#jstreeSettingsModal");
      this.uncheckOption("#jstree-settings-showid");
      this.uncheckOption("#jstree-settings-showorder");
      this.uncheckOption("#jstree-settings-showpages");

      //set first value in select
      let name = "treeSortType";
      this.click({ css: "#jstreeSettingsModal div.DTE_Field_" + name + " button.dropdown-toggle" });
      let firstOption = "div.dropdown-menu.show ul li:first-child .dropdown-item";
      this.waitForElement(locate(firstOption), 5);
      this.waitForEnabled(locate(firstOption), 5);
      this.click(firstOption);
      this.wait(0.3);

      this.checkOption("#jstree-settings-treeSortOrderAsc");

      this.click("#jstree-settings-submit");

      this.jstreeWaitForLoader();
    },

    jstreeFilter(value, type=null){
      this.fillField('#tree-folder-search-input', value);
      if (type != null) {
        this.click({ css: "#jstreeSearchTable div.filter-input-prepend button.btn-outline-secondary"});
        this.click(locate('div.dropdown-menu.show .dropdown-item').withText(type));
      }
      this.clickCss('#tree-folder-search-button');
      this.wait(0.3);
      this.waitForInvisible("div.dt-processing", 40);
    },

    //vo vlastnostiach adresaru nastavi parent adresar na korenovy
    groupSetRootParent() {
      let rootgroupName;
      switch (this.getConfLng()) {
          case 'en':
              rootgroupName = "Root folder";
              break;
          case 'cs':
              rootgroupName = "Kořenový adresář";
              break;
          default:
              rootgroupName = "Koreňový priečinok";
              break;
      }

      this.clickCss('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'); // zmena na korenovy adresar
      this.waitForElement("div.jsTree-wrapper");
      this.wait(1);
      this.waitForText(rootgroupName, 5);
      this.wait(1);
      this.click(rootgroupName, 'div.jsTree-wrapper');
      this.wait(1);
      this.waitForValue('#editorAppDTE_Field_editorFields-parentGroupDetails .input-group input', '/', 10);
    },

    // vytvorenie korenoveho adresara s dvomi podadresarmi
    createFolderStructure(randomNumber, alsoSubfolders=true) {
      //reset DT sorting/columns
      this.dtResetTable("datatableInit");

      // premenne
      var auto_name = 'name-autotest-' + randomNumber;
      var auto_subfolder_one = 'subone-autotest-' + randomNumber;
      var auto_subfolder_two = 'subtwo-autotest-' + randomNumber;
      // vytvorenie materskeho priecinka name-autotest
      this.say('Pridanie noveho priecinka ' + auto_name);
      this.waitForElement(buttons.btn.tree_add_button, 10);
      this.click(buttons.btn.tree_add_button);
      this.dtWaitForEditor("groups-datatable");
      this.fillField('#DTE_Field_groupName', auto_name);
      this.groupSetRootParent();
      this.wait(1);
      this.dtEditorSave();
      this.waitForText(auto_name, 20);
      this.wait(1);
      this.jstreeClick(auto_name);
      if (alsoSubfolders) {
        // vytvorenie 1. podpriecinka
        this.say('Pridanie noveho podpriecinka ' + auto_subfolder_one);
        this.click(buttons.btn.tree_add_button);
        this.dtWaitForEditor("groups-datatable");
        this.fillField('#DTE_Field_groupName', auto_subfolder_one);
        this.wait(1);
        this.dtEditorSave();
        this.waitForText(auto_subfolder_one, 20);
        this.wait(1);
        // vytvorenie 2. podpriecinka
        this.say('Pridanie noveho podpriecinka ' + auto_subfolder_two);
        this.jstreeClick(auto_name);
        this.click(buttons.btn.tree_add_button);
        this.dtWaitForEditor("groups-datatable");
        this.fillField('#DTE_Field_groupName', auto_subfolder_two);
        this.wait(1);
        this.dtEditorSave();
        this.waitForText(auto_subfolder_two, 20);
        this.wait(1);
      }
    },

    // vymazanie priecinka
    deleteFolderStructure(randomNumber) {
      var auto_name = 'name-autotest-' + randomNumber;
      this.say('Zmazanie priecinka name-autotest');
      this.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
      this.waitForText('Webové stránky', 10);
      this.click(locate('.jstree-anchor').withText(auto_name));
      this.dtWaitForLoader();
      this.wait(1);
      this.click(buttons.btn.tree_delete_button);
      this.wait(1);
      this.waitForText('Zmazať', 10);
      this.wait(1);
      this.click('Zmazať');
      this.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
      this.wait(1);
      this.waitForElement('.jstree-anchor', 20);
      this.dtWaitForLoader();
      this.waitForInvisible(locate('.jstree-anchor').withText(auto_name), 10);
    },

    // ------------ FUNKCIE PRE WEB STRÁNKY ----------------
    createNewWebPage(randomNumber) {
      // premenne
      var auto_webPage = 'webPage-autotest-' + randomNumber;
      // vytvorenie webstranky
      this.say('Pridanie novej web stranky ' + auto_webPage);
      this.waitForElement(buttons.btn.add_button, 10);
      this.wait(1);
      this.click(buttons.btn.add_button);
      this.dtWaitForEditor();
      this.clickCss('#pills-dt-datatableInit-basic-tab');
      this.waitForElement('#DTE_Field_title');
      this.forceClick('#DTE_Field_title');
      this.clearField('#DTE_Field_title');
      this.fillField('#DTE_Field_title', auto_webPage);
      this.forceClick('#DTE_Field_navbar');
      this.clearField('#DTE_Field_navbar');
      this.fillField('#DTE_Field_navbar', auto_webPage);
      // vypni zobrazenie web stranky a vyhladanie cez vyhladavace
      this.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
      this.click(locate('.custom-control.form-switch').withChild('#DTE_Field_searchable_0').find('.form-check-label'));
      // Pridanie textu do obsahu
      this.clickCss('#pills-dt-datatableInit-content-tab');
      this.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
      this.waitForElement("span.cke_toolbox");
      this.wait(2);
      this.waitForElement('#trEditor', 10);
      this.clickCss('#trEditor');
      //this.pressKey('ArrowLeft');
      //this.pressKey('ArrowRight');
      this.type('<!-- This is an autotest -->');
      // Nastav datumy v perex tabe
      this.clickCss('#pills-dt-datatableInit-perex-tab');
      this.fillField('#DTE_Field_publishStartDate', "01.01.2022 00:00:00");
      this.fillField('#DTE_Field_publishEndDate', "03.01.2022 00:00:00");
      this.fillField('#DTE_Field_eventDateDate', "02.01.2022 00:00:00");
      // Ulozenie
      this.click(locate('#datatableInit_modal').find('button.btn.btn-primary'));
      this.dtEditorWaitForLoader();
      this.waitForText("webPage-autotest-" + randomNumber, 20);
      this.wait(1);
    },

    editCreatedWebPage(randomNumber) {
      // premenne
      var auto_webPage = 'webPage-autotest-' + randomNumber;
      var edit_webpage = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning'));
      // editovanie web stranky
      this.say('Editovanie web stranky ' + auto_webPage);
      this.waitForElement(locate('#datatableInit_wrapper .dt-scroll-body .dt-row-edit').withText(auto_webPage), 10);
      this.forceClick(locate('#datatableInit_wrapper .dt-scroll-body tr').withText(auto_webPage).find('.dt-select-td.sorting_1'));
      this.waitForText('1 riadok označený', 10);
      this.click(edit_webpage);
      this.dtWaitForEditor();
      this.waitForText('Obsah', 10);
      this.switchTo('.cke_wysiwyg_frame.cke_reset');
      this.waitForText('is an autotest', 10); //bez this, pretoze to sa potom zmeni na Toto
      this.switchTo();
      this.clickCss('#pills-dt-datatableInit-basic-tab');
      this.waitForText(auto_webPage, 10);
    },

    checkEditedWebPage(randomNumber) {
      // premenne
      var auto_webPage = 'webPage-autotest-' + randomNumber;
      var edit_webpage = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning'));
      var auto_name = 'name-autotest-' + randomNumber;
      // Kontrola editovanej stranky
      this.say('Kontrola editovanej web stranky ' + auto_webPage);
      this.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
      this.waitForVisible('#SomStromcek', 20);
      this.seeAndClick(auto_name);
      this.waitForElement(locate('.even').withText(auto_webPage), 10);
      //toto netreba, oznaci to click pod tymto this.click(locate('.even').find('.dt-select-td.sorting_1'));
      this.forceClick(locate('.even').withText(auto_webPage).find('.dt-select-td.sorting_1'));
      this.waitForText('1 riadok označený', 10);
      this.click(edit_webpage);
      this.dtWaitForEditor();
      this.waitForText('Obsah', 10);
    },

    deleteCreatedWebPage(randomNumber, waitForInvisible=true) {
      this.dtWaitForLoader();
      // premenne
      var auto_webPage = 'webPage-autotest-' + randomNumber;
      var delete_webpage = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider'));
      // vymazanie webstranky
      this.say('Vymazanie web stranky ' + auto_webPage);
      this.waitForElement(locate('#datatableInit .even').withText(auto_webPage), 10);
      //this.click(locate('.even').find('.dt-select-td.sorting_1'));
      this.forceClick(locate('#datatableInit .even').withText(auto_webPage).find('.dt-select-td.sorting_1'));
      this.waitForElement(locate('#datatableInit .even.selected').withText(auto_webPage), 10);
      this.waitForText('1 riadok označený', 10);
      this.click(delete_webpage);
      this.dtWaitForEditor();
      this.waitForText('Zmazať', 10);
      this.click('Zmazať');
      this.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
      this.wait(1);
      this.waitForElement('#datatableInit .odd.is-default-page', 20);
      if (waitForInvisible) this.waitForInvisible(locate('#datatableInit .even').withText(auto_webPage), 10);
      this.wait(1);
    },

    // ---------------------- Structure MIRRORING ---------------------------------------------------
    // vytvorenie zakladnej struktury adresarov v sk a en
    wj9CreateMirroringFolder(randomNumber, alsoDe=false) {
      var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
      var auto_folder_en = 'en-mir-autotest-' + randomNumber;
      var auto_folder_de = 'de-mir-autotest-' + randomNumber;
      var domainName = 'mirroring.tau27.iway.sk';

      // prepnutie na domenu mirroring.tau27.iway.sk
      this.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=9811");
      this.dtWaitForLoader();

      // vytvorenie materskeho priecinka en-autotest/de-autotest
      var folders = [auto_folder_sk, auto_folder_en];
      if (alsoDe) folders = [auto_folder_sk, auto_folder_en, auto_folder_de];
      var index = 0;
      for (var folder of folders) {
        this.say('Pridanie noveho priecinka ' + folder);
        this.click(buttons.btn.tree_add_button);
        this.dtWaitForEditor("groups-datatable");
        this.fillField('#DTE_Field_groupName', folder);
        //vyber korenovy adresar
        this.groupSetRootParent();
        //nastav domenu
        this.wait(0.5);
        this.waitForElement("div.DTE_Field_Name_domainName");
        this.fillField('#DTE_Field_domainName', domainName);
        //nastav poradie usporiadanie
        this.clickCss("#pills-dt-groups-datatable-menu-tab");
        this.fillField("#DTE_Field_sortPriority", ""+(2000+(index*10)));
        this.dtEditorSave();
        index++;
      }

      //over zobrazenie
      this.waitForText(auto_folder_sk, 10, "#SomStromcek");
      this.waitForText(auto_folder_en, 10, "#SomStromcek");
      if (alsoDe) this.waitForText(auto_folder_de, 10, "#SomStromcek");
    }
  });
}