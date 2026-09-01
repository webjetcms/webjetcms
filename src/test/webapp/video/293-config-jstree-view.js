Feature("video.293-config-jstree-view");

const treeSelector = "#SomStromcek";
const tableWrapper = "#configurationDatatable_wrapper";
const changedNode = `${treeSelector} li[data-configuration-view='changed']`;
const customNode = `${treeSelector} li[data-configuration-view='custom']`;
const allNode = `${treeSelector} li[data-configuration-view='all']`;
const appsNode = `${treeSelector} li[data-configuration-module='apps']`;
const formsNode = `${treeSelector} li[data-configuration-module='apps.form']`;

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
Hľadáte jednu konfiguračnú premennú v dlhom zozname nastavení?

Doteraz sa zobrazovali iba nastavenia s hodnotou uloženou v systéme a cesta ku konkrétnej položke mohla trvať zbytočne dlho.

Vo WebJET CMS je teraz orientácia v konfigurácii jednoduchšia. Na ľavej strane pribudol strom, ktorý rozdeľuje nastavenia do logických pohľadov a oblastí.

Po otvorení zostáva zvolený pohľad Zmenené. Nájdete v ňom nastavenia, ktoré majú hodnotu uloženú v systéme. Pohľad Zákaznícke sústredí vlastné nastavenia vašej inštalácie. A v pohľade Všetky uvidíte kompletný zoznam vrátane nastavení, ktoré stále používajú predvolenú hodnotu.

Nastavenia si môžete prezerať aj podľa oblastí. Stačí rozbaliť napríklad bezpečnosť a potom prihlásenie cez externé služby. Tabuľka sa zúži iba na súvisiace položky. Výber širšej oblasti zahŕňa aj jej podskupiny. Ak poznáte názov oblasti, použite vyhľadávanie modulov a dostanete sa k nej ešte rýchlejšie.

Jedno nastavenie môže súvisieť s viacerými časťami systému. Preto sa zobrazí vo všetkých relevantných vetvách a nájdete ho tam, kde ho prirodzene očakávate.

Výsledkom je menej zdĺhavého posúvania, lepší prehľad a rýchlejšia správa konfigurácie aj pri veľkom množstve nastavení.

Podrobný popis nových pohľadov a práce s konfiguračnými premennými nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.
`);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(`
| 0-4 s | Detail dlhej tabuľky, strom zatiaľ mimo záberu. Krátky scroll. Voliteľný titulok: „Jedno nastavenie. Dlhý zoznam.“ |
| 4-12 s | Ukážte počet strán alebo pokračujte krátkym scrollovaním zoznamu. |
| 12-23 s | Plynulo odhaľte celú obrazovku so stromom vľavo. |
| 23-39 s | Ukážte predvolený pohľad **Zmenené**, potom kliknite na **Zákaznícke** a **Všetky**. Po každom kliknutí počkajte približne sekundu. |
| 39-59 s | Rozbaľte security, kliknite naň, následne rozbaľte oauth2. Potom do poľa **Hľadať modul** zadajte oauth2. |
| 59-68 s | Voliteľne strihom ukážte rovnakú premennú xhrFileUploadAllowedExtensions vo vetvách apps.form, security a files.upload. Jednoduchšia alternatíva je titulok „Jedno nastavenie • viac relevantných oblastí“. |
| 68-76 s | Celkový pohľad na strom a prefiltrovanú tabuľku. Titulok: „Menej hľadania. Lepší prehľad.“ |
| 76-90 s | Kliknite na **Pomocník** a ukážte dokumentáciu konfigurácie. Záverečný titulok: „Podrobný návod nájdete v popise videa.“ |
`);
});

Scenario("293-config-jstree-view", ({ I, DT, login }) => {
    login("admin");
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
    I.videoClick(`${treeSelector} li[data-configuration-module='system.performance'] > a.jstree-anchor`);

    I.videoClick("#tree-folder-search-clear-button");
    I.wait(10);
}).tag("@video");
