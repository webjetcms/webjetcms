# P02 — úložisko, konečné ID a základ kontextu

Dátum: 4. november 2026. Stav: **DONE v rozsahu P02 na profile MariaDB 12.2.2 / InnoDB / jedna zapisovacia inštancia**. P03 sa nezačal. Funkcia zostáva predvolene vypnutá; redakčné UI, obsahové adaptéry, gateway, upload a publisher ešte nie sú implementované.

## Východisko a overenie P01

- Vetva `feature/58858-publishing-packages`, vstupný commit `439613efa` (`feat: P01`), čistý pracovný strom. Používateľ už medzi etapami commitol P01; jeho kód ani experimenty sa nenahrádzali. Zmeny P02 zostávajú necommitnuté.
- Prečítané `STAV.md`, `P01-vysledky.md`, orchestrácia a vstupy P02. Aktuálny kód potvrdil centrálny izolačný guard v DBPool a globálny scope flagu; žiadne branch produkčné triedy ani tabuľky pred P02 neboli implementované.
- Zachovaná jednorazová DB z P01 sa znovu spustila z `/tmp/webjet-p01-db/data`, na `127.0.0.1:33316`, schéma `webjet_p01`. Zistená MariaDB **12.2.2**, Connector/J **3.5.10**, Java **17.0.18**, Gradle **8.14**. Všetkých deväť dotknutých pôvodných tabuliek malo InnoDB. Reálnych **77/31/23** stĺpcov documents/groups/media zodpovedá odovzdanej matici.
- P01 core izolačná skúška, DBPool unit testy, scope flagu a News SQL experiment sa znova vykonali a prešli. P01 JSP experiment nebol prerobený na produkčný gateway.
- Pracovný testovací server `http://iwcm.interway.sk:8086` používal existujúci `poolman-local.xml` a DB na 3306 iba na flag-off regresiu. Host je v `/etc/hosts` aj systémovom resolveri mapovaný na `127.0.0.1`. Automatická kontrola browser beh dvakrát odmietla; po výslovnom povolení používateľa pre tento host oba scenáre prešli.
- Na pracovnom profile sa pri bežnom štarte aplikovala prídavná migrácia piatich tabuliek. Pôvodné MyISAM tabuľky sa nekonvertovali, vetvy sa nezapli a pracovná DB sa nereštartovala. Skutočný engine pracovného profilu preto naďalej vylučuje aktiváciu.
- Hlavný agent implementoval schému, ID, JDBC služby a integráciu. Dvaja pomocníci so zdedeným modelom pripravili kontext/API a polia/snapshoty/B-L-W; následne nezávisle skontrolovali integráciu. Presný identifikátor modelu a reasoning klienta: **neoverené**. Nebol použitý odlišný model ani agentový framework. Zapisovacie Java overenia bežali sekvenčne.

## Implementácia a platné API

Zdrojový balík: [sk.iway.iwcm.branch](../../../src/main/java/sk/iway/iwcm/branch/BranchStore.java).

