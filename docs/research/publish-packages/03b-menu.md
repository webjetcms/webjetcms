# Menu webu vo vetve

**Stav: návrh, bez implementácie.** Doplnenie N4 z 3. novembra 2026 výslovne zahŕňa `menu_ul_li.jsp` a `MenuULLI` do povinného jadra. Rozpracovaný web potrebuje navigáciu podľa rovnakého výsledku ako stránky a Novinky. Tento dokument spresňuje [náhľad a Java integráciu](03-nahlad-a-java.md); nepridáva ďalšie tabuľky ani samostatný generátor menu.

## Existujúci tok

```text
ShowDoc → účinná stránka / hlavička / menu dokument
  → !INCLUDE(/components/menu/menu_ul_li.jsp, ...)
  → PageParams + request atribúty
  → MenuULLI.doTree(..., request)
  → DocDB + GroupsDB
  → HTML alebo !UL_LI_MENU! / !ROOT_GROUP_*!
  → výstupný prepis URL do preview gateway
```

[menu_ul_li.jsp](../../../src/main/webapp/components/menu/menu_ul_li.jsp) číta parametre, nastaví `ulLiOpenAllItems`, `ulLiForceDocid`, ďalšie prezentačné atribúty a zavolá `MenuULLI.doTree`. `onlySetVariables` uloží výsledok do requestu namiesto priameho výpisu. [MenuApp](../../../src/main/java/sk/iway/iwcm/components/menu/MenuApp.java) poskytuje editor parametrov, ale jeho `componentPath` stále smeruje na tento JSP. Pôvodné uložené INCLUDE aj verejné signatúry `doTree` zostanú použiteľné.

[MenuULLI](../../../src/main/java/sk/iway/iwcm/components/menu/MenuULLI.java) nemá vlastné SQL ani spoločnú cache výsledného HTML. Má však viac samostatných čítacích ciest:

| Miesto | Čo musí dostať z účinného pohľadu |
| --- | --- |
| Vstupné `doTree` | Aktuálnu stránku, `rootGroupId`, jej predkov, `getRoot`/`getParentGroups`, posun `startOffset` a default stránku koreňa. |
| `getGroupTree` | Rodičov aktívnej stránky pre otvorenú cestu a CSS triedy. |
| Rekurzívne `doTree` | `getGroupsAll`, `getGroup`, `getBasicDocDetailsByGroup`, dostupnosť/default stránky, názvy, poradie a odkazy. |
| `hasChilds` | Tú istú množinu priečinkov a stránok pre `hasChild`/`hasNoChild` a Bootstrap dropdown. |
| `isShowIn` | Účinné nastavenia stránky a existujúcu identitu používateľa; pre menu `isShowInMenu(request)`. |
| `menuIncludePerex`, `menuInfoDirName` | Celý účinný dokument cez `getDoc`, vrátane perexu alebo názvu a HTML. Metadáta zoznamu tu nestačia. |
| Premenné koreňa menu | Účinné ID, default docId, odkaz, názov a navigačný názov koreňa. |

## Úprava MenuULLI a JSP

1. Na vstupe `doTree` získať overený `BranchContext` requestu a fasády `DocDB`/`GroupsDB` z jeho `BranchView`. Použiť mechanizmus z dokumentu 03; nevytvárať vlastné načítanie pracovných tabuliek v menu. Bez branch kontextu zostáva produkčný tok.
2. V súkromnej rekurzii a `getGroupTree` používať tie isté fasády z vstupu, napríklad ich odovzdaním parametrami. `hasChilds` ich už prijíma. Verejné overloady možno zachovať; nevzniká druhá trieda na generovanie pracovného menu. Existujúca `MenuULLI` zostáva v `sk.iway.iwcm.components.menu`, nové spoločné služby patria do `sk.iway.iwcm.branch`.
3. Aktuálnu stránku odvodzovať aj z účinného `request.docDetails`, keď gateway nemá query parameter `docid`. Zachovať význam explicitného `docid`, prihlasovacieho `origDocId` a `forceDocid` (posledný má prednosť), ale cieľ vždy rozlíšiť cez rovnaký účinný pohľad a jeho rozsah. Nové ID stránky je platné ešte pred merge. Neprevziať produkčnú kópiu aktuálnej stránky zo session.
4. Pre chýbajúcu alebo zmazanú stránku nepoužiť `getBasicDocDetails(id, true)` ako dôkaz existencie: dnes vracia náhradný objekt `??? docId=...`. Použiť overené načítanie a ošetriť neprítomnosť. Bez platnej aktuálnej stránky sa nevytvorí falošná aktívna cesta; bez koreňa potrebného pre `rootGroupId=0`/`startOffset` sa vráti prázdny výsledok s diagnostikou. Explicitný koreň sa nesmie nahradiť domovským alebo celo-webovým fallbackom.
5. `BranchView` poskytne úplný strom v povolenom rozsahu, vrátane priečinkov skrytých v menu. Viditeľnosť sa rozhoduje až v `MenuULLI`: explicitný koreň na prvej úrovni má dnes výnimku, takže predbežné odfiltrovanie všetkých hidden priečinkov by rozbilo sekčné menu. Pracovné DELETE sa naopak do účinnej množiny vôbec nedostane.
6. V JSP zachovať parametre a formát INCLUDE. Konkrétne integračné úpravy sú zachovanie kontextu pri include, izolácia výsledných request premenných a obsluha chyby. Pri začiatku každého volania vynulovať jeho výsledné `!ROOT_GROUP_ID!`, `!ROOT_GROUP_DOCID!`, `!ROOT_GROUP_LINK!`, `!ROOT_GROUP_NAME!` a `!ROOT_GROUP_NAVBAR!`; predčasný návrat nesmie ponechať koreň iného menu z toho istého requestu. Následne sa naplnia aktuálnym výsledkom. `!UL_LI_MENU!` JSP vyčistí/nahradí iba pri `onlySetVariables=true`, aj keď je nový výsledok prázdny. Bežný priamy výpis ďalšieho menu zachová predtým uložené HTML na neskorší výpis. `ulLiSkipMenuGeneration` zostane funkčné.
7. Chybu načítania vetvy nenechať zaniknúť v dnešnom širokom `catch` rekurzie ako úspešné čiastočné HTML. PREVIEW ju odovzdá spoločnej obsluhe chyby komponentu s diagnostikou; nikdy nezopakuje menu nad produkciou. Očakávané prázdne menu kvôli nastaveniam nie je chyba. Zastaranú voľbu Q16 odmietne spoločná mapa ešte pred renderom.

