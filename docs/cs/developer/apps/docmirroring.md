# Zrcadlení struktury

Uživatelský popis zrcadlení struktury je v [dokumentaci pro redaktora](../../redactor/apps/docmirroring/README.md). Tato dokumentace obsahuje pouze technický popis.

Základní java package je `sk.iway.iwcm.components.structuremirroring` a adresář pro JSP komponenty `/components/structuremirroring/`.

## Způsob propojení

Propojení jednotlivých adresářů a stránek využívá databázový atribut `sync_id`, původně používaný pro synchronizaci mezi WebJETy. Taková funkcionalita se ale aktuálně nepoužívá (využívá se export a import v XML formátu) a daný atribut nebyl využíván.

Do atributu `sync_id` se propojeným adresářem a stránkám nastavuje hodnota z generátoru klíčů voláním `PkeyGenerator.getNextValue("structuremirroring")`. Hodnota `sync_id` navzájem propojených adresářů nebo stránek musí samozřejmě být stejná. Podle hodnoty v `sync_id` lze v databázi vyhledat propojené adresáře/stránky.

## Inicializace propojení

Propojení se nastavuje v konfigurační proměnné `structureMirroringConfig` kde na každém řádku je seznam ID adresářů (hlavní adresáře dané jazykové mutace), které mají být propojeny. Nastavení konfigurační proměnné vyvolá událost, kterou poslouchá `SaveListener.handleConfSave` a následně vyvolá `MirroringService.checkRootGroupsConfig()`. Zde se zkontroluje atribut `syncId` zadefinovaných adresářů, pokud není nastaven automaticky se nastaví.

Volání `MirroringService.isEnabled(int groupId)` ověřuje, zda je pro zadané ID adresáře zapnuto zrcadlení struktury.

## Propojení adresářů

Propojení adresářů je realizováno posloucháním události změny adresáře `SaveListener.handleGroupSave`, co následně volá `GroupMirroringServiceV9.handleGroupSave`.

Poznámka: výjimka je pro adresář s názvem **Nový podadresář** (překladový klíč `editor.dir.new_dir`), který vznikne ve stromové struktuře kliknutím na přidat nový adresář v kontextovém menu. Takto by vznikly v ostatních jazykových mutacích adresáře `Nový podadresár`, což by nemělo praktický význam (jelikož změna názvu adresáře již nemění vytvořené zrcadlené kopie).

Při události `WebjetEventType.ON_START` se kontroluje, zda adresář již má nastaveno `syncId`. Pokud ne, víme, že se jedná o nový adresář, který ještě není zrcadlený a nastavíme mu novou hodnotu `syncId`.

Při události `WebjetEventType.AFTER_SAVE`, tedy po uložení adresáře podle `syncId` hledáme zrcadlené adresáře. Pokud žádné neexistují, vytvoří se nové adresáře. Je využito API `groupsDB.setGroup(mirror, false);` s druhým `false` atributem, který **nevyvolá** znovu událost po uložení adresáře (což by způsobilo rekurzi).

Při existenci odkazu se ověřuje, zda nastala změna rodičovského adresáře. To je poměrně komplikovaná detekce. Nejprve se získá seznam propojených adresářů pro aktuální adresář. Následně se ověřuje, zda skutečně propojené adresáře mají rodiče z uvedeného seznamu. Pokud ne, tak se provede přesun adresáře do korektního.

Ověření priority uspořádání je jednoduché - ověřují se propojené adresáře a shoda hodnoty priority uspořádání. Pokud není stejná, nastaví se.

Při události `WebjetEventType.AFTER_DELETE` se voláním `GroupsDB.deleteGroup` vymažou i zrcadlené adresáře. Ty se typicky přesunou do koše. Zůstane jim sice nastaveno `syncId`, ale jelikož se nacházejí mimo zrcadlené struktury, tak jejich následné změny již nejsou zrcadlené.

## Propojení web stránek

Propojení web stránek je implementováno v `DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)`, což řeší uložení web stránky v editoru stránek. Objekt `DocDetails` ale neobsahuje hodnotu `syncId`, proto je v prvním kroku hodnota do objektu nastavena (dle `docId`).

Podobně jako pro adresář při události `WebjetEventType.ON_START` se pro stránku, která `syncId` nemá nastaveno vygeneruje nová hodnota.

Při události `WebjetEventType.AFTER_SAVE` se získají propojené stránky. Pokud žádné neexistují vytvoří se voláním `DocDB.saveDoc(mirror, false)`, atribut `false` řeší problém s rekurzí (nevyvolá znovu události). Jediný problém, který může nastat jsou práva a schvalování, to aktuální implementace neřeší. Pokud redaktor nemá práva na jinou jazykovou mutaci web stránka se stejně vytvoří.

Pokud již aktuálně ukládaná stránka má propojené stránky podobně jako pro adresář se ověřuje správnost rodičovského adresáře a priority uspořádání.

Mazání stránky se detekuje v `SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)`. Při události `WebjetEventType.AFTER_DELETE` se získá seznam zrcadlených stránek a vyvolá se `DeleteServlet.deleteDoc`.

## Vynucení obnovení stromové struktury

Pokud změna ve struktuře vyžaduje obnovení stromové struktury lze nastavit v událostech atribut:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

Tento je následně v REST službách po dokončení uložení ověřen a je vyvoláno obnovení stromové struktury. Volání přes `RequestBean` bylo použito z důvodu, že je dostupný během celého `requestu` a je staticky dostupný.
