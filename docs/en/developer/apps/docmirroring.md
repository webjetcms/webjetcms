# Mirroring the structure

A user-friendly description of structure mirroring is in the [editor documentation](../../redactor/apps/docmirroring/README.md). This documentation contains only a technical description.

The basic java package is ```sk.iway.iwcm.components.structuremirroring``` and the directory for JSP components is ```/components/structuremirroring/```.

## Connection method

The linking of individual directories and pages uses the database attribute ```sync_id```, originally used for synchronization between WebJETs. However, such functionality is not currently used (export and import in XML format are used) and the given attribute was not used.

The value from the key generator is set to the attribute ```sync_id``` for linked directories and pages by calling ```PkeyGenerator.getNextValue("structuremirroring")```. The value ```sync_id``` of the directories or pages linked to each other must of course be the same. The value in ```sync_id``` allows you to search for linked directories/pages in the database.

## Initializing the connection

The link is set in the configuration variable ```structureMirroringConfig``` where each line contains a list of directory IDs (main directories of a given language mutation) that are to be linked. Setting the configuration variable triggers an event that ```SaveListener.handleConfSave``` listens to and then triggers ```MirroringService.checkRootGroupsConfig()```. Here the attribute ```syncId``` of the defined directories is checked, if it is not set it is set automatically.

The call ```MirroringService.isEnabled(int groupId)``` verifies whether structure mirroring is enabled for the specified directory ID.

## Linking directories

Directory linking is implemented by listening to the directory change event ```SaveListener.handleGroupSave```, which in turn calls ```GroupMirroringServiceV9.handleGroupSave```.

Note: the exception is for the directory named **New subdirectory** (translation key ```editor.dir.new_dir```), which is created in the tree structure by clicking on add new directory in the context menu. This would create directories ```Nový podadresár``` in other language mutations, which would have no practical meaning (since changing the directory name does not change the created mirror copies).

The ```WebjetEventType.ON_START``` event checks whether the directory already has ```syncId``` set. If not, we know that it is a new directory that is not yet mirrored and we set it to a new value of ```syncId```.

For event ```WebjetEventType.AFTER_SAVE```, i.e. after saving the directory according to ```syncId```, we look for mirrored directories. If none exist, new directories are created. The ```groupsDB.setGroup(mirror, false);``` API is used with a second ```false``` attribute, which **does not** raise the event again after saving the directory (which would cause recursion).

When a link exists, the parent directory of each mirrored version is checked. If it does not match the mapping of the new parent, the existing version is moved. Only after its location is updated is it determined whether to create the missing copies. This preserves the IDs during the move and prevents duplicate folders.

Saving a mirrored folder with the parameter `publishEvents=false` will not call `SaveGroupListener`. Therefore, `GroupMirroringServiceV9` will call `WebpagesService.regenerateUrl(groupId, currentUser, request, true)` separately in an HTTP request after the move. Automatically generated or inherited URLs of pages in the moved folder and its subfolders will be restored to the new location.

Verifying the sort priority is simple - the linked directories are checked and the sort priority value matches. If it is not the same, it is set.

In the event ```WebjetEventType.ON_DELETE```, calling ```GroupsDB.deleteGroup``` also deletes the mirrored directories. The event is processed before the source directory is moved to the trash. Mirroring is only performed if `MirroringService.isEnabled(groupId)` is true for the source directory. Therefore, the value of `syncId` retained in the trash alone is not sufficient; an exception may be, for example, the root directory whose ID was left in the mirroring configuration.

## Linking websites

The web page link is implemented in ```DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)```, which solves the problem of saving the web page in the page editor. The object ```DocDetails``` does not contain the value ```syncId```, so in the first step the value is set to the object (according to ```docId```).

Similar to the directory, a new value is generated for a page that does not have ```syncId``` set for the ```WebjetEventType.ON_START``` event.

The linked pages are retrieved at the ```WebjetEventType.AFTER_SAVE``` event. If none exist, they are created by calling ```DocDB.saveDoc(mirror, false)```, the ```false``` attribute solves the recursion problem (does not call the events again). The only problem that may arise is rights and approval, the current implementation does not solve this. If the editor does not have rights to another language mutation, the website will be created anyway.

If the page being saved already has linked pages, similar to a directory, the existing versions are first moved to the correct folders. Their new location is taken into account when detecting missing copies, so that another page is not created instead of the move. The sorting priority is then checked.

When saving a working version (`doc.getEditorFields().isRequestPublish() == false`), translation of the content of mirrored pages with `available=true` is skipped. Unpublished versions can be translated according to the existing rules for `structureMirroringAutoTranslatorLogin`. When publishing, these rules are also evaluated for published versions. In the old procedure, via `EditorForm`, `EditorDB` passes the actual publishing status to the `AFTER_SAVE` event and `SaveListener.handleEditorSave` passes it to `DocEditorFields`.

Page deletion is detected at ```SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)```. On event ```WebjetEventType.ON_DELETE```, a list of mirrored pages is retrieved and `EditorService.deleteWebpage(syncedDoc, false)` is called for each. Mirroring must be active for the source page folder.

Before invoking `ON_DELETE`, `EditorService.deleteWebpageLogic` through `checkPermissions(currentUser, mirror, true)` checks for permissions to all mirrored pages that have not yet been deleted in this request. If the check fails, deletion of the source page and its mirrored versions will not begin. The check is performed in a branch that can already perform the deletion; requests awaiting approval continue to use the approval process. `GroupsRestController.deleteItem` also requires permission `deleteDir` when deleting a folder.

`DeleteRequestContext` records completed deletions, including moving to the trash, in the attributes of the current request. The key distinguishes the item type and its ID. `EditorService` and `GroupsDB` record items only after the deletion is complete; a failed operation or an approval request is not considered a deletion. When selecting multiple mirrored items in bulk, items that have already been processed are skipped, so they are not deleted a second time after being moved to the trash.

