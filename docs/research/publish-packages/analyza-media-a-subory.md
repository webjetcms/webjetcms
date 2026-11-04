# Médiá a súbory: podklady pre návrh publikačných balíkov

**Stav: overená analýza zdrojového kódu k 1. novembru 2026, bez implementácie.** Návrhy v tomto dokumente sú kandidáti na posúdenie. Nie sú schváleným technickým zadaním. Čísla riadkov sa úmyselne neviažu do odkazov; orientačný bod tvorí uvedená trieda a metóda.

Potvrdené hranice ovplyvňujúce tento dokument:

- **Q1:** živá overlay; nezmenené položky sa čítajú z aktuálnej produkcie.
- **Q2:** jedna branch pokrýva jednu doménu.
- **Q3:** postupné zverejnenie položiek, bez prísľubu atómového prepnutia celého výsledku.
- **Q4:** redakčné súbory; kód a šablóny sa v branch nemenia.

Potvrdené sú tiež **Q7**: dodatočné metadáta sa v branch neupravujú, existujúce členstvo médií v kategóriách sa verzionuje; a **Q8**: náhľad je dostupný iba prihláseným používateľom administrácie s príslušným prístupom. Technické názvy a integračné riešenia nižšie sú návrhy.

Finálna voľba jednoduchej politiky rieši konflikt celého objektu výberom produkcie alebo pracovnej verzie; polia sa automaticky nespájajú. Zdieľané systémové stránky ostávajú iba na čítanie. Súbory používajú existujúce mapovanie úložiska a oprávnenia; publikovanie spoločnej fyzickej cesty je povolené s výrazným upozornením na možný dosah na viac domén. Nevzniká nová evidencia vlastníctva súborov podľa domén ani skener ich použitia.

Doplnené potvrdené hranice: **Q17** povoľuje vytvorenie prázdneho súborového adresára a operácie s jednotlivými súbormi; presun/premenovanie/mazanie adresárov a ich rekurzívne kopírovanie sú mimo prvej verzie. **Q18** ponecháva galerijné cesty iba na čítanie až do samostatného rozšírenia E03, aj pri pokuse meniť ich cez správcu súborov.

## 1. Čo predstavuje tabuľka media

[Media](../../../src/main/java/sk/iway/spirit/model/Media.java) je samostatný záznam s názvami, odkazom na súbor, thumbnailom, poradím, textami a voliteľnými poľami. Samotné binárne súbory v nej nie sú.

| Vlastnosť | Overené správanie | Dôsledok pre branch |
| --- | --- | --- |
| Identita | PK `media_id`; `@TableGenerator` používa kľúč `media`. | Existujúce médium možno pri merge aktualizovať pod pôvodným ID. |
| Vlastník | `media_fk_table_name` a `media_fk_id`. | Médiá nemusia patriť stránkam; prvý rozsah musí určiť, či podporuje iba vlastníka `documents`. |
| Doména | Pole `domain_id`. | Doménový kontext sa musí zachovať pri čítaní aj merge; názov domény a toto číslo nie sú všeobecne zameniteľné. |
| Kategórie | M:N cez `media_group_to_media` na [MediaGroupBean](../../../src/main/java/sk/iway/spirit/model/MediaGroupBean.java), tabuľka `media_groups`. | Verzia média musí uchovať aj členstvo v kategóriách. Samotná kópia stĺpcov `media` nestačí. |
| Platnosť pri zobrazení | `lastUpdate` sa používa pri filtrovaní médií; verejné načítanie môže kontrolovať dostupnosť stránky. | Pri merge a preview nestačí iba vložiť záznam s aktuálnym časom. Treba zachovať zmysel týchto filtrov. |

Potvrdený rozsah verzuje záznam média spolu so zoznamom už existujúcich `media_group_id`. Správa samotných kategórií ostáva mimo branch.

### Stabilné ID

