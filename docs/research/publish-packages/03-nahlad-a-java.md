# 03. Kontext vetvy, Java rozhrania a náhľad webu

Stav: **technický návrh na spoločné posúdenie**. Tento dokument určuje navrhované správanie a integračné body; neobsahuje vykonanú implementáciu. Overený súčasný stav je v [analýze čítania](analyza-citanie-a-nahlad.md), súbory a médiá v [samostatnej analýze](analyza-media-a-subory.md).

Vychádza z potvrdeného rozsahu: živý overlay jednej domény, postupné zlúčenie, obsahové súbory, stabilné ID aj nových objektov, štandardné menu/novinky/navigácia/médiá, náhľad pre prihlásených administrátorov a prispôsobenie vlastných komponentov cez adaptér. Úpravy súvisiacich tabuliek mimo dohodnutého rozsahu nie sú súčasťou vetvy. Publikovať celý balík smie používateľ s potrebnými právami ku všetkým položkám.

## 1. Rozdelenie zodpovednosti

Produkčné `documents`, `groups` a `media` zostanú produkčným zdrojom. Pracovné verzie budú v piatich tabuľkách návrhu dátového modelu: `content_branch`, `content_branch_document`, `content_branch_group`, `content_branch_media`, `content_branch_file`. Pracovné záznamy majú pôvodnú identitu objektu, typ operácie, základ porovnania a typované pracovné polia. Súbory majú logickú cestu, odtlačky a interné umiestnenie pracovného obsahu. Presné stĺpce a prideľovanie ID určuje dokument dátového modelu.

Tento dokument pokrýva čítanie výsledného obsahu. Ukladanie pracovnej verzie a produkčné zlúčenie sú samostatné vstupy: načítanie vetvového `DocDetails` nikdy samo osebe neoprávňuje zavolať produkčné `DocDetailsRepository.save`.

Navrhované nové Java triedy patria do balíka `sk.iway.iwcm.components.contentbranch`. DTO a entity patria k tomuto balíku, nie do novej všeobecnej CMS vrstvy.

| Navrhovaná trieda | Úloha |
| --- | --- |
| `ContentBranchContext` | Overený kontext jedného requestu: režim, ID vetvy, doména a používateľ. |
| `ContentBranchFilter` | Vyhodnotí admin kontext alebo preview gateway, skontroluje prístup a nastaví kontext pred routovaním. |
| `ContentBranchView` | Výsledný strom, základné metadáta, URL mapa a načítané pracovné záznamy počas jedného requestu. |
| `ContentBranchReadService` | Načíta produkciu a pracovné verzie, zostaví výsledné objekty; poskytne aj rozhranie zákazníckym komponentom. |
| `ContentBranchSql` | Zostaví parametrizovaný zdroj stránok/médií pre podporované SQL zoznamy. |
| `ContentBranchDocumentQuery` | Administračné filtre a stránkovanie nad výsledným zdrojom stránok. |
| `ContentBranchPreviewUrls` | Prevod logickej URL do preview gateway; používa sa aj pre komponenty a presmerovania. |
| `ContentBranchPreviewResponseWrapper` | Pre podporovaný HTML výstup upraví navigačné a asset URL bez zmeny uloženého obsahu. |
| `ContentBranchRestController` | Výber vetvy, údaje o aktívnom kontexte a odkaz na náhľad. Ostatné riadiace operácie definujú dokumenty UI a merge. |

`ContentBranchView` je obyčajný objekt requestu. Nepotrebuje vlastný scheduler, zámky ani spoločný distribuovaný cache systém. Výsledné `DocDetails` a `GroupDetails` sú oddelené objekty, ktoré nemajú väzbu na produkčný JPA persistence context ani zdieľané produkčné cache.

Pre časové filtre Noviniek view uchová jeden request údaj `previewAt`: budúci termín `publish_at` v stave `SCHEDULED`, inak čas začiatku requestu. Nejde o nový DB stĺpec ani globálne prepisovanie času. Presné pravidlá a označenie v náhľade určuje [03a — Novinky](03a-novinky.md).

