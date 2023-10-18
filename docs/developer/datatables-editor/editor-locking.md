# Upozornenie o konflikte

Upozornenie o konflikte zabezpečuje funkcionalitu, ktorá upozorní používateľa počas editácie záznamu, ak aj iný používateľ edituje rovnaký záznam v rovnakej tabuľke.

![](editor-locking.png)

## Backend

Implementácia backend logiky sa nachádza v súbore [EditorLockingRestController.java](../../../src/main/java/sk/iway/iwcm/system/datatable/editorlocking/EditorLockingRestController.java). REST url tejto služby je ```/admin/rest/editorlocking```. Tento controller sa stará o pridávanie a mazanie ```editoLocking``` záznamov z cache pamäte. V cache sú tieto záznamy uložené ako zoznam ```EditorLockingBean``` entít pre každú tabuľku samostatne (samostatný cache záznam), kde každá entita predstavuje editovanie jedného záznamu konkrétnym používateľom.

```java
@RestController
@RequestMapping("/admin/rest/editorlocking")
@ResponseBody
public class EditorLockingRestController {
```

### Pridanie záznamu

Po zavolaní REST url ```/admin/rest/editorlocking/open/{entityId}/{tableUniqueId}``` sa z cache pamäte získa objekt obsahujúci zoznam všetkých ```EditorLockingBean``` entít podľa ```tableUniqueId``` (ak v pamäti neexistuje vytvorí sa nový zoznam) volaním ```getCacheList(tableUniqueId);```. Overí sa či už obsahuje entitu podľa ```entityId``` a ```userId```. Ak nie vytvorí sa nová ```EditorLockingBean``` entita, ktorá sa do zoznamu pridá. Pred uložením sa z poľa odstránia exspirované ```editorLocking``` záznamy. Metóda vráti zoznam ostatných používateľov, ktorý editujú rovnaký záznam (alebo prázdne pole).

```java
@GetMapping({ "/open/{entityId}/{tableUniqueId}" })
public List<UserDto> addEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```
### Vymazanie záznamu

Po zavolaní REST url ```/admin/rest/editorlocking/close/{entityId}/{tableUniqueId}``` sa z cache pamäte získa objekt obsahujúci zoznam všetkých ```EditorLockingBean``` entít volaním ```getCacheList(tableUniqueId);```. Podľa parametrov sa vyhľadá daný ```EditorLockingBean``` v zozname a odstráni sa.

```java
@GetMapping({ "/close/{entityId}/{tableUniqueId}" })
public void removeEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```

Záznam je v uloženom zozname zapamätaný na 2 minúty (definované v konštante ```CACHE_EXPIRE_MINUTES```). Z používateľského rozhrania je služba ```addEdit``` volaná každú minútu, ak používateľ len jednoducho zatvorí okno bez zatvorenia dialógového okna editora záznam teda exspiruje po 2 minútach.

### Ukladanie do cache

Zoznam používateľov editujúcich rovnakú tabuľku sa ukladá do ```Cache``` objektu s kľúčom ```"editor.locking-"+tableUniqueId```. Logika je v metóde ```private List<EditorLockingBean> getCacheList(String tableUniqueId)```. Ak pre danú ```tableUniqueId``` zoznam v cache neexistuje vytvorí sa, ak existuje predĺži sa mu platnosť o 7 minút. Ak nebude na tabuľku ```tableUniqueId``` počas 7 minút vykonané žiadne volanie, záznam úplne exspiruje z cache

## Frontend

Hlavná implementácia frontend logiky sa nachádza v súbore [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js).

Funkcia ```bindEditorNotify``` obsahuje 2 eventy, ktoré sú nabidnodvané a volajú sa pri otvorení a zatvoní editora. Vstupným parametrom tejto funckie je EDITOR, z ktorého sa získavjú ďalšie potrebné informácie ako unikátny názov tabuľky alebo id editovaného záznamu. Samotné volanie funkcie sa vykonáva v súbore [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js). Zavolaný event pri otvorení editora ďalej zavolá ```callAddEditorLocking``` funkciu (plus nastaví 60 sekundový interval, ktorý sa preruší až pri zatvorení editora) a event pri zatvorení editora zavolá ```callRemoveEditorLocking```  funkciu.

Unikátne meno tabuľky sa generuje vo funkcii ```getUniqueTableId(TABLE)``` a je vytvorené z URL adresy REST rozhrania (znak / je nahradený za znak -, prefix ```/admin/rest/``` je odstránený).

REST služba je volaná pri otvorení dialógového okna editora a následne každých 60 sekúnd.

### Pridanie záznamu

Funkcia ```callAddEditorLocking``` je volaná udalosťou po otvorení editora. Ak je hodnota ```entityId``` získaná z EDITOR-a iná ako ```null``` alebo -1 (čo reprezentuje nový záznam), pomocou ajax volania sa zavolá REST url pre pridanie nového ```editorLocking```. Návratová hodnota z Backend-u je pole ostatných používateľov, ktorý práve editujú ten istý záznam v rovnakej tabuľke. Ak pole nie je prázdne, pomocou ```WJ.notifyInfo``` bude používateľ opakovane upozornený, ktorý ďalší používatelia editujú rovnaký záznam.

### Vymazanie záznamu

Funkcia ```callRemoveEditorLocking```  je volaná udalosťou po zatvorení editora. Ak je hodnota ```entityId``` získaná z EDITOR-a iná ako ```null``` alebo -1 (čo reprezentuje nový záznam), pomocou ajax volania sa zavolá REST url pre vymazanie už existujúceho ```editorLocking``` záznamu.