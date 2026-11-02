# Čítanie obsahu a náhľad celej pracovnej vetvy

Stav: **overená analýza existujúcich zdrojov, architektonické alternatívy zatiaľ neschválené**. Overené 1. novembra 2026 v aktuálnom checkoute. Nebol spustený server ani vykonané zmeny implementácie.

Na túto analýzu nadväzuje [konkrétny návrh kontextu, Java rozhraní a náhľadu](03-nahlad-a-java.md). Pri rozpracovaní riešenia má prednosť tento novší návrh; nižšie zostáva aj porovnanie pôvodne zvažovaných alternatív.

Doplnenie z 2. novembra: [03a — Novinky](03a-novinky.md) obsahuje podrobne overenú cestu `NewsApp`/`NewsActionBean`, kritériá, SQL zdroj, kategorizáciu a návrh zmien. Výber existujúcich perex skupín je podľa N1 podporovaný; nižšie uvedená pôvodná otázka rozsahu `perex_group_doc` je tým uzatvorená. Atribúty zostávajú read-only.

Tento dokument vysvetľuje, čo dnes načítava stránky, priečinky a ich zoznamy, a kde by bolo potrebné pripojiť pracovnú vetvu. Používateľ potvrdil **overlay nad aktuálnou produkciou, jednu doménu, postupné zlúčenie a obsahové súbory bez kódu a šablón**. Aktuálny výsledok všetkých produktových rozhodnutí vrátane podpory zákazníckych adaptérov je v [01-rozhodnutia.md](01-rozhodnutia.md); podrobný databázový návrh je v [02-datovy-model.md](02-datovy-model.md).

## Hlavné zistenie

Náhľad jednej historickej stránky už existuje. Náhľad celého webu vyžaduje aj vetvové vyhodnotenie URL, stromu priečinkov, menu, hlavičiek, pätičiek a zoznamov stránok. Samotná výmena objektu v `ShowDoc` by ukázala upravené telo stránky, ale ostatné časti webu by naďalej čítali produkciu.

Zachovanie pôvodných ID existujúcich stránok a priečinkov je prirodzené pri oddelenom úložisku vetiev: vetvová verzia odkazuje na pôvodné `doc_id` alebo `group_id`, pri zlúčení sa aktualizuje pôvodný záznam. Náročnejšia časť je zabezpečiť, aby všetky podporované spôsoby čítania videli rovnaký výsledok.

## Overené dátové modely

| Oblasť | Dnešné správanie | Dôsledok pre návrh |
| --- | --- | --- |
| [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) | JPA entita tabuľky `documents`; jediný `@Id` je `doc_id`, generovaný stratégiou `IDENTITY`. | Identita entity aj verejné API predpokladajú jedno číslo stránky. `branch_id` nemožno pridať do primárneho kľúča bez ďalších zásahov. |
| [DocBasic](../../../src/main/java/sk/iway/iwcm/doc/DocBasic.java) | Spoločné polia stránky a histórie; zahŕňa obsah aj redakčné a zobrazovacie údaje. | Návrh musí odlíšiť prenášané obsahové polia od odvodených a prevádzkových údajov. |
| [DocHistory](../../../src/main/java/sk/iway/iwcm/doc/DocHistory.java) | Samostatné `history_id`, odkaz na `docId`, stav schvaľovania a publikovania. | ID histórie nie je identita stránky. História sama neposkytuje kompletnú vetvu stromu a súborov. |
| [GroupDetails](../../../src/main/java/sk/iway/iwcm/doc/GroupDetails.java) | Model priečinka používaný JDBC/cache vrstvou, nie JPA entita. | Samotná nová JPA konfigurácia nevyrieši vetvové priečinky. |
| [Aktuálna testovacia DB schéma](../../../.github/workflows/blank_web_autotest.sql) | `documents` má PK `doc_id`, `groups` PK `group_id`; stránky sa viažu na priečinok. Priečinok obsahuje rodiča, doménu, predvolenú stránku a nastavenia navigácie. | Neexistuje všeobecný stĺpec `domain_id` na oboch tabuľkách, podľa ktorého by sa dal bezpečne odfiltrovať celý web. |
| [DocDetailsRepository](../../../src/main/java/sk/iway/iwcm/doc/DocDetailsRepository.java) | Kombinuje JPA dotazy, špecifikácie a natívne SQL. | Zmena iba `DocDB` nepokryje administráciu ani všetkých konzumentov repository. |