Koordináciu a spoločný prehľad rozšíri navrhovaný `ContentBranchTypeHandler` s identitou `type + objectKey` a spoločným obalom `BranchChange` podľa [08](08-rozsirovanie-modulov.md). Dnešné typy používajú vlastné pracovné tabuľky; budúce moduly môžu použiť jednu spoločnú JSON tabuľku. Tento kontrakt nenahrádza modulové čítanie: detail/zoznam/render sa zapoja explicitným adaptérom, nie zachytávaním všetkých SQL/JPA volaní.

## 2. Dva odlišné kontexty používateľa

### Administrácia

Výber v administrácii sa uloží do session pre vybranú doménu. Zmena domény vyberie produkciu; novú vetvu používateľ vyberie výslovne. Prepínač obnoví otvorenú obsahovú obrazovku.

Každá podporovaná administračná obrazovka dostane pri načítaní `contentBranchId`; hodnota `0` znamená produkciu. Jej AJAX požiadavky vrátane uploadu posielajú túto hodnotu ako hlavičku `X-WebJET-Content-Branch`, prípadne ako multipart pole s rovnakým významom. Server ju porovná s aktuálnym výberom v session a doménou. Nesúlad vráti `409` s pokynom obnoviť obrazovku. Chýbajúci údaj v pracovnom režime sa nesmie interpretovať ako súhlas so zápisom do produkcie.

Táto kontrola rieši bežný prípad dvoch otvorených záložiek: prepnutie vetvy v jednej záložke nezmení nepozorovane cieľ uloženia formulára v druhej. Nerieši súbežné editovanie pomocou zámkov.

Riadiace endpointy `/admin/rest/content-branches/**` používajú explicitné ID cieľovej vetvy a produkčný technický kontext. To je potrebné najmä pri porovnávaní a merge: produkčné čítanie nesmie náhodou vrátiť pracovnú verziu len preto, že ju má používateľ vybranú v editore.

### Verejný náhľad

Náhľad používa vlastnú cestu:

```text
Produkcia: https://www.example.sk/kampan/ponuka.html
Náhľad:    https://www.example.sk/__branch-preview/42/kampan/ponuka.html
```

ID vetvy v URL nie je prihlasovací token. Server pri každom requeste overí riadne prihláseného administrátora, oprávnenie na náhľad a príslušnosť vetvy k doméne. Prihlásenie prebieha existujúcim spôsobom na tom istom hostiteľovi. Anonymné zdieľanie náhľadu ani prenos prihlasovania medzi doménami nie sú súčasťou prvej verzie.

Preview cesta je nezávislá od výberu v administrácii. Používateľ môže mať vedľa seba produkciu a náhľad dvoch vetiev; náhľad vetvy 42 zostane vetvou 42 aj po prepnutí editora na inú vetvu. Bežná produkčná URL nikdy nezíska vetvový obsah iba podľa session.

## 3. Filter a životnosť requestu

V [V9SpringConfig.configureSecurity](../../../src/main/java/sk/iway/webjet/v9/V9SpringConfig.java) už existuje poradie `ApiTokenAuthFilter` → `SetCharacterEncodingFilter` → `FileArchivFilter`. `SetCharacterEncodingFilter` pripravuje používateľa a `RequestBean`. Servletový [PathFilter](../../../src/main/java/sk/iway/iwcm/PathFilter.java) následne rieši virtuálne URL.

Navrhované poradie je:

```text
existujúce prihlásenie a RequestBean
  → ContentBranchFilter
  → existujúce ďalšie filtre
  → PathFilter
  → ShowDoc / komponent / podporovaný asset
```

Registráciu treba overiť integračným testom reálneho filter chainu. Samotná existencia Spring beanu nezaručuje správne poradie ani zahrnutie statických súborov.

`ContentBranchFilter` vykoná tieto kroky:

1. Rozpozná produkčný request, podporovanú admin obrazovku alebo `/__branch-preview/{id}/...`.
2. Pre vetvu načíta jej stav a doménu, overí používateľa a požadovaný režim. Pri chybe skončí bez fallbacku na produkciu.
3. Uloží nemenný `ContentBranchContext` do request atribútu. Pre staršie statické helpery bude dostupný cez existujúci `RequestBean` a jeho request, bez druhého globálneho registra requestov.
4. Pri preview vytvorí request wrapper s logickou cestou bez gateway prefixu. Vonkajšiu URL ponechá v samostatnom atribúte pre spätný prevod odkazov a diagnostiku.
5. Nastaví súkromné `no-store` hlavičky, `isPreview` a ochranu pred indexovaním. Vnorený forward/include používa ten istý kontext a tú istú `ContentBranchView`.
6. V `finally` uvoľní referencie na view a vlastné request atribúty. Prenos kontextu do nového background threadu nie je implicitný.

