Feature("video.356-osk764-styles-combo");

// Source: https://github.com/webjetcms/webjetcms/pull/356
// Documentation: https://docs.webjetcms.sk/latest/sk/redactor/webpages/working-in-editor/
// One continuous manual walkthrough. No page content is saved.
// Record with the default headed Chromium: headless mode ignores this profile's native zoom.
// From src/test/webapp: npm run video video/356-osk764-styles-combo.js
const videoPlan = {
    language: "sk",
    notes: "Návod do manuálu, 16:9. Jeden súvislý záber bez návratov na začiatok. Vystrihnúť prípravu, obe klapky a záverečné zatvorenie editora. Pri finálnom strihu pripojiť štandardné outro assets/outro.mp4. Schválený slovenský hlas má 105,36 s; obraz časovať podľa konkrétnych pokynov v nahrávke.",
    shots: [
        {
            id: "styles-walkthrough",
            type: "auto",
            durationSeconds: 106,
            title: "Choose, apply and remove styles in Page Builder",
            "text-sk": `Vo WebJET CMS môžete nastaviť vzhľad textu aj celého kontajnera. Kliknite do odseku a otvorte ponuku Štýly v paneli nástrojov.
Kliknutím na Zbaliť všetko zobrazíte prehľad skupín. Nájdete tu Formát odseku, Farbu textu, Pozadie aj Orámovanie. Názvy a dostupné štýly závisia od šablóny stránky.
Rozbaľte Farbu textu. Pri každom štýle vidíte malý náhľad, podľa ktorého si môžete vybrať jeho vzhľad.
Do vyhľadávania napíšte zelený. Ponuka vyhľadáva podľa názvu aj CSS triedy. Vyberte Zelený text odseku. Výsledok ihneď vidíte v obsahu a použitý štýl v hornej časti ponuky.
Vyhľadávanie vymažte a zapnite Všetky štýly. Vyhľadajte kontajnera. Zobrazí sa aj štýl, ktorý na odsek nemožno použiť, preto je neaktívny.
Vyhľadávanie opäť vymažte a Všetky štýly vypnite. Ak chcete upraviť väčšiu časť stránky, kliknite v hornej ceste na Kontajner.
Vyhľadajte pozadie a vyberte Jemné varovné pozadie. Farba sa použije na celý kontajner, nie iba na odsek.
Pozadie zrušíte kliknutím na Odstrániť pri skupine Farba pozadia. Kontajner sa vráti k pôvodnému pozadiu.
V hornej ceste sa vráťte na Odsek. Farbu textu odstránite krížikom pri názve aktívneho štýlu.
Ponuku zatvoríte krížikom. Ďalšie možnosti práce so štýlmi nájdete v príslušnej časti manuálu.`,
            notes: "Otvoriť Štýly nad pravým odsekom. Zbaliť skupiny a ukázať ich názvy, rozbaliť Farbu textu a náhľady. Vyhľadať zelený a použiť štýl. Zapnúť Všetky štýly a vyhľadať neaktívne Modré pozadie kontajnera. Vymazať filter, vypnúť Všetky štýly, vybrať Kontajner, vyhľadať pozadie a použiť Jemné varovné pozadie. Odstrániť pozadie cez skupinu, vrátiť sa na Odsek a odstrániť zelený štýl cez čip. Zatvoriť ponuku. Všetko v jednom priechode; bez uloženia stránky.",
            shot: async ({ I, shot, paragraph, container, panel, group, item, search, cue }) => {
                // Each cue follows one line of the canonical narration above.
                await cue(shot, 1, async () => {
                    await I.videoClick(paragraph);
                    await I.waitForVisible(".cke_button__wjstyles:visible", 10);
                    await I.videoClick(".cke_button__wjstyles:visible");
                    await I.waitForVisible(panel, 10);
                });
                await cue(shot, 2, async () => {
                    await I.videoClick(`${panel} .wj-styles__toggle-all`);
                    await I.see("Rozbaliť všetko", panel);
                    for (const name of ["Formát odseku", "Farba textu", "Pozadie", "Orámovanie"]) {
                        await I.seeElement(group(name));
                    }
                });
                await cue(shot, 3, async () => {
                    await I.videoClick(group("Farba textu"));
                    await I.waitForVisible(item("Zelený text odseku"), 10);
                    await I.seeElement(`${panel} .wj-styles__group-content:not([hidden]) .wj-styles__sample`);
                    await I.moveCursorTo(item("Zelený text odseku"));
                });
                await cue(shot, 4, async () => {
                    await I.videoClick(search);
                    await I.fillField(search, "zelený");
                    await I.waitForVisible(item("Zelený text odseku"), 10);
                    await I.wait(2); // Reading hold for the filtered result, not application synchronization.
                    await I.videoClick(item("Zelený text odseku"));
                    await I.waitForElement(`${paragraph}.paragraph-green`, 10);
                    await I.see("Zelený text odseku", `${panel} .wj-styles__chips`);
                });
                await cue(shot, 5, async () => {
                    await I.videoClick(`${panel} .wj-styles__search-clear`);
                    await I.videoClick(`${panel} .wj-styles__filters label`);
                    await I.seeCheckboxIsChecked(`${panel} .wj-styles__filters input`);
                    await I.videoClick(search);
                    await I.fillField(search, "kontajnera");
                    await I.waitForVisible(item("Modré pozadie kontajnera"), 10);
                    await I.seeElement(locate(`${panel} .wj-styles__item[disabled]`).withText("Modré pozadie kontajnera"));
                });
                await cue(shot, 6, async () => {
                    await I.videoClick(`${panel} .wj-styles__search-clear`);
                    await I.videoClick(`${panel} .wj-styles__filters label`);
                    await I.dontSeeCheckboxIsChecked(`${panel} .wj-styles__filters input`);
                    await I.videoClick(`${panel} .wj-styles__target-option[data-type=container]`);
                    await I.seeElement(`${panel} .wj-styles__target-option[data-type=container][aria-pressed=true]`);
                });
                await cue(shot, 7, async () => {
                    await I.videoClick(search);
                    await I.fillField(search, "pozadie");
                    await I.waitForVisible(item("Jemné varovné pozadie"), 10);
                    await I.videoClick(item("Jemné varovné pozadie"));
                    await I.waitForElement(`${container}.bg-warning-subtle`, 10);
                    await I.seeElement(`${paragraph}.paragraph-green`);
                });
                await cue(shot, 8, async () => {
                    await I.videoClick(`${panel} button[aria-label="Odstrániť: Farba pozadia"]`);
                    await I.dontSeeElement(`${container}.bg-warning-subtle`);
                    await I.seeElement(`${paragraph}.paragraph-green`);
                });
                await cue(shot, 9, async () => {
                    await I.videoClick(locate(`${panel} .wj-styles__target-option`).withText("Odsek"));
                    await I.waitForVisible(`${panel} .wj-styles__chip`, 10);
                    await I.videoClick(locate(`${panel} .wj-styles__chip`).withText("Zelený text odseku"));
                    await I.dontSeeElement(`${paragraph}.paragraph-green`);
                    await I.see("Bez zvolených štýlov", `${panel} .wj-styles__chips`);
                });
                await cue(shot, 10, async () => {
                    await I.videoClick(`${panel} .wj-styles__close`);
                    await I.waitForInvisible(panel, 10);
                    await I.seeElement(paragraph);
                });
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

/** Prepares temporary page content shared by the walkthrough and thumbnail. */
function stylesVideoContext({ I, DTE }) {
    const iframe = "#DTE_Field_data-pageBuilderIframe";
    const fixture = ".styles-video-autotest";
    const paragraph = `${fixture} .video-paragraph`;
    const container = `${fixture} > .container`;
    const panel = ".wj-styles:not([hidden])";
    const search = `${panel} .wj-styles__search`;
    const group = name => locate(`${panel} .wj-styles__group-toggle`).withText(name);
    const item = name => locate(`${panel} .wj-styles__item`).withText(name);
    // CodeceptJS 3.6's FrameLocator does not implement waitForFunction.
    const waitForEditorFrame = predicate => I.usePlaywrightTo("wait for the Page Builder fixture", async ({ page }) => {
        const frame = await (await page.locator(iframe).elementHandle()).contentFrame();
        await frame.waitForFunction(predicate, null, { timeout: 20000 });
    });
    // Reading holds derive from the actual narration; measured speech still needs a final timing pass.
    const cue = async (shot, number, action) => {
        const lines = shot.narration.split("\n").filter(line => line.trim());
        const words = text => text.trim().split(/\s+/u).length;
        const targetSeconds = shot.durationSeconds * words(lines[number - 1]) / words(shot.narration);
        await I.say(`Narration cue ${number}/${lines.length}`);
        const start = Date.now();
        await action();
        await I.wait(Math.max(1.5, targetSeconds - (Date.now() - start) / 1000));
    };
    const prepareEditor = async () => {
        await I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
        await DTE.waitForEditor();
        await I.clickCss("div.DTED.show button.maximize");
        await I.waitForInvisible("div.DTED.show button.maximize", 10);
        await I.switchTo(iframe);
        await I.waitForVisible(".pb-workbench", 20);
        await I.clickCss("a[title=Desktop]");
        await I.executeScript(() => {
            const wrapper = document.querySelector("#wjInline-docdata");
            wrapper.querySelectorAll("[data-ckeditor-instance]").forEach(element => {
                CKEDITOR.instances[element.dataset.ckeditorInstance]?.destroy(true);
            });
            wrapper.querySelectorAll(":scope > .pb-section").forEach(element => element.remove());
            wrapper.insertAdjacentHTML("afterbegin", `
                <section class="py-4 styles-video-autotest">
                    <div class="container">
                        <div class="row align-items-center">
                            <div class="col-12 col-md-6"><div class="column-content">
                                <img src="/components/htmlbox/objects/placeholder/content-computer.jpg" class="img-fluid" alt="Príprava webovej stránky">
                            </div></div>
                            <div class="col-12 col-md-6"><div class="column-content">
                                <h2>Vzhľad vašej stránky</h2>
                                <p class="video-paragraph">Upravte vzhľad textu a zvýraznite dôležité informácie. Vyberte si farbu, pozadie alebo orámovanie pripravené pre vašu stránku.</p>
                                <p>Jednotný vzhľad pomôže návštevníkom zorientovať sa v obsahu.</p>
                            </div></div>
                        </div>
                    </div>
                </section>
            `);
            window.markPbElements("doc_data");
            window.pageBuilder.set_workbench_guides("hidden", false);
        });
        await waitForEditorFrame(() => {
            const fields = Array.from(document.querySelectorAll(".styles-video-autotest [data-ckeditor-instance]"));
            return fields.length === 2 && fields.every(field => CKEDITOR.instances[field.dataset.ckeditorInstance]?.status === "ready");
        });
        await I.clickCss(paragraph);
        await I.waitForVisible(".cke_button__wjstyles:visible", 10);
        await waitForEditorFrame(() => {
            const field = document.querySelector(".video-paragraph").closest("[data-ckeditor-instance]");
            const picker = CKEDITOR.instances[field.dataset.ckeditorInstance]?.wjstylescombo;
            return picker?.items.some(item => item.classes.includes("bg-warning-subtle"));
        });
        await I.executeScript(() => Promise.all([document.fonts.ready, ...Array.from(document.querySelectorAll(".styles-video-autotest img")).map(image => image.decode())]).then(() => true));
    };
    const closeEditor = async () => {
        await I.switchTo();
        await DTE.cancel();
        await I.waitForInvisible("div.DTED.show", 10);
    };
    return { paragraph, container, panel, group, item, search, cue, prepareEditor, closeEditor };
}

Scenario("YouTube thumbnail", async ({ I, DTE, login }) => {
    const context = stylesVideoContext({ I, DTE });
    await login("admin");
    await context.prepareEditor();
    await I.clickCss(".cke_button__wjstyles:visible");
    await I.waitForVisible(context.panel, 10);
    await I.clickCss(`${context.panel} .wj-styles__toggle-all`);
    await I.click(context.group("Farba textu"));
    await I.waitForVisible(context.item("Zelený text odseku"), 10);
    await I.videoTitle("Ako nastaviť\nštýly stránky", 50, "glow", { fitScene: true });
    await context.closeEditor();
}).tag("@title");

Scenario("356-osk764-styles-combo", async ({ I, DTE, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const context = stylesVideoContext({ I, DTE });
    await recordVideoPlan(I, {
        plan: videoPlan,
        context,
        setup: async () => {
            await login("admin");
        },
        prepare: context.prepareEditor,
        cleanup: context.closeEditor
    });
}).tag("@video");
