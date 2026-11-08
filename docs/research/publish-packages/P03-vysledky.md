# P03 — účinné čítanie a preklikateľný náhľad

Dátum: 4. november 2026. Stav: **DONE na uvedenom MariaDB/InnoDB POC profile**. Rozsah je iba P03. P04/P05, merge a scheduler sa nezačali; žiadny Git commit ani produkčné nasadenie.

## Úpravy po používateľskom review — 8. november 2026

- Index/STAGE zostal nezmenený; všetky nižšie uvedené opravy sú iba v pracovnom strome. Pôvodné legacy `text*.properties` sú opäť bajtovo zhodné s HEAD. P03 kľúče `branches.label` a `branches.production` sú iba v `text-webjet9.properties`, `text_en-webjet9.properties` a `text_cz-webjet9.properties`; nemecký P03 prírastok sa odstránil.
- DocDB list aj count používajú `int psCounter = 1`, návratový index `branchSource.bind` a `ps.setTimestamp(psCounter++, ...)`. Regresia vykonala skutočné SQL pre NEW/OLD/NEXT a explicitný dátum, s parametrami účinného zdroja pred dátumovými parametrami.
- Doplnené dôvody odbočiek od LIVE toku: účinné JPA/SQL zoznamy, oddelené objekty/cache/URL/doména, zákaz vedľajších zápisov a implicitného vytvorenia Koša, odmietnutie vlastných News adaptérov, viditeľné chyby namiesto tichého prázdneho náhľadu. ThumbTag vysvetľuje obídenie externého thumb servera aj LIVE cache, gateway kontrolu prístupu a zachovanie nepodporovaných parametrov na explicitné odmietnutie.
- `Tools.getRealPath` zostáva resolverom fyzickej cesty aj pre existujúce zapisovacie volania (`FileTools.moveFile`, výstupné streamy/mkdir). Globálne branch premapovanie by ovplyvnilo aj tieto zápisy a nezachytilo by korektne DELETE bez LIVE fallbacku. `BranchFiles.exists` ostáva výslovne čítací adaptér; nepoužité lokálne `IwcmFile` a importy v GalleryToolsForCore/MediaDB sa odstránili.
- **Overenie po review: 12/12, bez chýb/preskočení** — BranchViewTest, BranchResourcesTest a BranchGatewayTest (reálne DB/JSP/menu/News/thumbnail). Log `/tmp/webjet-p03-review-tests-final.log`, XML/HTML/log `build/test-results/p03/review-2026-11-08/final/`. Predchádzajúcich 85/85 a browser 3/3 nižšie označuje pôvodné dokončenie 4. novembra; celý browser sa pre dnešné komentáre/presun prekladov/mechanický JDBC refaktoring neopakoval. Legacy bundle bajty, jedinečnosť nových kľúčov, nezmenený staged diff a `git diff --check` sa overili samostatne.
- Pôvodný `/tmp/webjet-p01-db/data` už nemal systémové tabuľky a odmietol štart. Zostal zachovaný. Po úspešnej SHA-256 kontrole sa pause SQL dump obnovil do **nového `/tmp/webjet-p03-review-db/data`**, rovnaká MariaDB 12.2.2/InnoDB na 33316 a rovnaký poolman profil. Gateway fixture po obnove ID narazila na staré súbory; teraz používa vlastný jednorazový private root. Prvý neúspešný beh 11/12 je archivovaný v `review-2026-11-08/first-run/` a nevydáva sa za finálny výsledok. Pôvodné private dáta sa neprepisovali.
- Po testoch sa review DB korektne vypla. Plný app server sa vôbec nespúšťal, pracovná DB 3306 ostala nedotknutá. Aktuálne štartovacie príkazy nižšie používajú nový datadir/socket; historický checkpoint na konci ostáva nezmenený.

## Dokončenie po prerušení — 4. november 2026

Na tom istom počítači sa obnovila zachovaná MariaDB na porte 33316. Overené `@@version=12.2.2`, presný datadir, InnoDB enginy, existujúce vetvy 128/129, profil, private bajty aj lokálne prihlásenie. Žiadna manuálna fixture sa nevytvárala znova; pracovná DB 3306 zostala nedotknutá.

