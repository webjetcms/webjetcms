# Trvalé ID nových stránok, priečinkov a médií

Požiadavka je potvrdená: aj objekt vytvorený až vo vetve dostane konečné kladné ID, ktoré sa pri merge nezmení. Tento dokument navrhuje konkrétny mechanizmus. Potvrdená je aj podmienka InnoDB pre dotknuté tabuľky MySQL/MariaDB bez automatickej konverzie existujúcich tabuliek. Nasledujúce kroky sú návrhom, nie vykonanou implementáciou.

## Odporúčanie

Pre stránky a priečinky ponechať existujúci natívny generátor databázy. Pracovná vetva z neho spotrebuje číslo rovnako ako produkčné vytvorenie. Bežný editor, JDBC importy a zákaznícke inserty bez explicitného ID tak naďalej používajú jeden zdroj čísel.

Produkčné `documents.doc_id` a `groups.group_id` ostanú jednoduché primárne kľúče. V `content_branch_document` a `content_branch_group` je samostatné `row_id` identitou pracovného riadka a `doc_id`/`group_id` konečnou identitou obsahu. Rezervovanie nemení ID už existujúcich objektov.

Pre databázy so samostatnou sekvenciou sa číslo prečíta priamo. Pri `AUTO_INCREMENT` a `IDENTITY` ho získa krátky technický insert, ktorý sa v tej istej transakcii zmaže. V produkčnej tabuľke nezostane žiadny potvrdený zástupný riadok.

## Jedna krátka operácia pri vytvorení objektu

Navrhované služby `ContentIdAllocator` a pracovný repository spolupracujú na **jednom JDBC spojení**. Verejná operácia je „vytvor pracovnú stránku/priečinok“, nie „vráť voľné číslo a niekedy ho ulož“.

### MySQL, MariaDB a SQL Server

```text
BEGIN
  INSERT minimálny technický riadok do documents alebo groups bez ID
  prečítaj vygenerované ID z tohto INSERTu
  DELETE tento technický riadok podľa získaného ID
  INSERT pracovný riadok do content_branch_document alebo content_branch_group
         s týmto doc_id alebo group_id a označením novej položky
COMMIT
vráť uložený pracovný objekt redaktorovi
```

- Technický insert použije JDBC a platné povinné stĺpce aktuálnej schémy. Nevolá `EditorFacade`, `GroupsDB.save()`, JPA audit, históriu ani redakčné udalosti. Jeho hodnoty slúžia iba na pridelenie ID.
- ID sa číta cez `getGeneratedKeys()` príslušného statementu alebo ekvivalent daného drivera. Nepoužíva sa `MAX(id)`. Musí sa prečítať pred vložením pracovného riadka, ktorého `row_id` má vlastný generátor.
- Pracovný insert musí používať to isté spojenie; nesmie sa nepozorovane presunúť do samostatnej JPA transakcie.
- **ID ani úspešný výsledok sa nevrátia pred úspešným commitom.** Pri chybe sa celá operácia rollbackne; preskočené číslo neprekáža a nevracia sa do obehu.
- Spojenie sa po operácii uprace vrátane `autoCommit` a izolačnej úrovne. Operácia trvá len počas niekoľkých SQL príkazov, nie počas editovania alebo celého merge.

