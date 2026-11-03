# Rozširovanie vetiev o ďalšie moduly

**Stav: architektonické odporúčanie, bez implementácie.** Doplnené 2. novembra 2026 na požiadavku posúdiť galériu, bannery a rast počtu pracovných tabuliek. Používateľ následne zúžil príklad bannera na vytvorenie nového a úpravu názvu, popisu a obrázka; riešenie zapisovania štatistík sa nepožaduje. Tento dokument nepridáva moduly do pôvodného rozsahu P01–P08.

## Odporúčanie

**Nepokračovať pravidlom „ďalší modul = ďalšia pracovná tabuľka“.** Ponechať typované úložiská jadra a pre ďalšie bežné redakčné moduly použiť jednu spoločnú tabuľku `branch_items` s explicitnými JSON snapshotmi. Modul dodá adaptér svojho čítania, pracovného uloženia a publikovania. Porovnanie, konflikty, plánovanie, stav, prehľad zmien a pokračovanie po chybe zostanú spoločné.

V prvej implementácii ponechať päť tabuliek z [02](02-datovy-model.md). Koordinátor a UI použijú spoločnú identitu a obal zmien dnešných štyroch typov. Podľa [priority POC](ORCHESTRACIA.md#záväzná-priorita-jednoduchý-a-čitateľný-poc) stačia priame volania konkrétnych služieb, bez prípravného plugin frameworku. Spoločnú šiestu tabuľku vytvoriť až s prvým ďalším modulom a overiť ju na reálnom bannere. Galéria a ďalší podobný modul potom využijú rovnakú tabuľku.

Tento smer umožní napríklad **päť tabuliek pre jadro, šesť spolu po doplnení bannerov aj galérie**. Nie je to sľub, že žiadny budúci modul nikdy nebude potrebovať ďalšiu SQL projekciu. Taká výnimka musí mať konkrétny dôvod v jeho filtrovaní a objeme dát, nie iba nový názov aplikácie.

## Kde skutočne vzniká práca

Treba oddeliť tri problémy:

| Problém | Čo sa dá zdieľať | Čo zostane modulové |
| --- | --- | --- |
| Uloženie pracovného obsahu | Tabuľka zmien, BASE/WORK/resolved snapshot, hashe, operácia a checkpoint. | Explicitný obsahový DTO a význam jeho polí. |
| Čítanie náhľadu | Overený branch kontext, súbory, URL helper, základné prekrytie podľa ID. | Zdroj kandidátov, filtre, radenie, count/paging, renderer a cache modulu. |
| Publikovanie | Konflikt celého objektu, termín, práva koordinátora, priebeh a obnova. | Validácia konkrétneho obsahu, konečné ID, databázový zápis, väzby a potrebné následky. |

Jedna JSON tabuľka vyrieši rast pracovných schém. Nezmení automaticky existujúce JPA repository, priame SQL alebo súborové zápisy. Tieto integračné miesta by bolo potrebné upraviť aj pri typovanej tabuľke. Nevyhnutná práca na module sa preto nemá odhadovať podľa počtu nových tabuliek.

## Porovnanie alternatív

| Riešenie | Prínos | Cena a obmedzenie | Záver pre WebJET |
| --- | --- | --- | --- |
| Typovaná pracovná kópia každej produkčnej tabuľky | Prirodzené SQL a známe typy stĺpcov. | Opakované migrácie/mapovanie; kopíruje fyzické tabuľky aj tam, kde používateľ mení jeden malý celok. | Použiť tam, kde to vyžadujú konkrétne query; nie ako univerzálne pravidlo. |
| Jedna JSON tabuľka pre úplne všetko | Minimum pracovných schém, spoločný zápis. | Existujúce SQL nevie bežne filtrovať pracovné JSON rovnako ako `documents`. Treba DB špecifické výrazy alebo preniesť filtre a veľké množiny do Java. | Neodporúčané pre povinné News filtre/count/paging. |
| Typované jadro + spoločné JSON položky modulov | Zachová SQL jadra, ďalšie bežné moduly nepridávajú pracovné tabuľky. | Dva úložiskové adaptéry, ale jeden životný cyklus. Modulové čítanie zostáva potrebné. | Odporúčaná cesta. |
| Spoločný JSON obsah + generované typované projekcie | Jednotný zdroj pracovných zmien, projekcie pre vyhľadávanie. | Synchronizácia dvoch reprezentácií, obnova projekcií a ich migrácie. Pre správne čítanie stále môžu pribúdať tabuľky. | Zvažovať až pri potrebe spoločnej obsahovej platformy; dnes zbytočný medzikrok. |
| `branch_id` priamo vo všetkých produkčných tabuľkách | Dáta vetiev sú dostupné rovnakému SQL modelu. | Zmena identity riadkov, joins, unikátností a všetkých čítacích/zapisovacích ciest. Neadaptovaný dotaz môže vrátiť pracovný obsah produkcii. | Podstatne väčší zásah do dnešného WebJET; nevyrieši automaticky súbory ani vedľajšie účinky. |
| Kópia DB a súborov pre vetvu | Izolované prostredie prirodzene obsahuje viac modulov. | Prepínanie datasource/úložiska, cache a integrácií; nové ID a selektívny merge aj tak treba vyriešiť. Kópia nezodpovedá potvrdenému živému overlay. | Vhodná na testovacie prostredie, nie na tento redakčný balík. |

Neskoršie zjednotenie `groups` a `media` do JSON je technicky možné, ale samo neprinesie automatické moduly. Pri priečinkoch stačí graf v pamäti; médiá navyše potrebujú filtrované zoznamy a kategorizáciu. Teraz ich presúvať iba kvôli počtu tabuliek neprináša dostatočný úžitok. Rozhodujúce je, aby nad nimi nebol samostatný merge/UI engine. Dokumenty ponechať typované kvôli [Novinkám](03a-novinky.md).

## Čo pripraviť už v P01–P08

Položka dostane vnútornú identitu `type + objectKey`, napríklad `document:123`, `group:45`, `media:900`, neskôr `banner:810` alebo `gallery.image:911`. Súbor ponechá dnešnú normalizovanú logickú cestu a `path_key`. Identita balíka je samostatná. Typ nie je názov SQL tabuľky ani Java trieda poslaná klientom.

**Pre POC stačí enum typu a explicitné volania konkrétnych služieb.** Spoločný `BranchChange` nesie údaje pre porovnanie a UI; nevyžaduje hierarchiu generických tried. Nasledujúca tabuľka rozdeľuje zodpovednosti, nepredpisuje samostatný interface a implementáciu pre každú z nich:

| Zodpovednosť typu | Kontrakt |
| --- | --- |
| Pracovné položky | Načíta vlastné zmeny z existujúcej typovanej tabuľky alebo neskôr zo spoločného úložiska; vráti spoločný obal `BranchChange`. |
| Snapshot | Pozná podporované polia, kanonizáciu a načítanie aktuálneho LIVE obsahu. Nečíta produkciu cez aktívny branch fallback. |
| Validácia | Overí obsah, pôvodný/cieľový objekt, používateľa, rozsah a konkrétne referencie. Samotné oprávnenie otvoriť balík nestačí. |
| Zápis DB | Použije prijatú `Connection` a zvolený obsah. Nevlastní samostatný commit; audit a checkpoint zostanú v transakcii položky. |
| Dokončenie a UI | Oznámi potrebnú invalidáciu cache, názov/odkaz položky a popisy podporovaných polí pre spoločný diff. |

Koordinátor zostáva vlastníkom B/L/W porovnania, rozhodnutia LIVE/WORK, stavu vetvy, termínu a evidencie vykonania. Navrhovaný `ContentRowWriter` je nízka zapisovacia vrstva jadra; konkrétne služby ju môžu použiť. Súborové kroky zostávajú v súborovej službe a **nevkladajú sa do DB transakcie** len preto, že majú rovnaký obal položky.

Malý `BranchTypeHandler` možno vyčleniť až pri E01/E02, ak reálne zjednoduší zapojenie prvého ďalšieho modulu. Ani vtedy nie je Spring register povinný; postačí explicitné priradenie podporovaných typov k službám. Pojem „handler“ nižšie označuje obsluhu konkrétneho typu, nie požiadavku na vlastný framework. Spoločný lifecycle a `type + objectKey` umožnia toto rozdelenie neskôr bez zmeny pracovných dát.

Nie je potrebný univerzálny jazyk filtrov, automatický preklad JPA, reflexívne kopírovanie entity ani dynamické spúšťanie kódu z JSON. Neznámy/nedostupný typ existujúcej pracovnej položky zastaví prípravu publikovania s vysvetlením; položka sa nepreskočí.

Čítanie zostáva samostatnou zodpovednosťou modulu: napríklad budúci `BannerBranchAdapter` sa zapojí do `BannerDB` a administrácie. Publikačný handler nie je transparentnou náhradou všetkých modulových query. Adaptér náhľadu musí podľa Q16 rešpektovať spoločnú mapu zvolených LIVE/WORK výsledkov; editor uchováva WORK. Rovný cieľ sa normalizuje na LIVE/no-op a zastaraná voľba vyžiada nové porovnanie. Spoločné stĺpce zahŕňajú aj `resolved_choice`, nie iba snapshot výsledku.

**Jeden typ má práve jedno autoritatívne pracovné úložisko.** Dnešný dokument nebude súčasne uložený v `branch_documents` aj v `branch_items`. Spoločný obal je Java pohľad, nie ďalší povinný DB riadok. Ak sa niektorý typ neskôr presunie do iného storage, potrebuje jednorazovú migráciu existujúcich zmien, nie trvalý dvojitý zápis.

## Jedna budúca tabuľka `branch_items`

Tabuľka preberie spoločné stĺpce zo sekcie „Spoločné pracovné stĺpce“ v [02](02-datovy-model.md): `row_id`, `branch_id`, operáciu, základ, hashe, metadáta úpravy, vyriešený výsledok a checkpointy. Navyše:

| Stĺpec | Logický typ | Účel |
| --- | --- | --- |
| `item_type` | VARCHAR(64), NOT NULL | Explicitne podporovaný stabilný typ, napr. `banner`, `gallery.image`. |
| `object_key` | VARCHAR(128), NOT NULL | Kanonická identita objektu; pre tieto dva moduly desiatkový zápis konečného ID. Dlhé súborové cesty patria existujúcej súborovej službe. |
| `schema_version` | INT, NOT NULL | Verzia explicitného formátu všetkých obsahových snapshotov položky. |
| `work_snapshot` | TEXT/CLOB | Pracovný obsahový DTO ako JSON; DELETE vyjadruje operácia, nie náhodný NULL poľa. |

Jedinečnosť `(branch_id, item_type, object_key)`; index `(branch_id, item_type)`. ID typu a objektu sú krátke interné hodnoty; fyzické typy/indexy sa overia na podporovaných DB. Na vnútorné JSON polia sa v prvom rozsahu nerobia SQL filtre ani indexy. Kľúč sa porovnáva ako identita, nie ako číselné poradie.

Príklad obsahového banneru, ilustračný JSON, nie serializácia `BannerBean`:

```json
{
  "name": "Letná ponuka",
  "primaryHeader": "Víkend pri vode",
  "secondaryHeader": "Ponuka na jún",
  "descriptionText": "<p>Ubytovanie s raňajkami.</p>",
  "imageLink": "/images/kampane/leto.jpg",
  "imageLinkMobile": "/images/kampane/leto-mobile.jpg"
}
```

Pri novom bannere snapshot obsahuje aj minimum potrebné na vytvorenie, napríklad typ, existujúcu skupinu/umiestnenie a hodnoty inicializované podľa bežného editora. Odkaz na súbor je logická cesta; bajty sú v `branch_files` a existujúcom neverejnom úložisku. Pri zmene toho istého obrázka v editore súborov a v module sa používa rovnaká súborová položka, nie dve nezávislé pracovné kópie.

Formát patrí konkrétnemu codec/mapperu typu. Poradie JSON kľúčov a množinových zoznamov ID nesmie meniť hash; pri zozname s obsahovo významným poradím sa poradie zachová. Pri kompatibilnom rozšírení použije mapper známu predvolenú hodnotu; pri zmene významu alebo premenovaní polí migruje BASE/WORK/resolved spolu a zneplatní staré vyriešenie konfliktu. Neznáma verzia dá chybu. Otvorená vetva po upgrade nesmie ticho stratiť pole. Nie je potrebná všeobecná revízna databáza.

Jednotkou je používateľsky zmysluplný objekt. Ak sa neskôr povolí editácia obmedzení bannera, zoradené ID stránok/priečinkov z `banner_doc`/`banner_gr` môžu byť v tom istom snapshote. Nevznikajú preto ďalšie dve pracovné tabuľky. Jeden obrovský JSON celej galérie by naopak zbytočne robil konflikt pri úprave dvoch rôznych obrázkov; galéria použije položku na obrázok.

## Pravidlo pre query: najprv výsledná množina, potom filter

Pre malé redakčné kolekcie stačí tento postup:

1. Načítať úplnú vhodne ohraničenú LIVE množinu, napríklad bannery aktuálneho modulového rozsahu alebo obrázky vybraného albumu. Výsledky sú oddelené od spravovaných entít/cache.
2. Načítať zmeny daného typu vo vetve. Pracovné DELETE odstráni ID, UPSERT vloží/nahradí podporované hodnoty. Zohľadniť aj presuny do/z vybranej skupiny alebo albumu.
3. Použiť výsledný rozsah, práva, filtre a poradie; až potom počet a stránkovanie/výber bannera.

Prvá produkčná strana alebo už filtrovaný bannerový zoznam nestačia: pracovná úprava môže dostať objekt do výsledku, hoci LIVE filtru nevyhovoval. Načítanie celej kolekcie nie je vhodné pre neobmedzené celowebové vyhľadávanie fotografií; prvý galerijný adaptér preto pracuje s vybraným albumom. P01/prvý modul zmeria reálny rozsah dát. Počet redaktorov nehovorí, koľko obrázkov musí query spracovať.

Ak neskôr modul potrebuje veľké SQL filtrované zoznamy, najskôr spresniť jeho query. Typovaná projekcia je možná až ako odôvodnená výnimka. Namiesto tichého orezania výsledku sa nepodporovaný rozsah jasne obmedzí alebo dopracuje. Spoločný storage nemá sľubovať bezplatnú podporu ľubovoľného SQL.

## Prvý príklad: základný banner

**Rozsah podľa posledného pokynu používateľa:** vytvoriť nový banner; upraviť názov, popis a obrázok existujúceho; vidieť ho vo vetve a publikovať pod rovnakým ID. Štatistiky sa nevetvujú a ich zapisovanie sa v tomto rozšírení neupravuje. Existujúce počítadlá sa pri merge iba neprepíšu zo snapshotu; nová izolácia štatistík ani testy nulových preview štatistík nie sú podmienkou dokončenia.

| Dnešné miesto | Potrebné zapojenie |
| --- | --- |
| [BannerBean](../../../src/main/java/sk/iway/iwcm/components/banner/model/BannerBean.java) | Obrázkový typ 1: `name`, `bannerLocation`; nemá samostatný vykresľovaný popis. Obsahový typ 4: `primaryHeader`, `secondaryHeader`, `descriptionText`, `imageLink`, `imageLinkMobile`. Použiť skutočné polia a dnešnú sanitizáciu, nevymýšľať univerzálne `title`/`description`. |
| ID `WJGen_banner_banners` | Použiť striktnú vetvu spoločného `PkeyGenerator` podľa 02a, pre kľúč `banner_banners`; merge vloží riadok s rezervovaným ID. |
| [BannerRestController](../../../src/main/java/sk/iway/iwcm/components/banner/BannerRestController.java) | Detail, nový/uložený riadok a zoznam prepnúť na adaptér. Vetvový SAVE nevolá produkčné repository. Existujúce pokročilé nastavenia a obmedzenia zostanú v prvom rozsahu iba na čítanie; nepublikovať ich starú kópiu. |
| [admin/index.html](../../../src/main/webapp/apps/banner/admin/index.html) | Už používa `serverSide:false`; spoločná výsledná množina sa hodí k dnešnému klientskemu filtrovaniu. Zachovať branch header a ochranu starej záložky. |
| [BannerDB](../../../src/main/java/sk/iway/iwcm/components/banner/BannerDB.java): `getBanners`, `getBanner` | Zostaviť výsledné kandidáty pred výberom, použiť existujúce pravidlá umiestnenia. Oddeliť pracovné objekty od spoločnej cache/JPA entít; žiadna potreba univerzálneho query engine. |
| [banner.jsp](../../../src/main/webapp/components/banner/banner.jsp) | Zachovať bežné serverové vykreslenie typov 1 a 4. Obrázky a CSS thumbnail URL obsahového typu viesť cez existujúci preview helper, aby zobrazili pracovné súbory. |

`BannerDB` dnes používa spoločnú cache a niektoré filtre menia odovzdaný zoznam. Pre vetvu stačí request kópia a obídenie výsledkovej cache; nepridáva sa cache framework. Nepodporované AJAX/iframe varianty sa v prvom inventári podpory označia; základné zadanie nevyžaduje prestavať všetky historické spôsoby vloženia bannera.

Pri existujúcom bannere sa pracovný snapshot a konflikt týkajú podporovaných redakčných polí. Pokročilé kritériá, obmedzenia a prevádzkové počítadlá sa čítajú z dnešnej produkcie a merge ich zachová. Nový banner dostane platné predvolené nastavenia a vybrané existujúce umiestnenie. Vlastné rozširovanie kampaní, kvót, rotácie alebo historických štatistík nie je súčasťou tohto kroku. Ak v umiestnení prebieha náhodný výber viacerých bannerov, náhľad sám negarantuje výber rovnakého kandidáta pri každom obnovení; na základné overenie stačí jednoznačné testovacie umiestnenie.

Práva zostávajú `menuBanner`, prípadné obmedzenie `cmp_banner_seeall`/`clientId` a dnešný doménový rozsah modulu. Numerické `domain_id` nemožno automaticky považovať za vlastníctvo jednej webovej domény. Ak sú bannery v danom nasadení zdieľaným redakčným zdrojom, adaptér to pomenuje v UI a prehľade dosahu; nevzniká nový register všetkých použití banneru.

**Dôkaz dokončenia:** nový banner s konečným ID; zmena obsahu a obrázka iba vo vetve; rozdiel medzi LIVE a preview; konflikt pri odlišnej produkčnej zmene podporovaného poľa; po merge rovnaké ID a vybraný obsah. Pracovné obrázky využijú existujúce súborové testy. Zápisy štatistík nie sú predmetom tejto akceptácie. Nadviazať na [banner.js](../../../src/test/webapp/tests/apps/banner/banner.js) a [app-banner.js](../../../src/test/webapp/tests/apps/banner/app-banner.js).

## Druhý príklad: obrázky galérie

Podľa Q18 sú galerijné cesty v P01–P08 iba na čítanie. Až toto rozšírenie povolí podporované spoločné úpravy metadát, originálu a variantov; obyčajný upload ani elFinder zákaz dovtedy neobídu.

Jedna položka `gallery.image:<image_id>` obsahuje podporované metadáta obrázka, napríklad názov/cestu, jazykové popisy, autora, poradie a odkazy na existujúce kategórie. Bajty originálu a variantov patria spoločnému súborovému úložisku. `send_count` sa nevracia zo snapshotu do produkcie. Kategórie sú priamo v `gallery.perex_group`; nový všeobecný model kategórií nie je potrebný.

Odporúčaný prvý rozsah je pridať obrázok a upraviť popis/obrázok v **existujúcom nakonfigurovanom albume**. `gallery_dimension` obsahuje aj názov, perex, autora a dátum albumu, nielen rozmery. Pri tomto úzkom rozsahu ostane read-only; tvorba/presun albumov, hromadná regenerácia a ďalšie transformácie nie sú automaticky zahrnuté.

| Dnešné miesto | Dôsledok pre budúci adaptér |
| --- | --- |
| [GalleryDB](../../../src/main/java/sk/iway/iwcm/gallery/GalleryDB.java): `getImages`, `getGalleryBeanTable`, `getFilesFromDir`, `convertFilesToGbean` | Verejný zoznam skladá DB metadáta a filesystem, potom filtruje/radí/stránkuje. Prekryť obidva vstupy pred týmto výpočtom. Samotný pracovný DB záznam nový obrázok nezobrazí. |
| `getImageByID`, `getGalleryBean`, prev/next a count helpery | Detail a navigácia musia používať rovnaké účinné metadáta a súbory ako zoznam. Deklarovať podporované core renderery. |
| [GalleryRestController](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryRestController.java) | JPA stránkovanie nahradiť vo vetve zložením celého vybraného albumu pred filtrom/paging. Podporované save operácie nesmú automaticky presúvať/mazať produkčné súbory. |
| [GalleryService](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryService.java), [UploadService](../../../src/main/java/sk/iway/iwcm/admin/upload/UploadService.java) | Upload vytvára metadáta aj mimo hlavného controllera. Musí volať rovnaký pracovný adaptér a súborovú službu. |
| `GalleryDB.resizePicture` | Nie je čistý generátor bajtov: volá aj `setImage`. Oddeliť generovanie pracovných súborov od explicitného uloženia metadát. Samotná zmena fyzickej cesty nestačí. |
| [GalleryActionBean](../../../src/main/java/sk/iway/iwcm/gallery/GalleryActionBean.java) | Render môže vytvárať adresár. Pri preview musí pracovať s účinným úložiskom bez založenia produkčného obsahu. |
| `checkAndCreateGallery`, [GalleryTreeService](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryTreeService.java) | V prvom rozsahu nezakladať skryto albumové riadky, redirecty ani prepisy ostatných stránok. Neskorší albumový typ môže použiť tú istú všeobecnú tabuľku, nie ďalšiu pracovnú schému. |

Galéria má konkrétnu dodatočnú cenu: [GalleryEntity](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryEntity.java) používa `WJGen_gallery`, ale v `GalleryDB` existujú aj natívne INSERT bez `image_id`. Rezervácia cez samotný `PkeyGenerator` preto nie je dostatočne preukázaná. Pred povolením nových pracovných obrázkov treba zjednotiť relevantné LIVE aj branch tvorby na jednom alokátore a overiť súbeh s bežným uploadom. Oprava patrí galérii; nepridáva sa preventívne do prvej verzie stránok.

Podľa použitého resize režimu sa dnes pracuje s originálom `o_...`, hlavným a malým `s_...` súborom; nie vždy existujú všetky tri. Modul pripraví iba skutočne potrebné položky a ich závislosti. Prehľad ich môže zoskupiť pod obrázok, ale porovnanie a obnova súborov ostávajú spoločné. Odobratie obrázka z balíka skontroluje jeho pracovné súbory: nezmaže súbor používaný inou ponechanou položkou a pri čisto pomocných variantoch ponúkne aj ich odobratie.

**Dôkaz dokončenia úzkeho rozsahu:** nový obrázok je v pracovnom zozname/count aj detaile; produkcia má pôvodné metadáta a bajty; merge zachová ID, popisy a obrázky; nevzniknú neplánované albumové/redirect zmeny. Crop, rotate, duplikácia, presun a albumový lifecycle sa pridávajú až výslovným rozšírením rozsahu.

## Postup zapojenia ďalšieho modulu

| Krok | Práca | Dokončené, keď |
| --- | --- | --- |
| 1. Rozsah | Pomenovať používateľské operácie, obsahový objekt, LIVE/read-only polia a podporované renderery. | Je jasné, čo znamená „modul funguje vo vetve“; názov modulu sám nestačí. |
| 2. Inventár | Vypísať tabuľky, ID generátory, UI/API zápisy, read query, súbory a skryté obsahové účinky. | Nezostala známa podporovaná operácia obchádzajúca pracovný zápis. |
| 3. DTO a storage | Vytvoriť mapper podporovaných polí, verziu snapshotu a explicitnú obsluhu typu; prednostne spoločná JSON tabuľka. | BASE/WORK/LIVE porovnanie zachová význam polí a nemení prevádzkové dáta. |
| 4. Čítanie | Napojenie detailu/zoznamu/renderu, efektívnych súborov a URL. | Nový objekt a zmena vstupujúca do filtra sa prejavia pred paging; LIVE cache sa nemení. |
| 5. Editácia | Kontext administrácie, práva, stable ID, pracovný save a jeho podporované súborové operácie. | Nový aj existujúci obsah zostáva izolovaný až do merge. |
| 6. Publikovanie | Handler zápisu a účinkov, validačné referencie, umiestnenie do pevného poradia merge, spoločný diff. | Konflikt, merge aj pokračovanie používajú existujúci koordinátor a zachovajú ID. |
| 7. Overenie | Príbeh redaktora v preview/LIVE, práva, konflikty, súbory a konkrétne modulu vlastné riziká. | Existuje report a inventár podpory; bez tvrdenia, že registrácia handlera automaticky pokryla všetky cesty. |

Pri prvom ďalšom module sa v kroku 3 jednorazovo vytvorí `branch_items` a spoločný repository/codec kontrakt. Ďalšie moduly pridajú DTO, handler/adaptér a testy. Nemajú opakovať scheduler, stavový automat, merge obrazovku ani vlastné úložisko príloh.

Poradie publikovania ostáva jednoduché: nové stránky/priečinky a potrebné súbory pred modulom, ktorý ich používa; modulové referencie sa upravia pred zmazaním ich cieľov. Zatiaľ stačí doplniť konkrétne miesto typu v pevnom pláne z dokumentu 04. Všeobecný grafový orchestrátor nie je potrebný. Obsahový cyklus, ktorý daný typ nevie vykonať týmto poradím, sa uvedie ako nepodporovaná operácia, nie ako dôvod vopred vybudovať workflow engine.

## Etapy, zadávanie a orientačná cena

| Rozsah | Kedy | Orientácia po dokončenom jadre |
| --- | --- | --- |
| Spoločná identita/obal a koordinátor s konkrétnymi službami typov | V P02/P06, s overením v P08. | Bez povinného handler rozhrania/DI registra. Nejde o implementáciu budúcich modulov. |
| E01: všeobecné úložisko prvého modulu | Až spolu s prvým konkrétnym rozšírením. | Približne 2–4 vývojárske dni na storage/mapper integráciu a relevantné DB testy. |
| E02: základný banner podľa zúženého rozsahu | Odporúčaný prvý používateľ E01. | Približne 3–6 dní navyše; bez úprav štatistík a prestavby všetkých spôsobov renderovania. |
| E03: obrázky v existujúcich albumoch | Po overení E01, podľa samostatného zadania. | Približne 5–10 dní; cena závisí od ID a oddelenia upload/resize zápisov. |
| Rozšírená galéria s crop/rotate/duplikáciou/presunmi | Samostatné rozhodnutie rozsahu. | Celková galerijná práca môže byť 10–20 dní namiesto úzkeho rozsahu; albumový lifecycle a globálne regenerácie treba naceniť osobitne. |

Ide o pracovné odhady pre vývojára poznajúceho WebJET, vrátane cieleného overenia a s hotovým branch jadrom/súbormi. Nie sú garanciou a nepripočítavajú sa automaticky k P01–P08. E01 sa platí raz; ďalšia rovnaká pracovná tabuľka sa pri E03 nevytvára. Pri spresnení P01 sa pôvodný odhad musí upraviť, ak overený spoločný kontrakt jeho rozsah významne zmení.

Príklad budúceho zadania agentovi po dokončení jadra:

```text
Implementuj E01 a E02 podľa docs/research/publish-packages/08-rozsirovanie-modulov.md.
Riaď sa ORCHESTRACIA.md a skontroluj skutočný stav jadra v STAV.md.
Rozsah: nové bannery, názov/popisy/obrázky existujúcich bannerov,
bežný serverový render typov 1 a 4 a spoločný merge. Štatistiky nemeníš.
Použi jednu spoločnú branch_items, nevytváraj bannerovú pracovnú tabuľku.
Dokonči overenia dohodnutého rozsahu, zapíš výsledky a skonči.
Bez Git commitov a bez zapnutia na produkcii.
```

Pre ďalší modul sa zmení typ a jeho konkrétne operácie; E01 sa neopakuje, ak už existuje a spĺňa kontrakt. Tento text je návod na neskoršie zadanie, nie pokyn vykonať ho teraz.
