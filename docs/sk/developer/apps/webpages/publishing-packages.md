# Publikačné balíky v šablónach a komponentoch

Náhľad vykresľuje zvolený výsledok LIVE/WORK v existujúcom layoute. Samotná URL gateway nezmení priame zákaznícke SQL alebo JPA na pracovné čítanie. Pred použitím komponentu v pracovnej verzii overte jeho čítanie, odkazy, súbory, cache a vedľajšie účinky. Konkrétny inventár je v [návode správcu](../../../admin/setup/publishing-packages.md).

## Čítanie a kontext

`BranchFilter` overí prihláseného administrátora, doménu a výber; komponent používa už overený request. `BranchContexts.current(request)` rozlišuje `LIVE`, `ADMIN` a `PREVIEW`. Konštrukcia kontextu zo samotného query parametra nie je autorizácia. ADMIN uchováva WORK, PREVIEW používa spoločné rozhodnutia výsledku; nevyriešený konflikt je označený pracovný predpoklad, zastarané rozhodnutie sa odmietne.

Pre jednoduché čítanie slúži `sk.iway.iwcm.branch.BranchReadService`: `getDoc`, `getGroup`, `documents`, `groups`, `docIdFromUrl`, `docLink`. Adaptované bežné `DocDB`, `GroupsDB`, `MediaDB` a `DocDB.getDocLink` používajú účinné dáta. Odkazy skladajte existujúcim API, ponechajte logické URL v uloženom obsahu. Fyzické súkromné cesty nepatria do HTML ani do databázových odkazov.

Pre SQL zoznam dokumentov je určený `BranchSql.documentSource("d")`. Jeho `Fragment.sql()` tvorí účinnú reláciu a `bind(statement, firstIndex)` naviaže argumenty a vráti ďalší index. Na túto reláciu aplikujte doménové/prístupové filtre, count, radenie a paging. Dodatočná výmena objektov až po `LIMIT` dá nesprávny počet aj poradie. JPA repository bez adaptácie stále číta LIVE; nejde o automatický filter všetkých entít.

Výsledky pracovného requestu neukladajte do zdieľanej LIVE cache. Kontext sa nesmie prenášať singletonom ani ThreadLocal mimo existujúceho request lifecycle. Technický LIVE scope cez `BranchContexts.live(...)` použite len po vlastnej autorizácii operácie a vždy v `try-with-resources`; nemení výber session.

## Správa pracovných verzií a domény

`BranchRepository` rozširuje `DomainIdRepository` a `BranchEntity` ukladá `domain_id`. DataTables tak používa štandardné filtrovanie pri zapnutom oddelení údajov domén a kontrolu ID domény odoslaného editorom. Načítanie konkrétneho záznamu zabezpečuje zdedené `getOneItem(...)` bez vlastného override. Vytvorenie nastavuje ID domény na serveri cez `CloudToolsForCore.getDomainId()`.

Podľa potvrdeného Q20 je `enableStaticFilesExternalDir=true` podmienkou funkcie. Oddelenie balíkov používa iba `domainId`; varianta so spoločným ID `1` sa nepodporuje. Názov domény zostáva potrebný na zobrazenie a URL náhľadu, nie ako druhý kľúč vlastníctva. Session výber uchováva `domainId`; API očakáva `expectedDomainId`, hlavička `X-WebJET-Branch-Domain` a parameter vnoreného editora `branchDomain` prenášajú číselné ID. Náhľad a cron načítajú `domain_id` z balíka a cez `GroupsDB` odvodia routovaciu doménu. Mapa doménových ID zostáva z LIVE aj po odstránení koreňa vo WORK. Súbory sa mapujú existujúcim `Tools.getRealPath()` v overenom doménovom kontexte.

## JPA a transakčné úložisko

`BranchRestController` spája správu DataTable, zoznam prepínača, výber kontextu a náhľad. Používa zdedené `getOneItem`, `insertItem` a `editItem` z `DatatableRestControllerV2`. `DomainIdRepository` zabezpečuje doménové načítanie a Editor nastavuje/kontroluje `domainId`. `beforeSave` inicializuje nový balík, kontroluje pripravenosť pri vytvorení a nastavuje autora a čas úpravy. Názov validujú anotácie entity; farba má iba limit 16 znakov podľa DB stĺpca, bez validácie členstva v palete UI. Automatický audit poskytuje `AuditEntityListener`.

Entita používa bežné JPA mapovanie bez obmedzení `insertable=false` a `updatable=false`. Pri úprave metadát štandardný DataTable merge zachová hodnoty polí označených `hiddenEditor=true`. Vytváranie používa výlučne DataTable Editor na `/admin/rest/branches/editor`; `beforeSave` nastaví stav DRAFT, revíziu 0 a vyprázdni plánovanie, priebeh a chybu publikovania. Samostatný create endpoint ani `BranchService.create` sa nepoužívajú. `BranchService` zabezpečuje metadátové čítanie a publikačné prechody; samostatná metóda `updateMetadata` a jej zámkový query už nie sú potrebné. Publikačné prechody naďalej používajú podmienené JPQL/SQL operácie s kontrolou stavu a revízie.

