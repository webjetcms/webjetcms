# Stav návrhu a realizácie

## Návrh

- Dátum vzniku: 1. november 2026. Posledné doplnenie: 3. november 2026.
- Produktové rozhodnutia Q1–Q19: uzatvorené; Q10/Q13 používateľ delegoval podľa jednoduchosti implementácie. Pri poslednej kontrole potvrdil náhľad zvoleného výsledku, vytvorenie súborového adresára bez rekurzívnych operácií, galériu iba na čítanie a náhľad na admin hostiteľovi.
- Technický návrh: po opakovanej kontrole pripravený na zadanie **P01**, bez ďalšej otvorenej produktovej otázky. [Výsledok kontroly](09-kontrola-pripravenosti.md) eviduje opravené medzery a predpoklady, ktoré musí dokázať reálna implementácia. Pripravenosť dokumentácie neznamená vykonané DB/browser testy ani automatické povolenie P02–P08.
- Doplnenie N1: povinné `NewsApp`/`NewsActionBean`, podrobný návrh [03a](03a-novinky.md), perex skupiny existujúcich definícií, čas náhľadu a porovnanie s výsledkom po merge. Aktualizované etapy a test T16/N01–N12. Vykonaná nezávislá kontrola voči zdrojom; nepredstavuje runtime overenie.
- Doplnenie N2: [budúce moduly](08-rozsirovanie-modulov.md), porovnanie architektúr a odporúčanie typované jadro + jedna spoločná JSON tabuľka rozšírení. Dnešný plán pripravuje spoločný kontrakt; samotný storage/moduly sú neskoršie E01–E03. Používateľov úzky bannerový rozsah výslovne vynecháva úpravy štatistík. Overené zdrojové cesty banneru a galérie, bez runtime testov.
- Spresnenie UI: [05](05-rozhranie.md) obsahuje postup vytvorenia a aktivácie balíka, formulár, rozbalený prepínač pod doménou a pravidlá stavov/záložiek. Doplnené create/list API v 03 a príslušné overenia. Pokračovanie návrhu prebieha vo worktree `webjetcms-feature-58858-publishing-packages` na vetve `feature/58858-publishing-packages`.
- Doplnenie N3: skrátené a zjednotené názvy tabuliek a Java tried. Triedy/rozhrania používajú prefix `Branch` v jadrovom balíku `sk.iway.iwcm.branch`; zjednotené sú aj kontextové polia, hlavičky, konfigurácia, REST a adresáre. Pomenovanie tabuliek je podľa [dátového modelu](02-datovy-model.md#odporúčanie). Mapované tabuľky zachovajú presný produkčný názov s prefixom `branch_`; aktualizované sú aj SQL príklady, etapy a názvy pomocných úložísk.
- Doplnenie N4: povinný `menu_ul_li.jsp`/`MenuULLI` podľa [03b](03b-menu.md), vrátane editoru parametrov `MenuApp`, rekurzie, aktívnej cesty, perex/HTML, premenných a cache. T03 je rozpracované na M01–M11 v P01/P03/P04/P06/P08. Overené zdrojové integračné body a existujúce testy; bez runtime skúšok.
- Doplnenie N5: záväzná [priorita jednoduchého čitateľného POC](ORCHESTRACIA.md#záväzná-priorita-jednoduchý-a-čitateľný-poc). Konkrétne služby bez povinného handler/DI registra, bez hypotetických frameworkov a zriedkavých prevádzkových scenárov. Prvé overenie na jednom reálnom DB profile/inštancii; ďalšie profily až pred deklarovaním podpory. Ručný POC míľnik po P06, dohodnuté plánovanie zostáva P07. Obnova koša sa runtime overí v P06. Izolácia, ID, oprávnenia a náhľad sa nemenia.
- Implementácia: **nespustená**, aktuálne zadanie povoľuje iba Markdown návrhy.
- Skutočné runtime/DB/browser testy tejto funkcie: **nevykonané**; triedy, tabuľky a testy popísané v návrhu ešte neexistujú.
- Dokumentačné overenie: 18 Markdown dokumentov, kontrola existencie lokálnych odkazov a ich Markdown kotiev, párovania blokov kódu a koncových whitespace; bez zistených chýb. `git diff --check` prešiel. Git zmeny sú výlučne návrhové Markdown súbory v tomto adresári, bez commitov. Zjednotený rozsah jadra, budúcich modulov, bannerových štatistík a etapového zadávania. Implementačné kroky zostávajú nespustené.

## Kroky

| Krok | Stav | Výsledok |
| --- | --- | --- |
| P01 | NOT_STARTED | — |
| P02 | NOT_STARTED | — |
| P03 | NOT_STARTED | — |
| P04 | NOT_STARTED | — |
| P05 | NOT_STARTED | — |
| P06 | NOT_STARTED | — |
| P07 | NOT_STARTED | — |
| P08 | NOT_STARTED | — |

Prípustné evidenčné stavy krokov: `NOT_STARTED`, `IN_PROGRESS`, `PARTIAL`, `DONE`, `BLOCKED`. Ide iba o stav práce agentov, nie o databázové stavy publikačnej vetvy. Pri BLOCKED sa zapíše konkrétna prekážka a čo ju odstráni.

## Výsledky krokov

Zatiaľ žiadne. Budúci agent doplní výsledok podľa šablóny v [ORCHESTRACIA.md](ORCHESTRACIA.md).

POC profil zatiaľ nie je zvolený; prvý implementačný krok zapíše konkrétnu DB/verziu/driver a šablónu z dostupného testovacieho prostredia. `DONE` jednotlivých krokov sa vzťahuje na tento uvedený profil. Ostatné DB/nasadenia sa do vykonaného overenia nezapočítajú.

Budúce E01 (spoločný storage), E02 (základný banner) a E03 (úzka galéria) sú iba návrhy z dokumentu 08, zatiaľ nezadané na implementáciu. Ich budúca evidencia sa vedie oddelene od P01–P08.
