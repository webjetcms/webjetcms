Feature("video.274-58637-volitelne-polia-multiselect-ciselnik");

// WebJET changes #58529 (PR 253) and #58637 (PR 274).
const videoPlan = {
    language: "sk",
    shots: [
        {
            id: "nastavenie", type: "auto", durationSeconds: 70, title: "Configure the service type for one page",
            "text-sk": "Ako zabezpečíte, aby redaktori označovali služby na webe jednotne? Vo WebJET CMS im môžete pripraviť pole s hotovými možnosťami. V Nastaveniach otvorte Dátové tabuľky a prejdite na kartu Voliteľné polia. Pridajte nový záznam. Voľbou entity určíte, pre ktorý druh záznamov pole nastavujete. Pre webové stránky vyberte DocDetails. Vyberte voľné písmeno a do poľa ID entity zadajte číslo stránky Školenie pre redaktorov. Toto nastavenie tak bude platiť len pre ňu. Ako typ poľa vyberte Prepínač. Pod názvom Typ služby redaktor uvidí možnosti, z ktorých si môže vybrať jednu. Do poľa Tooltip poľa napíšte krátku nápovedu a zapnite Povinné pole. Ako zdroj možností vyberte Číselník. Zadajte ID pripraveného číselníka Typy služieb. V stĺpci Názov vyberte prvý reťazec, ktorý obsahuje názov služby. V stĺpci Hodnota vyberte druhý reťazec. Ten obsahuje stabilný kód, ktorý sa uloží so stránkou. Nastavenie pridajte.",
            notes: "Show the complete navigation, page scope, label, hint, required switch and both column pickers before submitting.",
            prepare: async ({ demo }) => { await demo.listPage(); },
            shot: async ({ I, DT, DTE, demo }) => {
                const { f, cue, fill, pick } = demo;
                await cue("start");
                await I.wait(3);
                await I.videoClick('a[data-menu-id="config"]');
                await cue("settings");
                await I.videoClick('.md-main-menu--open a[href="/admin/v9/settings/fields-settings/"]');
                await I.waitForVisible('a.nav-link[href="/admin/v9/settings/custom-fields/"]', 12);
                await I.videoClick('a.nav-link[href="/admin/v9/settings/custom-fields/"]');
                await DT.waitForLoader("customFieldsDataTable");
                await cue("settings-table");
                await I.videoClick("#customFieldsDataTable_wrapper button.buttons-create");
                await DTE.waitForEditor("customFieldsDataTable");
                await cue("add-rule");
                await fill("#DTE_Field_className", "DocDetails");
                const entity = locate("ul.dt-autocomplete-select li div").withText(demo.className);
                await I.waitForVisible(entity, 10);
                await I.videoClick(entity);
                await cue("entity");
                await pick('button[data-id="DTE_Field_alphabet"]', f.letter);
                await fill("#DTE_Field_entityId", String(f.docId));
                await cue("scope");
                await pick('button[data-id="DTE_Field_type"]', "Prepínač");
                await cue("radio");
                await fill("#DTE_Field_label", "Typ služby");
                await cue("label");
                await fill("#DTE_Field_tooltip", demo.tooltip);
                await cue("hint");
                await I.videoClick("#DTE_Field_required_0");
                await I.seeCheckboxIsChecked("#DTE_Field_required_0");
                await cue("required");
                await I.videoClick('.DTE_Field_Name_optionsSource input[value="enumeration"]');
                const link = "#DTE_Field_enumeration ";
                await I.waitForVisible(link + "input", 10);
                await fill(link + "input", String(f.typeId));
                await cue("enum-id");
                await pick(link + ".options-value-2 button.dropdown-toggle", "string1");
                await cue("name-column");
                await pick(link + ".options-value-3 button.dropdown-toggle", "string2");
                await cue("value-column");
                await demo.save("customFieldsDataTable");
                await I.waitForText("Typ služby", 10, "#customFieldsDataTable");
                await I.assertTrue(Boolean(await f.rule()), "The scoped rule was saved by the editor");
                await cue("saved-rule");
            }
        },
        {
            id: "redaktor", type: "auto", durationSeconds: 37, title: "Validate the empty choice and reopen the saved page",
            "text-sk": "Teraz otvorte stránku Školenie pre redaktorov. Na karte Voliteľné polia už vidíte Typ služby a tri možnosti: Konzultácia, Školenie a Technická podpora. Hviezdička pri názve označuje povinný údaj. Ak prejdete na informačnú ikonu, zobrazí sa vaša nápoveda. Skúste stránku uložiť, kým je výber prázdny. Pri poli sa zobrazí hlásenie: Voliteľné pole je nastavené ako povinné. Vyberte Školenie a stránku uložte. Keď ju znova otvoríte a vrátite sa na kartu Voliteľné polia, Školenie zostane označené.",
            notes: "Keep the real tooltip and inline error readable. Reopen the saved document and check both the checked radio and its stored code.",
            prepare: async ({ demo }) => { await demo.f.ensureRule(); await demo.listPage(); },
            shot: async ({ I, demo }) => {
                await demo.openFromList();
                await demo.fields();
                await demo.checkOptions(3, "");
                await demo.cue("three");
                await I.moveCursorTo(demo.box() + " button.btn-tooltip");
                await I.waitForText(demo.tooltip, 10, ".tooltip.show");
                await demo.cue("tooltip");
                await I.wait(3);
                await I.videoClick("#datatableInit_modal .DTE_Footer button.btn-primary");
                await I.waitForText("Voliteľné pole je nastavené ako povinné.", 12, demo.box() + " .text-danger");
                await demo.cue("error");
                await I.wait(3);
                await I.videoClick(demo.box() + ' input[value="skolenie"]');
                await demo.cue("chosen");
                await demo.save("datatableInit");
                await demo.checkValue();
                await demo.cue("page-saved");
                await demo.reopen();
                await demo.fields();
                await demo.checkOptions(3, "skolenie");
                await demo.cue("persisted");
            }
        },
        {
            id: "ciselnik", type: "auto", durationSeconds: 31, title: "Extend the shared service list",
            "text-sk": "Keď pribudne ďalšia služba, otvorte číselník Typy služieb. V jeho dátach vidíte pri každom názve aj príslušný kód. Pridajte záznam, zadajte názov Servisný zásah a kód servis. Po uložení pribudne v tabuľke štvrtý riadok. Znova otvorte stránku a prejdite na Voliteľné polia. Servisný zásah už máte v ponuke a Školenie zostalo vybraté. Zoznam možností môžete spravovať na jednom mieste, pretože pole ho načítava z číselníka.",
            notes: "Show both enumeration columns, add the fourth row and verify all four real radio labels on the reopened page.",
            prepare: async ({ I, demo }) => {
                await demo.f.ensureRule();
                await demo.f.value("skolenie");
                await I.amOnPage("/apps/enumeration/admin/#" + demo.f.typeId);
                await I.waitForVisible("#tree-folder-search-input", 12);
                await I.fillField("#tree-folder-search-input", "Typy služieb");
                await I.clickCss("#tree-folder-search-button");
                await I.waitForFunction(() => document.querySelectorAll("#SomStromcek a.jstree-anchor").length === 1, 12);
            },
            shot: async ({ I, DTE, demo }) => {
                await demo.cue("enum-ready");
                await I.videoClick('#SomStromcek a[title="Typy služieb"]');
                await I.seeInCurrentUrl("#" + demo.f.typeId);
                await I.waitForText("Technická podpora", 15, "#enumerationDataDataTable");
                await I.seeElement('#SomStromcek a.jstree-clicked[title="Typy služieb"]');
                await demo.cue("enum-list");
                await I.videoClick("#enumerationDataDataTable_wrapper button.buttons-create");
                await DTE.waitForEditor("enumerationDataDataTable");
                await demo.fill("#DTE_Field_fieldA", "Servisný zásah");
                await demo.cue("service-name");
                await demo.fill("#DTE_Field_fieldB", "servis");
                await demo.cue("service-code");
                await demo.save("enumerationDataDataTable");
                await I.waitForText("Servisný zásah", 10, "#enumerationDataDataTable");
                await demo.cue("four-rows");
                await demo.reopen();
                await demo.fields();
                await demo.checkOptions(4, "skolenie");
                await demo.checkValue();
                await demo.cue("four-options");
                await I.wait(3);
            }
        },
        {
            id: "dokumentacia", type: "auto", durationSeconds: 21, title: "Find other field types in the documentation",
            "text-sk": "Názov, nápovedu, povinnosť aj spôsob výberu nastavíte priamo v administrácii. Ak potrebujete rozbaľovací zoznam alebo výber viacerých možností, môžete siahnuť po ďalších typoch polí. Ich prehľad aj postup nastavenia nájdete v dokumentácii WebJET CMS v časti Voliteľné polia.",
            notes: "Review the saved settings, open the real type picker and finish on the Slovak documentation article. Append the full brand animation.",
            prepare: async ({ I, DTE, DT, demo }) => {
                await demo.f.ensureRule();
                await I.amOnPage("/admin/v9/settings/custom-fields/");
                await DT.waitForLoader("customFieldsDataTable");
                await DT.filterEquals("tooltip", demo.tooltip, "#customFieldsDataTable_wrapper");
                await I.waitForText("Typ služby", 10, "#customFieldsDataTable tbody");
                await I.click(locate("#customFieldsDataTable tbody tr").withText("Typ služby").find("td:first-child"));
                await I.clickCss("#customFieldsDataTable_wrapper button.buttons-edit");
                await DTE.waitForEditor("customFieldsDataTable");
                await I.seeInField("#DTE_Field_entityId", String(demo.f.docId));
            },
            shot: async ({ I, demo }) => {
                await I.seeInField("#DTE_Field_label", "Typ služby");
                await I.seeCheckboxIsChecked("#DTE_Field_required_0");
                await demo.cue("recap");
                await I.wait(2);
                await I.videoClick('button[data-id="DTE_Field_type"]');
                await I.waitForText("Výberové pole viac možností", 10, ".dropdown-menu.show");
                await demo.cue("type-picker");
                await I.wait(3);
                await I.pressKey("Escape");
                await I.amOnPage("https://docs.webjetcms.sk/latest/sk/frontend/datatables/custom-fields-settings");
                await I.waitForText("Voliteľné polia", 20, "article h1");
                await I.usePlaywrightTo("wait for the first illustration and the article heading", async ({ page }) => {
                    await page.locator("article img").first().evaluate(img => img.decode());
                    await page.evaluate(() => document.fonts.ready);
                });
                await demo.cue("guide");
                await I.wait(3);
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

Scenario("YouTube thumbnail", async ({ I, DTE, login }) => {
    const { recording } = require("./assets/274-typ-sluzby/recording.cjs");
    if (process.argv.includes("dry-run")) return;
    const demo = recording(I, DTE);
    try {
        login("admin");
        await demo.setup();
        await demo.f.ensureRule();
        await demo.f.value("skolenie");
        await I.usePlaywrightTo("frame the page title and three service choices", async ({ page }) => {
            await page.setViewportSize({ width: 1512, height: 780 });
        });
        await demo.reopen();
        await I.clickCss("#pills-dt-datatableInit-fields-tab");
        await demo.checkOptions(3, "skolenie");
        await I.waitForFunction(() => getComputedStyle(document.querySelector("#pills-dt-datatableInit-fields")).opacity === "1", 10);
        await I.waitForVisible(demo.box() + " input[type=radio][value=skolenie]", 10);
        await demo.frameThumbnail();
        await I.videoTitle("Vlastné polia\nv administrácii", 62, "glow", { fitScene: true });
    } finally { await demo.close(); }
}).tag("@title");

Scenario("274-58637-volitelne-polia-multiselect-ciselnik", async ({ I, DTE, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const { recording } = require("./assets/274-typ-sluzby/recording.cjs");
    if (process.argv.includes("dry-run")) return;
    const demo = recording(I, DTE);
    try {
        await recordVideoPlan(I, { plan: videoPlan, context: { demo }, setup: async () => {
            login("admin");
            await demo.setup();
        } });
    } finally { await demo.close(); }
}).tag("@video");
