# Zrkadlenie štruktúry

Používateľský opis zrkadlenia štruktúry je v [dokumentácii pre redaktora](../../redactor/apps/docmirroring/README.md). V tejto dokumentácii sa nachádza len technický opis.

Základný java package je ```sk.iway.iwcm.components.structuremirroring``` a adresár pre JSP komponenty ```/components/structuremirroring/```.

## Spôsob prepojenia

Prepojenie jednotlivých adresárov a stránok využíva databázový atribút ```sync_id```, pôvodne používaný pre synchronizáciu medzi WebJETmi. Takáto funkcionalita sa ale aktuálne nepoužíva (využíva sa export a import v XML formáte) a daný atribút nebol využívaný.

Do atribútu ```sync_id``` sa prepojeným adresárom a stránkam nastavuje hodnota z generátora kľúčov volaním ```PkeyGenerator.getNextValue("structuremirroring")```. Hodnota ```sync_id``` navzájom prepojených adresárov alebo stránok musí samozrejme byť rovnaká. Podľa hodnoty v ```sync_id``` sa dajú v databáze vyhľadať prepojené adresáre/stránky.

## Inicializácia prepojenia

Prepojenie sa nastavuje v konfiguračnej premennej ```structureMirroringConfig``` kde na každom riadku je zoznam ID adresárov (hlavné adresáre danej jazykovej mutácie), ktoré majú byť prepojené. Nastavenie konfiguračnej premennej vyvolá udalosť, ktorú počúva ```SaveListener.handleConfSave``` a následne vyvolá ```MirroringService.checkRootGroupsConfig()```. Tu sa skontroluje atribút ```syncId``` zadefinovaných adresárov, ak nie je nastavený automaticky sa nastaví.

Volanie ```MirroringService.isEnabled(int groupId)``` overuje, či pre zadané ID adresára je zapnuté zrkadlenie štruktúry.

## Prepojenie adresárov

Prepojenie adresárov je realizované počúvaním udalosti zmeny adresára ```SaveListener.handleGroupSave```, čo následne volá ```GroupMirroringServiceV9.handleGroupSave```.

Poznámka: výnimka je pre adresár s názvom **Nový podadresár** (prekladový kľúč ```editor.dir.new_dir```), ktorý vznikne v stromovej štruktúre kliknutím na pridať nový adresár v kontextovom menu. Takto by vznikli v ostatných jazykových mutáciách adresáre ```Nový podadresár```, čo by nemalo praktický význam (keďže zmena názvu adresára už nemení vytvorené zrkadlené kópie).

Pri udalosti ```WebjetEventType.ON_START``` sa kontroluje, či adresár už má nastavené ```syncId```. Ak nie, vieme, že sa jedná o nový adresár, ktorý ešte nie je zrkadlený a nastavíme mu novú hodnotu ```syncId```.

Pri udalosti ```WebjetEventType.AFTER_SAVE```, čiže po uložení adresára podľa ```syncId``` hľadáme zrkadlené adresáre. Ak žiadne neexistujú, vytvoria sa nové adresáre. Je využité API ```groupsDB.setGroup(mirror, false);``` s druhým ```false``` atribútom, ktorý **nevyvolá** znova udalosť po uložení adresára (čo by spôsobilo rekurziu).

Pri existencii prepojenia sa overuje rodičovský adresár každej zrkadlenej verzie. Ak nezodpovedá mapovaniu nového rodiča, existujúca verzia sa presunie. Až po aktualizácii jej umiestnenia sa zisťuje, či treba vytvoriť chýbajúce kópie. Tým sa pri presune zachovajú ID a nevzniknú duplicitné priečinky.

Uloženie zrkadleného priečinka s parametrom `publishEvents=false` nevyvolá `SaveGroupListener`. Preto `GroupMirroringServiceV9` po presune v HTTP požiadavke samostatne volá `WebpagesService.regenerateUrl(groupId, currentUser, request, true)`. Obnovia sa automaticky generované alebo zdedené URL stránok v presunutom priečinku a jeho podpriečinkoch podľa nového umiestnenia.