## Preview mirrored items before deleting

The `sk.iway.iwcm.editor.service.DeleteMirrorsService` service provides data for the deletion confirmation window. It only retrieves the data, the deletion itself continues to execute the existing procedure via `ON_DELETE` events.

| Requirement | Result |
| --- | --- |
| `POST /admin/rest/web-pages/deletion-preview` | Directly mirrored versions of selected websites. |
| `POST /admin/rest/groups/deletion-preview` | Directly mirrored folders without a breakdown of their pages and subfolders. |

Both requests accept the form parameter `ids`, i.e. a positive ID separated by a comma. They require login, permission `menuWebpages`, and access to the selected items. For pages, `EditorService.isPageEditable(user, doc, true)` is verified, for folders, permissions `editDir`, `deleteDir`, and `GroupsDB.isGroupEditable`. In MultiWeb mode, the tenant domain is also checked.

The response `DeleteMirrorsResult` contains:

- `items` – list of items with fields `id`, `fullPath` and `domainName`. Selected items and duplicates are excluded.
- `hiddenMirrors` – value `true` if there is also a mirrored item whose path the user is not allowed to see. The client displays a general warning without identifying it.

Successfully loading the preview does not confirm the permission to delete all mirrored pages. After loading, the client will allow confirmation even at `hiddenMirrors=true`, but deleting the page itself will still perform the above-mentioned rights check on all its versions.

Preview is based on `syncId > 1` and `MirroringService.isEnabled(...)` for the source item, as is deletion. An invalid ID is rejected with a status of `400`, a non-existent item with a status of `404`, and an inaccessible source item with a status of `403`.

The `bindDeleteMirrors` function in `admin/v9/src/js/pages/web-pages-list/delete-mirrors.js` blocks the deletion confirmation during loading. It keeps it blocked on request error or invalid response format. After closing or reopening the window, the old request is canceled and its delayed response is ignored.

## Cloning and unmirroring

`CloneStructureService` stores a temporary mapping in the attribute `RequestBean` named `structureMirroringConfig` during cloning. The methods `MirroringService.isEnabled` and `getRootIds` will use this mapping only in the given request. `MirroringService.isCloneAction()` will recognize cloning by the presence of the attribute. The validated source and target folder IDs are read from the parameters `srcGroupId` and `destGroupId` in `RequestBean`.

Cloning does not change the global values ​​`structureMirroringDisabledOnCreate` or `syncGroupAndWebpageTitle`. Mirror services will preserve the display of source pages and the way folders are displayed in the menu when cloning; `GroupsService.canSyncTitle` will disable the synchronization of folder names with the main page during this request. Concurrent requests use the normal settings.

The original attribute value is restored in the `finally` block. At `keepMirroring=true`, the mapping is then merged with the current shared configuration and saved via `ConfDB.setName`. The `saveMirroringConfig` method is synchronized within a single JVM so that concurrent clones do not overwrite each other's added mappings. At `keepMirroring=false`, the shared configuration is not changed; `syncId` is removed in the target and also in the source, if the source did not have its own `syncId` before cloning.

`POST /apps/clone_structure/admin/cancel_sync` accepts the parameter `rootGroupId` and requires the right `cmp_clone_structure`. An invalid or non-existent ID is rejected with the status `400`. Before calling `MirroringService.clearSyncId`, `GroupsDB.isGroupEditable` is checked for the entire subtree including the root; with `multiDomainEnabled=true` also for the membership in the current domain. With `adminCheckUserGroups=true`, `DocDB.canAccess(doc, user, true)` is additionally checked for each page. In case of insufficient rights or an incorrect domain, `403` is returned without changing the links.

## Mirroring in the administration tree structure

`GroupsTreeService` adds the list `mirrors` to the nodes `GroupsJsTreeItem`. Each link contains `groupId`, `language` and `path`. The data is added only to the main tree of web pages, when the **Show mirroring** option is enabled, on first rendering or when requested with `showMirrorLinks=true`, and only to users with `menuWebpages` and `cmp_mirroring` rights. The system tree, trash and folder selection fields do not receive these links.

Links are searched by `syncId` ; target folders are filtered by domain, trash, and permissions. The language is specified by `GroupMirroringServiceV9.getLanguage`, normalized to lowercase with a hyphen, and the code `cz` is changed to `cs`. Links are sorted by language and ID. Both the folder index and search results are shared within a single HTTP request, so the next request will re-evaluate the current permissions and data.

The display of both language links and diagnostic icons is controlled by the user setting `showMirrorings` in `jstreeSettings_web-pages-list` ; it is passed in the parameter `showMirrorings` when loading the tree. The `sk.iway.iwcm.doc.mirroring.rest.MirroringService.getGroupProblems(request)` method uses the same checks as the mirror table and its own service instance for each call. Multiple mapping takes precedence over uneven nesting. Diagnostics are passed in `mirroringProblem` with the fields `icon` and `title`. The frontend displays it to the right before the language links; the regular folder icon does not change. Folders with the icon `ti ti-folder-x`, which indicates restricted access, do not receive diagnostics.

The `sk.iway.iwcm.doc.mirroring.rest.MirroringService` service for administrative tables has a scope of `@RequestScope`. The domain, operation type, languages ​​found, language mappings to fields, and validation errors are only stored during a single request so that they are not carried over between concurrent operations.

## Forcing tree structure to be restored

If a change in the structure requires a tree structure to be restored, the following attribute can be set in the events:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

This is then validated in the REST services after the save is complete and a tree structure refresh is invoked. The call via ```RequestBean``` was used because it is available throughout ```requestu``` and is statically available.
