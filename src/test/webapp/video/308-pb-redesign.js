Feature("video.308-pb-redesign");

// Source: https://github.com/webjetcms/webjetcms/pull/308, reviewed through e9dec3c2e.
// Compare with origin/main: floating gear palettes are replaced by the shared toolbar.
// Documentation: https://docs.webjetcms.sk/latest/sk/redactor/webpages/pagebuilder
// The shot plan describes the edited timeline. Short holds below provide editing room;
// extend them to the recorded narration and insert the generated head clip.
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
            "notes": "Insert the separately generated Jack / Home Vlog Style intro, with its reference background expanded to 16:9. Generate it with npm run head video/308-pb-redesign.js."
        },
        {
            "id": "old-editor",
            "type": "auto",
            "durationSeconds": 12,
            "title": "Before the update",
            "text-sk": "V staršej verzii ste prešli myšou nad obsah a nástroje otvorili ozubeným kolieskom pri farebnom rámiku.",
            "notes": "Show the legacy Page Builder on demo.webjetcms.sk while it still runs the version before this update. Hover a column and open its gear palette without saving changes.",
            prepare: async ({ I, DTE, iframe, oldColumn }) => {
                await I.amOnPage("https://demo.webjetcms.sk/logoff.do?forward=/admin/logon/");
                // Keep the recording tab and log in on the demo origin, not CODECEPT_URL.
                await I.relogin("admin", false);
                await I.amOnPage("https://demo.webjetcms.sk/admin/v9/webpages/web-pages-list/?docid=57");
                await DTE.waitForEditor();
                await I.clickCss("div.DTED.show button.maximize");
                await I.waitForInvisible("div.DTED.show button.maximize", 10);
                await I.switchTo(iframe);
                await I.waitForVisible(oldColumn, 20);
                await I.dontSeeElement(".pb-workbench");
            },
            shot: async ({ I, oldColumn }) => {
                const toolbar = oldColumn.find("aside.pb-toolbar").first();
                await I.moveCursorTo(oldColumn);
                await I.waitForVisible(toolbar, 10);
                await I.wait(3);
                await I.videoClick(toolbar);
                await I.waitForVisible(toolbar.find(".pb-toolbar-button__style"), 10);
                await I.wait(6);
            }
        },
        {
            "id": "text-editing",
            "type": "auto",
            "durationSeconds": 18,
            "title": "Direct text editing and stable selection",
            "text-sk": "Jemný rámik pod myšou teraz napovie, čo môžete vybrať. Kliknite priamo do textu a rovno píšte. Blok zostane vybraný aj pri prechode myšou inde. Jeho nástroje sú v pevnej lište pod textovým editorom. Počas písania sa rámik výberu zjemní.",
            "notes": "Kliknúť do „Naše služby“, ukázať jeden rámik a pevnú lištu, dopísať krátky text. MANUAL: detail jemnejšieho rámika počas písania.",
            shot: async ({ I, services, fixture, typeText }) => {
                await I.moveCursorTo(`${fixture} .row > .pb-column:nth-child(2) h2`);
                await I.waitForVisible(".pb-outline.is-hover[data-type=column]:not([hidden])", 10);
                await I.wait(3);
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
            "text-sk": "Najprv si vysvetlime štruktúru. Modrá sekcia je veľká časť stránky, napríklad predstavenie služieb. V nej je ružový kontajner, ktorý drží obsah pokope. Sivý riadok usporadúva stĺpce vedľa seba. Zelený stĺpec obsahuje text, obrázok alebo aplikáciu. Oranžová označuje opakovateľnú položku alebo duplikovateľný riadok. Tieto úrovne nie sú novým spôsobom skladania stránky. Nové ovládanie vám ich pomáha jasnejšie rozlíšiť.",
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
            "durationSeconds": 20,
            "title": "Find the original tools in More actions",
            "text-sk": "Kde teraz nájdete pôvodné nástroje? Otvorte Ďalšie akcie. Tu je Štýl, vloženie pred alebo za blok, presun, duplikovanie, pridanie do obľúbených a zmazanie. Šírka stĺpca a Duplikovať vedľa sú priamo v lište. Ponuka sa prispôsobí výberu.",
            "notes": "Open More actions for a column and hold the complete menu. Do not delete content or save favorites; the next shot demonstrates Style independently.",
            shot: async ({ I, services, action }) => {
                await I.videoClick(services);
                await I.videoClick(action("more"));
                await I.waitForVisible(action("style"), 10);
                await I.waitForVisible(action("add_to_favorites"), 10);
                await I.wait(6);
                await I.pressKey("Escape");
            }
        },
        {
            "id": "style-settings",
            "type": "auto",
            "durationSeconds": 32,
            "title": "Explore the compact style properties",
            "text-sk": "Aj Štýl má nové, kompaktné okno. V hlavičke vidíte, ktorý blok upravujete. Vlastnosti sú rozdelené do rozbaľovacích skupín. Pozadie a odsadenie môžete nechať otvorené súčasne. Prepojením hodnôt nastavíte rovnaké odsadenie na všetkých stranách. Zmenu hneď vidíte na stránke. Uložiť potvrdí štýl, Zrušiť vráti nepotvrdené úpravy. Celú stránku potom uložíte hlavným tlačidlom editora.",
            "notes": "Open Style for the selected column. Show its title and content label, collapse Identification, expand Background and Spacing together, set linked padding and cancel. The fixture is browser-only; do not save the page.",
            prepare: async ({ I, action }) => {
                await I.click(action("more"));
                await I.click(action("style"));
                await I.waitForVisible(".pb-modal [name=selector-id]", 10);
            },
            shot: async ({ I, waitForPageBuilder }) => {
                await I.see("Štýl stĺpca", ".pb-modal .header-title");
                await I.see("Naše služby", ".pb-modal__context");
                await I.wait(3);
                await I.videoClick('.pb-modal .pb-style-accordion[data-input-group-id="10"] > button');
                await I.videoClick('.pb-modal .pb-style-accordion[data-input-group-id="01"] > button');
                await I.waitForVisible(".pb-modal [name=background-image]", 10);
                await I.videoClick('.pb-modal .pb-style-accordion[data-input-group-id="03"] > button');
                await I.waitForVisible(".pb-modal [name=padding-top]", 10);
                await I.fillField(".pb-modal [name=padding-top]", "24");
                await I.pressKey("Tab");
                await waitForPageBuilder("wait for linked padding and its live preview", () => {
                    const panel = document.querySelector(".pb-modal");
                    return ["top", "bottom", "left", "right"].every(side => panel.querySelector(`[name=padding-${side}]`).value === "24") &&
                        getComputedStyle(document.querySelector(".pb-video-autotest .column-content")).paddingTop === "24px";
                });
                await I.wait(5);
                await I.videoClick(".pb-modal__footer__button-close");
                await I.waitForInvisible(".pb-modal", 10);
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
            "title": "Choose a section insertion point",
            "text-sk": "Kliknite na plus v hornej lište. Blok nemusíte vopred označiť. Lištu dočasne nahradí modrý pomocník s tlačidlom Ukončiť. Priamo v stránke sa ukážu miesta vloženia. Modré pásy pridávajú sekcie, ružové kontajnery a zelené pluská stĺpce. Popis vám povie, pred ktorý blok alebo za ktorý blok vkladáte. Rozbalené medzery sú iba dočasnou pomôckou.",
            "notes": "Show the toolbar plus, colored insertion points and position labels. Choose the section boundary between Services and Contact and open the section library.",
            shot: async ({ I, action }) => {
                await I.videoClick(action("insert"));
                await I.waitForVisible(".pb-insert-hint", 10);
                await I.waitForVisible(".pb-insert-point[data-type=section]", 10);
                await I.wait(5);
                await I.saveScreenshot("308-pb-redesign-insert.png");
                await I.executeScript(() => {
                    const point = window.pageBuilder.ui.insertPoints.find(point =>
                        point.type === "section" && point.previous?.matches(".pb-video-autotest") && point.next?.matches(".pb-video-contact-autotest"));
                    point.button.attr("data-autotest-insert", "true");
                    point.button[0].focus();
                });
                await I.videoClick("[data-autotest-insert]");
                await I.waitForVisible(".pb-library--section", 10);
                await I.seeElement(".pb-insert-context");
            }
        },
        {
            "id": "library",
            "type": "auto",
            "durationSeconds": 33,
            "title": "Explore the library and insert a section",
            "text-sk": "Vyberte miesto a otvorí sa kompaktné okno pre príslušný typ bloku. Hlavička pripomína, kam vkladáte. Zostávajú karty Základné, Knižnica a Obľúbené. Kliknutím na blok ho rovno vložíte. Nový blok sa označí a môžete upraviť jeho obsah. Krížikom knižnicu zatvoríte a vrátite sa k vybranému plusku. Celý režim ukončíte tlačidlom Ukončiť alebo klávesom Escape.",
            "notes": "Show Basic, Library and Favorites for sections and the insertion context between Services and Contact. Cancel the library, show focus returning to the plus, reopen it and insert a basic section. Show the selected section with focus in its content, then demonstrate ending insertion mode.",
            prepare: async ({ I, action }) => {
                await I.click(action("insert"));
                await I.waitForVisible(".pb-insert-point[data-type=section]", 10);
                await I.executeScript(() => {
                    const point = window.pageBuilder.ui.insertPoints.find(point =>
                        point.type === "section" && point.previous?.matches(".pb-video-autotest") && point.next?.matches(".pb-video-contact-autotest"));
                    point.button.attr("data-autotest-insert", "true");
                    point.button[0].focus();
                });
                await I.click("[data-autotest-insert]");
                await I.waitForVisible(".pb-library--section", 10);
            },
            shot: async ({ I, action, waitForPageBuilder }) => {
                for (const type of ["basic", "library", "favorite"]) {
                    await I.videoClick(`.pb-library .library-tab-link[data-library-type=${type}]`);
                    await I.waitForVisible(`.pb-library .library-tab-item--${type}`, 10);
                    await I.wait(3);
                }
                await I.videoClick(".pb-library__close");
                await I.waitForInvisible(".pb-library", 10);
                await I.waitForVisible("[data-autotest-insert]", 10);
                await I.wait(3);
                await I.videoClick("[data-autotest-insert]");
                await I.waitForVisible(".pb-library--section", 10);
                await I.videoClick(".pb-library .library-tab-link[data-library-type=basic]");
                await I.videoClick(locate(".pb-library .library-tab-item--basic .library-template-block--section .library-tab-item-button").first());
                await I.waitForInvisible(".pb-library", 10);
                await I.waitForInvisible(".pb-insert-layer", 10);
                await waitForPageBuilder("wait for the inserted section to be selected and its editor to receive focus", () => {
                    const selected = window.pageBuilder.ui.selected;
                    const field = selected?.querySelector("[data-ckeditor-instance]");
                    const editor = field && CKEDITOR.instances[field.dataset.ckeditorInstance];
                    return selected?.matches(".pb-section") && editor?.status === "ready" && editor.focusManager.hasFocus;
                });
                await I.wait(4);
                await I.videoClick(action("insert"));
                await I.waitForVisible(".pb-insert-hint", 10);
                await I.videoClick(action("end-insert"));
                await I.waitForInvisible(".pb-insert-layer", 10);
            }
        },
        {
            "id": "library-search",
            "type": "auto",
            "durationSeconds": 25,
            "title": "Find a block with categories and filters",
            "text-sk": "V Knižnici rozbalíte kategóriu a uvidíte náhľady blokov s názvami. Naraz je otvorená jedna kategória. Hľadanie podľa názvu môžete spojiť so štítkom. Počty pri kategóriách ukazujú vyhovujúce bloky. Všetky zruší iba štítok a ponechá hľadaný text. Ak sa nič nenájde, Vyčistiť filtre obnoví celý zoznam.",
            "notes": "Open the section library independently through Insert after. Expand Contact, combine the form search with its tag, show All preserving the query, then an empty result and Clear filters. Template block names and tags remain Slovak even in other administration languages.",
            prepare: async ({ I, action }) => {
                await I.clickCss(".pb-workbench-path [data-type=section]");
                await I.click(action("more"));
                await I.click(action("after"));
                await I.waitForVisible(".pb-library--section", 10);
                await I.clickCss(".pb-library .library-tab-link[data-library-type=library]");
            },
            shot: async ({ I }) => {
                const library = ".pb-library--section .library-tab-item--library .library-template-block--section";
                await I.videoClick(`${library} [data-library-item-id="c2VjdGlvbi9Db250YWN0"] .library-group-toggle`);
                await I.waitForVisible(`${library} .library-tab-item-button__toggler.active .library-full-width-item`, 10);
                await I.wait(3);
                await I.fillField(`${library} .library-filter-input`, "form");
                await I.videoClick(`${library} [data-library-tag="Formulár"]`);
                await I.waitForVisible(`${library} .library-tab-item-button__toggler.active .library-full-width-item`, 10);
                await I.wait(3);
                await I.videoClick(`${library} [data-library-tag=""]`);
                await I.seeInField(`${library} .library-filter-input`, "form");
                await I.fillField(`${library} .library-filter-input`, "nenájdený blok");
                await I.waitForVisible(`${library} .library-empty`, 10);
                await I.videoClick(`${library} .library-clear-filters`);
                await I.seeInField(`${library} .library-filter-input`, "");
                await I.videoClick(".pb-library__close");
                await I.waitForInvisible(".pb-library", 10);
            }
        },
        {
            "id": "duplicate-move",
            "type": "auto",
            "durationSeconds": 39,
            "title": "Duplicate and reorder items",
            "text-sk": "Pri opakovaní obsahu vyskúšajte Duplikovať vedľa. Kópia vznikne hneď za výberom a automaticky sa označí. Netreba vyberať cieľ. Na malú zmenu poradia použite v Ďalších akciách Posunúť vyššie alebo Posunúť nižšie. Na okraji zoznamu je príslušná akcia neaktívna. Pôvodný presun s výberom miesta zostáva dostupný. Pri opakovateľných položkách sa presúvate len medzi kompatibilnými položkami rovnakého rodiča.",
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
            "text-sk": "Mobil, tablet a desktop prepínate pri výbere editora. Vyberte stĺpec a jeho šírku. Lištu nahradí pomocník s označením zariadenia. Šípkami priamo v stĺpcoch meníte rozloženie pre túto veľkosť. Ukončiť alebo Escape obnoví lištu. Na mobile otvoríte skrátenú cestu tlačidlom pri type bloku. Panel Štruktúra sa po výbere zatvorí.",
            "notes": "Mobil > stĺpec > šírka 12, zmeniť na 11 a späť na 12. Rozbaliť skrátenú cestu, otvoriť Štruktúru a výberom ju zavrieť. Tablet a Desktop, návrat k rozloženiu pre desktop. MANUAL: zväčšiť detail mobilného iframe a vystrihnúť prázdnu plochu mimo neho.",
            shot: async ({ I, services, fixture, action, treeRow, waitForPageBuilder }) => {
                await I.videoClick("a[title=Mobil]");
                await waitForPageBuilder("wait for the mobile viewport", () => window.innerWidth < 768);
                await I.videoClick(services);
                await I.videoClick(action("resize"));
                await I.waitForVisible(".pb-workbench.is-resizing .pb-resize-hint", 10);
                await I.see("Mobil", ".pb-resize-hint");
                await I.waitForVisible(`${fixture} .pb-size-changer__down`, 10);
                await I.videoClick(locate(`${fixture} .pb-size-changer__down`).first());
                await I.waitForElement(`${fixture} .col-11`, 10);
                await I.wait(3);
                await I.videoClick(locate(`${fixture} .pb-size-changer__up`).first());
                await I.waitForElement(`${fixture} .col-12`, 10);
                await I.videoClick(action("end-resize"));
                await I.waitForInvisible(".pb-resize-hint", 10);
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
            "type": "auto",
            "durationSeconds": 7,
            "title": "Preview the resulting page",
            "text-sk": "Na kontrolu výslednej stránky použite samostatný Náhľad.",
            "notes": "Click Preview and switch to its tab. Reopen the same preview URL in the recording tab and slowly scroll to the bottom so the resulting page is included in the main WebM. Cut out the tab cleanup and repeated navigation.",
            prepare: async ({ I }) => {
                await I.switchTo();
                await I.waitForVisible("#datatableInit_modal button.btn-preview", 10);
            },
            shot: async ({ I, services, closeEditor }) => {
                await I.videoClick("#datatableInit_modal button.btn-preview");
                await I.usePlaywrightTo("wait for the preview tab", async ({ page }) => {
                    await page.waitForFunction(() => window.previewWindow != null && !window.previewWindow.closed);
                });
                await I.switchToNextTab();
                await I.waitForVisible(services, 20);
                const previewUrl = await I.grabCurrentUrl();
                await I.closeCurrentTab();
                await closeEditor();
                // Playwright records each tab separately; retain this view in the main recording.
                await I.amOnPage(previewUrl);
                await I.waitForVisible(services, 20);
                await I.videoScroll();
                await I.wait(5);
            }
        },
        {
            "id": "outro",
            "type": "auto",
            "durationSeconds": 18,
            "title": "Page Builder documentation",
            "text-sk": "Pri ďalšej úprave teda začnite výberom obsahu. V ceste alebo v Štruktúre overte správnu úroveň a potom použite nástroje hornej lišty. Podrobný návod k Page Builderu nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.",
            "notes": "Open the Slovak documentation home page during preparation. Click the editor manual and Page Builder sidebar links, then slowly scroll through the article. Include its public URL in the video description.",
            prepare: async ({ I }) => {
                await I.switchTo();
                await I.amOnPage("http://docs.interway.sk:3000/sk/");
                await I.waitForVisible(locate(".sidebar-nav a").withText("Manuál pre redaktora"), 20);
                await I.waitForVisible("article h1", 20);
            },
            shot: async ({ I }) => {
                await I.wait(1);
                await I.videoClick(locate(".sidebar-nav a").withText("Manuál pre redaktora"));
                await I.waitForVisible(locate(".sidebar-nav a").withText("Page Builder"), 20);
                await I.wait(1);
                await I.usePlaywrightTo("scroll the documentation menu to Page Builder", async ({ page }) => {
                    await page.locator(".sidebar-nav").getByRole("link", { name: "Page Builder", exact: true }).evaluate(async link => {
                        await document.fonts.ready;
                        const sidebar = link.closest(".sidebar");
                        const start = sidebar.scrollTop;
                        const linkBox = link.getBoundingClientRect();
                        const target = start + linkBox.top - sidebar.getBoundingClientRect().top -
                            (sidebar.clientHeight - linkBox.height) / 2;
                        const end = Math.max(0, Math.min(target, sidebar.scrollHeight - sidebar.clientHeight));
                        if (Math.abs(end - start) < 1) return;

                        // Center the link over one second without moving the article.
                        const startedAt = performance.now();
                        await new Promise(resolve => {
                            const scrollStep = now => {
                                const progress = Math.min((now - startedAt) / 1000, 1);
                                const eased = progress * progress * (3 - 2 * progress);
                                sidebar.scrollTo({ top: start + (end - start) * eased, behavior: "instant" });
                                if (progress < 1) requestAnimationFrame(scrollStep);
                                else resolve();
                            };
                            requestAnimationFrame(scrollStep);
                        });
                    });
                });
                await I.videoClick(locate(".sidebar-nav a").withText("Page Builder"));
                await I.waitForText("Page Builder", 20, "article h1");
                await I.wait(2);
                await I.videoScroll();
            }
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
    const oldColumn = locate(".pb-column").first();
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
    const closeEditor = async () => {
        await I.switchTo();
        DTE.cancel();
        await I.waitForInvisible("div.DTED.show", 10);
    };

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
        context: { iframe, oldColumn, fixture, services, action, treeRow, waitForPageBuilder, typeText, closeEditor },
        setup: async () => {
            login("admin");
            Document.resetPageBuilderMode();
        },
        prepare: async shot => {
            if (shot.id === "old-editor" || shot.id === "outro") return;
            await prepareEditor();
        },
        cleanup: async shot => {
            if (shot.id === "outro" || shot.id === "preview") return;
            await closeEditor();
        }
    });
}).tag("@video");