Overenie priority usporiadania je jednoduché - overujú sa prepojené adresáre a zhoda hodnoty priority usporiadania. Ak nie je rovnaká, nastaví sa.

Pri udalosti ```WebjetEventType.ON_DELETE``` sa volaním ```GroupsDB.deleteGroup``` vymažú aj zrkadlené adresáre. Udalosť sa spracuje pred presunom zdrojového adresára do koša. Zrkadlenie sa vykoná len vtedy, ak pre zdrojový adresár platí `MirroringService.isEnabled(groupId)`. Samotná zachovaná hodnota `syncId` v koši preto nestačí; výnimkou môže byť napríklad koreňový adresár, ktorého ID zostalo v konfigurácii zrkadlenia.

## Prepojenie web stránok

Prepojenie web stránok je implementované v ```DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)```, čo rieši uloženie web stránky v editore stránok. Objekt ```DocDetails``` ale neobsahuje hodnotu ```syncId```, preto je v prvom kroku hodnota do objektu nastavená (podľa ```docId```).

Podobne ako pre adresár pri udalosti ```WebjetEventType.ON_START``` sa pre stránku, ktorá ```syncId``` nemá nastavené vygeneruje nová hodnota.

Pri udalosti ```WebjetEventType.AFTER_SAVE``` sa získajú prepojené stránky. Ak žiadne neexistujú vytvoria sa volaním ```DocDB.saveDoc(mirror, false)```, atribút ```false``` rieši problém s rekurziou (nevyvolá znova udalosti). Jediný problém, ktorý môže nastať sú práva a schvaľovanie, to aktuálna implementácia nerieši. Ak redaktor nemá práva na inú jazykovú mutáciu web stránka sa aj tak vytvorí.

Ak už aktuálne ukladaná stránka má prepojené stránky, podobne ako pri adresári sa najprv presunú existujúce verzie do správnych priečinkov. Ich nové umiestnenie sa zohľadní pri zisťovaní chýbajúcich kópií, aby sa namiesto presunu nevytvorila ďalšia stránka. Následne sa overí priorita usporiadania.

Pri ukladaní pracovnej verzie (`doc.getEditorFields().isRequestPublish() == false`) sa preskočí preklad obsahu zrkadlených stránok s `available=true`. Nezverejnené verzie sa môžu preložiť podľa existujúcich pravidiel pre `structureMirroringAutoTranslatorLogin`. Pri publikovaní sa tieto pravidlá vyhodnotia aj pre zverejnené verzie. V starom postupe cez `EditorForm` odovzdáva `EditorDB` skutočný stav publikovania do udalosti `AFTER_SAVE` a `SaveListener.handleEditorSave` ho prenesie do `DocEditorFields`.

Mazanie stránky sa deteguje v ```SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)```. Pri udalosti ```WebjetEventType.ON_DELETE``` sa získa zoznam zrkadlených stránok a pre každú sa zavolá `EditorService.deleteWebpage(syncedDoc, false)`. Zrkadlenie musí byť aktívne pre priečinok zdrojovej stránky.

Pred vyvolaním `ON_DELETE` overí `EditorService.deleteWebpageLogic` cez `checkPermissions(currentUser, mirror, true)` práva ku všetkým zrkadleným stránkam, ktoré sa ešte v tejto požiadavke nezmazali. Ak kontrola zlyhá, mazanie zdrojovej stránky a jej zrkadlených verzií sa nezačne. Kontrola sa vykonáva vo vetve, ktorá už môže mazanie uskutočniť; požiadavky čakajúce na schválenie naďalej používajú schvaľovací postup. `GroupsRestController.deleteItem` pri mazaní priečinka vyžaduje aj právo `deleteDir`.