Testovacia schéma používa InnoDB, starší [blank_web.sql](../../../src/main/webapp/WEB-INF/sql/blank_web.sql) obsahuje MyISAM. Tento dokument z toho nevyvodzuje nové transakčné požiadavky. Pri finálnom návrhu migrácie treba vychádzať z podporovaných databáz a existujúcich zákazníckych schém, nie z predpokladu jednej univerzálnej transakcie.

## Dnešná cesta verejnej požiadavky

```text
URL návštevníka
  → PathFilter: URL → docId, doména, kanonické presmerovanie
  → ShowDoc: stránka, oprávnenia, priečinok, šablóna
  → DocDB: obsah stránky a ďalších vložených stránok
  → GroupsDB: priečinky, rodičia a nastavenia navigácie
  → JSP/Thymeleaf a komponenty
       → menu, breadcrumbs: prevažne DocDB/GroupsDB
       → novinky: tiež samostatné SQL cez NewsQuery
  → cache/HTTP odpoveď
```

### Routovanie a hlavná stránka

[PathFilter](../../../src/main/java/sk/iway/iwcm/PathFilter.java) používa `DocDB.getVirtualPathDocId(path, domain)`; pri zapnutom A/B testovaní cez `ABTesting`. Následne porovnáva URL pomocou `getDocLink` a môže vrátiť kanonické presmerovanie. Kontext vetvy preto musí existovať **pred vyhodnotením URL**, nie až pri renderovaní.

[ShowDoc](../../../src/main/java/sk/iway/iwcm/doc/ShowDoc.java) dnes umožňuje vložiť objekt cez request atribút `ShowdocAction.showDocData`, použiť `ShowDocBean.forceShowDoc` alebo načítať konkrétne `historyid`. Tieto mechanizmy sú užitočné pre náhľad jednej stránky, ale nemenia automaticky URL mapu ani následné čítanie súvisiacich stránok.

### Kontext domény, prihlásenie a súborové helpery — doplnené auditom

[SetCharacterEncodingFilter.registerDataContext](../../../src/main/java/sk/iway/iwcm/SetCharacterEncodingFilter.java) vytvára `RequestBean` už pred navrhovaným filtrom branch. Kopíruje doň URL a doménu; odkaz `RequestBean.request` nastavuje iba pre vybrané elFinder URL. Kontext náhľadu preto nemožno všeobecne získavať cez tento odkaz. [RequestBean](../../../src/main/java/sk/iway/iwcm/RequestBean.java) už poskytuje request-local `setAttribute/getAttribute`; netreba ďalší globálny register.

`DocDB.getDomain(request)` bezpodmienečne uprednostňuje session `preview.editorDomainName`, ktorú nastavuje administrácia. [CloudToolsForCore](../../../src/main/java/sk/iway/iwcm/common/CloudToolsForCore.java) číta doménu aj logickú URL z `RequestBean`; URL ovplyvňuje osobitné mapovanie spoločných doménových adresárov. Vetva potrebuje explicitnú prednosť overenej domény a zosúladenie týchto hodnôt pred obsahovým routovaním, bez zmeny admin session. Samotné odstránenie prefixu z URI nestačí.

[AdminLogonController.showForm](../../../src/main/java/sk/iway/iwcm/logon/AdminLogonController.java) pri `multiDomainAdminHost` na inom hostiteľovi vracia 404 alebo presmeruje login na admin host. Q19 preto určuje preview na hostiteľovi administrácie: tam zostane session, zatiaľ čo logická obsahová doména pochádza z overenej branch. Novší dokument 03 rozlišuje tieto dve identity a prevádza obsahové odkazy na admin gateway; nevyžaduje login na každej verejnej doméne.

