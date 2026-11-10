# P08 — Regresia a pilot

**Stav: PARTIAL**, 10. november 2026. Pilot a nové regresie prešli; chýba úspešný pôvodný `apps/news.js` a úplná LIVE regresia schvaľovania/zrkadlenia/multigroup. Vetva `feature/58858-publishing-packages`, východisko `a3951964f` (P07 vrátane review cronu z 10. novembra). Rozsah končí P08; bez commitov a produkčného zapnutia.

## Prostredie a platnosť dôkazov

MariaDB **12.2.2/InnoDB**, Connector/J **3.5.10**, Java **17.0.18**, jedna inštancia `127.0.0.1:33316/webjet_p01`, lokálny filesystem, profil `poolman-local-p01.xml`, súkromné súbory `/tmp/webjet-p03-private`. WebJET používa HTTP 8086/HTTPS 8446. Pracovná DB 3306 sa nepoužíva. Predvolené `branchesEnabled` aj `branchSingleWriterConfirmed` v zdrojovom `ConstantsV9` zostávajú `false`; zapnutý je iba existujúci izolovaný POC profil.

`db-profile.txt` zachytáva skutočné InnoDB tabuľky a cron #14 s `cluster_node=all-admin`. Serverový predvolený isolation je `REPEATABLE-READ`; skutočné JDBC/JPA a dirty-read guardy sú osobitne overené testami. Existujúci migračný test použil aj pomocnú čistú schému `webjet_p02_clean` na tej istej izolovanej MariaDB 33316. Nešlo o ďalší podporovaný DB profil ani o pracovnú DB 3306.

Inventár: `/templates/aceintegration/jet/blank.jsp`, `MenuApp` + `menu_ul_li.jsp`/`MenuULLI`, `NewsApp` + `news-velocity.jsp`/`NewsActionBean` a fixture Velocity šablóny. Ďalej bežný editor, médiá, moderný/CKEditor upload a jednotlivé operácie elFinder podľa P05. Toto nepotvrdzuje kompatibilitu ľubovoľných šablón, PageBuilder layoutov, vlastného SQL/JPA, hookov alebo ďalších aplikácií.

Staršie výsledky sú konkrétne historické dôkazy, nie tvrdenie o novom spustení každého testu. V P08 sa zopakovala celá dostupná branch sada vrátane reálneho DB/SQL/JSP renderu, editorového/súborového preflightu, merge, cache, perex/file-history review zmien a novembrového cronu. Reštartové rezervácie z P01/P02 a P06 obnova medzi procesmi sa neopakujú bez príčiny; ich pôvodné reporty zostávajú priradené.

## Matica dôkazov

`PASS` sa vzťahuje výhradne na uvedený POC profil a podporované operácie. `PARTIAL` pomenúva chýbajúcu povinnú časť. Cesty `build/...` sú lokálne ignorované reporty; dlhodobým záznamom sú výsledky P01–P08 a verziované testy.