Navrhovaná reprezentácia:

```java
public record ContentBranchContext(
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

## 4. Výsledný pohľad na obsah

Pre objekt s ID `x` platí:

| Pracovný záznam vo vetve | Výsledok čítania |
| --- | --- |
| Neexistuje | Aktuálny produkčný objekt. |
| Vloženie alebo zmena | Úplný pracovný objekt pod jeho stabilným ID. |
| Zmazanie | Objekt vo výslednom pohľade neexistuje. |

Základ porovnania `base_snapshot` sa pri bežnom náhľade nezobrazuje; slúži na trojcestné porovnanie pri merge. Náhľad neprepisuje tento základ novšou produkciou.

`ContentBranchView` postupne načíta:

- produkčný strom domény a pracovné zmeny priečinkov;
- výsledné základné metadáta stránok potrebné pre menu a URL;
- obsah konkrétnych stránok až pri ich použití;
- médiá pri požiadavke komponentu alebo editora.

Načítanie používa oddelené objekty. Pri produkčnom fallbacks možno čerpať z existujúcich cache, ale pred zmenou odvodených údajov alebo vrátením objektu do editora sa vytvorí kópia. V prvom kroku netreba do pamäte načítať HTML celého webu.

Médium zahŕňa aj pracovný zoznam priradení do existujúcich kategórií z `group_ids_snapshot`. Čítač nesmie pre pracovné médium nahradiť tento zoznam produkčnou M:N väzbou. Kategórie zostávajú produkčným číselníkom; ich definície sa vo vetve nemenia. Filtrovanie podľa kategórie musí použiť výsledné členstvo pred count/radením/stránkovaním, podľa postupu v [dátovom modeli](02-datovy-model.md).

Strom sa zostaví v dvoch krokoch: najskôr hodnoty a väzby podľa ID, potom odvodené cesty, URL a navigačné údaje. Tým sa vyhne rekurzívnemu vytváraniu `DocDB`/`GroupsDB` pri výpočte navbaru. Odstránený podstrom a presuny musia zodpovedať operáciám pripraveným zapisovacou službou; náhľad nesmie ponechať osirelé položky ako náhodný fallback.

Pri editácii zostávajú dostupné produkčné šablóny a číselníky. Zmena definície šablóny sa vo vetve neukladá. Referencia na zdieľaný obsah mimo rozsahu jednej domény sa môže čítať produkčne podľa existujúceho oprávnenia; úprava takého obsahu musí byť serverom odmietnutá podľa rozsahu určeného dátovým návrhom.

## 5. Zapojenie DocDB a GroupsDB bez zmeny verejných ID

[DocDB](../../../src/main/java/sk/iway/iwcm/doc/DocDB.java) aj [GroupsDB](../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java) majú súkromné konštruktory, spoločné inštancie a veľa metód pracujúcich priamo s ich kolekciami. Navrhuje sa ponechať existujúce signatúry a rozlíšiť produkčnú inštanciu od fasády jedného vetvového requestu.

- Produkčné `getInstance()` vracia dnešnú spoločnú inštanciu.
- Vetvové `getInstance()` vracia fasádu uloženú v `ContentBranchView`.
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
| `DocDB.getInstance` a publikačná kontrola | Vo vetvovom režime nespúšťa `DocPublishService.checkWebpagesToPublish`. Produkčná cesta zostáva zachovaná. |
| Zápisové metódy a `updateInternalCaches` | Pracovný zápis používa príslušnú vetvovú službu. Náhodné volanie produkčnej mutácie vo vetve sa odmietne; nevytvorí neúplný produkčný zápis. |

Treba prejsť aj static helpery, ktoré obchádzajú inštanciu. Integračný test iba `getDoc` nie je dokončením tohto kroku. Konkrétny inventár a jeho uzavretie patrí do implementačnej úlohy podľa [analýzy](analyza-citanie-a-nahlad.md).

Globálne pomocné údaje administrácie sa nezužujú na vetvu: napríklad [LayoutService](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) dnes získava ponuku domén cez `GroupsDB.getUserRootDomainNames`. Tento zoznam sa naďalej odvodzuje z produkcie a oprávnení, aby po výbere vetvy nezmizla možnosť prepnúť doménu. Zdieľané System referencie sa čítajú podľa dohodnutého read-only pravidla; nepatria medzi nové vetvové zápisy.

## 6. SQL zdroj pred filtrom a stránkovaním

Výsledný zdroj stránok vznikne ako produkčné riadky bez prekrytých ID plus pracovné riadky bez zmazaní. Nasledujúci zápis je vysvetľujúci pseudokód; stĺpce a doménový rozsah určuje dátový model:

```sql
SELECT d.<explicitny_zoznam_stlpcov>
FROM documents d
WHERE d.group_id IN (<povoleny_vysledny_rozsah>)
  AND NOT EXISTS (
      SELECT 1
      FROM content_branch_document b
      WHERE b.branch_id = ? AND b.doc_id = d.doc_id
  )