- Prvý opakovaný spoločný beh nad posledným BranchMedia/SSL stavom prešiel **84/84** a browser **2/2**. Dôkazy: `build/test-results/p03/resumed-final/` a `resumed-browser/`.
- Finálna nezávislá kontrola našla a opravila dva konkrétne problémy: jednostranné admin rozsahy `range:od`/`daterange:od` a prvotné prihlásenie cez samostatný admin host bez session domény. Default sa teraz vyberie iba z povolených domén používateľa; explicitná cudzia doména zostáva 403 a starý kontext 409. ID/dátumové hranice majú reálny DB dôkaz; nový bootstrap má pozitívne aj negatívne kontroly.
- Aktuálna spoločná sada po opravách: **85/85, 0 chýb, 0 preskočení**. Log `/tmp/webjet-p03-completion-final.log`, kompletné XML + HTML report `build/test-results/p03/completion-final/`.
- Posledný plný browser: **3/3**, vrátane WORK `require_ssl=true`, normálneho prihlásenia a vytvorenia bez aktivácie. Nový prípad cez transportný host `127.0.0.1` overil povolenú obsahovú doménu `iwcm.interway.sk`, aktiváciu vetvy, reálny ShowDoc/menu/News a načítaný 16×16 thumbnail bez zmeny transportného hosta. Log `/tmp/webjet-p03-completion-browser.log`, screenshoty a kópia logu `build/test-results/p03/completion-browser/`.
- Obaja pomocníci ukončili read-only kontrolu; opravy integroval hlavný agent. Ďalší potvrdený blocker v pridelenom rozsahu nezostal. Celý vlastný diff vrátane nových súborov sa kontroloval; P04/P05 sa nezačali.

Historický checkpoint je na konci dokumentu a opisuje stav pri odchode; aktuálne výsledky a procesy uvádzajú tieto úvodné časti.

## Prostredie a vstup P02

- Existujúci worktree a vetva `feature/58858-publishing-packages`, východisko `6ef23d343`, vstupný strom čistý. P02 API sa rozšírilo, nevznikol druhý storage ani druhé B/L/W rozhodovanie.
- Obnovená zachovaná jednorazová MariaDB **12.2.2**, Connector/J **3.5.10**, Java **17.0.18**, schéma `webjet_p01` na `127.0.0.1:33316`, InnoDB. Dáta `/tmp/webjet-p03-review-db/data`, súkromné manuálne súbory `/tmp/webjet-p03-private`.
- `poolman-local-p01.xml` je obnovený ignorovaný lokálny profil s oboma datasources na tejto DB. Existujúci `poolman-local.xml` ani pracovná DB na 3306 sa nemenili a MyISAM sa nekonvertoval. Funkcia je v zdrojových predvolených nastaveniach naďalej vypnutá; zapnuté sú iba lokálne POC konfiguračné hodnoty.
- Hlavný implementátor a najviac dvaja pomocníci so zdedeným rovnakým modelom; presný identifikátor modelu/reasoning klienta neoverený. Zapisovacie behy sa koordinovali sekvenčne; skorý krátky prebeh gateway a kombinovanej sady sa prekryl, následné spoločné sady sa vykonali samostatne; presný časový rozsah pri prerušení je v checkpointe.

## API a integračné body

