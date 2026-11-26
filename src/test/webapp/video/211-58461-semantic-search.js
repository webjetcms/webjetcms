// PR #211 and task #58521 (PR #242). Seed the DEV folder with assets/211-penzion/prepare.cjs.
Feature("video.211-58461-semantic-search");

const videoPlan = {
    language: "sk",
    shots: [
        {
            id: "otazka", type: "auto", durationSeconds: 36, title: "Ask in your own words",
            "text-sk": "Predstavte si, že si vyberáte penzión na víkend. Cestujete so psom a chcete vedieť aj to, kde zaparkujete. Do vyhľadávania zadáte otázku: Môžeme prísť so psom a nechať auto u vás? Kliknite na Hľadať. Medzi výsledkami vidíte Pobyt so zvieratami aj Dopravu a parkovanie. Už z krátkych ukážok môžete posúdiť, ktorý odkaz otvoriť. Pozrite sa na podmienky pobytu so zvieratami. Penzión vás po predchádzajúcej dohode ubytuje aj so psom. Poplatok je osem eur za noc.",
            prepare: async ({ baseline, asVisitor, visit }) => {
                await baseline(false);
                await asVisitor();
                await visit();
            },
            shot: async ({ I, ask, result, demo }) => {
                await I.wait(4);
                await ask();
                await I.dontSeeElement(".rag-answer");
                await I.wait(7);
                await I.videoClick(result(demo.pages[1]));
                await I.waitForText("8 eur za noc", 15, "main");
                await I.wait(6);
            },
            notes: "Keep both titles and excerpts in frame. Hold the pet policy for the price. Add the native opening AI notice during editing."
        },
        {
            id: "nastavenie", type: "auto", durationSeconds: 38, title: "Enable the answer in the search application",
            "text-sk": "Ak spravujete web, môžete návštevníkovi ponúknuť aj krátku odpoveď priamo pri výsledkoch. V administrácii otvorte stránku s vyhľadávaním. V jej obsahu kliknite na vloženú aplikáciu Vyhľadávanie. Na karte Základné už máte vybratý sémantický typ vyhľadávania. Pri voľbe Pridať odpoveď RAG zatiaľ vidíte Nie. Prepnite ju na Áno. Tým povolíte AI odpoveď pre toto vyhľadávanie. Potvrďte nastavenia aplikácie. Vrátite sa do editora stránky, kde zmenu ešte uložte.",
            prepare: async ({ baseline, asEditor, I, searchURL, demo }) => {
                await baseline(false);
                const expected = new URL(demo.url(demo.pages[1]), searchURL).href;
                if (await I.grabCurrentUrl() !== expected) await I.amOnPage(expected);
                await asEditor();
            },
            shot: async ({ I, DTE, editorListURL, demo }) => {
                await I.amOnPage(editorListURL);
                const pageLink = locate("#datatableInit tbody a").withText(demo.search.title);
                await I.waitForVisible(pageLink, 20);
                await I.wait(3);
                await I.videoClick(pageLink);
                await DTE.waitForEditor();
                await I.clickCss("#pills-dt-datatableInit-content-tab");
                await DTE.waitForCkeditor();
                await I.waitForFunction(() => {
                    const frame = document.querySelector(".cke_wysiwyg_frame")?.contentDocument?.querySelector("iframe.wj_component");
                    return frame && frame.contentWindow.parentNode === frame;
                }, [], 30);
                await I.wait(2);
                await I.switchTo(".cke_wysiwyg_frame");
                await I.switchTo("iframe.wj_component");
                await I.videoClick(".inlineComponentEdit");
                await I.switchTo();
                await I.waitForVisible(".cke_dialog_ui_iframe", 30);
                await I.switchTo(".cke_dialog_ui_iframe");
                await I.switchTo("#editorComponent");
                await DTE.waitForModal("component-datatable_modal");
                await I.seeCheckboxIsChecked('input[name="searchType"][value="semantic"]');
                await I.seeCheckboxIsChecked('input[name="answerAllowed"][value="falseValue"]');
                await I.wait(4);
                const yes = await I.grabAttributeFrom('input[name="answerAllowed"][value="trueValue"]', "id");
                await I.videoClick('label[for="' + yes + '"]');
                await I.seeCheckboxIsChecked('input[name="answerAllowed"][value="trueValue"]');
                await I.wait(2);
                await I.switchTo();
                await I.videoClick(".cke_dialog_ui_button_ok");
                await I.waitForInvisible(".cke_dialog_ui_iframe", 15);
                await I.wait(1);
                await I.videoClick("div.DTED.show .DTE_Footer button.btn-primary");
                await I.waitForInvisible("div.DTED.show", 30);
            },
            notes: "Show the actual application dialog, its two selected radio controls, confirmation and page save. Keep window transitions full-frame."
        },
        {
            id: "odpoved", type: "auto", durationSeconds: 46, title: "Read the answer and verify its source",
            "text-sk": "Vráťte sa na verejnú stránku penziónu a položte tú istú otázku. Čo sa po uložení zmenilo? Nad zoznamom stránok si teraz môžete prečítať AI odpoveď. Na jednom mieste sa dozviete, že pobyt so psom si máte dohodnúť vopred a stojí osem eur za noc. Ako ubytovaní hostia môžete zaparkovať vo dvore zadarmo. Sémantické vyhľadávanie vybralo stránky podľa významu vašej otázky. Z ich obsahu AI zostavila stručné zhrnutie. Pod odpoveďou máte naďalej oba odkazy. Otvorte si Dopravu a parkovanie. Priamo v podmienkach penziónu si môžete overiť, kde je parkovisko a pre koho je bezplatné.",
            prepare: async ({ I, baseline, asVisitor, searchURL }) => {
                // Only a standalone retake needs to establish the saved state.
                if (process.env.VIDEO_SHOT?.trim()) await baseline(true);
                await asVisitor();
                if (await I.grabCurrentUrl() === "about:blank") await I.amOnPage(searchURL);
            },
            shot: async ({ I, visit, ask, result, demo, checkAnswer }) => {
                await visit();
                await ask();
                await I.waitForVisible(".rag-answer .card-text", 75);
                checkAnswer(await I.grabTextFrom(".rag-answer .card-text"));
                await I.wait(12);
                await I.scrollTo(result(demo.pages[2]));
                await I.wait(3);
                await I.videoClick(result(demo.pages[2]));
                await I.waitForText("bezplatné", 15, "main");
                await I.wait(5);
            },
            notes: "Keep the generated answer intact and readable. Check its fee, advance agreement and parking against both source pages before export."
        },
        {
            id: "dokumentacia", type: "auto", durationSeconds: 15, title: "Find the documentation",
            "text-sk": "Na vašom webe sa tak môžete pýtať vlastnými slovami, aj keď nepoznáte názvy stránok. Ak chcete takéto vyhľadávanie pripraviť pre svojich návštevníkov, postup a technické nastavenia nájdete v dokumentácii WebJET CMS v časti Sémantické vyhľadávanie.",
            shot: async ({ I, docsURL }) => {
                await I.amOnPage(docsURL);
                await I.waitForVisible("article h1", 30);
                await I.waitForText("Sémantické vyhľadávanie", 20, "article h1");
                await I.wait(7);
            },
            notes: "Hold the article title and introduction. Append the entire standard WebJET animation after the voice and closing hold."
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan");
    I.say(formatShotPlan(videoPlan));
});

Scenario("YouTube thumbnail", async ({ I }) => {
    if (process.argv.includes("dry-run")) return;
    const { contextFor } = require("./assets/211-penzion/recording.cjs");
    const context = await contextFor(I);
    let wasEnabled;
    try {
        wasEnabled = await context.baseline(true);
        await context.visit();
        await context.ask();
        await I.waitForVisible(".rag-answer .card-text", 75);
        context.checkAnswer(await I.grabTextFrom(".rag-answer .card-text"));
        await I.usePlaywrightTo("frame the visitor's question and AI answer", async ({ page }) => {
            await page.setViewportSize({ width: 980, height: 585 });
            await page.locator("#searchWords").evaluate(field => {
                window.scrollBy(0, field.getBoundingClientRect().top - 20);
            });
        });
        await I.videoTitle("Spýtajte sa webu\nvlastnými slovami", 66, "glow");
    } finally {
        try {
            if (wasEnabled === false) await context.baseline(false);
        } finally {
            await context.close();
        }
    }
}).tag("@title");

Scenario("211-58461-semantic-search", async ({ I, DTE }) => {
    if (process.argv.includes("dry-run")) return;
    const { recordVideoPlan } = require("../helpers/feature_video_plan");
    const { contextFor } = require("./assets/211-penzion/recording.cjs");
    const context = process.env.VIDEO_SHOT?.trim() === "dokumentacia"
        ? { docsURL: require("./assets/211-penzion/recording.cjs").docsURL } : await contextFor(I);
    try {
        await recordVideoPlan(I, { plan: videoPlan, context: { ...context, DTE } });
    } finally {
        if (context.close) await context.close();
    }
}).tag("@video");