`DeleteRequestContext` eviduje dokončené zmazania vrátane presunu do koša v atribútoch aktuálnej požiadavky. Kľúč rozlišuje typ položky a jej ID. `EditorService` a `GroupsDB` zaznamenajú položky až po dokončení mazania; neúspešná operácia ani žiadosť o schválenie sa za zmazanie nepovažujú. Pri hromadnom výbere viacerých zrkadlených položiek sa už spracované položky preskočia, takže sa po presune do koša druhýkrát nezmažú.

## Náhľad zrkadlených položiek pred zmazaním

Služba `sk.iway.iwcm.editor.service.DeleteMirrorsService` poskytuje údaje pre potvrdzovacie okno mazania. Údaje iba načíta, samotné mazanie naďalej vykonáva existujúci postup cez udalosti `ON_DELETE`.

| Požiadavka | Výsledok |
| --- | --- |
| `POST /admin/rest/web-pages/delete-mirrors` | Priamo zrkadlené verzie vybraných web stránok. |
| `POST /admin/rest/groups/delete-mirrors` | Priamo zrkadlené priečinky bez rozpisu ich stránok a podpriečinkov. |

Obe požiadavky prijímajú formulárový parameter `ids`, teda kladné ID oddelené čiarkou. Vyžadujú prihlásenie, právo `menuWebpages` a prístup k vybraným položkám. Pri stránkach sa overuje `EditorService.isPageEditable(user, doc, true)`, pri priečinkoch práva `editDir`, `deleteDir` a `GroupsDB.isGroupEditable`. V MultiWeb režime sa kontroluje aj doména nájomcu.

Odpoveď `DeleteMirrorsResult` obsahuje:

- `items` – zoznam položiek s poľami `id`, `fullPath` a `domainName`. Vybrané položky a duplicity sú vylúčené.
- `hiddenMirrors` – hodnota `true`, ak existuje aj zrkadlená položka, ktorej cestu používateľ nesmie vidieť. Klient zobrazí všeobecné upozornenie bez jej identifikácie.

Úspešné načítanie náhľadu nepotvrdzuje oprávnenie zmazať všetky zrkadlené stránky. Klient po načítaní povolí potvrdenie aj pri `hiddenMirrors=true`, ale samotné mazanie stránky ešte vykoná vyššie uvedenú kontrolu práv ku všetkým jej verziám.

Náhľad vychádza z `syncId > 1` a z `MirroringService.isEnabled(...)` pre zdrojovú položku, rovnako ako mazanie. Neplatné ID sa odmietne stavom `400`, neexistujúca položka stavom `404` a neprístupná zdrojová položka stavom `403`.

Funkcia `bindDeleteMirrors` v `admin/v9/src/js/pages/web-pages-list/delete-mirrors.js` počas načítania blokuje potvrdenie mazania. Pri chybe požiadavky alebo neplatnom formáte odpovede ho ponechá zablokované. Po zatvorení alebo opätovnom otvorení okna sa stará požiadavka zruší a jej oneskorená odpoveď sa ignoruje.

## Klonovanie a zrušenie zrkadlenia

`CloneStructureService` počas klonovania uloží dočasné mapovanie do atribútu `RequestBean` s názvom `structureMirroringConfig`. Metódy `MirroringService.isEnabled` a `getRootIds` použijú toto mapovanie iba v danej požiadavke. `MirroringService.isCloneAction()` rozpozná klonovanie podľa prítomnosti atribútu. Overené ID zdrojového a cieľového priečinka sa čítajú z parametrov `srcGroupId` a `destGroupId` v `RequestBean`.

Klonovanie nemení globálne hodnoty `structureMirroringDisabledOnCreate` ani `syncGroupAndWebpageTitle`. Zrkadliace služby pri klonovaní zachovajú zobrazovanie zdrojových stránok a spôsob zobrazenia priečinkov v menu; `GroupsService.canSyncTitle` počas tejto požiadavky vypne synchronizáciu názvu priečinka s hlavnou stránkou. Súbežné požiadavky používajú bežné nastavenia.

