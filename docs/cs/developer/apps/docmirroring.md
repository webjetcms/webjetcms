# Zrcadlení struktury

Uživatelský popis zrcadlení struktury je v [dokumentaci pro redaktora](../../redactor/apps/docmirroring/README.md). Tato dokumentace obsahuje pouze technický popis.

Základní java package je ```sk.iway.iwcm.components.structuremirroring``` a adresář pro JSP komponenty ```/components/structuremirroring/```.

## Způsob propojení

Propojení jednotlivých adresářů a stránek využívá databázový atribut ```sync_id```, původně používaný pro synchronizaci mezi WebJETy. Taková funkcionalita se ale aktuálně nepoužívá (využívá se export a import v XML formátu) a daný atribut nebyl využíván.

Do atributu ```sync_id``` se propojeným adresářem a stránkám nastavuje hodnota z generátoru klíčů voláním ```PkeyGenerator.getNextValue("structuremirroring")```. Hodnota ```sync_id``` navzájem propojených adresářů nebo stránek musí samozřejmě být stejná. Podle hodnoty v ```sync_id``` lze v databázi vyhledat propojené adresáře/stránky.

## Inicializace propojení

Propojení se nastavuje v konfigurační proměnné ```structureMirroringConfig``` kde na každém řádku je seznam ID adresářů (hlavní adresáře dané jazykové mutace), které mají být propojeny. Nastavení konfigurační proměnné vyvolá událost, kterou poslouchá ```SaveListener.handleConfSave``` a následně vyvolá ```MirroringService.checkRootGroupsConfig()```. Zde se zkontroluje atribut ```syncId``` zadefinovaných adresářů, pokud není nastaven automaticky se nastaví.

Volání ```MirroringService.isEnabled(int groupId)``` ověřuje, zda je pro zadané ID adresáře zapnuto zrcadlení struktury.

## Propojení adresářů

Propojení adresářů je realizováno posloucháním události změny adresáře ```SaveListener.handleGroupSave```, což následně volá ```GroupMirroringServiceV9.handleGroupSave```.

Poznámka: výjimka je pro adresář s názvem **Nový podadresář** (překladový klíč ```editor.dir.new_dir```), který vznikne ve stromové struktuře kliknutím na přidat nový adresář v kontextovém menu. Takto by vznikly v ostatních jazykových mutacích adresáře ```Nový podadresár```, což by nemělo praktický význam (jelikož změna názvu adresáře již nemění vytvořené zrcadlené kopie).

Při události ```WebjetEventType.ON_START``` se kontroluje, zda adresář již má nastaveno ```syncId```. Pokud ne, víme, že se jedná o nový adresář, který ještě není zrcadlený a nastavíme mu novou hodnotu ```syncId```.

Při události ```WebjetEventType.AFTER_SAVE```, tedy po uložení adresáře podle ```syncId``` hledáme zrcadlené adresáře. Pokud žádné neexistují, vytvoří se nové adresáře. Je využito API ```groupsDB.setGroup(mirror, false);``` s druhým ```false``` atributem, který **nevyvolá** znovu událost po uložení adresáře (což by způsobilo rekurzi).

Při existenci odkazu se ověřuje rodičovský adresář každé zrcadlené verze. Pokud neodpovídá mapování nového rodiče, stávající verze se přesune. Až po aktualizaci jejího umístění se zjišťuje, zda je třeba vytvořit chybějící kopie. Tím se při přesunu zachovají ID a nevzniknou duplicitní složky.

Uložení zrcadlené složky s parametrem `publishEvents=false` nevyvolá `SaveGroupListener`. Proto `GroupMirroringServiceV9` po přesunu v HTTP požadavku samostatně volá `WebpagesService.regenerateUrl(groupId, currentUser, request, true)`. Obnoví se automaticky generované nebo zděděné URL stránek v přesunuté složce a její podsložkách podle nového umístění.

Ověření priority uspořádání je jednoduché - ověřují se propojené adresáře a shoda hodnoty priority uspořádání. Pokud není stejná, nastaví se.

Při události ```WebjetEventType.ON_DELETE``` se voláním ```GroupsDB.deleteGroup``` vymažou i zrcadlené adresáře. Událost se zpracuje před přesunem zdrojového adresáře do koše. Zrcadlení se provede pouze tehdy, pokud pro zdrojový adresář platí `MirroringService.isEnabled(groupId)`. Samotná zachovaná hodnota `syncId` v koši proto nestačí; výjimkou může být například kořenový adresář, jehož ID zůstalo v konfiguraci zrcadlení.

## Propojení web stránek

Propojení web stránek je implementováno v ```DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)```, což řeší uložení web stránky v editoru stránek. Objekt ```DocDetails``` ale neobsahuje hodnotu ```syncId```, proto je v prvním kroku hodnota do objektu nastavena (podle ```docId```).

