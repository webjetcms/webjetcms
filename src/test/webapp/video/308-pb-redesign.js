Feature("video.308-pb-redesign");

// Source: https://github.com/webjetcms/webjetcms/pull/308, feature/pb-redesign at 3addf9ca4.
// Compare with origin/main: floating gear palettes are replaced by the shared toolbar.
// Documentation: https://docs.webjetcms.sk/latest/sk/redactor/webpages/pagebuilder
// The shot plan describes the edited timeline. Short holds below provide editing room;
// extend them to the recorded narration and insert the manual footage and head clips.
// Run from src/test/webapp: npm run video video/308-pb-redesign.js
// Generate speech only on request: npm run audio video/308-pb-redesign.js
// Generate the talking-head intro: npm run head video/308-pb-redesign.js

// Reorder shots here; ids remain stable and timing is derived from durationSeconds.
const videoPlan = {
    "language": "sk",
    "notes": "16:9, 1920 x 1080. Setup, fixture resets, cleanup and slates are editing material; cut them out. Durations are estimates for the edited narration, not automation waits.",
    "shots": [
        {
            "id": "intro",
            "type": "head",
            "durationSeconds": 18,
            "title": "New Page Builder: familiar tools",
            "text-sk": "Používali ste Page Builder a po aktualizácii hľadáte známe nástroje? V tomto videu si ukážeme, kam sa presunuli a ako sa pracuje s novou verziou. Stránku naďalej skladáte z pripravených blokov. Vylepšili sme ale spôsob ich výberu a úprav.",
            "notes": "Insert the separately generated Jack / Home Vlog Style intro, cropped to 16:9. Generate it with npm run head video/308-pb-redesign.js."
        },
        {
            "id": "old-editor",
            "type": "manual",
            "durationSeconds": 12,
            "title": "Before the update",
            "text-sk": "V staršej verzii ste prešli myšou nad obsah a nástroje otvorili ozubeným kolieskom pri farebnom rámiku.",
            "notes": "Autentická stará snímka docs/sk/redactor/webpages/pagebuilder.png: farebné rámiky, ovládač pri bloku a plávajúca paleta. Označiť „Pred aktualizáciou“. Nový editor neupravovať tak, aby predstieral starý."
        },
        {
            "id": "text-editing",
            "type": "auto",
            "durationSeconds": 18,
            "title": "Direct text editing and stable selection",
            "text-sk": "Teraz kliknete priamo do obsahu. Vyberie sa príslušný blok a jeho nástroje nájdete na jednom mieste, v pevnej lište pod nástrojmi textového editora. Kliknutím do textu môžete rovno písať. Výber zostáva stabilný a rámik sa počas písania zjemní.",
            "notes": "Kliknúť do „Naše služby“, ukázať jeden rámik a pevnú lištu, dopísať krátky text. MANUAL: detail jemnejšieho rámika počas písania.",
            shot: async ({ I, services, typeText }) => {
                await I.videoClick(services);
                await I.waitForVisible(".pb-outline[data-type=column]:not([hidden])", 10);
                await I.pressKey("End");
                await typeText(" Spoločne.");
                await I.waitForText("Spoločne.", 10, services);
                await I.wait(3); // Presentation hold after the content assertion.
            }
        },
        {
            "id": "hierarchy",
            "type": "auto",
            "durationSeconds": 26,
            "title": "Understand the block hierarchy",
            "text-sk": "Najprv si vysvetlime štruktúru. Modrá sekcia je veľká časť stránky, napríklad predstavenie služieb. V nej je červený kontajner, ktorý drží obsah pokope. Riadok usporadúva stĺpce vedľa seba. Zelený stĺpec obsahuje text, obrázok alebo aplikáciu. Oranžová označuje opakovateľnú položku alebo duplikovateľný riadok. Tieto úrovne nie sú novým spôsobom skladania stránky. Nové ovládanie vám ich pomáha jasnejšie rozlíšiť.",
            "notes": "Postupne vybrať Stĺpec, Riadok, Kontajner a Sekcia v ceste. MANUAL: postupne pripájať popisky úrovní k reálnemu záberu, zachovať farby rozhrania.",
            shot: async ({ I, services }) => {
                for (const type of ["row", "container", "section"]) {
                    await I.videoClick(`.pb-workbench-path [data-type=${type}]`);
                    await I.waitForElement(`.pb-workbench-path [data-type=${type}][aria-current=location]`, 10);
                    await I.wait(3); // Editing room for the hierarchy explanation.
                    await I.videoClick(services);
                }
            }
        },
        {
            "id": "ancestor-path",
            "type": "auto",
            "durationSeconds": 21,
            "title": "Select the right level in the toolbar",
            "text-sk": "Pozrite sa na cestu v hornej lište. Ukazuje, do ktorej sekcie, kontajnera a riadka patrí vybraný stĺpec. Chcete upraviť pozadie celej sekcie? Kliknite v ceste na Sekcia. Chcete pracovať len so stĺpcom? Vyberte ho v obsahu. Pred každou akciou si tak ľahko skontrolujete, ktorej časti sa zmena týka.",
            "notes": "Klik na Sekcia v ceste, návrat do stĺpca cez obsah. Zdôrazniť, ktorá úroveň dostane akciu.",
            shot: async ({ I, services }) => {
                await I.videoClick(".pb-workbench-path [data-type=section]");
                await I.waitForElement(".pb-workbench-path [data-type=section][aria-current=location]", 10);
                await I.wait(4);
                await I.videoClick(services);
                await I.waitForElement(".pb-workbench-path [data-type=column][aria-current=location]", 10);
                await I.wait(3);
            }
        },
        {
            "id": "element-actions",
            "type": "auto",
            "durationSeconds": 21,
            "title": "Actions for rows, items and standalone text",
            "text-sk": "Nie každý prvok má rovnaké možnosti. Bežný riadok slúži na orientáciu. Opakovateľnú položku možno kopírovať, presúvať a zmazať, ale nemá vlastné nastavenie štýlu ani šírky stĺpca. Samostatný editovateľný text nemá štrukturálne akcie. Ak tlačidlo nevidíte, overte si, aký typ prvku máte vybraný.",
            "notes": "Vybrať obyčajný riadok, opakovateľnú položku „Konzultácia“ a samostatný text. Ukázať rozdielny rozsah tlačidiel.",
            shot: async ({ I, fixture, action }) => {
                await I.videoClick(".pb-workbench-path [data-type=row]");
                await I.waitForElement(".pb-workbench-path [data-type=row][aria-current=location]", 10);
                await I.wait(3);
                await I.videoClick(locate(`${fixture} li.pb-duplicable-element`).first());
                await I.waitForElement(".pb-workbench-path [data-type=item][aria-current=location]", 10);
                await I.seeElement(action("duplicate-adjacent"));
                await I.dontSeeElement(action("resize"));
                await I.wait(3);
                await I.videoClick(`${fixture} .video-note`);
                await I.waitForElement(".pb-workbench-path [data-type=text][aria-current=location]", 10);
                await I.dontSeeElement(action("duplicate-adjacent"));
                await I.wait(3);
            }
        },
        {
            "id": "more-actions",
            "type": "auto",
            "durationSeconds": 27,
            "title": "Find the original tools in More actions",
            "text-sk": "Kde teraz nájdete pôvodné nástroje? Otvorte Ďalšie akcie. Tu je Štýl s nastavením pozadia, zarovnania či odsadenia. Nájdete tu aj vloženie pred alebo za blok, presun, pôvodné duplikovanie, pridanie do obľúbených a zmazanie. Šírka stĺpca a nové Duplikovať vedľa sú priamo v lište. Ponuka sa vždy prispôsobí výberu.",
            "notes": "Stĺpec > Ďalšie akcie > Štýl. Detail existujúcich vlastností, zavrieť Zrušiť. Znova otvoriť menu a ukázať pôvodné operácie; nič nezmazať ani neukladať do obľúbených.",
            shot: async ({ I, services, action }) => {
                await I.videoClick(services);
                await I.videoClick(action("more"));
                await I.waitForVisible(action("style"), 10);
                await I.wait(3);
                await I.videoClick(action("style"));
                await I.waitForVisible(".pb-modal", 10);
                await I.wait(5);
                await I.videoClick(".pb-modal__footer__button-close");
                await I.waitForInvisible(".pb-modal", 10);
                await I.videoClick(action("more"));
                await I.waitForVisible(action("add_to_favorites"), 10);
                await I.wait(4);
                await I.pressKey("Escape");
            }
        },
        {
            "id": "structure",
            "type": "auto",
            "durationSeconds": 30,
            "title": "Navigate and search the structure tree",
            "text-sk": "Na dlhšej stránke pomôže tlačidlo Štruktúra. Otvorí strom blokov vľavo nad obsahom. Kliknutie na vetvu ju vyberie a rozbalí. Šípkou pri nej ju môžete zbaliť. Názvy vychádzajú z nadpisov alebo textu blokov. Do vyhľadávania stačí napísať napríklad Kontakt. Po výbere sa presuniete na príslušné miesto stránky.",
            "notes": "Otvoriť Štruktúru, vybrať a rozbaliť sekciu kliknutím na názov, zbaliť šípkou. Vyhľadať Kontakt, vybrať stĺpec a ukázať presun na obsah bez zúženia plátna.",
            shot: async ({ I, treeRow, action }) => {
                await I.videoClick(action("structure"));
                await I.waitForVisible(".pb-structure", 10);
                await I.videoClick(treeRow("section", "Naše služby"));
                await I.waitForElement(locate(".pb-structure [role=treeitem][data-type=section][aria-expanded=true]").withText("Naše služby"), 10);
                await I.saveScreenshot("308-pb-redesign-structure.png");
                await I.wait(3);
                await I.videoClick(locate(".pb-structure > ul > li > div > [data-pb-expand]").first());
                await I.fillField(".pb-structure input[type=search]", "Kontakt");
                await I.waitForVisible(treeRow("column", "Kontakt"), 10);
                await I.videoClick(treeRow("column", "Kontakt"));
                await I.waitForElement(".pb-workbench-path [data-type=column][aria-current=location]", 10);
                await I.wait(4);
            }
        },
        {
            "id": "hidden-blocks",
            "type": "auto",
            "durationSeconds": 28,
            "title": "Hidden blocks and keyboard navigation",
            "text-sk": "Označenie Skrytý znamená, že blok práve nie je viditeľný. Jeho výber ho nezobrazí ani neprepne aktívnu kartu. Strom používajte na orientáciu a výber. Na presun slúžia akcie v hornej lište. Funguje aj klávesnica: šípky na pohyb a rozbaľovanie, Enter na výber a Escape na zatvorenie panelu.",
            "notes": "Vyhľadať „Sezónna ponuka“, vybrať Skrytý, obsah zostáva skrytý. Vymazať filter, ukázať pohyb klávesnicou a zavrieť Escape. MANUAL: krátky popis „Presun cez Ďalšie akcie“.",
            prepare: async ({ I, action }) => {
                await I.click(action("structure"));
                await I.waitForVisible(".pb-structure", 10);
            },
            shot: async ({ I, treeRow, services }) => {
                await I.fillField(".pb-structure input[type=search]", "Sezónna ponuka");
                await I.waitForVisible(treeRow("section", "Sezónna ponuka"), 10);
                await I.videoClick(treeRow("section", "Sezónna ponuka"));
                await I.see("Skrytý", ".pb-structure");
                await I.dontSeeElement(".pb-video-hidden-autotest");
                await I.wait(4);
                await I.fillField(".pb-structure input[type=search]", "");
                await I.videoClick(treeRow("section", "Naše služby"));
                await I.pressKey("ArrowRight");
                await I.pressKey("ArrowDown");
                await I.pressKey("Enter");
                await I.pressKey("Escape");
                await I.waitForInvisible(".pb-structure", 10);
                await I.videoClick(services);
            }
        },
        {
            "id": "insertion",
            "type": "auto",
            "durationSeconds": 34,
            "title": "Choose an insertion point",
            "text-sk": "Pridávanie blokov má nový vstup. Kliknite na plus v hornej lište. Nemusíte predtým hľadať ozubené koliesko ani označiť blok. Priamo v stránke sa ukážu miesta vloženia. Modré pásy pridávajú sekcie, ružové kontajnery a zelené pluská stĺpce. Popis vám povie, pred ktorý blok alebo za ktorý blok vkladáte. Rozbalené medzery sú iba dočasnou pomôckou.",
            "notes": "Plus v lište, modré a ružové pásy, zelené pluská a popisy polôh. Vybrať miesto na stĺpec medzi existujúcimi stĺpcami.",
            shot: async ({ I, action }) => {
                await I.videoClick(action("insert"));
                await I.waitForVisible(".pb-insert-hint", 10);
                await I.waitForVisible(".pb-insert-point[data-type=column]", 10);
                await I.wait(5);
                await I.saveScreenshot("308-pb-redesign-insert.png");
                await I.executeScript(() => {
                    const point = window.pageBuilder.ui.insertPoints.find(point =>
                        point.type === "column" && point.previous && point.next && point.parent.closest(".pb-video-autotest"));
                    point.button.attr("data-autotest-insert", "true");
                    point.button[0].focus();
                });
                await I.videoClick("[data-autotest-insert]");
                await I.waitForVisible(".pb-library--column", 10);
                await I.seeElement(".pb-insert-context");
            }
        },
        {
            "id": "library",
            "type": "auto",
            "durationSeconds": 33,
            "title": "Explore the library and insert a column",
            "text-sk": "Vyberte miesto a otvorí sa výber blokov príslušného typu. Zostávajú známe karty Základné, Knižnica a Obľúbené. V knižnici naďalej nájdete bloky pripravené pre svoj web, vyhľadávanie a štítky. Po vložení sa nový blok označí a môžete upraviť jeho obsah. Ak knižnicu zatvoríte, vrátite sa k vybranému plusku. Celý režim ukončíte cez Ukončiť, Escape alebo opätovným kliknutím na plus.",
            "notes": "Ukázať karty Základné, Knižnica, Obľúbené a kontext vloženia. Zrušiť knižnicu, ukázať návrat na plus. Otvoriť znova, vložiť základný stĺpec a ukázať fokus v jeho obsahu. Nový režim plus ukončiť cez Ukončiť.",
            prepare: async ({ I, action }) => {
                await I.click(action("insert"));
                await I.waitForVisible(".pb-insert-point[data-type=column]", 10);
                await I.executeScript(() => {
                    const point = window.pageBuilder.ui.insertPoints.find(point =>
                        point.type === "column" && point.previous && point.next && point.parent.closest(".pb-video-autotest"));
                    point.button.attr("data-autotest-insert", "true");
                    point.button[0].focus();
                });
                await I.click("[data-autotest-insert]");
                await I.waitForVisible(".pb-library--column", 10);
            },
            shot: async ({ I, action, waitForPageBuilder }) => {
                for (const type of ["basic", "library", "favorite"]) {
                    await I.videoClick(`.pb-library .library-tab-link[data-library-type=${type}]`);
                    await I.waitForVisible(`.pb-library .library-tab-item--${type}`, 10);
                    await I.wait(3);
                }
                await I.videoClick(".pb-library__footer__button");
                await I.waitForInvisible(".pb-library", 10);
                await I.waitForVisible("[data-autotest-insert]", 10);
                await I.wait(3);
                await I.videoClick("[data-autotest-insert]");
                await I.waitForVisible(".pb-library--column", 10);
                await I.videoClick(".pb-library .library-tab-link[data-library-type=basic]");
                await I.videoClick(locate(".pb-library .library-tab-item--basic .library-template-block--column .library-tab-item-button").first());
                await I.waitForInvisible(".pb-library", 10);
                await I.waitForInvisible(".pb-insert-layer", 10);
                await waitForPageBuilder("wait for the inserted column editor to receive focus", () => {
                    const field = window.pageBuilder.ui.selected?.querySelector("[data-ckeditor-instance]");
                    const editor = field && CKEDITOR.instances[field.dataset.ckeditorInstance];
                    return editor?.status === "ready" && editor.focusManager.hasFocus;
                });
                await I.wait(4);
                await I.videoClick(action("insert"));
                await I.waitForVisible(".pb-insert-hint", 10);
                await I.videoClick(action("end-insert"));
                await I.waitForInvisible(".pb-insert-layer", 10);
            }
        },
        {
            "id": "duplicate-move",
            "type": "auto",
            "durationSeconds": 39,
            "title": "Duplicate and reorder items",
            "text-sk": "Pri opakovaní obsahu vyskúšajte Duplikovať vedľa. Kópia vznikne hneď za výberom a automaticky sa označí. Netreba vyberať cieľ. Na malú zmenu poradia použite v Ďalších akciách presun pred predchádzajúci alebo za nasledujúci blok. Na okraji zoznamu je príslušná akcia neaktívna. Pôvodný presun s výberom miesta zostáva dostupný. Pri opakovateľných položkách sa presúvate len medzi kompatibilnými položkami rovnakého rodiča.",
            "notes": "Vybrať Konzultácia, Duplikovať vedľa, podržať vybranú kópiu. Presunúť ju za Podpora a ukázať neaktívny ďalší presun na konci. Otvoriť pôvodný Presunúť, ukázať povolené ciele, zrušiť Escape.",
            shot: async ({ I, fixture, action }) => {
                await I.videoClick(locate(`${fixture} li.pb-duplicable-element`).withText("Konzultácia"));
                await I.videoClick(action("duplicate-adjacent"));
                await I.waitForElement(`${fixture} .video-services-list > li:nth-child(3)`, 10);
                await I.waitForText("Konzultácia", 10, `${fixture} .video-services-list > li:nth-child(2)`);
                await I.wait(3);
                await I.videoClick(action("more"));
                await I.videoClick(action("next"));
                await I.waitForText("Konzultácia", 10, `${fixture} .video-services-list > li:nth-child(3)`);
                await I.waitForText("Podpora", 10, `${fixture} .video-services-list > li:nth-child(2)`);
                await I.videoClick(action("more"));
                await I.seeElement(`${action("next")}:disabled`);
                await I.wait(4);
                await I.videoClick(action("move"));
                await I.waitForElement("#wjInline-docdata.pb-is-moving-child", 10);
                await I.wait(4);
                await I.pressKey("Escape");
                await I.waitForInvisible("#wjInline-docdata.pb-is-moving-child", 10);
            }
        },
        {
            "id": "responsive",
            "type": "auto",
            "durationSeconds": 28,
            "title": "Responsive widths and the compact path",
            "text-sk": "Aj prepínanie mobilu, tabletu a desktopu zostáva pri výbere editora. Vyberte zariadenie, potom stĺpec a jeho šírku. Nastavujete rozloženie pre danú veľkosť. V úzkom zobrazení sa cesta skráti a nadradené prvky otvoríte tlačidlom vedľa aktuálneho typu. Panel Štruktúra sa po výbere na úzkej obrazovke zatvorí.",
            "notes": "Mobil > stĺpec > šírka 12, zmeniť na 11 a späť na 12. Rozbaliť skrátenú cestu, otvoriť Štruktúru a výberom ju zavrieť. Tablet a Desktop, návrat k rozloženiu pre desktop. MANUAL: zväčšiť detail mobilného iframe a vystrihnúť prázdnu plochu mimo neho.",
            shot: async ({ I, services, fixture, action, treeRow, waitForPageBuilder }) => {
                await I.videoClick("a[title=Mobil]");
                await waitForPageBuilder("wait for the mobile viewport", () => window.innerWidth < 768);
                await I.videoClick(services);
                await I.videoClick(action("resize"));
                await I.waitForVisible(`${fixture} .pb-size-changer__down`, 10);
                await I.videoClick(locate(`${fixture} .pb-size-changer__down`).first());
                await I.waitForElement(`${fixture} .col-11`, 10);
                await I.wait(3);
                await I.videoClick(locate(`${fixture} .pb-size-changer__up`).first());
                await I.waitForElement(`${fixture} .col-12`, 10);
                await I.pressKey("Escape");
                await I.videoClick(action("ancestors"));
                await I.waitForVisible(".pb-workbench-path.is-expanded", 10);
                await I.wait(3);
                await I.pressKey("Escape");
                await I.videoClick(action("structure"));
                await I.fillField(".pb-structure input[type=search]", "Naše služby");
                await I.videoClick(treeRow("column", "Naše služby"));
                await I.waitForInvisible(".pb-structure", 10);
                await I.videoClick("a[title=Tablet]");
                await waitForPageBuilder("wait for the tablet viewport", () => window.innerWidth >= 768 && window.innerWidth < 1200);
                await I.wait(3);
                await I.videoClick("a[title=Desktop]");
                await waitForPageBuilder("wait for the desktop viewport", () => window.innerWidth >= 1200);
                await I.videoClick(services);
            }
        },
        {
            "id": "guides",
            "type": "auto",
            "durationSeconds": 22,
            "title": "Switch between guide modes",
            "text-sk": "Ikona oka postupne prepína rámik vybraného bloku, skryté rámiky a rámiky celej jeho hierarchie. Posledný režim pomôže pochopiť vnorenie. Prehliadač si voľbu pamätá. Nástroje a obsah zostávajú dostupné aj bez rámikov.",
            "notes": "S otvorenou Štruktúrou prepnúť oko: vybraný blok > žiadne rámiky > celá hierarchia > vybraný blok. Panel zostáva otvorený. Zavrieť ho a podržať čistý záber editora.",
            shot: async ({ I, action }) => {
                await I.videoClick(action("structure"));
                await I.fillField(".pb-structure input[type=search]", "");
                for (const mode of ["hidden", "all", "selected"]) {
                    await I.videoClick(action("guides"));
                    await I.waitForElement(`${action("guides")}[data-pb-guides=${mode}]`, 10);
                    await I.seeElement(".pb-structure");
                    await I.wait(4);
                }
                await I.videoClick(".pb-structure [data-pb-action=close-structure]");
                await I.waitForInvisible(".pb-structure", 10);
                await I.wait(5);
            }
        },
        {
            "id": "preview",
            "type": "manual",
            "durationSeconds": 7,
            "title": "Preview the resulting page",
            "text-sk": "Na kontrolu výslednej stránky použite samostatný Náhľad.",
            "notes": "Zväčšiť samostatné tlačidlo Náhľad v päte editora; prípadné otvorenie ďalšej karty natočiť samostatne."
        },
        {
            "id": "outro",
            "type": "manual",
            "durationSeconds": 18,
            "title": "Page Builder documentation",
            "text-sk": "Pri ďalšej úprave teda začnite výberom obsahu. V ceste alebo v Štruktúre overte správnu úroveň a potom použite nástroje hornej lišty. Podrobný návod k Page Builderu nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.",
            "notes": "Zopakovať cestu „Obsah > správna úroveň > akcia“ a ukázať slovenský návod https://docs.webjetcms.sk/latest/sk/redactor/webpages/pagebuilder. Pred publikovaním overiť, že verejný návod už opisuje tento PR. Outro s odkazom v popise videa."
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("ElevenLabs Head", ({ I }) => {
    I.generateHead(videoPlan);
}).tag("@head");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

Scenario("308-pb-redesign", async ({ I, DTE, Document, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const iframe = "#DTE_Field_data-pageBuilderIframe";
    const fixture = ".pb-video-autotest";
    const services = `${fixture} .video-services`;
    const action = name => `.pb-workbench [data-pb-action=${name}]`;
    const treeRow = (type, text) => locate(`.pb-structure [role=treeitem][data-type=${type}] > div`).withText(text);
    // CodeceptJS 3.6 uses a FrameLocator that has no waitForFunction method.
    const waitForPageBuilder = (description, predicate) => I.usePlaywrightTo(description, async ({ page }) => {
        const frame = await (await page.locator(iframe).elementHandle()).contentFrame();
        await frame.waitForFunction(predicate, null, { timeout: 20000 });
    });
    // Playwright typing supports Slovak characters that CodeceptJS I.type treats as key names.
    const typeText = text => I.usePlaywrightTo("type the Slovak demonstration text", async ({ page }) => {
        await page.keyboard.type(text, { delay: 70 });
    });

    // Reopen the editor and replace browser-only content before each shot, so order is independent.
    const prepareEditor = async () => {
        await I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
        DTE.waitForEditor();
        await I.clickCss("div.DTED.show button.maximize");
        await I.waitForInvisible("div.DTED.show button.maximize", 10);
        await I.switchTo(iframe);
        await I.waitForVisible(".pb-workbench", 20);
        await I.clickCss("a[title=Desktop]");
        await waitForPageBuilder("wait for the initial desktop viewport", () => window.innerWidth >= 1200);

        // Prepare isolated, browser-only content. Closing the editor discards every change.
        await I.executeScript(() => {
            const wrapper = document.querySelector("#wjInline-docdata");
            wrapper.querySelectorAll("[data-ckeditor-instance]").forEach(element => {
                CKEDITOR.instances[element.dataset.ckeditorInstance]?.destroy(true);
            });
            wrapper.querySelectorAll(":scope > .pb-section").forEach(element => element.remove());
            wrapper.insertAdjacentHTML("afterbegin", `
    <section class="pb-video-autotest" style="padding:32px 0;background:#f4f7fa">
    <div class="container"><div class="row">
    <div class="col-12 col-md-6 col-xl-6"><div class="column-content">
    <h2>Naše služby</h2><p class="video-services">Pomôžeme vám vytvoriť prehľadnú webovú stránku.</p>
    <ul class="video-services-list"><li class="pb-duplicable">Konzultácia</li><li class="pb-duplicable">Podpora</li></ul>
    </div></div>
    <div class="col-12 col-md-6 col-xl-6"><h2>Prečo si vybrať nás</h2><p>Jasný postup, praktické skúsenosti a priestor pre vaše nápady.</p></div>
    </div><p class="pb-editable video-note">Spoločne pripravíme obsah, ktorý dáva zmysel.</p></div>
    </section>
    <section class="pb-video-contact-autotest" style="padding:64px 0">
    <div class="container"><div class="row"><div class="col-12"><h2>Kontakt</h2><p>Dohodnime si úvodné stretnutie.</p></div></div></div>
    </section>
    <section class="pb-video-hidden-autotest" style="display:none">
    <div class="container"><div class="row"><div class="col-12"><h2>Sezónna ponuka</h2></div></div></div>
    </section>`);
            window.markPbElements("doc_data");
            window.pageBuilder.set_workbench_guides("selected", false);
        });
        await waitForPageBuilder("wait for the temporary content editors", () => {
            const fields = Array.from(document.querySelectorAll(".pb-video-autotest [data-ckeditor-instance]"));
            return fields.length === 3 && fields.every(field => CKEDITOR.instances[field.dataset.ckeditorInstance]?.status === "ready");
        });

        await I.clickCss(services);
        await I.waitForElement(".pb-workbench-path [data-type=column][aria-current=location]", 10);
    };

    await recordVideoPlan(I, {
        plan: videoPlan,
        context: { fixture, services, action, treeRow, waitForPageBuilder, typeText },
        setup: async () => {
            login("admin");
            Document.resetPageBuilderMode();
        },
        prepare: prepareEditor,
        cleanup: async () => {
            await I.switchTo();
            DTE.cancel();
            await I.waitForInvisible("div.DTED.show", 10);
        }
    });
}).tag("@video");