Pôvodná hodnota atribútu sa obnoví v bloku `finally`. Pri `keepMirroring=true` sa mapovanie následne zlúči s aktuálnou zdieľanou konfiguráciou a uloží cez `ConfDB.setName`. Metóda `saveMirroringConfig` je synchronizovaná v rámci jednej JVM, aby súbežné klonovania navzájom neprepísali pridané mapovania. Pri `keepMirroring=false` sa zdieľaná konfigurácia nemení; odstránia sa `syncId` v cieli a aj v zdroji, ak zdroj pred klonovaním nemal vlastné `syncId`.

Konfiguračné premenné `mirroringMode`, `cloneActionSrcId` a `cloneActionDestId` boli odstránené. Vlastné rozšírenia majú stav klonovania zisťovať cez `MirroringService.isCloneAction()` a ID čítať z uvedených parametrov aktuálnej požiadavky.

`POST /apps/clone_structure/admin/cancel_sync` prijíma parameter `rootGroupId` a vyžaduje právo `cmp_clone_structure`. Neplatné alebo neexistujúce ID sa odmietne stavom `400`. Pred volaním `MirroringService.clearSyncId` sa pre celý podstrom vrátane koreňa overí `GroupsDB.isGroupEditable`; pri `multiDomainEnabled=true` aj príslušnosť k aktuálnej doméne. Pri `adminCheckUserGroups=true` sa navyše overí `DocDB.canAccess(doc, user, true)` pre každú stránku. Pri nedostatočných právach alebo nesprávnej doméne sa vráti `403` bez zmeny prepojení.

## Zrkadlenie v stromovej štruktúre administrácie

`GroupsTreeService` dopĺňa do uzlov `GroupsJsTreeItem` zoznam `mirrors`. Každý odkaz obsahuje `groupId`, `language` a `path`. Údaje sa pridávajú len do hlavného stromu web stránok, pri prvom vykreslení alebo pri požiadavke s `showMirrorLinks=true`, a len používateľom s právami `menuWebpages` a `cmp_mirroring`. Systémový strom, kôš a výberové polia priečinkov tieto odkazy nedostávajú.

Prepojenia sa vyhľadávajú podľa `syncId`; cieľové priečinky sa filtrujú podľa domény, koša a oprávnení. Jazyk sa určí cez `GroupMirroringServiceV9.getLanguage`, normalizuje sa na malé písmená so spojovníkom a kód `cz` sa zmení na `cs`. Odkazy sú zoradené podľa jazyka a ID. Index priečinkov aj výsledky vyhľadávania sa zdieľajú iba v rámci jednej HTTP požiadavky, takže ďalšia požiadavka znova vyhodnotí aktuálne oprávnenia a údaje.

Zobrazenie diagnostických ikon riadi používateľské nastavenie `showBadMirrorings` v `jstreeSettings_web-pages-list`; pri načítaní stromu sa prenáša parametrom `showBadMirrorings`. Metóda `sk.iway.iwcm.doc.mirroring.rest.MirroringService.getGroupProblems(request)` používa rovnaké kontroly ako tabuľka zrkadlenia a pre každé volanie vlastnú inštanciu služby. Viacnásobné mapovanie má prednosť pred nerovnomerným vnorením. Ikona `ti ti-folder-x`, ktorá označuje obmedzený prístup, sa neprepisuje.

Služba `sk.iway.iwcm.doc.mirroring.rest.MirroringService` pre administračné tabuľky má rozsah `@RequestScope`. Doména, typ operácie, nájdené jazyky, mapovanie jazykov na polia a validačné chyby sa uchovávajú iba počas jednej požiadavky, aby sa neprenášali medzi súbežnými operáciami.

## Vynútenie obnovenia stromovej štruktúry

Ak zmena v štruktúre vyžaduje obnovenie stromovej štruktúry je možné nastaviť v udalostiach atribút:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

Tento je následne v REST službách po dokončení uloženia overený a je vyvolané obnovenie stromovej štruktúry. Volanie cez ```RequestBean``` bolo použité z dôvodu, že je dostupný počas celého ```requestu``` a je staticky dostupný.