Niektoré core čítania kontrolujú fyzický súbor ešte pred vytvorením HTML. [MediaDB.filterByAvailability/isExists](../../../src/main/java/sk/iway/spirit/MediaDB.java) overujú lokálnu prílohu cez LIVE filesystem. `DocBasic.getPerexImageSmall/Normal/Original` deleguje na [GalleryToolsForCore](../../../src/main/java/sk/iway/iwcm/common/GalleryToolsForCore.java), ktorý volí variant pomocou `IwcmFile.exists`. [ThumbTag](../../../src/main/java/sk/iway/iwcm/tags/ThumbTag.java) pri `thumbExternalServer` môže vypísať externú produkčnú cache URL. Podporovaný branch náhľad potrebuje explicitne zapojiť tieto helpery do file overlay a obísť externý thumbnail server; iba prepísanie výsledných HTML URL nedoplní už vynechanú prílohu.

`PathFilter.setStaticContentHeaders` môže neskôr nastaviť `Cache-Control: max-age=...`, aj keď skorší filter nastavil no-store. `writeAndCacheFile` používa globálny URL kľúč. Preview musí obísť túto obsahovú cache a zachovať privátne no-store hlavičky až do odoslania odpovede, aj pri statických a conditional requestoch.

### Hlavičky, pätičky, menu a navigácia

`ShowDoc` načítava cez `DocDB.getDoc` aj hlavičku, pätičku, menu, pravé menu a dokumenty z polí šablóny. Ich referencie sú obyčajné ID stránok. Náhľad musí pri týchto čítaniach zachovať rovnaký kontext ako hlavná stránka.

[MenuULLI](../../../src/main/java/sk/iway/iwcm/components/menu/MenuULLI.java) používa najmä `GroupsDB.getGroupsAll`, `getGroup`, `DocDB.getBasicDocDetailsByGroup`, `getBasicDocDetails` a `getDocLink`. Pokrytie týchto metód môže obslúžiť bežné menu aj sitemapu bez novej implementácie ich vykresľovania.

[NavbarStandard](../../../src/main/java/sk/iway/iwcm/doc/NavbarStandard.java) prechádza rodičov cez `GroupsDB`; [NavbarService](../../../src/main/java/sk/iway/iwcm/doc/NavbarService.java) vyberá implementáciu navigácie. Potrebný je celý výsledný strom, nie len nahradenie jedného objektu priečinka. `GroupDetails` navyše obsahuje odvodené navigačné údaje, ktoré musia vzniknúť z rovnakého pohľadu na stránky.

### Zoznamy stránok a novinky

[DocDB](../../../src/main/java/sk/iway/iwcm/doc/DocDB.java) obsahuje priame SQL pre `getDocByGroup`, `getDocPerex` a ich počty. [NewsQuery](../../../src/main/java/sk/iway/iwcm/components/news/NewsQuery.java) vytvára vlastné `FROM documents d` a pripája používateľov, perex skupiny a pri príslušnom režime atribúty. [NewsActionBean](../../../src/main/java/sk/iway/iwcm/components/news/NewsActionBean.java) zostavuje filtre dostupnosti, dátumu publikovania, oprávnení, priečinkov, poradia a stránkovania.

Nestačí načítať produkčnú stránku výsledkov a potom nahradiť objekty ich vetvovými verziami. Taký postup:

- nenájde nové stránky, ktoré v produkčnom výsledku neboli;
- môže ponechať stránky odstránené alebo presunuté vo vetve;
- nesprávne vyhodnotí filter podľa názvu, dostupnosti alebo dátumu;
- vráti nesprávne počty a poradie pri stránkovaní.

Výsledný obsah vetvy musí byť vstupom **pred filtrom, počítaním, radením a stránkovaním**. Platí to pre SQL reláciu aj pre prípadný samostatný vetvový vyhodnocovač.

## Cache a implicitné vedľajšie účinky