## Zachované pravidlá menu

Účinná množina vznikne pred výpočtom menu: zmena rodiča, pridanie alebo zmazanie stránky/priečinka sa musí prejaviť aj v otvorenej ceste a odkaze default stránky. Hodnoty sa preberajú podľa mapy Q16, teda zo zvoleného LIVE alebo WORK; rovný cieľ je LIVE/no-op. Editor parametrov stále používa ADMIN pohľad WORK.

| Oblasť | Pravidlo |
| --- | --- |
| Názvy a poradie | Použiť účinné `navbar`, `navbarName`, `groupName` a priority. Skupiny dodržiavajú dnešné radenie `getGroupsAll` podľa `sort_priority, group_name`; stránky `getBasicDocDetailsByGroup(..., ORDER_PRIORITY)`. Nenahradiť skupinové poradie odlišným `groupsDbSorter`, ktorý má iný rozhodovací kľúč. |
| Korene a hĺbka | Zachovať `rootGroupId=0`, explicitné ID, starší parameter `groupId`, `startOffset`, `maxLevel` a `openAllItems`. `getRoot`/`getParentGroups` majú aj pravidlá navigačného názvu; nestačí len ísť po `parent_group_id` až k nule. |
| Viditeľnosť | Zachovať `available`, `isShowInMenu`, interné priečinky, režimy `menuType`/`loggedMenuType`, `onlyFolders` a dnešné pravidlá default stránky vrátane `DEFAULTPAGESUBMENU`. Nová položka sa nepridá nasilu, ak nastaveniam menu nevyhovuje. |
| Prihlásenie | Použiť skutočnú identitu a dnešné session pravidlá vrátane `menuDisabledGroup`. Chránené odkazy dnes môžu zostať v menu s triedou `protected`/`protectedAllow`; automaticky ich nevyhadzovať. Samotný prístup k cieľu ďalej kontroluje renderer stránky. |
| HTML a pomocné triedy | Zachovať `classes=none/basic/full/bootstrap`, aktívne triedy, `hasChild`/`hasNoChild`, `rootUlId`, span a ostatné prezentačné parametre. Výpočty používajú účinné položky, nemení sa dizajn menu. |
| Default stránka | Zmena na novú stránku používa jej stabilné ID a novú logickú URL. Zmazanie cieľa bez náhrady je neplatná referencia; v preview sa nevytvorí odkaz na zmazaný cieľ a porovnanie ukáže prekážku. Nevyberať inú stránku automaticky. |

Dnešné `hasChilds` má jednoduchšie pravidlá než úplný render a `menuType` sa na niektorých miestach číta bez session. Tieto zvláštnosti sa nesmú meniť iba v náhľade. Prípadná nevyhnutná oprava musí byť spoločná pre LIVE aj PREVIEW a overená regresiou. Rovnako pri zhodnej priorite stránok dnešný kód nezaručuje rozhodovanie podľa ID; test zhody poradia použije jednoznačné priority. Nový stabilný rozhodovací kľúč by vyžadoval rovnakú zmenu produkčného radenia.

Menu dnes nepoužíva `publish_start`/`publish_end` ani časové filtre Noviniek. Náhľad ich do menu nepridáva a `previewAt` nepreberá ako ďalší filter. Naplánovaná vetva ukáže menu nad zvoleným obsahom a jeho dostupnosťou; nesimuluje vykonanie budúcich individuálnych plánov.

## Odkazy, premenné a cache

