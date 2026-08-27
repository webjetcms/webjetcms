Feature('components.configuration');

var randomNumber, name, value;
var datatableName = "configurationDatatable";

async function getConfigurationIdColumnLayout(I) {
    return await I.executeScript(() => {
        const table = configurationDatatable;
        table.columns.adjust();

        const rowIndexes = table.rows({ search: "applied" }).indexes().toArray();
        if (rowIndexes.length !== 1) throw new Error(`Expected one filtered configuration row, got ${rowIndexes.length}`);

        const row = table.row(rowIndexes[0]);
        const idCell = row.node().querySelector("td.dt-select-td");
        const idContent = idCell.querySelector(":scope > .datatable-column-width");
        return {
            id: Number(row.data().id),
            width: idCell.getBoundingClientRect().width,
            idContentDisplay: getComputedStyle(idContent).display
        };
    });
}

Before(({ I, login, DT }) => {

    login('admin');
    I.amOnPage("/admin/v9/settings/configuration/");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        name = "name-autotest-" + randomNumber;
        value = "value-autotest-" + randomNumber+"<script>alert('TEST');</script> &#39; poKUS frame-ancestors 'self' \"quoted\"";
    }
    DT.addContext('config','#configurationDatatable_wrapper');
});

Scenario('zoznam konfiguracnych premennych', ({ I }) => {

    I.see("Predvolená hodnota (default value)");
    I.seeElement("#configurationDatatable.dt-hide-id");
    I.seeElement("#configurationDatatable tbody td.dt-select-td");
    I.dontSeeElement("#configurationDatatable.dt-hide-id tbody td.dt-select-td > .datatable-column-width");
});

