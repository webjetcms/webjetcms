# Vyhľadávanie

## Rýchle vyhľadávanie v hlavičke

Tlačidlo **Hľadať…** v hlavičke otvorí vyhľadávanie ponad aktuálnu stránku. Je dostupné v celej administrácii. Otvoríte ho aj klávesmi **Ctrl+K** vo Windows alebo **⌘K** na Macu; po otvorení môžete hneď písať. Skratka nepreberá príkazy pri písaní v inom textovom poli alebo editore.

Pred zadaním výrazu uvidíte posledné hľadania a odkazy na dostupné sekcie. Históriu si prehliadač pamätá osobitne pre prihláseného používateľa a doménu. Jednotlivé záznamy aj celú históriu môžete odstrániť.

Rozsah **Všetko** zobrazuje najviac tri výsledky z každej oblasti. Kliknutím na rozsah alebo odkaz **Všetky** zobrazíte výsledky iba z vybratej oblasti. Ponuka sa riadi vašimi oprávneniami:

- **Web stránky** – návrhy podľa názvu alebo URL stránky a hľadanie v obsahu. Návrh stránky otvorí jej editor.
- **Súbory** – hľadanie v názvoch a indexovanom obsahu súborov.
- **Administrácia** – odkazy na dostupné položky menu vrátane ich kariet.
- **Používatelia** – hľadanie podľa mena, priezviska, celého mena, emailu alebo prihlasovacieho mena.
- **Textové kľúče** – hľadanie kľúčov a ich prekladov.
- **Dokumentácia** – odkaz na vyhľadanie výrazu na docs.webjetcms.sk v novej karte. Výsledky dokumentácie sa zatiaľ nenačítavajú priamo do dialógu.

Na úvodnej stránke sa predvolí **Všetko**. Pri otvorení z podporovanej sekcie sa predvolí jej rozsah. Zmena rozsahu platí do zatvorenia dialógu. V poli ho prepnete aj pomocou **Alt+šípka vľavo/vpravo**. **Tab** a **Shift+Tab** presúvajú fokus medzi ovládacími prvkami, šípky hore/dole vyberajú výsledok a **Enter** ho otvorí.

Odkaz **Otvoriť výsledky v sekcii…** prenesie hľadaný výraz do príslušnej sekcie, kde môžete použiť ďalšie filtre. Ak sa nič nenašlo, pri zúženom rozsahu môžete zvoliť **Hľadať všade**. Chyba spojenia zobrazí správu **Vyhľadávanie je dočasne nedostupné** a tlačidlo **Skúsiť znova**.

Dialóg zavriete klávesom **Esc**, tlačidlom zatvorenia alebo kliknutím mimo neho. Fokus sa vráti na tlačidlo v hlavičke; hľadaný výraz sa do tlačidla neprepisuje.

## Pokročilé vyhľadávanie

Sekcia **Vyhľadávanie** poskytuje tabuľky pre webové stránky, súbory a textové kľúče. Otvoríte ju odkazom **Otvoriť výsledky v sekcii…** z príslušného rozsahu rýchleho vyhľadávania. Zachová hľadaný výraz a zvolí zodpovedajúcu kartu.

![](search.png)

## Kľúčové funkcie:
1. **Fulltextové vyhľadávanie**:
   - Vyhľadávanie prebieha nielen v titulkoch stránok, ale aj v ich textovom obsahu.
   - Používateľ môže nájsť obsah aj na základe kľúčových slov, ktoré sa nachádzajú v texte dokumentov alebo súborov.

2. **Filtrovanie výsledkov**:
   - Okrem fulltextového vyhľadávania je možné využiť stĺpcové filtre pre presnejšie zobrazenie výsledkov.
   - Tabuľky umožňujú filtrovanie na základe stĺpcov, ako sú `Názov web stránky`, `Meno autora` alebo `Kľúč`.

3. **Podpora viacerých typov obsahu**:
   - Webové stránky
   - Súbory
   - Textové kľúče

## Používanie sekcie Vyhľadávanie

### Rozhranie Vyhľadávania:

Sekcia je rozdelená do niekoľkých kariet. Každá karta umožňuje špecifické vyhľadávanie a filtrovanie:

#### 1. Web stránky
- **Fulltextové vyhľadávanie:** Zadajte kľúčové slovo, ktoré sa nachádza v texte stránky alebo jej titulku.
- **Obmedzenie podľa stromovej štruktúry** ![](tree-filter.png ":no-zoom") Môžete obmedziť vyhľadávanie výberom priečinka v ktorom sa bude vyhľadávať (hľadá sa aj v pod priečinkoch).
- **Filtrovanie:** Použite stĺpce ako `Názov web stránky` a `Meno autora` na zúženie výsledkov.
- **Práva používateľov:** Zobrazené výsledky závisia od práv jednotlivých používateľov. Používateľ vidí iba tie stránky, na ktoré má oprávnenie.

#### 2. Súbory
- **Fulltextové vyhľadávanie:** Vyhľadávajte text, ktorý sa nachádza v obsahu súborov, ako aj v názvoch súborov.
- **Filtrovanie:** Stĺpce ako `Názov súboru` a `URL adresa` pomáhajú presne špecifikovať výsledky.
- **Symbol oka:** Kliknutím na ikonu oka ![](icon-eye.png ":no-zoom") pri názve súboru si môžete súbor zobraziť.
- **URL adresa:** Odkazy ![](link.png ":no-zoom") umožňujú rýchlu navigáciu do adresára v Prieskumníkovi.

!> Upozornenie: na vyhľadávanie v súboroch sa používa [plno textový index súborov](../../files/fbrowser/folder-settings/README.md#indexovanie), nájdené sú teda len súbory, ktoré sú indexované.

#### 3. Textové kľúče
- **Fulltextové vyhľadávanie:** Nájdite textové kľúče podľa obsahu vo všetkých dostupných jazykoch.
- **Filtrovanie:** Používajte stĺpce, ako `Kľúč` alebo `Jazyk`, na presnejšie vyhľadávanie.

## Príklady použitia:
- **Vyhľadávanie vo web stránkach:**
  - Hľadáte frázu `naštartoval obchodnú stránku`.
  - Výsledky obsahujú stránky, kde sa táto fráza nachádza v texte. Pre zúženie výsledkov použijete filter `sales` v stĺpci pre `Názov stránky`.

- **Vyhľadávanie v súboroch:**
  - Hľadáte `only for users in Bankari group`.
  - Výsledky obsahujú všetky súbory, kde sa táto fráza nachádza v texte.

- **Vyhľadávanie textových kľúčov:**
  - Hľadáte `Pridať adresár`.
  - Výsledky obsahujú kľúče so slovenským textom `Pridať adresár`. Následne môžete použiť filter `addGroup` v stĺpci `Kľúč` na zúženie výsledkov.

