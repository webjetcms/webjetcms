Feature("video.283-58714-prekladove-kluce-zobrazovat-stromovu-strukturu");

const treeSelector = "#SomStromcek";
const tableSelector = "#datatableInit";
const tableWrapper = `${tableSelector}_wrapper`;
const rootNode = "#translation-key-root-node";
const buttonNode = `${treeSelector} li[data-translation-key-prefix='button']`;
const componentsNode = `${treeSelector} li[data-translation-key-prefix='components']`;
const mapNode = `${treeSelector} li[data-translation-key-prefix='components.map']`;
const widthNode = `${treeSelector} li[data-translation-key-prefix='components.map.width']`;
const gridEditorNode = `${treeSelector} li[data-translation-key-prefix='grideditor']`;

// Move whole shot objects to reorder narration, slates and browser actions.
const videoPlan = {
    "language": "sk",
    "notes": "Durations estimate the edited narration. Cut setup, cleanup and slates; use the runner's transition holds when editing.",
    "shots": [
        {
            "id": "intro",
            "type": "manual",
            "durationSeconds": 5,
            "title": "Find translation keys faster",
            "text-sk": "Keď spravujete veľa prekladových kľúčov, nájsť správnu skupinu v jednom dlhom zozname môže zbytočne zdržiavať.",
            "notes": "Create an opening card: Prekladové kľúče - rýchlejšia orientácia v strome, with the WebJET CMS logo."
        },
        {
            "id": "overview",
            "type": "auto",
            "durationSeconds": 9,
            "title": "The complete list and prefix tree",
            "text-sk": "WebJET CMS preto zobrazuje prefixy prekladových kľúčov v prehľadnej stromovej štruktúre. Naľavo vidíte prefixy, ktoré poznáte z názvov kľúčov. Jednotlivé časti oddelené bodkou vytvárajú prirodzené úrovne stromu.",
            "notes": "Show the selected root and the second page of results.",
            shot: async ({ I, DT }) => {
                await I.see("Všetky prekladové kľúče", treeSelector);
                await I.videoClick(locate(`${tableWrapper} button.page-link`).withText("2"), 0.3);
                await I.waitForElement(locate(`${tableWrapper} li.dt-paging-button.page-item.active button.page-link`).withText("2"), 20);
                DT.waitForLoader();
                await I.wait(5);
            }
        },
        {
            "id": "button-prefix",
            "type": "auto",
            "durationSeconds": 10,
            "title": "Filter familiar button labels",
            "text-sk": "Po otvorení zostáva zvolená možnosť Všetky prekladové kľúče, takže vidíte kompletný obsah. Kliknutím na známy prefix button si okamžite zobrazíte iba texty tlačidiel.",
            "notes": "Select button and show button.add and button.continue.",
            shot: async ({ I, DT }) => {
                await I.videoClick(`${buttonNode} > a.jstree-anchor`, 0.35);
                await I.waitForElement(`${buttonNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.waitForText("button.add", 20, tableSelector);
                await I.see("button.continue", tableSelector);
                await I.wait(7);
            }
        },
        {
            "id": "hierarchy",
            "type": "auto",
            "durationSeconds": 22,
            "title": "Follow the dot-separated hierarchy",
            "text-sk": "Stačí vybrať napríklad prefix components, potom map a napokon width. Tabuľka sa pri každom výbere automaticky zúži. Zostanú v nej iba kľúče patriace do zvolenej vetvy. Nemusíte ručne skladať filter ani prechádzať nesúvisiace výsledky.",
            "notes": "Select components, map and width. Hold each result before continuing.",
            shot: async ({ I, DT }) => {
                await I.videoClick(`${componentsNode} > a.jstree-anchor`, 0.5);
                await I.jstreeWaitForLoader();
                await I.waitForElement(`${mapNode} > a.jstree-anchor`, 20);
                await I.waitForElement(`${componentsNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.wait(3);
                await I.videoClick(`${mapNode} > a.jstree-anchor`, 0.3);
                await I.jstreeWaitForLoader();
                await I.waitForElement(`${widthNode} > a.jstree-anchor`, 20);
                await I.waitForElement(`${mapNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.wait(3);
                await I.videoClick(`${widthNode} > a.jstree-anchor`, 0.45);
                await I.waitForElement(`${widthNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.waitForText("components.map.width.short", 20, tableSelector);
                await I.dontSee("components.map.address", tableSelector);
                await I.wait(7);
            }
        },
        {
            "id": "search",
            "type": "auto",
            "durationSeconds": 15,
            "title": "Search and clear the prefix tree",
            "text-sk": "Ak poznáte názov prefixu, použite vyhľadávanie priamo nad stromom. WebJET CMS zobrazí zodpovedajúcu vetvu a jedným kliknutím opäť upraví obsah tabuľky. Vyhľadávanie môžete kedykoľvek vyčistiť.",
            "notes": "Find grideditor, select it, then clear the search.",
            shot: async ({ I, DT }) => {
                await I.videoClick("#tree-folder-search-input", 0.25);
                await I.fillField("#tree-folder-search-input", "grideditor");
                await I.videoClick("#tree-folder-search-button", 0.4);
                await I.jstreeWaitForLoader();
                await I.waitForElement(`${gridEditorNode} > a.jstree-search`, 20);
                await I.wait(3);
                await I.videoClick(`${gridEditorNode} > a.jstree-anchor`, 0.55);
                await I.jstreeWaitForLoader();
                await I.waitForElement(`${gridEditorNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.waitForText("grideditor.column", 20, tableSelector);
                await I.wait(5);
                await I.videoClick("#tree-folder-search-clear-button", 0.3);
                await I.jstreeWaitForLoader();
                await I.dontSeeInField("#tree-folder-search-input", "grideditor");
                await I.waitForElement(`${componentsNode} > a.jstree-anchor`, 20);
                await I.wait(3);
            }
        },
        {
            "id": "restore-all",
            "type": "auto",
            "durationSeconds": 16,
            "title": "Restore the full list",
            "text-sk": "Možnosť Všetky prekladové kľúče zruší výber prefixu a vráti celý zoznam. Zároveň získate späť celý strom, takže môžete bez zdržania pokračovať v ďalšom hľadaní.\n\nNové usporiadanie zrýchľuje orientáciu, udržiava prehľad aj pri veľkom počte kľúčov a necháva zvolenú vetvu aj výsledky stále na jednej obrazovke.",
            "notes": "Start with a filtered prefix, then return to all keys and hold the complete tree.",
            prepare: async ({ I, DT }) => {
                await I.clickCss(`${buttonNode} > a.jstree-anchor`);
                await I.waitForElement(`${buttonNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.waitForText("button.add", 20, tableSelector);
            },
            shot: async ({ I, DT }) => {
                await I.videoClick(`${rootNode} > a.jstree-anchor`, 0.4);
                await I.waitForElement(`${rootNode} > a.jstree-clicked[aria-selected='true']`, 20);
                DT.waitForLoader();
                await I.waitForElement(`${componentsNode} > a.jstree-anchor`, 20);
                await I.videoClick(locate(`${tableWrapper} button.page-link`).withText("2"), 0.3);
                await I.waitForElement(locate(`${tableWrapper} li.dt-paging-button.page-item.active button.page-link`).withText("2"), 20);
                DT.waitForLoader();
                await I.wait(8);
            }
        },
        {
            "id": "documentation",
            "type": "auto",
            "durationSeconds": 13,
            "title": "Translation key documentation",
            "text-sk": "Podrobný návod na správu prekladových kľúčov nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.",
            "notes": "Scroll the documentation in the recording tab. Add the final documentation caption during editing.",
            shot: async ({ I }) => {
                await I.videoDocumentation("https://docs.webjetcms.sk/latest/sk/admin/settings/translation-keys/README");
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

Scenario("283-prekladove-kluce-zobrazovat-stromovu-strukturu", async ({ I, DT, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    await recordVideoPlan(I, {
        plan: videoPlan,
        context: { DT },
        setup: async () => { login("admin"); },
        prepare: async shot => {
            if (shot.id === "documentation") return;
            await I.amOnPage("/admin/v9/settings/translation-keys/");
            await I.waitForVisible(treeSelector, 20);
            await I.waitForElement(`${rootNode} > a.jstree-clicked[aria-selected='true']`, 20);
            await I.waitForVisible(`${tableWrapper} table`, 20);
            DT.waitForLoader();
        }
    });
}).tag("@video");