| Oblasť | Platný kontrakt |
| --- | --- |
| Request pohľad | `BranchView.current()` vracia `null` pre LIVE; `context/name/previewAt/hasConflicts`, účinné skupiny, metadata, dokumenty a memoizované rozhodnutia typu + identity. ADMIN vždy číta WORK, PREVIEW používa existujúci `BranchChange.decide`/`BranchDecision`. Stará voľba vyvolá 409. |
| Čítacie API | `BranchReadService.getDoc/getGroup/groups/documents/docIdFromUrl/docLink`; oddelené `DocDetails` a request-local `GroupsDB`. Metadata a URL index nečítajú celé LIVE HTML. |
| SQL | `BranchSql.documentSource(alias)` vracia `Fragment.sql()` a `bind(statement,firstIndex)` s návratovým nasledujúcim indexom. Explicitná P02 projekcia pred WHERE/count/order/paging. Nezmenené LIVE HTML ostáva v DB, pri odvodených URL sa bindujú iba odvodené hodnoty. |
| Core | `DocDB` detail/metadáta/URL/zoznamy/perex/count a `GroupsDB` strom/korene/predkovia. Prepočet zdedených URL a root polí zo zmenených predkov, zachovanie existujúcich uložených URL vrátane kolíznych suffixov. LIVE kôš aj predkovia sú súčasťou snapshotového stavu. |
| Novinky | Pôvodný `NewsApp → news-velocity.jsp → NewsActionBean → NewsQuery → Velocity`. Účinné korene/default stránky, všetky štandardné filtre a poradia, jeden `previewAt`, unikátne ID pred paging, rovnaký count. Vlastné ResultCriteria/contextClasses vo vetve vracajú vysvetlenú chybu. |
| Menu | Pôvodný `menu_ul_li.jsp`/`MenuULLI`, účinné fasády, aktuálna stránka z requestu aj bez query docid, korene/rekurzia/default odkazy, perex/HTML a ROOT premenné. Chyba branch čítania neznamená LIVE fallback. |
| Admin zoznamy | `BranchDocumentQuery` pred filtrami/count/paging; `WebpagesDatatable` a `WebpagesService`, vrátane News override. Povolené obsahové stĺpce, rozsahy, autor, kategórie, dostupnosť/status a korene vrátane rekurzie; neznámy filter sa odmietne. Zoznam zachová `DATA_NOT_LOADED`, detail načíta HTML. História/schvaľovacie/špeciálne zoznamy sú explicitne mimo pracovného zoznamu. |
| Session/hranica | `BranchSelection.Selected(branchId,domainName)`, hlavičky **X-WebJET-Branch** a **X-WebJET-Branch-Domain**. `BranchFilter` po autentifikácii a RequestBean, pred PathFilter. Kontrola domény platí aj pre LIVE 0; hlavičky jQuery aj fetch nesú kontext načítanej obrazovky. |
| Gateway | `/__branch-preview/{id}/...` na admin hostiteľovi. Doména z DB + doménové oprávnenia, vlastná URL nezávislá od admin výberu, logická cesta vo wrapperi, obnovený RequestBean v finally. Povolené iba existujúce stránky a podporované immutable assety; interné cesty a historické/forward parametre sa odmietnu. |
| Výstup | `BranchPreviewUrls` a response wrapper: parser HTML, URL/redirect/srcset/lazy/base/inline CSS, no-store aj pri chybách/conditional/static výstupe. CacheTag/WriteTag bez branch cache; ShowDoc bez zápisu návštevnosti. Formuláre sú disabled a lokálny action je v gateway, ktorá odmieta POST. |
| Súbory | `BranchFiles.read/exists/serve/thumbnail/liveSnapshot`, `workPath(branchId,rowId)` → private root/branchId/rowId/content. Overenie hashov a hranice private úložiska, pracovný DELETE nepadá späť do LIVE. Rovnaké Q16 rozhodnutie ako ostatné typy. |
| Médiá | Oddelené `BranchMedia` objekty a `MediaDB` vetvová čítacia cesta. WORK členstvo kategórií z P02 snapshotu, marker z výsledného dátumu vlastníka iba pri zmenenom WORK; LIVE/no-op marker zachovaný. Existencia pracovných PDF pred renderom aj varianty perex obrázkov cez GalleryToolsForCore. |
| Thumbnaily | Privátny branch/content-hash/rozmerový priestor. Priamy existujúci pixelový `GalleryDBTools.cropAndResize`, bez zapisovania GalleryDB metadát alebo produkčného thumb cache. ThumbTag obchádza externý thumb server. |
| Zápisová ochrana | Obsahové POST/PUT/DELETE aj legacy JSP/action/upload cesty sa v aktívnej branch odmietnu. Explicitne povolené čítacie POST stromu a News selectorov. Riadiace branch API a technický refresher sú LIVE. DocDB/GroupsDB zapisovacie vstupy majú dodatočný guard pred vedľajšími účinkami. |

Riadiace REST API dopĺňa existujúce GET/POST kolekcie:

- `GET /admin/rest/branches/context` → `branchId,domainName,name,editable=false` v P03.
- `POST /admin/rest/branches/context` s `{branchId,expectedDomainName}`; 0 explicitne vyberie produkciu, CSRF a doménové kontroly ostávajú.
- `GET /admin/rest/branches/{id}/preview-link?docId=...`; bez docId vyberie dostupnú default stránku doménového koreňa. Vráti URL internej stránky; jej `external_link` následne spracuje ShowDoc redirect a wrapper ponechá vonkajší cieľ.
- Prepínač pod doménou, textový indikátor v hlavičke, formulár vytvorenia a samostatné `Upravovať web`/`Otvoriť náhľad`. Samotné vytvorenie nemení session výber.

## Overenia

