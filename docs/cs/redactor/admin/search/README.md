# Vyhledávání

## Přehled

Sekce **Vyhledávání** v administraci umožňuje uživatelům vyhledávat obsah ve více oblastech, jako jsou webové stránky, soubory a textové klíče. Vyhledávání poskytuje nástroje na filtrování výsledků a na zúžení rozsahu vyhledávání, což zlepšuje efektivitu práce s obsahem.

![](search.png)

## Přístup do sekce:

Do sekce **Vyhledávání** se dostaneme v administraci pomocí ikony lupy ![](../icon-search.png ":no-zoom") v hlavičce.

Podívejte také [Hlavička](../README#hlavička) pro další informace.

## Klíčové funkce:

1. **Fulltextové vyhledávání**:
    - Vyhledávání probíhá nejen v titulcích stránek, ale také v jejich textovém obsahu.
    - Uživatel může najít obsah také na základě klíčových slov, která se nacházejí v textu dokumentů nebo souborů.

2. **Filtrování výsledků**:
    - Kromě fulltextového vyhledávání lze využít sloupcových filtrů pro přesnější zobrazení výsledků.
    - Tabulky umožňují filtrování na základě sloupců, jako jsou `Názov web stránky`, `Meno autora` nebo `Kľúč`.

3. **Podpora více typů obsahu**:
    - Webové stránky
    - Soubory
    - Textové klíče

## Používání sekce Vyhledávání

### Rozhraní Vyhledávání:

Sekce je rozdělena do několika karet. Každá karta umožňuje specifické vyhledávání a filtrování:

#### 1. Web stránky

- **Fulltextové vyhledávání:** Zadejte klíčové slovo, které se nachází v textu stránky nebo její titulku.
- **Omezení podle stromové struktury** Můžete omezit vyhledávání výběrem složky ve které se bude vyhledávat (hledá se iv pod složkách).![](tree-filter.png ":no-zoom")Filtrování:
- **&#x20;Použijte sloupce jako&#x20;** &#x20;a `Názov web stránky` k zúžení výsledků.`Meno autora`Práva uživatelů:
- **&#x20;Zobrazené výsledky závisí na právech jednotlivých uživatelů. Uživatel vidí pouze ty stránky, ke kterým má oprávnění.** &#x32;. Soubory

#### Fulltextové vyhledávání:
- **&#x20;Vyhledávejte text, který se nachází v obsahu souborů i v názvech souborů.** &#x46;iltrování:
- **&#x20;Sloupce jako&#x20;** &#x20;a `Názov súboru` pomáhají přesně specifikovat výsledky.`URL adresa`Symbol oka:
- **&#x20;Klepnutím na ikonu oka&#x20;** &#x20;při názvu souboru si můžete soubor zobrazit.![](icon-eye.png ":no-zoom")URL adresa:
- **&#x20;Odkazy&#x20;** &#x20;umožňují rychlou navigaci do adresáře v Průzkumníkovi.![](link.png ":no-zoom")!> Upozornění: pro vyhledávání v souborech se používá&#x20;

plně textový index souborů[, nalezeny jsou tedy jen soubory, které jsou indexovány.](../../files/fbrowser/folder-settings/README.md#indexování) 3. Textové klíče

#### Fulltextové vyhledávání:
- **&#x20;Najděte textové klíče podle obsahu ve všech dostupných jazycích.** &#x46;iltrování:
- **&#x20;Používejte sloupce, jako&#x20;** &#x20;nebo `Kľúč`, pro přesnější vyhledávání.`Jazyk`Příklady použití:

## Vyhledávání ve web stránkách:
- **Hledáte frázi&#x20;**
  - .`naštartoval obchodnú stránku`Výsledky obsahují stránky, kde se tato fráze nachází v textu. Pro zúžení výsledků použijete filtr&#x20;
  - &#x20;ve sloupci pro `sales`.`Názov stránky`Vyhledávání v souborech:
- **Hledáte&#x20;**
  - .`only for users in Bankari group`Výsledky obsahují všechny soubory, kde se tato fráze nachází v textu.
  - Vyhledávání textových klíčů:
- **Hledáte&#x20;**
  - .`Pridať adresár`Výsledky obsahují klíče se slovenským textem&#x20;
  - . Následně můžete použít filtr `Pridať adresár` ve sloupci `addGroup` k zúžení výsledků.`Kľúč`