| Oblasť | Zavedená zmluva |
| --- | --- |
| Metadata balíka | `BranchEntity`, `BranchRepository`, `BranchService.create(domainName,name,description,userId)`, `list(domainName,includeClosed)`, `find(id,domainName)`. Vytvorenie prázdneho DRAFT, revízia 0, bez termínu a bez snapshotu webu. |
| Typované riadky | `BranchRow` + `BranchDocumentEntity`, `BranchGroupEntity`, `BranchMediaEntity`, `BranchFileEntity` a ich štyri repositories. Samostatné entity, vypnutá zdieľaná JPA cache. Každý typ číta výhradne svoju pracovnú tabuľku. Spring a JPA scan sú zaregistrované. |
| Vnútorný JDBC writer vetvy | `BranchStore.create(context,type,values)` rezervuje a potvrdí WORK; `put(context,type,id,values,expectedHash,delete)` zachová prvý BASE aj row_id a odmietne zastaraný edit. Metódy majú package prístup: ide o kontrakt pre dôveryhodné služby/fixture, nie autorizovaný editor. |
| Súborová fixture | `BranchStore.putFile(...)`, `BranchFilePaths.normalize/key`. FILE metadáta a DIRECTORY marker sú v rovnakej tabuľke, adresár nemá bajty/veľkosť/MIME. P02 file fixture je create-only. Nevytvára súbor ani adresár v LIVE filesystéme. |
| Spoločné porovnanie | `BranchType` (DOCUMENT/GROUP/MEDIA/FILE), `BranchChange`, `BranchSnapshot`, `BranchDecision.resolve(...)`; `BranchStore.change(type,row)` vytvorí obal z konkrétneho úložiska. Stabilná identita je typ + obsahové ID alebo normalizovaná logická cesta. Bez handler registra a bez branch_items. |
| Mapovanie | `BranchFields` a tri explicitné mapové mappery: `snapshot`, `project`, `initialValues`. [Úplná matica stĺpcov](P02-matica-stlpcov.md) určuje WORK, odvodené, prevádzkové, mimo rozsahu a nové hodnoty. |
| Natívne ID | `BranchStore.create` vykoná technický INSERT → generated key → DELETE → WORK INSERT → COMMIT na jednom JDBC spojení. Návrat až po commite, kladný int; žiadne MAX+1 ani potvrdený obsahový zástupný riadok. |
| ID médií | `PkeyGenerator.getNextValueAsLongStrict("media")` používa rovnaký singleton, bean a bloky ako legacy/WJGen API. Zlyhanie DB, mapovania, maxima, bootstrapu či chýbajúci riadok vyvolá výnimku. Bootstrap iba zvýši čítač; UPDATE musí zmeniť jeden riadok pred čítaním LAST_INSERT_ID. Pri zlyhaní následného WORK insertu ostáva medzera. |
| Explicitný LIVE INSERT | `BranchRowWriter.insertDocumentWithId`, `insertGroupWithId`, `insertMediaWithId(connection,liveContext,values,categoryIds)`. Vyžaduje explicitný LIVE a otvorenú transakciu volajúceho. Pri duplicite SQL chyba, nikdy upsert; kategórie jedného média na rovnakom spojení. Publisher/audit/história tu ešte nie sú. |
| Kontext | Nemenný `BranchContext(branchId,domainName,userId,mode)` s LIVE/ADMIN/PREVIEW. `BranchContexts.current`, `open`, `live`, `requireSameSelection`; request atribút a existujúci RequestBean, vnorený scope obnoví pôvodný stav aj pri výnimke. Mimo requestu LIVE. Žiadny nový ThreadLocal register. |

Nový objekt možno ďalším `put` upraviť aj bez LIVE riadka. DELETE existujúcej stránky/priečinka vytvára porovnávací stav TRASH, média/súboru ABSENT. DELETE nového objektu sa odmietne; P04/P05 pridajú odobratie pracovnej položky. Zmazanie v P02 nepresúva žiadny LIVE obsah do koša.

`base_snapshot` je verzovaný kanonický JSON vrátane explicitného ACTIVE/TRASH/ABSENT. Hashe ignorujú poradie properties, transient polia, počítadlá, synchronizačné a individuálne plánovacie hodnoty. Čísla/dátumy majú jednotnú reprezentáciu, perex skupiny aj kategórie média sú zoradená množina. Dlhé HTML sa neorezáva. Hash po uložení sa odvodzuje z vráteného riadka: DATETIME na tomto profile má sekundovú presnosť.

B/L/W pravidlá: W=B alebo L=W znamená LIVE/no-op; L=B znamená WORK; obojstranne odlišná zmena je konflikt s WORK náhľadom a zákazom zápisu. Ručná voľba sa overí vstupnými hashmi pred no-op normalizáciou; neplatná voľba vyvolá `StaleChoiceException`. `resolved_snapshot` nenahrádza typované WORK.

### REST

- `GET /admin/rest/branches?includeClosed=false`: Summary `id,name,domainName,state`; default vynechá MERGED/CANCELLED.
- `POST /admin/rest/branches`: `{ "name": "Kampaň", "description": "...", "expectedDomainName": "www.example.sk" }` → **201**, Summary a Location. Výber session sa nemení.
- Server určí doménu z existujúceho admin výberu; query parameter neurčuje vlastníka. Overí admina, nové právo `menuBranches` a dostupnú doménu cez existujúce GroupsDB práva. POST zachová CSRF pravidlá a existujúcu API-token výnimku.
- Vypnutá funkcia 404 na aplikačnej hranici, nedostatočné práva/doména/CSRF 403, neplatný názov 400, nezhoda expectedDomainName alebo nepripravenosť 409 s dôvodom. Spring security môže neautorizovaný request odmietnuť skôr.
- Modulové právo je defaultDisabled, podmienené flagom a bez odkazu na neexistujúce UI. Preklady SK/CZ/EN/DE sú doplnené. UI prepínač, session selection endpointy a preview-link nie sú v P02.

