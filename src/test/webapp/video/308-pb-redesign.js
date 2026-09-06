Feature("video.308-pb-redesign");

// Source: https://github.com/webjetcms/webjetcms/pull/308, feature/pb-redesign at 3addf9ca4.
// Compare with origin/main: floating gear palettes are replaced by the shared toolbar.
// Documentation: https://docs.webjetcms.sk/latest/sk/redactor/webpages/pagebuilder
// The shot plan describes the edited timeline. Short holds below provide editing room;
// extend them to the recorded narration and insert the explicitly marked manual shots.
// Run from src/test/webapp: npm run video video/308-pb-redesign.js
// Generate speech only on request: npm run audio video/308-pb-redesign.js

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
Používali ste Page Builder a po aktualizácii hľadáte známe nástroje? V tomto videu si ukážeme, kam sa presunuli a ako sa pracuje s novou verziou. Stránku naďalej skladáte z pripravených blokov. Vylepšili sme ale spôsob ich výberu a úprav.

V staršej verzii ste prešli myšou nad obsah a nástroje otvorili ozubeným kolieskom pri farebnom rámiku. Teraz kliknete priamo do obsahu. Vyberie sa príslušný blok a jeho nástroje nájdete na jednom mieste, v pevnej lište pod nástrojmi textového editora. Kliknutím do textu môžete rovno písať. Výber zostáva stabilný a rámik sa počas písania zjemní.

Najprv si vysvetlime štruktúru. Modrá sekcia je veľká časť stránky, napríklad predstavenie služieb. V nej je červený kontajner, ktorý drží obsah pokope. Riadok usporadúva stĺpce vedľa seba. Zelený stĺpec obsahuje text, obrázok alebo aplikáciu. Oranžová označuje opakovateľnú položku alebo duplikovateľný riadok. Tieto úrovne nie sú novým spôsobom skladania stránky. Nové ovládanie vám ich pomáha jasnejšie rozlíšiť.

Pozrite sa na cestu v hornej lište. Ukazuje, do ktorej sekcie, kontajnera a riadka patrí vybraný stĺpec. Chcete upraviť pozadie celej sekcie? Kliknite v ceste na Sekcia. Chcete pracovať len so stĺpcom? Vyberte ho v obsahu. Pred každou akciou si tak ľahko skontrolujete, ktorej časti sa zmena týka.

Nie každý prvok má rovnaké možnosti. Bežný riadok slúži na orientáciu. Opakovateľnú položku možno kopírovať, presúvať a zmazať, ale nemá vlastné nastavenie štýlu ani šírky stĺpca. Samostatný editovateľný text nemá štrukturálne akcie. Ak tlačidlo nevidíte, overte si, aký typ prvku máte vybraný.

Kde teraz nájdete pôvodné nástroje? Otvorte Ďalšie akcie. Tu je Štýl s nastavením pozadia, zarovnania či odsadenia. Nájdete tu aj vloženie pred alebo za blok, presun, pôvodné duplikovanie, pridanie do obľúbených a zmazanie. Šírka stĺpca a nové Duplikovať vedľa sú priamo v lište. Ponuka sa vždy prispôsobí výberu.

Na dlhšej stránke pomôže tlačidlo Štruktúra. Otvorí strom blokov vľavo nad obsahom. Kliknutie na vetvu ju vyberie a rozbalí. Šípkou pri nej ju môžete zbaliť. Názvy vychádzajú z nadpisov alebo textu blokov. Do vyhľadávania stačí napísať napríklad Kontakt. Po výbere sa presuniete na príslušné miesto stránky.

Označenie Skrytý znamená, že blok práve nie je viditeľný. Jeho výber ho nezobrazí ani neprepne aktívnu kartu. Strom používajte na orientáciu a výber. Na presun slúžia akcie v hornej lište. Funguje aj klávesnica: šípky na pohyb a rozbaľovanie, Enter na výber a Escape na zatvorenie panelu.

