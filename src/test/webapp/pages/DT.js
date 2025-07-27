const { I } = inject();
const buttons = require('./buttons.js')

/**
 * Funkcie pre pracu s datatabulkou
 */

module.exports = {
    btn : buttons.btn,

    /**
     * Add new context to the datatable for button selectors
     * @param {string} newContextKey - key of the new context
     * @param {string} newContextSelector - selector of the new context
     * @example
     * DT.addContext('inquiryStat', '#inquiryStatDataTable_wrapper');
     * DT.addContext('config','#configurationDatatable_wrapper');
     * then you can use selectors:
     * I.click(DT.btn.inquiryStat_add_button);
     * I.click(DT.btn.config_add_button);
     **/
    addContext(newContextKey, newContextSelector) {
        buttons.addContext(newContextKey, newContextSelector);
    },

    waitForLoader(name) {
        if (typeof name == "undefined") { name = "div.dt-processing"; }

        if (name.indexOf(".") == -1 && name.indexOf("#") == -1) {
            name = "#" + name + "_processing";
        }

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

    _filter(name, value, type=null, containerSelector = null) {
        var nameType = name;
        if (name.indexOf(".") != -1) {
            nameType = name.replace(/\./gi, "-");
            name = name.replace(/\./gi, "\\.");
        }

        if (containerSelector == null || containerSelector == "") containerSelector = "";
        else containerSelector = containerSelector + " ";

        var value1 = value;
        if (value1.length>2) value1 = "**"; //just placeholder
        I.fillField({ css: containerSelector+"div.dt-scroll-headInner input.dt-filter-" + name }, value1);
        //este raz, niekedy to nechyti prve pismena
        I.fillField({ css: containerSelector+"div.dt-scroll-headInner input.dt-filter-" + name }, value);

        //If its from/to its number/date NOT a string
        if (type != null && name.startsWith("from-") == false && name.startsWith("to-") == false) {
            I.click({ css: containerSelector+"div.dt-scroll-headInner th.dt-th-" + nameType + " div.filter-input-prepend button.btn-outline-secondary" });

            //this dialog is not withon container, it is in body element
            if(type == 'contains') {
                I.click(locate('div.dropdown-menu.show .dropdown-item').withChild("span > i.ti-arrows-horizontal"));
            } else if(type == 'startwith') {
                I.click(locate('div.dropdown-menu.show .dropdown-item').withChild("span > i.ti-arrow-right-bar"));
            } else if(type == 'endwith') {
                I.click(locate('div.dropdown-menu.show .dropdown-item').withChild("span > i.ti-arrow-left-bar"));
            } else if(type == 'equals') {
                I.click(locate('div.dropdown-menu.show .dropdown-item').withChild("span > i.ti-equal"));
            }
        }

        //for date fields remove from- or to- prefix, there is only one search button
        if (name.startsWith("from-")) name = name.substring(5);
        if (name.startsWith("to-")) {
            name = name.substring(3);
            I.wait(2);
        }

        //because of firefox we use forceClick
        I.forceClick({ css: containerSelector+"div.dt-scroll-headInner button.dt-filtrujem-" + name });
        //this.waitForLoader();
        //this is faster
        I.wait(0.3);
        I.waitForInvisible("div.dt-processing", 40);
    },

    //this will expect that the filter is preselected to contains, otherwise use filterContainsForce,
    // it's like that because default is contains so the tests are faster
    filterContains(name, value, containerSelector = null) {
        this._filter(name, value, null, containerSelector);
    },
    filterContainsForce(name, value, containerSelector = null) {
        this._filter(name, value, 'contains', containerSelector);
    },
    filterStartsWith(name, value, containerSelector = null) {
        this._filter(name, value, 'startwith', containerSelector);
    },
    filterEndsWith(name, value, containerSelector = null) {
        this._filter(name, value, 'endwith', containerSelector);
    },
    filterEquals(name, value, containerSelector = null) {
        this._filter(name, value, 'equals', containerSelector);
    },
    filterId(name, value, containerSelector = null) {
        this._filter(name, value, null, containerSelector);
    },

    setDates(dateFrom, dateTo, containerSelector = null) {
        const fillDates = () => {
            I.fillField({css: "input.dt-filter-from-dayDate"}, dateFrom);
            I.fillField({css: "input.dt-filter-to-dayDate"}, dateTo);
            I.click({css: "button.dt-filtrujem-dayDate"});
        };

        if (containerSelector) {
            within(containerSelector, fillDates);
        } else {
            fillDates();
        }
    },

    setExtfilterDate(value) {
        I.fillField("div.dt-extfilter-dayDate > form > div.input-group > input.datepicker.min", value);
        I.click("div.dt-extfilter-dayDate > form > div.input-group > button.filtrujem");
        this.waitForLoader();
    },

    clearFilter(name) {
        //for date fields remove from- or to- prefix, there is only one search button
        if (name.startsWith("from-")) name = name.substring(5);
        if (name.startsWith("to-")) {
            name = name.substring(3);
            I.wait(2);
        }

        I.click({ css: "#dt-filter-labels-link-" + name });
        this.waitForLoader();
    },

    filterSelect(name, text, containerSelector = null) {
        if (name.indexOf(".") != -1) name = name.replace(/\./gi, "\\.");

        const expandOption = () => { I.click({ css: "div.dt-scroll-headInner div.dt-filter-" + name + " button.btn-outline-secondary" }); };
        const selectOption = () => { I.click({ css: "div.dt-scroll-headInner button.dt-filtrujem-" + name }); };

        if(containerSelector) {
            within(containerSelector, expandOption);
        } else {
            expandOption();
        }

        I.click(locate('div.dropdown-menu.show .dropdown-item').withText(text));

        if(containerSelector) {
            within(containerSelector, selectOption);
        } else {
            selectOption();
        }

        this.waitForLoader();
    },

    /**
     * Retrieves the number of records from chosen datatable
     */
    async getRecordCount(name) {
        const text = await I.grabTextFrom('#' + name + '_info');
        const match = text.match(/z\s(\d+)$/);
        if (match) {
            return parseInt(match[1], 10);
        }
    },

    //zresetuje nastavenie datatabulky (poradie stlpcov, usporiadanie, zoznam stlpcov)
    resetTable(name = "datatableInit") {
      var container = "#"+name+"_wrapper";
      I.wait(1); //must be here otherwise on webpages it will not reset sorting correctly after load
      this.waitForLoader();
      I.clickCss(container+" button.buttons-settings");
      I.clickCss(container+" button.buttons-colvis");
      I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
      I.clickCss(container+" div.colvispostfix_wrapper button.buttons-colvisRestore");
      I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
      this.waitForLoader();
    },

    /**
     * Shows a column in the data table based on the column name (columnText).
     * @param {string} columnText - The name of the column to be shown.
     * @param {string} tableId - The ID of the table wrapper (default is "datatableInit").
     */
    async showColumn(columnText, tableId="datatableInit") {
        var container = "#"+tableId+"_wrapper";
        I.clickCss(container+" button.buttons-settings");
        I.clickCss(container+" button.buttons-colvis");
        I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");

        //First check, if button is already active
        const count = await I.grabNumberOfVisibleElements(locate("div.colvisbtn_wrapper button.buttons-columnVisibility.dt-button-active").withText(columnText));

        if (count == 0) {
            //Not active, click
            I.click(locate("div.colvisbtn_wrapper button.buttons-columnVisibility").withText(columnText));
        }

        I.clickCss("button.btn.btn-primary.dt-close-modal");
        I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
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
    },

    checkExtfilterDates(dateFrom, dateTo) {
        I.seeInField("div.md-breadcrumb input.dt-filter-from-dayDate", dateFrom);
        I.seeInField("div.md-breadcrumb input.dt-filter-to-dayDate", dateTo);
    },

    /**
     * Delete all rows in datatable selecting all and clicking remove button
     * WARNING: use DT.filter before this function to select only rows you want to delete
     * @param {String} name
     */
    deleteAll(name = "datatableInit") {
        var container = "#"+name+"_wrapper";

        I.clickCss(container + " div.dt-scroll-headInner button.buttons-select-all");
        I.clickCss(container + " div.dt-buttons button.buttons-remove");
        I.waitForVisible("#" + name + "_modal", 200);
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.waitForInvisible("div.DTE_Processing_Indicator", 200);
    },

    checkFooterSumValues(I, tableId, values) {
        for(let i = 0; i < values.length; i++) {
            let postfix = "";
            if(values[i] != null && values[i].length > 0) {
                //element "b" is present only if there is value to show
                postfix = " > b";
            }
            I.see(values[i] + "", "#" + tableId + "_wrapper .dt-scroll-footInner > table > tfoot > tr > td:nth-child(" + (i + 1) + ")" + postfix );
        }
    }
}