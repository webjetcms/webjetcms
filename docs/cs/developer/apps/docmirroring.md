# Zrcadlení struktury

Uživatelský popis struktury zrcadlení je v části [dokumentace k editoru](../../redactor/apps/docmirroring/README.md). V této dokumentaci je uveden pouze technický popis.

Základní balíček java je `sk.iway.iwcm.components.structuremirroring` a adresář pro komponenty JSP `/components/structuremirroring/`.

## Metoda propojení

Propojení jednotlivých adresářů a stránek používá databázový atribut `sync_id`, který se původně používal pro synchronizaci mezi WebJety. Taková funkce se však v současné době nepoužívá (používá se export a import ve formátu XML) a atribut nebyl využit.

V atributu `sync_id` propojené adresáře a stránky jsou nastaveny na hodnotu z generátoru klíčů voláním `PkeyGenerator.getNextValue("structuremirroring")`. Hodnota `sync_id` propojených adresářů nebo stránek musí být samozřejmě stejné. Podle hodnoty v `sync_id` propojené adresáře/stránky lze vyhledávat v databázi.

## Inicializace propojení

Odkaz je nastaven v konfigurační proměnné `structureMirroringConfig` kde každý řádek obsahuje seznam ID adresářů (hlavních adresářů daného jazyka), které mají být propojeny. Nastavení konfigurační proměnné vyvolá událost, která naslouchá `SaveListener.handleConfSave` a následně vyvolá `MirroringService.checkRootGroupsConfig()`. Zde je zaškrtnut atribut `syncId` definovaných adresářů, pokud není nastaven, nastaví se automaticky.

Volání na `MirroringService.isEnabled(int groupId)` ověří, zda je pro zadané ID adresáře povoleno zrcadlení struktury.

## Propojení adresářů

Propojení adresářů se provádí nasloucháním události změny adresáře. `SaveListener.handleGroupSave`, který následně volá `GroupMirroringServiceV9.handleGroupSave`.

Poznámka: výjimka se týká adresáře s názvem **Nový podadresář** (překladatelský klíč `editor.dir.new_dir`), který se vytvoří ve stromové struktuře kliknutím na tlačítko Přidat nový adresář v kontextové nabídce. Tím by se vytvořily adresáře v jiných jazycích `Nový podadresár` což by nemělo praktický význam (protože změna názvu adresáře již nezmění vytvořené zrcadlené kopie).

V případě `WebjetEventType.ON_START` zkontroluje se, zda adresář již nemá nastaveno `syncId`. Pokud ne, víme, že se jedná o nový adresář, který ještě není zrcadlený, a nastavíme pro něj novou hodnotu. `syncId`.

V případě `WebjetEventType.AFTER_SAVE`, tj. po uložení adresáře podle příkazu `syncId` hledání zrcadlených adresářů. Pokud žádné neexistují, vytvoří se nové adresáře. Používá se rozhraní API `groupsDB.setGroup(mirror, false);` s ostatními `false` atribut, který **nevyvolá** po uložení adresáře (což by způsobilo rekurzi).

Pokud odkaz existuje, ověřuje, zda došlo ke změně nadřazeného adresáře. Jedná se o poměrně složitou detekci. Nejprve je pro aktuální adresář získán seznam propojených adresářů. Poté se ověří, zda skutečně propojené adresáře mají rodiče z tohoto seznamu. Pokud tomu tak není, provede se přesun adresáře do správného adresáře.

Ověření priority řazení je jednoduché - ověřují se propojené adresáře a shodné hodnoty priority řazení. Pokud se neshodují, nastaví se.

V případě `WebjetEventType.AFTER_DELETE` s výzvou `GroupsDB.deleteGroup` zrcadlené adresáře jsou také odstraněny. Ty jsou obvykle přesunuty do koše. Zůstanou nastaveny `syncId`, ale protože se nacházejí mimo zrcadlenou strukturu, jejich následné změny se již nezrcadlí.

## Propojení webových stránek

Propojení webových stránek je implementováno v `DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)`, který řeší uložení webové stránky v editoru stránek. Objekt `DocDetails` ale neobsahuje hodnotu `syncId`, takže v prvním kroku se objektu nastaví hodnota (podle příkazu `docId`).

Podobně jako adresář pro událost `WebjetEventType.ON_START` pro stránku, která `syncId` nebyla nastavena, vygeneruje novou hodnotu.

V případě `WebjetEventType.AFTER_SAVE` jsou získány propojené stránky. Pokud žádné neexistují, vytvoří se voláním `DocDB.saveDoc(mirror, false)`, atribut `false` řeší problém s rekurzí (nespustí události znovu). Jediný problém, který může nastat, jsou práva a schvalování, současná implementace to neřeší. Pokud editor nemá práva k jinému jazyku, stránka se přesto vytvoří.

Pokud je na aktuálně uloženou stránku již navázáno, ověří se správnost nadřazeného adresáře a priority řazení.

Odstranění stránky je detekováno v `SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)`. V případě `WebjetEventType.AFTER_DELETE` je načten seznam zrcadlených stránek a. `DeleteServlet.deleteDoc`.

## Vynucení obnovy stromové struktury

Pokud změna ve struktuře vyžaduje obnovení stromové struktury, lze v událostech nastavit atribut:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

Po dokončení ukládání a vyvolání obnovy stromové struktury se tato skutečnost ověří ve službách REST. Volání prostřednictvím `RequestBean` byla použita z toho důvodu, že je dostupná v celém světě. `requestu` a je staticky k dispozici.