`BranchService` nemá spoločnú transakciu na úrovni triedy; jednotlivé aktualizačné JPQL metódy v `BranchRepository` majú vlastné `@Transactional`. Metadáta načítava štandardné `GET /admin/rest/branches/{id}`; report porovnania a publikovania používa `GET /admin/rest/branches/{id}/progress`.

DataTable mazanie deleguje na doménovo obmedzené `BranchService.delete`. Zmazanie entity cez JPA odstráni aj `branch_documents`, `branch_groups`, `branch_media` a `branch_files` cez FK s `ON DELETE CASCADE`. `BranchFiles.deleteBranch` odstráni súkromný adresár verzie aj jej thumbnail cache. Mazanie je nezávislé od behu publishera a neobmedzuje ho stav verzie. Publikovania radí existujúci jednovláknový executor. Súbežné zmazanie môže publisher ukončiť chybou; už vykonané LIVE zápisy sa nevracajú späť. Samotné mazanie nezasahuje do LIVE obsahu ani histórie. `afterDelete` vyčistí aktívny session výber a použije štandardné presmerovanie DataTable na obnovenie stránky. Import a export sú v lište skryté.

Náhľad načítava doménu balíka cez JPA. Scheduler používa repository pri výbere splatných balíkov a označení potreby zásahu. Dokončenie a chyba publishera používajú podmienené JPQL aktualizácie s kontrolou stavu, pri scheduleri aj revízie a termínu. Médiá a kategórie sa čítajú existujúcimi JPA repozitármi; JPQL načíta aj kategorizáciu a obnoví dáta po natívnom WORK/LIVE zápise. Binárne porovnanie používa `BranchFileRepository` na načítanie súkromnej súborovej identity.

Natívny JDBC zostáva v rezervácii existujúcich produkčných ID, v klasifikovaných WORK/LIVE zápisoch a checkpointoch, ktoré musia zdieľať jednu transakciu a spojenie. Rovnako zostáva v účinných SQL reláciách stránok a Noviniek a pri kontrole fyzickej schémy, engine a DB oprávnení. Tieto transakcie nerozdeľujte volaním nezávislého JPA repository uprostred zápisu.

## Overené aplikácie

V editore sú explicitne povolené iba `sk.iway.iwcm.components.menu.MenuApp` a `sk.iway.iwcm.components.news.NewsApp`, s renderermi `/components/menu/menu_ul_li.jsp` a `/components/news/news-velocity.jsp`. Povolenie riadi `BranchApps`. Inú aplikáciu treba najprv adaptovať a overiť, samotné doplnenie do mapy kompatibilitu nezabezpečí.

`MenuULLI` zachováva existujúce parametre vrátane koreňa, hĺbky, tried a `onlySetVariables`; číta účinný strom. `NewsActionBean` vykonáva filter/count/paging nad účinnou SQL množinou. Používa existujúce perex kategórie, štandardné criteria a jeden `previewAt` na request. Vlastné `ResultCriteria` a `contextClasses` v preview nie sú podporované. Dátum autora/uloženia nie je čas publikovania balíka.

Testujte skutočný JSP/Velocity render, preklik cez menu a Novinky, dve vetvy/LIVE a zahriatu cache. Pred merge zachyťte výsledok s rovnakou šablónou, časom a identitou a porovnajte ho s LIVE po merge. Normalizovať možno hostiteľa, prefix gateway a lištu, obsahové rozdiely sa nesmú zakryť. P08 dopĺňa aj anonymný verejný výsledok pre obsah bez prístupového obmedzenia.

## Zápisy a rozšírenia

Bežné ukladanie stránok musí ísť cez `EditorFacade` a branch editor, upload cez existujúce adaptované vstupy. Priamy SQL/JPA zápis v komponente obíde izoláciu, práva a históriu. Náhľad nie je sandbox pre ľubovoľné JSP: formulárové objednávky, emaily a zákaznícke zápisy sa v ňom nepovoľujú. Pred publikovaním sa odmietajú aj nepodporované fulltext hooky a multigroup zápisy.

Jadro má štyri typy `DOCUMENT`, `GROUP`, `MEDIA`, `FILE`, spoločný `BranchChange`/`BranchDecision`, prípravu a priebeh. Každý typ má jediné WORK úložisko; súborové bajty sa nepublikujú v DB transakcii. Neznámy typ REST odmietne, nesmie sa preskočiť. Nový modul potrebuje vlastný dohodnutý kontrakt a testy; banner/galéria ani generický register/JSON storage nie sú súčasťou P08.