**Aktuálny spoločný Gradle beh: 85/85 úspešných, 0 chybných, 0 preskočených**, vrátane posledných mediálnych, SSL a review opráv. Reporty sú v `build/test-results/p03/completion-final/`, log `/tmp/webjet-p03-completion-final.log`. **Plný browser 3/3** je archivovaný v `build/test-results/p03/completion-browser/`. Frontend build z predošlého behu zostáva platný: jeho produkčné zdroje sa pri pokračovaní nemenili; 2 existujúce warnings sú v `/tmp/webjet-p03-admin-build-final.log`. Historické skoré zlyhania fixture a starší 84-testový snapshot sa nevydávajú za dôkaz posledného zdrojového stavu.

Testy používajú skutočné InnoDB riadky a pripravené bajty. Gateway vykonáva pôvodné JSP, Stripes, WriteTag aj Velocity; nevracia mockované obsahové zoznamy. Embedded session je fixture, plný server používa normálne prihlásenie lokálneho testovacieho účtu.

Pri reálnom renderovaní sa opravilo:

- antispamové obfuskovanie formulára do JavaScriptu obchádzalo HTML wrapper;
- bežný GalleryDB resize skúšal zapisovať galériu aj pri preview; nový adaptér volá iba pixelovú transformáciu;
- MariaDB `TINYINT(1)` pri enum hodnotách -1/2 vracal cez getObject Boolean; spoločné čítanie explicitne načíta číselné menu/follow hodnoty. GROUP navbar/sitemap hodnoty zostávajú enum 0–3, DOCUMENT hodnoty nullable Boolean; nemení sa schéma;
- metadata/URL index sa nesmie zostaviť počas mapovania nedokončeného stromu cez lazy navbar odkaz;
- staré záložky a legacy mutácie mimo /admin nesmú obísť branch guard;
- Tomcat 11 forward uzatváral reálny transport cez response wrapper; izolovaný existujúci Spring mock delegate zachytáva status/hlavičky/telo do finálneho odoslania. LogicalRequest mení cestu iba pri REQUEST, nie vo vnútri forward/include;
- branch bootstrap administrácie nevytvára produkčný Kôš a jeho nepodporená karta zostáva skrytá.

### Povinné renderovacie prípady

| Rozsah | Vykonaný dôkaz |
| --- | --- |
| N01–N02 | Nové/zmenené/presunuté/zmazané novinky, perex kategórie include/exclude, oba perexGroupUseJoin režimy, count a poradie v skutočnom NewsActionBean/SQL/Velocity. |
| N03–N04 | Účinné podpriečinky/hĺbka/default stránky/docMode/neplatný koreň; dostupnosť, perex a päť publish režimov pri presnom previewAt vrátane NULL/rovnosti. |
| N05–N07 | Štandardné radenia, opakované filtre/tag/author/search, loadData/offset/paging/nula; atribútový OR bez duplicít a enrichment; identita a deduplikácia dvoch komponentov. |
| N08 | Skutočný news-velocity.jsp cez Stripes/Velocity v gateway, ďalšia strana, detail, WORK obrázok/PDF/thumbnail a nezávislé LIVE/dve vetvy. |
| M01–M02 | Skutočný menu_ul_li.jsp: nové a zmenené stránky/priečinky, nová default stránka, presun, zmazanie, aktívna cesta a odkazy. |
| M03–M05 | Reálny JSP s root=0/explicit/skrytým koreňom, hĺbkou/offsetom/onlyFolders/forceDocid, dostupnosťou a skutočnou identitou; none/basic/full/bootstrap. |
| M06–M07 | WORK perex/HTML s obrázkom; onlySetVariables, zachovanie uloženého menu pri priamom druhom výpise, vyčistenie neplatného ROOT a uloženého prázdneho menu. |
| M08–M09 | Preklik po zahriatí cache, dve vetvy/LIVE, absolútne/relatívne/vonkajšie URL; LIVE/WORK/W=B/L=W aj 409 pri starej voľbe, vrátane HTML menu. |
| Forward | Skutočný nested forward cez servlet do blank.jsp a následné menu/News JSP; zhodný obsah s include, jeden banner a zachované gateway paging URL. |

HTML a obrázkové dôkazy sú v `build/test-results/p03-gateway/`, najmä `forward-*.html`, `m03-*.html` až `m09-*.html`, `page2.html`, `variables.html`, `thumbnail.png`. `BranchNewsRenderTest` vykonáva skutočný action/SQL/Velocity render, nenahrádza ho mockom.