Pridávanie blokov má nový vstup. Kliknite na plus v hornej lište. Nemusíte predtým hľadať ozubené koliesko ani označiť blok. Priamo v stránke sa ukážu miesta vloženia. Modré pásy pridávajú sekcie, ružové kontajnery a zelené pluská stĺpce. Popis vám povie, pred ktorý blok alebo za ktorý blok vkladáte. Rozbalené medzery sú iba dočasnou pomôckou.

Vyberte miesto a otvorí sa výber blokov príslušného typu. Zostávajú známe karty Základné, Knižnica a Obľúbené. V knižnici naďalej nájdete bloky pripravené pre svoj web, vyhľadávanie a štítky. Po vložení sa nový blok označí a môžete upraviť jeho obsah. Ak knižnicu zatvoríte, vrátite sa k vybranému plusku. Celý režim ukončíte cez Ukončiť, Escape alebo opätovným kliknutím na plus.

Pri opakovaní obsahu vyskúšajte Duplikovať vedľa. Kópia vznikne hneď za výberom a automaticky sa označí. Netreba vyberať cieľ. Na malú zmenu poradia použite v Ďalších akciách presun pred predchádzajúci alebo za nasledujúci blok. Na okraji zoznamu je príslušná akcia neaktívna. Pôvodný presun s výberom miesta zostáva dostupný. Pri opakovateľných položkách sa presúvate len medzi kompatibilnými položkami rovnakého rodiča.

Aj prepínanie mobilu, tabletu a desktopu zostáva pri výbere editora. Vyberte zariadenie, potom stĺpec a jeho šírku. Nastavujete rozloženie pre danú veľkosť. V úzkom zobrazení sa cesta skráti a nadradené prvky otvoríte tlačidlom vedľa aktuálneho typu. Panel Štruktúra sa po výbere na úzkej obrazovke zatvorí.

Ikona oka postupne prepína rámik vybraného bloku, skryté rámiky a rámiky celej jeho hierarchie. Posledný režim pomôže pochopiť vnorenie. Prehliadač si voľbu pamätá. Nástroje a obsah zostávajú dostupné aj bez rámikov. Na kontrolu výslednej stránky použite samostatný Náhľad.