UNION ALL
SELECT b.<pracovne_stlpce_v_rovnakom_poradi>
FROM content_branch_document b
WHERE b.branch_id = ?
  AND b.operation <> 'DELETE'
  AND b.group_id IN (<povoleny_vysledny_rozsah>)
```

Produkčná časť musí rešpektovať aj spôsob rezervácie nových ID z dátového návrhu. Prípadný technický rezervačný záznam sa nesmie stať výslednou produkčnou stránkou.

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

`ContentBranchSql` vracia SQL fragment spolu s bind parametrami. Názvy stĺpcov a tabuliek pochádzajú z kódu; ID vetvy, hodnoty filtrov a rozsahy sa bindujú. Nejde o zachytávač alebo textový prepis všetkých SQL dotazov v aplikácii.

V [NewsQuery](../../../src/main/java/sk/iway/iwcm/components/news/NewsQuery.java) treba upraviť `getSelectSql`, `getNewsList`, `getNewsCount` a viazanie argumentov tak, aby používali rovnaký zdroj. `setArguments` musí vedieť nadviazať na index po parametroch zdroja. Podrobný záväzný kontrakt pre `NewsApp`/`NewsActionBean`, kritériá, čas, perex skupiny, jedinečné ID pred paging a konzistentné počty je v [03a](03a-novinky.md). Štandardné `DatabaseCriteria` sa podporujú. Zákaznícke `ResultCriteria` v prvom preview rozsahu vrátia vysvetlenie nepodporovaného rozšírenia; ich LIVE API sa nemení a nepridáva sa skrytá zmena významu vlastných filtrov.

`doc_atr` a `doc_atr_def` sa čítajú produkčne a ich priradenia sa vo vetve neupravujú. Nová stránka nemá vlastné atribúty. Server odmietne pokus meniť ich cez skrytý alebo ručne vytvorený request. Perex kategorizácia je po doplnení N1 podporovaná cez účinné `d.perex_group`; Novinky nesmú pripojiť zastarané produkčné priradenia `perex_group_doc` k pracovnej stránke. Tento odvodený index sa obnoví až pri merge a definície perex skupín zostanú produkčné.

## 7. JPA a administračné zoznamy

`DocDetailsRepository` zostáva produkčným repository. Menenie jeho `@Table`, pripojenia alebo globálneho filtra podľa session by zasiahlo ďalšie aplikácie a úlohy na pozadí. Entity pracovných tabuliek majú vlastné repository a registráciu v existujúcom JPA scan nastavení.

Vetvový výsledok natívneho dotazu sa mapuje do nového `DocDetails` pomocou overeného row mappera. Nepoužiť mapovanie výsledku do spravovanej produkčnej entity s rovnakým ID, ktorá by mohla skončiť v JPA cache alebo sa zapísať pri flush.

Konkrétne rozdelenie v existujúcom kóde:

| Vstup | Produkcia | Vetva |
| --- | --- | --- |
| [WebpagesDatatable.getAllItems](../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java) a [WebpagesService.getAllItems](../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java) | Dnešný JPA tok. | `ContentBranchDocumentQuery`, potom spoločné `preparePage` a redakčné options. |
| `WebpagesDatatable.findByColumns` | Dnešné JPA specifications. | Zachytiť pred volaním `super.findByColumns`; filter vyhodnotiť nad výslednou SQL reláciou. |
| `getOneItem` a `WebpagesService.getOneItem` | Dnešný detail/história. | Pracovný detail alebo produkčný fallback v oddelenom objekte; pracovný režim sa nemieša s `historyId`. |
| `insertItem`, `editItem`, `deleteItem`, `processAction` | Dnešný zapisovací tok. | Explicitná vetvová služba. Nepodporované hromadné operácie sa odmietnu pred zápisom. |
| [GroupsTreeService.getItems](../../../src/main/java/sk/iway/iwcm/doc/GroupsTreeService.java) | Dnešné dáta. | Rovnaká tvorba uzlov a kontrola práv nad výsledným grafom. |
| [GroupsTreeRestController](../../../src/main/java/sk/iway/iwcm/doc/GroupsTreeRestController.java), presun/uloženie/zmazanie | Dnešné služby. | Vetvové presuny a prepočty bez produkčných vedľajších účinkov. |

JPA `Specification` nemožno automaticky vykonať nad ľubovoľným `UNION` dotazom. Pre vetvové obrazovky sa preto normalizujú parametre DataTables na malý interný `DocumentFilter` a povolené triedenie. Existujúce spracovanie typov a operátorov sa použije tam, kde je oddeliteľné od JPA; do služby sa neprenášajú neoverené názvy stĺpcov.

Minimálny podporovaný rozsah musí zodpovedať bežnému editoru: aktuálny alebo rekurzívny priečinok, oprávnené stránky, názov, ID, URL, dostupnosť, dátumy, autor a viditeľné obsahové stĺpce. Pre aktívne ovládacie prvky musí fungovať rovnaké filtrovanie ako v produkcii. Špeciálne administratívne zoznamy založené na histórii/schvaľovaní nie sú pracovným prehľadom vetvy; UI ich musí jasne oddeliť. Neimplementovaný filter sa nesmie ticho ignorovať.

Pri výstupe zoznamu zachovať `DATA_NOT_LOADED`. Detail na uloženie sa musí vždy načítať v úplnej podobe; vetva nesmie urobiť z neúplného riadku tabuľky uložený HTML obsah.

## 8. Klikateľný preview gateway

Vo vnútri renderovania sa používa logická cesta `/kampan/ponuka.html`. Až výstup sa preloží na `/__branch-preview/42/kampan/ponuka.html`. Uložené HTML, `virtual_path`, `media_link` ani parametre komponentov sa týmto prefixom nemenia.

`ContentBranchPreviewUrls` vykoná jeden spoločný prevod:

| Vstup | Správanie |
| --- | --- |
| Relatívna URL stránky alebo assetu | Vyhodnotí sa voči logickej URL aktuálneho dokumentu a prevedie do gateway. |
| Koreňová URL tej istej domény | Dostane gateway prefix. |
| Absolútna HTTP(S) URL tej istej domény | Prevedie sa do gateway toho istého hostiteľa. |
| `#sekcia`, `mailto:`, `tel:`, `data:`, externý hostiteľ | Zostáva bez zmeny. |
| URL už v tej istej gateway | Nepridáva sa druhý prefix. |
| Interná admin alebo zakázaná cesta | Nepovažuje sa za preview resource; platia bežné pravidlá prístupu. |