Finálny Java príkaz (server počas kompilácie/testov zastavený):

```sh
JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem \
WEBJET_P03_DB=true WEBJET_P02_DB=true WEBJET_P01_CORE=true \
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
  --tests 'sk.iway.iwcm.branch.BranchNewsSqlPocTest' \
  --tests 'sk.iway.iwcm.branch.BranchResourcesTest' \
  --tests 'sk.iway.iwcm.branch.BranchPreviewUrlsTest' \
  --tests 'sk.iway.iwcm.branch.BranchViewTest' \
  --tests 'sk.iway.iwcm.branch.BranchGatewayTest' \
  --tests 'sk.iway.iwcm.branch.BranchFilterTest' \
  --tests 'sk.iway.iwcm.branch.BranchNewsRenderTest'
```

## Manuálne vyskúšanie

Existujúca pripravená fixture:

- Prihlásenie: `http://iwcm.interway.sk:8086/admin/logon/`.
- Lokálny účet `tester-p03`; jeho náhodné heslo je iba v `/tmp/webjet-p03-login.properties` s právami 600. Nie je v repozitári ani v tomto dokumente.
- Strom/zoznam: `http://iwcm.interway.sk:8086/admin/v9/webpages/web-pages-list/?groupid=1940095`.
- Produkcia: `http://iwcm.interway.sk:8086/p03-manual/`.
- Vetva A: `http://iwcm.interway.sk:8086/__branch-preview/128/p03-manual/`.
- Vetva B: `http://iwcm.interway.sk:8086/__branch-preview/129/p03-manual/`.
- Manifest konkrétnych ID je `/tmp/webjet-p03-manual.properties`; po novej čistej fixture budú ID iné.

Po prihlásení vybrať v prepínači `P03 manual A`, overiť názov v hlavičke a pracovné novinky v zozname. Otvoriť Náhľad, prekliknúť menu/detail, ďalšiu stranu Noviniek, obrázok, thumbnail a PDF. Vedľa otvoriť produkciu a druhú vetvu. Bežné editovanie/upload sa v P03 serverovo odmietnu; pracovné dáta vytvorila fixture.

Reprodukcia prostredia nad zachovaným P02 profilom:

```sh
mariadbd --no-defaults --datadir=/tmp/webjet-p03-review-db/data \
  --socket=/tmp/webjet-p03-review-db/mysql.sock --port=33316 --bind-address=127.0.0.1 \
  --pid-file=/tmp/webjet-p03-review-db/mysql.pid --log-error=/tmp/webjet-p03-review-db/server.log
```

Ignorovaný `poolman-local-p01.xml` vytvoriť z docker MariaDB profilu; obe URL smerujú na `jdbc:mariadb://127.0.0.1:33316/webjet_p01`, lokálny účet z P02. Neprepisovať `poolman-local.xml`. Java sa v tomto prostredí volí cez `JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem`.

Fixture vytvárať iba raz na uvedenej jednorazovej DB, mimo bežiaceho WebJETu:

```sh
WEBJET_P03_MANUAL=true ./gradlew --offline test \
  -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchManualFixtureTest'
```

Pri prvom štarte štandardný bootstrap `defaultDisabled` zakáže nové právo menuBranches všetkým existujúcim účtom; fixture účtu po tomto prvom bootstrape udeliť iba toto právo. Na bežných používateľoch sa nič automaticky neudeľuje.

```sh
npm --prefix src/main/webapp/admin/v9 run prod
webjetDbname=/poolman-local-p01.xml ./gradlew --offline \
  -PhttpPort=8086 -PhttpsPort=8446 -PdebugPort=5008 appStart
```

Gradle kompiláciu/Java testy nespúšťať súbežne so štartom plného servera; počas rebuildovania výstupu môže jeho classloader dočasne stratiť triedy. Browser test používa pripravený manifest a normálne prihlásenie:

```sh
cd src/test/webapp
WEBJET_P03_BROWSER=true CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branch-p03.js --steps
```

## Hranice a odovzdanie P04/P05

**Presný ďalší krok:** počkať na zadanie P04 alebo P05. Pri P04 načítať tento výsledok, P02 maticu a jeho sekciu plánu; nadviazať na `BranchStore`, `BranchView` a kontextové hlavičky. Pri P05 nadviazať na serverom odvodenú `BranchFiles.workPath` a existujúce adresárové oprávnenia. P03 už nevyžaduje opakovanie prerušeného testu; všetky záverečné behy dobehli.

