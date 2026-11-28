# P01 — technické overenie

Dátum: 3. november 2026. Stav: **DONE pre uvedený jednorazový InnoDB profil a minimálnu aceintegration šablónu**. Ide o technický dôkaz podľa hranice P01, nie o použiteľnú editáciu alebo hotový produkčný náhľad. P02 sa nezačal.

Prvý beh skončil PARTIAL s DB/SQL dôkazom. Po potvrdení pracovného prostredia používateľom sa doplnil core izolačný guard, skúšobný gateway s reálnym JSP a thumbnailom a browser regresia. Predchádzajúci záver o neprítomnosti šablón sa opravuje: zástupné sú iba `default` JSP; reálne `aceintegration/jet` JSP existujú.

## Profil a prostredie

- Worktree `webjetcms-feature-58858-publishing-packages`, vetva `feature/58858-publishing-packages`, východiskový commit `d3f1d9b65b06ed3fe46115426327084dc6a64a9d`; vstupný pracovný strom bol čistý. Bez commitov, produkčnej migrácie, nasadenia alebo zapnutia funkcie.
- **Zvolený POC profil:** MariaDB **12.2.2**, MariaDB Connector/J **3.5.10**, InnoDB, jedna samostatná zapisovacia inštancia. Java Temurin 17.0.18, Gradle 8.14, Tomcat/Jasper 11.0.13, EclipseLink 5.0.0-B13. Repliky, failover a iné DB nie sú overené.
- Jednorazová DB `webjet_p01`, `jdbc:mariadb://127.0.0.1:33316/webjet_p01`, dáta `/tmp/webjet-p01-db/data`, socket `/tmp/webjet-p01-db/mysql.sock`. Repozitárová CI schéma `.github/workflows/blank_web_autotest.sql`, existujúce startup aktualizácie a tri označené testovacie default stránky/priečinky s ID `900001–900003`. Všetky finálne DB/SQL/embedded JSP skúšky prešli nad týmto profilom.
- **Samostatná LIVE regresia:** používateľ výslovne povolil pracovnú DB `webjetcms_autotest_web` cez existujúci `poolman-local.xml`/Debug Local DB a prípadnú úpravu obsahu. Server tohto worktree bežal na `http://iwcm.interway.sk:8086`, HTTPS 8446, debugger 5008. Prihlásenie existujúceho testera a reálny web boli overené CodeceptJS/Playwright. Nejde o produkčné nasadenie.
- **Šablóny:** embedded gateway používa nezmenené kópie reálnych `templates/aceintegration/jet/blank.jsp`, `menu_ul_li.jsp`, taglibov, `WriteTag` a skutočný `ThumbServlet`. LIVE browser navštívil existujúcu `/apps/menu/` na plnej aceintegration šablóne. Plný WORK overlay nad celým layoutom nie je týmto overený.
- CodeceptJS, Playwright, admin npm závislosti aj `dist` sú dostupné. Docker daemon nie je dostupný; jednorazová DB používa nainštalované MariaDB binárky.
- Hlavný agent implementoval a integroval výsledok. Dvaja pomocníci zdedili rovnaký model: jeden čítacie cesty a JSP experiment, druhý ID skúšku a DBPool guard. Presný identifikátor modelu/reasoning z klienta nie je overený. Výsledky a celý diff boli nezávisle skontrolované; finálne testy bežali sekvenčne.

## Dôležité zistenie o pracovnej DB

Pracovná DB na porte 3306 **nespĺňa InnoDB podmienku rezervácií**:

| Tabuľka | Engine |
| --- | --- |
| documents, documents_history, groups_scheduler, media, media_group_to_media | InnoDB |
| groups, pkey_generator, _adminlog_, perex_group_doc | MyISAM |

Skúška potvrdila dôsledok: technický riadok `groups` bol viditeľný aj pri READ_COMMITTED a rollback ho neodstránil. Vlastný riadok `121670` s presným názvom `autotest P01 technical group` bol následne cielene odstránený; kontrola našla **0 vlastných technických priečinkov aj stránok**. Žiadny engine sa nekonvertoval, databáza na 3306 sa nereštartovala. Test teraz preveruje engine ešte pred INSERTom a netransakčné tabuľky odmietne.