[WJGenSequence.getGeneratedValue](../../../src/main/java/sk/iway/iwcm/system/jpa/WJGenSequence.java) používa [PkeyGenerator](../../../src/main/java/sk/iway/iwcm/PkeyGenerator.java). Generátor je existujúci stavebný prvok na rezervovanie nového konečného ID bez vytvorenia verejného média. Pri implementácii treba overiť explicitné vloženie s prideleným ID cez zvolenú perzistenčnú cestu; samotná anotácia `@GeneratedValue` toto rozhodnutie nerieši.

Pri existujúcich médiách je prirodzený merge `UPDATE` podľa `media_id`; pri zmazaných `DELETE`, pri nových `INSERT` pod vopred prideleným ID. Je to kandidát zachovania identity, nie existujúca branch funkcionalita.

## 2. Čítanie a zápis médií má viac vstupov

| Miesto | Dnešný tok | Potrebný zásah, ak sa zvolí overlay |
| --- | --- | --- |
| [MediaRestController.getAllItems/getOneItem](../../../src/main/java/sk/iway/iwcm/components/media/MediaRestController.java) | Administrácia používa priamo `MediaRepository`; podporuje samostatný prehľad aj médiá stránky. | Branch zoznam, detail, filtrovanie a stránkovanie musia zohľadniť pracovné vloženia, úpravy a zmazania. |
| `MediaRestController.beforeSave` | Nastaví vlastníka, doménu, poradie, čas a prevedie editorové kategórie na entity. Uloženie pokračuje cez DataTable tok. | Validácia sa dá využiť, ale konečný zápis musí smerovať do branch. Nestačí zmeniť iba čítanie. |
| [MediaDB.getMedia(int)](../../../src/main/java/sk/iway/spirit/MediaDB.java) | Načíta entitu cez JPA podľa ID. | Musí rešpektovať branch pri priamom odkaze na médium. |
| `MediaDB.getMediaByMediaGroups` | EclipseLink query nad `Media`, kategóriami a vlastníkom; následne časové a dostupnostné filtre. Existuje osobitný per-page plánovaný náhľad. | Branch resolver musí zachovať filtre aj kategórie. Už existujúca planned vetva je orientačný bod, nie riešenie pre celý web. |
| `MediaDB.filterByAvailability` | Pri odkaze pod `/files` alebo `/archiv` overí existenciu cez produkčný `IwcmFile`; podľa režimu následne kontroluje ochranu adresára. | Pri podporovanom súbore musí existencia vychádzať z pracovného obsahu, tombstone alebo LIVE fallbacku. Inak sa nové pracovné PDF stratí zo zoznamu a pracovné zmazanie sa môže tváriť ako dostupný súbor. Práva sa naďalej overia pre logickú cestu. |
| [MediaService.assignDocIdToMedia/deleteTempMedia](../../../src/main/java/sk/iway/iwcm/editor/service/MediaService.java) | Médiá neuloženej stránky používa cez `documents_temp` a ID používateľa. | Dočasný kontext potrebuje rozlíšiť branch, aby rovnaký používateľ nespracoval médiá z iného rozpracovaného obsahu. |
| `MediaService.updateMediaLink` a [MediaRepository.updateMedia](../../../src/main/java/sk/iway/iwcm/components/media/MediaRepository.java) | Premenovanie odkazu môže urobiť hromadný update produkčných záznamov. | V branch sa musia meniť pracovné verzie príslušných médií. |

Pri mediálnej kategórii reprezentovanej v branch ako zoznam ID sa pri čítaní môže poskladať bežný dátový objekt pre existujúce zobrazenie. Nemal by sa tým zmeniť zdieľaný produkčný JPA objekt ani jeho lazy kolekcia. Presná podoba DTO/entity adaptéra patrí do následného návrhu perzistencie.

### Dátum média a viditeľnosť bez nového uloženia stránky

[MediaDB.getMedia(DocDetails)](../../../src/main/java/sk/iway/spirit/MediaDB.java) aj [media.jsp](../../../src/main/webapp/components/media/media.jsp) odovzdávajú dátum vlastníckej stránky. `filterByLastUpdateDate` ponechá médiá s prázdnym `lastUpdate` alebo s hodnotou najviac rovnou dátumu stránky. Vnorený `MediaRestController.beforeSave` však nastavuje čas na teraz. Ide o súčasný mechanizmus, ktorý môže skryť rozpracované médiá pred ďalším uložením stránky; nie je bezpečné zrušiť ho globálne alebo iba podľa toho, či ide o historický náhľad.