Scenario('hierarchical configuration tree', async ({ I, DT, a11y }) => {
    const treeSelector = "#SomStromcek";
    const tableWrapper = "#configurationDatatable_wrapper";
    const changedNode = `${treeSelector} li[data-configuration-view='changed']`;
    const customNode = `${treeSelector} li[data-configuration-view='custom']`;
    const allNode = `${treeSelector} li[data-configuration-view='all']`;
    const appsNode = `${treeSelector} li[data-configuration-module='apps']`;
    const formsNode = `${treeSelector} li[data-configuration-module='apps.form']`;
    const securityNode = `${treeSelector} li[data-configuration-module='security']`;
    const oauth2Node = `${treeSelector} li[data-configuration-module='security.oauth2']`;

    I.waitForElement(`${treeSelector}[role='tree'][aria-label='Strom konfiguračných premenných'][aria-describedby='configuration-tree-description']`, 20);
    I.waitForElement(`${changedNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.see("Zmenené", changedNode);
    I.see("Zákaznícke", customNode);
    I.see("Všetky", allNode);
    I.dontSeeElement(`${treeSelector}[aria-multiselectable='true']`);
    I.assertEqual(await I.executeScript(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view")), "changed");
    const viewOrder = await I.executeScript(() => {
        return Array.from(document.querySelectorAll("#SomStromcek > ul.jstree-container-ul > li[data-configuration-view]"))
            .map((node) => node.dataset.configurationView)
            .join(",");
    });
    I.assertEqual(viewOrder, "changed,custom,all");
    await a11y.check(treeSelector);

    I.clickCss(`${customNode} > a.jstree-anchor`);
    I.waitForElement(`${customNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "custom", 20);
    DT.waitForLoader();
    I.waitForVisible(`${tableWrapper} [data-dtbtn='create']`, 10);
    I.waitForVisible(`${tableWrapper} [data-dtbtn='import']`, 10);

    I.clickCss(`${securityNode} > a.jstree-anchor`);
    I.waitForElement(`${oauth2Node} > a.jstree-anchor`, 20);
    DT.waitForLoader();
    I.waitForFunction(() => {
        const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
        const names = configurationDatatable.rows().data().toArray().map((row) => row.name);
        return url.searchParams.get("module") === "security" && names.includes("captchaType");
    }, 20);
    I.pressKey("o");
    I.waitForFunction(() => document.activeElement?.closest("li")?.dataset.configurationModule === "security.oauth2", 20);

    const focusState = await I.executeScript(() => {
        const style = getComputedStyle(document.activeElement);
        return {
            outlineStyle: style.outlineStyle,
            outlineWidth: parseFloat(style.outlineWidth),
            boxShadow: style.boxShadow
        };
    });
    I.assertEqual(focusState.outlineStyle, "solid");
    I.assertTrue(focusState.outlineWidth >= 2, "Tree item must have a visible keyboard focus indicator");
    I.assertNotEqual(focusState.boxShadow, "none");

    I.pressKey("Enter");
    I.waitForFunction(() => {
        const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
        return url.searchParams.get("view") === "module" && url.searchParams.get("module") === "security.oauth2";
    }, 20);
    DT.waitForLoader();
    I.waitForFunction(() => {
        const names = configurationDatatable.rows().data().toArray().map((row) => row.name);
        return names.includes("oauth2_githubClientId") && !names.includes("captchaType");
    }, 20);
    I.waitForInvisible(`${tableWrapper} [data-dtbtn='create']`, 10);
    I.waitForInvisible(`${tableWrapper} [data-dtbtn='import']`, 10);
    I.assertEqual(await I.executeScript(() => $("#SomStromcek").jstree(true).get_selected().length), 1);

    I.clickCss(`${appsNode} > a.jstree-anchor`);
    I.waitForElement(`${formsNode} > a.jstree-anchor`, 20);
    I.clickCss(`${formsNode} > a.jstree-anchor`);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("module") === "apps.form", 20);
    DT.waitForLoader();
    I.waitForFunction(() => {
        const names = configurationDatatable.rows().data().toArray().map((row) => row.name);
        return names.includes("xhrFileUploadAllowedExtensions") && !names.includes("galleryEnableWatermarking");
    }, 20);

    I.fillField("#tree-folder-search-input", "oauth2");
    I.clickCss("#tree-folder-search-button");
    I.waitForElement(`${oauth2Node} > a.jstree-search`, 20);
    I.seeElement(securityNode);

    I.clickCss("#tree-folder-search-clear-button");
    I.waitForElement(`${changedNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "changed", 20);
    DT.waitForLoader();

    I.clickCss(`${allNode} > a.jstree-anchor`);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "all", 20);
    DT.waitForLoader();
    I.waitForVisible(`${tableWrapper} [data-dtbtn='create']`, 10);
    I.waitForVisible(`${tableWrapper} [data-dtbtn='import']`, 10);

    const defaultOnlyName = await I.executeScript(() => {
        configurationDatatable.rows().deselect();
        const rowIndex = configurationDatatable.rows().indexes().toArray().find((index) => {
            return configurationDatatable.row(index).data().databaseValuePresent === false;
        });
        if (rowIndex == null) return null;
        const row = configurationDatatable.row(rowIndex);
        row.select();
        return row.data().name;
    });
    I.assertTrue(Boolean(defaultOnlyName), "All view must contain a default-only configuration variable");
    I.waitForElement(`${tableWrapper} button.buttons-remove.disabled`, 10);

    I.clickCss(`${changedNode} > a.jstree-anchor`);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "changed", 20);
    DT.waitForLoader();
    const selectedDatabaseValue = await I.executeScript(() => {
        configurationDatatable.rows().deselect();
        const row = configurationDatatable.row(0);
        if (row.data()?.databaseValuePresent !== true) return false;
        row.select();
        return true;
    });
    I.assertTrue(selectedDatabaseValue, "Changed view must contain database-backed values");
    I.waitForFunction(() => !document.querySelector("#configurationDatatable_wrapper button.buttons-remove").classList.contains("disabled"), 20);
});

Scenario('latest configuration module response wins', ({ I }) => {
    const appsNode = "#SomStromcek li[data-configuration-module='apps'] > a.jstree-anchor";
    const formsNode = "#SomStromcek li[data-configuration-module='apps.form'] > a.jstree-anchor";

    I.waitForElement(appsNode, 20);
    I.usePlaywrightTo("delay the parent configuration module response", async ({ page }) => {
        await page.waitForFunction(() => {
            return typeof configurationDatatable !== "undefined" && configurationDatatable.rows().count() > 0;
        }, null, { timeout: 20000 });

        await page.evaluate(() => {
            const sourceRow = configurationDatatable.row(0).data();
            const originalAjax = $.ajax;

            window.configurationAjaxRaceTest = {
                originalAjax,
                delayedRequest: null,
                parentResponse: {
                    content: [{ ...sourceRow, name: "galleryEnableWatermarking" }],
                    totalElements: 1,
                    options: {}
                }
            };

            $.ajax = function (options) {
                const requestUrl = typeof options === "string" ? options : options?.url;
                if (requestUrl == null) return originalAjax.apply(this, arguments);

                const url = new URL(requestUrl, location.origin);
                const isParentRequest = url.searchParams.get("view") === "module" && url.searchParams.get("module") === "apps";

                if (!isParentRequest) return originalAjax.apply(this, arguments);

                const deferred = $.Deferred();
                const jqXHR = deferred.promise({
                    readyState: 1,
                    status: 0,
                    statusText: "",
                    abort(statusText = "abort") {
                        this.readyState = 0;
                        this.statusText = statusText;
                        deferred.rejectWith(options.context || options, [this, statusText, statusText]);
                        return this;
                    }
                });

                window.configurationAjaxRaceTest.delayedRequest = { options, jqXHR };
                return jqXHR;
            };
        });

        try {
            await page.locator(appsNode).click();
            await page.waitForFunction(() => window.configurationAjaxRaceTest?.delayedRequest != null, null, { timeout: 20000 });

            const parentTracked = await page.evaluate(() => {
                return configurationDatatable.context[0].jqXHR === window.configurationAjaxRaceTest.delayedRequest.jqXHR;
            });

            await page.locator(formsNode).waitFor({ state: "visible", timeout: 20000 });
            await page.locator(formsNode).click();
            await page.waitForFunction(() => {
                const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
                const names = configurationDatatable.rows().data().toArray().map((row) => row.name);
                return url.searchParams.get("module") === "apps.form" && names.includes("xhrFileUploadAllowedExtensions");
            }, null, { timeout: 20000 });

            const parentAbortState = await page.evaluate(() => {
                const test = window.configurationAjaxRaceTest;
                const delayedRequest = test.delayedRequest;
                const aborted = delayedRequest.jqXHR.readyState === 0 && delayedRequest.jqXHR.statusText === "abort";

                if (!aborted) {
                    delayedRequest.jqXHR.readyState = 4;
                    delayedRequest.jqXHR.status = 200;
                    delayedRequest.jqXHR.statusText = "success";
                    delayedRequest.options.success(test.parentResponse, "success", delayedRequest.jqXHR);
                }

                return {
                    aborted,
                    readyState: delayedRequest.jqXHR.readyState,
                    statusText: delayedRequest.jqXHR.statusText
                };
            });

            const state = await page.evaluate(() => ({
                module: new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("module"),
                names: configurationDatatable.rows().data().toArray().map((row) => row.name)
            }));

            if (!parentTracked) throw new Error("DataTables did not retain the parent jqXHR");
            if (!parentAbortState.aborted) throw new Error(`Expected the parent jqXHR to be aborted, got ${JSON.stringify(parentAbortState)}`);
            if (state.module !== "apps.form") throw new Error(`Expected apps.form to remain selected, got '${state.module}'`);
            if (!state.names.includes("xhrFileUploadAllowedExtensions")) throw new Error("The child module data was replaced");
            if (state.names.includes("galleryEnableWatermarking")) throw new Error("A stale parent module response replaced the child module data");
        } finally {
            await page.evaluate(() => {
                const test = window.configurationAjaxRaceTest;
                if (test != null) $.ajax = test.originalAjax;
                delete window.configurationAjaxRaceTest;
            });
        }
    });
});

Scenario('temporary setting hides encryption and scheduled change', ({ I, DTE }) => {
    I.click("button.buttons-create");
    DTE.waitForEditor(datatableName);

    I.waitForVisible("div.DTE_Field_Name_encrypt", 10);
    I.waitForVisible("div.DTE_Field_Name_datePrepared", 10);

    DTE.clickSwitch("temporary_0");
    I.waitForInvisible("div.DTE_Field_Name_encrypt", 10);
    I.waitForInvisible("div.DTE_Field_Name_datePrepared", 10);

    DTE.clickSwitch("temporary_0");
    I.waitForVisible("div.DTE_Field_Name_encrypt", 10);
    I.waitForVisible("div.DTE_Field_Name_datePrepared", 10);

    DTE.cancel();
});

Scenario('pridanie konfiguracnej premennej @baseTest', ({ I, DT, DTE }) => {

    I.click("button.buttons-create");
    I.click("Pridať");
    DTE.waitForEditor(datatableName);

    I.see("Povinné pole", "div.DTE_Field_Name_name");
    I.fillField("#DTE_Field_name", name);
    I.fillField("#DTE_Field_value", value);
    DTE.save();

    I.see("Predvolená hodnota (default value)");

    //over ze je v zozname (bez reloadu)
    I.fillField("input.dt-filter-name", randomNumber);
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.see(name);

    //po pridani noveho zaznamu a hned jeho editacii padlo REST volanie - nebolo nastavene ID zaznamu
    I.click(name);
    DTE.waitForEditor(datatableName);
    DTE.save();
    I.dontSee("JSON parse error");
});

Scenario('custom configuration variables view @baseTest', async ({ I, DT }) => {
    const customNode = "#SomStromcek li[data-configuration-view='custom'] > a.jstree-anchor";

    I.waitForElement(customNode, 20);
    I.clickCss(customNode);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "custom", 20);
    DT.waitForLoader();

    DT.filterEquals("name", name);
    I.waitForText(name, 10, "#configurationDatatable");
    const customRows = await I.executeScript(() => configurationDatatable.rows({ search: "applied" }).data().toArray());
    I.assertEqual(customRows.length, 1);
    I.assertEqual(customRows[0].name, name);
    I.assertTrue(customRows[0].databaseValuePresent, "A database-only variable must be present in the Custom view");
});

Scenario('docasna hodnota sa zobrazi, ale do editora sa nacita databazova @baseTest', async ({ I, DT, DTE, a11y }) => {
    const temporaryValue = "temporary-value-autotest-" + randomNumber;
    const databaseValueSelector = "#configurationDatatable tbody [data-conf-value='database-inactive']";

    DT.filterEquals("name", name);
    const initialIdLayout = await getConfigurationIdColumnLayout(I);
    I.click(name);
    DTE.waitForEditor(datatableName);

    I.fillField("#DTE_Field_value", temporaryValue);
    DTE.clickSwitch("temporary_0");
    DTE.save();

    I.waitForText(temporaryValue, 10, "#configurationDatatable");
    I.waitForVisible(databaseValueSelector, 10);
    I.see(value, "#configurationDatatable");
    I.dontSee("DB (neaktívna):", "#configurationDatatable");
    I.dontSeeElement("#configurationDatatable .configuration-value__database-label");
    I.dontSeeElement("#configurationDatatable .configuration-value .visually-hidden");
    I.see(temporaryValue, "#configurationDatatable [data-conf-value='current']");
    I.see(value, databaseValueSelector);
    I.seeElement(databaseValueSelector + "[data-bs-toggle='tooltip'][tabindex='0']");

    I.moveCursorTo(databaseValueSelector);
    I.waitForElement(databaseValueSelector + "[aria-describedby]", 5);
    let tooltipId = await I.grabAttributeFrom(databaseValueSelector, "aria-describedby");
    I.assertTrue(Boolean(tooltipId), "Database value tooltip must describe its trigger");
    I.waitForVisible("#" + tooltipId + ".tooltip.show[role='tooltip']", 5);
    I.see("Hodnota uložená v databáze, momentálne neaktívna", "#" + tooltipId + ".tooltip.show[role='tooltip']");

    I.moveCursorTo("#" + tooltipId);
    // Verify that the tooltip stays open beyond its 300 ms hide grace period.
    I.wait(0.5);
    I.seeElement("#" + tooltipId + ".tooltip.show[role='tooltip']");
    I.pressKey("Escape");
    I.waitToHide("#" + tooltipId, 5);
    I.waitForInvisible(databaseValueSelector + "[aria-describedby]", 5);

    I.moveCursorTo("#configurationDatatable tbody td.dt-select-td");
    I.moveCursorTo(databaseValueSelector);
    I.waitForElement(databaseValueSelector + "[aria-describedby]", 5);
    tooltipId = await I.grabAttributeFrom(databaseValueSelector, "aria-describedby");
    I.waitForVisible("#" + tooltipId + ".tooltip.show[role='tooltip']", 5);
    await I.executeScript(() => configurationDatatable.draw(false));
    I.waitToHide("#" + tooltipId, 5);
    I.waitForInvisible(databaseValueSelector + "[aria-describedby]", 5);

    I.focus(databaseValueSelector);
    I.waitForElement(databaseValueSelector + "[aria-describedby]", 5);
    tooltipId = await I.grabAttributeFrom(databaseValueSelector, "aria-describedby");
    I.assertTrue(Boolean(tooltipId), "Focused database value must expose its tooltip through aria-describedby");
    I.waitForVisible("#" + tooltipId + ".tooltip.show[role='tooltip']", 5);
    I.see("Hodnota uložená v databáze, momentálne neaktívna", "#" + tooltipId + ".tooltip.show[role='tooltip']");
    I.pressKey("Escape");
    I.waitToHide("#" + tooltipId, 5);
    I.waitForInvisible(databaseValueSelector + "[aria-describedby]", 5);
    I.blur(databaseValueSelector);

    const adjustedIdLayout = await getConfigurationIdColumnLayout(I);
    I.assertTrue(Number.isSafeInteger(adjustedIdLayout.id), "Configuration ID must be a safe JavaScript integer");
    I.assertEqual(adjustedIdLayout.id, initialIdLayout.id);
    I.assertEqual(adjustedIdLayout.idContentDisplay, "none");
    I.assertTrue(adjustedIdLayout.width <= initialIdLayout.width + 1, "ID column must stay compact after recalculating column widths");

    await a11y.check(databaseValueSelector);
    I.click("#configurationDatatable tbody td.dt-select-td");
    I.waitForVisible("#configurationDatatable tbody tr.selected", 5);
    await a11y.check(databaseValueSelector);

    I.click(name);
    DTE.waitForEditor(datatableName);
    I.seeInField("#DTE_Field_value", value);
    DTE.cancel();

    I.see(temporaryValue, "#configurationDatatable");
    I.see(value, "#configurationDatatable");
});

Scenario('vyhladanie konfiguracnej premennej @baseTest', ({ I, DT }) => {

    //hladanie podla mena
    I.fillField("input.dt-filter-name", randomNumber);
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.see(name);

    //hladanie podla hodnoty
    I.fillField("input.dt-filter-value", "&#39; pokus");
    I.pressKey('Enter', "input.dt-filter-value");
    DT.waitForLoader();
    I.see(value);

    for (const specialValue of ["<script>", "</script>", '"quoted"']) {
        I.fillField("input.dt-filter-value", specialValue);
        I.pressKey('Enter', "input.dt-filter-value");
        DT.waitForLoader();
        I.see(name);
    }
});

Scenario("upravenie konfiguracnej premennej @baseTest", ({ I, DTE }) => {

    I.fillField("input.dt-filter-name", name);
    I.pressKey('Enter', "input.dt-filter-name");

    I.click("th.dt-format-selector");
    I.click("th.dt-format-selector");
    I.click(name);
    DTE.waitForEditor(datatableName);

    I.seeInField("#DTE_Field_value", value);

    value = value + ".changed";

    I.fillField("#DTE_Field_value", value);
    DTE.save();

    I.see(value);

    I.click(name);
    DTE.waitForEditor(datatableName);

    I.seeInField("#DTE_Field_value", value);
});

//chyba v nastaveni id
//detto pri editacii existujuceho, ked sa ulozilo a dalo sa znova editovat padlo
Scenario("overenie nastavenia ID-edit @baseTest", ({ I, DT, DTE }) => {

    DT.filterContains("name", name)
    I.click(name);
    DTE.waitForEditor(datatableName);
    DTE.save();

    I.wait(1);

    I.click(name);
    DTE.waitForEditor(datatableName);
    DTE.save();
    I.dontSee("JSON parse error");
});

Scenario("zmazanie konfiguracnej premennej @baseTest", ({ I, DT }) => {

    I.fillField("input.dt-filter-value", value);
    I.pressKey('Enter', "input.dt-filter-name");

    I.click("th.dt-format-selector");
    I.click("th.dt-format-selector");
    I.click("td.dt-select-td");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();

    I.fillField("input.dt-filter-value", value);
    I.pressKey('Enter', "input.dt-filter-value");
    I.dontSee(name);
});

Scenario("oznacovanie vyfiltrovanych riadkov", async ({ I }) => {
    const assert = require('assert');
    I.see("Názov konfigurácie");
    I.fillField("input.dt-filter-name", "admin");
    I.pressKey('Enter', 'input.dt-filter-name');
    I.click('.dt-filter-id');
    const info = await I.grabHTMLFrom('.select-item:first-child');
    const infoData = info.split(" ").shift();
    within('tbody', async () => {
        const numOfElements = await I.grabNumberOfVisibleElements('tr');
        assert.equal(+numOfElements, +infoData);
    });
});

Scenario("planovanie a historia", async ({ I, DTE }) => {
    const historyName = "aaatest";
    I.click("button.toast-close-button")
    I.fillField("input.dt-filter-value", historyName);
    I.pressKey('Enter', "input.dt-filter-name");

    I.click(historyName);
    DTE.waitForEditor(datatableName);

    I.clickCss("#pills-dt-configurationDatatable-advanced-tab");
    I.see("aaatest - CHANGED - manual 16:10B");
    I.dontSee("aaatest zakladna hodnota-v3");
    I.clickCss("#datatableFieldDTE_Field_confPrepared tr:nth-child(3) td");
    I.clickCss("#datatableFieldDTE_Field_confPrepared_wrapper button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Chyba: zmazať je možné len záznamy v budúcnosti");
    I.click("div.DTE_Action_Remove div.DTE_Header button.btn-close-editor");

    I.clickCss("#pills-dt-configurationDatatable-history-tab")
    I.see("aaatest zakladna hodnota-v3");
    I.dontSee("aaatest - CHANGED - manual 16:10B");
});

Scenario("overenie prav editacie vsetkych premennych", ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/settings/configuration/?removePerm=conf.show_all_variables");
    I.dontSee("structureMirroringConfig");

    DT.filterContains("name", "structureMirroringConfig");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.click(DT.btn.config_add_button);
    DTE.waitForEditor("configurationDatatable");

    I.dontSee("História", "#pills-dt-editor-configurationDatatable");

    I.fillField("#DTE_Field_name", "structureMirroringConfig");
    I.fillField("#DTE_Field_value", "34364,34365:mirroring.tau27.iway.sk");
    DTE.save();
    I.seeElement("#configurationDatatable_modal");
    I.see("Na editáciu tejto konfiguračnej premennej nemáte právo");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty.");

    //skus vyplnit planovanu zmenu
    I.fillField("#DTE_Field_datePrepared", "09.09.2037 08:40:15");
    I.pressKey("Tab");
    DTE.save();
    I.see("Na editáciu tejto konfiguračnej premennej nemáte právo");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty.");

    DTE.cancel();
});

Scenario("logout", ({ I }) => {
    I.logout();
});

const testConfiguration = "smsSendMaxlength";
Scenario("check setting oldValue after delete", async ({ I, DT, DTE }) => {
    const oldValue = 140;
    const newValue = 299792;
    const tableWrapper = "#configurationDatatable_wrapper";
    const allNode = "#SomStromcek li[data-configuration-view='all'] > a.jstree-anchor";
    const integrationsNode = "#SomStromcek li[data-configuration-module='integrations'] > a.jstree-anchor";
    const smsNode = "#SomStromcek li[data-configuration-module='integrations.sms'] > a.jstree-anchor";

    I.amOnPage("/admin/v9/settings/configuration/");
    I.waitForElement(allNode, 20);
    I.clickCss(allNode);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "all", 20);
    DT.waitForLoader();
    DT.filterEquals("name", testConfiguration);

    const hasExistingOverride = await I.executeScript(() => {
        const index = configurationDatatable.rows({ search: "applied" }).indexes().toArray()[0];
        return configurationDatatable.row(index).data()?.databaseValuePresent === true;
    });
    if (hasExistingOverride) {
        I.clickCss("#configurationDatatable tbody td.dt-select-td");
        I.waitForFunction(() => !document.querySelector("#configurationDatatable_wrapper button.buttons-remove").classList.contains("disabled"), 20);
        I.clickCss(`${tableWrapper} button.buttons-remove`);
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waitForLoader();
    }

    I.say("Check actual and default value");
        I.click(testConfiguration);
        DTE.waitForEditor("configurationDatatable");
        I.seeInField("#DTE_Field_value", oldValue);
        I.seeInField("#DTE_Field_oldValue", oldValue);

    I.say("Change value and check that value stay changed. But original value of not changed.");
        DTE.fillField("value", newValue);
        DTE.save();
        DT.waitForLoader();
        I.click(testConfiguration);
        DTE.waitForEditor("configurationDatatable");
        I.seeInField("#DTE_Field_value", newValue);
        I.seeInField("#DTE_Field_oldValue", oldValue);
        DTE.cancel();

    I.say("Reset the database override directly in the integrations.sms module.");
        I.clickCss(integrationsNode);
        I.waitForElement(smsNode, 20);
        I.clickCss(smsNode);
        I.waitForFunction(() => {
            const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
            return url.searchParams.get("view") === "module" && url.searchParams.get("module") === "integrations.sms";
        }, 20);
        DT.waitForLoader();
        I.see(testConfiguration, "#configurationDatatable");
        I.clickCss("#configurationDatatable tbody td.dt-select-td");
        I.waitForFunction(() => !document.querySelector("#configurationDatatable_wrapper button.buttons-remove").classList.contains("disabled"), 20);
        I.clickCss(`${tableWrapper} button.buttons-remove`);
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waitForLoader();

    I.say("Check that the default row remains in the module after reset.");
        I.see(testConfiguration, "#configurationDatatable");
        const resetRow = await I.executeScript(() => {
            const index = configurationDatatable.rows({ search: "applied" }).indexes().toArray()[0];
            return configurationDatatable.row(index).data();
        });
        I.assertEqual(resetRow.value, String(oldValue));
        I.assertFalse(resetRow.databaseValuePresent);
        I.clickCss("#configurationDatatable tbody td.dt-select-td");
        I.waitForElement(`${tableWrapper} button.buttons-remove.disabled`, 10);
        I.click(testConfiguration);
        DTE.waitForEditor("configurationDatatable");
        I.seeInField("#DTE_Field_value", oldValue);
        I.seeInField("#DTE_Field_oldValue", oldValue);
        DTE.cancel();
});

Scenario("Post delete", async ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/configuration/");
    DT.filterEquals("name", testConfiguration);
    const rowCount = await I.grabNumberOfVisibleElements('#configurationDatatable > tbody > tr > td.dt-row-edit');
    if(rowCount > 0) {
        I.clickCss("td.dt-select-td");
        I.clickCss("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waitForLoader();
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
});

Scenario("odhlasenie", ({ I }) => {
    I.logout();
});

Scenario("configuration permissions", ({ DT }) => {
    DT.checkPerms("menuConfig", "/admin/v9/settings/configuration/", "configurationDatatable");
});
