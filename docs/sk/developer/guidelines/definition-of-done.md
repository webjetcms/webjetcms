# Definícia hotového - Definition of Done

Tento dokument je v stave písania, nie je hotový, zatiaľ sa jedná skôr o poznámky.

## Code review

- za komentujte všetky debug-ovacie ```console.log``` z JavaScript súborov
- zamyslite sa, či pridaná premenná/```property``` má zmysluplný názov (jednoznačne je možné určiť čo reprezentuje)
- všetky texty musia byť načítane z prekladových ```properties``` súborov, v kóde nesmie byť žiaden text (zobrazujúci sa v UI) natvrdo

## Testy

Kód je potrebné pokryť automatizovanými testami. Testy musia byť opakovateľne spustiteľné, je potrebné ich overiť viac násobným testovaním. Pozor je potrebné dať na asynchrónnosť vykonávania a korektné volania waitFor.

Manuálne je potrebné vykonať testy exportu a importu dát datatabuľky:

- Export dát do Excel formátu.
- Import dát ako nové záznamy - dôjde k duplikovaniu záznamov.
- Import dát s aktualizáciou podľa stĺpca s menom (nie podľa ID). Pred importom upravte niektoré údaje a po importe s párovaním na základe mena (nie ID) overte že sa nastavili na pôvodné hodnoty.
- Prepnite si doménu a overte zobrazenie údajov v inej doméne. Väčšina aplikácií má oddelené údaje podľa domény. Naimportujte Excel ako nové záznamy a overte, že údaje sa nachádzajú v pôvodnej aj zmenenej doméne. Upravte niektorý údaj v Excel-i a vykonajte import podľa mena a overte, že k zmene údajov došlo len v novej doméne a nie aj v pôvodnej z ktorej bol Excel (overí sa tak korektné párovanie na základe mena s výberom aktuálnej domény).

## Dokumentácia

**Doplnenie dokumentácie sa dobre robí** počas code review v **merge requeste**. Preto si vždy pripravte ```merge request``` a následne v liste ```Changes``` prejdite všetky zmenené súbory. **Pri každej zmene sa zamyslite**, či je z kódu zrozumiteľná a či nemodifikuje aktuálne správanie, alebo nedopĺňa vlastnosti existujúceho kódu. Všímajte si:

Pri každej zmene sa zamyslite, či vám je jasný dôvod tejto zmeny a či vám bude jasný aj o rok a hlavne, či bude jasný inému programátorovi. Nezrozumiteľné zmeny je nutné komentovať priamo v zdrojovom kóde - dokumentácia a komentáre v zdrojovom kóde idú ruka v ruke spolu a môžu byť na prvý pohľad duplicitné. To ale nevadí, dokumentáciu píšte viac slovne, komentáre viac technicky.

!>**Upozornenie:** prístup k zdrojovému kódu nemusia mať všetci vývojári, dokumentácia je dostupná aj pre klientov, ktorý majú záujem programovať vlastnosti pre WebJET. Dokumentácia musí byť natoľko obsiahla, aby umožňovala použiť danú komponentu aj bez zdrojových kódov.

Odporúčam vám mať **otvorené okno vyhľadávania** v ```docs``` priečinku, a keď vidíte v ```Changes``` liste zmenu v nejakej premennej/konfigurácii/objekte, tak ju vyhľadáte v dokumentácii (priečinok ./docs). Ak ju nájdete, text doplníte o zmenu, ak nenájdete, doplníte celý opis zmeny.

Pri písaní dokumentácie dávajte pozor na používané [termíny/výrazy](terms.md).

### Nová funkčnosť

Vytvorte MD súbor vo vhodnej sekcii (alebo rozšírte existujúci) a opíšte ako funkčnosť funguje, aké sú myšlienkové pochody. Dokument by mal mať nasledovnú štruktúru:

- čo je cieľom novej komponenty - opíšte v jednom/dvoch odstavcoch na čo komponenta slúži
- konfiguračné možnosti/nastavenia - ak má komponenta konfiguračné možnosti/nastavenia uveďte zoznam konfiguračných premenných s popisom
- príklady použitia - praktické ukážky použitia, ideálne s odkazom na súbor kde je komponenta reálne použitá
- implementačné detaily - detaily implementácie, popíšte hlavné metódy/funkcie, aký je ich význam, uveďte odkazy do zdrojových súborov

### Zmena existujúcej funkčnosti

- rozširuje sa konfigurácia (napr. pridané nové option v JS frameworku) - doplňte existujúcu dokumentáciu

### Changelog

Po spísaní dokumentácie ju ```pushnite``` a znova prejdite zoznam zmien v ```gitlabe```. Spíšte hlavné body do súboru [CHANGELOG.md](../../CHANGELOG.md) s odkazmi na podrobnejšiu dokumentáciu. V súbore changelog zachovajte nasledovnú štruktúru:

- nadpis ROK.TÝŽDEŇ
- nadpis modulu + základný popis, za nadpisom uveďte číslo tiketu
- všeobecné - do sekcie uveďte všeobecné/drobné zmeny netýkajúce sa priamo zmeny v module, vždy na začiatku uveďte číslo tiketu
- dokumentácia - uveďte zmeny/nové časti v dokumentácii s odkazmi

Po doplnení changelogu skontrolujte súbor [ROADMAP.md](../../ROADMAP.md), ak ste implementovali niektorú časť z roadmap-y označte ju za vyriešenú. Doplňte aj číslo tiketu na koniec riadku v zmene.
