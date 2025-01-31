# Vyhľadávanie

## Prehľad
Sekcia **Vyhľadávanie** v administrácii umožňuje používateľom vyhľadávať obsah vo viacerých oblastiach, ako sú webové stránky, súbory a textové kľúče. Vyhľadávanie poskytuje nástroje na filtrovanie výsledkov a na zúženie rozsahu vyhľadávania, čo zlepšuje efektivitu práce s obsahom.

![](search.png)

## Prístup do sekcie:
Do sekcie **Vyhľadávanie** sa dostaneme v administrácii pomocou ikony lupy ![](../icon-search.png ":no-zoom") v hlavičke.

Pozrite tiež [Hlavička](../README#hlavička) pre ďalšie informácie.

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

