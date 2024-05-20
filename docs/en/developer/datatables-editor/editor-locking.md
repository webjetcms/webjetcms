# Conflict notice

Conflict notification provides functionality that alerts the user while editing a record if another user is also editing the same record in the same table.

![](editor-locking.png)

## Backend

The implementation of the backend logic can be found in the file [EditorLockingRestController.java](../../../src/main/java/sk/iway/iwcm/system/datatable/editorlocking/EditorLockingRestController.java). The REST url of this service is `/admin/rest/editorlocking`. This controller takes care of adding and deleting `editoLocking` records from the memory cache. In the cache, these records are stored as a list `EditorLockingBean` entities for each table separately (a separate cache record), where each entity represents the editing of one record by a particular user.

```java
@RestController
@RequestMapping("/admin/rest/editorlocking")
@ResponseBody
public class EditorLockingRestController {
```

### Adding an entry

After calling the REST url `/admin/rest/editorlocking/open/{entityId}/{tableUniqueId}` an object containing a list of all `EditorLockingBean` entities by `tableUniqueId` (if it does not exist in memory, a new list is created) by calling `getCacheList(tableUniqueId);`. It is checked whether it already contains the entity according to `entityId` a `userId`. If not, a new `EditorLockingBean` entity to be added to the list. Before saving, the expired entities are removed from the field. `editorLocking` Records. The method returns a list of other users who edit the same record (or an empty field).

```java
@GetMapping({ "/open/{entityId}/{tableUniqueId}" })
public List<UserDto> addEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```

### Deleting an entry

After calling the REST url `/admin/rest/editorlocking/close/{entityId}/{tableUniqueId}` an object containing a list of all `EditorLockingBean` entities by calling `getCacheList(tableUniqueId);`. According to the parameters, the `EditorLockingBean` in the list and removed.

```java
@GetMapping({ "/close/{entityId}/{tableUniqueId}" })
public void removeEdit(
    @PathVariable("entityId") int entityId,
    @PathVariable("tableUniqueId") String tableUniqueId,
    HttpServletRequest request) {
```

The entry is remembered in the saved list for 2 minutes (defined in the constant `CACHE_EXPIRE_MINUTES`). From the user interface, the service is `addEdit` called every minute, if the user just simply closes the window without closing the editor dialog, the record will expire after 2 minutes.

### Caching

The list of users editing the same table is stored in `Cache` object with key `"editor.locking-"+tableUniqueId`. The logic is in the method `private List<EditorLockingBean> getCacheList(String tableUniqueId)`. If for a given `tableUniqueId` the cached list does not exist, if it exists it is extended by 7 minutes. If there is no table `tableUniqueId` no call made within 7 minutes, the record is completely expired from the cache

## Frontend

The main implementation of the frontend logic is in the file [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js).

Feature `bindEditorNotify` contains 2 events, which are set and called when the editor opens and closes. The input parameter to this funck is EDITOR, from which other necessary information is obtained such as the unique table name or id of the record being edited. The function call itself is made in the file [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js). The called event will further call the editor when the editor is opened `callAddEditorLocking` function (plus sets a 60 second interval that is interrupted only when the editor is closed) and event calls when the editor is closed `callRemoveEditorLocking` function.

The unique table name is generated in the function `getUniqueTableId(TABLE)` and is created from the URL of the REST interface (the / character is replaced by the - character, the prefix `/admin/rest/` is removed).

The REST service is called when the editor dialog is opened and every 60 seconds thereafter.

### Adding an entry

Feature `callAddEditorLocking` is called by an event after the editor is opened. If the value is `entityId` obtained from EDITOR other than `null` or -1 (representing a new record), an ajax call will call the REST url to add the new `editorLocking`. The return value from the Backend is an array of other users who are currently editing the same record in the same table. If the field is not empty, using the `WJ.notifyInfo` the user will be repeatedly notified which other users are editing the same record.

### Deleting an entry

Feature `callRemoveEditorLocking` is called by an event after the editor is closed. If the value is `entityId` obtained from EDITOR other than `null` or -1 (representing a new record), an ajax call will call the REST url to delete the existing `editorLocking` of record.
