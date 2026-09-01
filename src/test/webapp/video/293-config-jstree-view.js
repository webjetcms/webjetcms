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

    // Shot 1: simulate the legacy full-width list without the configuration tree.
    I.executeScript(() => {
        document.querySelector(".configuration-tree-layout > .tree-col").classList.add("d-none");
        const datatableColumn = document.querySelector(".configuration-tree-layout > .datatable-col");
        datatableColumn.classList.remove("col-md-8");
        datatableColumn.classList.add("col-md-12");
        configurationDatatable.columns.adjust();
    });
    I.waitForInvisible(".configuration-tree-layout > .tree-col", 5);
    I.seeElement(".configuration-tree-layout > .datatable-col.col-md-12");
    I.wait(3);

    // Browse several pages to demonstrate the length of the legacy list.
    for (const page of [2, 3, 4, 5]) {
        I.videoClick(locate(`${tableWrapper} button.page-link`).withText(String(page)));
        I.waitForElement(
            locate(`${tableWrapper} li.dt-paging-button.page-item.active button.page-link`).withText(String(page)),
            20
        );
        DT.waitForLoader();
    }
    I.wait(1);

    // Shot 2: reveal the new tree with the Changed view selected by default.
    I.amOnPage("/admin/v9/settings/configuration/");
    I.waitForVisible(".configuration-tree-layout > .tree-col", 5);
    I.seeElement(".configuration-tree-layout > .datatable-col.col-md-8");
    I.waitForElement(`${changedNode} > a.jstree-clicked[aria-selected='true']`, 5);
    I.see("Zmenené", changedNode);
    I.see("Zákaznícke", customNode);
    I.see("Všetky", allNode);
    I.see("Hľadať modul", "#tree-folder-search-label");

    // Shot 3: switch between customer-defined and all available variables.
    I.videoClick(`${customNode} > a.jstree-anchor`);
    I.waitForElement(`${customNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "custom", 20);
    DT.waitForLoader();

    I.videoClick(`${allNode} > a.jstree-anchor`);
    I.waitForElement(`${allNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "all", 20);
    DT.waitForLoader();

    // Shot 4: select a module to narrow the table to a relevant group.
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

    // Shot 5: search the module tree and keep the selected module after clearing it.
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
