# Definice hotového - Definition of Done

Tento dokument je ve stavu psaní, není hotov, zatím se jedná spíše o poznámky.

## Code review

- za komentujte všechny debugovací `console.log` z JavaScript souborů
- zamyslete se, zda přidaná proměnná/`property` má smysluplný název (jednoznačně lze určit co reprezentuje)
- všechny texty musí být načteny z překladových `properties` souborů, v kódu nesmí být žádný text (zobrazující se v UI) natvrdo

## Testy

Kód je třeba pokrýt automatizovanými testy. Testy musí být opakovatelně spustitelné, je třeba je ověřit vícenásobným testováním. Pozor je třeba dát na asynchronnost provádění a korektní volání waitFor.

Manuálně je třeba provést testy exportu a importu dat datatabulky:
- Export dat do Excel formátu.
- Import dat jako nové záznamy – dojde k duplikování záznamů.
- Import dat s aktualizací podle sloupce se jménem (ne podle ID). Před importem upravte některé údaje a po importu s párováním na základě jména (ne ID) ověřte, že se nastavily na původní hodnoty.
- Přepněte si doménu a ověřte zobrazení údajů v jiné doméně. Většina aplikací má oddělená data podle domény. Naimportujte Excel jako nové záznamy a ověřte, že údaje se nacházejí v původní i změněné doméně. Upravte některý údaj v Excelu a proveďte import podle jména a ověřte, že ke změně údajů došlo pouze v nové doméně a ne iv původní z níž byl Excel (ověří se tak korektní párování na základě jména s výběrem aktuální domény).

## Dokumentace

**Doplnění dokumentace se dobře provádí** během code review v **merge requeste**. Proto si vždy připravte `merge request` a následně v dopise `Changes` přejděte všechny změněné soubory. **Při každé změně se zamyslete**, zda je z kódu srozumitelná a zda nemodifikuje aktuální chování, nebo nedoplňuje vlastnosti stávajícího kódu. Všímejte si:

Při každé změně se zamyslete, zda vám je jasný důvod této změny a zda vám bude jasný io rok a hlavně, zda bude jasný jinému programátorovi. Nesrozumitelné změny je nutné komentovat přímo ve zdrojovém kódu - dokumentace a komentáře ve zdrojovém kódu jdou ruku v ruce spolu a mohou být na první pohled duplicitní. To ale nevadí, dokumentaci pište více slovně, komentáře více technicky.

!>**Upozornění:** přístup ke zdrojovému kódu nemusí mít všichni vývojáři, dokumentace je dostupná i pro klienty, kteří mají zájem programovat vlastnosti pro WebJET. Dokumentace musí být natolik obsáhlá, aby umožňovala použít danou komponentu i bez zdrojových kódů.

Doporučuji vám mít **otevřené okno vyhledávání** v `docs` složky, a když vidíte v `Changes` listu změnu v nějaké proměnné/konfiguraci/objektu, tak ji vyhledáte v dokumentaci (složku ./docs). Pokud ji naleznete, text doplníte o změnu, pokud nenajdete, doplníte celý popis změny.

Při psaní dokumentace dávejte pozor na používané [termíny/výrazy](terms.md).

### Nová funkčnost

Vytvořte MD soubor ve vhodné sekci (nebo rozšiřte stávající) a popište jak funkčnost funguje, jaké jsou myšlenkové pochody. Dokument by měl mít následující strukturu:
- co je cílem nové komponenty - popište v jednom/dvou odstavcích k čemu komponenta slouží
- možnosti konfigurace/nastavení - pokud má komponenta možnosti konfigurace/nastavení uveďte seznam konfiguračních proměnných s popisem
- příklady použití - praktické ukázky použití, ideální s odkazem na soubor kde je komponenta reálně použita
- implementační detaily - detaily implementace, popište hlavní metody/funkce, jaký je jejich význam, uveďte odkazy do zdrojových souborů

### Změna stávající funkčnosti

- rozšiřuje se konfigurace (např. přidáno nové option v JS frameworku) - doplňte stávající dokumentaci

### Changelog

Po sepsání dokumentace ji `pushnite` a znovu přejděte seznam změn v `gitlabe`. Sepište hlavní body do souboru [CHANGELOG.md](../../CHANGELOG.md) s odkazy na podrobnější dokumentaci. V souboru changelog zachovejte následující strukturu:
- nadpis ROK.TÝDEN
- nadpis modulu + základní popis, za nadpisem uveďte číslo tiketu
- obecné - do sekce uveďte všeobecné/drobné změny netýkající se přímo změny v modulu, vždy na začátku uveďte číslo tiketu
- dokumentace - uveďte změny/nové části v dokumentaci s odkazy

Po doplnění changelogu zkontrolujte soubor [ROADMAP.md](../../ROADMAP.md), pokud jste implementovali některou část z roadmap-y označte ji za vyřešenou. Doplňte také číslo tiketu na konec řádku ve změně.