- Overený je iba MariaDB 12.2.2/InnoDB/single-writer a lokálny filesystem. Iný profil, MyISAM, IwcmFsDB, repliky, failover, reverse proxy/CDN a viac JVM časových zón nie sú deklarované ako podporené. Branch výber aj preview overujú základnú readiness; vytvorenie balíka zachováva plný P02 activation preflight.
- Podporená reprezentatívna JSP šablóna je `aceintegration/jet/blank.jsp` cez WriteTag a core menu/Novinky. Produkčné definície šablón sa nemenia vo vetve. Vlastné SQL/JPA, CDN, analytické skripty, dynamické fetch/iframe a load-more potrebujú explicitný adaptér. Všeobecné vyhľadávanie/Lucene/sémantické indexy ostávajú LIVE; nepatria do tohto preview kontraktu.
- Thumbnaily sú v POC runtime overené pre lokálne w/h zmenšenie PNG v uvedenej šablóne. Adaptér povoľuje aj JPEG/GIF/WebP, ich samostatný runtime beh sa tu nevykonal. Dodatočné ip/c/q/noip transformácie sa explicitne odmietnu (žiadny tichý produkčný fallback); ich parametre ThumbTag zachová. AVIF a uložené galerijné crop/watermark správanie nie sú týmto krokom overené.
- Pracovné súborové bajty pripravuje iba fixture v serverom odvodenej ceste. P05 doplní autorizované uploady/atómové nahradenie/rename/list/mkdir a dnešné adresárové práva, bez galerijných produkčných vedľajších účinkov. `BranchFiles` nie je všeobecný virtuálny filesystem.
- P04 doplní autorizovanú editáciu stránok/priečinkov, validáciu referencií/cyklov/domény/URL, SAVE a plný News/Menu selector tok. Dnešné branch zápisy obsahu sú blokované; interný P02 storage nie je bezpečný editor.
- P06 dodá používateľské voľby konfliktov a merge. Dnešné Q16 voľby pripravujú fixture cez existujúce choice/hash stĺpce; náhľad ich iba vyhodnotí. P07 scheduler sa nezačal.
- Všeobecné zoznamové/exportné/historické obrazovky a nepodporované zapisovacie cesty v branch nemajú produkčný fallback. Technické prihlásenie/refresher a riadiace API majú explicitný LIVE kontext.

## Aktuálny lokálny stav po dokončení

Po dokončení boli POC MariaDB 33316 a WebJET 8086/8446 ponechané pre manuálne vyskúšanie. Na následný pokyn používateľa kvôli šetreniu batérie sa oba korektne zastavili (app server PID 57005 cez appStop, DB cez overený socket). Pred ručným vyskúšaním ich treba znovu spustiť podľa príkazov vyššie. Účet, vetvy 128/129, bajty aj pôvodný pause archív sú zachované. Browser vytvára označené prázdne drafty `autotest-p03-created-*`; nemení produkčný obsah. Lokálne prihlasovacie heslo ostáva iba v súbore s právami 600. Bežné Gradle daemony ani pracovná DB 3306 sa nezastavujú.

Korektné zastavenie iba týchto POC procesov:

```sh
JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem \
webjetDbname=/poolman-local-p01.xml ./gradlew --offline \
  -PhttpPort=8086 -PhttpsPort=8446 -PdebugPort=5008 appStop
mariadb-admin --no-defaults --socket=/tmp/webjet-p03-review-db/mysql.sock -u root shutdown
```

## Historický checkpoint prerušenia — už nepopisuje aktuálny beh

## Bezpečné prerušenie na pokyn používateľa — 4. november 2026

**P03 je PARTIAL, prerušené; nie DONE.** Po pokyne sa nezačala ďalšia implementácia ani nový test. Už spustený `BranchResourcesTest` dobehol úspešne **1/1**, bez chýb a preskočení. Obaja pomocníci sú dokončení/neaktívni, ich zmeny a handoff prevzal hlavný agent. Goal mode nebol aktívny (`get_goal` vrátil null).

### Hotové a posledná zmena

Implementované sú účinné stránky/priečinky/URL/strom a admin zoznamy, P02 B/L/W mapovanie pre PREVIEW vs ADMIN, filter a overený doménový kontext, admin-host gateway, HTML/redirect/CSS prepis, oddelenie cache, reálne News/menu, MediaDB/súbory/perex varianty a privátne thumbnaily, základ vytvorenia/výberu/indikátora vetvy a serverové blokovanie nepodporovaných zápisov. Podrobný inventár API je nižšie.