Query parametre, fragment, UTF-8 a context path sa zachovajú. Redirect odpoveď aj `sendRedirect` použijú ten istý helper, aby kanonické presmerovanie nevyhodilo používateľa z náhľadu.

Response wrapper spracuje HTML parserom, nie globálnym nahradením reťazca. Pokryje odkazy, `src`, relevantné `href`, `srcset`, `poster`, lokálne `form[action]`/`formaction`, známe lazy-load atribúty používané core šablónami a prípadný `<base>`. Core komponenty generujúce URL v JSON použijú helper priamo. Spracovanie URL v inline štýloch a JavaScriptom dopĺňaných atribútoch musí mať testované pravidlá pre podporované šablóny.

V gateway sa nesmie vykonávať všeobecné odosielanie formulárov ani ďalšie produkčné mutácie aplikácií. Náhľad slúži na zobrazenie; zapisovacie akcie webových komponentov majú v náhľade zrozumiteľný blokovaný stav. Core formuláre vypnú submit ovládanie a ich lokálny action smeruje do gateway, ktorá zapisovací request odmietne aj po manuálnom opätovnom zapnutí tlačidla. Core JavaScriptové odosielanie používa rovnaký preview helper; zákaznícky skript s pevnou produkčnou URL potrebuje adaptér. Redakčné ukladanie prebieha cez autorizované admin endpointy mimo gateway.

