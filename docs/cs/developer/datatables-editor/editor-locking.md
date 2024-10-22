# Oznámení o konfliktu

Oznámení o konfliktu poskytuje funkci, která uživatele při úpravě záznamu upozorní, pokud stejný záznam ve stejné tabulce upravuje i jiný uživatel.

![](editor-locking.png)

## Backend

Implementaci logiky backendu najdete v souboru [EditorLockingRestController.java](../../../src/main/java/sk/iway/iwcm/system/datatable/editorlocking/EditorLockingRestController.java). REST url této služby je `/admin/rest/editorlocking`. Tento řadič se stará o přidávání a mazání `editoLocking` záznamy z mezipaměti. Ve vyrovnávací paměti jsou tyto záznamy uloženy jako seznam `EditorLockingBean` entity pro každou tabulku zvlášť (samostatný záznam v mezipaměti), přičemž každá entita představuje editaci jednoho záznamu konkrétním uživatelem.

```java
@RestController
@RequestMapping("/admin/rest/editorlocking")
@ResponseBody
public class EditorLockingRestController {
```

### Přidání položky

Po zavolání url REST `/admin/rest/editorlocking/open/{entityId}/{tableUniqueId}` objekt obsahující seznam všech `EditorLockingBean` entity podle `tableUniqueId` (pokud v paměti neexistuje, vytvoří se nový seznam) voláním `getCacheList(tableUniqueId);`. Zkontroluje se, zda již obsahuje entitu podle `entityId` a `userId`. V opačném případě je třeba vytvořit nový `EditorLockingBean` entita, která má být přidán do seznamu. Před uložením jsou entity, jejichž platnost vypršela, z pole odstraněny. `editorLocking` Záznamy. Metoda vrací seznam ostatních uživatelů, kteří upravují stejný záznam (nebo prázdné pole).

```java
@GetMapping({ "/open/{entityId}/{tableUniqueId}" })
public List<UserDto> addEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```

### Odstranění položky

Po zavolání url REST `/admin/rest/editorlocking/close/{entityId}/{tableUniqueId}` objekt obsahující seznam všech `EditorLockingBean` entit voláním `getCacheList(tableUniqueId);`. Podle parametrů `EditorLockingBean` v seznamu a odstraněny.

```java
@GetMapping({ "/close/{entityId}/{tableUniqueId}" })
public void removeEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```

Záznam je v uloženém seznamu zapamatován po dobu 2 minut (definováno v konstantě `CACHE_EXPIRE_MINUTES`). V uživatelském rozhraní je služba `addEdit` volán každou minutu, pokud uživatel okno jednoduše zavře, aniž by zavřel dialogové okno editoru, platnost záznamu vyprší po 2 minutách.

### Ukládání do mezipaměti

Seznam uživatelů, kteří upravují stejnou tabulku, je uložen ve složce `Cache` objekt s klíčem `"editor.locking-"+tableUniqueId`. Logika je v metodě `private List<EditorLockingBean> getCacheList(String tableUniqueId)`. Pokud pro daný `tableUniqueId` seznam v mezipaměti neexistuje, pokud existuje, je prodloužen o 7 minut. Pokud tabulka neexistuje `tableUniqueId` do 7 minut nebylo uskutečněno žádné volání, záznam je z mezipaměti zcela vymazán.

## Frontend

Hlavní implementace logiky frontendu se nachází v souboru [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js).

Funkce `bindEditorNotify` obsahuje 2 události, které se nastavují a volají při otevření a zavření editoru. Vstupním parametrem této funce je EDITOR, z něhož se získávají další potřebné informace, jako je jedinečný název tabulky nebo id editovaného záznamu. Samotné volání funkce se provádí v souboru [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js). Volaná událost bude dále volat editor při otevření editoru. `callAddEditorLocking` (navíc nastaví 60sekundový interval, který se přeruší pouze při zavření editoru) a volání událostí při zavření editoru. `callRemoveEditorLocking` funkce.

Jedinečný název tabulky je generován ve funkci `getUniqueTableId(TABLE)` a je vytvořen z adresy URL rozhraní REST (znak / je nahrazen znakem -, předpona `/admin/rest/` je odstraněn).

Služba REST je volána při otevření dialogového okna editoru a poté každých 60 sekund.

### Přidání položky

Funkce `callAddEditorLocking` je vyvolán událostí po otevření editoru. Pokud je hodnota `entityId` získané od EDITORA jiné než `null` nebo -1 (což představuje nový záznam), zavolá se ajaxové volání REST url a přidá se nový záznam. `editorLocking`. Návratovou hodnotou z Backendu je pole dalších uživatelů, kteří právě upravují stejný záznam ve stejné tabulce. Pokud pole není prázdné, pomocí příkazu `WJ.notifyInfo` bude uživatel opakovaně upozorněn, kteří další uživatelé upravují stejný záznam.

### Odstranění položky

Funkce `callRemoveEditorLocking` je volán událostí po zavření editoru. Pokud je hodnota `entityId` získané od EDITORA jiné než `null` nebo -1 (což představuje nový záznam), zavolá se ajaxové volání REST url pro smazání stávajícího záznamu. `editorLocking` záznamu.
