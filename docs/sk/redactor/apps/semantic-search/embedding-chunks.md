# Sémantický index

Sémantický index prevádza obsah stránok na vektorové reprezentácie (`embedding`) pomocou nastaveného AI poskytovateľa a ukladá ich do vektorovej databázy. Používa sa pre sémantické vyhľadávanie, hybridné vyhľadávanie aj pre generovanie RAG odpovede vo vyhľadávaní.

Pre presnejšie výsledky sa obsah rozdeľuje na menšie časti - **chunky**. Každý chunk je indexovaný samostatne, čo systému umožňuje porovnávať dotazy s konkrétnymi časťami textu a nie s celou stránkou naraz.

Správu vektorov nájdete v sekcii **Nastavenia → Sémantický index**.

Index je rozdelený na karty **Webové stránky** a **Markdown dokumenty**.

!>**Upozornenie:** Indexovanie **neprebieha okamžite**. Každá požiadavka (pridanie, úprava, vymazanie) sa zaradí do **fronty** a spracuje sa v pravidelných intervaloch pomocou cron úlohy.

Na zobrazenie zoznamu indexovaných objektov je potrebné mať právo Sémantický index.

## Indexovanie Markdown dokumentácie

Markdown dokumentácia a jej index sú spoločné pre všetky domény. Korene nastavuje správca globálne v `ragMarkdownFolders`. Indexovanie používa asistenta `RAG-EMB-INDEX` domény, ktorá požiadavku zaradila do fronty; tejto doméne sa zaznamená aj spotreba. Vyhľadávanie používa asistenta aktuálnej domény. Poskytovateľ a model vyhľadávania musia zodpovedať indexu.

Na karte **Markdown dokumenty** vyberte nakonfigurovaný koreň alebo jeho podpriečinok v strome. Prepínač **Zobraziť aj z podpriečinkov** nastavuje rozsah tabuľky aj akčných dialógov; po vypnutí sa použijú iba súbory priamo vo vybranom priečinku. Tabuľka obsahuje údaje **Cesta dokumentu** a **Názov dokumentu**, ktoré slúžia len na čítanie. Výber a nastavenie prepínača sa zachovajú pri prepnutí kariet.

Správca môže použiť aj serverové priečinky, napríklad `file:/srv/manuals`. Alias `file:/docs` vyžaduje globálne mapovanie v `symlinkTranslate`, napríklad `/docs/|/srv/documentation/webjetcms/`. Lokálne indexovanie samo nesprístupní súbory cez web.

**Dokumentácia musí používať jazykové priečinky**, napríklad `sk`, `en` alebo `cs`. Jazyk sa určí z najbližšieho rozpoznaného priečinka v celej ceste vrátane koreňa: súbory pod `file:/docs/sk/admin/users` dostanú jazyk `sk`. Rozpoznané jazyky vychádzajú z globálnej premennej `languages`; `sk`, `en` a `cs` sú podporované vždy. Súbory bez jazykového priečinka sa pri indexovaní preskočia.

Tlačidlá **Pridať indexovanie** a **Odstrániť indexovanie** otvoria dialóg, v ktorom môžete zmeniť priečinok aj zahrnutie podpriečinkov. Dialóg zobrazuje celkový počet dokumentov, počet indexovaných dokumentov a počet vo fronte pre zvolenú akciu. Pri indexovaní sa počítajú súbory s rozpoznaným jazykom a indexom aktuálneho poskytovateľa, modelu a odvodeného jazyka. Pri odstránení sa zahrnú všetky indexy vo vybranom rozsahu vrátane záznamov už neexistujúcich súborov.

Po potvrdení sa požiadavky zaradia do fronty. Spracuje ich `sk.iway.iwcm.rag.service.RagIndexCronTask`; chyby zostávajú vo fronte na ďalší pokus. Odstránenie zruší aj čakajúcu indexáciu rovnakého zdroja z inej domény. Požiadavky ostatných priečinkov zostanú zachované. Ak nie sú nastavené žiadne korene, zobrazí sa informácia o potrebnej konfigurácii a akčné tlačidlá sú vypnuté.