| Prípad | Stav | Konkrétny dôkaz / výsledok |
| --- | --- | --- |
| T01 | PASS | [P02 reštartové fázy a tabuľka ID](P02-vysledky.md#overenia-a-dôkazy), P01 rollback/produkčný insert/reštart; P08 `BranchStorageTest`, `BranchReservationTest.strictMediaFailureAndMonotonicBootstrap`, `BranchMergeTest.twentyPagePackagePublishesStableIdsAuthorDatesMediaAndMovedFolder`. Pilot #1913 zachoval všetky ID. |
| T02 | PASS | [P04 obsahová izolácia](P04-vysledky.md#overený-profil-a-dôkazy), [P05 dump/bajty](P05-vysledky.md#izolácia-a-zachované-dáta); P08 `BranchEditorTest`, `BranchFilesTest`, `BranchMediaEditorTest` opäť PASS. FulltextExecuteApps pri save neprehráva živé INCLUDE; publikačné hooky sa odmietajú. |
| T03 | PASS | M01–M11 nižšie: P03 reálny gateway JSP, P04 editor, P06 voľby a merge, P08 presná zhoda verejného menu pri off/on a pred/po merge, skutočný anonymný preklik. |
| T04 | PASS | P02 `storageIsolationStableIdsAndStaleEdits`; P03/P08 `BranchViewTest.previewDecisionLiveWorkNoopAndStaleChoice`, `BranchGatewayTest` dve vetvy/LIVE, P06/P08 no-op a LIVE voľba pri presune. |
| T05 | PASS | P05 admin-preview review 20 Java + 3 browser; P08 `BranchAccessTest`, všetkých 15 `BranchFilterTest`, reálny gateway a anonymný preview 403. Platný LIVE payload obmedzeného redaktora mimo jeho koreňa vrátil chybu oprávnenia a verejný obsah ostal presne rovnaký. |
| T06 | PASS | P04 skutočný starý iframe; P08 `BranchContextsTest.rejectsChangedBranchAndChangedDomainEvenForLive`, `BranchFilterTest` frozen hlavičky/doména, stale media/upload v P05. |
| T07 | PASS | P05 7/7 browser, A/B/LIVE originál/thumbnail/DELETE, PDF, oba uploady/elFinder; P08 7 `BranchFilesTest`, `BranchResourcesTest`, reálny gateway. Pilot dokazuje odlišné verejné/pracovné bajty pred merge a anonymné WORK bajty po ňom. Externá CDN nie je súčasťou profilu. |
| T08 | PASS | P08 všetkých 5 `BranchDecisionTest`, `BranchMergeTest` stale voľba, LIVE/WORK/no-op a obsadené rezervované ID; skutočný LIVE konflikt #1913 vyriešený cez UI. P06 pôvodný browser overil obe voľby. |
| T09 | PASS | P05 media-only save/cutoff/kategórie; P08 všetky 4 `BranchMediaEditorTest` a kompletný merge so starým/novým médiom a zachovaným vlastníkom. |
| T10 | PASS | P03/P08 `BranchNewsRenderTest.n01n02OverlayCategoriesAndPaging` v oboch JOIN režimoch, `n05FiltersOrderLoadDataAndOffsets`, `BranchViewTest.legacySqlListsCountAndPagingUseEffectiveSource`; nové/zmazané riadky pred count/LIMIT. |
| T11 | PASS | P04 CRUD/presun/revert; P06 UI obnova podstromu; P08 opäť `subtreeTrashKeepsNestingAndMandatorySnapshotsWhenHistoryDisabled`, preflight potomkov/URL a odvodená LIVE voľba. Pôvodná obnova cez UI zachovala všetky ID aj vnorenie. |
| T12 | PASS | [P07 vrátane cron review](P07-vysledky.md#úprava-po-review-admin-cron-bez-presného-mena-uzla-10-november-2026), skutočný cron po odhlásení; P08 11 `p07*` DB testov, 3 scheduler hranice a 8 existujúcich cron/cluster testov. Individuálne plány sa pri kolízii nerušia. |
| T13 | PARTIAL | P04/P08 odmietnutie multigroup/System/foreign zápisov, P06/P08 preflight schvaľovania a explicitné publikovanie bez zrkadlenia PASS. Úplný pôvodný LIVE schvaľovací/zrkadliaci/multigroup tok s off/on na reprezentatívnych fixture ešte nemá P08 dôkaz. |
| T14 | PASS | P02 reálne odmietnutie engine/oprávnení/storage; P08 `BranchStorageTest` a `BranchFlagScopeTest`; verejné HTML/Menu/News pri off/on presne zhodné. Iné DB sú serverom odmietané a nie sú deklarované ako podporované. |
| T15 | PASS | P06 skutočný nový proces + UI resume a obnova koša; P08 opäť checkpoint rollback, audit po commite, perex API, file replace pred checkpointom, finálne účinky, stale LIVE a čiastočný P07 merge. Opakovanie nevytvára ďalšiu históriu ani ID. |
| T16 | PARTIAL | N01–N12 nižšie majú reálny SQL/render/editor/merge dôkaz; P08 pridáva off/on a anonymný výsledok. Pôvodný `apps/news.js` bol spustený, ale jeho `Test editor logic` zastavila chýbajúca fixture `Jet portal 4`; plná pôvodná sada nie je PASS. |

| Prípad | Stav | Konkrétny dôkaz / výsledok |
| --- | --- | --- |
| N01 | PASS | P03 a P08 `n01n02OverlayCategoriesAndPaging`: nové/upravené/presunuté/zmazané ID, účinný list/count/paging, LIVE ostáva pôvodný. |
| N02 | PASS | Ten istý parametrizovaný test s `perexGroupUseJoin=false/true`, include/exclude a viac kategórií bez duplicít; P04 reálne priradenie kategórie. |
| N03 | PASS | P03/P08 `n03SubgroupsDefaultsAndInvalidRoots`, `BranchGatewayTest`: účinný strom/default/docMode/hĺbka a neplatný koreň. |
| N04 | PASS | P03/P08 `n04AvailabilityPerexAndPublicationTime`, nové predvolené hodnoty v `BranchMappingTest`/P04 editore; všetkých päť publish režimov vrátane NULL/hranice. |
| N05 | PASS | P03/P08 `n05FiltersOrderLoadDataAndOffsets`: opakované filtre, sort/loadData/offset/strana 2/nula. |
| N06 | PASS | P03/P08 `n06AttributeOrAndCustomCriteriaBoundary`: skutočný SQL OR/deduplikácia/enrichment a odmietnutie custom criteria. |
| N07 | PASS | P03/P08 `n07ProtectionAndRepeatedComponents`; P05/P08 media-only vlastník, výsledné dátumy a helpery médií. |
| N08 | PASS | P03 gateway a browser (paging/detail/obrázok/PDF/dve vetvy/cache), P05 upload; P08 reálny gateway + pilotné prekliky menu/News a príloha. |
| N09 | PASS | P04 5/5 browser a následný apps-refactor 2/2: nový koreň, News iframe, uloženie a stale 409; P08 `nestedIframeBootstrapChecksBothFrozenContextValues` a skutočné editorové uloženia. |
| N10 | PASS | P06 5/5 browser #1661 s LIVE aj WORK, autor/SAVE_DATE/perex/médiá/ID; P08 #1913 celé News HTML a poradie 21 ID pred/po zhodné aj anonymne. |
| N11 | PASS | P06 perex review, P08 `categoryPreflightAndExistingPerexApiPreserveOtherPages`: existujúce API po commite, pred DONE, zachovaná cudzia väzba a odmietnutie zaniknutej kategórie. |
| N12 | PASS | P07 skutočný termín/posun/zrušenie a autor/dátumy; P08 `n12MovedCancelledAndStoppedScheduleUsesOneRequestTime` a `p07ScheduledClaimPublishesSameTargetsAndRejectsSecondClaim`. |

| Prípad | Stav | Konkrétny dôkaz / výsledok |
| --- | --- | --- |
| M01 | PASS | P03 skutočný JSP a P08 `productionGatewayRealJspNewsMenuAndFiles`: názvy/navbar/poradie/default/nové objekty bez LIVE zmeny. |
| M02 | PASS | P03/P04 reálny presun/mazanie a aktívna cesta; P08 gateway + `newDefaultMoveDeleteAndHiddenRoot`. |
| M03 | PASS | P03 a zopakovaný P08 gateway `verifyMenuCases`: root 0/explicit/hidden, offset/maxLevel/openAll/onlyFolders/forceDocid, aktuálna stránka bez query docid. |
| M04 | PASS | P03/P08 gateway + `perexFullHtmlAvailabilityProtectionAndSitemap`: menu/logged/internal/available/práva a žiadny fallback po DELETE. |
| M05 | PASS | P03/P08 skutočný JSP: none/basic/full/bootstrap a presunutá otvorená cesta; fixture `m05-*`. |
| M06 | PASS | P03/P08 skutočný JSP WORK perex/HTML s odkazom a obrázkom, P05 pracovné bajty. |
| M07 | PASS | P03/P08 gateway `variables.html`: `onlySetVariables`, `ulLiSkipMenuGeneration`, ROOT, dve menu a zachovanie prvého uloženého menu. |
| M08 | PASS | P03 gateway/browser dve vetvy a zahriata cache, relatívne/absolútne/vonkajšie URL; P08 skutočný menu preklik pred/po merge aj anonymne. |
| M09 | PASS | P03/P08 gateway fixture voľby LIVE/WORK/W=B/L=W/stale; P06 UI obe voľby a P08 UI WORK po skutočnej LIVE zmene. |
| M10 | PASS | P04 5/5 + apps-refactor 2/2: MenuApp nový pracovný koreň/SAVE/reopen/JSP; P08 kontextové guardy a gateway opäť PASS. |
| M11 | PASS | P06 presné pred/po HTML vrátane aktívnych tried a cieľov; P08 `public-off.json` = `public-on.json`, `before-merge.preview` = `public-after.after`, iba host/prefix normalizované. |

Spoločné štyri typy: P08 report #1913 obsahuje `DOCUMENT`, `GROUP`, `MEDIA`, `FILE`, všetkých 25 položiek prejde spoločným porovnaním a skončí `DONE`. Odoslanie typu `BANNER` na `/resolve` vráti **400** a nezmení revíziu. Typ je uzavretý enum, nemá paralelné generické úložisko; neznámy typ sa nemôže potichu uložiť/preskočiť. Nepotrebovalo sa produkčné rozšírenie bannera/galérie.

## Pilot: 20 stránok a tri účty

Vlastný balík **#1913**, `autotest P08 pilot`, koreň **#1944070**, home **#1949375**. Dvadsať stránok, dve priečinkové zmeny, dve médiá a jedna príloha tvoria **25 položiek**. Home je starší vlastník média a nie je dvadsiatou prvou obsahovou zmenou; News preto vypisujú 21 položiek.

Účty `autotest-p08-a`, `autotest-p08-b`, `autotest-p08-c` majú samostatné prihlásenia a editáciu obmedzenú na pilotný koreň. Cez normálny editor pripravili **7 + 7 + 6** stránok vrátane WORK-only stránok; každý účet prešiel menu a svoj detail v náhľade. Účet C pripravil aj skutočnú konkurenčnú LIVE zmenu. Konflikt stránky a jej default priečinka sa vyriešil v porovnávacom UI voľbou WORK a balík sa publikoval tlačidlom **Publikovať**.

Pred publikovaním anonymné HTML vracalo **200**, News 19 pôvodných položiek, príloha BASE. Pri vypnutom aj zapnutom flagu sa celé News/Menu zhodovali. Po publikovaní je balík **MERGED**, revízia **56**, **25/25 DONE**, bez chyby. Anonymné HTML **200**, News 21 ID a celé HTML News/Menu presne zodpovedajú poslednému náhľadu. Menu aj News prekliknú na obsah správneho redaktora. Anonymná príloha **200** má presné WORK bajty; anonymný náhľad je **403**. HTML neobsahuje fyzický súkromný adresár.

P07 404 bolo reprodukované aj s novou fixture na hostiteľovi `iwcm.interway.sk`. Príčinou je existujúci lokálny `domain_redirects` alias na `demo.webjetcms.sk`, zatiaľ čo obsah bol v doméne iwcm. Menili sa iba vlastné P08 riadky na `127.0.0.1`; alias ani P05/P06/P07 dáta sa nemenili. Po obnovení doménovej cache verejné HTML vracia 200. Nešlo o opravu produkčného routovania.

## Vykonané behy

- Spoločná Java sada: **149 XML invokácií, 132 PASS, 0 failures/errors, 17 podmienených skipov**, `build/test-results/p08/java-regression/`, súhrn `java-regression-summary.json`, log `/tmp/webjet-p08-java.log`. Preskočené boli opt-in P01 experimenty/reštartové fázy a jednorazové fixture; ich staršie dôkazy sú vyššie.
- Jednorazová príprava vlastného pilotu: **1/1 PASS**, `java-fixture/`, `/tmp/webjet-p08-fixture.log`. Manifest `/tmp/webjet-p08-pilot.properties`; heslá iba v lokálnom súbore s právami 600 `/tmp/webjet-p08-login.properties`.
- Finálna doplnková Java sada: **41/41 PASS, 0 skipov**, `java-final/`, `java-final-summary.json`, `/tmp/webjet-p08-java-final.log`. Dva nové skutočné NewsActionBean/SQL/Velocity off/on prípady, štyri typy so spoločným DONE, reálne pooled/JPA čítanie počas nepotvrdenej rezervácie, pôvodný off/on izolačný probe, DBPoolIsolation a existujúce NewsRestController/NewsTreeService testy. Java zapisovacie testy vždy pri zastavenom POC serveri.
- Browser príprava/off/on/tri účty: **6 PASS** v prípravnom behu. Prvý pokus o LIVE konflikt nesprávne použil `I.selectOption(..., '0')`: tento helper najprv hľadá text obsahujúci `0`, čím vybral P08. Nevykonal sa merge a LIVE ostal pôvodný. Test teraz vyberá **Produkcia** podľa názvu a overí serverom vložený kontext `branchId=0`. Pracovný titulok bol obnovený cez editor.
- Dotknuté finálne browser scenáre: **2/2 PASS**, `/tmp/webjet-p08-browser-final.log`, `browser-publication.log`. Pilot sa znova nezakladal ani nepublikoval opakovane. Doklady jednotlivých účtov a celých komponentov sú v `browser/editor-{a,b,c}.json`, `public-{off,on}.json`, `before-merge.json`, `published.json`, `public-after.json`; screenshoty `build/test/branch-p08-{comparison,public-result}.png`.
- Pôvodný `tests/apps/news.js`, scenár **Test editor logic**: **FAIL pre chýbajúcu fixture `Jet portal 4`**, nie PASS. Prešiel vstup do NewsApp a kontrola jeho kariet; zastavil sa pri výbere pomenovaného priečinka. Žiadna stránka sa neuložila. Log `/tmp/webjet-p08-existing-news.log`, screenshot `build/test/Test_editor_logic.failed.png`. Lokálny wrapper menil iba auto-login na existujúci POC účet; pôvodný scenár sa neupravoval.
- Nový `branch-p08-regression.js`: všetkých **10 scenárov má finálny PASS v nadväzujúcich behoch**. Spoločný beh overil prípravu, obnovenie flagu, práva a meranie; dva individuálne plány potrebovali opravu čakania testu. Finálne dotknuté produkčné scenáre + cleanup **4/4 PASS** (`/tmp/webjet-p08-production-final.log`), spresnený test platného neoprávneného payloadu **1/1 PASS** (`/tmp/webjet-p08-permissions-final.log`). Nejde o tvrdenie 10/10 v jednom neprerušenom behu.

## Produkčná regresia a načítanie off/on

Vlastný samostatný koreň **#1944074**, stránky **#1949398/#1949399** a `/autotest-p08-regression/{false,true}.html`. Nejde o zmeny zachovaných dém ani o stránky kampane. Test cez bežný editor pri oboch hodnotách flagu založil/upravil obsah, overil verejnú URL, pridal médium, načítal históriu a uložil individuálny termín. Pred termínom zostal pôvodný obsah verejný, neskôr ho publikoval skutočný `DocPublishService`; nezostal aktívny budúci plán. Finálne HTML oboch stránok obsahuje `scheduled`. Reporty `regression/production-{false,true}.json`.

Prvý prípravný beh omylom prečítal z jsTree ID pôvodne vybraného rodiča namiesto práve vytvoreného priečinka. Dve vlastné testovacie stránky sa pri zastavenom serveri presunuli do vlastného regresného koreňa; manifest a test teraz používajú ID zo skutočnej save odpovede a explicitný vrchný rodič. Kampaň opäť obsahuje iba pôvodných 21 News položiek. Pri plánovaní sa test zmenil na ohraničené opakovanie anonymného čítania/assertion namiesto oddelenej čakacej podmienky a ďalšieho predčasného odčítania; samotný plán sa ukladá iba raz. Pôvodné neúspešné pokusy sa nepočítajú ako PASS.

Práva: konto A má editáciu iba pilotného koreňa. Existujúce LIVE DataTable API odmietlo čítanie aj **platný** zápis inej stránky hlásením `Nemáte právo na túto editáciu`. Jeho existujúci transportný kontrakt je HTTP 200 s `error`, nie 403. Test navyše porovnal celé anonymné HTML pred/po; zostalo rovnaké. `regression/permissions.json`. Prvý nedostatočný pokus s prázdnym záznamom bol nahradený platným payloadom načítaným administrátorom, aby sa nedokazovala len povinnosť poľa navbar.

Meranie bežného anonymného HTML renderu rovnakej kampane: 3 zahrievacie requesty + **20 sekvenčných requestov** pre každú hodnotu, bez cookies a bez browser cache. Každá odpoveď mala 200 a skutočné News/menu. Off medián **18,1 ms** (15,0–22,0 ms), on **17,9 ms** (14,9–25,1 ms). Rozdiel −0,2 ms je v šume lokálneho merania; výkonový prínos ani produkčnú kapacitu z toho nevyvodzujeme. Ide o HTML/SQL render, nie sťahovanie všetkých statických assetov, súbeh či záťažový test. Surové vzorky `regression/load-{false,true}.json`.

Preservácia: metadata a všetky WORK riadky P05 **#841/#842**, P06 **#1315** a P07 **#1789** sú pred/po bajtovo zhodné, SHA-256 **`8bd5c436e5f7d892aead2dc247d45fc3c0205be4f7b1352b73f736832d6154c6`**. Všetkých **12** aktívnych súkromných súborov má hash zhodný s WORK metadátami. `preserved-{before,after}.sql`, `preserved-summary.json`, `preserved-binaries.json`. Demá zostávajú DRAFT; P06 má revíziu 25. POC DB aj WebJET zostávajú spustené na review.

## Návody a release podklady

- [Návod redaktora](../../sk/redactor/webpages/publishing-packages.md): tímová príprava, náhľad, konflikt, publikovanie, termín a chyba.
- [Návod správcu](../../sk/admin/setup/publishing-packages.md): inventár, proxy/cache, cron `all-admin`/konkrétny admin uzol, aktivácia, vypnutie a obnova vrátane horných hraníc rezervovaných ID.
- [Návod autora šablón/komponentov](../../sk/developer/apps/webpages/publishing-packages.md): overený kontext, fasády, SQL pred limitom, odkazy/cache a vedľajšie účinky.

Návody sú zapojené do príslušných sidebarov a vstupných stránok. `CHANGELOG-2026.md` má jednu súvislú POC položku namiesto starých priebežných tvrdení „merge/plánovanie ešte nie je“. Číslo PR sa nevymýšľalo; ROADMAP nemá zodpovedajúcu položku, preto sa v nej nič neoznačilo za hotové. Java testy kompilujú aj nové testy; syntax oboch JS, lokálne Markdown odkazy a `git diff --check` prešli. Frontend zdroje ani produkčné Java API sa v P08 nemenili, nový admin build nebol potrebný.

## Opakovanie overení

Všetky príkazy sú pre tento worktree, Java 17 a bežiacu izolovanú DB 33316. Pred Java DB testami zastaviť iba POC WebJET. Spoločná regresia:

```sh
JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem \
WEBJET_P02_DB=true WEBJET_P03_DB=true WEBJET_P04_DB=true \
WEBJET_P05_DB=true WEBJET_P06_DB=true WEBJET_P07_DB=true \
./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.*' \
  --tests 'sk.iway.iwcm.system.cluster.ClusterCronTaskJUnitTest'
```

Po P08 pridaných testoch sa celkový počet oproti prvému behu zmení; neposudzovať výsledok len podľa starého počtu 149. Pre finálny doplnok použiť `WEBJET_P03_DB=true WEBJET_P08_DB=true WEBJET_P01_CORE=true`, `WEBJET_P01_JDBC_URL=jdbc:mariadb://127.0.0.1:33316/webjet_p01` a výber `BranchNewsRenderTest`, `BranchMergeTest.twentyPagePackagePublishesStableIdsAuthorDatesMediaAndMovedFolder`, `BranchCoreReadPocTest`, `BranchIdPocTest.coreIsolationGuardSeparatesFlagOffAndOn`, `DBPoolIsolationTest`, `NewsRestControllerTest`, `NewsTreeServiceTest`.

Browser z `src/test/webapp` s bežiacim POC serverom:

```sh
WEBJET_P08_BROWSER=true CODECEPT_URL=http://127.0.0.1:8086 CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branch-p08-regression.js --steps
```

Celý pilotný `branch-p08.js` je jednorazový príbeh nad DRAFT fixture; nad už MERGED #1913 ho **neopakovať**. Publikovaný výsledok možno bez zápisu overiť cez `--grep 'P08 anonymous'`. Nový celý pilot potrebuje nové fixture/manifest a nové účty; staré demá sa nesmú regenerovať. Lokálny auto-login wrapper pôvodného News scenára bol po odovzdaní odstránený; na jeho dokončenie treba pripraviť ekvivalentné pomenované fixture a testovací login, nie smerovať runner na pracovný web.

## Ako si výsledok preklikať

Server používa verejnú aj admin doménu **127.0.0.1**, port **8086**. Login `tester-p05`, heslo z existujúceho `/tmp/webjet-p05-login.properties`; pilotné účty majú heslá v `/tmp/webjet-p08-login.properties`. Heslá nie sú v Gite ani v reportoch.

1. Bez prihlásenia otvorte [verejnú kampaň](http://127.0.0.1:8086/p06-autotest-p08-test-e8d53b2c/). Prekliknite položky redaktorov A/B/C cez menu aj Novinky.
2. Otvorte [prílohu](http://127.0.0.1:8086/files/branch-p06-autotest/p08-test-e8d53b2c.txt); obsahuje publikovanú WORK verziu.
3. Prihláste sa a otvorte [dokončený balík #1913](http://127.0.0.1:8086/admin/v9/webpages/web-pages-list/?groupid=1944070#branchCompare=1913). Skontrolujte všetky štyri typy a dokončené položky. Uzavretý balík sa už neupravuje.
4. Pre ďalší pokus vytvorte **novú pracovnú verziu** v doméne 127.0.0.1. Nevkladajte zmeny do zachovaných P05 #841/#842, P06 #1315 ani P07 #1789. Ich pôvodné návody zostávajú platné; nejde o anonymne dostupné P08 stránky.

Štart/stop POC DB a WebJETu je v [P04 návode](P04-vysledky.md#opakovanie-testov-a-štart). Vždy použiť `poolman-local-p01.xml` a port 33316, nikdy pracovnú DB 3306.

## Zostávajúce povinné dôkazy a hranice

Zostáva dokončiť pôvodnú regresiu `apps/news.js` na ekvivalentných testovacích dátach a doložiť úplné LIVE schvaľovanie/zrkadlenie/multigroup pri off/on. Chýbajúce pomenované fixture sa neprehlasujú za výsledok POC pilotu. Stav P08 sa bez týchto dôkazov nesmie zmeniť na DONE.

Iné DB/topológie, externé súbory/CDN, SQL Server READ_COMMITTED dopad a kompletná obnova produkčnej zálohy nie sú overené. Na jedinom zadanom profile tieto iné prostredia neblokujú POC, ale nesmú sa aktivovať na základe jeho výsledku. Návod obnovy nie je vykonaný disaster-recovery drill. Publikovanie zostáva postupné, bez globálneho rollbacku alebo automatického failover.

Pracoval jeden hlavný agent bez delegovania; presný identifikátor modelu/reasoning v klientovi neoverený. Zmeny zostávajú necommitnuté pre review. Budúce E01–E03 sa nezačali.