Táto DB poslúžila na povolenú LIVE browser regresiu, nie ako podporený profil rezervácií. P02 musí pred aktiváciou kontrolovať všetky tabuľky zapisované transakciou podľa `02a`; úspech na jednorazovej InnoDB DB túto podmienku pracovnej DB neodstraňuje. Prípadná konverzia pracovného nasadenia vyžaduje osobitné rozhodnutie a nie je súčasťou P01.

## Rezervácia ID — JDBC časť T01

[BranchIdPocTest](../../../src/test/java/sk/iway/iwcm/branch/BranchIdPocTest.java) používa skutočné tabuľky `documents`/`groups` a skúšobnú InnoDB tabuľku `p01_id_reservations`, nie P02 úložisko.

`beforeDatabaseRestart` prešiel:

| Tabuľka | Rezervované nižšie/vyššie ID | Súbežný bežný insert | Rollbacknutá rezervácia | Ďalší bežný insert |
| --- | --- | --- | --- | --- |
| documents | 94 / 96 | 95 | 97 | 98 |
| groups | 68 / 70 | 69 | 71 | 72 |

Jedno spojenie vykonalo technický INSERT → `getGeneratedKeys()` → DELETE → INSERT pracovnej rezervácie → COMMIT. ID sa vrátilo až po commite. Technické obsahové riadky nezostali potvrdené. Pozastavený insert druhé spojenie pri READ_COMMITTED nevidelo, pozitívna kontrola READ_UNCOMMITTED ho videla. Vynútená chyba pracovného insertu skončila rollbackom a výnimkou; ďalšia alokácia fungovala.

Po korektnom vypnutí a novom spustení **iba jednorazovej DB** a v novom testovacom JVM prešiel `afterDatabaseRestart`:

| Tabuľka | Prvý natívny insert po reštarte | Explicitný insert vyššie → nižšie | Ďalší natívny insert |
| --- | --- | --- | --- |
| documents | 100 | 96 → 94 | 101 |
| groups | 73 | 70 → 68 | 74 |

Rezervácie sa načítali z DB, explicitná duplicita bola odmietnutá a generátor pokračoval bez kolízie. Ide o normálny reštart, nie crash/power-loss test. Test používa konzervatívnu podmienku zníženého Uptime; druhú fázu treba spustiť bezprostredne po reštarte. Médiá, PkeyGenerator, editor a výsledný merge zostávajú P02/P06.

## Core izolačný guard

[DBPool](../../../src/main/java/sk/iway/iwcm/DBPool.java) má jednu centrálnu podmienku: pri `branchesEnabled=true` oba izolačné helpery vynútia READ_COMMITTED. Flag sa štandardne nezapína; bez hodnoty ostáva false. Nevzniklo branch úložisko, kontext ani aktivačné UI.

- Izolácia sa overuje na fyzickom JDBC spojení, keďže Hikari proxy môže vracať uloženú hodnotu aj po ignorovanom driver setteri.
- Chyba overenia/nastavenia vyhodí výnimku, abortne a uzavrie spojenie. Checkout navyše vyradí známy Hikari záznam; pri chybe inicializácie už spojenie neunikne. Ostatné pooly/drivery sa tým nepovažujú za podporené.
- `branchesEnabled` je globálny aj pri zapnutom `constantsAliasSearch`; doménový override nemôže ochranu vypnúť ani samostatne zapnúť. Dva `BranchFlagScopeTest` prípady overujú oba smery aj zachovanie bežných doménových konštánt.
- Flag-off zachová požadovanú legacy izoláciu, Oracle skip a best-effort spracovanie izolácie; pri chybe checkoutu sa získaný handle uzavrie.
- [DBPoolIsolationTest](../../../src/test/java/sk/iway/iwcm/DBPoolIsolationTest.java): **21 úspešných invokácií**, vrátane skutočného Hikari poolu nad mockovaným fyzickým driverom, ignorovaného setteru a výmeny poškodeného spojenia.
- `coreIsolationGuardSeparatesFlagOffAndOn`: reálna MariaDB potvrdila dirty read pri vypnutom flagu a skrytie technického riadka pri zapnutom.
- [BranchCoreReadPocTest](../../../src/test/java/sk/iway/iwcm/branch/BranchCoreReadPocTest.java): počas pozastaveného INSERTu stránky/priečinka ich nevidelo pooled JDBC, DocDB detail/metadátová cache, GroupsDB cache, NewsQuery count ani streaming helpery `ComplexQuery` a `StatNewDB`. Bežné čítanie používateľa/štatistickej tabuľky fungovalo. Skutočný EclipseLink `DocDetailsRepository.findById` a JPA SQL priečinkov tiež nič nevrátili. Priamy JPA datasource mal REPEATABLE-READ, teda aspoň READ_COMMITTED.