Core náhľad tiež nezapisuje produkčnú návštevnosť: vstup `StatDB.add` v `ShowDoc` a súvisiace core meranie overia preview kontext. Bežné technické logovanie chýb zostáva. Vlastné analytické skripty šablóny sa vypnú jej preview adaptérom; samotné serverové `isPreview` nemôže zastaviť ľubovoľný JavaScript posielajúci údaje externej službe.

Toto pravidlo jadra nerozširuje budúcu úlohu bannerov o prestavbu ich štatistík. Používateľ pri N2 výslovne určil, že zapisovanie bannerových štatistík sa riešiť nemusí; rozsah príslušného modulu a jeho akceptácia sú v [08](08-rozsirovanie-modulov.md).

### Súbory, obrázky a thumbnaily

`/images/...` a `/files/...` v preview URL prejdú gateway pred priamym statickým servovaním. Súborová služba vyberie pracovný obsah, pracovné zmazanie alebo produkčný fallback podľa logickej cesty. Neposiela klientovi fyzickú staging cestu.

Výsledky thumbnailov majú vetvový priestor oddelený od produkcie; kľúč obsahuje aspoň ID vetvy, hash zdrojového obsahu a transformačné parametre. Gate chráni originál aj thumbnail. Detail napojenia na existujúce služby je v [analýze súborov](analyza-media-a-subory.md).

Rozsah súborov sa určí existujúcim mapovaním domény na úložisko a dnešnými adresárovými právami pod povolenými obsahovými koreňmi. Nepridáva sa nový register vlastníctva ciest. Ak rovnaký fyzický adresár používajú viaceré domény, UI upozorní na možný účinok publikovania aj mimo domény vetvy; prvá verzia neskenuje všetky referencie iných webov.

Nemenné CSS/JS šablóny môžu byť v preview obslúžené ako povolené produkčné zdroje. Ich definície sa vo vetve nemenia. Cesta gateway zabezpečuje odlišnú browser URL od produkcie; `no-store` bráni používaniu starého pracovného obsahu po ďalšej úprave.

Reverse proxy/CDN musí cestu `/__branch-preview/` posielať aplikácii bez verejnej cache a bez priameho servovania `/images` či `/files` podľa prípony. Táto konfigurácia je súčasťou zapnutia preview na konkrétnom nasadení. Bežné produkčné pravidlá sa nemenia.

### Hranica CSS a dynamického JavaScriptu

Relatívne URL vo vloženom CSS sa často vyriešia správne z gateway cesty CSS súboru. Koreňové `url(/images/...)`, JavaScript `fetch('/...')`, runtime vytvorený iframe alebo vlastný API klient však môžu gateway obísť. Návrh nesľubuje automatický bezpečný prepis ľubovoľného CSS/JavaScriptu.

Overovací krok P01 musí pokryť predvolené podporované šablóny: ich root-relative assets a dynamické požiadavky sa prispôsobia spoločným helperom alebo malým explicitným adaptérom. Zákaznícka šablóna s takýmto kódom potrebuje rovnaké overenie. Túto hranicu treba uviesť v dokumentácii integrácie; náhľad s neadaptovaným komponentom sa nesmie vydávať za úplnú reprezentáciu výsledného webu.

## 9. Minimálne rozhranie pre zákaznícky komponent

