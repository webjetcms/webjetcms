# Publikačné balíky: správa a obnova

Pracovné verzie vyžadujú `enableStaticFilesExternalDir=true`. Správa, API, náhľad, publikovanie aj cron oddeľujú balíky cez štandardné `domainId`; názov domény sa odvodzuje pre zobrazenie a URL. Výsledky prechodu sú v [registri realizácie](../../../research/publish-packages/STAV.md#zmena-požiadaviek-11-novembra-2026).

Na zapnutie pracovných verzií slúži `branchesEnabled=true`. Súkromné súbory sa predvolene ukladajú do `/WEB-INF/branches`; adresár sa vytvorí automaticky pri prvom použití. Premennú `branchStorageRoot` nastavujte iba vtedy, ak chcete použiť vlastný existujúci absolútny adresár. Prázdna hodnota tiež použije predvolené úložisko. Existujúca vlastná cesta zostáva zachovaná a jej súbory sa automaticky nepresúvajú.

Funkcia je experimentálny POC s predvoleným `branchesEnabled=false`. Overený profil je **MariaDB 12.2.2, InnoDB, MariaDB Connector/J 3.5.10, Java 17.0.18, jedna inštancia, jedna zapisovateľná DB a lokálny filesystem**. Testovacia DB je `127.0.0.1:33316/webjet_p01`. Toto overenie nepovoľuje produkčné zapnutie; to vyžaduje samostatný pokyn pre konkrétne nasadenie.

## Inventár a hranice

| Overená časť | Rozsah |
| --- | --- |
| Šablóna `/templates/aceintegration/jet/blank.jsp` | Skutočný JSP layout, menu a Novinky, verejný HTML render a náhľad. Lokálna integračná šablóna; nejde o potvrdenie všetkých zákazníckych šablón. |
| Šablóny `/templates/aceintegration/jet/article.jsp`, `sub-page.jsp` | Pôvodná LIVE sada Noviniek (T16) a schvaľovanie pri off/on (T13) na úplných referenčných dátach. |
| `MenuApp`, `menu_ul_li.jsp`, `MenuULLI` | Parametre v editore, účinný strom, odkazy, aktívna cesta, HTML/perex, premenné a porovnanie po merge. |
| `NewsApp`, `NewsActionBean`, `news-velocity.jsp` | SQL pred count/paging, existujúce perex kategórie, štandardné filtre/radenia, Velocity, čas náhľadu a výsledok po merge. |
| Editor, médiá, upload | Stránky/priečinky, mediálne väzby a kategórie; moderný upload, CKEditor, jednotlivé súborové operácie elFinder. |

Iné DB vrátane MySQL, SQL Servera, PostgreSQL/Oracle, MyISAM, viacuzlové nasadenie, CDN/externé storage, IwcmFsDB storage, plný PageBuilder layout a ľubovoľné zákaznícke hooky nie sú týmto deklarované ako podporované. `BranchReadiness` povoľuje overenú vetvu MariaDB 12.2 a overuje engine, stĺpce, oprávnenia a úložisko. Podrobný stav povinných prípadov je v [P08 výsledkoch](../../../research/publish-packages/P08-vysledky.md).

## Zapnutie na schválenom nasadení

1. Zálohujte konzistentne DB, verejné súbory a súkromné branch úložisko. Overte obnovu na oddelenom prostredí. Zaznamenajte generátory a ID podľa časti Obnova.
2. Nasaďte kód a databázové migrácie. Migrácia `Publishing branches` v `WEB-INF/sql/autoupdate-webjet9.xml` konvertuje `documents`, `groups`, `media`, `media_group_to_media`, `documents_history` a `groups_scheduler` na InnoDB. Tabuľky, ktoré už používajú InnoDB, neprebudováva. Ide o štandardnú aktualizáciu schémy aj pri vypnutom `branchesEnabled`; pri veľkých tabuľkách naplánujte konverziu mimo špičky, pretože môže blokovať zápisy a potrebuje priestor na prebudovanie tabuľky a indexov. Branch tabuľky `branches`, `branch_documents`, `branch_groups`, `branch_media` a `branch_files` sa vytvárajú s InnoDB. `pkey_generator` môže zostať na MyISAM: rozsahy ID prideľuje samostatným atómovým zápisom a spotrebované čísla sa nevracajú. Perex API potrebuje aj oprávnenia na `perex_group_doc`.
3. Overte zápis aplikačného účtu do `/WEB-INF`, kde sa automaticky vytvorí predvolené úložisko `/WEB-INF/branches`. Voliteľný `branchStorageRoot` môže smerovať na existujúci súkromný absolútny adresár mimo webrootu. Verejné cesty a symlinky z predvoleného úložiska mimo chráneného `/WEB-INF` sa odmietnu. Právo zápisu sa overuje vytvorením dočasného súboru. Zachovajte existujúci generátor `pkey_generator` pre `media`.
4. Overte jednu zapisovateľnú DB, aspoň `READ_COMMITTED` pre JDBC aj JPA a rovnaké konfiguračné hodnoty a kód na všetkých uzloch obsluhujúcich obsah. Aktuálny POC deklaruje iba jednu inštanciu; ďalšia topológia potrebuje vlastné overenie súborov/cache/session.
5. Overte `enableStaticFilesExternalDir=true` a existujúce mapovanie súborov jednotlivých domén. Nastavte `branchesEnabled=true` globálne a obnovte všetky dotknuté uzly. Konfigurácia pracovných verzií je globálna, nie prepínač jednej domény. Priraďte **Pracovné verzie** (`menuBranches`) a existujúce práva stránok, priečinkov a súborov. Prvé vytvorenie balíka vykoná kontrolu aktivácie a pri probléme vráti konkrétny dôvod.
6. Pred pilotom overte verejnú stránku bez cookies na jej skutočnej doméne. Hostiteľ administrácie môže byť iný; `domain_redirects` alias musí zodpovedať logickej doméne obsahu. Pôvodné lokálne POC fixture mali alias `iwcm.interway.sk → demo.webjetcms.sk`; P08 používa samostatnú doménu `127.0.0.1`.

Preflight publikovania odmietne `fulltextExecuteApps=true` a vlastný `fulltextDataAscMethod`. Súbory používajú existujúce externé priečinky domén. Branch save nesmie vykonať živé JSP/fulltext hooky. Test vlastného kódu patrí pred aktiváciu.

InnoDB je potrebné pre izoláciu technických rezervácií ID a spoločný rollback obsahu, histórie a záznamu o dokončenom publikačnom kroku. Celý balík sa publikuje postupne. InnoDB podporuje FULLTEXT aj `MATCH(title, data_asc) AGAINST (? IN BOOLEAN MODE)`; zachovajte spoločný FULLTEXT index na `(title, data_asc)`. Po konverzii z MyISAM overte vyhľadávanie vrátane krátkych slov a stopwords, pretože enginy používajú odlišné nastavenia indexovania.

## Proxy a cache

Náhľad má cestu `/admin/webpages/branch-preview/{id}/...` na **hostiteľovi administrácie**. Táto cesta musí ísť do WebJETu cez rovnaké admin/session/IP/HTTPS/Referer kontroly. Nepridávajte verejný statický alias na súkromný adresár. Ani obrázky, PDF a `/thumb/` vo vnútri gateway sa nesmú obslúžiť priamo z filesystemu alebo verejnej CDN. Cesty `/admin/rest/branches/` a `/admin/rest/branch-content` majú rovnaké pravidlo.

Na proxy vypnite zdieľanú cache pre celý gateway a admin REST, zachovajte `Cache-Control: private, no-store`, cookies, query parametre a `X-CSRF-Token`. Neignorujte session pri assetoch s príponou `.png`/`.pdf`. Verejné `/files/` a `/images/` používajú bežné produkčné bajty; po publikovaní dodržte aj existujúcu invalidáciu cache daného nasadenia.

`/WEB-INF/branches` nie je verejná cesta. Jeho obsah sa nezahŕňa do Gitu ani do distribučného WAR. Pri nasadení novej verzie aplikácie zachovajte tento adresár ako aplikačné dáta alebo ho obnovte spolu s databázou; pre úložisko nezávislé od aplikačného adresára použite vlastný `branchStorageRoot`.

Pri TLS terminácii nastavte existujúce `serverBeyoundProxy=true` a `requestIsSecureHeaderName=X-Forwarded-Proto`; `Tools.isSecure` očakáva hodnotu `https`. Dôveryhodná proxy musí **prepísať** `X-Forwarded-Host`/`X-Forwarded-Proto` zo svojho overeného hostiteľa/protokolu a nesmie veriť rovnomenným hlavičkám od návštevníka. Testujte presmerovanie na HTTPS aj priame odmietnutie anonymného assetu. Konkrétna proxy/CDN nebola v lokálnom POC spustená.

## Cron

Použite jedinú trvalú úlohu `sk.iway.iwcm.branch.BranchScheduler`, predvolene každých päť minút (`*/5 * * * *`). Interval môžete upraviť v Úlohách na pozadí. V poli **Beží na uzle** nastavte **`CronTask.clusterNode=all-admin`**, alebo konkrétny stabilný admin uzol. Presné meno uzla nie je povinné. Admin/public rolu určuje existujúce `clusterMyNodeType`; public uzol scheduler odmietne aj pri chybnej konfigurácii úlohy.

Náhodné oneskorenie 0–10 sekúnd rozloží admin behy. Pred dvojitým spracovaním chráni transakčné prevzatie `SCHEDULED → MERGING` podľa revízie, nie oneskorenie. Skontrolujte, že migrácia nevytvorila duplicitnú úlohu. POC úloha má ID 14; toto ID nekopírujte do cudzej DB. Nastavenie `all-admin` samo nepotvrdzuje otestovanie Kubernetes/failover. Cron používa čerstvé práva účtu uloženého v `scheduled_by` a nepotrebuje online redaktorskú session.

Úloha vyhľadá splatné naplánované balíky a uložené požiadavky na ručné publikovanie alebo pokračovanie priamo v tabuľke `branches`. Balíky spracuje postupne vo svojom cron vlákne, podobne ako `GroupPublisher` spracúva plánované zmeny priečinkov. Pre jednotlivé balíky sa nevytvárajú nové cron úlohy. Aj ručné publikovanie čaká na najbližší beh; pri predvolenom intervale môže začať približne o päť minút alebo neskôr, ak pred ním beží iný balík. Požiadavka zostáva uložená aj po reštarte. Po prevzatí sa jej príznak vymaže, takže chyba alebo pád nespustí automatické opakovanie. Vtedy použite kontrolu priebehu a pokračovanie podľa časti Obnova.

## Vypnutie

Najprv zrušte naplánovanie alebo počkajte na kontrolované dokončenie balíkov. Nevypínajte funkciu uprostred `MERGING` ako spôsob rollbacku. Zaznamenajte priebeh a pri chybe posúďte už verejné položky. Zatvorte pracovné formuláre a zvoľte **Hlavná verzia**. Zastavte branch cron, nastavte `branchesEnabled=false` na všetkých obsahových uzloch a obnovte uzly/session. Potom otvorte editor znovu.

Vypnutie nemaže WORK dáta, rezervované ID ani súkromné súbory a nevracia už publikovaný obsah. Úložisko zachovajte, kým nepotvrdíte, že sa nebude obnovovať žiadny balík. Pri ďalšom zapnutí zopakujte kontrolu podmienok aj konfliktov.

## Obnova zálohy so zachovaním ID

Počas obnovy zastavte aplikačné zápisy a cron na všetkých uzloch. Obnovujte DB, verejný filesystem, file-history a súkromný `branchStorageRoot` z jedného konzistentného bodu, vrátane `branches` a všetkých štyroch WORK tabuliek, snapshotov, rozhodnutí a checkpointov. Čiastočná obnova iba tabuľky `documents` alebo iba WORK súborov nezachová zmluvu publikovania.

Rezervácia nových ID používa rollbackovaný technický insert, ktorý spotrebuje AUTO_INCREMENT, a monotónny `pkey_generator` pre médiá. Rezervované ID preto nemusia byť riadkami LIVE tabuliek. Pred obnovou zaznamenajte `information_schema.TABLES.AUTO_INCREMENT` pre `documents`/`groups`, hodnotu generátora `media`, najvyššie ID vo všetkých LIVE/WORK tabuľkách a rezervácie zo všetkých zachovávaných záloh. Po obnove **neznižujte generátory**: ďalšie document/group ID musí byť vyššie než najvyššia známa rezervácia aj zachytená nasledujúca hodnota; mediálny generátor nesmie byť nižší než jeho zachytená hodnota a známe mediálne ID. Nestačí `MAX(id)` z produkcie. Presný formát/hodnotu generátora overte v `PkeyGenerator`, nevykonávajte univerzálny reset na nulu.

ID zachovávaných balíkov sa nikdy neprečíslujú. Pri obnove staršej zálohy, ktorá rezerváciu ešte neobsahovala, použite zachytené horné hranice aj vtedy, keď sa obsah balíka neobnovuje. Ak ich nemáte, rezervácie nemožno spoľahlivo odvodiť len z LIVE dát; balíky z novšieho bodu nepublikujte do takej DB bez manuálneho zosúladenia.

Najprv s vypnutým schedulerom overte práva, hash/stav súborov, DB profil, ID a verejný render na oddelenom prostredí. Pri `MERGE_FAILED` alebo po páde v `MERGING` prečítajte trvalý priebeh, porovnajte už aplikované položky s LIVE a použite existujúce overenie/pokračovanie. Nevynulujte checkpointy a neopakujte obsahové INSERTy ručne. Celý balík nemá automatický rollback; návod nie je dôkazom nacvičenej obnovy celej produkčnej zálohy.

Pre autorov šablón a komponentov je určený [integračný návod](../../developer/apps/webpages/publishing-packages.md), pre redaktorov [postup práce](../../redactor/webpages/publishing-packages.md).
