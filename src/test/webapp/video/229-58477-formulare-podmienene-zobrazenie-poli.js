Feature("video.229-58477-formulare-podmienene-zobrazenie-poli");

const videoPlan = {
    language: "sk",
    shots: [
        {
            id: "zobrazenie", type: "auto", durationSeconds: 52, title: "Show the company field for business enquiries",
            "text-sk": "Keď si cez váš web objednáva službu súkromná osoba, načo sa jej pýtať na názov firmy? Vo formulári Dopyt na službu môžete prispôsobiť otázky tomu, kto ho vypĺňa. V prvom kroku máte pripravené kontaktné údaje aj výber typu zákazníka. Otvorte uložené pole Názov firmy. Na karte Zobrazenie pridajte podmienku. V ponuke Pole formulára vyberte Typ zákazníka. Odpoveď môžete použiť v tom istom kroku alebo v niektorom z nasledujúcich krokov. Ako podmienku porovnania vyberte rovná sa. Otvorte porovnávanú hodnotu. Vidíte rovnaké možnosti, aké má návštevník. Vyberte Firmu priamo podľa názvu, technický kód zadávať nemusíte. Uložte podmienku a potom aj pole.",
            notes: "Show both labels in the real choice control, then the saved condition and the return to the field list.",
            prepare: async ({ demo }) => { await demo.admin(0); },
            shot: async ({ demo }) => {
                await demo.cue("list");
                await demo.open("Názov firmy");
                await demo.cue("name-editor");
                await demo.rule("visibility", "show");
            }
        },
        {
            id: "povinnost", type: "auto", durationSeconds: 49, title: "Require two fields without hiding the phone",
            "text-sk": "Firma už príslušné pole uvidí. Kedy ho musí aj vyplniť? Znova otvorte Názov firmy, tentoraz prejdite na kartu Povinnosť. Pridajte podmienku, vyberte Typ zákazníka, porovnanie rovná sa a možnosť Firma. Uložte pravidlo aj úpravu poľa. Zobrazenie a povinné vyplnenie tak máte nastavené samostatne. Pri telefóne potrebujete iné správanie. Otvorte Telefón. Na karte Zobrazenie vidíte prázdny zoznam, takže číslo môže zadať každý. Prejdite na Povinnosť a pridajte pravidlo. Opäť vyberte Typ zákazníka, rovná sa, Firma. Keď sú tu nastavené podmienky, práve tie určujú, kedy je údaj povinný. Uložte pravidlo a potvrďte úpravu telefónu.",
            notes: "Leave the empty phone visibility table on screen before setting and saving its requirement.",
            prepare: async ({ demo }) => { await demo.admin(1); },
            shot: async ({ I, DT, demo }) => {
                await demo.cue("requirements-list");
                await demo.open("Názov firmy");
                await demo.cue("required-name");
                await demo.rule("requirement", "require-name");
                await demo.open("Telefón");
                await demo.cue("phone-editor");
                await demo.tab("visibilityConditions");
                await DT.waitForLoader("datatableFieldDTE_Field_visibilityConditions");
                await I.dontSeeElement("#datatableFieldDTE_Field_visibilityConditions tbody tr[id]");
                await demo.cue("phone-always");
                await demo.rule("requirement", "require-phone");
            }
        },
        {
            id: "navstevnik", type: "auto", durationSeconds: 56, title: "Try both customer types and validate the public form",
            "text-sk": "Výsledok si vyskúšajte na verejnej stránke. Ako typ zákazníka vyberte Súkromná osoba. Formulár sa na názov firmy nepýta. Telefón zostáva viditeľný a môžete ho nechať prázdny. Pri pokračovaní sa skryté pole nekontroluje a jeho hodnota sa nepoužije. Teraz vyberte Firmu. Pribudol Názov firmy a pri ňom hviezdička. Všimnite si aj telefón. Zostal na obrazovke, no teraz má hviezdičku tiež. Vyplňte meno a e-mail. Názov firmy aj telefón zatiaľ ponechajte prázdne a kliknite na Pokračovať. Pri oboch poliach vidíte hlásenie, čo treba doplniť. Zadajte názov firmy a telefónne číslo. Po kliknutí na Pokračovať sa dostanete do druhého kroku, kde už môžete opísať svoj dopyt.",
            notes: "Film the public page, the two inline server errors and the successful step transition. Stop before final submission.",
            prepare: async ({ demo }) => { await demo.publicPage(); },
            shot: async ({ I, demo }) => {
                const next = ".multistep-form button[type=submit]";
                await demo.cue("public");
                await demo.customer("person");
                await demo.cue("person");
                await I.wait(1.5);
                await demo.customer("company");
                await demo.cue("company");
                await demo.fill("Meno", "Eva Nováková");
                await demo.fill("E-mail", "eva@example.com");
                await demo.cue("contact");
                await I.videoClick(next);
                await demo.errors();
                await demo.cue("errors");
                await I.wait(2);
                await demo.fill("Názov firmy", "Ateliér Lipa");
                await demo.cue("business-name");
                await demo.fill("Telefón", "0900 123 456");
                await demo.cue("number");
                await I.videoClick(next);
                await I.waitForVisible(demo.input("Text dopytu"), 15);
                await I.see("Váš dopyt", ".multistep-form");
                await demo.cue("step-two");
            }
        },
        {
            id: "vazby", type: "auto", durationSeconds: 37, title: "Trace the rules back from the controlling answer",
            "text-sk": "Keď chcete zistiť, ktoré polia používajú odpoveď návštevníka, otvorte v administrácii Typ zákazníka. Prejdite na kartu Väzby. V zozname Zobrazenie polí nájdete Názov firmy. V zozname Povinné vyplnenie polí sú dve položky, Názov firmy a Telefón. Z jedného miesta tak vidíte, čo sa zmení výberom typu zákazníka. Kliknite na Telefón v druhom zozname. Otvorí sa jeho editor. Na karte Povinnosť otvorte uloženú podmienku. Tu vidíte aj konkrétnu odpoveď Firma. Ak chcete pravidlo zmeniť, upravíte ho pri tomto závislom poli.",
            notes: "Hold both reverse lists in one view. Follow the phone link, then inspect the saved choice in its condition editor.",
            prepare: async ({ demo }) => { await demo.admin(3); },
            shot: async ({ I, demo }) => {
                await demo.cue("relations-list");
                await demo.open("Typ zákazníka");
                await demo.cue("source-editor");
                await demo.relations();
                await demo.cue("relations");
                await I.wait(2);
                await demo.phoneRule();
                await demo.cue("linked-value");
            }
        },
        {
            id: "dokumentacia", type: "auto", durationSeconds: 18, title: "Find conditions and relationships in the guide",
            "text-sk": "Návštevník tak vypĺňa kratší a zrozumiteľnejší formulár. Vy si v administrácii určíte, ktoré údaje od koho potrebujete. Postup nastavenia oboch pravidiel nájdete v dokumentácii WebJET CMS v časti Podmienené zobrazenie a validovanie položky. Hneď pod ňou nájdete aj časť Väzby.",
            notes: "Show the two sections of the Slovak guide in reading order. Hold the last heading before the complete brand outro.",
            prepare: async ({ demo }) => { await demo.docs("Podmienené zobrazenie"); },
            shot: async ({ I, demo }) => {
                await demo.cue("guide-conditions");
                await I.wait(3);
                await demo.docs("Väzby");
                await demo.cue("guide-relations");
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
    if (process.argv.includes("dry-run")) return;
    const demo = require("./assets/229-dopyt/recording.cjs").recording(I, DT, DTE);
    try {
        login("admin");
        await demo.start();
        await demo.publicPage();
        await demo.customer("company");
        await I.videoTitle("Formulár, ktorý sa prispôsobí\nodpovediam návštevníka", 54, "glow");
    } finally { await demo.finish(); }
}).tag("@title");

Scenario("229-58477-formulare-podmienene-zobrazenie-poli", async ({ I, DT, DTE, login }) => {
    if (process.argv.includes("dry-run")) return;
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const demo = require("./assets/229-dopyt/recording.cjs").recording(I, DT, DTE);
    try {
        await recordVideoPlan(I, { plan: videoPlan, context: { demo }, setup: async () => {
            login("admin");
            await demo.start();
        } });
    } finally { await demo.finish(); }
}).tag("@video");
