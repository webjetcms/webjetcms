const DT = require("./DT");
const DTE = require("./DTE");
const Document = require("./Document");

const { I } = inject();

//.ge aby preslo email validaciou
const CHANGE_TEXT = "-chan.ge";

module.exports = {

    async baseTest(options) {

        //close other tabs if there are left any open from previous tests
        I.closeOtherTabs();

        const dataTable = options.dataTable;
        const customRequiredFields = options.requiredFields;

        const requiredFields = [];
        const columns = await I.getDataTableColumns(dataTable);
        const id = await I.getDataTableId(dataTable);
        options.id = id;
        let url = await I.grabCurrentUrl();
        let sharp = url.indexOf("#");
        if (sharp>0) url = url.substring(0, sharp);
        options.url = url;

        options.columnTypes = [];
        options.columns = columns;

        I.say("Datatable.id="+options.id+" url="+url);
        try {
            // Testovanie nazvov stlpcov
            columns.forEach(column => {
                //console.log(column);
                let required = false;
                let type = "text";

                if (typeof column.editor != "undefined" && column.editor.required===true) required = true;
                if (typeof column.editor != "undefined") type = column.editor.type;

                //field type json is not supported for autofill
                if ("json"===type) required = false;

                I.say("Detekujem stlpce, sTitle="+column.sTitle+" required="+required+" type="+type+" name="+column.sName);

                options.columnTypes[column.sName] = type;

                //preskoc volitelne polia
                if (column.sTitle && "null"!=column.sTitle && column.visibleOriginal===true && column.sName.indexOf("field")!=0) I.see(column.sTitle);
                if (required === true) requiredFields.push(column);
            });
        } catch(err) {
            throw err;
        }

        if (customRequiredFields) {
            await this.testRequiredFields(customRequiredFields, options);
        } else {
            // Pokial sa nezadefinuje customRequiredFields hlada property - required z columns v datatabulke a podla nej testuje
            const columnsRequiredFields = requiredFields.map(field => field.sName);
            await this.testRequiredFields(columnsRequiredFields, options);
        }

        return options;
    },

    /**
     * Metoda testuje povinne polia pre datatabulku
     * @param {string[]} requiredFields pole pre povinne polia
     */
    async testRequiredFields(requiredFields, options) {
        const startDate = new Date();
        const randomText = I.getRandomText();
        const randomTextShort = I.getRandomTextShort();

        I.say("startDate="+startDate);
        I.say("startDate="+I.formatDateTime(startDate.getTime()));

        let container = options.container;
        if (typeof container == "undefined" || container==null) container = "";
        else container = container + " ";

        let containerModal = options.containerModal;
        if (typeof containerModal == "undefined" || containerModal==null) containerModal = "";
        else containerModal = containerModal + " ";

        let skipRefresh = false;
        if (typeof options.skipRefresh != "undefined") skipRefresh=options.skipRefresh;

        let skipSwitchDomain = false;
        if (typeof options.skipSwitchDomain != "undefined") skipSwitchDomain=options.skipSwitchDomain;
        options.skipSwitchDomain = skipSwitchDomain;

        let switchDomainName = "mirroring.tau27.iway.sk";
        if (typeof options.switchDomainName != "undefined") switchDomainName=options.switchDomainName;

        let testingData = options.testingData || {};
        /* Testovanie - pridavanie */
        I.say("Testovanie - pridavanie");
        I.click({css: container+'.dt-buttons .buttons-create'});
        DTE.waitForEditor(options.id);

        /* Testovanie - custom createSteps funkcia */
        I.say("Testing createSteps: "+(typeof options.createSteps));
        if (typeof options.createSteps == "function") {
            options.createSteps(I, options, DT, DTE);
        }

        I.click("Pridať", {css: containerModal+"div.DTE_Form_Buttons"});
        DTE.waitForLoader();
        let index = 0;
        for (const field of requiredFields) {
            let fieldFixed = field.replace(".", "\\.");
            let value = await I.grabValueFrom(`#DTE_Field_${field}`);
            if (typeof value == "undefined" || value == "") {
                I.see("Povinné pole", `div.DTE_Field_Name_${fieldFixed}`);
            } else {
                I.say("-->Pole "+field+" je vyplnene, hodnota="+value);
                testingData[index] = value;
            }
            index++;
        };

        index = 0;
        for (const field of requiredFields) {
            let value = await I.grabValueFrom(`#DTE_Field_${field}`);
            if (typeof value == "undefined" || value == "") {

                if (typeof testingData[field] != "undefined") testingData[index] = testingData[field];
                if (typeof testingData[index] == "undefined") {
                    if (field.toLocaleLowerCase().indexOf("email")!=-1) {
                        testingData[index] = `${field}-autotest-${randomTextShort}@onetimeusemail.com`;
                    } else {
                        testingData[index] = `${field}-autotest-${randomText}`;
                    }
                }
                I.say("setting testingData["+index+"]="+ testingData[index] );

                if ("quill" === options.columnTypes[field]) DTE.fillQuill(field, testingData[index]);
                else I.fillField(`#DTE_Field_${field}`, `${testingData[index]}`);

                I.click("Pridať", {css: containerModal+"div.DTE_Form_Buttons"});
                DTE.waitForLoader();
            }
            if (index != requiredFields.length - 1) I.dontSee("Povinné pole", `#DTE_Field_${field}`);

            index++;
        };
        options.testingData = testingData;

        //over, ze sa modal zatvoril/zaznam sa ulozil
        if (containerModal!="") I.dontSeeElement(containerModal);
        else I.dontSeeElement('div.modal');

        //because of possible force reload
        DT.waitForLoader();

        if (typeof options.afterCreateSteps == "function") {
            I.say("Testovanie - afterCreateSteps");
            options.afterCreateSteps(I, options, requiredFields, DT, DTE);
        }

        /* Testovanie - vyhladavanie */
        I.say("Testovanie - vyhladavanie");
        requiredFields.forEach((field, index) => {
            let fieldType = options.columnTypes[field];
            I.say(field+"["+index+"]=>"+testingData[index]+" type="+fieldType);
            if ("datetime" === fieldType || "date" === fieldType) {
                field = "from-"+field;
            }

            if ("select" === fieldType) {
                //DT.filterSelect(field, testingData[index]);
                //we must select by option value not text
                I.selectOption({ css: "div.dataTables_scrollHeadInner select.dt-filter-" + field }, testingData[index]);
                I.executeScript(({field}) => {
                    $("div.dataTables_scrollHeadInner select.dt-filter-" + field).selectpicker('refresh');
                }, {field});
                I.click({ css: "div.dataTables_scrollHeadInner button.dt-filtrujem-" + field });
                DT.waitForLoader();
            } else {
                DT.filter(field, testingData[index]);
            }
            I.see(testingData[index]);
            DT.clearFilter(field);
        });

        /* Testovanie - uprava  */
        I.say("Testovanie - uprava");
        DT.filter(requiredFields[0], testingData[0]);

        I.click({css: container+"td.dt-select-td"});
        I.click({css: container+"button.buttons-edit"});
        DTE.waitForEditor(options.id);

        /* Testovanie - custom editSteps funkcia */
        I.say("Testing editSteps: "+(typeof options.editSteps));
        if (typeof options.editSteps == "function") {
            options.editSteps(I, options, DT, DTE);
        }

        requiredFields.forEach((field, index) => {
            I.say("getting testingData["+index+"]="+ testingData[index] +" type="+options.columnTypes[field]);

            let fieldType = options.columnTypes[field];
            if ("datetime" === fieldType || "date" === fieldType || "select" === fieldType) return;

            if ("quill" === options.columnTypes[field]) DTE.fillQuill(field, testingData[index]+CHANGE_TEXT);
            else DTE.appendField(field, CHANGE_TEXT);
        });

        I.click("Uložiť", containerModal+"div.DTE_Form_Buttons");
        DTE.waitForLoader();

        //over, ze sa modal zatvoril
        if (containerModal!="") I.dontSeeElement(containerModal);
        else I.dontSeeElement('div.modal');

        //because of possible force reload
        DT.waitForLoader();

        /* Testovanie - vyhladanie upraveneho zaznamu */
        I.say("Testovanie - vyhladanie upraveneho zaznamu");
        if (skipRefresh == false) {
            I.refreshPage();
            DT.waitForLoader();
        }
        DT.filter(requiredFields[0], `${testingData[0]}${CHANGE_TEXT}`);
        I.see(`${testingData[0]}${CHANGE_TEXT}`, "div.dataTables_scrollBody");

        /* Testovanie - custom editSearchSteps funkcia */
        I.say("Testing editSearchSteps: "+(typeof options.editSearchSteps));
        if (typeof options.editSearchSteps == "function") {
            options.editSearchSteps(I, options, DT, DTE);
        }

        /* Testovanie - vyhladavanie zaznamu v inej domene */
        I.say("Testovanie - vyhladavanie zaznamu v inej domene");
        if (true !== options.skipSwitchDomain){
            previousSelectedDomain = await I.grabTextFrom('div.js-domain-toggler div.bootstrap-select button');
            previousSelectedDomain = previousSelectedDomain.trim();

            const firstRowId = await I.grabTextFrom('.datatable-column-width');

            I.click({css: container+"td.dt-select-td"});
            I.click({css: container+"button.buttons-edit"});
            DTE.waitForEditor(options.id);
            const dataTableName = options.dataTable;
            const newUrl = await I.executeScript(({dataTableName, firstRowId}) => {
                return WJ.urlAddPath(window[dataTableName].DATA.url, "/"+firstRowId);
            }, {dataTableName, firstRowId});
            I.clickCss(".btn-close-editor")

            Document.switchDomain(switchDomainName);
            DT.filter(requiredFields[0], `${testingData[0]}${CHANGE_TEXT}`);

            const itsThereIfFoundByName = await I.grabNumberOfVisibleElements(locate('a').withText( `${testingData[0]}${CHANGE_TEXT}`));

            DT.filter(requiredFields[0], '');
            DT.filter(options.columns[0].name, firstRowId);

            const itsThereIfFoundById = await I.grabNumberOfVisibleElements(locate('a').withText( `${testingData[0]}${CHANGE_TEXT}`));

            await I.executeScript((newUrl) =>
                $.get(newUrl).done((json)=>{$(".dataTables_scrollBody").text(JSON.stringify(json))}), newUrl
            );

            const response = await I.grabTextFrom(".dataTables_scrollBody");
            I.say("Response: "+response);

            I.say("Go back to previous domain");
            Document.switchDomain(previousSelectedDomain);

            I.assertNotContain(response, `${testingData[0]}${CHANGE_TEXT}`, 'Error I found value in REST API call from domain');

            if(itsThereIfFoundByName > 0) {
                let errorMsg = "Error I found value from domain by name " + previousSelectedDomain + " inside domain " + switchDomainName + ": " + requiredFields[0] + " - " + `${testingData[0]}${CHANGE_TEXT}`;
                I.assertEqual("", errorMsg);
            }

            if(itsThereIfFoundById > 0) {
                let errorMsg = "Error I found value from domain by id " + previousSelectedDomain + " inside domain " + switchDomainName + ": " + requiredFields[0] + " - " + `${testingData[0]}${CHANGE_TEXT}`;
                I.assertEqual("", errorMsg);
            }

            I.say("We must again filter out our record - state before we switched domains");
            DT.filter(requiredFields[0], `${testingData[0]}${CHANGE_TEXT}`);
            I.see(`${testingData[0]}${CHANGE_TEXT}`, "div.dataTables_scrollBody");
        }

        /* Testovanie - custom beforeDelete funkcia */
        I.say("Testing beforeDeleteSteps: "+(typeof options.beforeDeleteSteps));
        if (typeof options.beforeDeleteSteps == "function") {
            options.beforeDeleteSteps(I, options, DT, DTE);
        }

        /* Testovanie - zmazanie */
        I.say("Testovanie - zmazanie");
        if (skipRefresh==false) I.click(container+"td.dt-select-td");

        I.see(`${testingData[0]}${CHANGE_TEXT}`, ".dataTables_scrollBody table.dataTable tbody");

        I.click({css: container+'.dt-buttons .buttons-remove'});
        DTE.waitForEditor(options.id);
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader();
        DT.filter(requiredFields[0], `${testingData[0]}${CHANGE_TEXT}`);
        I.dontSee(`${testingData[0]}chan.ge`, ".dataTables_scrollBody table.dataTable tbody");
        I.dontSee(`${testingData[0]}${CHANGE_TEXT}`, ".dataTables_scrollBody table.dataTable tbody");

        //reloadni DT a over, ci to je skutocne zmazane
        I.say("Obnovenie DT a overenie zmazania");
        //I.click("#dt-filter-labels-link-availableGrooupsList");
        I.click({css: container+'.dt-buttons .buttons-refresh'});
        DT.waitForLoader();
        I.dontSee(`${testingData[0]}chan.ge`, ".dataTables_scrollBody table.dataTable tbody");
        I.dontSee(`${testingData[0]}${CHANGE_TEXT}`, ".dataTables_scrollBody table.dataTable tbody");
        I.see("Záznamy 0 až 0 z 0");

        await this.testAuditRecords(startDate.getTime(), testingData[0]);

        var perms = options.perms ? options.perms : null;
        I.say("perms="+perms);
        if (null == perms) I.say("POZOR je potrebne zadat option perms pre kontrolu prav, alebo prazdnu hodnotu.");
        I.assertNotEqual(perms, null);
        if (perms != null) {
            if (perms != '' && perms != '-') {
                I.say("Calling DT.CheckPerms");
                DT.checkPerms(perms, options.url, options.id);
            }
        }
    },

    /**
     * Otestuje, ci sa zapisali auditne zaznamy
     * @param {*} startTime - zaciatocny datum testu (timestamp)
     * @param {*} description - popis, ktory sa bude vyhladavat
     */
    async testAuditRecords(startTime, description) {
        I.say("Testujem auditne zaznamy");
        I.amOnPage("/admin/v9/apps/audit-search/");
        let formatted = I.formatDateTime(startTime);
        I.fillField({css: "input.dt-filter-from-createDate"}, formatted);
        //I.click({css: "button.dt-filtrujem-createDate"});
        I.fillField({css: "input.dt-filter-description"}, description);
        I.click({css: "button.dt-filtrujem-description"});
        DT.waitForLoader("#datatableInit_processing");
        //pause();
        let rows = await I.getTotalRows();
        //console.log(" mam rows=====", rows);
        I.assertAbove(rows+1, 3, "Nedostatocny pocet audit zaznamov");
    },

    /**
     * Ziska JSON property v objekte podla zadaneho mena, akcetupje aj nested property typu editorFields.groupCopyDetails
     * @param {*} obj
     * @param {*} path
     * @param {*} value
     */
    getJsonProperty(obj, path) {
        let schema = obj;  // a moving reference to internal objects within obj
        const pList = path.split('.');
        const len = pList.length;
        for (let i = 0; i < len - 1; i++) {
            const elem = pList[i];
            if (!schema[elem]) {
                schema[elem] = {}
            }
            schema = schema[elem];
        }
        return schema[pList[len - 1]];
    },

    openTabForField(field, columns, options) {
        columns.forEach(column => {
            if (column.sName == field) {
                if (typeof column.editor != "undefined" && column.editor.tab != undefined) {
                    I.click("#pills-dt-"+options.id+"-"+column.editor.tab+"-tab");
                }
            }
        });
    },

    /**
     * Otestuje import zo suboru zadaneho v options.file
     * @param {*} options
     */
    async importTest(options) {

        const dataTable = options.dataTable;
        const customRequiredFields = options.requiredFields;

        let requiredFields = [];
        const columns = await I.getDataTableColumns(dataTable);
        const id = await I.getDataTableId(dataTable);
        options.id = id;
        let url = await I.grabCurrentUrl();
        let sharp = url.indexOf("#");
        if (sharp>0) url = url.substring(0, sharp);
        options.url = url;
        I.say("Datatable.id="+options.id+" url="+url);

        var randomText = I.getRandomTextShort();
        try {
            // Testovanie nazvov stlpcov
            columns.forEach(column => {
                //console.log(column);
                let required = false;
                if (typeof column.editor != "undefined" && column.editor.required===true) required = true;
                I.say("Detekujem stlpce, sTitle="+column.sTitle+" required="+required);

                if (column.sTitle && column.visibleOriginal===true) I.see(column.sTitle);
                if (required === true) requiredFields.push(column);
            });
        } catch(err) {
            throw err;
        }

        if (customRequiredFields) requiredFields = customRequiredFields;
        else requiredFields = requiredFields.map(field => field.sName)

        let container = options.container;
        if (typeof container == "undefined" || container==null) container = "";
        else container = container + " ";

        let containerModal = options.containerModal;
        if (typeof containerModal == "undefined" || containerModal==null) containerModal = "";
        else containerModal = containerModal + " ";

        //over ze take data uz nemame, ak ano, padol predchadzajuci import
        options.rows.forEach(row => {
            for (var key in row) {
                let value = this.getJsonProperty(row, key);
                I.say("Iterating row, key="+key+" value="+value);
                DT.filter(key, value);
            }
            I.see("Nenašli sa žiadne vyhovujúce záznamy");
        });

        I.say("Importujem data");

        I.click({css: "button[data-dtbtn=import]"});
        DTE.waitForModal("datatableImportModal");

        I.attachFile('#insert-file', options.file);

        I.waitForEnabled("#submit-import", 5);
        I.click("#submit-import");

        DTE.waitForModalClose("datatableImportModal");
        DT.waitForLoader();

        //over ze sa zaznamy daju najst
        options.rows.forEach(row => {
            for (var key in row) {
                let value = this.getJsonProperty(row, key);
                I.say("Iterating row, key="+key+" value="+value);
                DT.filter(key, value);
                I.see(value, "div.dataTables_scrollBody");
            }
            I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
            I.see("Záznamy 1 až 1 z 1");
        });

        //uprav data cez klasicky editor, dopln changed na vsetky povinne stlpce okrem updateBy stlpca
        I.say("Aktualizujem data changed by");
        I.refreshPage();
        DT.waitForLoader();
        I.wait(0.5);
        options.rows.forEach(row => {
            for (var key in row) {
                let value = this.getJsonProperty(row, key);
                I.say("Iterating row, key="+key+" value="+value);
                DT.filter(key, value);
                I.see(value, "div.dataTables_scrollBody");
            }
            I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
            I.see("Záznamy 1 až 1 z 1");

            I.click({css: container+"td.dt-select-td"});
            I.click({css: container+"button.buttons-edit"});
            DTE.waitForEditor(options.id);

            requiredFields.forEach((field, index) => {
                //updateBy je vo formate nazov - field
                if (options.updateBy.indexOf(" - "+field)!=-1) return;
                I.click(`#DTE_Field_${field}`);
                DTE.appendField(field, CHANGE_TEXT);
            });

            I.click("Uložiť", containerModal+"div.DTE_Form_Buttons");
            DTE.waitForLoader();

            I.see(CHANGE_TEXT, "div.dataTables_scrollBody");
            I.see("Záznamy 1 až 1 z 1");
        });

        if (typeof options.preserveColumns != "undefined") {
            I.say("Setting preserve columns");

            I.click({css: container+"button.buttons-edit"});
            DTE.waitForEditor(options.id);

            options.preserveColumns.forEach((field, index) => {
                I.say("Updating preserved column "+field);
                this.openTabForField(field, columns, options);
                I.fillField(`#DTE_Field_${field}`, field+"-"+randomText);
            });

            DTE.save(options.id);
            I.dontSeeElement(".modal.DTED.show div.DTE_Form_Error");
        }

        //sprav update podla stlpca
        I.say("Aktualizujem data importom");
        I.refreshPage();
        DT.waitForLoader();

        let updateBy = options.updateBy;
        I.click({css: "button[data-dtbtn=import]"});
        DTE.waitForModal("datatableImportModal");

        I.attachFile('#insert-file', options.file);
        I.waitForEnabled("#submit-import", 5);

        I.click("Aktualizovať existujúce záznamy");
        I.waitForVisible("#dt-import-update-by-column");
        I.click({ css: "button[data-id=dt-settings-update-by-column]" });
        I.waitForElement(locate('div.dropdown-menu.show .dropdown-item').withText(updateBy), 5);
        I.forceClick(locate('div.dropdown-menu.show .dropdown-item').withText(updateBy));
        I.wait(0.5);

        I.click("#submit-import");

        DT.waitForLoader();
        //toto je haluz, ale ak to tu nie je, tak sa zrazu zobrazi dialog pre vyber suboru
        //I.wait(10);

        //over ze sa zaznamy daju najst a stale to je len jeden zaznam
        options.rows.forEach(row => {
            for (var key in row) {
                let value = this.getJsonProperty(row, key);
                I.say("Iterating row, key="+key+" value="+value);
                DT.filter(key, value);
                I.see(value, "div.dataTables_scrollBody");
                I.dontSee(value+CHANGE_TEXT, "div.dataTables_scrollBody");
            }
            I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
            I.see("Záznamy 1 až 1 z 1", "div.dt-footer-row");
        });

        if (typeof options.preserveColumns != "undefined") {
            I.say("Checking preserve columns");
            I.click({css: container+"td.dt-select-td"});
            I.click({css: container+"button.buttons-edit"});
            DTE.waitForEditor(options.id);

            options.preserveColumns.forEach((field, index) => {
                I.say("Updating preserved column "+field);
                this.openTabForField(field, columns, options);
                I.seeInField(`#DTE_Field_${field}`, field+"-"+randomText);
            });

            DTE.cancel(options.id);
        }

        //zmaz zaznam
        I.refreshPage();
        DT.waitForLoader();
        I.say("Mazem naimportovane data");
        options.rows.forEach(row => {
            for (var key in row) {
                let value = this.getJsonProperty(row, key);
                I.say("Iterating row, key="+key+" value="+value);
                DT.filter(key, value);
                I.see(value, "div.dataTables_scrollBody");
            }
            //over, ze sa nasiel jeden zaznam
            I.waitForText("Záznamy 1 až 1 z 1", 5, "div.dt-footer-row");

            //oznac ho a zmaz
            I.click(container+"td.dt-select-td");
            I.click(".buttons-remove", {css: container+'.dt-buttons'});
            I.waitForElement("div.DTE_Action_Remove");
            I.click("Zmazať", "div.DTE_Action_Remove");

            //over, ze sa nic nenaslo
            I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 5, "td.dataTables_empty");
        });

    }
}