Posledné zmeny pred prerušením:

- `ShowDoc` v PREVIEW nepresmerúva podľa obsahového `require_ssl`; TLS/host zostávajú transportom administrácie. Manuálny WORK dokument `1940123` v oboch fixture vetvách má teraz `require_ssl=1`, LIVE ostal nezmenený. **Plný browser tok s touto poslednou SSL fixture ešte nebežal.**
- `BranchMedia` teraz načíta skutočné LIVE definície `media_groups` (názov, available_groups, related_pages), namiesto neúplných beanov obsahujúcich iba ID. Posledný dobehnutý test overil kategorizáciu/filter aj LIVE/WORK voľbu existujúceho média, zachovaný LIVE cutoff 2019, WORK marker z dátumu vlastníka 2020 a stale choice. **Po tejto poslednej zmene ešte nebežala celá spoločná sada ani plný server/browser.**
- Rozšírený `BranchGatewayTest` už overil cudziu doménu 403 pre stránku aj asset, inú doménu session bez jej zmeny, conditional PNG 200/no-store a FILE Q16 vrátane DELETE bez fallbacku, LIVE/WORK a stale 409.

### Platné výsledky a čo zopakovať

| Beh | Výsledok a hranica |
| --- | --- |
| Posledná spoločná Java sada | **84/84**, 0 chýb/preskočení; `/tmp/webjet-p03-final.log`. Zahŕňa nové doménové/FILE Q16/mediálne časové dôkazy, ale predchádza poslednému doplneniu úplných definícií médií. Po pokračovaní zopakovať celý príkaz uvedený nižšie. |
| Archivovaný spoločný XML snapshot | `build/test-results/p03/final/` je starší úspešný snapshot 84/84; aktuálny časový rozsah je v `pause-2026-11-04/timestamps.txt`. Nevydávať ho za opakovaný celý beh nad poslednou zmenou BranchMedia. |
| Doplnková gateway/resources sada | **2/2**, `/tmp/webjet-p03-supplemental.log`, XML `build/test-results/p03/supplemental/`; doména/session/conditional a FILE/MEDIA Q16. |
| Aktuálny dobehnutý mediálny test | **1/1**, `/tmp/webjet-p03-media-final.log`; aktuálne XML + HTML report skopírované do `pause-2026-11-04/current-test-results/` a `current-test-report/`. Kompilácia posledného BranchMedia prešla v tomto behu. |
| Plný Codecept browser | **2/2**, `/tmp/webjet-p03-browser.log`, `build/test/branch-p03-{admin,preview-a,preview-b,create}.png`. Normálne prihlásenie, vytvorenie bez prepnutia, prepínač, stale 409, ShowDoc/menu/News/paging/images/thumb a dve vetvy/LIVE. Predchádza poslednej SSL fixture a načítaniu definícií médií, preto zopakovať. |
| Frontend build | `npm --prefix src/main/webapp/admin/v9 run prod` úspešný, 2 existujúce build warnings; `/tmp/webjet-p03-admin-build-final.log`. Natívny no-picker prepínač zobrazuje správnu vetvu a je disabled do dokončenia inicializácie. |

Nič z práve bežiaceho testu nezostalo nedokončené. Zostáva zopakovať spoločnú sadu a plný browser nad posledným zdrojovým stavom, skontrolovať finálny diff/inventár obmedzení a až potom rozhodnúť o uzavretí P03. **Nezačínať P04/P05.** Zostávajú aj už uvedené nepodporované profily, pokročilé thumb parametre a zákaznícke adaptéry; nie sú automaticky overené.

### Uložené prostredie a dáta