Zákaznícky komponent nemá priamo čítať pracovné tabuľky ani si vyrábať vlastný názov gateway. Dostane malé čítacie rozhranie a samostatný URL helper. Navrhované signatúry:

```java
public interface ContentBranchReader {
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

`ContentBranchReadService` môže implementovať prvé rozhranie; druhé implementuje spoločný helper. Ide o návrh API, uvedené triedy ešte neexistujú. Vstupy pre filtre tvoria typované polia a povolené operátory, nie ľubovoľný SQL text. Rovnaké rozhranie v produkčnom režime číta produkciu. `findDocument` nevydáva produkčný objekt, ak je dané ID vo vetve označené ako zmazané.

Pre špecifický komponent, ktorý potrebuje vlastné SQL joins, je interným integračným bodom:

```java
SqlFragment documentSource(ContentBranchContext context, String alias);

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
| `GET /admin/rest/content-branches` | Dostupné neuzavreté verzie aktuálnej admin domény pre prepínač, vrátane názvu a stavu. Prehľad môže výslovne vyžiadať aj uzavreté verzie cez `includeClosed=true`. |
| `POST /admin/rest/content-branches` s `{ "name": "Vianočná kampaň", "description": "...", "expectedDomainName": "www.firma.sk" }` | Overí práva a údaje aj zhodu očakávanej domény formulára s aktuálnou admin doménou servera; nesúlad vráti `409`. Vlastníka určí server a založí prázdnu `DRAFT` verziu bez termínu. Vráti `201` a identitu/názov/doménu/stav. Nemení admin výber, nevytvára snapshot webu ani vetvu z inej vetvy. |
| `GET /admin/rest/content-branches/context` | Produkcia alebo zvolená vetva pre aktuálnu doménu, názov a aktuálne oprávnenia. |
| `POST /admin/rest/content-branches/context` s `{ "branchId": 42 }` | Overenie a zmena výberu v administrácii; `0` vyberie produkciu. Existujúca CSRF ochrana zostáva. |
| `GET /admin/rest/content-branches/42/preview-link?docId=123` | Overená URL na výslednú stránku vetvy; pri zmazanej/nedostupnej položke vysvetlená chyba. |
| `GET /__branch-preview/42/...` | Chránený náhľad stránky alebo podporovaného assetu. |

Server odpovedá `403` pri nedostatočnom oprávnení, `404` pri neexistujúcom zdroji v príslušnom kontexte a `409` pri neplatnom stave alebo nesúlade administračnej obrazovky s výberom. Chyby používať v existujúcom štýle REST/DataTables, bez nového všeobecného API obalu.

UI `Vytvoriť` použije POST kolekcie a otvorí detail; `Upravovať web` až následne použije POST `/context` s vráteným ID. Rovnaký endpoint výberu používa prepínač. Pri chybe sa aktívny indikátor nezmení. Vybraný neuzavretý stav môže dovoliť iba čítanie; voľba kontextu sama nemení životný cyklus vetvy. Po uzavretí aktívnej verzie sa zápis odmietne a UI ponúkne explicitný prechod na produkciu; starý request sa nikdy automaticky nepresmeruje na LIVE. Konkrétne stavy, formulár a navigáciu určuje [návrh UI](05-rozhranie.md).

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
- strom s presunom, predvolenou stránkou a odmietnutím cyklu/osirelej väzby podľa zapisovacej validácie;
- SQL binding, count, poradie a stránkovanie na skutočne podporovaných databázach;
- admin detail/list/strom s rovnakým kontextom, neúplný list obsah a odmietnutie starého formulára po prepnutí vetvy;
- uloženie nového ID a jeho použitie v odkazoch bez prepisovania pri merge;
- produkčné publikovanie bez prieniku vetvového kontextu;
- odmietnutie úprav atribútov a ostatných väzieb mimo rozsahu;
- obídenie `WriteTag` a `CacheTag` cache pri preview, bez produkčného cache zápisu;
- jasnú chybu pre neplatnú gateway cestu a blokované zapisovacie akcie verejných aplikácií.

Dokončenie implementačného kroku znamená funkčné podporované cesty a uvedené overenia, nie iba kompiláciu nových tried. Tento dokument sám žiadny z týchto testov nevykonal.
