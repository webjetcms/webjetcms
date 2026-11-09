# P04 — Editácia stránok a priečinkov vo vetve

**Stav: DONE na nižšie uvedenom POC profile.** Implementácia z 8. novembra 2026 nad `849e7adb3` vo vetve `feature/58858-publishing-packages`. Zmeny zostávajú necommitnuté. Rozsah končí P04; P05–P08 sa nezačali. Východiská: [plán P04](06-plan-implementacie.md#p04--editácia-stránok-a-priečinkov), [P03 odovzdanie](P03-vysledky.md), [matica polí](P02-matica-stlpcov.md).

## Výsledné správanie

Redaktor používa existujúci editor webových stránok a formulár priečinka. V DRAFT vetve vytvára, upravuje, premenúva, presúva a maže obsah iba vo WORK. Nové stránky/priečinky používajú rezervované konečné ID z P02; v LIVE po rezervácii nezostáva obsahový riadok. Presun priečinka sa robí výberom rodiča vo formulári; staré ťahanie stromu je vo vetve vypnuté.

Náhľad zobrazuje uložený výsledok cez existujúci gateway. Tlačidlo Náhľad v editore najskôr uloží WORK. „Pracovné zmeny“ pri prepínači vetvy zobrazuje aj pracovné DELETE a umožňuje vrátiť prepis. Vrátenie existujúceho objektu obnoví preberanie aktuálneho LIVE, nejde o obnovu historického BASE. Zmazanie nového objektu odstráni jeho WORK; spotrebované ID sa nevracia. Nový priečinok s používanými závislosťami treba najskôr vyprázdniť. Vrátenie zmazaného existujúceho podstromu odstráni jeho pracovné tombstones spoločne a obnoví preberanie LIVE.

Uloženie zachová cieľovú dostupnosť a ID existujúcich perex skupín. Skutočný SAVE nastaví pracovného autora/dátum; odvodený zápis po presune priečinka tieto hodnoty stránok nemení. Dátumy/voľby individuálneho plánovania existujúcich stránok sa zachovávajú zo servera a vo vetve sa nemenia. Nová stránka preberá bežnú šablónu nového obsahu vrátane PageBuilder HTML; samostatný editor nevznikol.

Server kontroluje práva stránky aj pôvodného/cieľového priečinka, doménu, existenciu referencií/šablóny/perex skupiny, cykly stromu, nové kolízie URL a platnosť default stránky. Pri presune/zmazaní default stránky vyberie náhradu podľa poradia alebo nastaví 0; ide o explicitnú WORK zmenu priečinka. Nesúvisiace staré chybné URL/defaulty samy osebe neblokujú úpravu iného obsahu.

System a multigroup stránky možno otvoriť na čítanie s konkrétnym dôvodom; server odmieta ich SAVE. Nepodporované importy, duplikovanie, hromadné akcie, rekurzívne nastavenia priečinka, zmena domény/synchronizácie, história, atribúty, poznámky, médiá/upload a ostatné aplikácie sú blokované na serveri. Parametre podporovaných MenuApp a NewsApp sa ukladajú do INCLUDE existujúcim editorom. Nepodporovaný import/bulk vracia štandardnú DataTable odpoveď s chybou a HTTP 200; blokovanie stromového zápisu či component preview vracia 409. Ani jedna odmietnutá cesta nevykoná obsahový zápis. Definície perex skupín sa vo vetve nevytvárajú.

## Miesta implementácie

- `BranchEditor` je konkrétna autorizovaná služba pre dokument/priečinok. Používa `BranchStore` a krátku transakciu pod existujúcim zámkom vetvy; rezervácia ID, defaulty a závislé pracovné zmeny sú súčasťou tejto transakcie. Pri chybe sa vrátia späť.
- `BranchEditorRows` dynamicky číta všetky podporované stĺpce z `BranchFields`. Názvy vlastností zistí z JPA `@Column` vrátane zdedených polí, prípadne konvenciou `snake_case → camelCase`. V pamäti uchováva iba gettery; hodnoty číta pri každom uložení. Explicitné zostávajú perex ID, prevod dátumu a tri odlišne pomenované vlastnosti priečinka. Chýbajúci getter alebo nejednoznačné stĺpcové mapovanie vyvolá chybu. Prevádzkové a plánovacie hodnoty preberá z uloženého serverového riadka.
- `BranchView` dodáva hash skutočne zobrazenej verzie. WORK hash pochádza z rovnakej requestovej projekcie; LIVE hash z fyzických stĺpcov toho istého SQL čítania. Hash je aj súčasťou ADMIN údajov stromu, aby ho malo mazanie bez otvorenia formulára. Neskoršie načítanie nesmie pripojiť nový hash k starému obsahu. Prvý SAVE po zmene LIVE aj neskorší zastaraný WORK SAVE sa odmietnu.
- `EditorFacade`, `WebpagesService`, `GroupsRestController` a zdedená cesta `NewsRestController` odbočia pred produkčnými vedľajšími účinkami. Spoločná normalizácia HTML/URL je vyčlenená v `EditorService`; WORK nevykonáva aplikácie kvôli fulltextu. Generické CRUD udalosti nad stránkou/priečinkom sa vo WORK nepublikujú. Produkčná cesta zostáva zachovaná vrátane pôvodného dopĺňania prázdneho obsahu; ochrana nenahraného HTML sa pridáva iba vo WORK.
- `GET /admin/rest/branch-content` poskytuje oprávnené explicitné zmeny. `POST /admin/rest/branch-content/revert` prijíma `{type,id,hash}`, vyžaduje existujúce práva, CSRF aj nemenný kontext obrazovky.
- `BranchApps` obsahuje jedinú nemennú mapu podporovaných tried aplikácií a ich rendererov. Používajú ju `ComponentListener`, `ComponentsService` aj `component_preview.jsp`; rozšírenie podpory sa zapisuje na jednom mieste.
- `BranchFilter` povoľuje iba konkrétne P04 POST vstupy; import/bulk sa kontroluje aj v editor controlleroch. Priamy component preview prijíma iba jeden podporovaný INCLUDE, aj pri ručne zostavenom requeste. Rovnako overuje `branchId` a `branchDomain` pri bootstrapovaní vnorených News/component iframe a PageBuilder rámca. Aktualizácia URL po zatvorení editora zachováva ich INCLUDE a kontextové parametre. Starý formulár ani iframe sa po prepnutí session neprepadne do inej vetvy/LIVE: dostane 409.
- `branches.js` viaže fetch/jQuery na kontext obrazovky. Pri SAVE nesie hash a hodnoty vypnutých polí z pôvodne zobrazeného formulára. UI neobnovuje kontext potichu z druhého tabu. Pracovné ukladanie nepoužíva produkčný presence zámok bez identity vetvy.

## Overený profil a dôkazy

MariaDB **12.2.2/InnoDB**, Connector/J **3.5.10**, Java **17.0.18**, jedna zapisovacia inštancia `127.0.0.1:33316/webjet_p01`, datadir `/tmp/webjet-p03-review-db/data`. Oba datasources ignorovaného `poolman-local-p01.xml` smerujú na 33316. Pracovný profil `poolman-local.xml`, port 3306 a jeho enginy sa nemenili. Zdrojové feature flagy zostávajú predvolene vypnuté. Render používa existujúcu `aceintegration/jet/blank.jsp`, menu_ul_li.jsp/MenuULLI a news-velocity.jsp/NewsActionBean s pripravenou P03 Velocity šablónou.

| Overenie | Výsledok a rozsah |
| --- | --- |
| Spoločná Java regresia | **103/103**, 0 chýb/preskočení, vrátane opravy hashu stromu; posledný beh zahŕňa aj zachovanie pôvodnej LIVE normalizácie. P03 sada + nové `BranchEditorTest`, `BranchEditorRowsTest` a existujúce `WebpagesDatatableTest`/`WebpagesUrlTest`. |
| Nové integračné prípady | Reálna MariaDB: CRUD/stabilné ID, zachovanie PB HTML bez vykonania INCLUDE, defaulty, presun/mazanie/vrátenie podstromu, práva, cudzia doména, System/multigroup, URL/cyklus, plány, zastaraný LIVE/WORK hash. Fixture kontroluje port/katalóg pred zápisom a uprace vlastné riadky. |
| Filter/kontext | Bežné CRUD povolené, staré zápisy blokované; chýbajúci/nezhodný bootstrap kontext odmietnutý; inline ADMIN rámec nemá LIVE fallback. |
| P04 CodeceptJS/Playwright | **5/5**, bez zlyhaní. Oba povinné News/Menu scenáre, úplný CRUD tok, revert/stale formulár a serverová hranica. Navyše pôvodná P03 browser regresia **3/3**. Skutočné prihlásenie, existujúce modály, CKEditor, app dialógy, druhý tab a serverová hranica nepodporovaných akcií. |
| Admin frontend | `npm --prefix src/main/webapp/admin/v9 run prod` prešiel; 2 existujúce upozornenia na veľkosť bundle. |
| Izolácia LIVE | DB integračné testy porovnávajú celý obsah `documents`, `groups`, `documents_history`, `perex_group_doc`, `media`, `media_group_to_media`, `groups_scheduler`, `multigroup_mapping` pred/po WORK operáciách. UI dump siedmich sledovaných LIVE tabuliek je bajtovo zhodný pred/po. SHA-256 oboch: `45e844bffa24a7fc84b23896c8de6d8633d16c2cb4c1c53c427e2221e3d382c3`. Nezmenilo sa ani 13 existujúcich súborov Lucene indexu počas záverečných UI behov. |

Java dôkazy sú v `build/test-results/p04/java-final/` (22 XML súborov + Gradle log), log `/tmp/webjet-p04-final-java.log`. UI logy/screenshoty sú v `build/test-results/p04/browser-final/`, dumpy `before-ui.sql`/`after-ui.sql`, indexové SHA mapy a `profile-and-identities.txt` v `build/test-results/p04/`. SQL obsahové dôkazy majú lokálne práva 600. Finálny browser log: `/tmp/webjet-p04-verified-browser.log`; P03 regresia: `/tmp/webjet-p04-p03-browser-regression.log`. Priebežné neúspešné behy pri oprave testov/UI sa nepovažujú za finálny výsledok. Predošlá cielená sada mala 25/25; nezamieňa sa s finálnou spoločnou sadou. Java testy mockujú iba načítanie katalógu šablón v neúplnom BaseWebjetTest kontexte; rezervácie, JDBC/WORK služby a fingerprint sú skutočné. Reálny katalóg šablóny, kategória, app parametre aj JSP render sú overené plným serverom/browserom.

### Povinné redakčné scenáre

1. V bežnom editore vznikol nový pracovný priečinok a dostupná novinka s perexom, existujúcou kategóriou `1930083` a konečným ID. Filtrovaný render Noviniek ju zahrnul; nekategorizovanú ďalšiu stránku nezahrnul. Detail smeruje do gateway a zobrazuje pracovný obsah. LIVE rozcestník novinku neobsahuje. Editor NewsApp otvoril reálnu vnorenú správu Noviniek so správnou vetvou/doménou a cez ňu uložil zmenu perexu. Po SAVE zostali INCLUDE aj bootstrap parametre v URL zachované; po prepnutí v druhom tabe REST aj opätovný bootstrap starého iframe vracajú 409.
2. V MenuApp editore bol stromom zvolený nový pracovný priečinok. Po SAVE stránky a opätovnom otvorení INCLUDE zostalo jeho ID aj oba kontextové údaje zachované. Reálne menu reaguje na pridanie stránky, presun stránky do podpriečinka, premenovanie aj presun priečinka a zmazanie stránky/priečinka. Prekliky vracajú správne WORK HTML; pôvodná zaniknutá cesta má 404.
3. Serverový test navyše potvrdil objavenie nového News koreňa skenom INCLUDE uloženého iba vo WORK, odmietnutie importu/bulk/starého stromového zápisu a odmietnutie inej aplikácie či viacerých INCLUDE v component preview. Podporovaný Menu preview sa reálne vykreslil.
4. Tlačidlo „Pracovné zmeny“ vrátilo úpravu existujúcej stránky na LIVE. SAVE formulára po prepnutí do produkcie v druhom tabe bol 409 a text formulára zostal zachovaný.

Pre P04 sú splnené obsahové časti T02/T05/T06/T08/T13 a N09/M10 z [overovacej matice](07-overenie.md). T02 upload/médiá patrí P05; T08/T13 vykonanie merge a porovnanie po merge patrí P06. Tieto časti sa tu nevykazujú ako vykonané.

## Ako si výsledok vyskúšať

Server a izolovaná DB zostanú po odovzdaní spustené. Prihlásiť sa na [admin login](http://iwcm.interway.sk:8086/admin/logon/) účtom `tester-p03`; lokálne heslo je v `/tmp/webjet-p03-login.properties` a nepatrí do Git. V doméne `iwcm.interway.sk` vybrať pracovnú verziu **autotest P04 redakcia (#481)**.

- [Pracovný priečinok](http://iwcm.interway.sk:8086/admin/v9/webpages/web-pages-list/?groupid=1940781)
- [Editor novinky #1940896](http://iwcm.interway.sk:8086/admin/v9/webpages/web-pages-list/?docid=1940896)
- [Editor rozcestníka s Menu/News INCLUDE](http://iwcm.interway.sk:8086/admin/v9/webpages/web-pages-list/?docid=1940123)
- [Pracovný náhľad rozcestníka](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/481/p03-manual/)
- [Detail novej novinky](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/481/p03-manual/autotest-p04/)
- [Produkčný rozcestník na porovnanie](http://iwcm.interway.sk:8086/p03-manual/)

**News:** Otvoriť pracovný priečinok, vytvoriť ďalšiu stránku, zapnúť dostupnosť a v Perexe vyplniť perex a zvoliť `autotest P04 category`. Uložiť a otvoriť pracovný rozcestník; novinka pribudne do filtrovaného zoznamu. Rovnaká stránka bez kategórie v ňom nebude. V editore rozcestníka na karte Obsah otvoriť druhú vloženú aplikáciu (Novinky), kartu Novinky a novinku upraviť v iframe. Po uložení znovu otvoriť náhľad a vedľa LIVE rozcestník.

**Menu:** V editore rozcestníka na karte Obsah otvoriť prvú aplikáciu (Menu). Pole koreňového priečinka obsahuje `autotest P04 pracovný priečinok` (#1940781); možno stromom vybrať iný nový pracovný priečinok, potvrdiť aplikáciu a uložiť stránku. Znova otvoriť aplikáciu a skontrolovať zachovaný výber. Pod vybraným koreňom pridať dostupnú stránku s názvom v menu a dedičnou URL, prípadne podpriečinok. Presunúť stránku cez pole Umiestnenie; priečinok cez pole rodiča v jeho bežnom formulári. Náhľad rozcestníka ukáže nové menu a správne odkazy. Po zmazaní stránka z menu zmizne a jej stará URL v náhľade vráti 404. Prázdny priečinok môže podľa existujúceho MenuULLI kontraktu zostať s odkazom `#nopage`, ktorý neotvára inú stránku; zmazanie priečinka odstráni aj tento názov.

Na skúšku starého formulára ponechať editor otvorený, v druhom tabe zvoliť Produkciu a v prvom skúsiť uložiť. Hlásenie ponechá neuložený text vo formulári; treba ho skopírovať alebo obnoviť obrazovku v správnej vetve. Prepínač v tom istom tabe sa pri otvorenom editore pýta na zahodenie zmien.

### Opakovanie testov a štart

Z koreňa worktree, Java testy výhradne pri vypnutom plnom app serveri:

```sh
export JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem
WEBJET_P04_DB=true ./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchEditorTest' \
  --tests 'sk.iway.iwcm.branch.BranchEditorRowsTest' \
  --tests 'sk.iway.iwcm.branch.BranchFilterTest'
```

Aktuálny DB štart (iba ak na 33316 už nebeží):

```sh
mariadbd --no-defaults --datadir=/tmp/webjet-p03-review-db/data \
  --socket=/tmp/webjet-p03-review-db/mysql.sock --port=33316 --bind-address=127.0.0.1 \
  --pid-file=/tmp/webjet-p03-review-db/mysql.pid --log-error=/tmp/webjet-p03-review-db/server.log
```

WebJET štart po testoch:

```sh
webjetDbname=/poolman-local-p01.xml ./gradlew --offline \
  -PhttpPort=8086 -PhttpsPort=8446 -PdebugPort=5008 appStart
```

Browser z `src/test/webapp`:

```sh
WEBJET_P04_BROWSER=true CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branch-p04.js --steps
```

Test používa lokálne manifesty `/tmp/webjet-p03-manual.properties`, `/tmp/webjet-p04-manual.properties` a lokálny login. Základné demo zachová; vlastné pomocné položky presunu/zmazania uprace cez UI. Definícia existujúcej perex kategórie bola pripravená iba v izolovanej DB pred základným meraním; redakčný P04 tok ju nevytvára ani nemení.

Zastavenie iba tohto WebJETu: rovnaký Gradle príkaz s `appStop`. Izolovanú DB možno korektne zastaviť `mariadb-admin --no-defaults --socket=/tmp/webjet-p03-review-db/mysql.sock -u root shutdown`. Datadir aj manifesty zachovať na ďalšie review. Nepoužiť pracovný socket/port 3306.

## Neoverené body a hranice

- Iné DB, cluster, failover, Redis session deployment ani iné produkčné šablóny nie sú týmto overené; platia obmedzenia [P03](P03-vysledky.md#hranice-a-odovzdanie-p04p05). Žiadna aktivácia na pracovnej MyISAM DB.
- Plný vizuálny PageBuilder tok na samostatnej PB šablóne nebol preklikaný. Zachovanie PB HTML je DB testované; overenie kontextu jeho ADMIN rámca je vo filtri. P04 nezavádza vlastnú implementáciu editora.
- Nevykonaný je upload/médiá P05, merge/obnova produkčného koša P06, plánovanie balíka P07 a úplná regresia všetkých legacy produkčných schvaľovacích/prekladových workflow P08. UI a server ich vo vetve nesprístupňujú ako hotové.
- Vlastné SQL/aplikácie, dynamické routy, verejné fulltextové indexy a obchodné dáta mimo podporovaných adaptérov zostávajú produkčné. Náhľad negarantuje izoláciu ľubovoľného zákazníckeho kódu.
- Vrátenie zmeny je po celom objekte; netvorí opačný patch len jedného poľa. Automaticky materializovaná náhrada defaultu je samostatná viditeľná zmena priečinka a možno ju samostatne vrátiť až keď je referencovaná stránka opäť dostupná.
- Implementoval a kontroloval jeden agent bez delegovania; presný identifikátor modelu/reasoning v klientovi neoverený. Samostatná kontrola druhým človekom/agentom sa nevykazuje. Zmeny sú pripravené na používateľské review bez commitu.

## Doplnenie po review — dynamické mapovanie editora

Druhý ručný zoznam obsahových polí v `BranchEditorRows` bol odstránený. Pridaný klasifikovaný atribút sa načíta podľa metadát alebo pomenovacej konvencie; netreba ho dopĺňať do ďalšieho zoznamu getterov. Rozdelenie na WORK/odvodené/prevádzkové/mimo rozsahu zostáva v `BranchFields`. Existujúci `BranchReadiness` naďalej odmietne nový neklasifikovaný DB stĺpec. Dočasná Java property bez persistentného stĺpca sa automaticky nestáva obsahom vetvy.

Cielená regresia `BranchEditorRowsTest`, `BranchEditorTest`, `BranchMappingTest`: **18/18**, 0 chýb/preskočení, na rovnakej izolovanej DB 33316. Nové prípady overujú úplnosť skutočných podporovaných stĺpcov bez uloženého fallbacku, JPA názvy/konvenciu pre budúci atribút, aliasy a dátumy/perex, zachovanie serverových hodnôt a odmietnutie chýbajúceho či nejednoznačného mapovania. Existujúce P04 UI scenáre po zmene mappera: **5/5**, vrátane skutočného vytvorenia/uloženia/presunu/zmazania stránok a priečinkov a perexu v News iframe. LIVE dump zostal zhodný s pôvodnou P04 baseline. Dôkazy: `build/test-results/p04/rows-refactor/`.

## Doplnenie po review — centrálny zoznam podporovaných aplikácií

Zoznam MenuApp/NewsApp sa presunul do `BranchApps.SUPPORTED_APPS` ako nemenná mapa triedy na JSP renderer. Oba vstupy editora volajú `BranchApps.checkEditorSupport`; component preview overuje jeden podporovaný INCLUDE cez tú istú mapu. Odstránil sa aj samostatný regex s názvami podporovaných JSP. LIVE výber aplikácií ostáva bez tohto obmedzenia.

Cielené `BranchAppsTest` a `BranchFilterTest`: **14/14**, 0 chýb/preskočení. Overená podpora editorov v ADMIN/PREVIEW, odmietnutie inej/prázdnej triedy, zachovanie LIVE výberu a povolenie/odmietnutie úplných INCLUDE. Reálny browser beh **2/2**: Menu/News parametre vrátane vnoreného iframe a podporovaný/odmietnutý JSP náhľad. Dôkazy: `build/test-results/p04/apps-refactor/`.