Samostatný per-page plánovaný náhľad už obchádza tento filter pre svoju plánovanú sadu. Jeho publisher `PlannedChangesService.publishMedia` nastaví každej publikovanej kópii `lastUpdate` na `new Date(doc.getDateCreated() / 1000 * 1000)`, aby nebola novšia než stránka ani pri sekundovej presnosti DB. Branch použije tento **dátumový vzor**, nie jeho postup mazania a kopírovania sady médií.

Kontrakt v [dátovom modeli](02-datovy-model.md#médiá): pri skutočne zmenenom `WORK` UPSERT sa technický `last_update` odvodí z výslednej zvolenej vlastníckej stránky; náhľad premietne rovnaké pravidlo nad vlastníkom zo svojho účinného pohľadu. Vlastnícka stránka môže zostať bez pracovnej zmeny a jej dátum/autor sa neposúva. Nezmenené položky, voľba `LIVE` a no-op zachovajú pôvodný marker aj filter. `updated_at` eviduje čas pracovnej úpravy; odvodený marker nie je samostatným obsahovým konfliktom. Overenie musí zahrnúť pridané aj upravené médium pri nezmenenej staršej stránke a zachovanie dnešného per-page plánovania.

### Plánované médiá sa dnes publikujú s novými ID

[PlannedChangesService.publishMedia](../../../src/main/java/sk/iway/iwcm/doc/PlannedChangesService.java) pri publikovaní zmaže pôvodnú produkčnú sadu a uloží kópie plánovaných médií. Jeho `copyMedia` výslovne vynecháva `id` a `mediaId`.

Tento postup **nemožno priamo prevziať na merge zachovávajúci media_id**. Použiteľné sú jednotlivé kontroly a pravidlá pre údaje; samotné nahradenie sady potrebuje nový tok podľa stabilnej identity.

## 3. Súbory nemajú jedinú bránu pre všetky operácie

[Tools.getRealPath](../../../src/main/java/sk/iway/iwcm/Tools.java) prevádza virtuálne cesty, rieši externé doménové adresáre, zdieľané úložisko a symlinky. Nerozlišuje účel čítania a zápisu.

Pri štandardnom spoločnom webroote môže `/images/logo.png` predstavovať ten istý fyzický súbor pre viac domén. Jedna doména branch preto neznamená výlučné vlastníctvo všetkých súborových ciest. Návrh zachová dnešné mapovanie a kontrolu práv k cieľovému priečinku. UI pri spoločnom úložisku upozorní, že publikovanie súboru môže ovplyvniť ďalšie domény; bez nového skenera nebude tvrdiť presný zoznam jeho použití.

[IwcmFile](../../../src/main/java/sk/iway/iwcm/io/IwcmFile.java) pracuje s filesystemom, databázovým úložiskom a JAR zdrojmi. `exists`, `listFiles`, `delete` a `renameTo` obsahujú konkrétne operácie backendu. [IwcmOutputStream](../../../src/main/java/sk/iway/iwcm/io/IwcmOutputStream.java) aj pri prijatí `IwcmFile` prechádza na `java.io.File(file.getPath())`. [IwcmFsDB](../../../src/main/java/sk/iway/iwcm/io/IwcmFsDB.java) poskytuje ďalšie zápisové a kopírovacie operácie.

**Zmena samotného `Tools.getRealPath` nepostačuje na bezpečnú overlay.** Čítanie môže použiť produkciu, ak pracovný súbor chýba. Zápis musí vždy skončiť v pracovnom úložisku. Zoznam adresára musí zlúčiť položky oboch vrstiev a odstrániť pracovné zmazania. Jedna fyzická cesta bez určenia operácie tieto tri pravidlá nevyjadrí.

| Vstup | Overené implementačné miesto | Poznámka pre rozsah |
| --- | --- | --- |
| Základné operácie správcu súborov | [IwcmFsVolume](../../../src/main/java/sk/iway/iwcm/system/elfinder/IwcmFsVolume.java): create, delete, listing, streamy, rename. | Vhodné miesto na branch variant volume. |
| Vytvorenie volumes | [FsService.addFsVolume/addActualPageVolume](../../../src/main/java/sk/iway/iwcm/system/elfinder/FsService.java). | Už pripája per-page plánované adresáre; vie zvoliť inú volume implementáciu. |
| Starší elFinder upload | [UploadCommandExecutor](../../../src/main/java/cn/bluejoe/elfinder/controller/executors/UploadCommandExecutor.java). | Má aj priame operácie s `java.io.File`; úprava volume sama nestačí. |
| Rozbalenie archívu | [ExtractCommandExecutor](../../../src/main/java/cn/bluejoe/elfinder/controller/executors/ExtractCommandExecutor.java). | Priamo otvára ZIP a zapisuje súbory. Treba ho zapojiť alebo v branch nepovoliť. |
| elFinder premenovanie | [RenameCommandExecutor](../../../src/main/java/cn/bluejoe/elfinder/controller/executors/RenameCommandExecutor.java). | Pred `volume.rename` môže založiť redirect podľa `elfinderRedirectFolders`; po ňom volá `GalleryService.createOrUpdateGalleryEntity`. Zmena volume tieto DB zápisy nezastaví. |
| elFinder presun/vystrihnutie | [PasteCommandExecutor](../../../src/main/java/cn/bluejoe/elfinder/controller/executors/PasteCommandExecutor.java). | Pri cut vytvára redirecty a po zmazaní zdroja volá `FolderPropertiesService.deleteFolderProperties`. |
| elFinder mazanie adresára | [RmCommandExecutor](../../../src/main/java/cn/bluejoe/elfinder/controller/executors/RmCommandExecutor.java). | Po úspešnom volume delete osobitne maže vlastnosti adresára cez `FolderPropertiesService`. |
| Moderný upload | [AdminUploadServlet](../../../src/main/java/sk/iway/iwcm/admin/upload/AdminUploadServlet.java), [AdminUploadController](../../../src/main/java/sk/iway/iwcm/admin/upload/AdminUploadController.java). | Vlastný tok vrátane overwrite a keepboth. |
| CKEditor drag/drop | [EditorUpload](../../../src/main/java/sk/iway/iwcm/editor/rest/EditorUpload.java) → [UploadFileAction.execute](../../../src/main/java/sk/iway/iwcm/editor/UploadFileAction.java). | Zápis cez `IwcmFsDB.writeFiletoDest`, ďalšie spracovanie obrázkov. |
| Vlastnosti a premenovanie | [FilePropertiesService.saveFile](../../../src/main/java/sk/iway/iwcm/system/elfinder/FilePropertiesService.java). | Pri rename môže samostatne uložiť odvodený fulltextový dokument; zápis `file_atr` sa vykonáva aj bez voľby hromadne nahradiť odkazy. Nie je to čistá operácia premenovania súboru. |

Kandidátom je malá explicitná služba pre `read`, `list`, `put`, `delete` a `move`. Branch volume, podporované uploady a preview ju použijú priamo. Každý nepodporovaný zápisový vstup musí v pracovnom režime vrátiť zrozumiteľné obmedzenie; nesmie pokračovať produkčným zápisom.

Kontrola branch musí byť aj na úrovni uvedených príkazov **pred prvým vedľajším účinkom**. Pri podporovanom premenovaní/presune jednotlivého súboru sa nepoužije produkčný redirect hook ani galerijný zápis; pri nepodporovanej operácii sa odmietne celý príkaz, nie až jeho volume časť. Vlastnosti `file_atr` a `dirprop` zostávajú iba na čítanie: pracovné premenovanie/mazanie nesmie ich produkčné riadky meniť ani čistiť. Súborový rename nepoužije celý `FilePropertiesService.saveFile`; prípadné podporované závislé zmeny stránok sa musia stať viditeľnými pracovnými položkami. Podľa Q17 sa príkazy nad celým adresárom odmietnu s výnimkou `mkdir`; nevykoná sa ani ich predbežné čistenie metadát.

## 4. Súborové metadáta a vedľajšie účinky rozširujú pôvodný rozsah

`groups` označuje priečinky webových stránok. Nezahŕňa všetky nastavenia fyzických adresárov.

| Údaj alebo operácia | Zdroj | Dopad |
| --- | --- | --- |
| Atribúty súboru | `FilePropertiesService.saveFile` zapisuje `file_atr`. | Branch iba nad tromi zadanými tabuľkami ich neizoluje. |
| Vlastnosti súborového priečinka | [FolderPropertiesEntity](../../../src/main/java/sk/iway/iwcm/system/elfinder/FolderPropertiesEntity.java) mapuje `dirprop`. | Treba oddeliť od nastavení priečinka stránok v `groups`. |
| Galéria a odvodené obrázky | [UploadService.process](../../../src/main/java/sk/iway/iwcm/admin/upload/UploadService.java) volá `GalleryDB`, watermark, konverzie a generovanie variantov. | Binárne varianty aj galerijné údaje môžu byť vedľajší zápis. |
| Fulltext súborov | `UploadService.process`, `IwcmFsVolume.deleteFile`. | Pracovný upload alebo zmazanie nesmie bez rozhodnutia meniť produkčný index. |
| Hromadné premenovanie odkazov | `FilePropertiesService.saveFile` vie meniť obsah ďalších súborov aj `DocDB.replaceTextAll`. | Musí pracovať v rovnakej branch alebo sa v nej táto voľba vypne. |
| Zákaznícke spracovanie uploadu | `UploadFileAction.reflectionLoader`. | Vlastný hook môže zapisovať mimo definovaného rozsahu. |
| Archív súborov | Samostatná vetva `AdminUploadController` pre `fileArchive`. | Nemožno ho považovať za obyčajný upload súboru bez jeho aplikačných dát. |

Potvrdený rozsah zahŕňa redakčné súbory a existujúce dodatočné metadáta používa iba na čítanie. Konkrétne podporované koreňové cesty a operácie treba spísať v implementačnom zadaní. Úpravy galerijných dát, atribútov alebo adresárových práv ostávajú mimo branch. Ich potlačenie v podporovaných vstupoch nie je riešením zriedkavého súbehu používateľov, ale bežných vedľajších účinkov existujúcich tlačidiel.

Z Q18 vyplýva aj zákaz binárnych zápisov do galérie v P05. [GalleryDB.isGalleryFolder](../../../src/main/java/sk/iway/iwcm/gallery/GalleryDB.java) rozpoznáva galerijnú cestu podľa konfigurácie/nastavení vrátane ich dedenia. Kontrola preto nepoužije iba pevný prefix `/images/gallery`: overí každú menenú logickú cestu vrátane podadresárov a variantov `o_`/`s_`. Priame nahratie, nahradenie, zmazanie či premenovanie súboru nesmie obísť rovnaký zákaz. Mimo galérie sa podporované bežné obrázky a prílohy ukladajú do pracovnej vrstvy bez galerijných DB účinkov; prípadný `imageAlwaysCreateGalleryBean` hook sa tým nestáva povolenou výnimkou.

## 5. Náhľad musí vybrať správny súbor aj thumbnail

| Miesto | Dnešné správanie | Integrácia branch |
| --- | --- | --- |
| [PathFilter.doFilter](../../../src/main/java/sk/iway/iwcm/PathFilter.java) | Kontroluje plánované súbory na začiatku filtra. | Pracovný kontext a prístup treba overiť pred verejnou cache a conditional requestmi. |
| `PathFilter.writeAndCacheFile` | `FileCache` používa URL ako identitu. | Pracovný náhľad musí túto cache obísť alebo oddeliť kľúčom branch. |
| [FilePathTools.writeFileOut](../../../src/main/java/sk/iway/iwcm/common/FilePathTools.java) | Posiela `IwcmFile` cez `MultipartFileSender`, vrátane range/conditional správania. | Dá sa použiť na výstup už správne vybraného fyzického súboru. |
| [ThumbServlet.doGet/realPathSmall](../../../src/main/java/sk/iway/iwcm/editor/ThumbServlet.java) | Originál a thumbnail cache sa odvodzujú z obrazovej URL a fyzickej cesty. | Pracovný originál potrebuje samostatný cache priestor; nestačia HTTP no-cache hlavičky. |
| [PlannedAccess.isPathAccessible](../../../src/main/java/sk/iway/iwcm/doc/PlannedAccess.java) | Kontroluje admina a zápisové právo k priečinku, vypína cache, pri zamietnutí vracia 404. | Užitočný existujúci vzor; branch potrebuje aj overenie prístupu ku konkrétnej pracovnej verzii. |

Ak nginx alebo CDN posiela statické súbory priamo bez Java aplikácie, preview URL musí tieto požiadavky dostať do aplikácie. Cookie prepínač a zmena `PathFilter` samy takýto tok neovplyvnia. Potvrdená Q19 a dokument `03` určujú preview na hostiteľovi administrácie; proxy musí jeho prefix posielať do aplikácie vrátane obrázkov a thumbnailov.

Do stránok a médií je vhodné ukladať logické výsledné URL. Fyzická pracovná cesta patrí iba do interného úložiska. Náhľad potom vyberá obsah danej URL podľa autorizovaného branch kontextu.

### Zvolený transport náhľadu: prefix na admin hostiteľovi

Návrh používa `/__branch-preview/{branchId}/...` na hostiteľovi administrácie podľa Q19. Proxy ju vždy posiela Java aplikácii. Logická doména obsahu pochádza z overenej vetvy; prihlásenie používa existujúcu admin session podľa Q8. Samotné číslo branch v URL neudeľuje prístup. Konkrétny filter a prepis absolútnych odkazov určuje dokument `03`.

- Brána z prefixu zistí branch a logickú cestu, pre renderovanie nastaví overený kontext a po requeste ho uvoľní. Zostávajúca cesta určuje stránku, asset alebo thumbnail.
- V odpovedi sa interné odkazy, obrázky, `srcset`, lokálne formulárové ciele a ďalšie podporované URL prevedú na tento preview prefix. Obsah uložený v databáze sa tým nemení. `Location` pri internom presmerovaní tiež zachová branch.
- Originály aj thumbnaily prechádzajú rovnakou bránou; pracovné odpovede majú `no-store`. Interná thumbnail cache má oddelený branch adresár/kľúč a zneplatní sa pri pracovnej úprave originálu.
- Relatívne URL ostávajú v priestore prefixu, ale koreňové URL v CSS a URL vytvorené JavaScriptom môžu preview obísť. Tieto cesty treba zachytiť v používaných šablónach a renderovacích helperoch, alebo označiť ako nepodporované. Všeobecné textové nahradenie všetkých lomiek nie je riešenie.

Výhodou je jedno explicitné proxy pravidlo bez DNS/TLS pre každú branch a bez prenosu prihlásenia na verejné domény. P01 musí napriek uzatvorenému rozhodnutiu overiť reprezentatívnu šablónu vrátane navigácie, `!INCLUDE`, CSS obrázkov a JavaScriptových požiadaviek. Prefix nie je prísľub univerzálnej kompatibility ľubovoľnej šablóny.

### Kandidát súborovej overlay

Pre potvrdenú živú overlay môže evidencia zmeny obsahovať:

| Pole | Účel |
| --- | --- |
| `branch_id` | Vlastník pracovnej verzie. |
| `logical_path` | Výsledná cesta súboru v doméne branch; fyzické úložisko sa vyrieši z doménového kontextu. |
| `entry_kind` | `FILE` alebo `DIRECTORY`; explicitný adresárový marker pre `mkdir` používa tú istú pracovnú tabuľku. |
| `operation` | `UPSERT` alebo `DELETE`, rovnako ako ostatné pracovné typy. |
| `base_hash` | Hash produkčného súboru pri prvom prevzatí do úpravy; pri novom súbore príznak jeho pôvodnej neexistencie. |
| `staged_path` | Interný odkaz na pracovný binárny obsah pre `UPSERT`; v dátovom návrhu sa fyzická cesta môže odvodiť z branch ID a pracovného row ID. |

Čítanie súboru použije pracovný obsah, pracovné zmazanie alebo fallback. Presun jednotlivého súboru sa dá reprezentovať zmazaním pôvodnej cesty a vložením novej; osobitná identita súboru nie je nevyhnutná. Konflikt binárneho súboru rieši človek voľbou pracovnej alebo produkčnej verzie. Automatické spájanie obrázkov či PDF nie je potrebné. Aj konflikt média sa rieši voľbou celého podporovaného obsahu média; ak chce používateľ kombináciu, upraví médium bežným branch editorom a znova porovná aktuálne vstupy.

Samotné odvodenie adresárov zo súborových ciest nestačí pre dnešné [MkdirCommandExecutor](../../../src/main/java/cn/bluejoe/elfinder/controller/executors/MkdirCommandExecutor.java): používateľ najskôr vytvorí prázdny adresár a potom doň nahráva. Potvrdený dátový model preto používa `DIRECTORY/UPSERT` marker bez bajtov. Listing zobrazuje markery aj rodičov pracovných súborov. Pri merge sa nový adresár vytvorí; už existujúci adresár je po kontrole práv no-op, existujúci súbor na rovnakej ceste je kolízia. Adresár dedí LIVE `dirprop` najbližšieho predka, bez ďalšieho snapshotu alebo tabuľky. Odobratie markeru z balíka je lokálne a nesmie zmazať produkčný adresár; odmietne sa, ak by ponechalo závislé pracovné súbory alebo adresárové markery. Presun, premenovanie, mazanie a rekurzívne kopírovanie adresárov ostávajú serverovo blokované.

Živá overlay neuchováva celý web zo dňa vytvorenia branch. Nezmenené súbory sa v náhľade vyvíjajú spolu s produkciou. Pri súboroch prevzatých do úpravy je hash podklad na detekciu konfliktu; samotný hash neumožní zobraziť pôvodné bajty. Ak má porovnanie ukázať aj pôvodný binárny obsah, treba pri prevzatí zachovať jeho kópiu.

Pracovné fyzické cesty musia zostať mimo verejného priameho servovania. Validácia má pracovať s povolenou logickou cestou a po vyriešení fyzického umiestnenia overiť príslušnosť ku koreňu pracovného úložiska. Oprávnenia sa posudzujú pre logickú cieľovú cestu, nie podľa technického staging adresára.

## 6. Čo možno prevziať z plánovaného publikovania

[PlannedChangesService](../../../src/main/java/sk/iway/iwcm/doc/PlannedChangesService.java) už obsahuje užitočné primitíva:

- `preflight` overí zdroje, práva a podporované úložisko. Dnešná implementácia odmieta DB storage a vyžaduje spoločný filesystem pre pracovný a cieľový súbor.
- `publishFiles` zálohuje existujúci cieľ, uloží file history, pripraví dočasný súbor a atómovo nahradí jeden cieľ.
- `invalidateAsset` zneplatní WebJET `FileCache` a zodpovedajúce thumbnail varianty.
- Pracovné originály zostávajú dostupné do dokončenia publikovania, čo umožňuje opakovanie po chybe.

Atómovosť sa vzťahuje na jeden súbor. Už nahradené súbory zostanú publikované, ak neskorší krok zlyhá. Existujúca implementácia neposkytuje spoločnú transakciu nad súbormi, stránkami a médiami.

Pre potvrdený postupný merge je kandidátom oddelenie týchto primitív od per-page `docId` ciest a ich použitie koordinátorom balíka. Stav dokončených položiek a zrozumiteľná chyba sú potrebné na bezpečné opakovanie. To nevyžaduje návrh distribuovaného transakčného systému. Atómové prepnutie celého webu nie je súčasťou potvrdeného rozsahu.

## 7. UI integračné body

[sidebar.pug](../../../src/main/webapp/admin/v9/views/partials/sidebar.pug) obsahuje prepínač domény. [webjet.js](../../../src/main/webapp/admin/v9/src/js/webjet.js), funkcie `changeDomain` a `changeDomainImpl`, rieši potvrdenie, zmenu kontextu a reload bez starých query parametrov. Je to prirodzený vzor pre prepínač pracovnej verzie.

Správca súborov sa zostavuje v `FsService`. Tam môže vybraný pracovný kontext zvoliť branch volume a zobraziť rovnaké logické cesty. Nemusí nútiť redaktora vyberať technický staging priečinok.

Pri viacerých otvorených záložkách zapisovací request nesie branch ID obrazovky a server ho porovná s aktuálnym výberom v session. Po prepnutí branch v inej záložke zápis starého formulára odmietne; kontext sa nepresmeruje na novú branch ani na produkciu. Ide o kontrolu kontextu jedného requestu, nie o uzamykanie redaktorov. Preview URL si zachová vlastnú branch nezávisle od administrátorského výberu.

## 8. Overenie potrebné pred dokončením implementácie

Tieto scenáre sú podklady pre neskorší plán testov, nie teraz vykonané testy:

1. Upravený obrázok má v produkcii pôvodný obsah a v náhľade pracovný obsah pod rovnakou logickou URL; správne sa zobrazí aj thumbnail po jeho predchádzajúcom nacachovaní.
2. Nový súbor nie je dostupný návštevníkovi bez autorizovaného preview; pracovné zmazanie neodstráni produkčný súbor.
3. Listing správcu súborov ukáže vloženia, zmeny a zmazania; podporovaný upload, overwrite, rename a delete zapisujú iba do branch.
4. CKEditor drag/drop, moderný upload aj podporované elFinder príkazy majú rovnakú izoláciu. Nepodporovaná cesta skončí vysvetlenou chybou bez produkčného vedľajšieho účinku.
5. Existujúce médium zachová po merge `media_id`, vlastníka a kategórie; nové médium zachová svoje rezervované ID. Zmazané médium zmizne iba po merge.
6. Dočasné médiá nových stránok toho istého používateľa sa nemiešajú medzi produkciou a branch.
7. Konflikt súboru po produkčnej úprave zobrazí obe verzie; voľba produkcie súbor neprepíše, voľba branch použije vybraný pracovný obsah.
8. Po čiastočnom zlyhaní sa podľa schváleného modelu dá zrozumiteľne pokračovať; opakovanie nevytvorí nové identity médií ani nezmaže pracovné zdroje pred dokončením.
9. Pri externom statickom úložisku a reverse proxy sa overí konkrétna podporovaná konfigurácia a izolácia jednej domény. Nepodporované DB storage sa odhalí pred prvým zápisom.
10. Zmena branch v jednej záložke nespôsobí uloženie formulára z druhej záložky do iného kontextu.
11. Prázdny pracovný adresár prežije obnovenie správcu súborov, jeho rodičia sú viditeľní aj pre nový upload a merge nevytvára `dirprop`. Súbor na cieľovej ceste je kolízia; existujúci adresár je po kontrole práv no-op. Odobratie markeru so závislými položkami sa odmietne a nedotkne sa produkcie.
12. Pokusy o zápis do galérie cez moderný upload, CKEditor a elFinder skončia pred akoukoľvek zmenou bajtov alebo DB. Rovnako sa odmietnu nepodporované operácie nad celým adresárom; nevznikne redirect ani zápis/mazanie `file_atr`, `dirprop` alebo galérie.

Analýza vychádza z čítania uvedených zdrojov. Nevykonala integračný test úložísk, zákazníckych hookov ani runtime náhľadu. Úplný inventár zápisových vstupov treba uzavrieť pre schválený rozsah operácií pred zapnutím funkcie používateľom.
