# Definice slova Hotovo

Tento dokument je ve fázi psaní, není dokončen, zatím se jedná spíše o poznámky.

## Přezkum kódu

- pro komentář všech ladění `console.log` ze souborů JavaScript
- přemýšlet o tom, zda přidaná proměnná/`property` má smysluplný název (je možné jasně určit, co představuje).
- všechny texty musí být převzaty z překladu. `properties` nesmí být v kódu (zobrazeném v uživatelském rozhraní) pevně zadán žádný text.

## Testy

Kód musí být pokryt automatizovanými testy. Testy musí být opakovatelné, musí být ověřeny vícenásobným testováním. Je třeba dbát na asynchronní provádění a správné volání waitFor.

Ručně je třeba provést testy exportu a importu datových souborů:
- Export dat do formátu Excel.
- Importovat data jako nové záznamy - dojde k duplikaci záznamů.
- Import dat s aktualizací podle sloupce s názvem (nikoli podle ID). Před importem upravte některá data a po importu s porovnáním podle názvu (nikoli ID) ověřte, zda byla nastavena na původní hodnoty.
- Přepněte doménu a ověřte, zda se data zobrazují v jiné doméně. Většina aplikací má data oddělená podle domén. Importujte aplikaci Excel jako nové záznamy a ověřte, zda jsou data v původní i změněné doméně. Upravte některá data v Excelu a proveďte import podle názvu a ověřte, že ke změně dat došlo pouze v nové doméně a nikoli také v původní doméně, ze které byl Excel (tím se ověří správné přiřazení na základě názvu s výběrem aktuální domény).

## Dokumentace

**Dokončení dokumentace je dobře provedeno** během revize kódu v **požadavek na sloučení**. Proto se vždy připravte `merge request` a následně v dopise `Changes` procházet všechny změněné soubory. **Při každé změně myslete na to, že** zda je srozumitelný z kódu a zda mění skutečné chování nebo přidává funkce do stávajícího kódu. Upozornění:

Při každé změně se zamyslete nad tím, zda je vám důvod změny jasný a zda vám bude jasný i za rok, a zejména zda bude jasný i jinému programátorovi. Nesrozumitelné změny by měly být okomentovány přímo ve zdrojovém kódu - dokumentace a komentáře zdrojového kódu jdou ruku v ruce a mohou být na první pohled duplicitní. Ale to nevadí, dokumentaci pište spíše slovně, komentáře spíše technicky.

**Varování:** ne všichni vývojáři potřebují mít přístup ke zdrojovému kódu, dokumentace je k dispozici také klientům, kteří mají zájem o programování funkcí pro WebJET. Dokumentace musí být dostatečně obsáhlá, aby bylo možné komponentu používat i bez zdrojového kódu.

Doporučuji, abyste si **otevřít vyhledávací okno** v `docs` a když se ve složce `Changes` změnu v proměnné/konfiguraci/objektu, najdete ji v dokumentaci (složka ./docs). Pokud ji najdete, přidáte změnu do textu, pokud ji nenajdete, přidáte úplný popis změny.

Při psaní dokumentace věnujte pozornost použitým [termíny/výrazy](terms.md).

### Nové funkce

Vytvořte soubor MD ve vhodné sekci (nebo rozšiřte stávající) a popište, jak funkce funguje, jaké jsou myšlenkové postupy. Dokument by měl mít následující strukturu:
- jaký je účel nové součásti - popište v jednom/dvou odstavcích, k čemu daná součást slouží.
- konfigurační možnosti/nastavení - pokud má komponenta konfigurační možnosti/nastavení, uveďte seznam konfiguračních proměnných s popisem.
- příklady použití - praktické příklady použití, ideálně s odkazem na soubor, kde je komponenta skutečně použita.
- implementační detaily - implementační detaily, popis hlavních metod/funkcí, jejich význam, odkazy na zdrojové soubory.

### Změna stávajících funkcí

- konfigurace je rozšířena (např. přidána nová možnost v rámci JS) - přidat do stávající dokumentace

### Seznam změn

Po napsání dokumentace `pushnite` a projděte seznam změn v `gitlabe`. Zapište hlavní body do souboru [CHANGELOG.md](../../CHANGELOG.md) s odkazy na podrobnější dokumentaci. V souboru se seznamem změn zachovejte následující strukturu:
- záhlaví YEAR.WEEK
- Název modulu + základní popis, za názvem uveďte číslo tipu
- obecné - v sekci uveďte obecné/drobné změny, které přímo nesouvisí se změnou v modulu, na začátku vždy uveďte číslo tiketu.
- dokumentace - uveďte prosím seznam změn/nových částí dokumentace s odkazy.

Po přidání seznamu změn zkontrolujte soubor [ROADMAP.md](../../ROADMAP.md) pokud jste implementovali některou část plánu, označte ji jako vyřešenou. Na konec řádku ve změně přidejte také číslo tiketu.