`getDocLink` vracia logickú adresu zo zvoleného výsledku; `BranchPreviewUrls` a výstupný wrapper z dokumentu 03 ju preložia na admin host a prefix `/admin/webpages/branch-preview/{id}/...` práve raz. Platí to pre odkazy na stránky aj default stránky priečinkov, `!ROOT_GROUP_LINK!`, výpis cez `!UL_LI_MENU!` a obsah `menuInfoDirName`. Absolútny odkaz tej istej logickej domény zostane v preview; odkaz na inú doménu zostane externý. Fragmenty a query parametre sa zachovajú. Uložený navbar, HTML a parametre aplikácie sa prefixom nemenia.

Samotná `MenuULLI` nepotrebuje novú cache. Preview obchádza už navrhnuté HTML/component cache vrátane obalenia menu cez `WriteTag`/`CacheTag`. Pomocné výsledky menu patria jednému requestu a jednej invokácii; nesmú skončiť v produkčnom singletone alebo session. Test musí najskôr zahriať produkčnú cache, potom otvoriť dve vetvy a produkciu vedľa seba.

## Etapy a overenie

Menu je súčasť existujúceho **T03**, s väzbami na T04/T05/T08/T11. Nevzniká samostatná implementačná etapa. P01 prejde skutočný JSP s upravenou a novou položkou, presunom a preklikom; P03 dokončí čítacie scenáre nad fixture, P04 overí bežný editor a P06 porovná výsledok merge.

| ID | Povinný scenár | Etapa |
| --- | --- | --- |
| M01 | Nová/upravená stránka aj priečinok: názov, navigačný názov, priorita, nová default stránka; produkcia ostane pôvodná. | P01 základ, P03 |
| M02 | Presun/zmazanie stránky alebo podstromu; položka zmizne zo starého rodiča, aktívna cesta a `hasChild` vychádzajú z účinných dát. | P03, P04 |
| M03 | `rootGroupId=0`, explicitný a skrytý koreň, `startOffset`, `maxLevel`, `openAllItems`, `onlyFolders`; aktuálna stránka z gateway bez query `docid`, platný/neplatný `forceDocid`. | P03 |
| M04 | `available`, default stránka, menu/logged nastavenia, interné priečinky, chránené odkazy a skutočná identita; zmazaný koreň/cieľ nevytvorí produkčný fallback. | P03 |
| M05 | Triedy basic/full/bootstrap/none, otvorená cesta po presune a režim bez potomkov; výsledok zachová dnešné prezentačné pravidlá. | P03 |
| M06 | Pracovný perex aj celé HTML cez `menuIncludePerex` a `menuInfoDirName`, vrátane odkazu a obrázka v HTML. | P03, súbory P05 |
| M07 | Priamy výpis aj `onlySetVariables`, `ulLiSkipMenuGeneration`, všetky ROOT premenné a dve menu v jednom requeste. Neplatný druhý koreň nevráti ROOT premenné prvého; po uložení prvého menu a priamom výpise druhého možno stále vypísať prvé z `!UL_LI_MENU!`. | P03 |
| M08 | Klik z reálneho JSP menu na novú/premenovanú/default stránku zostane vo vetve; absolútne aj relatívne URL, vonkajší odkaz, zahriata cache, dve vetvy a produkcia. | P01 základ, P03 |
| M09 | Voľba LIVE/WORK zmení menu aj jeho HTML obsah; W=B a L=W použijú LIVE, zastaraná voľba vyžiada nové porovnanie. | P03 fixture, P06 UI |
| M10 | `MenuApp` vyberie nový pracovný koreň, uloží/reotvorí parametre a JSP ho vykreslí. Selector a požiadavky nesú `branchId` aj `branchDomain`; stará záložka vráti 409. | P04 |
| M11 | Pri rovnakých dátach, identite a parametroch sa menu pred merge zhoduje s produkciou po merge v položkách, poradí, aktívnych triedach a cieľoch. Normalizovať iba preview host/prefix/lištu. Produkčný render s flag off/on zostane kompatibilný. | P06, P08 |

P03 vyžaduje M01–M09 cez skutočný `menu_ul_li.jsp`; samotný test fasády alebo admin stromu nestačí. Nadviazať na [apps/menu.js](../../../src/test/webapp/tests/apps/menu.js), ktorý dnes overuje parametre a základné texty, a doplniť render/presuny/prekliky. [components/navbar.js](../../../src/test/webapp/tests/components/navbar.js) je súvisiaca regresia navigácie.

[sitemap/ul_li_jquery.jsp](../../../src/main/webapp/components/sitemap/ul_li_jquery.jsp) tiež volá `MenuULLI` s `isSitemap=true`; pri zmene spoločných metód zachovať jeho `isShowInSitemap` pre dokumenty a overiť túto cestu. Priečinky tu dnes stále používajú menu pravidlá, nie `getShowInSitemap`; tento význam sa nemení iba v preview. Ostatné generátory sitemap ani headless navigácia sa tým automaticky nestávajú overenými. [components/sitemap.js](../../../src/test/webapp/tests/components/sitemap.js) poskytuje regresné scenáre, ale jeho úspech s iným rendererom sám nedokazuje vykonanie vetvy `MenuULLI`.

Toto doplnenie bolo overené čítaním uvedených zdrojov. Nové implementačné testy ani CMS sa v návrhovej fáze nespúšťali.