Pri ďalšej úprave teda začnite výberom obsahu. V ceste alebo v Štruktúre overte správnu úroveň a potom použite nástroje hornej lišty. Podrobný návod k Page Builderu nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.
`);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(`
Formát: 16:9, 1920 x 1080. Návrh vysvetľujúceho videa pre používateľov staršej verzie.
Časy sú orientačná časová os strihu, nie časovanie automatizácie. Spresniť podľa hlasu.
Prihlásenie, prípravu dočasného obsahu a zatvorenie bez uloženia vystrihnúť.
0:00-0:18 | MANUAL | Titulok „Nový Page Builder: kde nájdete známe nástroje“. Celkový pohľad nového editora z nasledujúceho záberu.
0:18-0:30 | MANUAL | Autentická stará snímka docs/sk/redactor/webpages/pagebuilder.png: farebné rámiky, ovládač pri bloku a plávajúca paleta. Označiť „Pred aktualizáciou“. Nový editor neupravovať tak, aby predstieral starý.
0:30-0:48 | AUTO 1 | Kliknúť do „Naše služby“, ukázať jeden rámik a pevnú lištu, dopísať krátky text. MANUAL: detail jemnejšieho rámika počas písania.
0:48-1:14 | AUTO 2 | Postupne vybrať Stĺpec, Riadok, Kontajner a Sekcia v ceste. MANUAL: postupne pripájať popisky úrovní k reálnemu záberu, zachovať farby rozhrania.
1:14-1:35 | AUTO 2 | Klik na Sekcia v ceste, návrat do stĺpca cez obsah. Zdôrazniť, ktorá úroveň dostane akciu.
1:35-1:56 | AUTO 3 | Vybrať obyčajný riadok, opakovateľnú položku „Konzultácia“ a samostatný text. Ukázať rozdielny rozsah tlačidiel.
1:56-2:23 | AUTO 4 | Stĺpec > Ďalšie akcie > Štýl. Detail existujúcich vlastností, zavrieť Zrušiť. Znova otvoriť menu a ukázať pôvodné operácie; nič nezmazať ani neukladať do obľúbených.
2:23-2:53 | AUTO 5 | Otvoriť Štruktúru, vybrať a rozbaliť sekciu kliknutím na názov, zbaliť šípkou. Vyhľadať Kontakt, vybrať stĺpec a ukázať presun na obsah bez zúženia plátna.
2:53-3:21 | AUTO 6 | Vyhľadať „Sezónna ponuka“, vybrať Skrytý, obsah zostáva skrytý. Vymazať filter, ukázať pohyb klávesnicou a zavrieť Escape. MANUAL: krátky popis „Presun cez Ďalšie akcie“.
3:21-3:55 | AUTO 7 | Plus v lište, modré a ružové pásy, zelené pluská a popisy polôh. Vybrať miesto na stĺpec medzi existujúcimi stĺpcami.
3:55-4:28 | AUTO 8 | Ukázať karty Základné, Knižnica, Obľúbené a kontext vloženia. Zrušiť knižnicu, ukázať návrat na plus. Otvoriť znova, vložiť základný stĺpec a ukázať fokus v jeho obsahu. Nový režim plus ukončiť cez Ukončiť.
4:28-5:07 | AUTO 9 | Vybrať Konzultácia, Duplikovať vedľa, podržať vybranú kópiu. Presunúť ju za Podpora a ukázať neaktívny ďalší presun na konci. Otvoriť pôvodný Presunúť, ukázať povolené ciele, zrušiť Escape.
5:07-5:35 | AUTO 10 | Mobil > stĺpec > šírka 12, zmeniť na 11 a späť na 12. Rozbaliť skrátenú cestu, otvoriť Štruktúru a výberom ju zavrieť. Tablet a Desktop, návrat k rozloženiu pre desktop. MANUAL: zväčšiť detail mobilného iframe a vystrihnúť prázdnu plochu mimo neho vrátane druhej stopy kurzora v rodičovskom dokumente.
5:35-5:57 | AUTO 11 | S otvorenou Štruktúrou prepnúť oko: vybraný blok > žiadne rámiky > celá hierarchia > vybraný blok. Panel zostáva otvorený. Zavrieť ho a podržať čistý záber editora.
5:57-6:04 | MANUAL | Zväčšiť samostatné tlačidlo Náhľad v päte editora; prípadné otvorenie ďalšej karty natočiť samostatne.
6:04-6:22 | MANUAL | Zopakovať cestu „Obsah > správna úroveň > akcia“ a ukázať slovenský návod https://docs.webjetcms.sk/latest/sk/redactor/webpages/pagebuilder. Pred publikovaním overiť, že verejný návod už opisuje tento PR. Outro s odkazom v popise videa.
`);
});

Scenario("308-pb-redesign", async ({ I, DTE, Document, login }) => {
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

    login("admin");
    Document.resetPageBuilderMode();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.videoClick("div.DTED.show button.maximize");
    I.waitForInvisible("div.DTED.show button.maximize", 10);
    I.switchTo(iframe);
    I.waitForVisible(".pb-workbench", 20);

    // Prepare isolated, browser-only content. Closing the editor discards every change.
    I.executeScript(() => {
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

    // Shot 1: direct text editing and stable selection in the shared toolbar.
    I.videoClick(services);
    I.waitForVisible(".pb-outline[data-type=column]:not([hidden])", 10);
    I.pressKey("End");
    await typeText(" Spoločne.");
    I.waitForText("Spoločne.", 10, services);
    I.wait(3); // Presentation hold after the content assertion.

    // Shot 2: inspect the hierarchy without changing page content.
    for (const type of ["row", "container", "section"]) {
        I.videoClick(`.pb-workbench-path [data-type=${type}]`);
        I.waitForElement(`.pb-workbench-path [data-type=${type}][aria-current=location]`, 10);
        I.wait(3); // Editing room for the hierarchy explanation.
        I.videoClick(services);
    }

    // Shot 3: show the actions available for an item and for standalone text.
    I.videoClick(locate(`${fixture} li.pb-duplicable-element`).first());
    I.waitForElement(".pb-workbench-path [data-type=item][aria-current=location]", 10);
    I.seeElement(action("duplicate-adjacent"));
    I.dontSeeElement(action("resize"));
    I.wait(3);
    I.videoClick(`${fixture} .video-note`);
    I.waitForElement(".pb-workbench-path [data-type=text][aria-current=location]", 10);
    I.dontSeeElement(action("duplicate-adjacent"));
    I.wait(3);

    // Shot 4: find the old tools in More actions and cancel the style dialog.
    I.videoClick(services);
    I.videoClick(action("more"));
    I.waitForVisible(action("style"), 10);
    I.wait(3);
    I.videoClick(action("style"));
    I.waitForVisible(".pb-modal", 10);
    I.wait(5);
    I.videoClick(".pb-modal__footer__button-close");
    I.waitForInvisible(".pb-modal", 10);
    I.videoClick(action("more"));
    I.waitForVisible(action("add_to_favorites"), 10);
    I.wait(4);
    I.pressKey("Escape");

    // Shot 5: selecting a branch expands it; search selects and scrolls to a block.
    I.videoClick(action("structure"));
    I.waitForVisible(".pb-structure", 10);
    I.videoClick(treeRow("section", "Naše služby"));
    I.waitForElement(locate(".pb-structure [role=treeitem][data-type=section][aria-expanded=true]").withText("Naše služby"), 10);
    I.saveScreenshot("308-pb-redesign-structure.png");
    I.wait(3);
    I.videoClick(locate(".pb-structure > ul > li > div > [data-pb-expand]").first());
    I.fillField(".pb-structure input[type=search]", "Kontakt");
    I.waitForVisible(treeRow("column", "Kontakt"), 10);
    I.videoClick(treeRow("column", "Kontakt"));
    I.waitForElement(".pb-workbench-path [data-type=column][aria-current=location]", 10);
    I.wait(4);

    // Shot 6: hidden selection does not reveal authored content; use keyboard navigation.
    I.fillField(".pb-structure input[type=search]", "Sezónna ponuka");
    I.waitForVisible(treeRow("section", "Sezónna ponuka"), 10);
    I.videoClick(treeRow("section", "Sezónna ponuka"));
    I.see("Skrytý", ".pb-structure");
    I.dontSeeElement(".pb-video-hidden-autotest");
    I.wait(4);
    I.fillField(".pb-structure input[type=search]", "");
    I.videoClick(treeRow("section", "Naše služby"));
    I.pressKey("ArrowRight");
    I.pressKey("ArrowDown");
    I.pressKey("Enter");
    I.pressKey("Escape");
    I.waitForInvisible(".pb-structure", 10);
    I.videoClick(services);

    // Shot 7: choose an actual insertion point using the regression-tested focus pattern.
    I.videoClick(action("insert"));
    I.waitForVisible(".pb-insert-hint", 10);
    I.waitForVisible(".pb-insert-point[data-type=column]", 10);
    I.wait(5);
    I.saveScreenshot("308-pb-redesign-insert.png");
    I.executeScript(() => {
        const point = window.pageBuilder.ui.insertPoints.find(point =>
            point.type === "column" && point.previous && point.next && point.parent.closest(".pb-video-autotest"));
        point.button.attr("data-autotest-insert", "true");
        point.button[0].focus();
    });
    I.videoClick("[data-autotest-insert]");
    I.waitForVisible(".pb-library--column", 10);
    I.seeElement(".pb-insert-context");

    // Shot 8: tour the existing library, return to the destination, then insert a column.
    for (const type of ["basic", "library", "favorite"]) {
        I.videoClick(`.pb-library .library-tab-link[data-library-type=${type}]`);
        I.waitForVisible(`.pb-library .library-tab-item--${type}`, 10);
        I.wait(3);
    }
    I.videoClick(".pb-library__footer__button");
    I.waitForInvisible(".pb-library", 10);
    I.waitForVisible("[data-autotest-insert]", 10);
    I.wait(3);
    I.videoClick("[data-autotest-insert]");
    I.waitForVisible(".pb-library--column", 10);
    I.videoClick(".pb-library .library-tab-link[data-library-type=basic]");
    I.videoClick(locate(".pb-library .library-tab-item--basic .library-template-block--column .library-tab-item-button").first());
    I.waitForInvisible(".pb-library", 10);
    I.waitForInvisible(".pb-insert-layer", 10);
    await waitForPageBuilder("wait for the inserted column editor to receive focus", () => {
        const field = window.pageBuilder.ui.selected?.querySelector("[data-ckeditor-instance]");
        const editor = field && CKEDITOR.instances[field.dataset.ckeditorInstance];
        return editor?.status === "ready" && editor.focusManager.hasFocus;
    });
    I.wait(4);
    I.videoClick(action("insert"));
    I.waitForVisible(".pb-insert-hint", 10);
    I.videoClick(action("end-insert"));
    I.waitForInvisible(".pb-insert-layer", 10);

    // Shot 9: duplicate and reorder only the temporary items, then cancel targeted movement.
    I.videoClick(locate(`${fixture} li.pb-duplicable-element`).withText("Konzultácia"));
    I.videoClick(action("duplicate-adjacent"));
    I.waitForElement(`${fixture} .video-services-list > li:nth-child(3)`, 10);
    I.waitForText("Konzultácia", 10, `${fixture} .video-services-list > li:nth-child(2)`);
    I.wait(3);
    I.videoClick(action("more"));
    I.videoClick(action("next"));
    I.waitForText("Konzultácia", 10, `${fixture} .video-services-list > li:nth-child(3)`);
    I.waitForText("Podpora", 10, `${fixture} .video-services-list > li:nth-child(2)`);
    I.videoClick(action("more"));
    I.seeElement(`${action("next")}:disabled`);
    I.wait(4);
    I.videoClick(action("move"));
    I.waitForElement("#wjInline-docdata.pb-is-moving-child", 10);
    I.wait(4);
    I.pressKey("Escape");
    I.waitForInvisible("#wjInline-docdata.pb-is-moving-child", 10);

    // Shot 10: responsive widths and the compact ancestor path.
    I.videoClick("a[title=Mobil]");
    await waitForPageBuilder("wait for the mobile viewport", () => window.innerWidth < 768);
    I.videoClick(services);
    I.videoClick(action("resize"));
    I.waitForVisible(`${fixture} .pb-size-changer__down`, 10);
    I.videoClick(locate(`${fixture} .pb-size-changer__down`).first());
    I.waitForElement(`${fixture} .col-11`, 10);
    I.wait(3);
    I.videoClick(locate(`${fixture} .pb-size-changer__up`).first());
    I.waitForElement(`${fixture} .col-12`, 10);
    I.pressKey("Escape");
    I.videoClick(action("ancestors"));
    I.waitForVisible(".pb-workbench-path.is-expanded", 10);
    I.wait(3);
    I.pressKey("Escape");
    I.videoClick(action("structure"));
    I.fillField(".pb-structure input[type=search]", "Naše služby");
    I.videoClick(treeRow("column", "Naše služby"));
    I.waitForInvisible(".pb-structure", 10);
    I.videoClick("a[title=Tablet]");
    await waitForPageBuilder("wait for the tablet viewport", () => window.innerWidth >= 768 && window.innerWidth < 1200);
    I.wait(3);
    I.videoClick("a[title=Desktop]");
    await waitForPageBuilder("wait for the desktop viewport", () => window.innerWidth >= 1200);
    I.videoClick(services);

    // Shot 11: all three guide modes retain the toolbar and structure panel.
    I.videoClick(action("structure"));
    I.fillField(".pb-structure input[type=search]", "");
    for (const mode of ["hidden", "all", "selected"]) {
        I.videoClick(action("guides"));
        I.waitForElement(`${action("guides")}[data-pb-guides=${mode}]`, 10);
        I.seeElement(".pb-structure");
        I.wait(4);
    }
    I.videoClick(".pb-structure [data-pb-action=close-structure]");
    I.waitForInvisible(".pb-structure", 10);
    I.wait(5);

    // Discard the browser-only fixture. Preview and documentation are manual closing shots.
    I.switchTo();
    DTE.cancel();
}).tag("@video");