Podobně jako pro adresář při události ```WebjetEventType.ON_START``` se pro stránku, která ```syncId``` nemá nastaveno vygeneruje nová hodnota.

Při události ```WebjetEventType.AFTER_SAVE``` se získají propojené stránky. Pokud žádné neexistují vytvoří se voláním ```DocDB.saveDoc(mirror, false)```, atribut ```false``` řeší problém s rekurzí (nevyvolá znovu události). Jediný problém, který může nastat jsou práva a schvalování, to aktuální implementace neřeší. Pokud redaktor nemá práva na jinou jazykovou mutaci web stránka se stejně vytvoří.

Pokud již aktuálně ukládaná stránka má propojené stránky, podobně jako u adresáře se nejprve přesunou stávající verze do správných složek. Jejich nové umístění se zohlední při zjišťování chybějících kopií, aby se místo přesunu nevytvořila další stránka. Následně se ověří priorita uspořádání.

Při ukládání pracovní verze (`doc.getEditorFields().isRequestPublish() == false`) se přeskočí překlad obsahu zrcadlených stránek s `available=true`. Nezveřejněné verze mohou být přeloženy podle stávajících pravidel pro `structureMirroringAutoTranslatorLogin`. Při publikování se tato pravidla vyhodnotí i pro zveřejněné verze. Ve starém postupu přes `EditorForm` předává `EditorDB` skutečný stav publikování do události `AFTER_SAVE` a `SaveListener.handleEditorSave` ho přenese do `DocEditorFields`.

Mazání stránky se detekuje v ```SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)```. Při události ```WebjetEventType.ON_DELETE``` se získá seznam zrcadlených stránek a pro každou se zavolá `EditorService.deleteWebpage(syncedDoc, false)`. Zrcadlení musí být aktivní pro složku zdrojové stránky.

Před vyvoláním `ON_DELETE` ověří `EditorService.deleteWebpageLogic` přes `checkPermissions(currentUser, mirror, true)` práva ke všem zrcadleným stránkám, které se ještě v tomto požadavku nesmazaly. Pokud kontrola selže, mazání zdrojové stránky a jejích zrcadlených verzí se nezačne. Kontrola se provádí ve větvi, která již může mazání provést; požadavky čekající na schválení nadále používají schvalovací postup. `GroupsRestController.deleteItem` při mazání složky vyžaduje také právo `deleteDir`.

`DeleteRequestContext` eviduje dokončená smazání včetně přesunu do koše v atributech aktuálního požadavku. Klíč rozlišuje typ položky a její ID. `EditorService` a `GroupsDB` zaznamenají položky až po dokončení mazání; neúspěšná operace ani žádost o schválení se za smazání nepovažují. Při hromadném výběru více zrcadlených položek se již zpracované položky přeskočí, takže se po přesunu do koše podruhé nesmažou.

## Náhled zrcadlených položek před smazáním

Služba `sk.iway.iwcm.editor.service.DeleteMirrorsService` poskytuje údaje pro potvrzovací okno mazání. Data pouze načte, samotné mazání nadále provádí existující postup přes události `ON_DELETE`.

| Požadavek | Výsledek |
| --- | --- |
| `POST /admin/rest/web-pages/deletion-preview` | Přímo zrcadlené verze vybraných web stránek. |
| `POST /admin/rest/groups/deletion-preview` | Přímo zrcadlené složky bez rozpisu jejich stránek a podsložek. |

Oba požadavky přijímají formulářový parametr `ids`, tedy kladné ID oddělené čárkou. Vyžadují přihlášení, právo `menuWebpages` a přístup k vybraným položkám. U stránek se ověřuje `EditorService.isPageEditable(user, doc, true)`, u složek práva `editDir`, `deleteDir` a `GroupsDB.isGroupEditable`. V MultiWeb režimu se kontroluje i doména nájemce.

Odpověď `DeleteMirrorsResult` obsahuje:

- `items` – seznam položek s poli `id`, `fullPath` a `domainName`. Vybrané položky a duplicity jsou vyloučeny.
- `hiddenMirrors` – hodnota `true`, pokud existuje i zrcadlená položka, jejíž cestu uživatel nesmí vidět. Klient zobrazí obecné upozornění bez její identifikace.

Úspěšné načtení náhledu nepotvrzuje oprávnění smazat všechny zrcadlené stránky. Klient po načtení povolí potvrzení iu `hiddenMirrors=true`, ale samotné mazání stránky ještě provede výše uvedenou kontrolu práv ke všem jejím verzím.

Náhled vychází z `syncId > 1` az `MirroringService.isEnabled(...)` pro zdrojovou položku, stejně jako mazání. Neplatné ID se odmítne stavem `400`, neexistující položka stavem `404` a nepřístupná zdrojová položka stavem `403`.

