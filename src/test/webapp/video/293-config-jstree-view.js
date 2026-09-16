Feature("video.293-config-jstree-view");

const treeSelector = "#SomStromcek";
const tableWrapper = "#configurationDatatable_wrapper";
const changedNode = `${treeSelector} li[data-configuration-view='changed']`;
const customNode = `${treeSelector} li[data-configuration-view='custom']`;
const allNode = `${treeSelector} li[data-configuration-view='all']`;
const appsNode = `${treeSelector} li[data-configuration-module='apps']`;
const formsNode = `${treeSelector} li[data-configuration-module='apps.form']`;

const securityNode = `${treeSelector} li[data-configuration-module='security']`;
const oauth2Node = `${treeSelector} li[data-configuration-module='security.oauth2']`;

// Move whole shot objects to reorder narration, slates and browser actions.
const videoPlan = {
    "language": "sk",
    "notes": "Durations estimate the edited narration. Cut setup, cleanup and slates; use the runner's transition holds when editing.",
    "shots": [
        {
            "id": "long-list",
            "type": "auto",
            "durationSeconds": 12,
            "title": "Browse the saved configuration list",
            "text-sk": "Hľadáte jednu konfiguračnú premennú v dlhom zozname nastavení?\n\nDoteraz sa zobrazovali iba nastavenia s hodnotou uloženou v systéme a cesta ku konkrétnej položke mohla trvať zbytočne dlho.",
            "notes": "Show several pages of the current saved-settings list. Crop to the table during editing for the opening detail; this is not historical footage.",
            shot: async ({ I, DT }) => {
                for (const page of [2, 3, 4, 5]) {
                    await I.videoClick(locate(`${tableWrapper} button.page-link`).withText(String(page)));
                    await I.waitForElement(locate(`${tableWrapper} li.dt-paging-button.page-item.active button.page-link`).withText(String(page)), 20);
                    DT.waitForLoader();
                }
                await I.wait(3);
            }
        },
        {
            "id": "tree-overview",
            "type": "auto",
            "durationSeconds": 11,
            "title": "Show configuration views and modules",
            "text-sk": "Vo WebJET CMS je teraz orientácia v konfigurácii jednoduchšia. Na ľavej strane pribudol strom, ktorý rozdeľuje nastavenia do logických pohľadov a oblastí.",
            "notes": "Show the current tree and table together, with Changed selected.",
            shot: async ({ I }) => {
                await I.seeElement(".configuration-tree-layout > .datatable-col.col-md-8");
                await I.see("Zmenené", changedNode);
                await I.see("Zákaznícke", customNode);
                await I.see("Všetky", allNode);
                await I.see("Hľadať modul", "#tree-folder-search-label");
                await I.wait(5);
            }
        },
        {
            "id": "views",
            "type": "auto",
            "durationSeconds": 16,
            "title": "Switch between Changed, Custom and All",
            "text-sk": "Po otvorení zostáva zvolený pohľad Zmenené. Nájdete v ňom nastavenia, ktoré majú hodnotu uloženú v systéme. Pohľad Zákaznícke sústredí vlastné nastavenia vašej inštalácie. A v pohľade Všetky uvidíte kompletný zoznam vrátane nastavení, ktoré stále používajú predvolenú hodnotu.",
            "notes": "Start from Changed, then select Custom and All.",
            shot: async ({ I, DT }) => {
                await I.wait(3);
                await I.videoClick(`${customNode} > a.jstree-anchor`);
                await I.waitForElement(`${customNode} > a.jstree-clicked[aria-selected='true']`, 20);
                await I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "custom", 20);
                DT.waitForLoader();
                await I.wait(3);
                await I.videoClick(`${allNode} > a.jstree-anchor`);
                await I.waitForElement(`${allNode} > a.jstree-clicked[aria-selected='true']`, 20);
                await I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get("view") === "all", 20);
                DT.waitForLoader();
                await I.wait(4);
            }
        },
        {
            "id": "module-hierarchy",
            "type": "auto",
            "durationSeconds": 14,
            "title": "Narrow configuration by module",
            "text-sk": "Nastavenia si môžete prezerať aj podľa oblastí. Stačí rozbaliť napríklad bezpečnosť a potom prihlásenie cez externé služby. Tabuľka sa zúži iba na súvisiace položky. Výber širšej oblasti zahŕňa aj jej podskupiny.",
            "notes": "Select security and then security.oauth2 to match the narration. Sensitive values are masked in the recording only.",
            shot: async ({ I, DT }) => {
                await I.videoClick(`${securityNode} > a.jstree-anchor`);
                await I.waitForElement(`${securityNode} > a.jstree-clicked[aria-selected='true']`, 20);
                await I.waitForElement(`${oauth2Node} > a.jstree-anchor`, 20);
                DT.waitForLoader();
                await I.wait(3);
                await I.videoClick(`${oauth2Node} > a.jstree-anchor`);
                await I.waitForElement(`${oauth2Node} > a.jstree-clicked[aria-selected='true']`, 20);
                await I.waitForFunction(() => {
                    const url = new URL(configurationDatatable.getAjaxUrl(), location.origin);
                    const names = configurationDatatable.rows().data().toArray().map(row => row.name);
                    return url.searchParams.get("module") === "security.oauth2" && names.includes("oauth2_githubClientId") && !names.includes("captchaType");
                }, 20);
                DT.waitForLoader();
                await I.wait(4);
            }
        },
        {
            "id": "module-search",
            "type": "auto",
            "durationSeconds": 6,
            "title": "Search modules and clear the query",
            "text-sk": "Ak poznáte názov oblasti, použite vyhľadávanie modulov a dostanete sa k nej ešte rýchlejšie.",
            "notes": "Find oauth2, select it, clear the search and preserve the selected module.",
            shot: async ({ I, DT }) => {
                await I.videoClick("#tree-folder-search-input");
                await I.fillField("#tree-folder-search-input", "oauth2");
                await I.videoClick("#tree-folder-search-button");
                await I.waitForElement(`${oauth2Node} > a.jstree-search`, 20);
                await I.videoClick(`${oauth2Node} > a.jstree-anchor`);
                await I.waitForElement(`${oauth2Node} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.videoClick("#tree-folder-search-clear-button");
                await I.waitForElement(`${oauth2Node} > a.jstree-clicked[aria-selected='true']`, 20);
                await I.dontSeeInField("#tree-folder-search-input", "oauth2");
                await I.wait(4);
            }
        },
        {
            "id": "related-areas",
            "type": "manual",
            "durationSeconds": 9,
            "title": "One setting in related areas",
            "text-sk": "Jedno nastavenie môže súvisieť s viacerými časťami systému. Preto sa zobrazí vo všetkých relevantných vetvách a nájdete ho tam, kde ho prirodzene očakávate.",
            "notes": "Create a caption: Jedno nastavenie - viac relevantných oblastí. Optionally add a montage of xhrFileUploadAllowedExtensions in apps.form, security and files.upload."
        },
        {
            "id": "summary",
            "type": "auto",
            "durationSeconds": 8,
            "title": "Keep the selected module and results together",
            "text-sk": "Výsledkom je menej zdĺhavého posúvania, lepší prehľad a rýchlejšia správa konfigurácie aj pri veľkom množstve nastavení.",
            "notes": "Hold the Forms module and its filtered table. Add the benefit caption during editing.",
            prepare: async ({ I, DT }) => {
                await I.clickCss(`${appsNode} > a.jstree-anchor`);
                await I.waitForElement(`${formsNode} > a.jstree-anchor`, 20);
                await I.clickCss(`${formsNode} > a.jstree-anchor`);
                await I.waitForElement(`${formsNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.waitForText("xhrFileUploadAllowedExtensions", 20, tableWrapper);
            },
            shot: async ({ I }) => {
                await I.seeElement(`${formsNode} > a.jstree-clicked[aria-selected='true']`);
                await I.see("xhrFileUploadAllowedExtensions", tableWrapper);
                await I.wait(6);
            }
        },
        {
            "id": "documentation",
            "type": "auto",
            "durationSeconds": 14,
            "title": "Configuration documentation",
            "text-sk": "Podrobný popis nových pohľadov a práce s konfiguračnými premennými nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.",
            "notes": "Scroll the configuration documentation in the recording tab.",
            shot: async ({ I }) => {
                await I.videoDocumentation("https://docs.webjetcms.sk/latest/sk/admin/setup/configuration/README");
                await I.wait(5);
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

Scenario("293-config-jstree-view", async ({ I, DT, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    await recordVideoPlan(I, {
        plan: videoPlan,
        context: { DT },
        setup: async () => { login("admin"); },
        prepare: async shot => {
            if (shot.id === "documentation") return;
            await I.amOnPage("/admin/v9/settings/configuration/");
            await I.waitForVisible(".configuration-tree-layout > .tree-col", 20);
            await I.waitForElement(`${changedNode} > a.jstree-clicked[aria-selected='true']`, 20);
            await I.waitForVisible(`${tableWrapper} table`, 20);
            DT.waitForLoader();
            // Redact sensitive cells after every draw without changing table data or saved settings.
            await I.executeScript(() => {
                const maskValues = () => {
                    configurationDatatable.rows({ page: "current" }).every(function () {
                        if (!/(password|secret|privatekey|apikey|accesstoken|refreshtoken)/i.test(this.data().name)) return;
                        for (const column of ["value", "oldValue"]) {
                            const cell = configurationDatatable.cell(this.index(), `${column}:name`).node();
                            if (cell?.textContent.trim()) cell.textContent = "••••••••";
                        }
                    });
                };
                configurationDatatable.on("draw.dt.videoMask", maskValues);
                maskValues();
            });
        }
    });
}).tag("@video");
