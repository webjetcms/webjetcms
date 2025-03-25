# Upozornění o konfliktu

Upozornění o konfliktu zajišťuje funkcionalitu, která upozorní uživatele během editace záznamu, pokud i jiný uživatel edituje stejný záznam ve stejné tabulce.

![](editor-locking.png)

## Backend

Implementace backend logiky se nachází v souboru [EditorLockingRestController.java](../../../src/main/java/sk/iway/iwcm/system/datatable/editorlocking/EditorLockingRestController.java). REST url této služby je `/admin/rest/editorlocking`. Tento controller se stará o přidávání a mazání `editoLocking` záznamů z cache paměti. V cache jsou tyto záznamy uloženy jako seznam `EditorLockingBean` entit pro každou tabulku samostatně (samostatný cache záznam), kde každá entita představuje editování jednoho záznamu konkrétním uživatelem.

```java
@RestController
@RequestMapping("/admin/rest/editorlocking")
@ResponseBody
public class EditorLockingRestController {
```

### Přidání záznamu

Po zavolání REST url `/admin/rest/editorlocking/open/{entityId}/{tableUniqueId}` se z cache paměti získá objekt obsahující seznam všech `EditorLockingBean` entit podle `tableUniqueId` (pokud v paměti neexistuje vytvoří se nový seznam) voláním `getCacheList(tableUniqueId);`. Ověří se ať už obsahuje entitu podle `entityId` a `userId`. Pokud ne vytvoří se nová `EditorLockingBean` entita, která se do seznamu přidá. Před uložením se z pole odstraní exspirované `editorLocking` záznamy. Metoda vrátí seznam ostatních uživatelů, kteří editují stejný záznam (nebo prázdné pole).

```java
@GetMapping({ "/open/{entityId}/{tableUniqueId}" })
public List<UserDto> addEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```

### Vymazání záznamu

Po zavolání REST url `/admin/rest/editorlocking/close/{entityId}/{tableUniqueId}` se z cache paměti získá objekt obsahující seznam všech `EditorLockingBean` entit voláním `getCacheList(tableUniqueId);`. Podle parametrů se vyhledá daný `EditorLockingBean` v seznamu a odstraní se.

```java
@GetMapping({ "/close/{entityId}/{tableUniqueId}" })
public void removeEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```

Záznam je v uloženém seznamu zapamatován na 2 minuty (definováno v konstantě `CACHE_EXPIRE_MINUTES`). Z uživatelského rozhraní je služba `addEdit` volána každou minutu, pokud uživatel jen jednoduše zavře okno bez zavření dialogového okna editoru záznam tedy exspiruje po 2 minutách.

### Ukládání do cache

Seznam uživatelů editujících stejnou tabulku se ukládá do `Cache` objektu s klíčem `"editor.locking-"+tableUniqueId`. Logika je v metodě `private List<EditorLockingBean> getCacheList(String tableUniqueId)`. Pokud pro danou `tableUniqueId` seznam v cache neexistuje vytvoří se, pokud existuje prodlouží se mu platnost o 7 minut. Pokud nebude na tabulku `tableUniqueId` během 7 minut provedeno žádné volání, záznam zcela exspiruje z cache

## Frontend

Hlavní implementace frontend logiky se nachází v souboru [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js).

Funkce `bindEditorNotify` obsahuje 2 eventy, které jsou nabidnodvané a volají se při otevření a zavření editoru. Vstupním parametrem této funkce je EDITOR, ze kterého se získávají další potřebné informace jako unikátní název tabulky nebo id editovaného záznamu. Samotné volání funkce se provádí v souboru [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js). Zavolaný event při otevření editoru dále zavolá `callAddEditorLocking` funkci (plus nastaví 60 sekundový interval, který se přeruší až při zavření editoru) a event při zavření editoru zavolá `callRemoveEditorLocking` funkci.

Unikátní jméno tabulky se generuje ve funkci `getUniqueTableId(TABLE)` a je vytvořeno z URL adresy REST rozhraní (znak / je nahrazen za znak -, prefix `/admin/rest/` je odstraněn).

REST služba je volána při otevření dialogového okna editoru a následně každých 60 sekund.

### Přidání záznamu

Funkce `callAddEditorLocking` je volána událostí po otevření editoru. Pokud je hodnota `entityId` získaná z EDITORu jiná než `null` nebo -1 (což reprezentuje nový záznam), pomocí ajax volání se zavolá REST url pro přidání nového `editorLocking`. Návratová hodnota z Backendu je pole ostatních uživatelů, kteří právě editují tentýž záznam ve stejné tabulce. Pokud pole není prázdné, pomocí `WJ.notifyInfo` bude uživatel opakovaně upozorněn, který další uživatelé editují stejný záznam.

### Vymazání záznamu

Funkce `callRemoveEditorLocking` je volána událostí po zavření editoru. Pokud je hodnota `entityId` získaná z EDITORu jiná než `null` nebo -1 (což reprezentuje nový záznam), pomocí ajax volání se zavolá REST url pro vymazání již existujícího `editorLocking` záznamu.