Samotné `INSERT → ROLLBACK → vrátiť ID` nie je navrhovaným kontraktom. MySQL dokumentuje perzistentný počítadlový stav, ale aj možnosť opätovného použitia práve prideleného čísla pri páde pred zápisom redo logu na disk. Spojenie rezervácie s potvrdeným pracovným riadkom umožňuje opierať výsledok o bežnú záruku trvalosti databázového commitu. Toto spojenie je návrhovým záverom z databázovej dokumentácie, musí sa overiť integračným testom. [MySQL 8.4: InnoDB AUTO_INCREMENT](https://dev.mysql.com/doc/refman/8.4/en/innodb-auto-increment-handling.html).

MariaDB používa perzistentné InnoDB počítadlo od 10.2.4; rollback môže vytvoriť medzeru. SQL Server dokumentuje, že hodnoty spotrebované neúspešným alebo rollbacknutým insertom znovu negeneruje. [MariaDB: AUTO_INCREMENT v InnoDB](https://mariadb.com/docs/server/server-usage/storage-engines/innodb/auto_increment-handling-in-innodb), [SQL Server: IDENTITY](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-table-transact-sql-identity-property?view=sql-server-ver16).

### PostgreSQL a Oracle

```text
BEGIN
  prečítaj NEXTVAL existujúcej produkčnej sekvencie
  INSERT pracovný riadok s týmto konečným ID
COMMIT
vráť uložený pracovný objekt redaktorovi
```

PostgreSQL musí použiť sekvenciu skutočne priradenú stĺpcu, napríklad zistenú cez `pg_get_serial_sequence` s interným názvom tabuľky a stĺpca. Názov schémy sa nepreberá z požiadavky používateľa. Oracle v štandardnej schéme WebJET používa `S_documents` a `S_groups`; ich použitím sa zdieľa generátor s existujúcimi insert triggermi.

PostgreSQL prideľuje `nextval` atómovo a pri rollbacku hodnotu nevracia, zároveň upozorňuje na trvalosť až po commite. Oracle sekvencie prideľujú čísla nezávisle od commitu či rollbacku. [PostgreSQL: Sequence functions](https://www.postgresql.org/docs/current/functions-sequence.html), [Oracle: CREATE SEQUENCE](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-SEQUENCE.html).

## Nevyhnutná úprava čítania: nečítať nepotvrdené riadky

Toto je konkrétna podmienka, nie všeobecná úvaha o súbežnosti. [DBPool](../../../src/main/java/sk/iway/iwcm/DBPool.java) pri získaní spojenia nastavuje `autoCommit=true`; bežné `getConnection()` používa `READ_COMMITTED`, ale bezparametrické `getConnectionReadUncommited()` prepína na `READ_UNCOMMITTED` (riadky 478–549 a 602–627). Táto druhá cesta by mohla vidieť a uložiť do cache technický riadok ešte pred jeho zmazaním.

Odporúčaná zmena je **jedna centrálna podmienka v `DBPool.setTransactionIsolationReadUNCommited()`**: ak je zapnuté `contentBranchesEnabled`, použije `READ_COMMITTED`. Platí aj pre bežnú produkčnú požiadavku, ktorá by inak mohla vidieť technický riadok. Pri vypnutej funkcii zostáva doterajšie správanie. Tým sa pokryjú aj spoločné query helpery, ktoré izolačnú úroveň menia až po získaní spojenia; netreba osobitne upravovať každý obsahový SELECT.

Pri zapnutej funkcii musia oba izolačné helpery pri zlyhaní nastavenia skončiť chybou. Dnešné zachytenie a zalogovanie výnimky na riadkoch 602–633 nestačí. `getConnection()` v takom prípade nesmie vrátiť získané spojenie a musí ho uzavrieť; pri chybe resetovania sa nesmie vrátiť do poolu ako použiteľné. Pripravenosť zároveň overí najmenej `READ_COMMITTED` na datasource používanom priamo cez JPA. Globálna podmienka musí platiť na všetkých uzloch, ktoré čítajú rovnakú produkčnú DB, pred povolením rezervácií.

Nasledujúci inventár slúži na overenie pokrytia, nie ako zoznam pätnástich povinných prepisov call site. Počty a riadky zodpovedajú aktuálnemu checkoutu:

| Súbor | Miesta |
| --- | --- |
| [DocDB](../../../src/main/java/sk/iway/iwcm/doc/DocDB.java) | 592, 709, 797, 959, 1101, 1830, 3528, 5865 — 8 miest |
| [SearchAction](../../../src/main/java/sk/iway/iwcm/doc/SearchAction.java) | 689, 1247, 1366 — 3 miesta |
| [NewsQuery](../../../src/main/java/sk/iway/iwcm/components/news/NewsQuery.java) | 503, 682 — 2 miesta |
| [DocTools](../../../src/main/java/sk/iway/iwcm/common/DocTools.java) | 1014 — editovateľné stránky |
| [ExportManager](../../../src/main/java/sk/iway/iwcm/sync/export/ExportManager.java) | 338 — export stránok vrátane nezverejnených |

Spolu ide o **15 priamych obsahových volaní**, ale nie celý zoznam čítaní s touto izoláciou. Nepriame cesty sú:

- [ComplexQuery.list()](../../../src/main/java/sk/iway/iwcm/database/ComplexQuery.java), riadok 100, prepína izoláciu pri streaming režime. Používa ho [FulltextSearch](../../../src/main/java/sk/iway/iwcm/system/fulltext/FulltextSearch.java), riadok 134, pri indexovaní dokumentov.
- [StatNewDB.prepareStatement()](../../../src/main/java/sk/iway/iwcm/stat/StatNewDB.java), riadok 204, prepína izoláciu aj pri použití mimo štatistík. Obsah čítajú jeho volania v [TemplatesDB](../../../src/main/java/sk/iway/iwcm/doc/TemplatesDB.java), riadok 516, [SeoManager](../../../src/main/java/sk/iway/iwcm/components/seo/SeoManager.java), riadok 872, a [FileTools](../../../src/main/java/sk/iway/iwcm/FileTools.java), riadky 1168 a 1329; posledné miesto obsluhuje všeobecnú kontrolu použitia vrátane dokumentov a médií.

Oba helpery používajú `DBPool.setTransactionIsolationReadUNCommited()`, preto ich centrálna zmena pokryje. `GroupsDB` používa bežné spojenia. Vyhľadanie v core Java/JSP/XML/properties nenašlo SQL hint `NOLOCK` ani priame `Connection.setTransactionIsolation()` mimo `DBPool`; tento inventár sa pri implementácii zopakuje. Zákaznícky SQL, externý datasource alebo databázový pohľad s vlastným dirty read nie sú týmto zistením overené. Kontrakt podporovaného čítania obsahu je aspoň READ_COMMITTED.

Cena centrálnej podmienky je vedomá: pri zapnutej funkcii aj nesúvisiace core volania požadujúce dirty read, napríklad štatistiky a používatelia, čítajú potvrdené údaje. Overenie zahŕňa ich bežné načítanie; počet redaktorov neopisuje počet návštevníkov verejného webu. Alternatíva s lokálnou náhradou pätnástich volaní a oddelenými streaming helpermi by tento dosah zúžila, ale mala viac integračných miest a ľahšie by vynechala ďalšiu obsahovú cestu. Preto nie je prvou voľbou.

SQL Server môže pri READ_COMMITTED krátko počkať na prebiehajúci insert. Nie je potrebné kvôli tomu zapínať nové globálne snapshot nastavenie databázy; základná akceptácia overí aj bežné načítanie stránok počas tejto krátkej operácie.

## Merge nového objektu s už rezervovaným ID

Pracovný riadok je označený ako nový. Pri prvom použití merge overí, že v produkcii dané ID neexistuje, a vykoná **INSERT s explicitným ID**. Ak už existuje neočakávaný riadok, nevykoná jeho UPDATE; oznámi konflikt identity. Pri pokračovaní po chybe môže existovať riadok vložený predchádzajúcim pokusom tej istej položky: publikačná služba ho overí voči vopred uloženému výsledku a fáze aplikácie a pokračuje ďalším krokom. Writer sám nevykonáva všeobecný upsert. PK je poslednou kontrolou proti nesprávnemu prepísaniu.

Navrhovaný `ContentRowWriter` poskytne interné operácie `insertDocumentWithId(connection, values)` a `insertGroupWithId(connection, values)`. Neodvodzuje INSERT/UPDATE iba z kladného ID a nespolieha sa, že JPA `save()` s `@GeneratedValue(IDENTITY)` zachová predpridelené číslo pri novom riadku. Potrebné normalizácie, históriu, audit a cache riadi publikačná služba; tento writer má zodpovednosť iba za konkrétny zápis.

| DB | Explicitný insert |
| --- | --- |
| MySQL/MariaDB | Vloží rezervované `doc_id`/`group_id` do bežného AUTO_INCREMENT stĺpca. Natívne počítadlo už túto hodnotu spotrebovalo. |
| PostgreSQL | Vloží explicitnú hodnotu; existujúci default `nextval` sa pre tento riadok nepoužije. |
| Oracle | Vloží explicitnú hodnotu; štandardný WebJET trigger generuje číslo iba pri NULL. |
| SQL Server | Na tom istom spojení zapne `SET IDENTITY_INSERT documents ON` alebo `groups ON`, vykoná insert a vypne ho v `finally`. Zapnutá môže byť iba jedna tabuľka na spojení. |

SQL Server vyžaduje vlastníctvo tabuľky alebo oprávnenie `ALTER`; dostupnosť sa overí pred aktiváciou funkcie, nie až pri plánovanom merge. Ak vypnutie `IDENTITY_INSERT` zlyhá, spojenie sa nesmie vrátiť do poolu v neznámom stave. [SQL Server: SET IDENTITY_INSERT](https://learn.microsoft.com/en-us/sql/t-sql/statements/set-identity-insert-transact-sql?view=sql-server-ver17). WebJET už explicitný import ID používa v [admin/updatedb.jsp](../../../src/main/webapp/admin/updatedb.jsp), riadky 71–100; jeho skladanie všeobecného SQL však nie je API pre nový writer.

Bežné produkčné pridelenie ID zostáva zachované v [EditorService.docSave()](../../../src/main/java/sk/iway/iwcm/editor/service/EditorService.java), [DocDB.saveDoc()](../../../src/main/java/sk/iway/iwcm/doc/DocDB.java), starom [EditorDB](../../../src/main/java/sk/iway/iwcm/editor/EditorDB.java) a [GroupsDB.setGroup()](../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java). Inventár našiel tri priame JDBC inserty do dotknutých tabuliek: `DocDB:4561`, `EditorDB:839`, `GroupsDB:1016`; JPA mapovanie je v `DocDetails` a tiež v GDPR `DocumentsBean`. Žiadny z týchto generátorov sa nenahrádza samostatným počítadlom vetiev.

## Médiá: existujúci PkeyGenerator

`media_id` nie je natívne AUTO_INCREMENT. [Media](../../../src/main/java/sk/iway/spirit/model/Media.java) používa `WJGen_media`, ktorý cez [WJGenSequence](../../../src/main/java/sk/iway/iwcm/system/jpa/WJGenSequence.java) volá [PkeyGenerator](../../../src/main/java/sk/iway/iwcm/PkeyGenerator.java). Nová vetva musí čerpať z toho istého kľúča `media`; nevytvára druhý nezávislý generátor.

Pred použitím na trvalú rezerváciu sú potrebné dve malé úpravy existujúceho generátora:

1. Pridať striktné interné rozhranie, ktoré pri chybe alokácie vyhodí výnimku. Dnešné `getNextValueAsLong()` na riadkoch 373–418 výnimku zachytí a môže vrátiť počiatočnú hodnotu. Pracovné vytvorenie nesmie pri chybe dostať náhradné ID. Staré verejné rozhranie možno zachovať pre kompatibilitu; rezervácia použije striktnú cestu.
2. Bootstrap čítača smie hodnotu v `pkey_generator` iba zvýšiť. Dnešné `setMaxValue()` zapisuje bez podmienky; upraviť na zvýšenie len pri `value < požadované minimum`, aby inicializácia jednej inštancie neznížila už pridelený rozsah. Samotná alokácia bloku už používa krátku transakciu a atómový DB update.

V dotaze MySQL/MariaDB sa navyše overí, že aktualizácia zmenila práve jeden existujúci riadok generátora; nesmie sa prevziať staré connection-local `LAST_INSERT_ID()` po nulovej aktualizácii. Striktná cesta musí preniesť aj zlyhanie inicializácie a čítania maxima; nemôže sa spoliehať na dnešné pomocné metódy vracajúce `false` alebo `-1` po chybe. Alokátor používa rovnaký stav a bloky ako bežný `WJGenSequence`, nie druhú nezávislú cache rozsahov. Pri chybe uloženia pracovného média zostane iba medzera v číslovaní.

Nové médium publikuje `ContentRowWriter.insertMediaWithId(connection, values)` explicitným INSERTom. Existujúce médium sa aktualizuje podľa `media_id`. Kladné predpridelené ID nie je dôkaz, že riadok už existuje: dnešný [JpaDB.save()](../../../src/main/java/sk/iway/iwcm/database/JpaDB.java), riadky 184–197, podľa neho volí `merge()` namiesto `persist()`, a preto sa nepoužije bez osobitne overeného kontraktu pre predpridelené ID. `PlannedChangesService.publishMedia()` s vytváraním nových ID sa na tento účel nepoužije.

Zápis média zachová kategorizáciu podľa potvrdeného rozsahu. Existujúce väzby `media_group_to_media` patria do porovnávaného stavu média; nemajú sa bez kontroly nahradiť starým snapshotom, ak ich medzitým zmenila produkcia. Nové médium má explicitne definovaný počiatočný zoznam väzieb, nevytvára nové definície kategórií.

## Podmienky zapnutia a obnovy

- Aktuálne [inštalačné minimum](../../sk/install/setup/README.md) je MariaDB 10.11+, MySQL 8.4+, SQL Server 2012+, Oracle 11g+ a PostgreSQL 16+. V uvedených MySQL/MariaDB verziách je počítadlo perzistentné. InnoDB sa vyžaduje pre **všetky tabuľky skutočne zapisované v krátkej transakcii položky**: `documents`, `groups`, `pkey_generator`, `media`, `media_group_to_media`, nové pracovné tabuľky a tiež zapojené `documents_history`, história priečinkov v `groups_scheduler` a skutočná auditná tabuľka určená cez `ConfDB.ADMINLOG_TABLE_NAME`. Ak je určitá história konfiguráciou vypnutá, jej zápis sa nevykonáva; kontrola vychádza zo skutočného publikačného toku. Staré migrácie vytvárali aj históriu, audit a `pkey_generator` ako MyISAM. Konfigurácia `mariaDbDefaultEngine=InnoDB` sama nepreukazuje engine starších tabuliek; zapnutie ich nekonvertuje automaticky.
- Doplnenie N1 zahŕňa aj `perex_group_doc`, ak sa podľa konfigurácie pri merge stránky obnovuje. Jeho synchronizácia sa vykoná na rovnakom spojení ako zápis dokumentu/checkpointu; kontrola InnoDB ho vtedy nesmie vynechať. Podrobnosti sú v [03a — Novinky](03a-novinky.md).
- Celá krátka operácia musí ísť do jednej zapisovacej databázy. Repliky slúžiace iba na čítanie ID neprideľujú. Podpora pre prepínanie medzi viacerými zapisovacími DB uzlami vyžaduje samostatný dôkaz, že rezervácia je zachovaná aj po prepnutí; tento návrh neprisľubuje riešenie ľubovoľnej DB topológie.
- Povolenie funkcie nemá prečíslovať obsah, meniť existujúce PK ani zakladať rezervácie podľa `MAX(id)+1`. Použije existujúci korektne nastavený generátor.
- Zrušené vetvy svoje čísla neuvoľňujú. Pri databázovom importe alebo obnove sa musí obnoviť produkčný obsah, pracovné tabuľky aj generátory ako jeden celok. Ručné reseedovanie podľa samotného maxima produkčnej tabuľky by ignorovalo rezervácie vo vetvách; servisný postup musí zohľadniť aj ich maximá.
- Rozsah ID zostáva kladný `int`, ktorý očakávajú súčasné API. Pretečenie končí chybou, nie pretypovaním na záporné číslo.

## Krátky overovací krok pred zapnutím

Najskôr na každom podporovanom DB profile overiť samotné prideľovanie a explicitný merge bez zapínania celej funkcie:

1. Vytvoriť pracovný dokument a priečinok, overiť absenciu ich riadkov v produkcii a prítomnosť konečných ID vo vetve.
2. V druhej DB connection pozastaviť rezerváciu po inserte a overiť, že podporované obsahové čítania technický riadok nevrátia ani neuložia do cache. Zahrnúť oba streaming helpery, priame JPA čítanie, bežné načítanie štatistík/používateľov a chybové nastavenie izolácie; flag-off musí zachovať pôvodné správanie.
3. Vytvoriť bežný produkčný dokument/priečinok a ďalší pracovný objekt; všetky ID musia byť rozdielne.
4. Po reštarte DB a aplikácie vytvoriť ďalšie objekty a vložiť pracovné objekty s ich pôvodnými ID. Overiť aj sekvenciu „vyššie ID sa zverejní skôr než nižšie“.
5. Vynútiť chybu pracovného insertu; nevznikne produkčný zástupný riadok ani úspešná odpoveď s ID. Overiť rezerváciu po opätovnom spustení.
6. Na SQL Serveri overiť oprávnenia a vypnutie `IDENTITY_INSERT` aj pri chybe insertu. Bežný produkčný insert na opätovne použitom spojení musí fungovať.
7. Pre médiá overiť rezerváciu medzi dvoma inštanciami generátora, reštart, rast čítača, explicitnú chybu pri nedostupnej DB alebo chýbajúcom riadku generátora a následný INSERT s tým istým ID. Zachované väzby kategórií a produkčná zmena kategorizácie nesmú viesť k strate údajov.

Testy majú použiť reálne podporované databázy, nie H2. Tento krok je podmienkou aktivácie stabilných nových ID; text návrhu nenahrádza potvrdenie správania driverov a aktuálnych schém.

## Prečo sa nenavrhuje druhý globálny alokátor

Samostatný `PkeyGenerator` pre dokumenty/priečinky by vyžadoval previesť všetky bežné JPA a JDBC inserty na rovnaké prideľovanie. Inak by natívny insert mohol spotrebovať číslo už pridelené vetve. SQL Server navyše neumožňuje odstrániť vlastnosť IDENTITY jednoduchou zmenou existujúceho stĺpca; znamenalo by to prestavbu schémy alebo obchádzanie identity pri každom inserte. [SQL Server: IDENTITY](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-table-transact-sql-identity-property?view=sql-server-ver16).

Natívna rezervácia preto sústreďuje zmenu do vytvorenia pracovného objektu, explicitného merge a uvedených obsahových čítaní. Nevynucuje prepis všetkých dnešných producentov nových stránok.
