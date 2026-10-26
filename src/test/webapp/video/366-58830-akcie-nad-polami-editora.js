Feature("video.366-58830-akcie-nad-polami-editora");

// Source: https://github.com/webjetcms/webjetcms/pull/366
// Documentation source (the new article is not published on /latest yet):
// https://github.com/webjetcms/webjetcms/blob/feature/58830-akcie-nad-polami-editora/docs/sk/frontend/datatables/fields-settings.md
// Commands from src/test/webapp: npm run video:plan|video|audio|video:title video/366-58830-akcie-nad-polami-editora.js
const videoPlan = {
    language: "sk",
    notes: "Four continuous topic shots, 16:9 Full HD. Cut runner slates, setup and cleanup. Keep the UI and all three field creations in sequence. Add the opening AI disclosure and the complete standard outro during editing. Temporary settings are removed after recording; page 57 is never saved.",
    shots: [
        {
            id: "intro",
            type: "auto",
            durationSeconds: 21,
            title: "Adapt the editor to your needs",
            "text-sk": "Potrebujete v editore iné rozloženie polí? Vo WebJET CMS si ho teraz prispôsobíte priamo v administrácii. Môžete zmeniť názov poľa, presunúť ho do inej karty alebo skryť položky, ktoré nepoužívate. Ukážeme vám, ako si vytvoríte kartu SEO a sústredíte v nej tri súvisiace polia.",
            notes: "Hold the Fields settings screen while introducing the available options and the upcoming SEO example. Do not simulate renaming or hiding fields.",
            prepare: async ({ fixture }) => { await fixture.open("fields"); },
            shot: async ({ I }) => {
                await I.see("Nastavenia polí");
                await I.wait(19); // Readable opening background for narration, not a readiness wait.
            }
        },
        {
            id: "create-seo-tab",
            type: "auto",
            durationSeconds: 26,
            title: "Create the SEO tab after Basic",
            "text-sk": "V Nastaveniach kariet pridajte nový záznam. Pre webové stránky vyberte entitu DocDetails. Ako identifikátor novej karty zadajte seo a názov nastavte na SEO. Do poľa Zobraziť za kartou zadajte basic. Nová karta sa tak zaradí hneď za kartu Základné. Ostatné nastavenia môžete ponechať bez zmeny a záznam uložte.",
            notes: "Stay in one flow: switch settings section, create, enter DocDetails / seo / SEO / basic and save. Keep the full modal and Save button visible at the native recording zoom.",
            prepare: async ({ fixture }) => { await fixture.open("fields"); },
            shot: async ({ I, DT, DTE, fixture, fill, save }) => {
                // Narration cues: section and Add; entity; tab ID/title; ordering; save.
                await I.videoClick('a[href="/admin/v9/settings/tabs-settings/"]:visible');
                await DT.waitForLoader(fixture.tables.tabs);
                await I.videoClick("#tabsSettingsDataTable_wrapper button.buttons-create");
                await DTE.waitForEditor(fixture.tables.tabs);
                await fill("className", fixture.entity);
                await fill("tabId", "seo");
                await fill("title", "SEO");
                await fill("sortAfter", "basic");
                await I.clickCss("#tabsSettingsDataTable_modal .DTE_Header");
                await I.seeInField("#DTE_Field_sortAfter", "basic");
                await I.wait(3);
                await save("tabs", "seo");
                await I.see("SEO", "#tabsSettingsDataTable tbody");
            }
        },
        {
            id: "move-seo-fields",
            type: "auto",
            durationSeconds: 30,
            title: "Move three existing fields into SEO",
            "text-sk": "Teraz prejdite do Nastavení polí a pridajte záznam pre rovnakú entitu. Vyberte pole field R. V položke Vložiť do karty zadajte seo a uložte. Rovnakým postupom pridajte aj pole field S a presuňte ho do karty seo. Nakoniec nastavte rovnako pole field T. Pri všetkých troch poliach meníte iba ich umiestnenie. Názvy a ostatné vlastnosti zostávajú zachované.",
            notes: "Show all three creations without returning to a different screen. First field at normal explanatory pace; keep S and T readable. End on the saved rows, including the seo column.",
            prepare: async ({ fixture }) => { await fixture.ensureTab(); },
            shot: async ({ I, DT, DTE, fixture, fill, save }) => {
                await I.videoClick('a[href="/admin/v9/settings/fields-settings/"]:visible');
                await DT.waitForLoader(fixture.tables.fields);
                for (const field of fixture.fields) {
                    // Each narrated field is selected, assigned to seo, then saved on camera.
                    await I.videoClick("#fieldsSettingsDataTable_wrapper button.buttons-create");
                    await DTE.waitForEditor(fixture.tables.fields);
                    await fill("className", fixture.entity);
                    await fill("fieldName", field);
                    await fill("tab", "seo");
                    await I.clickCss("#fieldsSettingsDataTable_modal .DTE_Header");
                    await I.seeInField("#DTE_Field_tab", "seo");
                    await I.wait(field === "fieldR" ? 2 : 1);
                    await save("fields", field);
                    await I.see(field, "#fieldsSettingsDataTable tbody");
                }
                await I.wait(3);
            }
        },
        {
            id: "show-seo-result",
            type: "auto",
            durationSeconds: 25,
            title: "Show the SEO tab in page 57",
            "text-sk": "Otvorte webovú stránku v editore. Hneď za kartou Základné už vidíte novú kartu SEO. Kliknite na ňu a nájdete všetky tri presunuté polia. Súvisiace údaje máte na jednom mieste a rozhranie si viete usporiadať podľa potrieb vašej inštalácie. Ďalšie možnosti nastavenia nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.",
            notes: "Navigate to the requested docid=57 inside the shot, show SEO directly after Basic, click SEO and hold all three fields through the closing narration, then append the standard outro. Do not add a documentation text overlay or save the page.",
            prepare: async ({ fixture }) => { await fixture.ensureFields(); },
            shot: async ({ I, DTE, fixture }) => {
                await I.amOnPage(fixture.urls.page);
                await DTE.waitForEditor();
                await I.seeElement('#pills-dt-editor-datatableInit li:has(> #pills-dt-datatableInit-basic-tab) + li > #pills-dt-datatableInit-seo-tab');
                await I.wait(3);
                await I.videoClick("#pills-dt-datatableInit-seo-tab");
                for (const field of fixture.fields) {
                    await I.seeElement(`#pills-dt-datatableInit-seo #DTE_Field_${field}`);
                    await I.dontSeeElementInDOM(`#pills-dt-datatableInit-fields #DTE_Field_${field}`);
                }
                await I.wait(18); // Read the complete result and closing narration.
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

Scenario("YouTube thumbnail", async ({ I, DT, DTE, login }) => {
    const fixture = require("../helpers/fields_settings_video_fixture.js")(I, DT, DTE);
    login("admin");
    try {
        await fixture.checkBaseline();
        await fixture.ensureFields();
        await I.amOnPage(fixture.urls.page);
        await DTE.waitForEditor();
        await I.clickCss("#pills-dt-datatableInit-seo-tab");
        for (const field of fixture.fields) await I.seeElement(`#pills-dt-datatableInit-seo #DTE_Field_${field}`);
        await I.waitForFunction(() => getComputedStyle(document.querySelector("#pills-dt-datatableInit-seo")).opacity === "1", 10);
        await I.videoTitle("Polia a karty\npodľa vás", 58, "glow");
    } finally {
        await fixture.cleanup();
    }
}).tag("@title");

Scenario("366-58830-akcie-nad-polami-editora", async ({ I, DT, DTE, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const fixture = require("../helpers/fields_settings_video_fixture.js")(I, DT, DTE);
    const fill = async (field, value) => {
        await I.videoClick(`#DTE_Field_${field}`);
        await I.fillField(`#DTE_Field_${field}`, value);
        await I.pressKey("Tab");
        await I.seeInField(`#DTE_Field_${field}`, value);
    };
    const save = async (kind, key) => {
        const table = fixture.tables[kind];
        await I.videoClick(`#${table}_modal .DTE_Footer button.btn-primary`);
        await DTE.waitForLoader(table);
        await DT.waitForLoader(table);
        await I.waitForInvisible(`#${table}_modal`, 20);
        await fixture.remember(kind, key);
    };
    try {
        await recordVideoPlan(I, {
            plan: videoPlan,
            context: { fixture, fill, save },
            setup: async () => {
                login("admin");
                await fixture.checkBaseline();
            }
        });
    } finally {
        await fixture.cleanup();
    }
}).tag("@video");
