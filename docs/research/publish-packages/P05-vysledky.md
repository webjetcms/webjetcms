# P05 — Médiá a súbory v publikačnej vetve

**Stav: DONE na uvedenom POC profile.** Implementácia z 8. novembra 2026 nad `9b65884d6` vo vetve `feature/58858-publishing-packages`. Zmeny zostávajú necommitnuté. Rozsah končí P05; vykonanie merge a porovnanie po publikovaní patria P06.

Východiská: [plán P05](06-plan-implementacie.md#p05--médiá-a-súbory), [analýza médií a súborov](analyza-media-a-subory.md), [P02 dátové mapovanie](P02-matica-stlpcov.md), [P03 náhľad](P03-vysledky.md) a [P04 editor](P04-vysledky.md).

## Doplnenie 9. novembra 2026: administrátorská preview URL

Náhľad stránky, obrázkov, súborov aj thumbnailov používa `/admin/webpages/branch-preview/{branchId}/{logická cesta}`. `BranchPreviewUrls.PREFIX`, frontend, testovacie fixture a dokumentované odkazy sú aktualizované. Starý koreňový prefix nemá kompatibilný alias. Uložené HTML a mediálne odkazy naďalej obsahujú konečné logické cesty bez preview prefixu.

Pred nastavením vetvového kontextu a odstránením prefixu gateway volá `PathFilter.checkAdminRequest` nad pôvodnou požiadavkou. Používa existujúce `checkAdmin` (typ uzla a `adminEnableIPs`) a `checkAccessToAdmin` (prihlásenie/oprávnenia a Referer), vynucuje `adminRequireSSL`. HTTPS presmerovanie zachová celú vstupnú URI aj query ešte pred inicializáciou pôvodnej cesty v `PathFilter`. Odmietnutie v tejto vstupnej kontrole má priamy HTTP stav, aby chybová JSP neprepísala súkromné cache hlavičky. Kontroly vetvy, domény, konkrétneho obsahu a režim GET/HEAD zostávajú zachované.

Cielená Java regresia: **20/20 PASS** — `BranchFilterTest` 14, `BranchPreviewUrlsTest` 1, skutočný HTTP/JSP `BranchGatewayTest` 1 a `BranchMediaEditorTest` 4. Nové prípady overujú zákaz public uzla/nepovolenej IP, HTTPS presmerovanie a cudzí Referer ešte pred načítaním vetvy. Frontend build prešiel s 2 existujúcimi size warnings. Reporty tohto doplnenia sú oddelene v `build/test-results/p05/admin-preview/`; pôvodné P05 reporty zostali zachované.

Skutočné UI: **3/3 PASS** v cielenom CodeceptJS/Playwright behu — CKEditor drop a uloženie logických URL, serverové zákazy/stale kontext a administrátorské náhľady A/B. Overené vygenerovanie novej preview URL, interné odkazy, originály a thumbnaily A/B/LIVE, HEAD 200, stará URL 404 a anonymný náhľad 403 s `no-store`. Dôkazy `browser.log`, `admin-preview-urls.json`, `ckeditor.json`, `server-boundaries.json` v uvedenom adresári. Obsah 11 LIVE tabuliek a štyroch WORK tabuliek P04 #481 je pred/po zhodný; fyzický LIVE PNG zachoval pôvodný hash. Dôkazy `live-before.sql`/`live-after.sql`, `p04-before.sql`/`p04-after.sql` a `isolation.json`. Existujúci Git stage zostal nezmenený; doplnenie je necommitnuté.

## Výsledné správanie

Redaktor používa existujúci moderný upload, CKEditor drag/drop a elFinder. Vo vetve možno vytvoriť prázdny adresár a jednotlivý súbor nahrať, nahradiť, premenovať, presunúť alebo zmazať. Cesty v obsahu zostávajú výsledné logické URL. Bajty sa ukladajú do súkromného úložiska cez `BranchFiles.workPath`, pod existujúcu identitu `branch_files`; produkčný súbor sa pri týchto operáciách nemení.

Prázdny adresár má `DIRECTORY/UPSERT` marker bez bajtov. Listing zlúči LIVE položky, pracovné súbory, markery a ich rodičov a odstráni pracovné DELETE. Obnovenie správcu preto prázdny pracovný adresár nestratí. Cesta súboru sa nesmie použiť ako adresár ani po pracovnom zmazaní. Presun jednotlivého súboru vytvorí cieľovú WORK položku a odstráni alebo označí pôvodnú cestu ako DELETE. Pôvodný BASE snapshot a kópia pôvodných bajtov zostávajú podkladom porovnania v P06.

Zápis kontroluje explicitný kontext obrazovky, prihlásenie, CSRF, práva logického adresára, typ a skutočnú veľkosť súboru. Moderný upload zachováva aj svoj chunk tok a potvrdenie konfliktu cez „Prepísať“/ponechanie oboch súborov. Dočasný kľúč nahratia sa viaže na používateľa, vetvu a doménu. Starý formulár ani rozpracovaný upload sa po prepnutí kontextu nesmie uložiť do inej vetvy alebo produkcie.

Galérie vrátane zdedených galerijných adresárov a variantov `o_`/`s_` zostávajú iba na čítanie. Server odmieta aj operácie nad celým adresárom okrem `mkdir`, kopírovanie, archív/extract, resize, vytvorenie a textovú úpravu súboru, úpravu atribútov/práv a nepodporované archívne uploady. Podporované operácie nevolajú produkčné galerijné, fulltextové, redirectové ani zákaznícke upload hooky a nemenia `file_atr` alebo `dirprop`.

Pri pracovnej verzii sa zobrazuje dohodnuté upozornenie: „Pri spoločnom fyzickom úložisku môže publikovanie súboru ovplyvniť aj ďalšie domény. Presný zoznam použití sa nezisťuje.“ Správca používa jeden koreň „Všetky súbory“ s `/images` a `/files`.

Stránkové médiá sa pridávajú, upravujú a mažú samostatne cez vnorenú DataTable. Fungujú aj pri stránke, ktorá existuje iba vo WORK a už má svoje konečné ID. Pred prvým uložením novej stránky treba najskôr uložiť stránku do vetvy; pracovný tok nepoužíva produkčné `documents_temp` médiá používateľa.

Existujúce médium zachová `media_id`, nové dostane konečné rezervované ID. WORK uchováva poradie aj ID existujúcich kategórií. Filter kategórie sa aplikuje na účinný zoznam pred počtom výsledkov a stránkovaním. Úprava média má vlastný hash zobrazenej verzie; zastaraný zápis sa odmietne. Definície kategórií, import, duplikovanie a hromadné prepisy médií sú mimo pracovného CRUD.

Pri skutočne zmenenom WORK médiu sa zobrazovací `last_update` odvodí od účinného dátumu vlastníckej stránky na sekundy. Médium starej stránky je tak v náhľade viditeľné bez nového uloženia stránky. Autor ani SAVE_DATE vlastníka sa tým nemenia. No-op alebo zvolený LIVE obsah zachová dnešný cutoff; rozpracované produkčné médium s budúcim `last_update` sa tým nesprístupní.

## Hlavné miesta implementácie

- `BranchFiles` poskytuje `read/list/mkdir/put/delete/move`, súkromné bajty, BASE kópie a rollback nahradenia pri neúspechu DB operácie. `BranchStore` aktualizuje existujúci súborový riadok so zachovaným BASE a interným ID.
- `BranchFsVolume`, `BranchFileCommands` a connector odbočia pred legacy vedľajšími zápismi elFindera. Nepodporovaný príkaz sa odmietne na serveri; samotné vypnutie tlačidiel nie je ochrana.
- `AdminUploadServlet`/`AdminUploadController` a `UploadFileAction`/`EditorUpload` zapisujú pracovné bajty pri zachovaní existujúcich upload kontrol. JS posiela nemenný kontext obrazovky. Zobrazovací prefix v editore sa nesmie serializovať do uloženého obsahu.
- `BranchMediaEditor` používa existujúce P02 rezervácie a WORK operácie. `MediaRestController` poskytuje branch CRUD, detail, filtre a paging; `BranchMedia` skladá oddelené mediálne objekty a kategórie. `MediaService` nepreberá produkčné dočasné médiá a nepovolí implicitný hromadný LIVE prepis.
- P03 gateway aj thumbnail zostávajú spoločnou čítacou cestou. Thumbnail má súkromný kľúč obsahujúci vetvu a hash originálu. Vybraný DELETE vracia 404 a neskúša produkčný fallback.

## Prostredie a vlastné testovacie dáta

MariaDB **12.2.2/InnoDB**, Connector/J **3.5.10**, Java **17.0.18**, jedna lokálna inštancia `127.0.0.1:33316/webjet_p01`, lokálny filesystem. Datadir je `/tmp/webjet-p03-review-db/data`; ignorovaný profil je `poolman-local-p01.xml`. WebJET používa porty 8086/8446 a šablónu `aceintegration/jet/blank.jsp`. Zdrojové feature flagy zostávajú predvolene vypnuté. Pracovná DB na 3306 ani jej profil sa nemenia.

P05 používa vlastné dáta. Demo P04 **#481** sa nepoužíva na zapisovacie skúšky.

| Položka | Identita |
| --- | --- |
| Pracovná vetva A | `autotest P05 media/files A` — **#841** |
| Porovnávacia vetva B | `autotest P05 media/files B` — **#842** |
| Vlastný priečinok | **1941527** |
| Staršia LIVE stránka | **1941626**, dátum z roku 2020 |
| Stránka iba vo WORK A | **1941629** |
| Existujúce LIVE médium | **282** |
| Existujúca mediálna kategória | **32** |
| Obrázok na porovnanie | `/images/branch-p05-autotest/proof.png` |
| Existujúce PDF | `/files/branch-p05-autotest/proof.pdf` |

Manifest je `/tmp/webjet-p05-manual.properties`. Lokálny účet je `tester-p05`; heslo je iba v `/tmp/webjet-p05-login.properties`, nepatrí do Git ani do tohto dokumentu. Červený zdroj na nahradenie je `/tmp/webjet-p05-ui/proof.png`, zelený CKEditor zdroj `/tmp/webjet-p05-ui/ckeditor-autotest.png`.

## Vykonané overenia

| Overenie | Výsledok |
| --- | --- |
| Spoločná Java sada | **135 celkom, 123 PASS, 0 zlyhaní/chýb, 12 zámerne vypnutých scenárov** (jednorazové fixture, reštartové a osobitné P01 experimenty). Všetkých 7 `BranchFilesTest`, 4 `BranchMediaEditorTest` a 11 `BranchFilterTest` prešlo. XML v `build/test-results/p05/java-final/`, log `java-final.log`. |
| Frontend | Produkčný build prešiel; 2 existujúce upozornenia na veľkosť bundle. `build/test-results/p05/frontend.log`. |
| Skutočné UI | **7/7 PASS v jednom finálnom CodeceptJS/Playwright behu.** Moderný upload/Prepísať; elFinder mkdir/reopen/upload/F2 rename/cut/paste/delete; CKEditor drop a SAVE s logickými URL; mediálny CRUD; súborový DELETE; serverové zákazy/stale kontext; obnovenie obrázka na review. Log `build/test-results/p05/browser-final.log`, screenshoty `browser-final/`. |
| Obrázok a thumbnail A/B/LIVE | Pred uploadom všetky verzie modré. Po nahradení má A presné červené bajty a iný thumbnail; B aj LIVE sú bajtovo zhodné s pôvodným stavom. Po DELETE má A originál aj thumbnail **404**, B a LIVE stále **200** a pôvodné hashe. Na konci je A cez UI obnovená na červenú. Dôkazy `ui-assets.json`, `ui-delete.json`, `review-assets.json`. |
| CKEditor | Reálny drop nového PNG, úspešný upload a zobrazenie cez gateway. `getData()` a následný SAVE používajú logické `/images/...` URL; prefix sa neuložil. `ckeditor.json`. |
| Médiá / Novinky | Médium **282** starej stránky sa zmenilo bez SAVE stránky; skutočný News render ukázal pracovný titulok, LIVE pôvodný. Na WORK-only stránke sa moderným uploadom nahralo nové PDF do `/files/...`, médium sa pridalo, upravilo a zobrazilo v News, potom zmazalo. Nové médium **383** nemá LIVE riadok; po delete nezostal ani jeho WORK riadok. `new-media.json` a UI log. |
| Serverové hranice | Moderný aj elFinder upload do galérie/variantov odmietnutý. Archive/extract/duplicate/put, directory rename/delete a copy odmietnuté. Zastaraný moderný upload, médium aj elFinder po prepnutí A→B vracajú **409**; anonymný asset preview **403**. Staré per-page plánované úložisko tiež odmietne zápis; posledný cielený browser beh **1/1 PASS** je v `boundary-final.log`. CKEditor zákaz galérie, typ, práva, CSRF a skutočná veľkosť sa kontrolujú aj v integračných testoch. `server-boundaries.json`. |
| Kategórie / cutoff | Integračné testy overili kategórie pred count/paging, stabilné ID, poradie vrátane 0, práva, stale hash a zachovanie skrytého budúceho LIVE média pri no-op. Žiadne produkčné väzby sa nemenili. |
| Súborové konflikty | Jednorazový `BranchP05ManualTest.prepareRetainedFileConflicts` **1/1 PASS**. Tri pripravené konflikty content/new/delete, bez voľby výsledku a bez merge. `conflicts.properties`. |

Samostatný modul Súbory nebol v tomto izolovanom OSS profile dostupný. Obe nahrávania aj elFinder sa overili cez skutočný súborový dialóg bežného editora. Nový moderný upload používa rovnaký komponent `AdminUpload` aj v samostatnom správcovi; samostatná stránka na inom licencovanom profile sa tým nevykazuje ako preklikaná.

### Izolácia a zachované dáta

Obsah jedenástich LIVE tabuliek `documents`, `groups`, `documents_history`, `perex_group_doc`, `media`, `media_group_to_media`, `file_atr`, `dirprop`, `gallery`, `url_redirect`, `file_history` je pred/po UI zhodný. SHA-256 obsahových INSERT riadkov: **`7a49269afbff8c36b98f089fde79ef557a48292909968256eff9eec797029df3`**. Dôkazy `before-ui-content.sql`/`after-ui-content.sql`; pôvodné dumpy uchovávajú aj schému. Posun rezervovaných AUTO_INCREMENT čítačov je očakávaný a nie je obsahovým zápisom.

Stará stránka **1941626** má stále `author_id=1`, `date_created=2020-01-01 13:00:00` v tomto profile (`ORDER_SAVE_DATE` používa `date_created`) a **0 WORK riadkov**. Produkčné médium **282** má pôvodný titulok, poradie **10** a väzbu na kategóriu **32**. Nová stránka **1941629** ani médium **383** nemajú LIVE riadok. Tieto kontroly sú v `profile-and-identities.txt`.

Obsah všetkých štyroch pracovných tabuliek demo vetvy **P04 #481** je zhodný so vstupným snapshotom, SHA-256 **`6a6c5fb20bba675c0d3957250cdb40b858064d68afd0eb3d95f0b5227a682723`**. Nezmenilo sa **13 Lucene súborov**, fyzické produkčné PNG ani PDF; po negatívnych testoch nevznikol galerijný testovací adresár. `verification.json`, `lucene-before.json`/`lucene-after.json` a P04 dumpy obsahujú dôkazy. SQL súbory majú lokálne práva 600. Pracovná DB 3306 sa nepoužila.

T02/T05/T07/T09 a mediálne/súborové N07/N08 sú splnené v P05 rozsahu na uvedenom profile. Publikovanie a porovnanie po merge zostávajú P06. Pred odovzdaním prešiel vlastný diff a nezávislá kontrola súborovej časti; opravila aj FILE→DIRECTORY kolíziu po DELETE a doplnila jej regresie.

Server **8086/8446** a izolovaná DB **33316** zostávajú spustené na review. Zdrojové feature flagy zostávajú predvolene vypnuté. Finálne reporty sú z posledných úspešných behov; priebežné neúspešné behy sa nevykazujú ako splnené testy.

## Ako si výsledok vyskúšať

Prihlásiť sa na [admin login](http://iwcm.interway.sk:8086/admin/logon/) účtom `tester-p05`, v doméne `iwcm.interway.sk` vybrať **autotest P05 media/files A (#841)**.

- [Editor staršej stránky #1941626](http://iwcm.interway.sk:8086/admin/v9/webpages/web-pages-list/?docid=1941626)
- [Editor stránky iba vo WORK #1941629](http://iwcm.interway.sk:8086/admin/v9/webpages/web-pages-list/?docid=1941629)
- [Náhľad A so skutočnými Novinkami a médiami](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/841/p05-autotest/)
- [Náhľad B](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/842/p05-autotest/)
- [Verejná stránka](http://iwcm.interway.sk:8086/p05-autotest/)
- [Obrázok A](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/841/images/branch-p05-autotest/proof.png), [obrázok B](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/842/images/branch-p05-autotest/proof.png), [verejný obrázok](http://iwcm.interway.sk:8086/images/branch-p05-autotest/proof.png)
- [Thumbnail A](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/841/thumb/images/branch-p05-autotest/proof.png?w=16&h=16), [thumbnail B](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/842/thumb/images/branch-p05-autotest/proof.png?w=16&h=16), [verejný thumbnail](http://iwcm.interway.sk:8086/thumb/images/branch-p05-autotest/proof.png?w=16&h=16)

**Nahradenie obrázka:** V editore starej stránky otvoriť kartu Perex a tlačidlom pri poli obrázka otvoriť súborový picker. Prejsť cez „Všetky súbory“ do `/images/branch-p05-autotest`. Použiť „Nahrať súbor“, vybrať zelený `/tmp/webjet-p05-ui/green/proof.png` alebo vlastný obrázok s rovnakým názvom a pri konflikte stlačiť „Prepísať“. Stránku netreba uložiť; zavrieť picker a editor zrušiť. Obnoviť náhľad A a jeho thumbnail. B a verejná URL majú stále pôvodný modrý obrázok. Pri odovzdaní je A červená; pôvodný modrý zdroj je aj v `/tmp/webjet-p05-ui/blue/proof.png`.

**Úprava média bez uloženia stránky:** Otvoriť staršiu stránku #1941626, kartu Médiá a existujúce médium #282. Zmeniť titulok, prípadne poradie alebo priradenie k existujúcej kategórii, a uložiť iba vnorený formulár média. Hlavný editor stránky zrušiť bez uloženia. Obnoviť náhľad A: titulok média sa zmení aj vo vykreslených Novinkách. Verejná stránka a vetva B používajú pôvodné médium; autor a SAVE_DATE stránky zostávajú rovnaké. Rovnaký mediálny formulár možno použiť na WORK-only stránke #1941629; nové PDF má smerovať na výslednú logickú cestu, napríklad `/files/branch-p05-autotest/proof.pdf`.

## Opakovanie overení

Pred spustením overiť, či DB 33316 a WebJET 8086 už bežia. Java testy spúšťať pri zastavenom plnom app serveri; zapisovacie behy nad týmto profilom sekvenčne. [P04 výsledky](P04-vysledky.md#opakovanie-testov-a-štart) obsahujú aktuálne príkazy štartu/stopu. Nepoužiť port alebo socket pracovnej DB 3306.

Z koreňa worktree:

```sh
export JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem
WEBJET_P05_DB=true ./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchFilesTest' \
  --tests 'sk.iway.iwcm.branch.BranchMediaEditorTest'
```

Browser scenáre zo `src/test/webapp`:

```sh
WEBJET_P05_BROWSER=true CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branch-p05.js --steps
```

Na izolovanej DB možno pred opakovaním zmazať iba tento testovací WORK prepis príkazom `DELETE FROM branch_files WHERE branch_id=841 AND logical_path='/images/branch-p05-autotest/proof.png';` a zvýšiť `branches.revision` pre #841. Použiť výhradne vyššie uvedený socket/DB a zatvorené testovacie formuláre. Celý browser beh začína pôvodným obrázkom v A; pred opakovaným meraním treba vrátiť iba vlastný WORK prepis `/images/branch-p05-autotest/proof.png` (testovacia vetva 841), inak prvé porovnanie meria tú istú červenú verziu. Končí obnoveným červeným obrázkom. Retained fixture sa neregeneruje pri každom behu; test odmietne existujúce meno alebo cieľové súbory. Konfliktová fixture je tiež jednorazová a nesmie sa opakovane spustiť nad už pripravenými cestami.

## Hranice a odovzdanie P06

- Overený je uvedený MariaDB/InnoDB profil, jedna inštancia, lokálny filesystem a reprezentatívna šablóna. Platia ostatné [P03 hranice](P03-vysledky.md#hranice-a-odovzdanie-p04p05): iné DB/topológie, IwcmFsDB, externé statické úložiská/CDN a ľubovoľný zákaznícky kód sa tým nestávajú podporenými.
- Moderný upload podporuje chunk tok; legacy elFinder upload vo vetve prijíma jeden súbor bez chunkovania. Jeho chunk/directory upload a automatické konfliktové renames sa odmietnu s odkazom na moderný upload.
- Podporované sú súbory pod `/images` a `/files`. Galérie, archív súborov, staré per-page plánované úložisko, súborové a adresárové metadáta a celé adresárové operácie zostávajú v opísanom režime iba na čítanie alebo odmietnuté. Nevzniká skener použití súborov naprieč doménami.
- P06 musí vykonať konfliktové rozhodnutia, merge pod konečnými ID, zápis mediálnych väzieb a porovnanie výsledku po publikovaní. P05 pripravuje podklady; nevykonáva publikovanie ani scheduler.
- Pracoval hlavný agent a najviac dvaja pomocníci s rovnakým zdedeným nastavením. Presný identifikátor modelu/reasoning je neoverený. Súborovú implementáciu kontroloval aj druhý pomocník bez úprav rovnakých súborov. Žiadny Git commit ani produkčné nasadenie sa nevykonalo.
