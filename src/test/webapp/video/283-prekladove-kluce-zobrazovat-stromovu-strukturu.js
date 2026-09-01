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

Scenario("ElevenLabs", ({ I }) => {
    I.say(`
Keď spravujete veľa prekladových kľúčov, nájsť správnu skupinu v jednom dlhom zozname môže zbytočne zdržiavať.

WebJET CMS preto zobrazuje prefixy prekladových kľúčov v prehľadnej stromovej štruktúre. Naľavo vidíte prefixy, ktoré poznáte z názvov kľúčov. Jednotlivé časti oddelené bodkou vytvárajú prirodzené úrovne stromu.

Po otvorení zostáva zvolená možnosť Všetky prekladové kľúče, takže vidíte kompletný obsah. Kliknutím na známy prefix button si okamžite zobrazíte iba texty tlačidiel.

Stačí vybrať napríklad prefix components, potom map a napokon width. Tabuľka sa pri každom výbere automaticky zúži. Zostanú v nej iba kľúče patriace do zvolenej vetvy. Nemusíte ručne skladať filter ani prechádzať nesúvisiace výsledky.

Ak poznáte názov prefixu, použite vyhľadávanie priamo nad stromom. WebJET CMS zobrazí zodpovedajúcu vetvu a jedným kliknutím opäť upraví obsah tabuľky. Vyhľadávanie môžete kedykoľvek vyčistiť.

Možnosť Všetky prekladové kľúče zruší výber prefixu a vráti celý zoznam. Zároveň získate späť celý strom, takže môžete bez zdržania pokračovať v ďalšom hľadaní.

Nové usporiadanie zrýchľuje orientáciu, udržiava prehľad aj pri veľkom počte kľúčov a necháva zvolenú vetvu aj výsledky stále na jednej obrazovke.

Podrobný návod na správu prekladových kľúčov nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.
`);
});

Scenario("Shot plan", ({ I }) => {
    I.say(`
0:00-0:05 - MANUAL: titulná karta „Prekladové kľúče - rýchlejšia orientácia v strome“ s logom WebJET CMS.
0:05-0:14 - Úvodný rozdelený pohľad na kompletný zoznam, zvolený koreň Všetky prekladové kľúče a krátke prepnutie na druhú stranu výsledkov.
0:14-0:24 - Kliknúť na button a ukázať iba známe kľúče tlačidiel, napríklad button.add a button.continue.
0:24-0:35 - Postupne kliknúť na components a components.map; po každom výbere ukázať zúženú tabuľku a ďalšiu úroveň stromu.
0:35-0:46 - Kliknúť na components.map.width a ukázať iba dva zodpovedajúce kľúče v tabuľke.
0:46-1:01 - Do vyhľadávania stromu zadať grideditor, spustiť hľadanie, kliknúť na výsledok, ukázať kľúče grideditor a potom vyhľadávanie vyčistiť.
1:01-1:17 - Kliknúť na Všetky prekladové kľúče a podržať obnovený celý strom aj zoznam pre návrat k ďalšiemu hľadaniu a záverečné zhrnutie prínosov.
1:17-1:30 - MANUAL: otvoriť https://docs.webjetcms.sk/latest/sk/admin/settings/translation-keys/README a zobraziť záverečný titulok „Podrobný návod nájdete v dokumentácii WebJET CMS.“
`);
});

Scenario("283-prekladove-kluce-zobrazovat-stromovu-strukturu", ({ I, DT, login }) => {
    login("admin");
    I.amOnPage("/admin/v9/settings/translation-keys/");
    I.waitForVisible(treeSelector, 20);
    I.waitForElement(`${rootNode} > a.jstree-clicked[aria-selected='true']`, 20);
    I.waitForVisible(`${tableSelector}_wrapper table`, 20);
    DT.waitForLoader();

    // Shot 1: show the complete list and the new prefix tree side by side.
    I.see("Všetky prekladové kľúče", treeSelector);
    I.videoClick(locate(`${tableWrapper} button.page-link`).withText("2"), 0.3);
    I.waitForElement(
        locate(`${tableWrapper} li.dt-paging-button.page-item.active button.page-link`).withText("2"),
        20
    );
    DT.waitForLoader();
    I.wait(5);

    // Shot 2: select a familiar prefix and show only button labels.
    I.videoClick(`${buttonNode} > a.jstree-anchor`, 0.35);
    I.waitForElement(`${buttonNode} > a.jstree-clicked[aria-selected='true']`, 20);
    DT.waitForLoader();
    I.waitForText("button.add", 20, tableSelector);
    I.see("button.continue", tableSelector);
    I.wait(7);

    // Shot 3: follow the dot-separated hierarchy into a specific branch.
    I.videoClick(`${componentsNode} > a.jstree-anchor`, 0.5);
    I.jstreeWaitForLoader();
    I.waitForElement(`${mapNode} > a.jstree-anchor`, 20);
    I.waitForElement(`${componentsNode} > a.jstree-clicked[aria-selected='true']`, 20);
    DT.waitForLoader();
    I.wait(3);

    I.videoClick(`${mapNode} > a.jstree-anchor`, 0.3);
    I.jstreeWaitForLoader();
    I.waitForElement(`${widthNode} > a.jstree-anchor`, 20);
    I.waitForElement(`${mapNode} > a.jstree-clicked[aria-selected='true']`, 20);
    DT.waitForLoader();
    I.wait(3);

    I.videoClick(`${widthNode} > a.jstree-anchor`, 0.45);
    I.waitForElement(`${widthNode} > a.jstree-clicked[aria-selected='true']`, 20);
    DT.waitForLoader();
    I.waitForText("components.map.width.short", 20, tableSelector);
    I.dontSee("components.map.address", tableSelector);
    I.wait(7);

    // Shot 4: search the tree and use the result to filter the table.
    I.videoClick("#tree-folder-search-input", 0.25);
    I.fillField("#tree-folder-search-input", "grideditor");
    I.videoClick("#tree-folder-search-button", 0.4);
    I.jstreeWaitForLoader();
    I.waitForElement(`${gridEditorNode} > a.jstree-search`, 20);
    I.wait(3);

    I.videoClick(`${gridEditorNode} > a.jstree-anchor`, 0.55);
    I.jstreeWaitForLoader();
    I.waitForElement(`${gridEditorNode} > a.jstree-clicked[aria-selected='true']`, 20);
    DT.waitForLoader();
    I.waitForText("grideditor.column", 20, tableSelector);
    I.wait(5);

    I.videoClick("#tree-folder-search-clear-button", 0.3);
    I.jstreeWaitForLoader();
    I.dontSeeInField("#tree-folder-search-input", "grideditor");
    I.waitForElement(`${componentsNode} > a.jstree-anchor`, 20);
    I.wait(3);

    // Shot 5: restore the complete list and hold the final application state.
    I.videoClick(`${rootNode} > a.jstree-anchor`, 0.4);
    I.waitForElement(`${rootNode} > a.jstree-clicked[aria-selected='true']`, 20);
    DT.waitForLoader();
    I.waitForElement(`${componentsNode} > a.jstree-anchor`, 20);
    I.videoClick(locate(`${tableWrapper} button.page-link`).withText("2"), 0.3);
    I.waitForElement(
        locate(`${tableWrapper} li.dt-paging-button.page-item.active button.page-link`).withText("2"),
        20
    );
    DT.waitForLoader();
    I.wait(8);
}).tag("@video");