### Migrácia a aktivácia

[autoupdate-webjet9.xml](../../../src/main/webapp/WEB-INF/sql/autoupdate-webjet9.xml) obsahuje nový UpdateDBBean `04.11.2026 [codex] Publishing branches P02 - MariaDB InnoDB POC storage`. Vytvorí **iba päť** InnoDB tabuliek: branches, branch_documents, branch_groups, branch_media, branch_files. Má scope/identity indexy, FK na balík a SHA-256 path_key namiesto indexu dlhej cesty. Nekopíruje web ani nemení pôvodné PK. DDL je idempotentné pre opakovanie čiastočne aplikovanej migrácie.

Aktuálne je implementovaný a overený iba mysql variant na MariaDB **12.2**. MySQL ani iné DB/verzie sa zatiaľ nesmú aktivovať; readiness ich odmietne. Ďalšie varianty DDL a profily sú ďalšou samostatnou validačnou prácou.

Globálne konfiguračné kľúče, bez doménového override:

| Kľúč | Predvolená hodnota | Význam |
| --- | --- | --- |
| `branchesEnabled` | false | Zapne centrálny izolačný guard P01. P02 neobsahuje používateľskú aktiváciu editácie/náhľadu. |
| `branchSingleWriterConfirmed` | false | Výslovné prevádzkové potvrdenie jednej zapisovacej DB a rovnakého nastavenia všetkých obsahových uzlov. Nevytvára automatickú kontrolu cluster konfigurácií. |
| `branchStorageRoot` | prázdny | Existujúci absolútny private filesystem adresár oddelený od webrootu aj po rozlíšení symlinkov. Pripravenosť skúsi zápis a odstránenie temp súboru. |

`BranchReadiness.verify` overuje profil, writable/aspoň READ_COMMITTED, fyzický InnoDB všetkých transakčných tabuliek vrátane histórií, nakonfigurovaného auditu a perex_group_doc, klasifikáciu aktuálnej schémy, media generátor a private storage. Volá sa pred pracovným zápisom. `verifyActivation` navyše vykoná rollback DML probes vrátane explicitného obsahového ID a preverí izoláciu priameho datasource JPA. Úplnú kontrolu volá BranchService pri vytvorení balíka, na jeho JDBC spojení. InitServlet ju nevolá: nesplnené predpoklady voliteľnej admin funkcie nesmú zablokovať štart servera ani zobrazovanie bežného webu. Chyba odmietne iba operáciu vetvy (create API vráti 409). Nenahrádza oprávnenia k jednotlivým obsahovým položkám.

Chýbajúci riadok generátora media je konkrétna chyba pripravenosti. P01 fixture ho pôvodne nemala: test ho inicializoval v jednorazovej DB rovnako pomenovaným štandardným LIVE generátorom (media/media_id), nie novým čítačom vetiev. P02 migrácia neresetuje ani automaticky nevytvára obsahové generátory na nasadeniach.

## Overenia a dôkazy

| Overenie | Výsledok |
| --- | --- |
| Záverečný spoločný Gradle beh | **56/56**, 0 chybných, 0 preskočených. 21 DBPool, 2 flag scope, 1 pooled/JPA/cache core, 1 flag-off/on visibility, 4 News SQL, 6 mapovanie, 5 B/L/W, 3 kontext, 3 prístup, 3 REST, 6 storage, 1 strict generátor. |
| Úprava po review: nezávislý štart servera | Odstránené volanie z InitServlet, úplný preflight presunutý do vytvorenia balíka. Cielený opakovaný beh BranchStorageTest + BranchRestControllerTest: **9/9**, vrátane odmietnutých predpokladov a REST 409. Bez nového štartu webového servera; InitServlet má nulový diff oproti východisku P01. |
| Dve reštartové fázy | **1/1 + 1/1** v samostatných JVM s korektným reštartom iba DB 33316 medzi nimi. |
| Migrácie | Reálny XML update na existujúcej P01 DB; rovnaké XML SQL na prázdnej `webjet_p02_clean` a opakovane, presne 5 tabuliek. Aplikácia s vypnutým flagom aplikovala update aj na pracovnej DB. Legacy `updateDB` vracia false aj po úspechu; test kontroluje skutočnú schému a záznam v _db_, nie tento návrat. |
| JDBC storage | Dve vetvy a rovnaké LIVE ID, zachovaný prvý BASE/row_id, odmietnuté zastarané LIVE/WORK, nový objekt opakovane upravený bez LIVE, 315 KB HTML, perex tokeny, médium/kategórie, TRASH obal, DIRECTORY a FILE metadáta. LIVE obsah/história/index kategórií sa pracovným zápisom nezmenili. |
| Skutočné JPA | V9JpaDBConfig a päť repositories: save, flush/commit, clear a reload typovaných riadkov/metadát. Testové @Entity fixture z nesúvisiacich testov sú zo scanu testového procesu vylúčené; produkčný scan ostáva rovnaký. |
| Chyby a pripravenosť | Zlyhaný WORK insert dokumentu aj média, následná alokácia, chýbajúci generátor aj s cache, nedostupná DB, nulový atomic UPDATE, neznížený bootstrap, iný engine, chýbajúce single-writer potvrdenie, verejný/chýbajúci storage a skutočný SELECT-only DB účet. Všetko odmietnuté. |
| Flag-off browser | **2/2** existujúce Codecept scenáre: prihlásenie/menu/assety/thumbnail a strom/zoznam/filter/otvorenie existujúceho editora. Nejde o branch editor ani o WORK render. |
| Kontrola zdrojov | compileJava/compileTestJava počas Gradle behu, git diff --check, lokálne Markdown odkazy a kotvy. Nezávislá kontrola ID/izolácie integrácie; opravené zistenia. |