Inventár core Java/JSP/XML/properties potvrdil 15 priamych obsahových volaní dirty-read cesty z `02a` a oba streaming helpery. Nenašla sa ďalšia zmena izolácie mimo DBPool ani NOLOCK. To pokrýva tento overený core profil, nie zákaznícke priame SQL/externé datasources alebo všetky topológie.

## Účinný SQL zdroj Noviniek

[BranchNewsSqlPocTest](../../../src/test/java/sk/iway/iwcm/branch/BranchNewsSqlPocTest.java): **4 úspešné scenáre**, po dva pre `perexGroupUseJoin=false/true`.

- Scratch tabuľky kopírujú reálnu schému. Používa sa skutočný NewsQuery builder, DatabaseCriteria a projekcia FieldEnum + 12 polí DataAccessHelper. Parametre zdroja sa bindujú pred parametrami kritérií.
- Skúšobný MariaDB CTE zloží LIVE bez WORK prepisu a WORK UPSERT pred filtrami. Nová/zmenená stránka vstúpi do filtra; DELETE/presun von z filtra ju vyradí. Pripravená voľba LIVE a druhá vetva majú samostatný výsledok. Mapa B/L/W sa tu ešte nevypočítava.
- Pracovné `perex_group=1,2` filtruje reálne `DatabaseCriteria.tokenizedIds` bez zápisu produkčného perex indexu. Skúšobný plán odstráni nepotrebný perex join v oboch režimoch. Produkčný NewsQuery ešte opravený nie je.
- Zoznam `[2,6,1,5]`, stránky `[2,6]`/`[1,5]`, count 4. Radenie končí doc_id. Atribútový OR zasiahne viac riadkov tej istej stránky; GROUP BY doc_id deduplikuje pred vonkajším count/paging: `[2,1]`, count 2. Nová stránka bez potrebného LIVE atribútu sa nezaradí.
- HTML s diakritikou má **287 000 znakov**, UNION aj atribútový filter ho zachovajú. Bez DISTINCT nad celým obsahom. Na MariaDB ide o MEDIUMTEXT, nie dôkaz Oracle CLOB. `loadData=false` vynechá telo iba z výslednej projekcie. LIVE fixture zostala nezmenená.

CTE/count/deduplikácia sú skúšobné skladanie v teste, nie zapojené verejné list/count API alebo render Noviniek. List/count vznikajú s novým query objektom; oprava mutable `arguments`, obohatenia poradia, finálna matica read-only polí a zapojenie verejného API ostávajú P02/P03.

## JSP gateway a reálne LIVE overenie

[BranchPreviewPocTest](../../../src/test/java/sk/iway/iwcm/branch/BranchPreviewPocTest.java) prešiel cez embedded loopback Tomcat/Jasper, skutočný menu JSP/renderer, aceintegration blank.jsp/WriteTag a skutočný lokálny ThumbServlet:

- Pridaná položka/priečinok, premenovanie a presun existujúceho priečinka pod nový, preklik na jeho účinnú default stránku a otvorená menu cesta.
- LIVE a dve vetvy vracajú rozdielny obsah/odkazy; opakované zahriate načítanie LIVE zostáva rovnaké. Fixture používa oddelené obsahové fasády na request threade, nezapisuje produkčné singleton cache.
- Obrázok WORK má iné bajty než LIVE. Skutočný thumbnail vracia pracovné modré pixely a rozmery 16×16; LIVE obrázok ostáva červený. Galerijný zápis je v teste blokovaný, učenie povolených rozmerov vypnuté.
- Prepísané odkazy na root-relative CSS/JS sa skutočne načítajú cez gateway; malé explicitné fixture adaptéry zachovajú aj ich obrázkové URL. Nejde o univerzálny parser zákazníckeho JavaScriptu.
- No-store je zachované aj na thumbnaile. Test odmietne neautentifikovaný preview request, neznámu vetvu, interné/admin/spustiteľné cesty, traversal, historické riadiace parametre a POST.

Doplnený bol aj **reálny admin-host dôkaz**. Voliteľný `WEBJET_P01_ADMIN_HOST=true` na čas skúšky nainštaluje testovací JSP vstup z test/resources do lokálneho worktree. Každý request overuje skutočného administrátora cez UsersDB, povolí iba pevný fixture allowlist vetiev 42/43 a interne číta z určeného loopback prototypu. Privátny token zostáva vo WEB-INF. Hosť dostal 302 na prihlasovanie. Codecept s existujúcim testerom otvoril WORK cez skutočný admin odkaz, preklikol účinnú default stránku a dokázal dekódovanie pracovného 64×64 obrázka/16×16 thumbnailu aj CSS/JS resource requesty na `http://iwcm.interway.sk:8086`. Logická doména sa zobrazuje z pevnej trusted fixture `p01-content.example.invalid`.

Pôvodné Referer/CSRF a XSS kontroly zostali zapnuté: vstup sa otvára cez admin odkaz, thumbnail w/h sa prenášajú ako bežné parametre, nie ako vnorená query v target. Test odstránil JSP, privátny descriptor a synchronizačné markery vo finally. Toto je technická pomôcka v testovacom harness, nie navrhnutý produkčný proxy alebo dvojuzlové nasadenie.

**Hranica P01 prototypu:** autentifikácia vnútorného embedded fixture, obsahové fasády a URL adaptéry sú skúšobné; vonkajší admin vstup používa reálnu session. Verejná route nemá docid, vnútorný adaptér ho dodá do existujúceho MenuULLI kontraktu. Produkčný BranchFilter/PathFilter, doménový kontext, spoločné cache a plný WORK layout/head/combine/GDPR sú stále P03. Celé T03/T07/T10 a zákaznícke skripty sa nepovažujú za splnené.

Samostatný [CodeceptJS scenár](../../../src/test/webapp/tests/webpages/branch-p01.js) na skutočnom worktree s pracovnou DB overil existujúce admin prihlásenie, `/apps/menu/`/Anketa, reálnu aceintegration šablónu, logo, core thumbnail a HTTP 200 pre root-relative CSS/JS. Prešiel aj po reštarte servera s finálnym DBPool guardom a vypnutým flagom. LIVE baseline aj doplnený WORK admin-host preklik majú samostatné scenáre; nejde o hotový produkčný gateway.

## Kôš, identita a potrebné adaptéry

