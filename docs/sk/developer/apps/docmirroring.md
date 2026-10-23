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

Pri existencii prepojenia sa overuje, či nastala zmena rodičovského adresára. To je pomerne komplikovaná detekcia. Najskôr sa získa zoznam prepojených adresárov pre aktuálny adresár. Následne sa overuje, či skutočne prepojené adresáre majú rodičov z uvedeného zoznamu. Ak nie, tak sa vykoná presun adresára do korektného.

Overenie priority usporiadania je jednoduché - overujú sa prepojené adresáre a zhoda hodnoty priority usporiadania. Ak nie je rovnaká, nastaví sa.

Pri udalosti ```WebjetEventType.ON_DELETE``` sa volaním ```GroupsDB.deleteGroup``` vymažú aj zrkadlené adresáre. Udalosť sa spracuje pred presunom zdrojového adresára do koša. Zrkadlenie sa vykoná len vtedy, ak pre zdrojový adresár platí `MirroringService.isEnabled(groupId)`. Samotná zachovaná hodnota `syncId` v koši preto nestačí; výnimkou môže byť napríklad koreňový adresár, ktorého ID zostalo v konfigurácii zrkadlenia.

## Prepojenie web stránok

Prepojenie web stránok je implementované v ```DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)```, čo rieši uloženie web stránky v editore stránok. Objekt ```DocDetails``` ale neobsahuje hodnotu ```syncId```, preto je v prvom kroku hodnota do objektu nastavená (podľa ```docId```).

Podobne ako pre adresár pri udalosti ```WebjetEventType.ON_START``` sa pre stránku, ktorá ```syncId``` nemá nastavené vygeneruje nová hodnota.

Pri udalosti ```WebjetEventType.AFTER_SAVE``` sa získajú prepojené stránky. Ak žiadne neexistujú vytvoria sa volaním ```DocDB.saveDoc(mirror, false)```, atribút ```false``` rieši problém s rekurziou (nevyvolá znova udalosti). Jediný problém, ktorý môže nastať sú práva a schvaľovanie, to aktuálna implementácia nerieši. Ak redaktor nemá práva na inú jazykovú mutáciu web stránka sa aj tak vytvorí.

Ak už aktuálne ukladaná stránka má prepojené stránky podobne ako pre adresár sa overuje správnosť rodičovského adresára a priority usporiadania.

Mazanie stránky sa deteguje v ```SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)```. Pri udalosti ```WebjetEventType.ON_DELETE``` sa získa zoznam zrkadlených stránok a pre každú sa zavolá `EditorService.deleteWebpage(syncedDoc, false)`. Zrkadlenie musí byť aktívne pre priečinok zdrojovej stránky.

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

Náhľad vychádza z `syncId > 1` a z `MirroringService.isEnabled(...)` pre zdrojovú položku, rovnako ako mazanie. Neplatné ID sa odmietne stavom `400`, neexistujúca položka stavom `404` a neprístupná zdrojová položka stavom `403`.

Funkcia `bindDeleteMirrors` v `admin/v9/src/js/pages/web-pages-list/delete-mirrors.js` počas načítania blokuje potvrdenie mazania. Pri chybe požiadavky alebo neplatnom formáte odpovede ho ponechá zablokované. Po zatvorení alebo opätovnom otvorení okna sa stará požiadavka zruší a jej oneskorená odpoveď sa ignoruje.

## Zrkadlenie v stromovej štruktúre administrácie

`GroupsTreeService` dopĺňa do uzlov `GroupsJsTreeItem` zoznam `mirrors`. Každý odkaz obsahuje `groupId`, `language` a `path`. Údaje sa pridávajú len do hlavného stromu web stránok, pri prvom vykreslení alebo pri požiadavke s `showMirrorLinks=true`, a len používateľom s právami `menuWebpages` a `cmp_mirroring`. Systémový strom, kôš a výberové polia priečinkov tieto odkazy nedostávajú.

Prepojenia sa vyhľadávajú podľa `syncId`; cieľové priečinky sa filtrujú podľa domény, koša a oprávnení. Jazyk sa určí cez `GroupMirroringServiceV9.getLanguage`, normalizuje sa na malé písmená so spojovníkom a kód `cz` sa zmení na `cs`. Odkazy sú zoradené podľa jazyka a ID. Index priečinkov aj výsledky vyhľadávania sa zdieľajú iba v rámci jednej HTTP požiadavky, takže ďalšia požiadavka znova vyhodnotí aktuálne oprávnenia a údaje.

Zobrazenie diagnostických ikon riadi používateľské nastavenie `showBadMirrorings` v `jstreeSettings_web-pages-list`; pri načítaní stromu sa prenáša parametrom `showBadMirrorings`. Metóda `sk.iway.iwcm.doc.mirroring.rest.MirroringService.getGroupProblems(request)` používa rovnaké kontroly ako tabuľka zrkadlenia a pre každé volanie vlastnú inštanciu služby. Viacnásobné mapovanie má prednosť pred nerovnomerným vnorením. Ikona `ti ti-folder-x`, ktorá označuje obmedzený prístup, sa neprepisuje.

## Vynútenie obnovenia stromovej štruktúry

Ak zmena v štruktúre vyžaduje obnovenie stromovej štruktúry je možné nastaviť v udalostiach atribút:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

Tento je následne v REST službách po dokončení uloženia overený a je vyvolané obnovenie stromovej štruktúry. Volanie cez ```RequestBean``` bolo použité z dôvodu, že je dostupný počas celého ```requestu``` a je staticky dostupný.
