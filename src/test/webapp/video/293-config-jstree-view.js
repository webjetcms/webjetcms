Feature("video.293-config-jstree-view");

const treeSelector = "#SomStromcek";
const tableWrapper = "#configurationDatatable_wrapper";
const changedNode = `${treeSelector} li[data-configuration-view='changed']`;
const customNode = `${treeSelector} li[data-configuration-view='custom']`;
const allNode = `${treeSelector} li[data-configuration-view='all']`;
const appsNode = `${treeSelector} li[data-configuration-module='apps']`;
const formsNode = `${treeSelector} li[data-configuration-module='apps.form']`;

Before(({ login }) => {
    login("admin");
});

Scenario("293-config-jstree-view", ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/configuration/");
    I.waitForElement(`${changedNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForVisible(`${tableWrapper} table`, 20);
    DT.waitForLoader();

    // Shot 1: the default view contains configuration values changed by the customer.
    I.see("Zmenené", changedNode);
    I.see("Zákaznícke", customNode);
    I.see("Všetky", allNode);
    I.see("Hľadať modul", "#tree-folder-search-label");

    // Shot 2: switch between customer-defined and all available variables.
    I.videoClick(`${customNode} > a.jstree-anchor`);
    I.waitForElement(`${customNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "custom", 20);
    DT.waitForLoader();

    I.videoClick(`${allNode} > a.jstree-anchor`);
    I.waitForElement(`${allNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "all", 20);
    DT.waitForLoader();

    // Shot 3: select a module to narrow the table to a relevant group.
    I.videoClick(`${appsNode} > a.jstree-anchor`);
    I.waitForElement(`${appsNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForElement(`${formsNode} > a.jstree-anchor`, 20);
    I.waitForFunction(() => {
        const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
        return url.searchParams.get("view") === "module" &&
            url.searchParams.get("module") === "apps";
    }, 20);
    DT.waitForLoader();

    I.videoClick(`${formsNode} > a.jstree-anchor`);
    I.waitForElement(`${formsNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => {
        const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
        const names = configurationDatatable.rows().data().toArray().map((row) => row.name);
        return url.searchParams.get("view") === "module" &&
            url.searchParams.get("module") === "apps.form" &&
            names.includes("xhrFileUploadAllowedExtensions");
    }, 20);
    DT.waitForLoader();

    // Shot 4: search the module tree and keep the selected module after clearing it.
    I.videoClick("#tree-folder-search-input");
    I.fillField("#tree-folder-search-input", "form");
    I.videoClick("#tree-folder-search-button");
    I.waitForElement(`${formsNode} > a.jstree-search`, 20);

    I.videoClick("#tree-folder-search-clear-button");
    I.waitForElement(`${formsNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => {
        const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
        return url.searchParams.get("view") === "module" &&
            url.searchParams.get("module") === "apps.form";
    }, 20);
    DT.waitForLoader();

    I.wait(10);
});