- `EditorService.recoverWebpageFromTrash` načíta najvyššie actual history_id, inak najvyššie vôbec, a uloží celý DocDetails z histórie. Nestačí group_id/available: DELETE potrebuje úplný obnoviteľný snapshot pred zmenou so správnym actual, ID, umiestnením a dostupnosťou. Spresnené v jedinom záväznom zdroji [04](04-merge-a-publikovanie.md#8-krátke-zápisy-a-pokračovanie-po-chybe).
- GroupsService obnovuje rodiča koreňa z najnovšieho neplánovaného groups_scheduler, dostupnosť stránok z actual histórie/prípadne najnovšej podľa saveDate. Zachovať vnorenie aj group_id stránok; presunúť iba koreň. Bežný hard delete pri editorDisableHistory=true sa nepoužije. Runtime zmazanie/obnova zostáva P06.
- `document/group/media + konečné ID`, `file + normalizovaná logická cesta/path_key` postačí pre štyri typy jadra. BranchChange bude DTO pohľad, nie druhý zápis. Stačí enum a konkrétne služby; handler register, JSON storage, E01–E03 a druhý merge nevznikli.
- P03 musí zapojiť logickú URL/doménu pred PathFilter, samostatné fasády a súbory/thumbnail, perex/default odkazy, srcset, redirecty a parametre News paging. MenuULLI bez vnútorného docid adaptéra dnes môže zahodiť platný request.docDetails; potrebuje opravu. Samotný ShowdocAction.showDocData nemení menu/URL mapu. Plný aceintegration layout a vlastné dynamické aplikácie potrebujú ďalšie overenie, nie domnelú podporu z minimálnej šablóny.

## Reprodukcia, výsledky a dôkazy

Jednorazová inštancia bola vytvorená `mariadb-install-db --no-defaults --datadir=/tmp/webjet-p01-db/data --auth-root-authentication-method=normal --skip-test-db`; štart používa `mariadbd --no-defaults --datadir=/tmp/webjet-p01-db/data --socket=/tmp/webjet-p01-db/mysql.sock --port=33316 --bind-address=127.0.0.1 --pid-file=/tmp/webjet-p01-db/mysql.pid --log-error=/tmp/webjet-p01-db/server.log`. DB webjet_p01 dostala CI SQL cez vlastný socket. Pri presnej reprodukcii startup aktualizácií sa použil ignorovaný poolman-local-p01.xml vytvorený z docker MariaDB profilu s uvedenou loopback URL pre oba datasources. Žiadne prihlasovacie údaje sa neukladajú do návrhu.

ID fázy vyžadujú **čerstvú jednorazovú DB vrátane neprítomnosti p01_* scratch tabuliek**; samotný opakovaný CI import ich neodstráni. Nespúšťať obe fázy jedným wildcard výberom. Existujúce rezervácie pri pokračovaní nemaž:

```sh
WEBJET_P01_JDBC_URL=jdbc:mariadb://127.0.0.1:33316/webjet_p01 ./gradlew --offline test --tests 'sk.iway.iwcm.branch.BranchIdPocTest.beforeDatabaseRestart'
# Korektne reštartovať iba túto jednorazovú DB; bezprostredne v novej test JVM:
WEBJET_P01_JDBC_URL=jdbc:mariadb://127.0.0.1:33316/webjet_p01 ./gradlew --offline test --tests 'sk.iway.iwcm.branch.BranchIdPocTest.afterDatabaseRestart'
```

Pre gateway musí zdroj obsahovať tri dostupné rôzne default stránky. Použité fixture ID 900001–900003 sú iba v jednorazovej DB. Finálny spoločný beh:

```sh
WEBJET_P01_PREVIEW=true WEBJET_P01_CORE=true WEBJET_P01_JDBC_URL=jdbc:mariadb://127.0.0.1:33316/webjet_p01 ./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml --tests 'sk.iway.iwcm.branch.BranchPreviewPocTest' --tests 'sk.iway.iwcm.branch.BranchCoreReadPocTest' --tests 'sk.iway.iwcm.branch.BranchNewsSqlPocTest' --tests 'sk.iway.iwcm.branch.BranchIdPocTest.coreIsolationGuardSeparatesFlagOffAndOn' --tests 'sk.iway.iwcm.DBPoolIsolationTest' --tests 'sk.iway.iwcm.branch.BranchFlagScopeTest' --tests 'sk.iway.iwcm.editor.ThumbServletFallbackTest' --tests 'sk.iway.iwcm.editor.ThumbServletIsSizeAllowedTest' --tests 'sk.iway.iwcm.editor.rest.PreviewControllerTest'
```

**97 invokácií: 90 úspešných, 0 chybných, 7 preskočených.** Všetkých 30 nových vybraných P01 kontrol prešlo; zvyšok sú existujúce súvisiace regresie. Sedem AVIF resize prípadov preskočil existujúci predpoklad dostupnosti ImageMagick; nejde o overenú AVIF podporu. Obe samostatné reštartové ID fázy predtým prešli po jednej. Samostatný beh bez všetkých opt-in env prešiel: 2 globálne flag testy úspešné, 5 bežných DB/JSP metód preskočených, parameterizované DB scenáre deaktivované; žiadny dočasný vstup nevznikol. testImplementation Tomcat/Jasper/EL/ecj nevstupujú do aplikačného WAR.

LIVE browser z `src/test/webapp`:

```sh
CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false npx codeceptjs run tests/webpages/branch-p01.js --steps
```

Výsledok základného behu: **1 úspešný LIVE scenár**. S doplneným admin-host handoffom prešli **2 scenáre** a samostatný Java preview test s handoffom (1/1). Server sa spustil cez `webjetDbname=/poolman-local.xml ./gradlew --offline -PhttpPort=8086 -PhttpsPort=8446 -PdebugPort=5008 appStart`; prihlasovanie používa existujúce lokálne testovacie prostredie.

Pre opakovanie admin-host dôkazu najprv spusti lokálny pracovný server ako vyššie. V druhom termináli pridaj `WEBJET_P01_ADMIN_HOST=true` k samostatnému BranchPreviewPocTest (s WEBJET_P01_PREVIEW=true a uvedeným jednorazovým JDBC URL). Po vzniku `build/test-results/p01-preview/admin-host-ready` do 120 sekúnd spusti Codecept s `WEBJET_P01_ADMIN_HOST=true`. Zapíš exit výsledok ako PASS/FAIL do `build/test-results/p01-preview/admin-host-complete`; Java test výsledok overí a všetky dočasné vstupy odstráni. Screenshoty a environment.txt obsahujú dôkaz úspechu. Dočasné JSP nie je súčasťou aplikácie/WAR.

Dôkazy sú lokálne v `build/test-results/p01/final/` (JUnit XML a summary.txt), `build/test-results/p01-preview/` (LIVE/obe WORK HTML, default preklik, work-thumb.png, profil a admin-host PASS), `build/test/branch-p01-live-menu.png` , `build/test/branch-p01-admin-work.png` a v Codecept reportoch. Admin-host log/XML sú zvlášť v `build/test-results/p01/admin-host/`. Reporty sa ďalším Gradle/Codecept behom môžu prepísať. Staršie ID logy ostali v `build/test-results/p01/before/` a `after/`. `compileTestJava`, linky/kotvy zmenených MD a `git diff --check` prešli.

Po overení boli zastavené iba agentom spustené WebJET/P01 DB procesy; pracovná MariaDB na 3306 zostala bežať. Jednorazové dáta sú zachované v /tmp, dočasný ignorovaný Poolman profil odstránený. Zmeny zostávajú necommitnuté.

## Spresnený odhad a vstup do ďalšej etapy

P01 je na uvedenom profile dokončený. Zostávajúca vývojárska práca, nie termín ani čas agentov:

| Rozsah | Zostávajúci odhad | Predpoklad |
| --- | --- | --- |
| P02–P06, ručný POC | **40–69 dní** | P02 8–13, P03 8–14, P04 8–14, P05 8–14, P06 8–14; jeden InnoDB profil/inštancia. |
| P07–P08 | **14–22 dní** | Plánovanie, regresia a pilot konkrétneho podporovaného prostredia. |
| **Zostáva spolu** | **54–91 dní** | Bez ďalších DB, zákazníckych adaptérov a konverzie MyISAM. QA 15–25 dní osobitne. |

Oproti pôvodnému P03 zostáva rezerva 1–2 dni za potvrdené rekurzívne menu/default/cache integračné miesta a plný WORK layout. SQL projekcia/ID na zvolenom profile sú uskutočniteľné; veľká práca zostáva v save vedľajších účinkoch a čítaní. Spoločná identita/obal podľa 08 nepridáva etapu: 0,5–1 dňa explicitného mapovania je zahrnutý v P02/P06. Pôvodný PARTIAL odhad 2–4 dni na dokončenie P01 sa týmto výsledkom uzatvára, nejde o meranie rýchlosti vývojára.

Pre P02 je pripravený technický dôkaz a malý DBPool guard; nie schéma, alokátor ako verejné API, kontext, finálna matica polí ani produkčný gateway. Jediným podporeným POC predpokladom je uvedený InnoDB profil. P02 potrebuje samostatné zadanie používateľa.