| Miesto | Overený stav | Potrebná hranica |
| --- | --- | --- |
| [DocDB](../../../src/main/java/sk/iway/iwcm/doc/DocDB.java), `getInstance`, `getDoc`, `loadUrls` | Inštancia je uložená v servlet kontexte. Obsahová cache je podľa `docId`, existujú spoločné mapy URL podľa domén, základných údajov stránok a stránok v priečinkoch. | Vetvové objekty nesmú prepísať produkčné cache ani sa do nich uložiť pod samotným ID stránky. |
| [GroupsDB](../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java), `getInstance`, `getGroup`, `getGroupsAll` | Spoločná inštancia načítava všetky priečinky. Metódy vracajú objekty a kolekcie z cache. | Pre vetvu vytvoriť samostatné výsledné objekty a graf; neupravovať produkčné objekty na mieste. |
| `DocDB.getInstance(...)` | Pri získaní inštancie volá `DocPublishService.checkWebpagesToPublish`. | Prístup z náhľadu nesmie nepozorovane zmeniť kontext plánovaného publikovania. Produkčné publikovanie musí čítať a zapisovať explicitne produkciu. |
| [WriteTag](../../../src/main/java/sk/iway/iwcm/tags/WriteTag.java) | Request atribút `isPreview` vypína cache vložených komponentov. | Dá sa využiť pre vetvový náhľad; treba zachovať vypnutie čítania aj zápisu cache. |
| [CacheTag](../../../src/main/java/sk/iway/iwcm/tags/CacheTag.java) | Má vlastné čítanie a zápis spoločnej alebo session cache. | Samotný `isPreview` vo `WriteTag` ho nepokrýva. Vetvový náhľad potrebuje explicitné obídenie tejto cache. |
| [PathFilter](../../../src/main/java/sk/iway/iwcm/PathFilter.java), `setNoCacheHeaders`, `isNoCacheCookieRequired` | Existuje nastavenie `no-store` a bypass nginx cache pre prihlásených používateľov. | Náhľad musí zostať súkromný a bez verejnej cache aj pri každom ďalšom kliknutí. |

Pre niekoľkých redaktorov je vhodným kandidátom vypnúť HTML/component cache počas náhľadu a držať malý výsledný strom/metadáta iba v požiadavke. Prípadná spoločná cache vetiev je optimalizácia až po meraní. Toto je návrh na posúdenie, nie schválené rozhodnutie.

## Administrácia číta aj mimo DocDB

[WebpagesDatatable](../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java) a [WebpagesService](../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java) používajú `DocDetailsRepository` a JPA specifications pre zoznamy. Detail sa pripravuje aj cez `EditorFacade`. Samostatný výsledný pohľad pre verejný náhľad preto ešte nezaručuje správne údaje v editore.

[GroupsTreeService](../../../src/main/java/sk/iway/iwcm/doc/GroupsTreeService.java) a [GroupsTreeRestController](../../../src/main/java/sk/iway/iwcm/doc/GroupsTreeRestController.java) tvoria strom administrácie. Pri napojení vetvového stromu musia zostať zachované práva a režimy System, Kôš a výber priečinka.

Pri zmene priečinka sú dôležité aj zápisy, ktoré na pohľad vyzerajú ako pomocné operácie:

- `WebpagesService.regenerateUrl` priamo aktualizuje URL, uloženú cestu a koreňové priečinky stránok; následne môže volať `EditorDB.fixRenamedVirtualPath`.
- `WebpagesService.setAttributeToSubgroups` priamo mení hodnoty v `groups`.
- `GroupsDB` po uložení mení vlastné cache a odvodené údaje.

Tieto operácie musí vetvový pracovný postup presmerovať do svojho úložiska alebo ich v danom kroku sprístupniť až po implementácii podpory. Bežné uloženie vetvového priečinka nemôže zavolať produkčný prepočet URL bez kontroly jeho zápisov.

## Porovnanie neschválených architektonických možností

| Možnosť | Prínos | Náklady a hranice |
| --- | --- | --- |
| Oddelené tabuľky vetiev a kontextové čítanie | Zachová existujúce produkčné tabuľky, ID a správanie neadaptovaných produkčných čítačov. Dá sa zavádzať postupne. | Treba doplniť čítanie výsledného obsahu pre detail, zoznamy, strom a URL; neadaptovaný komponent v náhľade naďalej číta produkciu. |
| `branch_id` priamo v `documents`, `groups`, `media` | Verzie sú pri sebe v rovnakých tabuľkách. | Pri rovnakom pôvodnom ID treba zmeniť kľúče alebo pridať ďalšiu identitu. Každý dotaz, zápis, JPA identita a cache potrebuje správny filter. Neadaptované SQL môže čítať návrhy verejne alebo zmeniť viac verzií naraz. |
| Samostatná schéma/databáza pre vetvu | Existujúce názvy tabuliek a ID možno zachovať. | Potrebné je spoločné smerovanie JDBC aj JPA, oddelenie cache a riešenie väzieb na nekopírované tabuľky. Prevádzka a SQL sa líšia podľa databázy. |
| Samostatná inštancia aplikácie pre vetvu | Oddelí aj singleton cache a beh aplikácie. | Pridáva správu nasadenia a celej kópie prostredia. Je bližšie stagingu než malému redakčnému nástroju v CMS. |

