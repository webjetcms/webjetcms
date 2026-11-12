# 03. Kontext vetvy, Java rozhrania a náhľad webu

**Realizácia P02 (4. november 2026):** platné API, migrácia a hranice sú v [P02-vysledky.md](P02-vysledky.md), konkrétne polia v [matici stĺpcov](P02-matica-stlpcov.md). Nasledujúci návrh zahŕňa aj dosiaľ neimplementované etapy; stav kódu určuje [STAV.md](STAV.md).

Stav: **technický návrh na spoločné posúdenie**. Tento dokument určuje navrhované správanie a integračné body; neobsahuje vykonanú implementáciu. Overený súčasný stav je v [analýze čítania](analyza-citanie-a-nahlad.md), súbory a médiá v [samostatnej analýze](analyza-media-a-subory.md).

Vychádza z potvrdeného rozsahu: živý overlay jednej domény, postupné zlúčenie, obsahové súbory, stabilné ID aj nových objektov, štandardné menu/novinky/navigácia/médiá, náhľad pre prihlásených administrátorov a prispôsobenie vlastných komponentov cez adaptér. Úpravy súvisiacich tabuliek mimo dohodnutého rozsahu nie sú súčasťou vetvy. Publikovať celý balík smie používateľ s potrebnými právami ku všetkým položkám.

## 1. Rozdelenie zodpovednosti

Produkčné `documents`, `groups` a `media` zostanú produkčným zdrojom. Pracovné verzie budú v piatich tabuľkách návrhu dátového modelu: `branches`, `branch_documents`, `branch_groups`, `branch_media`, `branch_files`. Pracovné záznamy majú pôvodnú identitu objektu, typ operácie, základ porovnania a typované pracovné polia. Súbory majú logickú cestu, odtlačky a interné umiestnenie pracovného obsahu. Presné stĺpce a prideľovanie ID určuje dokument dátového modelu.

Tento dokument pokrýva čítanie výsledného obsahu. Ukladanie pracovnej verzie a produkčné zlúčenie sú samostatné vstupy: načítanie vetvového `DocDetails` nikdy samo osebe neoprávňuje zavolať produkčné `DocDetailsRepository.save`.

Navrhované nové Java triedy a rozhrania používajú prefix `Branch`, napríklad `BranchFilter`, `BranchContext` a `BranchReader`. Patria priamo do jadrového balíka `sk.iway.iwcm.branch`, so zdrojmi v `src/main/java/sk/iway/iwcm/branch/`. Do rovnakého balíka patria aj DTO a entity.