Pre test odmietnutia enginu sa iba v jednorazovej DB dočasne zmenil engine media_group_to_media na MyISAM a v finally späť na InnoDB. Nejde o konverziu pracovného nasadenia. Dočasný SELECT-only účet sa odstránil. Obsahové a branch fixture sa čistia; trvalé čítače sa neznižujú a medzery v ID zostávajú. Záverečná kontrola našla 0 balíkov a 0 riadkov reštartovej fixture; media čítač ostal 81. Agentom spustený WebJET 8086 a DB 33316 boli zastavené, pracovná DB 3306 ostala bežať. Ignorovaný poolman-local-p01.xml bol odstránený; dáta DB, čistá schéma a prázdny private test adresár ostali pre reprodukciu.

Reštartový dôkaz:

| Typ | WORK nižšie/vyššie | LIVE generátor medzi nimi | Prvé nové ID po reštarte | Explicitný INSERT po reštarte |
| --- | --- | --- | --- | --- |
| DOCUMENT | 900041 / 900043 | 900042 | 900044 | 900043 → 900041 |
| GROUP | 900034 / 900036 | 900035 | 900037 | 900036 → 900034 |
| MEDIA | 32 / 42 | 33 | 52 | 42 → 32 |

Explicitné inserty po reštarte test rollbackol. Overil aj odmietnutie duplicity. Nie je to produkčný merge ani crash/power-loss/failover test. Veľké dokumentové/priečinkové ID pochádzajú zo zachovaných fixture P01, nie z nového generátora.

Dôkazy: `build/test-results/p02/final/`, `/before/`, `/after/`, `/admin-readiness/`; logy `/tmp/webjet-p02-{final,before,after,browser,admin,server}.log`. Browser používa existujúci `build/test/branch-p01-live-menu.png`. Neobsahujú nový produkčný endpoint na náhľad.

### Reprodukcia

Najskôr spusti zachovanú jednorazovú MariaDB podľa P01 a vytvor ignorovaný profil poolman-local-p01.xml s oboma datasources na `jdbc:mariadb://127.0.0.1:33316/webjet_p01`. Java 17; private test root `/tmp/webjet-p02-private`. Žiadne prihlasovacie údaje sa nemajú commitnúť.

```sh
WEBJET_P02_DB=true WEBJET_P01_CORE=true \
WEBJET_P01_JDBC_URL=jdbc:mariadb://127.0.0.1:33316/webjet_p01 \
./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchStorageTest' \
  --tests 'sk.iway.iwcm.branch.BranchReservationTest.strictMediaFailureAndMonotonicBootstrap' \
  --tests 'sk.iway.iwcm.branch.BranchMappingTest' \
  --tests 'sk.iway.iwcm.branch.BranchDecisionTest' \
  --tests 'sk.iway.iwcm.branch.BranchContextsTest' \
  --tests 'sk.iway.iwcm.branch.BranchAccessTest' \
  --tests 'sk.iway.iwcm.branch.BranchRestControllerTest' \
  --tests 'sk.iway.iwcm.branch.BranchFlagScopeTest' \
  --tests 'sk.iway.iwcm.DBPoolIsolationTest' \
  --tests 'sk.iway.iwcm.branch.BranchCoreReadPocTest' \
  --tests 'sk.iway.iwcm.branch.BranchIdPocTest.coreIsolationGuardSeparatesFlagOffAndOn' \
  --tests 'sk.iway.iwcm.branch.BranchNewsSqlPocTest'

WEBJET_P02_DB=true WEBJET_P02_RESTART=before ./gradlew --offline test \
  -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchReservationTest.beforeDatabaseRestart'
# Korektne vypni a znovu spusti iba jednorazovú DB 33316; potom ihneď:
WEBJET_P02_DB=true WEBJET_P02_RESTART=after ./gradlew --offline test \
  -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchReservationTest.afterDatabaseRestart'
```