Oddelené tabuľky sú podľa preskúmaného kódu vhodným kandidátom na postupný vývoj, nie zatiaľ schváleným dátovým modelom. Schválená odpoveď Q1 určuje obsahový model:

- **Zvolený overlay nad produkciou:** nezmenené položky sa načítajú z aktuálnej produkcie; pre zmenené položky existuje vlastná verzia a základ pre porovnanie.
- **Nezvolený zmrazený fork:** aj nezmenené položky by sa museli načítať zo stavu pri vytvorení vetvy. Taký sľub návrh neposkytuje.

Pri samostatných tabuľkách treba rozhodnúť aj medzi typovanými stĺpcami a serializovaným payloadom. Typované stĺpce umožňujú jednoduchšie zostaviť výslednú SQL reláciu a použiť existujúce filtre. JSON/text payload zjednoduší uloženie snapshotu, ale sám nevyrieši SQL noviniek ani stránkované filtrovanie administrácie. Kandidát na ďalšie rozpracovanie je typovaný pracovný záznam stránky so základom porovnania v JSON a JSON snapshoty priečinkov/médií tam, kde ich stačí načítať do výsledného pohľadu. Konkrétne stĺpce ani tento rozdiel medzi typmi objektov ešte nie sú schválené. Automatická konverzia každého existujúceho SQL dotazu nie je súčasťou odporúčanej jednoduchej cesty.

Zmena iba [DBPool.getConnection(request)](../../../src/main/java/sk/iway/iwcm/DBPool.java) nestačí pre variant samostatných databáz: bežné `getConnection()` a `getConnectionReadUncommited()` používajú `iwcm`, [V9JpaDBConfig](../../../src/main/java/sk/iway/webjet/v9/V9JpaDBConfig.java) odovzdáva datasource do EntityManagerFactory pri konfigurácii.

## Navrhované integračné body na ďalšie rozpracovanie

Názvy nižšie pomenúvajú zodpovednosti. Neurčujú zatiaľ záväzný zoznam nových tried.

1. **Kontext vetvy v požiadavke.** Server overí používateľa a prístup k vetve pred routovaním. Kontext žije počas jednej požiadavky vrátane vnoreného renderovania; po jej skončení sa vyčistí. Scheduler a merge majú explicitný produkčný kontext.
2. **Čítač výsledného obsahu.** Vie načítať detail stránky, priečinok, výsledný strom, URL podľa ID a ID podľa URL. Produkčný režim používa existujúce cesty. Modely si ponechávajú pôvodné číselné ID.
3. **Zoznamový zdroj.** Podporí `DocDB` zoznamy, `NewsQuery` a administračné filtrovanie tak, aby sa nové, zmenené a odstránené položky prejavili pred stránkovaním.
4. **Oddelený zápis vetvy.** Zdieľa použiteľné validácie a normalizácie s existujúcim editorom, ale explicitne riadi perzistenciu, prepočet URL a vedľajšie účinky. Podrobný zapisovací tok patrí do samostatnej analýzy.
5. **Izolácia cache a súkromný náhľad.** Žiadny vetvový objekt sa neukladá do produkčnej cache. Podporované cache výstupu sa v náhľade obídu.
6. **Produkčné zlúčenie.** Porovnáva explicitne produkčné údaje a pri zápise zachová pôvodné ID. Nesmie použiť efektívny pohľad aktívnej vetvy ako údaj o produkcii.

Existujúce [HeadlessPageService](../../../src/main/java/sk/iway/iwcm/headless/service/HeadlessPageService.java) a [HeadlessNavigationService](../../../src/main/java/sk/iway/iwcm/headless/service/HeadlessNavigationService.java) tiež využívajú `DocDB`/`GroupsDB`. Ich prítomnosť však automaticky neznamená, že prvá verzia bude poskytovať verejné headless API pre vetvy; to je samostatné rozhodnutie o rozhraní a autorizácii.