Zoznam nižšie rozdeľuje zodpovednosti; nevyžaduje ku každej službe interface, factory ani ďalšie vrstvy. Podľa [priority POC](ORCHESTRACIA.md#záväzná-priorita-jednoduchý-a-čitateľný-poc) majú byť implementácie konkrétne a čitateľné. Integračné API pre zákaznícke komponenty má reálny účel, samotná možnosť budúceho rozšírenia však nestačí na nový abstraktný framework.

| Navrhovaná trieda | Úloha |
| --- | --- |
| `BranchContext` | Overený kontext jedného requestu: režim, ID vetvy, doména a používateľ. |
| `BranchFilter` | Vyhodnotí admin kontext alebo preview gateway, skontroluje prístup a nastaví kontext pred routovaním. |
| `BranchView` | Výsledný strom, základné metadáta, URL mapa a načítané pracovné záznamy počas jedného requestu. |
| `BranchReadService` | Načíta produkciu a pracovné verzie, zostaví výsledné objekty; poskytne aj rozhranie zákazníckym komponentom. |
| `BranchSql` | Zostaví parametrizovaný zdroj stránok/médií pre podporované SQL zoznamy. |
| `BranchDocumentQuery` | Administračné filtre a stránkovanie nad výsledným zdrojom stránok. |
| `BranchPreviewUrls` | Prevod logickej URL do preview gateway; používa sa aj pre komponenty a presmerovania. |
| `BranchPreviewResponseWrapper` | Pre podporovaný HTML výstup upraví navigačné a asset URL bez zmeny uloženého obsahu. |
| `BranchRestController` | Výber vetvy, údaje o aktívnom kontexte a odkaz na náhľad. Ostatné riadiace operácie definujú dokumenty UI a merge. |

`BranchView` je obyčajný objekt requestu. Nepotrebuje vlastný scheduler, zámky ani spoločný distribuovaný cache systém. Výsledné `DocDetails` a `GroupDetails` sú oddelené objekty, ktoré nemajú väzbu na produkčný JPA persistence context ani zdieľané produkčné cache.

Pre časové filtre Noviniek view uchová jeden request údaj `previewAt`: budúci termín `publish_at` v stave `SCHEDULED`, inak čas začiatku requestu. Nejde o nový DB stĺpec ani globálne prepisovanie času. Presné pravidlá a označenie v náhľade určuje [03a — Novinky](03a-novinky.md).

Koordinácia a spoločný prehľad použijú identitu `type + objectKey` a spoločný obal `BranchChange` podľa [08](08-rozsirovanie-modulov.md). Pre dnešné štyri typy stačia priame volania konkrétnych služieb; `BranchTypeHandler` možno vyčleniť pri skutočnom rozšírení, nie povinne v P02/P06. Dnešné typy používajú vlastné pracovné tabuľky; budúce moduly môžu použiť jednu spoločnú JSON tabuľku. Toto rozdelenie nenahrádza modulové čítanie: detail/zoznam/render sa zapoja explicitným adaptérom, nie zachytávaním všetkých SQL/JPA volaní.

## 2. Dva odlišné kontexty používateľa

### Administrácia

Výber v administrácii sa uloží do session pre vybranú doménu. Zmena domény vyberie produkciu; novú vetvu používateľ vyberie výslovne. Prepínač obnoví otvorenú obsahovú obrazovku.

Každá podporovaná administračná obrazovka dostane pri načítaní `branchId` a číselné `branchDomain` (`domainId`); hodnota ID vetvy `0` znamená produkciu. Jej AJAX požiadavky vrátane uploadu posielajú hlavičky `X-WebJET-Branch` a `X-WebJET-Branch-Domain`, prípadne zodpovedajúce multipart polia. Server porovná **obe** hodnoty s aktuálnym výberom v session a overí doménu cieľového objektu. Nesúlad vráti `409` s pokynom obnoviť obrazovku. Kontrola domény platí aj pre `branchId=0`: dve produkčné záložky rôznych domén majú rovnaké ID vetvy, ale iný cieľ. Chýbajúci kontext v pracovnom režime sa nesmie interpretovať ako súhlas so zápisom do produkcie.

Táto kontrola rieši bežný prípad dvoch otvorených záložiek: prepnutie vetvy v jednej záložke nezmení nepozorovane cieľ uloženia formulára v druhej. Nerieši súbežné editovanie pomocou zámkov.

Riadiace endpointy `/admin/rest/branches/**` používajú explicitné ID cieľovej vetvy a produkčný technický kontext. To je potrebné najmä pri porovnávaní a merge: produkčné čítanie nesmie náhodou vrátiť pracovnú verziu len preto, že ju má používateľ vybranú v editore.

### Verejný náhľad

Podľa Q19 náhľad používa vlastnú cestu **na hostiteľovi administrácie**, kde má používateľ existujúcu prihlasovaciu session. Verejná doména stránky je logická doména obsahu, nie podmienka prihlásenia:

```text
Produkcia: https://www.example.sk/kampan/ponuka.html
Náhľad:    https://admin.example.sk/admin/webpages/branch-preview/42/kampan/ponuka.html
```

ID vetvy v URL nie je prihlasovací token. Server pri každom requeste overí riadne prihláseného administrátora, oprávnenie na náhľad a prístup k doméne uloženej vo vetve. Doménu obsahu nepreberá z voľného query parametra ani aktuálneho výberu v admin session. Prihlásenie a bezpečný návrat na preview URL prebehnú na hostiteľovi administrácie; cookie sa medzi doménami neprenáša. Ak admin aj verejný web používajú rovnaký host, zostáva postup rovnaký. Anonymné zdieľanie nie je súčasťou prvej verzie.

Toto rozdelenie rešpektuje [AdminLogonController.showForm](../../../src/main/java/sk/iway/iwcm/logon/AdminLogonController.java): pri nastavení `multiDomainAdminHost` dnešné prihlasovanie na inom hostiteľovi vracia 404 alebo presmeruje na admin host. Zapnutie branch preto nevyžaduje povoľovať administrátorské prihlasovanie na každej verejnej doméne.

Preview cesta je nezávislá od výberu v administrácii. Používateľ môže mať vedľa seba produkciu a náhľad dvoch vetiev; náhľad vetvy 42 zostane vetvou 42 aj po prepnutí editora na inú vetvu. Bežná produkčná URL nikdy nezíska vetvový obsah iba podľa session.

Od 9. novembra 2026 je gateway pod `/admin/webpages/branch-preview/`. Pred odstránením prefixu volá `PathFilter.checkAdminRequest` nad pôvodnou požiadavkou: existujúce kontroly typu uzla a `adminEnableIPs`, vynútenie `adminRequireSSL`, prístupu do administrácie a Referera. Až potom overí vetvu/doménu a spracuje logickú cestu. Samotné umiestnenie URL pod `/admin/` tieto kontroly nenahrádza, pretože gateway beží pred bežným `PathFilter`. Predchádzajúci koreňový prefix nemá kompatibilný alias.

## 3. Filter a životnosť requestu

V [V9SpringConfig.configureSecurity](../../../src/main/java/sk/iway/webjet/v9/V9SpringConfig.java) už existuje poradie `ApiTokenAuthFilter` → `SetCharacterEncodingFilter` → `FileArchivFilter`. `SetCharacterEncodingFilter` pripravuje používateľa a `RequestBean`. Servletový [PathFilter](../../../src/main/java/sk/iway/iwcm/PathFilter.java) následne rieši virtuálne URL.

Navrhované poradie je:

```text
existujúce prihlásenie a RequestBean
  → BranchFilter
  → existujúce ďalšie filtre
  → PathFilter
  → ShowDoc / komponent / podporovaný asset
```

Registráciu treba overiť integračným testom reálneho filter chainu. Samotná existencia Spring beanu nezaručuje správne poradie ani zahrnutie statických súborov.

`BranchFilter` vykoná tieto kroky:

1. Rozpozná produkčný request, podporovanú admin obrazovku alebo `/admin/webpages/branch-preview/{id}/...`.
2. Pre vetvu načíta jej stav a doménu, overí používateľa a požadovaný režim. Pri chybe skončí bez fallbacku na produkciu.
3. Uloží nemenný `BranchContext` do request atribútu aj do existujúceho `RequestBean` cez jeho `setAttribute/getAttribute`. Rovnakým spôsobom sprístupní jedno view. Nespolieha sa na `RequestBean.getRequest()`: dnešný encoding filter tento odkaz nastavuje iba pre vybrané elFinder URL. Nevzniká druhý globálny register requestov.
4. Pri preview vytvorí request wrapper s logickou cestou bez gateway prefixu; zosúladí `getRequestURI`, `getServletPath`, `getPathInfo` a používané forward/include údaje. Vonkajšiu URL ponechá v samostatnom atribúte. Hostiteľ, TLS, cookie a autentifikačná session ostanú transportom administrácie; wrapper nepredstiera prihlásenie na verejnom hostiteľovi.
5. Pred prvým obsahovým čítaním opraví už existujúci `RequestBean.domain` na overenú doménu vetvy a `RequestBean.url` na logickú cestu. `DocDB.getDomain(request)` v preview uprednostní overený kontext pred `preview.editorDomainName` v session. Obsahové doménové kontroly v `PathFilter`, URL mapa a file mapovanie používajú tú istú logickú doménu; prihlasovacie/transportné kontroly ďalej skutočný admin host. Produkčné doménové presmerovanie nesmie odsunúť preview na verejný host. Preview nemení doménový ani vetvový výber v session.
6. Nastaví súkromné `no-store` hlavičky, `isPreview` a ochranu pred indexovaním. Vnorený forward/include používa ten istý kontext a tú istú `BranchView`. Výstupný wrapper udrží tieto hlavičky aj pri neskoršom statickom servovaní a pred commitom odpovede; `PathFilter.setStaticContentHeaders` ich nesmie prepísať na verejnú cache.
7. V `finally` uvoľní vlastné atribúty a view, obnoví zmenené hodnoty existujúceho `RequestBean`; jeho konečné odstránenie ostáva v životnom cykle encoding filtra. Prenos kontextu do nového background threadu nie je implicitný.

Navrhovaná reprezentácia:

```java
public record BranchContext(
    long branchId,
    String domainName,
    int userId,
    Mode mode
) {
    public enum Mode { LIVE, ADMIN, PREVIEW }
    public boolean isBranch() { return branchId > 0; }
}
```

Mimo webového requestu je predvolený režim `LIVE`. Operácia riadená ID vetvy ho dostane ako explicitný argument služby. Filter neodvodzuje vlastníka vetvy zo všeobecného `CloudToolsForCore.getDomainId()`, pretože doménové ID nemá vo všetkých režimoch rovnaký význam.

Gateway nie je ľubovoľný proxy na vnútorné URL. Dovolí verejné stránky a podporované asset cesty; nepustí `/admin`, `/WEB-INF`, interné pracovné úložisko ani ľubovoľný spustiteľný JSP súbor. Cesty normalizuje pred rozhodnutím o obsluhe. Bežné práva k stránkam a súborom zostávajú zachované.

Gateway odmietne aj legacy ovládacie parametre, ktoré menia zdroj alebo spôsob renderovania: `historyid`, `forward`, `forwarddoccompare`, `inlineEditorAdmin`. Samotná povolená `.html` cesta nestačí, pretože `PathFilter` ich môže preniesť do `ShowDoc`. Bežné parametre stránkovania, hľadania a filtrov Noviniek zostávajú funkčné. Preview sa nekombinuje s historickým náhľadom ani ľubovoľným JSP forwardom.

## 4. Výsledný pohľad na obsah

V administračnom editore (ADMIN) pre objekt s ID `x` platí:

| Pracovný záznam vo vetve | Výsledok čítania |
| --- | --- |
| Neexistuje | Aktuálny produkčný objekt. |
| Vloženie alebo zmena | Pracovné podporované polia pod stabilným ID, doplnené podľa pravidiel projekcie nižšie. |
| Zmazanie | Objekt vo výslednom pohľade neexistuje. |

Detail, metadáta aj SQL zdroj používajú rovnaké pravidlo: podporované redakčné polia z WORK, odvodené polia vypočítané z výsledného obsahu a prevádzkové či nepodporované read-only polia existujúceho objektu z aktuálneho LIVE. To zahŕňa `sync_id`/`sync_status` a existujúce `publish_start`/`publish_end`; pracovná kópia ich nesmie zmraziť, keď ich merge nesmie prepísať. Nový objekt bez LIVE riadka má dohodnuté počiatočné hodnoty vrátane dátumov podľa 03a. Zmena produkčného individuálneho plánu naďalej podlieha preflight kontrole. Nejde o automatické spájanie podporovaných obsahových polí: tie sa stále rozhodujú ako celý objekt.

### Výsledok konfliktu v náhľade (Q16)

PREVIEW používa rovnaké čisté trojcestné rozhodovanie ako merge v `04`. `BranchView` pripraví jednu mapu rozhodnutí `type + objectKey → LIVE/WORK` pre pracovné položky; strom, URL, detail, SQL, médiá a súbory používajú tú istú mapu. Načítava sa podporovaný obsah upravených položiek, nie HTML celého webu. Pri súbore sa platnosť voľby overí odtlačkom aktuálnych bajtov; úložisko výsledných kópií navyše netreba.

| Situácia | Náhľad |
| --- | --- |
| Bez pracovného riadka | Aktuálne LIVE. |
| Platná `resolved_choice` | Zvolená LIVE alebo WORK; platnosť overí aktuálny live/work hash. |
| Bez ručnej voľby, W=B | LIVE, aj keď si pracovný riadok uchoval starý obsah. |
| Bez ručnej voľby, L=W | LIVE bez ďalšieho zápisu; zachová aj technické hodnoty, napríklad pôvodný cutoff média. |
| Bez ručnej voľby, L=B a W je odlišné | WORK; zvolená DELETE položku skryje. |
| Nový nevyriešený konflikt | WORK s viditeľným upozornením na nevyriešené konflikty a odkazom na porovnanie. Výsledok publikovania ešte nie je určený. |
| Uložená voľba má neplatné vstupné hashe | `409` s odkazom na nové porovnanie; nepoužiť starú voľbu ani ju potichu zmeniť na WORK. |

Uloženie vedomej pracovnej úpravy vyčistí jej predošlé rozhodnutie a zvýši revíziu; editor na to pri zvolenom LIVE upozorní. Naopak samotné zistenie neskoršej produkčnej zmeny rozhodnutie potichu nevymaže. `resolved_snapshot` nie je ďalší zdroj renderovania ani podklad SQL: pracovné typované údaje sa zachovajú, mapa vyberie zdroj. Nová tabuľka či parsovanie JSON v databáze nie sú potrebné. Kombinácia neplatných väzieb po výbere sa zobrazí ako konkrétna prekážka, nie produkčný fallback.

Ak zvolený podporovaný cieľ už zodpovedá LIVE, spoločné rozhodovanie ho normalizuje na LIVE/no-op aj pri výslovnej voľbe WORK. Render tak nezmení technický marker média, ktorý publisher pri no-op nemení. Platnosť uloženej voľby sa overí pred touto normalizáciou.

Publisher po začatí merge používa zmrazený výsledok a známe checkpointy z `04`; svoj zápis nesmie mylne považovať za cudziu zmenu. Pre jednoduchosť sa preklikateľný náhľad v `MERGING`/`MERGE_FAILED` neotvára, UI ponúkne priebeh a produkciu. `MERGED`/`CANCELLED` ponúknu archívny detail zmien, nie historický náhľad celého webu. Náhľad je dostupný v `DRAFT`, `SCHEDULED`, `NEEDS_ATTENTION`, podľa platnosti rozhodnutí.

Základ porovnania `base_snapshot` sa v stránke nezobrazuje, ale vstupuje do rozhodovania mapy. Náhľad neprepisuje tento základ novšou produkciou.

`BranchView` postupne načíta:

- produkčný strom domény a pracovné zmeny priečinkov;
- výsledné základné metadáta stránok potrebné pre menu a URL;
- obsah konkrétnych stránok až pri ich použití;
- médiá pri požiadavke komponentu alebo editora.

Načítanie používa oddelené objekty. Pri produkčnom fallbacks možno čerpať z existujúcich cache, ale pred zmenou odvodených údajov alebo vrátením objektu do editora sa vytvorí kópia. V prvom kroku netreba do pamäte načítať HTML celého webu.

Médium zahŕňa aj pracovný zoznam priradení do existujúcich kategórií z `group_ids_snapshot`. Pre zvolený WORK čítač nesmie nahradiť tento zoznam produkčnou M:N väzbou; pri LIVE/no-op naopak použije aktuálne produkčné priradenia. Kategórie zostávajú produkčným číselníkom; ich definície sa vo vetve nemenia. Filtrovanie podľa kategórie musí použiť výsledné členstvo pred count/radením/stránkovaním, podľa postupu v [dátovom modeli](02-datovy-model.md).

Strom sa zostaví v dvoch krokoch: najskôr hodnoty a väzby podľa ID, potom odvodené cesty, URL a navigačné údaje. Tým sa vyhne rekurzívnemu vytváraniu `DocDB`/`GroupsDB` pri výpočte navbaru. Odstránený podstrom a presuny musia zodpovedať operáciám pripraveným zapisovacou službou; náhľad nesmie ponechať osirelé položky ako náhodný fallback.

Pri editácii zostávajú dostupné produkčné šablóny a číselníky. Zmena definície šablóny sa vo vetve neukladá. Referencia na zdieľaný obsah mimo rozsahu jednej domény sa môže čítať produkčne podľa existujúceho oprávnenia; úprava takého obsahu musí byť serverom odmietnutá podľa rozsahu určeného dátovým návrhom.

## 5. Zapojenie DocDB a GroupsDB bez zmeny verejných ID

[DocDB](../../../src/main/java/sk/iway/iwcm/doc/DocDB.java) aj [GroupsDB](../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java) majú súkromné konštruktory, spoločné inštancie a veľa metód pracujúcich priamo s ich kolekciami. Navrhuje sa ponechať existujúce signatúry a rozlíšiť produkčnú inštanciu od fasády jedného vetvového requestu.

- Produkčné `getInstance()` vracia dnešnú spoločnú inštanciu.
- Vetvové `getInstance()` vracia fasádu uloženú v `BranchView`.
- Nový interný konštruktor fasády prijme view. Nespustí produkčné preloady, publikovanie, cluster refresh ani mazanie spoločnej cache.
- Kolekcie priečinkov, základných stránok a URL vo fasáde patria view. Existujúce výpočty stromu a menu tak pracujú nad správnymi vstupmi.
- Metódy s priamym SQL majú explicitnú vetvovú cestu; samotná alternatívna inštancia ich SQL nezmení.
- `getInstance(true)` vo vetvovom kontexte obnoví view requestu. Produkčný refresh po merge vykonáva produkčný tok.

Nejde o subclass s prepisovaním desiatok metód. Ide o druhý interný režim existujúcej fasády s jednoznačným čítačom a vlastnými dátami. Pri implementácii je potrebné overiť, že inicializácia produkčného singletonu nikdy nezachytí aktívny vetvový kontext.

| Trieda/metódy | Navrhovaná zmena |
| --- | --- |
| `DocDB.getDoc` | Vetvový detail, fallback na produkciu bez zápisu pracovného objektu do produkčnej cache. `useCache=false` naďalej znamená bezpečný samostatný objekt na úpravu. |
| `getBasicDocDetails`, `getBasicDocDetailsByGroup` | Výsledné metadáta, vrátane vložení, presunov a zmazaní. |
| `getVirtualPathDocId`, `getDocIdFromURLImpl`, `getDocLink` | Rovnaká výsledná URL mapa a strom; vrátená logická URL sa na preview URL premení až na výstupe. |
| `getDocByGroup`, `getDocPerex` a ich count cesty | Parametrizovaný výsledný SQL zdroj pred filtrovaním a stránkovaním. |
| `GroupsDB.getGroup`, `getGroups`, `getGroupsAll`, `getGroupsTree`, rodičia a URL helpery | Použitie výsledného grafu view. |
| `GroupsDB.getParentGroupsCached`, `getAllSystemFolders` | Vo vetve obísť existujúce spoločné `Cache` kľúče a použiť request-local view. Tieto cache existujú aj mimo singleton kolekcií. |
| `menu_ul_li.jsp` → `MenuULLI.doTree`, `getGroupTree`, `hasChilds` | Povinná integrácia podľa [03b — Menu](03b-menu.md): rovnaké fasády v celej rekurzii, účinné korene/default stránky, viditeľnosť, aktívna cesta, perex/HTML a request premenné. Samotné prekrytie `getDoc` nestačí. |
| `DocDB.getInstance` a publikačná kontrola | Vo vetvovom režime nespúšťa `DocPublishService.checkWebpagesToPublish`. Produkčná cesta zostáva zachovaná. |
| Zápisové metódy a `updateInternalCaches` | Pracovný zápis používa príslušnú vetvovú službu. Náhodné volanie produkčnej mutácie vo vetve sa odmietne; nevytvorí neúplný produkčný zápis. |

Treba prejsť aj static helpery, ktoré obchádzajú inštanciu. Integračný test iba `getDoc` nie je dokončením tohto kroku. Konkrétny inventár a jeho uzavretie patrí do implementačnej úlohy podľa [analýzy](analyza-citanie-a-nahlad.md).

`MenuULLI` ostáva existujúcim rendererom v balíku komponentu; nevzniká samostatné branch menu. Zmenený strom a stránky sa skladajú pred jeho výpočtom z rovnakého `BranchView` a mapy Q16 ako zvyšok náhľadu. Dokument [03b](03b-menu.md) určuje konkrétne úpravy JSP/triedy, zachované parametre a povinné M01–M11 pre T03. Úspešný admin strom ani všeobecný test metadát nenahradia preklik cez výsledné JSP menu.

Globálne pomocné údaje administrácie sa nezužujú na vetvu: napríklad [LayoutService](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) dnes získava ponuku domén cez `GroupsDB.getUserRootDomainNames`. Tento zoznam sa naďalej odvodzuje z produkcie a oprávnení, aby po výbere vetvy nezmizla možnosť prepnúť doménu. Zdieľané System referencie sa čítajú podľa dohodnutého read-only pravidla; nepatria medzi nové vetvové zápisy.

## 6. SQL zdroj pred filtrom a stránkovaním

Výsledný zdroj stránok vznikne ako produkčné riadky bez prekrytých ID plus pracovné riadky bez zmazaní. ADMIN prekryje všetky pracovné ID; PREVIEW iba ID so zvoleným WORK podľa mapy v časti 4. Nasledujúci zápis je vysvetľujúci pseudokód; stĺpce a doménový rozsah určuje dátový model:

```sql
SELECT d.<explicitny_zoznam_stlpcov>
FROM documents d
WHERE d.group_id IN (<povoleny_vysledny_rozsah>)
  AND NOT EXISTS (
      SELECT 1
      FROM branch_documents b
      WHERE b.branch_id = ? AND b.doc_id = d.doc_id
        AND <b.doc_id_je_v_mnozine_WORK_pre_tento_rezim>
  )
UNION ALL
SELECT b.<pracovne_stlpce_v_rovnakom_poradi>
FROM branch_documents b
WHERE b.branch_id = ?
  AND <b.doc_id_je_v_mnozine_WORK_pre_tento_rezim>
  AND b.operation <> 'DELETE'
  AND b.group_id IN (<povoleny_vysledny_rozsah>)
```

Produkčná časť musí rešpektovať aj spôsob rezervácie nových ID z dátového návrhu. Prípadný technický rezervačný záznam sa nesmie stať výslednou produkčnou stránkou.

Predikát pracovnej množiny sa zostaví z overenej request mapy ako viazané ID; prázdna množina znamená čistý LIVE zdroj, nie neplatné `IN ()`. SQL bind limity sa dodržia bežným rozdelením množiny podľa DB. Vybraný LIVE objekt ostane v produkčnej časti a jeho WORK sa nevráti. Rovnaký postup platí pre médiá. Nevykonáva sa dodatočné skladanie už odstránkovaného výsledku.

Nad touto reláciou sa použije existujúci význam dotazu:

```sql
SELECT <pozadovane_stlpce>
FROM (<vysledny_zdroj>) d
LEFT JOIN users u ON u.user_id = d.author_id
WHERE <dostupnost, datum, prava, filtre>
ORDER BY <povolene_poradie>, d.doc_id
<strankovanie_podla_DB>
```

Nepoužívať `SELECT *`: technické stĺpce vetvy do projekcie nepatria a poradie oboch častí musí byť rovnaké. Vetvový zdroj doplní prevádzkové polia, ktoré sa nemajú publikovať zo snapshotu, podľa pravidiel dátového modelu; napríklad aktuálne počítadlá sa nesmú neúmyselne vrátiť na starú hodnotu.

`UNION ALL` je zámerný aj kvôli obsahovým CLOB poliam. Oracle LOB dokumentácia výslovne povoľuje túto operáciu, ale nie bežný `UNION` alebo `DISTINCT` nad CLOB. Konkrétnu projekciu, joins a stránkovanie musí P01 overiť na podporovaných verziách; HTML sa kvôli zjednodušeniu dotazu nesmie orezať na krátky VARCHAR. [Oracle 19c: SQL Semantics and LOBs](https://docs.oracle.com/en/database/oracle/oracle-database/19/adlob/SQL-semantics-and-LOBs.html).

`BranchSql` vracia SQL fragment spolu s bind parametrami. Názvy stĺpcov a tabuliek pochádzajú z kódu; ID vetvy, hodnoty filtrov a rozsahy sa bindujú. Nejde o zachytávač alebo textový prepis všetkých SQL dotazov v aplikácii.

V [NewsQuery](../../../src/main/java/sk/iway/iwcm/components/news/NewsQuery.java) treba upraviť `getSelectSql`, `getNewsList`, `getNewsCount` a viazanie argumentov tak, aby používali rovnaký zdroj. `setArguments` musí vedieť nadviazať na index po parametroch zdroja. Podrobný záväzný kontrakt pre `NewsApp`/`NewsActionBean`, kritériá, čas, perex skupiny, jedinečné ID pred paging a konzistentné počty je v [03a](03a-novinky.md). Štandardné `DatabaseCriteria` sa podporujú. Zákaznícke `ResultCriteria` v prvom preview rozsahu vrátia vysvetlenie nepodporovaného rozšírenia; ich LIVE API sa nemení a nepridáva sa skrytá zmena významu vlastných filtrov.

`doc_atr` a `doc_atr_def` sa čítajú produkčne a ich priradenia sa vo vetve neupravujú. Nová stránka nemá vlastné atribúty. Server odmietne pokus meniť ich cez skrytý alebo ručne vytvorený request. Perex kategorizácia je po doplnení N1 podporovaná cez účinné `d.perex_group`; Novinky nesmú pripojiť zastarané produkčné priradenia `perex_group_doc` k pracovnej stránke. Tento odvodený index sa obnoví až pri merge a definície perex skupín zostanú produkčné.

## 7. JPA a administračné zoznamy

`DocDetailsRepository` zostáva produkčným repository. Menenie jeho `@Table`, pripojenia alebo globálneho filtra podľa session by zasiahlo ďalšie aplikácie a úlohy na pozadí. Entity pracovných tabuliek majú vlastné repository a registráciu v existujúcom JPA scan nastavení.

Vetvový výsledok natívneho dotazu sa mapuje do nového `DocDetails` pomocou overeného row mappera. Nepoužiť mapovanie výsledku do spravovanej produkčnej entity s rovnakým ID, ktorá by mohla skončiť v JPA cache alebo sa zapísať pri flush.

Konkrétne rozdelenie v existujúcom kóde:

| Vstup | Produkcia | Vetva |
| --- | --- | --- |
| [WebpagesDatatable.getAllItems](../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java) a [WebpagesService.getAllItems](../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java) | Dnešný JPA tok. | `BranchDocumentQuery`, potom spoločné `preparePage` a redakčné options. |
| `WebpagesDatatable.findByColumns` | Dnešné JPA specifications. | Zachytiť pred volaním `super.findByColumns`; filter vyhodnotiť nad výslednou SQL reláciou. |
| `getOneItem` a `WebpagesService.getOneItem` | Dnešný detail/história. | Pracovný detail alebo produkčný fallback v oddelenom objekte; pracovný režim sa nemieša s `historyId`. |
| `insertItem`, `editItem`, `deleteItem`, `processAction` | Dnešný zapisovací tok. | Explicitná vetvová služba. Nepodporované hromadné operácie sa odmietnu pred zápisom. |
| [GroupsTreeService.getItems](../../../src/main/java/sk/iway/iwcm/doc/GroupsTreeService.java) | Dnešné dáta. | Rovnaká tvorba uzlov a kontrola práv nad výsledným grafom. |
| [GroupsTreeRestController](../../../src/main/java/sk/iway/iwcm/doc/GroupsTreeRestController.java), presun/uloženie/zmazanie | Dnešné služby. | Vetvové presuny a prepočty bez produkčných vedľajších účinkov. |

JPA `Specification` nemožno automaticky vykonať nad ľubovoľným `UNION` dotazom. Pre vetvové obrazovky sa preto normalizujú parametre DataTables na malý interný `DocumentFilter` a povolené triedenie. Existujúce spracovanie typov a operátorov sa použije tam, kde je oddeliteľné od JPA; do služby sa neprenášajú neoverené názvy stĺpcov.

Minimálny podporovaný rozsah musí zodpovedať bežnému editoru: aktuálny alebo rekurzívny priečinok, oprávnené stránky, názov, ID, URL, dostupnosť, dátumy, autor a viditeľné obsahové stĺpce. Pre aktívne ovládacie prvky musí fungovať rovnaké filtrovanie ako v produkcii. Špeciálne administratívne zoznamy založené na histórii/schvaľovaní nie sú pracovným prehľadom vetvy; UI ich musí jasne oddeliť. Neimplementovaný filter sa nesmie ticho ignorovať.

Pri výstupe zoznamu zachovať `DATA_NOT_LOADED`. Detail na uloženie sa musí vždy načítať v úplnej podobe; vetva nesmie urobiť z neúplného riadku tabuľky uložený HTML obsah.

## 8. Klikateľný preview gateway

Vo vnútri renderovania sa používa logická cesta `/kampan/ponuka.html`. Až výstup sa preloží na `/admin/webpages/branch-preview/42/kampan/ponuka.html`. Uložené HTML, `virtual_path`, `media_link` ani parametre komponentov sa týmto prefixom nemenia.

`BranchPreviewUrls` vykoná jeden spoločný prevod:

| Vstup | Správanie |
| --- | --- |
| Relatívna URL stránky alebo assetu | Vyhodnotí sa voči logickej verejnej URL aktuálneho dokumentu a prevedie do gateway na admin hostiteľovi. |
| Koreňová URL podporovanej stránky alebo assetu | Dostane gateway prefix na admin hostiteľovi. |
| Absolútna HTTP(S) URL logickej verejnej domény vetvy | Prevedie sa na admin host a gateway tejto vetvy. |
| `#sekcia`, `mailto:`, `tel:`, `data:`, externý hostiteľ | Zostáva bez zmeny. |
| URL už v tej istej gateway | Nepridáva sa druhý prefix. |
| Interná admin alebo zakázaná cesta | Nepovažuje sa za preview resource; platia bežné pravidlá prístupu. |

Odkazy na inú verejnú doménu ostanú externými odkazmi; nepreberajú pracovný kontext cudzieho webu. Helper rozlišuje logickú verejnú doménu vetvy od transportného admin hosta a neodvodzuje prvú zo samotného `request.getServerName()`. Admin navigácia sa neprepisuje na obsah webu. Query parametre, fragment, UTF-8 a context path sa zachovajú. Redirect odpoveď aj `sendRedirect` použijú ten istý helper, aby kanonické presmerovanie nevyhodilo používateľa z náhľadu.

Response wrapper spracuje HTML parserom, nie globálnym nahradením reťazca. Pokryje odkazy, `src`, relevantné `href`, `srcset`, `poster`, lokálne `form[action]`/`formaction`, známe lazy-load atribúty používané core šablónami a prípadný `<base>`. Core komponenty generujúce URL v JSON použijú helper priamo. Spracovanie URL v inline štýloch a JavaScriptom dopĺňaných atribútoch musí mať testované pravidlá pre podporované šablóny.

V gateway sa nesmie vykonávať všeobecné odosielanie formulárov ani ďalšie produkčné mutácie aplikácií. Náhľad slúži na zobrazenie; zapisovacie akcie webových komponentov majú v náhľade zrozumiteľný blokovaný stav. Core formuláre vypnú submit ovládanie a ich lokálny action smeruje do gateway, ktorá zapisovací request odmietne aj po manuálnom opätovnom zapnutí tlačidla. Core JavaScriptové odosielanie používa rovnaký preview helper; zákaznícky skript s pevnou produkčnou URL potrebuje adaptér. Redakčné ukladanie prebieha cez autorizované admin endpointy mimo gateway.

Core náhľad tiež nezapisuje produkčnú návštevnosť: vstup `StatDB.add` v `ShowDoc` a súvisiace core meranie overia preview kontext. Bežné technické logovanie chýb zostáva. Vlastné analytické skripty šablóny sa vypnú jej preview adaptérom; samotné serverové `isPreview` nemôže zastaviť ľubovoľný JavaScript posielajúci údaje externej službe.

Toto pravidlo jadra nerozširuje budúcu úlohu bannerov o prestavbu ich štatistík. Používateľ pri N2 výslovne určil, že zapisovanie bannerových štatistík sa riešiť nemusí; rozsah príslušného modulu a jeho akceptácia sú v [08](08-rozsirovanie-modulov.md).

### Súbory, obrázky a thumbnaily

`/images/...` a `/files/...` v preview URL prejdú gateway pred priamym statickým servovaním. Súborová služba vyberie pracovný obsah, pracovné zmazanie alebo produkčný fallback podľa logickej cesty. Neposiela klientovi fyzickú staging cestu.

Výsledky thumbnailov majú vetvový priestor oddelený od produkcie; kľúč obsahuje aspoň ID vetvy, hash zdrojového obsahu a transformačné parametre. Gate chráni originál aj thumbnail. Detail napojenia na existujúce služby je v [analýze súborov](analyza-media-a-subory.md).

Správny výber súboru je potrebný aj **pred** vytvorením HTML, nielen pri HTTP stiahnutí. `MediaDB.filterByAvailability` a `isExists` overujú lokálne prílohy cez LIVE filesystem; v branch musia použiť existenciu z file overlay, inak nový pracovný PDF z výpisu zmizne. `DocBasic.getPerexImageSmall/Normal/Original` cez [GalleryToolsForCore](../../../src/main/java/sk/iway/iwcm/common/GalleryToolsForCore.java) vyberá variant podľa existencie; rovnaké pravidlo platí aj tam. [ThumbTag](../../../src/main/java/sk/iway/iwcm/tags/ThumbTag.java) v preview obíde `thumbExternalServer` a vždy vydá chránenú gateway URL. Tieto konkrétne core čítače dostanú malý file-read/exists helper, nie globálny prepis `Tools.getRealPath` ani všeobecný virtuálny filesystem. Časový filter pracovného média použije výsledné `dateCreated` vlastníckej stránky podľa dátového modelu.

Zákaznícka šablóna, ktorá sama používa `java.io.File`, `IwcmFile` alebo vlastnú CDN URL, potrebuje explicitný adaptér rovnako ako vlastné SQL. HTML rewrite nedokáže obnoviť obrázok či prílohu, ktorú server predtým z výstupu vyradil.

Rozsah súborov sa určí existujúcim mapovaním domény na úložisko a dnešnými adresárovými právami pod povolenými obsahovými koreňmi. Nepridáva sa nový register vlastníctva ciest. Ak rovnaký fyzický adresár používajú viaceré domény, UI upozorní na možný účinok publikovania aj mimo domény vetvy; prvá verzia neskenuje všetky referencie iných webov.

Nemenné CSS/JS šablóny môžu byť v preview obslúžené ako povolené produkčné zdroje. Ich definície sa vo vetve nemenia. Cesta gateway zabezpečuje odlišnú browser URL od produkcie; `no-store` bráni používaniu starého pracovného obsahu po ďalšej úprave.

Reverse proxy/CDN na admin hostiteľovi musí cestu `/admin/webpages/branch-preview/` posielať aplikácii bez verejnej cache a bez priameho servovania `/images` či `/files` podľa prípony. Táto konfigurácia je súčasťou zapnutia preview na konkrétnom nasadení. Bežné produkčné pravidlá sa nemenia.

### Hranica CSS a dynamického JavaScriptu

Relatívne URL vo vloženom CSS sa často vyriešia správne z gateway cesty CSS súboru. Koreňové `url(/images/...)`, JavaScript `fetch('/...')`, runtime vytvorený iframe alebo vlastný API klient však môžu gateway obísť. Návrh nesľubuje automatický bezpečný prepis ľubovoľného CSS/JavaScriptu.

Overovací krok P01 musí pokryť predvolené podporované šablóny: ich root-relative assets a dynamické požiadavky sa prispôsobia spoločným helperom alebo malým explicitným adaptérom. Zákaznícka šablóna s takýmto kódom potrebuje rovnaké overenie. Túto hranicu treba uviesť v dokumentácii integrácie; náhľad s neadaptovaným komponentom sa nesmie vydávať za úplnú reprezentáciu výsledného webu.

## 9. Minimálne rozhranie pre zákaznícky komponent

Zákaznícky komponent nemá priamo čítať pracovné tabuľky ani si vyrábať vlastný názov gateway. Dostane malé čítacie rozhranie a samostatný URL helper. Navrhované signatúry:

```java
public interface BranchReader {
    Optional<DocDetails> findDocument(int docId);
    Optional<GroupDetails> findGroup(int groupId);
    List<GroupDetails> childGroups(int parentGroupId);
    Page<DocDetails> findDocuments(DocumentFilter filter, Pageable pageable);
    List<Media> findMedia(int docId);
}

public interface ContentPreviewUrls {
    String toPreviewUrl(String logicalUrl, HttpServletRequest request);
}
```

`BranchReadService` môže implementovať prvé rozhranie; druhé implementuje spoločný helper. Ide o návrh API, uvedené triedy ešte neexistujú. Vstupy pre filtre tvoria typované polia a povolené operátory, nie ľubovoľný SQL text. Rovnaké rozhranie v produkčnom režime číta produkciu. `findDocument` nevydáva produkčný objekt, ak je dané ID vo vetve označené ako zmazané.

Pre špecifický komponent, ktorý potrebuje vlastné SQL joins, je interným integračným bodom:

```java
SqlFragment documentSource(BranchContext context, String alias);

public interface SqlFragment {
    String sql();
    int bind(PreparedStatement statement, int firstIndex) throws SQLException;
}
```

`alias` je konštanta adaptéra, nie vstup používateľa. `SqlFragment` nesie SQL aj parametre konkrétneho fragmentu; implementácia viazania k nemu patrí. Adaptér nahradí iba zdroj `FROM documents d`, zachová vlastné joins a filtre a vykoná ich nad výsledným obsahom. Musí osobitne používať výsledný strom, ak svoj rozsah skladá z priečinkov.

Tieto helpery sú určené pre vedome upravený komponent. Neadaptované priame SQL ďalej číta produkciu. Prvá verzia nesleduje a neprepisuje všetky príkazy od zákazníckych modulov.

Všeobecné fulltextové, Lucene a sémantické vyhľadávanie v náhľade naďalej používa produkčné dáta/index. Nie je súčasťou garantovaného vetvového menu/noviniek/navigácie/médií. Vyhľadávací komponent musí tento rozsah v preview označiť alebo dočasne skryť ovládanie; nesmie predstierať výsledky pracovnej vetvy. Produkčný index sa aktualizuje až pri merge.

## 10. Navrhované riadiace API

Presné endpointy zoznamu balíkov a merge patria do príslušných dokumentov. Pre kontext a náhľad postačuje:

| Endpoint | Výsledok |
| --- | --- |
| `GET /admin/rest/branches` | Dostupné neuzavreté verzie aktuálnej admin domény pre prepínač, vrátane názvu a stavu. Prehľad môže výslovne vyžiadať aj uzavreté verzie cez `includeClosed=true`. |
| `POST /admin/rest/branches/editor` s DataTable akciou `create` | Jediná cesta vytvorenia pracovnej verzie cez štandardný Editor. Doménu nastavuje server; `beforeSave` inicializuje prázdnu `DRAFT` verziu bez termínu a bez snapshotu webu. Odpoveď má štandardný DataTable kontrakt. Aktívny admin výber sa nemení. Samostatný `POST /admin/rest/branches` nie je podporovaný. |
| `GET /admin/rest/branches/context` | Produkcia alebo zvolená vetva pre aktuálnu doménu, názov a aktuálne oprávnenia. |
| `POST /admin/rest/branches/context` s `{ "branchId": 42, "expectedDomainId": 123 }` | Overenie domény otvorenej obrazovky a zmena výberu v administrácii; `0` vyberie produkciu. Nesúlad domény vráti `409` aj pri voľbe `0`. Existujúca CSRF ochrana zostáva. |
| `GET /admin/rest/branches/42/preview-link?docId=123` | Overená URL na výslednú stránku vetvy; pri zmazanej/nedostupnej položke vysvetlená chyba. |
| `GET /admin/webpages/branch-preview/42/...` | Chránený náhľad stránky alebo podporovaného assetu. |

Server odpovedá `403` pri nedostatočnom oprávnení, `404` pri neexistujúcom zdroji v príslušnom kontexte a `409` pri neplatnom stave alebo nesúlade administračnej obrazovky s výberom. Chyby používať v existujúcom štýle REST/DataTables, bez nového všeobecného API obalu.

UI `Vytvoriť` otvorí štandardný DataTable editor na stránke Pracovné verzie. `Upravovať web` následne použije POST `/context` s ID označeného riadka. Rovnaký endpoint výberu používa prepínač. Pri chybe sa aktívny indikátor nezmení. Vybraný neuzavretý stav môže dovoliť iba čítanie; voľba kontextu sama nemení životný cyklus vetvy. Po uzavretí aktívnej verzie sa zápis odmietne a UI ponúkne explicitný prechod na produkciu; starý request sa nikdy automaticky nepresmeruje na LIVE. Konkrétne stavy, formulár a navigáciu určuje [návrh UI](05-rozhranie.md).

## 11. Overenie návrhu pri realizácii

P01 má potvrdiť praktickú priechodnosť najrizikovejšej cesty skôr, než sa dokončí celý merge nástroj:

1. Existujúca stránka a priečinok majú pod rovnakými ID pracovnú verziu. Produkcia zostane pôvodná.
2. Gateway vykreslí stránku, prejde cez menu na druhú stránku a zvládne kanonické presmerovanie po zmene URL.
3. Štandardné novinky správne zahrnú novú položku, vylúčia zmazanú a spočítajú filtrované stránkovanie.
4. Obrázok a existujúci thumbnail ukážu pracovné bajty iba cez autorizovanú gateway.
5. Produkcia a náhľad otvorené vedľa seba nepoužívajú vzájomné cache; zmena admin výberu nezmení otvorený preview odkaz.
6. Predvolená šablóna prejde kontrolou bežných, root-relative aj dynamických URL. Zistené adaptéry sa zapíšu do inventára podpory.

Ďalšie integračné testy musia overiť:

- authorizačné a doménové kontroly pred načítaním náhľadu, vrátane priameho asset URL;
- vyčistenie kontextu po chybe a nasledujúci produkčný request na tom istom serverovom threade;
- preview domény B na admin hoste pri session výbere domény A: route, `DocDB.getDomain`, `RequestBean`, médiá a file mapovanie musia čítať B bez zmeny session; login/redirect zostane na admin hoste;
- starú produkčnú záložku A po prepnutí admin domény na B: `branchId=0` nesmie obísť doménový `409`;
- nové pracovné PDF pri zapnutej kontrole dostupnosti médií, variant perex obrázka a `thumbExternalServer`: výstup musí používať branch súbory, nie produkčnú existenciu/cache;
- no-store aj po downstream statických hlavičkách, chybovej odpovedi a conditional requeste;
- strom s presunom, predvolenou stránkou a odmietnutím cyklu/osirelej väzby podľa zapisovacej validácie;
- SQL binding, count, poradie a stránkovanie na skutočne podporovaných databázach;
- admin detail/list/strom s rovnakým kontextom, neúplný list obsah a odmietnutie starého formulára po prepnutí vetvy;
- uloženie nového ID a jeho použitie v odkazoch bez prepisovania pri merge;
- produkčné publikovanie bez prieniku vetvového kontextu;
- odmietnutie úprav atribútov a ostatných väzieb mimo rozsahu;
- obídenie `WriteTag` a `CacheTag` cache pri preview, bez produkčného cache zápisu;
- jasnú chybu pre neplatnú gateway cestu a blokované zapisovacie akcie verejných aplikácií.

Dokončenie implementačného kroku znamená funkčné podporované cesty a uvedené overenia, nie iba kompiláciu nových tried. Tento dokument sám žiadny z týchto testov nevykonal.