Na bežiacom lokálnom pracovnom serveri, z `src/test/webapp`, so štandardným testovacím prihlásením:

```sh
CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
npx codeceptjs run tests/webpages/branch-p01.js --steps
CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
npx codeceptjs run tests/webpages/webpage-spring.js --grep 'Zoznamu web stranok' --steps
```

## Hranice a vstup pre P03

1. **Použi existujúcu maticu a čisté B/L/W.** P03 musí z mapových projekcií vytvoriť oddelené DocDetails/GroupDetails, rovnaké detail/SQL/menu/News výsledky a mapu rozhodnutí. Runtime rozpoznanie LIVE koša vrátane predkov ešte treba zapojiť; explicitný stav TRASH a jeho hash už existujú. Obnova koša ostáva P06.
2. **Zapoj filter až v P03.** BranchContexts je pripravený a testovaný, ale žiadny verejný request ani starý editor zatiaľ neprechádza do WORK iba podľa session. P03 dodá session výber, hlavičky, kontrolu starých záložiek, reálny filter chain, trusted doménu, gateway a čistenie kontextu. Riadiace/technické operácie musia zostať explicitne LIVE.
3. **Read-only hodnoty vždy z dnešného LIVE.** Mapper project to vyžaduje; u nového objektu z initialValues. data_asc/file_name/root_group výpočet dodá P03/P04 bez vykonania živého JSP alebo fulltext hooku. Metadata zo SQL listu nesmú uložiť DATA_NOT_LOADED ako HTML.
4. **Médiá:** group_ids_snapshot je autoritatívne WORK členstvo. WORK last_update sa odvodí zo zvolenej vlastníckej stránky iba pri skutočnej zmene; LIVE/no-op zachová cutoff. Výpis/filter/count a media/file existence helpery sú P03. RowWriter s kategóriami je iba nízka zapisovacia operácia pre budúci P06.
5. **Storage API nie je bezpečný editor.** P04/P05 musia pred interným writerom doplniť existujúce obsahové/adresárové/publikačné práva, doménu cieľa, validáciu referencií/URL/cyklov, zákaz zmien nepodporovaných polí a odobratie novej položky. Teraz je writer package-private a nie je vystavený REST endpointom.
6. **Súbory:** P02 nemá bajty, upload, produkčné mkdir ani delete. P05 použije serverom odvodený private priestor podľa row_id a atómové nahradenie WORK. Fixture prijíma iba dekódované cesty pod /images a /files; odmieta percentá, query/fragment, backslash, riadiace znaky a únik z koreňa. P05 musí doplniť existujúce storage mapovanie, práva a backendové pravidlá názvov/case sensitivity; žiadna univerzálna filesystem podpora sa týmto nesľubuje. Galéria je read-only.
7. **Podporený profil je úzky.** MariaDB 12.2.2, jediná lokálna zapisovacia inštancia a jednotné prostredie; iné DB, repliky, failover, IwcmFsDB a viac JVM časových zón neboli overené. Pracovná MyISAM DB nie je pripravená na aktiváciu.
8. **Žiadny production merge/scheduler.** Explicitný INSERT testuje technický zápis, nie históriu/audit/cache/obnovu/závislosti. P06/P07 ich musia doplniť. Kompletné T01, T02 a T04 sa preto nevykazujú ako hotové pre všetky etapy; P02 uzatvára ich ID/úložiskovú časť a T14 v tomto rozsahu.

Ďalšie odhady sa nemenia na základe rýchlosti agenta: zostávajú P03–P06 približne **32–56 vývojárskych dní**, P07–P08 **14–22**; samostatné QA podľa P01. Ručný použiteľný POC je až P06. Produkčné zapnutie nie je týmto výsledkom povolené.
