const { I } = inject();

/**
 * Funkcie pre pracu s datatabulkou
 */

module.exports = {

    waitForLoader(name) {
        if (typeof name == "undefined") { name = "div.dataTables_processing"; }
        I.waitForInvisible(name, 40);
        //ak by sa este nestihol zobrazit kym sa vykona prva podmienka, pre istotu pockame a potom skusime znova
        I.wait(0.3);
        I.waitForInvisible(name, 40);
        I.wait(0.2);
    },

    //skontroluje prava na datatabulke - zobrazi, odoberie pravo a overi zobrazenie chyby prav
    checkPerms(perms, url, datatableId=null) {
        let warningText = "Prístup k tejto stránke je zamietnutý";
        I.say("Testing permissions, perms=" + perms + " url=" + url + " warningText=" + warningText);
        I.amOnPage(url);
        this.waitForLoader();
        I.dontSee(warningText);

        let paramTokenizer = "?";
        if (url.indexOf("?")!=-1) paramTokenizer = "&";

        I.say("Zatvaram ostatne taby aby som sa vratil spravne");
        I.closeOtherTabs();
        I.say("Otvaram novy tab v ktorom odstranim prava");
        I.openNewTab();
        I.amOnPage(url + paramTokenizer + "removePerm=" + perms);
        I.closeCurrentTab();

        if (typeof datatableId != "undefined" && datatableId != null) I.click({ css: "#"+datatableId+"_wrapper button.buttons-refresh"});
        else I.click({css: "button.buttons-refresh"});

        I.say("Obnovujem datatabulku, overujem prava na REST sluzbu");
        I.waitForElement("#toast-container-webjet", 10);
        I.see(warningText);
        I.click({css: "#toast-container-webjet button.toast-close-button"});

        I.say("Overujem prava na zobrazenie celej stranky");
        I.amOnPage(url + paramTokenizer + "removePerm=" + perms);
        I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");

        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.waitUrlEquals('/admin/logon/', 10);
    },

    filter(name, value, type=null) {
        var nameType = name;
        if (name.indexOf(".") != -1) {
            nameType = name.replace(/\./gi, "-");
            name = name.replace(/\./gi, "\\.");
        }
        var value1 = value;
        if (value1.length>2) value1 = "**"; //just placeholder
        I.fillField({ css: "div.dataTables_scrollHeadInner input.dt-filter-" + name }, value1);
        //este raz, niekedy to nechyti prve pismena
        I.fillField({ css: "div.dataTables_scrollHeadInner input.dt-filter-" + name }, value);

        if (type != null) {
            I.click({ css: "div.dataTables_scrollHeadInner th.dt-th-" + nameType + " div.filter-input-prepend button.btn-outline-secondary" });
            I.click(locate('div.dropdown-menu.show .dropdown-item').withText(type));
        }

        //for date fields remove from- or to- prefix, there is only one search button
        if (name.startsWith("from-")) name = name.substring(5);
        if (name.startsWith("to-")) {
            name = name.substring(3);
            I.wait(2);
        }

        I.click({ css: "div.dataTables_scrollHeadInner button.dt-filtrujem-" + name });
        //this.waitForLoader();
        //this is faster
        I.wait(0.3);
        I.waitForInvisible(name, 40);
    },

    clearFilter(name) {
        I.click({ css: "#dt-filter-labels-link-" + name });
        this.waitForLoader();
    },

    filterSelect(name, value) {
        if (name.indexOf(".") != -1) name = name.replace(/\./gi, "\\.");
        I.click({ css: "div.dataTables_scrollHeadInner div.dt-filter-" + name + " button.btn-outline-secondary" });
        I.click(locate('div.dropdown-menu.show .dropdown-item').withText(value));
        I.click({ css: "div.dataTables_scrollHeadInner button.dt-filtrujem-" + name });
        this.waitForLoader();
    },

    //zresetuje nastavenie datatabulky (poradie stlpcov, usporiadanie, zoznam stlpcov)
    resetTable(name = "datatableInit") {
      var container = "#"+name+"_wrapper";
      this.waitForLoader();
      I.click(container+" button.buttons-settings");
      I.click(container+" button.buttons-colvis");
      I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
      I.click("Obnoviť");
      I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    },

    showColumn(columnText, tableId="datatableInit") {
        var container = "#"+tableId+"_wrapper";
        I.click(container+" button.buttons-settings");
        I.click(container+" button.buttons-colvis");
        I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
        I.click(columnText);
        I.click("button.btn.btn-primary.dt-close-modal");
        I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    },


    /**
     * Check cell value in selected row and column, index starts at 1
     * @param {*} name - table name/ID
     * @param {*} row - row number (starst at 1)
     * @param {*} col - col number (starts at 1)
     * @param {*} value - expected cell value
     */
    checkTableCell(name, row, col, value) {
        if (value == null || value == "") return;
        //we use waitForText instead of see, maybe table is still loading (problem with SEO module and graphs)
        //waitForElement because waitForText has problem with text like /Jet portal 4/Úvodná stránka
        I.waitForElement(locate("#"+name+" tbody tr:nth-child("+row+") td:nth-child("+col+")").withText(value), 10);
    },

    /**
     * Ceck cell values in selected row
     * @param {*} name - table name/ID
     * @param {*} row - row number (starst at 1)
     * @param {*} values - array of expected cell values
     */
    checkTableRow(name, row, values) {
        //codecept wrongly logs this call, we must log it manually
        var log = "DT.checkTableRow("+name+", "+row;
        var col;
        for (col=1; col<values.length; col++) {
            log += ", "+values[col-1];
        }
        log += ")";
        I.say(log);

        for (col=1; col<values.length; col++) {
            this.checkTableCell(name, row, col, values[col-1]);
        }
    }
}