Funkce `bindDeleteMirrors` v `admin/v9/src/js/pages/web-pages-list/delete-mirrors.js` během načítání blokuje potvrzení mazání. Při chybě požadavku nebo neplatném formátu odpovědi jej ponechá zablokováno. Po zavření nebo opětovném otevření okna se starý požadavek zruší a jeho opožděná odpověď se ignoruje.

## Klonování a zrušení zrcadlení

`CloneStructureService` během klonování uloží dočasné mapování do atributu `RequestBean` s názvem `structureMirroringConfig`. Metody `MirroringService.isEnabled` a `getRootIds` použijí toto mapování pouze v daném požadavku. `MirroringService.isCloneAction()` rozpozná klonování podle přítomnosti atributu. Ověřené ID zdrojové a cílové složky se čtou z parametrů `srcGroupId` a `destGroupId` v `RequestBean`.

Klonování nemění globální hodnoty `structureMirroringDisabledOnCreate` ani `syncGroupAndWebpageTitle`. Zrcadlící služby při klonování zachovají zobrazování zdrojových stránek a způsob zobrazení složek v menu; `GroupsService.canSyncTitle` během tohoto požadavku vypne synchronizaci názvu složky s hlavní stránkou. Souběžné požadavky používají běžná nastavení.

Původní hodnota atributu se obnoví v bloku `finally`. Při `keepMirroring=true` se mapování následně sloučí s aktuální sdílenou konfigurací a uloží přes `ConfDB.setName`. Metoda `saveMirroringConfig` je synchronizována v rámci jedné JVM, aby souběžná klonování navzájem nepřepsala přidaná mapování. U `keepMirroring=false` se sdílená konfigurace nemění; odstraní se `syncId` v cíli i ve zdroji, pokud zdroj před klonováním neměl vlastní `syncId`.

`POST /apps/clone_structure/admin/cancel_sync` přijímá parametr `rootGroupId` a vyžaduje právo `cmp_clone_structure`. Neplatné nebo neexistující ID se odmítne stavem `400`. Před voláním `MirroringService.clearSyncId` se pro celý podstrom včetně kořene ověří `GroupsDB.isGroupEditable` ; u `multiDomainEnabled=true` i příslušnost k aktuální doméně. U `adminCheckUserGroups=true` se navíc ověří `DocDB.canAccess(doc, user, true)` pro každou stránku. Při nedostatečných právech nebo nesprávné doméně se vrátí `403` beze změny propojení.

## Zrcadlení ve stromové struktuře administrace

`GroupsTreeService` doplňuje do uzlů `GroupsJsTreeItem` seznam `mirrors`. Každý odkaz obsahuje `groupId`, `language` a `path`. Údaje se přidávají pouze do hlavního stromu web stránek, při zapnuté volbě **Zobrazit zrcadlení**, při prvním vykreslení nebo při požadavku s `showMirrorLinks=true`, a pouze uživatelům s právy `menuWebpages` a `cmp_mirroring`. Systémový strom, koš a výběrová pole složek tyto odkazy nedostávají.

Odkazy se vyhledávají podle `syncId` ; cílové složky se filtrují podle domény, koše a oprávnění. Jazyk se určí přes `GroupMirroringServiceV9.getLanguage`, normalizuje se na malá písmena se spojovníkem a kód `cz` se změní na `cs`. Odkazy jsou seřazeny podle jazyka a ID. Index složek i výsledky vyhledávání se sdílejí pouze v rámci jednoho HTTP požadavku, takže další požadavek znovu vyhodnotí aktuální oprávnění a data.

Zobrazení jazykových odkazů i diagnostických ikon řídí uživatelské nastavení `showMirrorings` v `jstreeSettings_web-pages-list` ; při načítání stromu se přenáší parametrem `showMirrorings`. Metoda `sk.iway.iwcm.doc.mirroring.rest.MirroringService.getGroupProblems(request)` používá stejné kontroly jako tabulka zrcadlení a pro každé volání vlastní instanci služby. Vícenásobné mapování má přednost před nerovnoměrným vnořením. Diagnostika se přenáší v `mirroringProblem` s poli `icon` a `title`. Frontend ji zobrazí vpravo před jazykovými odkazy; běžná ikona složky se nemění. Složky s ikonou `ti ti-folder-x`, která označuje omezený přístup, diagnostiku nedostávají.

Služba `sk.iway.iwcm.doc.mirroring.rest.MirroringService` pro administrační tabulky má rozsah `@RequestScope`. Doména, typ operace, nalezené jazyky, mapování jazyků na pole a validační chyby se uchovávají pouze po dobu jednoho požadavku, aby se nepřenášely mezi souběžnými operacemi.

## Vynucení obnovení stromové struktury

Pokud změna ve struktuře vyžaduje obnovení stromové struktury lze nastavit v událostech atribut:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

Tento je následně v REST službách po dokončení uložení ověřen a je vyvoláno obnovení stromové struktury. Volání přes ```RequestBean``` bylo použito z důvodu, že je dostupný během celého ```requestu``` a je staticky dostupný.