- Worktree `/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms-feature-58858-publishing-packages`, vetva `feature/58858-publishing-packages`, východisko `6ef23d343`. Všetky zmeny necommitnuté; nezmazané ani nové untracked súbory.
- Java `/Users/jeeff/.sdkman/candidates/java/17.0.18-tem`, MariaDB 12.2.2 / Connector/J 3.5.10 / InnoDB. Jediný POC datasource `127.0.0.1:33316/webjet_p01`, plus testová čistá schéma `webjet_p02_clean` na tej istej inštancii.
- Zachované dáta `/tmp/webjet-p01-db/data`, socket pri behu `/tmp/webjet-p01-db/mysql.sock`; ignorovaný profil `src/main/resources/poolman-local-p01.xml` s oboma datasources na 33316. `poolman-local.xml` ani pracovná DB na 3306 sa nemenili.
- Manuálne vetvy **128 / 129** (`P03 manual A/B`), doména `iwcm.interway.sk`, groupId **1940095**, homeId **1940123**, detailId **1940124**. Manifest `/tmp/webjet-p03-manual.properties`.
- Bajty `/tmp/webjet-p03-private` (platné PDF + PNG + thumbnaily); testové `/tmp/webjet-p02-private`. Lokálny účet `tester-p03`, heslo len v `/tmp/webjet-p03-login.properties` (600), nie v dokumentácii.
- POC `_conf_` má branchesEnabled=true, branchSingleWriterConfirmed=true, branchStorageRoot=/tmp/webjet-p03-private a multiDomainEnabled=true; predvolené zdrojové flagy ostávajú vypnuté. Pracovnú MyISAM DB nikdy neaktivovať ani nekonvertovať.
- Trvalá ignorovaná záloha v **`build/test-results/p03/pause-2026-11-04/`**: `poc-databases.sql` (4 190 470 bajtov, SHA-256 vedľa), `database-state.txt`, `timestamps.txt`, všetky P03 logy, aktuálny report, `local/webjet-p03-private`, `local/webjet-p02-private`, lokálny profil, manifest aj login. Archív je 700, dump/prihlasovacie súbory 600; necommitovať ho. Pred stratou `/tmp` teda existuje obnova v worktree.

### Procesy pri prerušení

WebJET na 8086/8446 je zastavený. Po dokončení dumpu sa overilo `@@port=33316` a presný datadir, potom sa **iba POC MariaDB korektne zastavila cez mariadb-admin shutdown**. Pracovná DB 3306 sa nezastavovala. Žiadny test ani pomocník nepokračuje; nezastavovali sa cudzie Gradle daemony.

### Presný ďalší krok po pokyne pokračovať

1. Prečítať tento checkpoint, aktuálny diff a timestampy. Nepúšťať znovu vytváranie manuálnej fixture — už existuje a jej create test úmyselne odmieta duplicitu.
2. Spustiť zachovanú DB, skontrolovať lokálny profil a private dáta. Bežný štart v samostatnom termináli:

```sh
mariadbd --no-defaults --datadir=/tmp/webjet-p01-db/data \
  --socket=/tmp/webjet-p01-db/mysql.sock --port=33316 --bind-address=127.0.0.1 \
  --pid-file=/tmp/webjet-p01-db/mysql.pid --log-error=/tmp/webjet-p01-db/server.log
```

Ak `/tmp` po reštarte chýba, najskôr obnoviť private adresáre/manifest/login z `build/test-results/p03/pause-2026-11-04/local/`. Pri strate DB inicializovať **nový jednorazový** `/tmp/webjet-p01-db/data` cez `mariadb-install-db --no-defaults --datadir=/tmp/webjet-p01-db/data --auth-root-authentication-method=normal --skip-test-db`, spustiť ho presne na 33316 a importovať `poc-databases.sql` výhradne cez tento socket. Nikdy neimportovať do pracovnej DB 3306.

3. **Pri stále zastavenom WebJETe zopakovať 84-testový Java príkaz z časti Overenia nižšie**, s `JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem`. Archivovať nový celý XML/report oddelene od predchádzajúcich.
4. Potom spustiť WebJET (Java testy/kompiláciu nespúšťať súbežne so štartom servera):

```sh
JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem \
webjetDbname=/poolman-local-p01.xml ./gradlew --offline \
  -PhttpPort=8086 -PhttpsPort=8446 -PdebugPort=5008 appStart
```

5. Zopakovať normálny browser tok vrátane WORK require_ssl=true, overiť správny prepínač a vytvorenie bez aktivácie:

```sh
cd src/test/webapp
WEBJET_P03_BROWSER=true CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branch-p03.js --steps
```

Náhľady po štarte: `http://iwcm.interway.sk:8086/__branch-preview/128/p03-manual/` a `/__branch-preview/129/p03-manual/`; prihlásenie `/admin/logon/`, zoznam `/admin/v9/webpages/web-pages-list/?groupid=1940095`. Lokálne URL teraz nebežia, pretože procesy boli korektne zastavené. Potom urobiť poslednú kontrolu diffu a pravdivo doplniť stav; stále iba P03, bez commitov a deployu.