## Hranica zákazníckych komponentov

Komponent používajúci podporované `DocDB`/`GroupsDB` metódy môže získať vetvový obsah cez tieto vstupy. Komponent s vlastným SQL `SELECT ... FROM documents` nové úložisko vetiev automaticky neuvidí. Rovnako to platí pre vlastný externý vyhľadávací index.

Jednoduché textové hľadanie v `src/main/java` našlo priame SQL s `documents` v 53 súboroch a s `groups` v 18 súboroch. Je to orientačný rozsah auditu, nie počet nutných úprav: obsahuje administráciu aj úlohy na pozadí, nezachytí všetky dynamicky zostavené dotazy a neobsahuje zákaznícky kód mimo repozitára.

Dokončený návrh musí uviesť podporované core cesty a spôsob adaptácie vlastného komponentu. Bežné menu, breadcrumbs, zloženie stránky a štandardné novinky patria medzi hlavné kandidáty na povinné pokrytie. Tvrdenie „náhľad celého webu“ nemožno bez tejto hranice rozšíriť na ľubovoľnú zákaznícku aplikáciu.

Väzby `perex_group_doc`, `doc_atr` a `doc_atr_def` sú mimo používateľom pomenovaných troch tabuliek, ale existujúce zoznamy ich čítajú. Ich nezmenené produkčné údaje možno používať ako vstup náhľadu; možnosť meniť ich vo vetve musí mať vlastné rozhodnutie o rozsahu.

## Overenia pre budúcu implementáciu

Ide o akceptačné scenáre budúcich krokov, nie testy vykonané v tejto analýze. Začať možno existujúcimi testami v [webpages](../../../src/test/webapp/tests/webpages/) a príslušnými Java testami; nové scenáre použiť až k realizovanému správaniu.

| Scenár | Čo musí preukázať |
| --- | --- |
| Upravená stránka v dvoch súčasných reláciách | Redaktor v náhľade vidí návrh; anonymný návštevník a redaktor v produkcii pôvodný obsah. Poradie návštev nesmie rozhodovať o výsledku cache. |
| Preklik z hlavnej stránky cez menu a breadcrumbs | Každá ďalšia stránka používa tú istú vetvu; linky a predvolené stránky zodpovedajú výslednému stromu. |
| Zmena názvu alebo presun priečinka | Nové URL fungujú v náhľade, kanonické presmerovanie nevracia používateľa do produkčnej štruktúry, produkčné URL sa pred merge nezmenia. |
| Zmena hlavičky, pätičky alebo vloženej stránky | Celá zložená stránka používa rovnaký kontext. |
| Nová, presunutá, skrytá a odstránená stránka v novinkách | Zoznam, poradie, filter, počet aj ďalšia strana výsledkov zodpovedajú vetve. |
| Administračný zoznam a strom | Zobrazujú rovnaké údaje ako editor a náhľad, vrátane nových položiek a oprávnení. |
| Opakovaný náhľad s aktivovanými cache komponentov | Vetva nečíta produkčný fragment a nenahrádza produkčný fragment svojím výstupom. |
| Produkčná zmena po vytvorení vetvy | Nezmenená položka vo vetve prevezme aktuálnu produkciu; vlastná pracovná verzia a uložený základ porovnania zostanú zachované. |
| Prístup používateľa bez oprávnenia k vetve | Nedostane vetvový obsah priamym URL ani odhadnutým ID. |
| Vyvolanie plánovaného publikovania počas náhľadu | Scheduler zostáva v produkčnom kontexte a nepoužije vetvové dáta. |
| Vlastný komponent s priamym SQL | Pilot odhalí potrebu adaptácie a rozsah podpory je zdokumentovaný; test nesmie nesprávny produkčný výsledok označiť za správny náhľad. |

Pred rozhodnutím o celom dátovom modeli je užitočný malý technický overovací krok: načítať alternatívnu verziu existujúcej stránky a priečinka pod rovnakými ID, prekliknúť bežné menu a overiť zoznam noviniek s pridanou aj odstránenou položkou. Realizácia takého kroku patrí až do schválenej implementačnej fázy.
