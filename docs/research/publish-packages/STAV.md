# Stav návrhu a realizácie

## Návrh

- Dátum vzniku: 1. november 2026. Posledné doplnenie: 3. november 2026.
- Produktové rozhodnutia Q1–Q19: uzatvorené; Q10/Q13 používateľ delegoval podľa jednoduchosti implementácie. Pri poslednej kontrole potvrdil náhľad zvoleného výsledku, vytvorenie súborového adresára bez rekurzívnych operácií, galériu iba na čítanie a náhľad na admin hostiteľovi.
- Technický návrh: po opakovanej kontrole a realizácii technického kroku **P01**, bez ďalšej otvorenej produktovej otázky. [Výsledok kontroly](09-kontrola-pripravenosti.md) eviduje opravené medzery a predpoklady, ktoré musí dokázať reálna implementácia. Pripravenosť dokumentácie neznamená vykonané DB/browser testy ani automatické povolenie P02–P08.
- Doplnenie N1: povinné `NewsApp`/`NewsActionBean`, podrobný návrh [03a](03a-novinky.md), perex skupiny existujúcich definícií, čas náhľadu a porovnanie s výsledkom po merge. Aktualizované etapy a test T16/N01–N12. Vykonaná nezávislá kontrola voči zdrojom; nepredstavuje runtime overenie.
- Doplnenie N2: [budúce moduly](08-rozsirovanie-modulov.md), porovnanie architektúr a odporúčanie typované jadro + jedna spoločná JSON tabuľka rozšírení. Dnešný plán pripravuje spoločný kontrakt; samotný storage/moduly sú neskoršie E01–E03. Používateľov úzky bannerový rozsah výslovne vynecháva úpravy štatistík. Overené zdrojové cesty banneru a galérie, bez runtime testov.
- Spresnenie UI: [05](05-rozhranie.md) obsahuje postup vytvorenia a aktivácie balíka, formulár, rozbalený prepínač pod doménou a pravidlá stavov/záložiek. Doplnené create/list API v 03 a príslušné overenia. Pokračovanie návrhu prebieha vo worktree `webjetcms-feature-58858-publishing-packages` na vetve `feature/58858-publishing-packages`.
- Doplnenie N3: skrátené a zjednotené názvy tabuliek a Java tried. Triedy/rozhrania používajú prefix `Branch` v jadrovom balíku `sk.iway.iwcm.branch`; zjednotené sú aj kontextové polia, hlavičky, konfigurácia, REST a adresáre. Pomenovanie tabuliek je podľa [dátového modelu](02-datovy-model.md#odporúčanie). Mapované tabuľky zachovajú presný produkčný názov s prefixom `branch_`; aktualizované sú aj SQL príklady, etapy a názvy pomocných úložísk.
- Doplnenie N4: povinný `menu_ul_li.jsp`/`MenuULLI` podľa [03b](03b-menu.md), vrátane editoru parametrov `MenuApp`, rekurzie, aktívnej cesty, perex/HTML, premenných a cache. T03 je rozpracované na M01–M11 v P01/P03/P04/P06/P08. Overené zdrojové integračné body a existujúce testy; bez runtime skúšok.
- Doplnenie N5: záväzná [priorita jednoduchého čitateľného POC](ORCHESTRACIA.md#záväzná-priorita-jednoduchý-a-čitateľný-poc). Konkrétne služby bez povinného handler/DI registra, bez hypotetických frameworkov a zriedkavých prevádzkových scenárov. Prvé overenie na jednom reálnom DB profile/inštancii; ďalšie profily až pred deklarovaním podpory. Ručný POC míľnik po P06, dohodnuté plánovanie zostáva P07. Obnova koša sa runtime overí v P06. Izolácia, ID, oprávnenia a náhľad sa nemenia.
- Návod na spustenie: [orchestrácia](ORCHESTRACIA.md) doplnená o modely/roly, etapový postup a pokračovanie po prerušení. Odporúčanie GPT-6.1 Sol / High nemení aktuálne nastavenie klienta; skutočné modely zaznamená implementačný agent. Hlavný agent môže použiť najviac dvoch pomocníkov naraz, bez vlastného agentového frameworku. Žiadna etapa sa týmto doplnením nespustila.
- Implementácia: používateľ 3. novembra 2026 zadal **iba P01**. P02–P08 nie sú povolené.
- P01: **DONE ako technický experiment na jednorazovom InnoDB profile**. Dokázané ID/reštart, SQL, core izolácia a minimálny reálny JSP/thumbnail gateway; zvlášť LIVE browser regresia pracovného webu. Nejde o hotovú editáciu/náhľad z P02/P03. Pracovná DB s MyISAM zatiaľ nie je vhodná na rezervácie.
- Pred P01 prešlo dokumentačné overenie pôvodných 18 Markdown dokumentov. Aktuálne výsledky, skúšobné Java testy a medzery overenia sú evidované nižšie a v [P01-vysledky.md](P01-vysledky.md). Zmeny zostávajú bez commitov.

## Kroky

| Krok | Stav | Výsledok |
| --- | --- | --- |
| P01 | DONE | ID/SQL/core izolácia a minimálny JSP/thumbnail gateway vrátane reálneho admin-host prekliku prešli na uvedenom InnoDB profile. |
| P02 | NOT_STARTED | — |
| P03 | NOT_STARTED | — |
| P04 | NOT_STARTED | — |
| P05 | NOT_STARTED | — |
| P06 | NOT_STARTED | — |
| P07 | NOT_STARTED | — |
| P08 | NOT_STARTED | — |

Prípustné evidenčné stavy krokov: `NOT_STARTED`, `IN_PROGRESS`, `PARTIAL`, `DONE`, `BLOCKED`. Ide iba o stav práce agentov, nie o databázové stavy publikačnej vetvy. Pri BLOCKED sa zapíše konkrétna prekážka a čo ju odstráni.

## Výsledky krokov

### P01 — DONE (technické overenie na uvedenom profile)

- **Vetva/východisko:** `feature/58858-publishing-packages`, commit `d3f1d9b65b06ed3fe46115426327084dc6a64a9d`; pôvodný worktree čistý. Všetky zmeny bez commitov. Bez produkčného nasadenia alebo aktivácie; P02–P08 ostávajú NOT_STARTED.
- **POC profil:** MariaDB **12.2.2**, InnoDB, MariaDB Connector/J **3.5.10**, Java 17.0.18, jedna samostatná zapisovacia DB `webjet_p01` na loopback porte 33316. CI schéma, existujúce startup aktualizácie a označené fixture. Jednorazové dáta `/tmp/webjet-p01-db/data`; žiadna replika/failover ani iná DB nie je overená.
- **Pracovné prostredie od používateľa:** `poolman-local.xml`/Debug Local DB, `webjetcms_autotest_web` na 3306, `http://iwcm.interway.sk:8086`, existujúci tester login. Používateľ dovolil testovacie úpravy obsahu. Reálne `aceintegration/jet` JSP existujú; pôvodná kontrola zástupných default JSP bola neúplná. LIVE browser regresia prešla, pracovná DB sa nereštartovala.
- **Dôležité obmedzenie pracovnej DB:** groups, pkey_generator, _adminlog_ a perex_group_doc sú MyISAM. Preto nie je vhodná na rezervácie; žiadny engine sa nekonvertoval. Technický group riadok 121670 z neúspešnej izolačnej skúšky bol cielene odstránený; vlastné technické skupiny/stránky potom 0. Test teraz kontroluje engine pred INSERTom. Celý finálny DB/JSP dôkaz prešiel na samostatnej InnoDB DB, nie na tomto zmiešanom profile.
- **ID:** rezervácia documents 94/96, groups 68/70, súbežný natívny insert, rollback pracovného insertu a odmietnutie duplicity. Korektný reštart jednorazovej DB a nová JVM zachovali rezervácie; vyššie ID vložené pred nižším. Len JDBC časť T01, nie médiá/editor/merge alebo crash/power-loss.
- **SQL Novinky:** reálny query builder + skúšobný CTE pred filtrami/count/paging; nové/zmenené/zmazané/presunuté položky, tokenové perex priradenie, oba perexGroupUseJoin režimy a atribútový OR bez duplicít. `[2,6,1,5]`, count 4, stránky `[2,6]`/`[1,5]`; 287 000 znakov HTML zachovaných. Verejné NewsQuery API/render ostáva na zapojenie v P03.
- **Core guard:** DBPool pri branchesEnabled vynucuje a overuje fyzické READ_COMMITTED; pri chybe abort/close a Hikari eviction. Flag je štandardne vypnutý a v Constants nemá doménový override, keďže chráni spoločné generátory. Flag-off legacy správanie zachované. Prešli izolačné unit testy vrátane skutočného Hikari poolu, reálne pooled JDBC/streaming helpery, DocDB/GroupsDB cache, NewsQuery a EclipseLink repository; JPA datasource REPEATABLE-READ spĺňa aspoň READ_COMMITTED. Bežné čítanie používateľa/štatistiky fungovalo.
- **Gateway dôkaz:** embedded Tomcat/Jasper s nezmeneným menu_ul_li.jsp/MenuULLI, aceintegration jet/blank.jsp/WriteTag a reálnym ThumbServlet. Pridanie/premenovanie/presun/default preklik, dve vetvy a LIVE, odlišné bajty obrázka a pracovný 16×16 thumbnail, root-relative CSS/JS HTTP, no-store a odmietnutie nepovolených rout prešli.
- **Admin-host dôkaz:** dočasný testovací JSP vstup na skutočnom `iwcm.interway.sk:8086` pri každom requeste overil existujúcu admin session cez UsersDB. Hosť bol odmietnutý (302 na login). Browser otvoril WORK cez reálny admin odkaz, preklikol default stránku a načítal pracovný obrázok 64×64/thumbnail 16×16 aj CSS/JS na tom istom hostiteľovi. Bez CSRF/XSS výnimiek; rozmery thumbnailu sú obyčajné top-level w/h parametre. Tento vstup, private descriptor a markery test odstránil vo finally.
- **Hranica prototypu:** vnútorná autentifikácia embedded fixture, obsahové fasády a URL adaptéry sú skúšobné; vonkajší admin vstup používa reálne prihlásenie. Logická doména je pevná trusted fixture. Vnútorný request dodáva existujúci docid kontrakt. Testovací loopback proxy nie je budúcou prevádzkovou architektúrou: produkčný BranchFilter/PathFilter, doménový kontext, úplný WORK layout a izolácia spoločných cache zostávajú P03. Celé T01/T03/T07/T10 sa nepovažujú za hotové.
- **Kôš/identita:** obnova stránky potrebuje úplný historický snapshot; záväzný dokument 04 spresnený. Obnova podstromu sa runtime overí v P06. Štyri typy jadra vystačia s enum + DTO obalom a konkrétnymi službami; žiadny register, JSON storage alebo E01–E03 nevznikli.
- **Súbory/API:** malý centrálny guard v DBPool + globálny flag scope v Constants; skúšobné BranchIdPocTest, BranchCoreReadPocTest, BranchNewsSqlPocTest, BranchPreviewPocTest; DBPoolIsolationTest/BranchFlagScopeTest; Codecept webpages/branch-p01.js. Test-only Tomcat/Jasper závislosti nejdú do WAR. Žiadne branch migrácie, úložisko, alokátor ako verejné API ani produkčný gateway.
- **Overenie:** finálny spoločný Gradle beh a jednotlivé reštartové fázy; Záverečný spoločný beh: 97 Java invokácií, 90 úspešných/0 chybných/7 AVIF preskočených. Samostatný admin-host handoff Java test prešiel a Codecept má 2 úspešné scenáre (LIVE + WORK pod reálnou session). Detaily, presné príkazy, počty a preskočené AVIF podmienky sú v [P01-vysledky.md](P01-vysledky.md#reprodukcia-výsledky-a-dôkazy). Dôkazy `build/test-results/p01/final/`, `build/test-results/p01-preview/`, `build/test/branch-p01-live-menu.png` a `branch-p01-admin-work.png`. compileTestJava, Markdown linky/kotvy a git diff --check prešli.
- **Modely/delegovanie:** hlavný implementátor a dvaja pomocníci s rovnakým zdedeným modelom; presný identifikátor/reasoning neoverené. Výsledky aj celý diff skontrolované a integrované; finálne behy sekvenčne.
- **Upratanie:** zastavené iba agentom spustené WebJET/P01 DB procesy; pracovná MariaDB 3306 ostala bežať. Jednorazové dáta zachované v /tmp; dočasný ignorovaný Poolman profil odstránený.
- **Zostávajúci odhad:** P02–P06 ručný POC **40–69 vývojárskych dní**, P07–P08 **14–22**, spolu **54–91**, QA 15–25 dní osobitne. Menu rezerva 1–2 dni v P03; explicitná spoločná identita 0,5–1 deň zahrnutá v P02/P06. Ďalšie DB, zákaznícke adaptéry a konverzia MyISAM mimo odhadu. Nejde o termín alebo meranie výkonu agentov.
- **Vstup pre ďalšiu etapu:** P01 splnený pre vyššie uvedený InnoDB profil/minimálnu šablónu. P02 vyžaduje samostatné zadanie a musí zaviesť engine kontrolu, schému, konečné ID, kontext a maticu stĺpcov. Pracovná MyISAM DB ani ďalší profil sa nesmie automaticky aktivovať.

Budúce E01/E02/E03 ostávajú samostatné nezadané návrhy podľa 08. Prvý výsledok PARTIAL sa po doplnení prostredia a chýbajúcich dôkazov uzatvára týmto DONE v uvedenom obmedzenom rozsahu.