Pri prechode zo staršej verzie postupujte podľa [technickej dokumentácie](../../../custom-apps/apps/rag/markdown-search.md#správa-indexu-v-administrácii), najmä pri relatívnych cestách a starších doménových indexoch. Spoločné indexy s `domainId = 0` zostávajú platné; doména vo fronte určuje vlastníka spotreby.

!>Odstránenie indexu nemaže Markdown súbory ani nevypína ich automatickú indexáciu. Úloha `MarkdownIndexCronTask` pri ďalšom prehľadaní vytvorí index znova.

## Indexovanie webových stránok

Indexuje sa čistý text stránky bez HTML značiek. Do indexovania vstupujú iba webové stránky, ktoré sú povolené pre vyhľadávanie. Obsah sa rozdelí na chunky, ktoré sú zobrazené v tabuľke nižšie.

Každý chunk obsahuje tieto stĺpce:

- **ID entity** - ID webovej stránky.
- **Index časti** - poradie chunku v rámci stránky (0, 1, 2, ...).
- **Text časti** - text, pre ktorý bol vygenerovaný embedding. Samotný embedding sa v tabuľke nezobrazuje.
- **Poskytovateľ embeddingu** - poskytovateľ použitý pri vytvorení vektora, napr. `openai` alebo `gemini`.
- **Model embeddingu** - použitý embedding model, napr. `text-embedding-3-small` alebo `gemini-embedding-001`.
- **Dimenzie** - počet dimenzií vektora, napr. `1536`.
- **Jazyk** - jazyková verzia stránky.
- **Stav** - stav spracovania:
  - **COMPLETED** - úspešne spracovaný.
  - **ERROR** - nastala chyba.
  - **PENDING** - čaká na spracovanie.
- **Chybová správa** - popis chyby, ak spracovanie zlyhalo.
- **Dátum vytvorenia** - čas spracovania, nie čas pridania do fronty.

V databáze sa navyše ukladá `group_id` a stĺpce `root_group_l1`, `root_group_l2`, `root_group_l3`. Tieto hodnoty sa používajú na rýchle obmedzenie sémantického a hybridného vyhľadávania podľa priečinkov zvolených v aplikácii **Vyhľadávanie**.

![](datatable.png)

Po načítaní stránky sa zobrazí informačné oznámenie s aktuálnym poskytovateľom a modelom používaným na indexovanie. Nastavenie sa načíta zo systémového AI asistenta `RAG-EMB-INDEX`, ktorého môžete upraviť v sekcii **Nastavenia → AI asistenti**.

Ak systémový asistent ešte neexistuje, vytvorí sa automaticky podľa konfiguračných premenných `ragEmbeddingProvider` a `ragEmbeddingModel`. Po jeho vytvorení majú hodnoty nastavené v asistentovi prednosť pred konfiguračnými premennými.

!>**Upozornenie:** Po zmene poskytovateľa alebo modelu spustite indexovanie znova. Indexy vytvorené rôznymi kombináciami poskytovateľa a modelu sa ukladajú oddelene a môžu existovať súčasne aj pre rovnakú stránku. Vyhľadávací asistent `RAG-EMB-SEARCH` musí používať rovnaký identifikátor poskytovateľa a modelu ako index, v ktorom má vyhľadávať.

Položka vo fronte neobsahuje poskytovateľa ani model; tieto hodnoty sa načítajú z asistenta `RAG-EMB-INDEX` až pri spracovaní. Ak má rozpracovaná fronta dokončiť pôvodný index, nechajte ju pred zmenou asistenta úplne spracovať.

## Rozdelenie textu na chunky

Veľkosť chunkov sa nastavuje konfiguračnými premennými:

- `ragEmbeddingChunkSize` - približná cieľová veľkosť chunku v znakoch, predvolene `1000`. Maximálna veľkosť je o 50 % vyššia, teda predvolene `1500` znakov. Hodnota menšia alebo rovná nule vypne rozdeľovanie textu.
- `ragEmbeddingChunkOverlap` - približné prekrytie medzi susednými chunkmi v znakoch, predvolene `200`.

Chunky podľa možnosti začínajú a končia na rozpoznanej hranici vety alebo odseku, ktorá je najbližšie k cieľovej veľkosti. Veta zalomená do viacerých riadkov zostane spolu, pokiaľ sa zmestí do maximálnej veľkosti. Dlhšie vety alebo odseky sa rozdelia medzi slovami. Ak aj samotné slovo prekročí maximum, rozdelí sa uprostred.

Prekrytie sa podľa možnosti prispôsobuje celým vetám alebo odsekom, preto sa jeho skutočná veľkosť mení a môže byť aj nulová, ak je to potrebné na pokračovanie v texte alebo dodržanie maximálnej veľkosti. Každý ďalší chunk pridá nový obsah. Nové hranice sa na existujúce dokumenty použijú po opätovnom indexovaní.

Prekrytie sa používa na zachovanie kontextu medzi susednými časťami. Pri RAG odpovedi sa susedné chunky jednej stránky môžu znovu zlúčiť, pričom sa odstráni duplicitný text vzniknutý prekrytím.

!>**Upozornenie:** Staršie konfiguračné premenné `ragChunkSize` a `ragChunkOverlap` sa už nepoužívajú. Po zmene veľkosti chunkov alebo po prechode zo starších nastavení spustite opätovné indexovanie.

## Filtrovanie

V hlavičke tabuľky sú dostupné tieto filtre:

- **Výber priečinka** - zobrazí chunky len pre stránky z daného priečinka v rámci aktuálnej domény.
- **Zobraziť aj z podpriečinkov** - zahrnie do výsledkov aj stránky z podpriečinkov.
- **Poskytovateľ embeddingu** a **Model embeddingu** - obmedzia tabuľku na konkrétnu kombináciu uloženého indexu.

!>**Upozornenie:** Ak vyberiete **Koreňový priečinok** bez zapnutia možnosti **Zobraziť aj z podpriečinkov**, nezískate žiadne výsledky. Koreňový priečinok je virtuálny a neobsahuje stránky priamo.

## Presmerovanie z Webových stránok

V sekcii **Webové stránky** môžete pri zvolenom priečinku kliknúť na tlačidlo <button class="btn btn-sm buttons-selected btn-outline-secondary"><span><i class="ti ti-database-search"></i></span></button> v hlavičke priečinkov. Tým sa otvorí sekcia **Sémantický index** s automaticky nastaveným filtrom pre daný priečinok.

### Automatické indexovanie

Systém automaticky zaradí stránku do fronty pri:

- **vytvorení alebo úprave** - stránka sa indexuje alebo aktualizuje bez manuálneho zásahu,
- **zmazaní alebo presunutí do koša** - všetky súvisiace chunky sa odstránia z databázy,
- **obnovení z koša** - stránka sa opätovne indexuje.

### Manuálne indexovanie

Kliknite na tlačidlo <button class="btn btn-sm btn-success" type="button"><span><i class="ti ti-database-plus"></i></span></button> pre otvorenie dialógu indexovania.

Dialóg zobrazí prehľad stránok zvoleného priečinka - celkový počet, počet už indexovaných a počet vo fronte. Za indexované sa považujú iba stránky, ktoré majú index pre aktuálneho poskytovateľa a model asistenta `RAG-EMB-INDEX`. Index vytvorený iným poskytovateľom alebo modelom sa preto v tomto počte nezohľadní.

Manuálnu akciu možno vykonať iba nad priečinkami aktuálnej domény, pre ktoré má používateľ právo na úpravu. Pri výbere koreňového priečinka musí mať právo na všetky koreňové priečinky domény.

Priečinok aj voľba **Zobraziť aj z podpriečinkov** sa prevezmú z aktívneho filtra. Po potvrdení sa do fronty zaradia všetky vyhľadateľné stránky zo zvoleného rozsahu. Ak sa text chunku nezmenil, systém sa pokúsi použiť existujúci embedding s rovnakým poskytovateľom a modelom podľa jeho hash hodnoty. Opätovné indexovanie nahradí iba index aktuálnej kombinácie poskytovateľa a modelu; ostatné indexy rovnakej stránky zostanú zachované.

Akciu spustíte tlačidlom <button class="btn btn-primary"><i class="ti ti-check"></i> <span>Spustiť akciu</span></button>.

![](index-dialog.png)

### Manuálne odstránenie indexovania

Kliknite na tlačidlo <button class="btn btn-sm btn-danger" type="button"><span><i class="ti ti-database-minus"></i></span></button> pre otvorenie dialógu odstránenia indexov.

Dialóg prevezme priečinok aj voľbu **Zobraziť aj z podpriečinkov** a zobrazí rovnaký prehľad ako pri indexovaní, ale počet indexovaných stránok zahŕňa všetkých poskytovateľov a modely. Po potvrdení sa stránky zaradia do fronty na odstránenie všetkých chunkov pre stránky zvoleného rozsahu bez ohľadu na poskytovateľa a model.

Akciu spustíte tlačidlom <button class="btn btn-primary"><i class="ti ti-check"></i> <span>Spustiť akciu</span></button>.

![](remove-index-dialog.png)

## Chyby pri indexovaní

Ak pri indexovaní stránky nastane chyba, systém uloží záznam so stavom **ERROR** a skrátenou chybovou správou. Chyba sa zapisuje aj do administrátorského logu v kategórii **Vyhľadávanie** (`SEARCH`). Ak zlyhá spracovanie položky ešte na úrovni fronty, položka zostane vo fronte a systém sa ju pokúsi spracovať pri ďalšom behu cron úlohy.

!>**Upozornenie:** Konfiguračná premenná `ragEmbeddingDimensions` je globálna pre celú inštaláciu. Jej zmena vymaže celý sémantický index pre všetkých poskytovateľov a modely, pretože databázový stĺpec `vector(N)` má spoločnú dimenziu. Po zmene je potrebné znova indexovať celý obsah. Lokálny model `intfloat/multilingual-e5-base` vyžaduje hodnotu `768`.

## Detaily implementácie

Technický popis procesu indexovania nájdete v [dokumentácii pre vývojárov](../../../custom-apps/apps/rag/semantic-search/README.md).
