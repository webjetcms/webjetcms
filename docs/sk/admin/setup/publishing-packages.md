# Publikačné balíky: správa a obnova

Pracovné verzie vyžadujú `enableStaticFilesExternalDir=true`. Správa, API, náhľad, publikovanie aj cron oddeľujú balíky cez štandardné `domainId`; názov domény sa odvodzuje pre zobrazenie a URL. Výsledky prechodu sú v [registri realizácie](../../../research/publish-packages/STAV.md#zmena-požiadaviek-11-novembra-2026).

Na zapnutie pracovných verzií slúži `branchesEnabled=true`. Súkromné súbory sa predvolene ukladajú do `/WEB-INF/branches`; adresár sa vytvorí automaticky pri prvom použití. Premennú `branchStorageRoot` nastavujte iba vtedy, ak chcete použiť vlastný existujúci absolútny adresár. Prázdna hodnota tiež použije predvolené úložisko. Existujúca vlastná cesta zostáva zachovaná a jej súbory sa automaticky nepresúvajú.

> Pri vlastnom `branchStorageRoot` vyberte adresár, ktorý žiadna doména nesprístupňuje cez web. Ak používate `cloudStaticFilesDir`, overte skutočné umiestnenie verejných priečinkov `/files`, `/images` a `/shared` vrátane cieľov symbolických odkazov. Ak úložisko umiestnite pod niektorý z nich, návštevník môže stiahnuť pracovné súbory ešte pred publikovaním balíka.

Funkcia je experimentálny POC s predvoleným `branchesEnabled=false`. Podporované sú **MariaDB 10.11 LTS, 11.4 LTS a 12.2 s InnoDB**, **PostgreSQL od verzie 16** a **Microsoft SQL Server 2022 (16.x)**. Tieto profily počítajú s jednou inštanciou WebJETu, jednou zapisovateľnou databázou a lokálnym súborovým úložiskom. Pre rady LTS používajte aktuálnu opravnú verziu. Pred produkčným zapnutím funkciu overte na konkrétnom nasadení.

## Inventár a hranice

| Overená časť | Rozsah |
| --- | --- |
| Šablóna `/templates/aceintegration/jet/blank.jsp` | Skutočný JSP layout, menu a Novinky, verejný HTML render a náhľad. Lokálna integračná šablóna; nejde o potvrdenie všetkých zákazníckych šablón. |
| Šablóny `/templates/aceintegration/jet/article.jsp`, `sub-page.jsp` | Pôvodná LIVE sada Noviniek (T16) a schvaľovanie pri off/on (T13) na úplných referenčných dátach. |
| `MenuApp`, `menu_ul_li.jsp`, `MenuULLI` | Parametre v editore, účinný strom, odkazy, aktívna cesta, HTML/perex, premenné a porovnanie po merge. |
| `NewsApp`, `NewsActionBean`, `news-velocity.jsp` | SQL pred count/paging, existujúce perex kategórie, štandardné filtre/radenia, Velocity, čas náhľadu a výsledok po merge. |
| Editor, médiá, upload | Stránky/priečinky, mediálne väzby a kategórie; moderný upload, CKEditor, jednotlivé súborové operácie elFinder. |

Iné DB vrátane MySQL a Oracle, MyISAM, viacuzlové nasadenie, CDN/externé storage, IwcmFsDB storage, plný PageBuilder layout a ľubovoľné zákaznícke hooky nie sú týmto deklarované ako podporované. `BranchReadiness` overuje verziu servera, tabuľky a stĺpce, oprávnenia, izoláciu spojení a súkromné úložisko. Na MariaDB kontroluje aj InnoDB. Podrobný stav pôvodného pilotu je v [P08 výsledkoch](../../../research/publish-packages/P08-vysledky.md).

## Zapnutie na schválenom nasadení

1. Zálohujte konzistentne DB, verejné súbory a súkromné branch úložisko. Overte obnovu na oddelenom prostredí. Zaznamenajte generátory a ID podľa časti Obnova.
2. Nasaďte kód s vypnutým `branchesEnabled`. Migrácie vytvoria päť tabuliek balíkov a zaregistrujú plánovač. Na SQL Serveri overte [úroveň kompatibility, JDBC a práva](#microsoft-sql-server-2022). Na PostgreSQL overte [schému a sekvencie](#postgresql). Na MariaDB overte úložný engine existujúcich tabuliek a podľa potreby vykonajte [konverziu na InnoDB](#konverzia-existujúcich-tabuliek-na-innodb). Perex API potrebuje aj oprávnenia na `perex_group_doc`.
3. Overte zápis aplikačného účtu do `/WEB-INF`, kde sa automaticky vytvorí predvolené úložisko `/WEB-INF/branches`. Voliteľný `branchStorageRoot` môže smerovať na existujúci súkromný absolútny adresár mimo webrootu. Kontrola pri aktivácii porovnáva cestu s koreňom webovej aplikácie a pri predvolenom úložisku overuje, že aj po rozlíšení symbolických odkazov zostáva v `/WEB-INF`. Právo zápisu sa overuje vytvorením dočasného súboru. Zachovajte existujúci generátor `pkey_generator` pre `media`.
4. Overte jednu zapisovateľnú DB, aspoň `READ_COMMITTED` pre JDBC aj JPA a rovnaké konfiguračné hodnoty a kód na všetkých uzloch obsluhujúcich obsah. Aktuálny POC deklaruje iba jednu inštanciu; ďalšia topológia potrebuje vlastné overenie súborov/cache/session.
5. Overte `enableStaticFilesExternalDir=true` a existujúce mapovanie súborov jednotlivých domén. Nastavte `branchesEnabled=true` globálne a obnovte všetky dotknuté uzly. Konfigurácia pracovných verzií je globálna, nie prepínač jednej domény. Priraďte **Pracovné verzie** (`menuBranches`) a existujúce práva stránok, priečinkov a súborov. Prvé vytvorenie balíka vykoná kontrolu aktivácie a pri probléme vráti konkrétny dôvod.
6. Pred pilotom overte verejnú stránku bez cookies na jej skutočnej doméne. Hostiteľ administrácie môže byť iný; `domain_redirects` alias musí zodpovedať logickej doméne obsahu. Pôvodné lokálne POC fixture mali alias `iwcm.interway.sk → demo.webjetcms.sk`; P08 používa samostatnú doménu `127.0.0.1`.

Pri `fulltextExecuteApps=true` WebJET pri dokončovaní publikovania vykreslí aplikácie v anonymnej požiadavke a ich výstup zahrnie do SQL vyhľadávania. Medzi zápisom stránky a dokončením indexovania môže vyhľadávanie ešte používať jej pôvodný text. Pri chybe renderovania skontrolujte dôvod v priebehu publikovania a po odstránení chyby použite **Pokračovať**.

Kontrola kompatibility odmietne vlastný `fulltextDataAscMethod`. Súbory používajú existujúce externé priečinky domén. Ukladanie do pracovnej verzie nevykonáva živé JSP ani fulltext hooky. Vlastné aplikácie overte pred aktiváciou.

### Microsoft SQL Server 2022

Publikačné balíky vyžadujú **Microsoft SQL Server 2022 (16.x)** a úroveň kompatibility databázy aspoň **130**. Pri `branchesEnabled=false` pre bežnú prevádzku WebJET CMS naďalej platí minimum SQL Server 2012. Verziu servera a úroveň kompatibility zistíte priamo v databáze WebJETu.

```sql
SELECT SERVERPROPERTY('ProductVersion') AS server_version, name, compatibility_level
FROM sys.databases WHERE database_id = DB_ID();
```

Overený profil používa **SQL Server 2022 Developer CU25 (16.0.4255.1)** a **jTDS 1.3.1**. V zdroji `iwcm` nastavte ovládač `net.sourceforge.jtds.jdbc.Driver`, JDBC URL v tvare `jdbc:jtds:sqlserver://server:1433/databaza;sendStringParametersAsUnicode=true` a `<transactionIsolation>TRANSACTION_READ_COMMITTED</transactionIsolation>`. JDBC aj JPA musia používať rovnaký účet, predvolenú schému a zapisovateľnú databázu. Náhľad posiela vybrané pracovné riadky ako Unicode JSON. Databáza ich spracuje cez `OPENJSON` pred filtrovaním, počítaním a stránkovaním.

Pri nasadení potrebuje migračný účet `CREATE TABLE` v databáze a `ALTER` na schéme WebJETu na vytvorenie tabuliek a indexov. Pri vytváraní cudzích kľúčov potrebuje aj prístup `REFERENCES` k tabuľke `branches`. Registrácia plánovača používa `SELECT` a `INSERT` na `crontab`. Po aktualizácii overte jednu aktívnu úlohu `sk.iway.iwcm.branch.BranchScheduler` pre `all-admin`.

| Tabuľky | Oprávnenia aplikačného účtu pre balíky |
| --- | --- |
| `documents`, `groups` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` a `ALTER` pre zápis rezervovaného ID cez `IDENTITY_INSERT` |
| `branches`, `branch_documents`, `branch_groups`, `branch_media`, `branch_files`, `media`, `media_group_to_media`, `perex_group_doc` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` |
| `documents_history`, `pkey_generator` | `SELECT`, `INSERT`, `UPDATE` |
| `groups_scheduler`, `file_history` | `SELECT`, `INSERT` |
| `_adminlog_` | `INSERT` |

Tieto práva dopĺňajú bežné oprávnenia WebJETu na čítanie používateľov, šablón, kategórií a ďalších údajov webu.

Na tomto profile sme pri úrovni kompatibility 130 overili menu a Novinky s atribútovým filtrom v balíku s 20 zmenenými stránkami. Po reštarte databázy si nové stránky, priečinok a médium zachovali ID. Po publikovaní videl anonymný návštevník zvolené verzie LIVE a WORK vrátane média a prílohy. Po riadenej chybe indexovania redaktor dokončil publikovanie cez administráciu. Stránka zapísaná pred chybou mala aj po pokračovaní jeden záznam histórie a jeden publikačný audit.

### PostgreSQL

V JDBC URL zdroja `iwcm` nastavte parameter `currentSchema` na schému inštalácie, napríklad `currentSchema=webjet_cms`. JDBC aj priame spojenie JPA musia používať tú istú databázu a schému na zapisovateľnom primárnom serveri. Aplikačný účet potrebuje práva na zápis obsahu, histórie, auditu a tabuliek balíkov aj prístup k sekvenciám. Pri štandardných sekvenciách postačuje na pridelenie ďalšieho čísla oprávnenie `USAGE`.

Pred zapnutím spustite pod aplikačným účtom nasledujúci dotaz. Prvý stĺpec má obsahovať schému WebJETu, ďalšie dva názvy sekvencií priradených k LIVE stĺpcom.

```sql
SELECT current_schema(),
       pg_get_serial_sequence('documents', 'doc_id') AS document_sequence,
       pg_get_serial_sequence('groups', 'group_id') AS group_sequence;
```

Po aktualizácii overte v úlohách na pozadí, že pre `all-admin` je zaregistrovaná práve jedna aktívna úloha `BranchScheduler`.

Na PostgreSQL **16.16** si nové stránky, priečinok aj médium zachovali rezervované ID po reštarte databázy a pri publikovaní. Po výbere verzií LIVE a WORK videl anonymný návštevník rovnaké menu a Novinky ako redaktor v náhľade, vrátane média a prílohy. V druhom balíku nastala chyba pri dokončovaní indexovania. Pokračovanie cez administráciu dokončilo všetkých 25 položiek. Už publikovaná stránka mala aj po pokračovaní jediný záznam histórie a jeden publikačný audit. Pri vypnutých balíkoch sa zmena uložená cez bežný editor hneď zobrazila v LIVE a rozpracovaná verzia stránky zostala v balíku.

### Konverzia existujúcich tabuliek na InnoDB

InnoDB je potrebné pre izoláciu rezervácií ID a spoločný rollback obsahu, histórie a záznamu o dokončenom publikačnom kroku. V databáze WebJET CMS (datasource `iwcm`) overte engine všetkých šiestich tabuliek.

```sql
SELECT TABLE_NAME, ENGINE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('documents', 'groups', 'media', 'media_group_to_media',
                     'documents_history', 'groups_scheduler')
ORDER BY TABLE_NAME;
```

V servisnom okne spustite príslušný príkaz iba pre tabuľku, ktorá používa iný engine než InnoDB. Konverzia môže zdržať zápisy a potrebuje voľné miesto na prestavbu tabuliek a indexov.

```sql
ALTER TABLE `documents` ENGINE=InnoDB;
ALTER TABLE `groups` ENGINE=InnoDB;
ALTER TABLE `media` ENGINE=InnoDB;
ALTER TABLE `media_group_to_media` ENGINE=InnoDB;
ALTER TABLE `documents_history` ENGINE=InnoDB;
ALTER TABLE `groups_scheduler` ENGINE=InnoDB;
```

Zopakujte kontrolný dotaz a overte, že všetkých šesť tabuliek používa InnoDB. `pkey_generator` môže používať MyISAM, pretože rozsahy ID prideľuje samostatným atómovým zápisom. Pri tabuľke `documents` overte spoločný FULLTEXT index na `(title, data_asc)`. Porovnajte výsledky vyhľadávania pred konverziou a po nej vrátane krátkych slov a slov, ktoré FULLTEXT ignoruje.

## Proxy a cache

Náhľad má cestu `/admin/webpages/branch-preview/{id}/...` na **hostiteľovi administrácie**. Táto cesta musí ísť do WebJETu cez rovnaké admin/session/IP/HTTPS/Referer kontroly. Nepridávajte verejný statický alias na súkromný adresár. Ani obrázky, PDF a `/thumb/` vo vnútri gateway sa nesmú obslúžiť priamo z filesystemu alebo verejnej CDN. Cesty `/admin/rest/branches/` a `/admin/rest/branch-content` majú rovnaké pravidlo.

Na proxy vypnite zdieľanú cache pre celý gateway a admin REST, zachovajte `Cache-Control: private, no-store`, cookies, query parametre a `X-CSRF-Token`. Neignorujte session pri assetoch s príponou `.png`/`.pdf`. Verejné `/files/` a `/images/` používajú bežné produkčné bajty; po publikovaní dodržte aj existujúcu invalidáciu cache daného nasadenia.

`/WEB-INF/branches` nie je verejná cesta. Jeho obsah sa nezahŕňa do Gitu ani do distribučného WAR. Pri nasadení novej verzie aplikácie zachovajte tento adresár ako aplikačné dáta alebo ho obnovte spolu s databázou; pre úložisko nezávislé od aplikačného adresára použite vlastný `branchStorageRoot`.

Pri TLS terminácii nastavte existujúce `serverBeyoundProxy=true` a `requestIsSecureHeaderName=X-Forwarded-Proto`; `Tools.isSecure` očakáva hodnotu `https`. Dôveryhodná proxy musí **prepísať** `X-Forwarded-Host`/`X-Forwarded-Proto` zo svojho overeného hostiteľa/protokolu a nesmie veriť rovnomenným hlavičkám od návštevníka. Testujte presmerovanie na HTTPS aj priame odmietnutie anonymného assetu. Konkrétna proxy/CDN nebola v lokálnom POC spustená.

## Vzhľad panela náhľadu

Plávajúci panel `#branch-preview-status` používa farbu pracovnej verzie nastavenú v administrácii. Názov farby je dostupný v atribúte `data-branch-color`, napríklad `lavender`. Paleta je spoločná s administráciou.

Predvolený štýl `/admin/v9/dist/css/branch-preview.css` sa vkladá na začiatok `<head>`, pred CSS šablóny. Vlastný CSS súbor šablóny tak môže prepísať farby aj polohu panela bez `!important` a bez úpravy Java kódu:

```css
#branch-preview-status {
    --wj-branch-preview-background: #fff;
    --wj-branch-preview-color: #222;
    --wj-branch-preview-border-color: #666;
    bottom: 24px;
}
```

Premenná `--wj-branch-color` obsahuje základnú farbu pracovnej verzie. Ak nenastavíte samostatné premenné pre pozadie, text a okraj, ich odtiene sa z nej odvodia automaticky. Prepísaním `--wj-branch-color` môžete zmeniť celú farebnú schému panela; selektorom `#branch-preview-status[data-branch-color="lavender"]` môžete upraviť iba konkrétnu farbu.

## Cron

Použite jedinú trvalú úlohu `sk.iway.iwcm.branch.BranchScheduler`, predvolene každých päť minút (`*/5 * * * *`). Interval môžete upraviť v Úlohách na pozadí. V poli **Beží na uzle** nastavte **`CronTask.clusterNode=all-admin`**, alebo konkrétny stabilný admin uzol. Presné meno uzla nie je povinné. Admin/public rolu určuje existujúce `clusterMyNodeType`; public uzol scheduler odmietne aj pri chybnej konfigurácii úlohy.

Náhodné oneskorenie 0–10 sekúnd rozloží admin behy. Pred dvojitým spracovaním chráni transakčné prevzatie `SCHEDULED → MERGING` podľa revízie, nie oneskorenie. Skontrolujte, že migrácia nevytvorila duplicitnú úlohu. POC úloha má ID 14; toto ID nekopírujte do cudzej DB. Nastavenie `all-admin` samo nepotvrdzuje otestovanie Kubernetes/failover. Cron používa čerstvé práva účtu uloženého v `scheduled_by` a nepotrebuje online redaktorskú session.

Pri ručnom publikovaní alebo pokračovaní sa požiadavka uloží do tabuľky `branches` a spustí sa spracovanie na pozadí. Balíky sa spracúvajú postupne, takže požiadavka môže počkať na dokončenie iného balíka. Cron vyhľadáva splatné naplánované balíky a preberá aj ručné požiadavky, ktoré ostali vo fronte napríklad po reštarte. Po prevzatí sa príznak požiadavky vymaže. Ak sa potom spracovanie preruší, skontrolujte priebeh a použite pokračovanie podľa časti Obnova.

## Vypnutie

Najprv zrušte naplánovanie alebo počkajte na kontrolované dokončenie balíkov. Nevypínajte funkciu uprostred `MERGING` ako spôsob rollbacku. Zaznamenajte priebeh a pri chybe posúďte už verejné položky. Zatvorte pracovné formuláre a zvoľte **Hlavná verzia**. Zastavte branch cron, nastavte `branchesEnabled=false` na všetkých obsahových uzloch a obnovte uzly/session. Potom otvorte editor znovu.

Vypnutie nemaže WORK dáta, rezervované ID ani súkromné súbory a nevracia už publikovaný obsah. Úložisko zachovajte, kým nepotvrdíte, že sa nebude obnovovať žiadny balík. Pri ďalšom zapnutí zopakujte kontrolu podmienok aj konfliktov.

## Obnova zálohy so zachovaním ID

Počas obnovy zastavte aplikačné zápisy a cron na všetkých uzloch. Obnovujte DB, verejný filesystem, file-history a súkromný `branchStorageRoot` z jedného konzistentného bodu, vrátane `branches` a všetkých štyroch WORK tabuliek, snapshotov, rozhodnutí a checkpointov. Čiastočná obnova iba tabuľky `documents` alebo iba WORK súborov nezachová zmluvu publikovania.

Na MariaDB rezervácia spotrebuje `AUTO_INCREMENT` technickým vložením a zmazaním riadka v jednej transakcii. Na PostgreSQL balík vyberie ďalšie číslo priamo z existujúcej sekvencie `documents.doc_id` alebo `groups.group_id`. Na SQL Serveri sa po pridelení `IDENTITY` technický zápis vráti na predchádzajúci bod transakcie. Číslo zostane rezervované aj po reštarte databázy. Pri publikovaní ho WebJET vloží cez `IDENTITY_INSERT`. Všetky tri profily používajú pre médiá `PkeyGenerator`. Rezervované ID tak môže existovať iba v pracovnej verzii.

Pri zálohe zaznamenajte na MariaDB nasledujúce hodnoty `AUTO_INCREMENT` a na PostgreSQL stav oboch sekvencií vrátane `last_value` a `is_called`. Na SQL Serveri zaznamenajte aktuálne hodnoty `IDENT_CURRENT('documents')` a `IDENT_CURRENT('groups')`. Na každej z databáz zaznamenajte aj hodnotu generátora `media` a najvyššie ID v LIVE aj WORK tabuľkách. Po obnove musí ďalšie pridelené ID prekročiť všetky známe rezervácie vrátane rezervácií zo zachovávaných záloh. Pri obnove neznižujte zaznamenanú hodnotu počítadla ani stav sekvencií. Zachovajte tiež už pridelené rozsahy mediálneho generátora. Samotné `MAX(id)` v LIVE tabuľke neobsahuje čísla rezervované pre nové pracovné stránky, priečinky alebo médiá.

ID zachovávaných balíkov sa nikdy neprečíslujú. Pri obnove staršej zálohy, ktorá rezerváciu ešte neobsahovala, použite zachytené horné hranice aj vtedy, keď sa obsah balíka neobnovuje. Ak ich nemáte, rezervácie nemožno spoľahlivo odvodiť len z LIVE dát; balíky z novšieho bodu nepublikujte do takej DB bez manuálneho zosúladenia.

Najprv s vypnutým schedulerom overte práva, hash/stav súborov, DB profil, ID a verejný render na oddelenom prostredí. Pri `MERGE_FAILED` alebo po páde v `MERGING` prečítajte trvalý priebeh, porovnajte už aplikované položky s LIVE a použite existujúce overenie/pokračovanie. Nevynulujte checkpointy a neopakujte obsahové INSERTy ručne. Celý balík nemá automatický rollback; návod nie je dôkazom nacvičenej obnovy celej produkčnej zálohy.

Pre autorov šablón a komponentov je určený [integračný návod](../../developer/apps/webpages/publishing-packages.md), pre redaktorov [postup práce](../../redactor/webpages/publishing-packages.md).
